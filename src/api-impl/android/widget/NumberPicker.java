package android.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import java.util.Locale;

/**
 * android-14 frameworks/base core/java/android/widget/NumberPicker.java in its layout without selector wheel
 * (AOSP's legacy R.layout.number_picker, mHasSelectorWheel = false): an increment button, the value and a
 * decrement button, which change the value by one (AOSP changeValueByOne()). Value range, wrapping,
 * displayed values, formatter and listener follow AOSP. Not ported: the drawn selector wheel with fling and
 * the editable input field (the value is shown in a TextView); the buttons carry "+"/"-" instead of the
 * numberpicker_up/down drawables.
 */
public class NumberPicker extends LinearLayout {

	/* AOSP SELECTOR_WHEEL_ITEM_COUNT, the length of mSelectorIndices */
	private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;

	public interface OnValueChangeListener {
		void onValueChange(NumberPicker picker, int oldVal, int newVal);
	}

	public interface OnScrollListener {
		public static int SCROLL_STATE_IDLE = 0;
		public static int SCROLL_STATE_TOUCH_SCROLL = 1;
		public static int SCROLL_STATE_FLING = 2;

		public void onScrollStateChange(NumberPicker view, int scrollState);
	}

	public interface Formatter {
		public String format(int value);
	}

	private final Button mIncrementButton;
	private final Button mDecrementButton;
	private final TextView mInputText;
	private String[] mDisplayedValues;
	private int mMinValue;
	private int mMaxValue;
	private int mValue;
	private OnValueChangeListener mOnValueChangeListener;
	private OnScrollListener mOnScrollListener;
	private Formatter mFormatter;
	private long mLongPressUpdateInterval = 300;
	private boolean mWrapSelectorWheel;
	private boolean mWrapSelectorWheelPreferred = true;

	public NumberPicker(Context context) {
		this(context, null);
	}

	public NumberPicker(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.numberPickerStyle);
	}

	public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setOrientation(VERTICAL);
		mIncrementButton = new Button(context);
		mIncrementButton.setText("+");
		mIncrementButton.setOnClickListener(v -> changeValueByOne(true));
		mInputText = new TextView(context);
		mInputText.setGravity(Gravity.CENTER);
		mDecrementButton = new Button(context);
		mDecrementButton.setText("-");
		mDecrementButton.setOnClickListener(v -> changeValueByOne(false));
		addView(mIncrementButton, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addView(mInputText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addView(mDecrementButton, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		updateInputTextView();
	}

	public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
		mOnValueChangeListener = onValueChangedListener;
	}

	public void setOnScrollListener(OnScrollListener onScrollListener) {
		mOnScrollListener = onScrollListener;
	}

	public void setFormatter(Formatter formatter) {
		if (formatter == mFormatter) {
			return;
		}
		mFormatter = formatter;
		updateInputTextView();
	}

	public void setValue(int value) {
		setValueInternal(value, false);
	}

	public int getValue() {
		return mValue;
	}

	public int getMinValue() {
		return mMinValue;
	}

	public void setMinValue(int minValue) {
		if (mMinValue == minValue) {
			return;
		}
		if (minValue < 0) {
			throw new IllegalArgumentException("minValue must be >= 0");
		}
		mMinValue = minValue;
		if (mMinValue > mValue) {
			mValue = mMinValue;
		}
		updateWrapSelectorWheel();
		updateInputTextView();
	}

	public int getMaxValue() {
		return mMaxValue;
	}

	public void setMaxValue(int maxValue) {
		if (mMaxValue == maxValue) {
			return;
		}
		if (maxValue < 0) {
			throw new IllegalArgumentException("maxValue must be >= 0");
		}
		mMaxValue = maxValue;
		if (mMaxValue < mValue) {
			mValue = mMaxValue;
		}
		updateWrapSelectorWheel();
		updateInputTextView();
	}

	public boolean getWrapSelectorWheel() {
		return mWrapSelectorWheel;
	}

	public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
		mWrapSelectorWheelPreferred = wrapSelectorWheel;
		updateWrapSelectorWheel();
	}

	private void updateWrapSelectorWheel() {
		final boolean wrappingAllowed = (mMaxValue - mMinValue) >= SELECTOR_WHEEL_ITEM_COUNT;
		mWrapSelectorWheel = wrappingAllowed && mWrapSelectorWheelPreferred;
	}

	public void setOnLongPressUpdateInterval(long intervalMillis) {
		mLongPressUpdateInterval = intervalMillis;
	}

	public String[] getDisplayedValues() {
		return mDisplayedValues;
	}

	public void setDisplayedValues(String[] displayedValues) {
		if (mDisplayedValues == displayedValues) {
			return;
		}
		mDisplayedValues = displayedValues;
		updateInputTextView();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mIncrementButton.setEnabled(enabled);
		mDecrementButton.setEnabled(enabled);
		mInputText.setEnabled(enabled);
	}

	public void setSelectionDividerHeight(int height) {}

	public int getSelectionDividerHeight() {
		return 0;
	}

	public void setTextColor(int color) {
		mInputText.setTextColor(color);
	}

	public int getTextColor() {
		return mInputText.getCurrentTextColor();
	}

	public void setTextSize(float size) {
		mInputText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
	}

	public float getTextSize() {
		return mInputText.getTextSize();
	}

	private void setValueInternal(int current, boolean notifyChange) {
		if (mValue == current) {
			return;
		}
		if (mWrapSelectorWheel) {
			current = getWrappedSelectorIndex(current);
		} else {
			current = Math.max(current, mMinValue);
			current = Math.min(current, mMaxValue);
		}
		int previous = mValue;
		mValue = current;
		updateInputTextView();
		if (notifyChange) {
			notifyChange(previous, current);
		}
	}

	/* AOSP changeValueByOne() without selector wheel */
	private void changeValueByOne(boolean increment) {
		if (increment) {
			setValueInternal(mValue + 1, true);
		} else {
			setValueInternal(mValue - 1, true);
		}
	}

	private int getWrappedSelectorIndex(int selectorIndex) {
		if (selectorIndex > mMaxValue) {
			return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
		} else if (selectorIndex < mMinValue) {
			return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
		}
		return selectorIndex;
	}

	private void notifyChange(int previous, int current) {
		if (mOnValueChangeListener != null) {
			mOnValueChangeListener.onValueChange(this, previous, mValue);
		}
	}

	private String formatNumber(int value) {
		return (mFormatter != null) ? mFormatter.format(value) : formatNumberWithLocale(value);
	}

	/* AOSP formatNumberWithLocale() */
	private static String formatNumberWithLocale(int value) {
		return String.format(Locale.getDefault(), "%d", value);
	}

	private boolean updateInputTextView() {
		String text = (mDisplayedValues == null) ? formatNumber(mValue) : mDisplayedValues[mValue - mMinValue];
		if (!TextUtils.isEmpty(text)) {
			CharSequence beforeText = mInputText.getText();
			if (beforeText == null || !text.equals(beforeText.toString())) {
				mInputText.setText(text);
				return true;
			}
		}
		return false;
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return NumberPicker.class.getName();
	}
}
