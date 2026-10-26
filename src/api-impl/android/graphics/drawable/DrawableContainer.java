package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Rect;
import android.util.SparseArray;

public class DrawableContainer extends Drawable {

	private DrawableContainerState state;
	private int curIndex = -1;

	protected native long native_constructor();
	protected native void native_selectChild(long container, long child);

	public DrawableContainer() {
		setPaintable(native_constructor());
	}

	public boolean selectDrawable(int idx) {
		if (idx >= 0 && idx < state.childCount && idx != curIndex && state.getChild(idx) != null) {
			curIndex = idx;
			Drawable d = state.getChild(idx);
			if (d.isStateful())
				d.setState(getState());
			d.setBounds(getBounds());
			native_selectChild(paintable, d.paintable);
			invalidateSelf();
			return true;
		}
		return false;
	}

	@Override
	protected boolean onStateChange(int[] stateSet) {
		// let the current child follow the state too, e.g. a tint with a selector
		if (curIndex >= 0 && state.getChild(curIndex).isStateful())
			return state.getChild(curIndex).setState(stateSet);
		return false;
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		for (int i = 0; i < state.childCount; i++) {
			if (state.getChild(i) != null)
				state.getChild(i).setColorFilter(colorFilter);
		}
	}

	@Override
	public Insets getOpticalInsets() {
		if (curIndex >= 0 && state.getChild(curIndex) != null)
			return state.getChild(curIndex).getOpticalInsets();
		return Insets.NONE;
	}

	protected void setConstantState(DrawableContainerState state) {
		this.state = state;
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
		int mChangingConfigurations;
		int mChildrenChangingConfigurations;
		private boolean mCheckedConstantState;
		private boolean mCanConstantState;

		/* like AOSP: a copy gets new instances of the children, made from their constant states
		 * when they are first used */
		protected DrawableContainerState(DrawableContainerState orig, DrawableContainer owner, Resources res) {
			this.owner = owner;
			mSourceRes = res != null ? res : (orig != null ? orig.mSourceRes : null);

			if (orig != null) {
				mChangingConfigurations = orig.mChangingConfigurations;
				mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;

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
			mCheckedConstantState = false;
			return childCount++;
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

	public void setEnterFadeDuration(int duration) {}

	public void setExitFadeDuration(int duration) {}
}
