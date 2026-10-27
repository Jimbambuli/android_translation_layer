package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import com.android.internal.R;

/**
 * A TextView that implements {@link Checkable} and shows an optional check
 * mark drawable, as used by the single and multiple choice lists of dialogs.
 *
 * The check mark takes the place of the end (or, with checkMarkGravity start,
 * the left) compound drawable; a compound drawable set on the same side is not
 * shown while a check mark is set.
 */
public class CheckedTextView extends TextView implements Checkable {
	private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};

	private boolean mChecked;
	private Drawable mCheckMarkDrawable;
	private ColorStateList mCheckMarkTintList;
	private PorterDuff.Mode mCheckMarkTintMode;
	private int mCheckMarkGravity = Gravity.END;

	public CheckedTextView(Context context) {
		this(context, null);
	}

	public CheckedTextView(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.checkedTextViewStyle);
	}

	public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckedTextView, defStyleAttr, defStyleRes);
		mCheckMarkGravity = a.getInt(R.styleable.CheckedTextView_checkMarkGravity, Gravity.END);
		if (a.hasValue(R.styleable.CheckedTextView_checkMarkTint))
			mCheckMarkTintList = a.getColorStateList(R.styleable.CheckedTextView_checkMarkTint);
		final Drawable d = a.getDrawable(R.styleable.CheckedTextView_checkMark);
		if (d != null)
			setCheckMarkDrawable(d);
		setChecked(a.getBoolean(R.styleable.CheckedTextView_checked, false));
		a.recycle();
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			refreshDrawableState();
		}
	}

	public void setCheckMarkDrawable(int resId) {
		setCheckMarkDrawable(resId != 0 ? getContext().getDrawable(resId) : null);
	}

	public void setCheckMarkDrawable(Drawable d) {
		if (mCheckMarkDrawable != null)
			mCheckMarkDrawable.setCallback(null);
		mCheckMarkDrawable = d;
		if (d != null) {
			d.setCallback(this);
			if (mCheckMarkTintList != null)
				d.setTintList(mCheckMarkTintList);
			if (mCheckMarkTintMode != null)
				d.setTintMode(mCheckMarkTintMode);
			if (d.isStateful())
				d.setState(getDrawableState());
		}
		updateCheckMark();
	}

	public Drawable getCheckMarkDrawable() {
		return mCheckMarkDrawable;
	}

	public void setCheckMarkTintList(ColorStateList tint) {
		mCheckMarkTintList = tint;
		if (mCheckMarkDrawable != null)
			mCheckMarkDrawable.setTintList(tint);
	}

	public ColorStateList getCheckMarkTintList() {
		return mCheckMarkTintList;
	}

	public void setCheckMarkTintMode(PorterDuff.Mode tintMode) {
		mCheckMarkTintMode = tintMode;
		if (mCheckMarkDrawable != null)
			mCheckMarkDrawable.setTintMode(tintMode);
	}

	public PorterDuff.Mode getCheckMarkTintMode() {
		return mCheckMarkTintMode;
	}

	@Override
	protected void updateNativeCompoundDrawables() {
		updateCheckMark();
	}

	private void updateCheckMark() {
		Drawable[] d = getCompoundDrawables();
		long left = paintable(d[0]);
		long right = paintable(d[2]);
		if (mCheckMarkDrawable != null) {
			int gravity = Gravity.getAbsoluteGravity(mCheckMarkGravity, getLayoutDirection());
			if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT)
				left = mCheckMarkDrawable.paintable;
			else
				right = mCheckMarkDrawable.paintable;
		}
		native_setCompoundDrawables(widget, left, paintable(d[1]), right, paintable(d[3]));
	}

	private static long paintable(Drawable d) {
		return d != null ? d.paintable : 0;
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked())
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		return drawableState;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mCheckMarkDrawable != null && mCheckMarkDrawable.isStateful())
			mCheckMarkDrawable.setState(getDrawableState());
	}
}
