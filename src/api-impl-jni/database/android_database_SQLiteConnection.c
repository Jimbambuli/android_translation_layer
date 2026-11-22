/*
 * Copyright (C) 2011 The Android Open Source Project
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
/*
** Modified to support SQLite extensions by the SQLite developers:
** sqlite-dev@sqlite.org.
*/
/*
** Rewritten from C++ to C for Android Translation Layer:
*/

#include <assert.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <sqlite3.h>
#include <unicode/ucol.h>
#include <unicode/uiter.h>
#include <unicode/utypes.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_database_sqlite_SQLiteConnection.h"
#include "android_database_SQLiteCommon.h"
#include "sqlite3_android_phone.h"

enum {
	// Open flags.
	// Must be kept in sync with the constants defined in SQLiteDatabase.java.
	OPEN_READWRITE = 0x00000000,
	OPEN_READONLY = 0x00000001,
	OPEN_READ_MASK = 0x00000001,
	NO_LOCALIZED_COLLATORS = 0x00000010,
	CREATE_IF_NECESSARY = 0x10000000,
};

struct SQLiteConnection {
	sqlite3 *db;
	int openFlags;
	char *path;
	char *label;

	volatile bool canceled;
};

/* Busy timeout in milliseconds.
 * If another connection (possibly in another process) has the database locked for
 * longer than this amount of time then SQLite will generate a SQLITE_BUSY error.
 * The SQLITE_BUSY error is then raised as a SQLiteDatabaseLockedException.
 *
 * In ordinary usage, busy timeouts are quite rare.  Most databases only ever
 * have a single open connection at a time unless they are using WAL.  When using
 * WAL, a timeout could occur if one connection is busy performing an auto-checkpoint
 * operation.  The busy timeout needs to be long enough to tolerate slow I/O write
 * operations but not so long as to cause the application to hang indefinitely if
 * there is a problem acquiring a database lock.
 */
static const int BUSY_TIMEOUT_MS = 2500;

/*
** Collations as in AOSP external/sqlite/android/sqlite3_android.cpp (android-14): collate8 compares with an ICU UCollator,
** localized_collator_dtor closes it, register_localized_collators() registers "LOCALIZED" and "PHONEBOOK" for the locale
** given by SQLiteConnection.setLocaleFromConfiguration(), register_android_functions() registers "UNICODE" for the default
** ICU locale. Rewritten from C++ to C (the ICU C API is used as it is); the storage is UTF-8 (UTF16_STORAGE is 0 in
** android_database_SQLiteConnection.cpp).
*/
static int collate8(void *p, int n1, const void *v1, int n2, const void *v2)
{
	UCollator *coll = (UCollator *)p;
	UCharIterator i1, i2;
	UErrorCode status = U_ZERO_ERROR;

	uiter_setUTF8(&i1, (const char *)v1, n1);
	uiter_setUTF8(&i2, (const char *)v2, n2);

	UCollationResult result = ucol_strcollIter(coll, &i1, &i2, &status);

	if (result == UCOL_LESS) {
		return -1;
	} else if (result == UCOL_GREATER) {
		return 1;
	} else {
		return 0;
	}
}

static void localized_collator_dtor(void *collator)
{
	ucol_close((UCollator *)collator);
}

#define LOCALIZED_COLLATOR_NAME "LOCALIZED"
#define PHONEBOOK_COLLATOR_NAME "PHONEBOOK"

static int register_localized_collators(sqlite3 *handle, const char *systemLocale)
{
	static const char *const names[] = { LOCALIZED_COLLATOR_NAME, PHONEBOOK_COLLATOR_NAME };

	for (int i = 0; i < 2; i++) {
		UErrorCode status = U_ZERO_ERROR;
		UCollator *collator = ucol_open(systemLocale, &status);
		if (U_FAILURE(status)) {
			return -1;
		}

		ucol_setAttribute(collator, UCOL_STRENGTH, UCOL_PRIMARY, &status);
		if (U_FAILURE(status)) {
			ucol_close(collator);
			return -1;
		}

		int err = sqlite3_create_collation_v2(handle, names[i], SQLITE_UTF8, collator, collate8, localized_collator_dtor);
		if (err != SQLITE_OK) {
			return err;
		}
	}
	return SQLITE_OK;
}

