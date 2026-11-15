package android.media.quality;

/* AOSP android/media/quality/ParameterCapability.java */
import android.annotation.IntDef;
import android.annotation.StringDef;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Capability info of media quality parameters
 */
public final class ParameterCapability implements Parcelable {

    
    /**
     * None parameter type. It's used when a parameter is not supported.
     */
    public static final int TYPE_NONE = 0;

    /**
     * Integer parameter type
     */
    public static final int TYPE_INT = 1;

    /**
     * Long integer parameter type
     */
    public static final int TYPE_LONG = 2;

    /**
     * Double parameter type
     */
    public static final int TYPE_DOUBLE = 3;

    /**
     * String parameter type
     */
    public static final int TYPE_STRING = 4;

    
    /**
     * The key for the max possible value of this parameter.
     */
    public static final String CAPABILITY_MAX = "max";

    /**
     * The key for the min possible value of this parameter.
     */
    public static final String CAPABILITY_MIN = "min";

    /**
     * The key for the default value of this parameter.
     */
    public static final String CAPABILITY_DEFAULT = "default";

    /**
     * The key for the enumeration of this parameter.
     */
    public static final String CAPABILITY_ENUM = "enum";

    @NonNull
    private final String mName;
    private final boolean mIsSupported;
    @ParameterType
    private final int mType;
    @NonNull
    private final Bundle mCaps;

    
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeBoolean(mIsSupported);
        dest.writeInt(mType);
        dest.writeBundle(mCaps);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<ParameterCapability> CREATOR = new Creator<ParameterCapability>() {
        @Override
        public ParameterCapability createFromParcel(Parcel in) {
            return new ParameterCapability(in);
        }

        @Override
        public ParameterCapability[] newArray(int size) {
            return new ParameterCapability[size];
        }
    };

    
    /**
     * Gets parameter name.
     */
    @NonNull
    public String getParameterName() {
        return mName;
    }

    /**
     * Returns whether this parameter is supported or not.
     */
    public boolean isSupported() {
        return mIsSupported;
    }

    /**
     * Gets parameter type.
     *
     * <p>It's {@link #TYPE_NONE} if {@link #isSupported()} is {@code false}.
     */
    @ParameterType
    public int getParameterType() {
        return mType;
    }

    /**
     * Gets capability information.
     * <p>e.g. use the key {@link #CAPABILITY_MAX} to get the max value.
     */
    @NonNull
    public Bundle getCapabilities() {
        return new Bundle(mCaps);
    }
}
