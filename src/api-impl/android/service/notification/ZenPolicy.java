/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.service.notification;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes the interruption policy of a zen mode: which sounds and visual effects are allowed.
 * AOSP core/java/android/service/notification/ZenPolicy.java (android-16). The hidden apply(),
 * overwrittenFieldsToString(), getChannelType()-style helpers and the proto dump are left out.
 */
public final class ZenPolicy implements Parcelable {
	public static final int FIELD_MESSAGES = 1 << 0;
	public static final int FIELD_CALLS = 1 << 1;
	public static final int FIELD_CONVERSATIONS = 1 << 2;
	public static final int FIELD_ALLOW_CHANNELS = 1 << 3;
	public static final int FIELD_PRIORITY_CATEGORY_REMINDERS = 1 << 4;
	public static final int FIELD_PRIORITY_CATEGORY_EVENTS = 1 << 5;
	public static final int FIELD_PRIORITY_CATEGORY_REPEAT_CALLERS = 1 << 6;
	public static final int FIELD_PRIORITY_CATEGORY_ALARMS = 1 << 7;
	public static final int FIELD_PRIORITY_CATEGORY_MEDIA = 1 << 8;
	public static final int FIELD_PRIORITY_CATEGORY_SYSTEM = 1 << 9;
	public static final int FIELD_VISUAL_EFFECT_FULL_SCREEN_INTENT = 1 << 10;
	public static final int FIELD_VISUAL_EFFECT_LIGHTS = 1 << 11;
	public static final int FIELD_VISUAL_EFFECT_PEEK = 1 << 12;
	public static final int FIELD_VISUAL_EFFECT_STATUS_BAR = 1 << 13;
	public static final int FIELD_VISUAL_EFFECT_BADGE = 1 << 14;
	public static final int FIELD_VISUAL_EFFECT_AMBIENT = 1 << 15;
	public static final int FIELD_VISUAL_EFFECT_NOTIFICATION_LIST = 1 << 16;

	public static final int PRIORITY_CATEGORY_REMINDERS = 0;
	public static final int PRIORITY_CATEGORY_EVENTS = 1;
	public static final int PRIORITY_CATEGORY_MESSAGES = 2;
	public static final int PRIORITY_CATEGORY_CALLS = 3;
	public static final int PRIORITY_CATEGORY_REPEAT_CALLERS = 4;
	public static final int PRIORITY_CATEGORY_ALARMS = 5;
	public static final int PRIORITY_CATEGORY_MEDIA = 6;
	public static final int PRIORITY_CATEGORY_SYSTEM = 7;
	public static final int PRIORITY_CATEGORY_CONVERSATIONS = 8;
	public static final int NUM_PRIORITY_CATEGORIES = 9;

	public static final int VISUAL_EFFECT_FULL_SCREEN_INTENT = 0;
	public static final int VISUAL_EFFECT_LIGHTS = 1;
	public static final int VISUAL_EFFECT_PEEK = 2;
	public static final int VISUAL_EFFECT_STATUS_BAR = 3;
	public static final int VISUAL_EFFECT_BADGE = 4;
	public static final int VISUAL_EFFECT_AMBIENT = 5;
	public static final int VISUAL_EFFECT_NOTIFICATION_LIST = 6;
	public static final int NUM_VISUAL_EFFECTS = 7;

	public static final int PEOPLE_TYPE_UNSET = 0;
	public static final int PEOPLE_TYPE_ANYONE = 1;
	public static final int PEOPLE_TYPE_CONTACTS = 2;
	public static final int PEOPLE_TYPE_STARRED = 3;
	public static final int PEOPLE_TYPE_NONE = 4;

	public static final int CONVERSATION_SENDERS_UNSET = 0;
	public static final int CONVERSATION_SENDERS_ANYONE = 1;
	public static final int CONVERSATION_SENDERS_IMPORTANT = 2;
	public static final int CONVERSATION_SENDERS_NONE = 3;

	public static final int STATE_UNSET = 0;
	public static final int STATE_ALLOW = 1;
	public static final int STATE_DISALLOW = 2;

