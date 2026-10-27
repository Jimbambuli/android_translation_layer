#include <gtk/gtk.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_view_View.h"

#include "WrapperWidget.h"
#include "src/api-impl-jni/views/AndroidLayout.h"

G_DEFINE_TYPE(WrapperWidget, wrapper_widget, GTK_TYPE_WIDGET)

typedef enum { ATL_ID = 1,
	       ATL_ID_NAME,
	       ATL_CLASS_NAME,
	       ATL_SUPER_CLASS_NAMES,
	       N_PROPERTIES } WrapperWidgetProperty;
static GParamSpec *wrapper_widget_properties[N_PROPERTIES] = {
	NULL,
};

static void wrapper_widget_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec)
{
	switch ((WrapperWidgetProperty)property_id) {
		default:
			G_OBJECT_WARN_INVALID_PROPERTY_ID(object, property_id, pspec);
			break;
	}
}

static void wrapper_widget_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec)
{
	WrapperWidget *self = WRAPPER_WIDGET(object);

	JNIEnv *env = get_jni_env();

	jobject jobj = self->jobj;
	jclass class = _CLASS(jobj);

	switch ((WrapperWidgetProperty)property_id) {
		case ATL_ID: {
			jint id_jint = J__View__getId(env, jobj);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}

			const char *id = g_markup_printf_escaped("0x%08x", id_jint);
			g_value_set_string(value, id);
			break;
		}

		case ATL_ID_NAME: {
			jstring id_name_jstring = J__View__getIdName(env, jobj);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}

			const char *id_name = (*env)->GetStringUTFChars(env, id_name_jstring, NULL);
			g_value_set_string(value, id_name);

			(*env)->ReleaseStringUTFChars(env, id_name_jstring, id_name);
			break;
		}

		case ATL_CLASS_NAME: {
			jstring class_name_jstring = J__Class__getName(env, class);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}

			const char *class_name = (*env)->GetStringUTFChars(env, class_name_jstring, NULL);
			g_value_set_string(value, class_name);

			(*env)->ReleaseStringUTFChars(env, class_name_jstring, class_name);
			break;
		}

		case ATL_SUPER_CLASS_NAMES: {
			jstring super_classes_names_obj = J__View__getAllSuperClasses(env, jobj);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}

			const char *super_classes_names = (*env)->GetStringUTFChars(env, super_classes_names_obj, NULL);
			g_value_set_string(value, super_classes_names);

			(*env)->ReleaseStringUTFChars(env, super_classes_names_obj, super_classes_names);
			break;
		}

		default:
			G_OBJECT_WARN_INVALID_PROPERTY_ID(object, property_id, pspec);
			break;
	}
}

static void wrapper_widget_init(WrapperWidget *wrapper_widget)
{
	wrapper_widget->clip_children = TRUE;
}

static void wrapper_widget_dispose(GObject *wrapper_widget)
{
	GtkWidget *widget = GTK_WIDGET(wrapper_widget);
	g_clear_pointer(&WRAPPER_WIDGET(wrapper_widget)->outline_clip_path, gsk_path_unref);
	GtkWidget *child = gtk_widget_get_first_child(widget);
	while (child) {
		GtkWidget *_child = gtk_widget_get_next_sibling(child);
		gtk_widget_unparent(child);
		child = _child;
	}
	WrapperWidget *wrapper = WRAPPER_WIDGET(wrapper_widget);
	if (wrapper->jvm) {
		JNIEnv *env;
		(*wrapper->jvm)->GetEnv(wrapper->jvm, (void **)&env, JNI_VERSION_1_6);
		/* dispose may run more than once, and GTK may still call in afterwards */
		if (wrapper->jobj)
			_WEAK_UNREF(wrapper->jobj);
		wrapper->jobj = NULL;
		if (wrapper->canvas)
			_UNREF(wrapper->canvas);
		wrapper->canvas = NULL;
	}
	G_OBJECT_CLASS(wrapper_widget_parent_class)->dispose(wrapper_widget);
}

GtkSizeRequestMode wrapper_widget_get_request_mode(GtkWidget *widget)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	return gtk_widget_get_request_mode(wrapper->child);
}

void wrapper_widget_measure(GtkWidget *widget, GtkOrientation orientation, int for_size, int *minimum, int *natural, int *minimum_baseline, int *natural_baseline)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	gtk_widget_measure(wrapper->child, orientation, for_size, minimum, natural, minimum_baseline, natural_baseline);
	if (orientation == GTK_ORIENTATION_HORIZONTAL && (wrapper->layout_width > 0)) {
		*minimum = *natural = wrapper->layout_width;
	} else if (orientation == GTK_ORIENTATION_VERTICAL && (wrapper->layout_height > 0)) {
		*minimum = *natural = wrapper->layout_height;
	}
}

