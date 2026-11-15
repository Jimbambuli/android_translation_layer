/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.graphics;

/**
 * Gainmap metadata plus contents, after AOSP android-16.0.0_r4
 * graphics/java/android/graphics/Gainmap.java. AOSP keeps the metadata in a native
 * SkGainmapInfo (libs/hwui/jni/Gainmap.cpp); ATL keeps it in Java fields with the same
 * defaults as SkGainmapInfo. ATL does not render gainmaps, the base image is drawn as SDR.
 */
public final class Gainmap {

	public static final int GAINMAP_DIRECTION_SDR_TO_HDR = 0;
	public static final int GAINMAP_DIRECTION_HDR_TO_SDR = 1;

	private Bitmap mGainmapContents;
	/* SkGainmapInfo defaults */
	private float[] mRatioMin = {1f, 1f, 1f};
	private float[] mRatioMax = {2f, 2f, 2f};
	private float[] mGamma = {1f, 1f, 1f};
	private float[] mEpsilonSdr = {0f, 0f, 0f};
	private float[] mEpsilonHdr = {0f, 0f, 0f};
	private float mDisplayRatioSdr = 1f;
	private float mDisplayRatioHdr = 2f;
	private ColorSpace mAlternativeImagePrimaries;
	private int mDirection = GAINMAP_DIRECTION_SDR_TO_HDR;

	public Gainmap(Bitmap gainmapContents) {
		setGainmapContents(gainmapContents);
	}

	/* AOSP nCreateCopy: copies the metadata, takes the new contents */
	public Gainmap(Gainmap gainmap, Bitmap gainmapContents) {
		mRatioMin = gainmap.mRatioMin.clone();
		mRatioMax = gainmap.mRatioMax.clone();
		mGamma = gainmap.mGamma.clone();
		mEpsilonSdr = gainmap.mEpsilonSdr.clone();
		mEpsilonHdr = gainmap.mEpsilonHdr.clone();
		mDisplayRatioSdr = gainmap.mDisplayRatioSdr;
		mDisplayRatioHdr = gainmap.mDisplayRatioHdr;
		mAlternativeImagePrimaries = gainmap.mAlternativeImagePrimaries;
		mDirection = gainmap.mDirection;
		setGainmapContents(gainmapContents);
	}

	public Bitmap getGainmapContents() {
		return mGainmapContents;
	}

	public void setGainmapContents(Bitmap bitmap) {
		if (bitmap.isRecycled())
			throw new IllegalArgumentException("Bitmap is recycled");
		mGainmapContents = bitmap;
	}

	public void setRatioMin(float r, float g, float b) {
		mRatioMin = new float[] {r, g, b};
	}

	public float[] getRatioMin() {
		return mRatioMin.clone();
	}

	public void setRatioMax(float r, float g, float b) {
		mRatioMax = new float[] {r, g, b};
	}

	public float[] getRatioMax() {
		return mRatioMax.clone();
	}

	public void setGamma(float r, float g, float b) {
		mGamma = new float[] {r, g, b};
	}

	public float[] getGamma() {
		return mGamma.clone();
	}

	public void setEpsilonSdr(float r, float g, float b) {
		mEpsilonSdr = new float[] {r, g, b};
	}

	public float[] getEpsilonSdr() {
		return mEpsilonSdr.clone();
	}

	public void setEpsilonHdr(float r, float g, float b) {
		mEpsilonHdr = new float[] {r, g, b};
	}

	public float[] getEpsilonHdr() {
		return mEpsilonHdr.clone();
	}

	public void setDisplayRatioForFullHdr(float max) {
		if (!Float.isFinite(max) || max < 1f) {
			throw new IllegalArgumentException(
			    "setDisplayRatioForFullHdr must be >= 1.0f, got = " + max);
		}
		mDisplayRatioHdr = max;
	}

	public float getDisplayRatioForFullHdr() {
		return mDisplayRatioHdr;
	}

	public void setMinDisplayRatioForHdrTransition(float min) {
		if (!Float.isFinite(min) || min < 1f) {
			throw new IllegalArgumentException(
			    "setMinDisplayRatioForHdrTransition must be >= 1.0f, got = " + min);
		}
		mDisplayRatioSdr = min;
	}

	public float getMinDisplayRatioForHdrTransition() {
		return mDisplayRatioSdr;
	}

	public void setAlternativeImagePrimaries(ColorSpace colorSpace) {
		mAlternativeImagePrimaries = colorSpace;
	}

	public ColorSpace getAlternativeImagePrimaries() {
		return mAlternativeImagePrimaries;
	}

	public void setGainmapDirection(int direction) {
		if (direction != GAINMAP_DIRECTION_SDR_TO_HDR
		    && direction != GAINMAP_DIRECTION_HDR_TO_SDR) {
			throw new IllegalArgumentException("Invalid gainmap direction: " + direction);
		}
		mDirection = direction;
	}

	public int getGainmapDirection() {
		return mDirection;
	}
}
