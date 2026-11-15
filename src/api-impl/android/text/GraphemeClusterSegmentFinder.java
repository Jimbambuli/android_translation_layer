/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.text;

import java.text.BreakIterator;

/**
 * Implementation of {@code SegmentFinder} using grapheme clusters as the text segment. Whitespace
 * characters are also considered grapheme clusters.
 *
 * AOSP core/java/android/text/GraphemeClusterSegmentFinder.java (android-16). AOSP marks the breaks with
 * minikin's GraphemeBreak.isGraphemeBreak() over the advances of the paint; ATL takes the grapheme cluster
 * boundaries of the character BreakIterator, the same ones Paint.getTextRunCursor() moves the cursor by.
 * The paint is not needed for that.
 */
public class GraphemeClusterSegmentFinder extends SegmentFinder {
	private final boolean[] mIsGraphemeBreak;

	/**
	 * Constructs a GraphemeClusterSegmentFinder instance for the specified text which uses the
	 * provided TextPaint to determine grapheme cluster boundaries.
	 *
	 * @param text text to be segmented
	 * @param textPaint TextPaint used to draw the text
	 */
	public GraphemeClusterSegmentFinder(CharSequence text, TextPaint textPaint) {
		mIsGraphemeBreak = new boolean[text.length()];
		BreakIterator it = BreakIterator.getCharacterInstance();
		it.setText(text.toString());
		for (int b = it.first(); b != BreakIterator.DONE; b = it.next()) {
			if (b < mIsGraphemeBreak.length)
				mIsGraphemeBreak[b] = true;
		}
	}

	private int previousBoundary(int offset) {
		if (offset <= 0) return DONE;
		do {
			--offset;
		} while (offset > 0 && !mIsGraphemeBreak[offset]);
		return offset;
	}

	private int nextBoundary(int offset) {
		if (offset >= mIsGraphemeBreak.length) return DONE;
		do {
			++offset;
		} while (offset < mIsGraphemeBreak.length && !mIsGraphemeBreak[offset]);
		return offset;
	}

	@Override
	public int previousStartBoundary(int offset) {
		return previousBoundary(offset);
	}

	@Override
	public int previousEndBoundary(int offset) {
		if (offset == 0) return DONE;
		int boundary = previousBoundary(offset);
		// Check that there is another cursor position before, otherwise this is not a valid
		// end boundary.
		if (boundary == DONE || previousBoundary(boundary) == DONE) {
			return DONE;
		}
		return boundary;
	}

	@Override
	public int nextStartBoundary(int offset) {
		if (offset == mIsGraphemeBreak.length) return DONE;
		int boundary = nextBoundary(offset);
		// Check that there is another cursor position after, otherwise this is not a valid
		// start boundary.
		if (boundary == DONE || nextBoundary(boundary) == DONE) {
			return DONE;
		}
		return boundary;
	}

	@Override
	public int nextEndBoundary(int offset) {
		return nextBoundary(offset);
	}
}
