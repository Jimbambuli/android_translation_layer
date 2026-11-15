/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.app;

import android.media.AudioAttributes;
import android.net.Uri;
import android.provider.Settings;
import java.util.Arrays;
import java.util.Objects;

/**
 * A representation of settings that apply to a collection of similarly themed notifications.
 *
 * The public API of AOSP (android-14) without Parcel/XML: ATL keeps the channels of the app in
 * NotificationManager and shows notifications on the desktop the same way for every channel.
 */
public final class NotificationChannel {
	/**
	 * The id of the default channel for an app.
	 */
	public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

	/**
	 * The maximum length for text fields in a NotificationChannel. Fields will be truncated at this
	 * limit.
	 */
	public static final int MAX_TEXT_LENGTH = 1000;

	/**
	 * Default value for bubble preference.
	 */
	public static final int DEFAULT_ALLOW_BUBBLE = -1;
	/** Bubbles are allowed. */
	public static final int ALLOW_BUBBLE_ON = 1;
	/** Bubbles are not allowed. */
	public static final int ALLOW_BUBBLE_OFF = 0;

	private static final int DEFAULT_VISIBILITY = -1000;  // NotificationManager.VISIBILITY_NO_OVERRIDE
	private static final int DEFAULT_IMPORTANCE = -1000;  // NotificationManager.IMPORTANCE_UNSPECIFIED
	private static final boolean DEFAULT_SHOW_BADGE = true;

	private String mId;
	private String mName;
	private String mDesc;
	private int mImportance = DEFAULT_IMPORTANCE;
	private boolean mBypassDnd;
	private int mLockscreenVisibility = DEFAULT_VISIBILITY;
	private Uri mSound = Settings.System.DEFAULT_NOTIFICATION_URI;
	private boolean mLights;
	private int mLightColor = 0;
	private long[] mVibration;
	private boolean mVibrationEnabled;
	private android.os.VibrationEffect mVibrationEffect;
	private boolean mShowBadge = DEFAULT_SHOW_BADGE;
	private String mGroup;
	private AudioAttributes mAudioAttributes;
	private boolean mBlockableSystem = false;
	private int mAllowBubbles = DEFAULT_ALLOW_BUBBLE;
	private String mParentId = null;
	private String mConversationId = null;
	private boolean mImportantConvo = false;

	/**
	 * Creates a notification channel.
	 */
	public NotificationChannel(String id, CharSequence name, int importance) {
		this.mId = getTrimmedString(id);
		this.mName = name != null ? getTrimmedString(name.toString()) : null;
		this.mImportance = importance;
	}

	private String getTrimmedString(String input) {
		if (input != null && input.length() > MAX_TEXT_LENGTH) {
			return input.substring(0, MAX_TEXT_LENGTH);
		}
		return input;
	}

	/** @hide */
	public void setBlockable(boolean blockable) {
		mBlockableSystem = blockable;
	}

	/**
	 * Sets the user visible name of this channel.
	 */
	public void setName(CharSequence name) {
		mName = name != null ? getTrimmedString(name.toString()) : null;
	}

	/**
	 * Sets the user visible description of this channel.
	 */
	public void setDescription(String description) {
		mDesc = getTrimmedString(description);
	}

	/** @hide */
	public void setId(String id) {
		mId = id;
	}

	/**
	 * Sets what group this channel belongs to.
	 */
	public void setGroup(String groupId) {
		this.mGroup = groupId;
	}

	/**
	 * Sets whether notifications posted to this channel can appear as application icon badges
	 * in a Launcher.
	 */
	public void setShowBadge(boolean showBadge) {
		this.mShowBadge = showBadge;
	}

	/**
	 * Sets the sound that should be played for notifications posted to this channel and its
	 * audio attributes.
	 */
	public void setSound(Uri sound, AudioAttributes audioAttributes) {
		this.mSound = sound;
		this.mAudioAttributes = audioAttributes;
	}

	/**
	 * Sets whether notifications posted to this channel should display notification lights,
	 * on devices that support that feature.
	 */
	public void enableLights(boolean lights) {
		this.mLights = lights;
	}

