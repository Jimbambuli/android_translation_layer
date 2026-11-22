/*
 * ASurfaceControl / ASurfaceTransaction (AOSP frameworks/base/native/android/surface_control.cpp) on ATL.
 *
 * There is no SurfaceFlinger: the surfaces of a window form a tree here, and applying a transaction shows a buffer
 * of it in the window. Supported is the window of a Surface of another process (Surface.Endpoint, the GPU process
 * of Chromium drawing into the browser's SurfaceView): its widget shows AHardwareBuffers (dma-bufs) as they are,
 * over the same socket messages as MediaCodec's frames (android_view_SurfaceView.h).
 *
 * Composition: the widget shows one buffer. Of the visible surfaces with a buffer, that is the one with the
 * largest destination (lowest z-order among equal ones), normally the primary plane; the others (overlays) are not
 * shown. Geometry other than this choice, transforms, colors and HDR metadata have no effect.
 *
 * Callbacks run on a thread of the window, like AOSP's binder thread: OnCommit right after apply, OnComplete once
 * the buffers the transaction replaced are released by the widget (so their release fence is -1, signalled), and
 * a buffer's release callback (setBufferWithRelease) when it is released.
 */

#include <errno.h>
#include <poll.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <jni.h>

#include "../api-impl-jni/widgets/android_view_SurfaceView.h"
#include "hardware_buffer.h"
#include "native_window.h"

void AHardwareBuffer_acquire(struct AHardwareBuffer *buffer);
void AHardwareBuffer_release(struct AHardwareBuffer *buffer);

