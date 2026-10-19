#include <gtk/gtk.h>

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
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionDescribe(env);
	return ret;
}

JNIEXPORT jlong JNICALL Java_android_widget_TextView_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	const char *text = attribute_set_get_string(env, attrs, "text", NULL);

	//	_SET_OBJ_FIELD(this, "text", "Ljava/lang/String;", _JSTRING(text)); //TODO: sadly this might be needed, but it's not atm

	GtkWidget *wrapper = g_object_ref(wrapper_widget_new());
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

JNIEXPORT void JNICALL Java_android_widget_TextView_setTextSize(JNIEnv *env, jobject this, jfloat size)
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
	if (GTK_IS_BOX(box)) // subclasses like Button use a different widget
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
