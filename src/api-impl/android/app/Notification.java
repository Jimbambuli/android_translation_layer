package android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;

public class Notification implements Parcelable {

	public static final AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder()
	                                                                               .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
	                                                                               .setUsage(AudioAttributes.USAGE_NOTIFICATION)
	                                                                               .build();

	public long when;

	public int audioStreamType;

	public int icon;

	public PendingIntent deleteIntent;

	public CharSequence tickerText;

	public int iconLevel;

	public RemoteViews bigContentView;

	public RemoteViews contentView;

	public RemoteViews headsUpContentView;

	public long[] vibrate;

	public int ledARGB;

	public int ledOnMS;

	public int ledOffMS;

	public int flags;

	public int defaults;

	public Uri sound;

	public AudioAttributes audioAttributes;

	public Bundle extras;

	public String category;

	public static final int PRIORITY_DEFAULT = 0;
	public static final int PRIORITY_LOW = -1;
	public static final int PRIORITY_MIN = -2;
	public static final int PRIORITY_HIGH = 1;
	public static final int PRIORITY_MAX = 2;

	public int priority;

	String channelId;

	String text;
	String title;
	List<Action> actions = new ArrayList<Action>();
	PendingIntent intent;
	String iconPath;
	boolean ongoing;
	Style style;

	public String toString() {
		return "Notification [" + title + ", " + text + ", " + actions + "]";
	}

	public static final int DEFAULT_ALL = ~0;
	public static final int DEFAULT_SOUND = 1;
	public static final int DEFAULT_VIBRATE = 2;
	public static final int DEFAULT_LIGHTS = 4;

	public static final int FLAG_SHOW_LIGHTS = 0x00000001;
	public static final int FLAG_ONGOING_EVENT = 0x00000002;
	public static final int FLAG_INSISTENT = 0x00000004;
	public static final int FLAG_ONLY_ALERT_ONCE = 0x00000008;
	public static final int FLAG_AUTO_CANCEL = 0x00000010;
	public static final int FLAG_NO_CLEAR = 0x00000020;
	public static final int FLAG_FOREGROUND_SERVICE = 0x00000040;
	public static final int FLAG_HIGH_PRIORITY = 0x00000080;
	public static final int FLAG_LOCAL_ONLY = 0x00000100;
	public static final int FLAG_GROUP_SUMMARY = 0x00000200;
	public static final int FLAG_BUBBLE = 0x00001000;

	public static final int VISIBILITY_PUBLIC = 1;
	public static final int VISIBILITY_PRIVATE = 0;
	public static final int VISIBILITY_SECRET = -1;

	public static final int GROUP_ALERT_ALL = 0;
	public static final int GROUP_ALERT_SUMMARY = 1;
	public static final int GROUP_ALERT_CHILDREN = 2;

	public static final int BADGE_ICON_NONE = 0;
	public static final int BADGE_ICON_SMALL = 1;
	public static final int BADGE_ICON_LARGE = 2;

	public static final String EXTRA_INFO_TEXT = "android.infoText";
	public static final String EXTRA_SUB_TEXT = "android.subText";
	public static final String EXTRA_SHOW_WHEN = "android.showWhen";
	public static final String EXTRA_SHOW_CHRONOMETER = "android.showChronometer";
	public static final String EXTRA_PROGRESS = "android.progress";
	public static final String EXTRA_PROGRESS_MAX = "android.progressMax";
	public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";
	public static final String EXTRA_PEOPLE = "android.people";
	public static final String EXTRA_BACKGROUND_IMAGE_URI = "android.backgroundImageUri";
	public static final String EXTRA_COLORIZED = "android.colorized";
	public static final String EXTRA_REMOTE_INPUT_HISTORY = "android.remoteInputHistory";

	public int number;

	public int color;

	public int visibility;

	public PendingIntent fullScreenIntent;

	public Notification publicVersion;

	private String mGroupKey;
	private String mSortKey;
	private int mGroupAlertBehavior = GROUP_ALERT_ALL;
	private int mBadgeIcon = BADGE_ICON_NONE;
	private long mTimeout;
	private String mShortcutId;
	private CharSequence mSettingsText;
	private Icon mLargeIcon;
	private BubbleMetadata mBubbleMetadata;
	private boolean mAllowSystemGeneratedContextualActions = true;
	private int mForegroundServiceBehavior;

	public String getGroup() { return mGroupKey; }

	public String getSortKey() { return mSortKey; }

	public int getGroupAlertBehavior() { return mGroupAlertBehavior; }

	public int getBadgeIconType() { return mBadgeIcon; }

	public long getTimeoutAfter() { return mTimeout; }

	public String getShortcutId() { return mShortcutId; }

