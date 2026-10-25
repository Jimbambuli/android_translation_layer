package android.widget;

import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.android.internal.R;

public abstract class CompoundButton extends Button implements Checkable {
	Drawable button_drawable = null;
	public Drawable mButtonDrawable; // directly accessed by androidx

	public CompoundButton(Context context) {
		this(context, null);
	}

	public CompoundButton(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, 0);
	}

	public CompoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CompoundButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		TypedArray typedArray = context.obtainStyledAttributes(
		    attrs, R.styleable.CompoundButton, defStyleAttr, defStyleRes);
		if (typedArray.hasValue(R.styleable.CompoundButton_checked)) {
			this.setChecked(typedArray.getBoolean(R.styleable.CompoundButton_checked, false));
		}
		typedArray.recycle();
		setSwitchColorsFromTheme(context);
	}

	/* like Material's switch_thumb_material and switch_track_material: the thumb is
	 * colorSwitchThumbNormal (colorControlActivated when checked), the track colorForeground
	 * (colorControlActivated) at disabledAlpha */
	private void setSwitchColorsFromTheme(Context context) {
		/* sorted by id, as obtainStyledAttributes() expects */
		TypedArray a = context.obtainStyledAttributes(new int[] {R.attr.colorForeground, R.attr.disabledAlpha,
		                                                         R.attr.colorControlActivated, R.attr.colorSwitchThumbNormal});
		int foreground = a.getColor(0, 0xff000000);
		float alpha = a.getFloat(1, 0.3f);
		int activated = a.getColor(2, 0xff009688);
		int thumb = a.getColor(3, 0xfff1f1f1);
		a.recycle();
		native_setSwitchColors(widget, thumb, activated, withAlpha(foreground, alpha), withAlpha(activated, alpha));
	}

	private static int withAlpha(int color, float alpha) {
		return (Math.round(Color.alpha(color) * alpha) << 24) | (color & 0xffffff);
	}

	private native void native_setSwitchColors(long widget, int thumb, int thumbChecked, int track, int trackChecked);

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	@Override
	public native void native_setText(long widget, String text);

	public static interface OnCheckedChangeListener {
		@Export
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
	}

	/* kept here as on AOSP; the native side holds no reference to it, a global reference there
	 * would keep the listener, and with it usually this view, alive forever */
	private OnCheckedChangeListener mOnCheckedChangeListener;

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mOnCheckedChangeListener = listener;
	}

	@Override
	public native void setChecked(boolean checked);

	@Override
	public native boolean isChecked();

	public void toggle() {
		setChecked(!isChecked());
	}

	// following methods are overridden to prevent calling incompatible methods from superclasses
	@Override
	public void setOnClickListener(final OnClickListener l) {}
	@Override
	public void setTextColor(int color) {}
	@Override
	public void setTextSize(float size) {}
	/* the text as set, like AOSP's TextView.mText; no initializer, as Button's constructor sets the
	 * text from the attributes before the fields of this class are initialized */
	private CharSequence mText;

	@Override
	public void setText(CharSequence text) {
		mText = text == null ? "" : text;
		native_setText(widget, mText.toString());
	}

	@Override
	public CharSequence getText() {
		return mText == null ? "" : mText;
	}

	public void setButtonTintList(ColorStateList list) {
	}

	public void setButtonDrawable(Drawable drawable) {
		button_drawable = drawable;
	}

	public Drawable getButtonDrawable() {
		return button_drawable;
	}

	public ColorStateList getButtonTintList() {
		return null;
	}

	@Override
	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {}

	public PorterDuff.Mode getButtonTintMode() {
		return null;
	}
}
