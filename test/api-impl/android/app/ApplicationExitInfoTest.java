package android.app;

import android.os.Parcel;
import junit.framework.TestCase;

public class ApplicationExitInfoTest extends TestCase {
	public void testValuesAndConstants() {
		ApplicationExitInfo i = new ApplicationExitInfo();
		i.setReason(ApplicationExitInfo.REASON_USER_REQUESTED);
		i.setTimestamp(1234L);
		i.setProcessName("p");
		i.setStatus(9);
		assertEquals(10, i.getReason());
		assertEquals(1234L, i.getTimestamp());
		assertEquals("p", i.getProcessName());
		assertEquals(9, i.getStatus());
		assertEquals(16, ApplicationExitInfo.REASON_PACKAGE_UPDATED);
	}

	public void testParcelRoundTrip() {
		ApplicationExitInfo i = new ApplicationExitInfo();
		i.setReason(ApplicationExitInfo.REASON_CRASH);
		i.setTimestamp(77L);
		i.setDescription("boom");
		i.setPid(42);
		Parcel p = Parcel.obtain();
		i.writeToParcel(p, 0);
		p.setDataPosition(0);
		ApplicationExitInfo o = ApplicationExitInfo.CREATOR.createFromParcel(p);
		p.recycle();
		assertEquals(i, o);
		assertEquals("boom", o.getDescription());
		assertEquals(42, o.getPid());
	}
}
