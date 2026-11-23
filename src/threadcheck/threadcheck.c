/* libatl_threadcheck.so: debug checker for ATL's threading contract (doc/threading.md), preloaded when
 * ATL_THREADCHECK is set (main.c re-executes itself with LD_PRELOAD). Every GTK/GDK/GSK function and the
 * Wayland/EGL calls in gen_trampolines.py go through tc_enter(); a call that breaks the contract is reported once
 * per call site with the native and the Java stack, all calls are counted ("ATL-THREADCHECK" lines on stderr).
 *
 * Rules (GTK 4.24 docs/reference/gtk/section-threading.md, libwayland wayland-client.c, EGL 1.5 §3.7.3):
 * - GTK/GDK/GSK only on the main thread, except immutable objects (GdkTexture, GskRenderNode, GskTransform,
 *   GskPath, color states), builders (GtkSnapshot, Gdk*TextureBuilder, GskPathBuilder: any thread, one at a time,
 *   checked) and objects a thread created itself (a GskRenderer made off the main thread).
 * - the default queue of a wl_display is dispatched by one thread; another thread must not dispatch it, nor create
 *   objects on it or add listeners to them (events could be dispatched before the listener is set).
 * - an EGLContext is current in at most one thread, eglSwapBuffers only in the thread where the surface is current,
 *   a wl_egl_window is resized only by the thread that draws to it and outlives its EGLSurface. */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <execinfo.h>
#include <jni.h>
#include <link.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <unistd.h>

#include "tc_names.h"

extern char tc_ret_stub[];

enum category {
	CAT_MAIN,          /* main thread only */
	CAT_ANY,           /* immutable/threadsafe */
	CAT_BUILDER,       /* any thread, one at a time (first argument) */
	CAT_BUILDER_FREE,  /* like CAT_BUILDER, frees the builder */
	CAT_OWNED_NEW,     /* creates an object owned by the calling thread */
	CAT_OWNED,         /* main thread or the thread that created the first argument */
	CAT_INIT,          /* gtk_init: defines the main thread */
	CAT_WL_DISPATCH,   /* (display) default queue */
	CAT_WL_DISPATCH_Q, /* (display, queue) */
	CAT_WL_CREATE,     /* (proxy, opcode, interface, ...) */
	CAT_WL_LISTENER,   /* (proxy, ...) */
	CAT_WLEGL_RESIZE,
	CAT_WLEGL_DESTROY,
	CAT_EGL_MAKECURRENT,
	CAT_EGL_SWAP,
	CAT_EGL_CREATE_WINDOW, /* (dpy, config, native window, attribs) */
	CAT_EGL_DESTROY_SURFACE,
};

static uint8_t categories[TC_N];
static void *_Atomic real[TC_N];
static _Atomic uint32_t calls_off_main;
static pid_t main_tid;
static void *gtk_lo, *gtk_hi; /* libgtk-4 text: its own calls through the PLT are not ATL's */

static __thread int in_checker;
static __thread struct { void *ret; void *obj; uint32_t idx; } shadow[64];
static __thread int shadow_depth;

static void *(*p_wl_proxy_get_queue)(void *);
static void *(*p_wl_proxy_get_display)(void *);
static const char *(*p_wl_proxy_get_class)(void *);
static void *gdk_wl_display; /* the wl_display whose default queue libgtk dispatches */

static int starts(const char *s, const char *p) { return !strncmp(s, p, strlen(p)); }
static int ends(const char *s, const char *p) { size_t a = strlen(s), b = strlen(p); return a >= b && !strcmp(s + a - b, p); }

