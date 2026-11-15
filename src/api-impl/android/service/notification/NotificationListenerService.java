package android.service.notification;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.pm.ShortcutInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/* AOSP NotificationListenerService as far as it can be used without the NotificationManagerService, which
 * binds a listener only after the user granted it notification access in the settings (ATL has no such
 * setting: NotificationManager.isNotificationListenerAccessGranted() is false). Nobody binds the service,
 * so every call takes the path AOSP has for a listener that is not bound (isBound() false): nothing is
 * done and null/0 comes back */
public abstract class NotificationListenerService extends Service {
	public static final String SERVICE_INTERFACE = "android.service.notification.NotificationListenerService";
	public static final int REASON_CLICK = 1;
	public static final int REASON_CANCEL = 2;
	public static final int REASON_CANCEL_ALL = 3;
	public static final int REASON_ERROR = 4;
	public static final int REASON_PACKAGE_CHANGED = 5;
	public static final int REASON_USER_STOPPED = 6;
	public static final int REASON_PACKAGE_BANNED = 7;
	public static final int REASON_APP_CANCEL = 8;
	public static final int REASON_APP_CANCEL_ALL = 9;
	public static final int REASON_LISTENER_CANCEL = 10;
	public static final int REASON_LISTENER_CANCEL_ALL = 11;
	public static final int REASON_GROUP_SUMMARY_CANCELED = 12;
	public static final int REASON_GROUP_OPTIMIZATION = 13;
	public static final int REASON_PACKAGE_SUSPENDED = 14;
	public static final int REASON_PROFILE_TURNED_OFF = 15;
	public static final int REASON_UNAUTOBUNDLED = 16;
	public static final int REASON_CHANNEL_BANNED = 17;
	public static final int REASON_SNOOZED = 18;
	public static final int REASON_TIMEOUT = 19;
	public static final int REASON_CHANNEL_REMOVED = 20;
	public static final int REASON_CLEAR_DATA = 21;
	public static final int REASON_ASSISTANT_CANCEL = 22;
	public static final int REASON_LOCKDOWN = 23;
	public static final int REASON_BUNDLE_DISMISSED = 24;
	public static final int HINT_HOST_DISABLE_EFFECTS = 1;
	public static final int HINT_HOST_DISABLE_NOTIFICATION_EFFECTS = 1 << 1;
	public static final int HINT_HOST_DISABLE_CALL_EFFECTS = 1 << 2;
	public static final int INTERRUPTION_FILTER_ALL = 1;
	public static final int INTERRUPTION_FILTER_PRIORITY = 2;
	public static final int INTERRUPTION_FILTER_NONE = 3;
	public static final int INTERRUPTION_FILTER_ALARMS = 4;
	public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
	public static final int FLAG_FILTER_TYPE_CONVERSATIONS = 1;
	public static final int FLAG_FILTER_TYPE_ALERTING = 2;
	public static final int FLAG_FILTER_TYPE_SILENT = 4;
	public static final int FLAG_FILTER_TYPE_ONGOING = 8;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_ADDED = 1;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_UPDATED = 2;
	public static final int NOTIFICATION_CHANNEL_OR_GROUP_DELETED = 3;
	public static final String META_DATA_DISABLED_FILTER_TYPES = "android.service.notification.disabled_filter_types";
	public static final String META_DATA_DEFAULT_AUTOBIND = "android.service.notification.default_autobind_listenerservice";
	public static final String ACTION_SETTINGS_HOME = "android.service.notification.action.SETTINGS_HOME";
	@Deprecated
	public static final int SUPPRESSED_EFFECT_SCREEN_OFF = NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
	@Deprecated
	public static final int SUPPRESSED_EFFECT_SCREEN_ON = NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;
	public static final String META_DATA_DEFAULT_FILTER_TYPES = "android.service.notification.default_filter_types";

