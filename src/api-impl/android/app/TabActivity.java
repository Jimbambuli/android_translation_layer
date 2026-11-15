package android.app;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

/**
 * AOSP TabActivity (android16 core/java/android/app/TabActivity.java): the content must hold a TabHost with id
 * android.R.id.tabhost, which is set up in onContentChanged(); without content of its own the activity gets
 * AOSP's R.layout.tab_content. Not ported: onChildTitleChanged() (needs the LocalActivityManager of ActivityGroup,
 * which ATL's ActivityGroup does not have).
 */
public class TabActivity extends ActivityGroup {
	private TabHost mTabHost;
	private String mDefaultTab = null;
	private int mDefaultTabIndex = -1;

	public TabActivity() {
	}

	/**
	 * Sets the default tab that is the first tab highlighted.
	 *
	 * @param tag the name of the default tab
	 */
	public void setDefaultTab(String tag) {
		mDefaultTab = tag;
		mDefaultTabIndex = -1;
	}

	/**
	 * Sets the default tab that is the first tab highlighted.
	 *
	 * @param index the index of the default tab
	 */
	public void setDefaultTab(int index) {
		mDefaultTab = null;
		mDefaultTabIndex = index;
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		ensureTabHost();
		String cur = state.getString("currentTab");
		if (cur != null) {
			mTabHost.setCurrentTabByTag(cur);
		}
		if (mTabHost.getCurrentTab() < 0) {
			if (mDefaultTab != null) {
				mTabHost.setCurrentTabByTag(mDefaultTab);
			} else if (mDefaultTabIndex >= 0) {
				mTabHost.setCurrentTab(mDefaultTabIndex);
			}
		}
	}

	@Override
	protected void onPostCreate(Bundle icicle) {
		super.onPostCreate(icicle);

		ensureTabHost();

		if (mTabHost.getCurrentTab() == -1) {
			mTabHost.setCurrentTab(0);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		String currentTabTag = mTabHost.getCurrentTabTag();
		if (currentTabTag != null) {
			outState.putString("currentTab", currentTabTag);
		}
	}

	/**
	 * Updates the screen state (current list and other views) when the
	 * content changes.
	 *
	 *@see Activity#onContentChanged()
	 */
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mTabHost = findViewById(com.android.internal.R.id.tabhost);

		if (mTabHost == null) {
			throw new RuntimeException(
			    "Your content must have a TabHost whose id attribute is " +
			    "'android.R.id.tabhost'");
		}
		/* AOSP: mTabHost.setup(getLocalActivityManager()); ATL's ActivityGroup has no LocalActivityManager */
		mTabHost.setup();
	}

	private void ensureTabHost() {
		if (mTabHost == null) {
			setContentView(createTabContent());
		}
	}

	/* AOSP R.layout.tab_content, which ATL's framework-res does not have: a TabHost (android.R.id.tabhost) with a
	 * vertical LinearLayout of the TabWidget (android.R.id.tabs) and the FrameLayout (android.R.id.tabcontent) that
	 * takes the remaining height */
	private TabHost createTabContent() {
		TabHost tabHost = new TabHost(this, null);
		tabHost.setId(com.android.internal.R.id.tabhost);
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		TabWidget tabWidget = new TabWidget(this);
		tabWidget.setId(com.android.internal.R.id.tabs);
		tabWidget.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.addView(tabWidget, new LinearLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		FrameLayout tabContent = new FrameLayout(this);
		tabContent.setId(com.android.internal.R.id.tabcontent);
		linearLayout.addView(tabContent, new LinearLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		tabHost.addView(linearLayout, new FrameLayout.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return tabHost;
	}

	/**
	 * Returns the {@link TabHost} the activity is using to host its tabs.
	 *
	 * @return the {@link TabHost} the activity is using to host its tabs.
	 */
	public TabHost getTabHost() {
		ensureTabHost();
		return mTabHost;
	}

	/**
	 * Returns the {@link TabWidget} the activity is using to draw the actual tabs.
	 *
	 * @return the {@link TabWidget} the activity is using to draw the actual tabs.
	 */
	public TabWidget getTabWidget() {
		return mTabHost.getTabWidget();
	}
}
