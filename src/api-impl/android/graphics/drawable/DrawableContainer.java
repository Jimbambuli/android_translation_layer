package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.util.SparseArray;

public class DrawableContainer extends Drawable {

	private static final boolean DEFAULT_DITHER = true;
	private DrawableContainerState state;
	private int curIndex = -1;
	private boolean mMutated;
	/* AOSP DrawableContainer.mAlpha/mHasAlpha: the alpha set on the container, given to each child shown */
	private int mAlpha = 0xFF;
	private boolean mHasAlpha;

	protected native long native_constructor();
	protected native void native_selectChild(long container, long child);

	public DrawableContainer() {
		setPaintable(native_constructor());
	}

	public boolean selectDrawable(int idx) {
		if (idx >= 0 && idx < state.childCount && idx != curIndex && state.getChild(idx) != null) {
			curIndex = idx;
			Drawable d = state.getChild(idx);
			initializeDrawableForDisplay(d);
			native_selectChild(paintable, d.paintable);
			invalidateSelf();
			return true;
		}
		return false;
	}

	@Override
	public Drawable getCurrent() {
		return curIndex >= 0 ? state.getChild(curIndex) : null;
	}

	/** @hide */
	public int getCurrentIndex() {
		return curIndex;
	}

	/* the parts of AOSP's DrawableContainer that are not about drawing (ATL draws the selected
	 * child with a native paintable and has no fade animations, the durations are only kept) */
	@Override
	public void setDither(boolean dither) {
		if (state.mDither != dither) {
			state.mDither = dither;
			if (curIndex >= 0 && state.getChild(curIndex) != null) {
				state.getChild(curIndex).setDither(state.mDither);
			}
		}
	}

	@Override
	public void setAutoMirrored(boolean mirrored) {
		if (state.mAutoMirrored != mirrored) {
			state.mAutoMirrored = mirrored;
			if (curIndex >= 0 && state.getChild(curIndex) != null) {
				state.getChild(curIndex).setAutoMirrored(state.mAutoMirrored);
			}
		}
	}

	@Override
	public boolean isAutoMirrored() {
		return state.mAutoMirrored;
	}

	@Override
	public void jumpToCurrentState() {
		if (curIndex >= 0 && state.getChild(curIndex) != null) {
			state.getChild(curIndex).jumpToCurrentState();
		}
	}

	@Override
	public boolean setVisible(boolean visible, boolean restart) {
		boolean changed = super.setVisible(visible, restart);
		if (curIndex >= 0 && state.getChild(curIndex) != null) {
			state.getChild(curIndex).setVisible(visible, restart);
		}
		return changed;
	}

	/**
	 * @param res the resources used to inflate density-dependent values
	 * @hide
	 */
	protected final void updateDensity(Resources res) {
		state.updateDensity(res);
	}

	/**
	 * @hide
	 */
	public void clearMutated() {
		super.clearMutated();
		state.clearMutated();
		mMutated = false;
	}

	@Override
	public Drawable mutate() {
		if (!mMutated && super.mutate() == this) {
			final DrawableContainerState clone = cloneConstantState();
			clone.mutate();
			setConstantState(clone);
			mMutated = true;
		}
		return this;
	}

	/**
	 * Returns a shallow copy of the container's constant state to be used as
	 * the base state for {@link #mutate()}.
	 *
	 * @return a shallow copy of the constant state
	 */
	DrawableContainerState cloneConstantState() {
		return state;
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		// let the current child follow the state too, e.g. a tint with a selector
		if (curIndex >= 0 && state.getChild(curIndex).isStateful())
			return state.getChild(curIndex).setState(stateSet);
		return false;
	}

	/**
	 * Initializes a drawable for display in this container, like AOSP (without the alpha of the
	 * fade, the hotspot and the block of the invalidation callback, which ATL does not have).
	 *
	 * @param d The drawable to initialize.
	 */
	private void initializeDrawableForDisplay(Drawable d) {
		if (state.mHasColorFilter) {
			// Color filter always overrides tint.
			d.setColorFilter(state.mColorFilter);
		} else {
			if (state.mHasTintList) {
				d.setTintList(state.mTintList);
			}
			if (state.mHasTintMode) {
				d.setTintMode(state.mTintMode);
			}
		}
		// AOSP selectDrawable(): without an enter fade (ATL has none) the child gets the container's alpha
		if (mHasAlpha) {
			d.setAlpha(mAlpha);
		}
		d.setVisible(isVisible(), true);
		d.setDither(state.mDither);
		d.setState(getState());
		d.setLevel(getLevel());
		d.setBounds(getBounds());
		d.setLayoutDirection(getLayoutDirection());
		d.setAutoMirrored(state.mAutoMirrored);
	}

