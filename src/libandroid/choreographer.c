/*
 * AChoreographer (android/choreographer.h): frame callbacks on the thread of the choreographer's looper. As in
 * AOSP, a thread gets its choreographer only if it has a looper, and callbacks run from that looper. Frames
 * follow the display's refresh rate (GDK monitor, else 60 Hz); there is no vsync signal of the compositor here,
 * so the frame clock is a timer at that rate.
 */

#include <errno.h>
#include <gdk/gdk.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/timerfd.h>
#include <time.h>
#include <unistd.h>

#include "looper.h"

#define ALOOPER_POLL_CALLBACK (-2)
ALooper *ALooper_forThread(void);
void ALooper_acquire(ALooper *looper);

typedef struct AChoreographer AChoreographer;
typedef struct AChoreographerFrameCallbackData AChoreographerFrameCallbackData;
typedef int64_t AVsyncId;
typedef void (*AChoreographer_frameCallback)(long frameTimeNanos, void *data);
typedef void (*AChoreographer_frameCallback64)(int64_t frameTimeNanos, void *data);
typedef void (*AChoreographer_vsyncCallback)(const AChoreographerFrameCallbackData *callbackData, void *data);
typedef void (*AChoreographer_refreshRateCallback)(int64_t vsyncPeriodNanos, void *data);

enum callback_kind { FRAME, FRAME64, VSYNC };

struct frame_callback {
	enum callback_kind kind;
	void *func;
	void *data;
	int64_t due_nanos;
	struct frame_callback *next;
};

struct refresh_rate_callback {
	AChoreographer_refreshRateCallback func;
	void *data;
	struct refresh_rate_callback *next;
};

struct AChoreographer {
	ALooper *looper;
	int timer_fd;
	int64_t period_nanos;
	pthread_mutex_t lock;
	struct frame_callback *callbacks;
	struct refresh_rate_callback *refresh_rate_callbacks;
	AVsyncId vsync_id;
};

struct AChoreographerFrameCallbackData {
	int64_t frame_time_nanos;
	int64_t period_nanos;
	AVsyncId vsync_id;
};

static __thread AChoreographer *thread_choreographer;

static int64_t now_nanos(void)
{
	struct timespec ts;
	clock_gettime(CLOCK_MONOTONIC, &ts);
	return (int64_t)ts.tv_sec * 1000000000 + ts.tv_nsec;
}

static int64_t display_period_nanos(void)
{
	int refresh_mhz = 0;
	GdkDisplay *display = gdk_display_get_default();
	if (display) {
		GListModel *monitors = gdk_display_get_monitors(display);
		GdkMonitor *monitor = g_list_model_get_n_items(monitors) ? g_list_model_get_item(monitors, 0) : NULL;
		if (monitor) {
			refresh_mhz = gdk_monitor_get_refresh_rate(monitor);
			g_object_unref(monitor);
		}
	}
	return refresh_mhz > 0 ? (int64_t)1000000000000 / refresh_mhz : 1000000000 / 60;
}

/* the next frame at or after `when`, on the frame grid */
static void arm_timer(AChoreographer *choreographer, int64_t when)
{
	int64_t now = now_nanos();
	if (when < now)
		when = now;
	int64_t frame = (when + choreographer->period_nanos - 1) / choreographer->period_nanos * choreographer->period_nanos;
	struct itimerspec spec = { .it_value = { frame / 1000000000, frame % 1000000000 } };
	timerfd_settime(choreographer->timer_fd, TFD_TIMER_ABSTIME, &spec, NULL);
}

static int on_frame(int fd, int events, void *data)
{
	AChoreographer *choreographer = data;
	uint64_t expirations;
	if (read(fd, &expirations, sizeof(expirations)) < 0 && errno != EAGAIN)
		return 1;

	int64_t frame_time = now_nanos();
	frame_time -= frame_time % choreographer->period_nanos;

	pthread_mutex_lock(&choreographer->lock);
	struct frame_callback *due = NULL, **due_tail = &due, **it = &choreographer->callbacks;
	while (*it) {
		if ((*it)->due_nanos <= frame_time + choreographer->period_nanos / 2) {
			struct frame_callback *cb = *it;
			*it = cb->next;
			cb->next = NULL;
			*due_tail = cb;
			due_tail = &cb->next;
		} else {
			it = &(*it)->next;
		}
	}
	int64_t next_due = INT64_MAX;
	for (struct frame_callback *cb = choreographer->callbacks; cb; cb = cb->next)
		if (cb->due_nanos < next_due)
			next_due = cb->due_nanos;
	AVsyncId vsync_id = ++choreographer->vsync_id;
	pthread_mutex_unlock(&choreographer->lock);

	struct AChoreographerFrameCallbackData callback_data = { frame_time, choreographer->period_nanos, vsync_id };
	while (due) {
		struct frame_callback *cb = due;
		due = cb->next;
		switch (cb->kind) {
			case FRAME:
				((AChoreographer_frameCallback)cb->func)((long)frame_time, cb->data);
				break;
			case FRAME64:
				((AChoreographer_frameCallback64)cb->func)(frame_time, cb->data);
				break;
			case VSYNC:
				((AChoreographer_vsyncCallback)cb->func)(&callback_data, cb->data);
				break;
		}
		free(cb);
	}

	pthread_mutex_lock(&choreographer->lock);
	for (struct frame_callback *cb = choreographer->callbacks; cb; cb = cb->next) /* posted by the callbacks */
		if (cb->due_nanos < next_due)
			next_due = cb->due_nanos;
	if (next_due != INT64_MAX)
		arm_timer(choreographer, next_due);
	pthread_mutex_unlock(&choreographer->lock);
	return 1;
}

