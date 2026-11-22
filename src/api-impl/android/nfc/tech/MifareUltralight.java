package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP MifareUltralight (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class MifareUltralight extends BasicTagTechnology {
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_ULTRALIGHT = 1;
	public static final int TYPE_ULTRALIGHT_C = 2;
	public static final int PAGE_SIZE = 4;
	public static final String EXTRA_IS_UL_C = "isulc";

	private static final int NXP_MANUFACTURER_ID = 0x04;
	private static final int MAX_PAGE_COUNT = 256;

	private int mType;

	public static MifareUltralight get(Tag tag) {
		if (!tag.hasTech(TagTechnology.MIFARE_ULTRALIGHT))
			return null;
		return new MifareUltralight(tag);
	}

	public MifareUltralight(Tag tag) {
		super(tag, TagTechnology.MIFARE_ULTRALIGHT);
		/* AOSP: NfcA.get(tag) is null (NullPointerException) if the tag lacks NFC_A */
		NfcA a = NfcA.get(tag);
		mType = TYPE_UNKNOWN;
		if (a.getSak() == 0x00 && tag.getId()[0] == NXP_MANUFACTURER_ID) {
			Bundle extras = tag.getTechExtras(TagTechnology.MIFARE_ULTRALIGHT);
			if (extras.getBoolean(EXTRA_IS_UL_C)) {
				mType = TYPE_ULTRALIGHT_C;
			} else {
				mType = TYPE_ULTRALIGHT;
			}
		}
	}

	public int getType() {
		return mType;
	}

	public byte[] readPages(int pageOffset) throws IOException {
		validatePageIndex(pageOffset);
		checkConnected();
		byte[] cmd = {0x30, (byte)pageOffset};
		return transceive(cmd, false);
	}

	public void writePage(int pageOffset, byte[] data) throws IOException {
		validatePageIndex(pageOffset);
		checkConnected();
		byte[] cmd = new byte[data.length + 2];
		cmd[0] = (byte)0xA2;
		cmd[1] = (byte)pageOffset;
		System.arraycopy(data, 0, cmd, 2, data.length);
		transceive(cmd, false);
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}

	/* AOSP setTimeout(): INfcTag.setTimeout(); NullPointerException for a mock tag (no service) */
	public void setTimeout(int timeout) {
		setTimeoutInternal(TagTechnology.MIFARE_ULTRALIGHT, timeout);
	}

	/* AOSP getTimeout(): see setTimeout() */
	public int getTimeout() {
		return getTimeoutInternal(TagTechnology.MIFARE_ULTRALIGHT);
	}

	private static void validatePageIndex(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= MAX_PAGE_COUNT) {
			throw new IndexOutOfBoundsException("page out of bounds: " + pageIndex);
		}
	}
}
