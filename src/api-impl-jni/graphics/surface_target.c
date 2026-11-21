#include "surface_target.h"

#include "../defines.h"
#include "../util.h"
#include "../../libandroid/native_window.h"

void surface_target_clear(JNIEnv *env, struct surface_target *target)
{
	if (target->surface_texture)
		(*env)->DeleteGlobalRef(env, target->surface_texture);
	if (target->remote)
		ANativeWindow_release(target->remote);
	target->surface_texture = NULL;
	target->surface_view_widget = NULL;
	target->remote = NULL;
}

void surface_target_set(JNIEnv *env, struct surface_target *target, jobject surface)
{
	surface_target_clear(env, target);
	if (!surface)
		return;
	if (!_GET_LONG_FIELD(surface, "widget") && _GET_OBJ_FIELD(surface, "atl_endpoint", "Landroid/os/IBinder;")) {
		target->remote = ANativeWindow_fromSurface(env, surface);
		return;
	}
	GtkWidget *wrapper = _PTR(_GET_LONG_FIELD(surface, "widget"));
	jobject surface_texture = _GET_OBJ_FIELD(surface, "surfaceTexture", "Landroid/graphics/SurfaceTexture;");
	if (wrapper)
		target->surface_view_widget = SURFACE_VIEW_WIDGET(gtk_widget_get_first_child(wrapper));
	else if (surface_texture)
		target->surface_texture = (*env)->NewGlobalRef(env, surface_texture);
}

gboolean surface_target_is_set(const struct surface_target *target)
{
	return target->surface_view_widget || target->surface_texture || target->remote;
}

void surface_target_copy(JNIEnv *env, struct surface_target *dst, const struct surface_target *src)
{
	dst->surface_view_widget = src->surface_view_widget;
	dst->surface_texture = src->surface_texture ? (*env)->NewGlobalRef(env, src->surface_texture) : NULL;
	dst->remote = src->remote;
	if (dst->remote)
		ANativeWindow_acquire(dst->remote);
}

void surface_target_deliver(struct surface_target *target, GdkTexture *texture, long timestamp_ns, gboolean crop)
{
	if (target->remote) {
		ANativeWindow_remotePresentTexture(target->remote, texture, crop);
		g_object_unref(texture);
		ANativeWindow_release(target->remote);
	} else if (target->surface_texture) {
		JNIEnv *env = get_jni_env();
		jmethodID queue_buffer = _METHOD(_CLASS(target->surface_texture), "queueBuffer", "(JJ)V");
		(*env)->CallVoidMethod(env, target->surface_texture, queue_buffer, _INTPTR(texture), (jlong)timestamp_ns);
		(*env)->DeleteGlobalRef(env, target->surface_texture);
	} else {
		target->surface_view_widget->crop = crop;
		surface_view_widget_set_texture(target->surface_view_widget, texture, FALSE);
	}
	target->surface_texture = NULL;
	target->remote = NULL;
}
