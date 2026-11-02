package android.content.pm;

import java.util.Collections;
import java.util.List;

public class ShortcutManager {
	public void removeAllDynamicShortcuts() {
	}

	public List getShortcuts(int matchFlags) {
		return Collections.emptyList();
	}

	// published shortcuts are a launcher concept that has no equivalent outside AOSP
	public List<ShortcutInfo> getDynamicShortcuts() {
		return Collections.emptyList();
	}

	public void removeLongLivedShortcuts(List<String> shortcutIds) {
	}

	public void reportShortcutUsed(String shortcutId) {
	}

	public boolean setDynamicShortcuts(List<ShortcutInfo> shortcutInfoList) {
		return true;
	}

	// pinning needs a launcher that accepts shortcuts, as on an AOSP device whose launcher does not
	public boolean isRequestPinShortcutSupported() {
		return false;
	}

	// AOSP's default (ShortcutService.DEFAULT_MAX_SHORTCUTS_PER_ACTIVITY)
	public int getMaxShortcutCountPerActivity() {
		return 15;
	}

	public boolean isRateLimitingActive() {
		return false;
	}

	public boolean addDynamicShortcuts(List<ShortcutInfo> shortcutInfoList) {
		return true;
	}

	public boolean updateShortcuts(List<ShortcutInfo> shortcutInfoList) {
		return true;
	}

	public void removeDynamicShortcuts(List<String> shortcutIds) {
	}

	public void pushDynamicShortcut(ShortcutInfo shortcut) {
	}

	public List<ShortcutInfo> getManifestShortcuts() {
		return Collections.emptyList();
	}

	public List<ShortcutInfo> getPinnedShortcuts() {
		return Collections.emptyList();
	}
}
