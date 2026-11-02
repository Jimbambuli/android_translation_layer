#include <ctype.h>
#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include <androidfw/androidfw_c_api.h>

#include "defines.h"
#include "util.h"
#include "generated_headers/android_content_res_AssetManager.h"

#include <dirent.h>
#include <glib.h>

#define JAVA_ENUM_CLASS android_content_res_AssetManager
enum {
	JAVA_ENUM(STYLE_TYPE),
	JAVA_ENUM(STYLE_DATA),
	JAVA_ENUM(STYLE_ASSET_COOKIE),
	JAVA_ENUM(STYLE_RESOURCE_ID),
	JAVA_ENUM(STYLE_CHANGING_CONFIGURATIONS),
	JAVA_ENUM(STYLE_DENSITY),
	JAVA_ENUM(STYLE_NUM_ENTRIES),
};
#undef JAVA_ENUM_CLASS

#define JAVA_COOKIE(cookie)   (cookie != -1 ? (jint)(cookie + 1) : -1)
#define NATIVE_COOKIE(cookie) (cookie != -1 ? (ApkAssetsCookie)(cookie - 1) : -1)

void _AssetManager_unlock(struct AssetManager **asset_manager)
{
	AssetManager_unlock(*asset_manager);
}

#define AM_SCOPEDLOCK(asset_manager)      \
	AssetManager_lock(asset_manager); \
	__attribute__((__cleanup__(_AssetManager_unlock))) struct AssetManager *_RESERVED_am = asset_manager;

/* The colors of the system's theme overlay (android.atl.ATLThemeOverlay): on Android, SystemUI's
 * ThemeOverlayController makes fabricated overlays of the android package for them, and the
 * AssetManager of every app finds the overlay's value for these resources (ResTable with the idmap).
 * ATL has no overlays, so a value that resolved to one of these resources gets the overlay's color.
 * Only resources whose value in framework-res is a color are overlaid. */
static GRWLock overlay_lock;
static GHashTable *overlay_colors; // resource id -> ARGB

static GHashTable *loader_colors; // resource id -> ARGB, of any package (Resources.addLoaders())

static GHashTable *color_table(JNIEnv *env, jintArray ids, jintArray colors)
{
	GHashTable *table = NULL;
	if (ids && colors) {
		jsize count = MIN((*env)->GetArrayLength(env, ids), (*env)->GetArrayLength(env, colors));
		jint *id = (*env)->GetIntArrayElements(env, ids, NULL);
		jint *color = (*env)->GetIntArrayElements(env, colors, NULL);
		table = g_hash_table_new(g_direct_hash, g_direct_equal);
		for (jsize i = 0; i < count; i++)
			g_hash_table_insert(table, GUINT_TO_POINTER((guint32)id[i]), GUINT_TO_POINTER((guint32)color[i]));
		(*env)->ReleaseIntArrayElements(env, colors, color, JNI_ABORT);
		(*env)->ReleaseIntArrayElements(env, ids, id, JNI_ABORT);
	}
	return table;
}

