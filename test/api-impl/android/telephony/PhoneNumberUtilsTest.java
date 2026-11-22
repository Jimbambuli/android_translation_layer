/*
 * Host test for PhoneNumberUtils/PhoneNumberManager with AOSP's libphonenumber (national Swiss numbers). Run like
 * test/api-impl/android/app/job/JobSchedulerTest.java; api-impl.jar must hold the number metadata resources.
 */
package android.telephony;

import android.net.Uri;
import java.util.Arrays;
import junit.framework.TestCase;

public class PhoneNumberUtilsTest extends TestCase {
	public void testFormatNationalSwissNumber() {
		assertEquals("079 123 45 67", PhoneNumberUtils.formatNumber("0791234567", "CH"));
		assertEquals("+41 79 123 45 67", PhoneNumberUtils.formatNumber("+41791234567", "CH"));
	}

	public void testFormatNumberToE164() {
		assertEquals("+41791234567", PhoneNumberUtils.formatNumberToE164("079 123 45 67", "CH"));
	}

	public void testAreSamePhoneNumber() {
		assertTrue(PhoneNumberUtils.areSamePhoneNumber("0791234567", "+41791234567", "CH"));
		assertFalse(PhoneNumberUtils.areSamePhoneNumber("0791234567", "0791234568", "CH"));
	}

	public void testParsePhoneNumberNational() {
		ParsedPhoneNumber p = new PhoneNumberManager().parsePhoneNumber(Arrays.asList(Uri.parse("tel:0791234567")), "ch");
		assertEquals("+41791234567", p.getParsedPhoneNumber());
		assertTrue(p.isValidPhoneNumber());
	}
}
