/* JNI of android.opengl.Visibility, ported from AOSP Android 16 (frameworks/base/core/jni/android/opengl/util.cpp) to C.
 * AOSP's ArrayHelper template (Get/Release of one primitive array type) is one struct here, the array type is a tag. */
#include <jni.h>
#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include "generated_headers/android_opengl_Visibility.h"
#include "opengl_poly.h"

static void throw_iae(JNIEnv *env, const char *message)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"), message);
}

enum array_type { FLOAT_ARRAY,
	          CHAR_ARRAY,
	          INT_ARRAY };

typedef struct {
	enum array_type type;
	void *mData;
	int mLength;
	void *mBase;
	JNIEnv *mEnv;
	jarray mRef;
	jint mOffset;
	jint mMinSize;
	int mReleaseParam;
} ArrayHelper;

#define ARRAY_HELPER(type, env, ref, offset, minSize) {type, NULL, 0, NULL, env, ref, offset, minSize, JNI_ABORT}

static size_t element_size(enum array_type type)
{
	return type == FLOAT_ARRAY ? sizeof(jfloat) : type == CHAR_ARRAY ? sizeof(jchar)
	                                                                 : sizeof(jint);
}

/* We separate the bounds check from the initialization because we want to be able to bounds-check multiple
 * arrays, and we can't throw an exception after we've called GetPrimitiveArrayCritical. */
static bool check(ArrayHelper *h)
{
	if (!h->mRef) {
		throw_iae(h->mEnv, "array == null");
		return false;
	}
	if (h->mOffset < 0) {
		throw_iae(h->mEnv, "offset < 0");
		return false;
	}
	h->mLength = (*h->mEnv)->GetArrayLength(h->mEnv, h->mRef) - h->mOffset;
	if (h->mLength < h->mMinSize) {
		throw_iae(h->mEnv, "length - offset < n");
		return false;
	}
	return true;
}

static void bind(ArrayHelper *h)
{
	JNIEnv *env = h->mEnv;
	switch (h->type) {
		case FLOAT_ARRAY:
			h->mBase = (*env)->GetFloatArrayElements(env, h->mRef, NULL);
			break;
		case CHAR_ARRAY:
			h->mBase = (*env)->GetCharArrayElements(env, h->mRef, NULL);
			break;
		default:
			h->mBase = (*env)->GetIntArrayElements(env, h->mRef, NULL);
			break;
	}
	h->mData = (char *)h->mBase + h->mOffset * element_size(h->type);
}

static void commitChanges(ArrayHelper *h)
{
	h->mReleaseParam = 0;
}

static void release(ArrayHelper *h)
{
	JNIEnv *env = h->mEnv;
	if (!h->mBase)
		return;
	switch (h->type) {
		case FLOAT_ARRAY:
			(*env)->ReleaseFloatArrayElements(env, h->mRef, h->mBase, h->mReleaseParam);
			break;
		case CHAR_ARRAY:
			(*env)->ReleaseCharArrayElements(env, h->mRef, h->mBase, h->mReleaseParam);
			break;
		default:
			(*env)->ReleaseIntArrayElements(env, h->mRef, h->mBase, h->mReleaseParam);
			break;
	}
	h->mBase = NULL;
}

static inline void mx4transform(float x, float y, float z, float w, const float *pM, float *pDest)
{
	pDest[0] = pM[0 + 4 * 0] * x + pM[0 + 4 * 1] * y + pM[0 + 4 * 2] * z + pM[0 + 4 * 3] * w;
	pDest[1] = pM[1 + 4 * 0] * x + pM[1 + 4 * 1] * y + pM[1 + 4 * 2] * z + pM[1 + 4 * 3] * w;
	pDest[2] = pM[2 + 4 * 0] * x + pM[2 + 4 * 1] * y + pM[2 + 4 * 2] * z + pM[2 + 4 * 3] * w;
	pDest[3] = pM[3 + 4 * 0] * x + pM[3 + 4 * 1] * y + pM[3 + 4 * 2] * z + pM[3 + 4 * 3] * w;
}

