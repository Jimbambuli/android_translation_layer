#ifndef ZYGOTE_H
#define ZYGOTE_H

#include <jni.h>

/* serves the main process's requests for processes on fd; returns only in a process it forked */
void zygote_run(JNIEnv *env, int fd);

#endif