/* LOG_ALWAYS_FATAL in AOSP */
static void fatal(const char *msg)
{
	fprintf(stderr, "%s\n", msg);
	abort();
}
#define CHECK_NOT_NULL(name) \
	if (!(name))         \
		fatal("nullptr passed as " #name " argument");

#define ASURFACE_TRANSACTION_VISIBILITY_HIDE 0
#define ASURFACE_TRANSACTION_VISIBILITY_SHOW 1

struct ASurfaceTransactionStats;
typedef void (*OnComplete)(void *context, struct ASurfaceTransactionStats *stats);
typedef void (*OnBufferRelease)(void *context, int release_fence_fd);

static GMutex lock; /* all surfaces, displays and pending callbacks */

/* a buffer slot of the widget (ATL_SURFACE_MSG_BUFFER index) */
struct slot {
	struct AHardwareBuffer *buffer; /* what the widget has at this index (a reference) */
	bool busy;                      /* presented, not yet released */
	uint64_t presented;             /* sequence number of the last PRESENT */
};

/* the window of a root surface */
struct display {
	int refcount;
	struct ANativeWindow *window;
	struct slot slots[ATL_SURFACE_MAX_BUFFERS];
	uint64_t seq;
	int shown;         /* slot shown last, -1 */
	bool stop;         /* the root surface is gone: the thread ends */
	bool warned;
	GThread *thread;
	struct pending *pending;
};

struct ASurfaceControl {
	int refcount;
	struct ASurfaceControl *parent; /* a reference; NULL for a root or a detached surface */
	struct display *display;        /* only on a root */
	bool visible;
	int32_t z;
	ARect destination; /* in the parent; empty: the buffer's size */
	struct AHardwareBuffer *buffer;
	OnBufferRelease release_cb;
	void *release_ctx;
	int64_t acquire_time;
	/* where the buffer is shown: the slot of display and its PRESENT sequence (slot -1: not shown) */
	struct display *shown_on;
	int shown_slot;
	uint64_t shown_seq;
};

enum {
	CHANGE_BUFFER = 1 << 0,
	CHANGE_VISIBILITY = 1 << 1,
	CHANGE_Z = 1 << 2,
	CHANGE_DESTINATION = 1 << 3,
	CHANGE_PARENT = 1 << 4,
};

struct change {
	struct ASurfaceControl *sc; /* a reference */
	unsigned mask;
	struct AHardwareBuffer *buffer; /* a reference */
	int fence;
	OnBufferRelease release_cb;
	void *release_ctx;
	bool visible;
	int32_t z;
	ARect destination;
	struct ASurfaceControl *parent; /* a reference */
};

struct callback {
	OnComplete func;
	void *context;
};

struct ASurfaceTransaction {
	struct change *changes;
	size_t n_changes;
	struct callback *complete, *commit;
	size_t n_complete, n_commit;
};

/* what a callback reports for one surface */
struct surface_stats {
	struct ASurfaceControl *sc;
	int64_t acquire_time;
};

struct ASurfaceTransactionStats {
	int64_t latch_time;
	bool completed;
	struct surface_stats *surfaces;
	size_t n_surfaces;
};

/* a callback waiting on the display's thread until slot `slot` of the display has released PRESENT `seq` */
struct pending {
	struct pending *next;
	int slot; /* -1: nothing to wait for */
	uint64_t seq;
	/* OnComplete/OnCommit with stats, or a buffer release callback */
	OnComplete func;
	void *context;
	struct ASurfaceTransactionStats *stats;
	OnBufferRelease release_cb;
	void *release_ctx;
	struct pending *after; /* waits for these too (all buffers a transaction replaced) */
};

static int64_t now_ns(void)
{
	struct timespec ts;
	clock_gettime(CLOCK_MONOTONIC, &ts);
	return (int64_t)ts.tv_sec * 1000000000 + ts.tv_nsec;
}

/* ---- surfaces ---- */

static struct ASurfaceControl *sc_new(struct ASurfaceControl *parent)
{
	struct ASurfaceControl *sc = calloc(1, sizeof(*sc));
	sc->refcount = 1;
	sc->visible = true; /* layers are shown unless hidden (eHidden is not set by createSurface()) */
	sc->shown_slot = -1;
	if (parent) {
		parent->refcount++;
		sc->parent = parent;
	}
	return sc;
}

static void display_unref(struct display *display);

/* with lock held */
static void sc_unref(struct ASurfaceControl *sc)
{
	if (!sc || --sc->refcount > 0)
		return;
	if (sc->display) {
		sc->display->stop = true;
		display_unref(sc->display);
	}
	AHardwareBuffer_release(sc->buffer);
	sc_unref(sc->parent);
	free(sc);
}

static struct ASurfaceControl *sc_root(struct ASurfaceControl *sc)
{
	while (sc->parent)
		sc = sc->parent;
	return sc;
}

/* shown: it and all its parents are visible, and it hangs below a window */
static bool sc_shown(struct ASurfaceControl *sc)
{
	for (; sc; sc = sc->parent)
		if (!sc->visible)
			return false;
		else if (sc->display)
			return true;
	return false;
}

/* ---- displays ---- */

static void display_unref(struct display *display)
{
	if (--display->refcount > 0)
		return;
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS; i++)
		AHardwareBuffer_release(display->slots[i].buffer);
	ANativeWindow_release(display->window);
	free(display);
}

static bool pending_ready(struct display *display, struct pending *p)
{
	for (; p; p = p->after)
		if (p->slot >= 0 && display->slots[p->slot].busy && display->slots[p->slot].presented == p->seq)
			return false;
	return true;
}

static void pending_free(struct pending *p)
{
	while (p) {
		struct pending *after = p->after;
		free(p);
		p = after;
	}
}

static void stats_free(struct ASurfaceTransactionStats *stats)
{
	if (!stats)
		return;
	for (size_t i = 0; i < stats->n_surfaces; i++)
		sc_unref(stats->surfaces[i].sc);
	free(stats->surfaces);
	free(stats);
}

/* runs the callbacks whose buffers were released (all of them when the display goes away) */
static void display_run_pending(struct display *display, bool all)
{
	g_mutex_lock(&lock);
	struct pending *ready = NULL, **link = &display->pending;
	while (*link) {
		struct pending *p = *link;
		if (all || pending_ready(display, p)) {
			*link = p->next;
			p->next = ready;
			ready = p;
		} else {
			link = &p->next;
		}
	}
	g_mutex_unlock(&lock);
	/* in the order they were queued */
	struct pending *ordered = NULL;
	while (ready) {
		struct pending *next = ready->next;
		ready->next = ordered;
		ordered = ready;
		ready = next;
	}
	while (ordered) {
		struct pending *p = ordered;
		ordered = p->next;
		if (p->func) {
			p->stats->completed = true;
			p->func(p->context, p->stats);
		} else if (p->release_cb) {
			p->release_cb(p->release_ctx, -1);
		}
		g_mutex_lock(&lock);
		stats_free(p->stats);
		g_mutex_unlock(&lock);
		pending_free(p);
	}
}