	public static final int CHANNEL_POLICY_UNSET = 0;
	public static final int CHANNEL_POLICY_PRIORITY = 1;
	public static final int CHANNEL_POLICY_NONE = 2;

	private List<Integer> mPriorityCategories;
	private List<Integer> mVisualEffects;
	private int mPriorityMessages = PEOPLE_TYPE_UNSET;
	private int mPriorityCalls = PEOPLE_TYPE_UNSET;
	private int mConversationSenders = CONVERSATION_SENDERS_UNSET;
	private int mAllowChannels = CHANNEL_POLICY_UNSET;

	public ZenPolicy() {
		mPriorityCategories = new ArrayList<>(Collections.nCopies(NUM_PRIORITY_CATEGORIES, 0));
		mVisualEffects = new ArrayList<>(Collections.nCopies(NUM_VISUAL_EFFECTS, 0));
	}

	public ZenPolicy(List<Integer> priorityCategories, List<Integer> visualEffects, int priorityMessages,
	                 int priorityCalls, int conversationSenders, int allowChannels) {
		mPriorityCategories = priorityCategories;
		mVisualEffects = visualEffects;
		mPriorityMessages = priorityMessages;
		mPriorityCalls = priorityCalls;
		mConversationSenders = conversationSenders;
		mAllowChannels = allowChannels;
	}

	public static ZenPolicy getBasePolicyInterruptionFilterAlarms() {
		return new ZenPolicy.Builder().disallowAllSounds().allowAlarms(true).allowMedia(true)
		        .allowPriorityChannels(false).build();
	}

	public static ZenPolicy getBasePolicyInterruptionFilterNone() {
		return new ZenPolicy.Builder().disallowAllSounds().allowPriorityChannels(false).build();
	}

	public int getPriorityConversationSenders() {
		return mConversationSenders;
	}

	public int getPriorityMessageSenders() {
		return mPriorityMessages;
	}

	public int getPriorityCallSenders() {
		return mPriorityCalls;
	}

