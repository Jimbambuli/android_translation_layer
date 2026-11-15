package android.graphics.fonts;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/* AOSP frameworks/base graphics/java/android/graphics/fonts/FontVariationAxis.java (android-14) */
public final class FontVariationAxis {
	private final int mTag;
	private final String mTagString;
	private final float mStyleValue;

	public FontVariationAxis(String tagString, float styleValue) {
		if (!isValidTag(tagString)) {
			throw new IllegalArgumentException("Illegal tag pattern: " + tagString);
		}
		mTag = makeTag(tagString);
		mTagString = tagString;
		mStyleValue = styleValue;
	}

	/** @hide */
	public int getOpenTypeTagValue() {
		return mTag;
	}

	public String getTag() {
		return mTagString;
	}

	public float getStyleValue() {
		return mStyleValue;
	}

	@Override
	public String toString() {
		return "'" + mTagString + "' " + Float.toString(mStyleValue);
	}

	private static final Pattern TAG_PATTERN = Pattern.compile("[ -~]{4}");

	private static boolean isValidTag(String tagString) {
		return tagString != null && TAG_PATTERN.matcher(tagString).matches();
	}

	/** @hide */
	public static int makeTag(String tagString) {
		final char c1 = tagString.charAt(0);
		final char c2 = tagString.charAt(1);
		final char c3 = tagString.charAt(2);
		final char c4 = tagString.charAt(3);
		return (c1 << 24) | (c2 << 16) | (c3 << 8) | c4;
	}

	/* null if the settings name no axis, like AOSP */
	public static FontVariationAxis[] fromFontVariationSettings(String settings) {
		if (settings == null || settings.isEmpty()) {
			return null;
		}
		final ArrayList<FontVariationAxis> axisList = new ArrayList<>();
		final int length = settings.length();
		for (int i = 0; i < length; i++) {
			final char c = settings.charAt(i);
			if (Character.isWhitespace(c)) {
				continue;
			}
			if (!(c == '\'' || c == '"') || length < i + 6 || settings.charAt(i + 5) != c) {
				throw new IllegalArgumentException(
				    "Tag should be wrapped with double or single quote: " + settings);
			}
			final String tagString = settings.substring(i + 1, i + 5);
			i += 6; // Move to end of tag.
			int endOfValueString = settings.indexOf(',', i);
			if (endOfValueString == -1) {
				endOfValueString = length;
			}
			final float value;
			try {
				value = Float.parseFloat(settings.substring(i, endOfValueString));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
				    "Failed to parse float string: " + e.getMessage());
			}
			axisList.add(new FontVariationAxis(tagString, value));
			i = endOfValueString;
		}
		if (axisList.isEmpty()) {
			return null;
		}
		return axisList.toArray(new FontVariationAxis[0]);
	}

	public static String toFontVariationSettings(FontVariationAxis[] axes) {
		if (axes == null) {
			return "";
		}
		return TextUtils.join(",", axes);
	}

	/** @hide */
	public static String toFontVariationSettings(List<FontVariationAxis> axes) {
		if (axes == null) {
			return "";
		}
		return TextUtils.join(",", axes);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || !(o instanceof FontVariationAxis)) {
			return false;
		}
		FontVariationAxis axis = (FontVariationAxis)o;
		return axis.mTag == mTag && axis.mStyleValue == mStyleValue;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mTag, mStyleValue);
	}
}
