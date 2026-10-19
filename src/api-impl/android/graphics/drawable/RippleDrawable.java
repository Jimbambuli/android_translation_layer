package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RippleDrawable extends LayerDrawable {

	private ColorStateList color;
	private boolean focused;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

	public RippleDrawable(ColorStateList colorStateList, Drawable drawable, Drawable drawable2) {
		super(drawable == null ? new Drawable[] {} : new Drawable[] {drawable});
		color = colorStateList;
	}

	RippleDrawable() {}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.RippleDrawable);
		ColorStateList c = a.getColorStateList(R.styleable.RippleDrawable_color);
		if (c != null)
			color = c;
		a.recycle();
		super.inflate(r, parser, attrs, theme);
	}

	public void setColor(ColorStateList colorStateList) {
		color = colorStateList;
		invalidateSelf();
	}

	public void setRadius(int radius) {}

	public int getRadius() {
		return 0;
	}

	@Override
	public boolean isStateful() {
		return true;
	}

	/* the ripple animations are not drawn, but like AOSP's RippleBackground the focused state is
	 * shown as a highlight in the ripple color */
	@Override
	protected boolean onStateChange(int[] stateSet) {
		boolean focused = false;
		for (int state : stateSet) {
			if (state == R.attr.state_focused)
				focused = true;
		}
		if (focused == this.focused)
			return false;
		this.focused = focused;
		invalidateSelf();
		return true;
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
		if (focused && color != null) {
			paint.setColor(color.getColorForState(getState(), color.getDefaultColor()));
			Rect bounds = getBounds();
			if (N == 0) // borderless: a circle around the center
				canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), Math.max(bounds.width(), bounds.height()) / 2f, paint);
			else
				canvas.drawRect(bounds, paint);
		}
	}
}
