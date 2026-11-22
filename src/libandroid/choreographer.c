/*
 * AChoreographer (NDK choreographer.h, AOSP frameworks/native libs/nativedisplay/AChoreographer.cpp and
 * libs/gui/Choreographer.cpp): one choreographer per looper thread, frame callbacks run on that thread's
 * looper at the next vsync after they are due.
 *
 * There is no display vsync here. The Java Choreographer of ATL runs its frames at getRefreshRate() = 60 Hz
 * without vsync; this one takes the same rate as its vsync: a timerfd on CLOCK_MONOTONIC (the clock of
 * System.nanoTime() and of AOSP's systemTime(SYSTEM_TIME_MONOTONIC)) that fires on the 60 Hz grid, and is
 * only armed while callbacks are pending, like AOSP's scheduleVsync(). A frame has the one timeline this
 * clock can tell (as in ATL's Java Choreographer.FrameData): presented one frame interval after the frame.
 */

#include <inttypes.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/eventfd.h>
#include <sys/timerfd.h>
#include <time.h>
#include <unistd.h>

#include "looper.h"

#define ALOOPER_POLL_CALLBACK (-2)

#define REFRESH_RATE 60 // the rate of ATL's Java Choreographer (getRefreshRate())
#define VSYNC_PERIOD_NANOS (1000000000LL / REFRESH_RATE)
#define FRAME_TIMELINES_CAPACITY 7 // VsyncEventData::kFrameTimelinesCapacity

typedef int64_t AVsyncId;
typedef struct AChoreographer AChoreographer;
typedef struct AChoreographerFrameCallbackData AChoreographerFrameCallbackData;
typedef void (*AChoreographer_frameCallback)(long frameTimeNanos, void *data);
typedef void (*AChoreographer_frameCallback64)(int64_t frameTimeNanos, void *data);
typedef void (*AChoreographer_vsyncCallback)(const AChoreographerFrameCallbackData *callbackData, void *data);
typedef void (*AChoreographer_refreshRateCallback)(int64_t vsyncPeriodNanos, void *data);

struct frame_callback {
	struct frame_callback *next;
	AChoreographer_frameCallback callback;
	AChoreographer_frameCallback64 callback64;
	AChoreographer_vsyncCallback vsync_callback;
	void *data;
	int64_t due_time;
};

struct refresh_rate_callback {
	struct refresh_rate_callback *next;
	AChoreographer_refreshRateCallback callback;
	void *data;
	bool first_callback_fired;
};

struct frame_timeline {
	AVsyncId vsync_id;
	int64_t expected_presentation_time;
	int64_t deadline;
};

struct AChoreographerFrameCallbackData {
	int64_t frame_time_nanos;
	size_t frame_timelines_length;
	size_t preferred_frame_timeline_index;
	struct frame_timeline frame_timelines[FRAME_TIMELINES_CAPACITY];
	const AChoreographer *choreographer;
};

struct AChoreographer {
	ALooper *looper;
	int timer_fd; // the vsync
	int event_fd; // refresh rate updates, AOSP's MSG_HANDLE_REFRESH_RATE_UPDATES
	pthread_mutex_t lock;
	struct frame_callback *frame_callbacks; // sorted by due time
	struct refresh_rate_callback *refresh_rate_callbacks;
	int64_t armed_vsync; // the vsync timer_fd is set to, 0 when not armed
	bool in_callback;
};

static __thread AChoreographer *thread_choreographer;

