#include "pango/pango-layout.h"
#include <gdk/gdk.h>
#include <gsk/gsk.h>
#include <pango/pango.h>

struct AndroidPaint {
	GdkRGBA color;
	GskStroke *gsk_stroke;
	PangoFontDescription *font;
	PangoAlignment alignment;
	graphene_matrix_t color_matrix;
	graphene_vec4_t color_offset;
	float letter_spacing; /* in em, Paint.setLetterSpacing */
	bool is_fill : 1;
	bool is_stroke : 1;
	bool use_color_filter : 1;
};

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
