package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP NfcF (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NfcF extends BasicTagTechnology {
	public static final String EXTRA_SC = "systemcode";
	public static final String EXTRA_PMM = "pmm";

	private byte[] mSystemCode = null;
	private byte[] mManufacturer = null;

	public static NfcF get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NFC_F))
			return null;
		return new NfcF(tag);
	}

	public NfcF(Tag tag) {
		super(tag, TagTechnology.NFC_F);
		Bundle extras = tag.getTechExtras(TagTechnology.NFC_F);
		if (extras != null) {
			mSystemCode = extras.getByteArray(EXTRA_SC);
			mManufacturer = extras.getByteArray(EXTRA_PMM);
		}
	}

	public byte[] getSystemCode() {
		return mSystemCode;
	}

	public byte[] getManufacturer() {
		return mManufacturer;
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
