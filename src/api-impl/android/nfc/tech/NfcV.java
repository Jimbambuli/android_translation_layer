package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP NfcV (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NfcV extends BasicTagTechnology {
	public static final String EXTRA_RESP_FLAGS = "respflags";
	public static final String EXTRA_DSFID = "dsfid";

	private byte mRespFlags;
	private byte mDsfId;

	public static NfcV get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NFC_V))
			return null;
		return new NfcV(tag);
	}

	public NfcV(Tag tag) {
		super(tag, TagTechnology.NFC_V);
		Bundle extras = tag.getTechExtras(TagTechnology.NFC_V);
		mRespFlags = extras.getByte(EXTRA_RESP_FLAGS);
		mDsfId = extras.getByte(EXTRA_DSFID);
	}

	public byte getResponseFlags() {
		return mRespFlags;
	}

	public byte getDsfId() {
		return mDsfId;
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}
}
