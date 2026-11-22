/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* AIBinder and AStatus: frameworks/native/libs/binder/ndk/ibinder.cpp, ibinder_jni.cpp, parcel_jni.cpp and
 * status.cpp (android-16.0.0_r4). ATL has no binder driver, so every AIBinder is a local binder (ABBinder):
 * a transaction runs onTransact() in the calling thread as BBinder::transact() does, with RefBase's strong and
 * weak counts. */

#define _GNU_SOURCE /* asprintf */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <jni.h>

#include "binder_ndk.h"
#include "ndk_jni.h"

#define FIRST_CALL_TRANSACTION 0x00000001
#define LAST_CALL_TRANSACTION 0x00ffffff
#define FLAG_ONEWAY 0x01
#define FLAG_CLEAR_BUF 0x20
#define FLAG_PRIVATE_VENDOR 0x10000000
#define STRICT_MODE_PENALTY_GATHER (1u << 31)
#define UNSET_WORK_SOURCE (-1)
#define INTERFACE_HEADER (('S' << 24) | ('Y' << 16) | ('S' << 8) | 'T') /* Parcel.cpp kHeader */

void AParcel_delete(AParcel *parcel);

typedef void *(*AIBinder_Class_onCreate)(void *args);
typedef void (*AIBinder_Class_onDestroy)(void *userData);
typedef binder_status_t (*AIBinder_Class_onTransact)(AIBinder *binder, transaction_code_t code, const AParcel *in, AParcel *out);
typedef binder_status_t (*AIBinder_onDump)(AIBinder *binder, int fd, const char **args, uint32_t numArgs);
typedef void (*AIBinder_DeathRecipient_onBinderDied)(void *cookie);
typedef void (*AIBinder_DeathRecipient_onBinderUnlinked)(void *cookie);

struct AIBinder_Class {
	AIBinder_Class_onCreate on_create;
	AIBinder_Class_onDestroy on_destroy;
	AIBinder_Class_onTransact on_transact;
	AIBinder_onDump on_dump;
	bool write_header;
	const char *const *code_map;
	size_t code_map_length;
	char *descriptor;
	char16_t *descriptor16;
	size_t descriptor16_length;
};

/* wp<AIBinder> */
typedef struct AIBinder_Weak {
	struct binder_refs *refs;
	AIBinder *binder;
} AIBinder_Weak;

typedef struct AIBinder_DeathRecipient {
	AIBinder_DeathRecipient_onBinderDied on_died;
	AIBinder_DeathRecipient_onBinderUnlinked on_unlinked;
} AIBinder_DeathRecipient;

/* --- AIBinder_Class --- */

AIBinder_Class *AIBinder_Class_define(const char *interfaceDescriptor, AIBinder_Class_onCreate onCreate,
                                      AIBinder_Class_onDestroy onDestroy, AIBinder_Class_onTransact onTransact)
{
	if (!interfaceDescriptor || !onCreate || !onDestroy || !onTransact)
		return NULL;
	AIBinder_Class *clazz = calloc(1, sizeof(AIBinder_Class));
	if (!clazz)
		return NULL;
	clazz->on_create = onCreate;
	clazz->on_destroy = onDestroy;
	clazz->on_transact = onTransact;
	clazz->write_header = true;
	clazz->descriptor = strdup(interfaceDescriptor);
	clazz->descriptor16 = binder_utf16_from_utf8(interfaceDescriptor, &clazz->descriptor16_length);
	return clazz;
}

/* the LOG_ALWAYS_FATAL_IF() checks of ibinder.cpp */
static void fatal_if(bool condition, const char *message)
{
	if (condition) {
		fprintf(stderr, "libbinder_ndk: %s\n", message);
		abort();
	}
}

void AIBinder_Class_setOnDump(AIBinder_Class *clazz, AIBinder_onDump onDump)
{
	fatal_if(!clazz, "setOnDump requires non-null clazz");
	clazz->on_dump = onDump;
}

