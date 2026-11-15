package android.accessibilityservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayList;
import java.util.List;

/* AOSP AccessibilityService (core/java/android/accessibilityservice/AccessibilityService.java) as it behaves while
 * the system has not connected it: mConnectionId is unset, so AccessibilityInteractionClient finds no connection.
 * ATL has no accessibility manager that binds such a service, so onServiceConnected()/onAccessibilityEvent() are
 * never called and the calls that need the connection answer with null, false or an empty list, as the AOSP code does. */
public abstract class AccessibilityService extends Service {
	public static final String SERVICE_INTERFACE = "android.accessibilityservice.AccessibilityService";
	public static final String SERVICE_META_DATA = "android.accessibilityservice";

	public static final int GLOBAL_ACTION_BACK = 1;
	public static final int GLOBAL_ACTION_HOME = 2;
	public static final int GLOBAL_ACTION_RECENTS = 3;
	public static final int GLOBAL_ACTION_NOTIFICATIONS = 4;
	public static final int GLOBAL_ACTION_QUICK_SETTINGS = 5;
	public static final int GLOBAL_ACTION_POWER_DIALOG = 6;
	public static final int GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN = 7;
	public static final int GLOBAL_ACTION_LOCK_SCREEN = 8;
	public static final int GLOBAL_ACTION_TAKE_SCREENSHOT = 9;
	public static final int GLOBAL_ACTION_KEYCODE_HEADSETHOOK = 10;
	public static final int GLOBAL_ACTION_ACCESSIBILITY_BUTTON = 11;
	public static final int GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER = 12;
	public static final int GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT = 13;
	public static final int GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS = 14;
	public static final int GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE = 15;
	public static final int GLOBAL_ACTION_DPAD_UP = 16;
	public static final int GLOBAL_ACTION_DPAD_DOWN = 17;
	public static final int GLOBAL_ACTION_DPAD_LEFT = 18;
	public static final int GLOBAL_ACTION_DPAD_RIGHT = 19;
	public static final int GLOBAL_ACTION_DPAD_CENTER = 20;

	private AccessibilityServiceInfo mInfo;

	public abstract void onAccessibilityEvent(AccessibilityEvent event);

	public abstract void onInterrupt();

	protected void onServiceConnected() {}

	protected boolean onKeyEvent(KeyEvent event) {
		return false;
	}

	public List<AccessibilityWindowInfo> getWindows() {
		return new ArrayList<AccessibilityWindowInfo>();
	}

	public AccessibilityNodeInfo getRootInActiveWindow() {
		return getRootInActiveWindow(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID);
	}

	/* ATL: no window content is exposed to accessibility services */
	public AccessibilityNodeInfo getRootInActiveWindow(int prefetchingStrategy) {
		return null;
	}

	public AccessibilityNodeInfo findFocus(int focus) {
		return null;
	}

	public final void disableSelf() {}

	public final boolean dispatchGesture(GestureDescription gesture, GestureResultCallback callback, Handler handler) {
		return false;
	}

	public final boolean performGlobalAction(int action) {
		return false;
	}

	public final AccessibilityServiceInfo getServiceInfo() {
		return null; // no connection: AOSP returns null, not mInfo
	}

	public final void setServiceInfo(AccessibilityServiceInfo info) {
		mInfo = info;
	}

	public final IBinder onBind(Intent intent) {
		return null; // the system's accessibility manager is the only caller that gets the client wrapper
	}

	public static abstract class GestureResultCallback {
		public void onCompleted(GestureDescription gestureDescription) {}

		public void onCancelled(GestureDescription gestureDescription) {}
	}
}
