package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityManager {

	private static AccessibilityManager instance;

	/* one per process, as on AOSP (it was made anew on every getSystemService()) */
	public static synchronized AccessibilityManager getInstance(android.content.Context context) {
		if (instance == null)
			instance = new AccessibilityManager();
		return instance;
	}

	public static final int FLAG_CONTENT_ICONS = 1;
	public static final int FLAG_CONTENT_TEXT = 2;
	public static final int FLAG_CONTENT_CONTROLS = 4;

	public interface AccessibilityStateChangeListener {
		void onAccessibilityStateChanged(boolean enabled);
	}

	public interface TouchExplorationStateChangeListener {
		void onTouchExplorationStateChanged(boolean enabled);
	}

	public boolean isTouchExplorationEnabled() { return false; }

	public boolean isEnabled() { return false; }

	public List getEnabledAccessibilityServiceList(int feedbackTypeFlags) {
		return new ArrayList<>();
	}

	/* the listeners are kept as AOSP does; accessibility and touch exploration are never on here, so the state never
	 * changes and they are never called */
	private final ArrayList<AccessibilityStateChangeListener> mAccessibilityStateChangeListeners = new ArrayList<>();
	private final ArrayList<TouchExplorationStateChangeListener> mTouchExplorationStateChangeListeners = new ArrayList<>();

	public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
		synchronized (mAccessibilityStateChangeListeners) {
			if (!mAccessibilityStateChangeListeners.contains(listener))
				mAccessibilityStateChangeListeners.add(listener);
		}
		return true;
	}

	public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
		synchronized (mTouchExplorationStateChangeListeners) {
			if (!mTouchExplorationStateChangeListeners.contains(listener))
				mTouchExplorationStateChangeListeners.add(listener);
		}
		return true;
	}

	public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
		synchronized (mAccessibilityStateChangeListeners) {
			return mAccessibilityStateChangeListeners.remove(listener);
		}
	}

	public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
		synchronized (mTouchExplorationStateChangeListeners) {
			return mTouchExplorationStateChangeListeners.remove(listener);
		}
	}

	/* AOSP AccessibilityManager.sendAccessibilityEvent(): an event is only sent when accessibility is on */
	public void sendAccessibilityEvent(AccessibilityEvent event) {
		if (!isEnabled()) {
			throw new IllegalStateException("Accessibility off. Did you forget to check that?");
		}
	}

	public void interrupt() {
		if (!isEnabled()) {
			throw new IllegalStateException("Accessibility off. Did you forget to check that?");
		}
	}

	/* there is no accessibility timeout preference, so like AOSP without one keep the app's timeout */
	public int getRecommendedTimeoutMillis(int originalTimeout, int uiContentFlags) {
		return originalTimeout;
	}
}