	/* AOSP NotificationListenerService.RankingMap: the rankings of the active notifications, none here unless
	 * built from Rankings */
	public static class RankingMap implements Parcelable {
		private ArrayList<String> mOrderedKeys = new ArrayList<>();
		private HashMap<String, Ranking> mRankings = new HashMap<>();

		public RankingMap(Ranking[] rankings) {
			for (int i = 0; i < rankings.length; i++) {
				final String key = rankings[i].getKey();
				mOrderedKeys.add(key);
				mRankings.put(key, rankings[i]);
			}
		}

		private RankingMap(Parcel in) {
			final int count = in.readInt();
			mOrderedKeys.ensureCapacity(count);
			for (int i = 0; i < count; i++) {
				final Ranking r = new Ranking(in);
				final String key = r.getKey();
				mOrderedKeys.add(key);
				mRankings.put(key, r);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RankingMap other = (RankingMap)o;
			return mOrderedKeys.equals(other.mOrderedKeys) && mRankings.equals(other.mRankings);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			final int count = mOrderedKeys.size();
			out.writeInt(count);
			for (int i = 0; i < count; i++) {
				mRankings.get(mOrderedKeys.get(i)).writeToParcel(out, flags);
			}
		}

		public static final Creator<RankingMap> CREATOR = new Creator<RankingMap>() {
			@Override
			public RankingMap createFromParcel(Parcel source) {
				return new RankingMap(source);
			}

			@Override
			public RankingMap[] newArray(int size) {
				return new RankingMap[size];
			}
		};

		public String[] getOrderedKeys() {
			return mOrderedKeys.toArray(new String[0]);
		}

		public boolean getRanking(String key, Ranking outRanking) {
			if (mRankings.containsKey(key)) {
				outRanking.populate(mRankings.get(key));
				return true;
			}
			return false;
		}

		public Ranking getRawRankingObject(String key) {
			return mRankings.get(key);
		}
	}

	/* AOSP NotificationListenerService.Ranking: the ranking of one notification, filled by populate() */
	public static class Ranking {
		public static final int VISIBILITY_NO_OVERRIDE = -1000;

		public static final int USER_SENTIMENT_NEGATIVE = -1;
		public static final int USER_SENTIMENT_NEUTRAL = 0;
		public static final int USER_SENTIMENT_POSITIVE = 1;

		public static final int RANKING_DEMOTED = -1;
		public static final int RANKING_UNCHANGED = 0;
		public static final int RANKING_PROMOTED = 1;

		private String mKey;
		private int mRank = -1;
		private boolean mIsAmbient;
		private boolean mMatchesInterruptionFilter;
		private int mVisibilityOverride;
		private int mSuppressedVisualEffects;
		private int mImportance;
		private CharSequence mImportanceExplanation;
		private float mRankingScore;
		private String mOverrideGroupKey;
		private NotificationChannel mChannel;
		private ArrayList<String> mOverridePeople;
		private ArrayList<SnoozeCriterion> mSnoozeCriteria;
		private boolean mShowBadge;
		private int mUserSentiment = USER_SENTIMENT_NEUTRAL;
		private boolean mHidden;
		private long mLastAudiblyAlertedMs;
		private boolean mNoisy;
		private ArrayList<Notification.Action> mSmartActions;
		private ArrayList<CharSequence> mSmartReplies;
		private boolean mCanBubble;
		private boolean mIsTextChanged;
		private boolean mIsConversation;
		private ShortcutInfo mShortcutInfo;
		private int mRankingAdjustment;
		private boolean mIsBubble;
		private int mProposedImportance;
		private boolean mSensitiveContent;
		private String mSummarization;

		private static final int PARCEL_VERSION = 2;

		public Ranking() {}

