package android.media;

/**
 * Base class for MediaDrm exceptions
 */
public class MediaDrmException extends Exception implements MediaDrmThrowable {
	public MediaDrmException(String detailMessage) {
		this(detailMessage, 0, 0, 0);
	}

	/**
	 * @hide
	 */
	public MediaDrmException(String message, int vendorError, int oemError, int errorContext) {
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

	private final int mVendorError, mOemError, mErrorContext;
}
