/* Android's navigation bar under the window (ATL_TITLEBAR=system-navigation), for a title bar of the desktop
 * that has no back button: ◁ is back (as the Escape key), ○ goes back to the app's first activity (the one it
 * was started with), □ lists the open activities of the app, the shown one first, and goes back to the one
 * chosen. The bar is small (about 32 px) and takes the colors of the GTK theme. */

#include "navigation_bar.h"

#include "../api-impl-jni/app/android_app_Activity.h"

static const char navigation_bar_css[] =
	".atl-navigation-bar { min-height: 32px; padding: 0; border-top: 1px solid alpha(currentColor, 0.12); }\n"
	".atl-navigation-bar button { min-height: 28px; min-width: 72px; padding: 0; margin: 2px 0; }\n"
	".atl-navigation-bar button label { font-size: 16px; }\n"
	".atl-activity-list { padding: 4px; }\n";

static void on_back(GtkButton *button, gpointer user_data)
{
	current_activity_back_pressed();
}

static void on_home(GtkButton *button, gpointer user_data)
{
	activity_return_to(activity_count() - 1);
}

static void on_activity_chosen(GtkButton *button, GtkPopover *popover)
{
	int index = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(button), "atl-index"));
	gtk_popover_popdown(popover);
	activity_return_to(index);
}

/* the list is made anew each time it opens */
static void on_list_show(GtkPopover *popover, gpointer user_data)
{
	GtkWidget *list = gtk_box_new(GTK_ORIENTATION_VERTICAL, 2);
	gtk_widget_add_css_class(list, "atl-activity-list");
	int count = activity_count();
	for (int i = 0; i < count; i++) {
		char *title = activity_title(i);
		GtkWidget *item = gtk_button_new_with_label(title ? title : "?");
		g_free(title);
		gtk_widget_add_css_class(item, "flat");
		gtk_label_set_xalign(GTK_LABEL(gtk_button_get_child(GTK_BUTTON(item))), 0);
		g_object_set_data(G_OBJECT(item), "atl-index", GINT_TO_POINTER(i));
		g_signal_connect(item, "clicked", G_CALLBACK(on_activity_chosen), popover);
		gtk_box_append(GTK_BOX(list), item);
	}
	gtk_popover_set_child(popover, list);
}

static GtkWidget *bar_button(const char *symbol, const char *tooltip)
{
	GtkWidget *button = gtk_button_new_with_label(symbol);
	gtk_widget_add_css_class(button, "flat");
	gtk_widget_set_tooltip_text(button, tooltip);
	gtk_widget_set_focusable(button, FALSE); // the keyboard stays with the app
	return button;
}

GtkWidget *navigation_bar_new(void)
{
	GtkCssProvider *css = gtk_css_provider_new();
	gtk_css_provider_load_from_string(css, navigation_bar_css);
	gtk_style_context_add_provider_for_display(gdk_display_get_default(), GTK_STYLE_PROVIDER(css), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_unref(css);

	GtkWidget *bar = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 40);
	gtk_widget_add_css_class(bar, "atl-navigation-bar");
	gtk_widget_set_halign(bar, GTK_ALIGN_FILL);
	GtkWidget *buttons = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 40);
	gtk_widget_set_hexpand(buttons, TRUE);
	gtk_widget_set_halign(buttons, GTK_ALIGN_CENTER);

	GtkWidget *back = bar_button("◁", "Back");
	g_signal_connect(back, "clicked", G_CALLBACK(on_back), NULL);
	GtkWidget *home = bar_button("○", "First page of the app");
	g_signal_connect(home, "clicked", G_CALLBACK(on_home), NULL);
	GtkWidget *recents = gtk_menu_button_new();
	gtk_menu_button_set_label(GTK_MENU_BUTTON(recents), "□");
	gtk_menu_button_set_always_show_arrow(GTK_MENU_BUTTON(recents), FALSE);
	gtk_widget_set_tooltip_text(recents, "Open pages of the app");
	gtk_widget_set_focusable(recents, FALSE);
	gtk_menu_button_set_has_frame(GTK_MENU_BUTTON(recents), FALSE);
	GtkWidget *popover = gtk_popover_new();
	g_signal_connect(popover, "show", G_CALLBACK(on_list_show), NULL);
	gtk_menu_button_set_popover(GTK_MENU_BUTTON(recents), popover);

	gtk_box_append(GTK_BOX(buttons), back);
	gtk_box_append(GTK_BOX(buttons), home);
	gtk_box_append(GTK_BOX(buttons), recents);
	gtk_box_append(GTK_BOX(bar), buttons);
	return bar;
}