void wrapper_widget_allocate(GtkWidget *widget, int width, int height, int baseline)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	if (!width && !height) {
		width = wrapper->real_width;
		height = wrapper->real_height;
	}
	GtkAllocation allocation = {
		.x = 0,
		.y = 0,
		.width = width,
		.height = height,
	};

	if (wrapper->have_custom_computeScroll) {
		// The child needs to know its size before calling computeScroll, so we allocate it twice.
		// second allocate will not trigger onLayout, because of unchanged size
		gtk_widget_size_allocate(wrapper->child, &allocation, baseline);

		JNIEnv *env;
		(*wrapper->jvm)->GetEnv(wrapper->jvm, (void **)&env, JNI_VERSION_1_6);
		J__View__computeScroll(env, wrapper->jobj);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		allocation.x = -J__View__getScrollX(env, wrapper->jobj);
		allocation.y = -J__View__getScrollY(env, wrapper->jobj);
	}

	if (ATL_IS_ANDROID_LAYOUT(gtk_widget_get_layout_manager(wrapper->child))) {
		AndroidLayout *layout = ATL_ANDROID_LAYOUT(gtk_widget_get_layout_manager(wrapper->child));
		if (layout->real_width != width || layout->real_height != height) {
			layout->real_width = width;
			layout->real_height = height;
			if (!layout->needs_allocation)
				atl_safe_gtk_widget_queue_allocate(wrapper->child);
		}
		if (layout->needs_allocation)
			gtk_widget_size_allocate(wrapper->child, &allocation, baseline);
		else
			gtk_widget_size_allocate(wrapper->child, &(GtkAllocation){.x = allocation.x, .y = allocation.y}, baseline);
	} else {
		gtk_widget_size_allocate(wrapper->child, &allocation, baseline);
	}
	if (wrapper->background)
		gtk_widget_size_allocate(wrapper->background, &allocation, baseline);
}

/* SkDrawShadowMetrics (Skia, android-14) */
static const float kAmbientHeightFactor = 1.0f / 128.0f;
static const float kAmbientGeomFactor = 64.0f;
static const float kMaxAmbientRadius = 300 * (1.0f / 128.0f) * 64.0f;

static float divide_and_pin(float numer, float denom, float min, float max)
{
	return CLAMP(numer / denom, min, max);
}

static float ambient_blur_radius(float height)
{
	return MIN(height * kAmbientHeightFactor * kAmbientGeomFactor, kMaxAmbientRadius);
}

static float ambient_recip_alpha(float height)
{
	return 1.0f + MAX(height * kAmbientHeightFactor, 0.0f);
}

/* SkBlurMask::ConvertRadiusToSigma() */
static float radius_to_sigma(float radius)
{
	return radius > 0 ? 0.57735f * radius + 0.5f : 0.0f;
}

/* a rounded rectangle filled with color and blurred with a Gaussian of sigma; GSK's blur radius is
 * twice the standard deviation */
static void append_blurred(GdkSnapshot *snapshot, const GskRoundedRect *rect, const GdkRGBA *color, float sigma)
{
	gtk_snapshot_push_blur(snapshot, 2 * sigma);
	gtk_snapshot_push_rounded_clip(snapshot, rect);
	gtk_snapshot_append_color(snapshot, color, &rect->bounds);
	gtk_snapshot_pop(snapshot);
	gtk_snapshot_pop(snapshot);
}

/* The elevation shadow of AOSP: EndReorderBarrierDrawable::drawShadow() (hwui) draws it in the parent
 * right before the child, with SkShadowUtils::DrawShadow() for the outline at the child's Z and the
 * light source of the window. GSK draws Skia's blur variant of it: the ambient shadow is the outline
 * outset and blurred, the spot shadow the outline projected from the light onto the parent and
 * blurred (SkDrawShadowMetrics::GetSpotParams()). As in LightingInfo, the light is centered over the
 * window horizontally (ATL knows no display position) at lightY and lightZ. A translucent outline
 * (kTransparentOccluder) is drawn like an opaque one. */