static void replace_table(GHashTable **where, GHashTable *table)
{
	g_rw_lock_writer_lock(&overlay_lock);
	GHashTable *old = *where;
	*where = table;
	g_rw_lock_writer_unlock(&overlay_lock);
	if (old)
		g_hash_table_unref(old);
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_setOverlayColors(JNIEnv *env, jclass clazz, jintArray ids, jintArray colors)
{
	replace_table(&overlay_colors, color_table(env, ids, colors));
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_setLoaderColors(JNIEnv *env, jclass clazz, jintArray ids, jintArray colors)
{
	replace_table(&loader_colors, color_table(env, ids, colors));
}

/* the value of resource resid, with the overlay applied (type and data of a Res_value) */
static void overlay_value(uint32_t resid, uint32_t *type, uint32_t *data)
{
	if (*type < TYPE_FIRST_COLOR_INT || *type > TYPE_LAST_COLOR_INT)
		return;
	gpointer color;
	g_rw_lock_reader_lock(&overlay_lock);
	if ((loader_colors && g_hash_table_lookup_extended(loader_colors, GUINT_TO_POINTER(resid), NULL, &color)) ||
	    (resid >> 24 == 0x01 && overlay_colors && g_hash_table_lookup_extended(overlay_colors, GUINT_TO_POINTER(resid), NULL, &color))) {
		*type = TYPE_INT_COLOR_ARGB8;
		*data = GPOINTER_TO_UINT(color);
	}
	g_rw_lock_reader_unlock(&overlay_lock);
}

static void overlay_res_value(uint32_t resid, struct Res_value *value)
{
	uint32_t type = value->dataType, data = value->data;
	overlay_value(resid, &type, &data);
	value->dataType = type;
	value->data = data;
}

/* the STYLE_NUM_ENTRIES values per attribute of ResolveAttrs(), ApplyStyle() and RetrieveAttributes() */
static void overlay_style_values(uint32_t *out_values, jsize count)
{
	for (jsize i = 0; i < count; i++) {
		uint32_t *v = out_values + i * STYLE_NUM_ENTRIES;
		overlay_value(v[STYLE_RESOURCE_ID], &v[STYLE_TYPE], &v[STYLE_DATA]);
	}
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_openAsset(JNIEnv *env, jobject this, jstring _file_name, jint mode)
{
	const char *file_name = _CSTRING(_file_name);

	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, file_name, mode);
	android_log_printf(ANDROID_LOG_VERBOSE, "[" __FILE__ "]", "AssetManager_openAsset(%p, %s, %d) returns %p\n", asset_manager, file_name, mode, asset);

	return _INTPTR(asset);
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_openAssetFd(JNIEnv *env, jobject this, jstring _file_name, jint mode, jlongArray _offset, jlongArray _size)
{
	int fd;
	off_t offset;
	off_t size;

	const char *file_name = _CSTRING(_file_name);

	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, file_name, mode);
	android_log_printf(ANDROID_LOG_VERBOSE, "[" __FILE__ "]", "AssetManager_openAssetFd(%p, %s, %d, ...)\n", asset_manager, file_name, mode);
	if (asset == NULL)
		return -ENOENT; // AssetManager.java throws FileNotFoundException

	if (asset == NULL) {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/FileNotFoundException"), file_name);
		return -1;
	}

	fd = Asset_openFileDescriptor(asset, &offset, &size);

	(*env)->SetLongArrayRegion(env, _offset, 0, 1, (jlong[]){offset});
	(*env)->SetLongArrayRegion(env, _size, 0, 1, (jlong[]){size});

	return fd;
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_getAssetLength(JNIEnv *env, jobject this, jlong _asset)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Asset *asset = _PTR(_asset);
	return Asset_getLength(asset);
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_getAssetRemainingLength(JNIEnv *env, jobject this, jlong _asset)
{
	struct Asset *asset = _PTR(_asset);
	return Asset_getRemainingLength(asset);
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_readAsset(JNIEnv *env, jobject this, jlong _asset, jbyteArray b, jlong offset, jlong length)
{
	int ret;
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Asset *asset = _PTR(_asset);
	jbyte *array = _GET_BYTE_ARRAY_ELEMENTS(b);
	ret = Asset_read(asset, &array[offset], length);
	_RELEASE_BYTE_ARRAY_ELEMENTS(b, array);

	return ret;
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_readAssetChar(JNIEnv *env, jobject this, jlong _asset)
{
	int ret;
	uint8_t byte;
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Asset *asset = _PTR(_asset);
	ret = Asset_read(asset, &byte, 1);
	return (ret == 1) ? byte : -1;
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_seekAsset(JNIEnv *env, jobject this, jlong _asset, jlong offset, jint whence)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Asset *asset = _PTR(_asset);
	return Asset_seek(asset, offset, whence > 0 ? SEEK_END : (whence < 0 ? SEEK_SET : SEEK_CUR));
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_destroyAsset(JNIEnv *env, jobject this, jlong _asset)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Asset *asset = _PTR(_asset);
	Asset_delete(asset);
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_init(JNIEnv *env, jobject this, jint sdk_version)
{
	struct AssetManager *asset_manager = AssetManager_new();
	const struct ResTable_config config = {
		.density = /*ACONFIGURATION_DENSITY_MEDIUM*/ 160,
		.sdkVersion = sdk_version,
	};
	AssetManager_setConfiguration(asset_manager, &config);
	_SET_LONG_FIELD(this, "mObject", _INTPTR(asset_manager));
}

/* Like ResourcesManager.loadApkAssets(), reuse the ApkAssets of a path in every AssetManager: the
 * Resources of each override configuration have an AssetManager of their own, and Theme::SetTo()
 * between two AssetManagers only copies the values of ApkAssets they share. ATL never frees
 * ApkAssets, so the cache holds them for good. */
static const struct ApkAssets *load_apk_assets(const char *path)
{
	static GMutex lock;
	static GHashTable *cache;
	const struct ApkAssets *a;

	g_mutex_lock(&lock);
	if (!cache)
		cache = g_hash_table_new(g_str_hash, g_str_equal);
	a = g_hash_table_lookup(cache, path);
	if (!a) {
		if (path[strlen(path) - 1] == '/')
			a = ApkAssets_loadDir(strdup(path));
		else
			a = ApkAssets_load(strdup(path), false);
		if (a == NULL)
			android_log_printf(ANDROID_LOG_WARN, "[" __FILE__ "]", "failed to load ApkAssets from %s\n", path);
		else
			g_hash_table_insert(cache, g_strdup(path), (gpointer)a);
	}
	g_mutex_unlock(&lock);
	return a;
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_native_1setApkAssets(JNIEnv *env, jobject this, jobjectArray paths, int num_assets)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	const struct ApkAssets *apk_assets[num_assets];
	int actual_count = 0;
	for (int i = 0; i < num_assets; i++) {
		jstring path_jstr = (jstring)((*env)->GetObjectArrayElement(env, paths, i));
		if (path_jstr == NULL)
			continue;
		const char *path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
		const struct ApkAssets *a = load_apk_assets(path);
		(*env)->ReleaseStringUTFChars(env, path_jstr, path);
		if (a != NULL)
			apk_assets[actual_count++] = a;
	}
	if (actual_count > 0)
		AssetManager_setApkAssets(asset_manager, apk_assets, actual_count, true, true);
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_loadResourceValue(JNIEnv *env, jobject this, jint ident, jshort density, jobject outValue, jboolean resolve)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	uint32_t resId = ident;
	struct Res_value value;
	uint32_t outSpecFlags;
	struct ResTable_config outConfig;
	uint32_t ref = 0;
	ApkAssetsCookie cookie = AssetManager_getResource(asset_manager, resId, false, density,
	                                                  &value, &outConfig, &outSpecFlags);
	if (resolve) {
		cookie = AssetManager_resolveReference(asset_manager, cookie,
		                                       &value, &outConfig,
		                                       &outSpecFlags, &ref);
	}
	if (cookie >= 0)
		overlay_res_value(ref ? ref : resId, &value); // ref: the last reference followed, 0 for none
	if (cookie >= 0) {
		_SET_INT_FIELD(outValue, "type", value.dataType);
		_SET_INT_FIELD(outValue, "data", value.data);
		_SET_INT_FIELD(outValue, "resourceId", resId);
		_SET_INT_FIELD(outValue, "assetCookie", JAVA_COOKIE(cookie));
		/* the density of the configuration the value comes from, which sizes a bitmap */
		_SET_INT_FIELD(outValue, "density", outConfig.density);
		if (value.dataType == TYPE_STRING) {
			const struct ResStringPool *string_pool = AssetManager_getStringPoolForCookie(asset_manager, cookie);
			size_t len;
			const char16_t *string = ResStringPool_stringAt(string_pool, value.data, &len);
			_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", (*env)->NewString(env, string, len));
		} else {
			_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", NULL);
		}
	}
	return JAVA_COOKIE(cookie);
}

/* like AOSP's NativeGetResourceStringArray() (android_util_AssetManager.cpp): null if the bag cannot be
 * found or an entry cannot be resolved */
JNIEXPORT jobjectArray JNICALL Java_android_content_res_AssetManager_getArrayStringResource(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	const struct ResolvedBag *bag = AssetManager_getBag(asset_manager, ident);
	if (!bag)
		return NULL;
	jobjectArray array = (*env)->NewObjectArray(env, bag->entry_count, _CACHED_CLASS(String), NULL);
	if (array == NULL)
		return NULL;
	for (int i = 0; i < bag->entry_count; i++) {
		const struct ResolvedBag_Entry entry = bag->entries[i];
		struct Res_value value = entry.value;
		struct ResTable_config outConfig;
		uint32_t outSpecFlags;
		uint32_t ref;
		ApkAssetsCookie cookie = AssetManager_resolveReference(asset_manager, entry.cookie,
		                                                       &value, &outConfig,
		                                                       &outSpecFlags, &ref);
		if (cookie < 0)
			return NULL;
		if (value.dataType == TYPE_STRING) {
			const struct ResStringPool *string_pool = AssetManager_getStringPoolForCookie(asset_manager, cookie);
			if (string_pool == NULL)
				return NULL;
			size_t len;
			const char16_t *string = ResStringPool_stringAt(string_pool, value.data, &len);
			if (string == NULL)
				return NULL;
			jstring java_string = (*env)->NewString(env, string, len);
			if ((*env)->ExceptionCheck(env))
				return NULL;
			(*env)->SetObjectArrayElement(env, array, i, java_string);
			/* a large array of strings could overflow the local reference table */
			(*env)->DeleteLocalRef(env, java_string);
		}
	}

	return array;
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_getResourceIdentifier(JNIEnv *env, jobject this, jstring name_jstr, jstring type_jstr, jstring package_jstr)
{
	const char *name = "";
	const char *type = "";
	const char *package = "";
	uint32_t ret;

	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	if (name_jstr)
		name = (*env)->GetStringUTFChars(env, name_jstr, NULL);
	if (type_jstr)
		type = (*env)->GetStringUTFChars(env, type_jstr, NULL);
	if (package_jstr)
		package = (*env)->GetStringUTFChars(env, package_jstr, NULL);

	ret = AssetManager_getResourceId(asset_manager, name, type, package);
	if (name_jstr)
		(*env)->ReleaseStringUTFChars(env, name_jstr, name);
	if (type_jstr)
		(*env)->ReleaseStringUTFChars(env, type_jstr, type);
	if (package_jstr)
		(*env)->ReleaseStringUTFChars(env, package_jstr, package);
	return ret;
}

JNIEXPORT jobject JNICALL Java_android_content_res_AssetManager_getPooledString(JNIEnv *env, jobject this, jint cookie, jint index)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	const struct ResStringPool *string_pool = AssetManager_getStringPoolForCookie(asset_manager, NATIVE_COOKIE(cookie));
	size_t len;
	const char16_t *string = ResStringPool_stringAt(string_pool, index, &len);
	return (*env)->NewString(env, string, len);
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_newTheme(JNIEnv *env, jobject this)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct Theme *theme = AssetManager_newTheme(asset_manager);
	return _INTPTR(theme);
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_deleteTheme(JNIEnv *env, jobject this, jlong theme_ptr)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	Theme_delete(_PTR(theme_ptr));
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_applyThemeStyle(JNIEnv *env, jobject this, jlong theme_ptr, jint styleRes, jboolean force)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	Theme_applyStyle(_PTR(theme_ptr), styleRes, force);
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_loadThemeAttributeValue(JNIEnv *env, jobject this, jlong theme_ptr, jint ident, jobject outValue, jboolean resolve)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct Theme *theme = _PTR(theme_ptr);
	struct Res_value value;
	uint32_t outSpecFlags;
	struct ResTable_config outConfig;
	uint32_t ref = 0;
	int cookie = Theme_getAttribute(theme, ident, &value, &outSpecFlags);
	if (resolve) {
		cookie = AssetManager_resolveReference(asset_manager, cookie,
		                                       &value, &outConfig,
		                                       &outSpecFlags, &ref);
	}
	if (cookie >= 0)
		overlay_res_value(ref, &value);
	if (cookie >= 0) {
		_SET_INT_FIELD(outValue, "type", value.dataType);
		_SET_INT_FIELD(outValue, "data", value.data);
		_SET_INT_FIELD(outValue, "resourceId", ref);
		_SET_INT_FIELD(outValue, "assetCookie", JAVA_COOKIE(cookie));
		_SET_INT_FIELD(outValue, "density", outConfig.density);
		if (value.dataType == TYPE_STRING) {
			const struct ResStringPool *string_pool = AssetManager_getStringPoolForCookie(asset_manager, cookie);
			size_t len;
			const char16_t *string = ResStringPool_stringAt(string_pool, value.data, &len);
			_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", (*env)->NewString(env, string, len));
		} else {
			_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", NULL);
		}
	}
	return JAVA_COOKIE(cookie);
}

/* function ported from AOSP - Copyright 2006, The Android Open Source Project */
JNIEXPORT jboolean JNICALL Java_android_content_res_AssetManager_resolveAttrs(JNIEnv *env, jobject this,
                                                                              jlong theme_ptr, jint def_style_attr,
                                                                              jint def_style_res, jintArray java_values,
                                                                              jintArray java_attrs, jintArray out_java_values,
                                                                              jintArray out_java_indices)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Theme *theme = _PTR(theme_ptr);
	const jsize attrs_len = (*env)->GetArrayLength(env, java_attrs);
	const jsize out_values_len = (*env)->GetArrayLength(env, out_java_values);
	if (out_values_len < (attrs_len * STYLE_NUM_ENTRIES)) {
		(*env)->ThrowNew(env, _CACHED_CLASS(IndexOutOfBoundsException), "outValues too small");
		return false;
	}

	jint *attrs = (jint *)(*env)->GetPrimitiveArrayCritical(env, java_attrs, NULL);
	if (attrs == NULL) {
		return true;
	}

	jint *values = NULL;
	jsize values_len = 0;
	if (java_values != NULL) {
		values_len = (*env)->GetArrayLength(env, java_values);
		values = (jint *)(*env)->GetPrimitiveArrayCritical(env, java_values, NULL);
		if (values == NULL) {
			(*env)->ReleasePrimitiveArrayCritical(env, java_attrs, attrs, JNI_ABORT);
			return false;
		}
	}

	jint *out_values = (jint *)(*env)->GetPrimitiveArrayCritical(env, out_java_values, NULL);
	if (!out_values) {
		(*env)->ReleasePrimitiveArrayCritical(env, java_attrs, attrs, JNI_ABORT);
		if (values) {
			(*env)->ReleasePrimitiveArrayCritical(env, java_values, values, JNI_ABORT);
		}
		return false;
	}

	jint *out_indices = NULL;
	if (out_java_indices) {
		jsize out_indices_len = (*env)->GetArrayLength(env, out_java_indices);
		if (out_indices_len > attrs_len) {
			out_indices = (jint *)(*env)->GetPrimitiveArrayCritical(env, out_java_indices, NULL);
			if (!out_indices) {
				(*env)->ReleasePrimitiveArrayCritical(env, java_attrs, attrs, JNI_ABORT);
				if (values)
					(*env)->ReleasePrimitiveArrayCritical(env, java_values, values, JNI_ABORT);
				(*env)->ReleasePrimitiveArrayCritical(env, out_java_values, out_values, JNI_ABORT);
				return false;
			}
		}
	}

	bool ret = ResolveAttrs(theme, def_style_attr, def_style_res,
	                        (uint32_t *)values, values_len,
	                        (uint32_t *)attrs, attrs_len,
	                        (uint32_t *)out_values, (uint32_t *)out_indices);
	overlay_style_values((uint32_t *)out_values, attrs_len);
	if (out_indices)
		(*env)->ReleasePrimitiveArrayCritical(env, out_java_indices, out_indices, 0);

	(*env)->ReleasePrimitiveArrayCritical(env, out_java_values, out_values, 0);

	if (values)
		(*env)->ReleasePrimitiveArrayCritical(env, java_values, values, JNI_ABORT);

	(*env)->ReleasePrimitiveArrayCritical(env, java_attrs, attrs, JNI_ABORT);
	return ret;
}

JNIEXPORT jboolean JNICALL Java_android_content_res_AssetManager_retrieveAttributes(JNIEnv *env, jobject this,
                                                                                    jlong parser_ptr,
                                                                                    jintArray java_attrs, jint attrs_len,
                                                                                    jlong out_values, jlong out_indices)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct ResXMLParser *parser = (struct ResXMLParser *)_PTR(parser_ptr);

	jint *attrs = (*env)->GetIntArrayElements(env, java_attrs, 0);

	jboolean ret = RetrieveAttributes(asset_manager, parser, (uint32_t *)attrs, attrs_len, (uint32_t *)_PTR(out_values), (uint32_t *)_PTR(out_indices));
	overlay_style_values((uint32_t *)_PTR(out_values), attrs_len);

	(*env)->ReleaseIntArrayElements(env, java_attrs, attrs, JNI_ABORT);

	return ret;
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_applyStyle(JNIEnv *env, jobject this,
                                                                        jlong theme_ptr, jlong parser_ptr,
                                                                        jint def_style_attr, jint def_style_res,
                                                                        jintArray java_attrs, jint attrs_len,
                                                                        jlong out_values, jlong out_indices)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct Theme *theme = _PTR(theme_ptr);
	struct ResXMLParser *parser = (struct ResXMLParser *)_PTR(parser_ptr);

	jint *attrs = (*env)->GetIntArrayElements(env, java_attrs, 0);

	ApplyStyle(theme, parser, def_style_attr, def_style_res, (uint32_t *)attrs, attrs_len, (uint32_t *)_PTR(out_values), (uint32_t *)_PTR(out_indices));
	overlay_style_values((uint32_t *)_PTR(out_values), attrs_len);

	(*env)->ReleaseIntArrayElements(env, java_attrs, attrs, JNI_ABORT);
}

/* like AOSP's NativeGetResourceArraySize(): -1 if the bag cannot be found */
JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_getArraySize(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	const struct ResolvedBag *bag = AssetManager_getBag(asset_manager, ident);
	if (!bag)
		return -1;
	return bag->entry_count;
}

/* like AOSP's NativeGetResourceArray(): -1 if the bag cannot be found or an entry cannot be resolved,
 * IllegalArgumentException if the output array is too small */
JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_retrieveArray(JNIEnv *env, jobject this, jint ident, jintArray outArray)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	const struct ResolvedBag *bag = AssetManager_getBag(asset_manager, ident);
	if (!bag)
		return -1;

	const jsize out_data_length = (*env)->GetArrayLength(env, outArray);
	if ((*env)->ExceptionCheck(env))
		return -1;

	if ((jsize)bag->entry_count > out_data_length * STYLE_NUM_ENTRIES) {
		(*env)->ThrowNew(env, _CACHED_CLASS(IllegalArgumentException), "Input array is not large enough");
		return -1;
	}

	jint *array = (*env)->GetIntArrayElements(env, outArray, NULL);
	if (array == NULL)
		return -1;
	for (int i = 0; i < bag->entry_count; i++) {
		const struct ResolvedBag_Entry entry = bag->entries[i];
		struct Res_value value = entry.value;
		struct ResTable_config outConfig;
		uint32_t outSpecFlags;
		uint32_t ref = 0;
		ApkAssetsCookie cookie = AssetManager_resolveReference(asset_manager, entry.cookie,
		                                                       &value, &outConfig,
		                                                       &outSpecFlags, &ref);
		if (cookie < 0) {
			(*env)->ReleaseIntArrayElements(env, outArray, array, JNI_ABORT);
			return -1;
		}
		overlay_res_value(ref, &value);

		array[i * STYLE_NUM_ENTRIES + STYLE_TYPE] = value.dataType;
		array[i * STYLE_NUM_ENTRIES + STYLE_DATA] = value.data;
		array[i * STYLE_NUM_ENTRIES + STYLE_ASSET_COOKIE] = JAVA_COOKIE(cookie);
		array[i * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID] = ref;
	}
	(*env)->ReleaseIntArrayElements(env, outArray, array, 0);
	return bag->entry_count;
}

JNIEXPORT jstring JNICALL Java_android_content_res_AssetManager_getResourceName(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct resource_name res_name;
	bool ret = AssetManager_getResourceName(asset_manager, ident, &res_name);
	if (!ret)
		return NULL;

	const gchar *type = res_name.type ?: g_utf16_to_utf8(res_name.type16, res_name.type_len, NULL, NULL, NULL);
	const gchar *entry = res_name.entry ?: g_utf16_to_utf8(res_name.entry16, res_name.entry_len, NULL, NULL, NULL);

	gchar *result = g_strdup_printf("%.*s:%.*s/%.*s",
	                                res_name.package ? (int)res_name.package_len : 0,
	                                res_name.package ?: "",
	                                type ? (int)res_name.type_len : 0,
	                                type ?: "",
	                                entry ? (int)res_name.entry_len : 0,
	                                entry ?: "");
	jstring result_jstr = _JSTRING(result);
	free(result);
	return result_jstr;
}

JNIEXPORT jstring JNICALL Java_android_content_res_AssetManager_getResourcePackageName(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct resource_name res_name;
	bool ret = AssetManager_getResourceName(asset_manager, ident, &res_name);
	return (ret && res_name.package) ? (*env)->NewStringUTF(env, res_name.package) : NULL;
}

JNIEXPORT jstring JNICALL Java_android_content_res_AssetManager_getResourceTypeName(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct resource_name res_name;
	bool ret = AssetManager_getResourceName(asset_manager, ident, &res_name);
	if (!ret)
		return NULL;

	if (res_name.type)
		return (*env)->NewStringUTF(env, res_name.type);
	else if (res_name.type16)
		return (*env)->NewString(env, res_name.type16, res_name.type_len);
	else
		return NULL;
}

JNIEXPORT jstring JNICALL Java_android_content_res_AssetManager_getResourceEntryName(JNIEnv *env, jobject this, jint ident)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct resource_name res_name;
	bool ret = AssetManager_getResourceName(asset_manager, ident, &res_name);
	if (!ret)
		return NULL;

	if (res_name.entry)
		return (*env)->NewStringUTF(env, res_name.entry);
	else if (res_name.entry16)
		return (*env)->NewString(env, res_name.entry16, res_name.entry_len);
	else
		return NULL;
}

JNIEXPORT jint JNICALL Java_android_content_res_AssetManager_loadResourceBagValue(JNIEnv *env, jobject this, jint ident, jint bagEntryId, jobject outValue, jboolean resolve)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	ApkAssetsCookie cookie = -1;

	const struct ResolvedBag *bag = AssetManager_getBag(asset_manager, ident);
	if (!bag)
		return -1;
	for (int i = 0; i < bag->entry_count; i++) {
		const struct ResolvedBag_Entry entry = bag->entries[i];
		if (entry.key == bagEntryId) {
			struct Res_value value = entry.value;
			struct ResTable_config outConfig;
			uint32_t outSpecFlags;
			uint32_t ref = 0;
			cookie = AssetManager_resolveReference(asset_manager, entry.cookie,
			                                       &value, &outConfig,
			                                       &outSpecFlags, &ref);
			overlay_res_value(ref, &value);

			_SET_INT_FIELD(outValue, "type", value.dataType);
			_SET_INT_FIELD(outValue, "data", value.data);
			_SET_INT_FIELD(outValue, "resourceId", ref);
			_SET_INT_FIELD(outValue, "assetCookie", JAVA_COOKIE(cookie));
			if (value.dataType == TYPE_STRING) {
				const struct ResStringPool *string_pool = AssetManager_getStringPoolForCookie(asset_manager, cookie);
				size_t len;
				const char16_t *string = ResStringPool_stringAt(string_pool, value.data, &len);
				_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", (*env)->NewString(env, string, len));
			} else {
				_SET_OBJ_FIELD(outValue, "string", "Ljava/lang/CharSequence;", NULL);
			}
			break;
		}
	}

	return JAVA_COOKIE(cookie);
}

/* like AOSP's NativeThemeCopy(): lock both AssetManagers, the themes may belong to different ones */
JNIEXPORT void JNICALL Java_android_content_res_AssetManager_copyTheme(JNIEnv *env, jobject this, jlong dest, jobject src_assets, jlong src)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	struct AssetManager *src_asset_manager = _PTR(_GET_LONG_FIELD(src_assets, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	if (src_asset_manager != asset_manager) {
		AssetManager_lock(src_asset_manager);
		Theme_setTo(_PTR(dest), _PTR(src));
		AssetManager_unlock(src_asset_manager);
	} else {
		Theme_setTo(_PTR(dest), _PTR(src));
	}
}

/* ResTable_config::setBcp47Locale() and what it uses, from frameworks/base/libs/androidfw/ResourceTypes.cpp
 * (android-14), in C: libandroidfw's C API only exposes the struct, not the C++ methods. Not taken: computeScript()
 * (it needs the locale data table of LocaleData.cpp), so localeScriptWasComputed stays set without a script. */
static void pack_language_or_region(const char *in, const char base, char out[2])
{
	if (in[2] == 0 || in[2] == '-') {
		out[0] = in[0];
		out[1] = in[1];
	} else {
		uint8_t first = (in[0] - base) & 0x007f;
		uint8_t second = (in[1] - base) & 0x007f;
		uint8_t third = (in[2] - base) & 0x007f;

		out[0] = (0x80 | (third << 2) | (second >> 3));
		out[1] = ((second << 5) | first);
	}
}

enum locale_parser_kind {
	LPS_BASE,
	LPS_UNICODE_EXTENSION,
	LPS_IGNORE_THE_REST
};

enum locale_unicode_kind {
	LUS_NO_KEY,
	LUS_EXPECT_KEY,
	LUS_IGNORE_KEY,
	LUS_NUMBERING_SYSTEM
};

struct locale_parser_state {
	enum locale_parser_kind parser_state;
	enum locale_unicode_kind unicode_state;
};

static struct locale_parser_state assign_locale_component(struct ResTable_config *config, const char *start, size_t size, struct locale_parser_state state)
{
	/* It is assumed that this function is not invoked with state.parser_state set to LPS_IGNORE_THE_REST. */
	if (state.parser_state == LPS_UNICODE_EXTENSION) {
		switch (size) {
			case 1:
				/* Other BCP 47 extensions are not supported at the moment */
				state.parser_state = LPS_IGNORE_THE_REST;
				break;
			case 2:
				if (state.unicode_state == LUS_NO_KEY || state.unicode_state == LUS_EXPECT_KEY) {
					/* Analyze Unicode extension key. Currently only 'nu' (numbering system) is supported. */
					if ((start[0] == 'n' || start[0] == 'N') && (start[1] == 'u' || start[1] == 'U'))
						state.unicode_state = LUS_NUMBERING_SYSTEM;
					else
						state.unicode_state = LUS_IGNORE_KEY;
				} else {
					/* Keys are not allowed in other state allowed, ignore the rest. */
					state.parser_state = LPS_IGNORE_THE_REST;
				}
				break;
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
				switch (state.unicode_state) {
					case LUS_NUMBERING_SYSTEM:
						/* Accept only the first occurrence of the numbering system. */
						if (config->localeNumberingSystem[0] == '\0') {
							for (size_t i = 0; i < size; ++i)
								config->localeNumberingSystem[i] = tolower(start[i]);
							state.unicode_state = LUS_EXPECT_KEY;
						} else {
							state.parser_state = LPS_IGNORE_THE_REST;
						}
						break;
					case LUS_IGNORE_KEY:
						/* Unsupported Unicode keyword. Ignore. */
						state.unicode_state = LUS_EXPECT_KEY;
						break;
					case LUS_EXPECT_KEY:
						/* A keyword followed by an attribute is not allowed. */
						state.parser_state = LPS_IGNORE_THE_REST;
						break;
					case LUS_NO_KEY:
						/* Extension attribute. Do nothing. */
						break;
				}
				break;
			default:
				/* Unexpected field length - ignore the rest and treat as an error */
				state.parser_state = LPS_IGNORE_THE_REST;
		}
		return state;
	}

	switch (size) {
		case 0:
			state.parser_state = LPS_IGNORE_THE_REST;
			break;
		case 1:
			state.parser_state = (start[0] == 'u' || start[0] == 'U') ? LPS_UNICODE_EXTENSION : LPS_IGNORE_THE_REST;
			break;
		case 2:
		case 3:
			if (config->language[0])
				pack_language_or_region(start, '0', config->country);
			else
				pack_language_or_region(start, 'a', config->language);
			break;
		case 4:
			if ('0' <= start[0] && start[0] <= '9') {
				/* this is a variant, so fall through */
			} else {
				config->localeScript[0] = toupper(start[0]);
				for (size_t i = 1; i < 4; ++i)
					config->localeScript[i] = tolower(start[i]);
				break;
			}
			/* fall through */
		case 5:
		case 6:
		case 7:
		case 8:
			for (size_t i = 0; i < size; ++i)
				config->localeVariant[i] = tolower(start[i]);
			break;
		default:
			state.parser_state = LPS_IGNORE_THE_REST;
	}

	return state;
}

static void res_table_config_set_bcp47_locale(struct ResTable_config *config, const char *in)
{
	/* clearLocale() */
	memset(config->language, 0, sizeof(config->language));
	memset(config->country, 0, sizeof(config->country));
	memset(config->localeScript, 0, sizeof(config->localeScript));
	memset(config->localeVariant, 0, sizeof(config->localeVariant));
	memset(config->localeNumberingSystem, 0, sizeof(config->localeNumberingSystem));

	const char *start = in;
	const char *separator;
	struct locale_parser_state state = {LPS_BASE, LUS_NO_KEY};
	while ((separator = strchr(start, '-'))) {
		const size_t size = separator - start;
		state = assign_locale_component(config, start, size, state);
		if (state.parser_state == LPS_IGNORE_THE_REST) {
			fprintf(stderr, "Invalid BCP-47 locale string: %s\n", in);
			break;
		}
		start = (separator + 1);
	}

	if (state.parser_state != LPS_IGNORE_THE_REST) {
		const size_t size = strlen(start);
		assign_locale_component(config, start, size, state);
	}

	config->localeScriptWasComputed = (config->localeScript[0] == '\0');
}

JNIEXPORT void JNICALL Java_android_content_res_AssetManager_setConfiguration(
    JNIEnv *env, jobject this, jint mcc, jint mnc, jstring locale,
    jint orientation, jint touchscreen, jint density, jint keyboard,
    jint keyboardHidden, jint navigation, jint screenWidth, jint screenHeight,
    jint smallestScreenWidthDp, jint screenWidthDp, jint screenHeightDp,
    jint screenLayout, jint uiMode, jint majorVersion)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	struct ResTable_config config = {
		.mcc = mcc,
		.mnc = mnc,
		.orientation = orientation,
		.touchscreen = touchscreen,
		.density = density,
		.keyboard = keyboard,
		.navigation = navigation,
		.screenWidth = screenWidth,
		.screenHeight = screenHeight,
		.smallestScreenWidthDp = smallestScreenWidthDp,
		.screenWidthDp = screenWidthDp,
		.screenHeightDp = screenHeightDp,
		.screenLayout = screenLayout,
		.uiMode = uiMode,
		.sdkVersion = majorVersion
	};
	if (locale) {
		/* as android_util_AssetManager.cpp NativeSetConfiguration(): ResTable_config::setBcp47Locale() */
		const char *locale_str = (*env)->GetStringUTFChars(env, locale, NULL);
		res_table_config_set_bcp47_locale(&config, locale_str);
		(*env)->ReleaseStringUTFChars(env, locale, locale_str);
	}
	AssetManager_setConfiguration(asset_manager, &config);
}

JNIEXPORT jobjectArray JNICALL Java_android_content_res_AssetManager_list(JNIEnv *env, jobject this, jstring path_jstr)
{
	const char *path = "";

	path = (*env)->GetStringUTFChars(env, path_jstr, NULL);
	if (!path_jstr)
		return NULL;

	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)

	struct AssetDir *asset_dir = AssetManager_openDir(asset_manager, path);
	(*env)->ReleaseStringUTFChars(env, path_jstr, path);
	if (!asset_dir) {
		(*env)->ThrowNew(env, _CACHED_CLASS(FileNotFoundException), path);
		return NULL;
	}

	const size_t file_count = AssetDir_getFileCount(asset_dir);

	jobjectArray array = (*env)->NewObjectArray(env, file_count, _CACHED_CLASS(String), NULL);
	if (!array)
		return NULL;

	for (size_t i = 0; i < file_count; i++) {
		const char *asset = AssetDir_getFileName(asset_dir, i);
		jstring asset_jstr = (*env)->NewStringUTF(env, asset);
		(*env)->SetObjectArrayElement(env, array, i, (*env)->NewStringUTF(env, asset));
		(*env)->DeleteLocalRef(env, asset_jstr);
	}

	return array;
}

