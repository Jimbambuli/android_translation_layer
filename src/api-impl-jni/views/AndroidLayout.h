#ifndef ANDROID_LAYOUT_H
#define ANDROID_LAYOUT_H

#include <gtk/gtk.h>
#include <jni.h>

#define MEASURE_SPEC_UNSPECIFIED ((int)(0u << 30))
#define MEASURE_SPEC_EXACTLY     ((int)(1u << 30))
#define MEASURE_SPEC_AT_MOST     ((int)(2u << 30)) /* unsigned: no shift into the sign bit */
#define MEASURE_SPEC_MASK        ((int)(0x3u << 30))

#define MATCH_PARENT             (-1)
#define WRAP_CONTENT             (-2)

G_DECLARE_FINAL_TYPE(AndroidLayout, android_layout, ATL, ANDROID_LAYOUT, GtkLayoutManager);

struct _AndroidLayout {
	GtkLayoutManager parent_instance;
	int width;
	int height;
	int real_width;
	int real_height;
	gboolean needs_allocation;
};

GtkLayoutManager *android_layout_new(void);
void android_layout_set_params(AndroidLayout *layout, int width, int height);

void widget_set_needs_allocation(GtkWidget *widget);

#endif // ANDROID_LAYOUT_H
