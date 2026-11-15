package android.service.notification;

import android.app.Notification;
import android.os.Parcel;
import android.os.UserHandle;

/* AOSP StatusBarNotification (android-16): a posted notification with its package, id, tag,
 * user and post time. Without parceling, cloning and the logging helpers. */
public class StatusBarNotification {
	private final String pkg;
	private final int id;
	private final String tag;
	private final String key;
	private String groupKey;
	private final int uid;
	private final String opPkg;
	private final int initialPid;
	private final Notification notification;
	private final UserHandle user;
	private final long postTime;
	private String overrideGroupKey;

	public StatusBarNotification(String pkg, String opPkg, int id, String tag, int uid, int initialPid, Notification notification, UserHandle user, String overrideGroupKey, long postTime) {
		if (pkg == null) throw new NullPointerException();
		if (notification == null) throw new NullPointerException();

		this.pkg = pkg;
		this.opPkg = opPkg;
		this.id = id;
		this.tag = tag;
		this.uid = uid;
		this.initialPid = initialPid;
		this.notification = notification;
		this.user = user;
		this.postTime = postTime;
		this.overrideGroupKey = overrideGroupKey;
		this.key = key();
		this.groupKey = groupKey();
	}

	@Deprecated
	public StatusBarNotification(String pkg, String opPkg, int id, String tag, int uid, int initialPid, int score, Notification notification, UserHandle user, long postTime) {
		this(pkg, opPkg, id, tag, uid, initialPid, notification, user, null, postTime);
	}

	/* AOSP StatusBarNotification(Parcel): the fields in the order of writeToParcel(). The log instance id that may
	 * follow is a single int in AOSP (InstanceId.writeToParcel), ATL has no such field and only reads it past */
	public StatusBarNotification(Parcel in) {
		this.pkg = in.readString();
		this.opPkg = in.readString();
		this.id = in.readInt();
		if (in.readInt() != 0) {
			this.tag = in.readString();
		} else {
			this.tag = null;
		}
		this.uid = in.readInt();
		this.initialPid = in.readInt();
		this.notification = new Notification(in);
		this.user = UserHandle.readFromParcel(in);
		this.postTime = in.readLong();
		if (in.readInt() != 0) {
			this.overrideGroupKey = in.readString();
		}
		if (in.readInt() != 0) {
			in.readInt();
		}
		this.key = key();
		this.groupKey = groupKey();
	}

	private String key() {
		String sbnKey = user.getIdentifier() + "|" + pkg + "|" + id + "|" + tag + "|" + uid;
		if (overrideGroupKey != null && getNotification().isGroupSummary()) {
			sbnKey = sbnKey + "|" + overrideGroupKey;
		}
		return sbnKey;
	}

	private String groupKey() {
		if (overrideGroupKey != null) {
			return user.getIdentifier() + "|" + pkg + "|" + "g:" + overrideGroupKey;
		}
		final String group = getNotification().getGroup();
		final String sortKey = getNotification().getSortKey();
		if (group == null && sortKey == null) {
			// a group of one
			return key;
		}
		return user.getIdentifier() + "|" + pkg + "|" + (group == null ? "c:" + notification.getChannelId() : "g:" + group);
	}

	public boolean isGroup() {
		return overrideGroupKey != null || isAppGroup();
	}

	public boolean isAppGroup() {
		return getNotification().getGroup() != null || getNotification().getSortKey() != null;
	}

	public boolean isOngoing() {
		return (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
	}

	public boolean isClearable() {
		return ((notification.flags & Notification.FLAG_ONGOING_EVENT) == 0) && ((notification.flags & Notification.FLAG_NO_CLEAR) == 0);
	}

	@Deprecated
	public int getUserId() {
		return this.user.getIdentifier();
	}

	public String getPackageName() {
		return pkg;
	}

	public int getId() {
		return id;
	}

	public String getTag() {
		return tag;
	}

	public int getUid() {
		return uid;
	}

	public String getOpPkg() {
		return opPkg;
	}

	public int getInitialPid() {
		return initialPid;
	}

	public Notification getNotification() {
		return notification;
	}

	public UserHandle getUser() {
		return user;
	}

	public long getPostTime() {
		return postTime;
	}

	public String getKey() {
		return key;
	}

	public String getGroupKey() {
		return groupKey;
	}

	public String getGroup() {
		if (overrideGroupKey != null) {
			return overrideGroupKey;
		}
		return getNotification().getGroup();
	}

	public void setOverrideGroupKey(String overrideGroupKey) {
		this.overrideGroupKey = overrideGroupKey;
		groupKey = groupKey();
	}

	public String getOverrideGroupKey() {
		return overrideGroupKey;
	}

	@Override
	public String toString() {
		return String.format("StatusBarNotification(pkg=%s user=%s id=%d tag=%s key=%s: %s)", this.pkg, this.user, this.id, this.tag, this.key, this.notification);
	}
}
