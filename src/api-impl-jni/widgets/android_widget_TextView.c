#include <gtk/gtk.h>
#include <hb.h>

#include "../defines.h"
#include "../util.h"

#include "WrapperWidget.h"

#include "../generated_headers/android_widget_TextView.h"

static GtkLabel *box_get_label(JNIEnv *env, GtkWidget *box)
{
	GtkWidget *label = gtk_widget_get_last_child(GTK_WIDGET(box));
	if (label && !GTK_IS_LABEL(label))
		label = gtk_widget_get_prev_sibling(label);
	return GTK_IS_LABEL(label) ? GTK_LABEL(label) : NULL;
}

/* links in the markup stand for the ClickableSpans of the text, the TextView handles them */
static gboolean on_activate_link(GtkLabel *label, const char *uri, gpointer user_data)
{
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(gtk_widget_get_parent(GTK_WIDGET(label))));
	jmethodID onLinkActivated = _METHOD((*env)->FindClass(env, "android/widget/TextView"), "onLinkActivated", "(Ljava/lang/String;)Z");
	jboolean ret = (*env)->CallBooleanMethod(env, wrapper->jobj, onLinkActivated, _JSTRING(uri));
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return ret;
}

JNIEXPORT jlong JNICALL Java_android_widget_TextView_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	const char *text = attribute_set_get_string(env, attrs, "text", NULL);

	//	_SET_OBJ_FIELD(this, "text", "Ljava/lang/String;", _JSTRING(text)); //TODO: sadly this might be needed, but it's not atm

	GtkWidget *wrapper = g_object_ref_sink(wrapper_widget_new());
	GtkWidget *box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
	GtkWidget *label = gtk_label_new(text);
	gtk_label_set_wrap(GTK_LABEL(label), TRUE);
	gtk_label_set_xalign(GTK_LABEL(label), 0.f);
	gtk_label_set_yalign(GTK_LABEL(label), 0.f);
	gtk_widget_set_hexpand(label, TRUE);
	gtk_box_append(GTK_BOX(box), label);
	g_signal_connect(label, "activate-link", G_CALLBACK(on_activate_link), NULL);
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), box);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);

	PangoAttrList *pango_attrs = pango_attr_list_new();
	pango_attr_list_insert(pango_attrs, pango_attr_font_features_new("tnum"));
	gtk_label_set_attributes(GTK_LABEL(label), pango_attrs);
	pango_attr_list_unref(pango_attrs);

	return _INTPTR(box);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setText(JNIEnv *env, jobject this, jobject charseq)
{
	const char *text = charseq ? (*env)->GetStringUTFChars(env, charseq, NULL) : NULL;
	atl_safe_gtk_label_set_text(box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget"))), text ?: "");
	if (text)
		(*env)->ReleaseStringUTFChars(env, charseq, text);
}

/* we kinda need per-widget css */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setTextColor(JNIEnv *env, jobject this, jint color)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(_GET_LONG_FIELD(this, "widget")));

	GtkStyleContext *style_context = gtk_widget_get_style_context(widget);

	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(widget), "color_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));

	GtkCssProvider *css_provider = gtk_css_provider_new();

	char *css_string = g_markup_printf_escaped("* { color: #%06x%02x; }", color & 0xFFFFFF, (color >> 24) & 0xFF);
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);

	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_set_data(G_OBJECT(widget), "color_style_provider", css_provider);
}