static uint8_t classify(const char *n)
{
	static const char *const any_prefix[] = {"gdk_texture_", "gdk_memory_texture_new", "gdk_memory_format", "gdk_rgba_",
		"gdk_color_state_", "gdk_cicp_params_", "gsk_render_node_", "gsk_transform_", "gsk_path_", "gsk_stroke_",
		"gsk_rounded_rect_", "gsk_color_stop", "gsk_shadow", "gdk_cairo_set_source_rgba", "gdk_cairo_rectangle",
		"gdk_rectangle_", "gtk_snapshot_new", "gtk_border_", "gtk_requisition_", "gdk_memory_texture_builder_new",
		"gdk_dmabuf_texture_builder_new", "gdk_gl_texture_builder_new", "gsk_path_builder_new", NULL};
	static const char *const builder_prefix[] = {"gtk_snapshot_", "gdk_memory_texture_builder_", "gdk_dmabuf_texture_builder_",
		"gdk_gl_texture_builder_", "gsk_path_builder_", NULL};
	if (starts(n, "wl_display_dispatch_queue") || starts(n, "wl_display_roundtrip_queue") || !strcmp(n, "wl_display_prepare_read_queue"))
		return CAT_WL_DISPATCH_Q;
	if (starts(n, "wl_display_"))
		return CAT_WL_DISPATCH;
	if (starts(n, "wl_proxy_marshal"))
		return CAT_WL_CREATE;
	if (starts(n, "wl_proxy_add_"))
		return CAT_WL_LISTENER;
	if (!strcmp(n, "wl_egl_window_resize"))
		return CAT_WLEGL_RESIZE;
	if (!strcmp(n, "wl_egl_window_destroy"))
		return CAT_WLEGL_DESTROY;
	if (!strcmp(n, "eglMakeCurrent"))
		return CAT_EGL_MAKECURRENT;
	if (!strcmp(n, "eglSwapBuffers"))
		return CAT_EGL_SWAP;
	if (starts(n, "eglCreate"))
		return CAT_EGL_CREATE_WINDOW;
	if (!strcmp(n, "eglDestroySurface"))
		return CAT_EGL_DESTROY_SURFACE;
	if (!strcmp(n, "gtk_init") || !strcmp(n, "gtk_init_check"))
		return CAT_INIT;
	if (ends(n, "_get_type") || ends(n, "_error_quark"))
		return CAT_ANY;
	for (int i = 0; any_prefix[i]; i++)
		if (starts(n, any_prefix[i]))
			return CAT_ANY;
	if (starts(n, "gsk_") && strstr(n, "_node_")) /* render nodes: constructors and getters of immutable objects */
		return CAT_ANY;
	if (!strcmp(n, "gtk_snapshot_free_to_node") || !strcmp(n, "gtk_snapshot_free_to_paintable") ||
	    !strcmp(n, "gsk_path_builder_free_to_path") || !strcmp(n, "gsk_path_builder_unref"))
		return CAT_BUILDER_FREE;
	for (int i = 0; builder_prefix[i]; i++)
		if (starts(n, builder_prefix[i]))
			return CAT_BUILDER;
	if (starts(n, "gsk_") && ends(n, "_renderer_new"))
		return CAT_OWNED_NEW;
	if (starts(n, "gsk_renderer_"))
		return CAT_OWNED;
	return CAT_MAIN;
}

/* ---- small lock-protected pointer maps (debug only, simplicity over speed) ---- */
#define MAP_SIZE 8192
struct entry { void *key; pid_t tid; int32_t count; void *val; };
struct map { pthread_mutex_t lock; struct entry e[MAP_SIZE]; };
static struct map builders = {PTHREAD_MUTEX_INITIALIZER}; /* builder -> tid using it, number of calls in it */
static struct map owned = {PTHREAD_MUTEX_INITIALIZER};    /* object -> creating tid */
static struct map displays = {PTHREAD_MUTEX_INITIALIZER}; /* wl_display -> tid dispatching its default queue */
static struct map contexts = {PTHREAD_MUTEX_INITIALIZER}; /* EGLContext -> tid where current */
static struct map surfaces = {PTHREAD_MUTEX_INITIALIZER}; /* EGLSurface -> tid where current (0: none), val = wl_egl_window */