void wrapper_widget_snapshot_shadow(GtkWidget *parent, GtkWidget *child, GdkSnapshot *snapshot)
{
	if (!WRAPPER_IS_WIDGET(child) || !gtk_widget_get_visible(child))
		return;
	WrapperWidget *wrapper = WRAPPER_WIDGET(child);
	float z = wrapper->shadow_z;
	const float caster_alpha = gtk_widget_get_opacity(child) * wrapper->shadow_alpha;
	graphene_point_t origin, window_origin;
	GtkWidget *root = GTK_WIDGET(gtk_widget_get_root(child));
	if (z <= 0 || caster_alpha <= 0 || !gtk_widget_compute_point(child, parent, &GRAPHENE_POINT_INIT(0, 0), &origin) ||
	    !gtk_widget_compute_point(child, root, &GRAPHENE_POINT_INIT(0, 0), &window_origin))
		return;
	const float ambient_alpha = wrapper->ambient_shadow_alpha * caster_alpha;
	const float spot_alpha = wrapper->spot_shadow_alpha * caster_alpha;
	/* the light relative to the child */
	const float light_x = gtk_widget_get_width(root) / 2.0f - window_origin.x;
	const float light_y = wrapper->light_y - window_origin.y;
	const float light_z = wrapper->light_z;

	gtk_snapshot_save(snapshot);
	gtk_snapshot_translate(snapshot, &origin);

	if (ambient_alpha > 0) {
		/* SkShadowUtils::DrawShadow(), "draw with blur" for the ambient shadow */
		float devSpaceOutset = ambient_blur_radius(z);
		float oneOverA = ambient_recip_alpha(z);
		float blurRadius = 0.5f * devSpaceOutset * oneOverA;
		float strokeWidth = 0.5f * (devSpaceOutset - blurRadius);
		GskRoundedRect ambient;
		gsk_rounded_rect_init_from_rect(&ambient, &wrapper->shadow_rect, wrapper->shadow_radius);
		/* kStrokeAndFill_Style outsets the path by half the stroke width */
		gsk_rounded_rect_shrink(&ambient, -strokeWidth / 2, -strokeWidth / 2, -strokeWidth / 2, -strokeWidth / 2);
		append_blurred(snapshot, &ambient, &(GdkRGBA){0, 0, 0, ambient_alpha}, radius_to_sigma(blurRadius));
	}

	if (spot_alpha > 0 && light_z > z) {
		/* SkDrawShadowMetrics::GetSpotShadowTransform() with GetSpotParams() */
		float zRatio = divide_and_pin(z, light_z - z, 0.0f, 0.95f);
		float radius = wrapper->light_radius * zRatio;
		float scale = divide_and_pin(light_z, light_z - z, 1.0f, 1.95f);
		const graphene_rect_t *r = &wrapper->shadow_rect;
		GskRoundedRect spot;
		gsk_rounded_rect_init_from_rect(&spot, &GRAPHENE_RECT_INIT(scale * r->origin.x - zRatio * light_x, scale * r->origin.y - zRatio * light_y,
		                                                           scale * r->size.width, scale * r->size.height),
		                                scale * wrapper->shadow_radius);
		append_blurred(snapshot, &spot, &(GdkRGBA){0, 0, 0, spot_alpha}, radius_to_sigma(radius));
	}

	gtk_snapshot_restore(snapshot);
}

void wrapper_widget_set_shadow(WrapperWidget *wrapper, graphene_rect_t *rect, float radius, float z, float alpha)
{
	wrapper->shadow_rect = *rect;
	wrapper->shadow_radius = radius;
	wrapper->shadow_z = z;
	wrapper->shadow_alpha = alpha;
	GtkWidget *parent = gtk_widget_get_parent(GTK_WIDGET(wrapper));
	gtk_widget_queue_draw(parent ? parent : GTK_WIDGET(wrapper));
}

void wrapper_widget_set_outline_clip(WrapperWidget *wrapper, gboolean enabled, graphene_rect_t *rect, float radius, GskPath *path)
{
	wrapper->has_outline_clip = enabled;
	gsk_rounded_rect_init_from_rect(&wrapper->outline_clip, rect, radius);
	g_clear_pointer(&wrapper->outline_clip_path, gsk_path_unref);
	if (path)
		wrapper->outline_clip_path = gsk_path_ref(path);
	gtk_widget_queue_draw(GTK_WIDGET(wrapper));
}

/* does the parent view clip its children (ViewGroup.setClipChildren)? The parent of a WrapperWidget
 * is the parent view's widget, whose parent is that view's WrapperWidget */
static gboolean parent_clips_children(GtkWidget *wrapper)
{
	GtkWidget *parent = gtk_widget_get_parent(wrapper);
	parent = parent ? gtk_widget_get_parent(parent) : NULL;
	return !parent || !WRAPPER_IS_WIDGET(parent) || WRAPPER_WIDGET(parent)->clip_children;
}

/* android.view.animation.Transformation.TYPE_ALPHA and TYPE_MATRIX */
#define TRANSFORMATION_TYPE_ALPHA 0x1
#define TRANSFORMATION_TYPE_MATRIX 0x2

