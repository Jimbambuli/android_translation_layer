#ifndef SYSTEM_THEME_H
#define SYSTEM_THEME_H

#include <glib.h>

/* the GTK theme of the desktop for a sandboxed ATL, see system_theme.c */
void system_theme_init(const char *config_dir);
char *system_theme_host_config_dir(void);

#endif
