package android.view;

public class InflateException extends RuntimeException {

	public InflateException() {
		super();
	}

	public InflateException(Throwable cause) {
		super(cause);
	}

	public InflateException(String string, Throwable cause) {
		super(string, cause);
	}

	public InflateException(String string, Exception e) {
		super(string, e);
	}

	public InflateException(String string) {
		super(string);
	}
}
