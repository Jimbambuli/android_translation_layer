package android.media;

import static android.media.MediaRouter2Utils.toUniqueId;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.internal.util.Preconditions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/* AOSP media/java/android/media/MediaRoute2Info.java */
public final class MediaRoute2Info implements Parcelable {
	public static final Creator<MediaRoute2Info> CREATOR = new Creator<MediaRoute2Info>() {
		@Override
		public MediaRoute2Info createFromParcel(Parcel in) {
			return new MediaRoute2Info(in);
		}

		@Override
		public MediaRoute2Info[] newArray(int size) {
			return new MediaRoute2Info[size];
		}
	};

	public static final String ROUTE_ID_DEVICE = "DEVICE_ROUTE";

	public static final String ROUTE_ID_DEFAULT = "DEFAULT_ROUTE";
	public static final int CONNECTION_STATE_DISCONNECTED = 0;

	public static final int CONNECTION_STATE_CONNECTING = 1;

	public static final int CONNECTION_STATE_CONNECTED = 2;
	public static final int PLAYBACK_VOLUME_FIXED = 0;
	
	public static final int PLAYBACK_VOLUME_VARIABLE = 1;
	public static final int TYPE_UNKNOWN = 0;

	public static final int TYPE_BUILTIN_SPEAKER = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;

	public static final int TYPE_WIRED_HEADSET = AudioDeviceInfo.TYPE_WIRED_HEADSET;

	public static final int TYPE_WIRED_HEADPHONES = AudioDeviceInfo.TYPE_WIRED_HEADPHONES;

	public static final int TYPE_BLUETOOTH_A2DP = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;

	public static final int TYPE_HDMI = AudioDeviceInfo.TYPE_HDMI;

	public static final int TYPE_USB_DEVICE = AudioDeviceInfo.TYPE_USB_DEVICE;

	public static final int TYPE_USB_ACCESSORY = AudioDeviceInfo.TYPE_USB_ACCESSORY;

	public static final int TYPE_DOCK = AudioDeviceInfo.TYPE_DOCK;

	public static final int TYPE_USB_HEADSET = AudioDeviceInfo.TYPE_USB_HEADSET;

	public static final int TYPE_HEARING_AID = AudioDeviceInfo.TYPE_HEARING_AID;

	public static final int TYPE_BLE_HEADSET = AudioDeviceInfo.TYPE_BLE_HEADSET;

	public static final int TYPE_REMOTE_TV = 1001;

	public static final int TYPE_REMOTE_SPEAKER = 1002;

	public static final int TYPE_REMOTE_AUDIO_VIDEO_RECEIVER = 1003;

	public static final int TYPE_REMOTE_TABLET = 1004;

	public static final int TYPE_REMOTE_TABLET_DOCKED = 1005;

	public static final int TYPE_REMOTE_COMPUTER = 1006;

	public static final int TYPE_REMOTE_GAME_CONSOLE = 1007;

	public static final int TYPE_REMOTE_CAR = 1008;

	public static final int TYPE_REMOTE_SMARTWATCH = 1009;

	public static final int TYPE_REMOTE_SMARTPHONE = 1010;

	public static final int TYPE_GROUP = 2000;

	/* android-16 MediaRoute2Info suitability status */
	public static final int SUITABILITY_STATUS_SUITABLE_FOR_DEFAULT_TRANSFER = 0;
	public static final int SUITABILITY_STATUS_SUITABLE_FOR_MANUAL_TRANSFER = 1;
	public static final int SUITABILITY_STATUS_NOT_SUITABLE_FOR_TRANSFER = 2;

	public static final String FEATURE_LIVE_AUDIO = "android.media.route.feature.LIVE_AUDIO";

	public static final String FEATURE_LIVE_VIDEO = "android.media.route.feature.LIVE_VIDEO";

	public static final String FEATURE_LOCAL_PLAYBACK =
			"android.media.route.feature.LOCAL_PLAYBACK";

	public static final String FEATURE_REMOTE_PLAYBACK =
			"android.media.route.feature.REMOTE_PLAYBACK";

	public static final String FEATURE_REMOTE_AUDIO_PLAYBACK =
			"android.media.route.feature.REMOTE_AUDIO_PLAYBACK";

	public static final String FEATURE_REMOTE_VIDEO_PLAYBACK =
			"android.media.route.feature.REMOTE_VIDEO_PLAYBACK";

	public static final String FEATURE_REMOTE_GROUP_PLAYBACK =
			"android.media.route.feature.REMOTE_GROUP_PLAYBACK";