	public CharSequence getSettingsText() { return mSettingsText; }

	public Icon getLargeIcon() { return mLargeIcon; }

	public BubbleMetadata getBubbleMetadata() { return mBubbleMetadata; }

	public boolean getAllowSystemGeneratedContextualActions() { return mAllowSystemGeneratedContextualActions; }

	public int getForegroundServiceBehavior() { return mForegroundServiceBehavior; }

	public boolean isGroupSummary() { return (flags & FLAG_GROUP_SUMMARY) != 0; }

	public String getChannelId() {
		return channelId;
	}

	android.graphics.drawable.Icon mSmallIcon;

	public android.graphics.drawable.Icon getSmallIcon() {
		return mSmallIcon;
	}

	public static class Builder {
		private final Context mContext;
		private Notification notification;
		/* AOSP's mUserExtras and the extras the setters make, which build() puts together */
		private Bundle mUserExtras = new Bundle();
		private Bundle mExtras = new Bundle();

		public Builder(Context context) {
			mContext = context;
			notification = new Notification();
			notification.when = System.currentTimeMillis();
			notification.priority = PRIORITY_DEFAULT;
			notification.visibility = VISIBILITY_PRIVATE;
		}

		public Builder(Context context, String channelId) {
			this(context);
			notification.channelId = channelId;
		}

		private void setFlag(int mask, boolean value) {
			if (value) {
				notification.flags |= mask;
			} else {
				notification.flags &= ~mask;
			}
		}

		public Builder setWhen(long when) {
			notification.when = when;
			return this;
		}

		public Builder setSmallIcon(int icon, int level) {
			/* the drawable's file, also through an alias (@drawable/a for @drawable/b) */
			android.util.TypedValue value = new android.util.TypedValue();
			try {
				mContext.getResources().getValue(icon, value, true);
				notification.iconPath = value.string != null ? value.string.toString() : null;
			} catch (android.content.res.Resources.NotFoundException e) {
				notification.iconPath = null;
			}
			notification.icon = icon;
			notification.iconLevel = level;
			return this;
		}

		public Builder setContent(RemoteViews contentView) {
			notification.contentView = contentView;
			return this;
		}

		public Builder setTicker(CharSequence tickerText, RemoteViews contentView) {
			notification.tickerText = tickerText;
			return this;
		}

		public Builder setVibrate(long[] pattern) {
			notification.vibrate = pattern;
			notification.defaults &= ~DEFAULT_VIBRATE;
			return this;
		}

		public Builder setLights(int argb, int onMs, int offMs) {
			notification.ledARGB = argb;
			notification.ledOnMS = onMs;
			notification.ledOffMS = offMs;
			if (onMs != 0 || offMs != 0) {
				notification.flags |= FLAG_SHOW_LIGHTS;
			}
			return this;
		}

		public Builder setOngoing(boolean ongoing) {
			notification.ongoing = ongoing;
			setFlag(FLAG_ONGOING_EVENT, ongoing);
			return this;
		}

		public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
			setFlag(FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
			return this;
		}

		public Builder setAutoCancel(boolean autoCancel) {
			setFlag(FLAG_AUTO_CANCEL, autoCancel);
			return this;
		}

		public Builder setDefaults(int defaults) {
			notification.defaults = defaults;
			return this;
		}

		public Builder setContentTitle(CharSequence title) {
			notification.title = title != null ? title.toString() : null;
			return this;
		}

		public Builder setContentText(CharSequence text) {
			notification.text = text != null ? text.toString() : null;
			return this;
		}

		public Builder setContentInfo(CharSequence info) {
			mExtras.putCharSequence(EXTRA_INFO_TEXT, info);
			return this;
		}

		public Builder setContentIntent(PendingIntent intent) {
			notification.intent = intent;
			return this;
		}

		public Builder setDeleteIntent(PendingIntent intent) {
			notification.deleteIntent = intent;
			return this;
		}

		public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
			notification.fullScreenIntent = intent;
			setFlag(FLAG_HIGH_PRIORITY, highPriority);
			return this;
		}

		public Builder setLargeIcon(Bitmap icon) {
			return setLargeIcon(icon != null ? Icon.createWithBitmap(icon) : null);
		}

		public Builder setNumber(int number) {
			notification.number = number;
			return this;
		}

