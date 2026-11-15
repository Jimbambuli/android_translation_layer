package android.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

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

	/* no accessibility tool requests nodes of ATL's views (AOSP: valid only while a node request is in progress) */
	public boolean isRequestFromAccessibilityTool() { return false; }

	public List getEnabledAccessibilityServiceList(int feedbackTypeFlags) {
		return new ArrayList<>();
	}

	/* the listeners are kept as AOSP does; accessibility and touch exploration are never on here, so the state never
	 * changes and they are never called */
	private final ArrayList<AccessibilityStateChangeListener> mAccessibilityStateChangeListeners = new ArrayList<>();
	private final ArrayList<TouchExplorationStateChangeListener> mTouchExplorationStateChangeListeners = new ArrayList<>();

	/** AOSP: listener for changes of the enabled accessibility services */
	public interface AccessibilityServicesStateChangeListener {
		void onAccessibilityServicesStateChanged(AccessibilityManager manager);
	}

	/* AOSP keeps these with their executor; no service is ever enabled here, so they are never called */
	private final HashMap<AccessibilityServicesStateChangeListener, Executor> mServicesStateChangeListeners = new HashMap<>();

	public void addAccessibilityServicesStateChangeListener(Executor executor, AccessibilityServicesStateChangeListener listener) {
		synchronized (mServicesStateChangeListeners) {
			mServicesStateChangeListeners.put(listener, executor);
		}
	}

	/* AOSP: the listener runs on the manager's handler, the main looper's */
	public void addAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener) {
		addAccessibilityServicesStateChangeListener(new android.os.HandlerExecutor(new android.os.Handler(android.os.Looper.getMainLooper())), listener);
	}

	public boolean removeAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener) {
		synchronized (mServicesStateChangeListeners) {
			return mServicesStateChangeListeners.remove(listener) != null;
		}
	}

	/* AOSP AccessibilityManager.getInstalledAccessibilityServiceList(): without the accessibility system service
	 * the list is empty; ATL has no accessibility services */
	public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() {
		return Collections.emptyList();
	}

	public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
		synchronized (mAccessibilityStateChangeListeners) {
			if (!mAccessibilityStateChangeListeners.contains(listener))
				mAccessibilityStateChangeListeners.add(listener);
		}
		return true;
	}

	/* AOSP: the handler is where the listener is called back; the state never changes here, so it is not used */
	public void addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener, android.os.Handler handler) {
		addAccessibilityStateChangeListener(listener);
	}

	public void addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener, android.os.Handler handler) {
		addTouchExplorationStateChangeListener(listener);
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
