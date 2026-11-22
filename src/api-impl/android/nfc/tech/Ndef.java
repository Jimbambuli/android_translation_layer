package android.nfc.tech;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.os.Bundle;
import java.io.IOException;

/* AOSP Ndef (modules/Nfc) for mock tags and neard tags (see android.nfc.Tag). Constants and the extras-based
 * getters follow AOSP. */
public final class Ndef extends BasicTagTechnology {
	public static final int NDEF_MODE_READ_ONLY = 1;
	public static final int NDEF_MODE_READ_WRITE = 2;
	public static final int NDEF_MODE_UNKNOWN = 3;

	public static final String EXTRA_NDEF_MSG = "ndefmsg";
	public static final String EXTRA_NDEF_MAXLENGTH = "ndefmaxlength";
	public static final String EXTRA_NDEF_CARDSTATE = "ndefcardstate";
	public static final String EXTRA_NDEF_TYPE = "ndeftype";

	public static final int TYPE_OTHER = -1;
	public static final int TYPE_1 = 1;
	public static final int TYPE_2 = 2;
	public static final int TYPE_3 = 3;
	public static final int TYPE_4 = 4;
	public static final int TYPE_MIFARE_CLASSIC = 101;
	public static final int TYPE_ICODE_SLI = 102;

	public static final String UNKNOWN = "android.ndef.unknown";
	public static final String NFC_FORUM_TYPE_1 = "org.nfcforum.ndef.type1";
	public static final String NFC_FORUM_TYPE_2 = "org.nfcforum.ndef.type2";
	public static final String NFC_FORUM_TYPE_3 = "org.nfcforum.ndef.type3";
	public static final String NFC_FORUM_TYPE_4 = "org.nfcforum.ndef.type4";
	public static final String MIFARE_CLASSIC = "com.nxp.ndef.mifareclassic";
	public static final String ICODE_SLI = "com.nxp.ndef.icodesli";

	private final int mMaxNdefSize;
	private final int mCardState;
	private final NdefMessage mNdefMsg;
	private final int mNdefType;

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
			mNdefMsg = extras.getParcelable(EXTRA_NDEF_MSG, NdefMessage.class);
			mNdefType = extras.getInt(EXTRA_NDEF_TYPE);
		} else {
			throw new NullPointerException("NDEF tech extras are null.");
		}
	}

	public NdefMessage getCachedNdefMessage() {
		return mNdefMsg;
	}

	public String getType() {
		switch (mNdefType) {
			case TYPE_1:
				return NFC_FORUM_TYPE_1;
			case TYPE_2:
				return NFC_FORUM_TYPE_2;
			case TYPE_3:
				return NFC_FORUM_TYPE_3;
			case TYPE_4:
				return NFC_FORUM_TYPE_4;
			case TYPE_MIFARE_CLASSIC:
				return MIFARE_CLASSIC;
			case TYPE_ICODE_SLI:
				return ICODE_SLI;
			default:
				return UNKNOWN;
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
		/* neard can write records (org.neard.Tag.Write) but not a raw message; INfcTag.ndefWrite() failing */
		if (mTag.getNeardPath() != null)
			throw new IOException();
		throw new IOException("Mock tags don't support this operation.");
	}

	/* AOSP getNdefMessage(): checkConnected() first, a mock tag has no service */
	public NdefMessage getNdefMessage() throws IOException, FormatException {
		checkConnected();
		if (mTag.getNeardPath() != null) {
			/* AOSP: ndefRead(), TagLostException when the tag has gone; neard read the message when the tag
			 * came into range and keeps it while the tag stays */
			if (!com.android.server.nfc.NeardAdapter.isTagPresent(mTag.getNeardPath()))
				throw new android.nfc.TagLostException();
			return mNdefMsg;
		}
		throw new IOException("Mock tags don't support this operation.");
	}

	/* AOSP canMakeReadOnly(): false without a tag service */
	public boolean canMakeReadOnly() {
		return false;
	}

	/* AOSP makeReadOnly(): checkConnected() first, then false without a tag service */
	public boolean makeReadOnly() throws IOException {
		checkConnected();
		return false;
	}
}