/* ANativeWindow_remoteRead(), with the window's remote_lock: the widget let go of a buffer */
static void display_message(void *data, const struct atl_surface_msg *msg)
{
	struct display *display = data;
	if (msg->type != ATL_SURFACE_MSG_RELEASE || msg->index >= ATL_SURFACE_MAX_BUFFERS)
		return;
	g_mutex_lock(&lock);
	display->slots[msg->index].busy = false;
	g_mutex_unlock(&lock);
}

/* the binder thread of AOSP: reads the widget's messages and runs the callbacks */
static void *display_thread(void *data)
{
	struct display *display = data;
	bool stop = false;
	while (!stop) {
		bool alive = ANativeWindow_remoteRead(display->window, 16);
		g_mutex_lock(&lock);
		stop = display->stop || !alive;
		g_mutex_unlock(&lock);
		display_run_pending(display, stop);
	}
	ANativeWindow_remoteSetHandler(display->window, NULL, NULL);
	g_mutex_lock(&lock);
	display_unref(display);
	g_mutex_unlock(&lock);
	return NULL;
}

/* ---- surface control ---- */

struct ASurfaceControl *ASurfaceControl_createFromWindow(struct ANativeWindow *window, const char *debug_name)
{
	CHECK_NOT_NULL(window);
	CHECK_NOT_NULL(debug_name);
	/* a window of this process (a SurfaceView widget) or without a widget: no composer for it, as AOSP's
	 * createFromWindow() without a SurfaceComposerClient */
	if (window->remote_fd < 0 || window->remote_gone)
		return NULL;
	struct display *display = calloc(1, sizeof(*display));
	display->refcount = 2; /* the root surface and the thread */
	display->window = window;
	display->shown = -1;
	ANativeWindow_acquire(window);
	struct ASurfaceControl *sc = sc_new(NULL);
	sc->display = display;
	ANativeWindow_remoteSetHandler(window, display_message, display);
	display->thread = g_thread_new("SurfaceControl", display_thread, display);
	g_thread_unref(display->thread);
	return sc;
}

struct ASurfaceControl *ASurfaceControl_create(struct ASurfaceControl *parent, const char *debug_name)
{
	CHECK_NOT_NULL(parent);
	CHECK_NOT_NULL(debug_name);
	g_mutex_lock(&lock);
	struct ASurfaceControl *sc = sc_new(parent);
	g_mutex_unlock(&lock);
	return sc;
}

void ASurfaceControl_acquire(struct ASurfaceControl *sc)
{
	g_mutex_lock(&lock);
	sc->refcount++;
	g_mutex_unlock(&lock);
}

void ASurfaceControl_release(struct ASurfaceControl *sc)
{
	g_mutex_lock(&lock);
	sc_unref(sc);
	g_mutex_unlock(&lock);
}

/* surface_control.cpp: a Java SurfaceControl/Transaction without a native object is fatal; ATL's Java
 * classes (api-impl android/view/SurfaceControl.java) have none */
struct ASurfaceControl *ASurfaceControl_fromJava(JNIEnv *env, jobject surfaceControlObj)
{
	CHECK_NOT_NULL(env);
	CHECK_NOT_NULL(surfaceControlObj);
	fatal("surfaceControlObj passed to ASurfaceControl_fromJava is not valid");
	return NULL;
}

/* ---- transactions ---- */

struct ASurfaceTransaction *ASurfaceTransaction_create(void)
{
	return calloc(1, sizeof(struct ASurfaceTransaction));
}

static void change_clear(struct change *c)
{
	sc_unref(c->sc);
	AHardwareBuffer_release(c->buffer);
	if (c->fence >= 0)
		close(c->fence);
	sc_unref(c->parent);
}

static void transaction_clear(struct ASurfaceTransaction *t)
{
	g_mutex_lock(&lock);
	for (size_t i = 0; i < t->n_changes; i++)
		change_clear(&t->changes[i]);
	g_mutex_unlock(&lock);
	free(t->changes);
	free(t->complete);
	free(t->commit);
	*t = (struct ASurfaceTransaction){0};
}

