#include <gdk/gdk.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_graphics_drawable_Drawable.h"

JNIEXPORT jlong JNICALL Java_android_graphics_drawable_Drawable_native_1paintable_1from_1path(JNIEnv *env, jclass class, jstring pathStr)
{
	const char *path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	GdkTexture *texture = gdk_texture_new_from_filename(path, NULL);
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return _INTPTR(texture);
}

struct _JavaPaintable {
	GObject parent_instance;
	jobject drawable;
};
G_DECLARE_FINAL_TYPE(JavaPaintable, java_paintable, JAVA, PAINTABLE, GObject)
static void java_paintable_paintable_init(GdkPaintableInterface *iface);
G_DEFINE_TYPE_WITH_CODE(JavaPaintable, java_paintable, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(GDK_TYPE_PAINTABLE, java_paintable_paintable_init))

static void java_paintable_snapshot(GdkPaintable *gdk_paintable, GdkSnapshot *snapshot, double width, double height)
{
	JNIEnv *env = get_jni_env();
	JavaPaintable *paintable = JAVA_PAINTABLE(gdk_paintable);
	/* the drawable may be gone already (finalized or disposed), the paintable then draws nothing */
	jobject drawable = (*env)->NewLocalRef(env, paintable->drawable);
	if (!drawable)
		return;
	jobject canvas = J_new__GskCanvas(env, _INTPTR(snapshot));
	J__Drawable__setBounds(env, drawable, 0, 0, (int)width, (int)height);
	J__Drawable__draw(env, drawable, canvas);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	(*env)->DeleteLocalRef(env, canvas);
	(*env)->DeleteLocalRef(env, drawable);
}

static int java_paintable_get_intrinsic_width(GdkPaintable *gdk_paintable)
{
	JNIEnv *env = get_jni_env();
	JavaPaintable *paintable = JAVA_PAINTABLE(gdk_paintable);
	jobject drawable = (*env)->NewLocalRef(env, paintable->drawable);
	if (!drawable)
		return 0;
	int width = J__Drawable__getIntrinsicWidth(env, drawable);
	(*env)->DeleteLocalRef(env, drawable);
	return width > 0 ? width : 0;
}

static int java_paintable_get_intrinsic_height(GdkPaintable *gdk_paintable)
{
	JNIEnv *env = get_jni_env();
	JavaPaintable *paintable = JAVA_PAINTABLE(gdk_paintable);
	jobject drawable = (*env)->NewLocalRef(env, paintable->drawable);
	if (!drawable)
		return 0;
	int height = J__Drawable__getIntrinsicHeight(env, drawable);
	(*env)->DeleteLocalRef(env, drawable);
	return height > 0 ? height : 0;
}

static void java_paintable_init(JavaPaintable *java_paintable)
{
}

static void java_paintable_paintable_init(GdkPaintableInterface *iface)
{
	iface->snapshot = java_paintable_snapshot;
	iface->get_intrinsic_height = java_paintable_get_intrinsic_height;
	iface->get_intrinsic_width = java_paintable_get_intrinsic_width;
}

static void java_paintable_dispose(GObject *object)
{
	JavaPaintable *java_paintable = JAVA_PAINTABLE(object);
	JNIEnv *env = get_jni_env();
	if (java_paintable->drawable)
		_WEAK_UNREF(java_paintable->drawable);
	java_paintable->drawable = NULL;
	G_OBJECT_CLASS(java_paintable_parent_class)->dispose(object);
}

static void java_paintable_class_init(JavaPaintableClass *class)
{
	G_OBJECT_CLASS(class)->dispose = java_paintable_dispose;
}

JNIEXPORT jlong JNICALL Java_android_graphics_drawable_Drawable_native_1constructor(JNIEnv *env, jobject this)
{
	JavaPaintable *paintable = NULL;
	if (_METHOD(_CLASS(this), "draw", "(Landroid/graphics/Canvas;)V") != _CACHED_METHOD(Drawable__draw)) {
		paintable = g_object_new(java_paintable_get_type(), NULL);
		paintable->drawable = _WEAK_REF(this);
	}
	return _INTPTR(paintable);
}

static guint queue_invalidate_contents(GdkPaintable *paintable)
{
	gdk_paintable_invalidate_contents(paintable);
	g_object_unref(paintable);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_graphics_drawable_Drawable_native_1invalidate(JNIEnv *env, jobject this, jlong paintable_ptr)
{
	// GTK doesn't allow invalidating a paintable while it's being drawn, so we need to queue it up
	g_idle_add_full(G_PRIORITY_HIGH_IDLE + 20, G_SOURCE_FUNC(queue_invalidate_contents), g_object_ref(GDK_PAINTABLE(_PTR(paintable_ptr))), NULL);
}

JNIEXPORT void JNICALL Java_android_graphics_drawable_Drawable_native_1draw(JNIEnv *env, jobject this, jlong paintable_ptr, jlong snapshot_ptr, jint width, jint height)
{
	GdkSnapshot *snapshot = (GdkSnapshot *)_PTR(snapshot_ptr);
	GdkPaintable *paintable = GDK_PAINTABLE(_PTR(paintable_ptr));
	if (!JAVA_IS_PAINTABLE(paintable))
		gdk_paintable_snapshot(paintable, snapshot, width, height);
}

JNIEXPORT void JNICALL Java_android_graphics_drawable_Drawable_native_1ref(JNIEnv *env, jobject this, jlong paintable_ptr)
{
	g_object_ref(G_OBJECT(_PTR(paintable_ptr)));
}

/* also called from Drawable.finalize(), on the FinalizerDaemon thread */
static gboolean release_paintable(gpointer data)
{
	JavaPaintable *paintable = data;
	JNIEnv *env = get_jni_env();
	if (paintable->drawable)
		_WEAK_UNREF(paintable->drawable);
	paintable->drawable = NULL;
	g_object_unref(paintable);
	return G_SOURCE_REMOVE;
}

JNIEXPORT void JNICALL Java_android_graphics_drawable_Drawable_native_1unref(JNIEnv *env, jobject this, jlong paintable_ptr)
{
	GdkPaintable *paintable = _PTR(paintable_ptr);
	/* the drawable lets go of its own paintable (it is finalized or replaced it): drop the weak
	 * reference to it as well, on the main thread like the unref, instead of only once GTK lets go */
	if (JAVA_IS_PAINTABLE(paintable) && (*env)->IsSameObject(env, JAVA_PAINTABLE(paintable)->drawable, this))
		g_idle_add(release_paintable, paintable);
	else
		atl_unref_on_main_thread(paintable);
}
