/*
 * AOSP frameworks/base/native/android/system_health.cpp. ASystemHealthManager::create() needs the
 * "performance_hint" service (IHintManager) and returns NULL without it, as on ATL; every ASystemHealth_
 * query then returns ENOTSUP. ACpuHeadroomParams/AGpuHeadroomParams are plain data objects.
 */

#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* LOG_ALWAYS_FATAL in AOSP */
static void fatal(const char *func, const char *msg)
{
	fprintf(stderr, "%s: %s\n", func, msg);
	abort();
}

/* system_health.h */
enum {
	ACPU_HEADROOM_CALCULATION_TYPE_MIN = 0,
	ACPU_HEADROOM_CALCULATION_TYPE_AVERAGE = 1,
};
enum {
	AGPU_HEADROOM_CALCULATION_TYPE_MIN = 0,
	AGPU_HEADROOM_CALCULATION_TYPE_AVERAGE = 1,
};

/* android.os.CpuHeadroomParamsInternal / GpuHeadroomParamsInternal, with their defaults */
typedef struct ACpuHeadroomParams {
	int32_t *tids;
	size_t tids_size;
	int32_t calculationWindowMillis;
	int32_t calculationType;
} ACpuHeadroomParams;

typedef struct AGpuHeadroomParams {
	int32_t calculationWindowMillis;
	int32_t calculationType;
} AGpuHeadroomParams;

int ASystemHealth_getMaxCpuHeadroomTidsSize(size_t *outSize)
{
	if (!outSize)
		fatal(__func__, "outSize should not be null");
	return ENOTSUP;
}

int ASystemHealth_getCpuHeadroomCalculationWindowRange(int32_t *outMinMillis, int32_t *outMaxMillis)
{
	if (!outMinMillis)
		fatal(__func__, "outMinMillis should not be null");
	if (!outMaxMillis)
		fatal(__func__, "outMaxMillis should not be null");
	return ENOTSUP;
}

int ASystemHealth_getGpuHeadroomCalculationWindowRange(int32_t *outMinMillis, int32_t *outMaxMillis)
{
	if (!outMinMillis)
		fatal(__func__, "outMinMillis should not be null");
	if (!outMaxMillis)
		fatal(__func__, "outMaxMillis should not be null");
	return ENOTSUP;
}

int ASystemHealth_getCpuHeadroom(const ACpuHeadroomParams *params, float *outHeadroom)
{
	if (!outHeadroom)
		fatal(__func__, "outHeadroom should not be null");
	return ENOTSUP;
}

int ASystemHealth_getGpuHeadroom(const AGpuHeadroomParams *params, float *outHeadroom)
{
	if (!outHeadroom)
		fatal(__func__, "outHeadroom should not be null");
	return ENOTSUP;
}

int ASystemHealth_getCpuHeadroomMinIntervalMillis(int64_t *outMinIntervalMillis)
{
	if (!outMinIntervalMillis)
		fatal(__func__, "outMinIntervalMillis should not be null");
	return ENOTSUP;
}

int ASystemHealth_getGpuHeadroomMinIntervalMillis(int64_t *outMinIntervalMillis)
{
	if (!outMinIntervalMillis)
		fatal(__func__, "outMinIntervalMillis should not be null");
	return ENOTSUP;
}

void ACpuHeadroomParams_setCalculationWindowMillis(ACpuHeadroomParams *params, int windowMillis)
{
	if (windowMillis <= 0)
		fatal(__func__, "windowMillis should be positive");
	params->calculationWindowMillis = windowMillis;
}

void AGpuHeadroomParams_setCalculationWindowMillis(AGpuHeadroomParams *params, int windowMillis)
{
	if (windowMillis <= 0)
		fatal(__func__, "windowMillis should be positive");
	params->calculationWindowMillis = windowMillis;
}

int ACpuHeadroomParams_getCalculationWindowMillis(ACpuHeadroomParams *params)
{
	return params->calculationWindowMillis;
}

int AGpuHeadroomParams_getCalculationWindowMillis(AGpuHeadroomParams *params)
{
	return params->calculationWindowMillis;
}

/* the vector is resized first, then each tid is checked while copying */
void ACpuHeadroomParams_setTids(ACpuHeadroomParams *params, const int *tids, size_t tidsSize)
{
	if (!tids)
		fatal(__func__, "tids should not be null");
	int32_t *copy = tidsSize ? malloc(tidsSize * sizeof(int32_t)) : NULL;
	if (tidsSize && !copy)
		abort();
	free(params->tids);
	params->tids = copy;
	params->tids_size = tidsSize;
	for (size_t i = 0; i < tidsSize; i++) {
		if (tids[i] <= 0)
			fatal(__func__, "Invalid non-positive tid");
		params->tids[i] = tids[i];
	}
}

void ACpuHeadroomParams_setCalculationType(ACpuHeadroomParams *params, int32_t calculationType)
{
	if (calculationType < ACPU_HEADROOM_CALCULATION_TYPE_MIN || calculationType > ACPU_HEADROOM_CALCULATION_TYPE_AVERAGE)
		fatal(__func__, "calculationType should be one of ACpuHeadroomCalculationType values");
	params->calculationType = calculationType;
}

int32_t ACpuHeadroomParams_getCalculationType(ACpuHeadroomParams *params)
{
	return params->calculationType;
}

void AGpuHeadroomParams_setCalculationType(AGpuHeadroomParams *params, int32_t calculationType)
{
	if (calculationType < AGPU_HEADROOM_CALCULATION_TYPE_MIN || calculationType > AGPU_HEADROOM_CALCULATION_TYPE_AVERAGE)
		fatal(__func__, "calculationType should be one of AGpuHeadroomCalculationType values");
	params->calculationType = calculationType;
}

int32_t AGpuHeadroomParams_getCalculationType(AGpuHeadroomParams *params)
{
	return params->calculationType;
}

ACpuHeadroomParams *ACpuHeadroomParams_create(void)
{
	ACpuHeadroomParams *params = calloc(1, sizeof(ACpuHeadroomParams));
	if (!params)
		abort();
	params->calculationWindowMillis = 1000;
	params->calculationType = ACPU_HEADROOM_CALCULATION_TYPE_MIN;
	return params;
}

AGpuHeadroomParams *AGpuHeadroomParams_create(void)
{
	AGpuHeadroomParams *params = calloc(1, sizeof(AGpuHeadroomParams));
	if (!params)
		abort();
	params->calculationWindowMillis = 1000;
	params->calculationType = AGPU_HEADROOM_CALCULATION_TYPE_MIN;
	return params;
}

void ACpuHeadroomParams_destroy(ACpuHeadroomParams *params)
{
	if (params)
		free(params->tids);
	free(params);
}

void AGpuHeadroomParams_destroy(AGpuHeadroomParams *params)
{
	free(params);
}
