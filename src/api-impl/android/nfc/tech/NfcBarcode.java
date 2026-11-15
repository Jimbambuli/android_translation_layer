package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP NfcBarcode (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NfcBarcode extends BasicTagTechnology {
	public static final int TYPE_KOVIO = 1;
	public static final int TYPE_UNKNOWN = -1;
	public static final String EXTRA_BARCODE_TYPE = "barcodetype";

	private int mType;

	public static NfcBarcode get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NFC_BARCODE))
			return null;
		return new NfcBarcode(tag);
	}

	public NfcBarcode(Tag tag) {
		super(tag, TagTechnology.NFC_BARCODE);
		Bundle extras = tag.getTechExtras(TagTechnology.NFC_BARCODE);
		if (extras != null) {
			mType = extras.getInt(EXTRA_BARCODE_TYPE);
		} else {
			throw new NullPointerException("NfcBarcode tech extras are null.");
		}
	}

	public int getType() {
		return mType;
	}

	/* AOSP getBarcode(): for Kovio tags the barcode matches the ID, null otherwise */
	public byte[] getBarcode() {
		switch (mType) {
			case TYPE_KOVIO:
				return mTag.getId();
			default:
				return null;
		}
	}
}
