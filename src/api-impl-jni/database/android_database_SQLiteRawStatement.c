/*
 * Native methods of android.database.sqlite.SQLiteRawStatement (AOSP android_database_SQLiteRawStatement.cpp, Android 16),
 * rewritten from C++ to C for Android Translation Layer. They work on a sqlite3_stmt pointer; text is exchanged as UTF-16.
 */

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <sqlite3.h>

#include "../defines.h"
#include "../util.h"
#include "../generated_headers/android_database_sqlite_SQLiteRawStatement.h"
#include "android_database_SQLiteCommon.h"

/* the statement's database connection for the exception message */
#define STMT_DB(stmt) sqlite3_db_handle(stmt)

/* AOSP throwIfInvalidColumn(): the column must be in the current result row */
static int invalid_column(JNIEnv *env, sqlite3_stmt *stmt, jint col)
{
	if (col < 0 || col >= sqlite3_data_count(stmt)) {
		if (sqlite3_data_count(stmt) == 0) {
			throw_sqlite3_exception_errcode(env, SQLITE_MISUSE, "the statement has no result row");
		} else {
			throw_sqlite3_exception_errcode(env, SQLITE_RANGE, "column index out of range");
		}
		return 1;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindParameterCount(JNIEnv *env, jclass clazz, jlong stmtPtr)
{
	return sqlite3_bind_parameter_count(_PTR(stmtPtr));
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindParameterIndex(JNIEnv *env, jclass clazz, jlong stmtPtr, jstring name)
{
	const char *nameChars = (*env)->GetStringUTFChars(env, name, NULL);
	int index = sqlite3_bind_parameter_index(_PTR(stmtPtr), nameChars);
	(*env)->ReleaseStringUTFChars(env, name, nameChars);
	return index;
}

JNIEXPORT jstring JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindParameterName(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param)
{
	const char *name = sqlite3_bind_parameter_name(_PTR(stmtPtr), param);
	return name == NULL ? NULL : (*env)->NewStringUTF(env, name);
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnCount(JNIEnv *env, jclass clazz, jlong stmtPtr)
{
	return sqlite3_column_count(_PTR(stmtPtr));
}

/* SQLITE_ROW and SQLITE_DONE are returned; any other result is thrown, or returned if throwOnError is false */
JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeStep(JNIEnv *env, jclass clazz, jlong stmtPtr, jboolean throwOnError)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	int err = sqlite3_step(stmt);
	if (err == SQLITE_ROW || err == SQLITE_DONE) {
		return err;
	}
	if (throwOnError) {
		throw_sqlite3_exception_handle(env, STMT_DB(stmt));
	}
	return err;
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeReset(JNIEnv *env, jclass clazz, jlong stmtPtr, jboolean clear)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	int err = sqlite3_reset(stmt);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, STMT_DB(stmt));
		return;
	}
	if (clear) {
		err = sqlite3_clear_bindings(stmt);
		if (err != SQLITE_OK) {
			throw_sqlite3_exception_handle(env, STMT_DB(stmt));
		}
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeClearBindings(JNIEnv *env, jclass clazz, jlong stmtPtr)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	int err = sqlite3_clear_bindings(stmt);
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, STMT_DB(stmt));
	}
}

static void check_bind(JNIEnv *env, sqlite3_stmt *stmt, int err)
{
	if (err != SQLITE_OK) {
		throw_sqlite3_exception_handle(env, STMT_DB(stmt));
	}
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindBlob(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param, jbyteArray val, jint off, jint len)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	jbyte *value = (*env)->GetPrimitiveArrayCritical(env, val, NULL);
	int err = sqlite3_bind_blob(stmt, param, value + off, len, SQLITE_TRANSIENT);
	(*env)->ReleasePrimitiveArrayCritical(env, val, value, JNI_ABORT);
	check_bind(env, stmt, err);
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindDouble(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param, jdouble val)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	check_bind(env, stmt, sqlite3_bind_double(stmt, param, val));
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindInt(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param, jint val)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	check_bind(env, stmt, sqlite3_bind_int(stmt, param, val));
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindLong(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param, jlong val)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	check_bind(env, stmt, sqlite3_bind_int64(stmt, param, val));
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindNull(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	check_bind(env, stmt, sqlite3_bind_null(stmt, param));
}

JNIEXPORT void JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeBindText(JNIEnv *env, jclass clazz, jlong stmtPtr, jint param, jstring val)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	jsize valueLength = (*env)->GetStringLength(env, val);
	const jchar *value = (*env)->GetStringCritical(env, val, NULL);
	int err = sqlite3_bind_text16(stmt, param, value, valueLength * sizeof(jchar), SQLITE_TRANSIENT);
	(*env)->ReleaseStringCritical(env, val, value);
	check_bind(env, stmt, err);
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnType(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	return sqlite3_column_type(stmt, col);
}

JNIEXPORT jstring JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnName(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return NULL;
	const jchar *name = sqlite3_column_name16(stmt, col);
	if (name == NULL) {
		throw_sqlite3_exception_errcode(env, SQLITE_NOMEM, "out of memory for the column name");
		return NULL;
	}
	size_t length = 0;
	while (name[length]) {
		length += 1;
	}
	return (*env)->NewString(env, name, length);
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnBytes(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	return sqlite3_column_bytes(stmt, col);
}

JNIEXPORT jbyteArray JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnBlob(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return NULL;
	const void *blob = sqlite3_column_blob(stmt, col);
	if (blob == NULL) {
		/* a NULL value (or an empty blob, as in AOSP: an empty array for a zero-length blob) */
		if (sqlite3_column_type(stmt, col) == SQLITE_NULL) {
			return NULL;
		}
		return (*env)->NewByteArray(env, 0);
	}
	jsize size = sqlite3_column_bytes(stmt, col);
	jbyteArray array = (*env)->NewByteArray(env, size);
	if (array != NULL) {
		(*env)->SetByteArrayRegion(env, array, 0, size, blob);
	}
	return array;
}

/* copies up to len bytes of the column's value, starting at srcOffset, to val at off; returns the number of bytes copied */
JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnBuffer(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col, jbyteArray val, jint off, jint len, jint srcOffset)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	const void *blob = sqlite3_column_blob(stmt, col);
	if (blob == NULL) {
		return 0;
	}
	jint size = sqlite3_column_bytes(stmt, col);
	if (srcOffset >= size) {
		return 0;
	}
	jint n = size - srcOffset < len ? size - srcOffset : len;
	(*env)->SetByteArrayRegion(env, val, off, n, (const jbyte *)blob + srcOffset);
	return n;
}

JNIEXPORT jdouble JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnDouble(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	return sqlite3_column_double(stmt, col);
}

JNIEXPORT jint JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnInt(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	return sqlite3_column_int(stmt, col);
}

JNIEXPORT jlong JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnLong(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return 0;
	return sqlite3_column_int64(stmt, col);
}

JNIEXPORT jstring JNICALL Java_android_database_sqlite_SQLiteRawStatement_nativeColumnText(JNIEnv *env, jclass clazz, jlong stmtPtr, jint col)
{
	sqlite3_stmt *stmt = _PTR(stmtPtr);
	if (invalid_column(env, stmt, col))
		return NULL;
	const jchar *text = sqlite3_column_text16(stmt, col);
	if (text == NULL) {
		if (sqlite3_column_type(stmt, col) == SQLITE_NULL) {
			return NULL;
		}
		throw_sqlite3_exception_errcode(env, SQLITE_NOMEM, "out of memory for the column text");
		return NULL;
	}
	return (*env)->NewString(env, text, sqlite3_column_bytes16(stmt, col) / sizeof(jchar));
}
