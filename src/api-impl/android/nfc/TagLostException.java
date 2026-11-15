package android.nfc;

import java.io.IOException;

/* AOSP TagLostException (modules/Nfc) */
public class TagLostException extends IOException {
	public TagLostException() {
		super();
	}

	public TagLostException(String message) {
		super(message);
	}
}
