package android.app;

import android.atl.ATLLoadedApp;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

public class SearchManager {

	/* AOSP SearchManager.getSearchableInfo(): the search information the activity declares in its
	 * android.app.searchable meta-data, null if it declares none or an invalid one */
	public SearchableInfo getSearchableInfo(ComponentName componentName) {
		if (componentName == null) {
			return null;
		}
		Context context = ATLLoadedApp.getPrimaryApplication().getApplication();
		try {
			ActivityInfo ai = context.getPackageManager().getActivityInfo(componentName, PackageManager.GET_META_DATA);
			return SearchableInfo.getActivityMetaData(context, ai, 0);
		} catch (PackageManager.NameNotFoundException e) {
			return null;
		}
	}
}
