package android.view;

import android.graphics.Insets;
import android.graphics.Rect;

public class DisplayCutout {
	public static final DisplayCutout NO_CUTOUT = new DisplayCutout();

	public Insets getWaterfallInsets() {
		return Insets.NONE;
	}

	public int getSafeInsetBottom() {
		return 0;
	}

	public int getSafeInsetLeft() {
		return 0;
	}

	public int getSafeInsetRight() {
		return 0;
	}

	public int getSafeInsetTop() {
		return 0;
	}

	/** @hide */
	public Rect getSafeInsets() {
		return new Rect(0, 0, 0, 0);
	}

	/* the desktop has no cutout: the only instance is empty and insetting it gives the same */
	/** @hide */
	public boolean isEmpty() {
		return true;
	}

	/** @hide */
	public DisplayCutout inset(int insetLeft, int insetTop, int insetRight, int insetBottom) {
		return this;
	}
}
