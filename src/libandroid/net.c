#include <arpa/inet.h>
#include <arpa/nameser.h>
#include <errno.h>
#include <limits.h>
#include <netdb.h>
#include <pthread.h>
#include <resolv.h>
#include <stdatomic.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

typedef uint64_t net_handle_t;
#define NETWORK_UNSPECIFIED ((net_handle_t)0)

/* AOSP native/android/net.c; the magic value is the one of android.net.Network */
static const uint32_t kHandleMagic = 0xcafed00d;
static const uint32_t kHandleMagicSize = 32;

/* netid_client.h, DnsProxydProtocol.h (android.net.Network USE_LOCAL_NAMESERVERS_FLAG) */
#define NETID_UNSET 0u
#define NETID_USE_LOCAL_NAMESERVERS 0x80000000u
/* ATL's one network, the netId of android.net.Network() */
#define DEFAULT_NETID 100u
/* multinetwork.h ResNsendFlags */
#define ANDROID_RESOLV_NO_RETRY (1 << 0)
/* NetdClient.cpp MAX_CMD_SIZE, DnsResolver resolv_private.h MAXPACKET */
#define MAX_CMD_SIZE 4096
#define MAXPACKET (8 * 1024)
/* android_filesystem_config.h */
#define AID_ROOT 0
#define AID_SYSTEM 1000
#define AID_CLAT 1029
#define AID_DNS 1051
#define AID_USER_OFFSET 100000

static int getnetidfromhandle(net_handle_t handle, unsigned *netid)
{
	static const uint32_t k32BitMask = 0xffffffff;

	if (handle != NETWORK_UNSPECIFIED && (handle & k32BitMask) != kHandleMagic)
		return 0;
	if (netid != NULL)
		*netid = ((handle >> (CHAR_BIT * sizeof(k32BitMask))) & k32BitMask);
	return 1;
}

static net_handle_t gethandlefromnetid(unsigned netid)
{
	if (netid == NETID_UNSET)
		return NETWORK_UNSPECIFIED;
	return (((net_handle_t)netid) << kHandleMagicSize) | kHandleMagic;
}

/* AOSP binds the socket to the network through netd (setNetworkForSocket). ATL has one network, the
 * host's (android.net.Network.bindSocket() binds nothing either), so a valid handle leaves the socket
 * as it is. */
int android_setsocknetwork(net_handle_t network, int fd)
{
	unsigned netid;
	if (!getnetidfromhandle(network, &netid)) {
		errno = EINVAL;
		return -1;
	}
	return 0;
}

/* NetdClient.cpp: the process defaults that netd's client library keeps */
static atomic_uint netIdForProcess = NETID_UNSET;
static atomic_uint netIdForResolv = NETID_UNSET;

/* NetdClient.cpp getNetworkForResolv() */
static unsigned getnetworkforresolv(unsigned netid)
{
	if (netid != NETID_UNSET)
		return netid;
	if ((atomic_load(&netIdForResolv) & ~NETID_USE_LOCAL_NAMESERVERS) != NETID_UNSET)
		return atomic_load(&netIdForResolv);
	netid = atomic_load(&netIdForProcess);
	if (netid != NETID_UNSET)
		return netid;
	return atomic_load(&netIdForResolv);
}

/* NetdClient.cpp setNetworkForTarget(): netd lets the probe socket select a network only if it exists
 * (NetworkController::checkUserNetworkAccessLocked(): -ENONET), and ATL has just DEFAULT_NETID */
static int setnetworkfortarget(net_handle_t network, atomic_uint *target)
{
	unsigned netid, requested;
	if (!getnetidfromhandle(network, &netid)) {
		errno = EINVAL;
		return -1;
	}
	requested = netid;
	netid &= ~NETID_USE_LOCAL_NAMESERVERS;
	if (netid == NETID_UNSET) {
		atomic_store(target, netid);
		return 0;
	}
	if (netid != DEFAULT_NETID) {
		errno = ENONET;
		return -1;
	}
	atomic_store(target, requested);
	return 0;
}

