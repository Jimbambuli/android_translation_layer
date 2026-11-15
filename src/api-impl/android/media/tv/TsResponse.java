package android.media.tv;

/* AOSP media/java/android/media/tv/TsResponse.java */
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A response for TS (transport stream) from broadcast signal.
 */
public final class TsResponse extends BroadcastInfoResponse implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int RESPONSE_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_TS;

    public static final @NonNull Parcelable.Creator<TsResponse> CREATOR =
            new Parcelable.Creator<TsResponse>() {
                @Override
                public TsResponse createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public TsResponse[] newArray(int size) {
                    return new TsResponse[size];
                }
            };

    private final String mSharedFilterToken;

    static TsResponse createFromParcelBody(@NonNull Parcel in) {
        return new TsResponse(in);
    }

    public TsResponse(int requestId, int sequence, @ResponseResult int responseResult,
            @Nullable String sharedFilterToken) {
        super(RESPONSE_TYPE, requestId, sequence, responseResult);
        this.mSharedFilterToken = sharedFilterToken;
    }

    TsResponse(Parcel source) {
        super(RESPONSE_TYPE, source);
        mSharedFilterToken = source.readString();
    }

    /**
     * Gets a token of SharedFilter.
     *
     * <p>The token can be used to retrieve the transport stream from the filter.
     */
    @Nullable
    public String getSharedFilterToken() {
        return mSharedFilterToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mSharedFilterToken);
    }
}