		public Builder setProgress(int max, int progress, boolean indeterminate) {
			mExtras.putInt(EXTRA_PROGRESS, progress);
			mExtras.putInt(EXTRA_PROGRESS_MAX, max);
			mExtras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, indeterminate);
			return this;
		}

		public Builder setSubText(CharSequence subText) {
			mExtras.putCharSequence(EXTRA_SUB_TEXT, subText);
			return this;
		}

		public Builder setUsesChronometer(boolean useChronometer) {
			mExtras.putBoolean(EXTRA_SHOW_CHRONOMETER, useChronometer);
			return this;
		}

		public Builder setPriority(int priority) {
			notification.priority = priority;
			return this;
		}

		public Builder setShowWhen(boolean showWhen) {
			mExtras.putBoolean(EXTRA_SHOW_WHEN, showWhen);
			return this;
		}

		public Builder setLocalOnly(boolean localOnly) {
			setFlag(FLAG_LOCAL_ONLY, localOnly);
			return this;
		}

		public Builder setGroup(String groupKey) {
			notification.mGroupKey = groupKey;
			return this;
		}

		public Builder setSortKey(String sortKey) {
			notification.mSortKey = sortKey;
			return this;
		}

		public Builder setGroupSummary(boolean isGroupSummary) {
			setFlag(FLAG_GROUP_SUMMARY, isGroupSummary);
			return this;
		}

		public Builder setCategory(String category) {
			notification.category = category;
			return this;
		}

		public Builder setColor(int argb) {
			notification.color = argb;
			return this;
		}

		public Builder setVisibility(int visibility) {
			notification.visibility = visibility;
			return this;
		}

		public Builder setPublicVersion(Notification publicVersion) {
			notification.publicVersion = publicVersion;
			return this;
		}

		public Builder setSound(Uri sound, AudioAttributes audioAttributes) {
			notification.sound = sound;
			notification.audioAttributes = audioAttributes;
			return this;
		}

		public Builder addAction(Action action) {
			notification.actions.add(action);
			return this;
		}

		public Builder setStyle(Style style) {
			notification.style = style;
			if (style instanceof MediaStyle) {
				notification.ongoing = true;
			}
			return this;
		}

		public Builder setExtras(Bundle extras) {
			mUserExtras = extras;
			return this;
		}

		public Bundle getExtras() {
			if (mUserExtras == null) {
				mUserExtras = new Bundle();
			}
			return mUserExtras;
		}

		public Builder setLargeIcon(Icon icon) {
			notification.mLargeIcon = icon;
			return this;
		}

		public Builder setRemoteInputHistory(CharSequence[] history) {
			if (history == null) {
				mExtras.putCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY, null);
			} else {
				final int itemCount = Math.min(5, history.length);
				CharSequence[] text = new CharSequence[itemCount];
				System.arraycopy(history, 0, text, 0, itemCount);
				mExtras.putCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY, text);
			}
			return this;
		}

		public Builder setBadgeIconType(int icon) {
			notification.mBadgeIcon = icon;
			return this;
		}

		public Builder setSettingsText(CharSequence settingsText) {
			notification.mSettingsText = settingsText;
			return this;
		}

		public Builder setShortcutId(String shortcutId) {
			notification.mShortcutId = shortcutId;
			return this;
		}

		public Builder setTimeoutAfter(long durationMs) {
			notification.mTimeout = durationMs;
			return this;
		}

		public Builder setGroupAlertBehavior(int groupAlertBehavior) {
			notification.mGroupAlertBehavior = groupAlertBehavior;
			return this;
		}

		public Builder setSound(Uri sound) {
			notification.sound = sound;
			notification.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
			return this;
		}

		public Builder setSmallIcon(int icon) { return this.setSmallIcon(icon, -1); }

		/* AOSP Notification.Builder.setSmallIcon(Icon): ATL shows the notification with the icon's file; that is
		 * known for an icon of a resource, a bitmap icon has no file and the notification goes without one */
		public Builder setSmallIcon(android.graphics.drawable.Icon icon) {
			notification.mSmallIcon = icon;
			if (icon != null && icon.getType() == android.graphics.drawable.Icon.TYPE_RESOURCE) {
				return setSmallIcon(icon.getResId(), -1);
			}
			notification.iconPath = null;
			return this;
		}

		public Builder setTicker(CharSequence tickerText) {
			notification.tickerText = tickerText;
			return this;
		}

		public Builder addExtras(Bundle extras) {
			if (extras != null) {
				if (mUserExtras == null) {
					mUserExtras = new Bundle(extras);
				} else {
					mUserExtras.putAll(extras);
				}
			}
			return this;
		}

		public Builder setColorized(boolean colorize) {
			mExtras.putBoolean(EXTRA_COLORIZED, colorize);
			return this;
		}

		public Builder setCustomContentView(RemoteViews contentView) {
			notification.contentView = contentView;
			return this;
		}

		public Builder setCustomBigContentView(RemoteViews contentView) {
			notification.bigContentView = contentView;
			return this;
		}

		public Builder setCustomHeadsUpContentView(RemoteViews contentView) {
			notification.headsUpContentView = contentView;
			return this;
		}

		public Builder addPerson(String uri) {
			ArrayList<String> people = mExtras.getStringArrayList(EXTRA_PEOPLE);
			if (people == null) {
				people = new ArrayList<>();
				mExtras.putStringArrayList(EXTRA_PEOPLE, people);
			}
			people.add(uri);
			return this;
		}

		public Builder setAllowSystemGeneratedContextualActions(boolean allowed) {
			notification.mAllowSystemGeneratedContextualActions = allowed;
			return this;
		}

		public Builder setForegroundServiceBehavior(int behavior) {
			notification.mForegroundServiceBehavior = behavior;
			return this;
		}

		/* API 29: there are no bubbles, the notification is shown as a normal one */
		public Builder setBubbleMetadata(BubbleMetadata data) {
			notification.mBubbleMetadata = data;
			return this;
		}

		/* AOSP: the extras of the setters, over them what the app put in with setExtras() and addExtras() */
		public Notification build() {
			Bundle extras = new Bundle(mExtras);
			if (mUserExtras != null) {
				extras.putAll(mUserExtras);
			}
			notification.extras = extras;
			if ((notification.defaults & DEFAULT_LIGHTS) != 0) {
				notification.flags |= FLAG_SHOW_LIGHTS;
			}
			return notification;
		}
	}

	public static class Action {

		int icon;
		String title;
		PendingIntent intent;
		List<RemoteInput> remoteInputs = new ArrayList<RemoteInput>();

		public RemoteInput[] getRemoteInputs() {
			return remoteInputs.isEmpty() ? null : remoteInputs.toArray(new RemoteInput[0]);
		}

		public String toString() {
			return "Action [" + icon + ", " + title + ", " + intent + "]";
		}

		public static final class Builder {

			private Action action;
			public Builder(int icon, CharSequence title, PendingIntent intent) {
				action = new Action();
				action.icon = icon;
				action.title = String.valueOf(title);
				action.intent = intent;
			}

			public Builder(Icon icon, CharSequence title, PendingIntent intent) {
				this(0, title, intent);
			}

			public Builder addExtras(Bundle extras) { return this; }

			public Builder addRemoteInput(RemoteInput remoteInput) {
				action.remoteInputs.add(remoteInput);
				return this;
			}

			public Builder setAllowGeneratedReplies(boolean allowGeneratedReplies) { return this; }

			public Builder setSemanticAction(int semanticAction) { return this; }

			public Builder setContextual(boolean isContextual) { return this; }

			public Builder setAuthenticationRequired(boolean authenticationRequired) { return this; }

			public Action build() {
				return action;
			}
		}
	}

	public static abstract class Style {}

	public static class MediaStyle extends Style {

		public MediaStyle setShowActionsInCompactView(int... viewActions) { return this; }

		public MediaStyle setMediaSession(MediaSession.Token token) { return this; }
	}

	public static class BigTextStyle extends Style {

		public BigTextStyle(Notification.Builder builder) {}

		public BigTextStyle setBigContentTitle(CharSequence title) { return this; }

		public BigTextStyle bigText(CharSequence text) { return this; }

		public BigTextStyle setSummaryText(CharSequence summaryText) { return this; }
	}

	public static class InboxStyle extends Style {

		public InboxStyle(Notification.Builder builder) {}

		public InboxStyle setBigContentTitle(CharSequence title) { return this; }

		public InboxStyle setSummaryText(CharSequence summaryText) { return this; }

		public InboxStyle addLine(CharSequence line) { return this; }
	}

	/* API 29/30. Kept only so that apps can build it; ATL shows no bubbles. */
	public static final class BubbleMetadata {
		private BubbleMetadata() {}

		public static final class Builder {
			public Builder() {}
			public Builder(String shortcutId) {}
			public Builder(PendingIntent intent, android.graphics.drawable.Icon icon) {}

			public Builder setIntent(PendingIntent intent) { return this; }
			public Builder setIcon(android.graphics.drawable.Icon icon) { return this; }
			public Builder setDesiredHeight(int height) { return this; }
			public Builder setDesiredHeightResId(int heightResId) { return this; }
			public Builder setAutoExpandBubble(boolean shouldExpand) { return this; }
			public Builder setSuppressNotification(boolean shouldSuppressNotif) { return this; }
			public Builder setSuppressableBubble(boolean suppressBubble) { return this; }
			public Builder setDeleteIntent(PendingIntent deleteIntent) { return this; }

			public BubbleMetadata build() {
				return new BubbleMetadata();
			}
		}
	}
}
