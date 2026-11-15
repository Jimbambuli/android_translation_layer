package android.media;

/* AOSP media/java/android/media/MediaSyncEvent.java */
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class MediaSyncEvent implements Parcelable {

	public static final int SYNC_EVENT_NONE = 0;

	public static final int SYNC_EVENT_PRESENTATION_COMPLETE = 1;

	@SystemApi
	public static final int SYNC_EVENT_SHARE_AUDIO_HISTORY = 100;

	public static MediaSyncEvent createEvent(int eventType)
							throws IllegalArgumentException {
		if (!isValidType(eventType)) {
			throw (new IllegalArgumentException(eventType
					+ "is not a valid MediaSyncEvent type."));
		} else {
			return new MediaSyncEvent(eventType);
		}
	}

	private final int mType;
	private int mAudioSession = 0;

	private MediaSyncEvent(int eventType) {
		mType = eventType;
	}

	public MediaSyncEvent setAudioSessionId(int audioSessionId)
			throws IllegalArgumentException {
		if (audioSessionId > 0) {
			mAudioSession = audioSessionId;
		} else {
			throw (new IllegalArgumentException(audioSessionId + " is not a valid session ID."));
		}
		return this;
	}

	public int getType() {
		return mType;
	}

	public int getAudioSessionId() {
		return mAudioSession;
	}

	private static boolean isValidType(int type) {
		switch (type) {
			case SYNC_EVENT_NONE:
			case SYNC_EVENT_PRESENTATION_COMPLETE:
			case SYNC_EVENT_SHARE_AUDIO_HISTORY:
				return true;
			default:
				return false;
		}
	}

	// Parcelable implementation
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		Objects.requireNonNull(dest);
		dest.writeInt(mType);
		dest.writeInt(mAudioSession);
	}

	private MediaSyncEvent(Parcel in) {
		mType = in.readInt();
		mAudioSession = in.readInt();
	}

	public static final @NonNull Parcelable.Creator<MediaSyncEvent> CREATOR =
			new Parcelable.Creator<MediaSyncEvent>() {

		public MediaSyncEvent createFromParcel(Parcel p) {
			return new MediaSyncEvent(p);
		}
		public MediaSyncEvent[] newArray(int size) {
			return new MediaSyncEvent[size];
		}
	};

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MediaSyncEvent that = (MediaSyncEvent) o;
		return ((mType == that.mType)
				&& (mAudioSession == that.mAudioSession));
	}

	@Override
	public int hashCode() {
		return Objects.hash(mType, mAudioSession);
	}

	@Override
	public String toString() {
		return new String("MediaSyncEvent:"
				+ " type=" + typeToString(mType)
				+ " session=" + mAudioSession);
	}

	public static @NonNull String typeToString(int type) {
		switch (type) {
			case SYNC_EVENT_NONE:
				return "SYNC_EVENT_NONE";
			case SYNC_EVENT_PRESENTATION_COMPLETE:
				return "SYNC_EVENT_PRESENTATION_COMPLETE";
			case SYNC_EVENT_SHARE_AUDIO_HISTORY:
				return "SYNC_EVENT_SHARE_AUDIO_HISTORY";
			default:
				return "unknown event type " + type;
		}
	}

}
