package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;

/**
 * The ActionBar of an Activity whose theme asks for one (what AOSP's WindowDecorActionBar is).
 * ATL has no decor with a bar widget: the state (display options, navigation, tabs, subtitle,
 * custom view, ...) is kept and the callbacks of tabs and list navigation fire as in AOSP; the
 * title is the title of the toplevel window.
 */
class ActivityActionBar extends ActionBar {
	private final Activity mActivity;
	private CharSequence mTitle;
	private boolean mTitleSet;
	private CharSequence mSubtitle;
	private Drawable mIcon, mLogo;
	private View mCustomView;
	private int mDisplayOptions = DISPLAY_SHOW_TITLE | DISPLAY_SHOW_HOME;
	private int mNavigationMode = NAVIGATION_MODE_STANDARD;
	private boolean mShowing = true;
	private Drawable mBackground;
	private float mElevation;
	private int mHeight = 0;
	private final ArrayList<TabImpl> mTabs = new ArrayList<>();
	private TabImpl mSelectedTab;
	private SpinnerAdapter mAdapter;
	private OnNavigationListener mNavListener;
	private int mSelectedNavigationItem = -1;
	private final ArrayList<OnMenuVisibilityListener> mMenuListeners = new ArrayList<>();

	ActivityActionBar(Activity activity) {
		mActivity = activity;
		mTitle = activity.getTitle();
	}

	@Override
	public void setCustomView(View view) {
		setCustomView(view, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	@Override
	public void setCustomView(View view, LayoutParams layoutParams) {
		if (view != null && layoutParams != null)
			view.setLayoutParams(layoutParams);
		mCustomView = view;
	}

	@Override
	public void setCustomView(int resId) {
		setCustomView(android.view.LayoutInflater.from(mActivity).inflate(resId, null, false));
	}

	@Override
	public void setIcon(int resId) {
		mIcon = resId != 0 ? mActivity.getDrawable(resId) : null;
	}

	@Override
	public void setIcon(Drawable icon) {
		mIcon = icon;
	}

	@Override
	public void setLogo(int resId) {
		mLogo = resId != 0 ? mActivity.getDrawable(resId) : null;
	}

	@Override
	public void setLogo(Drawable logo) {
		mLogo = logo;
	}

	@Override
	public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
		mAdapter = adapter;
		mNavListener = callback;
		mSelectedNavigationItem = adapter != null && adapter.getCount() > 0 ? 0 : -1;
	}

	@Override
	public void setSelectedNavigationItem(int position) {
		switch (mNavigationMode) {
			case NAVIGATION_MODE_TABS:
				selectTab(mTabs.get(position));
				break;
			case NAVIGATION_MODE_LIST:
				if (position < 0 || mAdapter == null || position >= mAdapter.getCount())
					throw new IllegalArgumentException("position " + position + " out of range");
				mSelectedNavigationItem = position;
				if (mNavListener != null)
					mNavListener.onNavigationItemSelected(position, mAdapter.getItemId(position));
				break;
			default:
				throw new IllegalStateException("setSelectedNavigationIndex not valid for current navigation mode");
		}
	}

	@Override
	public int getSelectedNavigationIndex() {
		switch (mNavigationMode) {
			case NAVIGATION_MODE_TABS:
				return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
			case NAVIGATION_MODE_LIST:
				return mSelectedNavigationItem;
			default:
				return -1;
		}
	}

	@Override
	public int getNavigationItemCount() {
		switch (mNavigationMode) {
			case NAVIGATION_MODE_TABS:
				return mTabs.size();
			case NAVIGATION_MODE_LIST:
				return mAdapter != null ? mAdapter.getCount() : 0;
			default:
				return 0;
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		mTitleSet = true;
		mActivity.getWindow().setTitle(title);
	}

	@Override
	public void setTitle(int resId) {
		setTitle(mActivity.getResources().getString(resId));
	}

	@Override
	public void setSubtitle(CharSequence subtitle) {
		mSubtitle = subtitle;
	}

	@Override
	public void setSubtitle(int resId) {
		setSubtitle(mActivity.getResources().getString(resId));
	}

	@Override
	public void setDisplayOptions(int options) {
		mDisplayOptions = options;
	}

	@Override
	public void setDisplayOptions(int options, int mask) {
		mDisplayOptions = (options & mask) | (mDisplayOptions & ~mask);
	}

	private void setOption(int option, boolean on) {
		setDisplayOptions(on ? option : 0, option);
	}

	@Override
	public void setDisplayUseLogoEnabled(boolean useLogo) {
		setOption(DISPLAY_USE_LOGO, useLogo);
	}

	@Override
	public void setDisplayShowHomeEnabled(boolean showHome) {
		setOption(DISPLAY_SHOW_HOME, showHome);
	}

	@Override
	public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
		setOption(DISPLAY_HOME_AS_UP, showHomeAsUp);
	}

	@Override
	public void setDisplayShowTitleEnabled(boolean showTitle) {
		setOption(DISPLAY_SHOW_TITLE, showTitle);
	}

	@Override
	public void setDisplayShowCustomEnabled(boolean showCustom) {
		setOption(DISPLAY_SHOW_CUSTOM, showCustom);
	}

	@Override
	public void setBackgroundDrawable(Drawable d) {
		mBackground = d;
	}

	@Override
	public View getCustomView() {
		return mCustomView;
	}

	@Override
	public CharSequence getTitle() {
		return mTitle;
	}

	@Override
	public CharSequence getSubtitle() {
		return mSubtitle;
	}

	@Override
	public int getNavigationMode() {
		return mNavigationMode;
	}

	@Override
	public void setNavigationMode(int mode) {
		if (mode == mNavigationMode)
			return;
		if (mode == NAVIGATION_MODE_TABS)
			mSelectedTab = mTabs.isEmpty() ? null : mTabs.get(0);
		mNavigationMode = mode;
	}

	@Override
	public int getDisplayOptions() {
		return mDisplayOptions;
	}

	@Override
	public Tab newTab() {
		return new TabImpl();
	}

	@Override
	public void addTab(Tab tab) {
		addTab(tab, mTabs.isEmpty());
	}

	@Override
	public void addTab(Tab tab, boolean setSelected) {
		addTab(tab, mTabs.size(), setSelected);
	}

	@Override
	public void addTab(Tab tab, int position) {
		addTab(tab, position, mTabs.isEmpty());
	}

	@Override
	public void addTab(Tab tab, int position, boolean setSelected) {
		TabImpl t = (TabImpl)tab;
		if (t.mListener == null)
			throw new IllegalStateException("Action Bar Tab must have a Callback");
		t.mPosition = position;
		mTabs.add(position, t);
		for (int i = position + 1; i < mTabs.size(); i++)
			mTabs.get(i).mPosition = i;
		if (setSelected)
			t.select();
	}

	@Override
	public void removeTab(Tab tab) {
		removeTabAt(tab.getPosition());
	}

	@Override
	public void removeTabAt(int position) {
		if (position < 0 || position >= mTabs.size())
			return;
		int selected = mSelectedTab != null ? mSelectedTab.getPosition() : mTabs.size() - 1;
		mTabs.remove(position);
		for (int i = position; i < mTabs.size(); i++)
			mTabs.get(i).mPosition = i;
		if (selected == position)
			selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
	}

	@Override
	public void removeAllTabs() {
		mTabs.clear();
		mSelectedTab = null;
	}

	@Override
	public void selectTab(Tab tab) {
		if (getNavigationMode() != NAVIGATION_MODE_TABS) {
			return;
		}
		TabImpl t = (TabImpl)tab;
		FragmentTransaction trans = null;
		if (mActivity.getFragmentManager() != null)
			trans = mActivity.getFragmentManager().beginTransaction().disallowAddToBackStack();
		if (mSelectedTab == t) {
			if (mSelectedTab != null)
				mSelectedTab.mListener.onTabReselected(mSelectedTab, trans);
		} else {
			if (mSelectedTab != null)
				mSelectedTab.mListener.onTabUnselected(mSelectedTab, trans);
			mSelectedTab = t;
			if (mSelectedTab != null)
				mSelectedTab.mListener.onTabSelected(mSelectedTab, trans);
		}
		if (trans != null && !trans.isEmpty())
			trans.commit();
	}

	@Override
	public Tab getSelectedTab() {
		return mSelectedTab;
	}

	@Override
	public Tab getTabAt(int index) {
		return index < 0 || index >= mTabs.size() ? null : mTabs.get(index);
	}

	@Override
	public int getTabCount() {
		return mTabs.size();
	}

	@Override
	public int getHeight() {
		return mShowing ? mHeight : 0;
	}

	@Override
	public void show() {
		mShowing = true;
	}

	@Override
	public void hide() {
		mShowing = false;
	}

	@Override
	public boolean isShowing() {
		return mShowing;
	}

	@Override
	public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
		mMenuListeners.add(listener);
	}

