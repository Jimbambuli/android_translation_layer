package android.view;

import android.graphics.Insets;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the area of the display that is not functional for displaying content
 * (AOSP android.view.DisplayCutout, android-16.0.0_r4).
 *
 * ATL: the desktop window has no cutout, so the system only ever hands out NO_CUTOUT. Apps may build their own
 * instances (constructors, Builder) and those keep their safe insets, bounding rects and waterfall insets as in
 * AOSP. AOSP derives getCutoutPath() from the device's cutout specification; ATL has none, so the path is the one
 * given to the Builder (AOSP: the same Path through its cache), otherwise null.
 */
public final class DisplayCutout {
	public static final int BOUNDS_POSITION_LEFT = 0;
	public static final int BOUNDS_POSITION_TOP = 1;
	public static final int BOUNDS_POSITION_RIGHT = 2;
	public static final int BOUNDS_POSITION_BOTTOM = 3;
	public static final int BOUNDS_POSITION_LENGTH = 4;

	private static final Rect ZERO_RECT = new Rect();

	public static final DisplayCutout NO_CUTOUT = new DisplayCutout(ZERO_RECT, Insets.NONE,
	        new Rect[] {ZERO_RECT, ZERO_RECT, ZERO_RECT, ZERO_RECT}, null, false);

	private final Rect mSafeInsets;
	private final Insets mWaterfallInsets;
	private final Rect[] mBounds;
	private final Path mCutoutPath;

	public DisplayCutout(Insets safeInsets, Rect boundLeft, Rect boundTop, Rect boundRight, Rect boundBottom) {
		this(safeInsets.toRect(), Insets.NONE, new Rect[] {boundLeft, boundTop, boundRight, boundBottom}, null, true);
	}

	public DisplayCutout(Insets safeInsets, Rect boundLeft, Rect boundTop, Rect boundRight, Rect boundBottom,
	                     Insets waterfallInsets) {
		this(safeInsets.toRect(), waterfallInsets, new Rect[] {boundLeft, boundTop, boundRight, boundBottom}, null, true);
	}

	@Deprecated
	public DisplayCutout(Rect safeInsets, List<Rect> boundingRects) {
		this(safeInsets, Insets.NONE, extractBoundsFromList(safeInsets, boundingRects), null, true);
	}

	private DisplayCutout(Rect safeInsets, Insets waterfallInsets, Rect[] bounds, Path cutoutPath, boolean copyArguments) {
		mSafeInsets = getCopyOrRef(safeInsets, copyArguments);
		mWaterfallInsets = waterfallInsets == null ? Insets.NONE : waterfallInsets;
		mBounds = new Rect[BOUNDS_POSITION_LENGTH];
		for (int i = 0; i < BOUNDS_POSITION_LENGTH; ++i) {
			mBounds[i] = getCopyOrRef(bounds[i], copyArguments);
		}
		mCutoutPath = cutoutPath;
	}

	private static Rect getCopyOrRef(Rect r, boolean copyArguments) {
		if (r == null) {
			return ZERO_RECT;
		} else if (copyArguments) {
			return new Rect(r);
		} else {
			return r;
		}
	}

	/** @hide AOSP: sorts the bounding rects of the deprecated constructor to their edges */
	public static Rect[] extractBoundsFromList(Rect safeInsets, List<Rect> boundingRects) {
		Rect[] sortedBounds = new Rect[BOUNDS_POSITION_LENGTH];
		for (int i = 0; i < sortedBounds.length; ++i) {
			sortedBounds[i] = ZERO_RECT;
		}
		if (safeInsets != null && boundingRects != null) {
			final boolean topBottomInset = safeInsets.top > 0 || safeInsets.bottom > 0;
			for (Rect bound : boundingRects) {
				if (topBottomInset) {
					if (bound.top == 0) {
						sortedBounds[BOUNDS_POSITION_TOP] = bound;
					} else {
						sortedBounds[BOUNDS_POSITION_BOTTOM] = bound;
					}
				} else {
					if (bound.left == 0) {
						sortedBounds[BOUNDS_POSITION_LEFT] = bound;
					} else {
						sortedBounds[BOUNDS_POSITION_RIGHT] = bound;
					}
				}
			}
		}
		return sortedBounds;
	}

	public Insets getWaterfallInsets() {
		return mWaterfallInsets;
	}

