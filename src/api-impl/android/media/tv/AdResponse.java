package android.media.tv;

/* AOSP media/java/android/media/tv/AdResponse.java */
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An advertisement response which can be sent to TV interactive App service to inform AD status.
 */
public final class AdResponse implements Parcelable {
    
    public static final int RESPONSE_TYPE_PLAYING = 1;
    public static final int RESPONSE_TYPE_FINISHED = 2;
    public static final int RESPONSE_TYPE_STOPPED = 3;
    public static final int RESPONSE_TYPE_ERROR = 4;
    public static final int RESPONSE_TYPE_BUFFERING = 5;

    public static final @NonNull Parcelable.Creator<AdResponse> CREATOR =
            new Parcelable.Creator<AdResponse>() {
                @Override
                public AdResponse createFromParcel(Parcel source) {
                    return new AdResponse(source);
                }

                @Override
                public AdResponse[] newArray(int size) {
                    return new AdResponse[size];
                }
            };

    private final int mId;
    private final @ResponseType int mResponseType;
    private final long mElapsedTime;

    public AdResponse(int id, @ResponseType int responseType, long elapsedTime) {
        mId = id;
        mResponseType = responseType;
        mElapsedTime = elapsedTime;
    }

    private AdResponse(Parcel source) {
        mId = source.readInt();
        mResponseType = source.readInt();
        mElapsedTime = source.readLong();
    }

    /**
     * Gets the ID of AD response.
     */
    public int getId() {
        return mId;
    }

    /**
     * Gets the response type.
     */
    @ResponseType
    public int getResponseType() {
        return mResponseType;
    }

    /**
     * Gets the playback elapsed time in milliseconds.
     *
     * @return The playback elapsed time. -1 if no valid elapsed time.
     */
    public long getElapsedTimeMillis() {
        return mElapsedTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeInt(mResponseType);
        dest.writeLong(mElapsedTime);
    }
}
