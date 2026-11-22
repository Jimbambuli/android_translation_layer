/*
 * AOSP frameworks/base/native/android/performance_hint.cpp. APerformanceHintManager::create() returns NULL
 * when there is no "performance_hint" service (IHintManager), which is ATL's case: its Java
 * PerformanceHintManager hands out no instance either. So APerformanceHint_getManager() is NULL, no manager
 * and no session ever exists, and isFeatureSupported() is false. A pointer passed as manager or session can
 * only be a dead one; those calls answer as a failed service call does (EPIPE, NULL). AWorkDuration and
 * ASessionCreationConfig are plain data objects.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include <jni.h>

/* LOG_ALWAYS_FATAL in AOSP */
static void fatal(const char *func, const char *msg)
{
	fprintf(stderr, "%s: %s\n", func, msg);
	abort();
}
#define VALIDATE_PTR(ptr) \
	if (!(ptr))       \
		fatal(__func__, #ptr " is nullptr");
#define VALIDATE_INT(value, cmp) \
	if (!((value)cmp))       \
		return EINVAL;

/* hal::WorkDuration (hardware/interfaces/power/aidl/.../WorkDuration.aidl) */
struct AWorkDuration {
	int64_t timeStampNanos;
	int64_t durationNanos;
	int64_t workPeriodStartTimestampNanos;
	int64_t cpuDurationNanos;
	int64_t gpuDurationNanos;
	int64_t intendedPresentTimestampNanos;
};

/* hal::SessionMode, kept as the set modesToEnable */
enum {
	MODE_POWER_EFFICIENCY = 1 << 0,
	MODE_GRAPHICS_PIPELINE = 1 << 1,
	MODE_AUTO_CPU = 1 << 2,
	MODE_AUTO_GPU = 1 << 3,
};

/* android.os.SessionCreationConfig. layers stays empty: ATL's windows have no SurfaceControl handle and
 * no ASurfaceControl exists, so layersFromNativeSurfaces() finds nothing */
struct ASessionCreationConfig {
	int32_t *tids;
	size_t tids_size;
	int64_t targetWorkDurationNanos;
	uint32_t modes;
};

typedef struct APerformanceHintManager APerformanceHintManager;
typedef struct APerformanceHintSession APerformanceHintSession;
typedef struct AWorkDuration AWorkDuration;
typedef struct ASessionCreationConfig ASessionCreationConfig;

APerformanceHintManager *APerformanceHint_getManager(void)
{
	return NULL;
}

APerformanceHintSession *APerformanceHint_createSession(APerformanceHintManager *manager, const int32_t *threadIds, size_t size,
                                                        int64_t initialTargetWorkDurationNanos)
{
	VALIDATE_PTR(manager)
	VALIDATE_PTR(threadIds)
	return NULL;
}

int APerformanceHint_createSessionUsingConfig(APerformanceHintManager *manager, ASessionCreationConfig *sessionCreationConfig,
                                              APerformanceHintSession **sessionOut)
{
	VALIDATE_PTR(manager)
	VALIDATE_PTR(sessionCreationConfig)
	VALIDATE_PTR(sessionOut)
	*sessionOut = NULL;
	return EPIPE;
}

/* create(): a preferred rate <= 0 is reported as -1 */
int64_t APerformanceHint_getPreferredUpdateRateNanos(APerformanceHintManager *manager)
{
	VALIDATE_PTR(manager)
	return -1;
}

int APerformanceHint_getMaxGraphicsPipelineThreadsCount(APerformanceHintManager *manager)
{
	VALIDATE_PTR(manager)
	return 0;
}

int APerformanceHint_updateTargetWorkDuration(APerformanceHintSession *session, int64_t targetDurationNanos)
{
	VALIDATE_PTR(session)
	VALIDATE_INT(targetDurationNanos, >= 0)
	return EPIPE;
}

int APerformanceHint_reportActualWorkDuration(APerformanceHintSession *session, int64_t actualDurationNanos)
{
	VALIDATE_PTR(session)
	VALIDATE_INT(actualDurationNanos, > 0)
	return EPIPE;
}

void APerformanceHint_closeSession(APerformanceHintSession *session)
{
	VALIDATE_PTR(session)
}

int APerformanceHint_setThreads(APerformanceHintSession *session, const pid_t *threadIds, size_t size)
{
	VALIDATE_PTR(session)
	VALIDATE_PTR(threadIds)
	if (size == 0)
		return EINVAL;
	return EPIPE;
}

int APerformanceHint_setPreferPowerEfficiency(APerformanceHintSession *session, bool enabled)
{
	VALIDATE_PTR(session)
	return EPIPE;
}

int APerformanceHint_reportActualWorkDuration2(APerformanceHintSession *session, AWorkDuration *workDurationPtr)
{
	VALIDATE_PTR(session)
	VALIDATE_PTR(workDurationPtr)
	VALIDATE_INT(workDurationPtr->durationNanos, > 0)
	VALIDATE_INT(workDurationPtr->workPeriodStartTimestampNanos, > 0)
	VALIDATE_INT(workDurationPtr->cpuDurationNanos, >= 0)
	VALIDATE_INT(workDurationPtr->gpuDurationNanos, >= 0)
	VALIDATE_INT(workDurationPtr->gpuDurationNanos + workDurationPtr->cpuDurationNanos, > 0)
	return EPIPE;
}

int APerformanceHint_notifyWorkloadIncrease(APerformanceHintSession *session, bool cpu, bool gpu, const char *debugName)
{
	VALIDATE_PTR(session)
	VALIDATE_PTR(debugName)
	return EPIPE;
}

int APerformanceHint_notifyWorkloadReset(APerformanceHintSession *session, bool cpu, bool gpu, const char *debugName)
{
	VALIDATE_PTR(session)
	VALIDATE_PTR(debugName)
	return EPIPE;
}

int APerformanceHint_notifyWorkloadSpike(APerformanceHintSession *session, bool cpu, bool gpu, const char *debugName)
{
	VALIDATE_PTR(session)
	VALIDATE_PTR(debugName)
	return EPIPE;
}

int APerformanceHint_setNativeSurfaces(APerformanceHintSession *session, void **nativeWindows, size_t nativeWindowsSize,
                                       void **surfaceControls, size_t surfaceControlsSize)
{
	VALIDATE_PTR(session)
	return EPIPE;
}

/* without a manager "clearly whatever it is isn't supported" */
bool APerformanceHint_isFeatureSupported(int32_t feature)
{
	return false;
}

/* getSessionFromJava() on the NULL instance: ATL's Java PerformanceHintManager never creates a Session */
APerformanceHintSession *APerformanceHint_borrowSessionFromJava(JNIEnv *env, jobject sessionObj)
{
	VALIDATE_PTR(env)
	VALIDATE_PTR(sessionObj)
	fatal(__func__, "no PerformanceHintManager instance");
	return NULL;
}

AWorkDuration *AWorkDuration_create(void)
{
	AWorkDuration *workDuration = calloc(1, sizeof(AWorkDuration));
	if (!workDuration)
		abort();
	return workDuration;
}

void AWorkDuration_release(AWorkDuration *aWorkDuration)
{
	VALIDATE_PTR(aWorkDuration)
	free(aWorkDuration);
}

/* the setters only log (WARN_INT) an out-of-range value and store it */
void AWorkDuration_setActualTotalDurationNanos(AWorkDuration *aWorkDuration, int64_t actualTotalDurationNanos)
{
	VALIDATE_PTR(aWorkDuration)
	aWorkDuration->durationNanos = actualTotalDurationNanos;
}

void AWorkDuration_setWorkPeriodStartTimestampNanos(AWorkDuration *aWorkDuration, int64_t workPeriodStartTimestampNanos)
{
	VALIDATE_PTR(aWorkDuration)
	aWorkDuration->workPeriodStartTimestampNanos = workPeriodStartTimestampNanos;
}

void AWorkDuration_setActualCpuDurationNanos(AWorkDuration *aWorkDuration, int64_t actualCpuDurationNanos)
{
	VALIDATE_PTR(aWorkDuration)
	aWorkDuration->cpuDurationNanos = actualCpuDurationNanos;
}

void AWorkDuration_setActualGpuDurationNanos(AWorkDuration *aWorkDuration, int64_t actualGpuDurationNanos)
{
	VALIDATE_PTR(aWorkDuration)
	aWorkDuration->gpuDurationNanos = actualGpuDurationNanos;
}

ASessionCreationConfig *ASessionCreationConfig_create(void)
{
	ASessionCreationConfig *config = calloc(1, sizeof(ASessionCreationConfig));
	if (!config)
		abort();
	return config;
}

void ASessionCreationConfig_release(ASessionCreationConfig *config)
{
	VALIDATE_PTR(config)
	free(config->tids);
	free(config);
}

void ASessionCreationConfig_setTids(ASessionCreationConfig *config, const pid_t *tids, size_t size)
{
	VALIDATE_PTR(config)
	VALIDATE_PTR(tids)
	if (size == 0)
		fatal(__func__, "Invalid value. Check failed: (size > 0)");
	int32_t *copy = malloc(size * sizeof(int32_t));
	if (!copy)
		abort();
	memcpy(copy, tids, size * sizeof(int32_t));
	free(config->tids);
	config->tids = copy;
	config->tids_size = size;
}

void ASessionCreationConfig_setTargetWorkDurationNanos(ASessionCreationConfig *config, int64_t targetWorkDurationNanos)
{
	VALIDATE_PTR(config)
	config->targetWorkDurationNanos = targetWorkDurationNanos;
}

static void set_mode(ASessionCreationConfig *config, uint32_t mode, bool enabled)
{
	if (enabled)
		config->modes |= mode;
	else
		config->modes &= ~mode;
}

void ASessionCreationConfig_setPreferPowerEfficiency(ASessionCreationConfig *config, bool enabled)
{
	VALIDATE_PTR(config)
	set_mode(config, MODE_POWER_EFFICIENCY, enabled);
}

void ASessionCreationConfig_setGraphicsPipeline(ASessionCreationConfig *config, bool enabled)
{
	VALIDATE_PTR(config)
	set_mode(config, MODE_GRAPHICS_PIPELINE, enabled);
}

void ASessionCreationConfig_setNativeSurfaces(ASessionCreationConfig *config, void **nativeWindows, size_t nativeWindowsSize,
                                              void **surfaceControls, size_t surfaceControlsSize)
{
	VALIDATE_PTR(config)
}

void ASessionCreationConfig_setUseAutoTiming(ASessionCreationConfig *config, bool cpu, bool gpu)
{
	VALIDATE_PTR(config)
	set_mode(config, MODE_AUTO_CPU, cpu);
	set_mode(config, MODE_AUTO_GPU, gpu);
}