void AIBinder_Class_setTransactionCodeToFunctionNameMap(AIBinder_Class *clazz, const char *const *transactionCodeToFunction, size_t length)
{
	fatal_if(!clazz || !transactionCodeToFunction, "Valid clazz and transactionCodeToFunction are needed to set code to function mapping.");
	fatal_if(clazz->code_map != NULL, "Failed to set transactionCodeToFunction to clazz! Is transactionCodeToFunction already set?");
	clazz->code_map = transactionCodeToFunction;
	clazz->code_map_length = length;
}

const char *AIBinder_Class_getFunctionName(AIBinder_Class *clazz, transaction_code_t code)
{
	fatal_if(!clazz, "Valid clazz is needed to get function name for requested transaction code");
	if (!clazz->code_map || code < FIRST_CALL_TRANSACTION || code - FIRST_CALL_TRANSACTION >= clazz->code_map_length)
		return NULL;
	return clazz->code_map[code - FIRST_CALL_TRANSACTION];
}

void AIBinder_Class_disableInterfaceTokenHeader(AIBinder_Class *clazz)
{
	fatal_if(!clazz, "disableInterfaceTokenHeader requires non-null clazz");
	clazz->write_header = false;
}

const char *AIBinder_Class_getDescriptor(const AIBinder_Class *clazz)
{
	fatal_if(!clazz, "getDescriptor requires non-null clazz");
	return clazz->descriptor;
}

/* --- AIBinder: ABBinder with RefBase counting (the object goes with the last strong reference, the
 * weakref_impl with the last weak one; every strong reference holds a weak one) --- */

AIBinder *AIBinder_new(const AIBinder_Class *clazz, void *args)
{
	if (!clazz)
		return NULL;
	AIBinder *binder = calloc(1, sizeof(AIBinder));
	struct binder_refs *refs = malloc(sizeof(struct binder_refs));
	if (!binder || !refs) {
		free(binder);
		free(refs);
		return NULL;
	}
	atomic_init(&refs->strong, 1);
	atomic_init(&refs->weak, 1);
	binder->refs = refs;
	binder->clazz = clazz;
	binder->user_data = clazz->on_create(args);
	return binder;
}

static void dec_weak(struct binder_refs *refs)
{
	if (atomic_fetch_sub(&refs->weak, 1) == 1)
		free(refs);
}

void AIBinder_incStrong(AIBinder *binder)
{
	if (!binder)
		return;
	atomic_fetch_add(&binder->refs->weak, 1);
	atomic_fetch_add(&binder->refs->strong, 1);
}

void AIBinder_decStrong(AIBinder *binder)
{
	if (!binder)
		return;
	struct binder_refs *refs = binder->refs;
	if (atomic_fetch_sub(&refs->strong, 1) == 1) { /* ~ABBinder, then ~BBinder releases the extension */
		binder->clazz->on_destroy(binder->user_data);
		AIBinder_decStrong(binder->ext);
		free(binder);
	}
	dec_weak(refs);
}

int32_t AIBinder_debugGetRefCount(AIBinder *binder)
{
	if (!binder)
		return -1;
	return atomic_load(&binder->refs->strong);
}

bool AIBinder_isRemote(const AIBinder *binder)
{
	return false;
}

/* BBinder::isBinderAlive() */
bool AIBinder_isAlive(const AIBinder *binder)
{
	return binder != NULL;
}

/* BBinder::pingBinder() */
binder_status_t AIBinder_ping(AIBinder *binder)
{
	return binder ? STATUS_OK : STATUS_UNEXPECTED_NULL;
}

binder_status_t AIBinder_dump(AIBinder *binder, int fd, const char **args, uint32_t numArgs)
{
	if (!binder)
		return STATUS_UNEXPECTED_NULL;
	if (!binder->clazz->on_dump)
		return STATUS_OK;
	return binder_prune_status(binder->clazz->on_dump(binder, fd, args, numArgs));
}

/* a local binder never has a class other than its own: AIBinder::associateClass() */
bool AIBinder_associateClass(AIBinder *binder, const AIBinder_Class *clazz)
{
	return binder && clazz && binder->clazz == clazz;
}

const AIBinder_Class *AIBinder_getClass(AIBinder *binder)
{
	return binder ? binder->clazz : NULL;
}

void *AIBinder_getUserData(AIBinder *binder)
{
	return binder ? binder->user_data : NULL;
}