		/* AOSP Ranking.writeToParcel (a Ranking can be parceled but is not Parcelable); the char sequence list is
		 * the length (-1 for null) and the elements, as Parcel.writeCharSequenceList */
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(PARCEL_VERSION);
			out.writeString(mKey);
			out.writeInt(mRank);
			out.writeBoolean(mIsAmbient);
			out.writeBoolean(mMatchesInterruptionFilter);
			out.writeInt(mVisibilityOverride);
			out.writeInt(mSuppressedVisualEffects);
			out.writeInt(mImportance);
			out.writeCharSequence(mImportanceExplanation);
			out.writeFloat(mRankingScore);
			out.writeString(mOverrideGroupKey);
			out.writeParcelable(mChannel, flags);
			out.writeStringList(mOverridePeople);
			out.writeTypedList(mSnoozeCriteria);
			out.writeBoolean(mShowBadge);
			out.writeInt(mUserSentiment);
			out.writeBoolean(mHidden);
			out.writeLong(mLastAudiblyAlertedMs);
			out.writeBoolean(mNoisy);
			out.writeTypedList(mSmartActions);
			if (mSmartReplies == null) {
				out.writeInt(-1);
			} else {
				out.writeInt(mSmartReplies.size());
				for (CharSequence cs : mSmartReplies) {
					out.writeCharSequence(cs);
				}
			}
			out.writeBoolean(mCanBubble);
			out.writeBoolean(mIsTextChanged);
			out.writeBoolean(mIsConversation);
			out.writeParcelable(mShortcutInfo, flags);
			out.writeInt(mRankingAdjustment);
			out.writeBoolean(mIsBubble);
			out.writeInt(mProposedImportance);
			out.writeBoolean(mSensitiveContent);
			out.writeString(mSummarization);
		}

		public Ranking(Parcel in) {
			final ClassLoader cl = getClass().getClassLoader();

			final int version = in.readInt();
			if (version != PARCEL_VERSION) {
				throw new IllegalArgumentException("malformed Ranking parcel: " + in + " version "
						+ version + ", expected " + PARCEL_VERSION);
			}
			mKey = in.readString();
			mRank = in.readInt();
			mIsAmbient = in.readBoolean();
			mMatchesInterruptionFilter = in.readBoolean();
			mVisibilityOverride = in.readInt();
			mSuppressedVisualEffects = in.readInt();
			mImportance = in.readInt();
			mImportanceExplanation = in.readCharSequence();
			mRankingScore = in.readFloat();
			mOverrideGroupKey = in.readString();
			mChannel = in.readParcelable(cl, NotificationChannel.class);
			mOverridePeople = in.createStringArrayList();
			mSnoozeCriteria = in.createTypedArrayList(SnoozeCriterion.CREATOR);
			mShowBadge = in.readBoolean();
			mUserSentiment = in.readInt();
			mHidden = in.readBoolean();
			mLastAudiblyAlertedMs = in.readLong();
			mNoisy = in.readBoolean();
			mSmartActions = in.createTypedArrayList(Notification.Action.CREATOR);
			final int replies = in.readInt();
			if (replies < 0) {
				mSmartReplies = null;
			} else {
				mSmartReplies = new ArrayList<>(replies);
				for (int i = 0; i < replies; i++) {
					mSmartReplies.add(in.readCharSequence());
				}
			}
			mCanBubble = in.readBoolean();
			mIsTextChanged = in.readBoolean();
			mIsConversation = in.readBoolean();
			mShortcutInfo = in.readParcelable(cl, ShortcutInfo.class);
			mRankingAdjustment = in.readInt();
			mIsBubble = in.readBoolean();
			mProposedImportance = in.readInt();
			mSensitiveContent = in.readBoolean();
			mSummarization = in.readString();
		}

		public String getKey() {
			return mKey;
		}

		public int getRank() {
			return mRank;
		}

		public boolean isAmbient() {
			return mIsAmbient;
		}

		public int getLockscreenVisibilityOverride() {
			return mVisibilityOverride;
		}

		public int getSuppressedVisualEffects() {
			return mSuppressedVisualEffects;
		}

		public boolean matchesInterruptionFilter() {
			return mMatchesInterruptionFilter;
		}

		public int getImportance() {
			return mImportance;
		}

		public CharSequence getImportanceExplanation() {
			return mImportanceExplanation;
		}

