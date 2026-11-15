package android.appwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/* AOSP core/java/android/appwidget/AppWidgetProvider.java: onReceive() dispatches the widget broadcasts to the
 * callbacks, which are empty in AOSP. ATL has no widget host that sends them; an app may send them itself. */
public class AppWidgetProvider extends BroadcastReceiver {
	public AppWidgetProvider() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_ENABLE_AND_UPDATE.equals(action)) {
			this.onReceive(context, new Intent(intent).setAction(AppWidgetManager.ACTION_APPWIDGET_ENABLED));
			this.onReceive(context, new Intent(intent).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
		} else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				if (appWidgetIds != null && appWidgetIds.length > 0) {
					this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
				}
			}
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			Bundle extras = intent.getExtras();
			if (extras != null && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
				final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
				this.onDeleted(context, new int[] {appWidgetId});
			}
		} else if (AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(action)) {
			Bundle extras = intent.getExtras();
			if (extras != null && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID) && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS)) {
				int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
				Bundle widgetExtras = extras.getBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
				this.onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), appWidgetId, widgetExtras);
			}
		} else if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
			this.onEnabled(context);
		} else if (AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action)) {
			this.onDisabled(context);
		} else if (AppWidgetManager.ACTION_APPWIDGET_RESTORED.equals(action)) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int[] oldIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS);
				int[] newIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				if (oldIds != null && oldIds.length > 0) {
					this.onRestored(context, oldIds, newIds);
					this.onUpdate(context, AppWidgetManager.getInstance(context), newIds);
				}
			}
		}
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	}

	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
	}

	public void onDeleted(Context context, int[] appWidgetIds) {
	}

	public void onEnabled(Context context) {
	}

	public void onDisabled(Context context) {
	}

	public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
	}
}