int android_setprocnetwork(net_handle_t network)
{
	return setnetworkfortarget(network, &netIdForProcess);
}

int android_getprocnetwork(net_handle_t *network)
{
	if (network == NULL) {
		errno = EINVAL;
		return -1;
	}
	*network = gethandlefromnetid(atomic_load(&netIdForProcess) & ~NETID_USE_LOCAL_NAMESERVERS);
	return 0;
}

int android_setprocdns(net_handle_t network)
{
	return setnetworkfortarget(network, &netIdForResolv);
}

/* NetdClient.cpp getNetworkForDns() asks dnsproxyd ("getdnsnetid"), which answers with
 * NetworkController::getNetworkContext().app_netid: an unset netId becomes the default network */
int android_getprocdns(net_handle_t *network)
{
	if (network == NULL) {
		errno = EINVAL;
		return -1;
	}
	unsigned netid = getnetworkforresolv(NETID_UNSET);
	unsigned local = netid & NETID_USE_LOCAL_NAMESERVERS;
	netid &= ~NETID_USE_LOCAL_NAMESERVERS;
	if (netid == NETID_UNSET)
		netid = DEFAULT_NETID;
	*network = gethandlefromnetid(netid | local);
	return 0;
}

/* every valid handle names the host's network, so the lookup is the host's getaddrinfo() */
int android_getaddrinfofornetwork(net_handle_t network, const char *node, const char *service,
                                  const struct addrinfo *hints, struct addrinfo **res)
{
	unsigned netid;
	if (!getnetidfromhandle(network, &netid)) {
		errno = EINVAL;
		return EAI_SYSTEM;
	}
	return getaddrinfo(node, service, hints, res);
}

/* AOSP sends the query to dnsproxyd and returns the socket on which the answer arrives:
 * a big-endian int32 -errno, or the rcode followed by the int32 answer length and the answer
 * (DnsProxyListener.cpp ResNSendHandler::run()). Here a thread asks the host resolver
 * (/etc/resolv.conf) with res_nsend() and answers the same way on a socket pair. */
struct res_query {
	int fd;
	uint32_t flags;
	size_t len;
	uint8_t msg[];
};

static int send_all(int fd, const void *buf, size_t len)
{
	while (len > 0) {
		ssize_t n = send(fd, buf, len, MSG_NOSIGNAL);
		if (n < 0) {
			if (errno == EINTR)
				continue;
			return -1;
		}
		buf = (const uint8_t *)buf + n;
		len -= n;
	}
	return 0;
}

static int send_be32(int fd, int32_t value)
{
	uint32_t be = htonl((uint32_t)value);
	return send_all(fd, &be, sizeof(be));
}

/* DnsProxyListener.cpp parseQuery(): the message must hold a question */
static int parse_query(const uint8_t *msg, size_t len)
{
	if (len < HFIXEDSZ || ((msg[4] << 8) | msg[5]) == 0)
		return 0;
	int n = dn_skipname(msg + HFIXEDSZ, msg + len);
	return n >= 0 && HFIXEDSZ + (size_t)n + QFIXEDSZ <= len;
}

static void *res_send_thread(void *arg)
{
	struct res_query *query = arg;
	uint8_t answer[MAXPACKET];
	struct __res_state state;
	int32_t len;
	int rcode = 0;

	memset(&state, 0, sizeof(state));
	if (!parse_query(query->msg, query->len)) {
		len = -EINVAL;
	} else if (res_ninit(&state) < 0) {
		len = errno ? -errno : -ESRCH;
	} else {
		/* "send a single request to a single resolver"; the host resolver has no cache to skip */
		if (query->flags & ANDROID_RESOLV_NO_RETRY) {
			state.retry = 1;
			state.nscount = 1;
		}
		errno = 0;
		len = res_nsend(&state, query->msg, query->len, answer, sizeof(answer));
		if (len < 0)
			len = errno ? -errno : -ETIMEDOUT;
		else
			rcode = ((HEADER *)answer)->rcode;
		res_nclose(&state);
	}

	if (len < 0)
		send_be32(query->fd, len);
	else if (send_be32(query->fd, rcode) == 0 && send_be32(query->fd, len) == 0)
		send_all(query->fd, answer, len);
	close(query->fd);
	free(query);
	return NULL;
}

