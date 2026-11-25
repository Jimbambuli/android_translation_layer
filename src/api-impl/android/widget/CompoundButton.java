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

	private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};

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
		/* AOSP: the button drawable comes from the style; ToggleButton has none and shows only its text */
		final Drawable d = typedArray.getDrawable(R.styleable.CompoundButton_button);
		if (d != null) {
			setButtonDrawable(d);
		}
		if (typedArray.hasValue(R.styleable.CompoundButton_checked)) {
			this.setChecked(typedArray.getBoolean(R.styleable.CompoundButton_checked, false));
		}
		typedArray.recycle();
		setSwitchColorsFromTheme(context);
		/* AndroidX SwitchCompat/MaterialSwitch and ATL's own Switch draw thumb and track in onDraw() like AOSP's Switch;
		 * the GtkSwitch would be drawn under them, so it stays as state holder only. The same without a button
		 * drawable (ToggleButton), where AOSP's onDraw() draws none. */
		mDrawsItself = (overridesOnDraw(getClass()) || button_drawable == null) && !(this instanceof CheckBox || this instanceof RadioButton);
		if (mDrawsItself)
			gtk(() -> native_hideSwitch(widget));
	}

	private boolean mDrawsItself;

	private static boolean overridesOnDraw(Class<?> cls) {
		for (; cls != null && (cls == Switch.class || !cls.getName().startsWith("android.widget.")); cls = cls.getSuperclass()) {
			try {
				cls.getDeclaredMethod("onDraw", android.graphics.Canvas.class);
				return true;
			} catch (NoSuchMethodException e) {
			}
		}
		return false;
	}

	/* hides the GtkSwitch (CheckBox and RadioButton have none) */
	private native boolean native_hideSwitch(long widget);

	/* AOSP's TextView makes room for the compound paddings, through which SwitchCompat reserves the
	 * width of the switch it draws; the native measurement only knows the label */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!mDrawsItself)
			return;
		int extra = getCompoundPaddingLeft() - getPaddingLeft() + getCompoundPaddingRight() - getPaddingRight();
		if (extra > 0)
			setMeasuredDimension(resolveSizeAndState(getMeasuredWidth() + extra, widthMeasureSpec, 0), getMeasuredHeightAndState());
	}

	/* AOSP: a click toggles the button. The shown GtkSwitch toggles itself; a switch the app
	 * draws gets its clicks through onTouchEvent() -> performClick() like on AOSP, which clicks
	 * only clickable views (the switch of a SwitchPreference is not: the row toggles it) */
	@Override
	public boolean performClick() {
		if (mDrawsItself && isClickable())
			toggle();
		return super.performClick();
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
		gtk(() -> native_setSwitchColors(widget, thumb, activated, withAlpha(foreground, alpha), withAlpha(activated, alpha)));
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

	/* the state of the native widget; kept here too for isChecked() off the main thread */
	private boolean mChecked;

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		gtk(() -> native_setChecked(checked));
	}

	@Override
	public boolean isChecked() {
		return gtkReady() ? native_isChecked() : mChecked;
	}

	protected native void native_setChecked(boolean checked);
	protected native boolean native_isChecked();

	@Override
	/* Copyright (C) 2007 The Android Open Source Project */
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	@Override
	/* Copyright (C) 2007 The Android Open Source Project */
	protected void drawableStateChanged() {
		super.drawableStateChanged();

		final Drawable buttonDrawable = button_drawable;
		if (buttonDrawable != null && buttonDrawable.isStateful()
		    && buttonDrawable.setState(getDrawableState())) {
			invalidateDrawable(buttonDrawable);
		}
	}

	public void toggle() {
		setChecked(!isChecked());
	}

	// following methods are overridden to prevent calling incompatible methods from superclasses
	@Override
	public void setOnClickListener(final OnClickListener l) {}
	@Override
	public void setTextColor(int color) {}
	@Override
	protected void applyNativeTextSize(float size) {}
	/* the text as set, like AOSP's TextView.mText; no initializer, as Button's constructor sets the
	 * text from the attributes before the fields of this class are initialized */
	private CharSequence mText;

	@Override
	public void setText(CharSequence text) {
		mText = text == null ? "" : text;
		gtk(() -> native_setText(widget, mText.toString()));
	}

	@Override
	public CharSequence getText() {
		return mText == null ? "" : mText;
	}

	/* AOSP CompoundButton.setButtonTintList()/setButtonTintMode(): the tint of the button drawable, kept and applied to it */
	private ColorStateList mButtonTintList;
	private PorterDuff.Mode mButtonTintMode;

	public void setButtonTintList(ColorStateList list) {
		mButtonTintList = list;
		applyButtonTint();
	}

	public void setButtonTintMode(PorterDuff.Mode tintMode) {
		mButtonTintMode = tintMode;
		applyButtonTint();
	}

	private void applyButtonTint() {
		if (button_drawable != null && (mButtonTintList != null || mButtonTintMode != null)) {
			button_drawable = button_drawable.mutate();
			if (mButtonTintList != null)
				button_drawable.setTintList(mButtonTintList);
			if (mButtonTintMode != null)
				button_drawable.setTintMode(mButtonTintMode);
		}
	}

	public void setButtonDrawable(Drawable drawable) {
		button_drawable = drawable;
		applyButtonTint();
	}

	/* AOSP CompoundButton.setButtonDrawable(int): resource 0 clears the drawable; the same resource is not reloaded */
	private int mButtonResource;

	public void setButtonDrawable(int resId) {
		final int resource = resId != 0 ? resId : 0;
		if (resource != 0 && resource == mButtonResource) {
			return;
		}
		mButtonResource = resource;
		Drawable d = null;
		if (mButtonResource != 0) {
			d = getContext().getDrawable(mButtonResource);
		}
		setButtonDrawable(d);
	}

	public Drawable getButtonDrawable() {
		return button_drawable;
	}

	public ColorStateList getButtonTintList() {
		return mButtonTintList;
	}

	@Override
	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {}

	public PorterDuff.Mode getButtonTintMode() {
		return mButtonTintMode;
	}
}
