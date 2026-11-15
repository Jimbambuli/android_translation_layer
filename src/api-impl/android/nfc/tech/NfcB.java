package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP NfcB (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NfcB extends BasicTagTechnology {
	public static final String EXTRA_APPDATA = "appdata";
	public static final String EXTRA_PROTINFO = "protinfo";

	private byte[] mAppData;
	private byte[] mProtInfo;

	public static NfcB get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NFC_B))
			return null;
		return new NfcB(tag);
	}

	public NfcB(Tag tag) {
		super(tag, TagTechnology.NFC_B);
		Bundle extras = tag.getTechExtras(TagTechnology.NFC_B);
		mAppData = extras.getByteArray(EXTRA_APPDATA);
		mProtInfo = extras.getByteArray(EXTRA_PROTINFO);
	}

	public byte[] getApplicationData() {
		return mAppData;
	}

	public byte[] getProtocolInfo() {
		return mProtInfo;
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}
}
