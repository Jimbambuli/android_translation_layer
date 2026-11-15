package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP IsoDep (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class IsoDep extends BasicTagTechnology {
	public static final String EXTRA_HI_LAYER_RESP = "hiresp";
	public static final String EXTRA_HIST_BYTES = "histbytes";

	private byte[] mHiLayerResponse = null;
	private byte[] mHistBytes = null;

	public static IsoDep get(Tag tag) {
		if (!tag.hasTech(TagTechnology.ISO_DEP))
			return null;
		return new IsoDep(tag);
	}

	public IsoDep(Tag tag) {
		super(tag, TagTechnology.ISO_DEP);
		Bundle extras = tag.getTechExtras(TagTechnology.ISO_DEP);
		if (extras != null) {
			mHiLayerResponse = extras.getByteArray(EXTRA_HI_LAYER_RESP);
			mHistBytes = extras.getByteArray(EXTRA_HIST_BYTES);
		}
	}

	/* AOSP setTimeout(): mTag.getTagService() is null on a mock tag, NullPointerException */
	public void setTimeout(int timeout) {
		throw new NullPointerException();
	}

	/* AOSP getTimeout(): see setTimeout() */
	public int getTimeout() {
		throw new NullPointerException();
	}

	public byte[] getHistoricalBytes() {
		return mHistBytes;
	}

	public byte[] getHiLayerResponse() {
		return mHiLayerResponse;
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}

	/* AOSP isExtendedLengthApduSupported(): null service of a mock tag, NullPointerException */
	public boolean isExtendedLengthApduSupported() {
		throw new NullPointerException();
	}
}
