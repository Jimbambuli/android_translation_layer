package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import com.android.internal.R;
import java.util.List;

/* AOSP ShareActionProvider (frameworks/base/core/java/android/widget/ShareActionProvider.java), sub menu part:
 * onPrepareSubMenu() lists the activities that can handle the share intent (the first four, the rest in a
 * "See all" sub menu) and selecting one starts the intent for it (after onShareTargetSelected(), like
 * ActivityChooserModel.chooseActivity() does). Not nachgebaut: ActivityChooserModel/ActivityChooserView, i.e. the
 * usage history that orders the activities and the action view (onCreateActionView() returns an empty View). */
public class ShareActionProvider extends ActionProvider {
	public interface OnShareTargetSelectedListener {
		public boolean onShareTargetSelected(ShareActionProvider source, Intent intent);
	}

	private static final int DEFAULT_INITIAL_ACTIVITY_COUNT = 4;
	public static final String DEFAULT_SHARE_HISTORY_FILE_NAME = "share_history.xml";

	private final Context mContext;
	private String mShareHistoryFileName = DEFAULT_SHARE_HISTORY_FILE_NAME;
	private OnShareTargetSelectedListener mOnShareTargetSelectedListener;
	private Intent mIntent;
	private List<ResolveInfo> mActivities;

	public ShareActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	public void setOnShareTargetSelectedListener(OnShareTargetSelectedListener listener) {
		mOnShareTargetSelectedListener = listener;
	}

	@Override
	public View onCreateActionView() {
		return new View(mContext);
	}

	@Override
	public boolean hasSubMenu() {
		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		subMenu.clear();
		PackageManager packageManager = mContext.getPackageManager();
		mActivities = mIntent == null ? null : packageManager.queryIntentActivities(mIntent, 0);
		final int expandedActivityCount = mActivities == null ? 0 : mActivities.size();
		final int collapsedActivityCount = Math.min(expandedActivityCount, DEFAULT_INITIAL_ACTIVITY_COUNT);
		for (int i = 0; i < collapsedActivityCount; i++) {
			ResolveInfo activity = mActivities.get(i);
			subMenu.add(0, i, i, activity.loadLabel(packageManager)).setIcon(activity.loadIcon(packageManager)).setOnMenuItemClickListener(mOnMenuItemClickListener);
		}
		if (collapsedActivityCount < expandedActivityCount) {
			SubMenu expandedSubMenu = subMenu.addSubMenu(Menu.NONE, collapsedActivityCount, collapsedActivityCount, mContext.getString(R.string.activity_chooser_view_see_all));
			for (int i = 0; i < expandedActivityCount; i++) {
				ResolveInfo activity = mActivities.get(i);
				expandedSubMenu.add(0, i, i, activity.loadLabel(packageManager)).setIcon(activity.loadIcon(packageManager)).setOnMenuItemClickListener(mOnMenuItemClickListener);
			}
		}
	}

	public void setShareHistoryFileName(String shareHistoryFile) {
		mShareHistoryFileName = shareHistoryFile;
	}

	public void setShareIntent(Intent shareIntent) {
		if (shareIntent != null) {
			final String action = shareIntent.getAction();
			if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
				shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			}
		}
		mIntent = shareIntent;
	}

	private final OnMenuItemClickListener mOnMenuItemClickListener = new OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			final int itemId = item.getItemId();
			if (mIntent == null || mActivities == null || itemId < 0 || itemId >= mActivities.size()) {
				return true;
			}
			ResolveInfo chosen = mActivities.get(itemId);
			Intent launchIntent = new Intent(mIntent);
			launchIntent.setComponent(new android.content.ComponentName(chosen.activityInfo.packageName, chosen.activityInfo.name));
			if (mOnShareTargetSelectedListener != null) {
				mOnShareTargetSelectedListener.onShareTargetSelected(ShareActionProvider.this, launchIntent);
			}
			mContext.startActivity(launchIntent);
			return true;
		}
	};
}
