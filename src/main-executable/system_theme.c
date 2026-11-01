/* The GTK theme of the desktop, also in a sandbox.
 *
 * GTK takes its theme from settings.ini and the user's gtk.css in $XDG_CONFIG_HOME/gtk-4.0 and looks for the
 * theme in the themes directories of $XDG_DATA_DIRS. In a Flatpak these are the app's own directories, so the
 * windows would have GTK's default theme on every desktop. With read access to the host's system files
 * (--filesystem=host-os:ro, under /run/host) and to its GTK configuration (--filesystem=xdg-config/gtk-4.0:ro,
 * xdg-config/gtk-3.0:ro), ATL does what GTK does outside the sandbox:
 *
 *  - the host's themes and icons become visible by adding /run/host/usr/share to $XDG_DATA_DIRS (before GTK
 *    reads it, see system_theme_add_data_dirs())
 *  - theme, icons, cursor and the layout of the title bar buttons come from the host's settings.ini
 *    (the font stays Roboto for the app, see main.c; the title bar gets the desktop's font)
 *  - the host's gtk.css (KDE: @import 'colors.css', the colors of the color scheme) is user CSS as in GTK
 *  - the title of the title bar is in the desktop's font.
 * The window buttons are the theme's (Breeze draws them like KWin's Breeze decoration). KDE's
 * window_decorations.css is not used: it is written for GTK 3 (class "titlebutton", images only in gtk-3.0),
 * GTK 4 does not load it, and GTK 4.24 draws its images (SVG from Qt) as nothing, which hid the buttons.
 *
 * A change of these files (another theme or color scheme) is applied while the app runs. Without these files
 * GTK keeps its default theme and the dark variant from the settings portal. */

#include <gtk/gtk.h>
#include <string.h>

#include "system_theme.h"

static char *config_dir = NULL;
static GtkCssProvider *user_provider = NULL;
static GtkCssProvider *titlebar_provider = NULL;
static GPtrArray *monitors = NULL;
static guint reload_source = 0;

/* add the host's share directories with themes to $XDG_DATA_DIRS, at the end so the app's own data comes first;
 * must run before GLib reads the variable (g_get_system_data_dirs() keeps the first result) */
void system_theme_add_data_dirs(const char *host_share)
{
	char *themes = g_build_filename(host_share, "themes", NULL);
	gboolean has_themes = g_file_test(themes, G_FILE_TEST_IS_DIR);
	g_free(themes);
	if (!has_themes)
		return;
	const char *dirs = g_getenv("XDG_DATA_DIRS");
	if (!dirs || !*dirs)
		dirs = "/usr/local/share:/usr/share"; // the default of the XDG Base Directory Specification
	char **list = g_strsplit(dirs, ":", -1);
	gboolean present = g_strv_contains((const char *const *)list, host_share);
	g_strfreev(list);
	if (!present) {
		char *new_dirs = g_strconcat(dirs, ":", host_share, NULL);
		g_setenv("XDG_DATA_DIRS", new_dirs, TRUE);
		g_free(new_dirs);
	}
}

/* the host's configuration directory: Flatpak passes the host's $XDG_CONFIG_HOME as $HOST_XDG_CONFIG_HOME
 * if it was set, the default is ~/.config */
char *system_theme_host_config_dir(void)
{
	const char *host = g_getenv("HOST_XDG_CONFIG_HOME");
	if (host && *host)
		return g_strdup(host);
	return g_build_filename(g_get_home_dir(), ".config", NULL);
}

static char *config_file(const char *gtk_dir, const char *name)
{
	return g_build_filename(config_dir, gtk_dir, name, NULL);
}

/* the settings of settings.ini that make up the look; not the font (Roboto for the app), not the dark variant
 * (settings portal) and not gtk-xft-dpi (for X11, the scale comes from the compositor) */
static const char *string_settings[] = {"gtk-theme-name", "gtk-icon-theme-name", "gtk-cursor-theme-name", "gtk-decoration-layout"};