		public float getRankingScore() {
			return mRankingScore;
		}

		public int getProposedImportance() {
			return mProposedImportance;
		}

		public boolean hasSensitiveContent() {
			return mSensitiveContent;
		}

		public String getOverrideGroupKey() {
			return mOverrideGroupKey;
		}

		public NotificationChannel getChannel() {
			return mChannel;
		}

		public int getUserSentiment() {
			return mUserSentiment;
		}

		public List<String> getAdditionalPeople() {
			return mOverridePeople;
		}

		public List<SnoozeCriterion> getSnoozeCriteria() {
			return mSnoozeCriteria;
		}

		public List<Notification.Action> getSmartActions() {
			return mSmartActions == null ? Collections.emptyList() : mSmartActions;
		}

		public void setSmartActions(ArrayList<Notification.Action> smartActions) {
			mSmartActions = smartActions;
		}

		public List<CharSequence> getSmartReplies() {
			return mSmartReplies == null ? Collections.emptyList() : mSmartReplies;
		}

		public boolean canShowBadge() {
			return mShowBadge;
		}

		public boolean isSuspended() {
			return mHidden;
		}

		public long getLastAudiblyAlertedMillis() {
			return mLastAudiblyAlertedMs;
		}

		public boolean canBubble() {
			return mCanBubble;
		}

		public boolean isTextChanged() {
			return mIsTextChanged;
		}

		public boolean isNoisy() {
			return mNoisy;
		}

		public boolean isConversation() {
			return mIsConversation;
		}

		public boolean isBubble() {
			return mIsBubble;
		}

		public ShortcutInfo getConversationShortcutInfo() {
			return mShortcutInfo;
		}

		public String getSummarization() {
			return mSummarization;
		}

		public int getRankingAdjustment() {
			return mRankingAdjustment;
		}

		public void populate(String key, int rank, boolean matchesInterruptionFilter,
		                     int visibilityOverride, int suppressedVisualEffects, int importance,
		                     CharSequence explanation, String overrideGroupKey,
		                     NotificationChannel channel, ArrayList<String> overridePeople,
		                     ArrayList<SnoozeCriterion> snoozeCriteria, boolean showBadge,
		                     int userSentiment, boolean hidden, long lastAudiblyAlertedMs,
		                     boolean noisy, ArrayList<Notification.Action> smartActions,
		                     ArrayList<CharSequence> smartReplies, boolean canBubble,
		                     boolean isTextChanged, boolean isConversation, ShortcutInfo shortcutInfo,
		                     int rankingAdjustment, boolean isBubble, int proposedImportance,
		                     boolean sensitiveContent, String summarization) {
			mKey = key;
			mRank = rank;
			mIsAmbient = importance < NotificationManager.IMPORTANCE_LOW;
			mMatchesInterruptionFilter = matchesInterruptionFilter;
			mVisibilityOverride = visibilityOverride;
			mSuppressedVisualEffects = suppressedVisualEffects;
			mImportance = importance;
			mImportanceExplanation = explanation;
			mOverrideGroupKey = overrideGroupKey;
			mChannel = channel;
			mOverridePeople = overridePeople;
			mSnoozeCriteria = snoozeCriteria;
			mShowBadge = showBadge;
			mUserSentiment = userSentiment;
			mHidden = hidden;
			mLastAudiblyAlertedMs = lastAudiblyAlertedMs;
			mNoisy = noisy;
			mSmartActions = smartActions;
			mSmartReplies = smartReplies;
			mCanBubble = canBubble;
			mIsTextChanged = isTextChanged;
			mIsConversation = isConversation;
			mShortcutInfo = shortcutInfo;
			mRankingAdjustment = rankingAdjustment;
			mIsBubble = isBubble;
			mProposedImportance = proposedImportance;
			mSensitiveContent = sensitiveContent;
			mSummarization = (summarization == null || summarization.isEmpty()) ? null : summarization;
		}