static int register_android_functions(sqlite3 *handle)
{
	UErrorCode status = U_ZERO_ERROR;

	UCollator *collator = ucol_open(NULL, &status);
	if (U_FAILURE(status)) {
		return -1;
	}

	int err = sqlite3_create_collation_v2(handle, "UNICODE", SQLITE_UTF8, collator, collate8, localized_collator_dtor);
	if (err != SQLITE_OK)
		return err;
	/* sqlite3_android.cpp: PHONE_NUMBERS_EQUAL (telephony and contacts providers) */
	return register_phone_numbers_equal(handle);
}

// Called each time a statement begins execution, when tracing is enabled.
static void sqliteTraceCallback(void *data, const char *sql)
{
	// struct SQLiteConnection* connection = data;
	// printf(SQLITE_TRACE_TAG " %s: \"%s\"\n",
	// 		connection->label, sql);
}

// Called each time a statement finishes execution, when profiling is enabled.
static void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm)
{
	// struct SQLiteConnection* connection = data;
	// printf(SQLITE_PROFILE_TAG " %s: \"%s\" took %0.3f ms\n",
	// 		connection->label, sql, tm * 0.000001f);
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeOpen(JNIEnv *env, jclass clazz, jstring pathStr, jint openFlags,
                                                                                 jstring labelStr, jboolean enableTrace, jboolean enableProfile, jint lookasideSlotSize, jint lookasideSlotCount)
{
	int sqliteFlags;
	if (openFlags & CREATE_IF_NECESSARY) {
		sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
	} else if (openFlags & OPEN_READONLY) {
		sqliteFlags = SQLITE_OPEN_READONLY;
	} else {
		sqliteFlags = SQLITE_OPEN_READWRITE;
	}

	const char *pathChars = (*env)->GetStringUTFChars(env, pathStr, NULL);
	char *path = strdup(pathChars);
	(*env)->ReleaseStringUTFChars(env, pathStr, pathChars);

	const char *labelChars = (*env)->GetStringUTFChars(env, labelStr, NULL);
	char *label = strdup(labelChars);
	(*env)->ReleaseStringUTFChars(env, labelStr, labelChars);

	sqlite3 *db;
	int err = sqlite3_open_v2(path, &db, sqliteFlags, NULL);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_errcode(env, err, "Could not open database");
		return 0;
	}
	// Set the lookaside configuration (android_database_SQLiteConnection.cpp: nativeOpen, SQLiteDatabase.OpenParams)
	if (lookasideSlotSize >= 0 && lookasideSlotCount >= 0) {
		err = sqlite3_db_config(db, SQLITE_DBCONFIG_LOOKASIDE, NULL, lookasideSlotSize, lookasideSlotCount);
		if (err != SQLITE_OK) {
			throw_sqlite3_exception_handle_message(env, db, "Cannot set lookaside");
			sqlite3_close(db);
			return 0;
		}
	}
	err = register_android_functions(db);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_errcode(env, err, "Could not register collation");
		sqlite3_close(db);
		return 0;
	}

	// Check that the database is really read/write when that is what we asked for.
	if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
		throw_sqlite3_exception_handle_message(env, db, "Could not open the database in read/write mode.");
		sqlite3_close(db);
		return 0;
	}

	// Set the default busy handler to retry automatically before returning SQLITE_BUSY.
	err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, db, "Could not set busy timeout");
		sqlite3_close(db);
		return 0;
	}

	// Create wrapper object.
	struct SQLiteConnection *connection = malloc(sizeof(struct SQLiteConnection));
	connection->db = db;
	connection->openFlags = openFlags;
	connection->path = path;
	connection->label = label;

	// Enable tracing and profiling if requested.
	if (enableTrace) {
		sqlite3_trace(db, &sqliteTraceCallback, connection);
	}
	if (enableProfile) {
		sqlite3_profile(db, &sqliteProfileCallback, connection);
	}

	// printf("Opened connection %p with label '%s'\n", db, label);
	return _INTPTR(connection);
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeClose(JNIEnv *env, jclass clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);

	if (connection) {
		// printf("Closing connection %p", connection->db);
		int err = sqlite3_close(connection->db);
		if (err != SQLITE_OK) {
			// This can happen if sub-objects aren't closed first.  Make sure the caller knows.
			fprintf(stderr, "sqlite3_close(%p) failed: %d\n", connection->db, err);
			throw_sqlite3_exception_handle_message(env, connection->db, "Count not close db.");
			return;
		}

		free(connection->path);
		free(connection->label);
		free(connection);
	}
}

