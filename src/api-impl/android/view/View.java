package android.view;

import android.R;
import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.atl.GskCanvas;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.LayoutDirection;
import android.util.Property;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.autofill.AutofillId;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.translation.ViewTranslationCallback;
import java.lang.CharSequence;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class WindowId {}

public class View implements Drawable.Callback {

	// --- constants from android source

	/**
	 * The logging tag used by this class with android.util.Log.
	 */
	protected static final String TAG = "View";

	/**
	 * When set to true, apps will draw debugging information about their layouts.
	 *
	 * @hide
	 */
	public static final String DEBUG_LAYOUT_PROPERTY = "debug.layout";

	/**
	 * Used to mark a View that has no ID.
	 */
	public static final int NO_ID = -1;

	/**
	 * Signals that compatibility booleans have been initialized according to
	 * target SDK versions.
	 */
	private static boolean sCompatibilityDone = false;

	/**
	 * Use the old (broken) way of building MeasureSpecs.
	 */
	private static boolean sUseBrokenMakeMeasureSpec = false;

	/**
	 * Ignore any optimizations using the measure cache.
	 */
	private static boolean sIgnoreMeasureCache = false;

	/**
	 * This view does not want keystrokes. Use with TAKES_FOCUS_MASK when
	 * calling setFlags.
	 */
	private static final int NOT_FOCUSABLE = 0x00000000;

	/**
	 * This view wants keystrokes. Use with TAKES_FOCUS_MASK when calling
	 * setFlags.
	 */
	private static final int FOCUSABLE = 0x00000001;

	/**
	 * Mask for use with setFlags indicating bits used for focus.
	 */
	private static final int FOCUSABLE_MASK = 0x00000001;

	/**
	 * This view will adjust its padding to fit sytem windows (e.g. status bar)
	 */
	private static final int FITS_SYSTEM_WINDOWS = 0x00000002;

	/**
	 * This view is visible.
	 * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
	 * android:visibility}.
	 */
	public static final int VISIBLE = 0x00000000;

	/**
	 * This view is invisible, but it still takes up space for layout purposes.
	 * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
	 * android:visibility}.
	 */
	public static final int INVISIBLE = 0x00000004;

	/**
	 * This view is invisible, and it doesn't take any space for layout
	 * purposes. Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
	 * android:visibility}.
	 */
	public static final int GONE = 0x00000008;

	/**
	 * Mask for use with setFlags indicating bits used for visibility.
	 * {@hide}
	 */
	static final int VISIBILITY_MASK = 0x0000000C;

	private static final int[] VISIBILITY_FLAGS = {VISIBLE, INVISIBLE, GONE};

	/**
	 * This view is enabled. Interpretation varies by subclass.
	 * Use with ENABLED_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int ENABLED = 0x00000000;

	/**
	 * This view is disabled. Interpretation varies by subclass.
	 * Use with ENABLED_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int DISABLED = 0x00000020;

	/**
	 * Mask for use with setFlags indicating bits used for indicating whether
	 * this view is enabled
	 * {@hide}
	 */
	static final int ENABLED_MASK = 0x00000020;

	/**
	 * This view won't draw. {@link #onDraw(android.graphics.Canvas)} won't be
	 * called and further optimizations will be performed. It is okay to have
	 * this flag set and a background. Use with DRAW_MASK when calling setFlags.
	 * {@hide}
	 */
	static final int WILL_NOT_DRAW = 0x00000080;

	/**
	 * Mask for use with setFlags indicating bits used for indicating whether
	 * this view is will draw
	 * {@hide}
	 */
	static final int DRAW_MASK = 0x00000080;

	/**
	 * <p>This view doesn't show scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_NONE = 0x00000000;

	/**
	 * <p>This view shows horizontal scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_HORIZONTAL = 0x00000100;

	/**
	 * <p>This view shows vertical scrollbars.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_VERTICAL = 0x00000200;

	/**
	 * <p>Mask for use with setFlags indicating bits used for indicating which
	 * scrollbars are enabled.</p>
	 * {@hide}
	 */
	static final int SCROLLBARS_MASK = 0x00000300;

	/**
	 * Indicates that the view should filter touches when its window is obscured.
	 * Refer to the class comments for more information about this security feature.
	 * {@hide}
	 */
	static final int FILTER_TOUCHES_WHEN_OBSCURED = 0x00000400;

	/**
	 * Set for framework elements that use FITS_SYSTEM_WINDOWS, to indicate
	 * that they are optional and should be skipped if the window has
	 * requested system UI flags that ignore those insets for layout.
	 */
	static final int OPTIONAL_FITS_SYSTEM_WINDOWS = 0x00000800;

	/**
	 * <p>This view doesn't show fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_NONE = 0x00000000;

	/**
	 * <p>This view shows horizontal fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_HORIZONTAL = 0x00001000;

	/**
	 * <p>This view shows vertical fading edges.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_VERTICAL = 0x00002000;

	/**
	 * <p>Mask for use with setFlags indicating bits used for indicating which
	 * fading edges are enabled.</p>
	 * {@hide}
	 */
	static final int FADING_EDGE_MASK = 0x00003000;

	/**
	 * <p>Indicates this view can be clicked. When clickable, a View reacts
	 * to clicks by notifying the OnClickListener.<p>
	 * {@hide}
	 */
	static final int CLICKABLE = 0x00004000;

	/**
	 * <p>Indicates this view is caching its drawing into a bitmap.</p>
	 * {@hide}
	 */
	static final int DRAWING_CACHE_ENABLED = 0x00008000;

	/**
	 * <p>Indicates that no icicle should be saved for this view.<p>
	 * {@hide}
	 */
	static final int SAVE_DISABLED = 0x000010000;

	/**
	 * <p>Mask for use with setFlags indicating bits used for the saveEnabled
	 * property.</p>
	 * {@hide}
	 */
	static final int SAVE_DISABLED_MASK = 0x000010000;

	/**
	 * <p>Indicates that no drawing cache should ever be created for this view.<p>
	 * {@hide}
	 */
	static final int WILL_NOT_CACHE_DRAWING = 0x000020000;

	/**
	 * <p>Indicates this view can take / keep focus when int touch mode.</p>
	 * {@hide}
	 */
	static final int FOCUSABLE_IN_TOUCH_MODE = 0x00040000;

	/**
	 * <p>Enables low quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_LOW = 0x00080000;

	/**
	 * <p>Enables high quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_HIGH = 0x00100000;

	/**
	 * <p>Enables automatic quality mode for the drawing cache.</p>
	 */
	public static final int DRAWING_CACHE_QUALITY_AUTO = 0x00000000;

	private static final int[] DRAWING_CACHE_QUALITY_FLAGS = {
		DRAWING_CACHE_QUALITY_AUTO, DRAWING_CACHE_QUALITY_LOW, DRAWING_CACHE_QUALITY_HIGH
	};

	/**
	 * <p>Mask for use with setFlags indicating bits used for the cache
	 * quality property.</p>
	 * {@hide}
	 */
	static final int DRAWING_CACHE_QUALITY_MASK = 0x00180000;

	/**
	 * <p>
	 * Indicates this view can be long clicked. When long clickable, a View
	 * reacts to long clicks by notifying the OnLongClickListener or showing a
	 * context menu.
	 * </p>
	 * {@hide}
	 */
	static final int LONG_CLICKABLE = 0x00200000;

	/**
	 * <p>Indicates that this view gets its drawable states from its direct parent
	 * and ignores its original internal states.</p>
	 *
	 * @hide
	 */
	static final int DUPLICATE_PARENT_STATE = 0x00400000;

	/**
	 * The scrollbar style to display the scrollbars inside the content area,
	 * without increasing the padding. The scrollbars will be overlaid with
	 * translucency on the view's content.
	 */
	public static final int SCROLLBARS_INSIDE_OVERLAY = 0;

	/**
	 * The scrollbar style to display the scrollbars inside the padded area,
	 * increasing the padding of the view. The scrollbars will not overlap the
	 * content area of the view.
	 */
	public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;

	/**
	 * The scrollbar style to display the scrollbars at the edge of the view,
	 * without increasing the padding. The scrollbars will be overlaid with
	 * translucency.
	 */
	public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;

	/**
	 * The scrollbar style to display the scrollbars at the edge of the view,
	 * increasing the padding of the view. The scrollbars will only overlap the
	 * background, if any.
	 */
	public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

	/**
	 * Mask to check if the scrollbar style is overlay or inset.
	 * {@hide}
	 */
	static final int SCROLLBARS_INSET_MASK = 0x01000000;

	/**
	 * Mask to check if the scrollbar style is inside or outside.
	 * {@hide}
	 */
	static final int SCROLLBARS_OUTSIDE_MASK = 0x02000000;

	/**
	 * Mask for scrollbar style.
	 * {@hide}
	 */
	static final int SCROLLBARS_STYLE_MASK = 0x03000000;

	/**
	 * View flag indicating that the screen should remain on while the
	 * window containing this view is visible to the user.  This effectively
	 * takes care of automatically setting the WindowManager's
	 * {@link WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON}.
	 */
	public static final int KEEP_SCREEN_ON = 0x04000000;

	/**
	 * View flag indicating whether this view should have sound effects enabled
	 * for events such as clicking and touching.
	 */
	public static final int SOUND_EFFECTS_ENABLED = 0x08000000;

	/**
	 * View flag indicating whether this view should have haptic feedback
	 * enabled for events such as long presses.
	 */
	public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;

	/**
	 * <p>Indicates that the view hierarchy should stop saving state when
	 * it reaches this view.  If state saving is initiated immediately at
	 * the view, it will be allowed.
	 * {@hide}
	 */
	static final int PARENT_SAVE_DISABLED = 0x20000000;

	/**
	 * <p>Mask for use with setFlags indicating bits used for PARENT_SAVE_DISABLED.</p>
	 * {@hide}
	 */
	static final int PARENT_SAVE_DISABLED_MASK = 0x20000000;

	/**
	 * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
	 * should add all focusable Views regardless if they are focusable in touch mode.
	 */
	public static final int FOCUSABLES_ALL = 0x00000000;

	/**
	 * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
	 * should add only Views focusable in touch mode.
	 */
	public static final int FOCUSABLES_TOUCH_MODE = 0x00000001;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus to the previous selectable
	 * item.
	 */
	public static final int FOCUS_BACKWARD = 0x00000001;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus to the next selectable
	 * item.
	 */
	public static final int FOCUS_FORWARD = 0x00000002;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus to the left.
	 */
	public static final int FOCUS_LEFT = 0x00000011;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus up.
	 */
	public static final int FOCUS_UP = 0x00000021;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus to the right.
	 */
	public static final int FOCUS_RIGHT = 0x00000042;

	/**
	 * Use with {@link #focusSearch(int)}. Move focus down.
	 */
	public static final int FOCUS_DOWN = 0x00000082;

	/**
	 * Bits of {@link #getMeasuredWidthAndState()} and
	 * {@link #getMeasuredWidthAndState()} that provide the actual measured size.
	 */
	public static final int MEASURED_SIZE_MASK = 0x00ffffff;

	/**
	 * Bits of {@link #getMeasuredWidthAndState()} and
	 * {@link #getMeasuredWidthAndState()} that provide the additional state bits.
	 */
	public static final int MEASURED_STATE_MASK = 0xff000000;

	/**
	 * Bit shift of {@link #MEASURED_STATE_MASK} to get to the height bits
	 * for functions that combine both width and height into a single int,
	 * such as {@link #getMeasuredState()} and the childState argument of
	 * {@link #resolveSizeAndState(int, int, int)}.
	 */
	public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;

	/**
	 * Bit of {@link #getMeasuredWidthAndState()} and
	 * {@link #getMeasuredWidthAndState()} that indicates the measured size
	 * is smaller that the space the view would like to have.
	 */
	public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

	// --- and some flags

	/**
	 * Indicates that this view has reported that it can accept the current drag's content.
	 * Cleared when the drag operation concludes.
	 * @hide
	 */
	/* the bits of AOSP's mPrivateFlags2 that ATL keeps in Java: the raw and the resolved text alignment */
	static final int PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT = 13;
	static final int PFLAG2_TEXT_ALIGNMENT_MASK = 0x00000007 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;
	static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED = 0x00000008 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;
	static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT = 17;
	static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK = 0x00000007 << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;
	private static final int TEXT_ALIGNMENT_DEFAULT = 1 /* TEXT_ALIGNMENT_GRAVITY */;
	static final int TEXT_ALIGNMENT_RESOLVED_DEFAULT = 1 /* TEXT_ALIGNMENT_GRAVITY */;
	private static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT = TEXT_ALIGNMENT_RESOLVED_DEFAULT << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;
	int mPrivateFlags2 = (TEXT_ALIGNMENT_DEFAULT << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT) | PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;

	static final int PFLAG2_DRAG_CAN_ACCEPT = 0x00000001;

	/**
	 * Indicates that this view is currently directly under the drag location in a
	 * drag-and-drop operation involving content that it can accept.  Cleared when
	 * the drag exits the view, or when the drag operation concludes.
	 * @hide
	 */
	static final int PFLAG2_DRAG_HOVERED = 0x00000002;

	/**
	 * Horizontal layout direction of this view is from Left to Right.
	 * Use with {@link #setLayoutDirection}.
	 */
	public static final int LAYOUT_DIRECTION_LTR = LayoutDirection.LTR;

	/**
	 * Horizontal layout direction of this view is from Right to Left.
	 * Use with {@link #setLayoutDirection}.
	 */
	public static final int LAYOUT_DIRECTION_RTL = LayoutDirection.RTL;

	/**
	 * Horizontal layout direction of this view is inherited from its parent.
	 * Use with {@link #setLayoutDirection}.
	 */
	public static final int LAYOUT_DIRECTION_INHERIT = LayoutDirection.INHERIT;

	/**
	 * Horizontal layout direction of this view is from deduced from the default language
	 * script for the locale. Use with {@link #setLayoutDirection}.
	 */
	public static final int LAYOUT_DIRECTION_LOCALE = LayoutDirection.LOCALE;

	/**
	 * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
	 * @hide
	 */
	static final int PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT = 2;

	/**
	 * Mask for use with private flags indicating bits used for horizontal layout direction.
	 * @hide
	 */
	static final int PFLAG2_LAYOUT_DIRECTION_MASK = 0x00000003 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

	/**
	 * Indicates whether the view horizontal layout direction has been resolved and drawn to the
	 * right-to-left direction.
	 * @hide
	 */
	static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL = 4 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

	/**
	 * Indicates whether the view horizontal layout direction has been resolved.
	 * @hide
	 */
	static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED = 8 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

	/**
	 * Mask for use with private flags indicating bits used for resolved horizontal layout direction.
	 * @hide
	 */
	static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK = 0x0000000C << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

	/**
	 * Indicates no axis of view scrolling.
	 */
	public static final int SCROLL_AXIS_NONE = 0;

	/**
	 * Indicates scrolling along the horizontal axis.
	 */
	public static final int SCROLL_AXIS_HORIZONTAL = 1 << 0;

	/**
	 * Indicates scrolling along the vertical axis.
	 */
	public static final int SCROLL_AXIS_VERTICAL = 1 << 1;

	/**
	 * Always allow a user to over-scroll this view, provided it is a
	 * view that can scroll.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_ALWAYS = 0;

	/**
	 * Allow a user to over-scroll this view only if the content is large
	 * enough to meaningfully scroll, provided it is a view that can scroll.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

	/**
	 * Never allow a user to over-scroll this view.
	 *
	 * @see #getOverScrollMode()
	 * @see #setOverScrollMode(int)
	 */
	public static final int OVER_SCROLL_NEVER = 2;

	// --- apparently there's more...

	@Deprecated
	public static final int STATUS_BAR_HIDDEN = 1; // 0x1

	@Deprecated
	public static final int STATUS_BAR_VISIBLE = 0; // 0x0

	public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4; // 0x4

	public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2; // 0x2

	public static final int SYSTEM_UI_FLAG_IMMERSIVE = 2048; // 0x800

	public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096; // 0x1000

	public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024; // 0x400

	public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512; // 0x200

	public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256; // 0x100

	public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1; // 0x1

	public static final int SYSTEM_UI_FLAG_VISIBLE = 0; // 0x0

	public static final int SYSTEM_UI_LAYOUT_FLAGS = 1536; // 0x600

	public static final int TEXT_ALIGNMENT_CENTER = 4; // 0x4

	public static final int TEXT_ALIGNMENT_GRAVITY = 1; // 0x1

	public static final int TEXT_ALIGNMENT_INHERIT = 0; // 0x0

	public static final int TEXT_ALIGNMENT_TEXT_END = 3; // 0x3

	public static final int TEXT_ALIGNMENT_TEXT_START = 2; // 0x2

	public static final int TEXT_ALIGNMENT_VIEW_END = 6; // 0x6

	public static final int TEXT_ALIGNMENT_VIEW_START = 5; // 0x5

	public static final int TEXT_DIRECTION_ANY_RTL = 2; // 0x2

	public static final int TEXT_DIRECTION_FIRST_STRONG = 1; // 0x1

	public static final int TEXT_DIRECTION_INHERIT = 0; // 0x0

	public static final int TEXT_DIRECTION_LOCALE = 5; // 0x5

	public static final int TEXT_DIRECTION_LTR = 3; // 0x3

	public static final int TEXT_DIRECTION_RTL = 4; // 0x4

