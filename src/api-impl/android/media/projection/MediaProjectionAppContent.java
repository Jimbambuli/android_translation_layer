package android.media.projection;

/* AOSP android/media/projection/MediaProjectionAppContent.java */
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import android.annotation.NonNull;

import java.util.Objects;

/**
 * Holds information about content an app can share via the MediaProjection APIs.
 * <p>
 * An application requesting a {@link MediaProjection session} can add its own content in the
 * list of available content along with the whole screen or a single application.
 * <p>
 * Each instance of {@link MediaProjectionAppContent} contains an id that is used to identify the
 * content chosen by the user back to the advertising application, thus the meaning of the id is
 * only relevant to that application and must uniquely identify a content to be shared.
 */
public final class MediaProjectionAppContent implements Parcelable {

    private final Bitmap mThumbnail;
    private final CharSequence mTitle;
    private final int mId;

    /**
     * Constructor to pass a thumbnail, title and id.
     *
     * @param thumbnail The thumbnail representing this content to be shown to the user.
     * @param title     A user visible string representing the title of this content.
     * @param id        An arbitrary int defined by the advertising application to be fed back once
     *                  the user made their choice.
     */
    public MediaProjectionAppContent(@NonNull Bitmap thumbnail, @NonNull CharSequence title,
            int id) {
        mThumbnail = Objects.requireNonNull(thumbnail, "thumbnail can't be null").asShared();
        mTitle = Objects.requireNonNull(title, "title can't be null");
        mId = id;
    }

    
    
    
    private MediaProjectionAppContent(Parcel in) {
        mThumbnail = in.readParcelable(this.getClass().getClassLoader(), Bitmap.class);
        mTitle = in.readCharSequence();
        mId = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mThumbnail, flags);
        dest.writeCharSequence(mTitle);
        dest.writeInt(mId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<MediaProjectionAppContent> CREATOR =
            new Creator<>() {
                @NonNull
                @Override
                public MediaProjectionAppContent createFromParcel(@NonNull Parcel in) {
                    return new MediaProjectionAppContent(in);
                }

                @NonNull
                @Override
                public MediaProjectionAppContent[] newArray(int size) {
                    return new MediaProjectionAppContent[size];
                }
            };
}