/* this is used to avoid queing layout changes in the middle of snapshotting */
int snapshot_in_progress = 0;
static void wrapper_widget_snapshot(GtkWidget *widget, GdkSnapshot *snapshot)
{
	snapshot_in_progress++;

	WrapperWidget *wrapper = WRAPPER_WIDGET(widget);
	jobject transformation = NULL;
	gboolean push_opacity = FALSE;
	if (wrapper->animating && wrapper->jobj) {
		/* view animation: the transformation applies to everything the view draws (AOSP: View.draw() with applyLegacyAnimation()) */
		JNIEnv *env = get_jni_env();
		transformation = J__View__applyLegacyAnimation(env, wrapper->jobj);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		if (transformation) {
			int type = J__Transformation__getTransformationType(env, transformation);
			gtk_snapshot_save(snapshot);
			if (type & TRANSFORMATION_TYPE_MATRIX) {
				jobject matrix = J__Transformation__getMatrix(env, transformation);
				gtk_snapshot_transform_matrix(snapshot, _PTR(_GET_LONG_FIELD(matrix, "native_instance")));
				(*env)->DeleteLocalRef(env, matrix);
			}
			if (type & TRANSFORMATION_TYPE_ALPHA) {
				gtk_snapshot_push_opacity(snapshot, J__Transformation__getAlpha(env, transformation));
				push_opacity = TRUE;
			}
			(*env)->DeleteLocalRef(env, transformation);
		}
		if (!_GET_BOOL_FIELD(wrapper->jobj, "animationMore") && wrapper->animation_tick) {
			gtk_widget_remove_tick_callback(widget, wrapper->animation_tick);
			wrapper->animation_tick = 0;
		}
	}
	/* AOSP order: the bounds (if the parent clips its children), the clip bounds, the outline */
	int clips = 0;
	if (wrapper->real_height > 0 && wrapper->real_width > 0 && parent_clips_children(widget)) {
		gtk_snapshot_push_clip(snapshot, &GRAPHENE_RECT_INIT(0, 0, wrapper->real_width, wrapper->real_height));
		clips++;
	}
	if (wrapper->has_clip_bounds) {
		gtk_snapshot_push_clip(snapshot, &wrapper->clip_bounds);
		clips++;
	}
	if (wrapper->has_outline_clip) {
		if (wrapper->outline_clip_path)
			gtk_snapshot_push_fill(snapshot, wrapper->outline_clip_path, GSK_FILL_RULE_WINDING);
		else
			gtk_snapshot_push_rounded_clip(snapshot, &wrapper->outline_clip);
		clips++;
	}
	if (wrapper->have_custom_draw_method) {
		JNIEnv *env = get_jni_env();
		_SET_LONG_FIELD(wrapper->canvas, "snapshot", _INTPTR(snapshot));
		J__View__draw(env, wrapper->jobj, wrapper->canvas);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	} else {
		GtkWidget *widget = &wrapper->parent_instance;
		GtkWidget *child = gtk_widget_get_first_child(widget);
		while (child) {
			gtk_widget_snapshot_child(widget, child, snapshot);
			child = gtk_widget_get_next_sibling(child);
		}
	}
	while (clips--)
		gtk_snapshot_pop(snapshot);
	if (push_opacity)
		gtk_snapshot_pop(snapshot);
	if (transformation)
		gtk_snapshot_restore(snapshot);

	struct atl_scrim *scrim = g_object_get_data(G_OBJECT(widget), "atl-scrim");
	if (scrim && scrim->alpha > 0) {
		float w = gtk_widget_get_width(widget), h = gtk_widget_get_height(widget);
		GdkRGBA black = {0, 0, 0, scrim->alpha};
		if (scrim->outside) { // the parent clips this to where the other page shows
			gtk_snapshot_append_color(snapshot, &black, &GRAPHENE_RECT_INIT(-2 * w, -2 * h, 5 * w, 2 * h));
			gtk_snapshot_append_color(snapshot, &black, &GRAPHENE_RECT_INIT(-2 * w, h, 5 * w, 2 * h));
			gtk_snapshot_append_color(snapshot, &black, &GRAPHENE_RECT_INIT(-2 * w, 0, 2 * w, h));
			gtk_snapshot_append_color(snapshot, &black, &GRAPHENE_RECT_INIT(w, 0, 2 * w, h));
		} else {
			gtk_snapshot_append_color(snapshot, &black, &GRAPHENE_RECT_INIT(0, 0, w, h));
		}
	}

	snapshot_in_progress--;
}

static void wrapper_widget_class_init(WrapperWidgetClass *class)
{
	GObjectClass *object_class = G_OBJECT_CLASS(class);
	GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(class);

	object_class->dispose = wrapper_widget_dispose;

	widget_class->get_request_mode = wrapper_widget_get_request_mode;
	widget_class->measure = wrapper_widget_measure;
	widget_class->size_allocate = wrapper_widget_allocate;
	widget_class->snapshot = wrapper_widget_snapshot;

	object_class->set_property = wrapper_widget_set_property;
	object_class->get_property = wrapper_widget_get_property;

	// According to testing, these properties are not evaluated till we open the GtkInspector
	wrapper_widget_properties[ATL_ID] = g_param_spec_string("ATL-id", "ATL: ID", "ID of the component", "", G_PARAM_READABLE);
	wrapper_widget_properties[ATL_ID_NAME] = g_param_spec_string("ATL-id-name", "ATL: ID name", "Name of the ID of the component", "", G_PARAM_READABLE);
	wrapper_widget_properties[ATL_CLASS_NAME] = g_param_spec_string("ATL-class-name", "ATL: Class name", "Name of the class of the component", "", G_PARAM_READABLE);
	wrapper_widget_properties[ATL_SUPER_CLASS_NAMES] = g_param_spec_string("ATL-superclasses-names", "ATL: Super classes names", "Names of all the superclasses of the component class", "", G_PARAM_READABLE);

	g_object_class_install_properties(object_class, N_PROPERTIES, wrapper_widget_properties);
}

GtkWidget *wrapper_widget_new(void)
{
	return g_object_new(wrapper_widget_get_type(), NULL);
}

void wrapper_widget_set_child(WrapperWidget *parent, GtkWidget *child) // TODO: make sure there can only be one child
{
	gtk_widget_insert_before(child, GTK_WIDGET(parent), NULL);
	parent->child = child;
}