		public Ranking withAudiblyAlertedInfo(Ranking previous) {
			if (previous != null && previous.mLastAudiblyAlertedMs > 0 && this.mLastAudiblyAlertedMs <= 0) {
				this.mLastAudiblyAlertedMs = previous.mLastAudiblyAlertedMs;
			}
			return this;
		}

		public void populate(Ranking other) {
			populate(other.mKey, other.mRank, other.mMatchesInterruptionFilter, other.mVisibilityOverride,
			         other.mSuppressedVisualEffects, other.mImportance, other.mImportanceExplanation,
			         other.mOverrideGroupKey, other.mChannel, other.mOverridePeople, other.mSnoozeCriteria,
			         other.mShowBadge, other.mUserSentiment, other.mHidden, other.mLastAudiblyAlertedMs,
			         other.mNoisy, other.mSmartActions, other.mSmartReplies, other.mCanBubble,
			         other.mIsTextChanged, other.mIsConversation, other.mShortcutInfo,
			         other.mRankingAdjustment, other.mIsBubble, other.mProposedImportance,
			         other.mSensitiveContent, other.mSummarization);
		}

		public static String importanceToString(int importance) {
			switch (importance) {
			case NotificationManager.IMPORTANCE_UNSPECIFIED:
				return "UNSPECIFIED";
			case NotificationManager.IMPORTANCE_NONE:
				return "NONE";
			case NotificationManager.IMPORTANCE_MIN:
				return "MIN";
			case NotificationManager.IMPORTANCE_LOW:
				return "LOW";
			case NotificationManager.IMPORTANCE_DEFAULT:
				return "DEFAULT";
			case NotificationManager.IMPORTANCE_HIGH:
			case NotificationManager.IMPORTANCE_MAX:
				return "HIGH";
			default:
				return "UNKNOWN(" + String.valueOf(importance) + ")";
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Ranking other = (Ranking)o;
			return Objects.equals(mKey, other.mKey)
			       && Objects.equals(mRank, other.mRank)
			       && Objects.equals(mMatchesInterruptionFilter, other.mMatchesInterruptionFilter)
			       && Objects.equals(mVisibilityOverride, other.mVisibilityOverride)
			       && Objects.equals(mSuppressedVisualEffects, other.mSuppressedVisualEffects)
			       && Objects.equals(mImportance, other.mImportance)
			       && Objects.equals(mImportanceExplanation, other.mImportanceExplanation)
			       && Objects.equals(mOverrideGroupKey, other.mOverrideGroupKey)
			       && Objects.equals(mChannel, other.mChannel)
			       && Objects.equals(mOverridePeople, other.mOverridePeople)
			       && Objects.equals(mSnoozeCriteria, other.mSnoozeCriteria)
			       && Objects.equals(mShowBadge, other.mShowBadge)
			       && Objects.equals(mUserSentiment, other.mUserSentiment)
			       && Objects.equals(mHidden, other.mHidden)
			       && Objects.equals(mLastAudiblyAlertedMs, other.mLastAudiblyAlertedMs)
			       && Objects.equals(mNoisy, other.mNoisy)
			       /* Action has no equals(), as in AOSP only the list lengths are compared */
			       && ((mSmartActions == null ? 0 : mSmartActions.size())
			           == (other.mSmartActions == null ? 0 : other.mSmartActions.size()))
			       && Objects.equals(mSmartReplies, other.mSmartReplies)
			       && Objects.equals(mCanBubble, other.mCanBubble)
			       && Objects.equals(mIsTextChanged, other.mIsTextChanged)
			       && Objects.equals(mIsConversation, other.mIsConversation)
			       && Objects.equals((mShortcutInfo == null ? 0 : mShortcutInfo.getId()),
			                         (other.mShortcutInfo == null ? 0 : other.mShortcutInfo.getId()))
			       && Objects.equals(mRankingAdjustment, other.mRankingAdjustment)
			       && Objects.equals(mIsBubble, other.mIsBubble)
			       && Objects.equals(mProposedImportance, other.mProposedImportance)
			       && Objects.equals(mSensitiveContent, other.mSensitiveContent)
			       && Objects.equals(mSummarization, other.mSummarization);
		}
	}

