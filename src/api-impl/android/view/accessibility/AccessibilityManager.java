package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityManager {

	public static final int FLAG_CONTENT_ICONS = 1;
	public static final int FLAG_CONTENT_TEXT = 2;
	public static final int FLAG_CONTENT_CONTROLS = 4;

	public interface AccessibilityStateChangeListener {}

	public interface TouchExplorationStateChangeListener {}

	public boolean isTouchExplorationEnabled() { return false; }

	public boolean isEnabled() { return false; }

	public List getEnabledAccessibilityServiceList(int feedbackTypeFlags) {
		return new ArrayList<>();
	}

	public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
		return false;
	}

	public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
		return false;
	}

	public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
		return false;
	}

	public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
		return false;
	}

	/* there is no accessibility timeout preference, so like AOSP without one keep the app's timeout */
	public int getRecommendedTimeoutMillis(int originalTimeout, int uiContentFlags) {
		return originalTimeout;
	}
}
