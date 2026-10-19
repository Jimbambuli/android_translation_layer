package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;

public class DrawableContainer extends Drawable {

	private DrawableContainerState state;
	private int curIndex = -1;

	protected native long native_constructor();
	protected native void native_selectChild(long container, long child);

	public DrawableContainer() {
		setPaintable(native_constructor());
	}

	public boolean selectDrawable(int idx) {
		if (idx >= 0 && idx < state.childCount && idx != curIndex && state.drawables[idx] != null) {
			curIndex = idx;
			Drawable d = state.drawables[idx];
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
		if (curIndex >= 0 && state.drawables[curIndex].isStateful())
			return state.drawables[curIndex].setState(stateSet);
		return false;
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		for (int i = 0; i < state.childCount; i++) {
			if (state.drawables[i] != null)
				state.drawables[i].setColorFilter(colorFilter);
		}
	}

	protected void setConstantState(DrawableContainerState state) {
		this.state = state;
	}

	@Override
	public ConstantState getConstantState() {
		return state;
	}

	public static class DrawableContainerState extends ConstantState {

		private Drawable drawables[] = new Drawable[10];
		private int childCount = 0;
		private DrawableContainer owner;

		public DrawableContainerState(DrawableContainerState orig, DrawableContainer owner, Resources res) {
			this.owner = owner;
			if (orig != null) {
				drawables = orig.drawables.clone();
				childCount = orig.childCount;
			}
		}

		public int getCapacity() {
			return drawables.length;
		}

		public int getChildCount() {
			return childCount;
		}

		public Drawable[] getChildren() {
			return drawables;
		}

		public Drawable getChild(int idx) {
			return drawables[idx];
		}

		public int addChild(Drawable dr) {
			if (childCount >= drawables.length) {
				growArray(drawables.length, drawables.length * 2);
			}
			drawables[childCount] = dr;
			return childCount++;
		}

		public void growArray(int oldSize, int newSize) {
			Drawable[] newDrawables = new Drawable[newSize];
			System.arraycopy(drawables, 0, newDrawables, 0, oldSize);
			drawables = newDrawables;
		}

		@Override
		public Drawable newDrawable(Resources res) {
			return owner;
		}

		@Override
		public Drawable newDrawable() {
			return owner;
		}

		@Override
		public int getChangingConfigurations() {
			return owner.getChangingConfigurations();
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (curIndex != -1)
			state.drawables[curIndex].draw(canvas);
	}

	@Override
	public int getIntrinsicHeight() {
		return curIndex != -1 ? state.drawables[curIndex].getIntrinsicHeight() : -1;
	}

	@Override
	public int getIntrinsicWidth() {
		return curIndex != -1 ? state.drawables[curIndex].getIntrinsicWidth() : -1;
	}

	/* like AOSP, the container keeps its bounds and gives them to the child it selects */
	@Override
	protected void onBoundsChange(Rect bounds) {
		if (curIndex != -1)
			state.drawables[curIndex].setBounds(bounds);
	}

	public void setEnterFadeDuration(int duration) {}

	public void setExitFadeDuration(int duration) {}
}
