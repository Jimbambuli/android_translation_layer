package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ColorDrawable extends Drawable {

	private int color;
	private Paint paint;

	public ColorDrawable() {
		this.paint = new Paint();
	}

	public ColorDrawable(int color) {
		this.color = color;
		this.paint = new Paint();
		this.paint.setColor(color);
	}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ColorDrawable);
		setColor(a.getColor(R.styleable.ColorDrawable_color, color));
		a.recycle();
	}

	public int getColor() {
		return color;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawRect(getBounds(), paint);
	}

	@Override
	public void getOutline(Outline outline) {
		outline.setRect(getBounds());
		outline.setAlpha(Color.alpha(color) / 255.0f);
	}

	public void setColor(int color) {
		this.color = color;
		paint.setColor(color);
	}

	/* like AOSP's ColorState: what a new ColorDrawable needs, taken when asked for */
	@Override
	public ConstantState getConstantState() {
		return new ColorState(color);
	}

	final static class ColorState extends ConstantState {
		int mBaseColor;

		ColorState(int color) {
			mBaseColor = color;
		}

		@Override
		public Drawable newDrawable() {
			return new ColorDrawable(mBaseColor);
		}

		@Override
		public Drawable newDrawable(Resources res) {
			return new ColorDrawable(mBaseColor);
		}

		@Override
		public int getChangingConfigurations() {
			return 0;
		}
	}
}
