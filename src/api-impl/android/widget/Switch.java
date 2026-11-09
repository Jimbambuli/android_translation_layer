package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import com.android.internal.R;

/* AOSP Switch (android-14): the thumb and the track are the drawables of the switchStyle/thumb/track attributes
 * (Material: a 14dp track and a round thumb), drawn by the view itself; the GtkSwitch of CompoundButton stays
 * as the holder of the checked state only (CompoundButton hides it for a class that overrides onDraw()).
 * Not ported: showText (switch texts), the thumb animation and dragging the thumb, a click toggles. */
public class Switch extends CompoundButton {
	private Drawable mThumbDrawable;
	private Drawable mTrackDrawable;
	private int mThumbTextPadding;
	private int mSwitchMinWidth;
	private int mSwitchPadding;
	private boolean mSplitTrack;

	private float mThumbPosition;
	private int mSwitchWidth;
	private int mSwitchHeight;
	private int mThumbWidth;
	private int mSwitchLeft;
	private int mSwitchTop;
	private int mSwitchRight;
	private int mSwitchBottom;
	private final Rect mTempRect = new Rect();

	private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};

	public Switch(Context context) {
		this(context, null);
	}

	public Switch(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.switchStyle);
	}

	public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Switch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Switch, defStyleAttr, defStyleRes);
		mThumbDrawable = a.getDrawable(R.styleable.Switch_thumb);
		if (mThumbDrawable != null)
			mThumbDrawable.setCallback(this);
		mTrackDrawable = a.getDrawable(R.styleable.Switch_track);
		if (mTrackDrawable != null)
			mTrackDrawable.setCallback(this);
		mThumbTextPadding = a.getDimensionPixelSize(R.styleable.Switch_thumbTextPadding, 0);
		mSwitchMinWidth = a.getDimensionPixelSize(R.styleable.Switch_switchMinWidth, 0);
		mSwitchPadding = a.getDimensionPixelSize(R.styleable.Switch_switchPadding, 0);
		mSplitTrack = a.getBoolean(R.styleable.Switch_splitTrack, false);
		a.recycle();
		refreshDrawableState();
		mThumbPosition = isChecked() ? 1 : 0;
	}

	public void setTextOn(CharSequence text) {}
	public void setTextOff(CharSequence text) {}

	public void setThumbDrawable(Drawable thumb) {
		if (mThumbDrawable != null)
			mThumbDrawable.setCallback(null);
		mThumbDrawable = thumb;
		if (thumb != null)
			thumb.setCallback(this);
		requestLayout();
	}

	public Drawable getThumbDrawable() {
		return mThumbDrawable;
	}

	public void setTrackDrawable(Drawable track) {
		if (mTrackDrawable != null)
			mTrackDrawable.setCallback(null);
		mTrackDrawable = track;
		if (track != null)
			track.setCallback(this);
		requestLayout();
	}

	public Drawable getTrackDrawable() {
		return mTrackDrawable;
	}

	public void setSwitchMinWidth(int pixels) {
		mSwitchMinWidth = pixels;
		requestLayout();
	}

	public int getSwitchMinWidth() {
		return mSwitchMinWidth;
	}

	public void setSwitchPadding(int pixels) {
		mSwitchPadding = pixels;
		requestLayout();
	}

	public int getSwitchPadding() {
		return mSwitchPadding;
	}

	public void setThumbTextPadding(int pixels) {
		mThumbTextPadding = pixels;
		requestLayout();
	}

	public int getThumbTextPadding() {
		return mThumbTextPadding;
	}

	public void setSplitTrack(boolean splitTrack) {
		mSplitTrack = splitTrack;
		invalidate();
	}

	public boolean getSplitTrack() {
		return mSplitTrack;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final Rect padding = mTempRect;
		final int thumbWidth;
		final int thumbHeight;
		if (mThumbDrawable != null) {
			mThumbDrawable.getPadding(padding);
			thumbWidth = mThumbDrawable.getIntrinsicWidth() - padding.left - padding.right;
			thumbHeight = mThumbDrawable.getIntrinsicHeight();
		} else {
			thumbWidth = 0;
			thumbHeight = 0;
		}
		mThumbWidth = thumbWidth;

		final int trackHeight;
		if (mTrackDrawable != null) {
			mTrackDrawable.getPadding(padding);
			trackHeight = mTrackDrawable.getIntrinsicHeight();
		} else {
			padding.setEmpty();
			trackHeight = 0;
		}
		final int switchWidth = Math.max(mSwitchMinWidth, 2 * mThumbWidth + padding.left + padding.right);
		final int switchHeight = Math.max(trackHeight, thumbHeight);
		mSwitchWidth = switchWidth;
		mSwitchHeight = switchHeight;

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int measuredHeight = getMeasuredHeight();
		if (measuredHeight < switchHeight)
			setMeasuredDimension(getMeasuredWidthAndState(), switchHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		final int switchRight;
		final int switchLeft;
		if (isLayoutRtl()) {
			switchLeft = getPaddingLeft();
			switchRight = switchLeft + mSwitchWidth;
		} else {
			switchRight = getWidth() - getPaddingRight();
			switchLeft = switchRight - mSwitchWidth;
		}

		final int switchTop;
		final int switchBottom;
		switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
			default:
			case Gravity.TOP:
				switchTop = getPaddingTop();
				switchBottom = switchTop + mSwitchHeight;
				break;
			case Gravity.CENTER_VERTICAL:
				switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 - mSwitchHeight / 2;
				switchBottom = switchTop + mSwitchHeight;
				break;
			case Gravity.BOTTOM:
				switchBottom = getHeight() - getPaddingBottom();
				switchTop = switchBottom - mSwitchHeight;
				break;
		}
		mSwitchLeft = switchLeft;
		mSwitchTop = switchTop;
		mSwitchBottom = switchBottom;
		mSwitchRight = switchRight;
	}

	@Override
	public void draw(Canvas c) {
		final Rect padding = mTempRect;
		int thumbInitialLeft = mSwitchLeft + getThumbOffset();
		if (mTrackDrawable != null) {
			mTrackDrawable.getPadding(padding);
			thumbInitialLeft += padding.left;
			mTrackDrawable.setBounds(mSwitchLeft, mSwitchTop, mSwitchRight, mSwitchBottom);
		}
		if (mThumbDrawable != null) {
			mThumbDrawable.getPadding(padding);
			final int thumbLeft = thumbInitialLeft - padding.left;
			final int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
			mThumbDrawable.setBounds(thumbLeft, mSwitchTop, thumbRight, mSwitchBottom);
		}
		super.draw(c);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mTrackDrawable != null) {
			if (mSplitTrack && mThumbDrawable != null) {
				final Rect padding = mTempRect;
				mThumbDrawable.copyBounds(padding);
				final int saveCount = canvas.save();
				canvas.clipRect(padding, android.graphics.Region.Op.DIFFERENCE);
				mTrackDrawable.draw(canvas);
				canvas.restoreToCount(saveCount);
			} else {
				mTrackDrawable.draw(canvas);
			}
		}
		if (mThumbDrawable != null)
			mThumbDrawable.draw(canvas);
	}

	@Override
	public int getCompoundPaddingRight() {
		if (isLayoutRtl())
			return super.getCompoundPaddingRight();
		int padding = super.getCompoundPaddingRight() + mSwitchWidth;
		if (!TextUtils.isEmpty(getText()))
			padding += mSwitchPadding;
		return padding;
	}

	@Override
	public int getCompoundPaddingLeft() {
		if (!isLayoutRtl())
			return super.getCompoundPaddingLeft();
		int padding = super.getCompoundPaddingLeft() + mSwitchWidth;
		if (!TextUtils.isEmpty(getText()))
			padding += mSwitchPadding;
		return padding;
	}

	private int getThumbScrollRange() {
		if (mTrackDrawable == null)
			return 0;
		final Rect padding = mTempRect;
		mTrackDrawable.getPadding(padding);
		return mSwitchWidth - mThumbWidth - padding.left - padding.right;
	}

	private int getThumbOffset() {
		final float thumbPosition = isLayoutRtl() ? 1 - mThumbPosition : mThumbPosition;
		return (int)(thumbPosition * getThumbScrollRange() + 0.5f);
	}

	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		mThumbPosition = checked ? 1 : 0;
		refreshDrawableState();
		invalidate();
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
		final int[] state = getDrawableState();
		boolean changed = false;
		final Drawable thumbDrawable = mThumbDrawable;
		if (thumbDrawable != null && thumbDrawable.isStateful())
			changed |= thumbDrawable.setState(state);
		final Drawable trackDrawable = mTrackDrawable;
		if (trackDrawable != null && trackDrawable.isStateful())
			changed |= trackDrawable.setState(state);
		if (changed)
			invalidate();
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
	}

	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (mThumbDrawable != null)
			mThumbDrawable.jumpToCurrentState();
		if (mTrackDrawable != null)
			mTrackDrawable.jumpToCurrentState();
	}
}
