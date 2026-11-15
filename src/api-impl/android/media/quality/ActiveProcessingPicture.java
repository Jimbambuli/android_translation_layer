package android.media.quality;

/* AOSP android/media/quality/ActiveProcessingPicture.java */
import android.os.Parcel;
import android.os.Parcelable;

import android.annotation.NonNull;

/**
 * Active picture represents an image or video undergoing picture processing which uses a picture
 * profile. The picture profile is used to configure the picture processing parameters.
 */
public final class ActiveProcessingPicture implements Parcelable {
    private final int mId;
    private final String mProfileId;
    private final boolean mForGlobal;

    public ActiveProcessingPicture(int id, @NonNull String profileId) {
        mId = id;
        mProfileId = profileId;
        mForGlobal = true;
    }

    
    
    @NonNull
    public static final Creator<ActiveProcessingPicture> CREATOR = new Creator<ActiveProcessingPicture>() {
        @Override
        public ActiveProcessingPicture createFromParcel(Parcel in) {
            return new ActiveProcessingPicture(in);
        }

        @Override
        public ActiveProcessingPicture[] newArray(int size) {
            return new ActiveProcessingPicture[size];
        }
    };

    /**
     * An ID that uniquely identifies the active content.
     *
     * <p>The ID is assigned by the system to distinguish different active contents.
     */
    public int getId() {
        return mId;
    }

    /**
     * The ID of the picture profile used to configure the content.
     */
    @NonNull
    public String getProfileId() {
        return mProfileId;
    }

    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mProfileId);
        dest.writeBoolean(mForGlobal);
    }

    /* AOSP android/media/quality/ActiveProcessingPicture.java: ActiveProcessingPicture(Parcel in) */
    ActiveProcessingPicture(Parcel in) {
        mId = in.readInt();
        mProfileId = in.readString();
        mForGlobal = in.readBoolean();
    }
}
