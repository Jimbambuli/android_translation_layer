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

package android.graphics.text;

import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Result of text shaping of the single style text. The text is measured with the paint of its run, as
 * Paint.measureText() does for it.
 *
 * ATL: AOSP shapes the text with minikin and keeps the glyph advances; here the text and the paint of each run are
 * kept and measured when asked.
 */
public class MeasuredText {
	private final char[] mChars;
	private final int[] mRunEnds;
	private final Paint[] mRunPaints;
	private final float[] mRunReplacementWidths; /* NaN: a style run */
	private final boolean mComputeLayout;
	private final boolean mComputeHyphenation;

	private MeasuredText(char[] chars, int[] runEnds, Paint[] paints, float[] replacementWidths, boolean computeLayout, boolean computeHyphenation) {
		mComputeHyphenation = computeHyphenation;
		mChars = chars;
		mRunEnds = runEnds;
		mRunPaints = paints;
		mRunReplacementWidths = replacementWidths;
		mComputeLayout = computeLayout;
	}

	/** The text, for Canvas.drawTextRun(MeasuredText, ...) */
	public char[] getChars() {
		return mChars;
	}

	private void check(int start, int end) {
		if (start < 0 || end > mChars.length || start > end)
			throw new IllegalArgumentException("Invalid start or end offset: start=" + start + ", end=" + end + ", length=" + mChars.length);
	}

	/** Returns the width of a given range. */
	public float getWidth(int start, int end) {
		check(start, end);
		float width = 0;
		int runStart = 0;
		for (int i = 0; i < mRunEnds.length; i++) {
			int s = Math.max(start, runStart), e = Math.min(end, mRunEnds[i]);
			if (s < e) {
				if (Float.isNaN(mRunReplacementWidths[i])) {
					width += mRunPaints[i].measureText(mChars, s, e - s);
				} else {
					width += mRunReplacementWidths[i] * (e - s) / (mRunEnds[i] - runStart);
				}
			}
			runStart = mRunEnds[i];
		}
		return width;
	}

	/** Returns the bounding box of the text in the range, from the origin. */
	public void getBounds(int start, int end, Rect rect) {
		check(start, end);
		if (rect == null)
			throw new NullPointerException("rect can not be null");
		rect.set(0, 0, 0, 0);
		int runStart = 0;
		float x = 0;
		Rect run = new Rect();
		for (int i = 0; i < mRunEnds.length; i++) {
			int s = Math.max(start, runStart), e = Math.min(end, mRunEnds[i]);
			if (s < e && Float.isNaN(mRunReplacementWidths[i])) {
				String part = new String(mChars, s, e - s);
				mRunPaints[i].getTextBounds(part, 0, part.length(), run);
				run.offset((int)x, 0);
				if (rect.isEmpty()) {
					rect.set(run);
				} else {
					rect.union(run);
				}
				x += mRunPaints[i].measureText(mChars, s, e - s);
			}
			runStart = mRunEnds[i];
		}
	}

	/** Returns the width of the character at the offset. */
	public float getCharWidthAt(int offset) {
		if (offset < 0 || offset >= mChars.length)
			throw new IndexOutOfBoundsException("offset(" + offset + ") is larger than text length: " + mChars.length);
		return getWidth(offset, offset + 1);
	}

	/** Returns the size of native MeasuredText memory usage: none here */
	public int getMemoryUsage() {
		return 0;
	}

	/** Helper class for creating a MeasuredText. */
	public static final class Builder {
		private final char[] mText;
		private int mCurrentOffset = 0;
		private boolean mComputeHyphenation = false;
		private boolean mComputeLayout = true;
		private final java.util.ArrayList<Integer> mEnds = new java.util.ArrayList<>();
		private final java.util.ArrayList<Paint> mPaints = new java.util.ArrayList<>();
		private final java.util.ArrayList<Float> mWidths = new java.util.ArrayList<>();