static guint queue_queue_redraw(GtkWidget *widget)
{
	gtk_widget_queue_draw(widget);
	g_object_unref(widget);
	return G_SOURCE_REMOVE;
}

static gboolean animation_tick(GtkWidget *widget, GdkFrameClock *frame_clock, gpointer user_data)
{
	gtk_widget_queue_draw(widget);
	return G_SOURCE_CONTINUE;
}

void wrapper_widget_set_animating(WrapperWidget *wrapper, gboolean animating)
{
	wrapper->animating = animating;
	if (animating && !wrapper->animation_tick) {
		wrapper->animation_tick = gtk_widget_add_tick_callback(GTK_WIDGET(wrapper), animation_tick, NULL, NULL);
	} else if (!animating && wrapper->animation_tick) {
		gtk_widget_remove_tick_callback(GTK_WIDGET(wrapper), wrapper->animation_tick);
		wrapper->animation_tick = 0;
	}
}

void wrapper_widget_queue_draw(WrapperWidget *wrapper)
{
	if (wrapper->have_custom_draw_method) {
		/* schedule the call to gtk_widget_queue_draw for a future event loop pass in case we're currently inside the snapshot */
		/* GTK+ uses G_PRIORITY_HIGH_IDLE + 10 for resizing operations, and G_PRIORITY_HIGH_IDLE + 20 for redrawing operations. */
		g_idle_add_full(G_PRIORITY_HIGH_IDLE + 20, G_SOURCE_FUNC(queue_queue_redraw), g_object_ref(wrapper), NULL);
	}

	if (wrapper->child)
		gtk_widget_queue_draw(wrapper->child);
	if (wrapper->have_custom_computeScroll) {
		atl_safe_gtk_widget_queue_allocate(GTK_WIDGET(wrapper));
	}
}

static bool on_click(GtkGestureClick *gesture, int n_press, double x, double y, WrapperWidget *wrapper)
{
	JNIEnv *env = get_jni_env();
	jobject this = (*env)->NewLocalRef(env, wrapper->jobj);
	if (!this)
		return false;

	bool ret = J__View__performClick(env, this);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	(*env)->DeleteLocalRef(env, this);
	return ret;
}

#define KEYCODE_0             7 /* KEYCODE_[1-9] = [8-16] */
#define KEYCODE_DPAD_UP       19
#define KEYCODE_DPAD_DOWN     20
#define KEYCODE_DPAD_LEFT     21
#define KEYCODE_DPAD_RIGHT    22
#define KEYCODE_A             29 /* KEYCODE_[B-Z] = [30-54] */
#define KEYCODE_COMMA         55
#define KEYCODE_PERIOD        56
#define KEYCODE_TAB           61
#define KEYCODE_SPACE         62
#define KEYCODE_ENTER         66
#define KEYCODE_DEL           67
#define KEYCODE_GRAVE         68
#define KEYCODE_MINUS         69
#define KEYCODE_EQUALS        70
#define KEYCODE_LEFT_BRACKET  71
#define KEYCODE_RIGHT_BRACKET 72
#define KEYCODE_BACKSLASH     73
#define KEYCODE_SEMICOLON     74
#define KEYCODE_APOSTROPHE    75
#define KEYCODE_SLASH         76
#define KEYCODE_AT            77
#define KEYCODE_PLUS          81
#define KEYCODE_PAGE_UP       92
#define KEYCODE_PAGE_DOWN     93
#define KEYCODE_ESCAPE        111
#define KEYCODE_FORWARD_DEL   112
#define KEYCODE_MOVE_HOME     122
#define KEYCODE_MOVE_END      123
#define KEYCODE_INSERT        124
#define KEYCODE_F1            131 /* KEYCODE_F[2-12] = [132-142] */
#define KEYCODE_NUMPAD_0      144 /* KEYCODE_NUMPAD_[1-9] = [145-153] */