static size_t slot_of(void *key) { return (((uintptr_t)key >> 4) * 0x9E3779B97F4A7C15ull >> 40) % MAP_SIZE; }

/* linear probing without tombstones; NULL when the map is full */
static struct entry *map_get(struct map *m, void *key, int create)
{
	size_t h = slot_of(key);
	for (int i = 0; i < MAP_SIZE; i++) {
		struct entry *e = &m->e[(h + i) % MAP_SIZE];
		if (e->key == key)
			return e;
		if (!e->key) {
			if (!create)
				return NULL;
			*e = (struct entry){key, 0, 0, NULL};
			return e;
		}
	}
	return NULL;
}

/* backward shift deletion */
static void map_del(struct map *m, struct entry *e)
{
	size_t i = e - m->e, j = i;
	m->e[i].key = NULL;
	for (;;) {
		j = (j + 1) % MAP_SIZE;
		if (!m->e[j].key)
			return;
		size_t k = slot_of(m->e[j].key);
		if (i <= j ? (i < k && k <= j) : (i < k || k <= j))
			continue;
		m->e[i] = m->e[j];
		m->e[j].key = NULL;
		i = j;
	}
}

/* ---- reports ---- */
#define SITES 4096
static struct { _Atomic(uintptr_t) key; _Atomic uint32_t count; uint32_t idx; uint8_t kind; void *caller; } sites[SITES];
static const char *const kind_names[] = {"off-main", "builder-concurrent", "foreign-owned", "wl-default-queue",
	"egl-context-two-threads", "egl-swap-foreign-surface", "wl-egl-window-foreign-thread", "wl-egl-window-outlived",
	"wl-output-on-gdk-connection"};
enum { K_OFF_MAIN, K_BUILDER, K_OWNED, K_WL_QUEUE, K_EGL_CTX, K_EGL_SWAP, K_WLEGL_THREAD, K_WLEGL_LIFE, K_WL_GDK };

static pid_t gettid_(void) { return gettid(); }

static size_t describe(char *b, size_t n, void *addr)
{
	Dl_info info;
	if (dladdr(addr, &info) && info.dli_fname) {
		const char *base = strrchr(info.dli_fname, '/');
		return snprintf(b, n, "%s+0x%lx (%s)", base ? base + 1 : info.dli_fname, (unsigned long)((char *)addr - (char *)info.dli_fbase),
		                info.dli_sname ? info.dli_sname : "?");
	}
	return snprintf(b, n, "%p", addr);
}

