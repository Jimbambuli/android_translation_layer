package android.widget;

import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import com.android.internal.R;

public class SeekBar extends AbsSeekBar {

	public SeekBar(Context context) {
		this(context, null);
	}

	public SeekBar(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, 0);
	}

	/* AppCompatSeekBar calls this one */
	public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setColorsFromTheme(context);
	}

	/* like Material's seekbar: thumb and progress colorControlActivated, the rest of the
	 * track colorControlNormal at disabledAlpha */
	private void setColorsFromTheme(Context context) {
		/* sorted by id, as obtainStyledAttributes() expects */
		TypedArray a = context.obtainStyledAttributes(new int[] {R.attr.disabledAlpha, R.attr.colorControlNormal,
		                                                         R.attr.colorControlActivated});
		float alpha = a.getFloat(0, 0.3f);
		int normal = a.getColor(1, 0xff757575);
		int activated = a.getColor(2, 0xff009688);
		a.recycle();
		int track = (Math.round(Color.alpha(normal) * alpha) << 24) | (normal & 0xffffff);
		native_setColors(widget, activated, track);
	}

	private native void native_setColors(long widget, int activated, int track);

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	@Override
	protected native void native_setProgress(long widget, float fraction);
	protected native int native_getProgress(long widget);
	protected native void native_setMax(long widget, int max);
	@Override
	public void native_setIndeterminate(boolean indeterminate) {}

	@Override
	public void setMax(int max) {
		this.max = max;
		native_setMax(widget, max);
	}
	@Override
	public void setProgress(int progress) {
		this.progress = progress;
		native_setProgress(widget, progress);
	}

	public native void setOnSeekBarChangeListener(final OnSeekBarChangeListener l);

	@Override
	public int getProgress() {
		return native_getProgress(widget);
	}

	public static interface OnSeekBarChangeListener {
		@Export
		public void onStartTrackingTouch(SeekBar seekBar);

		@Export
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);

		@Export
		public void onStopTrackingTouch(SeekBar seekBar);
	}
}