AChoreographer *AChoreographer_getInstance(void)
{
	if (thread_choreographer)
		return thread_choreographer;
	ALooper *looper = ALooper_forThread();
	if (!looper)
		return NULL;
	AChoreographer *choreographer = calloc(1, sizeof(*choreographer));
	choreographer->looper = looper;
	choreographer->timer_fd = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK | TFD_CLOEXEC);
	choreographer->period_nanos = display_period_nanos();
	pthread_mutex_init(&choreographer->lock, NULL);
	ALooper_acquire(looper);
	ALooper_addFd(looper, choreographer->timer_fd, ALOOPER_POLL_CALLBACK, ALOOPER_EVENT_INPUT, on_frame, choreographer);
	thread_choreographer = choreographer;
	return choreographer;
}

static void post(AChoreographer *choreographer, enum callback_kind kind, void *func, void *data, int64_t delay_millis)
{
	struct frame_callback *cb = malloc(sizeof(*cb));
	*cb = (struct frame_callback){ kind, func, data, now_nanos() + delay_millis * 1000000, NULL };
	pthread_mutex_lock(&choreographer->lock);
	struct frame_callback **it = &choreographer->callbacks;
	while (*it)
		it = &(*it)->next;
	*it = cb;
	arm_timer(choreographer, cb->due_nanos);
	pthread_mutex_unlock(&choreographer->lock);
}

void AChoreographer_postFrameCallback(AChoreographer *choreographer, AChoreographer_frameCallback callback, void *data)
{
	post(choreographer, FRAME, callback, data, 0);
}

void AChoreographer_postFrameCallbackDelayed(AChoreographer *choreographer, AChoreographer_frameCallback callback, void *data, long delayMillis)
{
	post(choreographer, FRAME, callback, data, delayMillis);
}

void AChoreographer_postFrameCallback64(AChoreographer *choreographer, AChoreographer_frameCallback64 callback, void *data)
{
	post(choreographer, FRAME64, callback, data, 0);
}

void AChoreographer_postFrameCallbackDelayed64(AChoreographer *choreographer, AChoreographer_frameCallback64 callback, void *data, uint32_t delayMillis)
{
	post(choreographer, FRAME64, callback, data, delayMillis);
}

void AChoreographer_postVsyncCallback(AChoreographer *choreographer, AChoreographer_vsyncCallback callback, void *data)
{
	post(choreographer, VSYNC, callback, data, 0);
}

/* AOSP calls a new refresh rate callback with the current period, and again whenever it changes */
void AChoreographer_registerRefreshRateCallback(AChoreographer *choreographer, AChoreographer_refreshRateCallback callback, void *data)
{
	pthread_mutex_lock(&choreographer->lock);
	for (struct refresh_rate_callback *cb = choreographer->refresh_rate_callbacks; cb; cb = cb->next) {
		if (cb->func == callback && cb->data == data) {
			pthread_mutex_unlock(&choreographer->lock);
			return;
		}
	}
	struct refresh_rate_callback *cb = malloc(sizeof(*cb));
	*cb = (struct refresh_rate_callback){ callback, data, choreographer->refresh_rate_callbacks };
	choreographer->refresh_rate_callbacks = cb;
	int64_t period = choreographer->period_nanos;
	pthread_mutex_unlock(&choreographer->lock);
	callback(period, data);
}

void AChoreographer_unregisterRefreshRateCallback(AChoreographer *choreographer, AChoreographer_refreshRateCallback callback, void *data)
{
	pthread_mutex_lock(&choreographer->lock);
	for (struct refresh_rate_callback **it = &choreographer->refresh_rate_callbacks; *it; it = &(*it)->next) {
		if ((*it)->func == callback && (*it)->data == data) {
			struct refresh_rate_callback *cb = *it;
			*it = cb->next;
			free(cb);
			break;
		}
	}
	pthread_mutex_unlock(&choreographer->lock);
}

int64_t AChoreographerFrameCallbackData_getFrameTimeNanos(const AChoreographerFrameCallbackData *data)
{
	return data->frame_time_nanos;
}

/* one timeline: the next frame */
size_t AChoreographerFrameCallbackData_getFrameTimelinesLength(const AChoreographerFrameCallbackData *data)
{
	return 1;
}

size_t AChoreographerFrameCallbackData_getPreferredFrameTimelineIndex(const AChoreographerFrameCallbackData *data)
{
	return 0;
}

AVsyncId AChoreographerFrameCallbackData_getFrameTimelineVsyncId(const AChoreographerFrameCallbackData *data, size_t index)
{
	return data->vsync_id;
}

int64_t AChoreographerFrameCallbackData_getFrameTimelineExpectedPresentationTimeNanos(const AChoreographerFrameCallbackData *data, size_t index)
{
	return data->frame_time_nanos + data->period_nanos;
}

int64_t AChoreographerFrameCallbackData_getFrameTimelineDeadlineNanos(const AChoreographerFrameCallbackData *data, size_t index)
{
	return data->frame_time_nanos + data->period_nanos;
}
