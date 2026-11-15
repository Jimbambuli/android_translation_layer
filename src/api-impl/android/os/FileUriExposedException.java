package android.os;

/* AOSP FileUriExposedException: thrown by StrictMode.onFileUriExposed() */
public class FileUriExposedException extends RuntimeException {
	public FileUriExposedException(String message) {
		super(message);
	}
}
