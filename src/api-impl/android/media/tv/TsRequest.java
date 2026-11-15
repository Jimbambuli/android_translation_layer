package android.media.tv;

/* AOSP media/java/android/media/tv/TsRequest.java */
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request for TS (transport stream) from broadcast signal.
 */
public final class TsRequest extends BroadcastInfoRequest implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_TS;

    public static final @NonNull Parcelable.Creator<TsRequest> CREATOR =
            new Parcelable.Creator<TsRequest>() {
                @Override
                public TsRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public TsRequest[] newArray(int size) {
                    return new TsRequest[size];
                }
            };

    private final int mTsPid;

    static @NonNull TsRequest createFromParcelBody(@NonNull Parcel in) {
        return new TsRequest(in);
    }

    public TsRequest(int requestId, @RequestOption int option, int tsPid) {
        super(REQUEST_TYPE, requestId, option);
        mTsPid = tsPid;
    }

    TsRequest(@NonNull Parcel source) {
        super(REQUEST_TYPE, source);
        mTsPid = source.readInt();
    }

    /**
     * Gets the packet identifier (PID) of the TS (transport stream).
     */
    public int getTsPid() {
        return mTsPid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mTsPid);
    }
}
