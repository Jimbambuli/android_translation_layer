#include <gsk/gsk.h>

#include "../defines.h"

#include "../generated_headers/android_graphics_Path.h"

JNIEXPORT jlong JNICALL Java_android_graphics_Path_native_1create_1builder(JNIEnv *env, jclass this, jlong path_ptr, jlong builder_ptr)
{
	GskPathBuilder *builder = _PTR(builder_ptr);
	if (!builder)
		builder = gsk_path_builder_new();
	if (path_ptr) {
		GskPath *path = _PTR(path_ptr);
		gsk_path_builder_add_path(builder, path);
		gsk_path_unref(path);
	}
	return _INTPTR(builder);
}

JNIEXPORT jlong JNICALL Java_android_graphics_Path_native_1create_1path(JNIEnv *env, jclass this, jlong builder_ptr)
{
	GskPathBuilder *builder = _PTR(builder_ptr);
	if (!builder)
		builder = gsk_path_builder_new();
	return _INTPTR(gsk_path_builder_to_path(builder));
}

JNIEXPORT jlong JNICALL Java_android_graphics_Path_native_1ref_1path(JNIEnv *env, jclass this, jlong path_ptr)
{
	return _INTPTR(gsk_path_ref(_PTR(path_ptr)));
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1reset(JNIEnv *env, jclass this, jlong path_ptr, jlong builder_ptr)
{
	if (path_ptr)
		gsk_path_unref(_PTR(path_ptr));
	if (builder_ptr)
		gsk_path_builder_unref(_PTR(builder_ptr));
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1close(JNIEnv *env, jclass this, jlong builder_ptr)
{
	gsk_path_builder_close(_PTR(builder_ptr));
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1move_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x, jfloat y)
{
	gsk_path_builder_move_to(_PTR(builder_ptr), x, y);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1line_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x, jfloat y)
{
	gsk_path_builder_line_to(_PTR(builder_ptr), x, y);
}

/* translated to gsk_path_builder_svg_arc_to, maybe there are more performant ways to implement this? */
void Java_android_graphics_Path_native_1arc_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom,
                                                jfloat start_angle_deg, jfloat sweep_angle_deg, jboolean force_move_to)
{
	GskPathBuilder *builder = _PTR(builder_ptr);

	/* compute ellipse center and radii */
	const graphene_point_t center = GRAPHENE_POINT_INIT((left + right) / 2.0,
	                                                    (top + bottom) / 2.0);
	const double rx = fabs(right - left) / 2.0;
	const double ry = fabs(bottom - top) / 2.0;

	/* compute points on the ellipse from angles */
	const double start_angle = DEG2RAD(start_angle_deg);
	const double end_angle = DEG2RAD(start_angle_deg + sweep_angle_deg);

	graphene_point_t p1;
	graphene_point_t p2;
	graphene_point_init(&p1,
	                    center.x + rx * cos(start_angle),
	                    center.y + ry * sin(start_angle));
	graphene_point_init(&p2,
	                    center.x + rx * cos(end_angle),
	                    center.y + ry * sin(end_angle));

	/* handle force_move_to */
	if (force_move_to)
		gsk_path_builder_move_to(builder, p1.x, p1.y);
	else if (!graphene_point_equal(gsk_path_builder_get_current_point(builder), &p1))
		gsk_path_builder_line_to(builder, p1.x, p1.y);

	/* compute flags */
	const gboolean large_arc_flag = (fabs(sweep_angle_deg) >= 180.0);
	const gboolean sweep_flag = (sweep_angle_deg > 0.0);

	gsk_path_builder_svg_arc_to(builder, rx, ry, 0.0, large_arc_flag, sweep_flag, p2.x, p2.y);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1cubic_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x1, jfloat y1, jfloat x2, jfloat y2, jfloat x3, jfloat y3)
{
	gsk_path_builder_cubic_to(_PTR(builder_ptr), x1, y1, x2, y2, x3, y3);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1quad_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x1, jfloat y1, jfloat x2, jfloat y2)
{
	gsk_path_builder_quad_to(_PTR(builder_ptr), x1, y1, x2, y2);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1rel_1move_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x, jfloat y)
{
	gsk_path_builder_rel_move_to(_PTR(builder_ptr), x, y);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1rel_1line_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x, jfloat y)
{
	gsk_path_builder_rel_line_to(_PTR(builder_ptr), x, y);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1rel_1cubic_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x1, jfloat y1, jfloat x2, jfloat y2, jfloat x3, jfloat y3)
{
	gsk_path_builder_rel_cubic_to(_PTR(builder_ptr), x1, y1, x2, y2, x3, y3);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1rel_1quad_1to(JNIEnv *env, jclass this, jlong builder_ptr, jfloat x1, jfloat y1, jfloat x2, jfloat y2)
{
	gsk_path_builder_rel_quad_to(_PTR(builder_ptr), x1, y1, x2, y2);
}

struct path_foreach_data {
	GskPathBuilder *builder;
	graphene_matrix_t *matrix;
	graphene_point_t tmp_pts[4];
};
static gboolean path_foreach_transform(GskPathOperation op, const graphene_point_t *pts, gsize n_pts, float weight, gpointer user_data)
{
	struct path_foreach_data *data = user_data;
	for (gsize i = 0; i < n_pts; i++) {
		graphene_matrix_transform_point(data->matrix, &pts[i], &data->tmp_pts[i]);
	}
	switch (op) {
		case GSK_PATH_MOVE:
			gsk_path_builder_move_to(data->builder, data->tmp_pts[0].x, data->tmp_pts[0].y);
			break;
		case GSK_PATH_CLOSE:
			gsk_path_builder_close(data->builder);
			break;
		case GSK_PATH_LINE:
			gsk_path_builder_line_to(data->builder, data->tmp_pts[1].x, data->tmp_pts[1].y);
			break;
		case GSK_PATH_QUAD:
			gsk_path_builder_quad_to(data->builder, data->tmp_pts[1].x, data->tmp_pts[1].y, data->tmp_pts[2].x, data->tmp_pts[2].y);
			break;
		case GSK_PATH_CUBIC:
			gsk_path_builder_cubic_to(data->builder, data->tmp_pts[1].x, data->tmp_pts[1].y, data->tmp_pts[2].x, data->tmp_pts[2].y, data->tmp_pts[3].x, data->tmp_pts[3].y);
			break;
		case GSK_PATH_CONIC:
			gsk_path_builder_conic_to(data->builder, data->tmp_pts[1].x, data->tmp_pts[1].y, data->tmp_pts[2].x, data->tmp_pts[2].y, weight);
			break;
	}
	return TRUE;
}

void Java_android_graphics_Path_native_1add_1arc(JNIEnv *env, jclass this, jlong builder_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom,
                                                 jfloat start_angle_deg, jfloat sweep_angle_deg)
{
	GskPathBuilder *builder = _PTR(builder_ptr);
	GskPath *path;
	GskPathBuilder *arc_builder = gsk_path_builder_new();
	Java_android_graphics_Path_native_1arc_1to(env, this, _INTPTR(arc_builder), left, top, right, bottom, start_angle_deg, sweep_angle_deg, true);
	path = gsk_path_builder_free_to_path(arc_builder);
	gsk_path_builder_add_path(builder, path);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1add_1path(JNIEnv *env, jclass this, jlong builder_ptr, jlong path_ptr, jlong matrix_ptr)
{
	GskPathBuilder *builder = _PTR(builder_ptr);
	GskPath *path = _PTR(path_ptr);
	graphene_matrix_t *matrix = (graphene_matrix_t *)_PTR(matrix_ptr);
	if (graphene_matrix_is_identity(matrix)) {
		gsk_path_builder_add_path(builder, path);
	} else {
		struct path_foreach_data data = {
			.builder = builder,
			.matrix = matrix,
		};
		gsk_path_foreach(path, GSK_PATH_FOREACH_ALLOW_QUAD | GSK_PATH_FOREACH_ALLOW_CUBIC | GSK_PATH_FOREACH_ALLOW_CONIC, path_foreach_transform, &data);
	}
}

JNIEXPORT jlong JNICALL Java_android_graphics_Path_native_1transform(JNIEnv *env, jclass this, jlong path_ptr, jlong matrix_ptr)
{
	GskPath *path = _PTR(path_ptr);
	graphene_matrix_t *matrix = (graphene_matrix_t *)_PTR(matrix_ptr);
	struct path_foreach_data data = {
		.builder = gsk_path_builder_new(),
		.matrix = matrix,
	};
	gsk_path_foreach(path, GSK_PATH_FOREACH_ALLOW_QUAD | GSK_PATH_FOREACH_ALLOW_CUBIC | GSK_PATH_FOREACH_ALLOW_CONIC, path_foreach_transform, &data);
	gsk_path_unref(path);
	return _INTPTR(data.builder);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1add_1rect(JNIEnv *env, jclass this, jlong builder_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
	gsk_path_builder_add_rect(_PTR(builder_ptr), &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top));
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1add_1round_1rect(JNIEnv *env, jclass this, jlong builder_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloatArray radii_jobj)
{
	jfloat *radii = (*env)->GetFloatArrayElements(env, radii_jobj, NULL);
	GskRoundedRect round_rect = {
		.bounds = GRAPHENE_RECT_INIT(left, top, right - left, bottom - top),
		.corner = {{radii[0], radii[1]}, {radii[2], radii[3]}, {radii[4], radii[5]}, {radii[6], radii[7]}},
	};
	(*env)->ReleaseFloatArrayElements(env, radii_jobj, radii, 0);
	gsk_path_builder_add_rounded_rect(_PTR(builder_ptr), &round_rect);
}

JNIEXPORT void JNICALL Java_android_graphics_Path_native_1get_1bounds(JNIEnv *env, jclass this, jlong path_ptr, jobject bounds)
{
	graphene_rect_t rect;
	gsk_path_get_bounds(_PTR(path_ptr), &rect);
	_SET_FLOAT_FIELD(bounds, "left", rect.origin.x);
	_SET_FLOAT_FIELD(bounds, "top", rect.origin.y);
	_SET_FLOAT_FIELD(bounds, "right", rect.origin.x + rect.size.width);
	_SET_FLOAT_FIELD(bounds, "bottom", rect.origin.y + rect.size.height);
}

JNIEXPORT jboolean JNICALL Java_android_graphics_Path_native_1contains(JNIEnv *env, jclass class, jlong path_ptr, jint fill_rule, jboolean inverse, jfloat x, jfloat y)
{
	return gsk_path_in_fill(_PTR(path_ptr), &GRAPHENE_POINT_INIT(x, y), fill_rule) ^ inverse;
}

/* the segments of a GskPath as SkPath verbs (PathIterator.VERB_*) with the points of Skia's iterator: the start
 * point first (but for a move), a conic's weight at index 6 */
#define VERB_MOVE 0
#define VERB_LINE 1
#define VERB_QUAD 2
#define VERB_CONIC 3
#define VERB_CUBIC 4
#define VERB_CLOSE 5

struct path_segment {
	float verb;
	float pts[8];
};

static gboolean path_foreach_collect(GskPathOperation op, const graphene_point_t *pts, gsize n_pts, float weight, gpointer user_data)
{
	GArray *segments = user_data;
	struct path_segment s = {0};
	switch (op) {
		case GSK_PATH_MOVE:
			s.verb = VERB_MOVE;
			break;
		case GSK_PATH_CLOSE:
			s.verb = VERB_CLOSE;
			break;
		case GSK_PATH_LINE:
			s.verb = VERB_LINE;
			break;
		case GSK_PATH_QUAD:
			s.verb = VERB_QUAD;
			break;
		case GSK_PATH_CUBIC:
			s.verb = VERB_CUBIC;
			break;
		case GSK_PATH_CONIC:
			s.verb = VERB_CONIC;
			break;
	}
	for (gsize i = 0; i < n_pts && i < 4; i++) {
		s.pts[2 * i] = pts[i].x;
		s.pts[2 * i + 1] = pts[i].y;
	}
	if (op == GSK_PATH_CONIC)
		s.pts[6] = weight;
	g_array_append_val(segments, s);
	return TRUE;
}

static GArray *path_segments(GskPath *path)
{
	GArray *segments = g_array_new(FALSE, FALSE, sizeof(struct path_segment));
	gsk_path_foreach(path, GSK_PATH_FOREACH_ALLOW_QUAD | GSK_PATH_FOREACH_ALLOW_CUBIC | GSK_PATH_FOREACH_ALLOW_CONIC, path_foreach_collect, segments);
	return segments;
}

/* for PathIterator: 9 floats per segment, the verb and the 8 point values */
JNIEXPORT jfloatArray JNICALL Java_android_graphics_Path_native_1get_1segments(JNIEnv *env, jclass this, jlong path_ptr)
{
	GArray *segments = path_segments(_PTR(path_ptr));
	jsize n = segments->len * (sizeof(struct path_segment) / sizeof(float));
	jfloatArray array = (*env)->NewFloatArray(env, n);
	(*env)->SetFloatArrayRegion(env, array, 0, n, (const jfloat *)segments->data);
	g_array_free(segments, TRUE);
	return array;
}

/* number of points the verb adds (SkPathPriv::PtsInVerb) */
static int points_in_verb(int verb)
{
	switch (verb) {
		case VERB_MOVE:
		case VERB_LINE:
			return 1;
		case VERB_QUAD:
		case VERB_CONIC:
			return 2;
		case VERB_CUBIC:
			return 3;
		default:
			return 0;
	}
}

static const graphene_point_t *segment_point(const struct path_segment *s, int i)
{
	return (const graphene_point_t *)&s->pts[2 * i];
}

/* SkPath.cpp's Convexicator: the direction must turn the same way at every point, it may reverse at most twice */
enum dir_change { DIR_LEFT, DIR_RIGHT, DIR_STRAIGHT, DIR_BACKWARDS, DIR_INVALID, DIR_UNKNOWN };

struct convexicator {
	graphene_point_t first_pt, last_pt;
	graphene_vec2_t first_vec, last_vec;
	enum dir_change expected_dir;
	int reversals;
};

static enum dir_change direction_change(struct convexicator *c, float vx, float vy)
{
	float lx = graphene_vec2_get_x(&c->last_vec), ly = graphene_vec2_get_y(&c->last_vec);
	float cross = lx * vy - ly * vx;
	if (!isfinite(cross))
		return DIR_UNKNOWN;
	if (cross == 0)
		return lx * vx + ly * vy < 0 ? DIR_BACKWARDS : DIR_STRAIGHT;
	return cross > 0 ? DIR_RIGHT : DIR_LEFT;
}

static bool convexicator_add_vec(struct convexicator *c, float vx, float vy)
{
	enum dir_change dir = direction_change(c, vx, vy);
	switch (dir) {
		case DIR_LEFT:
		case DIR_RIGHT:
			if (c->expected_dir == DIR_INVALID)
				c->expected_dir = dir;
			else if (dir != c->expected_dir)
				return false;
			graphene_vec2_init(&c->last_vec, vx, vy);
			break;
		case DIR_STRAIGHT:
			break;
		case DIR_BACKWARDS:
			graphene_vec2_init(&c->last_vec, vx, vy);
			return ++c->reversals < 3;
		default:
			return false;
	}
	return true;
}

static void convexicator_set_move_pt(struct convexicator *c, const graphene_point_t *pt)
{
	c->first_pt = c->last_pt = *pt;
	c->expected_dir = DIR_INVALID;
}

static bool convexicator_add_pt(struct convexicator *c, const graphene_point_t *pt)
{
	if (graphene_point_equal(&c->last_pt, pt))
		return true;
	float vx = pt->x - c->last_pt.x, vy = pt->y - c->last_pt.y;
	if (graphene_point_equal(&c->first_pt, &c->last_pt) && c->expected_dir == DIR_INVALID) {
		graphene_vec2_init(&c->last_vec, vx, vy);
		c->first_vec = c->last_vec;
	} else if (!convexicator_add_vec(c, vx, vy)) {
		return false;
	}
	c->last_pt = *pt;
	return true;
}

static bool convexicator_close(struct convexicator *c)
{
	return convexicator_add_pt(c, &c->first_pt) && convexicator_add_vec(c, graphene_vec2_get_x(&c->first_vec), graphene_vec2_get_y(&c->first_vec));
}

/* SkPath.cpp IsConcaveBySign(): the x and y signs of the edges may change at most three times each */
static bool is_concave_by_sign(const graphene_point_t *points, int count)
{
	if (count <= 3)
		return false;
	const graphene_point_t *curr = &points[0];
	int dxes = 0, dyes = 0, last_sx = 2, last_sy = 2;
	for (int i = 1; i <= count; i++) {
		const graphene_point_t *p = &points[i % count];
		float vx = p->x - curr->x, vy = p->y - curr->y;
		if (vx != 0 || vy != 0) {
			if (!isfinite(vx) || !isfinite(vy))
				return true;
			int sx = vx < 0, sy = vy < 0;
			dxes += sx != last_sx;
			dyes += sy != last_sy;
			if (dxes > 3 || dyes > 3)
				return true;
			last_sx = sx;
			last_sy = sy;
		}
		curr = p;
	}
	return false;
}

/* AOSP Path.isConvex() -> SkPath::isConvex(), computeConvexity() in SkPath.cpp (Skia not in the local sources):
 * computed from the control points; an empty path, a point or a line count as convex, a second contour does not */
JNIEXPORT jboolean JNICALL Java_android_graphics_Path_native_1is_1convex(JNIEnv *env, jclass this, jlong path_ptr)
{
	GArray *segments = path_segments(_PTR(path_ptr));
	struct path_segment *seg = (struct path_segment *)segments->data;
	jboolean convex = JNI_TRUE;

	/* the points up to the last move (Skia leaves out a trailing move) for the sign test */
	GArray *points = g_array_new(FALSE, FALSE, sizeof(graphene_point_t));
	guint last_move = segments->len;
	while (last_move > 0 && (int)seg[last_move - 1].verb == VERB_MOVE)
		last_move--;
	for (guint i = 0; i < last_move; i++) {
		int verb = seg[i].verb;
		int first = verb == VERB_MOVE ? 0 : 1;
		for (int k = first; k < first + points_in_verb(verb); k++)
			g_array_append_val(points, *segment_point(&seg[i], k));
	}
	if (is_concave_by_sign((graphene_point_t *)points->data, points->len)) {
		convex = JNI_FALSE;
		goto out;
	}

	struct convexicator state = {.expected_dir = DIR_INVALID};
	int contour_count = 0;
	bool needs_close = false;
	for (guint i = 0; i < segments->len; i++) {
		int verb = seg[i].verb;
		if (contour_count == 0) {
			if (verb == VERB_MOVE) {
				convexicator_set_move_pt(&state, segment_point(&seg[i], 0));
			} else {
				contour_count = 1;
				needs_close = true;
			}
		}
		if (contour_count == 1) {
			if (verb == VERB_CLOSE || verb == VERB_MOVE) {
				if (!convexicator_close(&state)) {
					convex = JNI_FALSE;
					goto out;
				}
				needs_close = false;
				contour_count++;
			} else {
				for (int k = 1; k <= points_in_verb(verb); k++) {
					if (!convexicator_add_pt(&state, segment_point(&seg[i], k))) {
						convex = JNI_FALSE;
						goto out;
					}
				}
			}
		} else if (contour_count > 1 && verb != VERB_MOVE) {
			convex = JNI_FALSE; // more than one contour
			goto out;
		}
	}
	if (needs_close && !convexicator_close(&state))
		convex = JNI_FALSE;
out:
	g_array_free(points, TRUE);
	g_array_free(segments, TRUE);
	return convex;
}

/* AOSP Path.setLastPoint() -> SkPath::setLastPt(): replaces the last point, a move to it on an empty path */
JNIEXPORT jlong JNICALL Java_android_graphics_Path_native_1set_1last_1point(JNIEnv *env, jclass this, jlong path_ptr, jfloat x, jfloat y)
{
	GskPath *path = _PTR(path_ptr);
	GArray *segments = path_segments(path);
	gsk_path_unref(path);
	struct path_segment *seg = (struct path_segment *)segments->data;
	GskPathBuilder *builder = gsk_path_builder_new();
	gint last = segments->len - 1;
	while (last >= 0 && (int)seg[last].verb == VERB_CLOSE)
		last--;
	if (last < 0) {
		gsk_path_builder_move_to(builder, x, y);
	} else {
		int verb = seg[last].verb;
		int k = verb == VERB_MOVE ? 0 : points_in_verb(verb);
		seg[last].pts[2 * k] = x;
		seg[last].pts[2 * k + 1] = y;
	}
	for (guint i = 0; i < segments->len; i++) {
		const float *p = seg[i].pts;
		switch ((int)seg[i].verb) {
			case VERB_MOVE:
				gsk_path_builder_move_to(builder, p[0], p[1]);
				break;
			case VERB_LINE:
				gsk_path_builder_line_to(builder, p[2], p[3]);
				break;
			case VERB_QUAD:
				gsk_path_builder_quad_to(builder, p[2], p[3], p[4], p[5]);
				break;
			case VERB_CONIC:
				gsk_path_builder_conic_to(builder, p[2], p[3], p[4], p[5], p[6]);
				break;
			case VERB_CUBIC:
				gsk_path_builder_cubic_to(builder, p[2], p[3], p[4], p[5], p[6], p[7]);
				break;
			case VERB_CLOSE:
				gsk_path_builder_close(builder);
				break;
		}
	}
	g_array_free(segments, TRUE);
	return _INTPTR(builder);
}