static int64_t now_nanos(void)
{
	struct timespec ts;
	clock_gettime(CLOCK_MONOTONIC, &ts);
	return ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

// the lock must be held
static void schedule_vsync_locked(AChoreographer *choreographer)
{
	if (!choreographer->frame_callbacks)
		return;
	int64_t due = choreographer->frame_callbacks->due_time;
	int64_t now = now_nanos();
	if (due < now)
		due = now;
	int64_t vsync = (due / VSYNC_PERIOD_NANOS + 1) * VSYNC_PERIOD_NANOS;
	if (choreographer->armed_vsync && choreographer->armed_vsync <= vsync)
		return;
	struct itimerspec spec = {.it_value = {.tv_sec = vsync / 1000000000LL, .tv_nsec = vsync % 1000000000LL}};
	timerfd_settime(choreographer->timer_fd, TFD_TIMER_ABSTIME, &spec, NULL);
	choreographer->armed_vsync = vsync;
}

static int vsync_event(int fd, int events, void *data)
{
	AChoreographer *choreographer = data;
	uint64_t expirations;
	if (read(fd, &expirations, sizeof(expirations)) < 0)
		return 1; // the timer was set again since it fired

	int64_t now = now_nanos();
	int64_t vsync = now / VSYNC_PERIOD_NANOS * VSYNC_PERIOD_NANOS;
	struct frame_callback *callbacks = NULL;
	struct frame_callback **tail = &callbacks;

	pthread_mutex_lock(&choreographer->lock);
	choreographer->armed_vsync = 0;
	while (choreographer->frame_callbacks && choreographer->frame_callbacks->due_time < now) {
		*tail = choreographer->frame_callbacks;
		choreographer->frame_callbacks = choreographer->frame_callbacks->next;
		tail = &(*tail)->next;
	}
	*tail = NULL;
	pthread_mutex_unlock(&choreographer->lock);

	AChoreographerFrameCallbackData frame_data = {
		.frame_time_nanos = vsync,
		.frame_timelines_length = 1,
		.preferred_frame_timeline_index = 0,
		.frame_timelines = {{
		    .vsync_id = vsync / VSYNC_PERIOD_NANOS,
		    .expected_presentation_time = vsync + VSYNC_PERIOD_NANOS,
		    .deadline = vsync + VSYNC_PERIOD_NANOS,
		}},
		.choreographer = choreographer,
	};
	while (callbacks) {
		struct frame_callback *cb = callbacks;
		callbacks = cb->next;
		if (cb->vsync_callback) {
			choreographer->in_callback = true;
			cb->vsync_callback(&frame_data, cb->data);
			choreographer->in_callback = false;
		} else if (cb->callback64) {
			cb->callback64(vsync, cb->data);
		} else if (cb->callback) {
			cb->callback(vsync, cb->data);
		}
		free(cb);
	}

	pthread_mutex_lock(&choreographer->lock);
	schedule_vsync_locked(choreographer);
	pthread_mutex_unlock(&choreographer->lock);
	return 1;
}

// Choreographer::handleRefreshRateUpdates(): callbacks hear the period once when they register, and again when it changes (never here)
static int refresh_rate_event(int fd, int events, void *data)
{
	AChoreographer *choreographer = data;
	uint64_t count;
	if (read(fd, &count, sizeof(count)) < 0)
		return 1;

	for (;;) {
		AChoreographer_refreshRateCallback callback = NULL;
		void *callback_data = NULL;
		pthread_mutex_lock(&choreographer->lock);
		for (struct refresh_rate_callback *cb = choreographer->refresh_rate_callbacks; cb; cb = cb->next) {
			if (!cb->first_callback_fired) {
				cb->first_callback_fired = true;
				callback = cb->callback;
				callback_data = cb->data;
				break;
			}
		}
		pthread_mutex_unlock(&choreographer->lock);
		if (!callback)
			break;
		callback(VSYNC_PERIOD_NANOS, callback_data);
	}
	return 1;
}

AChoreographer *AChoreographer_getInstance(void)
{
	if (thread_choreographer)
		return thread_choreographer;

	ALooper *looper = ALooper_forThread();
	if (!looper) {
		fprintf(stderr, "AChoreographer: No looper prepared for thread\n");
		return NULL;
	}
	AChoreographer *choreographer = calloc(1, sizeof(AChoreographer));
	choreographer->timer_fd = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK | TFD_CLOEXEC);
	choreographer->event_fd = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
	if (choreographer->timer_fd < 0 || choreographer->event_fd < 0) {
		fprintf(stderr, "AChoreographer: Failed to initialize\n");
		if (choreographer->timer_fd >= 0)
			close(choreographer->timer_fd);
		if (choreographer->event_fd >= 0)
			close(choreographer->event_fd);
		free(choreographer);
		return NULL;
	}
	pthread_mutex_init(&choreographer->lock, NULL);
	ALooper_acquire(looper);
	choreographer->looper = looper;
	ALooper_addFd(looper, choreographer->timer_fd, ALOOPER_POLL_CALLBACK, ALOOPER_EVENT_INPUT, vsync_event, choreographer);
	ALooper_addFd(looper, choreographer->event_fd, ALOOPER_POLL_CALLBACK, ALOOPER_EVENT_INPUT, refresh_rate_event, choreographer);
	thread_choreographer = choreographer;
	return choreographer;
}

// Choreographer::postFrameCallbackDelayed(): callbacks of the same due time run in the order they were posted
static void post_frame_callback(AChoreographer *choreographer, AChoreographer_frameCallback cb, AChoreographer_frameCallback64 cb64,
                                AChoreographer_vsyncCallback vsync_callback, void *data, int64_t delay)
{
	struct frame_callback *callback = malloc(sizeof(struct frame_callback));
	*callback = (struct frame_callback){
		.callback = cb,
		.callback64 = cb64,
		.vsync_callback = vsync_callback,
		.data = data,
		.due_time = now_nanos() + delay,
	};
	pthread_mutex_lock(&choreographer->lock);
	struct frame_callback **pos = &choreographer->frame_callbacks;
	while (*pos && (*pos)->due_time <= callback->due_time)
		pos = &(*pos)->next;
	callback->next = *pos;
	*pos = callback;
	schedule_vsync_locked(choreographer);
	pthread_mutex_unlock(&choreographer->lock);
}