bool AIBinder_lt(const AIBinder *lhs, const AIBinder *rhs)
{
	return lhs < rhs;
}

/* BBinder::getExtension() */
binder_status_t AIBinder_getExtension(AIBinder *binder, AIBinder **outExt)
{
	if (!binder || !outExt) {
		if (outExt)
			*outExt = NULL;
		return STATUS_UNEXPECTED_NULL;
	}
	AIBinder_incStrong(binder->ext);
	*outExt = binder->ext;
	return STATUS_OK;
}

binder_status_t AIBinder_setExtension(AIBinder *binder, AIBinder *ext)
{
	if (!binder || !ext)
		return STATUS_UNEXPECTED_NULL;
	fatal_if(binder->parceled, "setExtension() should not be called after a binder object is parceled/sent to another process");
	AIBinder_incStrong(ext);
	AIBinder_decStrong(binder->ext);
	binder->ext = ext;
	return STATUS_OK;
}

/* --- AIBinder_Weak --- */

AIBinder_Weak *AIBinder_Weak_new(AIBinder *binder)
{
	if (!binder)
		return NULL;
	AIBinder_Weak *weak = malloc(sizeof(AIBinder_Weak));
	if (!weak)
		return NULL;
	atomic_fetch_add(&binder->refs->weak, 1);
	*weak = (AIBinder_Weak){binder->refs, binder};
	return weak;
}

void AIBinder_Weak_delete(AIBinder_Weak *weakBinder)
{
	if (!weakBinder)
		return;
	dec_weak(weakBinder->refs);
	free(weakBinder);
}

/* RefBase::weakref_type::attemptIncStrong(): fails once the last strong reference is gone */
AIBinder *AIBinder_Weak_promote(AIBinder_Weak *weakBinder)
{
	if (!weakBinder)
		return NULL;
	int strong = atomic_load(&weakBinder->refs->strong);
	while (strong > 0) {
		if (atomic_compare_exchange_weak(&weakBinder->refs->strong, &strong, strong + 1)) {
			atomic_fetch_add(&weakBinder->refs->weak, 1);
			return weakBinder->binder;
		}
	}
	return NULL;
}

AIBinder_Weak *AIBinder_Weak_clone(const AIBinder_Weak *weak)
{
	if (!weak)
		return NULL;
	AIBinder_Weak *clone = malloc(sizeof(AIBinder_Weak));
	if (!clone)
		return NULL;
	atomic_fetch_add(&weak->refs->weak, 1);
	*clone = *weak;
	return clone;
}

/* wp<T>::operator< */
bool AIBinder_Weak_lt(const AIBinder_Weak *lhs, const AIBinder_Weak *rhs)
{
	if (!lhs || !rhs)
		return lhs < rhs;
	if (lhs->binder == rhs->binder)
		return lhs->refs < rhs->refs;
	return lhs->binder < rhs->binder;
}

/* --- death recipients: a local binder cannot die. AIBinder_DeathRecipient::linkToDeath() gets
 * INVALID_OPERATION from BBinder::linkToDeath(); the transfer recipient it made calls onUnlinked when it goes. --- */

AIBinder_DeathRecipient *AIBinder_DeathRecipient_new(AIBinder_DeathRecipient_onBinderDied onBinderDied)
{
	if (!onBinderDied)
		return NULL;
	AIBinder_DeathRecipient *recipient = calloc(1, sizeof(AIBinder_DeathRecipient));
	if (recipient)
		recipient->on_died = onBinderDied;
	return recipient;
}

void AIBinder_DeathRecipient_setOnUnlinked(AIBinder_DeathRecipient *recipient, AIBinder_DeathRecipient_onBinderUnlinked onUnlinked)
{
	if (recipient)
		recipient->on_unlinked = onUnlinked;
}

void AIBinder_DeathRecipient_delete(AIBinder_DeathRecipient *recipient)
{
	free(recipient);
}

binder_status_t AIBinder_linkToDeath(AIBinder *binder, AIBinder_DeathRecipient *recipient, void *cookie)
{
	if (!binder || !recipient)
		return STATUS_UNEXPECTED_NULL;
	if (recipient->on_unlinked)
		recipient->on_unlinked(cookie);
	return STATUS_INVALID_OPERATION;
}