static int map_key_code(int key_code)
{
	if (key_code >= GDK_KEY_0 && key_code <= GDK_KEY_9)
		return key_code - GDK_KEY_0 + KEYCODE_0;
	if (key_code >= GDK_KEY_a && key_code <= GDK_KEY_z)
		return key_code - GDK_KEY_a + KEYCODE_A;
	else if (key_code >= GDK_KEY_A && key_code <= GDK_KEY_Z)
		return key_code - GDK_KEY_A + KEYCODE_A;
	else if (key_code >= GDK_KEY_F1 && key_code <= GDK_KEY_F12)
		return key_code - GDK_KEY_F1 + KEYCODE_F1;
	else if (key_code >= GDK_KEY_KP_0 && key_code <= GDK_KEY_KP_9)
		return key_code - GDK_KEY_KP_0 + KEYCODE_NUMPAD_0;
	switch (key_code) {
		case GDK_KEY_Up:
			return KEYCODE_DPAD_UP;
		case GDK_KEY_Down:
			return KEYCODE_DPAD_DOWN;
		case GDK_KEY_Left:
			return KEYCODE_DPAD_LEFT;
		case GDK_KEY_Right:
			return KEYCODE_DPAD_RIGHT;
		case GDK_KEY_comma:
			return KEYCODE_COMMA;
		case GDK_KEY_period:
			return KEYCODE_PERIOD;
		case GDK_KEY_Tab:
		case GDK_KEY_ISO_Left_Tab: // Shift+Tab
			return KEYCODE_TAB;
		case GDK_KEY_space:
			return KEYCODE_SPACE;
		case GDK_KEY_Return:
			return KEYCODE_ENTER;
		case GDK_KEY_BackSpace:
			return KEYCODE_DEL;
		case GDK_KEY_grave:
			return KEYCODE_GRAVE;
		case GDK_KEY_minus:
			return KEYCODE_MINUS;
		case GDK_KEY_equal:
			return KEYCODE_EQUALS;
		case GDK_KEY_bracketleft:
			return KEYCODE_LEFT_BRACKET;
		case GDK_KEY_bracketright:
			return KEYCODE_RIGHT_BRACKET;
		case GDK_KEY_backslash:
			return KEYCODE_BACKSLASH;
		case GDK_KEY_semicolon:
			return KEYCODE_SEMICOLON;
		case GDK_KEY_apostrophe:
			return KEYCODE_APOSTROPHE;
		case GDK_KEY_slash:
			return KEYCODE_SLASH;
		case GDK_KEY_at:
			return KEYCODE_AT;
		case GDK_KEY_plus:
			return KEYCODE_PLUS;
		case GDK_KEY_Page_Up:
			return KEYCODE_PAGE_UP;
		case GDK_KEY_Page_Down:
			return KEYCODE_PAGE_DOWN;
		case GDK_KEY_Escape:
			return KEYCODE_ESCAPE;
		case GDK_KEY_Delete:
			return KEYCODE_FORWARD_DEL;
		case GDK_KEY_Home:
			return KEYCODE_MOVE_HOME;
		case GDK_KEY_End:
			return KEYCODE_MOVE_END;
		case GDK_KEY_Insert:
			return KEYCODE_INSERT;
		default:
			return key_code;
	}
}

#define META_SHIFT_ON     (1 << 0)
#define META_ALT_ON       (1 << 1)
#define META_CTRL_ON      (1 << 12)
#define META_META_ON      (1 << 16)
#define META_CAPS_LOCK_ON (1 << 20)

static int map_meta_state(GdkModifierType state)
{
	int meta_state = 0;
	if (state & GDK_SHIFT_MASK)
		meta_state |= META_SHIFT_ON;
	if (state & GDK_CONTROL_MASK)
		meta_state |= META_CTRL_ON;
	if (state & GDK_META_MASK)
		meta_state |= META_META_ON;
	if (state & GDK_ALT_MASK)
		meta_state |= META_ALT_ON;
	if (state & GDK_LOCK_MASK)
		meta_state |= META_CAPS_LOCK_ON;
	return meta_state;
}

#define ACTION_DOWN 0
#define ACTION_UP   1

static gboolean on_key_pressed(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, WrapperWidget *wrapper)
{
	JNIEnv *env = get_jni_env();

	jobject key_event = J_new__KeyEvent(env, (jlong)0, (jlong)0, ACTION_DOWN, map_key_code(keyval), 0, map_meta_state(state));
	_SET_INT_FIELD(key_event, "unicodeValue", gdk_keyval_to_unicode(keyval));
	gboolean ret = J__View__dispatchKeyEvent(env, wrapper->jobj, key_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return ret;
}

static gboolean on_key_released(GtkEventControllerKey *controller, guint keyval, guint keycode, GdkModifierType state, WrapperWidget *wrapper)
{
	JNIEnv *env = get_jni_env();

	jobject key_event = J_new__KeyEvent(env, (jlong)0, (jlong)0, ACTION_UP, map_key_code(keyval), 0, map_meta_state(state));
	_SET_INT_FIELD(key_event, "unicodeValue", gdk_keyval_to_unicode(keyval));
	gboolean ret = J__View__dispatchKeyEvent(env, wrapper->jobj, key_event);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return ret;
}

GtkWidget *currently_unmapping = NULL;
GtkRoot *currently_unmapping_root = NULL;

/* View.onFocusChanged(): the view is focused when its wrapper or the wrapped widget has the GTK
 * focus. Compose needs this to activate its focus tree, it rejects key events otherwise. The
 * focus is shown (state_focused) only when GTK shows it, i.e. after keyboard navigation, like
 * Android shows it only outside of touch mode. */
/* GTK turns focus-visible on after it moved the focus, so the focused view learns it here */
static void focus_visible_changed_cb(GtkWindow *window, GParamSpec *pspec, gpointer user_data)
{
	GtkWidget *focus = gtk_root_get_focus(GTK_ROOT(window));
	while (focus && !WRAPPER_IS_WIDGET(focus))
		focus = gtk_widget_get_parent(focus);
	if (!focus || !WRAPPER_WIDGET(focus)->jobj)
		return;
	JNIEnv *env = get_jni_env();
	jobject view = (*env)->NewLocalRef(env, WRAPPER_WIDGET(focus)->jobj);
	if (!view) // garbage collected
		return;
	J__View__dispatchFocusChanged(env, view, TRUE, gtk_window_get_focus_visible(window));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, view);
}