		/** Construct a builder. The text will be copied internally. */
		public Builder(char[] text) {
			if (text == null)
				throw new NullPointerException("text can not be null");
			mText = text.clone();
		}

		/* AOSP Builder(MeasuredText): a builder for the same text (AOSP reuses the measured runs as a hint; ATL
		 * measures when asked, so only the text and the settings are taken) */
		public Builder(MeasuredText text) {
			if (text == null)
				throw new NullPointerException("text can not be null");
			if (!text.mComputeLayout)
				throw new IllegalArgumentException("The input MeasuredText must not be created with setComputeLayout(false).");
			mText = text.mChars.clone();
			mComputeHyphenation = text.mComputeHyphenation;
			mComputeLayout = text.mComputeLayout;
		}

		/* AOSP appendStyleRun with a LineBreakConfig; ATL does not keep the line break settings of a run */
		public Builder appendStyleRun(Paint paint, LineBreakConfig lineBreakConfig, int length, boolean isRtl) {
			return appendStyleRun(paint, length, isRtl);
		}

		/** Apply the style run to the text from the current offset to length characters on. */
		public Builder appendStyleRun(Paint paint, int length, boolean isRtl) {
			if (paint == null)
				throw new NullPointerException("paint can not be null");
			if (length <= 0)
				throw new IllegalArgumentException("length can not be negative");
			int end = mCurrentOffset + length;
			if (end > mText.length)
				throw new IllegalArgumentException("Style exceeds the text length");
			mEnds.add(end);
			mPaints.add(paint);
			mWidths.add(Float.NaN);
			mCurrentOffset = end;
			return this;
		}

		/** Used to inform the layout engine that the replacement width is given. */
		public Builder appendReplacementRun(Paint paint, int length, float width) {
			if (paint == null)
				throw new NullPointerException("paint can not be null");
			if (length <= 0)
				throw new IllegalArgumentException("length can not be negative");
			int end = mCurrentOffset + length;
			if (end > mText.length)
				throw new IllegalArgumentException("Replacement exceeds the text length");
			mEnds.add(end);
			mPaints.add(paint);
			mWidths.add(width);
			mCurrentOffset = end;
			return this;
		}

		public static final int HYPHENATION_MODE_NONE = 0;
		public static final int HYPHENATION_MODE_NORMAL = 1;
		public static final int HYPHENATION_MODE_FAST = 2;

		/* AOSP setComputeHyphenation(int): NORMAL and FAST both compute hyphenation (ATL has no fast variant) */
		public Builder setComputeHyphenation(int mode) {
			switch (mode) {
				case HYPHENATION_MODE_NORMAL:
				case HYPHENATION_MODE_FAST:
					mComputeHyphenation = true;
					break;
				case HYPHENATION_MODE_NONE:
				default:
					mComputeHyphenation = false;
					break;
			}
			return this;
		}

		/** By passing true to this method, the build method will compute all possible hyphenation pieces as well. */
		public Builder setComputeHyphenation(boolean computeHyphenation) {
			mComputeHyphenation = computeHyphenation;
			return this;
		}

		/** By passing true to this method, the build method will calculate all full layout information. */
		public Builder setComputeLayout(boolean computeLayout) {
			mComputeLayout = computeLayout;
			return this;
		}

		/** Creates a MeasuredText. */
		public MeasuredText build() {
			if (mCurrentOffset != mText.length)
				throw new IllegalStateException("Style info has not been provided for all text.");
			int[] ends = new int[mEnds.size()];
			Paint[] paints = new Paint[ends.length];
			float[] widths = new float[ends.length];
			for (int i = 0; i < ends.length; i++) {
				ends[i] = mEnds.get(i);
				paints[i] = mPaints.get(i);
				widths[i] = mWidths.get(i);
			}
			return new MeasuredText(mText, ends, paints, widths, mComputeLayout, mComputeHyphenation);
		}
	}
}
