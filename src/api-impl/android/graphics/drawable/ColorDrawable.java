package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;

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
}
