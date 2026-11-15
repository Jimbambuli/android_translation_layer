package android.os;

import android.util.AndroidRuntimeException;

public class BadParcelableException extends AndroidRuntimeException {
	public BadParcelableException(String msg) {
		super(msg);
	}

	public BadParcelableException(Exception cause) {
		super(cause);
	}

	/** @hide AOSP BadParcelableException(String, Throwable) */
	public BadParcelableException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
