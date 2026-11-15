package android.media.tv;

/* AOSP media/java/android/media/tv/StreamEventRequest.java */
import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request for Stream Event from broadcast signal.
 */
public final class StreamEventRequest extends BroadcastInfoRequest implements Parcelable {
    private static final int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_STREAM_EVENT;

    public static final @NonNull Parcelable.Creator<StreamEventRequest> CREATOR =
            new Parcelable.Creator<StreamEventRequest>() {
                @Override
                public StreamEventRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public StreamEventRequest[] newArray(int size) {
                    return new StreamEventRequest[size];
                }
            };

    private final Uri mTargetUri;
    private final String mEventName;

    static StreamEventRequest createFromParcelBody(Parcel in) {
        return new StreamEventRequest(in);
    }

    public StreamEventRequest(int requestId, int option, @NonNull Uri targetUri,
            @NonNull String eventName) {
        super(REQUEST_TYPE, requestId, option);
        this.mTargetUri = targetUri;
        this.mEventName = eventName;
    }

    StreamEventRequest(Parcel source) {
        super(REQUEST_TYPE, source);
        String uriString = source.readString();
        mTargetUri = uriString == null ? null : Uri.parse(uriString);
        mEventName = source.readString();
    }

    /**
     * Gets the URI for the DSM-CC Object or the event description file describing the event.
     */
    @NonNull
    public Uri getTargetUri() {
        return mTargetUri;
    }

    /**
     * Gets the name of the event.
     */
    @NonNull
    public String getEventName() {
        return mEventName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        String uriString = mTargetUri == null ? null : mTargetUri.toString();
        dest.writeString(uriString);
        dest.writeString(mEventName);
    }
}
