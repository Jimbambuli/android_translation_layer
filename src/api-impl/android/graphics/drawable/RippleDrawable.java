package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Outline;
import com.android.internal.R;

public class RippleDrawable extends LayerDrawable {

	public RippleDrawable(ColorStateList colorStateList, Drawable drawable, Drawable drawable2) {
		super(drawable == null ? new Drawable[] {} : new Drawable[] {drawable});
	}

	RippleDrawable() {}

	public void setColor(ColorStateList colorStateList) {}

	public void setRadius(int radius) {}

	public int getRadius() {
		return 0;
	}

	/* like AOSP: the outline of the first layer that is not the mask */
	@Override
	public void getOutline(Outline outline) {
		final ChildDrawable[] array = mLayerState.mChildren;
		final int N = mLayerState.mNum;
		for (int i = 0; i < N; i++) {
			if (array[i].mId != R.id.mask) {
				array[i].mDrawable.getOutline(outline);
				if (!outline.isEmpty())
					return;
			}
		}
	}

	@Override
	public void draw(Canvas canvas) {
		final ChildDrawable[] array = mLayerState.mChildren;
		final int N = mLayerState.mNum;
		for (int i = 0; i < N; i++) {
			if (array[i].mId != R.id.mask)
				array[i].mDrawable.draw(canvas);
		}
	}
}
