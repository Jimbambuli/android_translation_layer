/* Android's navigation bar under the window (ATL_TITLEBAR=system-navigation), for a title bar of the desktop
 * that has no back button: ◁ is back (as the Escape key), ○ is Home inside the app (its start activity with
 * FLAG_ACTIVITY_CLEAR_TOP: the activities above it finish, it gets onNewIntent()), □ lists the open activities
 * of the app, the shown one first, and brings the chosen one to the front (the others stay, as with
 * FLAG_ACTIVITY_REORDER_TO_FRONT). The bar is small (about 32 px) and takes the colors of the GTK theme; the
 * tooltips come from ATL_NAVIGATION_TOOLTIPS (three lines: back, home, open pages; e.g. in the desktop's
 * language from the launcher), else English. */

#include "navigation_bar.h"

#include "../api-impl-jni/app/android_app_Activity.h"

static const char navigation_bar_css[] =
	/* the window of an app that draws its own background has none (ATL-no-background): the bar takes the
	 * theme's window color (GTK's own and Breeze both name it theme_bg_color) */
	"actionbar.atl-navigation-bar > revealer > box { min-height: 32px; padding: 0; background-color: @theme_bg_color;"
	" border-top: 1px solid alpha(currentColor, 0.15); }\n"
	".atl-navigation-bar button { min-height: 28px; min-width: 72px; padding: 0; margin: 2px 0; }\n"
	".atl-navigation-bar button label { font-size: 16px; }\n"
	".atl-activity-list { padding: 4px; }\n";

static void on_back(GtkButton *button, gpointer user_data)
{
	current_activity_back_pressed();
}

static void on_home(GtkButton *button, gpointer user_data)
{
	activity_go_to_start();
}

static void on_activity_chosen(GtkButton *button, GtkPopover *popover)
{
	int index = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(button), "atl-index"));
	gtk_popover_popdown(popover);
	activity_bring_to_front(index);
}

/* the list is made anew each time it opens; an activity is listed once, and activities with the same title
 * (apps often title all of them with the app's name) get the name of their class */
static void on_list_show(GtkPopover *popover, gpointer user_data)
{
	GtkWidget *list = gtk_box_new(GTK_ORIENTATION_VERTICAL, 2);
	gtk_widget_add_css_class(list, "atl-activity-list");
	int count = activity_count();
	char **titles = g_new0(char *, count + 1);
	for (int i = 0; i < count; i++)
		titles[i] = activity_title(i);
	for (int i = 0; i < count; i++) {
		gboolean listed = FALSE, same_title = FALSE;
		for (int j = 0; j < count; j++) {
			if (j < i && activity_same(i, j))
				listed = TRUE;
			else if (j != i && !activity_same(i, j) && g_strcmp0(titles[i], titles[j]) == 0)
				same_title = TRUE;
		}
		if (listed)
			continue;
		char *class_name = same_title ? activity_class_name(i) : NULL;
		char *label = class_name ? g_strdup_printf("%s – %s", titles[i] ? titles[i] : "?", class_name) : g_strdup(titles[i] ? titles[i] : "?");
		g_free(class_name);
		GtkWidget *item = gtk_button_new_with_label(label);
		g_free(label);
		gtk_widget_add_css_class(item, "flat");
		gtk_label_set_xalign(GTK_LABEL(gtk_button_get_child(GTK_BUTTON(item))), 0);
		g_object_set_data(G_OBJECT(item), "atl-index", GINT_TO_POINTER(i));
		g_signal_connect(item, "clicked", G_CALLBACK(on_activity_chosen), popover);
		gtk_box_append(GTK_BOX(list), item);
	}
	for (int i = 0; i < count; i++)
		g_free(titles[i]);
	g_free(titles);
	gtk_popover_set_child(popover, list);
}

static GtkWidget *bar_button(const char *symbol, const char *tooltip)
{
	GtkWidget *button = gtk_button_new_with_label(symbol);
	gtk_widget_add_css_class(button, "flat");
	gtk_widget_set_tooltip_text(button, tooltip);
	return button;
}

GtkWidget *navigation_bar_new(void)
{
	GtkCssProvider *css = gtk_css_provider_new();
	gtk_css_provider_load_from_string(css, navigation_bar_css);
	gtk_style_context_add_provider_for_display(gdk_display_get_default(), GTK_STYLE_PROVIDER(css), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
	g_object_unref(css);

	/* an action bar: every GTK theme gives it the colors of a bar at the bottom of a window */
	GtkWidget *bar = gtk_action_bar_new();
	gtk_widget_add_css_class(bar, "atl-navigation-bar");
	gtk_widget_set_can_focus(bar, FALSE); // the keyboard stays with the app
	GtkWidget *buttons = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 40);

	const char *tooltips[3] = {"Back", "Start page of the app", "Open pages of the app"};
	char **given = g_strsplit(g_getenv("ATL_NAVIGATION_TOOLTIPS") ? g_getenv("ATL_NAVIGATION_TOOLTIPS") : "", "\n", 3);
	for (int i = 0; i < 3 && given[i]; i++)
		if (*given[i])
			tooltips[i] = given[i];

	GtkWidget *back = bar_button("◁", tooltips[0]);
	g_signal_connect(back, "clicked", G_CALLBACK(on_back), NULL);
	GtkWidget *home = bar_button("○", tooltips[1]);
	g_signal_connect(home, "clicked", G_CALLBACK(on_home), NULL);
	GtkWidget *recents = gtk_menu_button_new();
	gtk_menu_button_set_label(GTK_MENU_BUTTON(recents), "□");
	gtk_menu_button_set_always_show_arrow(GTK_MENU_BUTTON(recents), FALSE);
	gtk_widget_set_tooltip_text(recents, tooltips[2]);
	g_strfreev(given);
	gtk_menu_button_set_has_frame(GTK_MENU_BUTTON(recents), FALSE);
	GtkWidget *popover = gtk_popover_new();
	g_signal_connect(popover, "show", G_CALLBACK(on_list_show), NULL);
	gtk_menu_button_set_popover(GTK_MENU_BUTTON(recents), popover);

	gtk_box_append(GTK_BOX(buttons), back);
	gtk_box_append(GTK_BOX(buttons), home);
	gtk_box_append(GTK_BOX(buttons), recents);
	gtk_action_bar_set_center_widget(GTK_ACTION_BAR(bar), buttons);
	return bar;
}
