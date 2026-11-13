/*
 * The app's zygote, like Android's: a process of the app whose runtime was started (see zygote_main() in main.c)
 * without anything of GTK, Wayland or D-Bus, and which keeps only its main thread while it waits. The main process
 * (ATLBinderConnection.c) asks it over a SOCK_SEQPACKET socket for a process of the app (android:process); the
 * zygote forks one, which goes on in main() like a process started anew, but with the runtime ready.
 *
 * Request: one message, byte 0 = isolated (android:isolatedProcess), then the process name; the end of the main
 * process's socket pair for the new process comes along with SCM_RIGHTS. Reply: the pid as int32_t, or -errno.
 */

#define _GNU_SOURCE

#include <dirent.h>
#include <errno.h>
#include <jni.h>
#include <poll.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/signalfd.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#include "zygote.h"

static uint32_t thread_count(void)
{
	uint32_t n = 0;
	DIR *dir = opendir("/proc/self/task");
	if (!dir)
		return UINT32_MAX;
	struct dirent *entry;
	while ((entry = readdir(dir)))
		if (entry->d_name[0] != '.')
			n++;
	closedir(dir);
	return n;
}

static void print_threads(void)
{
	DIR *dir = opendir("/proc/self/task");
	struct dirent *entry;
	while (dir && (entry = readdir(dir))) {
		if (entry->d_name[0] == '.')
			continue;
		char path[64], name[32] = "";
		snprintf(path, sizeof(path), "/proc/self/task/%s/comm", entry->d_name);
		FILE *f = fopen(path, "re");
		if (f) {
			if (fgets(name, sizeof(name), f))
				name[strcspn(name, "\n")] = '\0';
			fclose(f);
		}
		fprintf(stderr, "ATL zygote: thread %s (%s)\n", entry->d_name, name);
	}
	if (dir)
		closedir(dir);
}

static void check_exception(JNIEnv *env, const char *what)
{
	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "ATL zygote: %s failed\n", what);
		(*env)->ExceptionDescribe(env);
		_exit(1);
	}
}

static void reply(int fd, int32_t value)
{
	while (send(fd, &value, sizeof(value), MSG_NOSIGNAL) < 0 && errno == EINTR)
		;
}

