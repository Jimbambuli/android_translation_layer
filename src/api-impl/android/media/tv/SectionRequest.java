package android.media.tv;

/* AOSP media/java/android/media/tv/SectionRequest.java */
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request for Section from broadcast signal.
 */
public final class SectionRequest extends BroadcastInfoRequest implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_SECTION;

    public static final @NonNull Parcelable.Creator<SectionRequest> CREATOR =
            new Parcelable.Creator<SectionRequest>() {
                @Override
                public SectionRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public SectionRequest[] newArray(int size) {
                    return new SectionRequest[size];
                }
            };

    private final int mTsPid;
    private final int mTableId;
    private final int mVersion;

    static SectionRequest createFromParcelBody(Parcel in) {
        return new SectionRequest(in);
    }

    public SectionRequest(int requestId, @RequestOption int option, int tsPid, int tableId,
            int version) {
        super(REQUEST_TYPE, requestId, option);
        mTsPid = tsPid;
        mTableId = tableId;
        mVersion = version;
    }

    SectionRequest(Parcel source) {
        super(REQUEST_TYPE, source);
        mTsPid = source.readInt();
        mTableId = source.readInt();
        mVersion = source.readInt();
    }

    /**
     * Gets the packet identifier (PID) of the TS (transport stream).
     */
    public int getTsPid() {
        return mTsPid;
    }

    /**
     * Gets the ID of the requested table.
     */
    public int getTableId() {
        return mTableId;
    }

    /**
     * Gets the version number of requested session. If it is null, value will be -1.
     * <p>The consistency of version numbers between request and response depends on
     * {@link BroadcastInfoRequest#getOption()}. If the request has RequestOption value
     * REQUEST_OPTION_AUTO_UPDATE, then the response may be set to the latest version which may be
     * different from the version of the request. Otherwise, response with a different version from
     * its request will be considered invalid.
     */
    public int getVersion() {
        return mVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mTsPid);
        dest.writeInt(mTableId);
        dest.writeInt(mVersion);
    }
}
