package android.app;

import java.util.ArrayList;
import java.util.List;

/* AOSP NotificationChannelGroup: id, name and description (cut to MAX_TEXT_LENGTH), and the channels of the group
 * as NotificationManager.getNotificationChannelGroups() fills them in */
public final class NotificationChannelGroup implements android.os.Parcelable {
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

	/* AOSP NotificationChannelGroup.writeToParcel()/NotificationChannelGroup(Parcel)/CREATOR (Android 16).
	 * The channels go through ParceledListSlice in AOSP (not in ATL): written with writeTypedList() after
	 * the same presence byte. ATL keeps no user locked fields (written as 0, skipped when read) */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(android.os.Parcel dest, int flags) {
		if (mId != null) {
			dest.writeByte((byte)1);
			dest.writeString(mId);
		} else {
			dest.writeByte((byte)0);
		}
		if (mName != null) {
			dest.writeByte((byte)1);
			dest.writeString(mName.toString());
		} else {
			dest.writeByte((byte)0);
		}
		if (mDescription != null) {
			dest.writeByte((byte)1);
			dest.writeString(mDescription);
		} else {
			dest.writeByte((byte)0);
		}
		if (mChannels != null) {
			dest.writeByte((byte)1);
			dest.writeTypedList(mChannels, flags);
		} else {
			dest.writeByte((byte)0);
		}
		dest.writeBoolean(mBlocked);
		dest.writeInt(0 /* mUserLockedFields */);
	}

	private NotificationChannelGroup(android.os.Parcel in) {
		if (in.readByte() != 0) {
			mId = getTrimmedString(in.readString());
		} else {
			mId = null;
		}
		if (in.readByte() != 0) {
			mName = getTrimmedString(in.readString());
		} else {
			mName = "";
		}
		if (in.readByte() != 0) {
			mDescription = getTrimmedString(in.readString());
		} else {
			mDescription = null;
		}
		if (in.readByte() != 0) {
			mChannels = in.createTypedArrayList(NotificationChannel.CREATOR);
		} else {
			mChannels = new ArrayList<>();
		}
		mBlocked = in.readBoolean();
		in.readInt(); // mUserLockedFields
	}

	public static final android.os.Parcelable.Creator<NotificationChannelGroup> CREATOR = new android.os.Parcelable.Creator<NotificationChannelGroup>() {
		@Override
		public NotificationChannelGroup createFromParcel(android.os.Parcel in) {
			return new NotificationChannelGroup(in);
		}

		@Override
		public NotificationChannelGroup[] newArray(int size) {
			return new NotificationChannelGroup[size];
		}
	};
}
