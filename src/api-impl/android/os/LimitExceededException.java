package android.os;

/* AOSP LimitExceededException */
public class LimitExceededException extends IllegalStateException {
	public LimitExceededException() {
		super();
	}

	public LimitExceededException(String message) {
		super(message);
	}
}