static int visibilityTest(float *pWS, float *pPositions, int positionsLength,
                          unsigned short *pIndices, int indexCount)
{
	int result = POLY_CLIP_OUT;

	if (indexCount < 3) {
		return POLY_CLIP_OUT;
	}

	// Find out how many vertices we need to transform
	// We transform every vertex between the min and max indices, inclusive.
	// This is OK for the data sets we expect to use with this function, but
	// for other loads it might be better to use a more sophisticated vertex
	// cache of some sort.

	int minIndex = 65536;
	int maxIndex = -1;
	for (int i = 0; i < indexCount; i++) {
		int index = pIndices[i];
		if (index < minIndex) {
			minIndex = index;
		}
		if (index > maxIndex) {
			maxIndex = index;
		}
	}

	if (maxIndex * 3 > positionsLength) {
		return -1;
	}

	int transformedIndexCount = maxIndex - minIndex + 1;
	float *pTransformed = malloc(sizeof(float) * transformedIndexCount * 4);

	if (pTransformed == 0) {
		return -2;
	}

	// Transform the vertices
	{
		const float *pSrc = pPositions + 3 * minIndex;
		float *pDst = pTransformed;
		for (int i = 0; i < transformedIndexCount; i++, pSrc += 3, pDst += 4) {
			mx4transform(pSrc[0], pSrc[1], pSrc[2], 1.0f, pWS, pDst);
		}
	}

	// Clip the triangles

	Poly poly;
	float *pDest = &poly.vert[0].sx;
	for (int i = 0; i < indexCount; i += 3) {
		poly.n = 3;
		memcpy(pDest, pTransformed + 4 * (pIndices[i] - minIndex), 4 * sizeof(float));
		memcpy(pDest + 4, pTransformed + 4 * (pIndices[i + 1] - minIndex), 4 * sizeof(float));
		memcpy(pDest + 8, pTransformed + 4 * (pIndices[i + 2] - minIndex), 4 * sizeof(float));
		result = poly_clip_to_frustum(&poly);
		if (result != POLY_CLIP_OUT) {
			free(pTransformed);
			return result;
		}
	}

	free(pTransformed);
	return result;
}

static inline float distance2(float x, float y, float z)
{
	return x * x + y * y + z * z;
}

static inline float distance(float x, float y, float z)
{
	return sqrtf(distance2(x, y, z));
}

JNIEXPORT void JNICALL Java_android_opengl_Visibility_computeBoundingSphere(JNIEnv *env, jclass clazz,
                                                                            jfloatArray positions_ref, jint positionsOffset, jint positionsCount,
                                                                            jfloatArray sphere_ref, jint sphereOffset)
{
	ArrayHelper positions = ARRAY_HELPER(FLOAT_ARRAY, env, positions_ref, positionsOffset, 0);
	ArrayHelper sphere = ARRAY_HELPER(FLOAT_ARRAY, env, sphere_ref, sphereOffset, 4);

	bool checkOK = check(&positions) && check(&sphere);
	if (!checkOK) {
		return;
	}

	bind(&positions);
	bind(&sphere);

	if (positionsCount < 1) {
		throw_iae(env, "positionsCount < 1");
		release(&positions);
		release(&sphere);
		return;
	}

	const float *pSrc = ((float *)positions.mData);

	// find bounding box
	float x0 = *pSrc++;
	float x1 = x0;
	float y0 = *pSrc++;
	float y1 = y0;
	float z0 = *pSrc++;
	float z1 = z0;

	for (int i = 1; i < positionsCount; i++) {
		{
			float x = *pSrc++;
			if (x < x0) {
				x0 = x;
			} else if (x > x1) {
				x1 = x;
			}
		}
		{
			float y = *pSrc++;
			if (y < y0) {
				y0 = y;
			} else if (y > y1) {
				y1 = y;
			}
		}
		{
			float z = *pSrc++;
			if (z < z0) {
				z0 = z;
			} else if (z > z1) {
				z1 = z;
			}
		}
	}

	// Because we know our input meshes fit pretty well into bounding boxes,
	// just take the diagonal of the box as defining our sphere.
	float *pSphere = ((float *)sphere.mData);
	float dx = x1 - x0;
	float dy = y1 - y0;
	float dz = z1 - z0;
	*pSphere++ = x0 + dx * 0.5f;
	*pSphere++ = y0 + dy * 0.5f;
	*pSphere++ = z0 + dz * 0.5f;
	*pSphere++ = distance(dx, dy, dz) * 0.5f;

	commitChanges(&sphere);
	release(&positions);
	release(&sphere);
}

static void normalizePlane(float *p)
{
	float rdist = 1.0f / distance(p[0], p[1], p[2]);
	for (int i = 0; i < 4; i++) {
		p[i] *= rdist;
	}
}

static inline float dot3(float x0, float y0, float z0, float x1, float y1, float z1)
{
	return x0 * x1 + y0 * y1 + z0 * z1;
}

static inline float signedDistance(const float *pPlane, float x, float y, float z)
{
	return dot3(pPlane[0], pPlane[1], pPlane[2], x, y, z) + pPlane[3];
}

// Return true if the sphere intersects or is inside the frustum