// Called each time a custom function is evaluated.
static void sqliteCustomFunctionCallback(sqlite3_context *context, int argc, sqlite3_value **argv)
{
	JNIEnv *env = get_jni_env();

	// Get the callback function object.
	// Create a new local reference to it in case the callback tries to do something
	// dumb like unregister the function (thereby destroying the global ref) while it is running.
	jobject functionObjGlobal = sqlite3_user_data(context);
	jobject functionObj = (*env)->NewLocalRef(env, functionObjGlobal);

	jobjectArray argsArray = (*env)->NewObjectArray(env, argc, (*env)->FindClass(env, "java/lang/String"), NULL);
	if (argsArray) {
		for (int i = 0; i < argc; i++) {
			const jchar *arg = sqlite3_value_text16(argv[i]);
			if (!arg) {
				fprintf(stderr, "NULL argument in custom_function_callback.  This should not happen.");
			} else {
				size_t argLen = sqlite3_value_bytes16(argv[i]) / sizeof(jchar);
				jstring argStr = (*env)->NewString(env, arg, argLen);
				if (!argStr) {
					goto error; // out of memory error
				}
				(*env)->SetObjectArrayElement(env, argsArray, i, argStr);
				(*env)->DeleteLocalRef(env, argStr);
			}
		}

		jclass custom_function_class = (*env)->FindClass(env, "android/database/sqlite/SQLiteCustomFunction");
		// TODO: Support functions that return values.
		(*env)->CallVoidMethod(env, functionObj,
		                       _METHOD(custom_function_class, "dispatchCallback", "([Ljava/lang/String;)V"), argsArray);

	error:
		(*env)->DeleteLocalRef(env, argsArray);
	}

	(*env)->DeleteLocalRef(env, functionObj);

	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "An exception was thrown by custom SQLite function.");
		/* LOGE_EX(env); */
		(*env)->ExceptionClear(env);
	}
}

// Called when a custom function is destroyed.
static void sqliteCustomFunctionDestructor(void *data)
{
	jobject functionObjGlobal = data;
	JNIEnv *env = get_jni_env();
	(*env)->DeleteGlobalRef(env, functionObjGlobal);
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeRegisterCustomFunction(JNIEnv *env, jclass clazz, jlong connectionPtr, jobject functionObj)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);

	jclass custom_function_class = (*env)->FindClass(env, "android/database/sqlite/SQLiteCustomFunction");

	jstring nameStr = (*env)->GetObjectField(env, functionObj, _FIELD_ID(custom_function_class, "name", "Ljava/lang/String;"));
	jint numArgs = (*env)->GetIntField(env, functionObj, _FIELD_ID(custom_function_class, "numArgs", "I"));

	jobject functionObjGlobal = (*env)->NewGlobalRef(env, functionObj);

	const char *name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	int err = sqlite3_create_function_v2(connection->db, name, numArgs, SQLITE_UTF16, functionObjGlobal,
	                                     &sqliteCustomFunctionCallback, NULL, NULL, &sqliteCustomFunctionDestructor);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);

	if (err != SQLITE_OK) {
		fprintf(stderr, "sqlite3_create_function returned %d", err);
		/* SQLite has called the destructor already, which deleted functionObjGlobal */
		throw_sqlite3_exception_handle(env, connection->db);
		return;
	}
}

/* android_database_SQLiteConnection.cpp: custom scalar/aggregate functions of SQLiteDatabase.setCustomScalarFunction()
 * and setCustomAggregateFunction(): java.util.function.UnaryOperator<String> / BinaryOperator<String> on text arguments */
