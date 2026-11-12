#include <gtk/gtk.h>
#include <arpa/inet.h>
#include <string.h>

#include "../defines.h"
#include "../util.h"
#include "NinePatchPaintable.h"

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

/* AOSP NinePatchPeeker::getPadding(): the padding of the 9-patch chunk (paddingLeft/Right/Top/Bottom, stored big-endian
 * in the PNG, see Res_png_9patch::fileToDevice) goes to the Rect, -1 for every side without a chunk. The chunk is not
 * scaled here, the NinePatchDrawable scales the padding to the density it is shown at. */
static void set_padding(JNIEnv *env, jobject rect, jbyteArray chunk)
{
	int32_t values[4] = { -1, -1, -1, -1 }; // left, top, right, bottom
	jbyte raw[sizeof(struct Res_png_9patch)];

	if (chunk && (*env)->GetArrayLength(env, chunk) >= (jsize)sizeof(raw)) {
		(*env)->GetByteArrayRegion(env, chunk, 0, sizeof(raw), raw);
		const struct Res_png_9patch *patch = (const struct Res_png_9patch *)raw;
		values[0] = ntohl(patch->paddingLeft);
		values[1] = ntohl(patch->paddingTop);
		values[2] = ntohl(patch->paddingRight);
		values[3] = ntohl(patch->paddingBottom);
	}
	jclass rect_class = (*env)->GetObjectClass(env, rect);
	(*env)->SetIntField(env, rect, (*env)->GetFieldID(env, rect_class, "left", "I"), values[0]);
	(*env)->SetIntField(env, rect, (*env)->GetFieldID(env, rect_class, "top", "I"), values[1]);
	(*env)->SetIntField(env, rect, (*env)->GetFieldID(env, rect_class, "right", "I"), values[2]);
	(*env)->SetIntField(env, rect, (*env)->GetFieldID(env, rect_class, "bottom", "I"), values[3]);
}

JNIEXPORT jlong JNICALL Java_android_graphics_BitmapFactory_nativeDecodeStream(JNIEnv *env, jclass, jobject is, jbyteArray storage, jobject outPadding, jobject opts, jobjectArray outNinePatchChunk)
{
	GInputStream *stream = java_input_stream_new(env, is, storage);
	GOutputStream *memory = g_memory_output_stream_new_resizable();
	g_output_stream_splice(memory, stream, G_OUTPUT_STREAM_SPLICE_CLOSE_SOURCE | G_OUTPUT_STREAM_SPLICE_CLOSE_TARGET, NULL, NULL);
	g_object_unref(stream);
	GBytes *bytes = g_memory_output_stream_steal_as_bytes(G_MEMORY_OUTPUT_STREAM(memory));
	g_object_unref(memory);

	jbyteArray chunk = NULL;
	if (outNinePatchChunk || outPadding) {
		chunk = find_ninepatch_chunk(env, bytes);
		if (chunk && outNinePatchChunk)
			(*env)->SetObjectArrayElement(env, outNinePatchChunk, 0, chunk);
	}

	/* honour Options.inSampleSize like AOSP: subsample the image by a power of two so an image
	 * loader (Coil/Glide) gets the smaller bitmap it asked for and does not keep the full-size one.
	 * inJustDecodeBounds only wants the full dimensions, so it is left unscaled. */
	int sample = 1;
	jboolean bounds_only = JNI_FALSE;
	if (opts) {
		jclass opts_class = (*env)->GetObjectClass(env, opts);
		bounds_only = (*env)->GetBooleanField(env, opts, (*env)->GetFieldID(env, opts_class, "inJustDecodeBounds", "Z"));
		if (!bounds_only)
			sample = (*env)->GetIntField(env, opts, (*env)->GetFieldID(env, opts_class, "inSampleSize", "I"));
	}

	if (outPadding && !bounds_only) // like AOSP, a decode of the bounds leaves the padding alone
		set_padding(env, outPadding, chunk);

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
	GError *error = NULL;
	GdkPixbuf *pixbuf = gdk_pixbuf_new_from_stream(memory_stream, NULL, &error);
	g_object_unref(memory_stream);

	/* like AOSP's decoder (nullObjectReturn() in BitmapFactory.cpp): data that cannot be decoded
	 * logs a line and gives no bitmap, so decodeStream() returns null */
	if (!pixbuf) {
		fprintf(stderr, "--- BitmapFactory: failed to decode image: %s\n", error ? error->message : "unknown error");
		g_clear_error(&error);
		return 0;
	}

	if (sample > 1) {
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
