package android.view.inputmethod;

public interface InputMethod {
	public static final int SHOW_EXPLICIT = 0x00001;
	public static final int SHOW_FORCED = 0x00002;

	public interface SessionCallback {
		public void sessionCreated(InputMethodSession session);
	}
}
