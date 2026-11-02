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
 * The window buttons are the theme's, unless KDE drew the buttons of the KWin decoration (see
 * load_window_buttons()). KDE's window_decorations.css itself is not used: it is written for GTK 3 (class
 * "titlebutton", images only in gtk-3.0), GTK 4 does not load it, and GTK 4.24 draws its images (SVG from
 * Qt) as nothing, which hid the buttons.
 *
 * A change of these files (another theme or color scheme) is applied while the app runs. Without these files
 * GTK keeps its default theme and the dark variant from the settings portal. */

#include <glib/gstdio.h>
#include <gtk/gtk.h>
#include <string.h>
#ifdef HAVE_LIBRSVG
#include <librsvg/rsvg.h>
#endif

#include "system_theme.h"

static char *config_dir = NULL;
static GtkCssProvider *user_provider = NULL;
static GtkCssProvider *titlebar_provider = NULL;
static GtkCssProvider *buttons_provider = NULL;
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

/* KDE (kde-gtk-config) draws the buttons of the current KWin decoration (Breeze, Oxygen, Aurorae themes) as SVG
 * files into gtk-3.0/assets, in every state, for the title bars of GTK 3. ATL puts the same pictures on the
 * buttons of its title bar, so they look and react as KWin's: drawn here with librsvg into PNG files (GTK 4 shows
 * these SVG files as nothing), for scale 1 and 2, in the cache, and on windowcontrols' buttons by CSS. The title
 * bar then also gets the height of KWin's (Breeze: 36 px with a 10 pt font, the GTK theme's is 48 px). Without
 * the files (another desktop, or no librsvg) the GTK theme's buttons and height stay. */
#define BUTTON_SIZE 24
#define TITLEBAR_HEIGHT 34 /* without the header bar's borders */

static const char *button_names[] = {"close", "maximize", "maximized", "minimize"};
static const struct {
	const char *file;
	const char *pseudo;
} button_states[] = {
	{"normal", ""},
	{"hover", ":hover"},
	{"active", ":active"},
	{"backdrop-normal", ":backdrop"},
	{"backdrop-hover", ":backdrop:hover"},
	{"backdrop-active", ":backdrop:active"},
};

#ifdef HAVE_LIBRSVG
static gboolean render_png(const char *svg, const char *png, int size)
{
	GError *error = NULL;
	RsvgHandle *handle = rsvg_handle_new_from_file(svg, &error);
	if (!handle) {
		g_clear_error(&error);
		return FALSE;
	}
	cairo_surface_t *surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, size, size);
	cairo_t *cr = cairo_create(surface);
	RsvgRectangle viewport = {0, 0, size, size};
	gboolean ok = rsvg_handle_render_document(handle, cr, &viewport, &error);
	cairo_destroy(cr);
	ok = ok && cairo_surface_write_to_png(surface, png) == CAIRO_STATUS_SUCCESS;
	cairo_surface_destroy(surface);
	g_object_unref(handle);
	g_clear_error(&error);
	return ok;
}

/* removes the pictures of earlier decorations */
static void remove_other_dirs(const char *parent, const char *keep)
{
	GDir *dir = g_dir_open(parent, 0, NULL);
	if (!dir)
		return;
	const char *name;
	while ((name = g_dir_read_name(dir))) {
		if (!strcmp(name, keep))
			continue;
		char *path = g_build_filename(parent, name, NULL);
		GDir *sub = g_dir_open(path, 0, NULL);
		if (sub) {
			const char *file;
			while ((file = g_dir_read_name(sub))) {
				char *file_path = g_build_filename(path, file, NULL);
				g_unlink(file_path);
				g_free(file_path);
			}
			g_dir_close(sub);
		}
		g_rmdir(path);
		g_free(path);
	}
	g_dir_close(dir);
}

/* the newest time and the total size of the SVG files, NULL if one is missing */
static char *assets_stamp(const char *assets)
{
	gint64 newest = 0, size = 0;
	for (int i = 0; i < G_N_ELEMENTS(button_names); i++) {
		for (int j = 0; j < G_N_ELEMENTS(button_states); j++) {
			char *svg_name = g_strdup_printf("%s-%s.svg", button_names[i], button_states[j].file);
			char *svg = g_build_filename(assets, svg_name, NULL);
			GStatBuf st;
			gboolean found = g_stat(svg, &st) == 0;
			g_free(svg);
			g_free(svg_name);
			if (!found)
				return NULL;
			newest = MAX(newest, (gint64)st.st_mtime);
			size += st.st_size;
		}
	}
	return g_strdup_printf("%" G_GINT64_FORMAT "-%" G_GINT64_FORMAT, newest, size);
}

/* the PNG files of KDE's buttons in a directory named after the SVG files' time and size (KDE writes them anew
 * for another decoration or color scheme); NULL if there are none */
