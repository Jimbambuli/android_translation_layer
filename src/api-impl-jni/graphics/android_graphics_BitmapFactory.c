#include <gtk/gtk.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_graphics_BitmapFactory.h"

struct _JavaInputStream {
	GInputStream parent_instance;

	jobject is;
	jbyteArray storage;
	int storage_size;
};

G_DECLARE_FINAL_TYPE(JavaInputStream, java_input_stream, ATL, JAVA_INPUT_STREAM, GInputStream);

static gssize java_input_stream_read(GInputStream *gstream, void *buffer, gsize count, GCancellable *cancellable, GError **error)
{
	JavaInputStream *stream = ATL_JAVA_INPUT_STREAM(gstream);
	JNIEnv *env = get_jni_env();

	count = MIN(count, stream->storage_size);

	count = J__InputStream__read(env, stream->is, stream->storage, 0, count);
	if (count == -1) { // end of stream
		return 0;
	}
	jbyte *storage_buf = (*env)->GetByteArrayElements(env, stream->storage, NULL);
	memcpy(buffer, storage_buf, count);
	(*env)->ReleaseByteArrayElements(env, stream->storage, storage_buf, 0);

	return count;
}

static void java_input_stream_class_init(JavaInputStreamClass *klass)
{
	klass->parent_class.read_fn = java_input_stream_read;
}

static void java_input_stream_init(JavaInputStream *self)
{
}

G_DEFINE_TYPE(JavaInputStream, java_input_stream, G_TYPE_INPUT_STREAM)

static GInputStream *java_input_stream_new(JNIEnv *env, jobject is, jbyteArray storage)
{
	JavaInputStream *stream = g_object_new(java_input_stream_get_type(), NULL);
	stream->is = is;
	stream->storage = storage;
	stream->storage_size = (*env)->GetArrayLength(env, storage);
	return &stream->parent_instance;
}

/* Like AOSP's decoder (which hands the "npTc" chunk to the Bitmap, frameworks/base/libs/hwui/jni/
 * BitmapFactory.cpp), find the 9-patch chunk of a PNG. It comes before the image data, so the scan
 * stops at IDAT. The bytes can come from untrusted sources: every length is checked against the
 * data, and the chunk is limited to 1 MB. Returns a byte[] without the CRC, or NULL. */
static jbyteArray find_ninepatch_chunk(JNIEnv *env, GBytes *bytes)
{
	gsize size;
	const guint8 *data = g_bytes_get_data(bytes, &size);
	static const guint8 signature[8] = { 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n' };
	gsize pos = 8;

	if (size < 8 || memcmp(data, signature, 8))
		return NULL;
	while (size - pos >= 12) { // length, type, (data), crc
		guint32 length = ((guint32)data[pos] << 24) | ((guint32)data[pos + 1] << 16) | ((guint32)data[pos + 2] << 8) | data[pos + 3];
		const guint8 *type = data + pos + 4;

		if (!memcmp(type, "IDAT", 4) || !memcmp(type, "IEND", 4))
			return NULL;
		if (length > size - pos - 12)
			return NULL;
		if (!memcmp(type, "npTc", 4)) {
			if (length > 1024 * 1024)
				return NULL;
			jbyteArray chunk = (*env)->NewByteArray(env, length);
			if (chunk)
				(*env)->SetByteArrayRegion(env, chunk, 0, length, (const jbyte *)(data + pos + 8));
			return chunk;
		}
		pos += 12 + (gsize)length;
	}
	return NULL;
}

JNIEXPORT jlong JNICALL Java_android_graphics_BitmapFactory_nativeDecodeStream(JNIEnv *env, jclass, jobject is, jbyteArray storage, jobject outPadding, jobject opts, jobjectArray outNinePatchChunk)
{
	GInputStream *stream = java_input_stream_new(env, is, storage);
	GOutputStream *memory = g_memory_output_stream_new_resizable();
	g_output_stream_splice(memory, stream, G_OUTPUT_STREAM_SPLICE_CLOSE_SOURCE | G_OUTPUT_STREAM_SPLICE_CLOSE_TARGET, NULL, NULL);
	g_object_unref(stream);
	GBytes *bytes = g_memory_output_stream_steal_as_bytes(G_MEMORY_OUTPUT_STREAM(memory));
	g_object_unref(memory);

	if (outNinePatchChunk) {
		jbyteArray chunk = find_ninepatch_chunk(env, bytes);
		if (chunk)
			(*env)->SetObjectArrayElement(env, outNinePatchChunk, 0, chunk);
	}

	/* honour Options.inSampleSize like AOSP: subsample the image by a power of two so an image
	 * loader (Coil/Glide) gets the smaller bitmap it asked for and does not keep the full-size one.
	 * inJustDecodeBounds only wants the full dimensions, so it is left unscaled. */
	int sample = 1;
	if (opts) {
		jclass opts_class = (*env)->GetObjectClass(env, opts);
		jboolean bounds_only = (*env)->GetBooleanField(env, opts, (*env)->GetFieldID(env, opts_class, "inJustDecodeBounds", "Z"));
		if (!bounds_only)
			sample = (*env)->GetIntField(env, opts, (*env)->GetFieldID(env, opts_class, "inSampleSize", "I"));
	}

	/* GTK decodes PNG, JPEG and TIFF itself, in process. gdk-pixbuf may hand every image to a sandboxed
	 * loader process (glycin), which costs about a millisecond even for a small icon, and apps decode
	 * many of those while they inflate a layout. */
	if (sample <= 1) {
		GdkTexture *texture = gdk_texture_new_from_bytes(bytes, NULL);
		if (texture) {
			g_bytes_unref(bytes);
			return _INTPTR(texture);
		}
	}

	GInputStream *memory_stream = g_memory_input_stream_new_from_bytes(bytes);
	g_bytes_unref(bytes);
	GdkPixbuf *pixbuf = gdk_pixbuf_new_from_stream(memory_stream, NULL, NULL);
	g_object_unref(memory_stream);

	if (pixbuf && sample > 1) {
		int factor = 1;
		while (factor * 2 <= sample) // round down to the nearest power of two, as AOSP's decoder does
			factor *= 2;
		int width = gdk_pixbuf_get_width(pixbuf) / factor;
		int height = gdk_pixbuf_get_height(pixbuf) / factor;
		GdkPixbuf *scaled = gdk_pixbuf_scale_simple(pixbuf, width < 1 ? 1 : width, height < 1 ? 1 : height, GDK_INTERP_BILINEAR);
		if (scaled) {
			g_object_unref(pixbuf);
			pixbuf = scaled;
		}
	}

	GdkTexture *texture = gdk_texture_new_for_pixbuf(pixbuf);
	g_object_unref(pixbuf);
	return _INTPTR(texture);
}
