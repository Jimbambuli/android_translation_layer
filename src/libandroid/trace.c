#include <stdbool.h>
#include <stdint.h>

/* NDK trace.h (AOSP frameworks/base/native/android/trace.cpp): the sections and counters go to atrace only while
 * the app tag is traced; nothing traces the app here, so ATrace_isEnabled() is false and the rest does nothing */

bool ATrace_isEnabled()
{
	return false;
}

void ATrace_beginSection(const char *sectionName)
{
}

void ATrace_endSection(void)
{
}

void ATrace_beginAsyncSection(const char *sectionName, int32_t cookie)
{
}

void ATrace_endAsyncSection(const char *sectionName, int32_t cookie)
{
}

void ATrace_setCounter(const char *counterName, int64_t counterValue)
{
}