static char *apply_settings(void)
{
	GKeyFile *key_file = g_key_file_new();
	char *path = config_file("gtk-4.0", "settings.ini");
	gboolean loaded = g_key_file_load_from_file(key_file, path, G_KEY_FILE_NONE, NULL);
	g_free(path);
	if (!loaded) {
		path = config_file("gtk-3.0", "settings.ini");
		loaded = g_key_file_load_from_file(key_file, path, G_KEY_FILE_NONE, NULL);
		g_free(path);
	}
	char *font_name = NULL;
	if (loaded) {
		GtkSettings *settings = gtk_settings_get_default();
		for (int i = 0; i < G_N_ELEMENTS(string_settings); i++) {
			char *value = g_key_file_get_string(key_file, "Settings", string_settings[i], NULL);
			if (value)
				g_object_set(settings, string_settings[i], g_strstrip(value), NULL);
			g_free(value);
		}
		GError *error = NULL;
		gint size = g_key_file_get_integer(key_file, "Settings", "gtk-cursor-theme-size", &error);
		if (!error && size > 0)
			g_object_set(settings, "gtk-cursor-theme-size", size, NULL);
		g_clear_error(&error);
		font_name = g_key_file_get_string(key_file, "Settings", "gtk-font-name", NULL);
	}
	g_key_file_free(key_file);
	return font_name;
}

static void load_css(char *font_name)
{
	char *path = config_file("gtk-4.0", "gtk.css");
	if (g_file_test(path, G_FILE_TEST_EXISTS))
		gtk_css_provider_load_from_path(user_provider, path);
	else
		gtk_css_provider_load_from_string(user_provider, "");
	g_free(path);

	GString *css = g_string_new("");
	if (font_name) {
		/* the title of the window in the desktop's font, as in the desktop's title bars (themes set a size of
		 * their own for .title) */
		PangoFontDescription *font = pango_font_description_from_string(font_name);
		const char *family = pango_font_description_get_family(font);
		if (family && pango_font_description_get_size(font) > 0)
			g_string_append_printf(css, "headerbar, headerbar .title { font-family: \"%s\"; font-size: %gpt; }\n", family,
			                       (double)pango_font_description_get_size(font) / PANGO_SCALE);
		pango_font_description_free(font);
	}
	gtk_css_provider_load_from_string(titlebar_provider, css->str);
	g_string_free(css, TRUE);
}

static void apply(void)
{
	char *font_name = apply_settings();
	load_css(font_name);
	g_free(font_name);
}

static gboolean reload(gpointer user_data)
{
	reload_source = 0;
	apply();
	return G_SOURCE_REMOVE;
}

/* the desktop writes several files for one change; apply them together */
static void config_changed(GFileMonitor *monitor, GFile *file, GFile *other_file, GFileMonitorEvent event, gpointer user_data)
{
	if (event == G_FILE_MONITOR_EVENT_ATTRIBUTE_CHANGED)
		return;
	if (reload_source)
		g_source_remove(reload_source);
	reload_source = g_timeout_add(200, reload, NULL);
}

static void watch(const char *gtk_dir)
{
	char *path = g_build_filename(config_dir, gtk_dir, NULL);
	GFile *file = g_file_new_for_path(path);
	GFileMonitor *monitor = g_file_monitor_directory(file, G_FILE_MONITOR_WATCH_MOVES, NULL, NULL);
	if (monitor) {
		g_signal_connect(monitor, "changed", G_CALLBACK(config_changed), NULL);
		g_ptr_array_add(monitors, monitor);
	}
	g_object_unref(file);
	g_free(path);
}

/* apply the GTK configuration in config_dir (the host's ~/.config) and follow its changes; needs GTK initialized */
void system_theme_init(const char *dir)
{
	if (config_dir)
		return;
	config_dir = g_strdup(dir);
	user_provider = gtk_css_provider_new();
	titlebar_provider = gtk_css_provider_new();
	GdkDisplay *display = gdk_display_get_default();
	gtk_style_context_add_provider_for_display(display, GTK_STYLE_PROVIDER(user_provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
	gtk_style_context_add_provider_for_display(display, GTK_STYLE_PROVIDER(titlebar_provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
	monitors = g_ptr_array_new_with_free_func(g_object_unref);
	watch("gtk-4.0");
	watch("gtk-3.0");
	apply();
}
