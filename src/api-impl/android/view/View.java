package android.view;

import android.credentials.GetCredentialException;
import android.credentials.GetCredentialRequest;
import android.credentials.GetCredentialResponse;
import android.credentials.CredentialOption;
import android.os.OutcomeReceiver;
import android.service.credentials.CredentialProviderService;
import android.R;
import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.atl.GskCanvas;
import android.atl.InputTrace;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.BlendMode;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
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
import android.widget.Checkable;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import android.view.contentcapture.ContentCaptureSession;
import android.content.ClipData;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
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

public class View implements Drawable.Callback, KeyEvent.Callback {

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
	public static final int FOCUSABLE = 0x00000001;

	/**
	 * Mask for use with setFlags indicating bits used for focus.
	 */
	private static final int FOCUSABLE_MASK = 0x00000011;

	/**
	 * This view determines focusability automatically. This is the default.
	 */
	public static final int FOCUSABLE_AUTO = 0x00000010;

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

	static final int DRAG_MASK = PFLAG2_DRAG_CAN_ACCEPT | PFLAG2_DRAG_HOVERED;

	/**
	 * Horizontal layout direction of this view is from Left to Right.
	 * Use with {@link #setLayoutDirection}.
	 */
	/* AOSP View.LAYOUT_DIRECTION_UNDEFINED (@hide) */
	public static final int LAYOUT_DIRECTION_UNDEFINED = LayoutDirection.UNDEFINED;
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

	/* AOSP's View.OnGenericMotionListener */
	public interface OnGenericMotionListener {
		boolean onGenericMotion(View v, MotionEvent event);
	}

	public interface OnSystemUiVisibilityChangeListener {
		/* AOSP View.OnSystemUiVisibilityChangeListener */
		public void onSystemUiVisibilityChange(int visibility);
	}

	public static interface OnKeyListener {
		boolean onKey(View v, int keyCode, KeyEvent event);
	}

	public interface OnContextClickListener {
		boolean onContextClick(View v);
	}

	public interface OnLongClickListener {
		public boolean onLongClick(View v);

		/* AOSP OnLongClickListener.onLongClickUseDefaultHapticFeedback(); ATL has no haptic feedback to skip */
		default boolean onLongClickUseDefaultHapticFeedback(View v) {
			return true;
		}
	}

	public interface OnHoverListener {
		boolean onHover(View v, MotionEvent event);
	}

	public interface OnApplyWindowInsetsListener {
		public WindowInsets onApplyWindowInsets(View v, WindowInsets insets);
	}