	public static final Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
		@Override
		public void setValue(View object, float value) {
			object.setScaleX(value);
		}
		@Override
		public Float get(View object) {
			return object.getScaleX();
		}
	};

	public static final Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
		@Override
		public void setValue(View object, float value) {
			object.setScaleY(value);
		}
		@Override
		public Float get(View object) {
			return object.getScaleY();
		}
	};

	// --- end of constants from android source

	// --- interfaces from android source
	public interface OnTouchListener {
		/**
		 * Called when a touch event is dispatched to a view. This allows listeners to
		 * get a chance to respond before the target view.
		 *
		 * @param v The view the touch event has been dispatched to.
		 * @param event The MotionEvent object containing full information about
		 *        the event.
		 * @return True if the listener has consumed the event, false otherwise.
		 */
		boolean onTouch(View v, MotionEvent event);
	}

	public interface OnScrollChangeListener {
		void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
	}

	public interface OnClickListener {
		void onClick(View v);
	}

	public interface OnAttachStateChangeListener {
		public void onViewAttachedToWindow(View v);
		public void onViewDetachedFromWindow(View v);
	}

	public interface OnGenericMotionListener {
		// TODO
	}

	public interface OnSystemUiVisibilityChangeListener {
		// TODO
	}

	public static interface OnKeyListener {
		boolean onKey(View v, int keyCode, KeyEvent event);
	}

	public interface OnLongClickListener {
		public boolean onLongClick(View v);
	}

	public interface OnHoverListener {
		// TODO
	}

	public interface OnApplyWindowInsetsListener {
		public WindowInsets onApplyWindowInsets(View v, WindowInsets insets);
	}

	public interface OnLayoutChangeListener {
		public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom);
	}

	public interface OnUnhandledKeyEventListener {
		// TODO
	}

	public interface OnFocusChangeListener {
		void onFocusChange(View v, boolean hasFocus);
	}

	public interface OnCreateContextMenuListener {
		/**
		 * Called when the context menu for this view is being built. It is not
		 * safe to hold onto the menu after this method returns.
		 *
		 * @param menu The context menu that is being built
		 * @param v The view for which the context menu is being built
		 * @param menuInfo Extra information about the item for which the
		 *            context menu should be shown. This information will vary
		 *            depending on the class of v.
		 */
		//void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo);
	}

	public interface OnDragListener {}
	// --- end of interfaces

	// --- subclasses

	/**
	 * A MeasureSpec encapsulates the layout requirements passed from parent to child.
	 * Each MeasureSpec represents a requirement for either the width or the height.
	 * A MeasureSpec is comprised of a size and a mode. There are three possible
	 * modes:
	 * <dl>
	 * <dt>UNSPECIFIED</dt>
	 * <dd>
	 * The parent has not imposed any constraint on the child. It can be whatever size
	 * it wants.
	 * </dd>
	 *
	 * <dt>EXACTLY</dt>
	 * <dd>
	 * The parent has determined an exact size for the child. The child is going to be
	 * given those bounds regardless of how big it wants to be.
	 * </dd>
	 *
	 * <dt>AT_MOST</dt>
	 * <dd>
	 * The child can be as large as it wants up to the specified size.
	 * </dd>
	 * </dl>
	 *
	 * MeasureSpecs are implemented as ints to reduce object allocation. This class
	 * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
	 */
	public static class MeasureSpec {
		private static final int MODE_SHIFT = 30;
		private static final int MODE_MASK = 0x3 << MODE_SHIFT;

		/**
		 * Measure specification mode: The parent has not imposed any constraint
		 * on the child. It can be whatever size it wants.
		 */
		public static final int UNSPECIFIED = 0 << MODE_SHIFT;

		/**
		 * Measure specification mode: The parent has determined an exact size
		 * for the child. The child is going to be given those bounds regardless
		 * of how big it wants to be.
		 */
		public static final int EXACTLY = 1 << MODE_SHIFT;

		/**
		 * Measure specification mode: The child can be as large as it wants up
		 * to the specified size.
		 */
		public static final int AT_MOST = 2 << MODE_SHIFT;

		/**
		 * Creates a measure specification based on the supplied size and mode.
		 *
		 * The mode must always be one of the following:
		 * <ul>
		 *  <li>{@link android.view.View.MeasureSpec#UNSPECIFIED}</li>
		 *  <li>{@link android.view.View.MeasureSpec#EXACTLY}</li>
		 *  <li>{@link android.view.View.MeasureSpec#AT_MOST}</li>
		 * </ul>
		 *
		 * <p><strong>Note:</strong> On API level 17 and lower, makeMeasureSpec's
		 * implementation was such that the order of arguments did not matter
		 * and overflow in either value could impact the resulting MeasureSpec.
		 * {@link android.widget.RelativeLayout} was affected by this bug.
		 * Apps targeting API levels greater than 17 will get the fixed, more strict
		 * behavior.</p>
		 *
		 * @param size the size of the measure specification
		 * @param mode the mode of the measure specification
		 * @return the measure specification based on size and mode
		 */
		public static int makeMeasureSpec(int size, int mode) {
			if (sUseBrokenMakeMeasureSpec) {
				return size + mode;
			} else {
				return (size & ~MODE_MASK) | (mode & MODE_MASK);
			}
		}

		/**
		 * Extracts the mode from the supplied measure specification.
		 *
		 * @param measureSpec the measure specification to extract the mode from
		 * @return {@link android.view.View.MeasureSpec#UNSPECIFIED},
		 *		 {@link android.view.View.MeasureSpec#AT_MOST} or
		 *		 {@link android.view.View.MeasureSpec#EXACTLY}
		 */
		public static int getMode(int measureSpec) {
			return (measureSpec & MODE_MASK);
		}

		/**
		 * Extracts the size from the supplied measure specification.
		 *
		 * @param measureSpec the measure specification to extract the size from
		 * @return the size in pixels defined in the supplied measure specification
		 */
		public static int getSize(int measureSpec) {
			return (measureSpec & ~MODE_MASK);
		}

		static int adjust(int measureSpec, int delta) {
			return makeMeasureSpec(getSize(measureSpec + delta), getMode(measureSpec));
		}

		/**
		 * Returns a String representation of the specified measure
		 * specification.
		 *
		 * @param measureSpec the measure specification to convert to a String
		 * @return a String with the following format: "MeasureSpec: MODE SIZE"
		 */
		public static String toString(int measureSpec) {
			int mode = getMode(measureSpec);
			int size = getSize(measureSpec);

			StringBuilder sb = new StringBuilder("MeasureSpec: ");

			if (mode == UNSPECIFIED)
				sb.append("UNSPECIFIED ");
			else if (mode == EXACTLY)
				sb.append("EXACTLY ");
			else if (mode == AT_MOST)
				sb.append("AT_MOST ");
			else
				sb.append(mode).append(" ");

			sb.append(size);
			return sb.toString();
		}
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	public static class DragShadowBuilder {
		private final WeakReference<View> mView;

		public DragShadowBuilder(View view) {
			mView = new WeakReference<View>(view);
		}

		public DragShadowBuilder() {
			mView = new WeakReference<View>(null);
		}

		final public View getView() {
			return mView.get();
		}

		public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
			final View view = mView.get();
			if (view != null) {
				outShadowSize.set(view.getWidth(), view.getHeight());
				outShadowTouchPoint.set(outShadowSize.x / 2, outShadowSize.y / 2);
			} else {
				Slog.e("View", "Asked for drag thumb metrics but no view");
			}
		}

		public void onDrawShadow(Canvas canvas) {
			final View view = mView.get();
			if (view != null) {
				view.draw(canvas);
			} else {
				Slog.e("View", "Asked to draw drag shadow but no view");
			}
		}
	}

	// --- end of subclasses

	public int id = NO_ID;
	private int system_ui_visibility = 0;
	public ViewParent parent;
	public AttributeSet attrs;
	protected ViewGroup.LayoutParams layout_params;
	private Context context;
	private Map<Integer, Object> tags = new HashMap<>();
	private Object tag;
	int gravity = -1; // fallback gravity for layout children

	int measuredWidth = 0;
	int measuredHeight = 0;

	private int left = 0;
	private int top = 0;
	private int right = 0;
	private int bottom = 0;

	private float translationX = 0;
	private float translationY = 0;
	/* AOSP RenderNode properties; native_layout() reads them when hasTransform is set */
	private float scaleX = 1;
	private float scaleY = 1;
	private float rotation = 0;
	private float pivotX = 0;
	private float pivotY = 0;
	private boolean pivotExplicit = false;
	private boolean hasTransform = false;
	private Matrix matrix;
	private Matrix inverseMatrix;

	private int scrollX = 0;
	private int scrollY = 0;

	ViewTreeObserver floating_observer = null;

	public long widget; // pointer

	private int oldWidthMeasureSpec = -1;
	private int oldHeightMeasureSpec = -1;
	private int oldWidth;
	private int oldHeight;
	protected boolean haveCustomMeasure = true;
	public boolean disallowIntercept = false;

	private int visibility = View.VISIBLE;
	private float alpha = 1.0f;
	private float transitionAlpha = 1.0f;
	private boolean duplicate_parent_state = false;

	/* the bits of AOSP's mPrivateFlags that ATL keeps in Java */
	int mPrivateFlags = PFLAG_FORCE_LAYOUT;
	/* ATL clears PFLAG_FORCE_LAYOUT when the view is measured (GTK measures and allocates separately), AOSP in layout() */
	static final int PFLAG_FORCE_LAYOUT                = 0x00001000;
	static final int PFLAG_LAYOUT_REQUIRED             = 0x00002000;
	static final int PFLAG_SELECTED                    = 0x00000004;
	static final int PFLAG_DRAWABLE_STATE_DIRTY        = 0x00000400;
	private static final int PFLAG_PRESSED             = 0x00004000;
	static final int PFLAG_ACTIVATED                   = 0x40000000;

	private int[] mDrawableState = null;

	/* the bits of AOSP's mPrivateFlags3 that ATL keeps */
	int mPrivateFlags3;
	static final int PFLAG3_IS_LAID_OUT = 0x4;
	private Drawable background;
	private int backgroundTint = 0;

	private int minWidth = 0;
	private int minHeight = 0;

	protected int paddingLeft = 0;
	protected int paddingTop = 0;
	protected int paddingRight = 0;
	protected int paddingBottom = 0;

	/* like AOSP's mUserPadding*: paddingLeft/paddingRight are resolved from the relative padding
	 * (start/end, which win if set) and the absolute one with the resolved layout direction */
	private static final int UNDEFINED_PADDING = Integer.MIN_VALUE;
	private int userPaddingStart = UNDEFINED_PADDING;
	private int userPaddingEnd = UNDEFINED_PADDING;
	private int userPaddingLeftInitial = 0;
	private int userPaddingRightInitial = 0;

	/* android:layoutDirection as set, and what it resolves to (AOSP's PFLAG2_LAYOUT_DIRECTION_*) */
	private int rawLayoutDirection = LAYOUT_DIRECTION_INHERIT;
	private boolean layoutDirectionResolved = false;
	private boolean layoutRtl = false;
	private boolean layoutParamsRtl = false; // the direction the layout params were resolved with

	private boolean atl_enabled = true;
	private boolean atl_focusable = true;
	private boolean atl_focusable_set; // set with setFocusable() or android:focusable

	private Handler handler;
	private Animation currentAnimation;
	private Transformation animationTransformation;
	private boolean animationMore; // read by the wrapper widget: draw the next frame

	/* AOSP's View.ListenerInfo, with the listeners ATL keeps in Java: the click listener and the
	 * lists of layout change and attach state change listeners */
	static class ListenerInfo {
		OnClickListener mOnClickListener;
		private ArrayList<OnLayoutChangeListener> mOnLayoutChangeListeners;
		private CopyOnWriteArrayList<OnAttachStateChangeListener> mOnAttachStateChangeListeners;
	}

	ListenerInfo mListenerInfo;

	ListenerInfo getListenerInfo() {
		if (mListenerInfo != null) {
			return mListenerInfo;
		}
		mListenerInfo = new ListenerInfo();
		return mListenerInfo;
	}

	private boolean clickable = false;
	private boolean longclickable = false;

	public static final Property<View, Float> TRANSLATION_X = new Property<View, Float>(Float.class, "translationX") {
		@Override
		public Float get(View view) {
			return view.getTranslationX();
		}
		@Override
		public void set(View view, Float value) {
			view.setTranslationX(value);
		}
	};

	public static final Property<View, Float> TRANSLATION_Y = new Property<View, Float>(Float.class, "translationY") {
		@Override
		public Float get(View view) {
			return view.getTranslationY();
		}
		@Override
		public void set(View view, Float value) {
			view.setTranslationY(value);
		}
	};

	public static final Property<View, Float> TRANSLATION_Z = new Property<View, Float>(Float.class, "translationZ") {
		@Override
		public Float get(View view) {
			return view.getTranslationZ();
		}
		@Override
		public void set(View view, Float value) {
			view.setTranslationZ(value);
		}
	};

	public static final Property<View, Float> ALPHA = new Property<View, Float>(Float.class, "alpha") {
		@Override
		public Float get(View view) {
			return view.getAlpha();
		}
		@Override
		public void set(View view, Float value) {
			view.setAlpha(value);
		}
	};

	public static final Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
		@Override
		public void setValue(View object, float value) {
			object.setRotation(value);
		}
		@Override
		public Float get(View object) {
			return object.getRotation();
		}
	};

	public View(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public View(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs, defStyle, 0);
	}

	public View(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
		this.context = context;

		widget = native_constructor(context, attrs);

		if (!sCompatibilityDone && context != null) {
			final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;

			sUseDefaultFocusHighlight = context.getResources().getBoolean(
					com.android.internal.R.bool.config_useDefaultFocusHighlight);

			sBrokenInsetsDispatch = targetSdkVersion < Build.VERSION_CODES.R;
			sCompatibilityDone = true;
		}

		TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.View, defStyle, defStyleRes);
		this.id = a.getResourceId(com.android.internal.R.styleable.View_id, View.NO_ID);
		mNextFocusLeftId = a.getResourceId(com.android.internal.R.styleable.View_nextFocusLeft, View.NO_ID);
		mNextFocusRightId = a.getResourceId(com.android.internal.R.styleable.View_nextFocusRight, View.NO_ID);
		mNextFocusUpId = a.getResourceId(com.android.internal.R.styleable.View_nextFocusUp, View.NO_ID);
		mNextFocusDownId = a.getResourceId(com.android.internal.R.styleable.View_nextFocusDown, View.NO_ID);
		mNextFocusForwardId = a.getResourceId(com.android.internal.R.styleable.View_nextFocusForward, View.NO_ID);
		mNextClusterForwardId = a.getResourceId(com.android.internal.R.styleable.View_nextClusterForward, View.NO_ID);
		if (a.hasValue(com.android.internal.R.styleable.View_backgroundTint))
			backgroundTint = a.getColor(com.android.internal.R.styleable.View_backgroundTint, 0);
		if (a.hasValue(com.android.internal.R.styleable.View_background)) {
			try {
				Drawable background = a.getDrawable(com.android.internal.R.styleable.View_background);

				if (background != null) {
					setBackground(background);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (a.hasValue(com.android.internal.R.styleable.View_foreground))
			setForeground(a.getDrawable(com.android.internal.R.styleable.View_foreground));
		if (a.hasValue(com.android.internal.R.styleable.View_foregroundGravity))
			setForegroundGravity(a.getInt(com.android.internal.R.styleable.View_foregroundGravity, Gravity.NO_GRAVITY));
		if (a.hasValue(com.android.internal.R.styleable.View_foregroundInsidePadding)) {
			if (mForegroundInfo == null) {
				mForegroundInfo = new ForegroundInfo();
			}
			mForegroundInfo.mInsidePadding = a.getBoolean(com.android.internal.R.styleable.View_foregroundInsidePadding,
					mForegroundInfo.mInsidePadding);
		}
		mDefaultFocusHighlightEnabled = a.getBoolean(com.android.internal.R.styleable.View_defaultFocusHighlightEnabled, true);
		if (a.hasValue(com.android.internal.R.styleable.View_focusable)) {
			atl_focusable = a.getBoolean(com.android.internal.R.styleable.View_focusable, true);
			atl_focusable_set = atl_focusable;
			if (!atl_focusable)
				native_setNativeFocusable(widget, false);
		}
		if (a.hasValue(com.android.internal.R.styleable.View_visibility)) {
			visibility = VISIBILITY_FLAGS[a.getInt(com.android.internal.R.styleable.View_visibility, 0)];
			native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
		}

		if (a.hasValue(com.android.internal.R.styleable.View_minWidth))
			minWidth = a.getDimensionPixelSize(com.android.internal.R.styleable.View_minWidth, 0);

		if (a.hasValue(com.android.internal.R.styleable.View_minHeight))
			minHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.View_minHeight, 0);

		int padding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_padding, -1);

		int paddingVertical = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingVertical, -1);
		int paddingHorizontal = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingHorizontal, -1);

		int paddingStart = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingStart, -1);
		int paddingEnd = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingEnd, -1);

		if (padding >= 0) {
			paddingLeft = padding;
			paddingTop = padding;
			paddingRight = padding;
			paddingBottom = padding;
		} else {
			if (paddingVertical >= 0) {
				paddingTop = paddingVertical;
				paddingBottom = paddingVertical;
			} else {
				paddingTop = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingTop, 0);
				paddingBottom = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingBottom, 0);
			}

			if (paddingHorizontal >= 0) {
				paddingLeft = paddingHorizontal;
				paddingRight = paddingHorizontal;
			} else {
				paddingLeft = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingLeft, 0);
				paddingRight = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingRight, 0);

				/* like AOSP, start/end override left/right once resolved */
				if (paddingStart >= 0)
					userPaddingStart = paddingStart;

				if (paddingEnd >= 0)
					userPaddingEnd = paddingEnd;
			}
		}
		userPaddingLeftInitial = paddingLeft;
		userPaddingRightInitial = paddingRight;
		resolvePaddingValues();

		native_setPadding(widget, paddingLeft, paddingTop, paddingRight, paddingBottom);

		if (a.hasValue(com.android.internal.R.styleable.View_layoutDirection)) {
			/* the attribute's values are ltr, rtl, inherit and locale, in the order of the constants */
			rawLayoutDirection = a.getInt(com.android.internal.R.styleable.View_layoutDirection, LAYOUT_DIRECTION_INHERIT);
		}

		if (a.hasValue(com.android.internal.R.styleable.View_tag))
			tag = a.getText(com.android.internal.R.styleable.View_tag);

		if (a.hasValue(com.android.internal.R.styleable.View_contentDescription))
			setContentDescription(a.getString(com.android.internal.R.styleable.View_contentDescription));
		if (a.hasValue(com.android.internal.R.styleable.View_tooltipText))
			setTooltipText(a.getText(com.android.internal.R.styleable.View_tooltipText));

		if (a.hasValue(com.android.internal.R.styleable.View_textAlignment)) {
			int textAlignment = a.getInt(com.android.internal.R.styleable.View_textAlignment, 0);
			setTextAlignment(textAlignment);
		}

		clickable = a.getBoolean(com.android.internal.R.styleable.View_clickable, false);
		longclickable = a.getBoolean(com.android.internal.R.styleable.View_longClickable, false);
		updateKeyboardFocusable();

		String handlerName = a.getString(com.android.internal.R.styleable.View_onClick);
		if (handlerName != null)
			setOnClickListener(new DeclaredOnClickListener(this, handlerName));

		if (a.hasValue(com.android.internal.R.styleable.View_stateListAnimator)) {
			int resId = a.getResourceId(com.android.internal.R.styleable.View_stateListAnimator, 0);
			setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, resId));
		}
		duplicate_parent_state = a.getBoolean(com.android.internal.R.styleable.View_duplicateParentState, false);
		a.recycle();
		onCreateDrawableState(0);
	}

	/* AOSP View.onCreateDrawableState(); ATL: state_focused follows whether GTK shows the focus
	 * (AOSP: focused, which outside of touch mode is the same), no accelerated/hovered/drag states */
	protected int[] onCreateDrawableState(int extraSpace) {
		if (duplicate_parent_state && parent instanceof View) {
			return ((View) parent).onCreateDrawableState(extraSpace);
		}

		int[] drawableState;

		int privateFlags = mPrivateFlags;

		int viewStateIndex = 0;
		if ((privateFlags & PFLAG_PRESSED) != 0) viewStateIndex |= StateSet.VIEW_STATE_PRESSED;
		if (isEnabled()) viewStateIndex |= StateSet.VIEW_STATE_ENABLED;
		if (focus_shown) viewStateIndex |= StateSet.VIEW_STATE_FOCUSED;
		if ((privateFlags & PFLAG_SELECTED) != 0) viewStateIndex |= StateSet.VIEW_STATE_SELECTED;
		if (hasWindowFocus()) viewStateIndex |= StateSet.VIEW_STATE_WINDOW_FOCUSED;
		if ((privateFlags & PFLAG_ACTIVATED) != 0) viewStateIndex |= StateSet.VIEW_STATE_ACTIVATED;

		drawableState = StateSet.get(viewStateIndex);

		if (extraSpace == 0) {
			return drawableState;
		}

		final int[] fullState;
		if (drawableState != null) {
			fullState = new int[drawableState.length + extraSpace];
			System.arraycopy(drawableState, 0, fullState, 0, drawableState.length);
		} else {
			fullState = new int[extraSpace];
		}

		return fullState;
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
		final int N = baseState.length;
		int i = N - 1;
		while (i >= 0 && baseState[i] == 0) {
			i--;
		}
		System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
		return baseState;
	}

	public View findViewById(int id) {
		if (this.id == id)
			return this;
		else
			return null;
	}

	@Export
	public void onDraw(Canvas canvas) {
		if (canvas instanceof GskCanvas)
			native_drawContent(widget, ((GskCanvas)canvas).snapshot);
	}

	@Export
	protected void dispatchDraw(Canvas canvas) {
		if (canvas instanceof GskCanvas)
			native_drawChildren(widget, ((GskCanvas)canvas).snapshot);
	}

	@Export
	public void draw(Canvas canvas) {
		if (canvas instanceof GskCanvas)
			native_drawBackground(widget, ((GskCanvas)canvas).snapshot);
		onDraw(canvas);
		// HACK: catch non critical exceptions happening in some composeUI apps
		try {
			dispatchDraw(canvas);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		onDrawForeground(canvas);
		drawDefaultFocusHighlight(canvas);
	}

	/* AOSP's default focus highlight (android-14 View.java) */
	private Drawable mDefaultFocusHighlight;
	private Drawable mDefaultFocusHighlightCache;
	boolean mDefaultFocusHighlightEnabled = true;
	private static boolean sUseDefaultFocusHighlight;

	/* before API 30, a view group stops handing out the insets once a child consumed them */
	static boolean sBrokenInsetsDispatch;

	public void setDefaultFocusHighlightEnabled(boolean defaultFocusHighlightEnabled) {
		mDefaultFocusHighlightEnabled = defaultFocusHighlightEnabled;
	}

	public final boolean getDefaultFocusHighlightEnabled() {
		return mDefaultFocusHighlightEnabled;
	}

	private Drawable getDefaultFocusHighlightDrawable() {
		if (mDefaultFocusHighlightCache == null) {
			if (context != null) {
				final int[] attrs = new int[] { android.R.attr.selectableItemBackground };
				final TypedArray ta = context.obtainStyledAttributes(attrs);
				mDefaultFocusHighlightCache = ta.getDrawable(0);
				ta.recycle();
			}
		}
		return mDefaultFocusHighlightCache;
	}

	/* ATL: the wrapper widget draws through Java instead of AOSP's PFLAG_SKIP_DRAW, and the bounds
	 * are set on each draw (there is no setFrame() to tell about a size change) */
	private void setDefaultFocusHighlight(Drawable highlight) {
		mDefaultFocusHighlight = highlight;
		if (highlight != null) {
			native_drawThroughJava(widget);
			if (highlight.isStateful()) {
				highlight.setState(getDrawableState());
			}
			if (isAttachedToWindow()) {
				highlight.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
			}
			// Set callback last, since the view may still be initializing.
			highlight.setCallback(this);
		}
		invalidate();
	}

	public boolean isDefaultFocusHighlightNeeded(Drawable background, Drawable foreground) {
		final boolean lackFocusState = (background == null || !background.isStateful()
				|| !background.hasFocusStateSpecified())
				&& (foreground == null || !foreground.isStateful()
				|| !foreground.hasFocusStateSpecified());
		return !isInTouchMode() && getDefaultFocusHighlightEnabled() && lackFocusState
				&& isAttachedToWindow() && sUseDefaultFocusHighlight;
	}

	private void switchDefaultFocusHighlight() {
		if (isFocused()) {
			final boolean needed = isDefaultFocusHighlightNeeded(background,
					mForegroundInfo == null ? null : mForegroundInfo.mDrawable);
			final boolean active = mDefaultFocusHighlight != null;
			if (needed && !active) {
				setDefaultFocusHighlight(getDefaultFocusHighlightDrawable());
			} else if (!needed && active) {
				// The highlight is no longer needed, so tear it down.
				setDefaultFocusHighlight(null);
			}
		}
	}

	/* ATL: drawn while GTK shows the focus (AOSP: while focused, which outside of touch mode is the same) */
	private void drawDefaultFocusHighlight(Canvas canvas) {
		if (mDefaultFocusHighlight != null && focus_shown) {
			mDefaultFocusHighlight.setBounds(0, 0, getWidth(), getHeight());
			mDefaultFocusHighlight.draw(canvas);
		}
	}

	public View(Context context) {
		this(context, null);
	}

	public final ViewParent getParent() {
		return parent;
	}

	@Export
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		applyLayoutParams(params);
		requestLayout();
	}

	/* without requesting a layout: ViewGroup uses this when it adds or re-attaches a child, as AOSP
	 * does by setting the child's params directly (RecyclerView re-attaches its children in every
	 * layout pass) */
	void applyLayoutParams(ViewGroup.LayoutParams params) {
		if (params == null) {
			throw new NullPointerException("Layout parameters cannot be null");
		}
		layoutParamsRtl = isLayoutRtl();
		params.resolveLayoutDirection(getLayoutDirection());

		int gravity = params.gravity;
		if (gravity == -1 && parent instanceof View)
			gravity = ((View)parent).gravity;
		if (gravity != -1)
			gravity = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());

		int leftMargin = 0;
		int topMargin = 0;
		int rightMargin = 0;
		int bottomMargin = 0;
		if (params instanceof ViewGroup.MarginLayoutParams) {
			leftMargin = ((ViewGroup.MarginLayoutParams)params).leftMargin;
			topMargin = ((ViewGroup.MarginLayoutParams)params).topMargin;
			rightMargin = ((ViewGroup.MarginLayoutParams)params).rightMargin;
			bottomMargin = ((ViewGroup.MarginLayoutParams)params).bottomMargin;
		}

		native_setLayoutParams(widget, params.width, params.height, gravity, params.weight, leftMargin, topMargin, rightMargin, bottomMargin);

		layout_params = params;
	}

	public ViewGroup.LayoutParams getLayoutParams() {
		return layout_params;
	}

	@Export
	protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
		this.measuredWidth = measuredWidth;
		this.measuredHeight = measuredHeight;
	}

	public Resources getResources() {
		return this.context.getResources();
	}

	public void setGravity(int gravity) {
		this.gravity = gravity;
	}

	private OnTouchListener on_touch_listener = null;

	@Export
	public boolean onTouchEventInternal(MotionEvent event, boolean handle_gestures) {
		boolean handled = false;
		if (on_touch_listener != null)
			handled = on_touch_listener.onTouch(this, event);

		if (!handled)
			handled = onTouchEvent(event);

		if (handle_gestures && !handled && event.getAction() == MotionEvent.ACTION_UP)
			handled = performClick();

		return handled;
	}

	private class LongClickRunnable implements Runnable {
		private boolean long_click_done = false;

		@Override
		public void run() {
			long_click_done = performLongClick();
		}
	}

	private LongClickRunnable pendingLongClick = null;

	/* AOSP View.CheckForTap, UnsetPressedState and isInScrollingContainer: a view inside a scrolling
	 * container shows the pressed state (and the ripple) only after the tap timeout, so that a scroll
	 * gesture doesn't flash it */
	private class CheckForTap implements Runnable {
		float x, y;

		@Override
		public void run() {
			pendingCheckForTap = null;
			drawableHotspotChanged(x, y);
			setPressed(true);
		}
	}

	private CheckForTap pendingCheckForTap = null;

	private final Runnable mUnsetPressedState = new Runnable() {
		@Override
		public void run() {
			setPressed(false);
		}
	};

	private boolean isInScrollingContainer() {
		ViewParent p = getParent();
		while (p != null && p instanceof ViewGroup) {
			if (((ViewGroup)p).shouldDelayChildPressedState()) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	private void removeTapCallback() {
		if (pendingCheckForTap != null) {
			removeCallbacks(pendingCheckForTap);
			pendingCheckForTap = null;
		}
	}

	@Export
	public boolean onTouchEvent(MotionEvent event) {
		if (isClickable() || isLongClickable()) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					if (isLongClickable()) {
						cancelLongPress();
						pendingLongClick = new LongClickRunnable();
						postDelayed(pendingLongClick, ViewConfiguration.getLongPressTimeout());
					}
					if (isInScrollingContainer()) {
						removeTapCallback();
						pendingCheckForTap = new CheckForTap();
						pendingCheckForTap.x = event.getX();
						pendingCheckForTap.y = event.getY();
						postDelayed(pendingCheckForTap, ViewConfiguration.getTapTimeout());
					} else {
						drawableHotspotChanged(event.getX(), event.getY());
						setPressed(true);
					}
					return true;
				case MotionEvent.ACTION_MOVE:
					drawableHotspotChanged(event.getX(), event.getY());
					if (event.getX() < 0 || event.getY() < 0 || event.getX() > getWidth() || event.getY() > getHeight()) {
						cancelLongPress();
						removeTapCallback();
						if (isPressed()) {
							setPressed(false);
						}
					}
					return true;
				case MotionEvent.ACTION_UP: {
					boolean long_click_done = pendingLongClick != null && pendingLongClick.long_click_done;
					cancelLongPress();
					if (pendingCheckForTap != null) {
						// pressed only now, so that the tap shows
						removeTapCallback();
						drawableHotspotChanged(event.getX(), event.getY());
						setPressed(true);
					}
					boolean handled = true;
					if (isClickable() && !long_click_done && event.getX() >= 0 && event.getY() >= 0 && event.getX() <= getWidth() && event.getY() <= getHeight())
						handled = performClick();
					if (isPressed() && !post(mUnsetPressedState)) {
						mUnsetPressedState.run();
					}
					return handled;
				}
				case MotionEvent.ACTION_CANCEL:
					cancelLongPress();
					removeTapCallback();
					setPressed(false);
					return true;
			}
		}

		return false;
	}

	public void setOnTouchListener(OnTouchListener l) {
		nativeSetOnTouchListener(widget);
		on_touch_listener = l;
		updateTouchTarget();
	}

	/* GTK gives a pointer event only to the widget under the pointer and its ancestors, while
	 * Android passes a touch that a view does not consume on to the views below it, e.g. from an
	 * icon to the clickable touch target behind it. In the Java dispatch (ViewGroup.dispatchTouchEvent)
	 * this is AOSP's own rule and needs nothing here; it is only the GTK path (native GTK widgets
	 * such as a ScrollView and its content, popups) that still lets GTK's picking decide, and AOSP
	 * has no counterpart for that.
	 * A subclass returns true here if it consumes touches only by AOSP's View.onTouchEvent() rule
	 * (clickable or long clickable) or by an OnTouchListener; such a view is left out of GTK's
	 * picking while none of these applies, so the event reaches what lies below it. */
	protected boolean passesUnconsumedTouches() {
		return false;
	}

	protected void updateTouchTarget() {
		if (passesUnconsumedTouches())
			native_setCanTarget(widget, isClickable() || isLongClickable() || on_touch_listener != null);
	}
	private native void native_setCanTarget(long widget, boolean can_target);
	protected native void nativeSetOnTouchListener(long widget);
	private OnApplyWindowInsetsListener on_apply_window_insets_listener = null;
	public void setOnClickListener(OnClickListener l) {
		if (!isClickable())
			setClickable(true);
		nativeSetOnClickListener(widget);
		getListenerInfo().mOnClickListener = l;
	}
	protected native void nativeSetOnClickListener(long widget);

	private OnScrollChangeListener on_scroll_change_listener = null;
	public void setOnScrollChangeListener(OnScrollChangeListener l) {}

	public /*native*/ void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {}
	public final int getWidth() {
		return right - left;
	}
	public final int getHeight() {
		return bottom - top;
	}

	protected native long native_constructor(Context context, AttributeSet attrs); // will create a custom GtkWidget with a custom drawing function
	private static native boolean native_hasAndroidLayout(long widget);
	private native void native_drawThroughJava(long widget);
	public native void native_setLayoutParams(long widget, int width, int height, int gravity, float weight, int leftMargin, int topMargin, int rightMargin, int bottomMargin);
	protected native void native_destructor(long widget);
	/**
	 * We decide between simple widgets which handles MEASURE_SPEC_AT_MOST the same way as
	 * MEASURE_SPEC_EXACTLY, and complex widgets which handles MEASURE_SPEC_AT_MOST by measuring the content
	 */
	protected native void native_measure(long widget, int widthMeasureSpec, int heightMeasureSpec);
	protected native void native_layout(long widget, int l, int t, int r, int b);
	protected native void native_requestLayout(long widget);
	protected native void native_setBackgroundDrawable(long widget, long paintable);
	protected native void native_queueAllocate(long widget);

	protected native void native_addClass(long widget, String className);
	protected native void native_removeClass(long widget, String className);

	protected native void native_addClasses(long widget, String[] classNames);
	protected native void native_removeClasses(long widget, String[] classNames);

	protected native void native_drawBackground(long widget, long snapshot);
	protected native void native_drawContent(long widget, long snapshot);
	protected void native_drawChildren(long widget, long snapshot) {} // override in ViewGroup

	/* should be called from onTouchEvent() of GtkWidget implementations */
	protected native boolean native_gtkEventPropagation();
	/* used by the DecorView to register a GtkEventController and wire it to dispatchTouchEvent() */
	protected native void native_registerTouchEventPropagation(long widget);

	// --- stubs

	/* kept for getContentDescription() (e.g. Toolbar and tooltips); there are no accessibility services */
	private CharSequence contentDescription;

	public void setContentDescription(CharSequence contentDescription) {
		this.contentDescription = contentDescription;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setOnKeyListener(OnKeyListener l) {}

	public void setFocusable(boolean focusable) {
		atl_focusable = focusable;
		atl_focusable_set = true;
		native_setNativeFocusable(widget, focusable);
		updateKeyboardFocusable();
	}
	public void setFocusable(int focusable) {}
	private boolean focusable_in_touch_mode = false;

	public void setFocusableInTouchMode(boolean focusableInTouchMode) {
		focusable_in_touch_mode = focusableInTouchMode;
	}

	public final boolean isFocusableInTouchMode() {
		return focusable_in_touch_mode;
	}
	/**
	 * Gives focus to this view or the descendant the layout asked for with <requestFocus/> (AOSP View.restoreDefaultFocus()).
	 */
	public boolean restoreDefaultFocus() {
		return requestFocus(View.FOCUS_DOWN);
	}

	public final boolean requestFocus() {
		return requestFocus(View.FOCUS_DOWN);
	}
	public final boolean requestFocus(int direction) {
		return requestFocus(direction, null);
	}
	/* AOSP: whether the view took the focus; ATL: whether GTK gave it to the view's widget */
	public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		if (atl_enabled && atl_focusable && visibility == VISIBLE) {
			return nativeRequestFocus(widget, direction);
		}
		return false;
	}
	public final boolean requestFocusFromTouch() {
		// TODO: if (isInTouchMode()) leave touch mode
		return requestFocus(View.FOCUS_DOWN);
	}
	private native boolean nativeRequestFocus(long widget, int direction);

	private native void nativeSetFullscreen(long widget, boolean fullscreen);

	public void setSystemUiVisibility(int visibility) {
		/* LAYOUT_FULLSCREEN mode would make the app window continue behind semi-transparent status bar.
		   Such a mode doesn't exist on desktop Linux, so we just use normal fullscreen for both modes. */
		nativeSetFullscreen(widget, (visibility & (View.SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)) != 0);
		system_ui_visibility = visibility;
	}
	public int getSystemUiVisibility() {
		return system_ui_visibility;
	};

	@Export
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (haveCustomMeasure) // calling native_measure here would create infinite loop
			setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
		else {
			native_measure(widget, widthMeasureSpec, heightMeasureSpec);
			// like AOSP's views, never measure below android:minWidth/minHeight (if the spec allows)
			int width = getMeasuredWidth();
			int height = getMeasuredHeight();
			int minWidth = getSuggestedMinimumWidth();
			int minHeight = getSuggestedMinimumHeight();
			if (width < minWidth || height < minHeight)
				setMeasuredDimension(resolveSize(Math.max(width, minWidth), widthMeasureSpec), resolveSize(Math.max(height, minHeight), heightMeasureSpec));
		}
	}

	public void setPressed(boolean pressed) {
		final boolean needsRefresh = pressed != ((mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED);

		if (pressed) {
			mPrivateFlags |= PFLAG_PRESSED;
		} else {
			mPrivateFlags &= ~PFLAG_PRESSED;
		}

		if (needsRefresh) {
			refreshDrawableState();
		}
		dispatchSetPressed(pressed);
	}

	protected void dispatchSetPressed(boolean pressed) {
	}

	/* AOSP; the long press is GTK's, so there is no callback to remove */
	private void resetPressedState() {
		if (!isEnabled()) {
			return;
		}

		if (isPressed()) {
			setPressed(false);
		}
	}

	public void setSelected(boolean selected) {
		//noinspection DoubleNegation
		if (((mPrivateFlags & PFLAG_SELECTED) != 0) != selected) {
			mPrivateFlags = (mPrivateFlags & ~PFLAG_SELECTED) | (selected ? PFLAG_SELECTED : 0);
			if (!selected) resetPressedState();
			invalidate();
			refreshDrawableState();
			dispatchSetSelected(selected);
		}
	}

	protected void dispatchSetSelected(boolean selected) {
	}

	public native Window native_get_window(long widget);
	public ViewTreeObserver getViewTreeObserver() {
		Window window = native_get_window(widget);
		if (window != null) {
			if (window.view_tree_observer == null)
				window.view_tree_observer = new ViewTreeObserver(window);
			return window.view_tree_observer;
		} else {
			floating_observer = new ViewTreeObserver(null);
			return floating_observer;
		}
	}

	protected void onFinishInflate() {}

	public void invalidateDrawable(Drawable drawable) {
		invalidate();
	}

	public void scheduleDrawable(Drawable drawable, Runnable runnable, long time) {
		postDelayed(runnable, time - SystemClock.uptimeMillis());
	}

	public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
		/* TODO */
	}

	public void unscheduleDrawable(Drawable drawable) {}

	public void invalidate(Rect dirty) {
		invalidate();
	}
	public void invalidate(int l, int t, int r, int b) {
		invalidate();
	}
	public void invalidate() {
		propagateInvalidation();
		nativeInvalidate(widget);
	}
	private static native void nativeInvalidate(long widget);

	// called from native code for GTK widgets or from invalidate() for Java widgets
	@Export
	private void propagateInvalidation() {
		for (View child = this; child.parent instanceof View; child = (View)child.parent)
			child.parent.onDescendantInvalidated(child, this);
	}

	protected native void native_setBackgroundColor(long widget, int color);
	public void setBackgroundColor(int color) {
		background = new ColorDrawable(color);
		native_setBackgroundColor(widget, color);
	}

	native void native_setVisibility(long widget, int visibility, float alpha, boolean sensitive);

	/* like ViewGroup.dispatchDraw() in AOSP, an invisible view is drawn while it has an animation (fade in/out) */
	private int getDrawnVisibility() {
		return visibility == INVISIBLE && currentAnimation != null ? VISIBLE : visibility;
	}

	protected void onVisibilityChanged(View changedView, int visibility) {
	}

	protected void dispatchVisibilityChanged(View changedView, int visibility) {
		onVisibilityChanged(changedView, visibility);
	}

	public void setVisibility(int visibility) {
		if (visibility == this.visibility)
			return;
		if ((visibility == View.GONE) != (this.visibility == View.GONE) && parent instanceof ViewGroup) {
			((ViewGroup)parent).requestLayout();
		}
		this.visibility = visibility;
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
		/* a view that goes while it animates: the animation runs on as on AOSP (see GoneAnimationTicker) */
		if (visibility == GONE && currentAnimation != null && animationMore)
			getHandler().post(new GoneAnimationTicker(currentAnimation));
		dispatchVisibilityChanged(this, visibility);
	}

	public void setPadding(int left, int top, int right, int bottom) {
		userPaddingStart = UNDEFINED_PADDING;
		userPaddingEnd = UNDEFINED_PADDING;
		userPaddingLeftInitial = left;
		userPaddingRightInitial = right;
		paddingLeft = left;
		paddingTop = top;
		paddingRight = right;
		paddingBottom = bottom;
		native_setPadding(widget, left, top, right, bottom);
	}
	public native void native_setPadding(long widget, int left, int top, int right, int bottom);

	public void setBackgroundResource(int resid) {
		setBackground(resid == 0 ? null : getContext().getDrawable(resid));
	}

	public void getHitRect(Rect outRect) {
		outRect.set(left, top, right, bottom);
	}
	public final boolean getLocalVisibleRect(Rect r) { return false; }

	@Export
	public final int getScrollX() {
		return scrollX;
	}

	@Export
	public final int getScrollY() {
		return scrollY;
	}

	public void scrollTo(int x, int y) {
		scrollX = x;
		scrollY = y;
		invalidate();
	}

	protected int computeVerticalScrollOffset() {
		return getScrollY();
	}

	protected int computeHorizontalScrollOffset() {
		return getScrollX();
	}

	/* Copyright (C) 2006 The Android Open Source Project */
	protected boolean overScrollBy(int deltaX, int deltaY,
	                               int scrollX, int scrollY,
	                               int scrollRangeX, int scrollRangeY,
	                               int maxOverScrollX, int maxOverScrollY,
	                               boolean isTouchEvent) {
		final int overScrollMode = getOverScrollMode();
		final boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
		final boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
		final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS ||
		                                     (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
		final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS ||
		                                   (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

		int newScrollX = scrollX + deltaX;
		if (!overScrollHorizontal) {
			maxOverScrollX = 0;
		}

		int newScrollY = scrollY + deltaY;
		if (!overScrollVertical) {
			maxOverScrollY = 0;
		}

		// Clamp values if at the limits and record
		final int left = -maxOverScrollX;
		final int right = maxOverScrollX + scrollRangeX;
		final int top = -maxOverScrollY;
		final int bottom = maxOverScrollY + scrollRangeY;

		boolean clampedX = false;
		if (newScrollX > right) {
			newScrollX = right;
			clampedX = true;
		} else if (newScrollX < left) {
			newScrollX = left;
			clampedX = true;
		}

		boolean clampedY = false;
		if (newScrollY > bottom) {
			newScrollY = bottom;
			clampedY = true;
		} else if (newScrollY < top) {
			newScrollY = top;
			clampedY = true;
		}

		onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

		return clampedX || clampedY;
	}

	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {}

	protected void onScrollChanged(int l, int t, int oldl, int oldt) {}

	public void getLocationOnScreen(int[] location) {
		Rect rect = new Rect();
		getGlobalVisibleRect(rect);
		location[0] = rect.left;
		location[1] = rect.top;
	}

	public boolean performHapticFeedback(int feedbackConstant) {
		return performHapticFeedback(feedbackConstant, 0);
	}

	public boolean performHapticFeedback(int feedbackConstant, int flags) {
		// arbitrary; TODO: better mimic what AOSP does
		Vibrator vibrator = (Vibrator)getContext().getSystemService("vibrator");
		vibrator.vibrate(10);
		return true; // FIXME why is it not void
	}

	public int getPaddingLeft() {
		return paddingLeft;
	}

	public int getPaddingRight() {
		return paddingRight;
	}

	public int getPaddingTop() {
		return paddingTop;
	}

	public int getPaddingBottom() {
		return paddingBottom;
	}

	public int getPaddingStart() {
		return isLayoutRtl() ? paddingRight : paddingLeft;
	}

	public int getPaddingEnd() {
		return isLayoutRtl() ? paddingLeft : paddingRight;
	}

	public void postInvalidate() {
		getHandler().post(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		});
	}

	public void postInvalidate(final int left, final int top, final int right, final int bottom) {
		getHandler().post(new Runnable() {
			@Override
			public void run() {
				invalidate(left, top, right, bottom);
			}
		});
	}

	public void setOnGenericMotionListener(View.OnGenericMotionListener l) {}

	/* AOSP's AttachInfo.mWindowToken (ViewRootImpl's IWindow binder): one per view hierarchy, kept by the
	 * root view of a window as for rootHasWindowFocus; null while the view is not attached */
	private IBinder windowToken;

	public IBinder getWindowToken() {
		if (!isAttachedToWindow())
			return null;
		View root = getRootView();
		if (root.windowToken == null)
			root.windowToken = new Binder();
		return root.windowToken;
	}

	public void getLocationInWindow(int[] location) {
		getLocationOnScreen(location);
	}

	/* AOSP View.addOnAttachStateChangeListener() */
	public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
		ListenerInfo li = getListenerInfo();
		if (li.mOnAttachStateChangeListeners == null) {
			li.mOnAttachStateChangeListeners = new CopyOnWriteArrayList<OnAttachStateChangeListener>();
		}
		li.mOnAttachStateChangeListeners.add(listener);
	}

	public Context getContext() {
		return this.context;
	}

	public void refreshDrawableState() {
		mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
		drawableStateChanged();

		ViewParent parent = this.parent;
		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).childDrawableStateChanged(this);
		}
	}

	/* AOSP View.drawableStateChanged(), without the scroll bar */
	protected void drawableStateChanged() {
		final int[] state = getDrawableState();
		boolean changed = false;

		final Drawable bg = background;
		if (bg != null && bg.isStateful()) {
			changed |= bg.setState(state);
		}

		final Drawable hl = mDefaultFocusHighlight;
		if (hl != null && hl.isStateful()) {
			changed |= hl.setState(state);
		}

		final Drawable fg = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
		if (fg != null && fg.isStateful()) {
			changed |= fg.setState(state);
		}

		if (state_list_animator != null) {
			state_list_animator.setState(state);
		}

		if (changed) {
			invalidate();
		}
	}

	public void setAccessibilityDelegate(AccessibilityDelegate delegate) {}

	public static class AccessibilityDelegate {
		public void sendAccessibilityEvent(View view, int eventType) {}
	}

	public Drawable getBackground() {
		return background;
	}

	public void setClickable(boolean clickable) {
		this.clickable = clickable;
		updateTouchTarget();
		updateKeyboardFocusable();
	}

	/* like AOSP's FOCUSABLE_AUTO: a clickable view can be focused with the keyboard (Tab), and
	 * Enter or Space click it (View.onKeyUp()); clicking it with the pointer does not focus it */
	/* AOSP's focusable: android:focusable/setFocusable(), or FOCUSABLE_AUTO, which follows clickable */
	private boolean isFocusableResolved() {
		return atl_focusable && (atl_focusable_set || clickable);
	}
	private void updateKeyboardFocusable() {
		native_setKeyboardFocusable(widget, isFocusableResolved());
	}
	private native void native_setKeyboardFocusable(long widget, boolean focusable);
	private native void native_setNativeFocusable(long widget, boolean focusable);

	public void setEnabled(boolean enabled) {
		boolean changed = atl_enabled != enabled;
		atl_enabled = enabled;
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
		if (changed)
			refreshDrawableState();
	}

	public CharSequence getContentDescription() {
		return contentDescription;
	}

	private OnLongClickListener on_long_click_listener = null;

	@Export
	public boolean performLongClick(float x, float y) {
		if (on_long_click_listener != null) {
			return on_long_click_listener.onLongClick(this);
		}
		return false;
	}
	public boolean performLongClick() {
		return performLongClick(Float.NaN, Float.NaN);
	}
	public void setOnLongClickListener(OnLongClickListener listener) {
		if (!isLongClickable())
			setLongClickable(true);
		nativeSetOnLongClickListener(widget);
		on_long_click_listener = listener;
		updateTouchTarget();
	}
	protected native void nativeSetOnLongClickListener(long widget);

	public void setLongClickable(boolean longClickable) {
		this.longclickable = longClickable;
		updateTouchTarget();
	}

	public void setOnHoverListener(OnHoverListener listener) {}

	@Export
	public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
		if ((mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT || widthMeasureSpec != oldWidthMeasureSpec || heightMeasureSpec != oldHeightMeasureSpec) {
			mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
			resolveRtlPropertiesIfNeeded();
			oldWidthMeasureSpec = widthMeasureSpec;
			oldHeightMeasureSpec = heightMeasureSpec;
			onMeasure(widthMeasureSpec, heightMeasureSpec);
			mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
		}
	}

	public final int getMeasuredState() {
		return (measuredWidth&MEASURED_STATE_MASK)
				| ((measuredHeight>>MEASURED_HEIGHT_STATE_SHIFT)
						& (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
	}

	public static int combineMeasuredStates(int curState, int newState) {
		return curState | newState;
	}

	@Export
	protected int getSuggestedMinimumHeight() {
		return getMinimumHeight();
	}

	@Export
	protected int getSuggestedMinimumWidth() {
		return getMinimumWidth();
	}

	/**
	 * Utility to reconcile a desired size and state, with constraints imposed
	 * by a MeasureSpec.  Will take the desired size, unless a different size
	 * is imposed by the constraints.  The returned value is a compound integer,
	 * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
	 * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
	 * size is smaller than the size the view wants to be.
	 *
	 * @param size How big the view wants to be
	 * @param measureSpec Constraints imposed by the parent
	 * @return Size information bit mask as defined by
	 * {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
	 */
	public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				result = size;
				break;
			case MeasureSpec.AT_MOST:
				if (specSize < size) {
					result = specSize | MEASURED_STATE_TOO_SMALL;
				} else {
					result = size;
				}
				break;
			case MeasureSpec.EXACTLY:
				result = specSize;
				break;
		}
		return result | (childMeasuredState & MEASURED_STATE_MASK);
	}

	public static int resolveSize(int size, int measureSpec) {
		return resolveSizeAndState(size, measureSpec, 0) & MEASURED_SIZE_MASK;
	}

	@Export
	public final int getMeasuredWidth() {
		return this.measuredWidth & MEASURED_SIZE_MASK;
	}

	public final int getMeasuredHeightAndState() {
		return measuredHeight;
	}

	@Export
	public final int getMeasuredHeight() {
		return this.measuredHeight & MEASURED_SIZE_MASK;
	}

	@Export
	protected void onLayout(boolean changed, int l, int t, int r, int b) {}

	private boolean layouted = false;
	private boolean in_layout = false;

	public void layout(int l, int t, int r, int b) {
		boolean sizeChanged = r - l != right - left || b - t != bottom - top;
		boolean changed = sizeChanged || l != left || t != top;
		int oldL = left;
		int oldT = top;
		int oldR = right;
		int oldB = bottom;
		int oldW = right - left;
		int oldH = bottom - top;
		this.layouted = true;
		this.left = l;
		this.top = t;
		this.right = r;
		this.bottom = b;
		if (this.in_layout)                   // check re-entrancy
			native_queueAllocate(widget); // GTK is not re-entrant-safe. Queue allocation
		else {
			layoutCall = true; // layoutInternal() leaves the layout change listeners to this method
			native_layout(widget, l, t, r, b);
			layoutCall = false;
		}
		/* GTK lays out native widgets (TextView, Button, ...) without layoutInternal(): let them
		 * know their size and layout like AOSP's layout() does (MaterialButton places its icon in
		 * onLayout()). Native containers (ScrollView) are left alone, GTK places their children */
		if (!(this instanceof ViewGroup) && !native_hasAndroidLayout(widget)) {
			if (sizeChanged)
				onSizeChanged(r - l, b - t, oldW, oldH);
			onLayout(changed, l, t, r, b);
		}
		// native widgets without Java children (e.g. WebView) are never laid out through layoutInternal()
		dispatchLayoutChange(changed, l, t, r, b, oldL, oldT, oldR, oldB);
		mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
		if (sizeChanged && needsOutline())
			invalidateOutline();
	}

	/* layout() is on the stack: it calls the OnLayoutChangeListeners itself, with the bounds from before */
	private boolean layoutCall;

	/* the end of AOSP's View.layout(): the OnLayoutChangeListeners are called once per change of the
	 * view's bounds, and once per layout after a measure (PFLAG_LAYOUT_REQUIRED) even if they stay the same */
	private void dispatchLayoutChange(boolean changed, int l, int t, int r, int b, int oldL, int oldT, int oldR, int oldB) {
		if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
			mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;

			ListenerInfo li = mListenerInfo;
			if (li != null && li.mOnLayoutChangeListeners != null) {
				ArrayList<OnLayoutChangeListener> listenersCopy = (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
				int numListeners = listenersCopy.size();
				for (int i = 0; i < numListeners; ++i) {
					listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
				}
			}
		}
	}

	/** Helper function to be called from GTKs LayoutManager via JNI */
	@Export
	private void layoutInternal(int width, int height) {
		in_layout = true;
		// if the layout is triggered from a native widget, we might not have measured yet
		// also measure again if a descendant requested a layout without our size changing
		if (!(parent instanceof ViewGroup) && ((mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT || width != getMeasuredWidth() || height != getMeasuredHeight())) {
			requestLayout();
			measure(width | MeasureSpec.EXACTLY, height | MeasureSpec.EXACTLY);
		}
		boolean changed = oldWidth != width || oldHeight != height;
		int oldL = left;
		int oldT = top;
		int oldR = left + oldWidth;
		int oldB = top + oldHeight;
		if (changed) {
			onSizeChanged(width, height, oldWidth, oldHeight);
			if (needsOutline())
				invalidateOutline();
		}
		bottom = top + height;
		right = left + width;
		if (this instanceof ViewGroup) {
			for (View child : ((ViewGroup)this).children)
				child.layouted = false;
		}
		onLayout(changed, 0, 0, width, height);
		// ReactViewGroup has onLayout implemented as no-op, so we need to walk children ourself in such cases
		if (this instanceof ViewGroup) {
			for (View child : ((ViewGroup)this).children) {
				if (!child.layouted && child.visibility != GONE)
					child.layout(child.left, child.top, child.right, child.bottom);
			}
		}
		if (!layoutCall)
			dispatchLayoutChange(changed, left, top, right, bottom, oldL, oldT, oldR, oldB);
		mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
		oldWidth = width;
		oldHeight = height;
		in_layout = false;
	}

	/** Prefill the measureSpec cache, so that AndroidLayout can use it from native code if GTK measure spec is incomplete */
	public void internalSetDefaultMeasureSpec(int widthMeasureSpec, int heightMeasureSpec) {
		this.oldWidthMeasureSpec = widthMeasureSpec;
		this.oldHeightMeasureSpec = heightMeasureSpec;
		requestLayout();
	}

	public int getLeft() {
		return left;
	}
	public int getTop() {
		return top;
	}
	public int getRight() {
		return right;
	}
	public int getBottom() {
		return bottom;
	}

	public void offsetTopAndBottom(int offset) {
		layout(left, top + offset, right, bottom + offset);
	}

	public void offsetLeftAndRight(int offset) {
		layout(left + offset, top, right + offset, bottom);
	}

	public void setBackgroundDrawable(Drawable backgroundDrawable) {
		this.background = backgroundDrawable;
		if (backgroundDrawable instanceof ColorDrawable) {
			native_setBackgroundColor(widget, ((ColorDrawable)backgroundDrawable).getColor());
			if (needsOutline())
				invalidateOutline();
			return;
		}
		if (backgroundDrawable != null) {
			backgroundDrawable.setCallback(this);
			if (backgroundTint != 0)
				backgroundDrawable.setTint(backgroundTint);
		}
		native_setBackgroundDrawable(widget, backgroundDrawable != null ? backgroundDrawable.paintable : 0);
		if (needsOutline())
			invalidateOutline();
	}

	public int getOverScrollMode() { return 0; }

	public void setFitsSystemWindows(boolean fitsSystemWindows) {}

	/* hidden in AOSP, looked up by AppCompat via reflection; like setFitsSystemWindows,
	 * the system window flags have no effect here */
	public void makeOptionalFitsSystemWindows() {}

	public void setWillNotDraw(boolean value) {}

	public void setScrollContainer(boolean isScrollContainer) {}

	public boolean removeCallbacks(Runnable action) {
		getHandler().removeCallbacks(action);
		return true;
	}

	public void requestLayout() {
		native_requestLayout(widget);
		View view = this;
		while ((view.mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0) {
			view.mPrivateFlags |= PFLAG_FORCE_LAYOUT;
			if (view.parent instanceof View)
				view = (View)view.parent;
			else
				break;
		}
	};

	public void setOverScrollMode(int mode) {}

	@Export
	public int getId() { return id; }

	@Export
	public String getIdName() {
		if (this.id == View.NO_ID) {
			return "NO_ID";
		}

		try {
			return getResources().getResourceName(this.id);
		} catch (Resources.NotFoundException e) {
			return "NOT_FOUND";
		}
	}

	@Export
	public String getAllSuperClasses() {
		StringBuilder sb = new StringBuilder();
		Class<?> currentClass = getClass();

		sb.append(currentClass.getCanonicalName());
		currentClass = currentClass.getSuperclass();
		while (currentClass != null) {
			sb.append(" << ").append(currentClass.getCanonicalName());
			currentClass = currentClass.getSuperclass();
		}
		return sb.toString();
	}

	public boolean postDelayed(Runnable action, long delayMillis) {
		getHandler().postDelayed(action, delayMillis);
		return true;
	}

	public boolean post(Runnable action) {
		getHandler().post(action);
		return true;
	}

	public void setSaveFromParentEnabled(boolean enabled) {
		saveFromParentEnabled = enabled;
	}

	public void setTag(int key, Object tag) {
		tags.put(key, tag);
	}
	public Object getTag(int key) {
		return tags.get(key);
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}
	public Object getTag() {
		return tag;
	}

	/* AOSP View.addOnLayoutChangeListener() / removeOnLayoutChangeListener() */
	public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
		ListenerInfo li = getListenerInfo();
		if (li.mOnLayoutChangeListeners == null) {
			li.mOnLayoutChangeListeners = new ArrayList<OnLayoutChangeListener>();
		}
		if (!li.mOnLayoutChangeListeners.contains(listener)) {
			li.mOnLayoutChangeListeners.add(listener);
		}
	}

	public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
		ListenerInfo li = mListenerInfo;
		if (li == null || li.mOnLayoutChangeListeners == null) {
			return;
		}
		li.mOnLayoutChangeListeners.remove(listener);
	}

	public boolean isSelected() {
		return (mPrivateFlags & PFLAG_SELECTED) != 0;
	}

	public void sendAccessibilityEvent(int eventType) {}

	public void setMinimumHeight(int minHeight) {
		this.minHeight = minHeight;
	}
	public void setMinimumWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	public void setActivated(boolean activated) {
		//noinspection DoubleNegation
		if (((mPrivateFlags & PFLAG_ACTIVATED) != 0) != activated) {
			mPrivateFlags = (mPrivateFlags & ~PFLAG_ACTIVATED) | (activated ? PFLAG_ACTIVATED : 0);
			invalidate();
			refreshDrawableState();
			dispatchSetActivated(activated);
		}
	}

	protected void dispatchSetActivated(boolean activated) {
	}

	public boolean isActivated() {
		return (mPrivateFlags & PFLAG_ACTIVATED) != 0;
	}

	public int getVisibility() { return visibility; }

	public boolean isInEditMode() { return false; }

	@Override
	@SuppressWarnings("deprecation")
	protected void finalize() throws Throwable {
		try {
			native_destructor(widget);
		} finally {
			super.finalize();
		}
	}

	public final int[] getDrawableState() {
		if ((mDrawableState != null) && ((mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) == 0)) {
			return mDrawableState;
		} else {
			mDrawableState = onCreateDrawableState(0);
			mPrivateFlags &= ~PFLAG_DRAWABLE_STATE_DIRTY;
			return mDrawableState;
		}
	}


	public void bringToFront() {}

	public boolean isEnabled() { return atl_enabled; }
	/* the GTK focus is in this view's widgets (ViewGroup refines this to AOSP's invariant) */
	public boolean hasFocus() { return nativeHasFocus(widget); }
	private static native boolean nativeHasFocus(long widget);
	public boolean isLayoutRequested() { return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT; }
	public int getBaseline() { return -1; }
	/* as AOSP for API >= 26: visible, enabled and focusable (FOCUSABLE_AUTO counts) */
	public boolean hasFocusable() {
		return visibility == VISIBLE && atl_enabled && isFocusableResolved();
	}
	private static native boolean nativeIsFocused(long widget);
	public boolean isFocused() {
		return nativeIsFocused(widget);
	}

	/**
	 * Cancels any animations for this view.
	 */
	public void clearAnimation() {
		if (currentAnimation != null) {
			currentAnimation.detach();
		}
		currentAnimation = null;
		native_setAnimating(widget, false);
		if (visibility == INVISIBLE)
			native_setVisibility(widget, visibility, alpha * transitionAlpha, atl_enabled);
		invalidate();
	}

	private ViewPropertyAnimator viewPropertyAnimator;
	public ViewPropertyAnimator animate() {
		if (viewPropertyAnimator == null)
			viewPropertyAnimator = new ViewPropertyAnimator(this);
		return viewPropertyAnimator;
	}

	public float getTranslationX() {
		return translationX;
	}

	public float getTranslationY() {
		return translationY;
	}

	public void setTranslationX(float translationX) {
		this.translationX = translationX;
		if (parent instanceof View)
			((View)parent).native_queueAllocate(((View)parent).widget);
	}

	public void setTranslationY(float translationY) {
		this.translationY = translationY;
		if (parent instanceof View)
			((View)parent).native_queueAllocate(((View)parent).widget);
	}

	public void setX(float x) {
		setTranslationX(x - left);
	}

	public void setY(float y) {
		setTranslationY(y - top);
	}

	public void setAlpha(float alpha) {
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
		this.alpha = alpha;
	}

	@Export
	public boolean onGenericMotionEvent(MotionEvent event) { return false; }

	@Export
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		return onGenericMotionEvent(event);
	}

	protected boolean awakenScrollBars() { return false; }

	protected native boolean native_getMatrix(long widget, long matrix);
	/* as AOSP's RenderProperties: translation, then rotation and scale about the pivot */
	public Matrix getMatrix() {
		if (matrix == null)
			matrix = new Matrix();
		matrix.setTranslate(translationX, translationY);
		matrix.preRotate(rotation, getPivotX(), getPivotY());
		matrix.preScale(scaleX, scaleY, getPivotX(), getPivotY());
		return matrix;
	}

	final Matrix getInverseMatrix() {
		if (inverseMatrix == null)
			inverseMatrix = new Matrix();
		getMatrix().invert(inverseMatrix);
		return inverseMatrix;
	}

	public boolean hasIdentityMatrix() {
		return translationX == 0 && translationY == 0 && !hasTransform;
	}

	/* GTK applies the transform when the parent allocates this view, see native_layout() */
	private void transformChanged() {
		boolean had = hasTransform;
		hasTransform = scaleX != 1 || scaleY != 1 || rotation != 0;
		if ((had || hasTransform) && parent instanceof View)
			((View)parent).native_queueAllocate(((View)parent).widget);
	}

	protected static final int[] EMPTY_STATE_SET = new int[0];

	protected static final int[] ENABLED_STATE_SET = new int[] {R.attr.state_enabled};

	protected static final int[] PRESSED_ENABLED_STATE_SET = new int[] {R.attr.state_pressed, R.attr.state_enabled};

	protected static final int[] SELECTED_STATE_SET = new int[] {R.attr.state_selected};

	protected static final int[] FOCUSED_STATE_SET = new int[] {R.attr.state_focused};

	/**
	 * Utility to return a default size. Uses the supplied size if the
	 * MeasureSpec imposed no constraints. Will get larger if allowed
	 * by the MeasureSpec.
	 *
	 * @param size Default size for this view
	 * @param measureSpec Constraints imposed by the parent
	 * @return The size this view should be.
	 */
	public static int getDefaultSize(int size, int measureSpec) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				result = size;
				break;
			case MeasureSpec.AT_MOST:
			case MeasureSpec.EXACTLY:
				result = specSize;
				break;
		}
		return result;
	}

	public static class BaseSavedState extends AbsSavedState {
		public BaseSavedState(Parcel source) {
			super(source);
		}

		public BaseSavedState(Parcel source, ClassLoader loader) {
			super(source, loader);
		}

		public BaseSavedState(Parcelable superState) {
			super(superState);
		}

		public static final Parcelable.Creator<BaseSavedState> CREATOR = new Parcelable.Creator<BaseSavedState>() {
			@Override
			public BaseSavedState createFromParcel(Parcel in) {
				return new BaseSavedState(in);
			}

			@Override
			public BaseSavedState[] newArray(int size) {
				return new BaseSavedState[size];
			}
		};
	}

	public void clearFocus() {}

	public static View inflate(Context context, int resource, ViewGroup root) {
		LayoutInflater factory = LayoutInflater.from(context);
		return factory.inflate(resource, root);
	}

	private boolean saveEnabled = true;
	private boolean saveFromParentEnabled = true;
	private static final int PFLAG_SAVE_STATE_CALLED   = 0x00020000;

	/* AOSP's view state saving (android-14 View.java), without the autofill state; ATL keeps
	 * SAVE_DISABLED and PARENT_SAVE_DISABLED as saveEnabled and saveFromParentEnabled */
	public void saveHierarchyState(SparseArray<Parcelable> container) {
		dispatchSaveInstanceState(container);
	}

	protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
		if (id != NO_ID && saveEnabled) {
			mPrivateFlags &= ~PFLAG_SAVE_STATE_CALLED;
			Parcelable state = onSaveInstanceState();
			if ((mPrivateFlags & PFLAG_SAVE_STATE_CALLED) == 0) {
				throw new IllegalStateException(
						"Derived class did not call super.onSaveInstanceState()");
			}
			if (state != null) {
				// Log.i("View", "Freezing #" + Integer.toHexString(mID)
				// + ": " + state);
				container.put(id, state);
			}
		}
	}

	protected Parcelable onSaveInstanceState() {
		mPrivateFlags |= PFLAG_SAVE_STATE_CALLED;
		return BaseSavedState.EMPTY_STATE;
	}

	public void restoreHierarchyState(SparseArray<Parcelable> container) {
		dispatchRestoreInstanceState(container);
	}

	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
		if (id != NO_ID) {
			Parcelable state = container.get(id);
			if (state != null) {
				// Log.i("View", "Restoreing #" + Integer.toHexString(mID)
				// + ": " + state);
				mPrivateFlags &= ~PFLAG_SAVE_STATE_CALLED;
				onRestoreInstanceState(state);
				if ((mPrivateFlags & PFLAG_SAVE_STATE_CALLED) == 0) {
					throw new IllegalStateException(
							"Derived class did not call super.onRestoreInstanceState()");
				}
			}
		}
	}

	protected void onRestoreInstanceState(Parcelable state) {
		mPrivateFlags |= PFLAG_SAVE_STATE_CALLED;
		if (state != null && !(state instanceof AbsSavedState)) {
			throw new IllegalArgumentException("Wrong state class, expecting View State but "
					+ "received " + state.getClass().toString() + " instead. This usually happens "
					+ "when two views of different type have the same id in the same hierarchy. "
					+ "This view's id is " + getId() + ". Make sure "
					+ "other views do not use the same id.");
		}
	}

	public boolean isSaveEnabled() {
		return saveEnabled;
	}

	public boolean isSaveFromParentEnabled() {
		return saveFromParentEnabled;
	}

	public void setDuplicateParentStateEnabled(boolean enabled) {
		duplicate_parent_state = enabled;
	}

	@Export
	/* AOSP View.performClick() (no autofill, no accessibility events) */
	public boolean performClick() {
		final boolean result;
		final ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnClickListener != null) {
			playSoundEffect(SoundEffectConstants.CLICK);
			li.mOnClickListener.onClick(this);
			result = true;
		} else {
			result = false;
		}
		return result;
	}

	public void playSoundEffect(int soundConstant) {}

	@Export
	public void computeScroll() {}

	public void jumpDrawablesToCurrentState() {
		if (state_list_animator != null)
			state_list_animator.jumpToCurrentState();
	}

	private OnFocusChangeListener onFocusChangeListener;

	public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
		onFocusChangeListener = l;
	}

	private boolean focus_shown;

	/* called from native code when the view's widget gains or loses the GTK focus; focusVisible is
	 * whether GTK shows the focus (keyboard navigation), which the drawable state follows */
	@Export
	private void dispatchFocusChanged(boolean gainFocus, boolean focusVisible) {
		if (focus_shown != focusVisible) {
			focus_shown = focusVisible;
			refreshDrawableState();
		}
		onFocusChanged(gainFocus, FOCUS_FORWARD, null);
	}

	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		// Here we check whether we still need the default focus highlight, and switch it on/off.
		switchDefaultFocusHighlight();

		if (onFocusChangeListener != null)
			onFocusChangeListener.onFocusChange(this, gainFocus);
	}


	public void setSaveEnabled(boolean enabled) {
		saveEnabled = enabled;
	}

	public boolean willNotDraw() { return false; }

	public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {}

	protected void onAnimationStart() {}

	protected void onAnimationEnd() {}

	/**
	 * Start the specified animation now.
	 */
	public void startAnimation(Animation animation) {
		animation.setStartTime(Animation.START_ON_FIRST_FRAME);
		setAnimation(animation);
		invalidate();
	}

	private native void native_setAnimating(long widget, boolean animating);

	/*
	 * Called by the wrapper widget while it draws this view, like View.applyLegacyAnimation() in
	 * AOSP: returns the transformation for this frame, or null when there is none, and sets
	 * animationMore if there are more frames. After the last frame the animation is cleared, unless
	 * it fills after (AOSP: ViewGroup.finishAnimatingView()).
	 */
	@Export
	private Transformation applyLegacyAnimation() {
		final Animation a = currentAnimation;
		if (a == null)
			return null;
		long drawingTime = AnimationUtils.currentAnimationTimeMillis();
		if (!a.isInitialized()) {
			View parentView = parent instanceof View ? (View)parent : this;
			a.initialize(getWidth(), getHeight(), parentView.getWidth(), parentView.getHeight());
			a.initializeInvalidateRegion(0, 0, getWidth(), getHeight());
			a.setListenerHandler(getHandler());
			onAnimationStart();
		}
		// animations are turned off (animator duration scale 0): show the end state at once
		if (ValueAnimator.getDurationScale() == 0) {
			if (a.getStartTime() == Animation.START_ON_FIRST_FRAME)
				a.setStartTime(drawingTime);
			drawingTime += a.computeDurationHint();
		}
		if (animationTransformation == null)
			animationTransformation = new Transformation();
		animationTransformation.clear();
		boolean more = a.getTransformation(drawingTime, animationTransformation, 1f);
		boolean ended = animationMore && !more && a.hasEnded();
		animationMore = more;
		if (ended) {
			// not while GTK draws; after the listener of the animation (it may change the visibility)
			getHandler().post(new Runnable() {
				@Override
				public void run() {
					if (currentAnimation != a)
						return;
					if (!a.getFillAfter())
						clearAnimation();
					onAnimationEnd();
				}
			});
		}
		return animationTransformation;
	}

	public void getDrawingRect(Rect rect) {
		rect.left = getScrollX();
		rect.top = getScrollY();
		rect.right = getScrollX() + getWidth();
		rect.bottom = getScrollY() + getHeight();
	}

	public Display getDisplay() {
		return new Display();
	}

	private int importantForAccessibility;
	public void setImportantForAccessibility(int importantForAccessibility) {
		this.importantForAccessibility = importantForAccessibility;
	}
	public int getImportantForAccessibility() {
		return importantForAccessibility;
	}

	public boolean getFitsSystemWindows() { return true; }

	public void setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener l) {
		on_apply_window_insets_listener = l;
	}

	/* AOSP, without PFLAG3_APPLYING_INSETS (ATL has no fitSystemWindows() to tell it to) */
	public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
		if (on_apply_window_insets_listener != null) {
			return on_apply_window_insets_listener.onApplyWindowInsets(this, insets);
		} else {
			return onApplyWindowInsets(insets);
		}
	}

	public final boolean isFocusable() { return atl_focusable; }
	@Export
	public boolean isClickable() { return clickable; }
	public boolean isLongClickable() { return longclickable; }

	/* like AOSP: LTR until the direction is resolved */
	public int getLayoutDirection() { return layoutRtl ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR; }

	/** @hide */
	public boolean isLayoutRtl() { return layoutRtl; }

	/** @hide the direction as set, not resolved */
	public int getRawLayoutDirection() { return rawLayoutDirection; }

	/** @hide */
	public boolean isLayoutDirectionInherited() { return rawLayoutDirection == LAYOUT_DIRECTION_INHERIT; }

	public void setLayoutDirection(int layoutDirection) {
		if (rawLayoutDirection == layoutDirection)
			return;
		rawLayoutDirection = layoutDirection;
		resetRtlProperties();
		resolveRtlPropertiesIfNeeded();
		requestLayout();
		invalidate();
	}

	/* AOSP's isRtlCompatibilityMode(): apps without android:supportsRtl stay LTR */
	private boolean hasRtlSupport() {
		ApplicationInfo info = getContext().getApplicationInfo();
		return info != null && info.hasRtlSupport() && info.targetSdkVersion >= 17 /* JELLY_BEAN_MR1 */;
	}

	/** @hide like AOSP: inherit from the parent once that is resolved; the root view of a window
	 * gets the configuration's direction there (as ViewRootImpl sets it on its view) */
	public boolean resolveLayoutDirection() {
		boolean rtl = false;
		if (hasRtlSupport()) {
			switch (rawLayoutDirection) {
				case LAYOUT_DIRECTION_INHERIT:
					if (!canResolveLayoutDirection())
						return false;
					if (parent instanceof View) {
						View p = (View)parent;
						if (!p.isLayoutDirectionResolved())
							return false;
						rtl = p.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
					} else {
						rtl = getResources().getConfiguration().getLayoutDirection() == LAYOUT_DIRECTION_RTL;
					}
					break;
				case LAYOUT_DIRECTION_RTL:
					rtl = true;
					break;
				case LAYOUT_DIRECTION_LOCALE:
					rtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL;
					break;
			}
		}
		layoutRtl = rtl;
		layoutDirectionResolved = true;
		return true;
	}

	/** @hide like AOSP: the layout direction first, then what depends on it (layout params,
	 * padding), and onRtlPropertiesChanged(); returns whether anything was resolved */
	public boolean resolveRtlPropertiesIfNeeded() {
		if (layoutDirectionResolved || !resolveLayoutDirection())
			return false;
		if (layout_params != null && layoutParamsRtl != layoutRtl)
			applyLayoutParams(layout_params);
		resolveTextAlignment();
		resolvePadding();
		onRtlPropertiesChanged(getLayoutDirection());
		return true;
	}

	/** @hide */
	public void resetRtlProperties() {
		layoutDirectionResolved = false;
		layoutRtl = false;
		resetResolvedTextAlignment();
	}

	/* paddingLeft/paddingRight from the relative and the absolute padding (AOSP's resolvePadding()) */
	private void resolvePaddingValues() {
		int left = userPaddingLeftInitial;
		int right = userPaddingRightInitial;
		if (layoutRtl) {
			if (userPaddingStart != UNDEFINED_PADDING)
				right = userPaddingStart;
			if (userPaddingEnd != UNDEFINED_PADDING)
				left = userPaddingEnd;
		} else {
			if (userPaddingStart != UNDEFINED_PADDING)
				left = userPaddingStart;
			if (userPaddingEnd != UNDEFINED_PADDING)
				right = userPaddingEnd;
		}
		paddingLeft = left;
		paddingRight = right;
	}

	/** @hide */
	public void resolvePadding() {
		int left = paddingLeft;
		int right = paddingRight;
		resolvePaddingValues();
		if (left != paddingLeft || right != paddingRight) {
			native_setPadding(widget, paddingLeft, paddingTop, paddingRight, paddingBottom);
			requestLayout();
		}
	}

	public void setBackground(Drawable background) {
		setBackgroundDrawable(background);
	}

	private float elevation;
	private float translation_z;
	private boolean has_shadow;
	public void setElevation(float elevation) {
		if (elevation != this.elevation) {
			this.elevation = elevation;
			invalidateOutline();
		}
	}
	public float getElevation() {
		return elevation;
	}

	public boolean isLaidOut() {
		return (mPrivateFlags3 & PFLAG3_IS_LAID_OUT) == PFLAG3_IS_LAID_OUT;
	}

	public void postOnAnimation(Runnable action) {
		postDelayed(action, 1000 / 60);
	}

	public void postOnAnimationDelayed(Runnable action, long delayMillis) {
		if (delayMillis < 1000 / 60)
			delayMillis = 1000 / 60;
		postDelayed(action, delayMillis);
	}

	public void setHorizontalScrollBarEnabled(boolean enabled) {}

	public void setVerticalScrollBarEnabled(boolean enabled) {}

	public void postInvalidateOnAnimation() {
		postInvalidate();
	}

	public void setPaddingRelative(int start, int top, int end, int bottom) {
		userPaddingStart = start;
		userPaddingEnd = end;
		if (isLayoutRtl()) {
			userPaddingLeftInitial = end;
			userPaddingRightInitial = start;
		} else {
			userPaddingLeftInitial = start;
			userPaddingRightInitial = end;
		}
		paddingLeft = userPaddingLeftInitial;
		paddingTop = top;
		paddingRight = userPaddingRightInitial;
		paddingBottom = bottom;
		native_setPadding(widget, paddingLeft, top, paddingRight, bottom);
	}

	private native boolean nativeIsAttachedToWindow(long widget);
	public boolean isAttachedToWindow() {
		return nativeIsAttachedToWindow(widget);
	}

	/* like AOSP, the whole window gets the insets dispatched again (on the next main loop iteration) */
	public void requestApplyInsets() {
		Window window = native_get_window(widget);
		if (window != null)
			window.requestApplyInsets();
	}

	/* the window's decor view overrides this and returns the window's dispatcher */
	public android.window.OnBackInvokedDispatcher findOnBackInvokedDispatcher() {
		return parent instanceof View ? ((View)parent).findOnBackInvokedDispatcher() : null;
	}

	public View getRootView() {
		View view = this;
		while (view.parent instanceof View) {
			view = (View)view.parent;
		}
		return view;
	}

	/* like AOSP: this view and all its ancestors are visible, and the topmost one is in a window
	 * (AOSP checks for the ViewRootImpl parent, the root views here have no parent) */
	public boolean isShown() {
		View current = this;
		while (current.visibility == VISIBLE) {
			if (!(current.parent instanceof View))
				return current.parent != null || current.isAttachedToWindow();
			current = (View)current.parent;
		}
		return false;
	}

	public int getWindowVisibility() { return VISIBLE; }

	public float getAlpha() { return alpha; }

	/* AOSP: the view if it has the focus */
	public View findFocus() { return isFocused() ? this : null; }

	public int getMinimumHeight() { return minHeight; }
	public int getMinimumWidth() { return minWidth; }

	public boolean isNestedScrollingEnabled() { return false; }

	private boolean clipToOutline;
	private boolean has_outline_clip;
	public void setClipToOutline(boolean clipToOutline) {
		if (this.clipToOutline == clipToOutline)
			return;
		this.clipToOutline = clipToOutline;
		invalidateOutline();
	}

	public boolean hasTransientState() { return false; }

	public final void cancelPendingInputEvents() {
		cancelLongPress();
	}

	public ViewOutlineProvider getOutlineProvider() { return outline_provider; }
	public void setOutlineProvider(ViewOutlineProvider provider) {
		outline_provider = provider;
		invalidateOutline();
	}
	private ViewOutlineProvider outline_provider = ViewOutlineProvider.BACKGROUND;

	public void setStateListAnimator(StateListAnimator stateListAnimator) {
		if (state_list_animator == stateListAnimator)
			return;
		if (state_list_animator != null)
			state_list_animator.setTarget(null);
		state_list_animator = stateListAnimator;
		if (stateListAnimator != null) {
			stateListAnimator.setTarget(this);
			stateListAnimator.setState(getDrawableState());
		}
	}
	private StateListAnimator state_list_animator;

	private static final AtomicInteger nextGeneratedId = new AtomicInteger(1);
	/**
     * Generate a value suitable for use in {@link #setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
	public static int generateViewId() {
		for (;;) {
			final int result = nextGeneratedId.get();
			// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;
			if (newValue > 0x00FFFFFF)
				newValue = 1; // Roll over to 1, not 0.
			if (nextGeneratedId.compareAndSet(result, newValue)) {
				return result;
			}
		}
	}

	public boolean isLayoutDirectionResolved() { return layoutDirectionResolved; }

	/* like AOSP: an inherited direction needs a parent that can resolve it; a view without a parent
	 * can when it is the root of a window */
	public boolean canResolveLayoutDirection() {
		if (rawLayoutDirection != LAYOUT_DIRECTION_INHERIT)
			return true;
		if (parent instanceof View)
			return ((View)parent).canResolveLayoutDirection();
		return attachedToWindow;
	}

	public boolean isPaddingRelative() {
		return userPaddingStart != UNDEFINED_PADDING || userPaddingEnd != UNDEFINED_PADDING;
	}

	/* AOSP's ForegroundInfo, without the tint */
	private static class ForegroundInfo {
		private Drawable mDrawable;
		private int mGravity = Gravity.FILL;
		private boolean mInsidePadding = true;
		private final Rect mSelfBounds = new Rect();
		private final Rect mOverlayBounds = new Rect();
	}

	private ForegroundInfo mForegroundInfo;

	/* AOSP; ATL: the wrapper widget is told to draw through Java instead of clearing PFLAG_SKIP_DRAW */
	public void setForeground(Drawable foreground) {
		if (mForegroundInfo == null) {
			if (foreground == null) {
				// Nothing to do.
				return;
			}
			mForegroundInfo = new ForegroundInfo();
		}

		if (foreground == mForegroundInfo.mDrawable) {
			// Nothing to do
			return;
		}

		if (mForegroundInfo.mDrawable != null) {
			if (isAttachedToWindow()) {
				mForegroundInfo.mDrawable.setVisible(false, false);
			}
			mForegroundInfo.mDrawable.setCallback(null);
			unscheduleDrawable(mForegroundInfo.mDrawable);
		}

		mForegroundInfo.mDrawable = foreground;
		if (foreground != null) {
			native_drawThroughJava(widget);
			foreground.setLayoutDirection(getLayoutDirection());
			if (foreground.isStateful()) {
				foreground.setState(getDrawableState());
			}
			if (isAttachedToWindow()) {
				foreground.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
			}
			// Set callback last, since the view may still be initializing.
			foreground.setCallback(this);
		}
		requestLayout();
		invalidate();
	}

	public boolean isForegroundInsidePadding() {
		return mForegroundInfo != null ? mForegroundInfo.mInsidePadding : true;
	}

	public int getForegroundGravity() {
		return mForegroundInfo != null ? mForegroundInfo.mGravity
				: Gravity.START | Gravity.TOP;
	}

	public void setForegroundGravity(int gravity) {
		if (mForegroundInfo == null) {
			mForegroundInfo = new ForegroundInfo();
		}

		if (mForegroundInfo.mGravity != gravity) {
			if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
				gravity |= Gravity.START;
			}

			if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
				gravity |= Gravity.TOP;
			}

			mForegroundInfo.mGravity = gravity;
			requestLayout();
		}
	}

	/* AOSP, without scroll indicators and bars; ATL: the bounds are computed on each draw, there is
	 * no setFrame() to tell about a size change */
	public void onDrawForeground(Canvas canvas) {
		final Drawable foreground = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
		if (foreground != null) {
			final Rect selfBounds = mForegroundInfo.mSelfBounds;
			final Rect overlayBounds = mForegroundInfo.mOverlayBounds;

			if (mForegroundInfo.mInsidePadding) {
				selfBounds.set(0, 0, getWidth(), getHeight());
			} else {
				selfBounds.set(getPaddingLeft(), getPaddingTop(),
						getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
			}

			final int ld = getLayoutDirection();
			Gravity.apply(mForegroundInfo.mGravity, foreground.getIntrinsicWidth(),
					foreground.getIntrinsicHeight(), selfBounds, overlayBounds, ld);
			foreground.setBounds(overlayBounds);

			foreground.draw(canvas);
		}
	}

	/* as AOSP: from the scroll offset, range and extent the view computes */
	public boolean canScrollVertically(int direction) {
		final int offset = computeVerticalScrollOffset();
		final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
		if (range == 0)
			return false;
		if (direction < 0)
			return offset > 0;
		return offset < range - 1;
	}

	public boolean isInTouchMode() { return false; }

	public void stopNestedScroll() {}

	public long getDrawingTime() {
		return System.currentTimeMillis();
	}

	private boolean keepScreenOn = false;
	private static native void native_keep_screen_on(long widget, boolean keepScreenOn);
	public void setKeepScreenOn(boolean screenOn) {
		if (isAttachedToWindow() && keepScreenOn != screenOn)
			native_keep_screen_on(widget, screenOn);
		keepScreenOn = screenOn;
	}

	public boolean getKeepScreenOn() {
		return keepScreenOn;
	}

	public void onRtlPropertiesChanged(int layoutDirection) {}

	private boolean attachedToWindow;

	/* like AOSP: onAttachedToWindow() and onDetachedFromWindow() alternate, each exactly once; called
	 * when GTK roots/unroots the widget, and for detaching also from ViewGroup and Window like AOSP */
	@Export
	void dispatchAttachedToWindow() {
		if (attachedToWindow)
			return;
		attachedToWindow = true;
		/* AOSP resolves the RTL properties when the view is measured after it was attached (the
		 * window's root view before); Compose initialises its layout direction there */
		resolveRtlPropertiesIfNeeded();
		onAttachedToWindow();

		ListenerInfo li = mListenerInfo;
		final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners = li != null ? li.mOnAttachStateChangeListeners : null;
		if (listeners != null && listeners.size() > 0) {
			// NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
			// perform the dispatching. The iterator is a safe guard against listeners that
			// could mutate the list by calling the various add/remove methods. This prevents
			// the array from being modified while we iterate it.
			for (OnAttachStateChangeListener listener : listeners) {
				listener.onViewAttachedToWindow(this);
			}
		}
	}

	@Export
	void dispatchDetachedFromWindow() {
		if (!attachedToWindow)
			return;
		onDetachedFromWindow();
		mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT; // AOSP's onDetachedFromWindowInternal()

		ListenerInfo li = mListenerInfo;
		final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners = li != null ? li.mOnAttachStateChangeListeners : null;
		if (listeners != null && listeners.size() > 0) {
			// see dispatchAttachedToWindow(): iterate, the list is a CopyOnWriteArrayList
			for (OnAttachStateChangeListener listener : listeners) {
				listener.onViewDetachedFromWindow(this);
			}
		}
		attachedToWindow = false;
	}

	protected void onAttachedToWindow() {
		mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT;

		if (keepScreenOn)
			native_keep_screen_on(widget, true);
		if (floating_observer != null) {
			getViewTreeObserver().merge(floating_observer);
			floating_observer = null;
		}
	}

	protected void onDetachedFromWindow() {
		if (keepScreenOn)
			native_keep_screen_on(widget, false);
	}

	public void setLayerType(int layerType, Paint paint) {}

	public float getZ() {
		return elevation + translation_z;
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {}

	/* AOSP, without the scroll bar */
	protected boolean verifyDrawable(Drawable who) {
		// Avoid verifying the scroll bar drawable so that we don't end up in
		// an invalidation loop. This effectively prevents the scroll bar
		// drawable from triggering invalidations and scheduling runnables.
		return who == background || (mForegroundInfo != null && mForegroundInfo.mDrawable == who)
				|| (mDefaultFocusHighlight == who);
	}

	/* AOSP View.drawableHotspotChanged */
	public void drawableHotspotChanged(float x, float y) {
		if (background != null) {
			background.setHotspot(x, y);
		}
		if (mDefaultFocusHighlight != null) {
			mDefaultFocusHighlight.setHotspot(x, y);
		}
		if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
			mForegroundInfo.mDrawable.setHotspot(x, y);
		}
	}

	/* the view's own opacity isn't tracked, so never claim to be opaque */
	public boolean isOpaque() {
		return false;
	}

	public boolean hasOverlappingRendering() {
		return true;
	}

	/* AOSP keeps the state in the AttachInfo of the window's ViewRootImpl, ATL in the Window */
	public KeyEvent.DispatcherState getKeyDispatcherState() {
		Window window = native_get_window(widget);
		return window != null ? window.mKeyDispatchState : null;
	}

	public Rect getClipBounds() {
		return clipBounds != null ? new Rect(clipBounds) : null;
	}

	public void setBackgroundTintList(ColorStateList tint) {}

	protected int computeHorizontalScrollRange() {
		return getWidth();
	}

	protected int computeHorizontalScrollExtent() {
		return getWidth();
	}

	protected int computeVerticalScrollRange() {
		return getHeight();
	}

	protected int computeVerticalScrollExtent() {
		return getHeight();
	}

	public void setAccessibilityLiveRegion(int mode) {}

	/* the outline is needed for the elevation shadow and for setClipToOutline() */
	private boolean needsOutline() {
		return getZ() > 0 || clipToOutline;
	}

	/* The outline gives the elevation shadow, which the parent draws below the view with the light
	 * source of the theme (see wrapper_widget_snapshot_shadow()), and the clip of setClipToOutline()
	 * (rect, round rect or, as AOSP since API 33, path) */
	public void invalidateOutline() {
		float z = getZ();
		Outline outline = null;
		if (needsOutline() && outline_provider != null && getWidth() > 0 && getHeight() > 0) {
			if (background != null)
				background.setBounds(0, 0, getWidth(), getHeight());
			outline = new Outline();
			outline.setAlpha(1.0f);
			outline_provider.getOutline(this, outline);
		}
		if (clipToOutline && outline != null && !outline.isEmpty()) {
			Rect rect = outline.mRect;
			long path = outline.mMode == Outline.MODE_PATH ? outline.mPath.getGskPath() : 0;
			native_setOutlineClip(widget, true, rect.left, rect.top, rect.right, rect.bottom, Math.max(outline.mRadius, 0), path);
			has_outline_clip = true;
		} else if (has_outline_clip) {
			native_setOutlineClip(widget, false, 0, 0, 0, 0, 0, 0);
			has_outline_clip = false;
		}
		if (z <= 0 || outline == null || outline.isEmpty() || outline.getAlpha() <= 0) {
			if (has_shadow)
				native_setShadow(widget, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			has_shadow = false;
			return;
		}
		Rect rect = new Rect();
		float radius = 0;
		if (outline.getRect(rect)) {
			radius = Math.max(outline.getRadius(), 0);
		} else if (outline.mPath != null) { // convex path: use its bounds
			RectF bounds = new RectF();
			outline.mPath.computeBounds(bounds, true);
			bounds.round(rect);
		}
		/* the light source of the window, as ThreadedRenderer reads it from the theme (Lighting) and
		 * keeps the alphas as 0..255 for hwui's LightingInfo */
		final TypedArray a = getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Lighting, 0, 0);
		final float lightY = a.getDimension(com.android.internal.R.styleable.Lighting_lightY, 0);
		final float lightZ = a.getDimension(com.android.internal.R.styleable.Lighting_lightZ, 0);
		final float lightRadius = a.getDimension(com.android.internal.R.styleable.Lighting_lightRadius, 0);
		final int ambientShadowAlpha = (int)(255 * a.getFloat(com.android.internal.R.styleable.Lighting_ambientShadowAlpha, 0) + 0.5f);
		final int spotShadowAlpha = (int)(255 * a.getFloat(com.android.internal.R.styleable.Lighting_spotShadowAlpha, 0) + 0.5f);
		a.recycle();
		native_setShadow(widget, rect.left, rect.top, rect.right, rect.bottom, radius, z, outline.getAlpha(),
				 ambientShadowAlpha / 255.f, spotShadowAlpha / 255.f, lightY, lightZ, lightRadius);
		has_shadow = true;
	}
	private native void native_setShadow(long widget, int left, int top, int right, int bottom, float radius, float z, float alpha,
					     float ambientAlpha, float spotAlpha, float lightY, float lightZ, float lightRadius);
	private native void native_setOutlineClip(long widget, boolean enabled, int left, int top, int right, int bottom, float radius, long path);

	public int getMeasuredWidthAndState() {
		return measuredWidth;
	}

	/* like AOSP: only mark this view, so that the next layout pass measures and lays it out again;
	 * no new layout pass is requested (ListView calls it while measuring its children) */
	public void forceLayout() {
		mPrivateFlags |= PFLAG_FORCE_LAYOUT;
	}

	/* AOSP View.removeOnAttachStateChangeListener() */
	public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
		ListenerInfo li = mListenerInfo;
		if (li == null || li.mOnAttachStateChangeListeners == null) {
			return;
		}
		li.mOnAttachStateChangeListeners.remove(listener);
	}

	@Export
	public boolean onInterceptTouchEvent(MotionEvent event) { return false; }

	@Export
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (on_touch_listener != null && on_touch_listener.onTouch(this, event))
			return true;
		return onTouchEvent(event);
	}

	public boolean canScrollHorizontally(int direction) {
		final int offset = computeHorizontalScrollOffset();
		final int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
		if (range == 0)
			return false;
		if (direction < 0)
			return offset > 0;
		return offset < range - 1;
	}

	protected native boolean native_getGlobalVisibleRect(long widget, Rect visibleRect);

	public boolean getGlobalVisibleRect(Rect visibleRect) {
		return native_getGlobalVisibleRect(widget, visibleRect);
	}

	public boolean onCheckIsTextEditor() { return false; }

	/* AOSP View.hasOnClickListeners() */
	public boolean hasOnClickListeners() {
		ListenerInfo li = mListenerInfo;
		return (li != null && li.mOnClickListener != null);
	}

	/* AOSP View.getRawTextAlignment() / setTextAlignment() (the GTK style classes follow the raw value) */
	public int getRawTextAlignment() {
		return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_MASK) >> PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;
	}

	public void setTextAlignment(int textAlignment) {
		if (textAlignment != getRawTextAlignment()) {
			// Reset the current and resolved text alignment
			mPrivateFlags2 &= ~PFLAG2_TEXT_ALIGNMENT_MASK;
			resetResolvedTextAlignment();
			// Set the new text alignment
			mPrivateFlags2 |= ((textAlignment << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT) & PFLAG2_TEXT_ALIGNMENT_MASK);
			// Do resolution
			resolveTextAlignment();
			// Notify change
			onRtlPropertiesChanged(getLayoutDirection());
			// Refresh
			requestLayout();
			invalidate();
		}
		String[] classesToRemove = {"ATL-text-align-left", "ATL-text-align-center", "ATL-text-align-right"};
		native_removeClasses(widget, classesToRemove);

		switch (textAlignment) {
			case TEXT_ALIGNMENT_CENTER:
				native_addClass(widget, "ATL-text-align-center");
				break;
			case TEXT_ALIGNMENT_TEXT_START:
			case TEXT_ALIGNMENT_VIEW_START:
				native_addClass(widget, "ATL-text-align-left");
				break;
			case TEXT_ALIGNMENT_TEXT_END:
			case TEXT_ALIGNMENT_VIEW_END:
				native_addClass(widget, "ATL-text-align-right");
				break;
		}
	}

	public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {}

	public StateListAnimator getStateListAnimator() { return state_list_animator; }

	public void requestFitSystemWindows() {}

	public boolean isPressed() {
		return (mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED;
	}

	public native void getWindowVisibleDisplayFrame(Rect rect);

	public void setRotation(float rotation) {
		if (this.rotation == rotation)
			return;
		this.rotation = rotation;
		transformChanged();
	}

	public float getRotation() { return rotation; }
	public void setRotationX(float deg) {}
	public void setRotationY(float deg) {}

	public float getRotationX() { return 0.f; }
	public float getRotationY() { return 0.f; }

	public void setScaleX(float scaleX) {
		if (this.scaleX == scaleX)
			return;
		this.scaleX = scaleX;
		transformChanged();
	}

	public void setScaleY(float scaleY) {
		if (this.scaleY == scaleY)
			return;
		this.scaleY = scaleY;
		transformChanged();
	}

	public float getScaleX() { return scaleX; }
	public float getScaleY() { return scaleY; }

	/* as AOSP: once one pivot is set, both are explicit; until then the pivot is the center */
	public void setPivotX(float pivot_x) {
		if (pivotExplicit && pivotX == pivot_x)
			return;
		if (!pivotExplicit)
			pivotY = getHeight() / 2f;
		pivotExplicit = true;
		pivotX = pivot_x;
		transformChanged();
	}

	public void setPivotY(float pivot_y) {
		if (pivotExplicit && pivotY == pivot_y)
			return;
		if (!pivotExplicit)
			pivotX = getWidth() / 2f;
		pivotExplicit = true;
		pivotY = pivot_y;
		transformChanged();
	}

	public float getPivotX() { return pivotExplicit ? pivotX : getWidth() / 2f; }
	public float getPivotY() { return pivotExplicit ? pivotY : getHeight() / 2f; }

	public boolean isPivotSet() { return pivotExplicit; }

	public void resetPivot() {
		pivotExplicit = false;
		transformChanged();
	}

	public float getTranslationZ() { return translation_z; }

	public void setTranslationZ(float translationZ) {
		if (translationZ != translation_z) {
			translation_z = translationZ;
			invalidateOutline();
		}
	}

	public int getWindowSystemUiVisibility() {
		return 0;
	}

	public void setScrollIndicators(int indicators, int mask) {}


	public ColorStateList getBackgroundTintList() { return null; }

	public PorterDuff.Mode getBackgroundTintMode() { return null; }

	public String getTransitionName() { return null; }

	public WindowId getWindowId() { return null; }

	public boolean isInLayout() {
		return false; // FIXME
	}

	public void setTextDirection(int textDirection) {}

	public Drawable getForeground() {
		return mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
	}

	public void setScrollbarFadingEnabled(boolean fadeEnabled) {}

	public void setScrollBarStyle(int style) {}

	public void setVerticalScrollbarPosition(int position) {}

	public void setNestedScrollingEnabled(boolean enabled) {}

	public void setTouchDelegate(TouchDelegate touchDelegate) {}

	public void setOnDragListener(OnDragListener onDragListener) {}

	public void setTransitionName(String transitionName) {}

	/**
	 * Get the animation currently associated with this view.
	 */
	public Animation getAnimation() {
		return currentAnimation;
	}

	public ViewOverlay getOverlay() {
		return new ViewOverlay();
	}

	public void cancelLongPress() {
		removeCallbacks(pendingLongClick);
		pendingLongClick = null;
	}

	/* AOSP View.getTextAlignment(): the resolved text alignment */
	public int getTextAlignment() {
		return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK) >> PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;
	}

	/* AOSP View.resolveTextAlignment(); ATL's parents are Views or the window's root (AOSP's ViewRootImpl:
	 * can resolve, is resolved, TEXT_ALIGNMENT_GRAVITY) */
	public boolean resolveTextAlignment() {
		// Reset any previous text alignment resolution
		mPrivateFlags2 &= ~(PFLAG2_TEXT_ALIGNMENT_RESOLVED | PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK);

		if (hasRtlSupport()) {
			// Set resolved text alignment flag depending on text alignment flag
			final int textAlignment = getRawTextAlignment();
			switch (textAlignment) {
				case TEXT_ALIGNMENT_INHERIT:
					// Check if we can resolve the text alignment
					if (!canResolveTextAlignment()) {
						// We cannot do the resolution if there is no parent so use the default
						mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
						// Resolution will need to happen again later
						return false;
					}

					// Parent has not yet resolved, so we still return the default
					if (parent instanceof View && !((View)parent).isTextAlignmentResolved()) {
						mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
						// Resolution will need to happen again later
						return false;
					}

					int parentResolvedTextAlignment = parent instanceof View ? ((View)parent).getTextAlignment() : TEXT_ALIGNMENT_GRAVITY;

					switch (parentResolvedTextAlignment) {
						case TEXT_ALIGNMENT_GRAVITY:
						case TEXT_ALIGNMENT_TEXT_START:
						case TEXT_ALIGNMENT_TEXT_END:
						case TEXT_ALIGNMENT_CENTER:
						case TEXT_ALIGNMENT_VIEW_START:
						case TEXT_ALIGNMENT_VIEW_END:
							// Resolved text alignment is the same as the parent resolved
							// text alignment
							mPrivateFlags2 |= (parentResolvedTextAlignment << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT);
							break;
						default:
							// Use default resolved text alignment
							mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
					}
					break;
				case TEXT_ALIGNMENT_GRAVITY:
				case TEXT_ALIGNMENT_TEXT_START:
				case TEXT_ALIGNMENT_TEXT_END:
				case TEXT_ALIGNMENT_CENTER:
				case TEXT_ALIGNMENT_VIEW_START:
				case TEXT_ALIGNMENT_VIEW_END:
					// Resolved text alignment is the same as text alignment
					mPrivateFlags2 |= (textAlignment << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT);
					break;
				default:
					// Use default resolved text alignment
					mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
			}
		} else {
			// Use default resolved text alignment
			mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
		}

		// Set the resolved
		mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED;
		return true;
	}

	/* AOSP View.canResolveTextAlignment() */
	public boolean canResolveTextAlignment() {
		switch (getRawTextAlignment()) {
			case TEXT_DIRECTION_INHERIT:
				if (parent instanceof View) {
					return ((View)parent).canResolveTextAlignment();
				}
				return parent != null;
			default:
				return true;
		}
	}

	/* AOSP View.resetResolvedTextAlignment() */
	public void resetResolvedTextAlignment() {
		// Reset any previous text alignment resolution
		mPrivateFlags2 &= ~(PFLAG2_TEXT_ALIGNMENT_RESOLVED | PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK);
		// Set to default
		mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
	}

	/* AOSP View.isTextAlignmentInherited() / isTextAlignmentResolved() */
	public boolean isTextAlignmentInherited() {
		return (getRawTextAlignment() == TEXT_ALIGNMENT_INHERIT);
	}

	public boolean isTextAlignmentResolved() {
		return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED) == PFLAG2_TEXT_ALIGNMENT_RESOLVED;
	}

	public View findViewWithTag(Object tag) {
		if (Objects.equals(tag, this.tag))
			return this;
		else
			return null;
	}

	/* like AOSP, shown when the mouse rests on the view; GTK's tooltip does the showing and hiding */
	private CharSequence tooltipText;

	public void setTooltipText(CharSequence tooltip) {
		tooltipText = (tooltip == null || tooltip.length() == 0) ? null : tooltip;
		native_setTooltipText(widget, tooltipText == null ? null : tooltipText.toString());
	}

	public CharSequence getTooltipText() {
		return tooltipText;
	}

	private native void native_setTooltipText(long widget, String tooltip);

	public int getImportantForAutofill() { return 0; }

	public void setImportantForAutofill(int flag) {}


	public void setHorizontalFadingEdgeEnabled(boolean horizontalFadingEdgeEnabled) {}

	public Handler getHandler() {
		if (handler == null)
			handler = new Handler(Looper.getMainLooper());
		return handler;
	}

	public boolean isHardwareAccelerated() {
		return false;
	}

	private Rect clipBounds;
	public void setClipBounds(Rect clipBounds) {
		if (clipBounds == null ? this.clipBounds == null : clipBounds.equals(this.clipBounds))
			return;
		this.clipBounds = clipBounds != null ? new Rect(clipBounds) : null;
		if (clipBounds != null)
			native_setClipBounds(widget, true, clipBounds.left, clipBounds.top, clipBounds.right, clipBounds.bottom);
		else
			native_setClipBounds(widget, false, 0, 0, 0, 0);
	}
	private native void native_setClipBounds(long widget, boolean enabled, int left, int top, int right, int bottom);

	public boolean getClipToOutline() { return clipToOutline; }

	public void setLeft(int left) {
		layout(left, top, right, bottom);
	}

	public void setTop(int top) {
		layout(left, top, right, bottom);
	}

	public void setRight(int right) {
		layout(left, top, right, bottom);
	}

	public void setBottom(int bottom) {
		layout(left, top, right, bottom);
	}

	public void setCameraDistance(float distance) {}

	public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) { return false; }

	public boolean requestRectangleOnScreen(Rect rectangle) { return false; }

	@Export
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	/* like AOSP; ATL calls it only where a view drives a drop-down (AutoCompleteTextView) */
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		return false;
	}

	@Export
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			return onKeyDown(event.getKeyCode(), event);
		else
			return false;
	}

	public WindowInsets getRootWindowInsets() {
		Window window = native_get_window(widget);
		return window != null ? window.getWindowInsets() : null;
	}

	public PointerIcon getPointerIcon() { return null; }

	public void setPointerIcon(PointerIcon pointerIcon) {}

	public IBinder getApplicationWindowToken() { return null; }

	public int getVerticalFadingEdgeLength() { return 0; }
	public int getVerticalScrollbarWidth() { return 0; }

	public void saveAttributeDataForStyleable(Context ctxt, int[] styleable, AttributeSet attrs, TypedArray t, int defStyleAttr, int defStyleRes) {}

	public final View requireViewById(int id) {
		View view = findViewById(id);
		if (view == null)
			throw new IllegalArgumentException("ID does not reference a View inside this View");
		return view;
	}
	public float getTransitionAlpha() {
		return transitionAlpha;
	}

	/* API 29; transitions fade with this, on top of the view's own alpha */
	public void setTransitionAlpha(float alpha) {
		transitionAlpha = alpha;
		native_setVisibility(widget, getDrawnVisibility(), this.alpha * transitionAlpha, atl_enabled);
	}

	/* API 29; sets the visibility without the side effects of setVisibility() */
	public void setTransitionVisibility(int visibility) {
		this.visibility = visibility;
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
	}

	/* API 29 */
	public void setLeftTopRightBottom(int left, int top, int right, int bottom) {
		layout(left, top, right, bottom);
	}

	/* like AOSP: the window focus change reaches every view of the window */
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		onWindowFocusChanged(hasFocus);
	}

	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) {
			if (isPressed()) {
				setPressed(false);
			}
			onFocusLost();
		}

		refreshDrawableState();
	}

	/* AOSP's AttachInfo.mHasWindowFocus, kept by the root view of a window: -1 for a window ATL
	 * doesn't tell about its focus (a dialog), which counts as focused */
	private int rootHasWindowFocus = -1;

	/** @hide ATL: what ViewRootImpl.handleWindowFocusChanged() does with the root view of a window */
	public void dispatchRootWindowFocusChanged(boolean hasWindowFocus) {
		rootHasWindowFocus = hasWindowFocus ? 1 : 0;
		dispatchWindowFocusChanged(hasWindowFocus);
	}

	public boolean hasWindowFocus() {
		return isAttachedToWindow() && getRootView().rootHasWindowFocus != 0;
	}

	/** @hide */
	protected void onFocusLost() {
		resetPressedState();
	}

	/**
	 * Sets the next animation to play for this view. The animation starts at the time set with
	 * {@link Animation#setStartTime(long)}, {@link #startAnimation(Animation)} starts it right away.
	 */
	public void setAnimation(Animation animation) {
		currentAnimation = animation;
		if (animation != null) {
			animation.reset();
			animationMore = true;
		}
		native_setAnimating(widget, animation != null);
		if (visibility == INVISIBLE)
			native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, atl_enabled);
		if (visibility == GONE && animation != null)
			getHandler().post(new GoneAnimationTicker(animation));
	}

	/* AOSP draws a gone view (at its last bounds) as long as it has an animation, so the animation
	 * runs and its listener is called. A gone GTK widget is not drawn, so advance it from here. */
	private class GoneAnimationTicker implements Runnable {
		private final Animation animation;

		GoneAnimationTicker(Animation animation) {
			this.animation = animation;
		}

		@Override
		public void run() {
			if (currentAnimation != animation || visibility != GONE)
				return;
			applyLegacyAnimation();
			if (animationMore)
				getHandler().postDelayed(this, 16);
		}
	}

	public boolean performAccessibilityAction(int action, Bundle arguments) { return false; }

	public boolean isDirty() { return false; }

	public float getX() {
		return getLeft() + getTranslationX();
	}
	public float getY() {
		return getTop() + getTranslationY();
	}

	public boolean getGlobalVisibleRect(Rect visibleRect, Point globalOffset) {
		boolean result = native_getGlobalVisibleRect(widget, visibleRect);
		globalOffset.set(visibleRect.left, visibleRect.top);
		return result;
	}

	public boolean isHovered() { return false; }

	public void scrollBy(int x, int y) {
		scrollTo(scrollX + x, scrollY + y);
	}

	public void setAccessibilityPaneTitle(CharSequence paneTitle) { accessibilityPaneTitle = paneTitle; }

	public void setAccessibilityHeading(boolean heading) { accessibilityHeading = heading; }

	public WindowInsets computeSystemWindowInsets(WindowInsets insets, Rect contentInsets) { return insets; }

	public boolean isDuplicateParentStateEnabled() { return duplicate_parent_state; }

	public void setBackgroundTintMode(PorterDuff.Mode tintMode) {}

	int mNextFocusLeftId = NO_ID;
	int mNextFocusRightId = NO_ID;
	int mNextFocusUpId = NO_ID;
	int mNextFocusDownId = NO_ID;
	int mNextFocusForwardId = NO_ID;
	int mNextClusterForwardId = NO_ID;

	public int getNextFocusLeftId() { return mNextFocusLeftId; }
	public void setNextFocusLeftId(int id) { mNextFocusLeftId = id; }
	public int getNextFocusRightId() { return mNextFocusRightId; }
	public void setNextFocusRightId(int id) { mNextFocusRightId = id; }
	public int getNextFocusUpId() { return mNextFocusUpId; }
	public void setNextFocusUpId(int id) { mNextFocusUpId = id; }
	public int getNextFocusDownId() { return mNextFocusDownId; }
	public void setNextFocusDownId(int id) { mNextFocusDownId = id; }
	public int getNextFocusForwardId() { return mNextFocusForwardId; }
	public void setNextFocusForwardId(int id) { mNextFocusForwardId = id; }
	public int getNextClusterForwardId() { return mNextClusterForwardId; }
	public void setNextClusterForwardId(int id) { mNextClusterForwardId = id; }

	/* AOSP View.findUserSetNextFocus() and findUserSetNextKeyboardNavigationCluster(), with
	 * findViewById() for findViewInsideOutShouldExist() */
	View findUserSetNextFocus(View root, int direction) {
		switch (direction) {
			case FOCUS_LEFT:
				return mNextFocusLeftId == NO_ID ? null : root.findViewById(mNextFocusLeftId);
			case FOCUS_RIGHT:
				return mNextFocusRightId == NO_ID ? null : root.findViewById(mNextFocusRightId);
			case FOCUS_UP:
				return mNextFocusUpId == NO_ID ? null : root.findViewById(mNextFocusUpId);
			case FOCUS_DOWN:
				return mNextFocusDownId == NO_ID ? null : root.findViewById(mNextFocusDownId);
			case FOCUS_FORWARD:
				return mNextFocusForwardId == NO_ID ? null : root.findViewById(mNextFocusForwardId);
			case FOCUS_BACKWARD:
				return id == NO_ID ? null : findViewWithNextFocusForwardId(root, id, false);
		}
		return null;
	}

	View findUserSetNextKeyboardNavigationCluster(View root, int direction) {
		switch (direction) {
			case FOCUS_FORWARD:
				return mNextClusterForwardId == NO_ID ? null : root.findViewById(mNextClusterForwardId);
			case FOCUS_BACKWARD:
				return id == NO_ID ? null : findViewWithNextFocusForwardId(root, id, true);
		}
		return null;
	}

	private static View findViewWithNextFocusForwardId(View v, int forwardTo, boolean cluster) {
		if ((cluster ? v.mNextClusterForwardId : v.mNextFocusForwardId) == forwardTo)
			return v;
		if (v instanceof ViewGroup) {
			ViewGroup group = (ViewGroup)v;
			for (int i = 0; i < group.getChildCount(); i++) {
				View found = findViewWithNextFocusForwardId(group.getChildAt(i), forwardTo, cluster);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	/* AOSP View.addFocusables(), canTakeFocus(): the views that a focus search can end in */
	public void addFocusables(ArrayList<View> views, int direction) {
		addFocusables(views, direction, isInTouchMode() ? FOCUSABLES_TOUCH_MODE : FOCUSABLES_ALL);
	}

	public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
		if (views == null)
			return;
		if (!canTakeFocus())
			return;
		if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE && !isFocusableInTouchMode())
			return;
		views.add(this);
	}

	private boolean canTakeFocus() {
		return visibility == VISIBLE && isFocusableResolved() && atl_enabled && getWidth() > 0 && getHeight() > 0;
	}

	/* AOSP View.addKeyboardNavigationClusters(), getTouchables(), addTouchables() */
	public void addKeyboardNavigationClusters(java.util.Collection<View> views, int direction) {
		if (!isKeyboardNavigationCluster())
			return;
		if (!hasFocusable())
			return;
		views.add(this);
	}

	public ArrayList<View> getTouchables() {
		ArrayList<View> result = new ArrayList<View>();
		addTouchables(result);
		return result;
	}

	public void addTouchables(ArrayList<View> views) {
		if ((isClickable() || isLongClickable()) && atl_enabled)
			views.add(this);
	}

	/* AOSP View.focusSearch() */
	public View focusSearch(int direction) {
		if (parent instanceof ViewGroup)
			return ((ViewGroup)parent).focusSearch(this, direction);
		return null;
	}

	public void setHasTransientState(boolean hasTransientState) {}

	protected void onConfigurationChanged(Configuration newConfig) {}

	public boolean isDrawingCacheEnabled() { return false; }

	public void setDrawingCacheEnabled(boolean enabled) {}

	public void setDrawingCacheQuality(int quality) {}

	public void buildDrawingCache(boolean autoScale) {}

	public Bitmap getDrawingCache() { return null; }

	public void announceForAccessibility(CharSequence text) {}

	public WindowInsetsController getWindowInsetsController() {
		return native_get_window(widget).getInsetsController();
	}

	public final boolean isKeyboardNavigationCluster() { return false; }

	public void setKeyboardNavigationCluster(boolean isCluster) {}

	public AutofillId getAutofillId() { return new AutofillId(); }

	public AccessibilityDelegate getAccessibilityDelegate() { return null; }

	public void setScrollX(int value) {
		this.scrollX = value;
	}
	public void setScrollY(int value) {
		this.scrollY = value;
	}

	@Export
	protected boolean dispatchHoverEvent(MotionEvent event) { return false; }

	public ActionMode startActionMode(ActionMode.Callback callback, int type) { return null; }

	public InputConnection onCreateInputConnection(EditorInfo outAttrs) { return null; }

	public ActionMode startActionMode(ActionMode.Callback callback) { return null; }

	/* AOSP: the whole view; a text view gives the cursor */
	public void getFocusedRect(Rect r) { getDrawingRect(r); }

	/**
	* An implementation of OnClickListener that attempts to lazily load a
	* named click handling method from a parent or ancestor context.
	* See AOSP commit d13f0e21bf8059779c9330a5993907ec88c8e781
	* Copyright (C) 2006 The Android Open Source Project
	*/
	private static class DeclaredOnClickListener implements OnClickListener {
		private final View mHostView;
		private final String mMethodName;
		private Method mResolvedMethod;
		private Context mResolvedContext;
		public DeclaredOnClickListener(@NonNull View hostView, @NonNull String methodName) {
			mHostView = hostView;
			mMethodName = methodName;
		}
		@Override
		public void onClick(@NonNull View v) {
			if (mResolvedMethod == null)
				resolveMethod(mHostView.getContext(), mMethodName);
			try {
				mResolvedMethod.invoke(mResolvedContext, v);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(
				    "Could not execute non-public method for android:onClick", e);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(
				    "Could not execute method for android:onClick", e);
			}
		}

		@NonNull
		private void resolveMethod(@Nullable Context context, @NonNull String name) {
			while (context != null) {
				try {
					if (!context.isRestricted()) {
						final Method method = context.getClass().getMethod(mMethodName, View.class);
						if (method != null) {
							mResolvedMethod = method;
							mResolvedContext = context;
							return;
						}
					}
				} catch (NoSuchMethodException e) {
					// Failed to find method, keep searching up the hierarchy.
				}
				if (context instanceof ContextWrapper) {
					context = ((ContextWrapper)context).getBaseContext();
				} else {
					// Can't search up the hierarchy, null out and fail.
					context = null;
				}
			}

			final int id = mHostView.getId();
			final String idText = id == NO_ID ? "" : " with id '" + mHostView.getResources().getResourceEntryName(id) + "'";
			throw new IllegalStateException("Could not find method " + mMethodName
			                                + "(View) in a parent or ancestor Context for android:onClick "
			                                + "attribute defined on view " + mHostView.getClass() + idText);
		}
	}
	public void setForceDarkAllowed(boolean forceDark) {}
	public void setWindowInsetsAnimationCallback(WindowInsetsAnimation.Callback callback) {}
	public void setViewTranslationCallback(ViewTranslationCallback callback) {}
	public void clearViewTranslationCallback() {}
	/* as AOSP: from the view's coordinates to those of its window (Compose positions its popups by
	 * this on API 29+) */
	public void transformMatrixToGlobal(Matrix matrix) {
		if (parent instanceof View) {
			View vp = (View)parent;
			vp.transformMatrixToGlobal(matrix);
			matrix.preTranslate(-vp.scrollX, -vp.scrollY);
		}
		matrix.preTranslate(left, top);
		if (!hasIdentityMatrix())
			matrix.preConcat(getMatrix());
	}

	public void transformMatrixToLocal(Matrix matrix) {
		if (parent instanceof View) {
			View vp = (View)parent;
			vp.transformMatrixToLocal(matrix);
			matrix.postTranslate(vp.scrollX, vp.scrollY);
		}
		matrix.postTranslate(-left, -top);
		if (!hasIdentityMatrix())
			matrix.postConcat(getInverseMatrix());
	}
	public void setAnimationMatrix(Matrix matrix) {}
	public Matrix getAnimationMatrix() { return null; }

	private static long nextUniqueDrawingId = 1;
	private long uniqueDrawingId;

	public long getUniqueDrawingId() {
		if (uniqueDrawingId == 0)
			uniqueDrawingId = nextUniqueDrawingId++;
		return uniqueDrawingId;
	}

	/* no content capture service: like AOSP without one, the session is null */
	private int importantForContentCapture = 0; // IMPORTANT_FOR_CONTENT_CAPTURE_AUTO
	public void setImportantForContentCapture(int mode) { importantForContentCapture = mode; }
	public int getImportantForContentCapture() { return importantForContentCapture; }
	public boolean isImportantForContentCapture() { return false; }
	public android.view.contentcapture.ContentCaptureSession getContentCaptureSession() { return null; }

	private int outlineAmbientShadowColor = 0xff000000;
	private int outlineSpotShadowColor = 0xff000000;
	public void setOutlineAmbientShadowColor(int color) { outlineAmbientShadowColor = color; }
	public int getOutlineAmbientShadowColor() { return outlineAmbientShadowColor; }
	public void setOutlineSpotShadowColor(int color) { outlineSpotShadowColor = color; }
	public int getOutlineSpotShadowColor() { return outlineSpotShadowColor; }

	private java.util.List<Rect> systemGestureExclusionRects = java.util.Collections.emptyList();
	public void setSystemGestureExclusionRects(java.util.List<Rect> rects) { systemGestureExclusionRects = new java.util.ArrayList<>(rects); }
	public java.util.List<Rect> getSystemGestureExclusionRects() { return systemGestureExclusionRects; }

	private boolean screenReaderFocusable;
	private boolean accessibilityHeading;
	private CharSequence accessibilityPaneTitle;
	private CharSequence stateDescription;
	public void setScreenReaderFocusable(boolean focusable) { screenReaderFocusable = focusable; }
	public boolean isScreenReaderFocusable() { return screenReaderFocusable; }
	public boolean isAccessibilityHeading() { return accessibilityHeading; }
	public CharSequence getAccessibilityPaneTitle() { return accessibilityPaneTitle; }
	public void setStateDescription(CharSequence description) { stateDescription = description; }
	public CharSequence getStateDescription() { return stateDescription; }
	public String[] getReceiveContentMimeTypes() { return null; }
	public void setRenderEffect(android.graphics.RenderEffect renderEffect) {}
	public boolean isShowingLayoutBounds() {
		return true;
	}

	/* AOSP View.callOnClick() */
	public boolean callOnClick() {
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnClickListener != null) {
			li.mOnClickListener.onClick(this);
			return true;
		}
		return false;
	}

	public void destroyDrawingCache() {}

	public void setHovered(boolean isHovered) {}

	public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {}

	public void setFadingEdgeLength(int length) {}

	public int getAccessibilityLiveRegion() { return 0; }

	public void setLabelFor(int id) {}

	public ArrayList<View> getFocusables(int direction) { return new ArrayList<View>(); }

	public WindowInsets onApplyWindowInsets(WindowInsets insets) {
		return insets;
	}

	public void setAccessibilityTraversalBefore(int id) {}

	public int getLayerType() { return 0; /* LAYER_TYPE_NONE */ }

	public boolean isAccessibilityFocused() { return false; }

	public float getCameraDistance() { return 0; }

	public OnFocusChangeListener getOnFocusChangeListener() { return onFocusChangeListener; }

	public int getTextDirection() { return 0; /* TEXT_DIRECTION_INHERIT */ }

	public TouchDelegate getTouchDelegate() { return null; }

	public void setZ(float z) {}
}