static void sqliteCustomScalarFunctionCallback(sqlite3_context *context, int argc, sqlite3_value **argv)
{
	JNIEnv *env = get_jni_env();
	jobject functionObjGlobal = sqlite3_user_data(context);
	jobject functionObj = (*env)->NewLocalRef(env, functionObjGlobal);

	jstring argString = (*env)->NewStringUTF(env, (const char *)sqlite3_value_text(argv[0]));
	jclass operatorClass = (*env)->FindClass(env, "java/util/function/UnaryOperator");
	jstring res = (jstring)(*env)->CallObjectMethod(env, functionObj, _METHOD(operatorClass, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;"), argString);
	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "Exception thrown by custom scalar function\n");
		sqlite3_result_error(context, "Exception thrown by custom scalar function", -1);
		(*env)->ExceptionClear(env);
	} else if (res != NULL) {
		const char *resChars = (*env)->GetStringUTFChars(env, res, NULL);
		sqlite3_result_text(context, resChars, -1, SQLITE_TRANSIENT);
		(*env)->ReleaseStringUTFChars(env, res, resChars);
	}
	(*env)->DeleteLocalRef(env, res);
	(*env)->DeleteLocalRef(env, argString);
	(*env)->DeleteLocalRef(env, functionObj);
}

static void sqliteCustomAggregateFunctionStep(sqlite3_context *context, int argc, sqlite3_value **argv)
{
	const unsigned char *arg = sqlite3_value_text(argv[0]);
	if (arg == NULL) {
		return; // NULL values are skipped
	}
	char **agg = sqlite3_aggregate_context(context, sizeof(char *));
	if (agg == NULL) {
		sqlite3_result_error_nomem(context);
		return;
	}
	if (*agg == NULL) {
		*agg = strdup((const char *)arg);
		return;
	}

	JNIEnv *env = get_jni_env();
	jobject functionObjGlobal = sqlite3_user_data(context);
	jobject functionObj = (*env)->NewLocalRef(env, functionObjGlobal);
	jstring aggString = (*env)->NewStringUTF(env, *agg);
	jstring argString = (*env)->NewStringUTF(env, (const char *)arg);
	jclass operatorClass = (*env)->FindClass(env, "java/util/function/BinaryOperator");
	jstring res = (jstring)(*env)->CallObjectMethod(env, functionObj, _METHOD(operatorClass, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), aggString, argString);
	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "Exception thrown by custom aggregate function\n");
		sqlite3_result_error(context, "Exception thrown by custom aggregate function", -1);
		(*env)->ExceptionClear(env);
		free(*agg);
		*agg = NULL;
	} else if (res != NULL) {
		const char *resChars = (*env)->GetStringUTFChars(env, res, NULL);
		free(*agg);
		*agg = strdup(resChars);
		(*env)->ReleaseStringUTFChars(env, res, resChars);
	}
	(*env)->DeleteLocalRef(env, res);
	(*env)->DeleteLocalRef(env, argString);
	(*env)->DeleteLocalRef(env, aggString);
	(*env)->DeleteLocalRef(env, functionObj);
}

