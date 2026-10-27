#include <gsk/gsk.h>
#include <math.h>

#include "../defines.h"

#include "../generated_headers/android_graphics_PathMeasure.h"

/* Android's PathMeasure walks the contours of a path one by one (nextContour()), GskPathMeasure
 * measures all of them as one. So the path is split into one GskPath per contour first (the
 * equivalent of Skia's SkContourMeasureIter, which skips contours without length), and each
 * contour gets its own GskPathMeasure. */

struct split {
	GPtrArray *contours; // GskPath *
	GskPathBuilder *cur;
	gboolean has_segment;
	gboolean closed;
	gboolean force_closed;
};

static void split_finish(struct split *s)
{
	if (!s->cur)
		return;
	if (s->has_segment) {
		if (s->force_closed && !s->closed)
			gsk_path_builder_close(s->cur);
		GskPath *path = gsk_path_builder_to_path(s->cur);
		GskPathMeasure *measure = gsk_path_measure_new(path);
		if (gsk_path_measure_get_length(measure) > 0)
			g_ptr_array_add(s->contours, path);
		else
			gsk_path_unref(path);
		gsk_path_measure_unref(measure);
	}
	gsk_path_builder_unref(s->cur);
	s->cur = NULL;
}

static gboolean split_op(GskPathOperation op, const graphene_point_t *pts, gsize n_pts, float weight, gpointer data)
{
	struct split *s = data;

	switch (op) {
	case GSK_PATH_MOVE:
		split_finish(s);
		s->cur = gsk_path_builder_new();
		s->has_segment = FALSE;
		s->closed = FALSE;
		gsk_path_builder_move_to(s->cur, pts[0].x, pts[0].y);
		break;
	case GSK_PATH_CLOSE:
		gsk_path_builder_close(s->cur);
		s->closed = TRUE;
		s->has_segment = TRUE;
		break;
	case GSK_PATH_LINE:
		gsk_path_builder_line_to(s->cur, pts[1].x, pts[1].y);
		s->has_segment = TRUE;
		break;
	case GSK_PATH_QUAD:
		gsk_path_builder_quad_to(s->cur, pts[1].x, pts[1].y, pts[2].x, pts[2].y);
		s->has_segment = TRUE;
		break;
	case GSK_PATH_CUBIC:
		gsk_path_builder_cubic_to(s->cur, pts[1].x, pts[1].y, pts[2].x, pts[2].y, pts[3].x, pts[3].y);
		s->has_segment = TRUE;
		break;
	case GSK_PATH_CONIC:
		gsk_path_builder_conic_to(s->cur, pts[1].x, pts[1].y, pts[2].x, pts[2].y, weight);
		s->has_segment = TRUE;
		break;
	}
	return TRUE;
}

/* the contours of the path as GskPath pointers (each owned by the caller); forceClosed closes the open ones */
JNIEXPORT jlongArray JNICALL Java_android_graphics_PathMeasure_native_1split(JNIEnv *env, jclass this, jlong path_ptr, jboolean force_closed)
{
	struct split s = { .contours = g_ptr_array_new(), .force_closed = force_closed };

	gsk_path_foreach(_PTR(path_ptr), GSK_PATH_FOREACH_ALLOW_QUAD | GSK_PATH_FOREACH_ALLOW_CUBIC | GSK_PATH_FOREACH_ALLOW_CONIC, split_op, &s);
	split_finish(&s);

	jlongArray result = (*env)->NewLongArray(env, s.contours->len);
	if (result) {
		jlong *elements = (*env)->GetLongArrayElements(env, result, NULL);
		for (guint i = 0; i < s.contours->len; i++)
			elements[i] = _INTPTR(g_ptr_array_index(s.contours, i));
		(*env)->ReleaseLongArrayElements(env, result, elements, 0);
	} else {
		for (guint i = 0; i < s.contours->len; i++)
			gsk_path_unref(g_ptr_array_index(s.contours, i));
	}
	g_ptr_array_free(s.contours, TRUE);
	return result;
}

JNIEXPORT void JNICALL Java_android_graphics_PathMeasure_native_1free_1path(JNIEnv *env, jclass this, jlong path_ptr)
{
	if (path_ptr)
		gsk_path_unref(_PTR(path_ptr));
}

JNIEXPORT jlong JNICALL Java_android_graphics_PathMeasure_native_1measure_1new(JNIEnv *env, jclass this, jlong path_ptr)
{
	return _INTPTR(gsk_path_measure_new(_PTR(path_ptr)));
}

JNIEXPORT void JNICALL Java_android_graphics_PathMeasure_native_1measure_1free(JNIEnv *env, jclass this, jlong measure_ptr)
{
	if (measure_ptr)
		gsk_path_measure_unref(_PTR(measure_ptr));
}