binder_status_t AIBinder_unlinkToDeath(AIBinder *binder, AIBinder_DeathRecipient *recipient, void *cookie)
{
	if (!binder || !recipient)
		return STATUS_UNEXPECTED_NULL;
	return STATUS_NAME_NOT_FOUND;
}

/* --- calling identity: IPCThreadState keeps the caller of the transaction the thread serves, else this
 * process; in ATL that is android.os.Binder (ATLBinderConnection for transactions from another process) --- */

static jint binder_java_int(const char *method, jint fallback)
{
	JNIEnv *env = ndk_jni_env();
	if (!env)
		return fallback;
	jclass binder = ndk_jni_class(env, "android/os/Binder");
	jmethodID id = binder ? (*env)->GetStaticMethodID(env, binder, method, "()I") : NULL;
	if (!id) {
		ndk_jni_threw(env, method);
		return fallback;
	}
	jint ret = (*env)->CallStaticIntMethod(env, binder, id);
	return ndk_jni_threw(env, method) ? fallback : ret;
}

uid_t AIBinder_getCallingUid(void)
{
	return binder_java_int("getCallingUid", getuid());
}

pid_t AIBinder_getCallingPid(void)
{
	return binder_java_int("getCallingPid", getpid());
}

/* IPCThreadState::getServingStackPointer(): set while the thread serves a transaction of another process */
bool AIBinder_isHandlingTransaction(void)
{
	return AIBinder_getCallingPid() != getpid();
}

/* --- transactions --- */

/* Parcel::writeInterfaceToken(): strict mode policy, work source, header, descriptor */
binder_status_t AIBinder_prepareTransaction(AIBinder *binder, AParcel **in)
{
	if (!binder || !in)
		return STATUS_UNEXPECTED_NULL;
	AParcel *p = binder_parcel_new(binder);
	if (!p)
		return STATUS_NO_MEMORY;
	int32_t err = STATUS_OK;
	if (binder->clazz->write_header) {
		binder_parcel_write_i32(p, STRICT_MODE_PENALTY_GATHER);
		binder_parcel_write_i32(p, UNSET_WORK_SOURCE);
		binder_parcel_write_i32(p, INTERFACE_HEADER);
		err = binder_parcel_write_string16(p, binder->clazz->descriptor16, binder->clazz->descriptor16_length);
	}
	if (err != STATUS_OK) {
		AParcel_delete(p);
		p = NULL;
	}
	*in = p;
	return binder_prune_status(err);
}

/* Parcel::enforceInterface() */
static bool check_interface(AParcel *data, const AIBinder_Class *clazz)
{
	int32_t v[3] = {0};
	for (int i = 0; i < 3; i++)
		if (binder_parcel_read_i32(data, &v[i]) != STATUS_OK)
			v[i] = 0;
	if (v[2] != INTERFACE_HEADER)
		return false;
	size_t len;
	const char16_t *s = binder_parcel_read_string16_inplace(data, &len);
	return len == clazz->descriptor16_length && (!len || !memcmp(s, clazz->descriptor16, len * sizeof(char16_t)));
}

/* AIBinder_transact() -> BBinder::transact() -> ABBinder::onTransact() */
binder_status_t AIBinder_transact(AIBinder *binder, transaction_code_t code, AParcel **in, AParcel **out, binder_flags_t flags)
{
	if (!in)
		return STATUS_UNEXPECTED_NULL;
	AParcel *data = *in; /* owned from here on */
	*in = NULL;
	binder_status_t ret;
	if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION)
		ret = STATUS_UNKNOWN_TRANSACTION;
	else if (flags & ~(FLAG_PRIVATE_VENDOR | FLAG_ONEWAY | FLAG_CLEAR_BUF))
		ret = STATUS_BAD_VALUE;
	else if (!binder || !data || !out)
		ret = STATUS_UNEXPECTED_NULL;
	else if (data->binder != binder)
		ret = STATUS_BAD_VALUE;
	else if (!(*out = binder_parcel_new(binder)))
		ret = STATUS_NO_MEMORY;
	else {
		data->pos = 0;
		if (binder->clazz->write_header && !check_interface(data, binder->clazz))
			ret = STATUS_BAD_TYPE;
		else
			ret = binder_prune_status(binder->clazz->on_transact(binder, code, data, *out));
		(*out)->pos = 0;
		if (ret != STATUS_OK) {
			AParcel_delete(*out);
			*out = NULL;
		}
	}
	AParcel_delete(data);
	return ret;
}

