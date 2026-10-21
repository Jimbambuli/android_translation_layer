package android.media;

/* API 18. Base checked exception for the MediaDrm family. */
public class MediaDrmException extends Exception {

	public MediaDrmException(String detailMessage) {
		super(detailMessage);
	}
}
