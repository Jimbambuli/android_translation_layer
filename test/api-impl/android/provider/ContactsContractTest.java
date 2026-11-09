package android.provider;

import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import junit.framework.TestCase;

public class ContactsContractTest extends TestCase {
	public void testDataKinds() {
		assertEquals("content://com.android.contacts/data/postals", StructuredPostal.CONTENT_URI.toString());
		assertEquals("vnd.android.cursor.item/postal-address_v2", StructuredPostal.CONTENT_ITEM_TYPE);
		assertEquals("data4", StructuredPostal.STREET);
		assertEquals(1, StructuredPostal.TYPE_HOME);
		assertEquals("vnd.android.cursor.item/name", StructuredName.CONTENT_ITEM_TYPE);
		assertEquals("content://com.android.contacts/data/phones", Phone.CONTENT_URI.toString());
		assertEquals("content://com.android.contacts/data/emails", Email.CONTENT_URI.toString());
	}

	public void testLabelResources() {
		assertTrue(StructuredPostal.getTypeLabelResource(StructuredPostal.TYPE_HOME) != 0);
		assertTrue(Phone.getTypeLabelResource(Phone.TYPE_MOBILE) != 0);
		assertTrue(Email.getTypeLabelResource(Email.TYPE_WORK) != 0);
		assertTrue(Im.getProtocolLabelResource(Im.PROTOCOL_SKYPE) != 0);
		assertTrue(Im.getTypeLabelResource(Im.TYPE_HOME) != 0);
	}

	public void testTables() {
		assertEquals("content://com.android.contacts/contacts/as_vcard", ContactsContract.Contacts.CONTENT_VCARD_URI.toString());
		assertEquals("content://com.android.contacts/phone_lookup", ContactsContract.PhoneLookup.CONTENT_FILTER_URI.toString().replaceAll("/$", ""));
		assertEquals("content://com.android.contacts/profile", ContactsContract.Profile.CONTENT_URI.toString());
		assertEquals("content://com.android.contacts/raw_contacts", ContactsContract.RawContacts.CONTENT_URI.toString());
		assertEquals("content://com.android.contacts/data", ContactsContract.Data.CONTENT_URI.toString());
		assertEquals("content://com.android.contacts", ContactsContract.AUTHORITY_URI.toString());
	}
}