	/**
	 * Sets the notification light color for notifications posted to this channel, if lights are
	 * {@link #enableLights(boolean) enabled} on this channel and the device supports that feature.
	 */
	public void setLightColor(int argb) {
		this.mLightColor = argb;
	}

	/**
	 * Sets whether notification posted to this channel should vibrate.
	 */
	/* AOSP NotificationChannel.hasUserSetSound(): (mUserLockedFields & USER_LOCKED_SOUND) != 0; the field is set by the
	 * notification service when the user changes the sound, ATL has no such settings UI */
	public boolean hasUserSetSound() {
		return false;
	}

	public void enableVibration(boolean vibration) {
		this.mVibrationEnabled = vibration;
	}

	/**
	 * Sets the vibration pattern for notifications posted to this channel. If the provided
	 * pattern is valid (non-null, non-empty), will enable vibration on this channel.
	 */
	public void setVibrationPattern(long[] vibrationPattern) {
		this.mVibrationEnabled = vibrationPattern != null && vibrationPattern.length > 0;
		this.mVibration = vibrationPattern;
		try {
			this.mVibrationEffect = android.os.VibrationEffect.createWaveform(vibrationPattern, -1);
		} catch (IllegalArgumentException | NullPointerException e) {
			this.mVibrationEffect = null;
		}
	}

	/* AOSP NotificationChannel.setVibrationEffect(): vibration is on exactly when an effect is given. ATL's
	 * VibrationEffect cannot be converted back to an off/on pattern (computeCreateWaveformOffOnTimingsOrNull), so
	 * the pattern is cleared, as AOSP does for an effect that is not a plain waveform. */
	public void setVibrationEffect(android.os.VibrationEffect effect) {
		this.mVibrationEnabled = effect != null;
		this.mVibrationEffect = effect;
		this.mVibration = null;
	}

	public android.os.VibrationEffect getVibrationEffect() {
		return mVibrationEffect;
	}

	/**
	 * Sets the level of interruption of this notification channel.
	 */
	public void setImportance(int importance) {
		this.mImportance = importance;
	}

	/**
	 * Sets whether or not notifications posted to this channel can interrupt the user in
	 * {@link android.app.NotificationManager.Policy#INTERRUPTION_FILTER_PRIORITY} mode.
	 */
	public void setBypassDnd(boolean bypassDnd) {
		this.mBypassDnd = bypassDnd;
	}

	/**
	 * Sets whether notifications posted to this channel appear on the lockscreen or not, and if so,
	 * whether they appear in a redacted form.
	 */
	public void setLockscreenVisibility(int lockscreenVisibility) {
		this.mLockscreenVisibility = lockscreenVisibility;
	}

	/**
	 * As of Android 11 this value is no longer respected.
	 */
	public void setAllowBubbles(boolean allowBubbles) {
		mAllowBubbles = allowBubbles ? ALLOW_BUBBLE_ON : ALLOW_BUBBLE_OFF;
	}

	/** @hide */
	public void setAllowBubbles(int allowed) {
		mAllowBubbles = allowed;
	}

	/**
	 * Sets this channel as being converastion-centric. Different settings and functionality may be
	 * exposed for conversation-centric channels.
	 */
	public void setConversationId(String parentChannelId, String conversationId) {
		mParentId = parentChannelId;
		mConversationId = conversationId;
	}

	/** @hide */
	public void setImportantConversation(boolean importantConvo) {
		mImportantConvo = importantConvo;
	}

	/**
	 * Returns the id of this channel.
	 */
	public String getId() {
		return mId;
	}

	/**
	 * Returns the user visible name of this channel.
	 */
	public CharSequence getName() {
		return mName;
	}

	/**
	 * Returns the user visible description of this channel.
	 */
	public String getDescription() {
		return mDesc;
	}

	/**
	 * Returns the user specified importance e.g. {@link NotificationManager#IMPORTANCE_LOW} for
	 * notifications posted to this channel.
	 */
	public int getImportance() {
		return mImportance;
	}

	/**
	 * Whether or not notifications posted to this channel can bypass the Do Not Disturb
	 * {@link NotificationManager#INTERRUPTION_FILTER_PRIORITY} mode.
	 */
	public boolean canBypassDnd() {
		return mBypassDnd;
	}