static size_t java_stack(char *b, size_t n)
{
	jint (*get_vms)(JavaVM **, jsize, jsize *) = dlsym(RTLD_DEFAULT, "JNI_GetCreatedJavaVMs");
	JavaVM *vm;
	jsize count = 0;
	JNIEnv *env;
	size_t len = 0;
	if (!get_vms || get_vms(&vm, 1, &count) != JNI_OK || count < 1)
		return snprintf(b, n, "ATL-THREADCHECK:   java: (no VM)\n");
	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
		return snprintf(b, n, "ATL-THREADCHECK:   java: (not a Java thread)\n");
	if ((*env)->ExceptionCheck(env))
		return snprintf(b, n, "ATL-THREADCHECK:   java: (exception pending)\n");
	if ((*env)->PushLocalFrame(env, 32) != JNI_OK)
		return 0;
	jclass thread_class = (*env)->FindClass(env, "java/lang/Thread");
	jobject thread = (*env)->CallStaticObjectMethod(env, thread_class, (*env)->GetStaticMethodID(env, thread_class, "currentThread", "()Ljava/lang/Thread;"));
	jobjectArray trace = (*env)->CallObjectMethod(env, thread, (*env)->GetMethodID(env, thread_class, "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
	jmethodID to_string = (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Object"), "toString", "()Ljava/lang/String;");
	jsize frames = trace ? (*env)->GetArrayLength(env, trace) : 0;
	for (jsize i = 2; i < frames && i < 18 && len < n; i++) { /* 0, 1: VMStack/Thread.getStackTrace */
		jstring s = (*env)->CallObjectMethod(env, (*env)->GetObjectArrayElement(env, trace, i), to_string);
		const char *c = s ? (*env)->GetStringUTFChars(env, s, NULL) : NULL;
		len += snprintf(b + len, n - len, "ATL-THREADCHECK:   java: %s\n", c ? c : "?");
		if (c)
			(*env)->ReleaseStringUTFChars(env, s, c);
	}
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionClear(env);
	(*env)->PopLocalFrame(env, NULL);
	return len < n ? len : n;
}

static void report(int kind, uint32_t idx, void *caller, const char *fmt, ...)
{
	/* a library calling its own exported function through the PLT (libwayland: wl_proxy_marshal_flags() ->
	 * wl_proxy_marshal_array_flags()) is that library's business; its outside caller was checked already */
	Dl_info caller_info, real_info;
	if (dladdr(caller, &caller_info) && dladdr(atomic_load(&real[idx]), &real_info) && caller_info.dli_fbase == real_info.dli_fbase)
		return;
	uintptr_t key = ((uintptr_t)caller << 5) ^ ((uintptr_t)idx << 3) ^ (uintptr_t)kind ^ 1;
	uintptr_t h = (key * 0x9E3779B97F4A7C15ull) >> 52;
	for (int i = 0; i < SITES; i++) {
		int s = (h + i) % SITES;
		uintptr_t expected = 0;
		if (atomic_load(&sites[s].key) == key) {
			atomic_fetch_add(&sites[s].count, 1);
			return;
		}
		if (atomic_compare_exchange_strong(&sites[s].key, &expected, key)) {
			sites[s].idx = idx;
			sites[s].kind = kind;
			sites[s].caller = caller;
			atomic_fetch_add(&sites[s].count, 1);
			break;
		}
		if (expected == key) {
			atomic_fetch_add(&sites[s].count, 1);
			return;
		}
	}
	/* first time for this site */
	static char buf[16384];
	static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
	char thread_name[17] = "";
	prctl(PR_GET_NAME, thread_name);
	pthread_mutex_lock(&lock);
	size_t n = sizeof(buf), len = snprintf(buf, n, "ATL-THREADCHECK: %s %s thread=\"%s\" tid=%d main=%d caller=", kind_names[kind], tc_names[idx], thread_name, gettid_(), main_tid);
	len += describe(buf + len, n - len, caller);
	va_list ap;
	va_start(ap, fmt);
	len += snprintf(buf + len, n - len, " ");
	len += vsnprintf(buf + len, n - len, fmt, ap);
	va_end(ap);
	len += snprintf(buf + len, n - len, "\n");
	void *frames[24];
	int count = backtrace(frames, 24);
	for (int i = 2; i < count && len < n - 256; i++) {
		len += snprintf(buf + len, n - len, "ATL-THREADCHECK:   #%d ", i - 2);
		len += describe(buf + len, n - len, frames[i]);
		len += snprintf(buf + len, n - len, "\n");
	}
	len += java_stack(buf + len, n - len);
	write(2, buf, len < n ? len : n - 1);
	pthread_mutex_unlock(&lock);
}

/* ---- the checks ---- */
static void *resolve(uint32_t idx)
{
	void *f = atomic_load(&real[idx]);
	if (!f) {
		f = dlsym(RTLD_NEXT, tc_names[idx]);
		if (!f) {
			fprintf(stderr, "ATL-THREADCHECK: %s not found\n", tc_names[idx]);
			abort();
		}
		atomic_store(&real[idx], f);
	}
	return f;
}

static int hook_return(uint32_t idx, void **ret_slot, void *obj)
{
	if (shadow_depth >= 64)
		return 0;
	shadow[shadow_depth].ret = *ret_slot;
	shadow[shadow_depth].obj = obj;
	shadow[shadow_depth].idx = idx;
	shadow_depth++;
	*ret_slot = tc_ret_stub;
	return 1;
}

static int default_queue(void *proxy)
{
	return p_wl_proxy_get_queue(proxy) == p_wl_proxy_get_queue(p_wl_proxy_get_display(proxy));
}

static pid_t display_dispatcher(void *display, pid_t tid)
{
	pthread_mutex_lock(&displays.lock);
	struct entry *e = map_get(&displays, display, 1);
	if (e && !e->tid)
		e->tid = tid;
	pid_t r = e ? e->tid : tid;
	pthread_mutex_unlock(&displays.lock);
	return r;
}

__attribute__((visibility("hidden"))) void *tc_enter(uint64_t idx, uint64_t *regs, void **ret_slot)
{
	void *f = resolve(idx);
	if (in_checker)
		return f;
	in_checker = 1;
	void *caller = *ret_slot;
	pid_t tid = gettid_();
	void *a0 = (void *)regs[0];
	switch (categories[idx]) {
		case CAT_INIT:
			if (tid != main_tid) {
				fprintf(stderr, "ATL-THREADCHECK: %s in tid %d, main thread was %d\n", tc_names[idx], tid, main_tid);
				main_tid = tid;
			}
			break;
		case CAT_MAIN:
			if (tid != main_tid && !(caller >= gtk_lo && caller < gtk_hi)) {
				atomic_fetch_add(&calls_off_main, 1);
				report(K_OFF_MAIN, idx, caller, "");
			}
			break;
		case CAT_BUILDER:
		case CAT_BUILDER_FREE:
			if (a0) {
				pthread_mutex_lock(&builders.lock);
				struct entry *e = map_get(&builders, a0, 1);
				pid_t other = 0;
				if (e) {
					if (e->count > 0 && e->tid != tid)
						other = e->tid;
					e->tid = tid;
					e->count++;
				}
				pthread_mutex_unlock(&builders.lock);
				if (other)
					report(K_BUILDER, idx, caller, "builder=%p also in use by tid %d", a0, other);
				if (e)
					hook_return(idx, ret_slot, a0);
			}
			break;
		case CAT_OWNED_NEW:
			if (tid != main_tid)
				hook_return(idx, ret_slot, NULL);
			break;
		case CAT_OWNED:
			if (tid != main_tid) {
				pthread_mutex_lock(&owned.lock);
				struct entry *e = map_get(&owned, a0, 0);
				pid_t owner = e ? e->tid : 0;
				pthread_mutex_unlock(&owned.lock);
				if (owner != tid)
					report(K_OWNED, idx, caller, "object=%p created by tid %d", a0, owner);
			}
			break;
		case CAT_WL_DISPATCH:
		case CAT_WL_DISPATCH_Q: {
			if (categories[idx] == CAT_WL_DISPATCH_Q && regs[1] && regs[1] != (uint64_t)p_wl_proxy_get_queue(a0))
				break; /* a queue of its own */
			if (!gdk_wl_display && caller >= gtk_lo && caller < gtk_hi)
				gdk_wl_display = a0;
			pid_t d = display_dispatcher(a0, tid);
			if (d != tid)
				report(K_WL_QUEUE, idx, caller, "display=%p default queue dispatched by tid %d", a0, d);
			break;
		}
		case CAT_WL_CREATE:
		case CAT_WL_LISTENER:
			if (categories[idx] == CAT_WL_CREATE && !regs[2])
				break; /* no new object */
			/* a global bound by someone else on GDK's connection: the compositor then tells GDK's objects about it
			 * (wl_surface.enter with a wl_output GDK does not know, WebView start, 2fcc0c359) */
			if (categories[idx] == CAT_WL_CREATE && a0 && gdk_wl_display && !(caller >= gtk_lo && caller < gtk_hi) &&
			    p_wl_proxy_get_display(a0) == gdk_wl_display && !strcmp(p_wl_proxy_get_class(a0), "wl_registry") &&
			    !strcmp(*(const char *const *)regs[2], "wl_output")) /* struct wl_interface starts with the name */
				report(K_WL_GDK, idx, caller, "wl_output bound on registry=%p of GDK's wl_display", a0);
			if (a0 && default_queue(a0)) {
				pid_t d = display_dispatcher(p_wl_proxy_get_display(a0), tid);
				if (d != tid)
					report(K_WL_QUEUE, idx, caller, "proxy=%p on the default queue of a display dispatched by tid %d", a0, d);
			}
			break;
		case CAT_EGL_MAKECURRENT: {
			void *ctx = (void *)regs[3];
			pthread_mutex_lock(&contexts.lock);
			struct entry *e = ctx ? map_get(&contexts, ctx, 0) : NULL;
			pid_t other = e && e->tid && e->tid != tid ? e->tid : 0;
			pthread_mutex_unlock(&contexts.lock);
			if (other)
				report(K_EGL_CTX, idx, caller, "context=%p current in tid %d", ctx, other);
			static __thread void *args[2]; /* (draw surface, context) for tc_exit */
			args[0] = (void *)regs[1];
			args[1] = ctx;
			hook_return(idx, ret_slot, args);
			break;
		}
		case CAT_EGL_SWAP: {
			void *surface = (void *)regs[1];
			pthread_mutex_lock(&surfaces.lock);
			struct entry *e = map_get(&surfaces, surface, 0);
			pid_t cur = e ? e->tid : 0;
			pthread_mutex_unlock(&surfaces.lock);
			if (cur && cur != tid)
				report(K_EGL_SWAP, idx, caller, "surface=%p current in tid %d", surface, cur);
			break;
		}
		case CAT_EGL_CREATE_WINDOW:
			hook_return(idx, ret_slot, (void *)regs[2]); /* the wl_egl_window */
			break;
		case CAT_EGL_DESTROY_SURFACE: {
			pthread_mutex_lock(&surfaces.lock);
			struct entry *e = map_get(&surfaces, (void *)regs[1], 0);
			if (e)
				map_del(&surfaces, e);
			pthread_mutex_unlock(&surfaces.lock);
			break;
		}
		case CAT_WLEGL_RESIZE:
		case CAT_WLEGL_DESTROY: {
			pid_t cur = 0;
			void *surface = NULL;
			pthread_mutex_lock(&surfaces.lock);
			for (int i = 0; i < MAP_SIZE; i++)
				if (surfaces.e[i].key && surfaces.e[i].val == a0) {
					surface = surfaces.e[i].key;
					cur = surfaces.e[i].tid;
				}
			pthread_mutex_unlock(&surfaces.lock);
			if (categories[idx] == CAT_WLEGL_DESTROY && surface)
				report(K_WLEGL_LIFE, idx, caller, "window=%p still has EGLSurface %p (current in tid %d)", a0, surface, cur);
			else if (cur && cur != tid)
				report(K_WLEGL_THREAD, idx, caller, "window=%p, its EGLSurface %p is current in tid %d", a0, surface, cur);
			break;
		}
	}
	in_checker = 0;
	return f;
}

__attribute__((visibility("hidden"))) void *tc_exit(void *ret)
{
	shadow_depth--;
	void *obj = shadow[shadow_depth].obj;
	uint32_t idx = shadow[shadow_depth].idx;
	pid_t tid = gettid_();
	struct entry *e;
	switch (categories[idx]) {
		case CAT_BUILDER:
		case CAT_BUILDER_FREE:
			pthread_mutex_lock(&builders.lock);
			e = map_get(&builders, obj, 0);
			if (e && --e->count <= 0)
				map_del(&builders, e); /* only calls in progress matter */
			pthread_mutex_unlock(&builders.lock);
			break;
		case CAT_OWNED_NEW:
			pthread_mutex_lock(&owned.lock);
			e = ret ? map_get(&owned, ret, 1) : NULL;
			if (e)
				e->tid = tid;
			pthread_mutex_unlock(&owned.lock);
			break;
		case CAT_EGL_MAKECURRENT:
			if ((uintptr_t)ret & 0xffffffff) { /* EGL_TRUE */
				static __thread void *cur_ctx, *cur_draw;
				void **args = obj;
				pthread_mutex_lock(&contexts.lock);
				if (cur_ctx && (e = map_get(&contexts, cur_ctx, 0)) && e->tid == tid)
					e->tid = 0;
				if (args[1] && (e = map_get(&contexts, args[1], 1)))
					e->tid = tid;
				pthread_mutex_unlock(&contexts.lock);
				pthread_mutex_lock(&surfaces.lock);
				if (cur_draw && (e = map_get(&surfaces, cur_draw, 0)) && e->tid == tid)
					e->tid = 0;
				if (args[0] && (e = map_get(&surfaces, args[0], 0)))
					e->tid = tid;
				pthread_mutex_unlock(&surfaces.lock);
				cur_ctx = args[1];
				cur_draw = args[0];
			}
			break;
		case CAT_EGL_CREATE_WINDOW:
			if (ret) {
				pthread_mutex_lock(&surfaces.lock);
				e = map_get(&surfaces, ret, 1);
				if (e) {
					e->tid = 0;
					e->val = obj;
				}
				pthread_mutex_unlock(&surfaces.lock);
			}
			break;
	}
	return shadow[shadow_depth].ret;
}

static void child_after_fork(void)
{
	main_tid = gettid_(); /* the zygote forks from its main thread, the child goes on in main() */
}

static int find_gtk(struct dl_phdr_info *info, size_t size, void *data)
{
	if (!strstr(info->dlpi_name, "libgtk-4.so"))
		return 0;
	for (int i = 0; i < info->dlpi_phnum; i++) {
		const ElfW(Phdr) *p = &info->dlpi_phdr[i];
		if (p->p_type == PT_LOAD && (p->p_flags & PF_X)) {
			gtk_lo = (void *)(info->dlpi_addr + p->p_vaddr);
			gtk_hi = (char *)gtk_lo + p->p_memsz;
		}
	}
	return 1;
}

static void summary(void)
{
	char line[512];
	int sites_used = 0;
	for (int i = 0; i < SITES; i++) {
		uint32_t count = atomic_load(&sites[i].count);
		if (!count)
			continue;
		sites_used++;
		size_t len = snprintf(line, sizeof(line), "ATL-THREADCHECK-SUMMARY: %s %s calls=%u caller=", kind_names[sites[i].kind], tc_names[sites[i].idx], count);
		len += describe(line + len, sizeof(line) - len, sites[i].caller);
		len += snprintf(line + len, sizeof(line) - len, "\n");
		write(2, line, len < sizeof(line) ? len : sizeof(line) - 1);
	}
	int len = snprintf(line, sizeof(line), "ATL-THREADCHECK-SUMMARY: pid %d, %d sites\n", getpid(), sites_used);
	write(2, line, len);
}

__attribute__((constructor)) static void init(void)
{
	main_tid = gettid_();
	for (uint32_t i = 0; i < TC_N; i++)
		categories[i] = classify(tc_names[i]);
	p_wl_proxy_get_queue = dlsym(RTLD_NEXT, "wl_proxy_get_queue");
	p_wl_proxy_get_display = dlsym(RTLD_NEXT, "wl_proxy_get_display");
	p_wl_proxy_get_class = dlsym(RTLD_NEXT, "wl_proxy_get_class");
	dl_iterate_phdr(find_gtk, NULL);
	pthread_atfork(NULL, NULL, child_after_fork);
	atexit(summary);
	fprintf(stderr, "ATL-THREADCHECK: active in pid %d, %d functions, main tid %d\n", getpid(), TC_N, main_tid);
}
