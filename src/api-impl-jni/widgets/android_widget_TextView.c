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
	GtkLabel *label = box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget")));
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
		hb_blob_t *head = hb_face_reference_table(hb_font_get_face(pango_font_get_hb_font(font)), HB_TAG('h', 'e', 'a', 'd'));
		unsigned int length;
		const uint8_t *data = (const uint8_t *)hb_blob_get_data(head, &length);
		if (length >= 44) {
			int units_per_em = data[18] << 8 | data[19];
			int y_min = (int16_t)(data[38] << 8 | data[39]);
			int y_max = (int16_t)(data[42] << 8 | data[43]);
			PangoFontDescription *description = pango_font_describe_with_absolute_size(font);
			float size = (float)pango_font_description_get_size(description) / PANGO_SCALE;
			pango_font_description_free(description);
			PangoFontMetrics *metrics = pango_font_get_metrics(font, NULL);
			top = MAX(0, (int)roundf(y_max * size / units_per_em - (float)pango_font_metrics_get_ascent(metrics) / PANGO_SCALE));
			bottom = MAX(0, (int)roundf(-y_min * size / units_per_em - (float)pango_font_metrics_get_descent(metrics) / PANGO_SCALE));
			pango_font_metrics_unref(metrics);
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

/* the baseline of the first line of the label at the given width, moved by the label's vertical
 * alignment within the given height */
JNIEXPORT jint JNICALL Java_android_widget_TextView_native_1getBaseline(JNIEnv *env, jobject this, jlong widget_ptr, jint width, jint height)
{
	GtkLabel *label = box_get_label(env, _PTR(widget_ptr));
	if (!label)
		return -1;
	int natural, baseline;
	gtk_widget_measure(GTK_WIDGET(label), GTK_ORIENTATION_VERTICAL, width > 0 ? width : -1, NULL, &natural, NULL, &baseline);
	if (baseline < 0)
		return -1;
	return baseline + MAX(height - natural, 0) * gtk_label_get_yalign(label);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setMarkup(JNIEnv *env, jobject this, jstring markup_jstr)
{
	GtkLabel *label = box_get_label(env, _PTR(_GET_LONG_FIELD(this, "widget")));
	if (!label)
		return;
	const char *markup = (*env)->GetStringUTFChars(env, markup_jstr, NULL);
	gtk_label_set_markup(label, markup);
	(*env)->ReleaseStringUTFChars(env, markup_jstr, markup);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setCompoundDrawablePadding(JNIEnv *env, jobject this, jlong widget_ptr, jint pad)
{
	GtkWidget *box = GTK_WIDGET(_PTR(widget_ptr));
	if (GTK_IS_BUTTON(box)) // a Button has the box of text and drawable in a GtkButton
		box = gtk_button_get_child(GTK_BUTTON(box));
	if (GTK_IS_BOX(box))
		gtk_box_set_spacing(GTK_BOX(box), pad);
}

JNIEXPORT void JNICALL Java_android_widget_TextView_native_1setCompoundDrawables(JNIEnv *env, jobject this, jlong widget_ptr, jlong left, jlong top, jlong right, jlong bottom)
{
	GtkWidget *box = GTK_WIDGET(_PTR(widget_ptr));
	gtk_orientable_set_orientation(GTK_ORIENTABLE(box), (left || right) ? GTK_ORIENTATION_HORIZONTAL : GTK_ORIENTATION_VERTICAL);

	GdkPaintable *paintable = _PTR(left ?: top); // paintable before text
	GtkWidget *picture = gtk_widget_get_first_child(box);
	if (GTK_IS_PICTURE(picture)) {
		gtk_picture_set_paintable(GTK_PICTURE(picture), paintable);
	} else if (paintable) {
		picture = gtk_picture_new_for_paintable(paintable);
		gtk_widget_insert_after(picture, box, NULL);
	}

	paintable = _PTR(right ?: bottom); // paintable after text
	picture = gtk_widget_get_last_child(box);
	if (GTK_IS_PICTURE(picture)) {
		gtk_picture_set_paintable(GTK_PICTURE(picture), paintable);
	} else if (paintable) {
		picture = gtk_picture_new_for_paintable(paintable);
		gtk_widget_insert_before(picture, box, NULL);
	}
}