/* --- JNI: ATL's android.os.Binder, BinderProxy and Parcel are Java only --- */

/* ibinderForJavaObject() (android_util_Binder.cpp) gives NULL for an object without native binder */
AIBinder *AIBinder_fromJavaBinder(JNIEnv *env, jobject binder)
{
	return NULL;
}

/* javaObjectForIBinder() would wrap the native binder in a BinderProxy; ATL's BinderProxy can only stand
 * for a binder of another ATL process (ATLBinderConnection) */
jobject AIBinder_toJavaBinder(JNIEnv *env, AIBinder *binder)
{
	return NULL;
}

/* parcelForJavaObject() (android_os_Parcel.cpp): a Parcel without native parcel */
AParcel *AParcel_fromJavaParcel(JNIEnv *env, jobject parcel)
{
	if (parcel)
		(*env)->ThrowNew(env, ndk_jni_class(env, "java/lang/IllegalStateException"), "Parcel has been finalized!");
	return NULL;
}

/* --- AStatus: status.cpp and binder/Status.cpp --- */

static const struct {
	int32_t status;
	const char *name;
} status_names[] = {
	{STATUS_OK, "OK"},
	{STATUS_UNKNOWN_ERROR, "UNKNOWN_ERROR"},
	{STATUS_NO_MEMORY, "NO_MEMORY"},
	{STATUS_INVALID_OPERATION, "INVALID_OPERATION"},
	{STATUS_BAD_VALUE, "BAD_VALUE"},
	{STATUS_BAD_TYPE, "BAD_TYPE"},
	{STATUS_NAME_NOT_FOUND, "NAME_NOT_FOUND"},
	{STATUS_PERMISSION_DENIED, "PERMISSION_DENIED"},
	{STATUS_NO_INIT, "NO_INIT"},
	{STATUS_ALREADY_EXISTS, "ALREADY_EXISTS"},
	{STATUS_DEAD_OBJECT, "DEAD_OBJECT"},
	{STATUS_FAILED_TRANSACTION, "FAILED_TRANSACTION"},
	{STATUS_BAD_INDEX, "BAD_INDEX"},
	{STATUS_NOT_ENOUGH_DATA, "NOT_ENOUGH_DATA"},
	{STATUS_WOULD_BLOCK, "WOULD_BLOCK"},
	{STATUS_TIMED_OUT, "TIMED_OUT"},
	{STATUS_UNKNOWN_TRANSACTION, "UNKNOWN_TRANSACTION"},
	{STATUS_FDS_NOT_ALLOWED, "FDS_NOT_ALLOWED"},
	{STATUS_UNEXPECTED_NULL, "UNEXPECTED_NULL"},
};

static const char *status_name(int32_t status)
{
	for (size_t i = 0; i < sizeof(status_names) / sizeof(status_names[0]); i++)
		if (status_names[i].status == status)
			return status_names[i].name;
	return NULL;
}

/* PruneStatusT() */
binder_status_t binder_prune_status(int32_t status)
{
	return status_name(status) ? status : STATUS_UNKNOWN_ERROR;
}

static const struct {
	int32_t exception;
	const char *name;
} exception_names[] = {
	{0, "EX_NONE"}, {-1, "EX_SECURITY"}, {-2, "EX_BAD_PARCELABLE"}, {-3, "EX_ILLEGAL_ARGUMENT"},
	{-4, "EX_NULL_POINTER"}, {-5, "EX_ILLEGAL_STATE"}, {-6, "EX_NETWORK_MAIN_THREAD"},
	{-7, "EX_UNSUPPORTED_OPERATION"}, {-8, "EX_SERVICE_SPECIFIC"}, {-9, "EX_PARCELABLE"},
	{EX_TRANSACTION_FAILED, "EX_TRANSACTION_FAILED"}, {EX_HAS_REPLY_HEADER, "EX_HAS_REPLY_HEADER"},
};