	/* AOSP DrawableContainer.setAlpha() without the fade animation */
	@Override
	public void setAlpha(int alpha) {
		if (!mHasAlpha || mAlpha != alpha) {
			mHasAlpha = true;
			mAlpha = alpha;
			if (getCurrent() != null) {
				getCurrent().setAlpha(alpha);
			}
		}
	}

	@Override
	public int getAlpha() {
		return mAlpha;
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		state.mHasColorFilter = true;

		if (state.mColorFilter != colorFilter) {
			state.mColorFilter = colorFilter;

			if (getCurrent() != null) {
				getCurrent().setColorFilter(colorFilter);
			}
		}
	}

	@Override
	public void setTintList(ColorStateList tint) {
		state.mHasTintList = true;

		if (state.mTintList != tint) {
			state.mTintList = tint;

			if (getCurrent() != null) {
				getCurrent().setTintList(tint);
			}
		}
	}

	@Override
	public void setTintMode(PorterDuff.Mode tintMode) {
		state.mHasTintMode = true;

		if (state.mTintMode != tintMode) {
			state.mTintMode = tintMode;

			if (getCurrent() != null) {
				getCurrent().setTintMode(tintMode);
			}
		}
	}

	private boolean needsMirroring() {
		return isAutoMirrored() && getLayoutDirection() == LayoutDirection.RTL;
	}

	/* AOSP DrawableContainer.getPadding(): the largest padding of all children, or with
	 * variablePadding the padding of the child shown */
	@Override
	public boolean getPadding(Rect padding) {
		final Rect r = state.getConstantPadding();
		boolean result;
		if (r != null) {
			padding.set(r);
			result = (r.left | r.top | r.bottom | r.right) != 0;
		} else {
			if (getCurrent() != null) {
				result = getCurrent().getPadding(padding);
			} else {
				result = super.getPadding(padding);
			}
		}
		if (needsMirroring()) {
			final int left = padding.left;
			final int right = padding.right;
			padding.left = right;
			padding.right = left;
		}
		return result;
	}

	@Override
	public Insets getOpticalInsets() {
		if (curIndex >= 0 && state.getChild(curIndex) != null)
			return state.getChild(curIndex).getOpticalInsets();
		return Insets.NONE;
	}

	protected void setConstantState(DrawableContainerState state) {
		this.state = state;

		// The locally cached drawables may have changed.
		if (curIndex >= 0) {
			Drawable d = state.getChild(curIndex);
			if (d != null) {
				initializeDrawableForDisplay(d);
				native_selectChild(paintable, d.paintable);
			}
		}
	}

	@Override
	public ConstantState getConstantState() {
		if (state.canConstantState()) {
			state.mChangingConfigurations = getChangingConfigurations();
			return state;
		}
		return null;
	}

	public abstract static class DrawableContainerState extends ConstantState {

		private Drawable drawables[];
		private int childCount;
		private DrawableContainer owner;
		private Resources mSourceRes;
		private SparseArray<ConstantState> mDrawableFutures;
		int mDensity = DisplayMetrics.DENSITY_DEFAULT;
		int mChangingConfigurations;
		int mChildrenChangingConfigurations;
		private boolean mCheckedConstantState;
		private boolean mCanConstantState;
		boolean mMutated;

		boolean mVariablePadding = false;
		boolean mCheckedPadding;
		Rect mConstantPadding;
		boolean mConstantSize = false;
		boolean mDither = DEFAULT_DITHER;
		boolean mAutoMirrored;
		int mEnterFadeDuration = 0;
		int mExitFadeDuration = 0;

		ColorFilter mColorFilter;
		boolean mHasColorFilter;

		ColorStateList mTintList;
		PorterDuff.Mode mTintMode;
		boolean mHasTintList;
		boolean mHasTintMode;

