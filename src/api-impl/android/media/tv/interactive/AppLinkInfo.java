package android.media.tv.interactive;

/* AOSP media/java/android/media/tv/interactive/AppLinkInfo.java */
import android.annotation.NonNull;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * App link information used by TV interactive app to launch Android apps.
 */
public final class AppLinkInfo implements Parcelable {
    private @NonNull ComponentName mComponentName;
    private @NonNull Uri mUri;

    /**
     * Creates a new AppLinkInfo.
     *
     * @param packageName Package Name of AppLinkInfo.
     * @param className Class Name of AppLinkInfo.
     * @param uriString Uri of AppLinkInfo.
     */
    public AppLinkInfo(
            @NonNull String packageName,
            @NonNull String className,
            @NonNull String uriString) {
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, packageName);
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, className);
        this.mComponentName = new ComponentName(packageName, className);
        this.mUri = Uri.parse(uriString);
    }

    /**
     * Gets component name of the App link, which contains package name and class name.
     */
    @NonNull
    public ComponentName getComponentName() {
        return mComponentName;
    }

    /**
     * Gets URI of the App link.
     */
    @NonNull
    public Uri getUri() {
        return mUri;
    }

    @Override
    public String toString() {
        return "AppLinkInfo { "
                + "packageName = " + mComponentName.getPackageName() + ", "
                + "className = " + mComponentName.getClassName() + ", "
                + "uri = " + mUri.toString()
                + " }";
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mComponentName.writeToParcel(dest, flags);
        String uriString = mUri == null ? null : mUri.toString();
        dest.writeString(uriString);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /* package-private */ AppLinkInfo(@NonNull Parcel in) {
        mComponentName = ComponentName.readFromParcel(in);
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mComponentName.getPackageName());
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mComponentName.getClassName());
        String uriString = in.readString();
        mUri = uriString == null ? null : Uri.parse(uriString);
    }

    @NonNull
    public static final Parcelable.Creator<AppLinkInfo> CREATOR =
            new Parcelable.Creator<AppLinkInfo>() {
                @Override
                public AppLinkInfo[] newArray(int size) {
                    return new AppLinkInfo[size];
                }

                @Override
                public AppLinkInfo createFromParcel(@NonNull Parcel in) {
                    return new AppLinkInfo(in);
                }
            };
}