/* NetdClient.cpp resNetworkSend() */
static int res_network_send(unsigned netid, const uint8_t *msg, size_t msglen, uint32_t flags)
{
	int sv[2];
	pthread_t thread;
	pthread_attr_t attr;

	/* the command "resnsend <netId> <flags> <base64 query>\0" must fit dnsproxyd's buffer */
	size_t cmdlen = snprintf(NULL, 0, "resnsend %u %u ", getnetworkforresolv(netid), flags) + (msglen + 2) / 3 * 4 + 1;
	if (cmdlen > MAX_CMD_SIZE)
		return -EMSGSIZE;

	struct res_query *query = malloc(sizeof(*query) + msglen);
	if (!query)
		return -ENOMEM;
	if (socketpair(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0, sv) < 0) {
		int err = errno;
		free(query);
		return -err;
	}
	query->fd = sv[1];
	query->flags = flags;
	query->len = msglen;
	memcpy(query->msg, msg, msglen);

	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
	int err = pthread_create(&thread, &attr, res_send_thread, query);
	pthread_attr_destroy(&attr);
	if (err) {
		close(sv[0]);
		close(sv[1]);
		free(query);
		return -err;
	}
	/* resNetworkSend() shuts down its sending side */
	shutdown(sv[0], SHUT_WR);
	return sv[0];
}

/* a query res_mkquery() cannot build has a negative length, which resNetworkSend() cannot encode */
int android_res_nquery(net_handle_t network, const char *dname, int ns_class, int ns_type, uint32_t flags)
{
	unsigned netid;
	uint8_t buf[MAX_CMD_SIZE];
	if (!getnetidfromhandle(network, &netid))
		return -ENONET;
	int len = res_mkquery(ns_o_query, dname, ns_class, ns_type, NULL, 0, NULL, buf, sizeof(buf));
	if (len < 0)
		return -EMSGSIZE;
	return res_network_send(netid, buf, len, flags);
}

int android_res_nsend(net_handle_t network, const uint8_t *msg, size_t msglen, uint32_t flags)
{
	unsigned netid;
	if (!getnetidfromhandle(network, &netid))
		return -ENONET;
	return res_network_send(netid, msg, msglen, flags);
}

/* NetdClient.cpp readBE32(), readData() */
static int read_be32(int fd, int32_t *result)
{
	uint32_t be;
	ssize_t n;
	do
		n = read(fd, &be, sizeof(be));
	while (n < 0 && errno == EINTR);
	if (n < (ssize_t)sizeof(be))
		return 0;
	*result = (int32_t)ntohl(be);
	return 1;
}

static int read_data(int fd, void *buf, size_t size)
{
	size_t current = 0;
	for (;;) {
		ssize_t rc = read(fd, (char *)buf + current, size - current);
		if (rc > 0) {
			current += rc;
			if (current == size)
				break;
		} else if (rc == 0) {
			return -EIO;
		} else if (errno != EINTR) {
			return -errno;
		}
	}
	return 0;
}

/* NetdClient.cpp resNetworkResult(); fd is closed in every case */
int android_res_nresult(int fd, int *rcode, uint8_t *answer, size_t anslen)
{
	int32_t result = 0, size = 0;
	int ret;

	if (!read_be32(fd, &result)) {
		ret = -errno;
	} else if (result < 0) {
		ret = result;
	} else {
		*rcode = result;
		if (!read_be32(fd, &size))
			ret = -EREMOTEIO;
		else if (anslen < (size_t)size)
			ret = -EMSGSIZE;
		else if ((ret = read_data(fd, answer, size)) == 0)
			ret = size;
	}
	close(fd);
	return ret;
}

