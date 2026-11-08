package android.accessibilityservice;

import android.os.Parcel;
import junit.framework.TestCase;

public class AccessibilityServiceInfoTest extends TestCase {
	public void testAccessibilityServiceInfo() {
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
		info.notificationTimeout = 100;
		info.setInteractiveUiTimeoutMillis(500);
		Parcel p = Parcel.obtain();
		info.writeToParcel(p, 0);
		p.setDataPosition(0);
		AccessibilityServiceInfo back = AccessibilityServiceInfo.CREATOR.createFromParcel(p);
		assertEquals(100, back.notificationTimeout);
		assertEquals(500, back.getInteractiveUiTimeoutMillis());
		assertEquals("[FEEDBACK_SPOKEN]", AccessibilityServiceInfo.feedbackTypeToString(AccessibilityServiceInfo.FEEDBACK_SPOKEN));
	}
}