static char *window_button_pictures(void)
{
	char *assets = g_build_filename(config_dir, "gtk-3.0", "assets", NULL);
	char *stamp = assets_stamp(assets);
	if (!stamp) {
		g_free(assets);
		return NULL;
	}
	char *parent = g_build_filename(g_get_user_cache_dir(), "atl", "window-buttons", NULL);
	char *dir = g_build_filename(parent, stamp, NULL);
	char *done = g_build_filename(dir, "complete", NULL);
	gboolean ok = g_file_test(done, G_FILE_TEST_EXISTS);
	if (!ok && g_mkdir_with_parents(dir, 0700) == 0) {
		ok = TRUE;
		for (int i = 0; ok && i < G_N_ELEMENTS(button_names); i++) {
			for (int j = 0; ok && j < G_N_ELEMENTS(button_states); j++) {
				char *svg_name = g_strdup_printf("%s-%s.svg", button_names[i], button_states[j].file);
				char *svg = g_build_filename(assets, svg_name, NULL);
				for (int scale = 1; ok && scale <= 2; scale++) {
					char *png_name = g_strdup_printf("%s-%s@%d.png", button_names[i], button_states[j].file, scale);
					char *png = g_build_filename(dir, png_name, NULL);
					ok = render_png(svg, png, BUTTON_SIZE * scale);
					g_free(png);
					g_free(png_name);
				}
				g_free(svg);
				g_free(svg_name);
			}
		}
		if (ok)
			ok = g_file_set_contents(done, "", 0, NULL);
	}
	if (ok)
		remove_other_dirs(parent, stamp);
	g_free(done);
	g_free(stamp);
	g_free(parent);
	g_free(assets);
	if (!ok) {
		g_free(dir);
		return NULL;
	}
	return dir;
}
#else
static char *window_button_pictures(void)
{
	return NULL;
}
#endif

static void append_picture(GString *css, const char *dir, const char *name, const char *state)
{
	g_string_append(css, "background-image: -gtk-scaled(");
	for (int scale = 1; scale <= 2; scale++) {
		char *png_name = g_strdup_printf("%s-%s@%d.png", name, state, scale);
		char *png = g_build_filename(dir, png_name, NULL);
		char *uri = g_filename_to_uri(png, NULL, NULL);
		g_string_append_printf(css, "%surl(\"%s\")", scale > 1 ? ", " : "", uri);
		g_free(uri);
		g_free(png);
		g_free(png_name);
	}
	g_string_append(css, "); }\n");
}

static void load_window_buttons(void)
{
	char *dir = window_button_pictures();
	if (!dir) {
		gtk_css_provider_load_from_string(buttons_provider, "");
		return;
	}
	GString *css = g_string_new("");
	g_string_append_printf(css,
	                       "headerbar.titlebar { min-height: %dpx; padding-top: 0; padding-bottom: 0; }\n"
	                       "headerbar.titlebar button.atl-back { min-height: 26px; min-width: 26px; padding: 0 2px; margin-top: 0; margin-bottom: 0; }\n"
	                       "headerbar.titlebar windowcontrols > button.close, headerbar.titlebar windowcontrols > button.maximize,"
	                       " headerbar.titlebar windowcontrols > button.minimize {"
	                       " min-width: %dpx; min-height: %dpx; padding: 0; margin: 0 3px; border: none; border-radius: 0;"
	                       " box-shadow: none; outline: none; background-color: transparent; background-repeat: no-repeat;"
	                       " background-position: center; background-size: %dpx %dpx; -gtk-icon-source: none; }\n"
	                       "headerbar.titlebar windowcontrols > button > image { opacity: 0; }\n",
	                       TITLEBAR_HEIGHT, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
	/* KWin's colors of the title bar where KDE's colors.css has them (the pictures are drawn for them), and a flat
	 * back button like KWin's buttons */
	char *colors = NULL;
	char *colors_path = config_file("gtk-4.0", "colors.css");
	if (!g_file_get_contents(colors_path, &colors, NULL, NULL)) {
		g_free(colors_path);
		colors_path = config_file("gtk-3.0", "colors.css");
		g_file_get_contents(colors_path, &colors, NULL, NULL);
	}
	g_free(colors_path);
	if (colors && strstr(colors, "theme_titlebar_background_breeze") && strstr(colors, "theme_titlebar_foreground_backdrop_breeze"))
		g_string_append(css,
		                "headerbar.titlebar { background-image: none; background-color: @theme_titlebar_background_breeze;"
		                " color: @theme_titlebar_foreground_breeze; }\n"
		                "headerbar.titlebar:backdrop { background-color: @theme_titlebar_background_backdrop_breeze;"
		                " color: @theme_titlebar_foreground_backdrop_breeze; }\n");
	g_free(colors);
	g_string_append(css,
	                "headerbar.titlebar button.atl-back { background: none; border: none; box-shadow: none; color: inherit; }\n"
	                "headerbar.titlebar button.atl-back:hover { background-color: alpha(currentColor, 0.15); }\n"
	                "headerbar.titlebar button.atl-back:active { background-color: alpha(currentColor, 0.3); }\n"
	                "headerbar.titlebar button.atl-back:disabled { opacity: 0.5; }\n");
	for (int i = 0; i < G_N_ELEMENTS(button_names); i++) {
		gboolean maximized = !strcmp(button_names[i], "maximized");
		for (int j = 0; j < G_N_ELEMENTS(button_states); j++) {
			g_string_append_printf(css, "%sheaderbar.titlebar windowcontrols > button.%s%s { ", maximized ? "window.maximized " : "",
			                       maximized ? "maximize" : button_names[i], button_states[j].pseudo);
			append_picture(css, dir, button_names[i], button_states[j].file);
		}
	}
	gtk_css_provider_load_from_string(buttons_provider, css->str);
	g_string_free(css, TRUE);
	g_free(dir);
}

static void apply(void)
{
	char *font_name = apply_settings();
	load_css(font_name);
	g_free(font_name);
	load_window_buttons();
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
	buttons_provider = gtk_css_provider_new();
	GdkDisplay *display = gdk_display_get_default();
	gtk_style_context_add_provider_for_display(display, GTK_STYLE_PROVIDER(user_provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
	gtk_style_context_add_provider_for_display(display, GTK_STYLE_PROVIDER(titlebar_provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
	gtk_style_context_add_provider_for_display(display, GTK_STYLE_PROVIDER(buttons_provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
	monitors = g_ptr_array_new_with_free_func(g_object_unref);
	watch("gtk-4.0");
	watch("gtk-3.0");
	watch("gtk-3.0/assets");
	apply();
}