		/* like AOSP: a copy gets new instances of the children, made from their constant states
		 * when they are first used */
		protected DrawableContainerState(DrawableContainerState orig, DrawableContainer owner, Resources res) {
			this.owner = owner;
			mSourceRes = res != null ? res : (orig != null ? orig.mSourceRes : null);

			mDensity = Drawable.resolveDensity(res, orig != null ? orig.mDensity : 0);

			if (orig != null) {
				mChangingConfigurations = orig.mChangingConfigurations;
				mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;

				mVariablePadding = orig.mVariablePadding;
				mConstantSize = orig.mConstantSize;
				mDither = orig.mDither;
				mAutoMirrored = orig.mAutoMirrored;
				mEnterFadeDuration = orig.mEnterFadeDuration;
				mExitFadeDuration = orig.mExitFadeDuration;
				mColorFilter = orig.mColorFilter;
				mHasColorFilter = orig.mHasColorFilter;
				mTintList = orig.mTintList;
				mTintMode = orig.mTintMode;
				mHasTintList = orig.mHasTintList;
				mHasTintMode = orig.mHasTintMode;

				if (orig.mDensity == mDensity) {
					if (orig.mCheckedPadding) {
						mConstantPadding = orig.mConstantPadding != null ? new Rect(orig.mConstantPadding) : null;
						mCheckedPadding = true;
					}
				}

				mCheckedConstantState = true;
				mCanConstantState = true;

				// Postpone cloning children and futures until we're absolutely
				// sure that we're done computing values for the original state.
				final Drawable[] origDr = orig.drawables;
				drawables = new Drawable[origDr.length];
				childCount = orig.childCount;

				final SparseArray<ConstantState> origDf = orig.mDrawableFutures;
				if (origDf != null) {
					mDrawableFutures = origDf.clone();
				} else {
					mDrawableFutures = new SparseArray<>(childCount);
				}

				// Create futures for drawables with constant states. If a
				// drawable doesn't have a constant state, then we can't clone
				// it and we'll have to reference the original.
				final int N = childCount;
				for (int i = 0; i < N; i++) {
					if (origDr[i] != null) {
						final ConstantState cs = origDr[i].getConstantState();
						if (cs != null) {
							mDrawableFutures.put(i, cs);
						} else {
							drawables[i] = origDr[i];
						}
					}
				}
			} else {
				drawables = new Drawable[10];
				childCount = 0;
			}
		}

		@Override
		public int getChangingConfigurations() {
			return mChangingConfigurations | mChildrenChangingConfigurations;
		}

		public int getCapacity() {
			return drawables.length;
		}

		public int getChildCount() {
			return childCount;
		}

		private void createAllFutures() {
			if (mDrawableFutures != null) {
				final int futureCount = mDrawableFutures.size();
				for (int keyIndex = 0; keyIndex < futureCount; keyIndex++) {
					final int index = mDrawableFutures.keyAt(keyIndex);
					final ConstantState cs = mDrawableFutures.valueAt(keyIndex);
					drawables[index] = prepareDrawable(cs.newDrawable(mSourceRes));
				}

				mDrawableFutures = null;
			}
		}

		/* AOSP also sets the owner as callback; this DrawableContainer is no Drawable.Callback
		 * and addChild() does not set it either */
		private Drawable prepareDrawable(Drawable child) {
			child = child.mutate();
			return child;
		}

		public Drawable[] getChildren() {
			// Create all futures for backwards compatibility.
			createAllFutures();

			return drawables;
		}

		public Drawable getChild(int index) {
			final Drawable result = drawables[index];
			if (result != null) {
				return result;
			}

			// Prepare future drawable if necessary.
			if (mDrawableFutures != null) {
				final int keyIndex = mDrawableFutures.indexOfKey(index);
				if (keyIndex >= 0) {
					final ConstantState cs = mDrawableFutures.valueAt(keyIndex);
					final Drawable prepared = prepareDrawable(cs.newDrawable(mSourceRes));
					drawables[index] = prepared;
					mDrawableFutures.removeAt(keyIndex);
					if (mDrawableFutures.size() == 0) {
						mDrawableFutures = null;
					}
					return prepared;
				}
			}

			return null;
		}

		public int addChild(Drawable dr) {
			if (childCount >= drawables.length) {
				growArray(drawables.length, drawables.length * 2);
			}
			drawables[childCount] = dr;
			mChildrenChangingConfigurations |= dr.getChangingConfigurations();
			mConstantPadding = null;
			mCheckedPadding = false;
			mCheckedConstantState = false;
			return childCount++;
		}