void zygote_run(JNIEnv *env, int fd)
{
	jclass daemons = (*env)->FindClass(env, "java/lang/Daemons");
	check_exception(env, "java.lang.Daemons");
	jclass hooks = (*env)->FindClass(env, "dalvik/system/ZygoteHooks");
	check_exception(env, "dalvik.system.ZygoteHooks");
	jclass math = (*env)->FindClass(env, "java/lang/Math");
	check_exception(env, "java.lang.Math");
	jmethodID daemons_stop = (*env)->GetStaticMethodID(env, daemons, "stop", "()V");
	jmethodID daemons_start = (*env)->GetStaticMethodID(env, daemons, "startPostZygoteFork", "()V");
	jmethodID pre_fork = (*env)->GetStaticMethodID(env, hooks, "nativePreFork", "()J");
	jmethodID post_fork_child = (*env)->GetStaticMethodID(env, hooks, "nativePostForkChild", "(JIZZLjava/lang/String;)V");
	jmethodID random_seed = (*env)->GetStaticMethodID(env, math, "setRandomSeedInternal", "(J)V");
	check_exception(env, "looking up the zygote hooks");

	/* like ZygoteHooks.preFork(): the runtime's daemons end here, the JIT's thread in nativePreFork(); the zygote
	 * then has only this thread (fork() takes only the calling thread along) */
	(*env)->CallStaticVoidMethod(env, daemons, daemons_stop);
	check_exception(env, "Daemons.stop()");
	jlong token = (*env)->CallStaticLongMethod(env, hooks, pre_fork);
	check_exception(env, "ZygoteHooks.nativePreFork()");
	for (uint32_t i = 0; thread_count() > 1; i++) {
		if (i == 5000) { // 5 s: a thread that does not end (the main process then starts its processes itself)
			fprintf(stderr, "ATL zygote: other threads keep running, cannot fork:\n");
			print_threads();
			_exit(1);
		}
		usleep(1000);
	}

	/* the processes are the zygote's children: it reaps them (their end reaches the main process as the end of
	 * their connection) */
	sigset_t sigchld;
	sigemptyset(&sigchld);
	sigaddset(&sigchld, SIGCHLD);
	sigprocmask(SIG_BLOCK, &sigchld, NULL);
	int sfd = signalfd(-1, &sigchld, SFD_CLOEXEC);

	for (;;) {
		struct pollfd fds[2] = {{.fd = fd, .events = POLLIN}, {.fd = sfd, .events = POLLIN}};
		if (poll(fds, 2, -1) < 0) {
			if (errno == EINTR)
				continue;
			_exit(1);
		}
		if (fds[1].revents & POLLIN) {
			struct signalfd_siginfo info;
			while (read(sfd, &info, sizeof(info)) < 0 && errno == EINTR)
				;
			int status;
			pid_t pid;
			while ((pid = waitpid(-1, &status, WNOHANG)) > 0)
				fprintf(stderr, "ATL zygote: process %d ended, status %d\n", pid, status);
		}
		if (!(fds[0].revents & (POLLIN | POLLHUP | POLLERR)))
			continue;

		char buf[4096];
		char control[CMSG_SPACE(sizeof(int))];
		struct iovec iov = {.iov_base = buf, .iov_len = sizeof(buf) - 1};
		struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
		ssize_t n = recvmsg(fd, &msg, MSG_CMSG_CLOEXEC);
		if (n < 0 && errno == EINTR)
			continue;
		if (n <= 0) // the main process has ended
			_exit(0);
		struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
		if (n < 2 || !cmsg || cmsg->cmsg_type != SCM_RIGHTS) {
			reply(fd, -EINVAL);
			continue;
		}
		int child_fd;
		memcpy(&child_fd, CMSG_DATA(cmsg), sizeof(int));
		buf[n] = '\0';
		int isolated = buf[0];
		const char *name = buf + 1;

		(*env)->CallStaticLongMethod(env, hooks, pre_fork);
		check_exception(env, "ZygoteHooks.nativePreFork()");
		fflush(NULL);
		pid_t pid = fork();
		if (pid == 0) {
			close(fd);
			close(sfd);
			sigprocmask(SIG_UNBLOCK, &sigchld, NULL);
			/* what the main process used to pass to a process it started (see ATLProcess.java and main.c); the
			 * received descriptor is close-on-exec, which does not matter without an exec */
			char fd_string[16];
			snprintf(fd_string, sizeof(fd_string), "%d", child_fd);
			setenv("ATL_PROCESS_NAME", name, 1);
			setenv("ATL_IPC_FD", fd_string, 1);
			setenv("ATL_SKIP_NATIVES_EXTRACTION", "1", 1);
			if (isolated)
				setenv("ATL_PROCESS_ISOLATED", "1", 1);
			unsetenv("ATL_ZYGOTE_FD");
			/* like ZygoteHooks.postForkChild() and postForkCommon(): the runtime's threads, the signal catcher and
			 * a seed of its own for Math.random() */
			(*env)->CallStaticVoidMethod(env, hooks, post_fork_child, token, 0, JNI_FALSE, JNI_FALSE, NULL);
			check_exception(env, "ZygoteHooks.nativePostForkChild()");
			struct timespec now;
			clock_gettime(CLOCK_MONOTONIC, &now);
			(*env)->CallStaticVoidMethod(env, math, random_seed, (jlong)(now.tv_sec * 1000000000LL + now.tv_nsec) ^ ((jlong)getpid() << 32));
			check_exception(env, "Math.setRandomSeedInternal()");
			(*env)->CallStaticVoidMethod(env, daemons, daemons_start);
			check_exception(env, "Daemons.startPostZygoteFork()");
			return;
		}
		int err = errno;
		close(child_fd);
		reply(fd, pid < 0 ? -err : pid);
	}
}