void AChoreographer_postFrameCallback(AChoreographer *choreographer, AChoreographer_frameCallback callback, void *data)
{
	post_frame_callback(choreographer, callback, NULL, NULL, data, 0);
}

void AChoreographer_postFrameCallbackDelayed(AChoreographer *choreographer, AChoreographer_frameCallback callback, void *data, long delayMillis)
{
	post_frame_callback(choreographer, callback, NULL, NULL, data, delayMillis * 1000000LL);
}

void AChoreographer_postFrameCallback64(AChoreographer *choreographer, AChoreographer_frameCallback64 callback, void *data)
{
	post_frame_callback(choreographer, NULL, callback, NULL, data, 0);
}

void AChoreographer_postFrameCallbackDelayed64(AChoreographer *choreographer, AChoreographer_frameCallback64 callback, void *data, uint32_t delayMillis)
{
	post_frame_callback(choreographer, NULL, callback, NULL, data, delayMillis * 1000000LL);
}

void AChoreographer_postVsyncCallback(AChoreographer *choreographer, AChoreographer_vsyncCallback callback, void *data)
{
	post_frame_callback(choreographer, NULL, NULL, callback, data, 0);
}

void AChoreographer_registerRefreshRateCallback(AChoreographer *choreographer, AChoreographer_refreshRateCallback callback, void *data)
{
	pthread_mutex_lock(&choreographer->lock);
	for (struct refresh_rate_callback *cb = choreographer->refresh_rate_callbacks; cb; cb = cb->next) {
		if (cb->callback == callback && cb->data == data) { // don't re-add callbacks
			pthread_mutex_unlock(&choreographer->lock);
			return;
		}
	}
	struct refresh_rate_callback **tail = &choreographer->refresh_rate_callbacks;
	while (*tail)
		tail = &(*tail)->next;
	*tail = calloc(1, sizeof(struct refresh_rate_callback));
	(*tail)->callback = callback;
	(*tail)->data = data;
	pthread_mutex_unlock(&choreographer->lock);
	// the display manager tells every choreographer the current refresh rate when one registers
	eventfd_write(choreographer->event_fd, 1);
}

void AChoreographer_unregisterRefreshRateCallback(AChoreographer *choreographer, AChoreographer_refreshRateCallback callback, void *data)
{
	pthread_mutex_lock(&choreographer->lock);
	struct refresh_rate_callback **pos = &choreographer->refresh_rate_callbacks;
	while (*pos) {
		if ((*pos)->callback == callback && (*pos)->data == data) {
			struct refresh_rate_callback *cb = *pos;
			*pos = cb->next;
			free(cb);
		} else {
			pos = &(*pos)->next;
		}
	}
	pthread_mutex_unlock(&choreographer->lock);
}

// LOG_ALWAYS_FATAL_IF in AOSP
static const AChoreographerFrameCallbackData *checked(const AChoreographerFrameCallbackData *data)
{
	if (!data->choreographer->in_callback) {
		fprintf(stderr, "AChoreographer: Data is only valid in callback\n");
		abort();
	}
	return data;
}

static const struct frame_timeline *checked_timeline(const AChoreographerFrameCallbackData *data, size_t index)
{
	if (index >= FRAME_TIMELINES_CAPACITY) {
		fprintf(stderr, "AChoreographer: Index out of bounds\n");
		abort();
	}
	return &checked(data)->frame_timelines[index];
}

int64_t AChoreographerFrameCallbackData_getFrameTimeNanos(const AChoreographerFrameCallbackData *data)
{
	return checked(data)->frame_time_nanos;
}

size_t AChoreographerFrameCallbackData_getFrameTimelinesLength(const AChoreographerFrameCallbackData *data)
{
	return checked(data)->frame_timelines_length;
}

size_t AChoreographerFrameCallbackData_getPreferredFrameTimelineIndex(const AChoreographerFrameCallbackData *data)
{
	return checked(data)->preferred_frame_timeline_index;
}

AVsyncId AChoreographerFrameCallbackData_getFrameTimelineVsyncId(const AChoreographerFrameCallbackData *data, size_t index)
{
	return checked_timeline(data, index)->vsync_id;
}

int64_t AChoreographerFrameCallbackData_getFrameTimelineExpectedPresentationTimeNanos(const AChoreographerFrameCallbackData *data, size_t index)
{
	return checked_timeline(data, index)->expected_presentation_time;
}

int64_t AChoreographerFrameCallbackData_getFrameTimelineDeadlineNanos(const AChoreographerFrameCallbackData *data, size_t index)
{
	return checked_timeline(data, index)->deadline;
}