/* PruneException(): EX_HAS_REPLY_HEADER (the last entry) is not an NDK exception */
static binder_exception_t prune_exception(int32_t exception)
{
	for (size_t i = 0; i < sizeof(exception_names) / sizeof(exception_names[0]) - 1; i++)
		if (exception_names[i].exception == exception)
			return exception;
	return EX_TRANSACTION_FAILED;
}

static AStatus *status_new(int32_t exception, int32_t error, const char *message)
{
	AStatus *status = malloc(sizeof(AStatus));
	if (status)
		*status = (AStatus){exception, error, message ? strdup(message) : NULL};
	return status;
}

AStatus *AStatus_newOk(void)
{
	static AStatus ok;
	return &ok;
}

/* Status::fromExceptionCode() */
AStatus *AStatus_fromExceptionCodeWithMessage(binder_exception_t exception, const char *message)
{
	exception = prune_exception(exception);
	return status_new(exception, exception == EX_TRANSACTION_FAILED ? STATUS_FAILED_TRANSACTION : STATUS_OK, message);
}

AStatus *AStatus_fromExceptionCode(binder_exception_t exception)
{
	return AStatus_fromExceptionCodeWithMessage(exception, NULL);
}

AStatus *AStatus_fromServiceSpecificErrorWithMessage(int32_t serviceSpecific, const char *message)
{
	return status_new(EX_SERVICE_SPECIFIC, serviceSpecific, message);
}

AStatus *AStatus_fromServiceSpecificError(int32_t serviceSpecific)
{
	return status_new(EX_SERVICE_SPECIFIC, serviceSpecific, NULL);
}

/* Status::fromStatusT() */
AStatus *AStatus_fromStatus(binder_status_t status)
{
	status = binder_prune_status(status);
	return status_new(status == STATUS_OK ? EX_NONE : EX_TRANSACTION_FAILED, status, NULL);
}

bool AStatus_isOk(const AStatus *status)
{
	return status->exception == EX_NONE;
}

binder_exception_t AStatus_getExceptionCode(const AStatus *status)
{
	return prune_exception(status->exception);
}

int32_t AStatus_getServiceSpecificError(const AStatus *status)
{
	return status->exception == EX_SERVICE_SPECIFIC ? status->error : 0;
}

binder_status_t AStatus_getStatus(const AStatus *status)
{
	return binder_prune_status(status->exception == EX_TRANSACTION_FAILED ? status->error : STATUS_OK);
}

const char *AStatus_getMessage(const AStatus *status)
{
	return status->message ? status->message : "";
}

/* Status::toString8() with exceptionToString() and statusToString() */
const char *AStatus_getDescription(const AStatus *status)
{
	char *ret = NULL, ex[16], detail[128] = "";
	if (status->exception == EX_NONE)
		return strdup("No error");
	const char *ex_name = NULL;
	for (size_t i = 0; i < sizeof(exception_names) / sizeof(exception_names[0]); i++)
		if (exception_names[i].exception == status->exception)
			ex_name = exception_names[i].name;
	snprintf(ex, sizeof(ex), "%d", status->exception);
	if (status->exception == EX_SERVICE_SPECIFIC) {
		snprintf(detail, sizeof(detail), "%d: ", status->error);
	} else if (status->exception == EX_TRANSACTION_FAILED) {
		const char *name = status_name(status->error);
		if (name)
			snprintf(detail, sizeof(detail), "%s: ", name);
		else
			snprintf(detail, sizeof(detail), "%d (%s): ", status->error, strerror(-status->error));
	}
	if (asprintf(&ret, "Status(%d, %s): '%s%s'", status->exception, ex_name ? ex_name : ex, detail, AStatus_getMessage(status)) < 0)
		return NULL;
	return ret;
}

void AStatus_deleteDescription(const char *description)
{
	free((char *)description);
}

void AStatus_delete(AStatus *status)
{
	if (!status || status == AStatus_newOk())
		return;
	free(status->message);
	free(status);
}