	@Override
	public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
		mMenuListeners.remove(listener);
	}

	@Override
	public void setElevation(float elevation) {
		mElevation = elevation;
	}

	@Override
	public float getElevation() {
		return mElevation;
	}

	@Override
	public void dispatchMenuVisibilityChanged(boolean visible) {
		for (OnMenuVisibilityListener l : new ArrayList<>(mMenuListeners))
			l.onMenuVisibilityChanged(visible);
	}

	@Override
	public void setWindowTitle(CharSequence title) {
		if (!mTitleSet)
			mTitle = title;
	}

	private class TabImpl extends Tab {
		TabListener mListener;
		Object mTag;
		Drawable mIcon;
		CharSequence mText, mContentDesc;
		int mPosition = -1;
		View mCustomView;

		@Override public int getPosition() { return mPosition; }
		@Override public Drawable getIcon() { return mIcon; }
		@Override public CharSequence getText() { return mText; }
		@Override public Tab setIcon(Drawable icon) { mIcon = icon; return this; }
		@Override public Tab setIcon(int resId) { return setIcon(mActivity.getDrawable(resId)); }
		@Override public Tab setText(CharSequence text) { mText = text; return this; }
		@Override public Tab setText(int resId) { return setText(mActivity.getResources().getText(resId)); }
		@Override public Tab setCustomView(View view) { mCustomView = view; return this; }
		@Override public Tab setCustomView(int layoutResId) {
			return setCustomView(android.view.LayoutInflater.from(mActivity).inflate(layoutResId, null, false));
		}
		@Override public View getCustomView() { return mCustomView; }
		@Override public Tab setTag(Object obj) { mTag = obj; return this; }
		@Override public Object getTag() { return mTag; }
		@Override public Tab setTabListener(TabListener listener) { mListener = listener; return this; }
		@Override public void select() { selectTab(this); }
		@Override public Tab setContentDescription(int resId) {
			return setContentDescription(mActivity.getResources().getText(resId));
		}
		@Override public Tab setContentDescription(CharSequence contentDesc) { mContentDesc = contentDesc; return this; }
		@Override public CharSequence getContentDescription() { return mContentDesc; }
	}
}