static void focus_changed_cb(GtkWidget *widget, GParamSpec *pspec, WrapperWidget *wrapper)
{
	gboolean focused = gtk_widget_has_focus(GTK_WIDGET(wrapper)) || (wrapper->child && gtk_widget_has_focus(wrapper->child));
	if (focused == wrapper->has_focus)
		return;
	wrapper->has_focus = focused;

	JNIEnv *env = get_jni_env();
	GtkRoot *root = gtk_widget_get_root(GTK_WIDGET(wrapper));
	gboolean visible = focused && GTK_IS_WINDOW(root) && gtk_window_get_focus_visible(GTK_WINDOW(root));
	if (GTK_IS_WINDOW(root) && !g_object_get_data(G_OBJECT(root), "atl-focus-visible-handler")) {
		g_signal_connect(root, "notify::focus-visible", G_CALLBACK(focus_visible_changed_cb), NULL);
		g_object_set_data(G_OBJECT(root), "atl-focus-visible-handler", GINT_TO_POINTER(1));
	}
	/* GTK moves the focus away from a removed widget only later, and its view may have been
	 * garbage collected by then (the wrapper holds a weak reference) */
	jobject view = (*env)->NewLocalRef(env, wrapper->jobj);
	if (!view)
		return;
	J__View__dispatchFocusChanged(env, view, focused, visible);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	(*env)->DeleteLocalRef(env, view);
}

static void root_changed_cb(WrapperWidget *wrapper, GParamSpec *pspec, gpointer user_data)
{
	GtkRoot *root = gtk_widget_get_root(GTK_WIDGET(wrapper));
	JNIEnv *env = get_jni_env();
	if (!wrapper->jobj) // the view was finalized
		return;

	/* like AOSP, onDetachedFromWindow() still sees the window (getViewTreeObserver() must return the
	 * window's observer, so that listeners added in onAttachedToWindow() can be removed). The old root
	 * is still alive here: it is either being destroyed or has just lost this widget. */
	if (root) {
		g_object_set_data(G_OBJECT(wrapper), "atl-root", root);
		J__View__dispatchAttachedToWindow(env, wrapper->jobj);
	} else {
		currently_unmapping = wrapper->child;
		currently_unmapping_root = g_object_get_data(G_OBJECT(wrapper), "atl-root");
		J__View__dispatchDetachedFromWindow(env, wrapper->jobj);
	}

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (!root) {
		currently_unmapping = NULL;
		currently_unmapping_root = NULL;
		g_object_set_data(G_OBJECT(wrapper), "atl-root", NULL);
	}
}

/* draw the view by calling its draw(), for views that draw something themselves (a foreground, or
 * an overridden onDraw()/draw()/dispatchDraw()) */
void wrapper_widget_draw_through_java(WrapperWidget *wrapper, JNIEnv *env)
{
	if (wrapper->have_custom_draw_method)
		return;
	wrapper->have_custom_draw_method = true;
	wrapper->canvas = _REF(J_new__GskCanvas(env, 0));
	gtk_widget_queue_draw(GTK_WIDGET(wrapper));
}

