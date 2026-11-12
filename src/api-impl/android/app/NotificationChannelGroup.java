package android.app;

import java.util.ArrayList;
import java.util.List;

/* AOSP NotificationChannelGroup: id, name and description (cut to MAX_TEXT_LENGTH), and the channels of the group
 * as NotificationManager.getNotificationChannelGroups() fills them in */
public class NotificationChannelGroup {
	private static final int MAX_TEXT_LENGTH = 1000;

	private final String mId;
	private CharSequence mName;
	private String mDescription;
	private boolean mBlocked;
	private List<NotificationChannel> mChannels = new ArrayList<>();

	public NotificationChannelGroup(String id, CharSequence name) {
		mId = getTrimmedString(id);
		mName = name != null ? getTrimmedString(name.toString()) : null;
	}

	private static String getTrimmedString(String input) {
		if (input != null && input.length() > MAX_TEXT_LENGTH) {
			return input.substring(0, MAX_TEXT_LENGTH);
		}
		return input;
	}

	public String getId() {
		return mId;
	}

	public CharSequence getName() {
		return mName;
	}

	public String getDescription() {
		return mDescription;
	}

	public List<NotificationChannel> getChannels() {
		return mChannels;
	}

	public boolean isBlocked() {
		return mBlocked;
	}

	public void setDescription(String description) {
		mDescription = getTrimmedString(description);
	}

	public void setBlocked(boolean blocked) {
		mBlocked = blocked;
	}

	public void addChannel(NotificationChannel channel) {
		mChannels.add(channel);
	}

	public void setChannels(List<NotificationChannel> channels) {
		mChannels = channels;
	}

	/* the group as the service hands it out: name, description and the given channels */
	NotificationChannelGroup copyWith(List<NotificationChannel> channels) {
		NotificationChannelGroup g = new NotificationChannelGroup(mId, mName);
		g.mDescription = mDescription;
		g.mBlocked = mBlocked;
		g.mChannels = channels;
		return g;
	}
}
