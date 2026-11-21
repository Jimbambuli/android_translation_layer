#ifndef SURFACE_TARGET_H
#define SURFACE_TARGET_H

#include <gtk/gtk.h>
#include <jni.h>

#include "../widgets/android_view_SurfaceView.h"

/* the consumer behind an android.view.Surface that a producer (MediaCodec, the camera) hands its frames to
 * as GdkTextures: a SurfaceView's widget, the SurfaceTexture of a Surface(SurfaceTexture), or the window of a
 * Surface of another process of the app (its frames go there as memory buffers) */
struct surface_target {
	SurfaceViewWidget *surface_view_widget;
	jobject surface_texture;       /* global ref */
	struct ANativeWindow *remote;  /* a reference */
};

/* sets the target to the Surface's consumer; none for null or a released Surface */
void surface_target_set(JNIEnv *env, struct surface_target *target, jobject surface);
void surface_target_clear(JNIEnv *env, struct surface_target *target);
gboolean surface_target_is_set(const struct surface_target *target);
/* copies the target with references of its own, for a frame delivered later on the main thread */
void surface_target_copy(JNIEnv *env, struct surface_target *dst, const struct surface_target *src);
/* on the main thread: the texture (taken over) to the consumer, which queues it as AOSP's BufferQueue does
 * (timestamp_ns is the frame's SurfaceTexture.getTimestamp()); drops the references of the copy */
void surface_target_deliver(struct surface_target *target, GdkTexture *texture, long timestamp_ns, gboolean crop);

#endif
