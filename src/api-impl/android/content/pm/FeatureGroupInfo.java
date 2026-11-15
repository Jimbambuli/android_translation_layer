package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A set of features that can be requested by an application. This corresponds
 * to information collected from the
 * AndroidManifest.xml's {@code <feature-group>} tag.
 */
public final class FeatureGroupInfo implements Parcelable {

    /**
     * The list of features that are required by this group.
     *
     * @see FeatureInfo#FLAG_REQUIRED
     */
    public FeatureInfo[] features;

    public FeatureGroupInfo() {
    }

    public FeatureGroupInfo(FeatureGroupInfo other) {
        features = other.features;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        /* AOSP writes the FeatureInfo array with writeTypedArray(); ATL's FeatureInfo is no Parcelable, so
         * the fields are written one by one (name, version, flags, reqGlEsVersion) */
        dest.writeInt(features == null ? -1 : features.length);
        if (features != null) {
            for (FeatureInfo fi : features) {
                dest.writeString(fi.name);
                dest.writeInt(fi.version);
                dest.writeInt(fi.flags);
                dest.writeInt(fi.reqGlEsVersion);
            }
        }
    }

    public static final @android.annotation.NonNull Creator<FeatureGroupInfo> CREATOR = new Creator<FeatureGroupInfo>() {
        @Override
        public FeatureGroupInfo createFromParcel(Parcel source) {
            FeatureGroupInfo group = new FeatureGroupInfo();
            int n = source.readInt();
            if (n >= 0) {
                group.features = new FeatureInfo[n];
                for (int i = 0; i < n; i++) {
                    FeatureInfo fi = new FeatureInfo();
                    fi.name = source.readString();
                    fi.version = source.readInt();
                    fi.flags = source.readInt();
                    fi.reqGlEsVersion = source.readInt();
                    group.features[i] = fi;
                }
            }
            return group;
        }

        @Override
        public FeatureGroupInfo[] newArray(int size) {
            return new FeatureGroupInfo[size];
        }
    };
}
