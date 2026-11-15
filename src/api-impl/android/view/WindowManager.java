package android.view;

import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowInsets.Side;
import android.view.WindowInsets.Type;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface WindowManager {

	/* AOSP WindowManager.BadTokenException / InvalidDisplayException (thrown by addView() on Android) */
	public static class BadTokenException extends RuntimeException {
		public BadTokenException() {
		}

		public BadTokenException(String name) {
			super(name);
		}
	}

	public static class InvalidDisplayException extends RuntimeException {
		public InvalidDisplayException() {
		}

		public InvalidDisplayException(String name) {
			super(name);
		}
	}

	/* AOSP WindowManager.isCrossWindowBlurEnabled() and listener methods: interface defaults
	 * (false / no-op); WindowManagerImpl overrides them like AOSP's WindowManagerImpl */
	default boolean isCrossWindowBlurEnabled() {
		return false;
	}

	default void addCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener) {
	}

	default void addCrossWindowBlurEnabledListener(java.util.concurrent.Executor executor,
			java.util.function.Consumer<Boolean> listener) {
	}

	default void removeCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener) {
	}

	/* AOSP 16 WindowManager screen recording state and callback defaults */
	int SCREEN_RECORDING_STATE_NOT_VISIBLE = 0;
	int SCREEN_RECORDING_STATE_VISIBLE = 1;

	default int addScreenRecordingCallback(java.util.concurrent.Executor executor,
			java.util.function.Consumer<Integer> callback) {
		throw new UnsupportedOperationException();
	}

	default void removeScreenRecordingCallback(java.util.function.Consumer<Integer> callback) {
		throw new UnsupportedOperationException();
	}
	/* AOSP WindowManager's defaults for the SurfaceControl input receivers: a WindowManager that does not implement
	 * them (ATL's has no input channels for a layer) throws, as AOSP's interface does */
	default android.window.InputTransferToken registerBatchedSurfaceControlInputReceiver(
			android.window.InputTransferToken hostInputTransferToken, SurfaceControl surfaceControl,
			Choreographer choreographer, SurfaceControlInputReceiver receiver) {
		throw new UnsupportedOperationException("registerBatchedSurfaceControlInputReceiver is not implemented");
	}

	default android.window.InputTransferToken registerUnbatchedSurfaceControlInputReceiver(
			android.window.InputTransferToken hostInputTransferToken, SurfaceControl surfaceControl,
			android.os.Looper looper, SurfaceControlInputReceiver receiver) {
		throw new UnsupportedOperationException("registerUnbatchedSurfaceControlInputReceiver is not implemented");
	}

	default void unregisterSurfaceControlInputReceiver(SurfaceControl surfaceControl) {
		throw new UnsupportedOperationException("unregisterSurfaceControlInputReceiver is not implemented");
	}

	default boolean transferTouchGesture(android.window.InputTransferToken transferFromToken,
			android.window.InputTransferToken transferToToken) {
		throw new UnsupportedOperationException("transferTouchGesture is not implemented");
	}

	public android.view.Display getDefaultDisplay();

	public void addView(View view, ViewGroup.LayoutParams params);

	public void updateViewLayout(View view, ViewGroup.LayoutParams params);

	public void removeView(View view);

	public void removeViewImmediate(View view);

	/**
	 * Returns the {@link WindowMetrics} according to the current system state.
	 */
	public WindowMetrics getCurrentWindowMetrics();

	/**
	 * Returns the largest {@link WindowMetrics} an app may expect in the current system state.
	 */
	public WindowMetrics getMaximumWindowMetrics();

    public class LayoutParams extends ViewGroup.LayoutParams implements Parcelable {
        public int x;
        public int y;
        public float horizontalWeight;
        public float verticalWeight;
        public int type;
        public static final int FIRST_APPLICATION_WINDOW = 1;
        public static final int TYPE_BASE_APPLICATION   = 1;
        public static final int TYPE_APPLICATION        = 2;
        public static final int TYPE_APPLICATION_STARTING = 3;
        public static final int TYPE_DRAWN_APPLICATION = 4;
        public static final int LAST_APPLICATION_WINDOW = 99;
        public static final int FIRST_SUB_WINDOW = 1000;
        public static final int TYPE_APPLICATION_PANEL = FIRST_SUB_WINDOW;
        public static final int TYPE_APPLICATION_MEDIA = FIRST_SUB_WINDOW + 1;
        public static final int TYPE_APPLICATION_SUB_PANEL = FIRST_SUB_WINDOW + 2;
        public static final int TYPE_APPLICATION_ATTACHED_DIALOG = FIRST_SUB_WINDOW + 3;
        public static final int TYPE_APPLICATION_MEDIA_OVERLAY  = FIRST_SUB_WINDOW + 4;
        public static final int TYPE_APPLICATION_ABOVE_SUB_PANEL = FIRST_SUB_WINDOW + 5;
        public static final int LAST_SUB_WINDOW = 1999;
        public static final int FIRST_SYSTEM_WINDOW     = 2000;
        public static final int TYPE_STATUS_BAR         = FIRST_SYSTEM_WINDOW;
        public static final int TYPE_SEARCH_BAR         = FIRST_SYSTEM_WINDOW+1;
        public static final int TYPE_PHONE              = FIRST_SYSTEM_WINDOW+2;
        public static final int TYPE_SYSTEM_ALERT       = FIRST_SYSTEM_WINDOW+3;
        public static final int TYPE_KEYGUARD           = FIRST_SYSTEM_WINDOW+4;
        public static final int TYPE_TOAST              = FIRST_SYSTEM_WINDOW+5;
        public static final int TYPE_SYSTEM_OVERLAY     = FIRST_SYSTEM_WINDOW+6;
        public static final int TYPE_PRIORITY_PHONE     = FIRST_SYSTEM_WINDOW+7;
        public static final int TYPE_SYSTEM_DIALOG      = FIRST_SYSTEM_WINDOW+8;
        public static final int TYPE_KEYGUARD_DIALOG    = FIRST_SYSTEM_WINDOW+9;
        public static final int TYPE_SYSTEM_ERROR       = FIRST_SYSTEM_WINDOW+10;
        public static final int TYPE_INPUT_METHOD       = FIRST_SYSTEM_WINDOW+11;
        public static final int TYPE_INPUT_METHOD_DIALOG= FIRST_SYSTEM_WINDOW+12;
        public static final int TYPE_WALLPAPER          = FIRST_SYSTEM_WINDOW+13;
        public static final int TYPE_STATUS_BAR_PANEL   = FIRST_SYSTEM_WINDOW+14;
        public static final int TYPE_SECURE_SYSTEM_OVERLAY = FIRST_SYSTEM_WINDOW+15;
        public static final int TYPE_DRAG               = FIRST_SYSTEM_WINDOW+16;
        public static final int TYPE_STATUS_BAR_SUB_PANEL = FIRST_SYSTEM_WINDOW+17;
        public static final int TYPE_POINTER = FIRST_SYSTEM_WINDOW+18;
        public static final int TYPE_NAVIGATION_BAR = FIRST_SYSTEM_WINDOW+19;
        public static final int TYPE_VOLUME_OVERLAY = FIRST_SYSTEM_WINDOW+20;
        public static final int TYPE_BOOT_PROGRESS = FIRST_SYSTEM_WINDOW+21;
        public static final int TYPE_INPUT_CONSUMER = FIRST_SYSTEM_WINDOW+22;
        public static final int TYPE_NAVIGATION_BAR_PANEL = FIRST_SYSTEM_WINDOW+24;
        public static final int TYPE_DISPLAY_OVERLAY = FIRST_SYSTEM_WINDOW+26;
        public static final int TYPE_MAGNIFICATION_OVERLAY = FIRST_SYSTEM_WINDOW+27;
        public static final int TYPE_PRIVATE_PRESENTATION = FIRST_SYSTEM_WINDOW+30;
        public static final int TYPE_VOICE_INTERACTION = FIRST_SYSTEM_WINDOW+31;
        public static final int TYPE_ACCESSIBILITY_OVERLAY = FIRST_SYSTEM_WINDOW+32;
        public static final int TYPE_VOICE_INTERACTION_STARTING = FIRST_SYSTEM_WINDOW+33;
        public static final int TYPE_DOCK_DIVIDER = FIRST_SYSTEM_WINDOW+34;
        public static final int TYPE_QS_DIALOG = FIRST_SYSTEM_WINDOW+35;
        public static final int TYPE_SCREENSHOT = FIRST_SYSTEM_WINDOW + 36;
        public static final int TYPE_PRESENTATION = FIRST_SYSTEM_WINDOW + 37;
        public static final int TYPE_APPLICATION_OVERLAY = FIRST_SYSTEM_WINDOW + 38;
        public static final int TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY = FIRST_SYSTEM_WINDOW + 39;
        public static final int TYPE_NOTIFICATION_SHADE = FIRST_SYSTEM_WINDOW + 40;
        public static final int TYPE_STATUS_BAR_ADDITIONAL = FIRST_SYSTEM_WINDOW + 41;
        public static final int LAST_SYSTEM_WINDOW      = 2999;
        public static final int INVALID_WINDOW_TYPE = -1;
        public @interface WindowType {}
        public static boolean isSystemAlertWindowType( int type) {
            switch (type) {
                case TYPE_PHONE:
                case TYPE_PRIORITY_PHONE:
                case TYPE_SYSTEM_ALERT:
                case TYPE_SYSTEM_ERROR:
                case TYPE_SYSTEM_OVERLAY:
                case TYPE_APPLICATION_OVERLAY:
                    return true;
            }
            return false;
        }
        public static final int MEMORY_TYPE_NORMAL = 0;
        public static final int MEMORY_TYPE_HARDWARE = 1;
        public static final int MEMORY_TYPE_GPU = 2;
        public static final int MEMORY_TYPE_PUSH_BUFFERS = 3;
        public int memoryType;
        public static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON     = 0x00000001;
        public static final int FLAG_DIM_BEHIND        = 0x00000002;
        public static final int FLAG_BLUR_BEHIND        = 0x00000004;
        public static final int FLAG_NOT_FOCUSABLE      = 0x00000008;
        public static final int FLAG_NOT_TOUCHABLE      = 0x00000010;
        public static final int FLAG_NOT_TOUCH_MODAL    = 0x00000020;
        public static final int FLAG_TOUCHABLE_WHEN_WAKING = 0x00000040;
        public static final int FLAG_KEEP_SCREEN_ON     = 0x00000080;
        public static final int FLAG_LAYOUT_IN_SCREEN   = 0x00000100;
        public static final int FLAG_LAYOUT_NO_LIMITS   = 0x00000200;
        public static final int FLAG_FULLSCREEN      = 0x00000400;
        public static final int FLAG_FORCE_NOT_FULLSCREEN   = 0x00000800;
        public static final int FLAG_DITHER             = 0x00001000;
        public static final int FLAG_SECURE             = 0x00002000;
        public static final int FLAG_SCALED             = 0x00004000;
        public static final int FLAG_IGNORE_CHEEK_PRESSES    = 0x00008000;
        public static final int FLAG_LAYOUT_INSET_DECOR = 0x00010000;
        public static final int FLAG_ALT_FOCUSABLE_IM = 0x00020000;
        public static final int FLAG_WATCH_OUTSIDE_TOUCH = 0x00040000;
        public static final int FLAG_SHOW_WHEN_LOCKED = 0x00080000;
        public static final int FLAG_SHOW_WALLPAPER = 0x00100000;
        public static final int FLAG_TURN_SCREEN_ON = 0x00200000;
        public static final int FLAG_DISMISS_KEYGUARD = 0x00400000;
        public static final int FLAG_SPLIT_TOUCH = 0x00800000;
        public static final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
        public static final int FLAG_LAYOUT_IN_OVERSCAN = 0x02000000;
        public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
        public static final int FLAG_LOCAL_FOCUS_MODE = 0x10000000;
        public static final int FLAG_SLIPPERY = 0x20000000;
        public static final int FLAG_LAYOUT_ATTACHED_IN_DECOR = 0x40000000;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;
        public @interface Flags {}
        public int flags;
        public static final int PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED = 1 << 1;
        public static final int PRIVATE_FLAG_WANTS_OFFSET_NOTIFICATIONS = 1 << 2;
        public static final int PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY = 1 << 3;
        public static final int SYSTEM_FLAG_SHOW_FOR_ALL_USERS = 1 << 4;
        public static final int PRIVATE_FLAG_UNRESTRICTED_GESTURE_EXCLUSION = 1 << 5;
        public static final int PRIVATE_FLAG_NO_MOVE_ANIMATION = 1 << 6;
        public static final int PRIVATE_FLAG_SYSTEM_ERROR = 1 << 8;
        public static final int PRIVATE_FLAG_OPTIMIZE_MEASURE = 1 << 9;
        public static final int PRIVATE_FLAG_DISABLE_WALLPAPER_TOUCH_EVENTS = 1 << 10;
        public static final int PRIVATE_FLAG_EDGE_TO_EDGE_ENFORCED = 1 << 11;
        public static final int PRIVATE_FLAG_LAYOUT_SIZE_EXTENDED_BY_CUTOUT = 1 << 12;
        public static final int PRIVATE_FLAG_FORCE_DECOR_VIEW_VISIBILITY = 1 << 13;
        public static final int PRIVATE_FLAG_LAYOUT_CHILD_WINDOW_IN_PARENT_FRAME = 1 << 14;
        public static final int PRIVATE_FLAG_FORCE_DRAW_BAR_BACKGROUNDS = 1 << 15;
        public static final int PRIVATE_FLAG_SUSTAINED_PERFORMANCE_MODE = 1 << 16;
        public static final int PRIVATE_FLAG_IMMERSIVE_CONFIRMATION_WINDOW = 1 << 17;
        public static final int SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS = 1 << 19;
        public static final int PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY = 1 << 20;
        public static final int PRIVATE_FLAG_EXCLUDE_FROM_SCREEN_MAGNIFICATION = 1 << 21;
        public static final int PRIVATE_FLAG_NOT_MAGNIFIABLE = 1 << 22;
        public static final int PRIVATE_FLAG_COLOR_SPACE_AGNOSTIC = 1 << 24;
        public static final int PRIVATE_FLAG_CONSUME_IME_INSETS = 1 << 25;
        public static final int PRIVATE_FLAG_APPEARANCE_CONTROLLED = 1 << 26;
        public static final int PRIVATE_FLAG_BEHAVIOR_CONTROLLED = 1 << 27;
        public static final int PRIVATE_FLAG_FIT_INSETS_CONTROLLED = 1 << 28;
        public static final int PRIVATE_FLAG_TRUSTED_OVERLAY = 1 << 29;
        public static final int PRIVATE_FLAG_INSET_PARENT_FRAME_BY_IME = 1 << 30;
        public static final int PRIVATE_FLAG_INTERCEPT_GLOBAL_DRAG_AND_DROP = 1 << 31;
        public @interface SystemFlags {}
        public @interface PrivateFlags {}
        public int privateFlags;
        public static boolean mayUseInputMethod(int flags) {
            return (flags & FLAG_NOT_FOCUSABLE) != FLAG_NOT_FOCUSABLE
                    && (flags & FLAG_ALT_FOCUSABLE_IM) != FLAG_ALT_FOCUSABLE_IM;
        }
        public static final int SOFT_INPUT_MASK_STATE = 0x0f;
        public static final int SOFT_INPUT_STATE_UNSPECIFIED = 0;
        public static final int SOFT_INPUT_STATE_UNCHANGED = 1;
        public static final int SOFT_INPUT_STATE_HIDDEN = 2;
        public static final int SOFT_INPUT_STATE_ALWAYS_HIDDEN = 3;
        public static final int SOFT_INPUT_STATE_VISIBLE = 4;
        public static final int SOFT_INPUT_STATE_ALWAYS_VISIBLE = 5;
        public static final int SOFT_INPUT_MASK_ADJUST = 0xf0;
        public static final int SOFT_INPUT_ADJUST_UNSPECIFIED = 0x00;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x10;
        public static final int SOFT_INPUT_ADJUST_PAN = 0x20;
        public static final int SOFT_INPUT_ADJUST_NOTHING = 0x30;
        public static final int SOFT_INPUT_IS_FORWARD_NAVIGATION = 0x100;
        public @interface SoftInputModeFlags {}
        public int softInputMode;
        /* gravity is ViewGroup.LayoutParams.gravity in ATL (-1 while none is set) */
        public float horizontalMargin;
        public float verticalMargin;
        public final Rect surfaceInsets = new Rect();
        public boolean hasManualSurfaceInsets;
        public boolean receiveInsetsIgnoringZOrder;
        public boolean preservePreviousSurfaceInsets = true;
        public int format;
        public int windowAnimations;
        public float alpha = 1.0f;
        public float dimAmount = 1.0f;
        public static final float BRIGHTNESS_OVERRIDE_NONE = -1.0f;
        public static final float BRIGHTNESS_OVERRIDE_OFF = 0.0f;
        public static final float BRIGHTNESS_OVERRIDE_FULL = 1.0f;
        public float screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
        public float buttonBrightness = BRIGHTNESS_OVERRIDE_NONE;
        public static final int ROTATION_ANIMATION_UNSPECIFIED = -1;
        public static final int ROTATION_ANIMATION_ROTATE = 0;
        public static final int ROTATION_ANIMATION_CROSSFADE = 1;
        public static final int ROTATION_ANIMATION_JUMPCUT = 2;
        public static final int ROTATION_ANIMATION_SEAMLESS = 3;
        public int rotationAnimation = ROTATION_ANIMATION_ROTATE;
        public IBinder token = null;
        public IBinder mWindowContextToken = null;
        public String packageName = null;
        public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        public float preferredRefreshRate;
        public int preferredDisplayModeId;
        public float preferredMinDisplayRefreshRate;
        public float preferredMaxDisplayRefreshRate;
        public static final int DISPLAY_FLAG_DISABLE_HDR_CONVERSION =  1 << 0;
        public @interface DisplayFlags {}
        private int mDisplayFlags;
        public @interface SystemUiVisibilityFlags {}
        public int systemUiVisibility;
        public int subtreeSystemUiVisibility;
        public boolean hasSystemUiListeners;
        @interface LayoutInDisplayCutoutMode {}
        public int layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER = 2;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 3;
        public static final int INPUT_FEATURE_NO_INPUT_CHANNEL = 1 << 0;
        public static final int INPUT_FEATURE_DISABLE_USER_ACTIVITY = 1 << 1;
        public static final int INPUT_FEATURE_SPY = 1 << 2;
        public @interface InputFeatureFlags {
        }
        public int inputFeatures;
        public long userActivityTimeout = -1;
        public long accessibilityIdOfAnchor = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
        public CharSequence accessibilityTitle;
        public long hideTimeoutMilliseconds = -1;
        public boolean preferMinimalPostProcessing = false;
        private int mBlurBehindRadius = 0;
        private int mColorMode = 0 /* ActivityInfo.COLOR_MODE_DEFAULT */;
        private float mDesiredHdrHeadroom = 0;
        private boolean mFrameRateBoostOnTouch = true;
        private boolean mIsFrameRatePowerSavingsBalanced = true;
        /* AOSP: aconfig flag toolkit_set_frame_rate_read_only, on in the releases that have the API */
        private static final boolean sToolkitSetFrameRateReadOnlyFlagValue = true;
        public final InsetsFlags insetsFlags = new InsetsFlags();
        private  int mFitInsetsTypes = Type.systemBars();
        private  int mFitInsetsSides = Side.all();
        private boolean mFitInsetsIgnoringVisibility = false;
        public  int forciblyShownTypes;
        public LayoutParams[] paramsForRotation;
        private boolean mWallpaperTouchEventsEnabled = true;
        public void setFitInsetsTypes( int types) {
            mFitInsetsTypes = types;
            privateFlags |= PRIVATE_FLAG_FIT_INSETS_CONTROLLED;
        }
        public void setFitInsetsSides( int sides) {
            mFitInsetsSides = sides;
            privateFlags |= PRIVATE_FLAG_FIT_INSETS_CONTROLLED;
        }
        public void setFitInsetsIgnoringVisibility(boolean ignore) {
            mFitInsetsIgnoringVisibility = ignore;
            privateFlags |= PRIVATE_FLAG_FIT_INSETS_CONTROLLED;
        }
        public void setTrustedOverlay() {
            privateFlags |= PRIVATE_FLAG_TRUSTED_OVERLAY;
        }
        public void setSystemApplicationOverlay(boolean isSystemApplicationOverlay) {
            if (isSystemApplicationOverlay) {
                privateFlags |= PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY;
            } else {
                privateFlags &= ~PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY;
            }
        }
        public boolean isSystemApplicationOverlay() {
            return (privateFlags & PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY)
                    == PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY;
        }
        public void setWallpaperTouchEventsEnabled(boolean enable) {
            mWallpaperTouchEventsEnabled = enable;
        }
        public boolean areWallpaperTouchEventsEnabled() {
            return mWallpaperTouchEventsEnabled;
        }
        public void setCanPlayMoveAnimation(boolean enable) {
            if (enable) {
                privateFlags &= ~PRIVATE_FLAG_NO_MOVE_ANIMATION;
            } else {
                privateFlags |= PRIVATE_FLAG_NO_MOVE_ANIMATION;
            }
        }
        public boolean canPlayMoveAnimation() {
            return (privateFlags & PRIVATE_FLAG_NO_MOVE_ANIMATION) == 0;
        }
        public  int getFitInsetsTypes() {
            return mFitInsetsTypes;
        }
        public  int getFitInsetsSides() {
            return mFitInsetsSides;
        }
        public boolean isFitInsetsIgnoringVisibility() {
            return mFitInsetsIgnoringVisibility;
        }
        private void checkNonRecursiveParams() {
            if (paramsForRotation == null) {
                return;
            }
            for (int i = paramsForRotation.length - 1; i >= 0; i--) {
                if (paramsForRotation[i].paramsForRotation != null) {
                    throw new IllegalArgumentException(
                            "Params cannot contain params recursively.");
                }
            }
        }
        public LayoutParams forRotation(int rotation) {
            if (paramsForRotation == null || paramsForRotation.length <= rotation
                    || paramsForRotation[rotation] == null) {
                return this;
            }
            return paramsForRotation[rotation];
        }
        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            type = TYPE_APPLICATION;
            format = PixelFormat.OPAQUE;
        }
        public LayoutParams(int _type) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            type = _type;
            format = PixelFormat.OPAQUE;
        }
        public LayoutParams(int _type, int _flags) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            type = _type;
            flags = _flags;
            format = PixelFormat.OPAQUE;
        }
        public LayoutParams(int _type, int _flags, int _format) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            type = _type;
            flags = _flags;
            format = _format;
        }
        public LayoutParams(int w, int h, int _type, int _flags, int _format) {
            super(w, h);
            type = _type;
            flags = _flags;
            format = _format;
        }
        public LayoutParams(int w, int h, int xpos, int ypos, int _type,
                int _flags, int _format) {
            super(w, h);
            x = xpos;
            y = ypos;
            type = _type;
            flags = _flags;
            format = _format;
        }
        public final void setTitle(CharSequence title) {
            if (null == title)
                title = "";
            mTitle = TextUtils.stringOrSpannedString(title);
        }
        public final CharSequence getTitle() {
            return mTitle != null ? mTitle : "";
        }
        public final void setSurfaceInsets(View view, boolean manual, boolean preservePrevious) {
            final int surfaceInset = (int) Math.ceil(view.getZ() * 2);
            if (surfaceInset == 0) {
                surfaceInsets.set(0, 0, 0, 0);
            } else {
                surfaceInsets.set(
                        Math.max(surfaceInset, surfaceInsets.left),
                        Math.max(surfaceInset, surfaceInsets.top),
                        Math.max(surfaceInset, surfaceInsets.right),
                        Math.max(surfaceInset, surfaceInsets.bottom));
            }
            hasManualSurfaceInsets = manual;
            preservePreviousSurfaceInsets = preservePrevious;
        }
        public boolean isHdrConversionEnabled() {
            return ((mDisplayFlags & DISPLAY_FLAG_DISABLE_HDR_CONVERSION) == 0);
        }
        public void setHdrConversionEnabled(boolean enabled) {
            if (!enabled) {
                mDisplayFlags |= DISPLAY_FLAG_DISABLE_HDR_CONVERSION;
            } else {
                mDisplayFlags &= ~DISPLAY_FLAG_DISABLE_HDR_CONVERSION;
            }
        }
        public void setColorMode( int colorMode) {
            mColorMode = colorMode;
        }
        public int getColorMode() {
            return mColorMode;
        }
        public void setDesiredHdrHeadroom(
                 float desiredHeadroom) {
            if (!Float.isFinite(desiredHeadroom)) {
                throw new IllegalArgumentException("desiredHeadroom must be finite: "
                        + desiredHeadroom);
            }
            if (desiredHeadroom != 0 && (desiredHeadroom < 1.0f || desiredHeadroom > 10000.0f)) {
                throw new IllegalArgumentException(
                        "desiredHeadroom must be 0.0 or in the range [1.0, 10000.0f], received: "
                                + desiredHeadroom);
            }
            mDesiredHdrHeadroom = desiredHeadroom;
        }
        public float getDesiredHdrHeadroom() {
            return mDesiredHdrHeadroom;
        }
        public void setFrameRateBoostOnTouchEnabled(boolean enabled) {
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                mFrameRateBoostOnTouch = enabled;
            }
        }
        public boolean getFrameRateBoostOnTouchEnabled() {
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                return mFrameRateBoostOnTouch;
            }
            return true;
        }
        public void setFrameRatePowerSavingsBalanced(boolean enabled) {
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                mIsFrameRatePowerSavingsBalanced = enabled;
            }
        }
        public boolean isFrameRatePowerSavingsBalanced() {
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                return mIsFrameRatePowerSavingsBalanced;
            }
            return true;
        }
        public void setBlurBehindRadius( int blurBehindRadius) {
            mBlurBehindRadius = blurBehindRadius;
        }
        public int getBlurBehindRadius() {
            return mBlurBehindRadius;
        }
        public final void setUserActivityTimeout(long timeout) {
            userActivityTimeout = timeout;
        }
        public final long getUserActivityTimeout() {
            return userActivityTimeout;
        }
        public final void setWindowContextToken( IBinder token) {
            mWindowContextToken = token;
        }
        public final IBinder getWindowContextToken() {
            return mWindowContextToken;
        }
        public int describeContents() {
            return 0;
        }
        public void writeToParcel(Parcel out, int parcelableFlags) {
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(type);
            out.writeInt(flags);
            out.writeInt(privateFlags);
            out.writeInt(softInputMode);
            out.writeInt(layoutInDisplayCutoutMode);
            out.writeInt(gravity);
            out.writeFloat(horizontalMargin);
            out.writeFloat(verticalMargin);
            out.writeInt(format);
            out.writeInt(windowAnimations);
            out.writeFloat(alpha);
            out.writeFloat(dimAmount);
            out.writeFloat(screenBrightness);
            out.writeFloat(buttonBrightness);
            out.writeInt(rotationAnimation);
            out.writeStrongBinder(token);
            out.writeStrongBinder(mWindowContextToken);
            out.writeString(packageName);
            TextUtils.writeToParcel(mTitle, out, parcelableFlags);
            out.writeInt(screenOrientation);
            out.writeFloat(preferredRefreshRate);
            out.writeInt(preferredDisplayModeId);
            out.writeFloat(preferredMinDisplayRefreshRate);
            out.writeFloat(preferredMaxDisplayRefreshRate);
            out.writeInt(systemUiVisibility);
            out.writeInt(subtreeSystemUiVisibility);
            out.writeBoolean(hasSystemUiListeners);
            out.writeInt(inputFeatures);
            out.writeLong(userActivityTimeout);
            out.writeInt(surfaceInsets.left);
            out.writeInt(surfaceInsets.top);
            out.writeInt(surfaceInsets.right);
            out.writeInt(surfaceInsets.bottom);
            out.writeBoolean(hasManualSurfaceInsets);
            out.writeBoolean(receiveInsetsIgnoringZOrder);
            out.writeBoolean(preservePreviousSurfaceInsets);
            out.writeLong(accessibilityIdOfAnchor);
            TextUtils.writeToParcel(accessibilityTitle, out, parcelableFlags);
            out.writeInt(mColorMode);
            out.writeLong(hideTimeoutMilliseconds);
            out.writeInt(insetsFlags.appearance);
            out.writeInt(insetsFlags.behavior);
            out.writeInt(mFitInsetsTypes);
            out.writeInt(mFitInsetsSides);
            out.writeBoolean(mFitInsetsIgnoringVisibility);
            out.writeBoolean(preferMinimalPostProcessing);
            out.writeInt(mBlurBehindRadius);
            out.writeBoolean(mWallpaperTouchEventsEnabled);
            out.writeInt(forciblyShownTypes);
            checkNonRecursiveParams();
            out.writeTypedArray(paramsForRotation, 0 );
            out.writeInt(mDisplayFlags);
            out.writeFloat(mDesiredHdrHeadroom);
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                out.writeBoolean(mFrameRateBoostOnTouch);
                out.writeBoolean(mIsFrameRatePowerSavingsBalanced);
            }
        }
        public static final  Parcelable.Creator<LayoutParams> CREATOR
                    = new Parcelable.Creator<LayoutParams>() {
            public LayoutParams createFromParcel(Parcel in) {
                return new LayoutParams(in);
            }
            public LayoutParams[] newArray(int size) {
                return new LayoutParams[size];
            }
        };
        public LayoutParams(Parcel in) {
            width = in.readInt();
            height = in.readInt();
            x = in.readInt();
            y = in.readInt();
            type = in.readInt();
            flags = in.readInt();
            privateFlags = in.readInt();
            softInputMode = in.readInt();
            layoutInDisplayCutoutMode = in.readInt();
            gravity = in.readInt();
            horizontalMargin = in.readFloat();
            verticalMargin = in.readFloat();
            format = in.readInt();
            windowAnimations = in.readInt();
            alpha = in.readFloat();
            dimAmount = in.readFloat();
            screenBrightness = in.readFloat();
            buttonBrightness = in.readFloat();
            rotationAnimation = in.readInt();
            token = in.readStrongBinder();
            mWindowContextToken = in.readStrongBinder();
            packageName = in.readString();
            mTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            screenOrientation = in.readInt();
            preferredRefreshRate = in.readFloat();
            preferredDisplayModeId = in.readInt();
            preferredMinDisplayRefreshRate = in.readFloat();
            preferredMaxDisplayRefreshRate = in.readFloat();
            systemUiVisibility = in.readInt();
            subtreeSystemUiVisibility = in.readInt();
            hasSystemUiListeners = in.readBoolean();
            inputFeatures = in.readInt();
            userActivityTimeout = in.readLong();
            surfaceInsets.left = in.readInt();
            surfaceInsets.top = in.readInt();
            surfaceInsets.right = in.readInt();
            surfaceInsets.bottom = in.readInt();
            hasManualSurfaceInsets = in.readBoolean();
            receiveInsetsIgnoringZOrder = in.readBoolean();
            preservePreviousSurfaceInsets = in.readBoolean();
            accessibilityIdOfAnchor = in.readLong();
            accessibilityTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            mColorMode = in.readInt();
            hideTimeoutMilliseconds = in.readLong();
            insetsFlags.appearance = in.readInt();
            insetsFlags.behavior = in.readInt();
            mFitInsetsTypes = in.readInt();
            mFitInsetsSides = in.readInt();
            mFitInsetsIgnoringVisibility = in.readBoolean();
            preferMinimalPostProcessing = in.readBoolean();
            mBlurBehindRadius = in.readInt();
            mWallpaperTouchEventsEnabled = in.readBoolean();
            forciblyShownTypes = in.readInt();
            paramsForRotation = in.createTypedArray(LayoutParams.CREATOR);
            mDisplayFlags = in.readInt();
            mDesiredHdrHeadroom = in.readFloat();
            if (sToolkitSetFrameRateReadOnlyFlagValue) {
                mFrameRateBoostOnTouch = in.readBoolean();
                mIsFrameRatePowerSavingsBalanced = in.readBoolean();
            }
        }
        public static final int LAYOUT_CHANGED = 1<<0;
        public static final int TYPE_CHANGED = 1<<1;
        public static final int FLAGS_CHANGED = 1<<2;
        public static final int FORMAT_CHANGED = 1<<3;
        public static final int ANIMATION_CHANGED = 1<<4;
        public static final int DIM_AMOUNT_CHANGED = 1<<5;
        public static final int TITLE_CHANGED = 1<<6;
        public static final int ALPHA_CHANGED = 1<<7;
        public static final int MEMORY_TYPE_CHANGED = 1<<8;
        public static final int SOFT_INPUT_MODE_CHANGED = 1<<9;
        public static final int SCREEN_ORIENTATION_CHANGED = 1<<10;
        public static final int SCREEN_BRIGHTNESS_CHANGED = 1<<11;
        public static final int ROTATION_ANIMATION_CHANGED = 1<<12;
        public static final int BUTTON_BRIGHTNESS_CHANGED = 1<<13;
        public static final int SYSTEM_UI_VISIBILITY_CHANGED = 1<<14;
        public static final int SYSTEM_UI_LISTENER_CHANGED = 1<<15;
        public static final int INPUT_FEATURES_CHANGED = 1<<16;
        public static final int PRIVATE_FLAGS_CHANGED = 1<<17;
        public static final int USER_ACTIVITY_TIMEOUT_CHANGED = 1<<18;
        public static final int TRANSLUCENT_FLAGS_CHANGED = 1<<19;
        public static final int SURFACE_INSETS_CHANGED = 1<<20;
        public static final int PREFERRED_REFRESH_RATE_CHANGED = 1 << 21;
        public static final int DISPLAY_FLAGS_CHANGED = 1 << 22;
        public static final int PREFERRED_DISPLAY_MODE_ID = 1 << 23;
        public static final int ACCESSIBILITY_ANCHOR_CHANGED = 1 << 24;
        public static final int ACCESSIBILITY_TITLE_CHANGED = 1 << 25;
        public static final int COLOR_MODE_CHANGED = 1 << 26;
        public static final int INSET_FLAGS_CHANGED = 1 << 27;
        public static final int MINIMAL_POST_PROCESSING_PREFERENCE_CHANGED = 1 << 28;
        public static final int BLUR_BEHIND_RADIUS_CHANGED = 1 << 29;
        public static final int PREFERRED_MIN_DISPLAY_REFRESH_RATE = 1 << 30;
        public static final int PREFERRED_MAX_DISPLAY_REFRESH_RATE = 1 << 31;
        private int[] mCompatibilityParamsBackup = null;
        public final int copyFrom(LayoutParams o) {
            int changes = 0;
            if (width != o.width) {
                width = o.width;
                changes |= LAYOUT_CHANGED;
            }
            if (height != o.height) {
                height = o.height;
                changes |= LAYOUT_CHANGED;
            }
            if (x != o.x) {
                x = o.x;
                changes |= LAYOUT_CHANGED;
            }
            if (y != o.y) {
                y = o.y;
                changes |= LAYOUT_CHANGED;
            }
            if (horizontalWeight != o.horizontalWeight) {
                horizontalWeight = o.horizontalWeight;
                changes |= LAYOUT_CHANGED;
            }
            if (verticalWeight != o.verticalWeight) {
                verticalWeight = o.verticalWeight;
                changes |= LAYOUT_CHANGED;
            }
            if (horizontalMargin != o.horizontalMargin) {
                horizontalMargin = o.horizontalMargin;
                changes |= LAYOUT_CHANGED;
            }
            if (verticalMargin != o.verticalMargin) {
                verticalMargin = o.verticalMargin;
                changes |= LAYOUT_CHANGED;
            }
            if (type != o.type) {
                type = o.type;
                changes |= TYPE_CHANGED;
            }
            if (flags != o.flags) {
                final int diff = flags ^ o.flags;
                if ((diff & (FLAG_TRANSLUCENT_STATUS | FLAG_TRANSLUCENT_NAVIGATION)) != 0) {
                    changes |= TRANSLUCENT_FLAGS_CHANGED;
                }
                flags = o.flags;
                changes |= FLAGS_CHANGED;
            }
            if (privateFlags != o.privateFlags) {
                privateFlags = o.privateFlags;
                changes |= PRIVATE_FLAGS_CHANGED;
            }
            if (softInputMode != o.softInputMode) {
                softInputMode = o.softInputMode;
                changes |= SOFT_INPUT_MODE_CHANGED;
            }
            if (layoutInDisplayCutoutMode != o.layoutInDisplayCutoutMode) {
                layoutInDisplayCutoutMode = o.layoutInDisplayCutoutMode;
                changes |= LAYOUT_CHANGED;
            }
            if (gravity != o.gravity) {
                gravity = o.gravity;
                changes |= LAYOUT_CHANGED;
            }
            if (format != o.format) {
                format = o.format;
                changes |= FORMAT_CHANGED;
            }
            if (windowAnimations != o.windowAnimations) {
                windowAnimations = o.windowAnimations;
                changes |= ANIMATION_CHANGED;
            }
            if (token == null) {
                token = o.token;
            }
            if (mWindowContextToken == null) {
                mWindowContextToken = o.mWindowContextToken;
            }
            if (packageName == null) {
                packageName = o.packageName;
            }
            if (!Objects.equals(mTitle, o.mTitle) && o.mTitle != null) {
                mTitle = o.mTitle;
                changes |= TITLE_CHANGED;
            }
            if (alpha != o.alpha) {
                alpha = o.alpha;
                changes |= ALPHA_CHANGED;
            }
            if (dimAmount != o.dimAmount) {
                dimAmount = o.dimAmount;
                changes |= DIM_AMOUNT_CHANGED;
            }
            if (screenBrightness != o.screenBrightness) {
                screenBrightness = o.screenBrightness;
                changes |= SCREEN_BRIGHTNESS_CHANGED;
            }
            if (buttonBrightness != o.buttonBrightness) {
                buttonBrightness = o.buttonBrightness;
                changes |= BUTTON_BRIGHTNESS_CHANGED;
            }
            if (rotationAnimation != o.rotationAnimation) {
                rotationAnimation = o.rotationAnimation;
                changes |= ROTATION_ANIMATION_CHANGED;
            }
            if (screenOrientation != o.screenOrientation) {
                screenOrientation = o.screenOrientation;
                changes |= SCREEN_ORIENTATION_CHANGED;
            }
            if (preferredRefreshRate != o.preferredRefreshRate) {
                preferredRefreshRate = o.preferredRefreshRate;
                changes |= PREFERRED_REFRESH_RATE_CHANGED;
            }
            if (preferredDisplayModeId != o.preferredDisplayModeId) {
                preferredDisplayModeId = o.preferredDisplayModeId;
                changes |= PREFERRED_DISPLAY_MODE_ID;
            }
            if (preferredMinDisplayRefreshRate != o.preferredMinDisplayRefreshRate) {
                preferredMinDisplayRefreshRate = o.preferredMinDisplayRefreshRate;
                changes |= PREFERRED_MIN_DISPLAY_REFRESH_RATE;
            }
            if (preferredMaxDisplayRefreshRate != o.preferredMaxDisplayRefreshRate) {
                preferredMaxDisplayRefreshRate = o.preferredMaxDisplayRefreshRate;
                changes |= PREFERRED_MAX_DISPLAY_REFRESH_RATE;
            }
            if (mDisplayFlags != o.mDisplayFlags) {
                mDisplayFlags = o.mDisplayFlags;
                changes |= DISPLAY_FLAGS_CHANGED;
            }
            if (systemUiVisibility != o.systemUiVisibility
                    || subtreeSystemUiVisibility != o.subtreeSystemUiVisibility) {
                systemUiVisibility = o.systemUiVisibility;
                subtreeSystemUiVisibility = o.subtreeSystemUiVisibility;
                changes |= SYSTEM_UI_VISIBILITY_CHANGED;
            }
            if (hasSystemUiListeners != o.hasSystemUiListeners) {
                hasSystemUiListeners = o.hasSystemUiListeners;
                changes |= SYSTEM_UI_LISTENER_CHANGED;
            }
            if (inputFeatures != o.inputFeatures) {
                inputFeatures = o.inputFeatures;
                changes |= INPUT_FEATURES_CHANGED;
            }
            if (userActivityTimeout != o.userActivityTimeout) {
                userActivityTimeout = o.userActivityTimeout;
                changes |= USER_ACTIVITY_TIMEOUT_CHANGED;
            }
            if (!surfaceInsets.equals(o.surfaceInsets)) {
                surfaceInsets.set(o.surfaceInsets);
                changes |= SURFACE_INSETS_CHANGED;
            }
            if (hasManualSurfaceInsets != o.hasManualSurfaceInsets) {
                hasManualSurfaceInsets = o.hasManualSurfaceInsets;
                changes |= SURFACE_INSETS_CHANGED;
            }
            if (receiveInsetsIgnoringZOrder != o.receiveInsetsIgnoringZOrder) {
                receiveInsetsIgnoringZOrder = o.receiveInsetsIgnoringZOrder;
                changes |= SURFACE_INSETS_CHANGED;
            }
            if (preservePreviousSurfaceInsets != o.preservePreviousSurfaceInsets) {
                preservePreviousSurfaceInsets = o.preservePreviousSurfaceInsets;
                changes |= SURFACE_INSETS_CHANGED;
            }
            if (accessibilityIdOfAnchor != o.accessibilityIdOfAnchor) {
                accessibilityIdOfAnchor = o.accessibilityIdOfAnchor;
                changes |= ACCESSIBILITY_ANCHOR_CHANGED;
            }
            if (!Objects.equals(accessibilityTitle, o.accessibilityTitle)
                    && o.accessibilityTitle != null) {
                accessibilityTitle = o.accessibilityTitle;
                changes |= ACCESSIBILITY_TITLE_CHANGED;
            }
            if (mColorMode != o.mColorMode) {
                mColorMode = o.mColorMode;
                changes |= COLOR_MODE_CHANGED;
            }
            if (mDesiredHdrHeadroom != o.mDesiredHdrHeadroom) {
                mDesiredHdrHeadroom = o.mDesiredHdrHeadroom;
                changes |= COLOR_MODE_CHANGED;
            }
            if (preferMinimalPostProcessing != o.preferMinimalPostProcessing) {
                preferMinimalPostProcessing = o.preferMinimalPostProcessing;
                changes |= MINIMAL_POST_PROCESSING_PREFERENCE_CHANGED;
            }
            if (mBlurBehindRadius != o.mBlurBehindRadius) {
                mBlurBehindRadius = o.mBlurBehindRadius;
                changes |= BLUR_BEHIND_RADIUS_CHANGED;
            }
            hideTimeoutMilliseconds = o.hideTimeoutMilliseconds;
            if (insetsFlags.appearance != o.insetsFlags.appearance) {
                insetsFlags.appearance = o.insetsFlags.appearance;
                changes |= INSET_FLAGS_CHANGED;
            }
            if (insetsFlags.behavior != o.insetsFlags.behavior) {
                insetsFlags.behavior = o.insetsFlags.behavior;
                changes |= INSET_FLAGS_CHANGED;
            }
            if (mFitInsetsTypes != o.mFitInsetsTypes) {
                mFitInsetsTypes = o.mFitInsetsTypes;
                changes |= LAYOUT_CHANGED;
            }
            if (mFitInsetsSides != o.mFitInsetsSides) {
                mFitInsetsSides = o.mFitInsetsSides;
                changes |= LAYOUT_CHANGED;
            }
            if (mFitInsetsIgnoringVisibility != o.mFitInsetsIgnoringVisibility) {
                mFitInsetsIgnoringVisibility = o.mFitInsetsIgnoringVisibility;
                changes |= LAYOUT_CHANGED;
            }
            if (forciblyShownTypes != o.forciblyShownTypes) {
                forciblyShownTypes = o.forciblyShownTypes;
                changes |= PRIVATE_FLAGS_CHANGED;
            }
            if (paramsForRotation != o.paramsForRotation) {
                if ((changes & LAYOUT_CHANGED) == 0) {
                    if (paramsForRotation != null && o.paramsForRotation != null
                            && paramsForRotation.length == o.paramsForRotation.length) {
                        for (int i = paramsForRotation.length - 1; i >= 0; i--) {
                            if (hasLayoutDiff(paramsForRotation[i], o.paramsForRotation[i])) {
                                changes |= LAYOUT_CHANGED;
                                break;
                            }
                        }
                    } else {
                        changes |= LAYOUT_CHANGED;
                    }
                }
                paramsForRotation = o.paramsForRotation;
                checkNonRecursiveParams();
            }
            if (mWallpaperTouchEventsEnabled != o.mWallpaperTouchEventsEnabled) {
                mWallpaperTouchEventsEnabled = o.mWallpaperTouchEventsEnabled;
                changes |= LAYOUT_CHANGED;
            }
            if (sToolkitSetFrameRateReadOnlyFlagValue
                    && mFrameRateBoostOnTouch != o.mFrameRateBoostOnTouch) {
                mFrameRateBoostOnTouch = o.mFrameRateBoostOnTouch;
                changes |= LAYOUT_CHANGED;
            }
            if (sToolkitSetFrameRateReadOnlyFlagValue
                    && mIsFrameRatePowerSavingsBalanced != o.mIsFrameRatePowerSavingsBalanced) {
                mIsFrameRatePowerSavingsBalanced = o.mIsFrameRatePowerSavingsBalanced;
                changes |= LAYOUT_CHANGED;
            }
            return changes;
        }
        private static boolean hasLayoutDiff(LayoutParams a, LayoutParams b) {
            return a.width != b.width || a.height != b.height || a.x != b.x || a.y != b.y
                    || a.horizontalMargin != b.horizontalMargin
                    || a.verticalMargin != b.verticalMargin
                    || a.layoutInDisplayCutoutMode != b.layoutInDisplayCutoutMode
                    || a.gravity != b.gravity
                    || a.mFitInsetsTypes != b.mFitInsetsTypes
                    || a.mFitInsetsSides != b.mFitInsetsSides
                    || a.mFitInsetsIgnoringVisibility != b.mFitInsetsIgnoringVisibility;
        }
        public String debug(String output) {
            output += "Contents of " + this + ":";
            Log.d("Debug", output);
            output = "";
            Log.d("Debug", output);
            Log.d("Debug", "");
            Log.d("Debug", "WindowManager.LayoutParams={title=" + mTitle + "}");
            return "";
        }
        public String toString() {
            return toString("");
        }
        public void dumpDimensions(StringBuilder sb) {
            sb.append('(');
            sb.append(x);
            sb.append(',');
            sb.append(y);
            sb.append(")(");
            sb.append((width == MATCH_PARENT ? "fill" : (width == WRAP_CONTENT
                    ? "wrap" : String.valueOf(width))));
            sb.append('x');
            sb.append((height == MATCH_PARENT ? "fill" : (height == WRAP_CONTENT
                    ? "wrap" : String.valueOf(height))));
            sb.append(")");
        }
        /* AOSP LayoutParams.toString(String): the fields of the window one per line; the names of the flags
         * and types AOSP takes from ViewDebug's annotations are given as numbers */
        public String toString(String prefix) {
            StringBuilder sb = new StringBuilder(256);
            sb.append('{');
            dumpDimensions(sb);
            if (horizontalWeight != 0) {
                sb.append(" hwgt=");
                sb.append(horizontalWeight);
            }
            if (verticalWeight != 0) {
                sb.append(" vwgt=");
                sb.append(verticalWeight);
            }
            sb.append(" ty=").append(type);
            if (format != PixelFormat.OPAQUE) {
                sb.append(" fmt=").append(format);
            }
            if (windowAnimations != 0) {
                sb.append(" wanim=0x").append(Integer.toHexString(windowAnimations));
            }
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                sb.append(" or=").append(screenOrientation);
            }
            if (alpha != 1.0f) {
                sb.append(" alpha=").append(alpha);
            }
            if (screenBrightness != BRIGHTNESS_OVERRIDE_NONE) {
                sb.append(" sbrt=").append(screenBrightness);
            }
            if (buttonBrightness != BRIGHTNESS_OVERRIDE_NONE) {
                sb.append(" bbrt=").append(buttonBrightness);
            }
            if (dimAmount != 1.0f) {
                sb.append(" dim=").append(dimAmount);
            }
            if (mColorMode != 0 /* ActivityInfo.COLOR_MODE_DEFAULT */) {
                sb.append(" colorMode=").append(mColorMode);
            }
            if (softInputMode != 0) {
                sb.append(prefix).append("  sim=#").append(Integer.toHexString(softInputMode));
            }
            if (flags != 0) {
                sb.append(prefix).append("  fl=#").append(Integer.toHexString(flags));
            }
            if (privateFlags != 0) {
                sb.append(prefix).append("  pfl=0x").append(Integer.toHexString(privateFlags));
            }
            if (systemUiVisibility != 0) {
                sb.append(prefix).append("  sysui=0x").append(Integer.toHexString(systemUiVisibility));
            }
            if (subtreeSystemUiVisibility != 0) {
                sb.append(prefix).append("  vsysui=0x").append(Integer.toHexString(subtreeSystemUiVisibility));
            }
            if (insetsFlags.appearance != 0) {
                sb.append(prefix).append("  apr=0x").append(Integer.toHexString(insetsFlags.appearance));
            }
            if (insetsFlags.behavior != 0) {
                sb.append(prefix).append("  bhv=0x").append(Integer.toHexString(insetsFlags.behavior));
            }
            if (mFitInsetsTypes != 0) {
                sb.append(prefix).append("  fitTypes=0x").append(Integer.toHexString(mFitInsetsTypes));
            }
            if (mFitInsetsSides != Side.all()) {
                sb.append(prefix).append("  fitSides=0x").append(Integer.toHexString(mFitInsetsSides));
            }
            if (mFitInsetsIgnoringVisibility) {
                sb.append(prefix).append("  fitIgnoreVis");
            }
            sb.append('}');
            return sb.toString();
        }
        public void scale(float scale) {
            x = (int) (x * scale + 0.5f);
            y = (int) (y * scale + 0.5f);
            if (width > 0) {
                width = (int) (width * scale + 0.5f);
            }
            if (height > 0) {
                height = (int) (height * scale + 0.5f);
            }
        }
        void backup() {
            int[] backup = mCompatibilityParamsBackup;
            if (backup == null) {
                backup = mCompatibilityParamsBackup = new int[4];
            }
            backup[0] = x;
            backup[1] = y;
            backup[2] = width;
            backup[3] = height;
        }
        void restore() {
            int[] backup = mCompatibilityParamsBackup;
            if (backup != null) {
                x = backup[0];
                y = backup[1];
                width = backup[2];
                height = backup[3];
            }
        }
        private CharSequence mTitle = null;
        public boolean isFullscreen() {
            return x == 0 && y == 0
                    && width == WindowManager.LayoutParams.MATCH_PARENT
                    && height == WindowManager.LayoutParams.MATCH_PARENT;
        }
        private static String layoutInDisplayCutoutModeToString(
                 int mode) {
            switch (mode) {
                case LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT:
                    return "default";
                case LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS:
                    return "always";
                case LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER:
                    return "never";
                case LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES:
                    return "shortEdges";
                default:
                    return "unknown(" + mode + ")";
            }
        }
        private static String softInputModeToString( int softInputMode) {
            final StringBuilder result = new StringBuilder();
            final int state = softInputMode & SOFT_INPUT_MASK_STATE;
            if (state != 0) {
                result.append("state=");
                switch (state) {
                    case SOFT_INPUT_STATE_UNCHANGED:
                        result.append("unchanged");
                        break;
                    case SOFT_INPUT_STATE_HIDDEN:
                        result.append("hidden");
                        break;
                    case SOFT_INPUT_STATE_ALWAYS_HIDDEN:
                        result.append("always_hidden");
                        break;
                    case SOFT_INPUT_STATE_VISIBLE:
                        result.append("visible");
                        break;
                    case SOFT_INPUT_STATE_ALWAYS_VISIBLE:
                        result.append("always_visible");
                        break;
                    default:
                        result.append(state);
                        break;
                }
                result.append(' ');
            }
            final int adjust = softInputMode & SOFT_INPUT_MASK_ADJUST;
            if (adjust != 0) {
                result.append("adjust=");
                switch (adjust) {
                    case SOFT_INPUT_ADJUST_RESIZE:
                        result.append("resize");
                        break;
                    case SOFT_INPUT_ADJUST_PAN:
                        result.append("pan");
                        break;
                    case SOFT_INPUT_ADJUST_NOTHING:
                        result.append("nothing");
                        break;
                    default:
                        result.append(adjust);
                        break;
                }
                result.append(' ');
            }
            if ((softInputMode & SOFT_INPUT_IS_FORWARD_NAVIGATION) != 0) {
                result.append("forwardNavigation").append(' ');
            }
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        }
        private static String rotationAnimationToString(int rotationAnimation) {
            switch (rotationAnimation) {
                case ROTATION_ANIMATION_UNSPECIFIED:
                    return "UNSPECIFIED";
                case ROTATION_ANIMATION_ROTATE:
                    return "ROTATE";
                case ROTATION_ANIMATION_CROSSFADE:
                    return "CROSSFADE";
                case ROTATION_ANIMATION_JUMPCUT:
                    return "JUMPCUT";
                case ROTATION_ANIMATION_SEAMLESS:
                    return "SEAMLESS";
                default:
                    return Integer.toString(rotationAnimation);
            }
        }
        private static String inputFeaturesToString(int inputFeatures) {
            final List<String> features = new ArrayList<>();
            if ((inputFeatures & INPUT_FEATURE_NO_INPUT_CHANNEL) != 0) {
                inputFeatures &= ~INPUT_FEATURE_NO_INPUT_CHANNEL;
                features.add("INPUT_FEATURE_NO_INPUT_CHANNEL");
            }
            if ((inputFeatures & INPUT_FEATURE_DISABLE_USER_ACTIVITY) != 0) {
                inputFeatures &= ~INPUT_FEATURE_DISABLE_USER_ACTIVITY;
                features.add("INPUT_FEATURE_DISABLE_USER_ACTIVITY");
            }
            if ((inputFeatures & INPUT_FEATURE_SPY) != 0) {
                inputFeatures &= ~INPUT_FEATURE_SPY;
                features.add("INPUT_FEATURE_SPY");
            }
            if (inputFeatures != 0) {
                features.add(Integer.toHexString(inputFeatures));
            }
            return String.join(" | ", features);
        }
        public boolean isModal() {
            return (flags & (FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE)) == 0;
        }
    }
}
