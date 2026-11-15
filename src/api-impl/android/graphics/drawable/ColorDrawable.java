package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ColorDrawable extends Drawable {

	private int color;
	/* AOSP ColorState.mUseColor: the base colour modulated by setAlpha() */
	private int useColor;
	private boolean mMutated;
	private Paint paint;

	public ColorDrawable() {
		this.paint = new Paint();
	}

	public ColorDrawable(int color) {
		this.color = color;
		this.useColor = color;
		this.paint = new Paint();
		this.paint.setColor(color);
	}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ColorDrawable);
		setColor(a.getColor(R.styleable.ColorDrawable_color, color));
		a.recycle();
	}

	/* AOSP ColorDrawable.getColor(): the colour with setAlpha() applied */
	public int getColor() {
		return useColor;
	}

	/* AOSP ColorDrawable.draw(): nothing is drawn for a fully transparent colour (and no colour filter) */
	@Override
	public void draw(Canvas canvas) {
		if ((useColor >>> 24) != 0 || paint.getColorFilter() != null) {
			paint.setColor(useColor);
			canvas.drawRect(getBounds(), paint);
		}
	}

	/* AOSP ColorDrawable.getAlpha() */
	@Override
	public int getAlpha() {
		return useColor >>> 24;
	}

	/* AOSP ColorDrawable.setAlpha(): applied to the alpha of the base colour */
	@Override
	public void setAlpha(int alpha) {
		alpha += alpha >> 7; // make it 0..256
		final int baseAlpha = color >>> 24;
		final int useAlpha = baseAlpha * alpha >> 8;
		final int newUseColor = (color << 8 >>> 8) | (useAlpha << 24);
		if (useColor != newUseColor) {
			useColor = newUseColor;
			invalidateSelf();
		}
	}

	/* AOSP ColorDrawable.getOpacity() */
	@Override
	public int getOpacity() {
		if (paint.getColorFilter() != null)
			return PixelFormat.TRANSLUCENT;
		switch (useColor >>> 24) {
			case 255:
				return PixelFormat.OPAQUE;
			case 0:
				return PixelFormat.TRANSPARENT;
		}
		return PixelFormat.TRANSLUCENT;
	}

	/* AOSP ColorDrawable.mutate(): the state (colour) is held per instance here already, so only the flag */
	@Override
	public Drawable mutate() {
		if (!mMutated && super.mutate() == this) {
			mMutated = true;
		}
		return this;
	}

	@Override
	public void getOutline(Outline outline) {
		outline.setRect(getBounds());
		outline.setAlpha(Color.alpha(useColor) / 255.0f);
	}

	/* AOSP setColor(): clobbers the effect of earlier setAlpha() calls */
	public void setColor(int color) {
		if (this.color != color || useColor != color) {
			this.color = this.useColor = color;
			paint.setColor(color);
			invalidateSelf();
		}
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
