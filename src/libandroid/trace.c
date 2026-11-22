#include <stdbool.h>
#include <stdint.h>

/* AOSP native/android/trace.cpp: every call goes to atrace for ATRACE_TAG_APP. ATL has no trace
 * daemon, so that tag is never enabled and atrace writes nothing. */
bool ATrace_isEnabled()
{
	return false;
}

void ATrace_beginSection(const char *sectionName) {}
void ATrace_endSection() {}
void ATrace_beginAsyncSection(const char *sectionName, int32_t cookie) {}
void ATrace_endAsyncSection(const char *sectionName, int32_t cookie) {}
void ATrace_setCounter(const char *counterName, int64_t counterValue) {}
