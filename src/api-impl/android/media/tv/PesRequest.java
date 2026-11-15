package android.media.tv;

/* AOSP media/java/android/media/tv/PesRequest.java */
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request for PES from broadcast signal.
 */
public final class PesRequest extends BroadcastInfoRequest implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_PES;

    public static final @NonNull Parcelable.Creator<PesRequest> CREATOR =
            new Parcelable.Creator<PesRequest>() {
                @Override
                public PesRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public PesRequest[] newArray(int size) {
                    return new PesRequest[size];
                }
            };

    private final int mTsPid;
    private final int mStreamId;

    static PesRequest createFromParcelBody(Parcel in) {
        return new PesRequest(in);
    }

    public PesRequest(int requestId, @RequestOption int option, int tsPid, int streamId) {
        super(REQUEST_TYPE, requestId, option);
        mTsPid = tsPid;
        mStreamId = streamId;
    }

    PesRequest(Parcel source) {
        super(REQUEST_TYPE, source);
        mTsPid = source.readInt();
        mStreamId = source.readInt();
    }

    /**
     * Gets the packet identifier (PID) of the TS (transport stream).
     */
    public int getTsPid() {
        return mTsPid;
    }

    /**
     * Gets the StreamID of requested PES.
     */
    public int getStreamId() {
        return mStreamId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mTsPid);
        dest.writeInt(mStreamId);
    }
}
