package android.app;

import android.app.Notification.MediaStyle;
import android.atl.ATLLoadedApp;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NotificationManager {

	public static final int IMPORTANCE_UNSPECIFIED = -1000;
	public static final int IMPORTANCE_NONE = 0;
	public static final int IMPORTANCE_MIN = 1;
	public static final int IMPORTANCE_LOW = 2;
	public static final int IMPORTANCE_DEFAULT = 3;
	public static final int IMPORTANCE_HIGH = 4;
	public static final int IMPORTANCE_MAX = 5;

	public static final String ACTION_APP_BLOCK_STATE_CHANGED = "android.app.action.APP_BLOCK_STATE_CHANGED";
	public static final String ACTION_AUTOMATIC_ZEN_RULE = "android.app.action.AUTOMATIC_ZEN_RULE";
	public static final String ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED = "android.app.action.AUTOMATIC_ZEN_RULE_STATUS_CHANGED";
	public static final String ACTION_CONSOLIDATED_NOTIFICATION_POLICY_CHANGED = "android.app.action.CONSOLIDATED_NOTIFICATION_POLICY_CHANGED";
	public static final String ACTION_INTERRUPTION_FILTER_CHANGED = "android.app.action.INTERRUPTION_FILTER_CHANGED";
	public static final String ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED";
	public static final String ACTION_NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED";
	public static final String ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED = "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED";
	public static final String ACTION_NOTIFICATION_POLICY_CHANGED = "android.app.action.NOTIFICATION_POLICY_CHANGED";
	public static final int AUTOMATIC_RULE_STATUS_UNKNOWN = -1;
	public static final int AUTOMATIC_RULE_STATUS_ENABLED = 1;
	public static final int AUTOMATIC_RULE_STATUS_DISABLED = 2;
	public static final int AUTOMATIC_RULE_STATUS_REMOVED = 3;
	public static final int AUTOMATIC_RULE_STATUS_ACTIVATED = 4;
	public static final int AUTOMATIC_RULE_STATUS_DEACTIVATED = 5;
	@Deprecated
	public static final String EXTRA_AUTOMATIC_RULE_ID = "android.app.extra.AUTOMATIC_RULE_ID";
	public static final String EXTRA_AUTOMATIC_ZEN_RULE_ID = "android.app.extra.AUTOMATIC_ZEN_RULE_ID";
	public static final String EXTRA_AUTOMATIC_ZEN_RULE_STATUS = "android.app.extra.AUTOMATIC_ZEN_RULE_STATUS";
	public static final String EXTRA_BLOCKED_STATE = "android.app.extra.BLOCKED_STATE";
	public static final String EXTRA_NOTIFICATION_CHANNEL_GROUP_ID = "android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID";
	public static final String EXTRA_NOTIFICATION_CHANNEL_ID = "android.app.extra.NOTIFICATION_CHANNEL_ID";
	public static final String EXTRA_NOTIFICATION_POLICY = "android.app.extra.NOTIFICATION_POLICY";
	public static final String META_DATA_AUTOMATIC_RULE_TYPE = "android.service.zen.automatic.ruleType";
	public static final String META_DATA_RULE_INSTANCE_LIMIT = "android.service.zen.automatic.ruleInstanceLimit";

	/* GNotificationPriority */
	private static final int G_NORMAL = 0, G_LOW = 1, G_HIGH = 2, G_URGENT = 3;

	/**
	 * The GNotificationPriority for the importance of the notification's channel or, without one, for its
	 * priority mapped to an importance as AOSP does for apps before channels; -1 when the channel is switched off.
	 */
	static int desktopPriority(Notification notification) {
		NotificationChannel channel;
		synchronized (channels) {
			channel = notification.channelId == null ? null : channels.get(notification.channelId);
		}
		int importance = channel == null ? IMPORTANCE_UNSPECIFIED : channel.getImportance();
		if (importance == IMPORTANCE_UNSPECIFIED) {
			int p = notification.priority;
			importance = p <= Notification.PRIORITY_MIN ? IMPORTANCE_MIN : p == Notification.PRIORITY_LOW ? IMPORTANCE_LOW : p == Notification.PRIORITY_DEFAULT ? IMPORTANCE_DEFAULT : p == Notification.PRIORITY_HIGH ? IMPORTANCE_HIGH : IMPORTANCE_MAX;
		}
		if (importance == IMPORTANCE_NONE)
			return -1;
		if (importance <= IMPORTANCE_LOW)
			return G_LOW;
		if (importance == IMPORTANCE_DEFAULT)
			return G_NORMAL;
		return importance == IMPORTANCE_HIGH ? G_HIGH : G_URGENT;
	}

	/* The desktop portals have no text field for a reply (RemoteInput): that button opens the app instead. */
	static PendingIntent buttonIntent(Notification notification, Notification.Action action) {
		return action.getRemoteInputs() == null ? action.actionIntent : notification.contentIntent;
	}

	private static int mpris_notification_id = -1;

	public void cancelAll() {
		StatusBarNotification[] all;
		synchronized (posted) {
			all = posted.values().toArray(new StatusBarNotification[0]);
		}
		for (StatusBarNotification sbn : all)
			cancel(sbn.getTag(), sbn.getId());
	}

	/* AOSP keeps the delegate per package in the NotificationManagerService; ATL runs one app */
	private static String notification_delegate;

	public void setNotificationDelegate(String delegate) {
		notification_delegate = delegate;
	}

	public String getNotificationDelegate() {
		return notification_delegate;
	}

	public void notify(String tag, int id, Notification notification) {
		/* Android 13: without POST_NOTIFICATIONS the system drops the app's notifications */
		if (android.content.pm.PackageManager.deniedByUser("android.permission.POST_NOTIFICATIONS")
		    && !(notification.style instanceof MediaStyle))
			return;
		/* AOSP NotificationManagerService.enqueueNotificationInternal(): an app that targets O or later has no
		 * default channel, a notification without one of its channels is not posted */
		if (!hasChannel(notification)) {
			System.out.println("No Channel found for pkg=" + ATLLoadedApp.getPrimaryApplication().getApplication().getPackageName()
			                   + ", channelId=" + notification.channelId + ", id=" + id + ", tag=" + tag);
			return;
		}
		if (desktopPriority(notification) >= 0 || notification.style instanceof MediaStyle)
			post(tag, id, notification);
		scheduleTimeout(tag, id, notification);
		if (notification.style instanceof MediaStyle) { // MPRIS content is handled by MediaSession implementation
			if (mpris_notification_id == -1) {
				Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
				nativeShowMPRIS(application.getPackageName(), application.get_app_label());
				mpris_notification_id = id;
			}
			return;
		}

		System.out.println("notify(" + tag + ", " + id + ", " + notification + ") called");
		int priority = desktopPriority(notification);
		if (priority < 0)
			return;
		String key = postedKey(tag, id);
		long builder = nativeInitBuilder();
		Notification.Action[] actions = notification.actions != null ? notification.actions : new Notification.Action[0];
		for (int i = 0; i < actions.length; i++)
			if (buttonIntent(notification, actions[i]) != null)
				nativeAddAction(builder, String.valueOf(actions[i].title), key, i);
		String iconPath = notification.iconPath == null || notification.iconPath.endsWith(".xml") ? null : notification.iconPath;
		/* the desktop notification has no expanded form: the text of a style (big text, lines, latest message) */
		CharSequence styleText = notification.style != null ? notification.style.desktopText() : null;
		String text = styleText != null ? styleText.toString() : notification.text;
		nativeShowNotification(builder, key, notification.title, text, iconPath, notification.ongoing, priority, notification.contentIntent != null);
	}

	/* AOSP: an app that targets O or later posts only to a channel it created; older apps post to the default channel */
	private static boolean hasChannel(Notification notification) {
		if (ATLLoadedApp.getPrimaryApplication().getApplication().getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.O)
			return true;
		synchronized (channels) {
			return notification.channelId != null && channels.containsKey(notification.channelId);
		}
	}

	/* AOSP NotificationManagerService: setTimeoutAfter() cancels the notification after the duration (REASON_TIMEOUT);
	 * an update or cancel of the notification ends the old timeout */
	private static final java.util.HashMap<String, Runnable> timeouts = new java.util.HashMap<>();

	private void scheduleTimeout(final String tag, final int id, Notification notification) {
		final String key = postedKey(tag, id);
		Handler handler = new Handler(Looper.getMainLooper());
		synchronized (timeouts) {
			Runnable old = timeouts.remove(key);
			if (old != null)
				handler.removeCallbacks(old);
			if (notification.getTimeoutAfter() <= 0)
				return;
			Runnable timeout = new Runnable() {
				@Override
				public void run() {
					synchronized (timeouts) {
						if (timeouts.get(key) != this)
							return;
					}
					cancel(tag, id);
				}
			};
			timeouts.put(key, timeout);
			handler.postDelayed(timeout, notification.getTimeoutAfter());
		}
	}

	/**
	 * A click at the desktop on the notification (actionIndex -1) or one of its buttons, called by the native code on
	 * the main thread. Like SystemUI and AOSP NotificationManagerService.onNotificationClick()/onNotificationActionClick():
	 * the PendingIntent of the posted notification is sent; a click on the notification cancels it when it has
	 * FLAG_AUTO_CANCEL (not one of a foreground service). Returns whether an activity was started.
	 */
	static boolean desktopAction(String key, int actionIndex) {
		StatusBarNotification sbn;
		synchronized (posted) {
			sbn = posted.get(key);
		}
		if (sbn == null) {
			System.out.println("No notification with key: " + key);
			return false;
		}
		Notification notification = sbn.getNotification();
		PendingIntent intent;
		if (actionIndex < 0)
			intent = notification.contentIntent;
		else if (notification.actions != null && actionIndex < notification.actions.length)
			intent = buttonIntent(notification, notification.actions[actionIndex]);
		else
			intent = null;
		if (actionIndex < 0 && (notification.flags & Notification.FLAG_AUTO_CANCEL) != 0
		    && (notification.flags & (Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_BUBBLE)) == 0)
			new NotificationManager().cancel(sbn.getTag(), sbn.getId());
		if (intent == null)
			return false;
		intent.send();
		return intent.isActivity();
	}

	/* AOSP NotificationManagerService keeps the posted notifications of the app for
	 * getActiveNotifications() until the app cancels them. ATL does not learn when the user closes
	 * one on the desktop, so those stay until the app cancels them. */
	private static final LinkedHashMap<String, StatusBarNotification> posted = new LinkedHashMap<>();

	private static String postedKey(String tag, int id) {
		return id + "|" + tag;
	}

	private static void post(String tag, int id, Notification notification) {
		Application application = ATLLoadedApp.getPrimaryApplication().getApplication();
		String pkg = application.getPackageName();
		StatusBarNotification sbn = new StatusBarNotification(pkg, pkg, id, tag, android.os.Process.myUid(), android.os.Process.myPid(), notification, android.os.Process.myUserHandle(), null, System.currentTimeMillis());
		synchronized (posted) {
			posted.remove(postedKey(tag, id)); // an update goes to the end, as the newest
			posted.put(postedKey(tag, id), sbn);
		}
	}

	public void notify(int id, Notification notification) {
		notify(null, id, notification);
	}

	public void cancel(final String tag, final int id) {
		final String key = postedKey(tag, id);
		synchronized (posted) {
			posted.remove(key);
		}
		synchronized (timeouts) {
			Runnable old = timeouts.remove(key);
			if (old != null)
				new Handler(Looper.getMainLooper()).removeCallbacks(old);
		}
		// remove_notification doesn't work reliably when sent directly after add_notification in GNOME session.
		// So we give some extra delay here.
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mpris_notification_id == id) {
					mpris_notification_id = -1;
					nativeCancelMPRIS();
				} else {
					nativeCancel(key);
				}
			}
		}, 100);
	}

	public void cancel(int id) {
		cancel(null, id);
	}

	/* AOSP keeps the channels per package in the NotificationManagerService; ATL runs one app */
	private static final LinkedHashMap<String, NotificationChannel> channels = new LinkedHashMap<>();

	/**
	 * Creates a notification channel that notifications can be posted to. Creating an existing
	 * channel with its original values performs no operation; only its name, description and
	 * group are updated and its importance lowered, as in AOSP (PreferencesHelper.createNotificationChannel(): the
	 * app may lower it while the user has changed nothing of the channel, which ATL's user cannot).
	 */
	public void createNotificationChannel(NotificationChannel channel) {
		synchronized (channels) {
			NotificationChannel existing = channels.get(channel.getId());
			if (existing == null) {
				channel.setOriginalImportance(channel.getImportance());
				channels.put(channel.getId(), channel);
				return;
			}
			existing.setName(channel.getName());
			existing.setDescription(channel.getDescription());
			if (channel.getGroup() != null && existing.getGroup() == null)
				existing.setGroup(channel.getGroup());
			if (channel.getImportance() < existing.getImportance())
				existing.setImportance(channel.getImportance());
			if (existing.getOriginalImportance() == IMPORTANCE_UNSPECIFIED)
				existing.setOriginalImportance(channel.getImportance());
		}
	}

	protected native long nativeInitBuilder();
	protected native void nativeAddAction(long builder, String title, String key, int actionIndex);
	protected native void nativeShowNotification(long builder, String key, String title, String text, String iconPath, boolean ongoing, int priority, boolean hasContentIntent);
	protected native void nativeShowMPRIS(String packageName, String identiy);
	protected native void nativeCancel(String key);
	protected native void nativeCancelMPRIS();

	/* AOSP keeps the groups with the channels in the NotificationManagerService */
	private static final LinkedHashMap<String, NotificationChannelGroup> groups = new LinkedHashMap<>();

	public void createNotificationChannelGroup(NotificationChannelGroup group) {
		synchronized (channels) {
			groups.put(group.getId(), group.copyWith(new ArrayList<NotificationChannel>()));
		}
	}

	/* AOSP: the group with the channels that belong to it */
	public NotificationChannelGroup getNotificationChannelGroup(String channelGroupId) {
		synchronized (channels) {
			NotificationChannelGroup group = groups.get(channelGroupId);
			if (group == null)
				return null;
			List<NotificationChannel> in = new ArrayList<>();
			for (NotificationChannel channel : channels.values())
				if (channelGroupId.equals(channel.getGroup()))
					in.add(channel);
			return group.copyWith(in);
		}
	}

	/* AOSP: deleting a group deletes its channels */
	public void deleteNotificationChannelGroup(String groupId) {
		synchronized (channels) {
			if (groups.remove(groupId) == null)
				return;
			channels.values().removeIf(channel -> groupId.equals(channel.getGroup()));
		}
	}

	/* AOSP: the conversation's own channel, or its parent channel when it has none; ATL keeps no conversation channels */
	public NotificationChannel getNotificationChannel(String channelId, String conversationId) {
		return getNotificationChannel(channelId);
	}

	public List<NotificationChannel> getNotificationChannels() {
		synchronized (channels) {
			return new ArrayList<>(channels.values());
		}
	}

	public List<NotificationChannelGroup> getNotificationChannelGroups() {
		synchronized (channels) {
			List<NotificationChannelGroup> list = new ArrayList<>();
			for (String id : groups.keySet())
				list.add(getNotificationChannelGroup(id));
			return list;
		}
	}

	/* AOSP getImportance asks NotificationManagerService.getPackageImportance: IMPORTANCE_DEFAULT while the app
	 * holds POST_NOTIFICATIONS, else IMPORTANCE_NONE (same test as areNotificationsEnabled here) */
	public int getImportance() {
		return areNotificationsEnabled() ? IMPORTANCE_DEFAULT : IMPORTANCE_NONE;
	}

	public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
	public static final int INTERRUPTION_FILTER_ALL = 1;
	public static final int INTERRUPTION_FILTER_PRIORITY = 2;
	public static final int INTERRUPTION_FILTER_NONE = 3;
	public static final int INTERRUPTION_FILTER_ALARMS = 4;

	/* AOSP: zenModeToInterruptionFilter(getZenMode()); ATL has no Do Not Disturb, the zen mode is off */
	public final int getCurrentInterruptionFilter() {
		return INTERRUPTION_FILTER_ALL;
	}

	/* AOSP: Do Not Disturb access is granted by the user in the settings, which ATL does not have */
	public boolean isNotificationPolicyAccessGranted() {
		return false;
	}

	/* AOSP NotificationManagerService.enforcePolicyAccess(): without the access granted by the user
	 * (isNotificationPolicyAccessGranted(), never in ATL) setInterruptionFilter throws */
	public final void setInterruptionFilter(int interruptionFilter) {
		throw new SecurityException("Notification policy access denied");
	}

	/* AOSP NotificationManager addAutomaticZenRule/updateAutomaticZenRule/removeAutomaticZenRule/
	 * getAutomaticZenRules/setAutomaticZenRuleState: the service enforces the policy access of the user
	 * (enforcePolicyAccess) first, which ATL never has (isNotificationPolicyAccessGranted()), so all of them
	 * throw the SecurityException like setInterruptionFilter(). */
	public String addAutomaticZenRule(AutomaticZenRule automaticZenRule) {
		throw new SecurityException("Notification policy access denied");
	}

	public boolean updateAutomaticZenRule(String id, AutomaticZenRule automaticZenRule) {
		throw new SecurityException("Notification policy access denied");
	}

	public boolean removeAutomaticZenRule(String id) {
		throw new SecurityException("Notification policy access denied");
	}

	public java.util.Map<String, AutomaticZenRule> getAutomaticZenRules() {
		throw new SecurityException("Notification policy access denied");
	}

	public void setAutomaticZenRuleState(String id, android.service.notification.Condition condition) {
		throw new SecurityException("Notification policy access denied");
	}

	/* AOSP deprecated areBubblesAllowed(): the service's per-app bubble preference of this package equals
	 * BUBBLE_PREFERENCE_ALL; an app nobody has configured has BUBBLE_PREFERENCE_NONE. (The service source is not
	 * in the AOSP tree used here: the default is not verified against it.) */
	@Deprecated
	public boolean areBubblesAllowed() {
		return false;
	}

	/* AOSP canPostPromotedNotifications(): the service's per-app promotion permission of this package; the
	 * default of an app nobody has changed is allowed. (Service source not in the tree, default not verified.) */
	public boolean canPostPromotedNotifications() {
		return true;
	}

	/* AOSP NotificationManager.getNotificationPolicy(): the Do Not Disturb policy of the manual rule
	 * (ZenModeConfig.toNotificationPolicy()); nobody changes it on ATL, so it is the one built from the
	 * ZenModeConfig DEFAULT_* values: alarms, media, calls and messages from starred contacts, repeat callers,
	 * important conversations, no priority channels; full screen intents, lights, peeking and ambient
	 * suppressed (with the legacy SCREEN_OFF/SCREEN_ON bits). Reading needs no policy access. */
	public Policy getNotificationPolicy() {
		return new Policy(Policy.PRIORITY_CATEGORY_ALARMS | Policy.PRIORITY_CATEGORY_MEDIA
		                  | Policy.PRIORITY_CATEGORY_CALLS | Policy.PRIORITY_CATEGORY_MESSAGES
		                  | Policy.PRIORITY_CATEGORY_REPEAT_CALLERS | Policy.PRIORITY_CATEGORY_CONVERSATIONS,
		                  Policy.PRIORITY_SENDERS_STARRED, Policy.PRIORITY_SENDERS_STARRED,
		                  Policy.SUPPRESSED_EFFECT_SCREEN_OFF | Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
		                  | Policy.SUPPRESSED_EFFECT_LIGHTS | Policy.SUPPRESSED_EFFECT_PEEK
		                  | Policy.SUPPRESSED_EFFECT_SCREEN_ON | Policy.SUPPRESSED_EFFECT_AMBIENT,
		                  0, Policy.CONVERSATION_SENDERS_IMPORTANT);
	}

	/* AOSP NotificationManager.Policy: the public part (constants, fields, constructors, parcel, equals) */
	public static class Policy implements android.os.Parcelable {
		public static final int PRIORITY_CATEGORY_REMINDERS = 1 << 0;
		public static final int PRIORITY_CATEGORY_EVENTS = 1 << 1;
		public static final int PRIORITY_CATEGORY_MESSAGES = 1 << 2;
		public static final int PRIORITY_CATEGORY_CALLS = 1 << 3;
		public static final int PRIORITY_CATEGORY_REPEAT_CALLERS = 1 << 4;
		public static final int PRIORITY_CATEGORY_ALARMS = 1 << 5;
		public static final int PRIORITY_CATEGORY_MEDIA = 1 << 6;
		public static final int PRIORITY_CATEGORY_SYSTEM = 1 << 7;
		public static final int PRIORITY_CATEGORY_CONVERSATIONS = 1 << 8;

		public static final int PRIORITY_SENDERS_ANY = 0;
		public static final int PRIORITY_SENDERS_CONTACTS = 1;
		public static final int PRIORITY_SENDERS_STARRED = 2;

		public static final int CONVERSATION_SENDERS_ANYONE = 1; // ZenPolicy values
		public static final int CONVERSATION_SENDERS_IMPORTANT = 2;
		public static final int CONVERSATION_SENDERS_NONE = 3;
		public static final int CONVERSATION_SENDERS_UNSET = -1;

		public static final int SUPPRESSED_EFFECTS_UNSET = -1;
		@Deprecated public static final int SUPPRESSED_EFFECT_SCREEN_OFF = 1 << 0;
		@Deprecated public static final int SUPPRESSED_EFFECT_SCREEN_ON = 1 << 1;
		public static final int SUPPRESSED_EFFECT_FULL_SCREEN_INTENT = 1 << 2;
		public static final int SUPPRESSED_EFFECT_LIGHTS = 1 << 3;
		public static final int SUPPRESSED_EFFECT_PEEK = 1 << 4;
		public static final int SUPPRESSED_EFFECT_STATUS_BAR = 1 << 5;
		public static final int SUPPRESSED_EFFECT_BADGE = 1 << 6;
		public static final int SUPPRESSED_EFFECT_AMBIENT = 1 << 7;
		public static final int SUPPRESSED_EFFECT_NOTIFICATION_LIST = 1 << 8;

		public static final int STATE_HAS_PRIORITY_CHANNELS = 1 << 0;
		public static final int STATE_PRIORITY_CHANNELS_BLOCKED = 1 << 1;
		public static final int STATE_UNSET = -1;

		public final int priorityCategories;
		public final int priorityCallSenders;
		public final int priorityMessageSenders;
		public final int priorityConversationSenders;
		public final int suppressedVisualEffects;
		public final int state;

		public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders) {
			this(priorityCategories, priorityCallSenders, priorityMessageSenders,
			     SUPPRESSED_EFFECTS_UNSET, STATE_UNSET, CONVERSATION_SENDERS_UNSET);
		}

		public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects) {
			this(priorityCategories, priorityCallSenders, priorityMessageSenders,
			     suppressedVisualEffects, STATE_UNSET, CONVERSATION_SENDERS_UNSET);
		}

		public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects, int priorityConversationSenders) {
			this(priorityCategories, priorityCallSenders, priorityMessageSenders,
			     suppressedVisualEffects, STATE_UNSET, priorityConversationSenders);
		}

		/* @hide in AOSP */
		public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects, int state, int priorityConversationSenders) {
			this.priorityCategories = priorityCategories;
			this.priorityCallSenders = priorityCallSenders;
			this.priorityMessageSenders = priorityMessageSenders;
			this.suppressedVisualEffects = suppressedVisualEffects;
			this.state = state;
			this.priorityConversationSenders = priorityConversationSenders;
		}

		public Policy(android.os.Parcel source) {
			this(source.readInt(), source.readInt(), source.readInt(), source.readInt(),
			     source.readInt(), source.readInt());
		}

		@Override
		public void writeToParcel(android.os.Parcel dest, int flags) {
			dest.writeInt(priorityCategories);
			dest.writeInt(priorityCallSenders);
			dest.writeInt(priorityMessageSenders);
			dest.writeInt(suppressedVisualEffects);
			dest.writeInt(state);
			dest.writeInt(priorityConversationSenders);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final android.os.Parcelable.Creator<Policy> CREATOR = new android.os.Parcelable.Creator<Policy>() {
			@Override
			public Policy createFromParcel(android.os.Parcel in) {
				return new Policy(in);
			}

			@Override
			public Policy[] newArray(int size) {
				return new Policy[size];
			}
		};

		/** @hide */
		public static final int[] ALL_PRIORITY_CATEGORIES = {
			PRIORITY_CATEGORY_ALARMS,
			PRIORITY_CATEGORY_MEDIA,
			PRIORITY_CATEGORY_SYSTEM,
			PRIORITY_CATEGORY_REMINDERS,
			PRIORITY_CATEGORY_EVENTS,
			PRIORITY_CATEGORY_MESSAGES,
			PRIORITY_CATEGORY_CALLS,
			PRIORITY_CATEGORY_REPEAT_CALLERS,
			PRIORITY_CATEGORY_CONVERSATIONS,
		};

		private static final int[] ALL_SUPPRESSED_EFFECTS = {
			SUPPRESSED_EFFECT_SCREEN_OFF,
			SUPPRESSED_EFFECT_SCREEN_ON,
			SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
			SUPPRESSED_EFFECT_LIGHTS,
			SUPPRESSED_EFFECT_PEEK,
			SUPPRESSED_EFFECT_STATUS_BAR,
			SUPPRESSED_EFFECT_BADGE,
			SUPPRESSED_EFFECT_AMBIENT,
			SUPPRESSED_EFFECT_NOTIFICATION_LIST
		};

		public static String suppressedEffectsToString(int effects) {
			if (effects <= 0) return "";
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ALL_SUPPRESSED_EFFECTS.length; i++) {
				final int effect = ALL_SUPPRESSED_EFFECTS[i];
				if ((effects & effect) != 0) {
					if (sb.length() > 0) sb.append(',');
					sb.append(effectToString(effect));
				}
				effects &= ~effect;
			}
			if (effects != 0) {
				if (sb.length() > 0) sb.append(',');
				sb.append("UNKNOWN_").append(effects);
			}
			return sb.toString();
		}
		public static String priorityCategoriesToString(int priorityCategories) {
			if (priorityCategories == 0) return "";
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ALL_PRIORITY_CATEGORIES.length; i++) {
				final int priorityCategory = ALL_PRIORITY_CATEGORIES[i];
				if ((priorityCategories & priorityCategory) != 0) {
					if (sb.length() > 0) sb.append(',');
					sb.append(priorityCategoryToString(priorityCategory));
				}
				priorityCategories &= ~priorityCategory;
			}
			if (priorityCategories != 0) {
				if (sb.length() > 0) sb.append(',');
				sb.append("PRIORITY_CATEGORY_UNKNOWN_").append(priorityCategories);
			}
			return sb.toString();
		}
		public static String prioritySendersToString(int prioritySenders) {
			switch (prioritySenders) {
				case PRIORITY_SENDERS_ANY: return "PRIORITY_SENDERS_ANY";
				case PRIORITY_SENDERS_CONTACTS: return "PRIORITY_SENDERS_CONTACTS";
				case PRIORITY_SENDERS_STARRED: return "PRIORITY_SENDERS_STARRED";
				default: return "PRIORITY_SENDERS_UNKNOWN_" + prioritySenders;
			}
		}
		private static String effectToString(int effect) {
			switch (effect) {
				case SUPPRESSED_EFFECT_FULL_SCREEN_INTENT:
					return "SUPPRESSED_EFFECT_FULL_SCREEN_INTENT";
				case SUPPRESSED_EFFECT_LIGHTS:
					return "SUPPRESSED_EFFECT_LIGHTS";
				case SUPPRESSED_EFFECT_PEEK:
					return "SUPPRESSED_EFFECT_PEEK";
				case SUPPRESSED_EFFECT_STATUS_BAR:
					return "SUPPRESSED_EFFECT_STATUS_BAR";
				case SUPPRESSED_EFFECT_BADGE:
					return "SUPPRESSED_EFFECT_BADGE";
				case SUPPRESSED_EFFECT_AMBIENT:
					return "SUPPRESSED_EFFECT_AMBIENT";
				case SUPPRESSED_EFFECT_NOTIFICATION_LIST:
					return "SUPPRESSED_EFFECT_NOTIFICATION_LIST";
				case SUPPRESSED_EFFECT_SCREEN_OFF:
					return "SUPPRESSED_EFFECT_SCREEN_OFF";
				case SUPPRESSED_EFFECT_SCREEN_ON:
					return "SUPPRESSED_EFFECT_SCREEN_ON";
				case SUPPRESSED_EFFECTS_UNSET:
					return "SUPPRESSED_EFFECTS_UNSET";
				default: return "UNKNOWN_" + effect;
			}
		}
		private static String priorityCategoryToString(int priorityCategory) {
			switch (priorityCategory) {
				case PRIORITY_CATEGORY_REMINDERS: return "PRIORITY_CATEGORY_REMINDERS";
				case PRIORITY_CATEGORY_EVENTS: return "PRIORITY_CATEGORY_EVENTS";
				case PRIORITY_CATEGORY_MESSAGES: return "PRIORITY_CATEGORY_MESSAGES";
				case PRIORITY_CATEGORY_CALLS: return "PRIORITY_CATEGORY_CALLS";
				case PRIORITY_CATEGORY_REPEAT_CALLERS: return "PRIORITY_CATEGORY_REPEAT_CALLERS";
				case PRIORITY_CATEGORY_ALARMS: return "PRIORITY_CATEGORY_ALARMS";
				case PRIORITY_CATEGORY_MEDIA: return "PRIORITY_CATEGORY_MEDIA";
				case PRIORITY_CATEGORY_SYSTEM: return "PRIORITY_CATEGORY_SYSTEM";
				case PRIORITY_CATEGORY_CONVERSATIONS: return "PRIORITY_CATEGORY_CONVERSATIONS";
				default: return "PRIORITY_CATEGORY_UNKNOWN_" + priorityCategory;
			}
		}

		/* AOSP Policy.allowPriorityChannels() (API 35); hasPriorityChannels() is @hide */
		public boolean hasPriorityChannels() {
			return (state & STATE_HAS_PRIORITY_CHANNELS) != 0;
		}

		public boolean allowPriorityChannels() {
			return state == STATE_UNSET || (state & STATE_PRIORITY_CHANNELS_BLOCKED) == 0;
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(priorityCategories, priorityCallSenders, priorityMessageSenders,
			                              suppressedVisualEffects, state, priorityConversationSenders);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Policy))
				return false;
			if (o == this)
				return true;
			final Policy other = (Policy)o;
			return other.priorityCategories == priorityCategories
			    && other.priorityCallSenders == priorityCallSenders
			    && other.priorityMessageSenders == priorityMessageSenders
			    && suppressedVisualEffectsEqual(suppressedVisualEffects, other.suppressedVisualEffects)
			    && other.state == this.state
			    && other.priorityConversationSenders == this.priorityConversationSenders;
		}

		private static boolean suppressedVisualEffectsEqual(int suppressedEffects, int otherSuppressedVisualEffects) {
			if (suppressedEffects == otherSuppressedVisualEffects)
				return true;
			if ((suppressedEffects & SUPPRESSED_EFFECT_SCREEN_ON) != 0)
				suppressedEffects |= SUPPRESSED_EFFECT_PEEK;
			if ((suppressedEffects & SUPPRESSED_EFFECT_SCREEN_OFF) != 0) {
				suppressedEffects |= SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
				suppressedEffects |= SUPPRESSED_EFFECT_LIGHTS;
				suppressedEffects |= SUPPRESSED_EFFECT_AMBIENT;
			}
			if ((otherSuppressedVisualEffects & SUPPRESSED_EFFECT_SCREEN_ON) != 0)
				otherSuppressedVisualEffects |= SUPPRESSED_EFFECT_PEEK;
			if ((otherSuppressedVisualEffects & SUPPRESSED_EFFECT_SCREEN_OFF) != 0) {
				otherSuppressedVisualEffects |= SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
				otherSuppressedVisualEffects |= SUPPRESSED_EFFECT_LIGHTS;
				otherSuppressedVisualEffects |= SUPPRESSED_EFFECT_AMBIENT;
			}
			if ((suppressedEffects & SUPPRESSED_EFFECT_SCREEN_ON) != (otherSuppressedVisualEffects & SUPPRESSED_EFFECT_SCREEN_ON)) {
				int currSuppressedEffects = (suppressedEffects & SUPPRESSED_EFFECT_SCREEN_ON) != 0 ? otherSuppressedVisualEffects : suppressedEffects;
				if ((currSuppressedEffects & SUPPRESSED_EFFECT_PEEK) == 0)
					return false;
			}
			if ((suppressedEffects & SUPPRESSED_EFFECT_SCREEN_OFF) != (otherSuppressedVisualEffects & SUPPRESSED_EFFECT_SCREEN_OFF)) {
				int currSuppressedEffects = (suppressedEffects & SUPPRESSED_EFFECT_SCREEN_OFF) != 0 ? otherSuppressedVisualEffects : suppressedEffects;
				if ((currSuppressedEffects & SUPPRESSED_EFFECT_FULL_SCREEN_INTENT) == 0
				    || (currSuppressedEffects & SUPPRESSED_EFFECT_LIGHTS) == 0
				    || (currSuppressedEffects & SUPPRESSED_EFFECT_AMBIENT) == 0)
					return false;
			}
			int thisWithoutOldEffects = suppressedEffects & ~SUPPRESSED_EFFECT_SCREEN_ON & ~SUPPRESSED_EFFECT_SCREEN_OFF;
			int otherWithoutOldEffects = otherSuppressedVisualEffects & ~SUPPRESSED_EFFECT_SCREEN_ON & ~SUPPRESSED_EFFECT_SCREEN_OFF;
			return thisWithoutOldEffects == otherWithoutOldEffects;
		}
	}

	/* AOSP NotificationManagerService.isPackagePaused(): true only while the package is suspended, which
	 * ATL never does */
	public boolean areNotificationsPaused() {
		return false;
	}

	/* AOSP: the listener is enabled by the user in the settings (enabled_notification_listeners), which ATL
	 * does not have */
	public boolean isNotificationListenerAccessGranted(android.content.ComponentName listener) {
		return false;
	}

	/* AOSP: the service allows full screen intents below target SDK 34, above with USE_FULL_SCREEN_INTENT,
	 * which ATL grants like every permission */
	public boolean canUseFullScreenIntent() {
		return true;
	}

	public boolean areNotificationsEnabled() {
		return !android.content.pm.PackageManager.deniedByUser("android.permission.POST_NOTIFICATIONS");
	}

	/* AOSP NotificationManagerService.deleteNotificationChannel(): the notifications of the channel go with it */
	public void deleteNotificationChannel(String channelId) {
		StatusBarNotification[] all;
		synchronized (posted) {
			all = posted.values().toArray(new StatusBarNotification[0]);
		}
		for (StatusBarNotification sbn : all)
			if (channelId.equals(sbn.getNotification().getChannelId()))
				cancel(sbn.getTag(), sbn.getId());
		synchronized (channels) {
			channels.remove(channelId);
		}
	}

	public void createNotificationChannelGroups(List<NotificationChannelGroup> groups) {
		for (NotificationChannelGroup group : groups)
			createNotificationChannelGroup(group);
	}

	public void createNotificationChannels(List<NotificationChannel> channels) {
		for (NotificationChannel channel : channels)
			createNotificationChannel(channel);
	}

	/* AOSP: the notifications the app posted and has not canceled */
	public StatusBarNotification[] getActiveNotifications() {
		synchronized (posted) {
			return posted.values().toArray(new StatusBarNotification[0]);
		}
	}

	public NotificationChannel getNotificationChannel(String channelId) {
		synchronized (channels) {
			return channels.get(channelId);
		}
	}

	/* AOSP NotificationManager members that ask INotificationManager; with the state of ATL (no Do Not Disturb rules,
	 * no policy access, no bubbles, the app's own package) */
	public static final int BUBBLE_PREFERENCE_NONE = 0;
	public static final int BUBBLE_PREFERENCE_ALL = 1;
	public static final int BUBBLE_PREFERENCE_SELECTED = 2;

	public boolean areAutomaticZenRulesUserManaged() { return true; }
	public boolean areBubblesEnabled() { return false; }
	public int getBubblePreference() { return BUBBLE_PREFERENCE_NONE; }
	public AutomaticZenRule getAutomaticZenRule(String id) { throw new SecurityException("Notification policy access denied"); }
	public int getAutomaticZenRuleState(String id) { throw new SecurityException("Notification policy access denied"); }
	public void setNotificationPolicy(Policy policy) { throw new SecurityException("Notification policy access denied"); }
	public Policy getConsolidatedNotificationPolicy() { return getNotificationPolicy(); }
	public boolean matchesCallFilter(android.net.Uri uri) { return true; }
	public boolean shouldHideSilentStatusBarIcons() { return false; }
	public boolean canNotifyAsPackage(String pkg) { return pkg != null && pkg.equals(ATLLoadedApp.getPrimaryApplication().getApplication().getPackageName()); }
	public void notifyAsPackage(String targetPackage, String tag, int id, Notification notification) {
		if (!canNotifyAsPackage(targetPackage))
			throw new SecurityException("Not allowed to notify as package " + targetPackage);
		notify(tag, id, notification);
	}
	public void cancelAsPackage(String targetPackage, String tag, int id) {
		if (!canNotifyAsPackage(targetPackage))
			throw new SecurityException("Not allowed to cancel as package " + targetPackage);
		cancel(tag, id);
	}
}