void ASurfaceTransaction_delete(struct ASurfaceTransaction *t)
{
	if (!t)
		return;
	transaction_clear(t);
	free(t);
}

struct ASurfaceTransaction *ASurfaceTransaction_fromJava(JNIEnv *env, jobject transactionObj)
{
	CHECK_NOT_NULL(env);
	CHECK_NOT_NULL(transactionObj);
	fatal("surfaceControlObj passed to ASurfaceTransaction_fromJava is not valid");
	return NULL;
}

/* the change of sc in t (every setter registers the surface for the callbacks, as AOSP's
 * registerSurfaceControlForCallback()) */
static struct change *change_of(struct ASurfaceTransaction *t, struct ASurfaceControl *sc)
{
	for (size_t i = 0; i < t->n_changes; i++)
		if (t->changes[i].sc == sc)
			return &t->changes[i];
	t->changes = realloc(t->changes, (t->n_changes + 1) * sizeof(*t->changes));
	struct change *c = &t->changes[t->n_changes++];
	*c = (struct change){.sc = sc, .fence = -1};
	g_mutex_lock(&lock);
	sc->refcount++;
	g_mutex_unlock(&lock);
	return c;
}

#define CHECK_SURFACE()                     \
	CHECK_NOT_NULL(aSurfaceTransaction); \
	CHECK_NOT_NULL(aSurfaceControl);

void ASurfaceTransaction_reparent(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, struct ASurfaceControl *newParent)
{
	CHECK_SURFACE();
	struct change *c = change_of(aSurfaceTransaction, aSurfaceControl);
	g_mutex_lock(&lock);
	sc_unref(c->parent);
	c->parent = newParent;
	if (newParent)
		newParent->refcount++;
	g_mutex_unlock(&lock);
	c->mask |= CHANGE_PARENT;
}

void ASurfaceTransaction_setVisibility(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, int8_t visibility)
{
	CHECK_SURFACE();
	if (visibility != ASURFACE_TRANSACTION_VISIBILITY_SHOW && visibility != ASURFACE_TRANSACTION_VISIBILITY_HIDE) {
		fprintf(stderr, "invalid visibility %d\n", visibility);
		abort();
	}
	struct change *c = change_of(aSurfaceTransaction, aSurfaceControl);
	c->visible = visibility == ASURFACE_TRANSACTION_VISIBILITY_SHOW;
	c->mask |= CHANGE_VISIBILITY;
}

void ASurfaceTransaction_setZOrder(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, int32_t z_order)
{
	CHECK_SURFACE();
	struct change *c = change_of(aSurfaceTransaction, aSurfaceControl);
	c->z = z_order;
	c->mask |= CHANGE_Z;
}

static void set_buffer(struct ASurfaceTransaction *t, struct ASurfaceControl *sc, struct AHardwareBuffer *buffer, int fence,
                       OnBufferRelease release_cb, void *release_ctx)
{
	struct change *c = change_of(t, sc);
	g_mutex_lock(&lock);
	AHardwareBuffer_release(c->buffer);
	g_mutex_unlock(&lock);
	if (c->fence >= 0)
		close(c->fence);
	AHardwareBuffer_acquire(buffer);
	c->buffer = buffer;
	c->fence = fence;
	c->release_cb = release_cb;
	c->release_ctx = release_ctx;
	c->mask |= CHANGE_BUFFER;
}

void ASurfaceTransaction_setBuffer(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, struct AHardwareBuffer *buffer, int acquire_fence_fd)
{
	CHECK_SURFACE();
	set_buffer(aSurfaceTransaction, aSurfaceControl, buffer, acquire_fence_fd, NULL, NULL);
}

void ASurfaceTransaction_setBufferWithRelease(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, struct AHardwareBuffer *buffer,
                                              int acquire_fence_fd, void *context, OnBufferRelease aReleaseCallback)
{
	CHECK_SURFACE();
	CHECK_NOT_NULL(aReleaseCallback);
	set_buffer(aSurfaceTransaction, aSurfaceControl, buffer, acquire_fence_fd, aReleaseCallback, context);
}

