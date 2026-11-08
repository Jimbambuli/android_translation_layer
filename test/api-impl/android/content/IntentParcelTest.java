/*
 * Host test for the Parcel round trip of Intent (with selector, categories, extras and a ClipData), ClipData with
 * ClipDescription, RemoteInput and SigningInfo (android-14 wire formats, Parcel.writeCharSequenceArray / writeArraySet).
 * Pure Java. Run like PersistableBundleTest (dx needs --min-sdk-version=26) with the class android.content.IntentParcelTest.
 */
package android.content;

import android.app.RemoteInput;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import junit.framework.TestCase;

public class IntentParcelTest extends TestCase {

	private static <T extends Parcelable> T roundTrip(T value, Parcelable.Creator<T> creator) {
		Parcel p = Parcel.obtain();
		value.writeToParcel(p, 0);
		p.setDataPosition(0);
		T r = creator.createFromParcel(p);
		assertEquals("parcel must be fully read", p.dataSize(), p.dataPosition());
		p.recycle();
		return r;
	}

	public void testIntent() {
		Intent selector = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org/x"));
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setComponent(new ComponentName("a.b", "a.b.C"));
		i.setDataAndType(Uri.parse("content://a.b/1"), "text/plain");
		i.setPackage("a.b");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		i.addCategory("cat.one");
		i.addCategory("cat.two");
		i.putExtra("s", "text");
		i.putExtra("n", 42);
		i.setSelector(selector);
		i.setClipData(ClipData.newPlainText("lbl", "clip text"));
		Intent r = roundTrip(i, Intent.CREATOR);
		assertEquals(Intent.ACTION_SEND, r.getAction());
		assertEquals(new ComponentName("a.b", "a.b.C"), r.getComponent());
		assertEquals(Uri.parse("content://a.b/1"), r.getData());
		assertEquals("text/plain", r.getType());
		assertEquals("a.b", r.getPackage());
		assertEquals(i.getFlags(), r.getFlags());
		assertEquals(i.getCategories(), r.getCategories());
		assertEquals("text", r.getStringExtra("s"));
		assertEquals(42, r.getIntExtra("n", 0));
		assertEquals(Intent.ACTION_VIEW, r.getSelector().getAction());
		assertEquals("clip text", r.getClipData().getItemAt(0).getText().toString());
	}

	public void testEmptyIntentAndArray() {
		Intent r = roundTrip(new Intent(), Intent.CREATOR);
		assertNull(r.getAction());
		assertNull(r.getData());
		assertNull(r.getComponent());
		assertNull(r.getSelector());
		assertNull(r.getClipData());
		assertTrue(r.getCategories().isEmpty());
		assertEquals(3, Intent.CREATOR.newArray(3).length);
	}

	public void testClipData() {
		ClipData c = new ClipData("label", new String[] {"text/plain", "text/uri-list"}, new ClipData.Item("t", "<b>t</b>"));
		c.addItem(new ClipData.Item(Uri.parse("content://x/2")));
		c.addItem(new ClipData.Item(new Intent("act")));
		c.getDescription().setExtras(extras());
		ClipData r = roundTrip(c, ClipData.CREATOR);
		assertEquals(3, r.getItemCount());
		assertEquals("label", r.getDescription().getLabel().toString());
		assertEquals(2, r.getDescription().getMimeTypeCount());
		assertEquals("text/uri-list", r.getDescription().getMimeType(1));
		assertEquals(c.getDescription().getTimestamp(), r.getDescription().getTimestamp());
		assertEquals("v", r.getDescription().getExtras().getString("k"));
		assertEquals("<b>t</b>", r.getItemAt(0).getHtmlText());
		assertEquals(Uri.parse("content://x/2"), r.getItemAt(1).getUri());
		assertEquals("act", r.getItemAt(2).getIntent().getAction());
	}

	private static PersistableBundle extras() {
		PersistableBundle b = new PersistableBundle();
		b.putString("k", "v");
		return b;
	}

	public void testRemoteInput() {
		Bundle ex = new Bundle();
		ex.putString("e", "x");
		RemoteInput in = new RemoteInput.Builder("key").setLabel("Reply").setChoices(new CharSequence[] {"yes", "no"})
		                     .setAllowFreeFormInput(false).addExtras(ex).build();
		assertEquals("key", in.getResultKey());
		assertFalse(in.getAllowFreeFormInput());
		RemoteInput r = roundTrip(in, RemoteInput.CREATOR);
		assertEquals("key", r.getResultKey());
		assertEquals("Reply", r.getLabel().toString());
		assertEquals(2, r.getChoices().length);
		assertEquals("no", r.getChoices()[1].toString());
		assertFalse(r.getAllowFreeFormInput());
		assertEquals("x", r.getExtras().getString("e"));
		assertTrue(new RemoteInput.Builder("k2").build().getAllowFreeFormInput());
		assertNull(roundTrip(new RemoteInput.Builder("k3").build(), RemoteInput.CREATOR).getChoices());
		try {
			new RemoteInput.Builder(null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testSigningInfo() {
		Signature[] sigs = {new Signature(new byte[] {1, 2, 3}), new Signature(new byte[] {4, 5})};
		SigningInfo r = roundTrip(new SigningInfo(sigs), SigningInfo.CREATOR);
		assertEquals(2, r.getApkContentsSigners().length);
		assertEquals(sigs[1], r.getApkContentsSigners()[1]);
		assertTrue(r.hasMultipleSigners());
		SigningInfo u = roundTrip(new SigningInfo(), SigningInfo.CREATOR);
		assertNull(u.getApkContentsSigners());
	}
}
