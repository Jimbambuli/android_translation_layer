#include <gsk/gsk.h>
#include <gtk/gtk.h>

#include "../defines.h"
#include "../widgets/WrapperWidget.h"

#include "../generated_headers/android_view_RenderNode.h"

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeCreateSnapshot(JNIEnv *env, jobject this)
{
	return _INTPTR(gtk_snapshot_new());
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeCreateNode(JNIEnv *env, jobject this, jlong snapshot_ptr)
{
	GskRenderNode *node = gtk_snapshot_free_to_node(GTK_SNAPSHOT(_PTR(snapshot_ptr)));
	if (!node)
		node = gsk_container_node_new(NULL, 0);
	return _INTPTR(node);
}

static GskRenderNode *patch_node(GskRenderNode *node, GskRenderNode *old_child, GskRenderNode *new_child)
{
	if (node == old_child)
		return gsk_render_node_ref(new_child);
	GskRenderNode *new_node;
	GskRenderNodeType type = gsk_render_node_get_node_type(node);
	switch (type) {
		case GSK_CONTAINER_NODE: {
			guint n_children = gsk_container_node_get_n_children(node);
			GskRenderNode *children[n_children];
			gboolean modified = FALSE;
			for (guint i = 0; i < n_children; i++) {
				children[i] = patch_node(gsk_container_node_get_child(node, i), old_child, new_child);
				modified |= children[i] != gsk_container_node_get_child(node, i);
			}
			if (modified)
				new_node = gsk_container_node_new(children, n_children);
			else
				new_node = gsk_render_node_ref(node);
			for (guint i = 0; i < n_children; i++)
				gsk_render_node_unref(children[i]);
			break;
		}
		case GSK_TRANSFORM_NODE: {
			GskRenderNode *child = patch_node(gsk_transform_node_get_child(node), old_child, new_child);
			if (child != gsk_transform_node_get_child(node))
				new_node = gsk_transform_node_new(child, gsk_transform_node_get_transform(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_CLIP_NODE: {
			GskRenderNode *child = patch_node(gsk_clip_node_get_child(node), old_child, new_child);
			if (child != gsk_clip_node_get_child(node))
				new_node = gsk_clip_node_new(child, gsk_clip_node_get_clip(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_OPACITY_NODE: {
			GskRenderNode *child = patch_node(gsk_opacity_node_get_child(node), old_child, new_child);
			if (child != gsk_opacity_node_get_child(node))
				new_node = gsk_opacity_node_new(child, gsk_opacity_node_get_opacity(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_ROUNDED_CLIP_NODE: {
			GskRenderNode *child = patch_node(gsk_rounded_clip_node_get_child(node), old_child, new_child);
			if (child != gsk_rounded_clip_node_get_child(node))
				new_node = gsk_rounded_clip_node_new(child, gsk_rounded_clip_node_get_clip(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_COLOR_MATRIX_NODE: {
			GskRenderNode *child = patch_node(gsk_color_matrix_node_get_child(node), old_child, new_child);
			if (child != gsk_color_matrix_node_get_child(node))
				new_node = gsk_color_matrix_node_new(child, gsk_color_matrix_node_get_color_matrix(node), gsk_color_matrix_node_get_color_offset(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_FILL_NODE: {
			GskRenderNode *child = patch_node(gsk_fill_node_get_child(node), old_child, new_child);
			if (child != gsk_fill_node_get_child(node))
				new_node = gsk_fill_node_new(child, gsk_fill_node_get_path(node), gsk_fill_node_get_fill_rule(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(child);
			break;
		}
		case GSK_MASK_NODE: {
			GskRenderNode *source = patch_node(gsk_mask_node_get_source(node), old_child, new_child);
			if (source != gsk_mask_node_get_source(node))
				new_node = gsk_mask_node_new(source, gsk_mask_node_get_mask(node), gsk_mask_node_get_mask_mode(node));
			else
				new_node = gsk_render_node_ref(node);
			gsk_render_node_unref(source);
			break;
		}
		default:
			new_node = gsk_render_node_ref(node);
	}
	return new_node;
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativePatchNode(JNIEnv *env, jobject this, jlong node_ptr, jlong old_child_ptr, jlong new_child_ptr)
{
	GskRenderNode *old_node = _PTR(node_ptr);
	GskRenderNode *new_node = patch_node(old_node, _PTR(old_child_ptr), _PTR(new_child_ptr));
	gsk_render_node_unref(old_node);
	return _INTPTR(new_node);
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeTransform(JNIEnv *env, jobject this, jlong node_ptr, jfloat scale_x, jfloat scale_y, jfloat translate_x, jfloat translate_y, jfloat rotate, jfloat pivot_x, jfloat pivot_y)
{
	GskRenderNode *node = _PTR(node_ptr);
	GskTransform *transform = NULL;
	GskRenderNode *transformed_node;
	/* hwui: translate, preRotate and preScale around the pivot; GSK applies the last operation first */
	transform = gsk_transform_translate(transform, &GRAPHENE_POINT_INIT(translate_x + pivot_x, translate_y + pivot_y));
	transform = gsk_transform_rotate(transform, rotate);
	transform = gsk_transform_scale(transform, scale_x, scale_y);
	transform = gsk_transform_translate(transform, &GRAPHENE_POINT_INIT(-pivot_x, -pivot_y));
	if (transform) {
		transformed_node = gsk_transform_node_new(node, transform);
		gsk_transform_unref(transform);
	} else {
		transformed_node = gsk_render_node_ref(node);
	}
	return _INTPTR(transformed_node);
}

/* android.graphics.RenderNode: like hwui, clip in the node's own coordinates, then apply the
 * alpha and the transforms (position and translation, scale and rotation around the pivot) */
JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeApplyProperties(JNIEnv *env, jobject this, jlong node_ptr, jfloat width, jfloat height, jboolean clip_to_bounds, jboolean clip_to_rect, jfloat clip_left, jfloat clip_top, jfloat clip_right, jfloat clip_bottom, jfloat clip_radius, jlong shadow_ptr, jfloat scale_x, jfloat scale_y, jfloat translate_x, jfloat translate_y, jfloat rotate, jfloat pivot_x, jfloat pivot_y, jfloat alpha)
{
	GskRenderNode *node = gsk_render_node_ref(_PTR(node_ptr));
	GskRenderNode *new_node;
	if (clip_to_bounds) {
		new_node = gsk_clip_node_new(node, &GRAPHENE_RECT_INIT(0, 0, width, height));
		gsk_render_node_unref(node);
		node = new_node;
	}
	if (clip_to_rect) {
		graphene_rect_t clip = GRAPHENE_RECT_INIT(clip_left, clip_top, clip_right - clip_left, clip_bottom - clip_top);
		if (clip_radius > 0) {
			GskRoundedRect rounded;
			gsk_rounded_rect_init_from_rect(&rounded, &clip, clip_radius);
			new_node = gsk_rounded_clip_node_new(node, &rounded);
		} else {
			new_node = gsk_clip_node_new(node, &clip);
		}
		gsk_render_node_unref(node);
		node = new_node;
	}
	if (alpha < 1.f) {
		new_node = gsk_opacity_node_new(node, MAX(alpha, 0.f));
		gsk_render_node_unref(node);
		node = new_node;
	}
	/* the shadow is drawn before the node and outside its clip; its alpha includes the node's */
	if (shadow_ptr) {
		GskRenderNode *children[] = {_PTR(shadow_ptr), node};
		new_node = gsk_container_node_new(children, 2);
		gsk_render_node_unref(node);
		node = new_node;
	}
	new_node = _PTR(Java_android_view_RenderNode_nativeTransform(env, this, _INTPTR(node), scale_x, scale_y, translate_x, translate_y, rotate, pivot_x, pivot_y));
	gsk_render_node_unref(node);
	return _INTPTR(new_node);
}

/* the elevation shadow of an outline (see atl_snapshot_shadow()); hwui places the light relative to the window,
 * which a render node does not know: here it is over the outline's center */
JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeCreateShadowNode(JNIEnv *env, jclass this_class, jfloat left, jfloat top, jfloat right, jfloat bottom, jfloat radius, jfloat z, jint ambient_color, jint spot_color, jfloat light_z, jfloat light_radius, jboolean transparent)
{
	GtkSnapshot *snapshot = gtk_snapshot_new();
	const GdkRGBA ambient = {((ambient_color >> 16) & 0xff) / 255.f, ((ambient_color >> 8) & 0xff) / 255.f, (ambient_color & 0xff) / 255.f, ((ambient_color >> 24) & 0xff) / 255.f};
	const GdkRGBA spot = {((spot_color >> 16) & 0xff) / 255.f, ((spot_color >> 8) & 0xff) / 255.f, (spot_color & 0xff) / 255.f, ((spot_color >> 24) & 0xff) / 255.f};
	atl_snapshot_shadow(snapshot, &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top), radius, z, &ambient, &spot,
	                    (left + right) / 2, (top + bottom) / 2, light_z, light_radius, transparent);
	return _INTPTR(gtk_snapshot_free_to_node(snapshot));
}

/* RenderEffect: a blur of sigma (GSK's radius is twice the standard deviation) and an offset */
JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeApplyEffect(JNIEnv *env, jclass this_class, jlong node_ptr, jfloat sigma, jfloat offset_x, jfloat offset_y)
{
	GskRenderNode *node = gsk_render_node_ref(_PTR(node_ptr));
	GskRenderNode *new_node;
	if (sigma > 0) {
		new_node = gsk_blur_node_new(node, 2 * sigma);
		gsk_render_node_unref(node);
		node = new_node;
	}
	if (offset_x != 0 || offset_y != 0) {
		GskTransform *transform = gsk_transform_translate(NULL, &GRAPHENE_POINT_INIT(offset_x, offset_y));
		new_node = gsk_transform_node_new(node, transform);
		gsk_transform_unref(transform);
		gsk_render_node_unref(node);
		node = new_node;
	}
	return _INTPTR(node);
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeClip(JNIEnv *env, jobject this, jlong node_ptr, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
	GskRenderNode *node = _PTR(node_ptr);
	GskRenderNode *clipped_node = gsk_clip_node_new(node, &GRAPHENE_RECT_INIT(left, top, right - left, bottom - top));
	gsk_render_node_unref(node);
	return _INTPTR(clipped_node);
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeStretch(JNIEnv *env, jobject this, jlong node_ptr, jfloat width, jfloat height, jfloat stretch_x, jfloat stretch_y)
{
	return _INTPTR(atl_stretch_node(_PTR(node_ptr), width, height, stretch_x, stretch_y));
}

JNIEXPORT void JNICALL Java_android_view_RenderNode_nativeUnref(JNIEnv *env, jobject this, jlong node_ptr)
{
	if (node_ptr != 0)
		gsk_render_node_unref(_PTR(node_ptr));
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeAddStubNode(JNIEnv *env, jobject this, jlong snapshot_ptr)
{
	GtkSnapshot *snapshot = _PTR(snapshot_ptr);
	GdkRGBA rgba = {
		.red = 0.f,
		.green = 0.f,
		.blue = 0.f,
		.alpha = 0.f,
	};
	GskRenderNode *stub_node = gsk_color_node_new(&rgba, &GRAPHENE_RECT_INIT(0.0f, 0.0f, 0.0f, 0.0f));
	gtk_snapshot_append_node(snapshot, stub_node);
	gsk_render_node_unref(stub_node);
	return _INTPTR(stub_node);
}

JNIEXPORT jlong JNICALL Java_android_view_RenderNode_nativeCreateEmpyNode(JNIEnv *env, jobject this)
{
	return _INTPTR(gsk_container_node_new(NULL, 0));
}