static bool sphereHitsFrustum(const float *pFrustum, const float *pSphere)
{
	float x = pSphere[0];
	float y = pSphere[1];
	float z = pSphere[2];
	float negRadius = -pSphere[3];
	for (int i = 0; i < 6; i++, pFrustum += 4) {
		if (signedDistance(pFrustum, x, y, z) <= negRadius) {
			return false;
		}
	}
	return true;
}

static void computeFrustum(const float *m, float *f)
{
	float m3 = m[3];
	float m7 = m[7];
	float m11 = m[11];
	float m15 = m[15];
	// right
	f[0] = m3 - m[0];
	f[1] = m7 - m[4];
	f[2] = m11 - m[8];
	f[3] = m15 - m[12];
	normalizePlane(f);
	f += 4;

	// left
	f[0] = m3 + m[0];
	f[1] = m7 + m[4];
	f[2] = m11 + m[8];
	f[3] = m15 + m[12];
	normalizePlane(f);
	f += 4;

	// top
	f[0] = m3 - m[1];
	f[1] = m7 - m[5];
	f[2] = m11 - m[9];
	f[3] = m15 - m[13];
	normalizePlane(f);
	f += 4;

	// bottom
	f[0] = m3 + m[1];
	f[1] = m7 + m[5];
	f[2] = m11 + m[9];
	f[3] = m15 + m[13];
	normalizePlane(f);
	f += 4;

	// far
	f[0] = m3 - m[2];
	f[1] = m7 - m[6];
	f[2] = m11 - m[10];
	f[3] = m15 - m[14];
	normalizePlane(f);
	f += 4;

	// near
	f[0] = m3 + m[2];
	f[1] = m7 + m[6];
	f[2] = m11 + m[10];
	f[3] = m15 + m[14];
	normalizePlane(f);
}

JNIEXPORT jint JNICALL Java_android_opengl_Visibility_frustumCullSpheres(JNIEnv *env, jclass clazz,
                                                                         jfloatArray mvp_ref, jint mvpOffset,
                                                                         jfloatArray spheres_ref, jint spheresOffset, jint spheresCount,
                                                                         jintArray results_ref, jint resultsOffset, jint resultsCapacity)
{
	float frustum[6 * 4];
	int outputCount;
	int *pResults;
	float *pSphere;
	ArrayHelper mvp = ARRAY_HELPER(FLOAT_ARRAY, env, mvp_ref, mvpOffset, 16);
	ArrayHelper spheres = ARRAY_HELPER(FLOAT_ARRAY, env, spheres_ref, spheresOffset, spheresCount * 4);
	ArrayHelper results = ARRAY_HELPER(INT_ARRAY, env, results_ref, resultsOffset, resultsCapacity);

	bool initializedOK = check(&mvp) && check(&spheres) && check(&results);
	if (!initializedOK) {
		return -1;
	}

	bind(&mvp);
	bind(&spheres);
	bind(&results);

	computeFrustum((float *)mvp.mData, frustum);

	// Cull the spheres

	pSphere = (float *)spheres.mData;
	pResults = (int *)results.mData;
	outputCount = 0;
	for (int i = 0; i < spheresCount; i++, pSphere += 4) {
		if (sphereHitsFrustum(frustum, pSphere)) {
			if (outputCount < resultsCapacity) {
				*pResults++ = i;
			}
			outputCount++;
		}
	}
	commitChanges(&results);
	release(&mvp);
	release(&spheres);
	release(&results);
	return outputCount;
}

JNIEXPORT jint JNICALL Java_android_opengl_Visibility_visibilityTest(JNIEnv *env, jclass clazz,
                                                                     jfloatArray ws_ref, jint wsOffset,
                                                                     jfloatArray positions_ref, jint positionsOffset,
                                                                     jcharArray indices_ref, jint indicesOffset, jint indexCount)
{

	ArrayHelper ws = ARRAY_HELPER(FLOAT_ARRAY, env, ws_ref, wsOffset, 16);
	ArrayHelper positions = ARRAY_HELPER(FLOAT_ARRAY, env, positions_ref, positionsOffset, 0);
	ArrayHelper indices = ARRAY_HELPER(CHAR_ARRAY, env, indices_ref, indicesOffset, 0);

	bool checkOK = check(&ws) && check(&positions) && check(&indices);
	if (!checkOK) {
		// Return value will be ignored, because an exception has been thrown.
		return -1;
	}

	if (indices.mLength < indexCount) {
		throw_iae(env, "length < offset + indexCount");
		return -1;
	}

	bind(&ws);
	bind(&positions);
	bind(&indices);

	jint result = visibilityTest((float *)ws.mData,
	                             (float *)positions.mData, positions.mLength,
	                             (unsigned short *)indices.mData, indexCount);
	release(&ws);
	release(&positions);
	release(&indices);
	return result;
}