/* family, weight and slant of the typeface as per-widget CSS, which also reaches the
 * label of buttons and the text of entries; the family is only known for app fonts */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setTypeface(JNIEnv *env, jobject this, jlong widget_ptr, jobject typeface)
{
	GtkWidget *widget = GTK_WIDGET(_PTR(widget_ptr));
	GtkStyleContext *style_context = gtk_widget_get_style_context(widget);

	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(widget), "typeface_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));
	g_object_set_data(G_OBJECT(widget), "typeface_style_provider", NULL);
	if (!typeface)
		return;

	jstring family = _GET_OBJ_FIELD(typeface, "family", "Ljava/lang/String;");
	const char *family_str = family ? (*env)->GetStringUTFChars(env, family, NULL) : NULL;
	char *family_css = family_str ? g_markup_printf_escaped("font-family: \"%s\";", family_str) : g_strdup("");
	if (family_str)
		(*env)->ReleaseStringUTFChars(env, family, family_str);

	char *css_string = g_strdup_printf("* { %s font-weight: %d; font-style: %s; }", family_css,
	                                   _GET_INT_FIELD(typeface, "weight"), _GET_BOOL_FIELD(typeface, "italic") ? "italic" : "normal");
	GtkCssProvider *css_provider = gtk_css_provider_new();
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);
	g_free(family_css);

	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_set_data_full(G_OBJECT(widget), "typeface_style_provider", css_provider, g_object_unref);
}
#pragma GCC diagnostic pop

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setTextSize(JNIEnv *env, jobject this, jfloat size)
{
	GtkLabel *label = box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget")));
	PangoAttrList *attrs;

	PangoAttrList *old_attrs = gtk_label_get_attributes(label);
	if (old_attrs)
		attrs = pango_attr_list_copy(old_attrs);
	else
		attrs = pango_attr_list_new();

	PangoAttribute *size_attr = pango_attr_size_new_absolute(size * PANGO_SCALE);
	pango_attr_list_change(attrs, size_attr);
	gtk_label_set_attributes(label, attrs);

	pango_attr_list_unref(attrs);
}