	public int getPriorityCategoryReminders() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_REMINDERS);
	}

	public int getPriorityCategoryEvents() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_EVENTS);
	}

	public int getPriorityCategoryMessages() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_MESSAGES);
	}

	public int getPriorityCategoryCalls() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_CALLS);
	}

	public int getPriorityCategoryRepeatCallers() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_REPEAT_CALLERS);
	}

	public int getPriorityCategoryAlarms() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_ALARMS);
	}

	public int getPriorityCategoryMedia() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_MEDIA);
	}

	public int getPriorityCategorySystem() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_SYSTEM);
	}

	public int getPriorityCategoryConversations() {
		return mPriorityCategories.get(PRIORITY_CATEGORY_CONVERSATIONS);
	}

	public int getVisualEffectFullScreenIntent() {
		return mVisualEffects.get(VISUAL_EFFECT_FULL_SCREEN_INTENT);
	}

	public int getVisualEffectLights() {
		return mVisualEffects.get(VISUAL_EFFECT_LIGHTS);
	}

	public int getVisualEffectPeek() {
		return mVisualEffects.get(VISUAL_EFFECT_PEEK);
	}

	public int getVisualEffectStatusBar() {
		return mVisualEffects.get(VISUAL_EFFECT_STATUS_BAR);
	}

	public int getVisualEffectBadge() {
		return mVisualEffects.get(VISUAL_EFFECT_BADGE);
	}

	public int getVisualEffectAmbient() {
		return mVisualEffects.get(VISUAL_EFFECT_AMBIENT);
	}

	public int getVisualEffectNotificationList() {
		return mVisualEffects.get(VISUAL_EFFECT_NOTIFICATION_LIST);
	}

	public int getAllowedChannels() {
		return mAllowChannels;
	}

	public int getPriorityChannelsAllowed() {
		switch (mAllowChannels) {
		case CHANNEL_POLICY_PRIORITY:
			return STATE_ALLOW;
		case CHANNEL_POLICY_NONE:
			return STATE_DISALLOW;
		default:
			return STATE_UNSET;
		}
	}

	public boolean shouldHideAllVisualEffects() {
		for (int i = 0; i < mVisualEffects.size(); i++) {
			if (mVisualEffects.get(i) != STATE_DISALLOW) {
				return false;
			}
		}
		return true;
	}

	public boolean shouldShowAllVisualEffects() {
		for (int i = 0; i < mVisualEffects.size(); i++) {
			if (mVisualEffects.get(i) != STATE_ALLOW) {
				return false;
			}
		}
		return true;
	}

	/* AOSP ZenPolicy.copy: through a parcel */
	public ZenPolicy copy() {
		final Parcel parcel = Parcel.obtain();
		try {
			writeToParcel(parcel, 0);
			parcel.setDataPosition(0);
			return CREATOR.createFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}

	public static final class Builder {
		private ZenPolicy mZenPolicy;

		public Builder() {
			mZenPolicy = new ZenPolicy();
		}

		public Builder(ZenPolicy policy) {
			if (policy != null) {
				mZenPolicy = policy.copy();
			} else {
				mZenPolicy = new ZenPolicy();
			}
		}

		public ZenPolicy build() {
			return new ZenPolicy(new ArrayList<>(mZenPolicy.mPriorityCategories),
			                     new ArrayList<>(mZenPolicy.mVisualEffects), mZenPolicy.mPriorityMessages,
			                     mZenPolicy.mPriorityCalls, mZenPolicy.mConversationSenders,
			                     mZenPolicy.mAllowChannels);
		}

		public Builder allowAllSounds() {
			for (int i = 0; i < mZenPolicy.mPriorityCategories.size(); i++) {
				mZenPolicy.mPriorityCategories.set(i, STATE_ALLOW);
			}
			mZenPolicy.mPriorityMessages = PEOPLE_TYPE_ANYONE;
			mZenPolicy.mPriorityCalls = PEOPLE_TYPE_ANYONE;
			mZenPolicy.mConversationSenders = CONVERSATION_SENDERS_ANYONE;
			return this;
		}

		public Builder disallowAllSounds() {
			for (int i = 0; i < mZenPolicy.mPriorityCategories.size(); i++) {
				mZenPolicy.mPriorityCategories.set(i, STATE_DISALLOW);
			}
			mZenPolicy.mPriorityMessages = PEOPLE_TYPE_NONE;
			mZenPolicy.mPriorityCalls = PEOPLE_TYPE_NONE;
			mZenPolicy.mConversationSenders = CONVERSATION_SENDERS_NONE;
			return this;
		}

		public Builder showAllVisualEffects() {
			for (int i = 0; i < mZenPolicy.mVisualEffects.size(); i++) {
				mZenPolicy.mVisualEffects.set(i, STATE_ALLOW);
			}
			return this;
		}

		public Builder hideAllVisualEffects() {
			for (int i = 0; i < mZenPolicy.mVisualEffects.size(); i++) {
				mZenPolicy.mVisualEffects.set(i, STATE_DISALLOW);
			}
			return this;
		}

		public Builder unsetPriorityCategory(int category) {
			mZenPolicy.mPriorityCategories.set(category, STATE_UNSET);
			if (category == PRIORITY_CATEGORY_MESSAGES) {
				mZenPolicy.mPriorityMessages = PEOPLE_TYPE_UNSET;
			} else if (category == PRIORITY_CATEGORY_CALLS) {
				mZenPolicy.mPriorityCalls = PEOPLE_TYPE_UNSET;
			} else if (category == PRIORITY_CATEGORY_CONVERSATIONS) {
				mZenPolicy.mConversationSenders = CONVERSATION_SENDERS_UNSET;
			}
			return this;
		}

		public Builder unsetVisualEffect(int effect) {
			mZenPolicy.mVisualEffects.set(effect, STATE_UNSET);
			return this;
		}

		public Builder allowReminders(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_REMINDERS, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowEvents(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_EVENTS, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowConversations(int audienceType) {
			if (audienceType == STATE_UNSET) {
				return unsetPriorityCategory(PRIORITY_CATEGORY_CONVERSATIONS);
			}
			if (audienceType == CONVERSATION_SENDERS_NONE) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_CONVERSATIONS, STATE_DISALLOW);
			} else if (audienceType == CONVERSATION_SENDERS_ANYONE || audienceType == CONVERSATION_SENDERS_IMPORTANT) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_CONVERSATIONS, STATE_ALLOW);
			} else {
				return this;
			}
			mZenPolicy.mConversationSenders = audienceType;
			return this;
		}

		public Builder allowMessages(int audienceType) {
			if (audienceType == STATE_UNSET) {
				return unsetPriorityCategory(PRIORITY_CATEGORY_MESSAGES);
			}
			if (audienceType == PEOPLE_TYPE_NONE) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_MESSAGES, STATE_DISALLOW);
			} else if (audienceType == PEOPLE_TYPE_ANYONE || audienceType == PEOPLE_TYPE_CONTACTS || audienceType == PEOPLE_TYPE_STARRED) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_MESSAGES, STATE_ALLOW);
			} else {
				return this;
			}
			mZenPolicy.mPriorityMessages = audienceType;
			return this;
		}

		public Builder allowCalls(int audienceType) {
			if (audienceType == STATE_UNSET) {
				return unsetPriorityCategory(PRIORITY_CATEGORY_CALLS);
			}
			if (audienceType == PEOPLE_TYPE_NONE) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_CALLS, STATE_DISALLOW);
			} else if (audienceType == PEOPLE_TYPE_ANYONE || audienceType == PEOPLE_TYPE_CONTACTS || audienceType == PEOPLE_TYPE_STARRED) {
				mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_CALLS, STATE_ALLOW);
			} else {
				return this;
			}
			mZenPolicy.mPriorityCalls = audienceType;
			return this;
		}

		public Builder allowRepeatCallers(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_REPEAT_CALLERS, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowAlarms(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_ALARMS, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowMedia(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_MEDIA, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowSystem(boolean allow) {
			mZenPolicy.mPriorityCategories.set(PRIORITY_CATEGORY_SYSTEM, allow ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder allowCategory(int category, boolean allow) {
			switch (category) {
			case PRIORITY_CATEGORY_REMINDERS:
				allowReminders(allow);
				break;
			case PRIORITY_CATEGORY_EVENTS:
				allowEvents(allow);
				break;
			case PRIORITY_CATEGORY_REPEAT_CALLERS:
				allowRepeatCallers(allow);
				break;
			case PRIORITY_CATEGORY_ALARMS:
				allowAlarms(allow);
				break;
			case PRIORITY_CATEGORY_MEDIA:
				allowMedia(allow);
				break;
			case PRIORITY_CATEGORY_SYSTEM:
				allowSystem(allow);
				break;
			}
			return this;
		}

		public Builder showFullScreenIntent(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_FULL_SCREEN_INTENT, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showLights(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_LIGHTS, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showPeeking(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_PEEK, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showStatusBarIcons(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_STATUS_BAR, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showBadges(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_BADGE, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showInAmbientDisplay(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_AMBIENT, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showInNotificationList(boolean show) {
			mZenPolicy.mVisualEffects.set(VISUAL_EFFECT_NOTIFICATION_LIST, show ? STATE_ALLOW : STATE_DISALLOW);
			return this;
		}

		public Builder showVisualEffect(int effect, boolean show) {
			switch (effect) {
			case VISUAL_EFFECT_FULL_SCREEN_INTENT:
				showFullScreenIntent(show);
				break;
			case VISUAL_EFFECT_LIGHTS:
				showLights(show);
				break;
			case VISUAL_EFFECT_PEEK:
				showPeeking(show);
				break;
			case VISUAL_EFFECT_STATUS_BAR:
				showStatusBarIcons(show);
				break;
			case VISUAL_EFFECT_BADGE:
				showBadges(show);
				break;
			case VISUAL_EFFECT_AMBIENT:
				showInAmbientDisplay(show);
				break;
			case VISUAL_EFFECT_NOTIFICATION_LIST:
				showInNotificationList(show);
				break;
			}
			return this;
		}

		public Builder allowPriorityChannels(boolean allow) {
			mZenPolicy.mAllowChannels = allow ? CHANNEL_POLICY_PRIORITY : CHANNEL_POLICY_NONE;
			return this;
		}

		public Builder allowChannels(int channelType) {
			mZenPolicy.mAllowChannels = channelType;
			return this;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(mPriorityCategories);
		dest.writeList(mVisualEffects);
		dest.writeInt(mPriorityMessages);
		dest.writeInt(mPriorityCalls);
		dest.writeInt(mConversationSenders);
		dest.writeInt(mAllowChannels);
	}

	public static final Creator<ZenPolicy> CREATOR = new Creator<ZenPolicy>() {
		@Override
		public ZenPolicy createFromParcel(Parcel source) {
			return new ZenPolicy(
			    trimList(source.readArrayList(Integer.class.getClassLoader(), Integer.class), NUM_PRIORITY_CATEGORIES),
			    trimList(source.readArrayList(Integer.class.getClassLoader(), Integer.class), NUM_VISUAL_EFFECTS),
			    source.readInt(), source.readInt(), source.readInt(), source.readInt());
		}

		@Override
		public ZenPolicy[] newArray(int size) {
			return new ZenPolicy[size];
		}
	};

	@Override
	public String toString() {
		return new StringBuilder(ZenPolicy.class.getSimpleName())
		    .append('{')
		    .append("priorityCategories=[").append(priorityCategoriesToString())
		    .append("], visualEffects=[").append(visualEffectsToString())
		    .append("], priorityCallsSenders=").append(peopleTypeToString(mPriorityCalls))
		    .append(", priorityMessagesSenders=").append(peopleTypeToString(mPriorityMessages))
		    .append(", priorityConversationSenders=").append(conversationTypeToString(mConversationSenders))
		    .append(", allowChannels=").append(channelTypeToString(mAllowChannels))
		    .append('}')
		    .toString();
	}

	public static String fieldsToString(int bitmask) {
		ArrayList<String> modified = new ArrayList<>();
		if ((bitmask & FIELD_MESSAGES) != 0) {
			modified.add("FIELD_MESSAGES");
		}
		if ((bitmask & FIELD_CALLS) != 0) {
			modified.add("FIELD_CALLS");
		}
		if ((bitmask & FIELD_CONVERSATIONS) != 0) {
			modified.add("FIELD_CONVERSATIONS");
		}
		if ((bitmask & FIELD_ALLOW_CHANNELS) != 0) {
			modified.add("FIELD_ALLOW_CHANNELS");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_REMINDERS) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_REMINDERS");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_EVENTS) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_EVENTS");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_REPEAT_CALLERS) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_REPEAT_CALLERS");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_ALARMS) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_ALARMS");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_MEDIA) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_MEDIA");
		}
		if ((bitmask & FIELD_PRIORITY_CATEGORY_SYSTEM) != 0) {
			modified.add("FIELD_PRIORITY_CATEGORY_SYSTEM");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_FULL_SCREEN_INTENT) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_FULL_SCREEN_INTENT");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_LIGHTS) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_LIGHTS");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_PEEK) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_PEEK");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_STATUS_BAR) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_STATUS_BAR");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_BADGE) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_BADGE");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_AMBIENT) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_AMBIENT");
		}
		if ((bitmask & FIELD_VISUAL_EFFECT_NOTIFICATION_LIST) != 0) {
			modified.add("FIELD_VISUAL_EFFECT_NOTIFICATION_LIST");
		}
		return "{" + String.join(",", modified) + "}";
	}

	private static ArrayList<Integer> trimList(ArrayList<Integer> list, int maxLength) {
		if (list == null || list.size() <= maxLength) {
			return list;
		}
		return new ArrayList<>(list.subList(0, maxLength));
	}

	private String priorityCategoriesToString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < mPriorityCategories.size(); i++) {
			if (mPriorityCategories.get(i) != STATE_UNSET) {
				builder.append(indexToCategory(i)).append("=").append(stateToString(mPriorityCategories.get(i))).append(" ");
			}
		}
		return builder.toString();
	}

	private String visualEffectsToString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < mVisualEffects.size(); i++) {
			if (mVisualEffects.get(i) != STATE_UNSET) {
				builder.append(indexToVisualEffect(i)).append("=").append(stateToString(mVisualEffects.get(i))).append(" ");
			}
		}
		return builder.toString();
	}

	private String indexToVisualEffect(int visualEffectIndex) {
		switch (visualEffectIndex) {
		case VISUAL_EFFECT_FULL_SCREEN_INTENT:
			return "fullScreenIntent";
		case VISUAL_EFFECT_LIGHTS:
			return "lights";
		case VISUAL_EFFECT_PEEK:
			return "peek";
		case VISUAL_EFFECT_STATUS_BAR:
			return "statusBar";
		case VISUAL_EFFECT_BADGE:
			return "badge";
		case VISUAL_EFFECT_AMBIENT:
			return "ambient";
		case VISUAL_EFFECT_NOTIFICATION_LIST:
			return "notificationList";
		}
		return null;
	}

	private String indexToCategory(int categoryIndex) {
		switch (categoryIndex) {
		case PRIORITY_CATEGORY_REMINDERS:
			return "reminders";
		case PRIORITY_CATEGORY_EVENTS:
			return "events";
		case PRIORITY_CATEGORY_MESSAGES:
			return "messages";
		case PRIORITY_CATEGORY_CALLS:
			return "calls";
		case PRIORITY_CATEGORY_REPEAT_CALLERS:
			return "repeatCallers";
		case PRIORITY_CATEGORY_ALARMS:
			return "alarms";
		case PRIORITY_CATEGORY_MEDIA:
			return "media";
		case PRIORITY_CATEGORY_SYSTEM:
			return "system";
		case PRIORITY_CATEGORY_CONVERSATIONS:
			return "convs";
		}
		return null;
	}

	private String stateToString(int state) {
		switch (state) {
		case STATE_UNSET:
			return "unset";
		case STATE_DISALLOW:
			return "disallow";
		case STATE_ALLOW:
			return "allow";
		}
		return "invalidState{" + state + "}";
	}

	public static String peopleTypeToString(int peopleType) {
		switch (peopleType) {
		case PEOPLE_TYPE_ANYONE:
			return "anyone";
		case PEOPLE_TYPE_CONTACTS:
			return "contacts";
		case PEOPLE_TYPE_NONE:
			return "none";
		case PEOPLE_TYPE_STARRED:
			return "starred_contacts";
		case STATE_UNSET:
			return "unset";
		}
		return "invalidPeopleType{" + peopleType + "}";
	}

	public static String conversationTypeToString(int conversationType) {
		switch (conversationType) {
		case CONVERSATION_SENDERS_ANYONE:
			return "anyone";
		case CONVERSATION_SENDERS_IMPORTANT:
			return "important";
		case CONVERSATION_SENDERS_NONE:
			return "none";
		case CONVERSATION_SENDERS_UNSET:
			return "unset";
		}
		return "invalidConversationType{" + conversationType + "}";
	}

	public static String channelTypeToString(int channelType) {
		switch (channelType) {
		case CHANNEL_POLICY_UNSET:
			return "unset";
		case CHANNEL_POLICY_PRIORITY:
			return "priority";
		case CHANNEL_POLICY_NONE:
			return "none";
		}
		return "invalidChannelType{" + channelType + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ZenPolicy)) return false;
		if (o == this) return true;
		final ZenPolicy other = (ZenPolicy)o;
		return Objects.equals(other.mPriorityCategories, mPriorityCategories)
		       && Objects.equals(other.mVisualEffects, mVisualEffects) && other.mPriorityCalls == mPriorityCalls
		       && other.mPriorityMessages == mPriorityMessages && other.mConversationSenders == mConversationSenders
		       && other.mAllowChannels == mAllowChannels;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mPriorityCategories, mVisualEffects, mPriorityCalls, mPriorityMessages, mConversationSenders,
		                    mAllowChannels);
	}
}
