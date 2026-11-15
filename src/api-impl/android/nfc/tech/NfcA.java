package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP NfcA (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NfcA extends BasicTagTechnology {
	public static final String EXTRA_SAK = "sak";
	public static final String EXTRA_ATQA = "atqa";

	private short mSak;
	private byte[] mAtqa;

	public static NfcA get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NFC_A))
			return null;
		return new NfcA(tag);
	}

	/* AOSP NfcA(Tag): NullPointerException if the tag has no NFC_A extras */
	public NfcA(Tag tag) {
		super(tag, TagTechnology.NFC_A);
		Bundle extras = tag.getTechExtras(TagTechnology.NFC_A);
		mSak = extras.getShort(EXTRA_SAK);
		mAtqa = extras.getByteArray(EXTRA_ATQA);
	}

	public byte[] getAtqa() {
		return mAtqa;
	}

	public short getSak() {
		return mSak;
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}

	/* AOSP setTimeout(): mTag.getTagService() is null on a mock tag, NullPointerException */
	public void setTimeout(int timeout) {
		throw new NullPointerException();
	}

	/* AOSP getTimeout(): see setTimeout() */
	public int getTimeout() {
		throw new NullPointerException();
	}
}