	/**
	 * Whether or not this channel represents a conversation.
	 */
	public boolean isConversation() {
		return mParentId != null && mConversationId != null;
	}

	/**
	 * Whether or not notifications in this conversation are considered important.
	 */
	public boolean isImportantConversation() {
		return mImportantConvo;
	}

	/**
	 * Returns the notification sound for this channel.
	 */
	public Uri getSound() {
		return mSound;
	}

	/**
	 * Returns the audio attributes for sound played by notifications posted to this channel.
	 */
	public AudioAttributes getAudioAttributes() {
		return mAudioAttributes;
	}

	/**
	 * Returns whether notifications posted to this channel trigger notification lights.
	 */
	public boolean shouldShowLights() {
		return mLights;
	}

	/**
	 * Returns the notification light color for notifications posted to this channel. Irrelevant
	 * unless {@link #shouldShowLights()}.
	 */
	public int getLightColor() {
		return mLightColor;
	}

	/**
	 * Returns whether notifications posted to this channel always vibrate.
	 */
	public boolean shouldVibrate() {
		return mVibrationEnabled;
	}

	/**
	 * Returns the vibration pattern for notifications posted to this channel. Will be ignored if
	 * vibration is not enabled ({@link #shouldVibrate()}.
	 */
	public long[] getVibrationPattern() {
		return mVibration;
	}

	/**
	 * Returns whether or not notifications posted to this channel are shown on the lockscreen in
	 * full or redacted form.
	 */
	public int getLockscreenVisibility() {
		return mLockscreenVisibility;
	}

	/**
	 * Returns whether notifications posted to this channel can appear as badges in a Launcher
	 * application.
	 */
	public boolean canShowBadge() {
		return mShowBadge;
	}

	/**
	 * Returns what group this channel belongs to.
	 */
	public String getGroup() {
		return mGroup;
	}

	/**
	 * Returns whether notifications posted to this channel are allowed to display outside of the
	 * notification shade, in a floating window on top of other apps.
	 */
	public boolean canBubble() {
		return mAllowBubbles == ALLOW_BUBBLE_ON;
	}

	/** @hide */
	public int getAllowBubbles() {
		return mAllowBubbles;
	}

	/**
	 * Returns the id of the parent notification channel to this channel, if it's a conversation
	 * related channel.
	 */
	public String getParentChannelId() {
		return mParentId;
	}

	/**
	 * Returns the {@link ShortcutInfo#getId() id} of the conversation backing this channel, if it's
	 * associated with a conversation.
	 */
	public String getConversationId() {
		return mConversationId;
	}

	/** @hide */
	public boolean isBlockable() {
		return mBlockableSystem;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		NotificationChannel that = (NotificationChannel)o;
		return getImportance() == that.getImportance() && mBypassDnd == that.mBypassDnd &&
		    getLockscreenVisibility() == that.getLockscreenVisibility() && mLights == that.mLights &&
		    getLightColor() == that.getLightColor() && mVibrationEnabled == that.mVibrationEnabled &&
		    mShowBadge == that.mShowBadge && mAllowBubbles == that.mAllowBubbles &&
		    Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) &&
		    Objects.equals(mDesc, that.mDesc) && Objects.equals(getSound(), that.getSound()) &&
		    Arrays.equals(mVibration, that.mVibration) && Objects.equals(getGroup(), that.getGroup()) &&
		    Objects.equals(getAudioAttributes(), that.getAudioAttributes()) &&
		    Objects.equals(mParentId, that.mParentId) && Objects.equals(mConversationId, that.mConversationId);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(getId(), getName(), mDesc, getImportance(), mBypassDnd,
		                          getLockscreenVisibility(), getSound(), mLights, getLightColor(),
		                          mVibrationEnabled, mShowBadge, getGroup(), getAudioAttributes(),
		                          mAllowBubbles, mParentId, mConversationId);
		result = 31 * result + Arrays.hashCode(mVibration);
		return result;
	}

	@Override
	public String toString() {
		return "NotificationChannel{mId='" + mId + "', mName=" + mName + ", mImportance=" + mImportance + "}";
	}
}
