package android.os;

/* AOSP BadTypeParcelableException: the Parcelable read is not of the type the caller required */
class BadTypeParcelableException extends BadParcelableException {
	BadTypeParcelableException(String msg) {
		super(msg);
	}

	BadTypeParcelableException(Exception cause) {
		super(cause);
	}

	BadTypeParcelableException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