	final String mId;
	final CharSequence mName;
	final List<String> mFeatures;
	final int mType;
	final boolean mIsSystem;
	final Uri mIconUri;
	final CharSequence mDescription;
	final int mConnectionState;
	final String mClientPackageName;
	final String mPackageName;
	final int mVolumeHandling;
	final int mVolumeMax;
	final int mVolume;
	final String mAddress;
	final Set<String> mDeduplicationIds;
	final Bundle mExtras;
	final String mProviderId;
	final boolean mIsVisibilityRestricted;
	final Set<String> mAllowedPackages;
	final int mSuitabilityStatus;

	MediaRoute2Info(Builder builder) {
		mId = builder.mId;
		mName = builder.mName;
		mFeatures = builder.mFeatures;
		mType = builder.mType;
		mIsSystem = builder.mIsSystem;
		mIconUri = builder.mIconUri;
		mDescription = builder.mDescription;
		mConnectionState = builder.mConnectionState;
		mClientPackageName = builder.mClientPackageName;
		mPackageName = builder.mPackageName;
		mVolumeHandling = builder.mVolumeHandling;
		mVolumeMax = builder.mVolumeMax;
		mVolume = builder.mVolume;
		mAddress = builder.mAddress;
		mDeduplicationIds = builder.mDeduplicationIds;
		mExtras = builder.mExtras;
		mProviderId = builder.mProviderId;
		mIsVisibilityRestricted = builder.mIsVisibilityRestricted;
		mAllowedPackages = builder.mAllowedPackages;
		mSuitabilityStatus = builder.mSuitabilityStatus;
	}

	MediaRoute2Info(Parcel in) {
		mId = in.readString();
		Preconditions.checkArgument(!TextUtils.isEmpty(mId));
		mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
		mFeatures = in.createStringArrayList();
		mType = in.readInt();
		mIsSystem = in.readBoolean();
		mIconUri = in.readParcelable(null, android.net.Uri.class);
		mDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
		mConnectionState = in.readInt();
		mClientPackageName = in.readString();
		mPackageName = in.readString();
		mVolumeHandling = in.readInt();
		mVolumeMax = in.readInt();
		mVolume = in.readInt();
		mAddress = in.readString();
		mDeduplicationIds = Set.of(in.readStringArray());
		mExtras = in.readBundle();
		mProviderId = in.readString();
		mIsVisibilityRestricted = in.readBoolean();
		mAllowedPackages = Set.of(in.createString8Array());
		mSuitabilityStatus = in.readInt();
	}

	public String getId() {
		if (!TextUtils.isEmpty(mProviderId)) {
			return toUniqueId(mProviderId, mId);
		} else {
			return mId;
		}
	}

	public CharSequence getName() {
		return mName;
	}

	public List<String> getFeatures() {
		return mFeatures;
	}

	public int getType() {
		return mType;
	}

	public boolean isSystemRoute() {
		return mIsSystem;
	}

	public Uri getIconUri() {
		return mIconUri;
	}

	public CharSequence getDescription() {
		return mDescription;
	}

	public int getConnectionState() {
		return mConnectionState;
	}