/* resNetworkCancel(): closing the socket; the thread's late answer then goes nowhere */
void android_res_cancel(int nsend_fd)
{
	close(nsend_fd);
}

/* Socket tagging: NetdClient.cpp tagSocket()/untagSocket() check the socket, then the Connectivity module's
 * BpfHandler.cpp tagSocket()/untagSocket() check the uids and the socket cookie and write the cookie tag map,
 * which only feeds traffic accounting. ATL has no accounting, so the map is just the set of tagged cookies
 * (untagging an untagged socket is ENOENT, as deleting a missing map entry). */
static pthread_mutex_t tagged_lock = PTHREAD_MUTEX_INITIALIZER;
static uint64_t *tagged;
static size_t tagged_count;

/* NetdClient.cpp checkSocket() */
static int check_socket(int fd)
{
	int family;
	socklen_t len = sizeof(family);
	if (fd < 0)
		return -EBADF;
	if (getsockopt(fd, SOL_SOCKET, SO_DOMAIN, &family, &len) == -1)
		return -errno;
	if (family != AF_INET && family != AF_INET6)
		return -EAFNOSUPPORT;
	return 0;
}

/* bpf/BpfUtils.h getSocketCookie() */
static int get_socket_cookie(int fd, uint64_t *cookie)
{
	socklen_t len = sizeof(*cookie);
	if (getsockopt(fd, SOL_SOCKET, SO_COOKIE, cookie, &len))
		return -errno;
	return 0;
}

static ssize_t find_tagged(uint64_t cookie)
{
	for (size_t i = 0; i < tagged_count; i++)
		if (tagged[i] == cookie)
			return i;
	return -1;
}

int android_tag_socket_with_uid(int sockfd, uint32_t tag, uid_t uid)
{
	uint64_t cookie;
	int ret = check_socket(sockfd);
	if (ret)
		return ret;
	/* FwmarkServer.cpp: -1 means the caller's uid */
	uid_t real_uid = getuid();
	if (uid == (uid_t)-1)
		uid = real_uid;
	/* hasUpdateDeviceStatsPermission() without a uid permission map entry */
	uint32_t app_id = real_uid % AID_USER_OFFSET;
	if (uid != real_uid && app_id != AID_ROOT && app_id != AID_SYSTEM && app_id != AID_DNS)
		return -EPERM;
	if (uid == AID_CLAT)
		return -EPERM;
	ret = get_socket_cookie(sockfd, &cookie);
	if (ret)
		return ret;
	pthread_mutex_lock(&tagged_lock);
	if (find_tagged(cookie) < 0) {
		uint64_t *grown = realloc(tagged, (tagged_count + 1) * sizeof(*tagged));
		if (!grown) {
			pthread_mutex_unlock(&tagged_lock);
			return -ENOMEM;
		}
		tagged = grown;
		tagged[tagged_count++] = cookie;
	}
	pthread_mutex_unlock(&tagged_lock);
	return 0;
}

int android_tag_socket(int sockfd, uint32_t tag)
{
	return android_tag_socket_with_uid(sockfd, tag, (uid_t)-1);
}

int android_untag_socket(int sockfd)
{
	uint64_t cookie;
	int ret = check_socket(sockfd);
	if (ret)
		return ret;
	ret = get_socket_cookie(sockfd, &cookie);
	if (ret)
		return ret;
	pthread_mutex_lock(&tagged_lock);
	ssize_t i = find_tagged(cookie);
	if (i < 0) {
		ret = -ENOENT;
	} else {
		tagged[i] = tagged[--tagged_count];
		ret = 0;
	}
	pthread_mutex_unlock(&tagged_lock);
	return ret;
}
