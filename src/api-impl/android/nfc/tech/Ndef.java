package android.nfc.tech;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP Ndef (modules/Nfc), reduced to mock tags (see android.nfc.Tag): ATL has no NFC service, so no Tag from a
 * device exists. Constants and the extras-based getters follow AOSP. */
public final class Ndef extends BasicTagTechnology {
	public static final int NDEF_MODE_READ_ONLY = 1;
	public static final int NDEF_MODE_READ_WRITE = 2;
	public static final int NDEF_MODE_UNKNOWN = 3;

	public static final String EXTRA_NDEF_MSG = "ndefmsg";
	public static final String EXTRA_NDEF_MAXLENGTH = "ndefmaxlength";
	public static final String EXTRA_NDEF_CARDSTATE = "ndefcardstate";
	public static final String EXTRA_NDEF_TYPE = "ndeftype";

	private final int mMaxNdefSize;
	private final int mCardState;

	/* AOSP Ndef.get(): null if the tag lacks the NDEF technology */
	public static Ndef get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NDEF))
			return null;
		return new Ndef(tag);
	}

	/* AOSP Ndef(Tag): reads the tech extras, NullPointerException if there are none */
	public Ndef(Tag tag) {
		super(tag, TagTechnology.NDEF);
		Bundle extras = tag.getTechExtras(TagTechnology.NDEF);
		if (extras != null) {
			mMaxNdefSize = extras.getInt(EXTRA_NDEF_MAXLENGTH);
			mCardState = extras.getInt(EXTRA_NDEF_CARDSTATE);
		} else {
			throw new NullPointerException("NDEF tech extras are null.");
		}
	}

	public int getMaxSize() {
		return mMaxNdefSize;
	}

	public boolean isWritable() {
		return (mCardState == NDEF_MODE_READ_WRITE);
	}

	/* AOSP writeNdefMessage(): checkConnected() first; a mock tag has no service ("Mock tags don't support this
	 * operation."), but checkConnected() already throws, as connect() cannot succeed on a mock tag */
	public void writeNdefMessage(NdefMessage msg) throws IOException, FormatException {
		checkConnected();
		throw new IOException("Mock tags don't support this operation.");
	}
}
