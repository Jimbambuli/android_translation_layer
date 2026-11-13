/* the transport of ATLBinderConnection: a SOCK_SEQPACKET socket pair between the app's main process and a process
 * of the app (android:process), one message per transaction, file descriptors with SCM_RIGHTS */

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <unistd.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_atl_ATLBinderConnection.h"

#define MAX_FDS 253 /* SCM_MAX_FD */

extern char **environ;

JNIEXPORT jintArray JNICALL Java_android_atl_ATLBinderConnection_nativeSocketPair(JNIEnv *env, jclass this)
{
	int sv[2];
	if (socketpair(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0, sv) < 0)
		return NULL;
	jintArray ret = (*env)->NewIntArray(env, 2);
	(*env)->SetIntArrayRegion(env, ret, 0, 2, sv);
	return ret;
}

/* starts this executable again with the command line and the environment of this process, plus extra_env;
 * keep_fd stays open in it */
static pid_t spawn_self(int keep_fd, char **extra_env)
{
	char *cmdline = NULL;
	size_t len = 0;
	FILE *f = fopen("/proc/self/cmdline", "re");
	if (!f)
		return -errno;
	char buf[4096];
	size_t n;
	while ((n = fread(buf, 1, sizeof(buf), f)) > 0) {
		cmdline = realloc(cmdline, len + n);
		memcpy(cmdline + len, buf, n);
		len += n;
	}
	fclose(f);
	int argc = 0;
	for (size_t i = 0; i < len; i++)
		if (cmdline[i] == '\0')
			argc++;
	char **argv = calloc(argc + 1, sizeof(char *));
	int a = 0;
	for (size_t i = 0; i < len;) {
		char *arg = cmdline + i;
		i += strlen(arg) + 1;
		char *next = i < len ? cmdline + i : NULL;
		/* a JVMTI agent (atl-doctor) belongs to the process it was started for: the other processes of the app
		 * do not load it, or they would all write into its files */
		if (!strncmp(arg, "--attach-agent=", 15) || !strncmp(arg, "--extra-jvm-option=-agent", 25))
			continue;
		if (next && (!strcmp(arg, "--attach-agent") || ((!strcmp(arg, "-X") || !strcmp(arg, "--extra-jvm-option")) && !strncmp(next, "-agent", 6)))) {
			i += strlen(next) + 1;
			continue;
		}
		argv[a++] = arg;
	}

	int envc = 0;
	while (environ[envc])
		envc++;
	int extrac = g_strv_length(extra_env);
	char **envp = calloc(envc + extrac + 1, sizeof(char *));
	int e = 0;
	for (int i = 0; i < envc; i++) {
		if (!strncmp(environ[i], "ATL_PROCESS_NAME=", 17) || !strncmp(environ[i], "ATL_IPC_FD=", 11) || !strncmp(environ[i], "ATL_PROCESS_ISOLATED=", 21) || !strncmp(environ[i], "ATL_ZYGOTE_FD=", 14))
			continue;
		envp[e++] = environ[i];
	}
	for (int i = 0; i < extrac; i++)
		envp[e++] = extra_env[i];

	/* dup2 onto the same number clears FD_CLOEXEC, so only this end of the pair stays open in the child */
	posix_spawn_file_actions_t actions;
	posix_spawn_file_actions_init(&actions);
	posix_spawn_file_actions_adddup2(&actions, keep_fd, keep_fd);
	pid_t pid;
	int ret = posix_spawn(&pid, "/proc/self/exe", &actions, NULL, argv, envp);
	posix_spawn_file_actions_destroy(&actions);
	free(envp);
	free(argv);
	free(cmdline);
	return ret ? -ret : pid;
}

/* the app's zygote (main-executable/zygote.c): started with the first process the app starts, it forks the others,
 * which saves each of them starting the runtime */
static pthread_mutex_t zygote_lock = PTHREAD_MUTEX_INITIALIZER;
static int zygote_fd = -1;
static pid_t zygote_pid;
/* a zygote that failed is not tried again */
static gboolean zygote_failed;