	/** @hide */
	public boolean isBoundsEmpty() {
		for (Rect rect : mBounds) {
			if (!rect.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** @hide */
	public boolean isEmpty() {
		return mSafeInsets.equals(ZERO_RECT);
	}

	public int getSafeInsetTop() {
		return mSafeInsets.top;
	}

	public int getSafeInsetBottom() {
		return mSafeInsets.bottom;
	}

	public int getSafeInsetLeft() {
		return mSafeInsets.left;
	}

	public int getSafeInsetRight() {
		return mSafeInsets.right;
	}

	/** @hide */
	public Rect getSafeInsets() {
		return new Rect(mSafeInsets);
	}

	public List<Rect> getBoundingRects() {
		List<Rect> result = new ArrayList<>();
		for (Rect bound : mBounds) {
			if (!bound.isEmpty()) {
				result.add(new Rect(bound));
			}
		}
		return result;
	}

	/** @hide */
	public Rect[] getBoundingRectsAll() {
		Rect[] rects = new Rect[BOUNDS_POSITION_LENGTH];
		for (int i = 0; i < BOUNDS_POSITION_LENGTH; ++i) {
			rects[i] = new Rect(mBounds[i]);
		}
		return rects;
	}

	public Rect getBoundingRectLeft() {
		return new Rect(mBounds[BOUNDS_POSITION_LEFT]);
	}

	public Rect getBoundingRectTop() {
		return new Rect(mBounds[BOUNDS_POSITION_TOP]);
	}

	public Rect getBoundingRectRight() {
		return new Rect(mBounds[BOUNDS_POSITION_RIGHT]);
	}

	public Rect getBoundingRectBottom() {
		return new Rect(mBounds[BOUNDS_POSITION_BOTTOM]);
	}

	/** AOSP: the cutout's shape, null when the display has none (ATL: only a Builder-given path) */
	public Path getCutoutPath() {
		if (mCutoutPath == null || mCutoutPath.isEmpty()) {
			return null;
		}
		return mCutoutPath;
	}

	/** @hide AOSP DisplayCutout.inset() */
	public DisplayCutout inset(int insetLeft, int insetTop, int insetRight, int insetBottom) {
		if (insetLeft == 0 && insetTop == 0 && insetRight == 0 && insetBottom == 0
		    || (isBoundsEmpty() && mWaterfallInsets.equals(Insets.NONE))) {
			return this;
		}
		Rect safeInsets = insetInsets(insetLeft, insetTop, insetRight, insetBottom, new Rect(mSafeInsets));
		if (insetLeft == 0 && insetTop == 0 && mSafeInsets.equals(safeInsets)) {
			return this;
		}
		Rect waterfallInsets = insetInsets(insetLeft, insetTop, insetRight, insetBottom, mWaterfallInsets.toRect());
		Rect[] bounds = getBoundingRectsAll();
		for (int i = 0; i < bounds.length; ++i) {
			if (!bounds[i].equals(ZERO_RECT)) {
				bounds[i].offset(-insetLeft, -insetTop);
			}
		}
		return new DisplayCutout(safeInsets, Insets.of(waterfallInsets), bounds, mCutoutPath, false);
	}

	private static Rect insetInsets(int insetLeft, int insetTop, int insetRight, int insetBottom, Rect insets) {
		if (insetTop > 0 || insets.top > 0) {
			insets.top = atLeastZero(insets.top - insetTop);
		}
		if (insetBottom > 0 || insets.bottom > 0) {
			insets.bottom = atLeastZero(insets.bottom - insetBottom);
		}
		if (insetLeft > 0 || insets.left > 0) {
			insets.left = atLeastZero(insets.left - insetLeft);
		}
		if (insetRight > 0 || insets.right > 0) {
			insets.right = atLeastZero(insets.right - insetRight);
		}
		return insets;
	}

	private static int atLeastZero(int value) {
		return value < 0 ? 0 : value;
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 48271 * result + mSafeInsets.hashCode();
		result = 48271 * result + Arrays.hashCode(mBounds);
		result = 48271 * result + mWaterfallInsets.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof DisplayCutout) {
			DisplayCutout c = (DisplayCutout)o;
			return mSafeInsets.equals(c.mSafeInsets) && Arrays.equals(mBounds, c.mBounds)
			    && mWaterfallInsets.equals(c.mWaterfallInsets) && mCutoutPath == c.mCutoutPath;
		}
		return false;
	}

	@Override
	public String toString() {
		return "DisplayCutout{insets=" + mSafeInsets + " waterfall=" + mWaterfallInsets
		    + " boundingRect={" + Arrays.toString(mBounds) + "}}";
	}

	/** A Builder for DisplayCutout (AOSP DisplayCutout.Builder) */
	public static final class Builder {
		private Insets mSafeInsets = Insets.NONE;
		private Insets mWaterfallInsets = Insets.NONE;
		private Path mCutoutPath;
		private final Rect mBoundingRectLeft = new Rect();
		private final Rect mBoundingRectTop = new Rect();
		private final Rect mBoundingRectRight = new Rect();
		private final Rect mBoundingRectBottom = new Rect();

		public Builder() {}

		public DisplayCutout build() {
			return new DisplayCutout(mSafeInsets.toRect(), mWaterfallInsets,
			                         new Rect[] {mBoundingRectLeft, mBoundingRectTop, mBoundingRectRight, mBoundingRectBottom},
			                         mCutoutPath, true);
		}

		public Builder setSafeInsets(Insets safeInsets) {
			mSafeInsets = safeInsets;
			return this;
		}

		public Builder setWaterfallInsets(Insets waterfallInsets) {
			mWaterfallInsets = waterfallInsets;
			return this;
		}

		public Builder setBoundingRectLeft(Rect boundingRectLeft) {
			mBoundingRectLeft.set(boundingRectLeft);
			return this;
		}

		public Builder setBoundingRectTop(Rect boundingRectTop) {
			mBoundingRectTop.set(boundingRectTop);
			return this;
		}

		public Builder setBoundingRectRight(Rect boundingRectRight) {
			mBoundingRectRight.set(boundingRectRight);
			return this;
		}

		public Builder setBoundingRectBottom(Rect boundingRectBottom) {
			mBoundingRectBottom.set(boundingRectBottom);
			return this;
		}

		public Builder setCutoutPath(Path cutoutPath) {
			mCutoutPath = cutoutPath;
			return this;
		}
	}
}
