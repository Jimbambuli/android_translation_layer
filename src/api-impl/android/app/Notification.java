package android.app;

import android.content.Context;
import android.content.LocusId;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Notification implements Parcelable {

	public static final AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder()
	                                                                               .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
	                                                                               .setUsage(AudioAttributes.USAGE_NOTIFICATION)
	                                                                               .build();

	public long when;

	/* AOSP: the legacy large icon; Builder.build() turns it into mLargeIcon when none is set */
	@Deprecated
	public Bitmap largeIcon;

	public Notification() {}

	/* AOSP Notification.clone() -> cloneInto(that, true): a heavy copy; extras, actions, vibrate, the RemoteViews
	 * and publicVersion are copied, PendingIntents, icons and the immutable values are shared */
	@Override
	public Notification clone() {
		Notification that = new Notification();
		that.when = when;
		that.largeIcon = largeIcon;
		that.audioStreamType = audioStreamType;
		that.icon = icon;
		that.deleteIntent = deleteIntent;
		that.tickerText = tickerText;
		that.iconLevel = iconLevel;
		that.bigContentView = bigContentView != null ? bigContentView.clone() : null;
		that.contentView = contentView != null ? contentView.clone() : null;
		that.headsUpContentView = headsUpContentView != null ? headsUpContentView.clone() : null;
		that.vibrate = vibrate != null ? vibrate.clone() : null;
		that.ledARGB = ledARGB;
		that.ledOnMS = ledOnMS;
		that.ledOffMS = ledOffMS;
		that.flags = flags;
		that.defaults = defaults;
		that.sound = sound;
		that.audioAttributes = audioAttributes;
		that.extras = extras != null ? new Bundle(extras) : null;
		that.category = category;
		that.priority = priority;
		that.channelId = channelId;
		that.text = text;
		that.title = title;
		if (actions != null) {
			that.actions = new Action[actions.length];
			for (int i = 0; i < actions.length; i++)
				if (actions[i] != null)
					that.actions[i] = actions[i].clone();
		}
		that.contentIntent = contentIntent;
		that.iconPath = iconPath;
		that.ongoing = ongoing;
		that.style = style;
		that.number = number;
		that.color = color;
		that.visibility = visibility;
		that.fullScreenIntent = fullScreenIntent;
		that.publicVersion = publicVersion != null ? publicVersion.clone() : null;
		that.mGroupKey = mGroupKey;
		that.mSortKey = mSortKey;
		that.mGroupAlertBehavior = mGroupAlertBehavior;
		that.mBadgeIcon = mBadgeIcon;
		that.mTimeout = mTimeout;
		that.mShortcutId = mShortcutId;
		that.mSettingsText = mSettingsText;
		that.mLargeIcon = mLargeIcon;
		that.mBubbleMetadata = mBubbleMetadata;
		that.mAllowSystemGeneratedContextualActions = mAllowSystemGeneratedContextualActions;
		that.mForegroundServiceBehavior = mForegroundServiceBehavior;
		that.mLocusId = mLocusId;
		that.mSmallIcon = mSmallIcon;
		return that;
	}

	/* AOSP Notification(Parcel) / readFromParcelImpl and writeToParcel: the public fields and those the
	 * getters answer. Not parcelled: the RemoteViews (contentView, bigContentView, headsUpContentView) and
	 * the Style object, which AOSP does not parcel either (Builder.recoverBuilder rebuilds it from extras). */
	public Notification(Parcel parcel) {
		when = parcel.readLong();
		icon = parcel.readInt();
		mSmallIcon = parcel.readTypedObject(Icon.CREATOR);
		number = parcel.readInt();
		contentIntent = parcel.readTypedObject(PendingIntent.CREATOR);
		deleteIntent = parcel.readTypedObject(PendingIntent.CREATOR);
		fullScreenIntent = parcel.readTypedObject(PendingIntent.CREATOR);
		tickerText = parcel.readCharSequence();
		iconLevel = parcel.readInt();
		mLargeIcon = parcel.readTypedObject(Icon.CREATOR);
		largeIcon = parcel.readTypedObject(Bitmap.CREATOR);
		defaults = parcel.readInt();
		flags = parcel.readInt();
		sound = parcel.readTypedObject(Uri.CREATOR);
		audioStreamType = parcel.readInt();
		audioAttributes = parcel.readTypedObject(AudioAttributes.CREATOR);
		vibrate = parcel.createLongArray();
		ledARGB = parcel.readInt();
		ledOnMS = parcel.readInt();
		ledOffMS = parcel.readInt();
		priority = parcel.readInt();
		category = parcel.readString8();
		mGroupKey = parcel.readString8();
		mSortKey = parcel.readString8();
		extras = parcel.readBundle();
		actions = parcel.createTypedArray(Action.CREATOR);
		publicVersion = parcel.readTypedObject(CREATOR);
		visibility = parcel.readInt();
		color = parcel.readInt();
		channelId = parcel.readString8();
		mTimeout = parcel.readLong();
		mShortcutId = parcel.readString8();
		String locusId = parcel.readString8();
		mLocusId = locusId != null ? new LocusId(locusId) : null;
		mBadgeIcon = parcel.readInt();
		mSettingsText = parcel.readCharSequence();
		mGroupAlertBehavior = parcel.readInt();
		mBubbleMetadata = parcel.readTypedObject(BubbleMetadata.CREATOR);
		mAllowSystemGeneratedContextualActions = parcel.readInt() != 0;
		mForegroundServiceBehavior = parcel.readInt();
		/* ATL's own fields for the desktop notification */
		title = parcel.readString8();
		text = parcel.readString8();
		iconPath = parcel.readString8();
		ongoing = parcel.readInt() != 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeLong(when);
		parcel.writeInt(icon);
		parcel.writeTypedObject(mSmallIcon, 0);
		parcel.writeInt(number);
		parcel.writeTypedObject(contentIntent, 0);
		parcel.writeTypedObject(deleteIntent, 0);
		parcel.writeTypedObject(fullScreenIntent, 0);
		parcel.writeCharSequence(tickerText);
		parcel.writeInt(iconLevel);
		parcel.writeTypedObject(mLargeIcon, 0);
		parcel.writeTypedObject(largeIcon, 0);
		parcel.writeInt(defaults);
		parcel.writeInt(this.flags);
		parcel.writeTypedObject(sound, 0);
		parcel.writeInt(audioStreamType);
		parcel.writeTypedObject(audioAttributes, 0);
		parcel.writeLongArray(vibrate);
		parcel.writeInt(ledARGB);
		parcel.writeInt(ledOnMS);
		parcel.writeInt(ledOffMS);
		parcel.writeInt(priority);
		parcel.writeString8(category);
		parcel.writeString8(mGroupKey);
		parcel.writeString8(mSortKey);
		parcel.writeBundle(extras);
		parcel.writeTypedArray(actions, 0);
		parcel.writeTypedObject(publicVersion, 0);
		parcel.writeInt(visibility);
		parcel.writeInt(color);
		parcel.writeString8(channelId);
		parcel.writeLong(mTimeout);
		parcel.writeString8(mShortcutId);
		parcel.writeString8(mLocusId != null ? mLocusId.getId() : null);
		parcel.writeInt(mBadgeIcon);
		parcel.writeCharSequence(mSettingsText);
		parcel.writeInt(mGroupAlertBehavior);
		parcel.writeTypedObject(mBubbleMetadata, 0);
		parcel.writeInt(mAllowSystemGeneratedContextualActions ? 1 : 0);
		parcel.writeInt(mForegroundServiceBehavior);
		parcel.writeString8(title);
		parcel.writeString8(text);
		parcel.writeString8(iconPath);
		parcel.writeInt(ongoing ? 1 : 0);
	}

	public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
		@Override
		public Notification createFromParcel(Parcel parcel) {
			return new Notification(parcel);
		}

		@Override
		public Notification[] newArray(int size) {
			return new Notification[size];
		}
	};

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
	/* AOSP: null without actions */
	public Action[] actions;
	public PendingIntent contentIntent;
	String iconPath;
	boolean ongoing;
	Style style;

	public String toString() {
		return "Notification [" + title + ", " + text + ", " + Arrays.toString(actions) + "]";
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

	public static final String EXTRA_TITLE = "android.title";
	public static final String EXTRA_TITLE_BIG = EXTRA_TITLE + ".big";
	public static final String EXTRA_TEXT = "android.text";
	public static final String EXTRA_SUMMARY_TEXT = "android.summaryText";
	public static final String EXTRA_BIG_TEXT = "android.bigText";
	public static final String EXTRA_LARGE_ICON_BIG = "android.largeIcon.big";
	public static final String EXTRA_PICTURE = "android.picture";
	public static final String EXTRA_PICTURE_ICON = "android.pictureIcon";
	public static final String EXTRA_PICTURE_CONTENT_DESCRIPTION = "android.pictureContentDescription";
	public static final String EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED = "android.showBigPictureWhenCollapsed";
	public static final String EXTRA_TEXT_LINES = "android.textLines";
	public static final String EXTRA_TEMPLATE = "android.template";
	public static final String EXTRA_MEDIA_SESSION = "android.mediaSession";
	public static final String EXTRA_MEDIA_REMOTE_DEVICE = "android.mediaRemoteDevice";
	public static final String EXTRA_MEDIA_REMOTE_ICON = "android.mediaRemoteIcon";
	public static final String EXTRA_MEDIA_REMOTE_INTENT = "android.mediaRemoteIntent";
	public static final String EXTRA_COMPACT_ACTIONS = "android.compactActions";
	public static final String CATEGORY_TRANSPORT = "transport";
	public static final String EXTRA_PEOPLE_LIST = "android.people.list";
	public static final String EXTRA_SELF_DISPLAY_NAME = "android.selfDisplayName";
	public static final String EXTRA_MESSAGING_PERSON = "android.messagingUser";
	public static final String EXTRA_CONVERSATION_TITLE = "android.conversationTitle";
	public static final String EXTRA_MESSAGES = "android.messages";
	public static final String EXTRA_HISTORIC_MESSAGES = "android.messages.historic";
	public static final String EXTRA_IS_GROUP_CONVERSATION = "android.isGroupConversation";
	public static final String EXTRA_CALL_TYPE = "android.callType";
	public static final String EXTRA_CALL_IS_VIDEO = "android.callIsVideo";
	public static final String EXTRA_CALL_PERSON = "android.callPerson";
	public static final String EXTRA_VERIFICATION_ICON = "android.verificationIcon";
	public static final String EXTRA_VERIFICATION_TEXT = "android.verificationText";
	public static final String EXTRA_ANSWER_INTENT = "android.answerIntent";
	public static final String EXTRA_DECLINE_INTENT = "android.declineIntent";
	public static final String EXTRA_HANG_UP_INTENT = "android.hangUpIntent";
	public static final String EXTRA_ANSWER_COLOR = "android.answerColor";
	public static final String EXTRA_DECLINE_COLOR = "android.declineColor";
	public static final String EXTRA_INFO_TEXT = "android.infoText";
	public static final String EXTRA_SUB_TEXT = "android.subText";

	public static final String EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText";
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
	private LocusId mLocusId;

	public LocusId getLocusId() {
		return mLocusId;
	}

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
		private final ArrayList<Action> mActions = new ArrayList<>();
		private final ArrayList<Person> mPersonList = new ArrayList<>();

		public Builder(Context context) {
			mContext = context;
			notification = new Notification();
			notification.when = System.currentTimeMillis();
			notification.priority = PRIORITY_DEFAULT;
			notification.visibility = VISIBILITY_PRIVATE;
		}

		/* AOSP Notification.Builder.setChannelId() */
		public Builder setChannelId(String channelId) {
			notification.channelId = channelId;
			return this;
		}

		public Builder(Context context, String channelId) {
			this(context);
			notification.channelId = channelId;
		}

		/* AOSP Builder.recoverBuilder() -> Builder(Context, Notification toAdopt): the builder takes the notification
		 * over with its actions, its people and its extras (build() writes them back). The Style is taken from the
		 * notification object; restoring it from EXTRA_TEMPLATE (Style.restoreFromExtras) is not provided, so a
		 * notification that came through a Parcel has no Style afterwards. */
		public static Builder recoverBuilder(Context context, Notification n) {
			Builder b = new Builder(context);
			b.notification = n;
			if (n.actions != null) {
				for (Action action : n.actions)
					b.mActions.add(action);
			}
			if (n.extras != null) {
				b.mExtras = new Bundle(n.extras);
				ArrayList<Person> people = n.extras.getParcelableArrayList(EXTRA_PEOPLE_LIST);
				if (people != null && !people.isEmpty())
					b.mPersonList.addAll(people);
			}
			if (n.style != null)
				n.style.setBuilder(b);
			return b;
		}

		/* AOSP Builder.createContentView(): the app's custom content view when it set one (useExistingRemoteView()).
		 * The standard templates of the system (applyStandardTemplate) and the decoration around a custom view
		 * (minimallyDecoratedContentView) are layouts of the platform that ATL does not have, so there is no
		 * view built from the builder's texts: null then. */
		@Deprecated
		public RemoteViews createContentView() {
			return notification.contentView;
		}

		/* AOSP Builder.createBigContentView(): the custom big content view when set, otherwise the expanded
		 * template of the system (not available, see createContentView()): null */
		@Deprecated
		public RemoteViews createBigContentView() {
			return notification.bigContentView;
		}

		/* AOSP Builder.createHeadsUpContentView(): the custom heads-up view when set; without one and without
		 * actions AOSP answers null as well, with actions it builds the heads-up template (not available): null */
		@Deprecated
		public RemoteViews createHeadsUpContentView() {
			return notification.headsUpContentView;
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
			mExtras.putCharSequence(EXTRA_TITLE, title);
			return this;
		}

		public Builder setContentText(CharSequence text) {
			notification.text = text != null ? text.toString() : null;
			mExtras.putCharSequence(EXTRA_TEXT, text);
			return this;
		}

		public Builder setContentInfo(CharSequence info) {
			mExtras.putCharSequence(EXTRA_INFO_TEXT, info);
			return this;
		}

		public Builder setContentIntent(PendingIntent intent) {
			notification.contentIntent = intent;
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

		/** Short critical text of the notification (AOSP 16); kept in the extras, not rendered. */
		public Builder setShortCriticalText(String shortCriticalText) {
			mExtras.putString(EXTRA_SHORT_CRITICAL_TEXT, shortCriticalText);
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

		/** @deprecated AOSP Notification.Builder.addAction(int, CharSequence, PendingIntent) */
		@Deprecated
		public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
			return addAction(new Action.Builder(icon, title, intent).build());
		}

		/* AOSP Notification.Builder.setActions() (hidden): replaces the actions, null entries are skipped */
		public Builder setActions(Action... actions) {
			mActions.clear();
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] != null)
					mActions.add(actions[i]);
			}
			return this;
		}

		public Builder addAction(Action action) {
			if (action != null) {
				mActions.add(action);
			}
			return this;
		}

		/* AOSP: the style is bound to this builder, it adds its extras in build() */
		public Builder setStyle(Style style) {
			if (notification.style != style) {
				notification.style = style;
				if (style != null) {
					style.setBuilder(this);
				}
			}
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

		/* AOSP: deprecated, the stream type is kept as given */
		public Builder setSound(Uri sound, int streamType) {
			notification.sound = sound;
			notification.audioStreamType = streamType;
			return this;
		}

		/* AOSP: deprecated, the same as build() */
		@Deprecated
		public Notification getNotification() {
			return build();
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

		/* AOSP: the persons go to the extras as EXTRA_PEOPLE_LIST */
		public Builder addPerson(Person person) {
			mPersonList.add(person);
			return this;
		}

		public Builder setLocusId(LocusId locusId) {
			notification.mLocusId = locusId;
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
			if (!mPersonList.isEmpty()) {
				extras.putParcelableArrayList(EXTRA_PEOPLE_LIST, new ArrayList<>(mPersonList));
			}
			if (notification.style != null) {
				notification.style.addExtras(extras);
			}
			notification.extras = extras;
			if (notification.mLargeIcon == null && notification.largeIcon != null) { // AOSP Builder.build()
				notification.mLargeIcon = Icon.createWithBitmap(notification.largeIcon);
			}
			if (notification.style instanceof MediaStyle && notification.category == null) { // AOSP MediaStyle.buildStyled()
				notification.category = CATEGORY_TRANSPORT;
			}
			if (notification.style instanceof CallStyle && mContext != null) { // AOSP CallStyle.buildStyled(); the host tests build without a context
				ArrayList<Action> actions = ((CallStyle)notification.style).getActionsListWithSystemActions();
				mActions.clear();
				mActions.addAll(actions);
			}
			notification.actions = mActions.isEmpty() ? null : mActions.toArray(new Action[0]);
			if ((notification.defaults & DEFAULT_LIGHTS) != 0) {
				notification.flags |= FLAG_SHOW_LIGHTS;
			}
			return notification;
		}
	}

	/**
	 * Structure to encapsulate a named action that can be shown as part of this notification (AOSP Notification.Action).
	 * The desktop notification shows the title and the PendingIntent of the button; the rest is kept as the app gave it.
	 */
	public static class Action implements Parcelable {
		/** {@link #extras} key: Keys to a {@link Parcelable} {@link ArrayList} of {@link RemoteInput}s. */
		private static final String EXTRA_DATA_ONLY_INPUTS = "android.extra.DATA_ONLY_INPUTS";

		public static final int SEMANTIC_ACTION_NONE = 0;
		public static final int SEMANTIC_ACTION_REPLY = 1;
		public static final int SEMANTIC_ACTION_MARK_AS_READ = 2;
		public static final int SEMANTIC_ACTION_MARK_AS_UNREAD = 3;
		public static final int SEMANTIC_ACTION_DELETE = 4;
		public static final int SEMANTIC_ACTION_ARCHIVE = 5;
		public static final int SEMANTIC_ACTION_MUTE = 6;
		public static final int SEMANTIC_ACTION_UNMUTE = 7;
		public static final int SEMANTIC_ACTION_THUMBS_UP = 8;
		public static final int SEMANTIC_ACTION_THUMBS_DOWN = 9;
		public static final int SEMANTIC_ACTION_CALL = 10;
		/** @hide */
		public static final int SEMANTIC_ACTION_MARK_CONVERSATION_AS_PRIORITY = 11;
		/** @hide */
		public static final int SEMANTIC_ACTION_CONVERSATION_IS_PHISHING = 12;

		private final Bundle mExtras;
		private Icon mIcon;
		private final RemoteInput[] mRemoteInputs;
		private boolean mAllowGeneratedReplies = true;
		private final @SemanticAction int mSemanticAction;
		private final boolean mIsContextual;
		private final boolean mAuthenticationRequired;

		/** Small icon representing the action. @deprecated Use {@link Action#getIcon()} instead. */
		@Deprecated
		public int icon;

		/** Title of the action. */
		public CharSequence title;

		/** Intent to send when the user invokes this action. May be null, in which case the action may be rendered in a disabled presentation. */
		public PendingIntent actionIntent;

		private Action(Parcel in) {
			if (in.readInt() != 0) {
				mIcon = Icon.CREATOR.createFromParcel(in);
				if (mIcon.getType() == Icon.TYPE_RESOURCE)
					icon = mIcon.getResId();
			}
			title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
			if (in.readInt() == 1)
				actionIntent = PendingIntent.CREATOR.createFromParcel(in);
			mExtras = Bundle.setDefusable(in.readBundle(), true);
			mRemoteInputs = in.createTypedArray(RemoteInput.CREATOR);
			mAllowGeneratedReplies = in.readInt() == 1;
			mSemanticAction = in.readInt();
			mIsContextual = in.readInt() == 1;
			mAuthenticationRequired = in.readInt() == 1;
		}

		/** @deprecated Use {@link android.app.Notification.Action.Builder}. */
		@Deprecated
		public Action(int icon, CharSequence title, PendingIntent intent) {
			this((Icon)null, title, intent, new Bundle(), null, true, SEMANTIC_ACTION_NONE, false, false);
			this.icon = icon; // getIcon() makes the Icon when it is asked for
		}

		private Action(Icon icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs,
		               boolean allowGeneratedReplies, @SemanticAction int semanticAction, boolean isContextual, boolean requireAuth) {
			this.mIcon = icon;
			if (icon != null && icon.getType() == Icon.TYPE_RESOURCE)
				this.icon = icon.getResId();
			this.title = title;
			this.actionIntent = intent;
			this.mExtras = extras != null ? extras : new Bundle();
			this.mRemoteInputs = remoteInputs;
			this.mAllowGeneratedReplies = allowGeneratedReplies;
			this.mSemanticAction = semanticAction;
			this.mIsContextual = isContextual;
			this.mAuthenticationRequired = requireAuth;
		}

		/** Return an icon representing the action. */
		public Icon getIcon() {
			if (mIcon == null && icon != 0) {
				// the icon of a resource id (the deprecated constructors): made when asked for
				mIcon = resourceIcon(icon);
			}
			return mIcon;
		}

		/** Get additional metadata carried around with this Action. */
		public Bundle getExtras() {
			return mExtras;
		}

		/** Return whether the platform should automatically generate possible replies for this {@link Action} */
		public boolean getAllowGeneratedReplies() {
			return mAllowGeneratedReplies;
		}

		/** Get the list of inputs to be collected from the user when this action is sent. May return null if no remote inputs were added. Only returns inputs which accept free-form text input. */
		public RemoteInput[] getRemoteInputs() {
			return mRemoteInputs;
		}

		/** Returns the {@code SemanticAction} associated with this {@link Action}. */
		public @SemanticAction int getSemanticAction() {
			return mSemanticAction;
		}

		/** Returns whether this is a contextual Action, i.e. whether the action is dependent on the notification message body. */
		public boolean isContextual() {
			return mIsContextual;
		}

		/** Get the list of inputs to be collected from the user that ONLY accept data when this action is sent. */
		public RemoteInput[] getDataOnlyRemoteInputs() {
			android.os.Parcelable[] inputs = mExtras != null ? mExtras.getParcelableArray(EXTRA_DATA_ONLY_INPUTS) : null;
			if (inputs == null)
				return null;
			RemoteInput[] result = new RemoteInput[inputs.length];
			for (int i = 0; i < inputs.length; i++)
				result[i] = (RemoteInput)inputs[i];
			return result;
		}

		/** Returns whether the OS should only send this action's {@link PendingIntent} on an unlocked device. */
		public boolean isAuthenticationRequired() {
			return mAuthenticationRequired;
		}

		/** Builder class for {@link Action} objects. */
		public static final class Builder {
			private final Icon mIcon;
			private final CharSequence mTitle;
			private final PendingIntent mIntent;
			private boolean mAllowGeneratedReplies = true;
			private final Bundle mExtras;
			private ArrayList<RemoteInput> mRemoteInputs;
			private int mStyleHint;
			private int mEmphasisHint;
			private @SemanticAction int mSemanticAction;
			private boolean mIsContextual;
			private boolean mAuthenticationRequired;
			private int mIconRes;

			/** @deprecated Use {@link #Builder(Icon, CharSequence, PendingIntent)} instead. */
			@Deprecated
			public Builder(int icon, CharSequence title, PendingIntent intent) {
				this((Icon)null, title, intent);
				mIconRes = icon;
			}

			public Builder(Icon icon, CharSequence title, PendingIntent intent) {
				this(icon, title, intent, new Bundle(), null, true, SEMANTIC_ACTION_NONE, false);
			}

			/** Construct a new builder, copying the content of an existing Action. */
			public Builder(Action action) {
				this(action.getIcon(), action.title, action.actionIntent, new Bundle(action.mExtras), action.getRemoteInputs(),
				     action.getAllowGeneratedReplies(), action.getSemanticAction(), action.isContextual());
				mAuthenticationRequired = action.isAuthenticationRequired();
			}

			private Builder(Icon icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs,
			                boolean allowGeneratedReplies, @SemanticAction int semanticAction, boolean isContextual) {
				mIcon = icon;
				mTitle = title;
				mIntent = intent;
				mExtras = extras;
				if (remoteInputs != null) {
					mRemoteInputs = new ArrayList<RemoteInput>(remoteInputs.length);
					java.util.Collections.addAll(mRemoteInputs, remoteInputs);
				}
				mAllowGeneratedReplies = allowGeneratedReplies;
				mSemanticAction = semanticAction;
				mIsContextual = isContextual;
			}

			/** Merge additional metadata into this builder. */
			public Builder addExtras(Bundle extras) {
				if (extras != null)
					mExtras.putAll(extras);
				return this;
			}

			/** Get the metadata Bundle used by this Builder. */
			public Bundle getExtras() {
				return mExtras;
			}

			/** Add an input to be collected from the user when this action is sent. */
			public Builder addRemoteInput(RemoteInput remoteInput) {
				if (mRemoteInputs == null)
					mRemoteInputs = new ArrayList<RemoteInput>();
				mRemoteInputs.add(remoteInput);
				return this;
			}

			/** Set whether the platform should automatically generate possible replies to add to {@link RemoteInput#getChoices()}. */
			public Builder setAllowGeneratedReplies(boolean allowGeneratedReplies) {
				mAllowGeneratedReplies = allowGeneratedReplies;
				return this;
			}

			/** Sets the {@code SemanticAction} for this {@link Action}. */
			public Builder setSemanticAction(@SemanticAction int semanticAction) {
				mSemanticAction = semanticAction;
				return this;
			}

			/** Style hint of the action (AOSP 16); stored only, the desktop notification does not render it. */
			public Builder setStyleHint(int styleHint) {
				mStyleHint = styleHint;
				return this;
			}

			/** Emphasis hint of the action (AOSP 16); stored only, the desktop notification does not render it. */
			public Builder setEmphasisHint(int emphasisHint) {
				mEmphasisHint = emphasisHint;
				return this;
			}

			/** Sets whether this {@link Action} is a contextual action. */
			public Builder setContextual(boolean isContextual) {
				mIsContextual = isContextual;
				return this;
			}

			/** Apply an extender to this action builder. */
			public Builder extend(Extender extender) {
				extender.extend(this);
				return this;
			}

			/** Sets whether the OS should only send this action's {@link PendingIntent} on an unlocked device. */
			public Builder setAuthenticationRequired(boolean authenticationRequired) {
				mAuthenticationRequired = authenticationRequired;
				return this;
			}

			/** Combine all of the options that have been set and return a new {@link Action} object. */
			public Action build() {
				/* AOSP puts the inputs that take data only (RemoteInput.isDataOnly(): allowed data types, no free-form text,
				 * no choices) into the extras; ATL's RemoteInput has no allowed data types, so every input takes text */
				RemoteInput[] textInputs = mRemoteInputs == null || mRemoteInputs.isEmpty() ? null : mRemoteInputs.toArray(new RemoteInput[0]);
				Action action = new Action(mIcon, mTitle, mIntent, mExtras, textInputs, mAllowGeneratedReplies, mSemanticAction, mIsContextual, mAuthenticationRequired);
				if (mIcon == null)
					action.icon = mIconRes;
				return action;
			}
		}

		/* the icon of a resource of the app (AOSP: Icon.createWithResource("", id), the package of the app) */
		private static Icon resourceIcon(int resId) {
			android.atl.ATLLoadedApp loaded = android.atl.ATLLoadedApp.getPrimaryApplication();
			android.app.Application app = loaded != null ? loaded.getApplication() : null;
			return app != null ? Icon.createWithResource(app, resId) : null;
		}

		@Override
		public Action clone() {
			return new Action(getIcon(), title, actionIntent, mExtras == null ? new Bundle() : new Bundle(mExtras), getRemoteInputs(),
			                  getAllowGeneratedReplies(), getSemanticAction(), isContextual(), isAuthenticationRequired());
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			final Icon ic = getIcon();
			if (ic != null) {
				out.writeInt(1);
				ic.writeToParcel(out, 0);
			} else {
				out.writeInt(0);
			}
			TextUtils.writeToParcel(title, out, flags);
			if (actionIntent != null) {
				out.writeInt(1);
				actionIntent.writeToParcel(out, flags);
			} else {
				out.writeInt(0);
			}
			out.writeBundle(mExtras);
			out.writeTypedArray(mRemoteInputs, flags);
			out.writeInt(mAllowGeneratedReplies ? 1 : 0);
			out.writeInt(mSemanticAction);
			out.writeInt(mIsContextual ? 1 : 0);
			out.writeInt(mAuthenticationRequired ? 1 : 0);
		}

		public static final Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>() {
			@Override
			public Action createFromParcel(Parcel in) {
				return new Action(in);
			}

			@Override
			public Action[] newArray(int size) {
				return new Action[size];
			}
		};

		/** Extender interface for use with {@link Builder#extend}. */
		public interface Extender {
			/** Apply this extender to a notification action builder. */
			public Builder extend(Builder builder);
		}

		@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
		@android.annotation.IntDef(prefix = {"SEMANTIC_ACTION_"}, value = {SEMANTIC_ACTION_NONE, SEMANTIC_ACTION_REPLY, SEMANTIC_ACTION_MARK_AS_READ, SEMANTIC_ACTION_MARK_AS_UNREAD, SEMANTIC_ACTION_DELETE, SEMANTIC_ACTION_ARCHIVE, SEMANTIC_ACTION_MUTE, SEMANTIC_ACTION_UNMUTE, SEMANTIC_ACTION_THUMBS_UP, SEMANTIC_ACTION_THUMBS_DOWN, SEMANTIC_ACTION_CALL, SEMANTIC_ACTION_MARK_CONVERSATION_AS_PRIORITY, SEMANTIC_ACTION_CONVERSATION_IS_PHISHING})
		public @interface SemanticAction {}

		public String toString() {
			return "Action [" + icon + ", " + title + ", " + actionIntent + "]";
		}
	}

	/* AOSP Notification.Style: what the styles put into the extras of the notification; the desktop notification
	 * has no expanded form, it shows the text of the style (desktopText()) instead of the content text */
	public static abstract class Style {
		private CharSequence mBigContentTitle;
		CharSequence mSummaryText = null;
		boolean mSummaryTextPresent = false;
		protected Builder mBuilder;

		protected void internalSetBigContentTitle(CharSequence title) {
			mBigContentTitle = title;
		}

		protected void internalSetSummaryText(CharSequence cs) {
			mSummaryText = cs;
			mSummaryTextPresent = true;
		}

		public void setBuilder(Builder builder) {
			if (mBuilder != builder) {
				mBuilder = builder;
				if (mBuilder != null) {
					mBuilder.setStyle(this);
				}
			}
		}

		protected void checkBuilder() {
			if (mBuilder == null) {
				throw new IllegalArgumentException("Style requires a valid Builder object");
			}
		}

		public Notification build() {
			checkBuilder();
			return mBuilder.build();
		}

		public void addExtras(Bundle extras) {
			if (mSummaryTextPresent) {
				extras.putCharSequence(EXTRA_SUMMARY_TEXT, mSummaryText);
			}
			if (mBigContentTitle != null) {
				extras.putCharSequence(EXTRA_TITLE_BIG, mBigContentTitle);
			}
			extras.putString(EXTRA_TEMPLATE, this.getClass().getName());
		}

		/* ATL: the body of the desktop notification, null for the content text */
		CharSequence desktopText() {
			return null;
		}
	}

	/* AOSP MediaStyle: the settings go into the extras; ATL's MediaSession.Token is not
	 * Parcelable, so EXTRA_MEDIA_SESSION stays out */
	public static class MediaStyle extends Style {
		private int[] mActionsToShowInCompact = null;
		private MediaSession.Token mToken;
		private CharSequence mDeviceName;
		private int mDeviceIcon;
		private PendingIntent mDeviceIntent;

		public MediaStyle() {}

		@Deprecated
		public MediaStyle(Builder builder) {
			setBuilder(builder);
		}

		public MediaStyle setShowActionsInCompactView(int... actions) {
			mActionsToShowInCompact = actions;
			return this;
		}

		public MediaStyle setMediaSession(MediaSession.Token token) {
			mToken = token;
			return this;
		}

		public MediaStyle setRemotePlaybackInfo(CharSequence deviceName, int iconResource, PendingIntent chipIntent) {
			mDeviceName = deviceName;
			mDeviceIcon = iconResource;
			mDeviceIntent = chipIntent;
			return this;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			if (mActionsToShowInCompact != null) {
				extras.putIntArray(EXTRA_COMPACT_ACTIONS, mActionsToShowInCompact);
			}
			if (mDeviceName != null) {
				extras.putCharSequence(EXTRA_MEDIA_REMOTE_DEVICE, mDeviceName);
			}
			if (mDeviceIcon > 0) {
				extras.putInt(EXTRA_MEDIA_REMOTE_ICON, mDeviceIcon);
			}
			if (mDeviceIntent != null) {
				extras.putParcelable(EXTRA_MEDIA_REMOTE_INTENT, mDeviceIntent);
			}
		}
	}

	/* AOSP DecoratedMediaCustomViewStyle: the custom views inside the media frame */
	public static class DecoratedMediaCustomViewStyle extends MediaStyle {
		public DecoratedMediaCustomViewStyle() {}
	}

	public static class BigTextStyle extends Style {
		private CharSequence mBigText;

		public BigTextStyle() {}

		public BigTextStyle(Builder builder) {
			setBuilder(builder);
		}

		public BigTextStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public BigTextStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}

		public BigTextStyle bigText(CharSequence cs) {
			mBigText = cs;
			return this;
		}

		public CharSequence getBigText() {
			return mBigText;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			extras.putCharSequence(EXTRA_BIG_TEXT, mBigText);
		}

		@Override
		CharSequence desktopText() {
			return mBigText;
		}
	}

	/* AOSP Notification: extras keys of ProgressStyle and MetricStyle (API 36) */
	public static final String EXTRA_PROGRESS_SEGMENTS = "android.progressSegments";
	public static final String EXTRA_PROGRESS_POINTS = "android.progressPoints";
	public static final String EXTRA_STYLED_BY_PROGRESS = "android.styledByProgress";
	public static final String EXTRA_PROGRESS_TRACKER_ICON = "android.progressTrackerIcon";
	public static final String EXTRA_PROGRESS_START_ICON = "android.progressStartIcon";
	public static final String EXTRA_PROGRESS_END_ICON = "android.progressEndIcon";
	static final String EXTRA_METRICS = "android.metrics";

	/* AOSP Notification.COLOR_DEFAULT (Color.TRANSPARENT) */
	public static final int COLOR_DEFAULT = 0;

	/* AOSP Notification.safeCharSequenceToString / safeString: cut to MAX_CHARSEQUENCE_LENGTH */
	public static String safeCharSequenceToString(CharSequence cs) {
		if (cs == null) return null;
		String s = cs.toString();
		if (s.length() > 5 * 1024) {
			s = s.substring(0, 5 * 1024);
		}
		return s;
	}

	/* AOSP: com.android.internal.util.Preconditions.checkArgument(boolean, String, Object...) */
	private static void checkArgument(boolean expression, String messageTemplate, Object... args) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(messageTemplate, args));
		}
	}

	/* AOSP Notification.hasPromotableCharacteristics, the branch without the ui_rich_ongoing flag
	 * (ATL has no aconfig flags): ATL does not keep a promoted-ongoing request, so it is not part of the check */
	public boolean hasPromotableCharacteristics() {
		boolean ongoing = (flags & FLAG_ONGOING_EVENT) != 0;
		if (!ongoing || isGroupSummary() || containsCustomViews() || !hasTitle()) {
			return false;
		}
		// Only "Ongoing CallStyle" notifications are promotable without EXTRA_COLORIZED
		if (style instanceof CallStyle && extras != null
				&& extras.getInt(EXTRA_CALL_TYPE, CallStyle.CALL_TYPE_UNKNOWN) == CallStyle.CALL_TYPE_ONGOING) {
			return true;
		}
		return extras != null && extras.getBoolean(EXTRA_COLORIZED) && hasPromotableStyle();
	}

	/* AOSP Notification.hasPromotableStyle */
	boolean hasPromotableStyle() {
		return style == null
				|| style instanceof BigTextStyle
				|| style instanceof CallStyle
				|| style instanceof MetricStyle
				|| style instanceof ProgressStyle;
	}

	/* AOSP Notification.containsCustomViews (ATL keeps no public version) */
	boolean containsCustomViews() {
		return contentView != null || bigContentView != null || headsUpContentView != null;
	}

	/* AOSP Notification.hasTitle */
	boolean hasTitle() {
		if (extras == null) {
			return false;
		}
		// CallStyle notifications only use the other person's name as the title.
		if (style instanceof CallStyle) {
			Person person = extras.getParcelable(EXTRA_CALL_PERSON, Person.class);
			return person != null && !TextUtils.isEmpty(person.getName());
		}
		if (!TextUtils.isEmpty(extras.getCharSequence(EXTRA_TITLE))) {
			return true;
		}
		// BigTextStyle notifications first use EXTRA_TITLE_BIG
		if (style instanceof BigTextStyle) {
			return !TextUtils.isEmpty(extras.getCharSequence(EXTRA_TITLE_BIG));
		}
		return false;
	}

	/* AOSP Notification.MetricStyle: the metrics go into the extras, the desktop notification renders none of the views */
	public static final class MetricStyle extends Style {
		private static final int MAX_METRICS = 3;

		private final List<Metric> mMetrics = new ArrayList<>();

		public MetricStyle() {
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof MetricStyle)) return false;
			MetricStyle that = (MetricStyle) obj;
			if (this == that) return true;
			return java.util.Objects.equals(this.mMetrics, that.mMetrics);
		}

		@Override
		public int hashCode() {
			return mMetrics.hashCode();
		}

		@Override
		public String toString() {
			return "MetricStyle{" + mMetrics + "}";
		}

		public MetricStyle addMetric(Metric metric) {
			mMetrics.add(java.util.Objects.requireNonNull(metric));
			return this;
		}

		public MetricStyle setMetrics(List<Metric> metrics) {
			mMetrics.clear();
			for (Metric metric : metrics) {
				mMetrics.add(java.util.Objects.requireNonNull(metric));
			}
			return this;
		}

		public List<Metric> getMetrics() {
			return java.util.Collections.unmodifiableList(mMetrics);
		}

		public boolean areNotificationsVisiblyDifferent(Style other) {
			if (other == null || getClass() != other.getClass()) {
				return true;
			}
			// Check whether the VISIBLE metrics (max 3) are the same.
			MetricStyle otherStyle = (MetricStyle) other;
			if (Math.min(this.getMetrics().size(), MAX_METRICS)
					!= Math.min(otherStyle.getMetrics().size(), MAX_METRICS)) {
				return true;
			}
			for (int i = 0; i < MAX_METRICS && i < this.getMetrics().size()
					&& i < otherStyle.getMetrics().size(); i++) {
				if (!java.util.Objects.equals(this.getMetrics().get(i), otherStyle.getMetrics().get(i))) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			final ArrayList<Bundle> bundles = new ArrayList<>();
			for (Metric metric : mMetrics) {
				bundles.add(Metric.toBundle(metric));
			}
			extras.putParcelableArrayList(EXTRA_METRICS, bundles);
		}

		/* AOSP MetricStyle.validate */
		public void validate() {
			if (mMetrics.isEmpty()) {
				throw new IllegalArgumentException("A MetricStyle must have at least one Metric");
			}
		}
	}

	/* AOSP Notification.Metric */
	public static final class Metric {
		private static final String KEY_VALUE = "value";
		private static final String KEY_LABEL = "label";

		private final MetricValue mValue;
		private final String mLabel;

		public Metric(MetricValue value, CharSequence label) {
			mValue = java.util.Objects.requireNonNull(value);
			mLabel = safeCharSequenceToString(java.util.Objects.requireNonNull(label));
			checkArgument(!mLabel.trim().isEmpty(), "Metric label is required");
		}

		private static Bundle toBundle(Metric metric) {
			Bundle bundle = new Bundle();
			bundle.putBundle(KEY_VALUE, MetricValue.toBundle(metric.mValue));
			bundle.putString(KEY_LABEL, metric.mLabel);
			return bundle;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Metric)) return false;
			Metric that = (Metric) obj;
			if (this == that) return true;
			return java.util.Objects.equals(this.mValue, that.mValue)
					&& java.util.Objects.equals(this.mLabel, that.mLabel);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(mValue, mLabel);
		}

		@Override
		public String toString() {
			return "Metric{" + "mValue=" + mValue + ", mLabel=" + mLabel + "}";
		}

		public MetricValue getValue() {
			return mValue;
		}

		public CharSequence getLabel() {
			return mLabel;
		}

		/* AOSP Notification.Metric.MetricValue: toValueString (the text rendering) is left out */
		public abstract static class MetricValue {
			private static final String KEY_TYPE = "_type";
			private static final int TYPE_TIME_DIFFERENCE = 1;
			private static final int TYPE_FIXED_DATE = 2;
			private static final int TYPE_FIXED_TIME = 3;
			private static final int TYPE_FIXED_INT = 4;
			private static final int TYPE_FIXED_FLOAT = 5;
			private static final int TYPE_FIXED_STRING = 6;

			// Restrict inheritance to inner classes of Notification.
			private MetricValue() { }

			private static Bundle toBundle(MetricValue value) {
				Bundle bundle = new Bundle();
				if (value instanceof TimeDifference) {
					bundle.putInt(KEY_TYPE, TYPE_TIME_DIFFERENCE);
				} else if (value instanceof FixedDate) {
					bundle.putInt(KEY_TYPE, TYPE_FIXED_DATE);
				} else if (value instanceof FixedTime) {
					bundle.putInt(KEY_TYPE, TYPE_FIXED_TIME);
				} else if (value instanceof FixedInt) {
					bundle.putInt(KEY_TYPE, TYPE_FIXED_INT);
				} else if (value instanceof FixedFloat) {
					bundle.putInt(KEY_TYPE, TYPE_FIXED_FLOAT);
				} else if (value instanceof FixedText) {
					bundle.putInt(KEY_TYPE, TYPE_FIXED_STRING);
				} else {
					throw new AssertionError("Impossible MetricValue subclass: " + value);
				}
				value.toBundle(bundle);
				return bundle;
			}

			protected abstract void toBundle(Bundle bundle);
		}

		/* AOSP Notification.Metric.TimeDifference */
		public static final class TimeDifference extends MetricValue {
			public static final int FORMAT_ADAPTIVE = 1;
			public static final int FORMAT_CHRONOMETER = 2;

			private static final String KEY_ZERO_TIME = "zeroTime";
			private static final String KEY_ZERO_ELAPSED_REALTIME = "zeroElapsedRealtime";
			private static final String KEY_PAUSED_DURATION = "pausedDuration";
			private static final String KEY_COUNT_DOWN = "countDown";
			private static final String KEY_FORMAT = "format";

			// One of these three will be present.
			private final java.time.Instant mZeroTime;
			private final Long mZeroElapsedRealtime;
			private final java.time.Duration mPausedDuration;
			private final boolean mCountDown;
			private final int mFormat;

			public static TimeDifference forTimer(java.time.Instant endTime, int format) {
				return new TimeDifference(java.util.Objects.requireNonNull(endTime),
						/* zeroElapsedRealtime= */ null, /* pausedDuration= */ null,
						/* countDown= */ true, format);
			}

			public static TimeDifference forTimer(long endTime, int format) {
				return new TimeDifference(/* zeroTime= */ null, endTime,
						/* pausedDuration= */ null, /* countDown= */ true, format);
			}

			public static TimeDifference forStopwatch(java.time.Instant startTime, int format) {
				return new TimeDifference(java.util.Objects.requireNonNull(startTime),
						/* zeroElapsedRealtime= */ null, /* pausedDuration= */ null,
						/* countDown= */ false, format);
			}

			public static TimeDifference forStopwatch(long startTime, int format) {
				return new TimeDifference(/* zeroTime= */ null, startTime,
						/* pausedDuration= */ null, /* countDown= */ false, format);
			}

			public static TimeDifference forPausedTimer(java.time.Duration remainingTime, int format) {
				return new TimeDifference(/* zeroTime= */ null, /* zeroElapsedRealtime= */ null,
						java.util.Objects.requireNonNull(remainingTime), /* countDown= */ true, format);
			}

			public static TimeDifference forPausedStopwatch(java.time.Duration elapsedTime, int format) {
				return new TimeDifference(/* zeroTime= */ null, /* zeroElapsedRealtime= */ null,
						java.util.Objects.requireNonNull(elapsedTime), /* countDown= */ false, format);
			}

			private TimeDifference(java.time.Instant zeroTime, Long zeroElapsedRealtime,
					java.time.Duration pausedDuration, boolean countDown, int format) {
				checkArgument((zeroTime != null ? 1 : 0) + (zeroElapsedRealtime != null ? 1 : 0)
								+ (pausedDuration != null ? 1 : 0) == 1,
						"Exactly one of zeroTime, zeroElapsedRealtime, or pausedDuration must be "
								+ "present; received %s,%s,%s",
						zeroTime, zeroElapsedRealtime, pausedDuration);
				checkArgument(format >= FORMAT_ADAPTIVE && format <= FORMAT_CHRONOMETER,
						"Invalid format: %s", format);
				mZeroTime = zeroTime;
				mZeroElapsedRealtime = zeroElapsedRealtime;
				mPausedDuration = pausedDuration;
				mCountDown = countDown;
				mFormat = format;
			}

			@Override
			protected void toBundle(Bundle bundle) {
				if (mZeroTime != null) {
					bundle.putLong(KEY_ZERO_TIME, mZeroTime.toEpochMilli());
				} else if (mZeroElapsedRealtime != null) {
					bundle.putLong(KEY_ZERO_ELAPSED_REALTIME, mZeroElapsedRealtime);
				} else if (mPausedDuration != null) {
					bundle.putLong(KEY_PAUSED_DURATION, mPausedDuration.toMillis());
				}
				bundle.putBoolean(KEY_COUNT_DOWN, mCountDown);
				bundle.putInt(KEY_FORMAT, mFormat);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof TimeDifference)) return false;
				TimeDifference that = (TimeDifference) obj;
				if (this == that) return true;
				return java.util.Objects.equals(this.mZeroTime, that.mZeroTime)
						&& java.util.Objects.equals(this.mZeroElapsedRealtime, that.mZeroElapsedRealtime)
						&& java.util.Objects.equals(this.mPausedDuration, that.mPausedDuration)
						&& this.mCountDown == that.mCountDown
						&& this.mFormat == that.mFormat;
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mZeroTime, mZeroElapsedRealtime, mPausedDuration, mCountDown,
						mFormat);
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder("TimeDifference{");
				if (mZeroTime != null) {
					sb.append("mZeroTime=").append(mZeroTime);
				} else if (mZeroElapsedRealtime != null) {
					sb.append("mZeroElapsedRealtime=").append(mZeroElapsedRealtime);
				} else if (mPausedDuration != null) {
					sb.append("mPausedDuration=").append(mPausedDuration);
				}
				sb.append(", mCountDown=").append(mCountDown)
						.append(", mFormat=").append(mFormat)
						.append("}");
				return sb.toString();
			}

			public java.time.Instant getZeroTime() {
				return mZeroTime;
			}

			public Long getZeroElapsedRealtime() {
				return mZeroElapsedRealtime;
			}

			public java.time.Duration getPausedDuration() {
				return mPausedDuration;
			}

			public boolean isStopwatch() {
				return !mCountDown;
			}

			public boolean isTimer() {
				return mCountDown;
			}

			public int getFormat() {
				return mFormat;
			}
		}

		/* AOSP Notification.Metric.FixedDate */
		public static final class FixedDate extends MetricValue {
			public static final int FORMAT_AUTOMATIC = 0;
			public static final int FORMAT_LONG_DATE = 1;
			public static final int FORMAT_SHORT_DATE = 2;

			private static final String KEY_VALUE = "value";
			private static final String KEY_FORMAT = "format";

			private final java.time.LocalDate mValue;
			private final int mFormat;

			public FixedDate(java.time.LocalDate value) {
				this(value, FORMAT_AUTOMATIC);
			}

			public FixedDate(java.time.LocalDate value, int format) {
				mValue = java.util.Objects.requireNonNull(value);
				checkArgument(format >= FORMAT_AUTOMATIC && format <= FORMAT_SHORT_DATE,
						"Invalid format: %s", format);
				mFormat = format;
			}

			@Override
			protected void toBundle(Bundle bundle) {
				bundle.putLong(KEY_VALUE, mValue.toEpochDay());
				bundle.putInt(KEY_FORMAT, mFormat);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof FixedDate)) return false;
				FixedDate that = (FixedDate) obj;
				if (this == that) return true;
				return java.util.Objects.equals(this.mValue, that.mValue)
						&& this.mFormat == that.mFormat;
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mValue, mFormat);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "{"
						+ "mValue=" + mValue
						+ ", mFormat=" + mFormat
						+ "}";
			}

			public java.time.LocalDate getValue() {
				return mValue;
			}

			public int getFormat() {
				return mFormat;
			}
		}

		/* AOSP Notification.Metric.FixedTime */
		public static final class FixedTime extends MetricValue {
			private static final String KEY_VALUE = "value";

			private final java.time.LocalTime mValue;

			public FixedTime(java.time.LocalTime value) {
				mValue = java.util.Objects.requireNonNull(value).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
			}

			@Override
			protected void toBundle(Bundle bundle) {
				bundle.putLong(KEY_VALUE, mValue.toSecondOfDay());
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof FixedTime)) return false;
				FixedTime that = (FixedTime) obj;
				if (this == that) return true;
				return java.util.Objects.equals(this.mValue, that.mValue);
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mValue);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "{" + "mValue=" + mValue + "}";
			}

			public java.time.LocalTime getValue() {
				return mValue;
			}
		}

		/* AOSP Notification.Metric.FixedInt */
		public static final class FixedInt extends MetricValue {
			private static final String KEY_VALUE = "value";
			private static final String KEY_UNIT = "unit";

			private final int mValue;
			private final String mUnit;

			public FixedInt(int value) {
				this(value, /* unit= */ null);
			}

			public FixedInt(int value, CharSequence unit) {
				mValue = value;
				mUnit = safeCharSequenceToString(unit);
			}

			@Override
			protected void toBundle(Bundle bundle) {
				bundle.putInt(KEY_VALUE, mValue);
				bundle.putString(KEY_UNIT, mUnit);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof FixedInt)) return false;
				FixedInt that = (FixedInt) obj;
				if (this == that) return true;
				return this.mValue == that.mValue
						&& java.util.Objects.equals(this.mUnit, that.mUnit);
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mValue, mUnit);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "{"
						+ "mValue=" + mValue
						+ ", mUnit=" + mUnit
						+ "}";
			}

			public int getValue() {
				return mValue;
			}

			public CharSequence getUnit() {
				return mUnit;
			}
		}

		/* AOSP Notification.Metric.FixedFloat */
		public static final class FixedFloat extends MetricValue {
			public static final int DEFAULT_MIN_FRACTION_DIGITS = 0;
			public static final int DEFAULT_MAX_FRACTION_DIGITS = 2;

			private static final int LOWER_BOUND_FRACTION_DIGITS = 0;
			private static final int UPPER_BOUND_FRACTION_DIGITS = 6;

			private static final String KEY_VALUE = "value";
			private static final String KEY_UNIT = "unit";
			private static final String KEY_MIN_FRACTION_DIGITS = "minDigits";
			private static final String KEY_MAX_FRACTION_DIGITS = "maxDigits";

			private final float mValue;
			private final String mUnit;
			private final int mMinFractionDigits;
			private final int mMaxFractionDigits;

			public FixedFloat(float value) {
				this(value, /* unit= */ null);
			}

			public FixedFloat(float value, CharSequence unit) {
				this(value, unit, DEFAULT_MIN_FRACTION_DIGITS, DEFAULT_MAX_FRACTION_DIGITS);
			}

			public FixedFloat(float value, CharSequence unit, int minFractionDigits,
					int maxFractionDigits) {
				mValue = value;
				mUnit = safeCharSequenceToString(unit);
				checkArgument(minFractionDigits >= LOWER_BOUND_FRACTION_DIGITS
								&& minFractionDigits <= UPPER_BOUND_FRACTION_DIGITS,
						"Invalid minFractionDigits: %s", minFractionDigits);
				checkArgument(maxFractionDigits >= LOWER_BOUND_FRACTION_DIGITS
								&& maxFractionDigits <= UPPER_BOUND_FRACTION_DIGITS,
						"Invalid maxFractionDigits: %s", maxFractionDigits);
				checkArgument(minFractionDigits <= maxFractionDigits,
						"Invalid minFractionDigits/maxFractionDigits: %s/%s",
						minFractionDigits, maxFractionDigits);
				mMinFractionDigits = minFractionDigits;
				mMaxFractionDigits = maxFractionDigits;
			}

			@Override
			protected void toBundle(Bundle bundle) {
				bundle.putFloat(KEY_VALUE, mValue);
				bundle.putString(KEY_UNIT, mUnit);
				bundle.putInt(KEY_MIN_FRACTION_DIGITS, mMinFractionDigits);
				bundle.putInt(KEY_MAX_FRACTION_DIGITS, mMaxFractionDigits);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof FixedFloat)) return false;
				FixedFloat that = (FixedFloat) obj;
				if (this == that) return true;
				return this.mValue == that.mValue
						&& java.util.Objects.equals(this.mUnit, that.mUnit)
						&& this.mMinFractionDigits == that.mMinFractionDigits
						&& this.mMaxFractionDigits == that.mMaxFractionDigits;
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mValue, mUnit, mMinFractionDigits, mMaxFractionDigits);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "{"
						+ "mValue=" + mValue
						+ ", mUnit=" + mUnit
						+ ", mMinFractionDigits=" + mMinFractionDigits
						+ ", mMaxFractionDigits=" + mMaxFractionDigits
						+ "}";
			}

			public float getValue() {
				return mValue;
			}

			public CharSequence getUnit() {
				return mUnit;
			}

			public int getMinFractionDigits() {
				return mMinFractionDigits;
			}

			public int getMaxFractionDigits() {
				return mMaxFractionDigits;
			}
		}

		/* AOSP Notification.Metric.FixedString (the API 36.1 name is FixedText) */
		public static final class FixedText extends MetricValue {
			private static final String KEY_VALUE = "value";
			private static final String KEY_UNIT = "unit";

			private final String mValue;
			private final String mUnit;

			public FixedText(CharSequence value) {
				this(value, null);
			}

			public FixedText(CharSequence value, CharSequence unit) {
				mValue = safeCharSequenceToString(java.util.Objects.requireNonNull(value));
				mUnit = safeCharSequenceToString(unit);
			}

			@Override
			protected void toBundle(Bundle bundle) {
				bundle.putString(KEY_VALUE, mValue);
				bundle.putString(KEY_UNIT, mUnit);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof FixedText)) return false;
				FixedText that = (FixedText) obj;
				if (this == that) return true;
				return java.util.Objects.equals(this.mValue, that.mValue)
						&& java.util.Objects.equals(this.mUnit, that.mUnit);
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mValue, mUnit);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "{"
						+ "mValue=" + mValue
						+ ", mUnit=" + mUnit
						+ "}";
			}

			public CharSequence getValue() {
				return mValue;
			}

			public CharSequence getUnit() {
				return mUnit;
			}
		}
	}

	/* AOSP Notification.ProgressStyle: the data of the progress bar goes into the extras, nothing is drawn;
	 * purgeResources/reduceImageSizes (ashmem, resource sizes) are left out */
	public static class ProgressStyle extends Style {
		private static final String KEY_ELEMENT_ID = "id";
		private static final String KEY_ELEMENT_COLOR = "colorInt";
		private static final String KEY_SEGMENT_LENGTH = "length";
		private static final String KEY_POINT_POSITION = "position";

		private static final int MAX_PROGRESS_SEGMENT_LIMIT = 10;
		private static final int MAX_PROGRESS_POINT_LIMIT = 4;
		private static final int DEFAULT_PROGRESS_MAX = 100;

		private List<Segment> mProgressSegments = new ArrayList<>();
		private List<Point> mProgressPoints = new ArrayList<>();
		private int mProgress = 0;
		private boolean mIndeterminate;
		private boolean mIsStyledByProgress = true;
		private Icon mTrackerIcon;
		private Icon mStartIcon;
		private Icon mEndIcon;

		public ProgressStyle() {
		}

		public boolean areNotificationsVisiblyDifferent(Style other) {
			if (other == null || getClass() != other.getClass()) {
				return true;
			}
			final ProgressStyle progressStyle = (ProgressStyle) other;
			if (!java.util.Objects.equals(mIndeterminate, progressStyle.mIndeterminate)) {
				return true;
			}
			boolean nonIndeterminateCheckResult = false;
			if (!mIndeterminate) {
				nonIndeterminateCheckResult = !java.util.Objects.equals(mProgress, progressStyle.mProgress)
						|| !java.util.Objects.equals(mIsStyledByProgress, progressStyle.mIsStyledByProgress)
						|| !java.util.Objects.equals(mProgressSegments, progressStyle.mProgressSegments)
						|| !java.util.Objects.equals(mProgressPoints, progressStyle.mProgressPoints)
						|| !java.util.Objects.equals(mTrackerIcon, progressStyle.mTrackerIcon);
			}
			return !java.util.Objects.equals(mStartIcon, progressStyle.mStartIcon)
					|| !java.util.Objects.equals(mEndIcon, progressStyle.mEndIcon)
					|| nonIndeterminateCheckResult;
		}

		public List<Segment> getProgressSegments() {
			return mProgressSegments;
		}

		public ProgressStyle setProgressSegments(List<Segment> progressSegments) {
			if (mProgressSegments == null) {
				mProgressSegments = new ArrayList<>();
			}
			mProgressSegments.clear();
			for (Segment segment : progressSegments) {
				addProgressSegment(segment);
			}
			return this;
		}

		public ProgressStyle addProgressSegment(Segment segment) {
			if (mProgressSegments == null) {
				mProgressSegments = new ArrayList<>();
			}
			if (segment.getLength() > 0) {
				mProgressSegments.add(segment);
				if (mProgressSegments.size() > MAX_PROGRESS_SEGMENT_LIMIT) {
					android.util.Log.w("Notification", "Progress segments limit ("
							+ MAX_PROGRESS_SEGMENT_LIMIT + ") is reached. "
							+ "All segments will be merged into one segment.");
				}
			} else {
				android.util.Log.w("Notification", "Dropped the segment. The length is not a positive integer.");
			}
			return this;
		}

		public List<Point> getProgressPoints() {
			return mProgressPoints;
		}

		public ProgressStyle setProgressPoints(List<Point> points) {
			if (mProgressPoints == null) {
				mProgressPoints = new ArrayList<>();
			}
			mProgressPoints.clear();
			for (Point point : points) {
				addProgressPoint(point);
			}
			return this;
		}

		public ProgressStyle addProgressPoint(Point point) {
			if (mProgressPoints == null) {
				mProgressPoints = new ArrayList<>();
			}
			if (point.getPosition() > 0) {
				mProgressPoints.add(point);
				if (mProgressPoints.size() > MAX_PROGRESS_POINT_LIMIT) {
					android.util.Log.w("Notification", "Progress points limit is reached. First"
							+ MAX_PROGRESS_POINT_LIMIT + " points will be rendered.");
				}
			} else {
				android.util.Log.w("Notification", "Dropped the point. The position is a negative or zero integer.");
			}
			return this;
		}

		public int getProgress() {
			return mProgress;
		}

		public ProgressStyle setProgress(int progress) {
			mProgress = progress;
			return this;
		}

		public int getProgressMax() {
			final List<Segment> progressSegment = mProgressSegments;
			if (progressSegment == null || progressSegment.isEmpty()) {
				return DEFAULT_PROGRESS_MAX;
			} else {
				int progressMax = 0;
				int validSegmentCount = 0;
				for (int i = 0; i < progressSegment.size(); i++) {
					int segmentLength = progressSegment.get(i).getLength();
					if (segmentLength > 0) {
						try {
							progressMax = Math.addExact(progressMax, segmentLength);
							validSegmentCount++;
						} catch (ArithmeticException e) {
							android.util.Log.e("Notification",
									"Notification.ProgressStyle segment total overflowed.", e);
							return DEFAULT_PROGRESS_MAX;
						}
					}
				}
				if (validSegmentCount == 0) {
					return DEFAULT_PROGRESS_MAX;
				}
				return progressMax;
			}
		}

		public boolean isProgressIndeterminate() {
			return mIndeterminate;
		}

		public ProgressStyle setProgressIndeterminate(boolean indeterminate) {
			mIndeterminate = indeterminate;
			return this;
		}

		public boolean isStyledByProgress() {
			return mIsStyledByProgress;
		}

		public ProgressStyle setStyledByProgress(boolean enabled) {
			mIsStyledByProgress = enabled;
			return this;
		}

		public Icon getProgressTrackerIcon() {
			return mTrackerIcon;
		}

		public ProgressStyle setProgressTrackerIcon(Icon trackerIcon) {
			mTrackerIcon = trackerIcon;
			return this;
		}

		public Icon getProgressStartIcon() {
			return mStartIcon;
		}

		public ProgressStyle setProgressStartIcon(Icon startIcon) {
			mStartIcon = startIcon;
			return this;
		}

		public Icon getProgressEndIcon() {
			return mEndIcon;
		}

		public ProgressStyle setProgressEndIcon(Icon endIcon) {
			mEndIcon = endIcon;
			return this;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			extras.putParcelableArrayList(EXTRA_PROGRESS_SEGMENTS,
					getProgressSegmentsAsBundleList(mProgressSegments));
			extras.putParcelableArrayList(EXTRA_PROGRESS_POINTS,
					getProgressPointsAsBundleList(mProgressPoints));
			extras.putInt(EXTRA_PROGRESS, mProgress);
			extras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, mIndeterminate);
			extras.putInt(EXTRA_PROGRESS_MAX, getProgressMax());
			extras.putBoolean(EXTRA_STYLED_BY_PROGRESS, mIsStyledByProgress);
			if (mTrackerIcon != null) {
				extras.putParcelable(EXTRA_PROGRESS_TRACKER_ICON, mTrackerIcon);
			} else {
				extras.remove(EXTRA_PROGRESS_TRACKER_ICON);
			}
			if (mStartIcon != null) {
				extras.putParcelable(EXTRA_PROGRESS_START_ICON, mStartIcon);
			} else {
				extras.remove(EXTRA_PROGRESS_START_ICON);
			}
			if (mEndIcon != null) {
				extras.putParcelable(EXTRA_PROGRESS_END_ICON, mEndIcon);
			} else {
				extras.remove(EXTRA_PROGRESS_END_ICON);
			}
		}

		public static ArrayList<Bundle> getProgressSegmentsAsBundleList(List<Segment> progressSegments) {
			final ArrayList<Bundle> segments = new ArrayList<>();
			if (progressSegments != null && !progressSegments.isEmpty()) {
				for (int i = 0; i < progressSegments.size(); i++) {
					final Segment segment = progressSegments.get(i);
					if (segment.getLength() <= 0) {
						continue;
					}
					final Bundle bundle = new Bundle();
					bundle.putInt(KEY_SEGMENT_LENGTH, segment.getLength());
					bundle.putInt(KEY_ELEMENT_ID, segment.getId());
					bundle.putInt(KEY_ELEMENT_COLOR, segment.getColor());
					segments.add(bundle);
				}
			}
			return segments;
		}

		public static ArrayList<Bundle> getProgressPointsAsBundleList(List<Point> progressPoints) {
			final ArrayList<Bundle> points = new ArrayList<>();
			if (progressPoints != null && !progressPoints.isEmpty()) {
				for (int i = 0; i < progressPoints.size(); i++) {
					final Point point = progressPoints.get(i);
					if (point.getPosition() < 0) {
						continue;
					}
					final Bundle bundle = new Bundle();
					bundle.putInt(KEY_POINT_POSITION, point.getPosition());
					bundle.putInt(KEY_ELEMENT_ID, point.getId());
					bundle.putInt(KEY_ELEMENT_COLOR, point.getColor());
					points.add(bundle);
				}
			}
			return points;
		}

		/* AOSP Notification.ProgressStyle.Segment */
		public static final class Segment {
			private int mLength;
			private int mId = 0;
			private int mColor = Notification.COLOR_DEFAULT;

			public Segment(int length) {
				mLength = length;
			}

			public int getLength() {
				return mLength;
			}

			public int getId() {
				return mId;
			}

			public Segment setId(int id) {
				mId = id;
				return this;
			}

			public int getColor() {
				return mColor;
			}

			public Segment setColor(int color) {
				mColor = color;
				return this;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final Segment segment = (Segment) o;
				return mLength == segment.mLength && mId == segment.mId
						&& mColor == segment.mColor;
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mLength, mId, mColor);
			}
		}

		/* AOSP Notification.ProgressStyle.Point */
		public static final class Point {
			private int mPosition;
			private int mId = 0;
			private int mColor = Notification.COLOR_DEFAULT;

			public Point(int position) {
				mPosition = position;
			}

			public int getPosition() {
				return mPosition;
			}

			public int getId() {
				return mId;
			}

			public Point setId(int id) {
				mId = id;
				return this;
			}

			public int getColor() {
				return mColor;
			}

			public Point setColor(int color) {
				mColor = color;
				return this;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final Point point = (Point) o;
				return mPosition == point.mPosition && mId == point.mId
						&& mColor == point.mColor;
			}

			@Override
			public int hashCode() {
				return java.util.Objects.hash(mPosition, mId, mColor);
			}
		}
	}

	public static class InboxStyle extends Style {
		private ArrayList<CharSequence> mTexts = new ArrayList<>(5);

		public InboxStyle() {}

		public InboxStyle(Builder builder) {
			setBuilder(builder);
		}

		public InboxStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public InboxStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}

		public InboxStyle addLine(CharSequence cs) {
			mTexts.add(cs);
			return this;
		}

		public ArrayList<CharSequence> getLines() {
			return mTexts;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			extras.putCharSequenceArray(EXTRA_TEXT_LINES, mTexts.toArray(new CharSequence[0]));
		}

		/* the lines one below the other, as the expanded notification lists them */
		@Override
		CharSequence desktopText() {
			return mTexts.isEmpty() ? null : TextUtils.join("\n", mTexts);
		}
	}

	public static class BigPictureStyle extends Style {
		private Icon mPictureIcon;
		private Icon mBigLargeIcon;
		private boolean mBigLargeIconSet = false;
		private CharSequence mPictureContentDescription;
		private boolean mShowBigPictureWhenCollapsed;

		public BigPictureStyle() {}

		public BigPictureStyle(Builder builder) {
			setBuilder(builder);
		}

		public BigPictureStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public BigPictureStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}

		public BigPictureStyle setContentDescription(CharSequence contentDescription) {
			mPictureContentDescription = contentDescription;
			return this;
		}

		public Icon getBigPicture() {
			return mPictureIcon;
		}

		public BigPictureStyle bigPicture(Bitmap b) {
			mPictureIcon = b == null ? null : Icon.createWithBitmap(b);
			return this;
		}

		public BigPictureStyle bigPicture(Icon icon) {
			mPictureIcon = icon;
			return this;
		}

		public BigPictureStyle showBigPictureWhenCollapsed(boolean show) {
			mShowBigPictureWhenCollapsed = show;
			return this;
		}

		public BigPictureStyle bigLargeIcon(Bitmap b) {
			return bigLargeIcon(b != null ? Icon.createWithBitmap(b) : null);
		}

		public BigPictureStyle bigLargeIcon(Icon icon) {
			mBigLargeIconSet = true;
			mBigLargeIcon = icon;
			return this;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			if (mBigLargeIconSet) {
				extras.putParcelable(EXTRA_LARGE_ICON_BIG, mBigLargeIcon);
			}
			if (mPictureContentDescription != null) {
				extras.putCharSequence(EXTRA_PICTURE_CONTENT_DESCRIPTION, mPictureContentDescription);
			}
			extras.putBoolean(EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED, mShowBigPictureWhenCollapsed);
			if (mPictureIcon == null) {
				extras.remove(EXTRA_PICTURE_ICON);
				extras.remove(EXTRA_PICTURE);
			} else if (mPictureIcon.getType() == Icon.TYPE_BITMAP) {
				extras.putParcelable(EXTRA_PICTURE, mPictureIcon.getBitmap());
				extras.remove(EXTRA_PICTURE_ICON);
			} else {
				extras.putParcelable(EXTRA_PICTURE_ICON, mPictureIcon);
				extras.remove(EXTRA_PICTURE);
			}
		}
	}

	/* AOSP MessagingStyle; the desktop notification shows the latest message (in a group with its sender) */
	public static class MessagingStyle extends Style {
		public static final int MAXIMUM_RETAINED_MESSAGES = 25;

		private Person mUser;
		private CharSequence mConversationTitle;
		private List<Message> mMessages = new ArrayList<>();
		private List<Message> mHistoricMessages = new ArrayList<>();
		private boolean mIsGroupConversation;

		public MessagingStyle(CharSequence userDisplayName) {
			this(new Person.Builder().setName(userDisplayName).build());
		}

		public MessagingStyle(Person user) {
			mUser = user;
		}

		public CharSequence getUserDisplayName() {
			return mUser.getName();
		}

		public Person getUser() {
			return mUser;
		}

		public MessagingStyle setConversationTitle(CharSequence conversationTitle) {
			mConversationTitle = conversationTitle;
			return this;
		}

		public CharSequence getConversationTitle() {
			return mConversationTitle;
		}

		public MessagingStyle addMessage(CharSequence text, long timestamp, CharSequence sender) {
			return addMessage(text, timestamp, sender == null ? null : new Person.Builder().setName(sender).build());
		}

		public MessagingStyle addMessage(CharSequence text, long timestamp, Person sender) {
			return addMessage(new Message(text, timestamp, sender));
		}

		public MessagingStyle addMessage(Message message) {
			mMessages.add(message);
			if (mMessages.size() > MAXIMUM_RETAINED_MESSAGES) {
				mMessages.remove(0);
			}
			return this;
		}

		public MessagingStyle addHistoricMessage(Message message) {
			mHistoricMessages.add(message);
			if (mHistoricMessages.size() > MAXIMUM_RETAINED_MESSAGES) {
				mHistoricMessages.remove(0);
			}
			return this;
		}

		public List<Message> getMessages() {
			return mMessages;
		}

		public List<Message> getHistoricMessages() {
			return mHistoricMessages;
		}

		public MessagingStyle setGroupConversation(boolean isGroupConversation) {
			mIsGroupConversation = isGroupConversation;
			return this;
		}

		public boolean isGroupConversation() {
			return mIsGroupConversation;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			if (mUser != null) {
				extras.putCharSequence(EXTRA_SELF_DISPLAY_NAME, mUser.getName());
				extras.putParcelable(EXTRA_MESSAGING_PERSON, mUser);
			}
			if (mConversationTitle != null) {
				extras.putCharSequence(EXTRA_CONVERSATION_TITLE, mConversationTitle);
			}
			if (!mMessages.isEmpty()) {
				extras.putParcelableArray(EXTRA_MESSAGES, Message.getBundleArrayForMessages(mMessages));
			}
			if (!mHistoricMessages.isEmpty()) {
				extras.putParcelableArray(EXTRA_HISTORIC_MESSAGES, Message.getBundleArrayForMessages(mHistoricMessages));
			}
			extras.putBoolean(EXTRA_IS_GROUP_CONVERSATION, mIsGroupConversation);
		}

		@Override
		CharSequence desktopText() {
			if (mMessages.isEmpty())
				return null;
			Message m = mMessages.get(mMessages.size() - 1);
			Person sender = m.getSenderPerson();
			if (mIsGroupConversation && sender != null && sender.getName() != null)
				return sender.getName() + ": " + m.getText();
			return m.getText();
		}

		public static final class Message {
			static final String KEY_TEXT = "text";
			static final String KEY_TIMESTAMP = "time";
			static final String KEY_SENDER = "sender";
			static final String KEY_SENDER_PERSON = "sender_person";
			static final String KEY_DATA_MIME_TYPE = "type";
			static final String KEY_DATA_URI = "uri";
			static final String KEY_EXTRAS_BUNDLE = "extras";

			private final CharSequence mText;
			private final long mTimestamp;
			private final Person mSender;
			private Bundle mExtras = new Bundle();
			private String mDataMimeType;
			private Uri mDataUri;

			public Message(CharSequence text, long timestamp, CharSequence sender) {
				this(text, timestamp, sender == null ? null : new Person.Builder().setName(sender).build());
			}

			public Message(CharSequence text, long timestamp, Person sender) {
				mText = text;
				mTimestamp = timestamp;
				mSender = sender;
			}

			public Message setData(String dataMimeType, Uri dataUri) {
				mDataMimeType = dataMimeType;
				mDataUri = dataUri;
				return this;
			}

			public CharSequence getText() {
				return mText;
			}

			public long getTimestamp() {
				return mTimestamp;
			}

			public Bundle getExtras() {
				return mExtras;
			}

			public CharSequence getSender() {
				return mSender == null ? null : mSender.getName();
			}

			public Person getSenderPerson() {
				return mSender;
			}

			public String getDataMimeType() {
				return mDataMimeType;
			}

			public Uri getDataUri() {
				return mDataUri;
			}

			private Bundle toBundle() {
				Bundle bundle = new Bundle();
				if (mText != null) {
					bundle.putCharSequence(KEY_TEXT, mText);
				}
				bundle.putLong(KEY_TIMESTAMP, mTimestamp);
				if (mSender != null) {
					bundle.putCharSequence(KEY_SENDER, mSender.getName());
					bundle.putParcelable(KEY_SENDER_PERSON, mSender);
				}
				if (mDataMimeType != null) {
					bundle.putString(KEY_DATA_MIME_TYPE, mDataMimeType);
				}
				if (mDataUri != null) {
					bundle.putParcelable(KEY_DATA_URI, mDataUri);
				}
				if (mExtras != null) {
					bundle.putBundle(KEY_EXTRAS_BUNDLE, mExtras);
				}
				return bundle;
			}

			static Bundle[] getBundleArrayForMessages(List<Message> messages) {
				Bundle[] bundles = new Bundle[messages.size()];
				for (int i = 0; i < bundles.length; i++) {
					bundles[i] = messages.get(i).toBundle();
				}
				return bundles;
			}
		}
	}

	/* AOSP DecoratedCustomViewStyle: the custom views of the builder inside the standard frame */
	public static class DecoratedCustomViewStyle extends Style {
		public DecoratedCustomViewStyle() {}
	}

	/* AOSP CallStyle, as kept in the extras; the desktop notification shows no answer/decline buttons of its own */
	public static class CallStyle extends Style {
		public static final int CALL_TYPE_UNKNOWN = 0;
		public static final int CALL_TYPE_INCOMING = 1;
		public static final int CALL_TYPE_ONGOING = 2;
		public static final int CALL_TYPE_SCREENING = 3;

		private int mCallType;
		private Person mPerson;
		private PendingIntent mAnswerIntent;
		private PendingIntent mDeclineIntent;
		private PendingIntent mHangUpIntent;
		private boolean mIsVideo;
		private Integer mAnswerButtonColor;
		private Integer mDeclineButtonColor;
		private Icon mVerificationIcon;
		private CharSequence mVerificationText;
		private static final String KEY_ACTION_PRIORITY = "key_action_priority";
		private static final int MAX_ACTION_BUTTONS = 3;

		public static CallStyle forIncomingCall(Person person, PendingIntent declineIntent, PendingIntent answerIntent) {
			return new CallStyle(CALL_TYPE_INCOMING, person, null,
			                     java.util.Objects.requireNonNull(declineIntent, "declineIntent is required"),
			                     java.util.Objects.requireNonNull(answerIntent, "answerIntent is required"));
		}

		public static CallStyle forOngoingCall(Person person, PendingIntent hangUpIntent) {
			return new CallStyle(CALL_TYPE_ONGOING, person, java.util.Objects.requireNonNull(hangUpIntent, "hangUpIntent is required"),
			                     null, null);
		}

		public static CallStyle forScreeningCall(Person person, PendingIntent hangUpIntent, PendingIntent answerIntent) {
			return new CallStyle(CALL_TYPE_SCREENING, person, java.util.Objects.requireNonNull(hangUpIntent, "hangUpIntent is required"),
			                     null, java.util.Objects.requireNonNull(answerIntent, "answerIntent is required"));
		}

		private CallStyle(int callType, Person person, PendingIntent hangUpIntent, PendingIntent declineIntent, PendingIntent answerIntent) {
			if (person == null || TextUtils.isEmpty(person.getName())) {
				throw new IllegalArgumentException("person must have a non-empty a name");
			}
			mCallType = callType;
			mPerson = person;
			mAnswerIntent = answerIntent;
			mDeclineIntent = declineIntent;
			mHangUpIntent = hangUpIntent;
		}

		public CallStyle setIsVideo(boolean isVideo) {
			mIsVideo = isVideo;
			return this;
		}

		public CallStyle setVerificationIcon(Icon verificationIcon) {
			mVerificationIcon = verificationIcon;
			return this;
		}

		public CallStyle setVerificationText(CharSequence verificationText) {
			mVerificationText = verificationText;
			return this;
		}

		public CallStyle setAnswerButtonColorHint(int color) {
			mAnswerButtonColor = color;
			return this;
		}

		public CallStyle setDeclineButtonColorHint(int color) {
			mDeclineButtonColor = color;
			return this;
		}

		@Override
		public void addExtras(Bundle extras) {
			super.addExtras(extras);
			extras.putInt(EXTRA_CALL_TYPE, mCallType);
			extras.putBoolean(EXTRA_CALL_IS_VIDEO, mIsVideo);
			extras.putParcelable(EXTRA_CALL_PERSON, mPerson);
			if (mVerificationIcon != null) {
				extras.putParcelable(EXTRA_VERIFICATION_ICON, mVerificationIcon);
			}
			if (mVerificationText != null) {
				extras.putCharSequence(EXTRA_VERIFICATION_TEXT, mVerificationText);
			}
			extras.putParcelable(EXTRA_ANSWER_INTENT, mAnswerIntent);
			extras.putParcelable(EXTRA_DECLINE_INTENT, mDeclineIntent);
			extras.putParcelable(EXTRA_HANG_UP_INTENT, mHangUpIntent);
			if (mAnswerButtonColor != null) {
				extras.putInt(EXTRA_ANSWER_COLOR, mAnswerButtonColor);
			}
			if (mDeclineButtonColor != null) {
				extras.putInt(EXTRA_DECLINE_COLOR, mDeclineButtonColor);
			}
		}

		/* AOSP: the system's buttons (decline or hang up first, answer last) become actions of the notification,
		 * so the desktop notification shows them like the app's; ATL draws neither their icons nor their colors */
		private Action makeAction(int title, PendingIntent intent) {
			Action action = new Action.Builder((Icon)null, mBuilder.mContext.getString(title), intent).build();
			action.getExtras().putBoolean(KEY_ACTION_PRIORITY, true);
			return action;
		}

		private Action makeNegativeAction() {
			if (mDeclineIntent == null)
				return makeAction(com.android.internal.R.string.call_notification_hang_up_action, mHangUpIntent);
			return makeAction(com.android.internal.R.string.call_notification_decline_action, mDeclineIntent);
		}

		private Action makeAnswerAction() {
			return mAnswerIntent == null ? null : makeAction(mIsVideo ? com.android.internal.R.string.call_notification_answer_video_action : com.android.internal.R.string.call_notification_answer_action, mAnswerIntent);
		}

		/* AOSP CallStyle.getActionsListWithSystemActions() */
		ArrayList<Action> getActionsListWithSystemActions() {
			final Action firstAction = makeNegativeAction();
			final Action lastAction = makeAnswerAction();
			int nonContextualActionSlotsRemaining = MAX_ACTION_BUTTONS;
			ArrayList<Action> resultActions = new ArrayList<>(MAX_ACTION_BUTTONS);
			resultActions.add(firstAction);
			--nonContextualActionSlotsRemaining;
			for (Action action : mBuilder.mActions) {
				if (action.isContextual()) {
					resultActions.add(action);
				} else if (action.getExtras().getBoolean(KEY_ACTION_PRIORITY)) {
					// drop the system actions of an earlier build()
				} else {
					resultActions.add(action);
					--nonContextualActionSlotsRemaining;
				}
				if (lastAction != null && nonContextualActionSlotsRemaining == 1) {
					resultActions.add(lastAction);
					--nonContextualActionSlotsRemaining;
				}
			}
			if (lastAction != null && nonContextualActionSlotsRemaining >= 1)
				resultActions.add(lastAction);
			return resultActions;
		}
	}

	/* AOSP Notification.BubbleMetadata (android-16): the values are kept and parcelled; ATL shows no bubbles */
	public static final class BubbleMetadata implements Parcelable {
		public static final int FLAG_AUTO_EXPAND_BUBBLE = 0x00000001;
		public static final int FLAG_SUPPRESS_NOTIFICATION = 0x00000002;
		public static final int FLAG_SUPPRESSABLE_BUBBLE = 0x00000004;
		public static final int FLAG_SUPPRESS_BUBBLE = 0x00000008;

		private PendingIntent mPendingIntent;
		private PendingIntent mDeleteIntent;
		private Icon mIcon;
		private int mDesiredHeight;
		private int mDesiredHeightResId;
		private int mFlags;
		private String mShortcutId;

		private BubbleMetadata(PendingIntent expandIntent, PendingIntent deleteIntent, Icon icon, int height, int heightResId, String shortcutId) {
			mPendingIntent = expandIntent;
			mIcon = icon;
			mDesiredHeight = height;
			mDesiredHeightResId = heightResId;
			mDeleteIntent = deleteIntent;
			mShortcutId = shortcutId;
		}

		private BubbleMetadata(Parcel in) {
			if (in.readInt() != 0)
				mPendingIntent = PendingIntent.CREATOR.createFromParcel(in);
			if (in.readInt() != 0)
				mIcon = Icon.CREATOR.createFromParcel(in);
			mDesiredHeight = in.readInt();
			mFlags = in.readInt();
			if (in.readInt() != 0)
				mDeleteIntent = PendingIntent.CREATOR.createFromParcel(in);
			mDesiredHeightResId = in.readInt();
			if (in.readInt() != 0)
				mShortcutId = in.readString8();
		}

		public String getShortcutId() { return mShortcutId; }
		public PendingIntent getIntent() { return mPendingIntent; }
		public PendingIntent getBubbleIntent() { return mPendingIntent; }
		public PendingIntent getDeleteIntent() { return mDeleteIntent; }
		public Icon getIcon() { return mIcon; }
		public Icon getBubbleIcon() { return mIcon; }
		public int getDesiredHeight() { return mDesiredHeight; }
		public int getDesiredHeightResId() { return mDesiredHeightResId; }
		public boolean getAutoExpandBubble() { return (mFlags & FLAG_AUTO_EXPAND_BUBBLE) != 0; }
		public boolean isNotificationSuppressed() { return (mFlags & FLAG_SUPPRESS_NOTIFICATION) != 0; }
		public boolean isBubbleSuppressable() { return (mFlags & FLAG_SUPPRESSABLE_BUBBLE) != 0; }
		public boolean isBubbleSuppressed() { return (mFlags & FLAG_SUPPRESS_BUBBLE) != 0; }
		public int getFlags() { return mFlags; }
		public void setFlags(int flags) { mFlags = flags; }

		public static final Parcelable.Creator<BubbleMetadata> CREATOR = new Parcelable.Creator<BubbleMetadata>() {
			@Override
			public BubbleMetadata createFromParcel(Parcel source) {
				return new BubbleMetadata(source);
			}

			@Override
			public BubbleMetadata[] newArray(int size) {
				return new BubbleMetadata[size];
			}
		};

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(mPendingIntent != null ? 1 : 0);
			if (mPendingIntent != null)
				mPendingIntent.writeToParcel(out, 0);
			out.writeInt(mIcon != null ? 1 : 0);
			if (mIcon != null)
				mIcon.writeToParcel(out, 0);
			out.writeInt(mDesiredHeight);
			out.writeInt(mFlags);
			out.writeInt(mDeleteIntent != null ? 1 : 0);
			if (mDeleteIntent != null)
				mDeleteIntent.writeToParcel(out, 0);
			out.writeInt(mDesiredHeightResId);
			out.writeInt(TextUtils.isEmpty(mShortcutId) ? 0 : 1);
			if (!TextUtils.isEmpty(mShortcutId))
				out.writeString8(mShortcutId);
		}

		public static final class Builder {
			private PendingIntent mPendingIntent;
			private Icon mIcon;
			private int mDesiredHeight;
			private int mDesiredHeightResId;
			private int mFlags;
			private PendingIntent mDeleteIntent;
			private String mShortcutId;

			@Deprecated
			public Builder() {}

			public Builder(String shortcutId) {
				if (TextUtils.isEmpty(shortcutId))
					throw new NullPointerException("Bubble requires a non-null shortcut id");
				mShortcutId = shortcutId;
			}

			public Builder(PendingIntent intent, Icon icon) {
				if (intent == null)
					throw new NullPointerException("Bubble requires non-null pending intent");
				if (icon == null)
					throw new NullPointerException("Bubbles require non-null icon");
				mPendingIntent = intent;
				mIcon = icon;
			}

			public Builder setIntent(PendingIntent intent) {
				if (mShortcutId != null)
					throw new IllegalStateException("Created as a shortcut bubble, cannot set a PendingIntent. Consider using BubbleMetadata.Builder(PendingIntent,Icon) instead.");
				if (intent == null)
					throw new NullPointerException("Bubble requires non-null pending intent");
				mPendingIntent = intent;
				return this;
			}

			public Builder setIcon(Icon icon) {
				if (mShortcutId != null)
					throw new IllegalStateException("Created as a shortcut bubble, cannot set an Icon. Consider using BubbleMetadata.Builder(PendingIntent,Icon) instead.");
				if (icon == null)
					throw new NullPointerException("Bubbles require non-null icon");
				mIcon = icon;
				return this;
			}

			public Builder setDesiredHeight(int height) {
				mDesiredHeight = Math.max(height, 0);
				mDesiredHeightResId = 0;
				return this;
			}

			public Builder setDesiredHeightResId(int heightResId) {
				mDesiredHeightResId = heightResId;
				mDesiredHeight = 0;
				return this;
			}

			public Builder setAutoExpandBubble(boolean shouldExpand) {
				setFlag(FLAG_AUTO_EXPAND_BUBBLE, shouldExpand);
				return this;
			}

			public Builder setSuppressNotification(boolean shouldSuppressNotif) {
				setFlag(FLAG_SUPPRESS_NOTIFICATION, shouldSuppressNotif);
				return this;
			}

			public Builder setSuppressableBubble(boolean suppressBubble) {
				setFlag(FLAG_SUPPRESSABLE_BUBBLE, suppressBubble);
				return this;
			}

			public Builder setDeleteIntent(PendingIntent deleteIntent) {
				mDeleteIntent = deleteIntent;
				return this;
			}

			public BubbleMetadata build() {
				if (mShortcutId == null && mPendingIntent == null)
					throw new NullPointerException("Must supply pending intent or shortcut to bubble");
				if (mShortcutId == null && mIcon == null)
					throw new NullPointerException("Must supply an icon or shortcut for the bubble");
				BubbleMetadata data = new BubbleMetadata(mPendingIntent, mDeleteIntent, mIcon, mDesiredHeight, mDesiredHeightResId, mShortcutId);
				data.setFlags(mFlags);
				return data;
			}

			public Builder setFlag(int mask, boolean value) {
				if (value)
					mFlags |= mask;
				else
					mFlags &= ~mask;
				return this;
			}
		}
	}
}
