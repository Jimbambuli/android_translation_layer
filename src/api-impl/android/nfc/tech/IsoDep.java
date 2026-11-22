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

	/* AOSP setTimeout(): INfcTag.setTimeout(); NullPointerException for a mock tag (no service) */
	public void setTimeout(int timeout) {
		setTimeoutInternal(TagTechnology.ISO_DEP, timeout);
	}

	/* AOSP getTimeout(): see setTimeout() */
	public int getTimeout() {
		return getTimeoutInternal(TagTechnology.ISO_DEP);
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

	/* AOSP isExtendedLengthApduSupported(): null service of a mock tag, NullPointerException; neard sends no APDUs */
	public boolean isExtendedLengthApduSupported() {
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		return false;
	}
}