#define CHECK_VALID_RECT(name)                                                  \
	if ((name).left > (name).right || (name).top > (name).bottom) {         \
		fprintf(stderr, "invalid arg passed as " #name " argument\n"); \
		abort();                                                         \
	}

void ASurfaceTransaction_setGeometry(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, const ARect *source,
                                     const ARect *destination, int32_t transform)
{
	CHECK_SURFACE();
	CHECK_VALID_RECT(*source);
	CHECK_VALID_RECT(*destination);
	struct change *c = change_of(aSurfaceTransaction, aSurfaceControl);
	c->destination = *destination;
	c->mask |= CHANGE_DESTINATION;
}

/* these only register the surface: crop, position, scale and transform only matter for the choice above
 * through setGeometry()'s destination */
void ASurfaceTransaction_setCrop(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, const ARect *crop)
{
	CHECK_SURFACE();
	CHECK_VALID_RECT(*crop);
	change_of(aSurfaceTransaction, aSurfaceControl);
}
void ASurfaceTransaction_setPosition(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, int32_t x, int32_t y)
{
	CHECK_SURFACE();
	change_of(aSurfaceTransaction, aSurfaceControl);
}
void ASurfaceTransaction_setBufferTransform(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, int32_t transform)
{
	CHECK_SURFACE();
	change_of(aSurfaceTransaction, aSurfaceControl);
}
void ASurfaceTransaction_setScale(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, float xScale, float yScale)
{
	CHECK_SURFACE();
	if (xScale < 0 || yScale < 0)
		fatal("invalid scale");
	change_of(aSurfaceTransaction, aSurfaceControl);
}

#define REGISTER_ONLY(name, ...)                                                                                                   \
	void name(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl, __VA_ARGS__) \
	{                                                                                                                          \
		CHECK_SURFACE();                                                                                                   \
		change_of(aSurfaceTransaction, aSurfaceControl);                                                                   \
	}
REGISTER_ONLY(ASurfaceTransaction_setBufferTransparency, int8_t transparency)
REGISTER_ONLY(ASurfaceTransaction_setDamageRegion, const void *rects, uint32_t count)
REGISTER_ONLY(ASurfaceTransaction_setBufferAlpha, float alpha)
REGISTER_ONLY(ASurfaceTransaction_setBufferDataSpace, int32_t aDataSpace)
REGISTER_ONLY(ASurfaceTransaction_setHdrMetadata_smpte2086, void *metadata)
REGISTER_ONLY(ASurfaceTransaction_setHdrMetadata_cta861_3, void *metadata)
REGISTER_ONLY(ASurfaceTransaction_setExtendedRangeBrightness, float currentBufferRatio, float desiredRatio)
REGISTER_ONLY(ASurfaceTransaction_setDesiredHdrHeadroom, float desiredRatio)
REGISTER_ONLY(ASurfaceTransaction_setLuts, const void *luts)
REGISTER_ONLY(ASurfaceTransaction_setColor, float r, float g, float b, float alpha, int32_t dataspace)
REGISTER_ONLY(ASurfaceTransaction_setFrameRate, float frameRate, int8_t compatibility)
REGISTER_ONLY(ASurfaceTransaction_setFrameRateWithChangeStrategy, float frameRate, int8_t compatibility, int8_t changeFrameRateStrategy)
REGISTER_ONLY(ASurfaceTransaction_setEnableBackPressure, bool enableBackPressure)
void ASurfaceTransaction_clearFrameRate(struct ASurfaceTransaction *aSurfaceTransaction, struct ASurfaceControl *aSurfaceControl)
{
	CHECK_SURFACE();
	change_of(aSurfaceTransaction, aSurfaceControl);
}
void ASurfaceTransaction_setDesiredPresentTime(struct ASurfaceTransaction *aSurfaceTransaction, int64_t desiredPresentTime) { CHECK_NOT_NULL(aSurfaceTransaction); }
void ASurfaceTransaction_setFrameTimeline(struct ASurfaceTransaction *aSurfaceTransaction, int64_t vsyncId) { CHECK_NOT_NULL(aSurfaceTransaction); }

static void add_callback(struct callback **list, size_t *n, OnComplete func, void *context)
{
	*list = realloc(*list, (*n + 1) * sizeof(**list));
	(*list)[(*n)++] = (struct callback){func, context};
}

void ASurfaceTransaction_setOnComplete(struct ASurfaceTransaction *aSurfaceTransaction, void *context, OnComplete func)
{
	CHECK_NOT_NULL(aSurfaceTransaction);
	CHECK_NOT_NULL(func);
	add_callback(&aSurfaceTransaction->complete, &aSurfaceTransaction->n_complete, func, context);
}

void ASurfaceTransaction_setOnCommit(struct ASurfaceTransaction *aSurfaceTransaction, void *context, OnComplete func)
{
	CHECK_NOT_NULL(aSurfaceTransaction);
	CHECK_NOT_NULL(func);
	add_callback(&aSurfaceTransaction->commit, &aSurfaceTransaction->n_commit, func, context);
}

/* with lock held: the surface of display's tree to show, NULL if none */
static struct ASurfaceControl *choose(struct display *display, struct ASurfaceControl **all, size_t n_all)
{
	struct ASurfaceControl *best = NULL;
	int64_t best_area = -1;
	int count = 0;
	for (size_t i = 0; i < n_all; i++) {
		struct ASurfaceControl *sc = all[i];
		if (!sc->buffer || sc_root(sc)->display != display || !sc_shown(sc))
			continue;
		count++;
		const ARect *d = &sc->destination;
		int64_t area = (d->right > d->left && d->bottom > d->top) ? (int64_t)(d->right - d->left) * (d->bottom - d->top)
		                                                           : (int64_t)sc->buffer->desc.width * sc->buffer->desc.height;
		if (area > best_area || (area == best_area && sc->z < best->z)) {
			best = sc;
			best_area = area;
		}
	}
	if (count > 1 && !display->warned) {
		display->warned = true;
		fprintf(stderr, "ASurfaceControl: %d surfaces with buffers, only the largest is shown\n", count);
	}
	return best;
}

/* all surfaces with a buffer: the ones the displays know through the transactions (kept in a list) */
static struct ASurfaceControl **known;
static size_t n_known;

static void know(struct ASurfaceControl *sc)
{
	for (size_t i = 0; i < n_known; i++)
		if (known[i] == sc)
			return;
	known = realloc(known, (n_known + 1) * sizeof(*known));
	known[n_known++] = sc;
	sc->refcount++;
}

/* drops surfaces only the list still holds */
static void forget_unused(void)
{
	for (size_t i = 0; i < n_known;) {
		if (known[i]->refcount == 1) {
			sc_unref(known[i]);
			known[i] = known[--n_known];
		} else {
			i++;
		}
	}
}

/* with lock held: shows sc's buffer in display; FALSE if no slot is free */
static bool present(struct display *display, struct ASurfaceControl *sc)
{
	struct AHardwareBuffer *buffer = sc->buffer;
	int slot = -1;
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS && slot < 0; i++)
		if (!display->slots[i].busy && display->slots[i].buffer == buffer)
			slot = i;
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS && slot < 0; i++)
		if (!display->slots[i].busy && !display->slots[i].buffer)
			slot = i;
	for (int i = 0; i < ATL_SURFACE_MAX_BUFFERS && slot < 0; i++)
		if (!display->slots[i].busy)
			slot = i;
	if (slot < 0)
		return false;
	struct slot *s = &display->slots[slot];
	if (s->buffer != buffer) {
		AHardwareBuffer_release(s->buffer);
		AHardwareBuffer_acquire(buffer);
		s->buffer = buffer;
		struct atl_surface_msg msg = {.type = ATL_SURFACE_MSG_BUFFER, .index = slot, .width = buffer->desc.width, .height = buffer->desc.height,
		                              .stride = buffer->strides[0], .offset = buffer->offsets[0], .fourcc = buffer->drm_format,
		                              .modifier = buffer->modifier, .top_first = 1};
		ANativeWindow_remoteSend(display->window, &msg, buffer->fd);
	}
	s->busy = true;
	s->presented = ++display->seq;
	display->shown = slot;
	sc->shown_on = display;
	sc->shown_slot = slot;
	sc->shown_seq = s->presented;
	ANativeWindow_remoteSend(display->window, &(struct atl_surface_msg){.type = ATL_SURFACE_MSG_PRESENT, .index = slot}, -1);
	return true;
}

