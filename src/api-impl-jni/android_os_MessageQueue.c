#include "defines.h"
#include "util.h"

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <glib.h>

#include "generated_headers/android_os_MessageQueue.h"
#include "../libandroid/looper.h"

struct native_message_queue {
	ALooper *looper;
	bool in_callback;
	bool is_main_thread;
};

GThread *main_thread_id;
ALooper *main_thread_looper;

/**
 * callback to execute java handlers on glib managed thread loops
 */
static gboolean dispatch_func(GSource *source, GSourceFunc callback, gpointer user_data)
{
	JavaVM *jvm = user_data;
	JNIEnv *env;
	(*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
	g_source_set_ready_time(source, -1); // clear previous timeout
	J__Looper__loop(env);
	if ((*env)->ExceptionCheck(env)) {
		/* On Android, an exception that leaves the main looper ends the app: the main thread dies,
		 * its uncaught exception handler runs and the process is killed. Do the same here. Left
		 * pending (ART's ExceptionDescribe keeps it), the next JNI call made CheckJNI abort the
		 * runtime, and dumping all threads froze the window for the better part of a minute. */
		fprintf(stderr, "FATAL EXCEPTION: main\n");
		(*env)->ExceptionDescribe(env);
		jthrowable exception = (*env)->ExceptionOccurred(env);
		(*env)->ExceptionClear(env);
		jclass thread_class = (*env)->FindClass(env, "java/lang/Thread");
		jobject thread = (*env)->CallStaticObjectMethod(env, thread_class, _STATIC_METHOD(thread_class, "currentThread", "()Ljava/lang/Thread;"));
		(*env)->CallVoidMethod(env, thread, _METHOD(thread_class, "dispatchUncaughtException", "(Ljava/lang/Throwable;)V"), exception);
		fflush(NULL);
		_exit(10);
	}

	return G_SOURCE_CONTINUE;
}
static GSourceFuncs source_funcs = {
	.dispatch = dispatch_func,
};
static GSource *source = NULL;

/* GTK calls into Java from the main loop, outside of any native method: the local references made
 * there are never freed (the thread never returns to Java), and they keep what they point to (old
 * activities, their windows and views) alive. So the main loop gets a local frame per iteration, like
 * a native method has one per call: it's dropped and pushed again before polling, when no callback
 * runs (depth 0; a loop nested in a callback must not drop the callback's references). */
static GPollFunc default_poll_func;
static gint local_frame_poll(GPollFD *fds, guint nfds, gint timeout)
{
	static gboolean frame_pushed = FALSE;
	if (g_main_depth() == 0) {
		JNIEnv *env = get_jni_env();
		if (frame_pushed)
			(*env)->PopLocalFrame(env, NULL);
		frame_pushed = (*env)->PushLocalFrame(env, 16) == 0;
	}
	return default_poll_func(fds, nfds, timeout);
}

/* from the main loop, so that the startup code (in GApplication's activate, which runs before the
 * loop) keeps its references */
static gboolean install_local_frame_poll(gpointer data)
{
	default_poll_func = g_main_context_get_poll_func(NULL);
	g_main_context_set_poll_func(NULL, local_frame_poll);
	return G_SOURCE_REMOVE;
}

void prepare_main_looper(JNIEnv *env)
{
	g_idle_add(install_local_frame_poll, NULL);
	main_thread_id = g_thread_self();
	main_thread_looper = ALooper_prepare(0);

	J__Looper__prepareMainLooper(env);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	source = g_source_new(&source_funcs, sizeof(GSource));
	JavaVM *jvm;
	(*env)->GetJavaVM(env, &jvm);
	g_source_set_callback(source, NULL, jvm, NULL); // func can be NULL, as we handle everything in the dispatch_func
	g_source_set_ready_time(source, 0);             // first call will be immediately
	g_source_attach(source, NULL);
}

JNIEXPORT jlong JNICALL Java_android_os_MessageQueue_nativeInit(JNIEnv *env, jclass this)
{
	struct native_message_queue *message_queue = malloc(sizeof(struct native_message_queue));

	message_queue->in_callback = false;
	message_queue->looper = ALooper_prepare(0);
	message_queue->is_main_thread = g_thread_self() == main_thread_id;

	return _INTPTR(message_queue);
}

JNIEXPORT void JNICALL Java_android_os_MessageQueue_nativeDestroy(JNIEnv *env, jclass this, jlong ptr)
{
	struct native_message_queue *message_queue = _PTR(ptr);

	free(message_queue);
}

JNIEXPORT jboolean JNICALL Java_android_os_MessageQueue_nativePollOnce(JNIEnv *env, jclass this, jlong ptr, jint timeout_millis)
{
	struct native_message_queue *message_queue = _PTR(ptr);

	if (message_queue->is_main_thread) { // thread loop is managed by glib
		if (timeout_millis) {
			if (timeout_millis != -1) // -1 means no more messages
				g_source_set_ready_time(source, g_source_get_time(source) + timeout_millis * 1000L);
			return true; // indicate that java side should return to block in glib managed loop
		} else {
			return false;
		}
	}

	//printf("Java_android_os_MessageQueue_nativePollOnce: entry (timeout: %d)\n", timeout_millis);
	message_queue->in_callback = true;
	ALooper_pollOnce(timeout_millis, NULL, NULL, NULL);
	message_queue->in_callback = false;
	//printf("Java_android_os_MessageQueue_nativePollOnce: exit\n");

	/* TODO: what's with the exception stuff */
	return false;
}

JNIEXPORT void JNICALL Java_android_os_MessageQueue_nativeWake(JNIEnv *env, jclass this, jlong ptr)
{
	struct native_message_queue *message_queue = _PTR(ptr);

	if (message_queue->is_main_thread) {        // thread loop is managed by glib
		g_source_set_ready_time(source, 0); // immediately
		g_main_context_wakeup(NULL);        // wake glib poll (needed when called from non-main thread)
		return;
	}

	ALooper_wake(message_queue->looper);
}

JNIEXPORT jboolean JNICALL Java_android_os_MessageQueue_nativeIsIdling(JNIEnv *env, jclass this, jlong ptr)
{
	struct native_message_queue *message_queue = _PTR(ptr);

	return ALooper_isPolling(message_queue->looper);
}
