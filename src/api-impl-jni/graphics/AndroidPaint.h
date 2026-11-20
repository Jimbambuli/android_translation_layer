#include "pango/pango-layout.h"
#include <gdk/gdk.h>
#include <gsk/gsk.h>
#include <gtk/gtk.h>
#include <jni.h>
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
	bool hinting : 1; /* Paint.setHinting(), HINTING_ON by default */
	bool font_hinted : 1; /* android_paint_pango_context(): the font of hinted_font hints itself */
	PangoFontDescription *hinted_font; /* the font description font_hinted was found for, NULL: none yet */
};

/* android_graphics_Paint.c */
void android_paint_apply_text_decoration(PangoLayout *layout, struct AndroidPaint *paint);
PangoContext *android_paint_pango_context(struct AndroidPaint *paint, const PangoFontDescription *desc);
bool android_paint_push_shadow(GtkSnapshot *snapshot, struct AndroidPaint *paint);
void android_paint_append_layout(GtkSnapshot *snapshot, PangoLayout *layout, struct AndroidPaint *paint);
void android_paint_append_glyphs(GtkSnapshot *snapshot, PangoFont *font, PangoGlyphString *glyphs, const graphene_point_t *origin, struct AndroidPaint *paint);
void android_paint_add_layout_outline(GskPathBuilder *builder, PangoLayout *layout, float x, float y);
void android_paint_add_glyphs_outline(GskPathBuilder *builder, PangoFont *font, PangoGlyphString *glyphs, float x, float y);

/* android_graphics_Path.c: adds the outline of the stroke of src (width 0: only the dashes of src) to dst, and src
 * itself for add_fill (Paint.getFillPath) */
void android_path_add_stroke(GskPathBuilder *dst, GskPath *src, const GskStroke *stroke, float width, float miter_limit, bool add_fill);

/* one glyph of a shaped run: its id in font (a reference the caller drops), its origin relative to the run's */
struct android_shaped_glyph {
	PangoGlyph id;
	float x;
	float y;
	PangoFont *font;
};
/* shapes text[0, len) (the context) and lays out the chars [start, end) of it in visual order: the advance of every
 * char of the context into advances (len floats, may be NULL), the glyphs of [start, end) into glyphs_out (may be
 * NULL), the total advance of [start, end) into total */
void android_paint_shape_run(struct AndroidPaint *paint, const jchar *text, int len, int start, int end, bool rtl,
                             float *advances, GArray *glyphs_out, float *total);

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