/* a wait for the buffer sc shows now (empty if it is not shown) */
static struct pending *wait_for_shown(struct ASurfaceControl *sc)
{
	struct pending *p = calloc(1, sizeof(*p));
	p->slot = -1;
	if (sc->shown_on && sc->shown_slot >= 0) {
		p->slot = sc->shown_slot;
		p->seq = sc->shown_seq;
	}
	return p;
}

static void queue(struct display *display, struct pending *p)
{
	struct pending **link = &display->pending;
	while (*link)
		link = &(*link)->next;
	*link = p;
}

static void wait_fence(int fence)
{
	if (fence < 0)
		return;
	while (poll(&(struct pollfd){.fd = fence, .events = POLLIN}, 1, -1) < 0 && (errno == EINTR || errno == EAGAIN))
		;
}

void ASurfaceTransaction_apply(struct ASurfaceTransaction *t)
{
	CHECK_NOT_NULL(t);
	int64_t latch = now_ns();
	/* the buffers are shown once the GPU finished them (AOSP latches a buffer when its acquire fence signals) */
	for (size_t i = 0; i < t->n_changes; i++)
		wait_fence(t->changes[i].fence);

	g_mutex_lock(&lock);
	struct pending *replaced = NULL; /* waits for the buffers this transaction replaces */
	struct display *display = NULL;  /* the window whose thread runs the callbacks */
	for (size_t i = 0; i < t->n_changes; i++) {
		struct change *c = &t->changes[i];
		struct ASurfaceControl *sc = c->sc;
		if (c->mask & CHANGE_PARENT) {
			if (c->parent)
				c->parent->refcount++;
			sc_unref(sc->parent);
			sc->parent = c->parent;
		}
		if (c->mask & CHANGE_VISIBILITY)
			sc->visible = c->visible;
		if (c->mask & CHANGE_Z)
			sc->z = c->z;
		if (c->mask & CHANGE_DESTINATION)
			sc->destination = c->destination;
		if (c->mask & CHANGE_BUFFER) {
			/* the old buffer is released once the widget lets go of it */
			struct pending *p = wait_for_shown(sc);
			p->release_cb = sc->release_cb;
			p->release_ctx = sc->release_ctx;
			struct pending *w = wait_for_shown(sc);
			w->after = replaced;
			replaced = w;
			struct display *on = sc->shown_on ? sc->shown_on : sc_root(sc)->display;
			if (sc->buffer && p->release_cb && on)
				queue(on, p);
			else
				free(p);
			AHardwareBuffer_release(sc->buffer);
			sc->buffer = c->buffer;
			c->buffer = NULL;
			sc->release_cb = c->release_cb;
			sc->release_ctx = c->release_ctx;
			sc->acquire_time = latch;
			sc->shown_on = NULL;
			sc->shown_slot = -1;
			know(sc);
		}
		if (!display)
			display = sc_root(sc)->display;
	}
	/* what each window shows now */
	struct display *displays[8];
	int n_displays = 0;
	for (size_t i = 0; i < n_known; i++) {
		struct display *d = sc_root(known[i])->display;
		bool seen = !d;
		for (int k = 0; k < n_displays && !seen; k++)
			seen = displays[k] == d;
		if (!seen && n_displays < 8)
			displays[n_displays++] = d;
	}
	for (int k = 0; k < n_displays; k++) {
		struct display *d = displays[k];
		struct ASurfaceControl *sc = choose(d, known, n_known);
		if (sc && !(sc->shown_on == d && sc->shown_slot >= 0 && d->shown == sc->shown_slot) && !present(d, sc))
			fprintf(stderr, "ASurfaceControl: no free buffer slot in the window, frame dropped\n");
	}
	forget_unused();

	/* the callbacks, with the stats of every surface of the transaction */
	struct callback *lists[2] = {t->commit, t->complete};
	size_t counts[2] = {t->n_commit, t->n_complete};
	for (int l = 0; l < 2; l++) {
		for (size_t k = 0; k < counts[l]; k++) {
			struct ASurfaceTransactionStats *stats = calloc(1, sizeof(*stats));
			stats->latch_time = latch;
			stats->n_surfaces = t->n_changes;
			stats->surfaces = calloc(t->n_changes ? t->n_changes : 1, sizeof(*stats->surfaces));
			for (size_t i = 0; i < t->n_changes; i++) {
				stats->surfaces[i] = (struct surface_stats){t->changes[i].sc, t->changes[i].sc->acquire_time};
				t->changes[i].sc->refcount++;
			}
			struct pending *p = calloc(1, sizeof(*p));
			p->slot = -1;
			p->func = lists[l][k].func;
			p->context = lists[l][k].context;
			p->stats = stats;
			/* OnComplete: once the replaced buffers are released, so their release fence is -1 (signalled) */
			if (l == 1) {
				for (struct pending *r = replaced; r; r = r->after) {
					struct pending *copy = calloc(1, sizeof(*copy));
					copy->slot = r->slot;
					copy->seq = r->seq;
					copy->after = p->after;
					p->after = copy;
				}
			}
			if (display) {
				queue(display, p);
			} else {
				/* no window below: AOSP's composer completes it all the same */
				g_mutex_unlock(&lock);
				p->stats->completed = true;
				p->func(p->context, p->stats);
				g_mutex_lock(&lock);
				stats_free(p->stats);
				pending_free(p);
			}
		}
	}
	pending_free(replaced);
	g_mutex_unlock(&lock);
	/* AOSP's apply() hands the state over and leaves the transaction empty */
	transaction_clear(t);
}

