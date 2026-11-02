package android.app;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides information about the colors of a wallpaper.
 *
 * ATL has no wallpaper service, WallpaperManager.getWallpaperColors() returns null, so only the
 * public constructors and getters of AOSP's class are here, without color extraction.
 */
public final class WallpaperColors {
	public static final int HINT_SUPPORTS_DARK_TEXT = 1 << 0;
	public static final int HINT_SUPPORTS_DARK_THEME = 1 << 1;

	private final List<Color> mMainColors = new ArrayList<>(3);
	private final int mColorHints;

	public WallpaperColors(Color primaryColor, Color secondaryColor, Color tertiaryColor) {
		this(primaryColor, secondaryColor, tertiaryColor, 0);
	}

	public WallpaperColors(Color primaryColor, Color secondaryColor, Color tertiaryColor, int colorHints) {
		if (primaryColor == null) {
			throw new IllegalArgumentException("Primary color should never be null.");
		}
		mMainColors.add(primaryColor);
		if (secondaryColor != null) {
			mMainColors.add(secondaryColor);
		}
		if (tertiaryColor != null) {
			if (secondaryColor == null) {
				throw new IllegalArgumentException("tertiaryColor can't be specified when "
				                                   + "secondaryColor is null");
			}
			mMainColors.add(tertiaryColor);
		}
		mColorHints = colorHints;
	}

	public Color getPrimaryColor() {
		return mMainColors.get(0);
	}

	public Color getSecondaryColor() {
		return mMainColors.size() < 2 ? null : mMainColors.get(1);
	}

	public Color getTertiaryColor() {
		return mMainColors.size() < 3 ? null : mMainColors.get(2);
	}

	public List<Color> getMainColors() {
		return java.util.Collections.unmodifiableList(mMainColors);
	}

	public int getColorHints() {
		return mColorHints;
	}
}
