package android.app;

import android.atl.ATLLoadedApp;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

public class SearchManager {

	/* AOSP SearchManager's keys and suggestion columns that SearchView and SuggestionsAdapter use */
	public final static String QUERY = "query";
	public final static String USER_QUERY = "user_query";
	public final static String APP_DATA = "app_data";
	public final static String ACTION_KEY = "action_key";
	public final static String EXTRA_DATA_KEY = "intent_extra_data_key";
	public final static String CURSOR_EXTRA_KEY_IN_PROGRESS = "in_progress";
	public final static String ACTION_MSG = "action_msg";
	public final static int FLAG_QUERY_REFINEMENT = 1 << 0;
	public final static String SUGGEST_URI_PATH_QUERY = "search_suggest_query";
	public final static String SUGGEST_PARAMETER_LIMIT = "limit";
	public final static String SUGGEST_COLUMN_TEXT_1 = "suggest_text_1";
	public final static String SUGGEST_COLUMN_TEXT_2 = "suggest_text_2";
	public final static String SUGGEST_COLUMN_TEXT_2_URL = "suggest_text_2_url";
	public final static String SUGGEST_COLUMN_ICON_1 = "suggest_icon_1";
	public final static String SUGGEST_COLUMN_ICON_2 = "suggest_icon_2";
	public final static String SUGGEST_COLUMN_INTENT_ACTION = "suggest_intent_action";
	public final static String SUGGEST_COLUMN_INTENT_DATA = "suggest_intent_data";
	public final static String SUGGEST_COLUMN_INTENT_EXTRA_DATA = "suggest_intent_extra_data";
	public final static String SUGGEST_COLUMN_INTENT_DATA_ID = "suggest_intent_data_id";
	public final static String SUGGEST_COLUMN_QUERY = "suggest_intent_query";
	public final static String SUGGEST_COLUMN_FLAGS = "suggest_flags";
	public final static String SUGGEST_MIME_TYPE = "vnd.android.cursor.dir/vnd.android.search.suggest";
	public final static String SUGGEST_COLUMN_FORMAT = "suggest_format";

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

	/* AOSP SearchManager.getSuggestions(): asks the suggestion provider of the searchable for suggestions on the
	 * query (content://<authority>/<path>/search_suggest_query[/<query>]?limit=<limit>) */
	public Cursor getSuggestions(SearchableInfo searchable, String query, int limit) {
		if (searchable == null) {
			return null;
		}
		String authority = searchable.getSuggestAuthority();
		if (authority == null) {
			return null;
		}
		Uri.Builder uriBuilder = new Uri.Builder()
		                             .scheme(ContentResolver.SCHEME_CONTENT)
		                             .authority(authority)
		                             .query("")
		                             .fragment("");
		final String contentPath = searchable.getSuggestPath();
		if (contentPath != null) {
			uriBuilder.appendEncodedPath(contentPath);
		}
		uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);
		String selection = searchable.getSuggestSelection();
		String[] selArgs = null;
		if (selection != null) {
			selArgs = new String[] {query};
		} else {
			uriBuilder.appendPath(query);
		}
		if (limit > 0) {
			uriBuilder.appendQueryParameter(SUGGEST_PARAMETER_LIMIT, String.valueOf(limit));
		}
		Uri uri = uriBuilder.build();
		Context context = ATLLoadedApp.getPrimaryApplication().getApplication();
		return context.getContentResolver().query(uri, null, selection, selArgs, null);
	}
}
