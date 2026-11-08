package android.view;

import android.content.Context;

/**
 * Represents an icon that can be used as a mouse pointer (AOSP android.view.PointerIcon). A system icon is
 * known by its type; ATL does not draw custom icons, the desktop's cursor of that type stands for it.
 */
public final class PointerIcon {
	public static final int TYPE_NOT_SPECIFIED = 1;
	public static final int TYPE_NULL = 0;
	public static final int TYPE_ARROW = 1000;
	public static final int TYPE_CONTEXT_MENU = 1001;
	public static final int TYPE_HAND = 1002;
	public static final int TYPE_HELP = 1003;
	public static final int TYPE_WAIT = 1004;
	public static final int TYPE_CELL = 1006;
	public static final int TYPE_CROSSHAIR = 1007;
	public static final int TYPE_TEXT = 1008;
	public static final int TYPE_VERTICAL_TEXT = 1009;
	public static final int TYPE_ALIAS = 1010;
	public static final int TYPE_COPY = 1011;
	public static final int TYPE_NO_DROP = 1012;
	public static final int TYPE_ALL_SCROLL = 1013;
	public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;
	public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;
	public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;
	public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;
	public static final int TYPE_ZOOM_IN = 1018;
	public static final int TYPE_ZOOM_OUT = 1019;
	public static final int TYPE_GRAB = 1020;
	public static final int TYPE_GRABBING = 1021;
	public static final int TYPE_DEFAULT = TYPE_ARROW;

	private final int mType;

	private PointerIcon(int type) {
		mType = type;
	}

	/** AOSP PointerIcon.getSystemIcon(): the icon of a type; TYPE_NOT_SPECIFIED is a mistake, invalid types throw */
	public static PointerIcon getSystemIcon(Context context, int type) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (type == TYPE_NOT_SPECIFIED || (type != TYPE_NULL && (type < TYPE_ARROW || type > TYPE_GRABBING || type == 1005))) {
			throw new IllegalArgumentException("Invalid pointer icon type: " + type);
		}
		return new PointerIcon(type);
	}

	public int getType() {
		return mType;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || !(other instanceof PointerIcon)) {
			return false;
		}
		return mType == ((PointerIcon)other).mType;
	}

	@Override
	public int hashCode() {
		return mType;
	}
}
