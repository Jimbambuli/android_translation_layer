package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import java.util.ArrayList;

/* AOSP android.graphics.drawable.AnimatedImageDrawable. AOSP's ImageDecoder makes one for an animated GIF, WebP or
 * HEIF; ATL's ImageDecoder decodes every image as a still (ImageInfo.isAnimated() is false), so the only
 * AnimatedImageDrawable is the empty one of the public constructor, which behaves as AOSP's empty one. */
public class AnimatedImageDrawable extends Drawable implements Animatable2 {

	public static final int REPEAT_INFINITE = -1;
	@Deprecated
	public static final int LOOP_INFINITE = REPEAT_INFINITE;
	private static final int REPEAT_UNDEFINED = -2;

	private int mRepeatCount = REPEAT_UNDEFINED;
	private int mAlpha = 255;
	private ColorFilter mColorFilter;
	private ArrayList<Animatable2.AnimationCallback> mAnimationCallbacks = null;

	public AnimatedImageDrawable() {}

	public void setRepeatCount(int repeatCount) {
		if (repeatCount < REPEAT_INFINITE) {
			throw new IllegalArgumentException("invalid value passed to setRepeatCount" + repeatCount);
		}
		mRepeatCount = repeatCount;
	}

	@Deprecated
	public void setLoopCount(int loopCount) {
		setRepeatCount(loopCount);
	}

	public int getRepeatCount() {
		throw new IllegalStateException("called getRepeatCount on empty AnimatedImageDrawable");
	}

	@Deprecated
	public int getLoopCount(int loopCount) {
		return getRepeatCount();
	}

	@Override
	public int getIntrinsicWidth() {
		return 0;
	}

	@Override
	public int getIntrinsicHeight() {
		return 0;
	}

	@Override
	public void draw(Canvas canvas) {
		throw new IllegalStateException("called draw on empty AnimatedImageDrawable");
	}

	@Override
	public void setAlpha(int alpha) {
		if (alpha < 0 || alpha > 255) {
			throw new IllegalArgumentException("Alpha must be between 0 and 255! provided " + alpha);
		}
		mAlpha = alpha;
	}

	@Override
	public int getAlpha() {
		return mAlpha;
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		mColorFilter = colorFilter;
	}

	@Override
	public ColorFilter getColorFilter() {
		return mColorFilter;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public boolean isRunning() {
		throw new IllegalStateException("called isRunning on empty AnimatedImageDrawable");
	}

	@Override
	public void start() {
		throw new IllegalStateException("called start on empty AnimatedImageDrawable");
	}

	@Override
	public void stop() {
		throw new IllegalStateException("called stop on empty AnimatedImageDrawable");
	}

	@Override
	public void registerAnimationCallback(Animatable2.AnimationCallback callback) {
		if (callback == null) {
			return;
		}
		if (mAnimationCallbacks == null) {
			mAnimationCallbacks = new ArrayList<Animatable2.AnimationCallback>();
		}
		if (!mAnimationCallbacks.contains(callback)) {
			mAnimationCallbacks.add(callback);
		}
	}

	@Override
	public boolean unregisterAnimationCallback(Animatable2.AnimationCallback callback) {
		if (callback == null || mAnimationCallbacks == null || !mAnimationCallbacks.remove(callback)) {
			return false;
		}
		if (mAnimationCallbacks.isEmpty()) {
			clearAnimationCallbacks();
		}
		return true;
	}

	@Override
	public void clearAnimationCallbacks() {
		mAnimationCallbacks = null;
	}
}