/* ---- stats ---- */

int64_t ASurfaceTransactionStats_getLatchTime(struct ASurfaceTransactionStats *stats)
{
	CHECK_NOT_NULL(stats);
	return stats->latch_time;
}

/* no present fence: the frame is shown when the callback runs */
int ASurfaceTransactionStats_getPresentFenceFd(struct ASurfaceTransactionStats *stats)
{
	CHECK_NOT_NULL(stats);
	if (!stats->completed)
		fatal("ASurfaceTransactionStats queried from an incomplete transaction callback");
	return -1;
}

void ASurfaceTransactionStats_getASurfaceControls(struct ASurfaceTransactionStats *stats, struct ASurfaceControl ***outASurfaceControls, size_t *outASurfaceControlsSize)
{
	CHECK_NOT_NULL(stats);
	CHECK_NOT_NULL(outASurfaceControls);
	CHECK_NOT_NULL(outASurfaceControlsSize);
	struct ASurfaceControl **list = calloc(stats->n_surfaces ? stats->n_surfaces : 1, sizeof(*list));
	for (size_t i = 0; i < stats->n_surfaces; i++)
		list[i] = stats->surfaces[i].sc;
	*outASurfaceControls = list;
	*outASurfaceControlsSize = stats->n_surfaces;
}

