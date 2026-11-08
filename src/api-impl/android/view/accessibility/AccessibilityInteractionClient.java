package android.view.accessibility;

import android.os.Bundle;
import java.util.Collections;
import java.util.List;

/**
 * AOSP AccessibilityInteractionClient: the way an AccessibilityNodeInfo or AccessibilityWindowInfo asks the accessibility
 * service it came from for more (its parent, a refreshed copy, the result of an action). ATL has no accessibility service,
 * so no node or window has a connection, and the answers are those AOSP gives for a node without one: nothing found,
 * the action not performed.
 */
public final class AccessibilityInteractionClient {
	private static final AccessibilityInteractionClient sInstance = new AccessibilityInteractionClient();

	private AccessibilityInteractionClient() {}

	public static AccessibilityInteractionClient getInstance() {
		return sInstance;
	}

	public AccessibilityNodeInfo findFocus(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int focusType) {
		return null;
	}

	public AccessibilityNodeInfo focusSearch(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int direction) {
		return null;
	}

	public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, boolean bypassCache, int prefetchFlags, Bundle arguments) {
		return null;
	}

	public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int connectionId, android.os.IBinder leashToken, long accessibilityNodeId, boolean bypassCache, int prefetchFlags, Bundle arguments) {
		return null;
	}

	public boolean performAccessibilityAction(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments) {
		return false;
	}

	public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String text) {
		return Collections.emptyList();
	}

	public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String viewId) {
		return Collections.emptyList();
	}

	public AccessibilityWindowInfo getWindow(int connectionId, int accessibilityWindowId) {
		return null;
	}

	public AccessibilityWindowInfo getWindow(int connectionId, int accessibilityWindowId, boolean bypassCache) {
		return null;
	}

	public void clearCache(int connectionId) {}
}