	public interface OnLayoutChangeListener {
		public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom);
	}

	/* AOSP View.OnUnhandledKeyEventListener: asked about a key the view tree did not handle */
	public interface OnUnhandledKeyEventListener {
		boolean onUnhandledKeyEvent(View v, KeyEvent event);
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

	/**
	 * Interface definition for a callback to be invoked when a drag is being dispatched
	 * to this view.
	 */
	public interface OnDragListener {
		/**
		 * Called when a drag event is dispatched to a view. This allows listeners
		 * to get a chance to override base View behavior.
		 */
		boolean onDrag(View v, DragEvent event);
	}
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
		/** @hide AOSP View.MeasureSpec.makeSafeMeasureSpec() (Android 16: the same as makeMeasureSpec()) */
		public static int makeSafeMeasureSpec(int size, int mode) {
			return makeMeasureSpec(size, mode);
		}

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
	private float rotationX = 0;
	private float rotationY = 0;
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
	/* as in AOSP, PFLAG_FORCE_LAYOUT is cleared when the view is laid out, not when it is measured */
	static final int PFLAG_FORCE_LAYOUT                = 0x00001000;
	static final int PFLAG_LAYOUT_REQUIRED             = 0x00002000;
	static final int PFLAG_SELECTED                    = 0x00000004;
	static final int PFLAG_DRAWABLE_STATE_DIRTY        = 0x00000400;
	private static final int PFLAG_PRESSED             = 0x00004000;
	static final int PFLAG_ACTIVATED                   = 0x40000000;
	static final int PFLAG_HOVERED                     = 0x10000000;

	private int[] mDrawableState = null;

	/* the bits of AOSP's mPrivateFlags3 that ATL keeps */
	int mPrivateFlags3;
	static final int PFLAG3_IS_LAID_OUT = 0x4;
	static final int PFLAG3_APPLYING_INSETS = 0x20;
	static final int PFLAG3_FITTING_SYSTEM_WINDOWS = 0x40;
	static final int PFLAG3_CLUSTER = 0x8000;
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

	/* AOSP's mViewFlags for the flags ATL has: ENABLED_MASK, FOCUSABLE_MASK, FOCUSABLE_IN_TOUCH_MODE, CLICKABLE and
	 * LONG_CLICKABLE (the visibility stays in the visibility field). Unlike AOSP the view starts out FOCUSABLE as well as
	 * FOCUSABLE_AUTO: GTK decides what a native widget can focus, so isFocusable() stays true until the app says otherwise. */
	int mViewFlags = ENABLED | FOCUSABLE | FOCUSABLE_AUTO | HAPTIC_FEEDBACK_ENABLED; /* AOSP View(Context): haptic feedback on */

	private Handler handler;
	private Animation currentAnimation;
	private Transformation animationTransformation;
	private boolean animationMore; // read by the wrapper widget: draw the next frame

	/* AOSP's View.ListenerInfo, with the listeners ATL keeps in Java: the click listener and the
	 * lists of layout change and attach state change listeners */
	static class ListenerInfo {
		OnClickListener mOnClickListener;
		OnKeyListener mOnKeyListener;
		OnGenericMotionListener mOnGenericMotionListener;
		OnHoverListener mOnHoverListener;
		OnDragListener mOnDragListener;
		OnCreateContextMenuListener mOnCreateContextMenuListener;
		OnSystemUiVisibilityChangeListener mOnSystemUiVisibilityChangeListener;
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

	/* AOSP View.X and View.Y: the properties of the view's position (getX()/setX(), getY()/setY()) */
	public static final Property<View, Float> X = new Property<View, Float>(Float.class, "x") {
		@Override
		public void set(View object, Float value) {
			object.setX(value);
		}

		@Override
		public Float get(View object) {
			return object.getX();
		}
	};

	public static final Property<View, Float> Y = new Property<View, Float>(Float.class, "y") {
		@Override
		public void set(View object, Float value) {
			object.setY(value);
		}

		@Override
		public Float get(View object) {
			return object.getY();
		}
	};


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

	/* AOSP View.ROTATION_X / ROTATION_Y */
	public static final Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
		@Override
		public void setValue(View object, float value) {
			object.setRotationX(value);
		}
		@Override
		public Float get(View object) {
			return object.getRotationX();
		}
	};

	public static final Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
		@Override
		public void setValue(View object, float value) {
			object.setRotationY(value);
		}
		@Override
		public Float get(View object) {
			return object.getRotationY();
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
		if (a.getBoolean(com.android.internal.R.styleable.View_fitsSystemWindows, false))
			setFitsSystemWindows(true);
		if (a.peekValue(com.android.internal.R.styleable.View_keyboardNavigationCluster) != null)
			setKeyboardNavigationCluster(a.getBoolean(com.android.internal.R.styleable.View_keyboardNavigationCluster, true));
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
			if (a.getBoolean(com.android.internal.R.styleable.View_focusable, true)) {
				setFlags(FOCUSABLE, FOCUSABLE_MASK); // GTK's default for the widget stays
			} else {
				setFocusable(false);
			}
		}
		if (a.hasValue(com.android.internal.R.styleable.View_visibility)) {
			visibility = VISIBILITY_FLAGS[a.getInt(com.android.internal.R.styleable.View_visibility, 0)];
			native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
		}
		if (a.hasValue(com.android.internal.R.styleable.View_alpha))
			setAlpha(a.getFloat(com.android.internal.R.styleable.View_alpha, 1f));

		if (a.hasValue(com.android.internal.R.styleable.View_minWidth))
			minWidth = a.getDimensionPixelSize(com.android.internal.R.styleable.View_minWidth, 0);

		if (a.hasValue(com.android.internal.R.styleable.View_minHeight))
			minHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.View_minHeight, 0);

		int padding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_padding, -1);

		int paddingVertical = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingVertical, -1);
		int paddingHorizontal = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingHorizontal, -1);

		int paddingStart = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingStart, -1);
		int paddingEnd = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingEnd, -1);

		/* AOSP View(Context, AttributeSet, int, int): setBackground() above took the padding of the
		 * background (the insets of an InsetDrawable, a nine-patch's padding); it stays the padding of
		 * every side the attributes do not define */
		boolean leftPaddingDefined = a.hasValue(com.android.internal.R.styleable.View_paddingLeft);
		boolean rightPaddingDefined = a.hasValue(com.android.internal.R.styleable.View_paddingRight);
		int leftPadding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingLeft, -1);
		int topPadding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingTop, -1);
		int rightPadding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingRight, -1);
		int bottomPadding = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingBottom, -1);

		if (padding >= 0) {
			leftPadding = padding;
			topPadding = padding;
			rightPadding = padding;
			bottomPadding = padding;
			leftPaddingDefined = true;
			rightPaddingDefined = true;
		} else {
			if (paddingHorizontal >= 0) {
				leftPadding = paddingHorizontal;
				rightPadding = paddingHorizontal;
				leftPaddingDefined = true;
				rightPaddingDefined = true;
			}
			if (paddingVertical >= 0) {
				topPadding = paddingVertical;
				bottomPadding = paddingVertical;
			}

			if (paddingHorizontal < 0) {
				/* like AOSP, start/end override left/right once resolved */
				if (paddingStart >= 0)
					userPaddingStart = paddingStart;

				if (paddingEnd >= 0)
					userPaddingEnd = paddingEnd;
			}
		}
		if (leftPaddingDefined)
			userPaddingLeftInitial = leftPadding;
		if (rightPaddingDefined)
			userPaddingRightInitial = rightPadding;
		if (topPadding >= 0)
			paddingTop = topPadding;
		if (bottomPadding >= 0)
			paddingBottom = bottomPadding;
		resolvePaddingValues();

		native_setPadding(widget, paddingLeft, paddingTop, paddingRight, paddingBottom);

		if (a.hasValue(com.android.internal.R.styleable.View_layoutDirection)) {
			/* the attribute's values are ltr, rtl, inherit and locale, in the order of the constants */
			rawLayoutDirection = a.getInt(com.android.internal.R.styleable.View_layoutDirection, LAYOUT_DIRECTION_INHERIT);
		}

		/* AOSP View(): the overScrollMode and soundEffectsEnabled attributes */
		if (a.hasValue(com.android.internal.R.styleable.View_overScrollMode))
			setOverScrollMode(a.getInt(com.android.internal.R.styleable.View_overScrollMode, OVER_SCROLL_IF_CONTENT_SCROLLS));
		if (a.hasValue(com.android.internal.R.styleable.View_soundEffectsEnabled))
			setSoundEffectsEnabled(a.getBoolean(com.android.internal.R.styleable.View_soundEffectsEnabled, true));

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

		if (a.hasValue(com.android.internal.R.styleable.View_clickable)) {
			setClickable(a.getBoolean(com.android.internal.R.styleable.View_clickable, false));
		}
		if (a.hasValue(com.android.internal.R.styleable.View_longClickable)) {
			setLongClickable(a.getBoolean(com.android.internal.R.styleable.View_longClickable, false));
		}

		String handlerName = a.getString(com.android.internal.R.styleable.View_onClick);
		if (handlerName != null)
			setOnClickListener(new DeclaredOnClickListener(this, handlerName));

		if (a.hasValue(com.android.internal.R.styleable.View_stateListAnimator)) {
			int resId = a.getResourceId(com.android.internal.R.styleable.View_stateListAnimator, 0);
			setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, resId));
		}
		duplicate_parent_state = a.getBoolean(com.android.internal.R.styleable.View_duplicateParentState, false);
		/* the transform attributes, as AOSP's View(Context, AttributeSet, int, int) */
		if (a.hasValue(com.android.internal.R.styleable.View_transformPivotX))
			setPivotX(a.getDimension(com.android.internal.R.styleable.View_transformPivotX, 0));
		if (a.hasValue(com.android.internal.R.styleable.View_transformPivotY))
			setPivotY(a.getDimension(com.android.internal.R.styleable.View_transformPivotY, 0));
		boolean transformSet = false;
		float tx = 0;
		float ty = 0;
		float tz = 0;
		float elevation = 0;
		float rotation = 0;
		float rotationX = 0;
		float rotationY = 0;
		float sx = 1f;
		float sy = 1f;
		if (a.hasValue(com.android.internal.R.styleable.View_translationX)) {
			tx = a.getDimension(com.android.internal.R.styleable.View_translationX, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_translationY)) {
			ty = a.getDimension(com.android.internal.R.styleable.View_translationY, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_translationZ)) {
			tz = a.getDimension(com.android.internal.R.styleable.View_translationZ, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_elevation)) {
			elevation = a.getDimension(com.android.internal.R.styleable.View_elevation, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_rotation)) {
			rotation = a.getFloat(com.android.internal.R.styleable.View_rotation, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_rotationX)) {
			rotationX = a.getFloat(com.android.internal.R.styleable.View_rotationX, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_rotationY)) {
			rotationY = a.getFloat(com.android.internal.R.styleable.View_rotationY, 0);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_scaleX)) {
			sx = a.getFloat(com.android.internal.R.styleable.View_scaleX, 1f);
			transformSet = true;
		}
		if (a.hasValue(com.android.internal.R.styleable.View_scaleY)) {
			sy = a.getFloat(com.android.internal.R.styleable.View_scaleY, 1f);
			transformSet = true;
		}
		if (transformSet) {
			setTranslationX(tx);
			setTranslationY(ty);
			setTranslationZ(tz);
			setElevation(elevation);
			setRotation(rotation);
			setRotationX(rotationX);
			setRotationY(rotationY);
			setScaleX(sx);
			setScaleY(sy);
		}
		a.recycle();
	}

	/* AOSP View.onCreateDrawableState(); ATL: state_focused follows whether GTK shows the focus
	 * (AOSP: focused, which outside of touch mode is the same), no accelerated state */
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
		if ((privateFlags & PFLAG_HOVERED) != 0) viewStateIndex |= StateSet.VIEW_STATE_HOVERED;

		final int privateFlags2 = mPrivateFlags2;
		if ((privateFlags2 & PFLAG2_DRAG_CAN_ACCEPT) != 0) {
			viewStateIndex |= StateSet.VIEW_STATE_DRAG_CAN_ACCEPT;
		}
		if ((privateFlags2 & PFLAG2_DRAG_HOVERED) != 0) {
			viewStateIndex |= StateSet.VIEW_STATE_DRAG_HOVERED;
		}

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

	@SuppressWarnings("unchecked")
	public <T extends View> T findViewById(int id) {
		if (id != NO_ID && this.id == id) // as in AOSP: views without an id are not found by it
			return (T) this;
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
		if (mOverlay != null && !mOverlay.isEmpty()) {
			mOverlay.draw(canvas);
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
		if (parent instanceof ViewGroup) {
			((ViewGroup)parent).onSetLayoutParams(this, params);
		}
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
	private TouchDelegate mTouchDelegate = null;

	/* AOSP View.setAllowClickWhenDisabled() (PFLAG4_ALLOW_CLICK_WHEN_DISABLED) */
	private boolean mAllowClickWhenDisabled;

	public void setAllowClickWhenDisabled(boolean clickableWhenDisabled) {
		mAllowClickWhenDisabled = clickableWhenDisabled;
	}

	@Export
	public boolean onTouchEventInternal(MotionEvent event, boolean handle_gestures) {
		boolean handled = false;
		if (on_touch_listener != null && isEnabled())
			handled = on_touch_listener.onTouch(this, event);

		if (!handled)
			handled = onTouchEvent(event);

		if (handle_gestures && !handled && event.getAction() == MotionEvent.ACTION_UP) {
			InputTrace.userTouch(this, InputTrace.CLICK);
			handled = performClick();
		}

		return handled;
	}

	/* AOSP's PFLAG_PREPRESSED: a view in a scrolling container is pressed only after the tap timeout */
	static final int PFLAG_PREPRESSED = 0x02000000;

	private CheckForLongPress mPendingCheckForLongPress;
	private CheckForTap mPendingCheckForTap = null;
	private PerformClick mPerformClick;
	private UnsetPressedState mUnsetPressedState;
	private boolean mHasPerformedLongPress;
	private boolean mIgnoreNextUpEvent;
	private boolean mInContextButtonPress;
	int mWindowAttachCount;

	/* AOSP View.getWindowAttachCount() */
	protected int getWindowAttachCount() {
		return mWindowAttachCount;
	}
	private float mLongClickX = Float.NaN;
	private float mLongClickY = Float.NaN;

	/* AOSP View.onTouchEvent() (android-14) without what ATL has no counterpart for: tooltips on touch
	 * (GTK shows the tooltip, see setTooltipText()), mouse button actions (performButtonActionOnTouchDown,
	 * the buttons are the native input stack's) and the touch classification (ATL's events are always
	 * CLASSIFICATION_NONE) */
	@Export
	public boolean onTouchEvent(MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();
		final int action = event.getAction();

		final boolean clickable = isClickable() || isLongClickable();

		if (mTouchDelegate != null) {
			if (mTouchDelegate.onTouchEvent(event)) {
				return true;
			}
		}

		if (!isEnabled() && !mAllowClickWhenDisabled) {
			// A disabled view that is clickable still consumes the touch events, it just doesn't respond to them.
			if (action == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {
				setPressed(false);
			}
			return clickable;
		}

		if (clickable) {
			switch (action) {
				case MotionEvent.ACTION_UP:
					boolean prepressed = (mPrivateFlags & PFLAG_PREPRESSED) != 0;
					if ((mPrivateFlags & PFLAG_PRESSED) != 0 || prepressed) {
						// take focus if we don't have it already and we should in touch mode.
						boolean focusTaken = false;
						if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
							focusTaken = requestFocus();
						}

						if (prepressed) {
							// The button is being released before we actually
							// showed it as pressed.  Make it show the pressed
							// state now (before scheduling the click) to ensure
							// the user sees it.
							setPressed(true, x, y);
						}

						if (!mHasPerformedLongPress && !mIgnoreNextUpEvent) {
							// This is a tap, so remove the longpress check
							removeLongPressCallback();

							// Only perform take click actions if we were in the pressed state
							if (!focusTaken) {
								// Use a Runnable and post this rather than calling
								// performClick directly. This lets other visual state
								// of the view update before click actions start.
								if (mPerformClick == null) {
									mPerformClick = new PerformClick();
								}
								if (!post(mPerformClick)) {
									InputTrace.userTouch(View.this, InputTrace.CLICK);
									performClick();
								}
							}
						}

						if (mUnsetPressedState == null) {
							mUnsetPressedState = new UnsetPressedState();
						}

						if (prepressed) {
							postDelayed(mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
						} else if (!post(mUnsetPressedState)) {
							// If the post failed, unpress right now
							mUnsetPressedState.run();
						}

						removeTapCallback();
					}
					mIgnoreNextUpEvent = false;
					break;

				case MotionEvent.ACTION_DOWN:
					mHasPerformedLongPress = false;

					// Walk up the hierarchy to determine if we're inside a scrolling container.
					boolean isInScrollingContainer = isInScrollingContainer();

					// For views inside a scrolling container, delay the pressed feedback for
					// a short period in case this is a scroll.
					if (isInScrollingContainer) {
						mPrivateFlags |= PFLAG_PREPRESSED;
						if (mPendingCheckForTap == null) {
							mPendingCheckForTap = new CheckForTap();
						}
						mPendingCheckForTap.x = event.getX();
						mPendingCheckForTap.y = event.getY();
						postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
					} else {
						// Not inside a scrolling container, so show the feedback right away
						setPressed(true, x, y);
						checkForLongClick(ViewConfiguration.getLongPressTimeout(), x, y);
					}
					break;

				case MotionEvent.ACTION_CANCEL:
					setPressed(false);
					removeTapCallback();
					removeLongPressCallback();
					mInContextButtonPress = false;
					mHasPerformedLongPress = false;
					mIgnoreNextUpEvent = false;
					break;

				case MotionEvent.ACTION_MOVE:
					drawableHotspotChanged(x, y);

					// Be lenient about moving outside of buttons
					if (!pointInView(x, y, ViewConfiguration.get(context).getScaledTouchSlop())) {
						// Outside button
						// Remove any future long press/tap checks
						removeTapCallback();
						removeLongPressCallback();
						if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
							setPressed(false);
						}
					}
					break;
			}

			return true;
		}

		return false;
	}

	/* AOSP View.isInScrollingContainer(), pointInView(), the remove*Callback() helpers */
	public boolean isInScrollingContainer() {
		ViewParent p = getParent();
		while (p != null && p instanceof ViewGroup) {
			if (((ViewGroup)p).shouldDelayChildPressedState()) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	public boolean pointInView(float localX, float localY, float slop) {
		return localX >= -slop && localY >= -slop && localX < ((right - left) + slop) && localY < ((bottom - top) + slop);
	}

	private void removeLongPressCallback() {
		if (mPendingCheckForLongPress != null) {
			removeCallbacks(mPendingCheckForLongPress);
		}
	}

	private void removePerformClickCallback() {
		if (mPerformClick != null) {
			removeCallbacks(mPerformClick);
		}
	}

	private void removeUnsetPressCallback() {
		if ((mPrivateFlags & PFLAG_PRESSED) != 0 && mUnsetPressedState != null) {
			setPressed(false);
			removeCallbacks(mUnsetPressedState);
		}
	}

	private void removeTapCallback() {
		if (mPendingCheckForTap != null) {
			mPrivateFlags &= ~PFLAG_PREPRESSED;
			removeCallbacks(mPendingCheckForTap);
		}
	}

	/* AOSP View.cancelLongPress() */
	public void cancelLongPress() {
		removeLongPressCallback();

		/*
		 * The prepressed state handled by the tap callback is a display
		 * construct, but the tap callback will post a long press callback
		 * less than its own timeout. Remove it here.
		 */
		removeTapCallback();
	}

	/* AOSP View.checkForLongClick() */
	private void checkForLongClick(long delay, float x, float y) {
		if (isLongClickable()) {
			mHasPerformedLongPress = false;

			if (mPendingCheckForLongPress == null) {
				mPendingCheckForLongPress = new CheckForLongPress();
			}
			mPendingCheckForLongPress.setAnchor(x, y);
			mPendingCheckForLongPress.rememberWindowAttachCount();
			mPendingCheckForLongPress.rememberPressedState();
			postDelayed(mPendingCheckForLongPress, delay);
		}
	}

	private final class CheckForLongPress implements Runnable {
		private int mOriginalWindowAttachCount;
		private float mX;
		private float mY;
		private boolean mOriginalPressedState;

		private CheckForLongPress() {
		}

		@Override
		public void run() {
			if ((mOriginalPressedState == isPressed()) && (parent != null) && mOriginalWindowAttachCount == mWindowAttachCount) {
				InputTrace.userTouch(View.this, InputTrace.LONG_CLICK);
				if (performLongClick(mX, mY)) {
					mHasPerformedLongPress = true;
				}
			}
		}

		public void setAnchor(float x, float y) {
			mX = x;
			mY = y;
		}

		public void rememberWindowAttachCount() {
			mOriginalWindowAttachCount = mWindowAttachCount;
		}

		public void rememberPressedState() {
			mOriginalPressedState = isPressed();
		}
	}

	private final class CheckForTap implements Runnable {
		public float x;
		public float y;

		@Override
		public void run() {
			mPrivateFlags &= ~PFLAG_PREPRESSED;
			setPressed(true, x, y);
			final long delay = ViewConfiguration.getLongPressTimeout() - ViewConfiguration.getTapTimeout();
			checkForLongClick(delay, x, y);
		}
	}

	private final class PerformClick implements Runnable {
		@Override
		public void run() {
			InputTrace.userTouch(View.this, InputTrace.CLICK);
			performClick();
		}
	}

	private final class UnsetPressedState implements Runnable {
		@Override
		public void run() {
			setPressed(false);
		}
	}

	/* AOSP View.setPressed(boolean, float, float) */
	private void setPressed(boolean pressed, float x, float y) {
		if (pressed) {
			drawableHotspotChanged(x, y);
		}

		setPressed(pressed);
	}

	public void setOnTouchListener(OnTouchListener l) {
		nativeSetOnTouchListener(widget);
		on_touch_listener = l;
	}

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
	public void setOnScrollChangeListener(OnScrollChangeListener l) {
		on_scroll_change_listener = l;
	}

	/* AOSP View.PUBLIC_STATUS_BAR_VISIBILITY_MASK, SYSTEM_UI_CLEARABLE_FLAGS */
	public static final int PUBLIC_STATUS_BAR_VISIBILITY_MASK = 0x00003FF7;
	public static final int SYSTEM_UI_CLEARABLE_FLAGS = SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_FULLSCREEN;

	/* AOSP View.setOnSystemUiVisibilityChangeListener()/dispatchSystemUiVisibilityChanged() */
	public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {
		getListenerInfo().mOnSystemUiVisibilityChangeListener = l;
	}

	public void dispatchSystemUiVisibilityChanged(int visibility) {
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnSystemUiVisibilityChangeListener != null) {
			li.mOnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange(visibility & PUBLIC_STATUS_BAR_VISIBILITY_MASK);
		}
	}

	/* AOSP ViewRootImpl.handleDispatchSystemUiVisibilityChanged(): the window's views hear the clearable flags in
	 * effect, from the main loop and only when they changed; here the flags are in effect as soon as they are set */
	private int mDispatchedSystemUiVisibility;
	public final int getWidth() {
		return right - left;
	}
	public final int getHeight() {
		return bottom - top;
	}

	protected native long native_constructor(Context context, AttributeSet attrs); // will create a custom GtkWidget with a custom drawing function
	static native boolean native_hasAndroidLayout(long widget);
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
	/* should be called from onGenericMotionEvent() of GtkWidget implementations that scroll by themselves:
	 * the GTK widget gets the wheel event, even when another view lies over it */
	protected native boolean native_gtkScrollTarget(long widget);
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

	/* AOSP View.setOnKeyListener(); ATL passes the keys on only when the view's widget has the focus
	 * and the key reaches its wrapper widget (nativeSetOnKeyListener() makes sure it listens) */
	public void setOnKeyListener(OnKeyListener l) {
		getListenerInfo().mOnKeyListener = l;
		nativeSetOnKeyListener(widget);
	}
	private native void nativeSetOnKeyListener(long widget);

	/* AOSP View.setFocusable(), setFocusableInTouchMode(), isFocusable(), isFocusableInTouchMode() */
	public void setFocusable(boolean focusable) {
		setFocusable(focusable ? FOCUSABLE : NOT_FOCUSABLE);
	}

	public void setFocusable(int focusable) {
		if ((focusable & (FOCUSABLE_AUTO | FOCUSABLE)) == 0) {
			setFlags(0, FOCUSABLE_IN_TOUCH_MODE);
		}
		setFlags(focusable, FOCUSABLE_MASK);
		/* ATL: what GTK's widget can focus follows an explicit setFocusable(), not FOCUSABLE_AUTO or touch mode */
		if ((focusable & FOCUSABLE_AUTO) == 0) {
			native_setNativeFocusable(widget, (focusable & FOCUSABLE) != 0);
		}
	}

	public void setFocusableInTouchMode(boolean focusableInTouchMode) {
		setFlags(focusableInTouchMode ? FOCUSABLE_IN_TOUCH_MODE : 0, FOCUSABLE_IN_TOUCH_MODE);

		if (focusableInTouchMode) {
			setFlags(FOCUSABLE, FOCUSABLE_MASK);
		}
	}

	public final boolean isFocusableInTouchMode() {
		return FOCUSABLE_IN_TOUCH_MODE == (mViewFlags & FOCUSABLE_IN_TOUCH_MODE);
	}

	/**
	 * AOSP View.setFlags() for the flags ATL keeps in mViewFlags; the parts for visibility, drawing and
	 * accessibility have no ATL counterpart, ATL tells GTK instead.
	 */
	void setFlags(int flags, int mask) {
		int old = mViewFlags;
		mViewFlags = (mViewFlags & ~mask) | (flags & mask);

		int changed = mViewFlags ^ old;
		if (changed == 0) {
			return;
		}

		int focusableChangedByAuto = 0;
		if (((mViewFlags & FOCUSABLE_AUTO) != 0) && (changed & (FOCUSABLE_MASK | CLICKABLE)) != 0) {
			final int newFocus;
			if ((mViewFlags & CLICKABLE) != 0) {
				newFocus = FOCUSABLE;
			} else {
				newFocus = NOT_FOCUSABLE;
			}
			mViewFlags = (mViewFlags & ~FOCUSABLE) | newFocus;
			focusableChangedByAuto = (old & FOCUSABLE) ^ (newFocus & FOCUSABLE);
			changed = (changed & ~FOCUSABLE) | focusableChangedByAuto;
		}

		if ((changed & FOCUSABLE) != 0) {
			if (((old & FOCUSABLE) == FOCUSABLE) && isFocused()) {
				clearFocus();
			}
		}

		if ((changed & (FOCUSABLE | FOCUSABLE_AUTO | CLICKABLE)) != 0) {
			updateKeyboardFocusable();
		}

		if ((changed & (CLICKABLE | LONG_CLICKABLE | ENABLED_MASK)) != 0) {
			updateHoverable();
		}

		if ((changed & ENABLED_MASK) != 0) {
			if ((mViewFlags & ENABLED_MASK) != ENABLED) {
				if (isFocused()) clearFocus();
			}
			native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
		}
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
		/* AOSP handleFocusGainInternal(): a focused view stays as it is (GTK would grab the focus again, and a
		 * GtkText then selects all of its text) */
		if (isFocused())
			return true;
		if (isEnabled() && isFocusable() && visibility == VISIBLE) {
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
		final View root = getRootView();
		final int clearable = visibility & SYSTEM_UI_CLEARABLE_FLAGS;
		if (isAttachedToWindow() && root.mDispatchedSystemUiVisibility != clearable) {
			root.mDispatchedSystemUiVisibility = clearable;
			root.post(new Runnable() {
				@Override
				public void run() {
					root.dispatchSystemUiVisibilityChanged(clearable);
				}
			});
		}
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
		invalidateInternal();
	}

	/* AOSP View.scheduleDrawable()/unscheduleDrawable(): the drawable is the token of its runnables (AOSP: the
	 * Choreographer's), so unscheduleDrawable(who) removes all of them */
	public void scheduleDrawable(Drawable drawable, Runnable runnable, long time) {
		if (runnable != null)
			getHandler().postAtTime(runnable, drawable, time);
	}

	public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
		if (runnable != null)
			getHandler().removeCallbacks(runnable, drawable);
	}

	public void unscheduleDrawable(Drawable drawable) {
		if (drawable != null)
			getHandler().removeCallbacksAndMessages(drawable);
	}

	public void invalidate(Rect dirty) {
		invalidateInternal();
	}
	public void invalidate(int l, int t, int r, int b) {
		invalidateInternal();
	}
	public void invalidate() {
		invalidateInternal();
	}
	/* as in AOSP, the other invalidations don't go through invalidate(), which a view may override: Material's
	 * BaseProgressIndicator.invalidate() invalidates its drawable, and ProgressBar.invalidateDrawable()
	 * invalidates the dirty rectangle */
	private void invalidateInternal() {
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
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
		/* a view that goes while it animates: the animation runs on as on AOSP (see GoneAnimationTicker) */
		if (visibility == GONE && currentAnimation != null && animationMore)
			getHandler().post(new GoneAnimationTicker(currentAnimation));
		dispatchVisibilityChanged(this, visibility);
		if (isAttachedToWindow()) {
			dispatchVisibilityAggregated(!(parent instanceof View) || ((View)parent).isShown());
		}
	}

	public void setPadding(int left, int top, int right, int bottom) {
		userPaddingStart = UNDEFINED_PADDING;
		userPaddingEnd = UNDEFINED_PADDING;
		userPaddingLeftInitial = left;
		userPaddingRightInitial = right;
		/* AOSP View.internalSetPadding(): a changed padding needs a new measure and layout */
		boolean paddingChanged = paddingLeft != left || paddingTop != top || paddingRight != right || paddingBottom != bottom;
		paddingLeft = left;
		paddingTop = top;
		paddingRight = right;
		paddingBottom = bottom;
		native_setPadding(widget, left, top, right, bottom);
		if (paddingChanged) {
			requestLayout();
		}
	}
	public native void native_setPadding(long widget, int left, int top, int right, int bottom);

	public void setBackgroundResource(int resid) {
		setBackground(resid == 0 ? null : getContext().getDrawable(resid));
	}

	public void getHitRect(Rect outRect) {
		outRect.set(left, top, right, bottom);
	}

	@Export
	public final int getScrollX() {
		return scrollX;
	}

	@Export
	public final int getScrollY() {
		return scrollY;
	}

	/* AOSP's View.scrollTo() */
	public void scrollTo(int x, int y) {
		if (scrollX != x || scrollY != y) {
			int oldX = scrollX;
			int oldY = scrollY;
			scrollX = x;
			scrollY = y;
			onScrollChanged(scrollX, scrollY, oldX, oldY);
			invalidate();
		}
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

	/* AOSP's View.onScrollChanged(), the part that informs the listener */
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		if (on_scroll_change_listener != null)
			on_scroll_change_listener.onScrollChange(this, l, t, oldl, oldt);
	}

	public void getLocationOnScreen(int[] location) {
		Rect rect = new Rect();
		getGlobalVisibleRect(rect);
		location[0] = rect.left;
		location[1] = rect.top;
	}

	public boolean performHapticFeedback(int feedbackConstant) {
		return performHapticFeedback(feedbackConstant, 0);
	}

	/* AOSP View.performHapticFeedbackForInputDevice() (API 35): ATL has one vibrator, the feedback of the view */
	public void performHapticFeedbackForInputDevice(int feedbackConstant, int inputDeviceId, int inputSource, int flags) {
		performHapticFeedback(feedbackConstant, flags);
	}

	/* AOSP View.performHapticFeedback(HapticFeedbackRequest): a usage that is set must be of the feedback class
	 * (USAGE_CLASS_MASK 0xF, USAGE_CLASS_FEEDBACK 0x2), else no feedback; the rest as the int/int variant */
	public boolean performHapticFeedback(android.os.vibrator.HapticFeedbackRequest request) {
		int usage = request.getUsage();
		if (usage != 0 /* USAGE_UNKNOWN */ && (usage & 0xF) != 0x2)
			return false;
		return performHapticFeedback(request.getFeedbackConstant(), request.getFlags());
	}

	public boolean performHapticFeedback(int feedbackConstant, int flags) {
		/* AOSP View.performHapticFeedback(): nothing if the view turned it off, unless FLAG_IGNORE_VIEW_SETTING */
		if ((flags & 0x0001 /* HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING, no ATL class */) == 0 && !isHapticFeedbackEnabled())
			return false;
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

	public void setOnGenericMotionListener(View.OnGenericMotionListener l) {
		getListenerInfo().mOnGenericMotionListener = l;
	}

	/* AOSP's AttachInfo.mWindowToken (ViewRootImpl's IWindow binder): one per view hierarchy, kept by the
	 * root view of a window as for rootHasWindowFocus; null while the view is not attached */
	private IBinder windowToken;

	/* AOSP AttachInfo.getRootSurfaceControl() (ViewRootImpl): the root of the hierarchy, kept by the root view */
	private RootSurfaceControl rootSurfaceControl;

	/* AOSP View.getRootSurfaceControl(): only non-null between onAttachedToWindow() and onDetachedFromWindow() */
	public AttachedSurfaceControl getRootSurfaceControl() {
		if (!isAttachedToWindow())
			return null;
		View root = getRootView();
		if (root.rootSurfaceControl == null)
			root.rootSurfaceControl = new RootSurfaceControl();
		return root.rootSurfaceControl;
	}

	public IBinder getWindowToken() {
		if (!isAttachedToWindow())
			return null;
		View root = getRootView();
		if (root.windowToken == null)
			root.windowToken = new Binder();
		return root.windowToken;
	}

	/* AOSP: the location in the window, whose origin is that of the root view. ATL's getLocationOnScreen() is in
	 * the surface of the toplevel (with the window frame and the header bar), so the root view's location there
	 * comes off; popups placed with showAtLocation() take window coordinates */
	public void getLocationInWindow(int[] location) {
		getLocationOnScreen(location);
		final View root = getRootView();
		if (root == this) {
			location[0] = 0;
			location[1] = 0;
			return;
		}
		final int[] rootLocation = new int[2];
		root.getLocationOnScreen(rootLocation);
		location[0] -= rootLocation[0];
		location[1] -= rootLocation[1];
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

	@Export
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

	/* AOSP View.setAccessibilityDelegate(): kept for getAccessibilityDelegate(); ATL has no accessibility service
	 * that would call it */
	private AccessibilityDelegate mAccessibilityDelegate;

	public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
		mAccessibilityDelegate = delegate;
	}

	/* AOSP View.AccessibilityDelegate: every method does what the view itself does */
	public static class AccessibilityDelegate {
		public void sendAccessibilityEvent(View host, int eventType) {
			host.sendAccessibilityEventInternal(eventType);
		}

		public boolean performAccessibilityAction(View host, int action, Bundle args) {
			return host.performAccessibilityActionInternal(action, args);
		}

		public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
			host.sendAccessibilityEventUncheckedInternal(event);
		}

		public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
			return host.dispatchPopulateAccessibilityEventInternal(event);
		}

		public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
			host.onPopulateAccessibilityEventInternal(event);
		}

		public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
			host.onInitializeAccessibilityEventInternal(event);
		}

		public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
			host.onInitializeAccessibilityNodeInfoInternal(info);
		}

		public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
			return host.onRequestSendAccessibilityEventInternal(child, event);
		}

		/** As AOSP: the delegate provides no AccessibilityNodeProvider of its own. */
		public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
			return null;
		}

		public AccessibilityNodeInfo createAccessibilityNodeInfo(View host) {
			return host.createAccessibilityNodeInfoInternal();
		}
	}

	public Drawable getBackground() {
		return background;
	}

	public void setClickable(boolean clickable) {
		setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
	}

	/* like AOSP's FOCUSABLE_AUTO: a clickable view can be focused with the keyboard (Tab), and
	 * Enter or Space click it (View.onKeyUp()); clicking it with the pointer does not focus it */
	/* AOSP's focusable: android:focusable/setFocusable(), or FOCUSABLE_AUTO, which follows clickable */
	private boolean isFocusableResolved() {
		return isFocusable() && ((mViewFlags & FOCUSABLE_AUTO) == 0 || isClickable());
	}
	private void updateKeyboardFocusable() {
		native_setKeyboardFocusable(widget, isFocusableResolved());
	}
	private native void native_setKeyboardFocusable(long widget, boolean focusable);
	private native void native_setNativeFocusable(long widget, boolean focusable);

	public void setEnabled(boolean enabled) {
		if (enabled == isEnabled()) return;

		setFlags(enabled ? ENABLED : DISABLED, ENABLED_MASK);

		refreshDrawableState();

		if (!enabled) {
			cancelPendingInputEvents();
		}
	}

	public CharSequence getContentDescription() {
		return contentDescription;
	}

	private OnLongClickListener on_long_click_listener = null;
	private OnContextClickListener mOnContextClickListener;
	private boolean mContextClickable;

	public void setOnContextClickListener(OnContextClickListener l) {
		if (!isContextClickable())
			setContextClickable(true);
		mOnContextClickListener = l;
	}

	public boolean isContextClickable() {
		return mContextClickable;
	}

	public void setContextClickable(boolean contextClickable) {
		mContextClickable = contextClickable;
		updateHoverable();
	}

	public boolean performContextClick(float x, float y) {
		return performContextClick();
	}

	public boolean performContextClick() {
		return mOnContextClickListener != null && mOnContextClickListener.onContextClick(this);
	}

	/* AOSP View.performLongClick() and performLongClickInternal(), without the accessibility event,
	 * context menu, tooltip and haptic feedback ATL has no counterpart for */
	public boolean performLongClick() {
		return performLongClickInternal(mLongClickX, mLongClickY);
	}

	@Export
	public boolean performLongClick(float x, float y) {
		mLongClickX = x;
		mLongClickY = y;
		final boolean handled = performLongClick();
		mLongClickX = Float.NaN;
		mLongClickY = Float.NaN;
		return handled;
	}

	private boolean performLongClickInternal(float x, float y) {
		boolean handled = false;
		if (on_long_click_listener != null) {
			handled = on_long_click_listener.onLongClick(this);
		}
		return handled;
	}
	public void setOnLongClickListener(OnLongClickListener listener) {
		if (!isLongClickable())
			setLongClickable(true);
		nativeSetOnLongClickListener(widget);
		on_long_click_listener = listener;
	}
	protected native void nativeSetOnLongClickListener(long widget);

	public void setLongClickable(boolean longClickable) {
		setFlags(longClickable ? LONG_CLICKABLE : 0, LONG_CLICKABLE);
	}

	/* AOSP's View.setOnHoverListener(); the hover events are delivered by a GTK motion controller, which is
	 * installed once a view has a hover listener (or overrides one of the hover methods) */
	public void setOnHoverListener(OnHoverListener l) {
		getListenerInfo().mOnHoverListener = l;
		nativeSetOnHoverListener(widget);
	}

	protected native void nativeSetOnHoverListener(long widget);

	@Export
	public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
		if ((mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT || widthMeasureSpec != oldWidthMeasureSpec || heightMeasureSpec != oldHeightMeasureSpec) {
			resolveRtlPropertiesIfNeeded();
			oldWidthMeasureSpec = widthMeasureSpec;
			oldHeightMeasureSpec = heightMeasureSpec;
			boolean root = !(parent instanceof View);
			if (root)
				sRootMeasureDepth++;
			try {
				onMeasure(widthMeasureSpec, heightMeasureSpec);
			} finally {
				if (root)
					sRootMeasureDepth--;
			}
			mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
		}
	}

	/* the root of a window is being measured (AOSP: ViewRootImpl.measureHierarchy()) */
	private static int sRootMeasureDepth;

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

	/* AOSP View.setFrame(): sets the view's bounds and tells whether they changed. ATL calls onSizeChanged() from its
	 * own layout path (after GTK has allocated the widget), not from here. */
	protected boolean setFrame(int left, int top, int right, int bottom) {
		boolean changed = false;
		if (this.left != left || this.right != right || this.top != top || this.bottom != bottom) {
			changed = true;
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}
		return changed;
	}

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
		setFrame(l, t, r, b);
		if (this.in_layout)                   // check re-entrancy
			native_queueAllocate(widget); // GTK is not re-entrant-safe. Queue allocation
		else {
			/* AOSP lays out a view that was measured since its last layout (onLayout()) even if its bounds
			 * stay the same; GTK calls layoutInternal() only for a widget that is marked for allocation */
			if ((mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED)
				native_queueAllocate(widget);
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
		mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
		mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
		if (sizeChanged && needsOutline())
			invalidateOutline();
	}

	/* GTK sized a native view (TextView, Button, ...) that no ViewGroup lays out: the root of a PopupWindow,
	 * a row of a Spinner's list, a button of ATL's AlertDialog. AOSP's ViewRootImpl measures and lays out such
	 * a root at the size of its window; here the size is GTK's, which View.layout() never hears of */
	@Export
	private void layoutNativeRoot(int width, int height) {
		if (parent instanceof ViewGroup || (getWidth() == width && getHeight() == height && (mPrivateFlags3 & PFLAG3_IS_LAID_OUT) != 0))
			return;
		measure(width | MeasureSpec.EXACTLY, height | MeasureSpec.EXACTLY);
		int oldW = getWidth();
		int oldH = getHeight();
		boolean changed = setFrame(0, 0, width, height);
		if (changed)
			onSizeChanged(width, height, oldW, oldH);
		onLayout(changed, 0, 0, width, height);
		mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
		mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
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
		sLayoutDepth++;
		try {
			layoutInternalImpl(width, height);
		} finally {
			sLayoutDepth--;
		}
		/* AOSP ViewRootImpl.performLayout(): views that requested a layout while the window was laid out and
		 * still need one (not laid out after the request, not gone) request it again: GTK lays out once more */
		if (sLayoutDepth == 0 && !sLayoutRequesters.isEmpty()) {
			ArrayList<View> requesters = new ArrayList<>(sLayoutRequesters);
			sLayoutRequesters.clear();
			for (View view : requesters) {
				if ((view.mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0)
					continue;
				boolean gone = false;
				for (View v = view; v != null; v = v.parent instanceof View ? (View)v.parent : null)
					gone |= v.visibility == GONE;
				if (gone)
					continue;
				for (View v = view; v != null && (v.mPrivateFlags & PFLAG_FORCE_LAYOUT) != 0; v = v.parent instanceof View ? (View)v.parent : null)
					v.mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
				view.requestLayout();
			}
		}
	}

	/* AOSP ViewRootImpl.mLayoutRequesters: views that requested a layout during a layout pass */
	private static final ArrayList<View> sLayoutRequesters = new ArrayList<>();

	/* the layout passes under way (AOSP: ViewRootImpl.mInLayout of the root the view is in) */
	private static int sLayoutDepth;

	private void layoutInternalImpl(int width, int height) {
		in_layout = true;
		// if the layout is triggered from a native widget, we might not have measured yet
		// also measure again if a descendant requested a layout without our size changing (and GTK did not measure since)
		if (!(parent instanceof ViewGroup) && ((mPrivateFlags & (PFLAG_FORCE_LAYOUT | PFLAG_LAYOUT_REQUIRED)) == PFLAG_FORCE_LAYOUT || width != getMeasuredWidth() || height != getMeasuredHeight())) {
			forceLayout();
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
		mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
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
		/* AOSP: the same drawable again changes nothing (no padding, no layout request). Material's FloatingActionButton
		 * sets its background in every onMeasure(): its padding was set back and requestLayout() was called, without end */
		if (backgroundDrawable == this.background)
			return;
		this.background = backgroundDrawable;
		if (backgroundDrawable != null) {
			/* AOSP View.setBackgroundDrawable() */
			resetResolvedDrawablesInternal();
			backgroundDrawable.setLayoutDirection(getLayoutDirection());
		}
		if (backgroundDrawable instanceof ColorDrawable) {
			native_setBackgroundColor(widget, ((ColorDrawable)backgroundDrawable).getColor());
			if (needsOutline())
				invalidateOutline();
			return;
		}
		if (backgroundDrawable != null) {
			backgroundDrawable.setCallback(this);
			/* AOSP View.setBackgroundDrawable(): a stateful background (selector, ripple) starts in the
			 * view's current state, not in the empty one (which a state_enabled=false item matches) */
			if (backgroundDrawable.isStateful())
				backgroundDrawable.setState(getDrawableState());
			if (backgroundTint != 0)
				backgroundDrawable.setTint(backgroundTint);
			applyBackgroundTint();
			/* View.setBackgroundDrawable() of android-14: the padding of the background (the insets of an
			 * InsetDrawable, a nine-patch's padding) becomes the view's padding, so that a window or view
			 * with such a background is measured and laid out with room for it */
			Rect bgPadding = new Rect();
			if (backgroundDrawable.getPadding(bgPadding)) {
				userPaddingStart = UNDEFINED_PADDING;
				userPaddingEnd = UNDEFINED_PADDING;
				if (layoutRtl) {
					userPaddingLeftInitial = bgPadding.right;
					userPaddingRightInitial = bgPadding.left;
				} else {
					userPaddingLeftInitial = bgPadding.left;
					userPaddingRightInitial = bgPadding.right;
				}
				/* AOSP View.setBackgroundDrawable(): a background that brings another padding needs a new measure */
				boolean bgPaddingChanged = paddingLeft != userPaddingLeftInitial || paddingRight != userPaddingRightInitial
				                           || paddingTop != bgPadding.top || paddingBottom != bgPadding.bottom;
				paddingLeft = userPaddingLeftInitial;
				paddingRight = userPaddingRightInitial;
				paddingTop = bgPadding.top;
				paddingBottom = bgPadding.bottom;
				native_setPadding(widget, paddingLeft, paddingTop, paddingRight, paddingBottom);
				if (bgPaddingChanged)
					requestLayout();
			}
		}
		native_setBackgroundDrawable(widget, backgroundDrawable != null ? backgroundDrawable.paintable : 0);
		if (needsOutline())
			invalidateOutline();
	}

	/* AOSP View.mOverScrollMode, set by the overScrollMode attribute */
	private int mOverScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS;

	public int getOverScrollMode() { return mOverScrollMode; }

	/* AOSP's View.setFitsSystemWindows(): the view pads itself by the system window insets it is given
	 * (fitSystemWindowsInt()) */
	public void setFitsSystemWindows(boolean fitSystemWindows) {
		setFlags(fitSystemWindows ? FITS_SYSTEM_WINDOWS : 0, FITS_SYSTEM_WINDOWS);
	}

	/* hidden in AOSP, looked up by AppCompat via reflection; the optional fitting is for PhoneWindow's
	 * content listener, which ATL's window has no counterpart for */
	public void makeOptionalFitsSystemWindows() {}


	/* AOSP View.setScrollContainer()/isScrollContainer(): PFLAG_SCROLL_CONTAINER, counted (PFLAG_SCROLL_CONTAINER_ADDED)
	 * only while the view is attached; ATL has no AttachInfo list of scroll containers to resize */
	private boolean mScrollContainer;
	public void setScrollContainer(boolean isScrollContainer) {
		mScrollContainer = isScrollContainer;
	}
	public boolean isScrollContainer() {
		return mScrollContainer && isAttachedToWindow();
	}

	public boolean removeCallbacks(Runnable action) {
		getHandler().removeCallbacks(action);
		return true;
	}

	/* AOSP View.requestLayout(): the view and its parents up to the first one that has requested a layout
	 * already are marked. Only a request that reaches the root unmarked asks GTK for a new layout pass
	 * (ViewRootImpl.requestLayout()); otherwise the pending or running pass of the marked parent measures
	 * the view, and GTK is only asked to lay it out. A request while the root is measured starts no new
	 * pass (ViewRootImpl.measureHierarchy()), one during the layout is handled after it */
	public void requestLayout() {
		if (sRootMeasureDepth == 0 && sLayoutDepth > 0 && parent instanceof View && !sLayoutRequesters.contains(this))
			sLayoutRequesters.add(this);
		boolean rootRequest = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0 || !(parent instanceof View);
		mPrivateFlags |= PFLAG_FORCE_LAYOUT;
		View view = this;
		while (view.parent instanceof View) {
			view = (View)view.parent;
			if ((view.mPrivateFlags & PFLAG_FORCE_LAYOUT) != 0) {
				rootRequest = false;
				break;
			}
			/* AOSP calls mParent.requestLayout(): a parent that overrides it gets the call and goes on from
			 * there; Compose's AndroidViewsHandler remeasures the layout node of the child that asked */
			if (overridesRequestLayout(view.getClass())) {
				view.requestLayout();
				rootRequest = false;
				break;
			}
			view.mPrivateFlags |= PFLAG_FORCE_LAYOUT;
			rootRequest = true;
		}
		if (rootRequest && sRootMeasureDepth == 0)
			native_requestLayout(widget);
		else
			native_queueAllocate(widget);
	};

	private static final Map<Class<?>, Boolean> requestLayoutOverridden = new HashMap<>();

	private static boolean overridesRequestLayout(Class<?> viewClass) {
		Boolean overridden = requestLayoutOverridden.get(viewClass);
		if (overridden == null) {
			try {
				overridden = viewClass.getMethod("requestLayout").getDeclaringClass() != View.class;
			} catch (NoSuchMethodException e) {
				overridden = false;
			}
			requestLayoutOverridden.put(viewClass, overridden);
		}
		return overridden;
	}

	public void setOverScrollMode(int mode) {
		if (mode != OVER_SCROLL_ALWAYS && mode != OVER_SCROLL_IF_CONTENT_SCROLLS && mode != OVER_SCROLL_NEVER) {
			throw new IllegalArgumentException("Invalid overscroll mode " + mode);
		}
		mOverScrollMode = mode;
	}

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

	/* AOSP View.setTagInternal(): the keyed tag of the framework's own ids, without checking the key */
	public void setTagInternal(int key, Object tag) {
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


	/* AOSP View.bringToFront() */
	public void bringToFront() {
		if (parent != null)
			parent.bringChildToFront(this);
	}

	public boolean isEnabled() {
		return (mViewFlags & ENABLED_MASK) == ENABLED;
	}
	/* the GTK focus is in this view's widgets (ViewGroup refines this to AOSP's invariant) */
	public boolean hasFocus() { return nativeHasFocus(widget); }
	private static native boolean nativeHasFocus(long widget);
	public boolean isLayoutRequested() { return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT; }
	public int getBaseline() { return -1; }
	/* AOSP View.hasFocusable(), hasExplicitFocusable() and hasFocusable(boolean, boolean) (API >= 26: FOCUSABLE_AUTO
	 * counts for hasFocusable() only); ATL resolves FOCUSABLE_AUTO with isFocusableResolved() */
	public boolean hasFocusable() {
		return hasFocusable(true, false);
	}

	public boolean hasExplicitFocusable() {
		return hasFocusable(false, true);
	}

	boolean hasFocusable(boolean allowAutoFocus, boolean dispatchExplicit) {
		// Invisible, gone, or disabled views are never focusable.
		if (visibility != VISIBLE || !isEnabled()) {
			return false;
		}

		// Only use effective focusable value when allowed.
		if (allowAutoFocus ? isFocusableResolved() : ((mViewFlags & FOCUSABLE_AUTO) == 0 && isFocusable())) {
			return true;
		}

		return false;
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
			native_setVisibility(widget, visibility, alpha * transitionAlpha, isEnabled());
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
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
		this.alpha = alpha;
	}

	@Export
	public boolean onGenericMotionEvent(MotionEvent event) { return false; }

	/* AOSP's View.dispatchGenericMotionEvent(): the pointer events that are not touches (the mouse
	 * buttons, the wheel, hovering) go to the child under the pointer first. The hover events keep
	 * their GTK path (dispatchHoverEvent() is called from the hover controllers). */
	@Export
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		final int source = event.getSource();
		if ((source & InputDevice.SOURCE_CLASS_POINTER) != 0) {
			final int action = event.getAction();
			if (action == MotionEvent.ACTION_HOVER_ENTER
					|| action == MotionEvent.ACTION_HOVER_MOVE
					|| action == MotionEvent.ACTION_HOVER_EXIT) {
				if (dispatchHoverEvent(event)) {
					return true;
				}
			} else if (dispatchGenericPointerEvent(event)) {
				return true;
			}
		} else if (dispatchGenericFocusedEvent(event)) {
			return true;
		}

		return dispatchGenericMotionEventInternal(event);
	}

	/* AOSP's View.dispatchGenericMotionEventInternal(), without the context click (not implemented) */
	private boolean dispatchGenericMotionEventInternal(MotionEvent event) {
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnGenericMotionListener != null
				&& isEnabled()
				&& li.mOnGenericMotionListener.onGenericMotion(this, event)) {
			return true;
		}

		return onGenericMotionEvent(event);
	}

	protected boolean dispatchGenericPointerEvent(MotionEvent event) {
		return false;
	}

	protected boolean dispatchGenericFocusedEvent(MotionEvent event) {
		return false;
	}

	protected boolean awakenScrollBars() { return false; }

	protected native boolean native_getMatrix(long widget, long matrix);
	/* as AOSP's RenderProperties.updateMatrix(): translation, then rotation and scale about the pivot; with a
	 * rotation about X or Y the camera projects the rotated view (native_layout() hands this matrix to GTK) */
	@Export
	public Matrix getMatrix() {
		if (matrix == null)
			matrix = new Matrix();
		if (!has3dRotation()) {
			matrix.setTranslate(translationX, translationY);
			matrix.preRotate(rotation, getPivotX(), getPivotY());
			matrix.preScale(scaleX, scaleY, getPivotX(), getPivotY());
		} else {
			if (camera == null)
				camera = new Camera();
			if (transform3D == null)
				transform3D = new Matrix();
			camera.setLocation(0, 0, cameraLocationZ);
			matrix.reset();
			camera.save();
			matrix.preScale(scaleX, scaleY, getPivotX(), getPivotY());
			camera.rotateX(rotationX);
			camera.rotateY(rotationY);
			camera.rotateZ(-rotation);
			camera.getMatrix(transform3D);
			transform3D.preTranslate(-getPivotX(), -getPivotY());
			transform3D.postTranslate(getPivotX() + translationX, getPivotY() + translationY);
			matrix.postConcat(transform3D);
			camera.restore();
		}
		return matrix;
	}

	/* MathUtils::isZero() of RenderProperties */
	private boolean has3dRotation() {
		return Math.abs(rotationX) >= 0.001f || Math.abs(rotationY) >= 0.001f;
	}

	final Matrix getInverseMatrix() {
		if (inverseMatrix == null)
			inverseMatrix = new Matrix();
		/* as AOSP's RenderNode: a matrix without inverse (scale 0) gives the identity */
		if (!getMatrix().invert(inverseMatrix))
			inverseMatrix.reset();
		return inverseMatrix;
	}

	public boolean hasIdentityMatrix() {
		return translationX == 0 && translationY == 0 && !hasTransform;
	}

	/* GTK applies the transform when the parent allocates this view, see native_layout() */
	private void transformChanged() {
		boolean had = hasTransform;
		hasTransform = scaleX != 1 || scaleY != 1 || rotation != 0 || rotationX != 0 || rotationY != 0;
		if ((had || hasTransform) && parent instanceof View)
			((View)parent).native_queueAllocate(((View)parent).widget);
	}

	protected static final int[] EMPTY_STATE_SET = new int[0];

	protected static final int[] PRESSED_STATE_SET = new int[] {R.attr.state_pressed};

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

	/* AOSP: a focused view gives up the focus (GTK's focus change calls onFocusChanged() and tells the parents);
	 * outside of touch mode the root view then places the focus again (rootViewRequestFocus()) */
	public void clearFocus() {
		if (!isFocused())
			return;
		nativeClearFocus(widget);
		if (!isInTouchMode()) {
			getRootView().requestFocus();
		}
	}
	private native void nativeClearFocus(long widget);

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

	/* AOSP View.setSoundEffectsEnabled / isSoundEffectsEnabled (SOUND_EFFECTS_ENABLED, on by default) */
	private boolean mSoundEffectsEnabled = true;

	public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
		mSoundEffectsEnabled = soundEffectsEnabled;
	}

	public boolean isSoundEffectsEnabled() {
		return mSoundEffectsEnabled;
	}

	/* AOSP View.playSoundEffect(), which hands the effect to the root (ViewRootImpl.playSoundEffect()) and from
	 * there to the AudioManager; a view that is not attached to a window plays nothing. ATL's AudioManager has
	 * no sound effect samples, so nothing is heard on the desktop. */
	public void playSoundEffect(int soundConstant) {
		if (!isSoundEffectsEnabled() || !isAttachedToWindow()) {
			return;
		}
		final android.media.AudioManager audioManager = (android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
		if (audioManager != null && soundConstant == SoundEffectConstants.CLICK) {
			audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK);
		}
	}

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


	/* AOSP View.setOnCreateContextMenuListener(): makes the view long-clickable and keeps the listener
	 * (ATL shows no context menus, see showContextMenu()) */
	public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
		if (!isLongClickable())
			setLongClickable(true);
		getListenerInfo().mOnCreateContextMenuListener = l;
	}

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

	public boolean getFitsSystemWindows() {
		return (mViewFlags & FITS_SYSTEM_WINDOWS) == FITS_SYSTEM_WINDOWS;
	}

	public void setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener l) {
		on_apply_window_insets_listener = l;
	}

	/* AOSP View.dispatchApplyWindowInsets() */
	public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
		try {
			mPrivateFlags3 |= PFLAG3_APPLYING_INSETS;
			if (on_apply_window_insets_listener != null) {
				return on_apply_window_insets_listener.onApplyWindowInsets(this, insets);
			} else {
				return onApplyWindowInsets(insets);
			}
		} finally {
			mPrivateFlags3 &= ~PFLAG3_APPLYING_INSETS;
		}
	}

	public final boolean isFocusable() {
		return FOCUSABLE == (mViewFlags & FOCUSABLE);
	}
	@Export
	public boolean isClickable() {
		return (mViewFlags & CLICKABLE) == CLICKABLE;
	}
	public boolean isLongClickable() {
		return (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
	}

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
		/* AOSP: the drawables before the padding, as the padding may depend on their direction */
		if (!areDrawablesResolved())
			resolveDrawables();
		resolvePadding();
		onRtlPropertiesChanged(getLayoutDirection());
		return true;
	}

	/* AOSP View.PFLAG2_DRAWABLE_RESOLVED */
	private boolean drawablesResolved = false;

	boolean areDrawablesResolved() { return drawablesResolved; }

	/** AOSP View.resolveDrawables(): hand the layout direction on to the background, the foreground
	 * and the default focus highlight, then onResolveDrawables() @hide */
	protected void resolveDrawables() {
		// Drawables resolution may need to happen before resolving the layout direction (which is
		// done only during the measure() call).
		// If the layout direction is not resolved yet, we cannot resolve the Drawables except in
		// one case: when the raw layout direction has not been defined as LAYOUT_DIRECTION_INHERIT.
		// So, if the raw layout direction is LAYOUT_DIRECTION_LTR or LAYOUT_DIRECTION_RTL or
		// LAYOUT_DIRECTION_LOCALE, we can "cheat" and we don't need to wait for the layout
		// direction to be resolved as its resolved value will be the same as its raw value.
		if (!isLayoutDirectionResolved() && getRawLayoutDirection() == View.LAYOUT_DIRECTION_INHERIT) {
			return;
		}

		final int layoutDirection = isLayoutDirectionResolved() ? getLayoutDirection() : getRawLayoutDirection();

		if (background != null) {
			background.setLayoutDirection(layoutDirection);
		}
		if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
			mForegroundInfo.mDrawable.setLayoutDirection(layoutDirection);
		}
		if (mDefaultFocusHighlight != null) {
			mDefaultFocusHighlight.setLayoutDirection(layoutDirection);
		}
		drawablesResolved = true;
		onResolveDrawables(layoutDirection);
	}

	/** AOSP View.onResolveDrawables(): called when the layout direction has been resolved, does nothing @hide */
	public void onResolveDrawables(int layoutDirection) {}

	/** AOSP View.resetResolvedDrawables() @hide */
	protected void resetResolvedDrawables() {
		resetResolvedDrawablesInternal();
	}

	void resetResolvedDrawablesInternal() {
		drawablesResolved = false;
	}

	/** @hide */
	public void resetRtlProperties() {
		layoutDirectionResolved = false;
		layoutRtl = false;
		resetResolvedDrawables();
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
		boolean paddingChanged = paddingLeft != userPaddingLeftInitial || paddingTop != top || paddingRight != userPaddingRightInitial || paddingBottom != bottom;
		paddingLeft = userPaddingLeftInitial;
		paddingTop = top;
		paddingRight = userPaddingRightInitial;
		paddingBottom = bottom;
		native_setPadding(widget, paddingLeft, top, paddingRight, bottom);
		if (paddingChanged)
			requestLayout();
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


	private boolean clipToOutline;
	private boolean has_outline_clip;
	public void setClipToOutline(boolean clipToOutline) {
		if (this.clipToOutline == clipToOutline)
			return;
		this.clipToOutline = clipToOutline;
		invalidateOutline();
	}

	/* AOSP View.hasTransientState() / setHasTransientState(): nested calls are counted, the parent is told when
	 * the state turns on or off (PFLAG2_HAS_TRANSIENT_STATE, mTransientStateCount) */
	private boolean mHasTransientState;
	private int mTransientStateCount = 0;

	public boolean hasTransientState() {
		return mHasTransientState;
	}

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
			if (isAttachedToWindow()) {
				stateListAnimator.setState(getDrawableState());
			}
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

	/* AOSP's ForegroundInfo, the tint as AOSP's TintInfo fields */
	private static class ForegroundInfo {
		private Drawable mDrawable;
		private ColorStateList mTintList;
		private BlendMode mBlendMode;
		private boolean mHasTintList;
		private boolean mHasTintMode;
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
			applyForegroundTint();
			foreground = mForegroundInfo.mDrawable;
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

	/* AOSP View.setForegroundTintList()/getForegroundTintList(), setForegroundTintMode()/getForegroundTintMode(),
	 * setForegroundTintBlendMode()/getForegroundTintBlendMode(), applyForegroundTint() */
	public void setForegroundTintList(ColorStateList tint) {
		if (mForegroundInfo == null)
			mForegroundInfo = new ForegroundInfo();
		mForegroundInfo.mTintList = tint;
		mForegroundInfo.mHasTintList = true;
		applyForegroundTint();
	}

	public ColorStateList getForegroundTintList() {
		return mForegroundInfo != null ? mForegroundInfo.mTintList : null;
	}

	public void setForegroundTintMode(PorterDuff.Mode tintMode) {
		BlendMode mode = null;
		if (tintMode != null)
			mode = BlendMode.fromValue(tintMode.nativeInt);
		setForegroundTintBlendMode(mode);
	}

	public void setForegroundTintBlendMode(BlendMode blendMode) {
		if (mForegroundInfo == null)
			mForegroundInfo = new ForegroundInfo();
		mForegroundInfo.mBlendMode = blendMode;
		mForegroundInfo.mHasTintMode = true;
		applyForegroundTint();
	}

	public PorterDuff.Mode getForegroundTintMode() {
		BlendMode blendMode = mForegroundInfo != null ? mForegroundInfo.mBlendMode : null;
		return blendMode != null ? BlendMode.blendModeToPorterDuffMode(blendMode) : null;
	}

	public BlendMode getForegroundTintBlendMode() {
		return mForegroundInfo != null ? mForegroundInfo.mBlendMode : null;
	}

	private void applyForegroundTint() {
		if (mForegroundInfo != null && mForegroundInfo.mDrawable != null
				&& (mForegroundInfo.mHasTintList || mForegroundInfo.mHasTintMode)) {
			mForegroundInfo.mDrawable = mForegroundInfo.mDrawable.mutate();
			if (mForegroundInfo.mHasTintList)
				mForegroundInfo.mDrawable.setTintList(mForegroundInfo.mTintList);
			if (mForegroundInfo.mHasTintMode)
				mForegroundInfo.mDrawable.setTintBlendMode(mForegroundInfo.mBlendMode);
			// The drawable (or one of its children) may not have been stateful before applying the tint
			if (mForegroundInfo.mDrawable.isStateful())
				mForegroundInfo.mDrawable.setState(getDrawableState());
		}
	}

	/* AOSP View.setRevealOnFocusHint()/getRevealOnFocusHint() (PFLAG3_NO_REVEAL_ON_FOCUS); final in AOSP, not
	 * here because ATL's TextView still declares its own final copy */
	private boolean mNoRevealOnFocus;
	public void setRevealOnFocusHint(boolean revealOnFocus) {
		mNoRevealOnFocus = !revealOnFocus;
	}
	public boolean getRevealOnFocusHint() {
		return !mNoRevealOnFocus;
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

	/** @hide */
	@android.annotation.IntDef(flag = true, prefix = {"SCROLL_CAPTURE_HINT_"}, value = {SCROLL_CAPTURE_HINT_AUTO, SCROLL_CAPTURE_HINT_EXCLUDE, SCROLL_CAPTURE_HINT_INCLUDE, SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS})
	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
	public @interface ScrollCaptureHint {}

	public static final int SCROLL_CAPTURE_HINT_AUTO = 0;
	public static final int SCROLL_CAPTURE_HINT_EXCLUDE = 0x1;
	public static final int SCROLL_CAPTURE_HINT_INCLUDE = 0x2;
	public static final int SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS = 0x4;

	/* AOSP keeps the hint in PFLAG4_SCROLL_CAPTURE_HINT_MASK, the callback in the ListenerInfo; ATL has no scroll
	 * capture (the screenshot of a window that scrolls), so both are only kept */
	private int mScrollCaptureHint;
	private ScrollCaptureCallback mScrollCaptureCallback;

	@ScrollCaptureHint
	public int getScrollCaptureHint() {
		return mScrollCaptureHint;
	}

	public void setScrollCaptureHint(@ScrollCaptureHint int hint) {
		// Since include/exclude are mutually exclusive, exclude takes precedence.
		if ((hint & SCROLL_CAPTURE_HINT_EXCLUDE) != 0)
			hint &= ~SCROLL_CAPTURE_HINT_INCLUDE;
		mScrollCaptureHint = hint & (SCROLL_CAPTURE_HINT_INCLUDE | SCROLL_CAPTURE_HINT_EXCLUDE | SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS);
	}

	public final void setScrollCaptureCallback(ScrollCaptureCallback callback) {
		mScrollCaptureCallback = callback;
	}

	private static native boolean nativeIsInTouchMode(long widget);

	/* AOSP View.isInTouchMode(): whether the window of the view is in touch mode, which the pointer enters and
	 * the keyboard leaves; for a view that is not attached AOSP asks the window manager, which says yes as long as
	 * nothing has left it */
	public boolean isInTouchMode() {
		return !isAttachedToWindow() || widget == 0 || nativeIsInTouchMode(widget);
	}


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
		mWindowAttachCount++;
		// We will need to evaluate the drawable state at least once.
		mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
		/* AOSP resolves the RTL properties when the view is measured after it was attached (the
		 * window's root view before); Compose initialises its layout direction there */
		resolveRtlPropertiesIfNeeded();
		onAttachedToWindow();
		/* AOSP View.dispatchAttachedToWindow(): the window the view joined is visible; the view hears it, and that it
		 * is visible itself if it and its ancestors are */
		onWindowVisibilityChanged(VISIBLE);
		if (isShown()) {
			onVisibilityAggregated(true);
		}

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

		if ((mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) != 0) {
			// If nobody has evaluated the drawable state yet, then do it now.
			refreshDrawableState();
		}
	}

	@Export
	void dispatchDetachedFromWindow() {
		if (!attachedToWindow)
			return;
		/* AOSP View.dispatchDetachedFromWindow(): the window is gone for the view */
		onWindowVisibilityChanged(GONE);
		if (isShown()) {
			onVisibilityAggregated(false);
		}
		onDetachedFromWindow();
		mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT; // AOSP's onDetachedFromWindowInternal()
		removeUnsetPressCallback();
		removeLongPressCallback();
		removePerformClickCallback();

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

	public static final int LAYER_TYPE_NONE = 0;
	public static final int LAYER_TYPE_SOFTWARE = 1;
	public static final int LAYER_TYPE_HARDWARE = 2;


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
				|| (mDefaultFocusHighlight == who) || (mOverlay != null && mOverlay.verifyDrawable(who));
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
	/* AOSP View.isOpaque() with computeOpaqueFlags(): an opaque background, no scroll bars or overlay scroll bars,
	 * and full alpha (getFinalAlpha() = alpha * transition alpha) */
	public boolean isOpaque() {
		final boolean opaqueBackground = background != null && background.getOpacity() == android.graphics.PixelFormat.OPAQUE;
		final int flags = mViewFlags;
		final boolean opaqueScrollbars = ((flags & SCROLLBARS_VERTICAL) == 0 && (flags & SCROLLBARS_HORIZONTAL) == 0)
			|| (flags & SCROLLBARS_STYLE_MASK) == SCROLLBARS_INSIDE_OVERLAY
			|| (flags & SCROLLBARS_STYLE_MASK) == SCROLLBARS_OUTSIDE_OVERLAY;
		return opaqueBackground && opaqueScrollbars && getAlpha() * getTransitionAlpha() >= 1.0f;
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

	/* AOSP View.getClipBounds(Rect): copies the clip bounds into outRect, false if there are none */
	public boolean getClipBounds(Rect outRect) {
		if (clipBounds != null) {
			outRect.set(clipBounds);
			return true;
		}
		return false;
	}

	/* AOSP View.setBackgroundTintList(): the tint (ColorStateList, stateful) and mode of the background
	 * drawable; AppCompat's ViewCompat and the Material buttons colour their backgrounds this way */
	private ColorStateList mBackgroundTintList;
	private PorterDuff.Mode mBackgroundTintMode;
	private boolean mHasBackgroundTintList;
	private boolean mHasBackgroundTintMode;

	public void setBackgroundTintList(ColorStateList tint) {
		mBackgroundTintList = tint;
		mHasBackgroundTintList = true;
		applyBackgroundTint();
	}

	private void applyBackgroundTint() {
		if (background != null && (mHasBackgroundTintList || mHasBackgroundTintMode)) {
			background = background.mutate();
			if (mHasBackgroundTintList)
				background.setTintList(mBackgroundTintList);
			if (mHasBackgroundTintMode)
				background.setTintMode(mBackgroundTintMode);
			if (background.isStateful())
				background.setState(getDrawableState());
		}
	}

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
		if (on_touch_listener != null && isEnabled() && on_touch_listener.onTouch(this, event))
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

	/* AOSP View.setHapticFeedbackEnabled()/isHapticFeedbackEnabled() */
	public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
		setFlags(hapticFeedbackEnabled ? HAPTIC_FEEDBACK_ENABLED : 0, HAPTIC_FEEDBACK_ENABLED);
	}
	public boolean isHapticFeedbackEnabled() {
		return HAPTIC_FEEDBACK_ENABLED == (mViewFlags & HAPTIC_FEEDBACK_ENABLED);
	}

	public StateListAnimator getStateListAnimator() { return state_list_animator; }

	/* AOSP View.requestFitSystemWindows(): up to ViewRootImpl, which applies the insets again; ATL's window does
	 * that in requestApplyInsets() (AOSP's requestApplyInsets() is requestFitSystemWindows()) */
	public void requestFitSystemWindows() {
		requestApplyInsets();
	}

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

	public void setRotationX(float rotationX) {
		if (this.rotationX == rotationX)
			return;
		this.rotationX = rotationX;
		transformChanged();
	}

	public void setRotationY(float rotationY) {
		if (this.rotationY == rotationY)
			return;
		this.rotationY = rotationY;
		transformChanged();
	}

	public float getRotationX() { return rotationX; }
	public float getRotationY() { return rotationY; }

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

	/* AOSP View.getWindowSystemUiVisibility(): the system UI flags the window has got, which here are the ones the
	 * last setSystemUiVisibility() of a view of the window set */
	public int getWindowSystemUiVisibility() {
		return isAttachedToWindow() ? getRootView().system_ui_visibility : 0;
	}

	/** @hide AOSP View.setDisabledSystemUiVisibility(): the system UI flags the window may not use while the
	 * action bar shows an action mode (ViewRootImpl takes them out). ATL's window has no system bars that
	 * the flags would hide, so there is nothing to take out. */
	public void setDisabledSystemUiVisibility(int flags) {
	}

	/** @hide AOSP View.hasContentOnApplyWindowInsetsListener(): whether the window applies the insets to its
	 * content, which AOSP's window does by default (Window.setDecorFitsSystemWindows(true)); ATL's window
	 * always fits its content (setDecorFitsSystemWindows() changes nothing), so it is the case for every
	 * attached view */
	protected boolean hasContentOnApplyWindowInsetsListener() {
		return isAttachedToWindow();
	}



	public ColorStateList getBackgroundTintList() { return mBackgroundTintList; }

	public PorterDuff.Mode getBackgroundTintMode() { return mBackgroundTintMode; }


	/* AOSP View.getWindowId(): the id of the window that holds the view, null while it is detached */
	public WindowId getWindowId() {
		return isAttachedToWindow() ? WindowId.of(getRootView()) : null;
	}

	/* AOSP View.isInLayout(): true while the root of the view is in a layout pass */
	public boolean isInLayout() {
		return sLayoutDepth > 0;
	}


	public Drawable getForeground() {
		return mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
	}

	/* AOSP View.setScrollbarFadingEnabled()/isScrollbarFadingEnabled(): kept in the ScrollabilityCache, false
	 * while there is none; ATL draws no Android scroll bars, so it is only kept */
	private boolean mFadeScrollBars;
	public void setScrollbarFadingEnabled(boolean fadeEnabled) {
		mFadeScrollBars = fadeEnabled;
	}
	public boolean isScrollbarFadingEnabled() {
		return mFadeScrollBars;
	}

	/* AOSP View.set/getScrollBarDefaultDelayBeforeFade(), set/getScrollBarFadeDuration(): kept in the
	 * ScrollabilityCache, ViewConfiguration's defaults while there is none */
	private int mScrollBarDefaultDelayBeforeFade = -1;
	private int mScrollBarFadeDuration = -1;
	public int getScrollBarDefaultDelayBeforeFade() {
		return mScrollBarDefaultDelayBeforeFade < 0 ? ViewConfiguration.getScrollDefaultDelay() : mScrollBarDefaultDelayBeforeFade;
	}
	public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
		mScrollBarDefaultDelayBeforeFade = scrollBarDefaultDelayBeforeFade;
	}
	public int getScrollBarFadeDuration() {
		return mScrollBarFadeDuration < 0 ? ViewConfiguration.getScrollBarFadeDuration() : mScrollBarFadeDuration;
	}
	public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
		mScrollBarFadeDuration = scrollBarFadeDuration;
	}

	/* AOSP View.setScrollBarStyle()/getScrollBarStyle(): bits of mViewFlags (no scroll bars to inset here) */
	public void setScrollBarStyle(int style) {
		mViewFlags = (mViewFlags & ~SCROLLBARS_STYLE_MASK) | (style & SCROLLBARS_STYLE_MASK);
	}
	public int getScrollBarStyle() {
		return mViewFlags & SCROLLBARS_STYLE_MASK;
	}

	/* AOSP keeps the size in the ScrollabilityCache and falls back to ViewConfiguration's; ATL draws
	 * no Android scroll bars, so the size is only kept for getScrollBarSize() */
	private int mScrollBarSize = -1;

	public int getScrollBarSize() {
		return mScrollBarSize < 0 ? ViewConfiguration.get(getContext()).getScaledScrollBarSize() : mScrollBarSize;
	}

	public void setScrollBarSize(int scrollBarSize) {
		mScrollBarSize = scrollBarSize;
	}

	/* AOSP View.getAttributeSourceResourceMap(): empty unless the developer option "view attribute inspection"
	 * (sDebugViewAttributes) recorded the sources, which ATL does not have */
	public Map<Integer, Integer> getAttributeSourceResourceMap() {
		return new HashMap<>();
	}

	/* AOSP View.setVerticalScrollbarThumbDrawable()/getVerticalScrollbarThumbDrawable(): kept for the getter, ATL
	 * draws no Android scroll bars */
	private Drawable mVerticalScrollbarThumbDrawable;
	public void setVerticalScrollbarThumbDrawable(Drawable drawable) {
		mVerticalScrollbarThumbDrawable = drawable;
	}
	public Drawable getVerticalScrollbarThumbDrawable() {
		return mVerticalScrollbarThumbDrawable;
	}

	/* AOSP View.setVerticalScrollbarPosition()/getVerticalScrollbarPosition(): kept (ATL draws no Android scroll
	 * bars, so nothing moves) */
	private int mVerticalScrollbarPosition;
	public void setVerticalScrollbarPosition(int position) {
		mVerticalScrollbarPosition = position;
	}
	public int getVerticalScrollbarPosition() {
		return mVerticalScrollbarPosition;
	}


	/* AOSP View.setTouchDelegate(). The delegate is asked in onTouchEvent(), which GTK calls for a view with a touch
	 * or click listener or an onTouchEvent() override; it does not add the touch controller itself, because that
	 * reallocates the wrapper (a SearchView sets its delegate from onLayout() and ended up laid out differently) */
	public void setTouchDelegate(TouchDelegate touchDelegate) {
		mTouchDelegate = touchDelegate;
	}

	/**
	 * Register a drag event listener callback object for this View. The parameter is
	 * an implementation of {@link android.view.View.OnDragListener}.
	 */
	public void setOnDragListener(OnDragListener l) {
		getListenerInfo().mOnDragListener = l;
	}

	/**
	 * Handles drag events sent by the system following a call to startDragAndDrop() or, in ATL, a drag of files
	 * from the desktop. As AOSP: accepted by default if there is an OnReceiveContentListener, a drop goes to
	 * performReceiveContent().
	 */
	public boolean onDragEvent(DragEvent event) {
		if (mOnReceiveContentListener == null) {
			return false;
		}
		// Accept drag events by default if there's an OnReceiveContentListener set.
		if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
			return true;
		}
		if (event.getAction() == DragEvent.ACTION_DROP) {
			final DragAndDropPermissions permissions = DragAndDropPermissions.obtain(event);
			if (permissions != null) {
				permissions.takeTransient();
			}
			final ContentInfo payload = new ContentInfo.Builder(event.getClipData(), ContentInfo.SOURCE_DRAG_AND_DROP)
				.setDragAndDropPermissions(permissions)
				.build();
			ContentInfo remainingPayload = performReceiveContent(payload);
			// Return true unless none of the payload was consumed.
			return remainingPayload != payload;
		}
		return false;
	}

	/**
	 * Detects if this View is enabled and has a drag event listener.
	 * If both are true, then it calls the drag event listener with the
	 * {@link android.view.DragEvent} it received. If the drag event listener returns
	 * {@code true}, then dispatchDragEvent() returns {@code true}.
	 * <p>
	 * For all other cases, the method calls the
	 * {@link android.view.View#onDragEvent(DragEvent) onDragEvent()} drag event handler
	 * method and returns its result.
	 * </p>
	 */
	public boolean dispatchDragEvent(DragEvent event) {
		event.mEventHandlerWasCalled = true;
		if (event.mAction == DragEvent.ACTION_DRAG_LOCATION ||
		    event.mAction == DragEvent.ACTION_DROP) {
			// About to deliver an event with coordinates to this view. Notify that now this view
			// has drag focus. This will send exit/enter events as needed.
			setDragFocus(this, event);
		}
		return callDragEventHandler(event);
	}

	final boolean callDragEventHandler(DragEvent event) {
		final boolean result;

		ListenerInfo li = mListenerInfo;
		//noinspection SimplifiableIfStatement
		if (li != null && li.mOnDragListener != null && (mViewFlags & ENABLED_MASK) == ENABLED
		    && li.mOnDragListener.onDrag(this, event)) {
			result = true;
		} else {
			result = onDragEvent(event);
		}

		switch (event.mAction) {
			case DragEvent.ACTION_DRAG_ENTERED: {
				mPrivateFlags2 |= View.PFLAG2_DRAG_HOVERED;
				refreshDrawableState();
			} break;
			case DragEvent.ACTION_DRAG_EXITED: {
				mPrivateFlags2 &= ~View.PFLAG2_DRAG_HOVERED;
				refreshDrawableState();
			} break;
			case DragEvent.ACTION_DRAG_ENDED: {
				mPrivateFlags2 &= ~View.DRAG_MASK;
				refreshDrawableState();
			} break;
		}

		return result;
	}

	boolean canAcceptDrag() {
		return (mPrivateFlags2 & PFLAG2_DRAG_CAN_ACCEPT) != 0;
	}

	/* AOSP ViewRootImpl.mCurrentDragView and setDragFocus(): the view that gets the drag's location, with
	 * ACTION_DRAG_EXITED and ACTION_DRAG_ENTERED as it changes. ATL: one drag at a time, one drag focus */
	static View sCurrentDragView;

	static void setDragFocus(View newDragTarget, DragEvent event) {
		if (sCurrentDragView != newDragTarget) {
			// Send EXITED and ENTERED notifications to the old and new drag focus views.

			final float tx = event.mX;
			final float ty = event.mY;
			final int action = event.mAction;
			final android.content.ClipData td = event.mClipData;
			// Position should not be available for ACTION_DRAG_ENTERED and ACTION_DRAG_EXITED.
			event.mX = 0;
			event.mY = 0;
			event.mClipData = null;

			if (sCurrentDragView != null) {
				event.mAction = DragEvent.ACTION_DRAG_EXITED;
				sCurrentDragView.callDragEventHandler(event);
			}

			if (newDragTarget != null) {
				event.mAction = DragEvent.ACTION_DRAG_ENTERED;
				newDragTarget.callDragEventHandler(event);
			}

			event.mAction = action;
			event.mX = tx;
			event.mY = ty;
			event.mClipData = td;
		}

		sCurrentDragView = newDragTarget;
	}


	/**
	 * Get the animation currently associated with this view.
	 */
	public Animation getAnimation() {
		return currentAnimation;
	}

	ViewOverlay mOverlay;

	public ViewOverlay getOverlay() {
		if (mOverlay == null) {
			mOverlay = new ViewOverlay(this);
		}
		return mOverlay;
	}

	/* the wrapper widget calls draw() only for views that draw something themselves: an overlay is drawn by it */
	void overlayChanged() {
		native_drawThroughJava(widget);
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


	/* AOSP View.requestRectangleOnScreen(): the parents, from the closest, are asked to make the rectangle (in the
	 * view's coordinates) visible, e.g. by scrolling; true if any of them moved */
	public static final int RECTANGLE_ON_SCREEN_REQUEST_SOURCE_UNDEFINED = 0x00000000;
	public static final int RECTANGLE_ON_SCREEN_REQUEST_SOURCE_SCROLL_ONLY = 0x00000001;
	public static final int RECTANGLE_ON_SCREEN_REQUEST_SOURCE_TEXT_CURSOR = 0x00000002;
	public static final int RECTANGLE_ON_SCREEN_REQUEST_SOURCE_INPUT_FOCUS = 0x00000003;

	public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
		return requestRectangleOnScreen(rectangle, immediate, RECTANGLE_ON_SCREEN_REQUEST_SOURCE_UNDEFINED);
	}

	/* AOSP requestRectangleOnScreen(Rect, boolean, int): the source tells the parents what asked (ViewParent default delegates) */
	public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate, int source) {
		if (parent == null) {
			return false;
		}
		View child = this;
		RectF position = new RectF();
		position.set(rectangle);
		ViewParent p = parent;
		boolean scrolled = false;
		while (p != null) {
			rectangle.set((int)position.left, (int)position.top, (int)position.right, (int)position.bottom);
			scrolled |= p.requestChildRectangleOnScreen(child, rectangle, immediate, source);
			position.offset(child.getLeft(), child.getTop());
			if (!(p instanceof View)) {
				break;
			}
			View parentView = (View)p;
			position.offset(-parentView.getScrollX(), -parentView.getScrollY());
			child = parentView;
			p = child.getParent();
		}
		return scrolled;
	}

	public boolean requestRectangleOnScreen(Rect rectangle) {
		return requestRectangleOnScreen(rectangle, false);
	}

	/* AOSP View.onKeyDown(): Enter and the other confirm keys press a clickable view */
	@Export
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.isConfirmKey(keyCode) && event.hasNoModifiers()) {
			if (!isEnabled()) {
				return true;
			}

			if (event.getRepeatCount() == 0) {
				// Long clickable items don't necessarily have to be clickable.
				final boolean clickable = isClickable() || isLongClickable();
				if (clickable) {
					final float x = getWidth() / 2f;
					final float y = getHeight() / 2f;
					setPressed(true, x, y);
					checkForLongClick(ViewConfiguration.getLongPressTimeout(), x, y);
					return true;
				}
			}
		}

		return false;
	}

	/* AOSP View.dispatchKeyShortcutEvent()/onKeyShortcut() */
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		return onKeyShortcut(event.getKeyCode(), event);
	}

	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return false;
	}

	/* AOSP View.onKeyLongPress() */
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	/* AOSP View.onKeyUp(): the confirm key released on a pressed clickable view clicks it */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (KeyEvent.isConfirmKey(keyCode) && event.hasNoModifiers()) {
			if (!isEnabled()) {
				return true;
			}
			if (isClickable() && isPressed()) {
				setPressed(false);

				if (!mHasPerformedLongPress) {
					// This is a tap, so remove the longpress check
					removeLongPressCallback();
					if (!event.isCanceled()) {
						InputTrace.userTouch(this, InputTrace.CLICK);
						return performClick();
					}
				}
			}
		}
		return false;
	}

	/* AOSP View.onKeyMultiple() */
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return false;
	}

	/* like AOSP; ATL calls it only where a view drives a drop-down (AutoCompleteTextView) */
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		return false;
	}

	/* AOSP View.dispatchKeyEvent(), without the input consistency verifier; the dispatcher state (tracking for
	 * startTracking() and long presses) is the window's, see getKeyDispatcherState(). AOSP's ViewGroup.dispatchKeyEvent()
	 * hands the key down to mFocused; here GTK delivers it to the focused widget's key controller (WrapperWidget.c). */
	@Export
	public boolean dispatchKeyEvent(KeyEvent event) {
		// Give any attached key listener a first crack at the event.
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnKeyListener != null && isEnabled() && li.mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
			return true;
		}

		if (event.dispatch(this, getKeyDispatcherState(), this)) {
			return true;
		}

		return false;
	}

	public WindowInsets getRootWindowInsets() {
		Window window = native_get_window(widget);
		return window != null ? window.getWindowInsets() : null;
	}



	/* AOSP View.getApplicationWindowToken(): the token of the window the view is in; ATL has no panel windows
	 * that belong to the window of an application, so it is the window token itself (null while detached) */
	public IBinder getApplicationWindowToken() {
		return getWindowToken();
	}

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
		native_setVisibility(widget, getDrawnVisibility(), this.alpha * transitionAlpha, isEnabled());
	}

	/* API 29; sets the visibility without the side effects of setVisibility() */
	public void setTransitionVisibility(int visibility) {
		this.visibility = visibility;
		native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
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
			if (mPerformClick != null) {
				removeCallbacks(mPerformClick);
			}
			if (mPendingCheckForLongPress != null) {
				removeCallbacks(mPendingCheckForLongPress);
			}
			removeTapCallback();
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
			native_setVisibility(widget, getDrawnVisibility(), alpha * transitionAlpha, isEnabled());
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

	/* AOSP View.performAccessibilityAction(): the delegate, else the view, does what an accessibility service asks */
	public boolean performAccessibilityAction(int action, Bundle arguments) {
		if (mAccessibilityDelegate != null) {
			return mAccessibilityDelegate.performAccessibilityAction(this, action, arguments);
		} else {
			return performAccessibilityActionInternal(action, arguments);
		}
	}

	public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
		switch (action) {
		case AccessibilityNodeInfo.ACTION_CLICK:
			if (isClickable()) {
				performClick();
				return true;
			}
			break;
		case AccessibilityNodeInfo.ACTION_LONG_CLICK:
			if (isLongClickable()) {
				performLongClick();
				return true;
			}
			break;
		case AccessibilityNodeInfo.ACTION_FOCUS:
			if (!hasFocus()) {
				// Get out of touch mode since accessibility wants to move focus around.
				return requestFocus();
			}
			break;
		case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS:
			if (hasFocus()) {
				clearFocus();
				return !isFocused();
			}
			break;
		case AccessibilityNodeInfo.ACTION_SELECT:
			if (!isSelected()) {
				setSelected(true);
				return isSelected();
			}
			break;
		case AccessibilityNodeInfo.ACTION_CLEAR_SELECTION:
			if (isSelected()) {
				setSelected(false);
				return !isSelected();
			}
			break;
		}
		return false;
	}

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


	public void scrollBy(int x, int y) {
		scrollTo(scrollX + x, scrollY + y);
	}

	public void setAccessibilityPaneTitle(CharSequence paneTitle) { accessibilityPaneTitle = paneTitle; }

	public void setAccessibilityHeading(boolean heading) { accessibilityHeading = heading; }

	public WindowInsets computeSystemWindowInsets(WindowInsets insets, Rect contentInsets) { return insets; }

	public boolean isDuplicateParentStateEnabled() { return duplicate_parent_state; }

	public void setBackgroundTintMode(PorterDuff.Mode tintMode) {
		mBackgroundTintMode = tintMode;
		mHasBackgroundTintMode = true;
		applyBackgroundTint();
	}

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
		return visibility == VISIBLE && isFocusableResolved() && isEnabled() && getWidth() > 0 && getHeight() > 0;
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
		if ((isClickable() || isLongClickable()) && isEnabled())
			views.add(this);
	}

	/* AOSP View.focusSearch() */
	public View focusSearch(int direction) {
		if (parent instanceof ViewGroup)
			return ((ViewGroup)parent).focusSearch(this, direction);
		return null;
	}

	public void setHasTransientState(boolean hasTransientState) {
		final boolean oldHasTransientState = hasTransientState();
		mTransientStateCount = hasTransientState ? mTransientStateCount + 1 : mTransientStateCount - 1;
		if (mTransientStateCount < 0) {
			mTransientStateCount = 0;
			android.util.Log.e("View", "hasTransientState decremented below 0: unmatched pair of setHasTransientState calls");
		} else if ((hasTransientState && mTransientStateCount == 1) || (!hasTransientState && mTransientStateCount == 0)) {
			// update flag if we've just incremented up from 0 or decremented down to 0
			mHasTransientState = hasTransientState;
			final boolean newHasTransientState = hasTransientState();
			if (parent != null && newHasTransientState != oldHasTransientState) {
				parent.childHasTransientStateChanged(this, newHasTransientState);
			}
		}
	}

	protected void onConfigurationChanged(Configuration newConfig) {}



	/* AOSP View.setDrawingCacheQuality()/getDrawingCacheQuality() */
	public void setDrawingCacheQuality(int quality) {
		setFlags(quality, DRAWING_CACHE_QUALITY_MASK);
	}
	public int getDrawingCacheQuality() {
		return mViewFlags & DRAWING_CACHE_QUALITY_MASK;
	}

	/* AOSP View.buildDrawingCache()/getDrawingCache(): the view drawn into a bitmap of its size, as the software
	 * drawing cache (rebuilt on each getDrawingCache() while the cache is enabled) */
	private Bitmap mDrawingCache;

	public void buildDrawingCache(boolean autoScale) {
		int width = right - left;
		int height = bottom - top;
		if (width <= 0 || height <= 0) {
			destroyDrawingCache();
			return;
		}
		if (mDrawingCache == null || mDrawingCache.isRecycled() || mDrawingCache.getWidth() != width || mDrawingCache.getHeight() != height) {
			destroyDrawingCache();
			mDrawingCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} else {
			mDrawingCache.eraseColor(0);
		}
		Canvas canvas = new Canvas(mDrawingCache);
		canvas.translate(-getScrollX(), -getScrollY());
		draw(canvas);
	}

	public Bitmap getDrawingCache() {
		return getDrawingCache(false);
	}

	public Bitmap getDrawingCache(boolean autoScale) {
		if ((mViewFlags & WILL_NOT_CACHE_DRAWING) == WILL_NOT_CACHE_DRAWING)
			return null;
		if (isDrawingCacheEnabled())
			buildDrawingCache(autoScale);
		return mDrawingCache;
	}

	public void announceForAccessibility(CharSequence text) {}

	public WindowInsetsController getWindowInsetsController() {
		/* like AOSP: the controller of the window the view is attached to; before that the one of the
		 * nearest ancestor (the DecorView of a window answers with the window's controller) */
		Window window = native_get_window(widget);
		if (window != null)
			return window.getInsetsController();
		ViewParent parent = getParent();
		if (parent instanceof View)
			return ((View) parent).getWindowInsetsController();
		return null;
	}

	/* AOSP View.isKeyboardNavigationCluster() and setKeyboardNavigationCluster(): the root of a group of views
	 * that Meta+Tab moves the focus between (Window.performKeyboardGroupNavigation()) */
	public final boolean isKeyboardNavigationCluster() {
		return (mPrivateFlags3 & PFLAG3_CLUSTER) != 0;
	}

	public void setKeyboardNavigationCluster(boolean isCluster) {
		if (isCluster) {
			mPrivateFlags3 |= PFLAG3_CLUSTER;
		} else {
			mPrivateFlags3 &= ~PFLAG3_CLUSTER;
		}
	}

	/* AOSP View.restoreFocusInCluster() and restoreFocusNotInCluster() */
	public boolean restoreFocusInCluster(int direction) {
		// Prioritize focusableByDefault over algorithmic focus selection.
		if (restoreDefaultFocus()) {
			return true;
		}
		return requestFocus(direction);
	}

	public boolean restoreFocusNotInCluster() {
		return requestFocus(View.FOCUS_DOWN);
	}


	public AccessibilityDelegate getAccessibilityDelegate() {
		return mAccessibilityDelegate;
	}

	public void setScrollX(int value) {
		this.scrollX = value;
	}
	public void setScrollY(int value) {
		this.scrollY = value;
	}

	/* AOSP's View.dispatchHoverEvent() */
	@Export
	protected boolean dispatchHoverEvent(MotionEvent event) {
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnHoverListener != null
				&& isEnabled()
				&& li.mOnHoverListener.onHover(this, event)) {
			return true;
		}

		return onHoverEvent(event);
	}

	/* the hover controllers call this: whether the view took the hover (ViewGroup.dispatchHoverEvent() of its
	 * ancestors asks, see hasHoverHandledDescendant(); GTK hands the motion to the deepest view first) */
	boolean mHoverHandled;

	@Export
	boolean dispatchHoverEventFromNative(MotionEvent event) {
		boolean handled = dispatchHoverEvent(event);
		mHoverHandled = handled && event.getAction() != MotionEvent.ACTION_HOVER_EXIT;
		return handled;
	}

	/* AOSP's View.onHoverEvent() without the accessibility events, the touch delegate's touch exploration and
	 * the scroll bars (ATL draws none) */
	@Export
	public boolean onHoverEvent(MotionEvent event) {
		final int action = event.getActionMasked();
		// If we consider ourself hoverable, or if we we're already hovered,
		// handle changing state in response to ENTER and EXIT events.
		if (isHoverable() || isHovered()) {
			switch (action) {
				case MotionEvent.ACTION_HOVER_ENTER:
					setHovered(true);
					break;
				case MotionEvent.ACTION_HOVER_EXIT:
					setHovered(false);
					break;
			}

			// Dispatch the event to onGenericMotionEvent before returning true.
			// This is to provide compatibility with existing applications that
			// handled HOVER_MOVE events in onGenericMotionEvent and that would
			// break because onHoverEvent consumes all events here.
			dispatchGenericMotionEventInternal(event);
			// The event was already handled by calling setHovered(), so always
			// return true.
			return true;
		}

		return false;
	}

	/* AOSP's View.isHoverable() */
	private boolean isHoverable() {
		final int viewFlags = mViewFlags;
		if ((viewFlags & ENABLED_MASK) == DISABLED) {
			return false;
		}

		return (viewFlags & CLICKABLE) == CLICKABLE
		    || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE
		    || isContextClickable();
	}

	/* a hoverable view needs the hover events to be hovered: its GTK motion controller delivers them */
	private void updateHoverable() {
		if (widget != 0 && isHoverable())
			nativeSetOnHoverListener(widget);
	}

	/* AOSP View.startActionMode(): the parents are asked (startActionModeForChild) */
	public ActionMode startActionMode(ActionMode.Callback callback, int type) {
		ViewParent parent = getParent();
		if (parent == null)
			return null;
		return parent.startActionModeForChild(this, callback, type);
	}

	public InputConnection onCreateInputConnection(EditorInfo outAttrs) { return null; }

	/* AOSP View.checkInputConnectionProxy(): whether another view may talk to the input method for this one */
	public boolean checkInputConnectionProxy(View view) { return false; }

	public ActionMode startActionMode(ActionMode.Callback callback) {
		return startActionMode(callback, ActionMode.TYPE_PRIMARY);
	}

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
	/* AOSP View.setForceDarkAllowed()/isForceDarkAllowed() (RenderNode, allowed by default); ATL has no force dark,
	 * so it is only kept */
	private boolean mForceDarkAllowed = true;
	public void setForceDarkAllowed(boolean forceDark) {
		mForceDarkAllowed = forceDark;
	}
	public boolean isForceDarkAllowed() {
		return mForceDarkAllowed;
	}
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
	/* AOSP View.requestPointerCapture()/releasePointerCapture()/hasPointerCapture(): through ViewRootImpl to the
	 * input system; ATL has no pointer capture (no GTK pointer lock), so a request is never granted */
	public void requestPointerCapture() {}

	/* AOSP View.POINTER_CAPTURE_MODE_*: the capture modes of requestPointerCapture(int) */
	public static final int POINTER_CAPTURE_MODE_UNCAPTURED = 0;
	public static final int POINTER_CAPTURE_MODE_ABSOLUTE = 1;
	public static final int POINTER_CAPTURE_MODE_RELATIVE = 2;

	/* AOSP View.requestPointerCapture(int): as requestPointerCapture(), never granted in ATL */
	public void requestPointerCapture(int mode) {}

	public void releasePointerCapture() {}

	public boolean hasPointerCapture() {
		return false;
	}

	/* AOSP View.setAnimationMatrix()/getAnimationMatrix(): RenderNode keeps a copy, applied outside the view's own
	 * transform (RenderNodeDrawable::setViewProperties()); native_layout() hands it to GTK. As in AOSP, getMatrix()
	 * and so the touch mapping ignore it (GTK's picking does follow it) */
	private Matrix animationMatrix;

	public void setAnimationMatrix(Matrix matrix) {
		animationMatrix = (matrix != null) ? new Matrix(matrix) : null;
		if (parent instanceof View)
			((View)parent).native_queueAllocate(((View)parent).widget);
	}

	public Matrix getAnimationMatrix() {
		return (animationMatrix != null) ? new Matrix(animationMatrix) : null;
	}

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
	/* AOSP View.IMPORTANT_FOR_CONTENT_CAPTURE_* and isImportantForContentCapture()
	 * (calculateIsImportantForContentCapture(), without the cache) */
	public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0x0;
	public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 0x1;
	public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 0x2;
	public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 0x4;
	public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 0x8;

	public boolean isImportantForContentCapture() {
		ViewParent parent = getParent();
		while (parent instanceof View) {
			final int parentImportance = ((View)parent).getImportantForContentCapture();
			if (parentImportance == IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
			    || parentImportance == IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS)
				return false;
			parent = parent.getParent();
		}
		final int importance = getImportantForContentCapture();
		if (importance == IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS || importance == IMPORTANT_FOR_CONTENT_CAPTURE_YES)
			return true;
		if (importance != IMPORTANT_FOR_CONTENT_CAPTURE_AUTO)
			return false;
		// a view group is important if at least one child is
		if (this instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup)this;
			for (int i = 0; i < group.getChildCount(); i++) {
				if (group.getChildAt(i).isImportantForContentCapture())
					return true;
			}
		}
		// explicit autofill hints make it important
		return getAutofillHints() != null;
	}

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
	public void setRenderEffect(android.graphics.RenderEffect renderEffect) {}
	/* AOSP View.isShowingLayoutBounds(): DEBUG_DRAW || mAttachInfo.mDebugLayout, both only set by the developer
	 * option "Show layout bounds" (debug.layout), which ATL does not have */
	public boolean isShowingLayoutBounds() {
		return false;
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

	public void destroyDrawingCache() {
		if (mDrawingCache != null) {
			mDrawingCache.recycle();
			mDrawingCache = null;
		}
	}




	/* AOSP View.getFocusables(): the views a focus search in the direction could go to */
	public ArrayList<View> getFocusables(int direction) {
		ArrayList<View> result = new ArrayList<View>(24);
		addFocusables(result, direction);
		return result;
	}

	/* AOSP View.onApplyWindowInsets(), without the framework's optional fitting (PhoneWindow) */
	public WindowInsets onApplyWindowInsets(WindowInsets insets) {
		if ((mPrivateFlags3 & PFLAG3_FITTING_SYSTEM_WINDOWS) == 0) {
			// We weren't called from within a direct call to fitSystemWindows,
			// call into it as a fallback in case we're in a class that overrides it
			// and has logic to perform.
			if (fitSystemWindows(insets.getSystemWindowInsetsAsRect())) {
				return insets.consumeSystemWindowInsets();
			}
		} else {
			// We were called from within a direct call to fitSystemWindows.
			if (fitSystemWindowsInt(insets.getSystemWindowInsetsAsRect())) {
				return insets.consumeSystemWindowInsets();
			}
		}
		return insets;
	}

	public boolean isAccessibilityFocused() { return false; }


	public OnFocusChangeListener getOnFocusChangeListener() { return onFocusChangeListener; }


	public TouchDelegate getTouchDelegate() {
		return mTouchDelegate;
	}

	/* AOSP View.setZ() */
	public void setZ(float z) {
		setTranslationZ(z - getElevation());
	}

	/* ---- AOSP View, accessibility. ATL has no accessibility service, so nothing asks for these, but an app's
	 * views and delegates call and override them and expect what AOSP returns and calls. The events and nodes
	 * are built as in AOSP; AccessibilityManager.isEnabled() is false, so sendAccessibilityEvent() sends none. ---- */

	public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;
	public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;
	public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;
	public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 0x00000004;
	public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0x00000000;
	public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 0x00000001;
	public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 0x00000002;
	public static final int ACCESSIBILITY_DATA_SENSITIVE_AUTO = 0x00000000;
	public static final int ACCESSIBILITY_DATA_SENSITIVE_YES = 0x00000001;
	public static final int ACCESSIBILITY_DATA_SENSITIVE_NO = 0x00000002;

	/** AOSP: ENABLED_FOCUSED_STATE_SET and the others of the combinations of the state sets */
	protected static final int[] ENABLED_FOCUSED_STATE_SET = new int[] {R.attr.state_enabled, R.attr.state_focused};

	private int mAccessibilityViewId = NO_ID;
	private int mLabelForId = NO_ID;
	private int mAccessibilityTraversalBeforeId = NO_ID;
	private int mAccessibilityTraversalAfterId = NO_ID;
	private int mAccessibilityDataSensitive = ACCESSIBILITY_DATA_SENSITIVE_AUTO;
	private int mAccessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_NONE;
	private static int sNextAccessibilityViewId;

	/* AOSP View.getAccessibilityViewId(): the id the node ids of an accessibility service are made of, given out
	 * once per view */
	public int getAccessibilityViewId() {
		if (mAccessibilityViewId == NO_ID) {
			mAccessibilityViewId = sNextAccessibilityViewId++;
		}
		return mAccessibilityViewId;
	}

	/* AOSP: the id of the window in the accessibility service's view of the windows; ATL has one window per
	 * root view and UNDEFINED_WINDOW_ID (-1) for a view that is in none */
	public int getAccessibilityWindowId() {
		return isAttachedToWindow() ? System.identityHashCode(getRootView()) & 0x7fffffff : -1;
	}

	public CharSequence getAccessibilityClassName() {
		return View.class.getName();
	}

	public int getLabelFor() {
		return mLabelForId;
	}

	public void setLabelFor(int id) {
		if (mLabelForId == id) {
			return;
		}
		mLabelForId = id;
		if (mLabelForId != View.NO_ID && this.id == NO_ID) {
			this.id = generateViewId();
		}
	}

	public int getAccessibilityTraversalBefore() {
		return mAccessibilityTraversalBeforeId;
	}

	public void setAccessibilityTraversalBefore(int beforeId) {
		if (mAccessibilityTraversalBeforeId == beforeId) {
			return;
		}
		mAccessibilityTraversalBeforeId = beforeId;
	}

	public int getAccessibilityTraversalAfter() {
		return mAccessibilityTraversalAfterId;
	}

	public void setAccessibilityTraversalAfter(int afterId) {
		if (mAccessibilityTraversalAfterId == afterId) {
			return;
		}
		mAccessibilityTraversalAfterId = afterId;
	}

	public int getAccessibilityLiveRegion() {
		return mAccessibilityLiveRegion;
	}

	public void setAccessibilityLiveRegion(int mode) {
		if (mode != mAccessibilityLiveRegion) {
			mAccessibilityLiveRegion = mode;
		}
	}

	/* AOSP View.setAccessibilityDataSensitive(): ACCESSIBILITY_DATA_SENSITIVE_* ; an auto view is sensitive
	 * when an ancestor is, isAccessibilityDataSensitive() answers for the view itself */
	public void setAccessibilityDataSensitive(int mode) {
		mAccessibilityDataSensitive = mode;
	}

	public boolean isAccessibilityDataSensitive() {
		if (mAccessibilityDataSensitive == ACCESSIBILITY_DATA_SENSITIVE_YES) {
			return true;
		}
		if (mAccessibilityDataSensitive == ACCESSIBILITY_DATA_SENSITIVE_NO) {
			return false;
		}
		ViewParent p = parent;
		while (p instanceof View) {
			View v = (View)p;
			if (v.mAccessibilityDataSensitive != ACCESSIBILITY_DATA_SENSITIVE_AUTO) {
				return v.mAccessibilityDataSensitive == ACCESSIBILITY_DATA_SENSITIVE_YES;
			}
			p = v.parent;
		}
		return false;
	}

	/* AOSP: the view takes part in the accessibility tree of an accessibility service */
	public boolean isImportantForAccessibility() {
		final int mode = getImportantForAccessibility();
		if (mode == IMPORTANT_FOR_ACCESSIBILITY_NO || mode == IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
			return false;
		}
		// Check parent mode to ensure we're not hidden.
		ViewParent p = parent;
		while (p instanceof View) {
			if (((View)p).getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
				return false;
			}
			p = p.getParent();
		}
		return mode == IMPORTANT_FOR_ACCESSIBILITY_YES || isActionableForAccessibility() || hasListenersForAccessibility()
		    || getAccessibilityNodeProvider() != null || getAccessibilityLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE
		    || isAccessibilityPane();
	}

	boolean isAccessibilityPane() {
		return getAccessibilityPaneTitle() != null;
	}

	/* AOSP View.isActionableForAccessibility() */
	public boolean isActionableForAccessibility() {
		return (isClickable() || isLongClickable() || isFocusable());
	}

	private boolean hasListenersForAccessibility() {
		ListenerInfo li = mListenerInfo;
		return mTouchDelegate != null || on_touch_listener != null || (li != null && (li.mOnKeyListener != null || li.mOnGenericMotionListener != null || li.mOnHoverListener != null || li.mOnDragListener != null));
	}

	/* AOSP View.includeForAccessibility(): without an accessibility service that asks for the unimportant
	 * views as well, a view is in the tree if it is important */
	public boolean includeForAccessibility() {
		return isAttachedToWindow() && isImportantForAccessibility();
	}

	/* AOSP View.getParentForAccessibility(): the closest ancestor that is in the accessibility tree */
	public ViewParent getParentForAccessibility() {
		if (parent instanceof View) {
			View parentView = (View)parent;
			if (parentView.includeForAccessibility()) {
				return parent;
			} else {
				return parentView.getParentForAccessibility();
			}
		}
		return null;
	}

	/* AOSP View.isVisibleToUser(): attached, shown, not transparent and with a part in the visible rectangle */
	public boolean isVisibleToUser() {
		return isVisibleToUser(null);
	}

	protected boolean isVisibleToUser(Rect boundInView) {
		if (!isAttachedToWindow() || getWindowVisibility() != VISIBLE) {
			return false;
		}
		View current = this;
		while (current != null) {
			if (current.getAlpha() <= 0 || current.getTransitionAlpha() <= 0 || current.getVisibility() != VISIBLE) {
				return false;
			}
			current = current.parent instanceof View ? (View)current.parent : null;
		}
		Rect visibleRect = new Rect();
		if (!getGlobalVisibleRect(visibleRect)) {
			return false;
		}
		if (boundInView != null) {
			boundInView.set(visibleRect);
		}
		return true;
	}

	/* AOSP View.addChildrenForAccessibility(): a view has no children to add */
	public void addChildrenForAccessibility(ArrayList<View> outChildren) {}

	public AccessibilityNodeProvider getAccessibilityNodeProvider() {
		if (mAccessibilityDelegate != null) {
			return mAccessibilityDelegate.getAccessibilityNodeProvider(this);
		}
		return null;
	}

	/* AOSP View.sendAccessibilityEvent(): the delegate, else the view itself, builds the event and sends it if
	 * an accessibility service asks for events */
	public void sendAccessibilityEvent(int eventType) {
		if (mAccessibilityDelegate != null) {
			mAccessibilityDelegate.sendAccessibilityEvent(this, eventType);
		} else {
			sendAccessibilityEventInternal(eventType);
		}
	}

	public void sendAccessibilityEventInternal(int eventType) {
		if (AccessibilityManager.getInstance(context).isEnabled()) {
			sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(eventType));
		}
	}

	public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
		if (mAccessibilityDelegate != null) {
			mAccessibilityDelegate.sendAccessibilityEventUnchecked(this, event);
		} else {
			sendAccessibilityEventUncheckedInternal(event);
		}
	}

	public void sendAccessibilityEventUncheckedInternal(AccessibilityEvent event) {
		// Panes disappearing are relevant even if though the view is no longer visible.
		boolean isWindowStateChanged = (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
		boolean isWindowDisappearedEvent = isWindowStateChanged && ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED) != 0);
		if (!isShown() && !isWindowDisappearedEvent) {
			return;
		}
		onInitializeAccessibilityEvent(event);
		// Only a subset of accessibility events populates text content.
		if ((event.getEventType() & POPULATING_ACCESSIBILITY_EVENT_TYPES) != 0) {
			dispatchPopulateAccessibilityEvent(event);
		}
		// In the beginning we called #isShown(), so we know that getParent() is not null.
		ViewParent parent = getParent();
		if (parent != null) {
			parent.requestSendAccessibilityEvent(this, event);
		}
	}

	private static final int POPULATING_ACCESSIBILITY_EVENT_TYPES = AccessibilityEvent.TYPE_VIEW_CLICKED | AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
	    | AccessibilityEvent.TYPE_VIEW_SELECTED | AccessibilityEvent.TYPE_VIEW_FOCUSED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
	    | AccessibilityEvent.TYPE_VIEW_HOVER_ENTER | AccessibilityEvent.TYPE_VIEW_HOVER_EXIT | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
	    | AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
	    | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED | AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED
	    | AccessibilityEvent.TYPE_ANNOUNCEMENT;

	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		boolean handled = false;
		if (mAccessibilityDelegate != null) {
			handled = mAccessibilityDelegate.dispatchPopulateAccessibilityEvent(this, event);
		} else {
			handled = dispatchPopulateAccessibilityEventInternal(event);
		}
		return handled;
	}

	public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
		onPopulateAccessibilityEvent(event);
		return false;
	}

	public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
		if (mAccessibilityDelegate != null) {
			mAccessibilityDelegate.onPopulateAccessibilityEvent(this, event);
		} else {
			onPopulateAccessibilityEventInternal(event);
		}
	}

	public void onPopulateAccessibilityEventInternal(AccessibilityEvent event) {
		if ((event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) && !TextUtils.isEmpty(getAccessibilityPaneTitle())) {
			event.getText().add(getAccessibilityPaneTitle());
		}
	}

	public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
		if (mAccessibilityDelegate != null) {
			mAccessibilityDelegate.onInitializeAccessibilityEvent(this, event);
		} else {
			onInitializeAccessibilityEventInternal(event);
		}
	}

	public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
		event.setSource(this);
		event.setClassName(getAccessibilityClassName());
		event.setPackageName(getContext().getPackageName());
		event.setEnabled(isEnabled());
		event.setContentDescription(getContentDescription());
		if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
			ArrayList<View> focusablesTempList = new ArrayList<View>();
			getRootView().addFocusables(focusablesTempList, View.FOCUS_FORWARD, FOCUSABLES_ALL);
			event.setItemCount(focusablesTempList.size());
			event.setCurrentItemIndex(focusablesTempList.indexOf(this));
		}
	}

	public AccessibilityNodeInfo createAccessibilityNodeInfo() {
		if (mAccessibilityDelegate != null) {
			return mAccessibilityDelegate.createAccessibilityNodeInfo(this);
		} else {
			return createAccessibilityNodeInfoInternal();
		}
	}

	public AccessibilityNodeInfo createAccessibilityNodeInfoInternal() {
		AccessibilityNodeProvider provider = getAccessibilityNodeProvider();
		if (provider != null) {
			return provider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID);
		} else {
			AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(this);
			onInitializeAccessibilityNodeInfo(info);
			return info;
		}
	}

	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		if (mAccessibilityDelegate != null) {
			mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(this, info);
		} else {
			onInitializeAccessibilityNodeInfoInternal(info);
		}
	}

	public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
		if (!isAttachedToWindow()) {
			return;
		}
		Rect bounds = new Rect();
		getDrawingRect(bounds);
		info.setBoundsInParent(bounds);
		int[] location = new int[2];
		getLocationOnScreen(location);
		bounds.offsetTo(location[0], location[1]);
		info.setBoundsInScreen(bounds);
		ViewParent parentForAccessibility = getParentForAccessibility();
		if (parentForAccessibility instanceof View) {
			info.setParent((View)parentForAccessibility);
		}
		info.setVisibleToUser(isVisibleToUser());
		info.setImportantForAccessibility(isImportantForAccessibility());
		info.setAccessibilityDataSensitive(isAccessibilityDataSensitive());
		info.setPackageName(getContext().getPackageName());
		info.setClassName(getAccessibilityClassName());
		info.setContentDescription(getContentDescription());
		info.setEnabled(isEnabled());
		info.setClickable(isClickable());
		info.setFocusable(isFocusable());
		info.setScreenReaderFocusable(isScreenReaderFocusable());
		info.setFocused(isFocused());
		info.setAccessibilityFocused(isAccessibilityFocused());
		info.setSelected(isSelected());
		info.setLongClickable(isLongClickable());
		info.setContextClickable(isContextClickable());
		info.setHeading(isAccessibilityHeading());
	}


	/* ---- AOSP View members that apps call and override and that ATL had no counterpart for. Where the state
	 * only matters to AOSP's own renderer or to a system service that ATL does not have, the state is kept and
	 * returned, as AOSP does; what stays without effect is said at the member. ---- */

	public static final int FIND_VIEWS_WITH_TEXT = 0x00000001;
	public static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 0x00000002;
	public static final int SCROLL_INDICATOR_TOP = 0x1;
	public static final int SCROLL_INDICATOR_BOTTOM = 0x2;
	public static final int SCROLL_INDICATOR_LEFT = 0x4;
	public static final int SCROLL_INDICATOR_RIGHT = 0x8;
	public static final int SCROLL_INDICATOR_START = 0x10;
	public static final int SCROLL_INDICATOR_END = 0x20;
	public static final int CONTENT_SENSITIVITY_AUTO = 0x0;
	public static final int CONTENT_SENSITIVITY_SENSITIVE = 0x1;
	public static final int CONTENT_SENSITIVITY_NOT_SENSITIVE = 0x2;

	/* AOSP View.findViewsWithText(): the views whose content description (or, for a TextView, text) holds the
	 * searched text, ignoring case */
	public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
		if ((flags & FIND_VIEWS_WITH_CONTENT_DESCRIPTION) != 0 && !TextUtils.isEmpty(searched) && !TextUtils.isEmpty(getContentDescription())) {
			String searchedLowerCase = searched.toString().toLowerCase();
			String contentDescriptionLowerCase = getContentDescription().toString().toLowerCase();
			if (contentDescriptionLowerCase.contains(searchedLowerCase)) {
				outViews.add(this);
			}
		}
	}

	/* AOSP View.getLocalVisibleRect(): the visible part of the view in its own coordinates */
	public final boolean getLocalVisibleRect(Rect r) {
		final Point offset = new Point();
		if (getGlobalVisibleRect(r, offset)) {
			r.offset(-offset.x, -offset.y); // make r local
			return true;
		}
		return false;
	}

	/* AOSP View.getFocusable(): FOCUSABLE_AUTO, FOCUSABLE or NOT_FOCUSABLE as set */
	public int getFocusable() {
		return (mViewFlags & FOCUSABLE_AUTO) > 0 ? FOCUSABLE_AUTO : mViewFlags & FOCUSABLE;
	}

	private boolean mFocusedByDefault;

	public boolean isFocusedByDefault() {
		return mFocusedByDefault;
	}

	public void setFocusedByDefault(boolean isFocusedByDefault) {
		mFocusedByDefault = isFocusedByDefault;
	}

	/* AOSP: the scroll bars and the fading edges are drawn by the view's own renderer. A GTK scrolled view draws
	 * its own, so these keep what the app set and the view draws neither; awakenScrollBars() reports that no
	 * scroll bar was shown (AOSP: false when the view has none to show). */
	private boolean mHorizontalScrollBarEnabled;
	private boolean mVerticalScrollBarEnabled;
	private boolean mHorizontalFadingEdgeEnabled;
	private boolean mVerticalFadingEdgeEnabled;
	private int mFadingEdgeLength = -1;

	public boolean isHorizontalScrollBarEnabled() {
		return mHorizontalScrollBarEnabled;
	}

	public void setHorizontalScrollBarEnabled(boolean enabled) {
		mHorizontalScrollBarEnabled = enabled;
	}

	public boolean isVerticalScrollBarEnabled() {
		return mVerticalScrollBarEnabled;
	}

	public void setVerticalScrollBarEnabled(boolean enabled) {
		mVerticalScrollBarEnabled = enabled;
	}

	public boolean isHorizontalFadingEdgeEnabled() {
		return mHorizontalFadingEdgeEnabled;
	}

	public void setHorizontalFadingEdgeEnabled(boolean enabled) {
		mHorizontalFadingEdgeEnabled = enabled;
	}

	public boolean isVerticalFadingEdgeEnabled() {
		return mVerticalFadingEdgeEnabled;
	}

	public void setVerticalFadingEdgeEnabled(boolean enabled) {
		mVerticalFadingEdgeEnabled = enabled;
	}

	public void setFadingEdgeLength(int length) {
		mFadingEdgeLength = length;
	}

	private int fadingEdgeLength() {
		return mFadingEdgeLength >= 0 ? mFadingEdgeLength : ViewConfiguration.get(context).getScaledFadingEdgeLength();
	}

	public int getVerticalFadingEdgeLength() {
		return isVerticalFadingEdgeEnabled() ? fadingEdgeLength() : 0;
	}

	public int getHorizontalFadingEdgeLength() {
		return isHorizontalFadingEdgeEnabled() ? fadingEdgeLength() : 0;
	}

	protected float getTopFadingEdgeStrength() {
		return computeVerticalScrollOffset() > 0 ? 1.0f : 0.0f;
	}

	protected float getBottomFadingEdgeStrength() {
		return computeVerticalScrollOffset() + computeVerticalScrollExtent() < computeVerticalScrollRange() ? 1.0f : 0.0f;
	}

	protected float getLeftFadingEdgeStrength() {
		return computeHorizontalScrollOffset() > 0 ? 1.0f : 0.0f;
	}

	protected float getRightFadingEdgeStrength() {
		return computeHorizontalScrollOffset() + computeHorizontalScrollExtent() < computeHorizontalScrollRange() ? 1.0f : 0.0f;
	}

	protected boolean awakenScrollBars(int startDelay) {
		return false;
	}

	protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
		return false;
	}

	/* AOSP View.getSolidColor(): the color a view with a fading edge fades to; 0, no color, by default */
	public int getSolidColor() {
		return 0;
	}

	private int mScrollIndicators;

	public int getScrollIndicators() {
		return mScrollIndicators;
	}

	public void setScrollIndicators(int indicators) {
		setScrollIndicators(indicators, SCROLL_INDICATOR_TOP | SCROLL_INDICATOR_BOTTOM | SCROLL_INDICATOR_LEFT | SCROLL_INDICATOR_RIGHT | SCROLL_INDICATOR_START | SCROLL_INDICATOR_END);
	}

	public void setScrollIndicators(int indicators, int mask) {
		mask &= SCROLL_INDICATOR_TOP | SCROLL_INDICATOR_BOTTOM | SCROLL_INDICATOR_LEFT | SCROLL_INDICATOR_RIGHT | SCROLL_INDICATOR_START | SCROLL_INDICATOR_END;
		mScrollIndicators = (mScrollIndicators & ~mask) | (indicators & mask);
	}

	private List<Rect> mPreferKeepClearRects;

	/* AOSP View.setPreferKeepClearRects(): the areas the system should keep clear of other windows' overlays;
	 * the desktop has no such overlays, the list is only kept */
	public void setPreferKeepClearRects(List<Rect> rects) {
		mPreferKeepClearRects = (rects == null || rects.isEmpty()) ? null : new ArrayList<Rect>(rects);
	}

	public List<Rect> getPreferKeepClearRects() {
		return mPreferKeepClearRects == null ? Collections.<Rect>emptyList() : new ArrayList<Rect>(mPreferKeepClearRects);
	}

	private boolean mFilterTouchesWhenObscured;

	/* AOSP: touches are dropped while another window covers the view; on a desktop GTK delivers them to the
	 * window under the pointer, so the flag is kept (getFilterTouchesWhenObscured()) and not acted on */
	public void setFilterTouchesWhenObscured(boolean enabled) {
		mFilterTouchesWhenObscured = enabled;
	}

	public boolean getFilterTouchesWhenObscured() {
		return mFilterTouchesWhenObscured;
	}

	private int mContentSensitivity = CONTENT_SENSITIVITY_AUTO;

	public final void setContentSensitivity(int mode) {
		mContentSensitivity = mode;
	}

	public final int getContentSensitivity() {
		return mContentSensitivity;
	}

	/* AOSP View.isContentSensitive(): auto is sensitive when an ancestor is */
	public final boolean isContentSensitive() {
		if (mContentSensitivity != CONTENT_SENSITIVITY_AUTO) {
			return mContentSensitivity == CONTENT_SENSITIVITY_SENSITIVE;
		}
		ViewParent p = parent;
		while (p instanceof View) {
			View v = (View)p;
			if (v.mContentSensitivity != CONTENT_SENSITIVITY_AUTO) {
				return v.mContentSensitivity == CONTENT_SENSITIVITY_SENSITIVE;
			}
			p = v.parent;
		}
		return false;
	}

	private float mFrameContentVelocity = 0;
	private float mRequestedFrameRate = 0;

	/* AOSP View.setFrameContentVelocity() and setRequestedFrameRate(): hints to the display's frame rate chooser.
	 * GTK paints at the monitor's rate, the hints are kept and returned */
	public void setFrameContentVelocity(float pixelsPerSecond) {
		mFrameContentVelocity = pixelsPerSecond;
	}

	public float getFrameContentVelocity() {
		return mFrameContentVelocity;
	}

	public void setRequestedFrameRate(float frameRate) {
		mRequestedFrameRate = frameRate;
	}

	public float getRequestedFrameRate() {
		return mRequestedFrameRate;
	}

	public void dispatchStartTemporaryDetach() {
		onStartTemporaryDetach();
	}

	public void dispatchFinishTemporaryDetach() {
		onFinishTemporaryDetach();
	}

	/* AOSP View.onStartTemporaryDetach(): the view is cleaned up for the time it is detached (a list view
	 * recycling it): its pending unpress goes (ATL has no PFLAG_CANCEL_NEXT_UP_EVENT) */
	public void onStartTemporaryDetach() {
		removeUnsetPressCallback();
	}

	public void onFinishTemporaryDetach() {
	}

	public void dispatchConfigurationChanged(Configuration newConfig) {
		onConfigurationChanged(newConfig);
	}

	public void onCancelPendingInputEvents() {
		cancelLongPress();
	}

	public void dispatchCancelPendingInputEvents() {
		onCancelPendingInputEvents();
	}

	/* AOSP View.onTrackballEvent() and dispatchTrackballEvent(): no trackball here, so the events never come, but
	 * a view that is sent one lets onTrackballEvent() decide */
	public boolean onTrackballEvent(MotionEvent event) {
		return false;
	}

	public boolean dispatchTrackballEvent(MotionEvent event) {
		return onTrackballEvent(event);
	}

	public boolean dispatchKeyEventPreIme(KeyEvent event) {
		return onKeyPreIme(event.getKeyCode(), event);
	}

	public boolean dispatchUnhandledMove(View focused, int direction) {
		return false;
	}

	/* AOSP View.fitSystemWindows(): the deprecated way to take the window insets; the view that fits system
	 * windows pads itself by them */
	@Deprecated
	protected boolean fitSystemWindows(Rect insets) {
		if ((mPrivateFlags3 & PFLAG3_APPLYING_INSETS) == 0) {
			if (insets == null) {
				// Null insets by definition have already been consumed.
				// This call cannot apply insets since there are none to apply,
				// so return false.
				return false;
			}
			// If we're not in the process of dispatching the newer apply insets call,
			// that means we're not in the compatibility path. Dispatch into the newer
			// apply insets path and take things from there.
			try {
				mPrivateFlags3 |= PFLAG3_FITTING_SYSTEM_WINDOWS;
				return dispatchApplyWindowInsets(new WindowInsets(insets)).isConsumed();
			} finally {
				mPrivateFlags3 &= ~PFLAG3_FITTING_SYSTEM_WINDOWS;
			}
		} else {
			// We're being called from the newer apply insets path.
			// Perform the standard fallback behavior.
			return fitSystemWindowsInt(insets);
		}
	}

	/* AOSP View.fitSystemWindowsInt() and computeSystemWindowInsets() (not optional): the view takes all of
	 * the system window insets as its padding (applyInsets()) */
	private boolean fitSystemWindowsInt(Rect insets) {
		if ((mViewFlags & FITS_SYSTEM_WINDOWS) == FITS_SYSTEM_WINDOWS) {
			setPadding(insets.left, insets.top, insets.right, insets.bottom);
			return new WindowInsets(insets).consumeSystemWindowInsets().isSystemWindowInsetsConsumed();
		}
		return false;
	}

	/* AOSP View.onWindowVisibilityChanged(): a view is told when the visibility of its window changes; ATL's window
	 * is visible while the view is attached */
	protected void onWindowVisibilityChanged(int visibility) {
	}

	public void dispatchWindowVisibilityChanged(int visibility) {
		onWindowVisibilityChanged(visibility);
	}

	/* AOSP View.onVisibilityAggregated(): the background, default focus highlight and foreground drawables follow
	 * the visibility (animated drawables stop while hidden); no scroll bars, autofill or accessibility panes here */
	public void onVisibilityAggregated(boolean isVisible) {
		final Drawable dr = background;
		if (dr != null && isVisible != dr.isVisible()) {
			dr.setVisible(isVisible, false);
		}
		final Drawable hl = mDefaultFocusHighlight;
		if (hl != null && isVisible != hl.isVisible()) {
			hl.setVisible(isVisible, false);
		}
		final Drawable fg = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
		if (fg != null && isVisible != fg.isVisible()) {
			fg.setVisible(isVisible, false);
		}
	}

	/* AOSP: tells onVisibilityAggregated() whether the view and all its ancestors are visible */
	boolean dispatchVisibilityAggregated(boolean isVisible) {
		final boolean thisVisible = getVisibility() == VISIBLE;
		// If we're not visible but something is telling us we are, ignore it.
		if (thisVisible || !isVisible) {
			onVisibilityAggregated(isVisible);
		}
		return thisVisible && isVisible;
	}

	public void onWindowSystemUiVisibilityChanged(int visible) {
	}

	public void dispatchWindowSystemUiVisiblityChanged(int visible) {
		onWindowSystemUiVisibilityChanged(visible);
	}

	/* AOSP View.onSetAlpha(): a subclass that draws with the alpha itself returns true */
	protected boolean onSetAlpha(int alpha) {
		return false;
	}

	/* AOSP View.onResolvePointerIcon(): the icon for the pointer over the view */
	public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
		return getPointerIcon();
	}

	private PointerIcon mPointerIcon;

	public PointerIcon getPointerIcon() {
		return mPointerIcon;
	}

	public void setPointerIcon(PointerIcon pointerIcon) {
		mPointerIcon = pointerIcon;
	}

	public View keyboardNavigationClusterSearch(View currentCluster, int direction) {
		if (isKeyboardNavigationCluster()) {
			currentCluster = this;
		}
		if (parent instanceof View) {
			return ((View)parent).keyboardNavigationClusterSearch(currentCluster, direction);
		}
		/* the root of the tree, as ViewRootImpl.keyboardNavigationClusterSearch(): the next cluster of the tree */
		return FocusFinder.getInstance().findNextKeyboardNavigationCluster(this, currentCluster, direction);
	}

	private int mLayerType;
	private Paint mLayerPaint;

	/* AOSP View.setLayerType(): the hardware layer of the view; a GTK widget renders into its own surface
	 * already, the type and the paint are kept and returned */
	public void setLayerType(int layerType, Paint paint) {
		if (layerType < 0 || layerType > 2) {
			throw new IllegalArgumentException("Layer type can only be one of: LAYER_TYPE_NONE, "
							   + "LAYER_TYPE_SOFTWARE or LAYER_TYPE_HARDWARE");
		}
		mLayerType = layerType;
		mLayerPaint = paint;
	}

	public void setLayerPaint(Paint paint) {
		mLayerPaint = paint;
	}

	public int getLayerType() {
		return mLayerType;
	}

	public void buildLayer() {
		if (mLayerType == 0) {
			return;
		}
		if (!isAttachedToWindow()) {
			throw new IllegalStateException("This view must be attached to a window first");
		}
	}

	private boolean mDrawingCacheEnabled;

	public boolean isDrawingCacheEnabled() {
		return mDrawingCacheEnabled;
	}

	public void setDrawingCacheEnabled(boolean enabled) {
		if (mDrawingCacheEnabled && !enabled)
			destroyDrawingCache(); // as AOSP's setFlags() when DRAWING_CACHE_ENABLED goes
		mDrawingCacheEnabled = enabled;
	}

	public void buildDrawingCache() {
		buildDrawingCache(false);
	}

	private boolean mWillNotDraw;

	public void setWillNotDraw(boolean willNotDraw) {
		mWillNotDraw = willNotDraw;
	}

	public boolean willNotDraw() {
		return mWillNotDraw;
	}

	/* AOSP View.setWillNotCacheDrawing()/willNotCacheDrawing() (WILL_NOT_CACHE_DRAWING): only a hint for the
	 * drawing cache, which ATL does not keep, so it is just remembered */
	private boolean mWillNotCacheDrawing;

	public void setWillNotCacheDrawing(boolean willNotCacheDrawing) {
		mWillNotCacheDrawing = willNotCacheDrawing;
	}

	public boolean willNotCacheDrawing() {
		return mWillNotCacheDrawing;
	}

	private String mTransitionName;

	public String getTransitionName() {
		return mTransitionName;
	}

	public void setTransitionName(String transitionName) {
		mTransitionName = transitionName;
	}

	private int mTextDirection = 0; // TEXT_DIRECTION_INHERIT

	public void setTextDirection(int textDirection) {
		mTextDirection = textDirection;
	}

	/* AOSP View.getTextDirection(): the resolved direction (TEXT_DIRECTION_INHERIT is resolved through the parents and
	 * the default is first strong, in an LTR or RTL locale) */
	public int getTextDirection() {
		int direction = mTextDirection;
		ViewParent p = parent;
		while (direction == 0 && p instanceof View) {
			direction = ((View)p).mTextDirection;
			p = ((View)p).parent;
		}
		return direction == 0 ? 1 /* TEXT_DIRECTION_FIRST_STRONG */ : direction;
	}

	/* RenderNode's camera: AOSP keeps the distance as the camera's z location in inches (Sk3DView), -8 by default */
	private float cameraLocationZ = -8;
	private Camera camera;
	private Matrix transform3D;

	public void setCameraDistance(float distance) {
		final float dpi = getResources().getDisplayMetrics().densityDpi;
		float z = -Math.abs(distance) / dpi;
		if (cameraLocationZ == z)
			return;
		cameraLocationZ = z;
		if (has3dRotation())
			transformChanged();
	}

	public float getCameraDistance() {
		final float dpi = getResources().getDisplayMetrics().densityDpi;
		return -(cameraLocationZ * dpi);
	}

	/* AOSP's View.isHovered(), setHovered() and onHoverChanged(): the hovered drawable state */
	public boolean isHovered() {
		return (mPrivateFlags & PFLAG_HOVERED) != 0;
	}

	public void setHovered(boolean hovered) {
		if (hovered) {
			if ((mPrivateFlags & PFLAG_HOVERED) == 0) {
				mPrivateFlags |= PFLAG_HOVERED;
				refreshDrawableState();
				onHoverChanged(true);
			}
		} else {
			if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
				mPrivateFlags &= ~PFLAG_HOVERED;
				refreshDrawableState();
				onHoverChanged(false);
			}
		}
	}

	public void onHoverChanged(boolean hovered) {
	}

	/* AOSP View.postInvalidateDelayed() and postInvalidateOnAnimation(): invalidate later / at the next frame */
	public void postInvalidateDelayed(long delayMilliseconds) {
		postDelayed(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		}, delayMilliseconds);
	}

	public void postInvalidateOnAnimation(int left, int top, int right, int bottom) {
		postInvalidate(left, top, right, bottom);
	}

	/* AOSP View.postInvalidateDelayed(long, int, int, int, int): invalidate the rect after the delay */
	public void postInvalidateDelayed(long delayMilliseconds, final int left, final int top, final int right, final int bottom) {
		postDelayed(new Runnable() {
			@Override
			public void run() {
				invalidate(left, top, right, bottom);
			}
		}, delayMilliseconds);
	}

	/* AOSP View.requestUnbufferedDispatch(): asks ViewRootImpl not to batch input to the frame.
	 * ATL dispatches each input event as it arrives, so there is nothing to switch off. */
	public final void requestUnbufferedDispatch(MotionEvent event) {
	}

	public final void requestUnbufferedDispatch(int source) {
	}

	/* AOSP View: PFLAG4_AUTO_HANDWRITING_ENABLED, set in the constructor. Only the flag is kept;
	 * ATL has no stylus handwriting (InputMethodManager.isStylusHandwritingAvailable() is false). */
	private boolean mAutoHandwritingEnabled = true;

	public void setAutoHandwritingEnabled(boolean enabled) {
		mAutoHandwritingEnabled = enabled;
	}

	public boolean isAutoHandwritingEnabled() {
		return mAutoHandwritingEnabled;
	}

	/* AOSP View.onCreateContextMenu(ContextMenu): empty hook; ATL never shows context menus */
	protected void onCreateContextMenu(ContextMenu menu) {}

	/* AOSP View.showContextMenu(): ATL has no context menus (the OnCreateContextMenuListener is never asked), so
	 * no menu is shown and AOSP's answer for a view whose menu could not be shown is false */
	public boolean showContextMenu() {
		return false;
	}

	/* AOSP View.showContextMenu(float, float): anchored at the view-relative position, asked of the parent chain */
	public boolean showContextMenu(float x, float y) {
		return getParent().showContextMenuForChild(this, x, y);
	}

	/* AOSP View.OnUnhandledKeyEventListener: asked after the tree did not handle a key, the listeners of the
	 * children first */
	private ArrayList<OnUnhandledKeyEventListener> mUnhandledKeyListeners;

	public void addOnUnhandledKeyEventListener(OnUnhandledKeyEventListener listener) {
		if (mUnhandledKeyListeners == null) {
			mUnhandledKeyListeners = new ArrayList<OnUnhandledKeyEventListener>();
		}
		mUnhandledKeyListeners.add(listener);
	}

	public void removeOnUnhandledKeyEventListener(OnUnhandledKeyEventListener listener) {
		if (mUnhandledKeyListeners != null) {
			mUnhandledKeyListeners.remove(listener);
			if (mUnhandledKeyListeners.size() == 0) {
				mUnhandledKeyListeners = null;
			}
		}
	}

	boolean hasUnhandledKeyListener() {
		return mUnhandledKeyListeners != null;
	}

	boolean dispatchUnhandledKeyEvent(KeyEvent evt) {
		if (mUnhandledKeyListeners != null) {
			for (int i = mUnhandledKeyListeners.size() - 1; i >= 0; i--) {
				if (mUnhandledKeyListeners.get(i).onUnhandledKeyEvent(this, evt)) {
					return true;
				}
			}
		}
		return false;
	}

	/* AOSP View.setOnReceiveContentListener() and performReceiveContent(): content (a paste, a drop, a keyboard
	 * image) goes to the listener, which gives back what it did not take; onReceiveContent() is the default for a
	 * view without one */
	private OnReceiveContentListener mOnReceiveContentListener;
	private String[] mOnReceiveContentMimeTypes;

	public void setOnReceiveContentListener(String[] mimeTypes, OnReceiveContentListener listener) {
		if (listener == null) {
			mimeTypes = null;
		}
		if (mimeTypes != null) {
			boolean hasMimeType = false;
			for (String mimeType : mimeTypes) {
				if (mimeType.startsWith("*")) {
					throw new IllegalArgumentException("A MIME type set here must not start with *: '" + mimeType + "'");
				}
				hasMimeType = true;
			}
			if (!hasMimeType) {
				throw new IllegalArgumentException("MIME types can't be empty");
			}
		}
		mOnReceiveContentMimeTypes = mimeTypes == null ? null : Arrays.copyOf(mimeTypes, mimeTypes.length);
		mOnReceiveContentListener = listener;
	}

	public String[] getReceiveContentMimeTypes() {
		return mOnReceiveContentMimeTypes;
	}

	public ContentInfo performReceiveContent(ContentInfo payload) {
		final OnReceiveContentListener listener = mOnReceiveContentListener;
		if (listener != null) {
			final ContentInfo remaining = listener.onReceiveContent(this, payload);
			return (remaining == null) ? null : onReceiveContent(remaining);
		}
		return onReceiveContent(payload);
	}

	public ContentInfo onReceiveContent(ContentInfo payload) {
		return payload;
	}

	/* AOSP View.startDragAndDrop(), updateDragShadow() and cancelDragAndDrop(): the drag of a view's data to a
	 * view of any window. ATL has no drag source (GTK's GtkDragSource is not wired to the views, only drops from the
	 * desktop reach onDragEvent()), so a drag cannot start: AOSP's answer for a drag that did not start is false, and
	 * with no drag going on updateDragShadow() and cancelDragAndDrop() do what AOSP does then, log and return. */
	public final boolean startDragAndDrop(ClipData data, DragShadowBuilder shadowBuilder, Object myLocalState, int flags) {
		Slog.w("View", "startDragAndDrop: ATL has no drag source, no drag is started");
		return false;
	}

	@Deprecated
	public final boolean startDrag(ClipData data, DragShadowBuilder shadowBuilder, Object myLocalState, int flags) {
		return startDragAndDrop(data, shadowBuilder, myLocalState, flags);
	}

	public final void updateDragShadow(DragShadowBuilder shadowBuilder) {
		Slog.w("View", "updateDragShadow called when not dragging.");
	}

	public final void cancelDragAndDrop() {
		Slog.w("View", "No active drag to cancel");
	}

	/* ---- AOSP View, autofill and assist structure. ATL has no autofill service or assist: the system never calls
	 * onProvide*Structure(), but the methods are the ones apps override and call themselves (a custom view fills
	 * its virtual children in them), and they fill the ViewStructure as AOSP does. ---- */

	public static final int AUTOFILL_TYPE_NONE = 0;
	public static final int AUTOFILL_TYPE_TEXT = 1;
	public static final int AUTOFILL_TYPE_TOGGLE = 2;
	public static final int AUTOFILL_TYPE_LIST = 3;
	public static final int AUTOFILL_TYPE_DATE = 4;
	public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0x0;
	public static final int IMPORTANT_FOR_AUTOFILL_YES = 0x1;
	public static final int IMPORTANT_FOR_AUTOFILL_NO = 0x2;
	public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 0x4;
	public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 0x8;
	public static final int AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 0x1;

	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
	public @interface AutofillType {
	}

	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
	public @interface AutofillImportance {
	}

	private String[] mAutofillHints;
	private AutofillId mAutofillId;
	private boolean mAutofillIdExplicitlySet;
	private int mAutofillViewId = NO_ID;
	private int mImportantForAutofill = IMPORTANT_FOR_AUTOFILL_AUTO;
	private static int sNextAutofillViewId = 1;
	private ContentCaptureSession mContentCaptureSession;

	public void setAutofillHints(String... autofillHints) {
		if (autofillHints == null || autofillHints.length == 0) {
			mAutofillHints = null;
		} else {
			mAutofillHints = autofillHints;
		}
	}

	public String[] getAutofillHints() {
		return mAutofillHints;
	}

	/* AOSP View.setPendingCredentialRequest()/clearPendingCredentialRequest()/getPendingCredentialRequest()/
	 * getPendingCredentialCallback(): the view's id is added to the candidate query data of every option. The
	 * request is fired by AutofillManager on focus; ATL has no credential provider and no autofill session, so it is
	 * only kept (as AOSP keeps it in ViewCredentialHandler) */
	private GetCredentialRequest mPendingCredentialRequest;
	private OutcomeReceiver<GetCredentialResponse, GetCredentialException> mPendingCredentialCallback;

	public void clearPendingCredentialRequest() {
		mPendingCredentialRequest = null;
		mPendingCredentialCallback = null;
	}

	public void setPendingCredentialRequest(GetCredentialRequest request,
			OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback) {
		java.util.Objects.requireNonNull(request, "request must not be null");
		java.util.Objects.requireNonNull(callback, "callback must not be null");
		for (CredentialOption option : request.getCredentialOptions()) {
			ArrayList<AutofillId> ids = option.getCandidateQueryData()
					.getParcelableArrayList(CredentialProviderService.EXTRA_AUTOFILL_ID, AutofillId.class);
			ids = ids != null ? ids : new ArrayList<AutofillId>();
			if (!ids.contains(getAutofillId())) {
				ids.add(getAutofillId());
			}
			option.getCandidateQueryData().putParcelableArrayList(CredentialProviderService.EXTRA_AUTOFILL_ID, ids);
		}
		mPendingCredentialRequest = request;
		mPendingCredentialCallback = callback;
	}

	public final GetCredentialRequest getPendingCredentialRequest() {
		return mPendingCredentialRequest;
	}

	public final OutcomeReceiver<GetCredentialResponse, GetCredentialException> getPendingCredentialCallback() {
		return mPendingCredentialCallback;
	}

	/* AOSP View.getAutofillId(): the id is made once, from an id that is unique in the app */
	public final AutofillId getAutofillId() {
		if (mAutofillId == null) {
			mAutofillId = new AutofillId(getAutofillViewId());
		}
		return mAutofillId;
	}

	private int getAutofillViewId() {
		if (mAutofillViewId == NO_ID) {
			mAutofillViewId = sNextAutofillViewId++;
		}
		return mAutofillViewId;
	}

	public void setAutofillId(AutofillId id) {
		if (isAttachedToWindow()) {
			throw new IllegalStateException("Cannot set autofill id when view is attached");
		}
		if (id != null && !id.isNonVirtual()) {
			throw new IllegalStateException("Cannot set autofill id assigned to virtual views");
		}
		if (id == null && !mAutofillIdExplicitlySet) {
			// Ignore reset because it was never explicitly set before.
			return;
		}
		mAutofillId = id;
		if (id != null) {
			mAutofillViewId = id.getViewId();
			mAutofillIdExplicitlySet = true;
		} else {
			mAutofillViewId = NO_ID;
			mAutofillIdExplicitlySet = false;
		}
	}

	public int getAutofillType() {
		return AUTOFILL_TYPE_NONE;
	}

	public AutofillValue getAutofillValue() {
		return null;
	}

	public void autofill(AutofillValue value) {
	}

	/* AOSP View.autofill(SparseArray<AutofillValue>): only in autofill compatibility mode (an autofill service with
	 * a compat allowlist), which ATL does not have, so nothing is filled */
	public void autofill(android.util.SparseArray<AutofillValue> values) {
	}

	public int getImportantForAutofill() {
		return mImportantForAutofill;
	}

	public void setImportantForAutofill(int mode) {
		mImportantForAutofill = mode;
	}

	/* AOSP View.isImportantForAutofill(): not hidden by an ancestor and either said so or has an id the app made */
	public final boolean isImportantForAutofill() {
		// Check parent mode to ensure we're not hidden.
		ViewParent p = parent;
		while (p instanceof View) {
			int parentImportance = ((View)p).getImportantForAutofill();
			if (parentImportance == IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS || parentImportance == IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS) {
				return false;
			}
			p = p.getParent();
		}
		final int importance = getImportantForAutofill();
		// First, check the explicit states.
		if (importance == IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS || importance == IMPORTANT_FOR_AUTOFILL_YES) {
			return true;
		}
		if (importance == IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS || importance == IMPORTANT_FOR_AUTOFILL_NO) {
			return false;
		}
		// Then use some heuristics to handle AUTO: a view with an explicit resource id of the app
		final int viewId = this.id;
		if (viewId != NO_ID && !isViewIdGenerated(viewId)) {
			Resources res = getResources();
			String entry = null;
			String pkg = null;
			try {
				entry = res.getResourceEntryName(viewId);
				pkg = res.getResourcePackageName(viewId);
			} catch (Resources.NotFoundException e) {
				// ignore
			}
			if (entry != null && pkg != null && pkg.equals(getContext().getPackageName())) {
				return true;
			}
		}
		// Otherwise, assume it's not important...
		return false;
	}

	private static boolean isViewIdGenerated(int id) {
		return (id & 0xFF000000) == 0 && (id & 0x00FFFFFF) != 0;
	}

	public void setContentCaptureSession(ContentCaptureSession contentCaptureSession) {
		mContentCaptureSession = contentCaptureSession;
	}

	/* AOSP View.getContentCaptureSession(): the session set on the view or its closest ancestor; without a
	 * ContentCaptureManager (ATL has none) the main session is null as well */
	public ContentCaptureSession getContentCaptureSession() {
		if (mContentCaptureSession != null) {
			return mContentCaptureSession;
		}
		ViewParent p = parent;
		while (p instanceof View) {
			if (((View)p).mContentCaptureSession != null) {
				return ((View)p).mContentCaptureSession;
			}
			p = p.getParent();
		}
		return null;
	}

	public void onProvideStructure(ViewStructure structure) {
		onProvideStructureForAssistOrAutofill(structure, false, 0);
	}

	public void onProvideAutofillStructure(ViewStructure structure, int flags) {
		onProvideStructureForAssistOrAutofill(structure, true, flags);
	}

	private void onProvideStructureForAssistOrAutofill(ViewStructure structure, boolean forAutofill, int flags) {
		final int viewId = this.id;
		if (viewId != NO_ID && !isViewIdGenerated(viewId)) {
			String pkg, type, entry;
			try {
				Resources res = getResources();
				entry = res.getResourceEntryName(viewId);
				type = res.getResourceTypeName(viewId);
				pkg = res.getResourcePackageName(viewId);
			} catch (Resources.NotFoundException e) {
				entry = type = pkg = null;
			}
			structure.setId(viewId, pkg, type, entry);
		} else {
			structure.setId(viewId, null, null, null);
		}
		if (forAutofill) {
			final int autofillType = getAutofillType();
			// Don't need to fill autofill info if view does not support it.
			if (autofillType != AUTOFILL_TYPE_NONE) {
				structure.setAutofillType(autofillType);
				structure.setAutofillHints(getAutofillHints());
				structure.setAutofillValue(getAutofillValue());
			}
			structure.setImportantForAutofill(getImportantForAutofill());
		}
		int ignoredParentLeft = 0;
		int ignoredParentTop = 0;
		if (forAutofill && (flags & AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS) == 0) {
			View parentGroup = null;
			ViewParent viewParent = getParent();
			if (viewParent instanceof View) {
				parentGroup = (View)viewParent;
			}
			while (parentGroup != null && !parentGroup.isImportantForAutofill()) {
				ignoredParentLeft += parentGroup.getLeft();
				ignoredParentTop += parentGroup.getTop();
				viewParent = parentGroup.getParent();
				if (viewParent instanceof View) {
					parentGroup = (View)viewParent;
				} else {
					break;
				}
			}
		}
		structure.setDimens(ignoredParentLeft + getLeft(), ignoredParentTop + getTop(), getScrollX(), getScrollY(), getRight() - getLeft(), getBottom() - getTop());
		if (!forAutofill) {
			if (hasTransform) {
				structure.setTransformation(getMatrix());
			}
			structure.setElevation(getZ());
			structure.setVisibility(getVisibility());
			structure.setEnabled(isEnabled());
			if (isClickable()) {
				structure.setClickable(true);
			}
			if (isFocusable()) {
				structure.setFocusable(true);
			}
			if (isFocused()) {
				structure.setFocused(true);
			}
			if (isAccessibilityFocused()) {
				structure.setAccessibilityFocused(true);
			}
			if (isSelected()) {
				structure.setSelected(true);
			}
			if (isActivated()) {
				structure.setActivated(true);
			}
			if (isLongClickable()) {
				structure.setLongClickable(true);
			}
			if (this instanceof Checkable) {
				structure.setCheckable(true);
				if (((Checkable)this).isChecked()) {
					structure.setChecked(true);
				}
			}
			if (isOpaque()) {
				structure.setOpaque(true);
			}
			if (isContextClickable()) {
				structure.setContextClickable(true);
			}
			structure.setClassName(getAccessibilityClassName().toString());
			structure.setContentDescription(getContentDescription());
		}
	}

	/* AOSP: views that draw virtual children (a calendar's cells) fill them here */
	public void onProvideVirtualStructure(ViewStructure structure) {
	}

	public void onProvideAutofillVirtualStructure(ViewStructure structure, int flags) {
	}

	public void dispatchProvideStructure(ViewStructure structure) {
		dispatchProvideStructureForAssistOrAutofill(structure, false, 0);
	}

	public void dispatchProvideAutofillStructure(ViewStructure structure, int flags) {
		dispatchProvideStructureForAssistOrAutofill(structure, true, flags);
	}

	private void dispatchProvideStructureForAssistOrAutofill(ViewStructure structure, boolean forAutofill, int flags) {
		if (forAutofill) {
			structure.setAutofillId(getAutofillId());
			onProvideAutofillStructure(structure, flags);
			onProvideAutofillVirtualStructure(structure, flags);
		} else {
			onProvideStructure(structure);
			onProvideVirtualStructure(structure);
		}
	}

	/* ---- AOSP View, nested scrolling (NestedScrollingChild): the view tells the closest ancestor that takes part
	 * (onStartNestedScroll() answers true) about its scrolling and lets it consume before and after ---- */

	private ViewParent mNestedScrollingParent;
	private boolean mNestedScrollingEnabled;
	private int[] mTempNestedScrollConsumed;

	public void setNestedScrollingEnabled(boolean enabled) {
		if (enabled) {
			mNestedScrollingEnabled = true;
		} else {
			stopNestedScroll();
			mNestedScrollingEnabled = false;
		}
	}

	public boolean isNestedScrollingEnabled() {
		return mNestedScrollingEnabled;
	}

	public boolean startNestedScroll(int axes) {
		if (hasNestedScrollingParent()) {
			// Already in progress
			return true;
		}
		if (isNestedScrollingEnabled()) {
			ViewParent p = getParent();
			View child = this;
			while (p != null) {
				if (p.onStartNestedScroll(child, this, axes)) {
					mNestedScrollingParent = p;
					p.onNestedScrollAccepted(child, this, axes);
					return true;
				}
				if (p instanceof View) {
					child = (View)p;
				}
				p = p.getParent();
			}
		}
		return false;
	}

	public void stopNestedScroll() {
		if (mNestedScrollingParent != null) {
			mNestedScrollingParent.onStopNestedScroll(this);
			mNestedScrollingParent = null;
		}
	}

	public boolean hasNestedScrollingParent() {
		return mNestedScrollingParent != null;
	}

	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
			if (dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0) {
				int startX = 0;
				int startY = 0;
				if (offsetInWindow != null) {
					getLocationInWindow(offsetInWindow);
					startX = offsetInWindow[0];
					startY = offsetInWindow[1];
				}
				mNestedScrollingParent.onNestedScroll(this, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
				if (offsetInWindow != null) {
					getLocationInWindow(offsetInWindow);
					offsetInWindow[0] -= startX;
					offsetInWindow[1] -= startY;
				}
				return true;
			} else if (offsetInWindow != null) {
				// No motion, no dispatch. Keep offsetInWindow up to date.
				offsetInWindow[0] = 0;
				offsetInWindow[1] = 0;
			}
		}
		return false;
	}

	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
			if (dx != 0 || dy != 0) {
				int startX = 0;
				int startY = 0;
				if (offsetInWindow != null) {
					getLocationInWindow(offsetInWindow);
					startX = offsetInWindow[0];
					startY = offsetInWindow[1];
				}
				if (consumed == null) {
					if (mTempNestedScrollConsumed == null) {
						mTempNestedScrollConsumed = new int[2];
					}
					consumed = mTempNestedScrollConsumed;
				}
				consumed[0] = 0;
				consumed[1] = 0;
				mNestedScrollingParent.onNestedPreScroll(this, dx, dy, consumed);
				if (offsetInWindow != null) {
					getLocationInWindow(offsetInWindow);
					offsetInWindow[0] -= startX;
					offsetInWindow[1] -= startY;
				}
				return consumed[0] != 0 || consumed[1] != 0;
			} else if (offsetInWindow != null) {
				offsetInWindow[0] = 0;
				offsetInWindow[1] = 0;
			}
		}
		return false;
	}

	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
			return mNestedScrollingParent.onNestedFling(this, velocityX, velocityY, consumed);
		}
		return false;
	}

	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
			return mNestedScrollingParent.onNestedPreFling(this, velocityX, velocityY);
		}
		return false;
	}

	/* AOSP View.setHandwritingBoundsOffsets() and setHandwritingDelegatorCallback(): the stylus handwriting
	 * delegation of the input method framework. ATL has no stylus handwriting service; the values are kept. */
	private float mHandwritingBoundsOffsetLeft;
	private float mHandwritingBoundsOffsetTop;
	private float mHandwritingBoundsOffsetRight;
	private float mHandwritingBoundsOffsetBottom;
	private Runnable mHandwritingDelegatorCallback;

	public void setHandwritingBoundsOffsets(float offsetLeft, float offsetTop, float offsetRight, float offsetBottom) {
		mHandwritingBoundsOffsetLeft = offsetLeft;
		mHandwritingBoundsOffsetTop = offsetTop;
		mHandwritingBoundsOffsetRight = offsetRight;
		mHandwritingBoundsOffsetBottom = offsetBottom;
	}

	public float getHandwritingBoundsOffsetLeft() {
		return mHandwritingBoundsOffsetLeft;
	}

	public float getHandwritingBoundsOffsetTop() {
		return mHandwritingBoundsOffsetTop;
	}

	public float getHandwritingBoundsOffsetRight() {
		return mHandwritingBoundsOffsetRight;
	}

	public float getHandwritingBoundsOffsetBottom() {
		return mHandwritingBoundsOffsetBottom;
	}

	public void setHandwritingDelegatorCallback(Runnable callback) {
		mHandwritingDelegatorCallback = callback;
	}

	public Runnable getHandwritingDelegatorCallback() {
		return mHandwritingDelegatorCallback;
	}

	/* AOSP View: the delegate side of stylus handwriting delegation, kept for the getters like the values above */
	private String mAllowedHandwritingDelegatePackageName;
	private boolean mIsHandwritingDelegate;
	private String mAllowedHandwritingDelegatorPackageName;
	private int mHandwritingDelegateFlags;

	public void setAllowedHandwritingDelegatePackage(String allowedPackageName) {
		mAllowedHandwritingDelegatePackageName = allowedPackageName;
	}

	public String getAllowedHandwritingDelegatePackageName() {
		return mAllowedHandwritingDelegatePackageName;
	}

	public void setIsHandwritingDelegate(boolean isHandwritingDelegate) {
		mIsHandwritingDelegate = isHandwritingDelegate;
	}

	public boolean isHandwritingDelegate() {
		return mIsHandwritingDelegate;
	}

	public void setAllowedHandwritingDelegatorPackage(String allowedPackageName) {
		mAllowedHandwritingDelegatorPackageName = allowedPackageName;
	}

	public String getAllowedHandwritingDelegatorPackageName() {
		return mAllowedHandwritingDelegatorPackageName;
	}

	public void setHandwritingDelegateFlags(int flags) {
		mHandwritingDelegateFlags = flags;
	}

	public int getHandwritingDelegateFlags() {
		return mHandwritingDelegateFlags;
	}
}
