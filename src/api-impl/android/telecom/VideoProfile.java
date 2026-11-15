package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

/* android.telecom.VideoProfile: the video state (STATE_*) and quality (QUALITY_*) of a call. The telecomm sources are
 * not in the local AOSP copy; the class is written after the SDK documentation and the API list
 * (api/framework-current.txt), CameraCapabilities is not provided. */
public class VideoProfile implements Parcelable {
	public static final int QUALITY_UNKNOWN = 0;
	public static final int QUALITY_HIGH = 1;
	public static final int QUALITY_MEDIUM = 2;
	public static final int QUALITY_LOW = 3;
	public static final int QUALITY_DEFAULT = 4;

	public static final int STATE_AUDIO_ONLY = 0x0;
	public static final int STATE_TX_ENABLED = 0x1;
	public static final int STATE_RX_ENABLED = 0x2;
	public static final int STATE_BIDIRECTIONAL = STATE_TX_ENABLED | STATE_RX_ENABLED;
	public static final int STATE_PAUSED = 0x4;

	private final int mVideoState;
	private final int mQuality;

	public static final Parcelable.Creator<VideoProfile> CREATOR = new Parcelable.Creator<VideoProfile>() {
		@Override
		public VideoProfile createFromParcel(Parcel source) {
			int state = source.readInt();
			int quality = source.readInt();
			return new VideoProfile(state, quality);
		}

		@Override
		public VideoProfile[] newArray(int size) {
			return new VideoProfile[size];
		}
	};

	public VideoProfile(int videoState) {
		this(videoState, QUALITY_DEFAULT);
	}

	public VideoProfile(int videoState, int quality) {
		mVideoState = videoState;
		mQuality = quality;
	}

	public int getVideoState() {
		return mVideoState;
	}

	public int getQuality() {
		return mQuality;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mVideoState);
		dest.writeInt(mQuality);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[VideoProfile videoState = ");
		sb.append(videoStateToString(mVideoState));
		sb.append(" videoQuality = ");
		sb.append(mQuality);
		sb.append("]");
		return sb.toString();
	}

	public static String videoStateToString(int videoState) {
		StringBuilder sb = new StringBuilder();
		sb.append("Audio");
		if (videoState == STATE_AUDIO_ONLY) {
			sb.append(" Only");
		} else {
			if (isTransmissionEnabled(videoState)) {
				sb.append(" Tx");
			}
			if (isReceptionEnabled(videoState)) {
				sb.append(" Rx");
			}
			if (isPaused(videoState)) {
				sb.append(" Pause");
			}
		}
		return sb.toString();
	}

	public static boolean isAudioOnly(int videoState) {
		return !hasState(videoState, STATE_TX_ENABLED) && !hasState(videoState, STATE_RX_ENABLED);
	}

	public static boolean isVideo(int videoState) {
		return hasState(videoState, STATE_TX_ENABLED) || hasState(videoState, STATE_RX_ENABLED)
		    || hasState(videoState, STATE_BIDIRECTIONAL);
	}

	public static boolean isTransmissionEnabled(int videoState) {
		return hasState(videoState, STATE_TX_ENABLED);
	}

	public static boolean isReceptionEnabled(int videoState) {
		return hasState(videoState, STATE_RX_ENABLED);
	}

	public static boolean isBidirectional(int videoState) {
		return hasState(videoState, STATE_BIDIRECTIONAL);
	}

	public static boolean isPaused(int videoState) {
		return hasState(videoState, STATE_PAUSED);
	}

	private static boolean hasState(int videoState, int state) {
		return (videoState & state) == state;
	}
}
