package android.nfc.tech;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import java.io.IOException;

/* AOSP NdefFormatable (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class NdefFormatable extends BasicTagTechnology {
	/* AOSP NdefFormatable.get(): null if the tag lacks the NDEF_FORMATABLE technology */
	public static NdefFormatable get(Tag tag) {
		if (!tag.hasTech(TagTechnology.NDEF_FORMATABLE))
			return null;
		return new NdefFormatable(tag);
	}

	public NdefFormatable(Tag tag) {
		super(tag, TagTechnology.NDEF_FORMATABLE);
	}

	/* AOSP format(firstMessage) -> format(firstMessage, false): checkConnected() first, which throws
	 * IllegalStateException("Call connect() first!") as connect() cannot succeed on a mock tag */
	public void format(NdefMessage firstMessage) throws IOException, FormatException {
		checkConnected();
		throw new IOException();
	}

	/* AOSP formatReadOnly(firstMessage) -> format(firstMessage, true) */
	public void formatReadOnly(NdefMessage firstMessage) throws IOException, FormatException {
		checkConnected();
		throw new IOException();
	}
}
