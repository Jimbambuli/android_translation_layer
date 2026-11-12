/* the transport of ATLBinderConnection: a SOCK_SEQPACKET socket pair between the app's main process and a process
 * of the app (android:process), one message per transaction, file descriptors with SCM_RIGHTS */

#include <errno.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
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

/* starts this executable again with the command line of this process; ATL_PROCESS_NAME and ATL_IPC_FD make
 * it a process of the app (see main.c), which talks to us over child_fd */
JNIEXPORT jint JNICALL Java_android_atl_ATLBinderConnection_nativeSpawn(JNIEnv *env, jclass this, jint child_fd, jstring process_name, jboolean isolated)
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
	char **envp = calloc(envc + 5, sizeof(char *));
	int e = 0;
	for (int i = 0; i < envc; i++) {
		if (!strncmp(environ[i], "ATL_PROCESS_NAME=", 17) || !strncmp(environ[i], "ATL_IPC_FD=", 11) || !strncmp(environ[i], "ATL_PROCESS_ISOLATED=", 21))
			continue;
		envp[e++] = environ[i];
	}
	const char *name = (*env)->GetStringUTFChars(env, process_name, NULL);
	char *name_env = g_strdup_printf("ATL_PROCESS_NAME=%s", name);
	(*env)->ReleaseStringUTFChars(env, process_name, name);
	char *fd_env = g_strdup_printf("ATL_IPC_FD=%d", child_fd);
	envp[e++] = name_env;
	envp[e++] = fd_env;
	/* the main process already extracted the native libraries */
	envp[e++] = "ATL_SKIP_NATIVES_EXTRACTION=1";
	/* a process of an isolated service (android:isolatedProcess); ATL does not restrict it further */
	if (isolated)
		envp[e++] = "ATL_PROCESS_ISOLATED=1";

	/* dup2 onto the same number clears FD_CLOEXEC, so only this end of the pair stays open in the child */
	posix_spawn_file_actions_t actions;
	posix_spawn_file_actions_init(&actions);
	posix_spawn_file_actions_adddup2(&actions, child_fd, child_fd);
	pid_t pid;
	int ret = posix_spawn(&pid, "/proc/self/exe", &actions, NULL, argv, envp);
	posix_spawn_file_actions_destroy(&actions);
	g_free(name_env);
	g_free(fd_env);
	free(envp);
	free(argv);
	free(cmdline);
	return ret ? -ret : pid;
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
