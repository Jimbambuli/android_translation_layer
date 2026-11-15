package android.media;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;

/* AOSP media/java/android/media/AudioPlaybackConfiguration.java. ATL has no AudioService, so no instance is ever
 * handed out (AudioManager.getActivePlaybackConfigurations() returns an empty list and AudioPlaybackCallback never
 * fires); the type carries the same fields and parcel layout as AOSP. The IPlayer binder of the parcel (AudioService
 * only, a player proxy) is written as null and ignored on reading, as AOSP does for a configuration without a
 * player shell. */
public final class AudioPlaybackConfiguration implements Parcelable {
	private static final int[] PLAYER_DEVICEIDS_INVALID = new int[0];
	private static final int PLAYER_STATE_IDLE = 1;

	private final int mPlayerIId;
	private int mPlayerType;
	private int mClientUid;
	private int mClientPid;
	private int mPlayerState;
	private AudioAttributes mPlayerAttr;

	private final Object mUpdateablePropLock = new Object();
	private int[] mDeviceIds = PLAYER_DEVICEIDS_INVALID;
	private int mSessionId;
	private FormatInfo mFormatInfo;
	private int mMutedState;

	AudioPlaybackConfiguration(int piid, int playerType, int uid, int pid, AudioAttributes attributes, int sessionId) {
		mPlayerIId = piid;
		mPlayerType = playerType;
		mClientUid = uid;
		mClientPid = pid;
		mMutedState = 0;
		mDeviceIds = PLAYER_DEVICEIDS_INVALID;
		mPlayerState = PLAYER_STATE_IDLE;
		mPlayerAttr = attributes;
		mSessionId = sessionId;
		mFormatInfo = FormatInfo.DEFAULT;
	}

	/* AOSP getAudioAttributes(): returns mPlayerAttr */
	public AudioAttributes getAudioAttributes() {
		return mPlayerAttr;
	}

	/* AOSP getAudioDeviceInfo() (deprecated, "never populated"): null without device ids, else the output device
	 * with that port id (AudioManager.getDeviceForPortId(id, GET_DEVICES_OUTPUTS)), null when it is gone */
	@Deprecated
	@Nullable
	public AudioDeviceInfo getAudioDeviceInfo() {
		final int[] deviceIds;
		synchronized (mUpdateablePropLock) {
			deviceIds = mDeviceIds;
		}
		if (deviceIds.length == 0) {
			return null;
		}
		for (AudioDeviceInfo device : new AudioManager().getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
			if (device.getId() == deviceIds[0]) {
				return device;
			}
		}
		return null;
	}

	public static final @NonNull Parcelable.Creator<AudioPlaybackConfiguration> CREATOR = new Parcelable.Creator<AudioPlaybackConfiguration>() {
		public AudioPlaybackConfiguration createFromParcel(Parcel p) {
			return new AudioPlaybackConfiguration(p);
		}

		public AudioPlaybackConfiguration[] newArray(int size) {
			return new AudioPlaybackConfiguration[size];
		}
	};

	@Override
	public int hashCode() {
		synchronized (mUpdateablePropLock) {
			return Objects.hash(mPlayerIId, Arrays.toString(mDeviceIds), mMutedState, mPlayerType, mClientUid, mClientPid, mSessionId);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		synchronized (mUpdateablePropLock) {
			dest.writeInt(mPlayerIId);
			dest.writeIntArray(mDeviceIds);
			dest.writeInt(mMutedState);
			dest.writeInt(mPlayerType);
			dest.writeInt(mClientUid);
			dest.writeInt(mClientPid);
			dest.writeInt(mPlayerState);
			mPlayerAttr.writeToParcel(dest, 0);
			dest.writeStrongInterface(null);
			dest.writeInt(mSessionId);
			mFormatInfo.writeToParcel(dest, 0);
		}
	}

	private AudioPlaybackConfiguration(Parcel in) {
		mPlayerIId = in.readInt();
		mDeviceIds = new int[in.readInt()];
		for (int i = 0; i < mDeviceIds.length; i++) {
			mDeviceIds[i] = in.readInt();
		}
		mMutedState = in.readInt();
		mPlayerType = in.readInt();
		mClientUid = in.readInt();
		mClientPid = in.readInt();
		mPlayerState = in.readInt();
		mPlayerAttr = AudioAttributes.CREATOR.createFromParcel(in);
		in.readStrongBinder();
		mSessionId = in.readInt();
		mFormatInfo = FormatInfo.CREATOR.createFromParcel(in);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof AudioPlaybackConfiguration))
			return false;

		AudioPlaybackConfiguration that = (AudioPlaybackConfiguration) o;

		return ((mPlayerIId == that.mPlayerIId) && Arrays.equals(mDeviceIds, that.mDeviceIds) && (mMutedState == that.mMutedState) &&
		        (mPlayerType == that.mPlayerType) && (mClientUid == that.mClientUid) && (mClientPid == that.mClientPid)) &&
		       (mSessionId == that.mSessionId);
	}

	/* AOSP AudioPlaybackConfiguration.FormatInfo (@hide): sample rate, channel mask and spatialization status */
	private static final class FormatInfo implements Parcelable {
		static final FormatInfo DEFAULT = new FormatInfo(false, 0, 0);
		final boolean mIsSpatialized;
		final int mNativeChannelMask;
		final int mSampleRate;

		FormatInfo(boolean isSpatialized, int nativeChannelMask, int sampleRate) {
			mIsSpatialized = isSpatialized;
			mNativeChannelMask = nativeChannelMask;
			mSampleRate = sampleRate;
		}

		@Override
		public String toString() {
			return "FormatInfo{" + "isSpatialized=" + mIsSpatialized + ", channelMask=0x" + Integer.toHexString(mNativeChannelMask) + ", sampleRate=" + mSampleRate + '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof FormatInfo))
				return false;
			FormatInfo that = (FormatInfo) o;
			return mIsSpatialized == that.mIsSpatialized && mNativeChannelMask == that.mNativeChannelMask && mSampleRate == that.mSampleRate;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mIsSpatialized, mNativeChannelMask, mSampleRate);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			dest.writeBoolean(mIsSpatialized);
			dest.writeInt(mNativeChannelMask);
			dest.writeInt(mSampleRate);
		}

		private FormatInfo(Parcel in) {
			this(in.readBoolean(), in.readInt(), in.readInt());
		}

		static final @NonNull Parcelable.Creator<FormatInfo> CREATOR = new Parcelable.Creator<FormatInfo>() {
			public FormatInfo createFromParcel(Parcel p) {
				return new FormatInfo(p);
			}

			public FormatInfo[] newArray(int size) {
				return new FormatInfo[size];
			}
		};
	}
}