static void sqliteCustomAggregateFunctionFinal(sqlite3_context *context)
{
	char **agg = sqlite3_aggregate_context(context, 0);
	if (agg == NULL || *agg == NULL) {
		sqlite3_result_null(context);
		return;
	}
	sqlite3_result_text(context, *agg, -1, SQLITE_TRANSIENT);
	free(*agg);
	*agg = NULL;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeRegisterCustomScalarFunction(JNIEnv *env, jclass clazz, jlong connectionPtr, jstring name, jobject function)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	jobject functionObjGlobal = (*env)->NewGlobalRef(env, function);
	const char *nameChars = (*env)->GetStringUTFChars(env, name, NULL);
	int err = sqlite3_create_function_v2(connection->db, nameChars, 1, SQLITE_UTF8, functionObjGlobal,
	                                     &sqliteCustomScalarFunctionCallback, NULL, NULL, &sqliteCustomFunctionDestructor);
	(*env)->ReleaseStringUTFChars(env, name, nameChars);
	if (err != SQLITE_OK) {
		/* SQLite has called the destructor already, which deleted functionObjGlobal */
		throw_sqlite3_exception_handle(env, connection->db);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeRegisterCustomAggregateFunction(JNIEnv *env, jclass clazz, jlong connectionPtr, jstring name, jobject function)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	jobject functionObjGlobal = (*env)->NewGlobalRef(env, function);
	const char *nameChars = (*env)->GetStringUTFChars(env, name, NULL);
	int err = sqlite3_create_function_v2(connection->db, nameChars, 1, SQLITE_UTF8, functionObjGlobal,
	                                     NULL, &sqliteCustomAggregateFunctionStep, &sqliteCustomAggregateFunctionFinal,
	                                     &sqliteCustomFunctionDestructor);
	(*env)->ReleaseStringUTFChars(env, name, nameChars);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, connection->db);
	}
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeLastInsertRowId(JNIEnv *env, jclass clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	return sqlite3_last_insert_rowid(connection->db);
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeChanges(JNIEnv *env, jclass clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	return sqlite3_changes(connection->db);
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeTotalChanges(JNIEnv *env, jclass clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	return sqlite3_total_changes(connection->db);
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeRegisterLocalizedCollators(JNIEnv *env, jclass clazz, jlong connectionPtr, jstring localeStr)
{
	/* android_database_SQLiteConnection.cpp: register_localized_collators(connection->db, locale, UTF16_STORAGE) */
	struct SQLiteConnection *connection = _PTR(connectionPtr);

	const char *locale = (*env)->GetStringUTFChars(env, localeStr, NULL);
	int err = register_localized_collators(connection->db, locale);
	(*env)->ReleaseStringUTFChars(env, localeStr, locale);

	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, connection->db);
	}
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativePrepareStatement(JNIEnv *env, jclass clazz, jlong connectionPtr, jstring sqlString)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);

	jsize sqlLength = (*env)->GetStringLength(env, sqlString);
	const jchar *sql = (*env)->GetStringCritical(env, sqlString, NULL);
	sqlite3_stmt *statement;
	int err = sqlite3_prepare16_v2(connection->db,
	                               sql, sqlLength * sizeof(jchar), &statement, NULL);
	(*env)->ReleaseStringCritical(env, sqlString, sql);

	if (err != SQLITE_OK) {
		// Error messages like 'near ")": syntax error' are not
		// always helpful enough, so construct an error string that
		// includes the query itself.
		const char *query = (*env)->GetStringUTFChars(env, sqlString, NULL);
		char *message = (char *)malloc(strlen(query) + 50);
		if (message) {
			strcpy(message, ", while compiling: "); // less than 50 chars
			strcat(message, query);
		}
		(*env)->ReleaseStringUTFChars(env, sqlString, query);
		throw_sqlite3_exception_handle_message(env, connection->db, message);
		free(message);
		return 0;
	}

	// printf("Prepared statement %p on connection %p\n", statement, connection->db);
	return _INTPTR(statement);
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeFinalizeStatement(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	// struct SQLiteConnection* connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	// We ignore the result of sqlite3_finalize because it is really telling us about
	// whether any errors occurred while executing the statement.  The statement itself
	// is always finalized regardless.
	// printf("Finalized statement %p on connection %p\n", statement, connection->db);
	sqlite3_finalize(statement);
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteConnection_nativeGetParameterCount(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	sqlite3_stmt *statement = _PTR(statementPtr);

	return sqlite3_bind_parameter_count(statement);
}

JNIEXPORT jboolean JNICALL Java_android_database_sqlite_SQLiteConnection_nativeIsReadOnly(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	sqlite3_stmt *statement = _PTR(statementPtr);

	return sqlite3_stmt_readonly(statement) != 0;
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteConnection_nativeGetColumnCount(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	sqlite3_stmt *statement = _PTR(statementPtr);

	return sqlite3_column_count(statement);
}

JNIEXPORT jstring JNICALL Java_android_database_sqlite_SQLiteConnection_nativeGetColumnName(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index)
{
	sqlite3_stmt *statement = _PTR(statementPtr);

	const jchar *name = (sqlite3_column_name16(statement, index));
	if (name) {
		size_t length = 0;
		while (name[length]) {
			length += 1;
		}
		return (*env)->NewString(env, name, length);
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeBindNull(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = sqlite3_bind_null(statement, index);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeBindLong(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index, jlong value)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = sqlite3_bind_int64(statement, index, value);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeBindDouble(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index, jdouble value)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = sqlite3_bind_double(statement, index, value);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeBindString(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index, jstring valueString)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	jsize valueLength = (*env)->GetStringLength(env, valueString);
	const jchar *value = (*env)->GetStringCritical(env, valueString, NULL);
	int err = sqlite3_bind_text16(statement, index, value, valueLength * sizeof(jchar),
	                              SQLITE_TRANSIENT);
	(*env)->ReleaseStringCritical(env, valueString, value);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeBindBlob(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr, jint index, jbyteArray valueArray)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	jsize valueLength = (*env)->GetArrayLength(env, valueArray);
	jbyte *value = ((*env)->GetPrimitiveArrayCritical(env, valueArray, NULL));
	int err = sqlite3_bind_blob(statement, index, value, valueLength, SQLITE_TRANSIENT);
	(*env)->ReleasePrimitiveArrayCritical(env, valueArray, value, JNI_ABORT);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeResetStatementAndClearBindings(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = sqlite3_reset(statement);
	if (err == SQLITE_OK) {
		err = sqlite3_clear_bindings(statement);
	}
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle_message(env, connection->db, NULL);
	}
}

static int executeNonQuery(JNIEnv *env, struct SQLiteConnection *connection, sqlite3_stmt *statement)
{
	int err;
	while (SQLITE_ROW == (err = sqlite3_step(statement)))
		;
	if (err != SQLITE_DONE) {
		throw_sqlite3_exception_handle(env, connection->db);
	}
	return err;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecute(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	executeNonQuery(env, connection, statement);
}

static int executeOneRowQuery(JNIEnv *env, struct SQLiteConnection *connection, sqlite3_stmt *statement)
{
	int err = sqlite3_step(statement);
	if (err != SQLITE_ROW) {
		throw_sqlite3_exception_handle(env, connection->db);
	}
	return err;
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecuteForLong(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = executeOneRowQuery(env, connection, statement);
	if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
		return sqlite3_column_int64(statement, 0);
	}
	return -1;
}

JNIEXPORT jstring JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecuteForString(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = executeOneRowQuery(env, connection, statement);
	if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
		const jchar *text = (sqlite3_column_text16(statement, 0));
		if (text) {
			size_t length = sqlite3_column_bytes16(statement, 0) / sizeof(jchar);
			return (*env)->NewString(env, text, length);
		}
	}
	return NULL;
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecuteForChangedRowCount(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = executeNonQuery(env, connection, statement);
	return err == SQLITE_DONE ? sqlite3_changes(connection->db) : -1;
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecuteForLastInsertedRowId(JNIEnv *env, jclass clazz, jlong connectionPtr, jlong statementPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	sqlite3_stmt *statement = _PTR(statementPtr);

	int err = executeNonQuery(env, connection, statement);
	return err == SQLITE_DONE && sqlite3_changes(connection->db) > 0
	         ? sqlite3_last_insert_rowid(connection->db)
	         : -1;
}

/*
** Note: The following symbols must be in the same order as the corresponding
** elements in the aMethod[] array in function nativeExecuteForCursorWindow().
*/
enum CWMethodNames {
	CW_CLEAR = 0,
	CW_SETNUMCOLUMNS = 1,
	CW_ALLOCROW = 2,
	CW_FREELASTROW = 3,
	CW_PUTNULL = 4,
	CW_PUTLONG = 5,
	CW_PUTDOUBLE = 6,
	CW_PUTSTRING = 7,
	CW_PUTBLOB = 8
};

/*
** An instance of this structure represents a single CursorWindow java method.
*/
struct CWMethod {
	jmethodID id;      /* Method id */
	const char *zName; /* Method name */
	const char *zSig;  /* Method JNI signature */
};

/*
** Append the contents of the row that SQL statement pStmt currently points to
** to the CursorWindow object passed as the second argument. The CursorWindow
** currently contains iRow rows. Return true on success or false if an error
** occurs.
*/
static jboolean copyRowToWindow(
    JNIEnv *env,
    jobject win,
    int iRow,
    sqlite3_stmt *pStmt,
    struct CWMethod *aMethod)
{
	int nCol = sqlite3_column_count(pStmt);
	int i;
	jboolean bOk;

	bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_ALLOCROW].id);
	for (i = 0; bOk && i < nCol; i++) {
		switch (sqlite3_column_type(pStmt, i)) {
			case SQLITE_NULL: {
				bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_PUTNULL].id, iRow, i);
				break;
			}

			case SQLITE_INTEGER: {
				jlong val = sqlite3_column_int64(pStmt, i);
				bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_PUTLONG].id, val, iRow, i);
				break;
			}

			case SQLITE_FLOAT: {
				jdouble val = sqlite3_column_double(pStmt, i);
				bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_PUTDOUBLE].id, val, iRow, i);
				break;
			}

			case SQLITE_TEXT: {
				jchar *pStr = (jchar *)sqlite3_column_text16(pStmt, i);
				int nStr = sqlite3_column_bytes16(pStmt, i) / sizeof(jchar);
				jstring val = (*env)->NewString(env, pStr, nStr);
				bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_PUTSTRING].id, val, iRow, i);
				(*env)->DeleteLocalRef(env, val);
				break;
			}

			default: {
				assert(sqlite3_column_type(pStmt, i) == SQLITE_BLOB);
				const jbyte *p = (const jbyte *)sqlite3_column_blob(pStmt, i);
				int n = sqlite3_column_bytes(pStmt, i);
				jbyteArray val = (*env)->NewByteArray(env, n);
				(*env)->SetByteArrayRegion(env, val, 0, n, p);
				bOk = (*env)->CallBooleanMethod(env, win, aMethod[CW_PUTBLOB].id, val, iRow, i);
				(*env)->DeleteLocalRef(env, val);
				break;
			}
		}

		if (bOk == 0) {
			(*env)->CallVoidMethod(env, win, aMethod[CW_FREELASTROW].id);
		}
	}

	return bOk;
}

