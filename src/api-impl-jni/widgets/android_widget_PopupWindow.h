#ifndef ANDROID_WIDGET_POPUPWINDOW_H
#define ANDROID_WIDGET_POPUPWINDOW_H

#include <gtk/gtk.h>

/* place an already parented popover like AOSP's PopupWindow.showAsDropDown(): below the anchor
 * (whose size in pixels is given, since the anchor's GTK allocation may be empty) or over it,
 * flipped above it if there is no room, aligned by the absolute horizontal gravity, and kept inside
 * the window */
void atl_popover_place_as_dropdown(GtkPopover *popover, GtkWidget *anchor, int anchor_width, int anchor_height, int xoff, int yoff, int gravity, gboolean overlap_anchor);
/* place an already parented popover like AOSP's PopupWindow.showAtLocation(): at (x, y) in the
 * window, measured from the edges selected by the absolute gravity */
void atl_popover_place_at_location(GtkPopover *popover, int gravity, int x, int y);
/* draw the popover's background and text in the given ARGB colors instead of the GTK theme's */
void atl_popover_set_colors(GtkPopover *popover, int background, int foreground);

#endif
