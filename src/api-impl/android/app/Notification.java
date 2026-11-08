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

	public String getGroup() { return null; }

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

		public Builder(Context context) {
			mContext = context;
			notification = new Notification();
		}

		public Builder(Context context, String channelId) {
			this(context);
			notification.channelId = channelId;
		}

		public Builder setWhen(long when) { return this; }

		public Builder setSmallIcon(int icon, int level) {
			/* the drawable's file, also through an alias (@drawable/a for @drawable/b) */
			android.util.TypedValue value = new android.util.TypedValue();
			try {
				mContext.getResources().getValue(icon, value, true);
				notification.iconPath = value.string != null ? value.string.toString() : null;
			} catch (android.content.res.Resources.NotFoundException e) {
				notification.iconPath = null;
			}
			return this;
		}

		public Builder setContent(RemoteViews contentView) { return this; }

		public Builder setTicker(CharSequence tickerText, RemoteViews contentView) { return this; }

		public Builder setVibrate(long[] pattern) { return this; }

		public Builder setLights(int argb, int onMs, int offMs) { return this; }

		public Builder setOngoing(boolean ongoing) {
			notification.ongoing = ongoing;
			return this;
		}

		public Builder setOnlyAlertOnce(boolean onlyAlertOnce) { return this; }

		public Builder setAutoCancel(boolean autoCancel) { return this; }

		public Builder setDefaults(int defaults) { return this; }

		public Builder setContentTitle(CharSequence title) {
			notification.title = title != null ? title.toString() : null;
			return this;
		}

		public Builder setContentText(CharSequence text) {
			notification.text = text != null ? text.toString() : null;
			return this;
		}

		public Builder setContentInfo(CharSequence info) { return this; }

		public Builder setContentIntent(PendingIntent intent) {
			notification.intent = intent;
			return this;
		}

		public Builder setDeleteIntent(PendingIntent intent) { return this; }

		public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) { return this; }

		public Builder setLargeIcon(Bitmap icon) { return this; }

		public Builder setNumber(int number) { return this; }

		public Builder setProgress(int max, int progress, boolean indeterminate) { return this; }

		public Builder setSubText(CharSequence subText) { return this; }

		public Builder setUsesChronometer(boolean useChronometer) { return this; }

		public Builder setPriority(int priority) {
			notification.priority = priority;
			return this;
		}

		public Builder setShowWhen(boolean showWhen) { return this; }

		public Builder setLocalOnly(boolean localOnly) { return this; }

		public Builder setGroup(String group) { return this; }

		public Builder setSortKey(String sortKey) { return this; }

		public Builder setGroupSummary(boolean isGroupSummary) { return this; }

		public Builder setCategory(String category) { return this; }

		public Builder setColor(int argb) { return this; }

		public Builder setVisibility(int visibility) { return this; }

		public Builder setPublicVersion(Notification notification) { return this; }

		public Builder setSound(Uri sound, AudioAttributes audioAttributes) { return this; }

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

		public Builder setExtras(Bundle extras) { return this; }

		public Builder setLargeIcon(Icon icon) { return this; }

		public Builder setRemoteInputHistory(CharSequence[] history) { return this; }

		public Builder setBadgeIconType(int iconType) { return this; }

		public Builder setSettingsText(CharSequence settingsText) { return this; }

		public Builder setShortcutId(String shortcutId) { return this; }

		public Builder setTimeoutAfter(long timeout) { return this; }

		public Builder setGroupAlertBehavior(int groupAlertBehavior) { return this; }

		public Builder setSound(Uri sound) { return this; }

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

		public Builder setTicker(CharSequence tickerText) { return this; }

		public Builder addExtras(Bundle bundle) { return this; }

		public Builder setColorized(boolean colorize) { return this; }

		public Builder setCustomContentView(RemoteViews contentView) { return this; }

		public Builder setCustomBigContentView(RemoteViews contentView) { return this; }

		public Builder setCustomHeadsUpContentView(RemoteViews contentView) { return this; }

		public Builder addPerson(String uri) { return this; }

		public Builder setAllowSystemGeneratedContextualActions(boolean allowed) { return this; }

		public Builder setForegroundServiceBehavior(int behavior) { return this; }

		/* API 29: there are no bubbles, the notification is shown as a normal one */
		public Builder setBubbleMetadata(BubbleMetadata data) { return this; }

		public Notification build() {
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