JNIEXPORT jfloat JNICALL Java_android_graphics_PathMeasure_native_1length(JNIEnv *env, jclass this, jlong measure_ptr)
{
	return gsk_path_measure_get_length(_PTR(measure_ptr));
}

JNIEXPORT jboolean JNICALL Java_android_graphics_PathMeasure_native_1is_1closed(JNIEnv *env, jclass this, jlong path_ptr)
{
	return gsk_path_is_closed(_PTR(path_ptr));
}

/* position and unit tangent at distance, which Java has clamped to [0, length] */
JNIEXPORT jboolean JNICALL Java_android_graphics_PathMeasure_native_1pos_1tan(JNIEnv *env, jclass this, jlong path_ptr, jlong measure_ptr,
                                                                              jfloat distance, jfloatArray pos_array, jfloatArray tan_array)
{
	GskPath *path = _PTR(path_ptr);
	GskPathMeasure *measure = _PTR(measure_ptr);
	GskPathPoint point;

	if (!gsk_path_measure_get_point(measure, distance, &point))
		return JNI_FALSE;
	if (pos_array) {
		graphene_point_t position;
		gsk_path_point_get_position(&point, path, &position);
		jfloat pos[2] = { position.x, position.y };
		(*env)->SetFloatArrayRegion(env, pos_array, 0, 2, pos);
	}
	if (tan_array) {
		graphene_vec2_t tangent;
		gsk_path_point_get_tangent(&point, path, distance >= gsk_path_measure_get_length(measure) ? GSK_PATH_FROM_START : GSK_PATH_TO_END, &tangent);
		jfloat tan[2] = { graphene_vec2_get_x(&tangent), graphene_vec2_get_y(&tangent) };
		(*env)->SetFloatArrayRegion(env, tan_array, 0, 2, tan);
	}
	return JNI_TRUE;
}

struct append {
	GskPathBuilder *dst;
	gboolean skip_move;
};

static gboolean append_op(GskPathOperation op, const graphene_point_t *pts, gsize n_pts, float weight, gpointer data)
{
	struct append *a = data;

	switch (op) {
	case GSK_PATH_MOVE:
		if (!a->skip_move)
			gsk_path_builder_move_to(a->dst, pts[0].x, pts[0].y);
		a->skip_move = FALSE;
		break;
	case GSK_PATH_CLOSE:
		gsk_path_builder_close(a->dst);
		break;
	case GSK_PATH_LINE:
		gsk_path_builder_line_to(a->dst, pts[1].x, pts[1].y);
		break;
	case GSK_PATH_QUAD:
		gsk_path_builder_quad_to(a->dst, pts[1].x, pts[1].y, pts[2].x, pts[2].y);
		break;
	case GSK_PATH_CUBIC:
		gsk_path_builder_cubic_to(a->dst, pts[1].x, pts[1].y, pts[2].x, pts[2].y, pts[3].x, pts[3].y);
		break;
	case GSK_PATH_CONIC:
		gsk_path_builder_conic_to(a->dst, pts[1].x, pts[1].y, pts[2].x, pts[2].y, weight);
		break;
	}
	return TRUE;
}

/* the part from start to end (0 <= start < end <= length) appended to the builder; without
 * startWithMoveTo it continues from the builder's current point */
JNIEXPORT jboolean JNICALL Java_android_graphics_PathMeasure_native_1segment(JNIEnv *env, jclass this, jlong path_ptr, jlong measure_ptr,
                                                                             jfloat start, jfloat end, jlong dst_builder_ptr, jboolean start_with_move_to)
{
	GskPath *path = _PTR(path_ptr);
	GskPathMeasure *measure = _PTR(measure_ptr);
	GskPathBuilder *dst = _PTR(dst_builder_ptr);
	GskPathPoint start_point, end_point;

	if (!gsk_path_measure_get_point(measure, start, &start_point) || !gsk_path_measure_get_point(measure, end, &end_point))
		return JNI_FALSE;

	GskPathBuilder *tmp = gsk_path_builder_new();
	gsk_path_builder_add_segment(tmp, path, &start_point, &end_point);
	GskPath *segment = gsk_path_builder_free_to_path(tmp);

	struct append a = { dst, !start_with_move_to && gsk_path_builder_get_current_point(dst) != NULL };
	gsk_path_foreach(segment, GSK_PATH_FOREACH_ALLOW_QUAD | GSK_PATH_FOREACH_ALLOW_CUBIC | GSK_PATH_FOREACH_ALLOW_CONIC, append_op, &a);
	gsk_path_unref(segment);
	return JNI_TRUE;
}
