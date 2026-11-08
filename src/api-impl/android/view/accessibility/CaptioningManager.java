package android.view.accessibility;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Contains methods for accessing and monitoring preferred video captioning state and visual properties (AOSP
 * CaptioningManager). ATL has no system caption settings: captioning is off, the font scale 1, no preferred locale, and
 * the style is the one AOSP uses without a user setting (white on black).
 */
public class CaptioningManager {
	private final ArrayList<CaptioningChangeListener> mListeners = new ArrayList<CaptioningChangeListener>();

	public boolean isEnabled() {
		return false;
	}

	public float getFontScale() {
		return 1.0f;
	}

	/** AOSP: the preferred caption locale, null if the user chose none */
	public Locale getLocale() {
		return null;
	}

	public CaptionStyle getUserStyle() {
		return CaptionStyle.DEFAULT;
	}

	public boolean isCallCaptioningEnabled() {
		return false;
	}

	public boolean isSystemAudioCaptioningEnabled() {
		return false;
	}

	public boolean isSystemAudioCaptioningUiEnabled() {
		return false;
	}

	public static final class CaptionStyle {
		private static final int COLOR_NONE_OPAQUE = 0x000000FF;
		public static final int COLOR_UNSPECIFIED = 0x00FFFFFF;
		private static final CaptionStyle WHITE_ON_BLACK;
		private static final CaptionStyle BLACK_ON_WHITE;
		private static final CaptionStyle YELLOW_ON_BLACK;
		private static final CaptionStyle YELLOW_ON_BLUE;
		private static final CaptionStyle UNSPECIFIED;
		public static final CaptionStyle DEFAULT;
		public static final CaptionStyle[] PRESETS;
		public static final int PRESET_CUSTOM = -1;
		public static final int EDGE_TYPE_UNSPECIFIED = -1;
		public static final int EDGE_TYPE_NONE = 0;
		public static final int EDGE_TYPE_OUTLINE = 1;
		public static final int EDGE_TYPE_DROP_SHADOW = 2;
		public static final int EDGE_TYPE_RAISED = 3;
		public static final int EDGE_TYPE_DEPRESSED = 4;

		public final int foregroundColor;
		public final int backgroundColor;
		public final int edgeType;
		public final int edgeColor;
		public final int windowColor;
		public final String mRawTypeface;

		private final boolean mHasForegroundColor;
		private final boolean mHasBackgroundColor;
		private final boolean mHasEdgeType;
		private final boolean mHasEdgeColor;
		private final boolean mHasWindowColor;
		private Typeface mParsedTypeface;

		private CaptionStyle(int foregroundColor, int backgroundColor, int edgeType, int edgeColor, int windowColor, String rawTypeface) {
			mHasForegroundColor = hasColor(foregroundColor);
			mHasBackgroundColor = hasColor(backgroundColor);
			mHasEdgeType = edgeType != EDGE_TYPE_UNSPECIFIED;
			mHasEdgeColor = hasColor(edgeColor);
			mHasWindowColor = hasColor(windowColor);
			// Always use valid colors, even when no override is specified, as AOSP does
			this.foregroundColor = mHasForegroundColor ? foregroundColor : Color.WHITE;
			this.backgroundColor = mHasBackgroundColor ? backgroundColor : Color.BLACK;
			this.edgeType = mHasEdgeType ? edgeType : EDGE_TYPE_NONE;
			this.edgeColor = mHasEdgeColor ? edgeColor : Color.BLACK;
			this.windowColor = mHasWindowColor ? windowColor : COLOR_NONE_OPAQUE;
			mRawTypeface = rawTypeface;
		}

		public static boolean hasColor(int packedColor) {
			// "Default" packed colors are indicated by zero alpha and non-zero red/blue.
			return (packedColor >>> 24) != 0 || (packedColor & 0xFFFF00) == 0;
		}

		public CaptionStyle applyStyle(CaptionStyle overlay) {
			final int newForegroundColor = overlay.hasForegroundColor() ? overlay.foregroundColor : foregroundColor;
			final int newBackgroundColor = overlay.hasBackgroundColor() ? overlay.backgroundColor : backgroundColor;
			final int newEdgeType = overlay.hasEdgeType() ? overlay.edgeType : edgeType;
			final int newEdgeColor = overlay.hasEdgeColor() ? overlay.edgeColor : edgeColor;
			final int newWindowColor = overlay.hasWindowColor() ? overlay.windowColor : windowColor;
			final String newRawTypeface = overlay.mRawTypeface != null ? overlay.mRawTypeface : mRawTypeface;
			return new CaptionStyle(newForegroundColor, newBackgroundColor, newEdgeType, newEdgeColor, newWindowColor, newRawTypeface);
		}

		public boolean hasBackgroundColor() {
			return mHasBackgroundColor;
		}

		public boolean hasForegroundColor() {
			return mHasForegroundColor;
		}

		public boolean hasEdgeType() {
			return mHasEdgeType;
		}

		public boolean hasEdgeColor() {
			return mHasEdgeColor;
		}

		public boolean hasWindowColor() {
			return mHasWindowColor;
		}

		public Typeface getTypeface() {
			if (mParsedTypeface == null && !TextUtils.isEmpty(mRawTypeface)) {
				mParsedTypeface = Typeface.create(mRawTypeface, Typeface.NORMAL);
			}
			return mParsedTypeface;
		}

		static {
			WHITE_ON_BLACK = new CaptionStyle(Color.WHITE, Color.BLACK, EDGE_TYPE_NONE, Color.BLACK, COLOR_NONE_OPAQUE, null);
			BLACK_ON_WHITE = new CaptionStyle(Color.BLACK, Color.WHITE, EDGE_TYPE_NONE, Color.BLACK, COLOR_NONE_OPAQUE, null);
			YELLOW_ON_BLACK = new CaptionStyle(Color.YELLOW, Color.BLACK, EDGE_TYPE_NONE, Color.BLACK, COLOR_NONE_OPAQUE, null);
			YELLOW_ON_BLUE = new CaptionStyle(Color.YELLOW, Color.BLUE, EDGE_TYPE_NONE, Color.BLACK, COLOR_NONE_OPAQUE, null);
			UNSPECIFIED = new CaptionStyle(COLOR_UNSPECIFIED, COLOR_UNSPECIFIED, EDGE_TYPE_UNSPECIFIED, COLOR_UNSPECIFIED, COLOR_UNSPECIFIED, null);
			// The ordering of these cannot change since AOSP stores the index directly in preferences.
			PRESETS = new CaptionStyle[] {WHITE_ON_BLACK, BLACK_ON_WHITE, YELLOW_ON_BLACK, YELLOW_ON_BLUE, UNSPECIFIED};
			DEFAULT = WHITE_ON_BLACK;
		}
	}

	public static abstract class CaptioningChangeListener {
		public void onEnabledChanged(boolean enabled) {}
		public void onUserStyleChanged(CaptionStyle userStyle) {}
		public void onLocaleChanged(Locale locale) {}
		public void onFontScaleChanged(float fontScale) {}
		public void onSystemAudioCaptioningChanged(boolean enabled) {}
		public void onSystemAudioCaptioningUiChanged(boolean enabled) {}
	}

	/* the listeners are kept; the settings never change here, so they are never called */
	public void addCaptioningChangeListener(CaptioningChangeListener listener) {
		synchronized (mListeners) {
			if (!mListeners.contains(listener))
				mListeners.add(listener);
		}
	}

	public void removeCaptioningChangeListener(CaptioningChangeListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}
}