static void zygote_stop(void)
{
	zygote_failed = TRUE;
	close(zygote_fd);
	zygote_fd = -1;
	kill(zygote_pid, SIGKILL);
	waitpid(zygote_pid, NULL, 0);
}

/* a process of the app from the zygote: its pid, -ECHILD if there is no zygote (the caller then starts the process
 * itself) or -errno if the fork failed */
static pid_t zygote_fork(int child_fd, const char *name, jboolean isolated)
{
	pthread_mutex_lock(&zygote_lock);
	if (zygote_failed) {
		pthread_mutex_unlock(&zygote_lock);
		return -ECHILD;
	}
	if (zygote_fd < 0) {
		int sv[2];
		if (socketpair(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0, sv) < 0) {
			zygote_failed = TRUE;
			pthread_mutex_unlock(&zygote_lock);
			return -ECHILD;
		}
		char *fd_env = g_strdup_printf("ATL_ZYGOTE_FD=%d", sv[1]);
		pid_t pid = spawn_self(sv[1], (char *[]){fd_env, NULL});
		g_free(fd_env);
		close(sv[1]);
		if (pid < 0) {
			close(sv[0]);
			zygote_failed = TRUE;
			pthread_mutex_unlock(&zygote_lock);
			return -ECHILD;
		}
		zygote_fd = sv[0];
		zygote_pid = pid;
		/* the first answer comes once the zygote's runtime is up; one that never comes is a zygote that hangs */
		struct timeval timeout = {.tv_sec = 30};
		setsockopt(zygote_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
	}

	size_t name_len = strlen(name);
	char *request = malloc(name_len + 1);
	request[0] = isolated ? 1 : 0;
	memcpy(request + 1, name, name_len);
	struct iovec iov = {.iov_base = request, .iov_len = name_len + 1};
	char control[CMSG_SPACE(sizeof(int))];
	memset(control, 0, sizeof(control));
	struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
	struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
	cmsg->cmsg_level = SOL_SOCKET;
	cmsg->cmsg_type = SCM_RIGHTS;
	cmsg->cmsg_len = CMSG_LEN(sizeof(int));
	memcpy(CMSG_DATA(cmsg), &child_fd, sizeof(int));
	ssize_t ret;
	do {
		ret = sendmsg(zygote_fd, &msg, MSG_NOSIGNAL);
	} while (ret < 0 && errno == EINTR);
	free(request);
	int32_t pid = -EPIPE;
	if (ret >= 0) {
		do {
			ret = recv(zygote_fd, &pid, sizeof(pid), 0);
		} while (ret < 0 && errno == EINTR);
	}
	if (ret != sizeof(pid)) {
		fprintf(stderr, "ATLProcess: the zygote does not answer (%s), starting processes without it\n", ret < 0 ? strerror(errno) : "it ended");
		zygote_stop();
		pid = -ECHILD;
	}
	pthread_mutex_unlock(&zygote_lock);
	return pid;
}

/* a process of the app (android:process), with ATL_PROCESS_NAME and ATL_IPC_FD (see main.c), which talks to us over
 * child_fd: forked by the app's zygote, or else started anew from this executable */
JNIEXPORT jint JNICALL Java_android_atl_ATLBinderConnection_nativeSpawn(JNIEnv *env, jclass this, jint child_fd, jstring process_name, jboolean isolated)
{
	const char *name = (*env)->GetStringUTFChars(env, process_name, NULL);
	pid_t pid = zygote_fork(child_fd, name, isolated);
	if (pid == -ECHILD) {
		char *name_env = g_strdup_printf("ATL_PROCESS_NAME=%s", name);
		char *fd_env = g_strdup_printf("ATL_IPC_FD=%d", child_fd);
		/* the main process already extracted the native libraries; ATL_PROCESS_ISOLATED: a process of an isolated
		 * service (android:isolatedProcess), which ATL does not restrict further */
		pid = spawn_self(child_fd, (char *[]){name_env, fd_env, "ATL_SKIP_NATIVES_EXTRACTION=1", isolated ? "ATL_PROCESS_ISOLATED=1" : NULL, NULL});
		g_free(name_env);
		g_free(fd_env);
	}
	(*env)->ReleaseStringUTFChars(env, process_name, name);
	return pid;
}

JNIEXPORT jint JNICALL Java_android_atl_ATLBinderConnection_nativeSend(JNIEnv *env, jclass this, jint fd, jbyteArray data, jintArray fds)
{
	jsize len = (*env)->GetArrayLength(env, data);
	jsize nfds = fds ? (*env)->GetArrayLength(env, fds) : 0;
	if (nfds > MAX_FDS)
		return -EMFILE;
	jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
	struct iovec iov = {.iov_base = bytes, .iov_len = len};
	struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1};
	char control[CMSG_SPACE(MAX_FDS * sizeof(int))];
	if (nfds) {
		memset(control, 0, sizeof(control));
		msg.msg_control = control;
		msg.msg_controllen = CMSG_SPACE(nfds * sizeof(int));
		struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
		cmsg->cmsg_level = SOL_SOCKET;
		cmsg->cmsg_type = SCM_RIGHTS;
		cmsg->cmsg_len = CMSG_LEN(nfds * sizeof(int));
		(*env)->GetIntArrayRegion(env, fds, 0, nfds, (jint *)CMSG_DATA(cmsg));
	}
	ssize_t ret;
	do {
		ret = sendmsg(fd, &msg, MSG_NOSIGNAL);
	} while (ret < 0 && errno == EINTR);
	int err = errno;
	(*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
	return ret < 0 ? -err : 0;
}

/* the next message, or null at the end of the connection; fds_out[0] takes the number of received file
 * descriptors, fds_out[1..] the descriptors */
JNIEXPORT jbyteArray JNICALL Java_android_atl_ATLBinderConnection_nativeReceive(JNIEnv *env, jclass this, jint fd, jintArray fds_out)
{
	ssize_t len;
	do {
		len = recv(fd, NULL, 0, MSG_PEEK | MSG_TRUNC);
	} while (len < 0 && errno == EINTR);
	if (len <= 0)
		return NULL;
	jbyte *bytes = malloc(len);
	struct iovec iov = {.iov_base = bytes, .iov_len = len};
	char control[CMSG_SPACE(MAX_FDS * sizeof(int))];
	struct msghdr msg = {.msg_iov = &iov, .msg_iovlen = 1, .msg_control = control, .msg_controllen = sizeof(control)};
	ssize_t ret;
	do {
		ret = recvmsg(fd, &msg, MSG_CMSG_CLOEXEC);
	} while (ret < 0 && errno == EINTR);
	if (ret <= 0) {
		free(bytes);
		return NULL;
	}
	jint nfds = 0;
	for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
		if (cmsg->cmsg_level != SOL_SOCKET || cmsg->cmsg_type != SCM_RIGHTS)
			continue;
		int n = (cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int);
		(*env)->SetIntArrayRegion(env, fds_out, 1 + nfds, n, (jint *)CMSG_DATA(cmsg));
		nfds += n;
	}
	(*env)->SetIntArrayRegion(env, fds_out, 0, 1, &nfds);
	jbyteArray data = (*env)->NewByteArray(env, ret);
	(*env)->SetByteArrayRegion(env, data, 0, ret, bytes);
	free(bytes);
	return data;
}

/* reaps a process of the app that ended */
JNIEXPORT jint JNICALL Java_android_atl_ATLBinderConnection_nativeWaitPid(JNIEnv *env, jclass this, jint pid)
{
	int status;
	pid_t ret;
	do {
		ret = waitpid(pid, &status, 0);
	} while (ret < 0 && errno == EINTR);
	return ret < 0 ? -errno : status;
}
