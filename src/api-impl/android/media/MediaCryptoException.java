package android.media;

/* AOSP android.media.MediaCryptoException, adapted (no @Nullable annotation). */
public final class MediaCryptoException extends Exception implements MediaDrmThrowable {

	private final int mVendorError, mOemError, mErrorContext;

	public MediaCryptoException(String detailMessage) {
		this(detailMessage, 0, 0, 0);
	}

	public MediaCryptoException(String message, int vendorError, int oemError, int errorContext) {
		super(message);
		mVendorError = vendorError;
		mOemError = oemError;
		mErrorContext = errorContext;
	}

	@Override
	public int getVendorError() {
		return mVendorError;
	}

	@Override
	public int getOemError() {
		return mOemError;
	}

	@Override
	public int getErrorContext() {
		return mErrorContext;
	}
}
