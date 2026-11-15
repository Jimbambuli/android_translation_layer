package android.graphics.text;

import android.graphics.fonts.Font;
import com.android.internal.util.Preconditions;
import java.util.Objects;

/**
 * Text shaping result object for single style text (AOSP android.graphics.text.PositionedGlyphs).
 * AOSP reads the glyphs from the native minikin Layout; ATL's TextRunShaper hands them over as arrays
 * (see android_graphics_text_TextRunShaper.c for the layout of the native result).
 */
public final class PositionedGlyphs {
	public static final float NO_OVERRIDE = Float.MIN_VALUE;

	private final float mXOffset;
	private final float mYOffset;
	private final int[] mGlyphIds;
	private final float[] mPositions;
	private final float mAdvance;
	private final float mAscent;
	private final float mDescent;
	private final int[] mFontIndex;
	private final Font[] mFonts;
	private final boolean[] mFakeBold;
	private final boolean[] mFakeItalic;

	/** @hide */
	PositionedGlyphs(Object[] shaped, float xOffset, float yOffset) {
		mXOffset = xOffset;
		mYOffset = yOffset;
		mGlyphIds = (int[])shaped[0];
		mPositions = (float[])shaped[1];
		float[] metrics = (float[])shaped[2];
		mAdvance = metrics[0];
		mAscent = metrics[1];
		mDescent = metrics[2];
		mFontIndex = (int[])shaped[3];
		String[] families = (String[])shaped[4];
		String[] files = (String[])shaped[5];
		int[] styles = (int[])shaped[6];
		mFonts = new Font[families.length];
		mFakeBold = new boolean[families.length];
		mFakeItalic = new boolean[families.length];
		for (int i = 0; i < families.length; i++) {
			mFonts[i] = Font.forShapedText(families[i], styles[i * 5], styles[i * 5 + 1], styles[i * 5 + 2], files[i]);
			mFakeBold[i] = styles[i * 5 + 3] != 0;
			mFakeItalic[i] = styles[i * 5 + 4] != 0;
		}
	}

	public float getAdvance() {
		return mAdvance;
	}

	public float getAscent() {
		return mAscent;
	}

	public float getDescent() {
		return mDescent;
	}

	public float getOffsetX() {
		return mXOffset;
	}

	public float getOffsetY() {
		return mYOffset;
	}

	public int glyphCount() {
		return mGlyphIds.length;
	}

	private void checkIndex(int index) {
		Preconditions.checkArgumentInRange(index, 0, glyphCount() - 1, "index");
	}

	public Font getFont(int index) {
		checkIndex(index);
		return mFonts[mFontIndex[index]];
	}

	public int getGlyphId(int index) {
		checkIndex(index);
		return mGlyphIds[index];
	}

	public float getGlyphX(int index) {
		checkIndex(index);
		return mPositions[index * 2] + mXOffset;
	}

	public float getGlyphY(int index) {
		checkIndex(index);
		return mPositions[index * 2 + 1] + mYOffset;
	}

	/* fontconfig's synthetic emboldening of the glyph's font (minikin: the FakedFont's fakery) */
	public boolean getFakeBold(int index) {
		checkIndex(index);
		return mFakeBold[mFontIndex[index]];
	}

	/* fontconfig's synthetic oblique of the glyph's font */
	public boolean getFakeItalic(int index) {
		checkIndex(index);
		return mFakeItalic[mFontIndex[index]];
	}

	/* ATL does not apply font variation overrides while shaping, so there is none */
	public float getWeightOverride(int index) {
		checkIndex(index);
		return NO_OVERRIDE;
	}

	public float getItalicOverride(int index) {
		checkIndex(index);
		return NO_OVERRIDE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PositionedGlyphs)) return false;
		PositionedGlyphs that = (PositionedGlyphs)o;
		if (mXOffset != that.mXOffset || mYOffset != that.mYOffset) return false;
		if (glyphCount() != that.glyphCount()) return false;
		for (int i = 0; i < glyphCount(); ++i) {
			if (getGlyphId(i) != that.getGlyphId(i)) return false;
			if (getGlyphX(i) != that.getGlyphX(i)) return false;
			if (getGlyphY(i) != that.getGlyphY(i)) return false;
			if (!getFont(i).equals(that.getFont(i))) return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = Objects.hash(mXOffset, mYOffset);
		for (int i = 0; i < glyphCount(); ++i) {
			hashCode = Objects.hash(hashCode, getGlyphId(i), getGlyphX(i), getGlyphY(i), getFont(i));
		}
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < glyphCount(); ++i) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append("[ ID = " + getGlyphId(i) + ","
			          + " pos = (" + getGlyphX(i) + "," + getGlyphY(i) + ")"
			          + " font = " + getFont(i) + " ]");
		}
		sb.append("]");
		return "PositionedGlyphs{"
		    + "glyphs = " + sb.toString()
		    + ", mXOffset=" + mXOffset
		    + ", mYOffset=" + mYOffset
		    + '}';
	}
}
