package android.media.tv;

/* AOSP media/java/android/media/tv/TableRequest.java */
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A request for Table from broadcast signal.
 */
public final class TableRequest extends BroadcastInfoRequest implements Parcelable {
    private static final @TvInputManager.BroadcastInfoType int REQUEST_TYPE =
            TvInputManager.BROADCAST_INFO_TYPE_TABLE;

    
    /** Program Association Table */
    public static final int TABLE_NAME_PAT = 0;
    /** Program Mapping Table */
    public static final int TABLE_NAME_PMT = 1;
    /**
     * Conditional Access Table
     */
    public static final int TABLE_NAME_CAT = 2;
    /**
     * Network Information Table
     */
    public static final int TABLE_NAME_NIT = 3;
    /**
     * Bouquet Association Table
     */
    public static final int TABLE_NAME_BAT = 4;
    /**
     * Service Description Table
     */
    public static final int TABLE_NAME_SDT = 5;
    /**
     * Event Information Table
     */
    public static final int TABLE_NAME_EIT = 6;
    /**
     * Time and Date Table
     */
    public static final int TABLE_NAME_TDT = 7;
    /**
     * Time Offset Table
     */
    public static final int TABLE_NAME_TOT = 8;
    /**
     * Selection Information Table
     */
    public static final int TABLE_NAME_SIT = 9;

    public static final @NonNull Parcelable.Creator<TableRequest> CREATOR =
            new Parcelable.Creator<TableRequest>() {
                @Override
                public TableRequest createFromParcel(Parcel source) {
                    source.readInt();
                    return createFromParcelBody(source);
                }

                @Override
                public TableRequest[] newArray(int size) {
                    return new TableRequest[size];
                }
            };

    private final int mTableId;
    private final @TableName int mTableName;
    private final int mVersion;

    static TableRequest createFromParcelBody(Parcel in) {
        return new TableRequest(in);
    }

    public TableRequest(int requestId, @RequestOption int option, int tableId,
            @TableName int tableName, int version) {
        super(REQUEST_TYPE, requestId, option);
        mTableId = tableId;
        mTableName = tableName;
        mVersion = version;
    }

    TableRequest(Parcel source) {
        super(REQUEST_TYPE, source);
        mTableId = source.readInt();
        mTableName = source.readInt();
        mVersion = source.readInt();
    }

    /**
     * Gets the ID of requested table.
     */
    public int getTableId() {
        return mTableId;
    }

    /**
     * Gets the name of requested table.
     */
    public @TableName int getTableName() {
        return mTableName;
    }

    /**
     * Gets the version number of requested table. If it is null, value will be -1.
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
        dest.writeInt(mTableId);
        dest.writeInt(mTableName);
        dest.writeInt(mVersion);
    }
}
