package android.media.quality;

/* AOSP android/media/quality/PictureProfile.java */
import android.annotation.IntDef;
import android.annotation.StringDef;
import android.media.tv.TvInputInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Profile for picture quality.
 */
public final class PictureProfile implements Parcelable {
    @Nullable
    private String mId;
    private final int mType;
    @NonNull
    private final String mName;
    @Nullable
    private final String mInputId;
    @NonNull
    private final String mPackageName;
    @NonNull
    private final PersistableBundle mParams;
    private final PictureProfileHandle mHandle;

    
    /**
     * System profile type.
     *
     * <p>A profile of system type is managed by the system, and readable to the package returned by
     * {@link #getPackageName()}.
     */
    public static final int TYPE_SYSTEM = 1;
    /**
     * Application profile type.
     *
     * <p>A profile of application type is managed by the package returned by
     * {@link #getPackageName()}.
     */
    public static final int TYPE_APPLICATION = 2;

    
    
    
    /**
     * Error code for unknown errors.
     */
    public static final int ERROR_UNKNOWN = 0;

    /**
     * Error code for missing necessary permission to handle the profiles.
     */
    public static final int ERROR_NO_PERMISSION = 1;

    /**
     * Error code for creating a profile with existing profile type and name.
     *
     * @see #getProfileType()
     * @see #getName()
     */
    public static final int ERROR_DUPLICATE = 2;

    /**
     * Error code for invalid argument.
     */
    public static final int ERROR_INVALID_ARGUMENT = 3;

    /**
     * Error code for the case when an operation requires an allowlist but the caller is not in the
     * list.
     *
     * @see MediaQualityManager#getPictureProfileAllowList()
     */
    public static final int ERROR_NOT_ALLOWLISTED = 4;

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
                            
    private PictureProfile(@NonNull Parcel in) {
        mId = in.readString();
        mType = in.readInt();
        mName = in.readString();
        mInputId = in.readString();
        mPackageName = in.readString();
        mParams = in.readPersistableBundle();
        mHandle = in.readParcelable(PictureProfileHandle.class.getClassLoader(),
                PictureProfileHandle.class);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeInt(mType);
        dest.writeString(mName);
        dest.writeString(mInputId);
        dest.writeString(mPackageName);
        dest.writePersistableBundle(mParams);
        dest.writeParcelable(mHandle, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<PictureProfile> CREATOR = new Creator<PictureProfile>() {
        @Override
        public PictureProfile createFromParcel(Parcel in) {
            return new PictureProfile(in);
        }

        @Override
        public PictureProfile[] newArray(int size) {
            return new PictureProfile[size];
        }
    };

    
    /**
     * Gets profile ID.
     *
     * <p>A profile ID is a globally unique ID generated and assigned by the system. For profile
     * objects retrieved from system (e.g {@link MediaQualityManager#getAvailablePictureProfiles})
     * this profile ID is non-null; For profiles built locally with {@link Builder}, it's
     * {@code null}.
     *
     * @return the unique profile ID; {@code null} if the profile is built locally with
     * {@link Builder}.
     */
    @Nullable
    public String getProfileId() {
        return mId;
    }

    
    /**
     * Gets profile type.
     */
    @ProfileType
    public int getProfileType() {
        return mType;
    }

    /**
     * Gets the profile name.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Gets the input ID if the profile is for a TV input.
     *
     * @return the corresponding TV input ID; {@code null} if the profile is not associated with a
     * TV input.
     *
     * @see TvInputInfo#getId()
     */
    @Nullable
    public String getInputId() {
        return mInputId;
    }

    /**
     * Gets the package name of this profile.
     *
     * <p>The package name defines the user of a profile. Only this specific package and system app
     * can access to this profile.
     *
     * @return the package name; {@code null} if the profile is built locally using
     * {@link Builder} and the package is not set.
     */
    @Nullable
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Gets the parameters of this profile.
     *
     * <p>The keys of commonly used parameters can be found in
     * {@link MediaQualityContract.PictureQuality}.
     *
     * @return The profile parameters. Empty bundle if parameters are not included in a query.
     */
    @NonNull
    public PersistableBundle getParameters() {
        return new PersistableBundle(mParams);
    }

    
    
    
    /**
     * A builder for {@link PictureProfile}.
     */
    public static final class Builder {
        @Nullable
        private String mId;
        private int mType = TYPE_APPLICATION;
        @NonNull
        private String mName;
        @Nullable
        private String mInputId;
        @NonNull
        private String mPackageName;
        @NonNull
        private PersistableBundle mParams;
        private PictureProfileHandle mHandle;

        /**
         * Creates a new Builder.
         */
        public Builder(@NonNull String name) {
            mName = name;
        }

        /**
         * Copy constructor of builder.
         */
        public Builder(@NonNull PictureProfile p) {
            mId = null; // ID needs to be reset
            mType = p.getProfileType();
            mName = p.getName();
            mPackageName = p.getPackageName();
            mInputId = p.getInputId();
            mParams = p.getParameters();
            mHandle = p.getHandle();
        }

        
        
        
        
        /**
         * Sets profile parameters.
         *
         * @see PictureProfile#getParameters()
         */
        @NonNull
        public Builder setParameters(@NonNull PersistableBundle params) {
            mParams = new PersistableBundle(params);
            return this;
        }

        
        /**
         * Builds the instance.
         */
        @NonNull
        public PictureProfile build() {

            PictureProfile o = new PictureProfile(
                    mId,
                    mType,
                    mName,
                    mInputId,
                    mPackageName,
                    mParams,
                    mHandle);
            return o;
        }
    }
}