JNIEXPORT jlong JNICALL Java_android_content_res_AssetManager_openXmlAssetNative(JNIEnv *env, jobject this, jint cookie, jstring _file_name)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	const char *file_name = (*env)->GetStringUTFChars(env, _file_name, NULL);
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, file_name, ACCESS_BUFFER);
	(*env)->ReleaseStringUTFChars(env, _file_name, file_name);
	if (asset == NULL)
		return 0; // AssetManager.java throws FileNotFoundException

	struct ResXMLTree *res_xml = ResXMLTree_new();
	ResXMLTree_setTo(res_xml, Asset_getBuffer(asset, true), Asset_getLength(asset), true);
	Asset_delete(asset);
	return _INTPTR(res_xml);
}

JNIEXPORT jintArray JNICALL Java_android_content_res_AssetManager_getSizeConfigurationsNative(JNIEnv *env, jobject this)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	uint16_t *sizes;
	size_t count = AssetManager_getSizeConfigurations(asset_manager, &sizes);
	jintArray array = (*env)->NewIntArray(env, count * 3);
	jint *elements = (*env)->GetIntArrayElements(env, array, NULL);
	for (size_t i = 0; i < count * 3; i++)
		elements[i] = sizes[i];
	(*env)->ReleaseIntArrayElements(env, array, elements, 0);
	free(sizes);
	return array;
}

JNIEXPORT jobjectArray JNICALL Java_android_content_res_AssetManager_getLocales(JNIEnv *env, jobject this)
{
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(this, "mObject"));
	AM_SCOPEDLOCK(asset_manager)
	char **locales = AssetManager_getLocales(asset_manager, false, true);
	int i = 0;
	while (locales[i] != NULL)
		i++;
	jobjectArray array = (*env)->NewObjectArray(env, i, _CACHED_CLASS(String), NULL);
	for (i = 0; locales[i] != NULL; i++) {
		(*env)->SetObjectArrayElement(env, array, i, (*env)->NewStringUTF(env, locales[i]));
		free(locales[i]);
	}
	free(locales);
	return array;
}
