package android.media.tv;

/* AOSP media/java/android/media/tv/TimelineRequest.java */
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request for Timeline from broadcast signal.
 */
public final class TimelineRequest extends BroadcastInfoRequest implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_TIMELINE;

    public static final @NonNull Parcelable.Creator<TimelineRequest> CREATOR =
            new Parcelable.Creator<TimelineRequest>() {
                @Override
                public TimelineRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public TimelineRequest[] newArray(int size) {
                    return new TimelineRequest[size];
                }
            };

    private final int mIntervalMillis;
    private final String mSelector;

    static TimelineRequest createFromParcelBody(Parcel in) {
        return new TimelineRequest(in);
    }

    public TimelineRequest(int requestId, @RequestOption int option, int intervalMillis) {
        super(REQUEST_TYPE, requestId, option);
        mIntervalMillis = intervalMillis;
        mSelector = null;
    }

    public TimelineRequest(int requestId, @RequestOption int option, int intervalMillis,
            @NonNull String selector) {
        super(REQUEST_TYPE, requestId, option);
        mIntervalMillis = intervalMillis;
        mSelector = selector;
    }

    TimelineRequest(Parcel source) {
        super(REQUEST_TYPE, source);
        mIntervalMillis = source.readInt();
        mSelector = source.readString();
    }

    /**
     * Gets the interval of TIS sending response to TIAS in millisecond.
     */
    public int getIntervalMillis() {
        return mIntervalMillis;
    }

    /**
     * Gets the timeline selector.
     * <p>The selector describes the type and location of timeline signalling. For example
     * {@code urn:dvb:css:timeline:pts} is a selector in DVB standard.
     *
     * @return the selector if it's set; {@code null} otherwise.
     */
    @Nullable
    public String getSelector() {
        return mSelector;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mIntervalMillis);
        dest.writeString(mSelector);
    }
}