	public void onNotificationPosted(StatusBarNotification sbn) {
		// optional
	}

	public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
		onNotificationPosted(sbn);
	}

	public void onNotificationRemoved(StatusBarNotification sbn) {
		// optional
	}

	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
		onNotificationRemoved(sbn);
	}

	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
		onNotificationRemoved(sbn, rankingMap);
	}

	public void onListenerConnected() {
		// optional
	}

	public void onListenerDisconnected() {
		// optional
	}

	public void onNotificationRankingUpdate(RankingMap rankingMap) {
		// optional
	}

	public void onSilentStatusBarIconsVisibilityChanged(boolean hideSilentStatusIcons) {
		// optional
	}

	public void onNotificationChannelModified(String pkg, UserHandle user, NotificationChannel channel,
	                                          int modificationType) {
		// optional
	}

	public void onNotificationChannelGroupModified(String pkg, UserHandle user, NotificationChannelGroup group,
	                                               int modificationType) {
		// optional
	}

	/* AOSP: each of these returns unless isBound(), nobody binds the service here */
	public final NotificationChannel createConversationNotificationChannelForPackage(String pkg, UserHandle user,
	                                                                                 String parentChannelId,
	                                                                                 String conversationId) {
		if (!isBound()) return null;
		return null;
	}

	public final List<NotificationChannelGroup> getNotificationChannelGroups(String pkg, UserHandle user) {
		if (!isBound()) return null;
		return null;
	}

	public final List<NotificationChannel> getNotificationChannels(String pkg, UserHandle user) {
		if (!isBound()) return null;
		return null;
	}

	public final void updateNotificationChannel(String pkg, UserHandle user, NotificationChannel channel) {
		if (!isBound()) return;
	}

	public final void migrateNotificationFilter(int defaultTypes, List<String> disallowedPkgs) {
		if (!isBound()) return;
	}

	public void onListenerHintsChanged(int hints) {
		// optional
	}

	public void onInterruptionFilterChanged(int interruptionFilter) {
		// optional
	}

	public final void cancelNotification(String pkg, String tag, int id) {}

	public final void cancelNotification(String key) {}

	public final void cancelAllNotifications() {
		cancelNotifications(null);
	}

	public final void cancelNotifications(String[] keys) {}

	public final void snoozeNotification(String key, long durationMs) {}

	public final void setNotificationsShown(String[] keys) {}

	public final void setOnNotificationPostedTrim(int trim) {}

	public StatusBarNotification[] getActiveNotifications() {
		return getActiveNotifications(null, 0);
	}

	public StatusBarNotification[] getActiveNotifications(String[] keys) {
		return getActiveNotifications(keys, 0);
	}

	public StatusBarNotification[] getActiveNotifications(int trim) {
		return getActiveNotifications(null, trim);
	}

	public StatusBarNotification[] getActiveNotifications(String[] keys, int trim) {
		return null; /* AOSP: asks the service only if isBound() */
	}

	public final StatusBarNotification[] getSnoozedNotifications() {
		return null;
	}

	public final int getCurrentListenerHints() {
		return 0;
	}

	public final int getCurrentInterruptionFilter() {
		return INTERRUPTION_FILTER_UNKNOWN;
	}

	public final void clearRequestedListenerHints() {}

	public final void requestListenerHints(int hints) {}

	public final void requestInterruptionFilter(int interruptionFilter) {}

	public RankingMap getCurrentRanking() {
		return null;
	}

	/* AOSP returns its binder wrapper; ATL has no INotificationListener to implement */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected boolean isBound() {
		return false;
	}

	/* AOSP asks the service to bind the listener again; a component that is not enabled in the settings is
	 * not bound */
	public static void requestRebind(ComponentName componentName) {}

	public static void requestUnbind(ComponentName componentName) {}

	public final void requestUnbind() {}
}