	public String getClientPackageName() {
		return mClientPackageName;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public int getVolumeHandling() {
		return mVolumeHandling;
	}

	public int getVolumeMax() {
		return mVolumeMax;
	}

	public int getVolume() {
		return mVolume;
	}

	public String getAddress() {
		return mAddress;
	}

	public Set<String> getDeduplicationIds() {
		return mDeduplicationIds;
	}

	public Bundle getExtras() {
		return mExtras == null ? null : new Bundle(mExtras);
	}

	public String getOriginalId() {
		return mId;
	}

	public String getProviderId() {
		return mProviderId;
	}

	public boolean hasAnyFeatures(Collection<String> features) {
		Objects.requireNonNull(features, "features must not be null");
		for (String feature : features) {
			if (getFeatures().contains(feature)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllFeatures(Collection<String> features) {
		Objects.requireNonNull(features, "features must not be null");
		for (String feature : features) {
			if (!getFeatures().contains(feature)) {
				return false;
			}
		}
		return true;
	}

	public boolean isValid() {
		if (TextUtils.isEmpty(getId()) || TextUtils.isEmpty(getName())
				|| TextUtils.isEmpty(getProviderId())) {
			return false;
		}
		return true;
	}

	public boolean isVisibleTo(String packageName) {
		return !mIsVisibilityRestricted || getPackageName().equals(packageName)
				|| mAllowedPackages.contains(packageName);
	}

	public void dump(PrintWriter pw, String prefix) {
		pw.println(prefix + "MediaRoute2Info");

		String indent = prefix + "  ";

		pw.println(indent + "mId=" + mId);
		pw.println(indent + "mName=" + mName);
		pw.println(indent + "mFeatures=" + mFeatures);
		pw.println(indent + "mType=" + getDeviceTypeString(mType));
		pw.println(indent + "mIsSystem=" + mIsSystem);
		pw.println(indent + "mIconUri=" + mIconUri);
		pw.println(indent + "mDescription=" + mDescription);
		pw.println(indent + "mConnectionState=" + mConnectionState);
		pw.println(indent + "mClientPackageName=" + mClientPackageName);
		pw.println(indent + "mPackageName=" + mPackageName);

		dumpVolume(pw, indent);

		pw.println(indent + "mAddress=" + mAddress);
		pw.println(indent + "mDeduplicationIds=" + mDeduplicationIds);
		pw.println(indent + "mExtras=" + mExtras);
		pw.println(indent + "mProviderId=" + mProviderId);
		pw.println(indent + "mIsVisibilityRestricted=" + mIsVisibilityRestricted);
		pw.println(indent + "mAllowedPackages=" + mAllowedPackages);
		pw.println(indent + "mSuitabilityStatus=" + mSuitabilityStatus);
	}

	private void dumpVolume(PrintWriter pw, String prefix) {
		String volumeHandlingName;

		switch (mVolumeHandling) {
			case PLAYBACK_VOLUME_FIXED:
				volumeHandlingName = "FIXED";
				break;
			case PLAYBACK_VOLUME_VARIABLE:
				volumeHandlingName = "VARIABLE";
				break;
			default:
				volumeHandlingName = "UNKNOWN";
				break;
		}

		String volume = String.format(Locale.US,
				"volume(current=%d, max=%d, handling=%s(%d))",
				mVolume, mVolumeMax, volumeHandlingName, mVolumeHandling);

		pw.println(prefix + volume);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MediaRoute2Info)) {
			return false;
		}
		MediaRoute2Info other = (MediaRoute2Info) obj;
		return Objects.equals(mId, other.mId)
				&& Objects.equals(mName, other.mName)
				&& Objects.equals(mFeatures, other.mFeatures)
				&& (mType == other.mType)
				&& (mIsSystem == other.mIsSystem)
				&& Objects.equals(mIconUri, other.mIconUri)
				&& Objects.equals(mDescription, other.mDescription)
				&& (mConnectionState == other.mConnectionState)
				&& Objects.equals(mClientPackageName, other.mClientPackageName)
				&& Objects.equals(mPackageName, other.mPackageName)
				&& (mVolumeHandling == other.mVolumeHandling)
				&& (mVolumeMax == other.mVolumeMax)
				&& (mVolume == other.mVolume)
				&& Objects.equals(mAddress, other.mAddress)
				&& Objects.equals(mDeduplicationIds, other.mDeduplicationIds)
				&& Objects.equals(mProviderId, other.mProviderId)
				&& (mIsVisibilityRestricted == other.mIsVisibilityRestricted)
				&& Objects.equals(mAllowedPackages, other.mAllowedPackages)
				&& mSuitabilityStatus == other.mSuitabilityStatus;
	}

	public int getSuitabilityStatus() {
		return mSuitabilityStatus;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mId, mName, mFeatures, mType, mIsSystem, mIconUri, mDescription,
				mConnectionState, mClientPackageName, mPackageName, mVolumeHandling, mVolumeMax,
				mVolume, mAddress, mDeduplicationIds, mProviderId, mIsVisibilityRestricted,
				mAllowedPackages, mSuitabilityStatus);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
				.append("MediaRoute2Info{ ")
				.append("id=").append(getId())
				.append(", name=").append(getName())
				.append(", features=").append(getFeatures())
				.append(", iconUri=").append(getIconUri())
				.append(", description=").append(getDescription())
				.append(", connectionState=").append(getConnectionState())
				.append(", clientPackageName=").append(getClientPackageName())
				.append(", volumeHandling=").append(getVolumeHandling())
				.append(", volumeMax=").append(getVolumeMax())
				.append(", volume=").append(getVolume())
				.append(", deduplicationIds=").append(String.join(",", getDeduplicationIds()))
				.append(", providerId=").append(getProviderId())
				.append(", isVisibilityRestricted=").append(mIsVisibilityRestricted)
				.append(", allowedPackages=").append(String.join(",", mAllowedPackages))
				.append(" }");
		return result.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		TextUtils.writeToParcel(mName, dest, flags);
		dest.writeStringList(mFeatures);
		dest.writeInt(mType);
		dest.writeBoolean(mIsSystem);
		dest.writeParcelable(mIconUri, flags);
		TextUtils.writeToParcel(mDescription, dest, flags);
		dest.writeInt(mConnectionState);
		dest.writeString(mClientPackageName);
		dest.writeString(mPackageName);
		dest.writeInt(mVolumeHandling);
		dest.writeInt(mVolumeMax);
		dest.writeInt(mVolume);
		dest.writeString(mAddress);
		dest.writeStringArray(mDeduplicationIds.toArray(new String[mDeduplicationIds.size()]));
		dest.writeBundle(mExtras);
		dest.writeString(mProviderId);
		dest.writeBoolean(mIsVisibilityRestricted);
		dest.writeString8Array(mAllowedPackages.toArray(new String[0]));
		dest.writeInt(mSuitabilityStatus);
	}

	private static String getDeviceTypeString(int deviceType) {
		switch (deviceType) {
			case TYPE_BUILTIN_SPEAKER:
				return "BUILTIN_SPEAKER";
			case TYPE_WIRED_HEADSET:
				return "WIRED_HEADSET";
			case TYPE_WIRED_HEADPHONES:
				return "WIRED_HEADPHONES";
			case TYPE_BLUETOOTH_A2DP:
				return "BLUETOOTH_A2DP";
			case TYPE_HDMI:
				return "HDMI";
			case TYPE_DOCK:
				return "DOCK";
			case TYPE_USB_DEVICE:
				return "USB_DEVICE";
			case TYPE_USB_ACCESSORY:
				return "USB_ACCESSORY";
			case TYPE_USB_HEADSET:
				return "USB_HEADSET";
			case TYPE_HEARING_AID:
				return "HEARING_AID";
			case TYPE_REMOTE_TV:
				return "REMOTE_TV";
			case TYPE_REMOTE_SPEAKER:
				return "REMOTE_SPEAKER";
			case TYPE_REMOTE_AUDIO_VIDEO_RECEIVER:
				return "REMOTE_AUDIO_VIDEO_RECEIVER";
			case TYPE_REMOTE_TABLET:
				return "REMOTE_TABLET";
			case TYPE_REMOTE_TABLET_DOCKED:
				return "REMOTE_TABLET_DOCKED";
			case TYPE_REMOTE_COMPUTER:
				return "REMOTE_COMPUTER";
			case TYPE_REMOTE_GAME_CONSOLE:
				return "REMOTE_GAME_CONSOLE";
			case TYPE_REMOTE_CAR:
				return "REMOTE_CAR";
			case TYPE_REMOTE_SMARTWATCH:
				return "REMOTE_SMARTWATCH";
			case TYPE_REMOTE_SMARTPHONE:
				return "REMOTE_SMARTPHONE";
			case TYPE_GROUP:
				return "GROUP";
			case TYPE_UNKNOWN:
			default:
				return String.format("UNKNOWN(%d)", deviceType);
		}
	}

	public static final class Builder {
		final String mId;
		final CharSequence mName;
		final List<String> mFeatures;

		int mType = TYPE_UNKNOWN;
		boolean mIsSystem;
		Uri mIconUri;
		CharSequence mDescription;
		int mConnectionState;
		String mClientPackageName;
		String mPackageName;
		int mVolumeHandling = PLAYBACK_VOLUME_FIXED;
		int mVolumeMax;
		int mVolume;
		String mAddress;
		Set<String> mDeduplicationIds;
		Bundle mExtras;
		String mProviderId;
		boolean mIsVisibilityRestricted;
		Set<String> mAllowedPackages;
		int mSuitabilityStatus;

		public Builder(String id, CharSequence name) {
			if (TextUtils.isEmpty(id)) {
				throw new IllegalArgumentException("id must not be empty");
			}
			if (TextUtils.isEmpty(name)) {
				throw new IllegalArgumentException("name must not be empty");
			}
			mId = id;
			mName = name;
			mFeatures = new ArrayList<>();
			mDeduplicationIds = Set.of();
			mAllowedPackages = Set.of();
			mSuitabilityStatus = SUITABILITY_STATUS_SUITABLE_FOR_DEFAULT_TRANSFER;
		}

		public Builder(MediaRoute2Info routeInfo) {
			this(routeInfo.mId, routeInfo);
		}

		public Builder(String id, MediaRoute2Info routeInfo) {
			if (TextUtils.isEmpty(id)) {
				throw new IllegalArgumentException("id must not be empty");
			}
			Objects.requireNonNull(routeInfo, "routeInfo must not be null");

			mId = id;
			mName = routeInfo.mName;
			mFeatures = new ArrayList<>(routeInfo.mFeatures);
			mType = routeInfo.mType;
			mIsSystem = routeInfo.mIsSystem;
			mIconUri = routeInfo.mIconUri;
			mDescription = routeInfo.mDescription;
			mConnectionState = routeInfo.mConnectionState;
			mClientPackageName = routeInfo.mClientPackageName;
			mPackageName = routeInfo.mPackageName;
			mVolumeHandling = routeInfo.mVolumeHandling;
			mVolumeMax = routeInfo.mVolumeMax;
			mVolume = routeInfo.mVolume;
			mAddress = routeInfo.mAddress;
			mDeduplicationIds = Set.copyOf(routeInfo.mDeduplicationIds);
			if (routeInfo.mExtras != null) {
				mExtras = new Bundle(routeInfo.mExtras);
			}
			mProviderId = routeInfo.mProviderId;
			mIsVisibilityRestricted = routeInfo.mIsVisibilityRestricted;
			mAllowedPackages = routeInfo.mAllowedPackages;
			mSuitabilityStatus = routeInfo.mSuitabilityStatus;
		}

		public Builder addFeature(String feature) {
			if (TextUtils.isEmpty(feature)) {
				throw new IllegalArgumentException("feature must not be null or empty");
			}
			mFeatures.add(feature);
			return this;
		}

		public Builder addFeatures(Collection<String> features) {
			Objects.requireNonNull(features, "features must not be null");
			for (String feature : features) {
				addFeature(feature);
			}
			return this;
		}

		public Builder clearFeatures() {
			mFeatures.clear();
			return this;
		}

		public Builder setType(int type) {
			mType = type;
			return this;
		}

		public Builder setSystemRoute(boolean isSystem) {
			mIsSystem = isSystem;
			return this;
		}

		public Builder setIconUri(Uri iconUri) {
			mIconUri = iconUri;
			return this;
		}

		public Builder setDescription(CharSequence description) {
			mDescription = description;
			return this;
		}

		public Builder setConnectionState(int connectionState) {
			mConnectionState = connectionState;
			return this;
		}

		public Builder setClientPackageName(String packageName) {
			mClientPackageName = packageName;
			return this;
		}

		public Builder setPackageName(String packageName) {
			mPackageName = packageName;
			return this;
		}

		public Builder setVolumeHandling(int volumeHandling) {
			mVolumeHandling = volumeHandling;
			return this;
		}

		public Builder setVolumeMax(int volumeMax) {
			mVolumeMax = volumeMax;
			return this;
		}

		public Builder setVolume(int volume) {
			mVolume = volume;
			return this;
		}

		public Builder setAddress(String address) {
			mAddress = address;
			return this;
		}

		public Builder setDeduplicationIds(Set<String> id) {
			mDeduplicationIds = Set.copyOf(id);
			return this;
		}

		public Builder setExtras(Bundle extras) {
			if (extras == null) {
				mExtras = null;
				return this;
			}
			mExtras = new Bundle(extras);
			return this;
		}

		public Builder setProviderId(String providerId) {
			if (TextUtils.isEmpty(providerId)) {
				throw new IllegalArgumentException("providerId must not be null or empty");
			}
			mProviderId = providerId;
			return this;
		}

		public Builder setVisibilityPublic() {
			mIsVisibilityRestricted = false;
			mAllowedPackages = Set.of();
			return this;
		}

		public Builder setVisibilityRestricted(Set<String> allowedPackages) {
			mIsVisibilityRestricted = true;
			mAllowedPackages = Set.copyOf(allowedPackages);
			return this;
		}

		public Builder setSuitabilityStatus(int suitabilityStatus) {
			mSuitabilityStatus = suitabilityStatus;
			return this;
		}

		public MediaRoute2Info build() {
			if (mFeatures.isEmpty()) {
				throw new IllegalArgumentException("features must not be empty!");
			}
			return new MediaRoute2Info(this);
		}
	}
}
