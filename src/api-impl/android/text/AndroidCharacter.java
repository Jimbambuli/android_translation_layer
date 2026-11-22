package android.text;

import android.icu.lang.UCharacter;
import android.icu.lang.UCharacterCategory;
import android.icu.lang.UProperty;

/* AOSP AndroidCharacter: its natives (core/jni/android_text_AndroidCharacter.cpp) ask ICU; here ICU4J does */
@Deprecated
public class AndroidCharacter {
	public static final int EAST_ASIAN_WIDTH_NEUTRAL = 0;
	public static final int EAST_ASIAN_WIDTH_AMBIGUOUS = 1;
	public static final int EAST_ASIAN_WIDTH_HALF_WIDTH = 2;
	public static final int EAST_ASIAN_WIDTH_FULL_WIDTH = 3;
	public static final int EAST_ASIAN_WIDTH_NARROW = 4;
	public static final int EAST_ASIAN_WIDTH_WIDE = 5;

	private static final int PROPERTY_UNDEFINED = -1;
	private static final int JAVA_LANG_CHARACTER_MAX_DIRECTIONALITY = 18;
	// ICU => JDK mapping
	private static final byte[] DIRECTIONALITY_MAP = {0, 1, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 2, 16, 17, 18, 8, 9};

	private static boolean isPair(char[] src, int i, int end) {
		return src[i] >= 0xD800 && src[i] <= 0xDBFF && i + 1 < end && src[i + 1] >= 0xDC00 && src[i + 1] <= 0xDFFF;
	}

	private static int codePoint(char high, char low) {
		return 0x00010000 + ((high - 0xD800) << 10) + (low & 0x3FF);
	}

	private static byte directionality(int c) {
		int dir = UCharacter.getDirection(c);
		if (dir < 0 || dir > JAVA_LANG_CHARACTER_MAX_DIRECTIONALITY || UCharacter.getType(c) == UCharacterCategory.UNASSIGNED)
			return PROPERTY_UNDEFINED;
		return DIRECTIONALITY_MAP[dir];
	}

	private static byte eastAsianWidth(int c) {
		int width = UCharacter.getIntPropertyValue(c, UProperty.EAST_ASIAN_WIDTH);
		if (width < 0 || width > UCharacter.getIntPropertyMaxValue(UProperty.EAST_ASIAN_WIDTH))
			return PROPERTY_UNDEFINED;
		return (byte)width;
	}

	public static void getDirectionalities(char[] src, byte[] dest, int count) {
		if (src.length < count || dest.length < count)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = 0; i < count; i++) {
			if (isPair(src, i, count)) {
				byte dir = directionality(codePoint(src[i], src[i + 1]));
				dest[i++] = dir;
				dest[i] = dir;
			} else {
				dest[i] = directionality(src[i]);
			}
		}
	}

	public static int getEastAsianWidth(char input) {
		return eastAsianWidth(input);
	}

	public static void getEastAsianWidths(char[] src, int start, int count, byte[] dest) {
		if (start < 0 || start > start + count || src.length < start + count || dest.length < count)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = 0; i < count; i++) {
			int srci = start + i;
			if (src[srci] >= 0xD800 && src[srci] <= 0xDBFF && i + 1 < count && src[srci + 1] >= 0xDC00 && src[srci + 1] <= 0xDFFF) {
				byte width = eastAsianWidth(codePoint(src[srci], src[srci + 1]));
				dest[i++] = width;
				dest[i] = width;
			} else {
				dest[i] = eastAsianWidth(src[srci]);
			}
		}
	}

	public static boolean mirror(char[] text, int start, int count) {
		if (start < 0 || start > start + count || text.length < start + count)
			throw new ArrayIndexOutOfBoundsException();
		boolean ret = false;
		for (int i = start; i < start + count; i++) {
			// XXX this thinks it knows that surrogates are never mirrored
			int c1 = text[i];
			int c2 = UCharacter.getMirror(c1);
			if (c1 != c2) {
				text[i] = (char)c2;
				ret = true;
			}
		}
		return ret;
	}

	public static char getMirror(char ch) {
		return (char)UCharacter.getMirror(ch);
	}
}