/* Paint.UNDERLINE_TEXT_FLAG and STRIKE_THRU_TEXT_FLAG of the TextView's paint: Skia draws them for the whole
 * text, here they are Pango attributes of the whole label */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setPaintFlags(JNIEnv *env, jobject this, jint flags)
{
	GtkWidget *box = _PTR(_GET_LONG_FIELD(this, "widget"));
	if (!GTK_IS_BOX(box)) // EditText (GtkText) and Button have widgets of their own
		return;
	GtkLabel *label = box_get_label(env, box);
	if (!label)
		return;
	PangoAttrList *old_attrs = gtk_label_get_attributes(label);
	PangoAttrList *attrs = old_attrs ? pango_attr_list_copy(old_attrs) : pango_attr_list_new();
	pango_attr_list_change(attrs, pango_attr_underline_new((flags & 0x08) ? PANGO_UNDERLINE_SINGLE : PANGO_UNDERLINE_NONE));
	pango_attr_list_change(attrs, pango_attr_strikethrough_new((flags & 0x10) != 0));
	gtk_label_set_attributes(label, attrs);
	pango_attr_list_unref(attrs);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setLines(JNIEnv *env, jobject this, jint lines, jint ellipsize_mode)
{
	GtkLabel *label = box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget")));
	if (!label)
		return;
	gtk_label_set_lines(label, lines);
	gtk_label_set_ellipsize(label, ellipsize_mode);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setAlignment(JNIEnv *env, jobject this, jfloat xalign, jfloat yalign)
{
	GtkWidget *widget = _PTR(_GET_LONG_FIELD(this, "widget"));
	/* a Button has the box of its label in a GtkButton */
	GtkLabel *label = box_get_label(env, GTK_IS_BUTTON(widget) ? gtk_button_get_child(GTK_BUTTON(widget)) : widget);
	if (!label)
		return;
	gtk_label_set_xalign(label, xalign);
	gtk_label_set_yalign(label, yalign);
	gtk_label_set_justify(label, xalign == 0.5f ? GTK_JUSTIFY_CENTER : xalign == 1.f ? GTK_JUSTIFY_RIGHT : GTK_JUSTIFY_LEFT);
}

/* AOSP's TextView includes the font padding by default (includeFontPadding): its first line starts
 * at the top of the font's bounding box (yMax of the head table, Skia's FontMetrics.top) instead of
 * its ascent, and its last line ends at the bottom (yMin) instead of the descent. The difference
 * goes to the label as a margin. */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setFontPadding(JNIEnv *env, jobject this, jlong widget_ptr, jboolean include)
{
	GtkLabel *label = box_get_label(env, _PTR(widget_ptr));
	if (!label)
		return;
	int top = 0, bottom = 0;
	PangoLayoutLine *line = include ? pango_layout_get_line_readonly(gtk_label_get_layout(label), 0) : NULL;
	if (line && line->runs) {
		PangoFont *font = ((PangoGlyphItem *)line->runs->data)->item->analysis.font;
		hb_font_t *hb_font = pango_font_get_hb_font(font);
		hb_blob_t *head = hb_face_reference_table(hb_font_get_face(hb_font), HB_TAG('h', 'e', 'a', 'd'));
		unsigned int length;
		const uint8_t *data = (const uint8_t *)hb_blob_get_data(head, &length);
		hb_font_extents_t extents;
		if (length >= 44 && hb_font_get_h_extents(hb_font, &extents)) {
			int units_per_em = data[18] << 8 | data[19];
			int y_min = (int16_t)(data[38] << 8 | data[39]);
			int y_max = (int16_t)(data[42] << 8 | data[43]);
			/* the size the run is laid out at, from the HarfBuzz font like its ascent and descent:
			 * pango's font description and metrics of a bitmap font (color emoji) are those of its
			 * strike (109 px for Noto Color Emoji), which made an 18 px flag label 146 px high */
			int x_scale, y_scale;
			hb_font_get_scale(hb_font, &x_scale, &y_scale);
			float size = (float)y_scale / PANGO_SCALE;
			top = MAX(0, (int)roundf(y_max * size / units_per_em - (float)extents.ascender / PANGO_SCALE));
			bottom = MAX(0, (int)roundf(-y_min * size / units_per_em + (float)extents.descender / PANGO_SCALE));
		}
		hb_blob_destroy(head);
	}
	int padding = top << 16 | bottom;
	if (GPOINTER_TO_INT(g_object_get_data(G_OBJECT(label), "font_padding")) == padding)
		return;
	g_object_set_data(G_OBJECT(label), "font_padding", GINT_TO_POINTER(padding));
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
	GtkStyleContext *style_context = gtk_widget_get_style_context(GTK_WIDGET(label));
	GtkCssProvider *old_provider = g_object_get_data(G_OBJECT(label), "font_padding_style_provider");
	if (old_provider)
		gtk_style_context_remove_provider(style_context, GTK_STYLE_PROVIDER(old_provider));
	GtkCssProvider *css_provider = gtk_css_provider_new();
	char *css_string = g_strdup_printf("* { margin-top: %dpx; margin-bottom: %dpx; }", top, bottom);
	gtk_css_provider_load_from_string(css_provider, css_string);
	g_free(css_string);
	gtk_style_context_add_provider(style_context, GTK_STYLE_PROVIDER(css_provider), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
#pragma GCC diagnostic pop
	g_object_set_data_full(G_OBJECT(label), "font_padding_style_provider", css_provider, g_object_unref);
}

/* the letter spacing of the label in pixels (TextView.setLetterSpacing() in ems times the text size); the label of a
 * Button is the one in the GtkButton's box */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setLetterSpacing(JNIEnv *env, jobject this, jlong widget_ptr, jfloat spacing)
{
	GtkWidget *widget = _PTR(widget_ptr);
	GtkLabel *label = box_get_label(env, GTK_IS_BUTTON(widget) ? gtk_button_get_child(GTK_BUTTON(widget)) : widget);
	if (!label)
		return;
	PangoAttrList *old_attrs = gtk_label_get_attributes(label);
	PangoAttrList *attrs = old_attrs ? pango_attr_list_copy(old_attrs) : pango_attr_list_new();
	pango_attr_list_change(attrs, pango_attr_letter_spacing_new(spacing * PANGO_SCALE));
	gtk_label_set_attributes(label, attrs);
	pango_attr_list_unref(attrs);
}

/* the baseline of the first line of the label at the given width, moved by the label's vertical
 * alignment within the given height */
JNIEXPORT jint JNICALL Java_android_widget_TextView_native_1getBaseline(JNIEnv *env, jobject this, jlong widget_ptr, jint width, jint height)
{
	GtkLabel *label = box_get_label(env, _PTR(widget_ptr));
	if (!label)
		return -1;
	int natural, baseline, minimum_width;
	/* the width comes from the layout and may be below the label's minimum; GTK would warn and measure for the minimum */
	gtk_widget_measure(GTK_WIDGET(label), GTK_ORIENTATION_HORIZONTAL, -1, &minimum_width, NULL, NULL, NULL);
	gtk_widget_measure(GTK_WIDGET(label), GTK_ORIENTATION_VERTICAL, width > 0 ? MAX(width, minimum_width) : -1, NULL, &natural, NULL, &baseline);
	if (baseline < 0)
		return -1;
	return baseline + MAX(height - natural, 0) * gtk_label_get_yalign(label);
}

/* the markup, with the placeholders of its ReplacementSpans: places is (UTF-16 index of the placeholder character in
 * the text of the markup, width, height, y relative to the baseline) per span, in pixels. Pango leaves a gap of that
 * size (PangoAttrShape); GtkLabel does not draw a shape, the TextView has the span draw there (native_getPlaceholderPosition) */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setMarkup(JNIEnv *env, jobject this, jstring markup_jstr, jintArray places_array)
{
	GtkLabel *label = box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget")));
	if (!label)
		return;
	const char *markup = (*env)->GetStringUTFChars(env, markup_jstr, NULL);
	gtk_label_set_markup(label, markup);
	(*env)->ReleaseStringUTFChars(env, markup_jstr, markup);

	PangoAttrList *pango_attrs = pango_attr_list_new();
	pango_attr_list_insert(pango_attrs, pango_attr_font_features_new("tnum"));
	/* the list replaces the one of the label, which carries the size of the text (native_setTextSize);
	 * the size spans of the markup are percentages of it. Without it every styled text had the default size. */
	PangoAttrList *old_attrs = gtk_label_get_attributes(label);
	if (old_attrs) {
		GSList *old = pango_attr_list_get_attributes(old_attrs);
		for (GSList *l = old; l; l = l->next) {
			PangoAttribute *attr = l->data;
			if (attr->klass->type == PANGO_ATTR_ABSOLUTE_SIZE || attr->klass->type == PANGO_ATTR_SIZE || attr->klass->type == PANGO_ATTR_LETTER_SPACING)
				pango_attr_list_insert(pango_attrs, attr);
			else
				pango_attribute_destroy(attr);
		}
		g_slist_free(old);
	}
	jsize count = places_array ? (*env)->GetArrayLength(env, places_array) / 4 : 0;
	GArray *bytes = g_array_new(FALSE, FALSE, sizeof(guint));
	if (count > 0) {
		jint *places = (*env)->GetIntArrayElements(env, places_array, NULL);
		const char *text = gtk_label_get_text(label);
		for (jsize i = 0; i < count; i++) {
			/* UTF-16 index to byte index */
			const char *p = text;
			jint units = 0;
			while (*p && units < places[i * 4]) {
				units += g_utf8_get_char(p) > 0xFFFF ? 2 : 1;
				p = g_utf8_next_char(p);
			}
			guint index = *p ? p - text : G_MAXUINT; /* G_MAXUINT: the text has no such place */
			g_array_append_val(bytes, index);
			if (!*p)
				continue;
			PangoRectangle rect = {0, places[i * 4 + 3] * PANGO_SCALE, places[i * 4 + 1] * PANGO_SCALE, places[i * 4 + 2] * PANGO_SCALE};
			PangoAttribute *shape = pango_attr_shape_new(&rect, &rect);
			shape->start_index = index;
			shape->end_index = g_utf8_next_char(p) - text;
			pango_attr_list_insert(pango_attrs, shape);
		}
		(*env)->ReleaseIntArrayElements(env, places_array, places, JNI_ABORT);
	}
	g_object_set_data_full(G_OBJECT(label), "atl-image-bytes", bytes, (GDestroyNotify)g_array_unref);
	gtk_label_set_attributes(label, pango_attrs);
	pango_attr_list_unref(pango_attrs);
}

/* where the label has put the placeholder of a ReplacementSpan: x of its left edge, top, baseline and bottom of its
 * line, in the view (the arguments of AOSP ReplacementSpan.draw()); none when an ellipsis has taken its place */
JNIEXPORT jboolean JNICALL Java_android_widget_TextView_native_1getPlaceholderPosition(JNIEnv *env, jobject this, jlong widget_ptr, jint index, jfloatArray place_array)
{
	GtkLabel *label = box_get_label(env, _PTR(widget_ptr));
	GArray *bytes = label ? g_object_get_data(G_OBJECT(label), "atl-image-bytes") : NULL;
	if (!bytes || index < 0 || (guint)index >= bytes->len || g_array_index(bytes, guint, index) == G_MAXUINT)
		return JNI_FALSE;
	GtkWidget *wrapper = gtk_widget_get_parent(GTK_WIDGET(_PTR(widget_ptr)));
	graphene_point_t origin;
	if (!gtk_widget_compute_point(GTK_WIDGET(label), wrapper, &GRAPHENE_POINT_INIT(0, 0), &origin))
		return JNI_FALSE;
	float pos[4];
	if (!atl_pango_layout_placeholder(gtk_label_get_layout(label), g_array_index(bytes, guint, index), pos))
		return JNI_FALSE;
	int offset_x, offset_y;
	gtk_label_get_layout_offsets(label, &offset_x, &offset_y);
	float x = origin.x + offset_x, y = origin.y + offset_y;
	jfloat place[4] = {x + pos[0], y + pos[1], y + pos[2], y + pos[3]};
	(*env)->SetFloatArrayRegion(env, place_array, 0, 4, place);
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setCompoundDrawablePadding(JNIEnv *env, jobject this, jlong widget_ptr, jint pad)
{
	GtkWidget *box = GTK_WIDGET(_PTR(widget_ptr));
	if (GTK_IS_BUTTON(box)) // a Button has the box of text and drawable in a GtkButton
		box = gtk_button_get_child(GTK_BUTTON(box));
	if (GTK_IS_BOX(box))
		gtk_box_set_spacing(GTK_BOX(box), pad);
}

/* A compound drawable, drawn as AOSP's TextView.onDraw() does: it takes the size of the drawable's bounds
 * at the time it is set (AOSP mDrawableSizeLeft/mDrawableHeightLeft ...), is centered across the box
 * (left and right vertically, top and bottom horizontally) and is drawn by Drawable.draw(), which moves
 * it by the bounds it has then (MaterialButton moves its icon next to the centered text in onLayout()). */
#define ATL_TYPE_COMPOUND_DRAWABLE (atl_compound_drawable_get_type())
G_DECLARE_FINAL_TYPE(AtlCompoundDrawable, atl_compound_drawable, ATL, COMPOUND_DRAWABLE, GtkWidget)

struct _AtlCompoundDrawable {
	GtkWidget parent_instance;
	GdkPaintable *paintable; // of the drawable, for its invalidations
	jobject drawable; // global reference to the android.graphics.drawable.Drawable
	int width;
	int height;
};

G_DEFINE_TYPE(AtlCompoundDrawable, atl_compound_drawable, GTK_TYPE_WIDGET)

static void atl_compound_drawable_measure(GtkWidget *widget, GtkOrientation orientation, int for_size, int *minimum, int *natural, int *minimum_baseline, int *natural_baseline)
{
	AtlCompoundDrawable *self = ATL_COMPOUND_DRAWABLE(widget);
	*minimum = *natural = orientation == GTK_ORIENTATION_HORIZONTAL ? self->width : self->height;
}

static void atl_compound_drawable_snapshot(GtkWidget *widget, GtkSnapshot *snapshot)
{
	AtlCompoundDrawable *self = ATL_COMPOUND_DRAWABLE(widget);
	if (!self->drawable)
		return;
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 8); // called from GTK, which never returns to Java to free them
	GtkOrientation orientation = gtk_orientable_get_orientation(GTK_ORIENTABLE(gtk_widget_get_parent(widget)));
	gtk_snapshot_save(snapshot);
	if (orientation == GTK_ORIENTATION_HORIZONTAL)
		gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT(0, (gtk_widget_get_height(widget) - self->height) / 2));
	else
		gtk_snapshot_translate(snapshot, &GRAPHENE_POINT_INIT((gtk_widget_get_width(widget) - self->width) / 2, 0));
	jobject canvas = J_new__GskCanvas(env, _INTPTR(snapshot));
	J__Drawable__draw(env, self->drawable, canvas);
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	gtk_snapshot_restore(snapshot);
	(*env)->PopLocalFrame(env, NULL);
}

static void compound_drawable_set(AtlCompoundDrawable *self, jobject drawable);

static void atl_compound_drawable_dispose(GObject *object)
{
	compound_drawable_set(ATL_COMPOUND_DRAWABLE(object), NULL);
	G_OBJECT_CLASS(atl_compound_drawable_parent_class)->dispose(object);
}

static void atl_compound_drawable_class_init(AtlCompoundDrawableClass *class)
{
	G_OBJECT_CLASS(class)->dispose = atl_compound_drawable_dispose;
	GTK_WIDGET_CLASS(class)->measure = atl_compound_drawable_measure;
	GTK_WIDGET_CLASS(class)->snapshot = atl_compound_drawable_snapshot;
}

static void atl_compound_drawable_init(AtlCompoundDrawable *self)
{
}

static void invalidate_contents_cb(GdkPaintable *paintable, GtkWidget *widget)
{
	gtk_widget_queue_draw(widget);
}

static void compound_drawable_set(AtlCompoundDrawable *self, jobject drawable)
{
	JNIEnv *env = get_jni_env();
	if (self->paintable)
		g_signal_handlers_disconnect_by_func(self->paintable, invalidate_contents_cb, self);
	g_clear_object(&self->paintable);
	if (self->drawable)
		(*env)->DeleteGlobalRef(env, self->drawable);
	self->drawable = NULL;
	self->width = self->height = 0;
	if (!drawable)
		return;
	self->drawable = (*env)->NewGlobalRef(env, drawable);
	GdkPaintable *paintable = _PTR(_GET_LONG_FIELD(drawable, "paintable"));
	if (paintable) {
		self->paintable = g_object_ref(paintable);
		g_signal_connect_object(paintable, "invalidate-contents", G_CALLBACK(invalidate_contents_cb), self, 0);
	}
	jobject bounds = _GET_OBJ_FIELD(drawable, "mBounds", "Landroid/graphics/Rect;");
	self->width = MAX(0, _GET_INT_FIELD(bounds, "right") - _GET_INT_FIELD(bounds, "left"));
	self->height = MAX(0, _GET_INT_FIELD(bounds, "bottom") - _GET_INT_FIELD(bounds, "top"));
	(*env)->DeleteLocalRef(env, bounds);
}

/* the compound drawable before (first == TRUE) or after the label in the box */
static void box_set_compound_drawable(GtkWidget *box, gboolean first, jobject drawable)
{
	GtkWidget *child = first ? gtk_widget_get_first_child(box) : gtk_widget_get_last_child(box);
	if (!ATL_IS_COMPOUND_DRAWABLE(child)) {
		if (!drawable)
			return;
		child = g_object_new(ATL_TYPE_COMPOUND_DRAWABLE, NULL);
		if (first)
			gtk_widget_insert_after(child, box, NULL);
		else
			gtk_widget_insert_before(child, box, NULL);
	}
	compound_drawable_set(ATL_COMPOUND_DRAWABLE(child), drawable);
	gtk_widget_set_visible(child, drawable != NULL);
	gtk_widget_queue_resize(child);
}

/* the box holds the label with at most one drawable before and one after it: left and right side by side,
 * else top and bottom above each other (a Button has the box in a GtkButton) */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setCompoundDrawables(JNIEnv *env, jobject this, jlong widget_ptr, jobject left, jobject top, jobject right, jobject bottom)
{
	GtkWidget *box = GTK_WIDGET(_PTR(widget_ptr));
	if (GTK_IS_BUTTON(box))
		box = gtk_button_get_child(GTK_BUTTON(box));
	gboolean horizontal = left || right;
	gtk_orientable_set_orientation(GTK_ORIENTABLE(box), horizontal ? GTK_ORIENTATION_HORIZONTAL : GTK_ORIENTATION_VERTICAL);
	/* AOSP: the text has the space between the paddings of the drawables, the gravity places it there */
	GtkLabel *label = box_get_label(env, box);
	if (label)
		gtk_widget_set_vexpand(GTK_WIDGET(label), !horizontal && (top || bottom));
	box_set_compound_drawable(box, TRUE, left ?: top);
	box_set_compound_drawable(box, FALSE, right ?: bottom);
}

/* TextView.setTextIsSelectable(): the GtkLabel lets the user select (and copy) its text */
JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setSelectable(JNIEnv *env, jobject this, jlong widget_ptr, jboolean selectable)
{
	GtkWidget *widget = _PTR(widget_ptr);
	GtkLabel *label = box_get_label(env, GTK_IS_BUTTON(widget) ? gtk_button_get_child(GTK_BUTTON(widget)) : widget);
	if (!label)
		return;
	gtk_label_set_selectable(label, selectable);
}