void wrapper_widget_set_jobject(WrapperWidget *wrapper, JNIEnv *env, jobject jobj)
{
	JavaVM *jvm;
	(*env)->GetJavaVM(env, &jvm);
	wrapper->jvm = jvm;
	wrapper->jobj = _WEAK_REF(jobj);
	jmethodID on_draw_method = _METHOD(_CLASS(jobj), "onDraw", "(Landroid/graphics/Canvas;)V");
	jmethodID dispatch_draw_method = _METHOD(_CLASS(jobj), "dispatchDraw", "(Landroid/graphics/Canvas;)V");
	jmethodID draw_method = _METHOD(_CLASS(jobj), "draw", "(Landroid/graphics/Canvas;)V");
	if (on_draw_method != _CACHED_METHOD(View__onDraw) || draw_method != _CACHED_METHOD(View__draw) || dispatch_draw_method != _CACHED_METHOD(View__dispatchDraw))
		wrapper_widget_draw_through_java(wrapper, env);

	jmethodID performClick_method = _METHOD(_CLASS(jobj), "performClick", "()Z");
	if (performClick_method != _CACHED_METHOD(View__performClick)) {
		GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_gesture_click_new());

		g_signal_connect(controller, "released", G_CALLBACK(on_click), wrapper);
		gtk_widget_add_controller(GTK_WIDGET(wrapper), controller);
		widget_set_needs_allocation(wrapper->child);
	}

	jmethodID ontouchevent_method = _METHOD(_CLASS(jobj), "onTouchEvent", "(Landroid/view/MotionEvent;)Z");
	jmethodID dispatchtouchevent_method = _METHOD(_CLASS(jobj), "dispatchTouchEvent", "(Landroid/view/MotionEvent;)Z");
	wrapper->custom_dispatch_touch = (dispatchtouchevent_method != _CACHED_METHOD(View__dispatchTouchEvent) && dispatchtouchevent_method != _CACHED_METHOD(ViewGroup__dispatchTouchEvent));
	if (ontouchevent_method != _CACHED_METHOD(View__onTouchEvent) || wrapper->custom_dispatch_touch) {
		_setOnTouchListener(env, jobj, GTK_WIDGET(wrapper));
	}

	jmethodID computeScroll_method = _METHOD(_CLASS(jobj), "computeScroll", "()V");
	if (computeScroll_method != _CACHED_METHOD(View__computeScroll)) {
		wrapper->have_custom_computeScroll = true;
	}

	jmethodID dispatch_key_event_method = _METHOD(_CLASS(jobj), "dispatchKeyEvent", "(Landroid/view/KeyEvent;)Z");
	jmethodID on_key_down_method = _METHOD(_CLASS(jobj), "onKeyDown", "(ILandroid/view/KeyEvent;)Z");
	if (dispatch_key_event_method != _CACHED_METHOD(View__dispatchKeyEvent) || on_key_down_method != _CACHED_METHOD(View__onKeyDown)) {
		GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_key_new());
		g_signal_connect(controller, "key-pressed", G_CALLBACK(on_key_pressed), wrapper);
		g_signal_connect(controller, "key-released", G_CALLBACK(on_key_released), wrapper);
		gtk_widget_add_controller(GTK_WIDGET(wrapper), controller);
		gtk_widget_set_focusable(GTK_WIDGET(wrapper), TRUE);
	}
	g_signal_connect(wrapper, "notify::root", G_CALLBACK(root_changed_cb), NULL);
	g_signal_connect(wrapper, "notify::has-focus", G_CALLBACK(focus_changed_cb), wrapper);
	if (wrapper->child)
		g_signal_connect(wrapper->child, "notify::has-focus", G_CALLBACK(focus_changed_cb), wrapper);
}

void wrapper_widget_set_layout_params(WrapperWidget *wrapper, int width, int height)
{
	wrapper->layout_width = width;
	wrapper->layout_height = height;
}

void wrapper_widget_set_background(WrapperWidget *wrapper, GdkPaintable *paintable)
{
	if (!paintable) {
		if (wrapper->background) {
			gtk_widget_unparent(wrapper->background);
			wrapper->background = NULL;
		}
		return;
	}
	if (!wrapper->background) {
		wrapper->background = gtk_picture_new();
		gtk_widget_insert_after(wrapper->background, GTK_WIDGET(wrapper), NULL);
	}
	gtk_picture_set_paintable(GTK_PICTURE(wrapper->background), paintable);
}

static gboolean on_touch_event_consume(GtkEventControllerLegacy *controller, GdkEvent *event)
{
	switch (gdk_event_get_event_type(event)) {
		case GDK_BUTTON_PRESS:
		case GDK_TOUCH_BEGIN:
		case GDK_BUTTON_RELEASE:
		case GDK_TOUCH_END:
		case GDK_MOTION_NOTIFY:
		case GDK_TOUCH_UPDATE:
			return TRUE;
		default:
			return FALSE;
	}
}

// Add default on touch listener, which just consumes all events to prevent bubbling to the parent
void wrapper_widget_consume_touch_events(WrapperWidget *wrapper)
{
	GtkEventController *old_controller = g_object_get_data(G_OBJECT(wrapper), "on_touch_listener");
	if (old_controller)
		return;
	GtkEventController *controller = GTK_EVENT_CONTROLLER(gtk_event_controller_legacy_new());
	g_signal_connect(controller, "event", G_CALLBACK(on_touch_event_consume), NULL);
	gtk_widget_add_controller(GTK_WIDGET(wrapper), controller);
	g_object_set_data(G_OBJECT(wrapper), "on_touch_listener", controller);
}

// ComposeUI layouts need to know when to redraw native widgets. There is no way in GTK to get
// notified about invalidations on specific widgets, but we get that global invalidation from the
// frame clock. Just flag it as invalidated on every global invalidation. The redraw will be a
// no-op in GTK anyway if redraw was not needed for the specific widget.
static void on_paint(GdkFrameClock *self, WrapperWidget *wrapper)
{
	JNIEnv *env = get_jni_env();
	J__View__propagateInvalidation(env, wrapper->jobj);
}

static void on_realize(GtkWidget *wrapper, gpointer user_data)
{
	GdkFrameClock *clock = gtk_widget_get_frame_clock(wrapper);
	g_signal_connect(clock, "paint", G_CALLBACK(on_paint), wrapper);
}

static void on_unrealize(GtkWidget *wrapper, gpointer user_data)
{
	GdkFrameClock *clock = gtk_widget_get_frame_clock(wrapper);
	g_signal_handlers_disconnect_by_data(clock, wrapper);
}

void wrapper_widget_register_invalidation_listener(WrapperWidget *wrapper)
{
	g_signal_connect(wrapper, "realize", G_CALLBACK(on_realize), NULL);
	g_signal_connect(wrapper, "unrealize", G_CALLBACK(on_unrealize), NULL);
}
