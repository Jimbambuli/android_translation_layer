package android.media.tv;

/* AOSP media/java/android/media/tv/TvInputInfo.java */

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.Objects;


/**
 * This class is used to specify meta information of a TV input.
 */
public final class TvInputInfo implements Parcelable {
    private static final boolean DEBUG = false;
    private static final String TAG = "TvInputInfo";

    
    // Should be in sync with frameworks/base/core/res/res/values/attrs.xml
    /**
     * TV input type: the TV input service is a tuner which provides channels.
     */
    public static final int TYPE_TUNER = 0;
    /**
     * TV input type: a generic hardware TV input type.
     */
    public static final int TYPE_OTHER = 1000;
    /**
     * TV input type: the TV input service represents a composite port.
     */
    public static final int TYPE_COMPOSITE = 1001;
    /**
     * TV input type: the TV input service represents a SVIDEO port.
     */
    public static final int TYPE_SVIDEO = 1002;
    /**
     * TV input type: the TV input service represents a SCART port.
     */
    public static final int TYPE_SCART = 1003;
    /**
     * TV input type: the TV input service represents a component port.
     */
    public static final int TYPE_COMPONENT = 1004;
    /**
     * TV input type: the TV input service represents a VGA port.
     */
    public static final int TYPE_VGA = 1005;
    /**
     * TV input type: the TV input service represents a DVI port.
     */
    public static final int TYPE_DVI = 1006;
    /**
     * TV input type: the TV input service is HDMI. (e.g. HDMI 1)
     */
    public static final int TYPE_HDMI = 1007;
    /**
     * TV input type: the TV input service represents a display port.
     */
    public static final int TYPE_DISPLAY_PORT = 1008;

    /**
     * Used as a String extra field in setup intents created by {@link #createSetupIntent()} to
     * supply the ID of a specific TV input to set up.
     */
    public static final String EXTRA_INPUT_ID = "android.media.tv.extra.INPUT_ID";

    private final ResolveInfo mService;
    private final String mId;
    private final int mType;
    private final CharSequence mLabel;
    private final int mLabelResId;
    private final String mSetupActivity;
    private final boolean mCanRecord;
    private final boolean mCanPauseRecording;
    private final int mTunerCount;
    private final String mParentId;
    private final Bundle mExtras;

    private TvInputInfo(ResolveInfo service, String id, int type, CharSequence label, int labelResId,
            String setupActivity, boolean canRecord, boolean canPauseRecording, int tunerCount,
            String parentId, Bundle extras) {
        mService = service;
        mId = id;
        mType = type;
        mLabel = label;
        mLabelResId = labelResId;
        mSetupActivity = setupActivity;
        mCanRecord = canRecord;
        mCanPauseRecording = canPauseRecording;
        mTunerCount = tunerCount;
        mParentId = parentId;
        mExtras = extras;
    }

    public String getId() {
        return mId;
    }

    public String getParentId() {
        return mParentId;
    }

    public ServiceInfo getServiceInfo() {
        return mService.serviceInfo;
    }