void ASurfaceTransactionStats_releaseASurfaceControls(struct ASurfaceControl **aSurfaceControls)
{
	CHECK_NOT_NULL(aSurfaceControls);
	free(aSurfaceControls);
}

static struct surface_stats *stats_of(struct ASurfaceTransactionStats *stats, struct ASurfaceControl *sc)
{
	for (size_t i = 0; i < stats->n_surfaces; i++)
		if (stats->surfaces[i].sc == sc)
			return &stats->surfaces[i];
	fatal("ASurfaceControl not found");
	return NULL;
}

int64_t ASurfaceTransactionStats_getAcquireTime(struct ASurfaceTransactionStats *stats, struct ASurfaceControl *aSurfaceControl)
{
	CHECK_NOT_NULL(stats);
	CHECK_NOT_NULL(aSurfaceControl);
	return stats_of(stats, aSurfaceControl)->acquire_time;
}

/* the callback runs after the widget released the previous buffers: their fence has signalled */
int ASurfaceTransactionStats_getPreviousReleaseFenceFd(struct ASurfaceTransactionStats *stats, struct ASurfaceControl *aSurfaceControl)
{
	CHECK_NOT_NULL(stats);
	CHECK_NOT_NULL(aSurfaceControl);
	if (!stats->completed)
		fatal("ASurfaceTransactionStats queried from an incomplete transaction callback");
	stats_of(stats, aSurfaceControl);
	return -1;
}
