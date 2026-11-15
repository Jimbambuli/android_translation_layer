#include "pango/pango-layout.h"
#include <gdk/gdk.h>
#include <gsk/gsk.h>
#include <gtk/gtk.h>
#include <pango/pango.h>

struct AndroidPaint {
	GdkRGBA color;
	GskStroke *gsk_stroke;
	PangoFontDescription *font;
	PangoAlignment alignment;
	graphene_matrix_t color_matrix;
	graphene_vec4_t color_offset;
	float letter_spacing; /* in em, Paint.setLetterSpacing */
	float text_skew_x; /* Paint.setTextSkewX */
	float shadow_radius; /* Paint.setShadowLayer, no shadow for <= 0 */
	float shadow_dx;
	float shadow_dy;
	GdkRGBA shadow_color;
	bool is_fill : 1;
	bool is_stroke : 1;
	bool use_color_filter : 1;
	bool underline : 1; /* Paint.UNDERLINE_TEXT_FLAG */
	bool strike_thru : 1; /* Paint.STRIKE_THRU_TEXT_FLAG */
	bool fake_bold : 1; /* Paint.FAKE_BOLD_TEXT_FLAG */
};

/* android_graphics_Paint.c */
void android_paint_apply_text_decoration(PangoLayout *layout, struct AndroidPaint *paint);
bool android_paint_push_text_shadow(GtkSnapshot *snapshot, struct AndroidPaint *paint);
void android_paint_append_layout(GtkSnapshot *snapshot, PangoLayout *layout, struct AndroidPaint *paint);
void android_paint_append_glyphs(GtkSnapshot *snapshot, PangoFont *font, PangoGlyphString *glyphs, const graphene_point_t *origin, struct AndroidPaint *paint);
void android_paint_add_layout_outline(GskPathBuilder *builder, PangoLayout *layout, float x, float y);
void android_paint_add_glyphs_outline(GskPathBuilder *builder, PangoFont *font, PangoGlyphString *glyphs, float x, float y);

/* Paint.setLetterSpacing as a Pango attribute over the whole text (Pango wants absolute units) */
static inline void android_paint_apply_letter_spacing(PangoLayout *layout, struct AndroidPaint *paint)
{
	if (paint->letter_spacing == 0.f)
		return;
	PangoAttrList *attrs = pango_attr_list_new();
	pango_attr_list_insert(attrs, pango_attr_letter_spacing_new(paint->letter_spacing * pango_font_description_get_size(paint->font)));
	pango_layout_set_attributes(layout, attrs);
	pango_attr_list_unref(attrs);
}
