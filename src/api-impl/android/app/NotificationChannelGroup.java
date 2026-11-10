package android.app;

public class NotificationChannelGroup {
	private final String mId;

	public NotificationChannelGroup(String a, CharSequence b) {
		mId = a;
	}

	public String getId() {
		return mId;
	}

	public void setDescription(String description) {}
}
