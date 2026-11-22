/*
 * AOSP frameworks/base/native/android/thermal.cpp against the "thermalservice" of a device without a thermal
 * HAL (services/.../power/thermal/ThermalManagerService.java with mHalReady false), as ATL's Java PowerManager
 * models it: the status stays THROTTLING_NONE, the headroom is NaN, thresholds throw IllegalStateException
 * (EPIPE here), a headroom listener is refused (EPIPE), and a status listener is accepted and told the
 * current status once, from another thread (postStatusListenerLocked()).
 */

#include <errno.h>
#include <math.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

/* thermal.h */
typedef enum AThermalStatus {
	ATHERMAL_STATUS_ERROR = -1,
	ATHERMAL_STATUS_NONE = 0,
} AThermalStatus;

typedef struct AThermalHeadroomThreshold {
	float headroom;
	AThermalStatus thermalStatus;
} AThermalHeadroomThreshold;

typedef void (*AThermal_StatusCallback)(void *data, AThermalStatus status);
typedef void (*AThermal_HeadroomCallback)(void *data, float headroom, float forecastHeadroom, int forecastSeconds,
                                          const AThermalHeadroomThreshold *thresholds, size_t thresholdsCount);

struct status_listener {
	AThermal_StatusCallback callback;
	void *data;
};

typedef struct AThermalManager {
	pthread_mutex_t lock;
	struct status_listener *listeners;
	size_t count;
	bool service_listener; /* mServiceStatusListener is registered with the service */
	bool posted;           /* a thread delivering the service's first onStatusChange() was started */
	pthread_t post;
} AThermalManager;

/* ThermalServiceStatusListener::onStatusChange() -> notifyStateChange(mStatus) */
static void *post_status(void *arg)
{
	AThermalManager *manager = arg;
	pthread_mutex_lock(&manager->lock);
	for (size_t i = 0; i < manager->count; i++)
		manager->listeners[i].callback(manager->listeners[i].data, ATHERMAL_STATUS_NONE);
	pthread_mutex_unlock(&manager->lock);
	return NULL;
}

AThermalManager *AThermal_acquireManager(void)
{
	AThermalManager *manager = calloc(1, sizeof(AThermalManager));
	if (!manager)
		abort();
	pthread_mutex_init(&manager->lock, NULL);
	return manager;
}

void AThermal_releaseManager(AThermalManager *manager)
{
	if (!manager)
		return;
	if (manager->posted)
		pthread_join(manager->post, NULL);
	pthread_mutex_destroy(&manager->lock);
	free(manager->listeners);
	free(manager);
}

AThermalStatus AThermal_getCurrentThermalStatus(AThermalManager *manager)
{
	return ATHERMAL_STATUS_NONE;
}

int AThermal_registerThermalStatusListener(AThermalManager *manager, AThermal_StatusCallback callback, void *data)
{
	pthread_t previous;
	bool join = false;

	pthread_mutex_lock(&manager->lock);
	if (!callback)
		goto einval;
	for (size_t i = 0; i < manager->count; i++)
		if (manager->listeners[i].callback == callback && manager->listeners[i].data == data)
			goto einval;
	struct status_listener *listeners = realloc(manager->listeners, (manager->count + 1) * sizeof(*listeners));
	if (!listeners) {
		pthread_mutex_unlock(&manager->lock);
		return ENOMEM;
	}
	manager->listeners = listeners;
	if (!manager->service_listener) {
		/* registerThermalStatusListener() succeeds and posts the current status to the new listener */
		previous = manager->post;
		join = manager->posted;
		if (pthread_create(&manager->post, NULL, post_status, manager)) {
			manager->post = previous;
			pthread_mutex_unlock(&manager->lock);
			return ENOMEM;
		}
		manager->posted = true;
		manager->service_listener = true;
	}
	manager->listeners[manager->count++] = (struct status_listener){callback, data};
	pthread_mutex_unlock(&manager->lock);
	if (join)
		pthread_join(previous, NULL);
	return 0;

einval:
	pthread_mutex_unlock(&manager->lock);
	return EINVAL;
}

int AThermal_unregisterThermalStatusListener(AThermalManager *manager, AThermal_StatusCallback callback, void *data)
{
	size_t i;

	pthread_mutex_lock(&manager->lock);
	for (i = 0; i < manager->count; i++)
		if (manager->listeners[i].callback == callback && manager->listeners[i].data == data)
			break;
	if (i == manager->count) {
		pthread_mutex_unlock(&manager->lock);
		return EINVAL;
	}
	/* the last one unregisters the service listener, which succeeds */
	if (manager->count == 1)
		manager->service_listener = false;
	manager->count--;
	memmove(&manager->listeners[i], &manager->listeners[i + 1], (manager->count - i) * sizeof(*manager->listeners));
	pthread_mutex_unlock(&manager->lock);
	return 0;
}

/* getThermalHeadroom() returns NaN while the HAL is not ready */
float AThermal_getThermalHeadroom(AThermalManager *manager, int forecastSeconds)
{
	return NAN;
}

/* getThermalHeadroomThresholds() throws IllegalStateException without the HAL: not
 * EX_UNSUPPORTED_OPERATION, so EPIPE */
int AThermal_getThermalHeadroomThresholds(AThermalManager *manager, const AThermalHeadroomThreshold **outThresholds, size_t *size)
{
	if (outThresholds == NULL || *outThresholds != NULL || size == NULL)
		return EINVAL;
	return EPIPE;
}

/* registerThermalHeadroomListener() returns false without the HAL, so no headroom listener is ever kept */
int AThermal_registerThermalHeadroomListener(AThermalManager *manager, AThermal_HeadroomCallback callback, void *data)
{
	if (!callback)
		return EINVAL;
	return EPIPE;
}

int AThermal_unregisterThermalHeadroomListener(AThermalManager *manager, AThermal_HeadroomCallback callback, void *data)
{
	return EINVAL;
}
