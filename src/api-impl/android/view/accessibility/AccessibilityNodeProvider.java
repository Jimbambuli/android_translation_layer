package android.view.accessibility;

import android.os.Bundle;
import java.util.List;

public class AccessibilityNodeProvider {
	/** The virtual id for the hosting View (AOSP: AccessibilityNodeProvider.HOST_VIEW_ID) */
	public static final int HOST_VIEW_ID = -1;

	/* AOSP: the defaults of a provider that has no virtual views to give */
	public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
		return null;
	}

	public boolean performAction(int virtualViewId, int action, Bundle arguments) {
		return false;
	}

	public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text, int virtualViewId) {
		return null;
	}

	public AccessibilityNodeInfo findFocus(int focus) {
		return null;
	}
}
