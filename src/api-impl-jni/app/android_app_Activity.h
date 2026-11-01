#include <gtk/gtk.h>
#include <jni.h>

void activity_start(JNIEnv *env, jobject activity_object);
void activity_window_ready(void);
void activity_close_all(void);
/* the window's child: the pages of the activities and the layers for their window animations */
GtkWidget *activity_pages_new(void);
GtkWidget *activity_pages_stack(void);
void current_activity_back_pressed(void);
/* for the navigation bar: the activities from the shown one (index 0) down to the first one */
int activity_count(void);
char *activity_title(int index);
void activity_return_to(int index);
/* the global configuration changed: the shown activity is relaunched or told (see Activity.ensureActivityConfiguration()) */
void activity_ensure_configuration(void);