		/**
		 * Updates the source density based on the resources used to inflate
		 * density-dependent values.
		 *
		 * @param res the resources used to inflate density-dependent values
		 */
		final void updateDensity(Resources res) {
			if (res != null) {
				mSourceRes = res;

				// The density may have changed since the last update (if any). Any
				// dimension-type attributes will need their default values scaled.
				final int targetDensity = Drawable.resolveDensity(res, mDensity);
				final int sourceDensity = mDensity;
				mDensity = targetDensity;

				if (sourceDensity != targetDensity) {
					mCheckedPadding = false;
				}
			}
		}

		private void mutate() {
			// No need to call createAllFutures, since future drawables will
			// mutate when they are prepared.
			final int N = childCount;
			final Drawable[] drawables = this.drawables;
			for (int i = 0; i < N; i++) {
				if (drawables[i] != null) {
					drawables[i].mutate();
				}
			}

			mMutated = true;
		}

		final void clearMutated() {
			final int N = childCount;
			final Drawable[] drawables = this.drawables;
			for (int i = 0; i < N; i++) {
				if (drawables[i] != null) {
					drawables[i].clearMutated();
				}
			}

			mMutated = false;
		}

		/**
		 * A boolean value indicating whether to use the maximum padding value
		 * of all frames in the set (false), or to use the padding value of the
		 * frame being shown (true). Default value is false.
		 */
		public final void setVariablePadding(boolean variable) {
			mVariablePadding = variable;
		}

		public final Rect getConstantPadding() {
			if (mVariablePadding) {
				return null;
			}

			if ((mConstantPadding != null) || mCheckedPadding) {
				return mConstantPadding;
			}

			createAllFutures();

			Rect r = null;
			final Rect t = new Rect();
			final int N = childCount;
			final Drawable[] drawables = this.drawables;
			for (int i = 0; i < N; i++) {
				if (drawables[i].getPadding(t)) {
					if (r == null) r = new Rect(0, 0, 0, 0);
					if (t.left > r.left) r.left = t.left;
					if (t.top > r.top) r.top = t.top;
					if (t.right > r.right) r.right = t.right;
					if (t.bottom > r.bottom) r.bottom = t.bottom;
				}
			}

			mCheckedPadding = true;
			return (mConstantPadding = r);
		}

		public final void setConstantSize(boolean constant) {
			mConstantSize = constant;
		}

		public final boolean isConstantSize() {
			return mConstantSize;
		}

		public final void setEnterFadeDuration(int duration) {
			mEnterFadeDuration = duration;
		}

		public final int getEnterFadeDuration() {
			return mEnterFadeDuration;
		}

		public final void setExitFadeDuration(int duration) {
			mExitFadeDuration = duration;
		}

		public final int getExitFadeDuration() {
			return mExitFadeDuration;
		}

		public void growArray(int oldSize, int newSize) {
			Drawable[] newDrawables = new Drawable[newSize];
			System.arraycopy(drawables, 0, newDrawables, 0, oldSize);
			drawables = newDrawables;
		}

		public synchronized boolean canConstantState() {
			if (mCheckedConstantState) {
				return mCanConstantState;
			}

			createAllFutures();

			mCheckedConstantState = true;

			final int N = childCount;
			final Drawable[] drawables = this.drawables;
			for (int i = 0; i < N; i++) {
				if (drawables[i].getConstantState() == null) {
					mCanConstantState = false;
					return false;
				}
			}

			mCanConstantState = true;
			return true;
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (curIndex != -1)
			state.getChild(curIndex).draw(canvas);
	}

	@Override
	public int getIntrinsicHeight() {
		return curIndex != -1 ? state.getChild(curIndex).getIntrinsicHeight() : -1;
	}

	@Override
	public int getIntrinsicWidth() {
		return curIndex != -1 ? state.getChild(curIndex).getIntrinsicWidth() : -1;
	}

	/* like AOSP, the container keeps its bounds and gives them to the child it selects */
	@Override
	protected void onBoundsChange(Rect bounds) {
		if (curIndex != -1)
			state.getChild(curIndex).setBounds(bounds);
	}

	/**
	 * Change the global fade duration when a new drawable is entering
	 * the scene.
	 *
	 * @param ms The amount of time to fade in milliseconds.
	 */
	public void setEnterFadeDuration(int ms) {
		state.mEnterFadeDuration = ms;
	}

	/**
	 * Change the global fade duration when a new drawable is leaving
	 * the scene.
	 *
	 * @param ms The amount of time to fade in milliseconds.
	 */
	public void setExitFadeDuration(int ms) {
		state.mExitFadeDuration = ms;
	}
}
