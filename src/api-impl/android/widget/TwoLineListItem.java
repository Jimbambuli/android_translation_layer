package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/* AOSP TwoLineListItem (frameworks/base/core/java/android/widget/TwoLineListItem.java) */
@Deprecated
public class TwoLineListItem extends RelativeLayout {
	private TextView mText1;
	private TextView mText2;

	public TwoLineListItem(Context context) {
		this(context, null, 0);
	}

	public TwoLineListItem(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TwoLineListItem(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TwoLineListItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		final TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TwoLineListItem, defStyleAttr,
		                                                    defStyleRes);
		a.recycle();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mText1 = findViewById(com.android.internal.R.id.text1);
		mText2 = findViewById(com.android.internal.R.id.text2);
	}

	public TextView getText1() {
		return mText1;
	}

	public TextView getText2() {
		return mText2;
	}
}
