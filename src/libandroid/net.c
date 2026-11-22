#include <errno.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>

typedef uint64_t net_handle_t;
#define NETWORK_UNSPECIFIED ((net_handle_t)0)

/* AOSP native/android/net.c; the magic value is the one of android.net.Network */
static const uint32_t kHandleMagic = 0xcafed00d;

static int getnetidfromhandle(net_handle_t handle, unsigned *netid)
{
	static const uint32_t k32BitMask = 0xffffffff;

	if (handle != NETWORK_UNSPECIFIED && (handle & k32BitMask) != kHandleMagic)
		return 0;
	if (netid != NULL)
		*netid = ((handle >> (CHAR_BIT * sizeof(k32BitMask))) & k32BitMask);
	return 1;
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
