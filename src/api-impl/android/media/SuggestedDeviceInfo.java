package android.media;

/* AOSP media/java/android/media/SuggestedDeviceInfo.java */

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public final class SuggestedDeviceInfo implements Parcelable {
	@NonNull
	public static final Creator<SuggestedDeviceInfo> CREATOR =
			new Creator<SuggestedDeviceInfo>() {
				@Override
				public SuggestedDeviceInfo createFromParcel(Parcel in) {
					return new SuggestedDeviceInfo(in);
				}

				@Override
				public SuggestedDeviceInfo[] newArray(int size) {
					return new SuggestedDeviceInfo[size];
				}
			};

	@NonNull private final String mDeviceDisplayName;

	@NonNull private final String mRouteId;

	private final int mType;

	@NonNull private final Bundle mExtras;

	private SuggestedDeviceInfo(Builder builder) {
		mDeviceDisplayName = builder.mDeviceDisplayName;
		mRouteId = builder.mRouteId;
		mType = builder.mType;
		mExtras = builder.mExtras;
	}

	private SuggestedDeviceInfo(Parcel in) {
		mDeviceDisplayName = in.readString();
		mRouteId = in.readString();
		mType = in.readInt();
		mExtras = in.readBundle();
	}

	@NonNull
	public String getDeviceDisplayName() {
		return mDeviceDisplayName;
	}

	@NonNull
	public String getRouteId() {
		return mRouteId;
	}

	public int getType() {
		return mType;
	}

	@NonNull
	public Bundle getExtras() {
		return mExtras;
	}

	// SuggestedDeviceInfo Parcelable implementation.
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString(mDeviceDisplayName);
		dest.writeString(mRouteId);
		dest.writeInt(mType);
		dest.writeBundle(mExtras);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SuggestedDeviceInfo)) {
			return false;
		}
		return Objects.equals(mDeviceDisplayName, ((SuggestedDeviceInfo) obj).mDeviceDisplayName)
				&& Objects.equals(mRouteId, ((SuggestedDeviceInfo) obj).mRouteId)
				&& mType == ((SuggestedDeviceInfo) obj).mType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mDeviceDisplayName, mRouteId, mType);
	}

	@Override
	public String toString() {
		return mDeviceDisplayName + " | " + mRouteId + " | " + mType;
	}

	public static final class Builder {
		@NonNull private final String mDeviceDisplayName;

		@NonNull private final String mRouteId;

		private final int mType;

		private Bundle mExtras = Bundle.EMPTY;

		public Builder(
				@NonNull String deviceDisplayName,
				@NonNull String routeId,
				int type) {
			if (TextUtils.isEmpty(deviceDisplayName)) {
				throw new IllegalArgumentException("Device display name cannot be empty");
			}
			mDeviceDisplayName = deviceDisplayName;

			if (TextUtils.isEmpty(routeId)) {
				throw new IllegalArgumentException("Route ID cannot be empty.");
			}
			mRouteId = routeId;
			mType = type;
		}

		@NonNull
		public SuggestedDeviceInfo build() {
			return new SuggestedDeviceInfo(this);
		}

		@NonNull
		public Builder setExtras(@NonNull Bundle extras) {
			mExtras = Objects.requireNonNull(extras, "Extras must not be null");
			return this;
		}
	}
}
