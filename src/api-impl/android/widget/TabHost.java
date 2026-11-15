package android.widget;

import android.view.View;

public class TabHost extends View {
	public TabHost(android.content.Context context) {
		super(context);
	}

	public TabHost(android.content.Context context, android.util.AttributeSet attrs) {
		super(context, attrs);
	}

	public TabHost(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TabHost(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public static class TabSpec {
		private String tag;
		public TabSpec(String tag) {
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		}

		public TabSpec setIndicator(CharSequence label) {
			return this;
		}

		public TabSpec setIndicator(CharSequence label, android.graphics.drawable.Drawable icon) {
			return this;
		}

		public TabSpec setIndicator(View view) {
			return this;
		}

		public TabSpec setContent(int viewId) {
			return this;
		}

		public TabSpec setContent(android.content.Intent intent) {
			return this;
		}

		public TabSpec setContent(android.widget.TabHost.TabContentFactory factory) {
			return this;
		}
	}

	public interface TabContentFactory {
		View createTabContent(String tag);
	}

	public TabSpec newTabSpec(String tag) {
		return new TabSpec(tag);
	}

	public void setup() {}

	/* AOSP TabHost keeps the spec list and the current index and notifies the listener on a change
	 * (android-14 core/java/android/widget/TabHost.java addTab/setCurrentTab/getCurrentTabTag).
	 * ATL has no tab widget or content frame, so nothing is shown. */
	private final java.util.List<TabSpec> mTabSpecs = new java.util.ArrayList<TabSpec>(2);
	private int mCurrentTab = -1;
	private OnTabChangeListener mOnTabChangeListener;

	public void addTab(TabSpec tabSpec) {
		if (tabSpec == null) {
			throw new IllegalArgumentException("tabSpec must be non-null");
		}
		mTabSpecs.add(tabSpec);
		if (mCurrentTab == -1) {
			setCurrentTab(0);
		}
	}

	public void setCurrentTab(int index) {
		if (index < 0 || index >= mTabSpecs.size()) {
			return;
		}
		if (index == mCurrentTab) {
			return;
		}
		mCurrentTab = index;
		if (mOnTabChangeListener != null) {
			mOnTabChangeListener.onTabChanged(getCurrentTabTag());
		}
	}

	public void setCurrentTabByTag(String tag) {
		for (int i = 0; i < mTabSpecs.size(); i++) {
			if (mTabSpecs.get(i).getTag().equals(tag)) {
				setCurrentTab(i);
				break;
			}
		}
	}

	public int getCurrentTab() {
		return mCurrentTab;
	}

	public String getCurrentTabTag() {
		if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) {
			return mTabSpecs.get(mCurrentTab).getTag();
		}
		return null;
	}

	public void setOnTabChangedListener(OnTabChangeListener listener) {
		mOnTabChangeListener = listener;
	}

	public interface OnTabChangeListener {
		void onTabChanged(String tabId);
	}
}