    public Intent createSetupIntent() {
        if (!TextUtils.isEmpty(mSetupActivity)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mService.serviceInfo.packageName, mSetupActivity);
            intent.putExtra(EXTRA_INPUT_ID, getId());
            return intent;
        }
        return null;
    }

    @Deprecated
    public Intent createSettingsIntent() {
        return null;
    }

    public int getType() {
        return mType;
    }

    public int getTunerCount() {
        return mTunerCount;
    }

    public boolean canRecord() {
        return mCanRecord;
    }

    public boolean canPauseRecording() {
        return mCanPauseRecording;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public boolean isPassthroughInput() {
        return mType != TYPE_TUNER;
    }

    /* AOSP TvInputInfo.isHidden(): TvInputSettings.isHidden() reads Settings.Secure.TV_INPUT_HIDDEN_INPUTS, which
     * ATL does not have; an unset setting is the empty set, so nothing is hidden. */
    public boolean isHidden(Context context) {
        return false;
    }

    public CharSequence loadLabel(@NonNull Context context) {
        if (mLabelResId != 0) {
            return context.getPackageManager().getText(mService.serviceInfo.packageName,
                    mLabelResId, null);
        } else if (!TextUtils.isEmpty(mLabel)) {
            return mLabel;
        }
        return mService.loadLabel(context.getPackageManager());
    }

    /* AOSP TvInputInfo.loadCustomLabel(): TvInputSettings.getCustomLabel() reads
     * Settings.Secure.TV_INPUT_CUSTOM_LABELS, which ATL does not have; an unset setting has no label. */
    public CharSequence loadCustomLabel(Context context) {
        return null;
    }

    /* AOSP TvInputInfo.loadIcon() without the Icon/icon-URI branches (only set for hardware inputs, which
     * need the hidden Builder setters): the service icon, or null if neither service nor application has one. */
    public Drawable loadIcon(@NonNull Context context) {
        if (mService.serviceInfo.icon == 0
                && mService.serviceInfo.applicationInfo.icon == 0) {
            return null;
        }
        return mService.serviceInfo.loadIcon(context.getPackageManager());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TvInputInfo)) {
            return false;
        }
        TvInputInfo obj = (TvInputInfo) o;
        return Objects.equals(mService, obj.mService)
                && TextUtils.equals(mId, obj.mId)
                && mType == obj.mType
                && TextUtils.equals(mLabel, obj.mLabel)
                && mLabelResId == obj.mLabelResId
                && TextUtils.equals(mSetupActivity, obj.mSetupActivity)
                && mCanRecord == obj.mCanRecord
                && mCanPauseRecording == obj.mCanPauseRecording
                && mTunerCount == obj.mTunerCount
                && TextUtils.equals(mParentId, obj.mParentId)
                && Objects.equals(mExtras, obj.mExtras);
    }

    @Override
    public String toString() {
        return "TvInputInfo{id=" + mId
                + ", pkg=" + mService.serviceInfo.packageName
                + ", service=" + mService.serviceInfo.name + "}";
    }

    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mService.writeToParcel(dest, flags);
        dest.writeString(mId);
        dest.writeInt(mType);
        TextUtils.writeToParcel(mLabel, dest, flags);
        dest.writeInt(mLabelResId);
        dest.writeString(mSetupActivity);
        dest.writeByte(mCanRecord ? (byte) 1 : 0);
        dest.writeByte(mCanPauseRecording ? (byte) 1 : 0);
        dest.writeInt(mTunerCount);
        dest.writeString(mParentId);
        dest.writeBundle(mExtras);
    }

    public static final @NonNull Parcelable.Creator<TvInputInfo> CREATOR =
            new Parcelable.Creator<TvInputInfo>() {
        @Override
        public TvInputInfo createFromParcel(Parcel in) {
            return new TvInputInfo(in);
        }

        @Override
        public TvInputInfo[] newArray(int size) {
            return new TvInputInfo[size];
        }
    };

    private TvInputInfo(Parcel in) {
        mService = ResolveInfo.CREATOR.createFromParcel(in);
        mId = in.readString();
        mType = in.readInt();
        mLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mLabelResId = in.readInt();
        mSetupActivity = in.readString();
        mCanRecord = in.readByte() == 1;
        mCanPauseRecording = in.readByte() == 1;
        mTunerCount = in.readInt();
        mParentId = in.readString();
        mExtras = in.readBundle();
    }

    public static final class Builder {
        private static final String XML_START_TAG_NAME = "tv-input";

        private final Context mContext;
        private final ResolveInfo mResolveInfo;
        private CharSequence mLabel;
        private int mLabelResId;
        private String mSetupActivity;
        private Boolean mCanRecord;
        private Boolean mCanPauseRecording;
        private Integer mTunerCount;
        private String mParentId;
        private Bundle mExtras;

        public Builder(Context context, ComponentName component) {
            if (context == null) {
                throw new IllegalArgumentException("context cannot be null.");
            }
            Intent intent = new Intent(TvInputService.SERVICE_INTERFACE).setComponent(component);
            mResolveInfo = context.getPackageManager().resolveService(intent,
                    PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
            if (mResolveInfo == null) {
                throw new IllegalArgumentException("Invalid component. Can't find the service.");
            }
            mContext = context;
        }

        public Builder setTunerCount(int tunerCount) {
            mTunerCount = tunerCount;
            return this;
        }

        public Builder setCanRecord(boolean canRecord) {
            mCanRecord = canRecord;
            return this;
        }

        @NonNull
        public Builder setCanPauseRecording(boolean canPauseRecording) {
            mCanPauseRecording = canPauseRecording;
            return this;
        }

        public Builder setExtras(Bundle extras) {
            mExtras = extras;
            return this;
        }

        public TvInputInfo build() {
            ComponentName componentName = new ComponentName(mResolveInfo.serviceInfo.packageName,
                    mResolveInfo.serviceInfo.name);
            String id = componentName.flattenToShortString();
            int type = TYPE_TUNER;
            parseServiceMetadata(type);
            return new TvInputInfo(mResolveInfo, id, type, mLabel, mLabelResId, mSetupActivity,
                    mCanRecord == null ? false : mCanRecord,
                    mCanPauseRecording == null ? false : mCanPauseRecording,
                    mTunerCount == null ? 0 : mTunerCount,
                    mParentId, mExtras);
        }

        private void parseServiceMetadata(int inputType) {
            ServiceInfo si = mResolveInfo.serviceInfo;
            PackageManager pm = mContext.getPackageManager();
            try (XmlResourceParser parser =
                         si.loadXmlMetaData(pm, TvInputService.SERVICE_META_DATA)) {
                if (parser == null) {
                    throw new IllegalStateException("No " + TvInputService.SERVICE_META_DATA
                            + " meta-data found for " + si.name);
                }

                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                }

                String nodeName = parser.getName();
                if (!XML_START_TAG_NAME.equals(nodeName)) {
                    throw new IllegalStateException("Meta-data does not start with "
                            + XML_START_TAG_NAME + " tag for " + si.name);
                }

                TypedArray sa = res.obtainAttributes(attrs,
                        com.android.internal.R.styleable.TvInputService);
                mSetupActivity = sa.getString(
                        com.android.internal.R.styleable.TvInputService_setupActivity);
                if (mCanRecord == null) {
                    mCanRecord = sa.getBoolean(
                            com.android.internal.R.styleable.TvInputService_canRecord, false);
                }
                if (mTunerCount == null && inputType == TYPE_TUNER) {
                    mTunerCount = sa.getInt(
                            com.android.internal.R.styleable.TvInputService_tunerCount, 1);
                }
                if (mCanPauseRecording == null) {
                    mCanPauseRecording = sa.getBoolean(
                            com.android.internal.R.styleable.TvInputService_canPauseRecording,
                            false);
                }

                sa.recycle();
            } catch (XmlPullParserException e) {
                throw new IllegalStateException("Failed reading meta-data for " + si.packageName, e);
            } catch (NameNotFoundException e) {
                throw new IllegalStateException("No resources found for " + si.packageName, e);
            } catch (Exception e) {
                throw new IllegalStateException("Failed reading meta-data for " + si.packageName, e);
            }
        }
    }
}
