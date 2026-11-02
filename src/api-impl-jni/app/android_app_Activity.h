#include <gtk/gtk.h>
#include <jni.h>

void activity_start(JNIEnv *env, jobject activity_object);
void activity_window_ready(void);
void activity_close_all(void);
/* the window's close request: TRUE if the app keeps running without its window (a foreground service runs) */
gboolean activity_window_close_request(void);
/* shows the app's window again (also after it was closed while a foreground service kept the app running) */
void activity_window_present(void);
/* ends the app, even while a foreground service runs */
void activity_app_quit(void);
/* the window's child: the pages of the activities and the layers for their window animations */
GtkWidget *activity_pages_new(void);
GtkWidget *activity_pages_stack(void);
void current_activity_back_pressed(void);
/* the keys that reach the app's window go to the shown activity (Window.dispatchKeyEventFromNative()) */
void activity_window_add_key_controller(GtkWindow *window);
/* for the navigation bar: the activities from the shown one (index 0) down to the first one */
int activity_count(void);
char *activity_title(int index);
char *activity_class_name(int index);
gboolean activity_same(int index, int other);
void activity_return_to(int index);
/* the global configuration changed: the shown activity is relaunched or told (see Activity.ensureActivityConfiguration()) */
void activity_ensure_configuration(void);
