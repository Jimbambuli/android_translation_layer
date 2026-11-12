package android.content.pm;

import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* AOSP ShortcutManager: the app's dynamic shortcuts as the ShortcutService keeps them, here for the time the
 * app runs. There is no launcher that shows or pins them, and none come from the manifest. */
public class ShortcutManager {
	public static final int FLAG_MATCH_MANIFEST = 1 << 0;
	public static final int FLAG_MATCH_DYNAMIC = 1 << 1;
	public static final int FLAG_MATCH_PINNED = 1 << 2;
	public static final int FLAG_MATCH_CACHED = 1 << 3;

	private static final Map<String, ShortcutInfo> dynamic = new LinkedHashMap<>();

	private static void putDynamic(ShortcutInfo shortcut) {
		shortcut.mDynamic = true;
		dynamic.put(shortcut.getId(), shortcut);
	}

	public void removeAllDynamicShortcuts() {
		synchronized (dynamic) {
			dynamic.clear();
		}
	}

	public List<ShortcutInfo> getShortcuts(int matchFlags) {
		return (matchFlags & FLAG_MATCH_DYNAMIC) != 0 ? getDynamicShortcuts() : new ArrayList<ShortcutInfo>();
	}

	public List<ShortcutInfo> getDynamicShortcuts() {
		synchronized (dynamic) {
			return new ArrayList<>(dynamic.values());
		}
	}

	public void removeLongLivedShortcuts(List<String> shortcutIds) {
		removeDynamicShortcuts(shortcutIds);
	}

	public void reportShortcutUsed(String shortcutId) {
	}

	public boolean setDynamicShortcuts(List<ShortcutInfo> shortcutInfoList) {
		synchronized (dynamic) {
			dynamic.clear();
			for (ShortcutInfo shortcut : shortcutInfoList)
				putDynamic(shortcut);
		}
		return true;
	}

	// pinning needs a launcher that accepts shortcuts, as on an AOSP device whose launcher does not
	public boolean isRequestPinShortcutSupported() {
		return false;
	}

	/* AOSP without a launcher that accepts pins: nothing is pinned, there is no result intent */
	public boolean requestPinShortcut(ShortcutInfo shortcut, IntentSender resultIntent) {
		return false;
	}

	public Intent createShortcutResultIntent(ShortcutInfo shortcut) {
		return null;
	}

	// AOSP's default (ShortcutService.DEFAULT_MAX_SHORTCUTS_PER_ACTIVITY)
	public int getMaxShortcutCountPerActivity() {
		return 15;
	}

	public boolean isRateLimitingActive() {
		return false;
	}

	/* AOSP ShortcutService.DEFAULT_MAX_ICON_DIMENSION_DP in pixels */
	public int getIconMaxWidth() {
		return (int)(96 * Resources.getSystem().getDisplayMetrics().density);
	}

	public int getIconMaxHeight() {
		return getIconMaxWidth();
	}

	public boolean addDynamicShortcuts(List<ShortcutInfo> shortcutInfoList) {
		synchronized (dynamic) {
			for (ShortcutInfo shortcut : shortcutInfoList)
				putDynamic(shortcut);
		}
		return true;
	}

	/* AOSP: only shortcuts the app has are updated */
	public boolean updateShortcuts(List<ShortcutInfo> shortcutInfoList) {
		synchronized (dynamic) {
			for (ShortcutInfo shortcut : shortcutInfoList)
				if (dynamic.containsKey(shortcut.getId()))
					putDynamic(shortcut);
		}
		return true;
	}

	public void removeDynamicShortcuts(List<String> shortcutIds) {
		synchronized (dynamic) {
			for (String id : shortcutIds)
				dynamic.remove(id);
		}
	}

	public void pushDynamicShortcut(ShortcutInfo shortcut) {
		synchronized (dynamic) {
			putDynamic(shortcut);
		}
	}

	/* AOSP: disabled shortcuts are no longer dynamic */
	public void disableShortcuts(List<String> shortcutIds) {
		removeDynamicShortcuts(shortcutIds);
	}

	public void disableShortcuts(List<String> shortcutIds, CharSequence disabledMessage) {
		removeDynamicShortcuts(shortcutIds);
	}

	public void enableShortcuts(List<String> shortcutIds) {
	}

	public List<ShortcutInfo> getManifestShortcuts() {
		return Collections.emptyList();
	}

	public List<ShortcutInfo> getPinnedShortcuts() {
		return Collections.emptyList();
	}
}