static jboolean setWindowNumColumns(
    JNIEnv *env,
    jobject win,
    sqlite3_stmt *pStmt,
    struct CWMethod *aMethod)
{
	int nCol;

	(*env)->CallVoidMethod(env, win, aMethod[CW_CLEAR].id);
	nCol = sqlite3_column_count(pStmt);
	return (*env)->CallBooleanMethod(env, win, aMethod[CW_SETNUMCOLUMNS].id, (jint)nCol);
}

/*
** This method has been rewritten for org.sqlite.database.*. The original
** android implementation used the C++ interface to populate a CursorWindow
** object. Since the NDK does not export this interface, we invoke the Java
** interface using standard JNI methods to do the same thing.
**
** This function executes the SQLite statement object passed as the 4th
** argument and copies one or more returned rows into the CursorWindow
** object passed as the 5th argument. The set of rows copied into the
** CursorWindow is always contiguous.
**
** The only row that *must* be copied into the CursorWindow is row
** iRowRequired. Ideally, all rows from iRowStart through to the end
** of the query are copied into the CursorWindow. If this is not possible
** (CursorWindow objects have a finite capacity), some compromise position
** is found (see comments embedded in the code below for details).
**
** The return value is a 64-bit integer calculated as follows:
**
**      (iStart << 32) | nRow
**
** where iStart is the index of the first row copied into the CursorWindow.
** If the countAllRows argument is true, nRow is the total number of rows
** returned by the query. Otherwise, nRow is one greater than the index of
** the last row copied into the CursorWindow.
*/
JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteConnection_nativeExecuteForCursorWindow(
    JNIEnv *env,
    jclass clazz,
    jlong connectionPtr, /* Pointer to SQLiteConnection C++ object */
    jlong statementPtr,  /* Pointer to sqlite3_stmt object */
    jobject win,         /* The CursorWindow object to populate */
    jint startPos,       /* First row to add (advisory) */
    jint iRowRequired,   /* Required row */
    jboolean countAllRows)
{
	sqlite3_stmt *pStmt = _PTR(statementPtr);

	struct CWMethod aMethod[] = {
		{0,         "clear",                     "()V"},
		{0, "setNumColumns",                    "(I)Z"},
		{0,      "allocRow",                     "()Z"},
		{0,   "freeLastRow",                     "()V"},
		{0,       "putNull",                   "(II)Z"},
		{0,       "putLong",                  "(JII)Z"},
		{0,     "putDouble",                  "(DII)Z"},
		{0,     "putString", "(Ljava/lang/String;II)Z"},
		{0,       "putBlob",                 "([BII)Z"},
	};
	jclass cls; /* Class android.database.CursorWindow */
	int i;      /* Iterator variable */
	int nRow;
	jboolean bOk;
	int iStart; /* First row copied to CursorWindow */

	/* Locate all required CursorWindow methods. */
	cls = (*env)->FindClass(env, "android/database/CursorWindow");
	for (i = 0; i < (sizeof(aMethod) / sizeof(struct CWMethod)); i++) {
		aMethod[i].id = (*env)->GetMethodID(env, cls, aMethod[i].zName, aMethod[i].zSig);
		if (aMethod[i].id == NULL) {
			char msgBuf[512];
			snprintf(msgBuf, sizeof(msgBuf), "Failed to find method CursorWindow.%s()", aMethod[i].zName);
			(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), msgBuf);
			return 0;
		}
	}

	/* Set the number of columns in the window */
	bOk = setWindowNumColumns(env, win, pStmt, aMethod);
	if (bOk == 0)
		return 0;

	nRow = 0;
	iStart = startPos;
	while (sqlite3_step(pStmt) == SQLITE_ROW) {
		/* Only copy in rows that occur at or after row index iStart. */
		if (nRow >= iStart && bOk) {
			bOk = copyRowToWindow(env, win, (nRow - iStart), pStmt, aMethod);
			if (bOk == 0) {
				/* The CursorWindow object ran out of memory. If row iRowRequired was
				** not successfully added before this happened, clear the CursorWindow
				** and try to add the current row again.  */
				if (nRow <= iRowRequired) {
					bOk = setWindowNumColumns(env, win, pStmt, aMethod);
					if (bOk == 0) {
						sqlite3_reset(pStmt);
						return 0;
					}
					iStart = nRow;
					bOk = copyRowToWindow(env, win, (nRow - iStart), pStmt, aMethod);
				}

				/* If the CursorWindow is still full and the countAllRows flag is not
				** set, break out of the loop here. If countAllRows is set, continue
				** so as to set variable nRow correctly.  */
				if (bOk == 0 && countAllRows == 0)
					break;
			}
		}

		nRow++;
	}

	/* Finalize the statement. If this indicates an error occurred, throw an
	** SQLiteException exception.  */
	int rc = sqlite3_reset(pStmt);
	if (rc != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, sqlite3_db_handle(pStmt));
		return 0;
	}

	jlong lRet = ((jlong)iStart) << 32 | ((jlong)nRow);
	return lRet;
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteConnection_nativeGetDbLookaside(JNIEnv *env, jobject clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);

	int cur = -1;
	int unused;
	sqlite3_db_status(connection->db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
	return cur;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeCancel(JNIEnv *env, jobject clazz, jlong connectionPtr)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	connection->canceled = true;
}

// Called after each SQLite VM instruction when cancelation is enabled.
static int sqliteProgressHandlerCallback(void *data)
{
	struct SQLiteConnection *connection = (data);
	return connection->canceled;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteConnection_nativeResetCancel(JNIEnv *env, jobject clazz, jlong connectionPtr, jboolean cancelable)
{
	struct SQLiteConnection *connection = _PTR(connectionPtr);
	connection->canceled = false;

	if (cancelable) {
		sqlite3_progress_handler(connection->db, 4, sqliteProgressHandlerCallback,
		                         connection);
	} else {
		sqlite3_progress_handler(connection->db, 0, NULL, NULL);
	}
}

JNIEXPORT jboolean JNICALL Java_android_database_sqlite_SQLiteConnection_nativeHasCodec(JNIEnv *env, jobject clazz)
{
#ifdef SQLITE_HAS_CODEC
	return true;
#else
	return false;
#endif
}
