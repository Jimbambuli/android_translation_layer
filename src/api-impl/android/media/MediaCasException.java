/* AOSP frameworks/base/media/java/android/media/MediaCasException.java
 * ATL has no CAS plugin service, as on a device without one: no CA system is supported and every
 * constructor throws UnsupportedCasException ("Unsupported casSystemId N"). */
package android.media;

public class MediaCasException extends Exception {
	private MediaCasException(String detailMessage) {
		super(detailMessage);
	}

	public static final class UnsupportedCasException extends MediaCasException {
		public UnsupportedCasException(String detailMessage) {
			super(detailMessage);
		}
	}

	public static final class NotProvisionedException extends MediaCasException {
		public NotProvisionedException(String detailMessage) {
			super(detailMessage);
		}
	}

	public static final class DeniedByServerException extends MediaCasException {
		public DeniedByServerException(String detailMessage) {
			super(detailMessage);
		}
	}

	public static final class ResourceBusyException extends MediaCasException {
		public ResourceBusyException(String detailMessage) {
			super(detailMessage);
		}
	}

	public static final class InsufficientResourceException extends MediaCasException {
		public InsufficientResourceException(String detailMessage) {
			super(detailMessage);
		}
	}
}
