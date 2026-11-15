package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/* AOSP core/java/android/hardware/display/DeviceProductInfo.java */
public final class DeviceProductInfo implements Parcelable {
	public static final int CONNECTION_TO_SINK_UNKNOWN =
			IDeviceProductInfoConstants.CONNECTION_TO_SINK_UNKNOWN;

	public static final int CONNECTION_TO_SINK_BUILT_IN =
			IDeviceProductInfoConstants.CONNECTION_TO_SINK_BUILT_IN;

	public static final int CONNECTION_TO_SINK_DIRECT =
			IDeviceProductInfoConstants.CONNECTION_TO_SINK_DIRECT;

	public static final int CONNECTION_TO_SINK_TRANSITIVE =
			IDeviceProductInfoConstants.CONNECTION_TO_SINK_TRANSITIVE;

	private final String mName;
	private final String mManufacturerPnpId;
	private final String mProductId;
	private final Integer mModelYear;
	private final ManufactureDate mManufactureDate;
	private final int mConnectionToSinkType;

	public DeviceProductInfo(
			String name,
			String manufacturerPnpId,
			String productId,
			Integer modelYear,
			ManufactureDate manufactureDate,
			int connectionToSinkType) {
		mName = name;
		mManufacturerPnpId = manufacturerPnpId;
		mProductId = productId;
		mModelYear = modelYear;
		mManufactureDate = manufactureDate;
		mConnectionToSinkType = connectionToSinkType;
	}

	public DeviceProductInfo(
			String name,
			String manufacturerPnpId,
			String productId,
			int modelYear,
			int connectionToSinkType) {
		mName = name;
		mManufacturerPnpId = Objects.requireNonNull(manufacturerPnpId);
		mProductId = Objects.requireNonNull(productId);
		mModelYear = modelYear;
		mManufactureDate = null;
		mConnectionToSinkType = connectionToSinkType;
	}

	private DeviceProductInfo(Parcel in) {
		mName = in.readString();
		mManufacturerPnpId = in.readString();
		mProductId = (String) in.readValue(null);
		mModelYear = (Integer) in.readValue(null);
		mManufactureDate = (ManufactureDate) in.readValue(null);
		mConnectionToSinkType = in.readInt();
	}

	public String getName() {
		return mName;
	}

	public String getManufacturerPnpId() {
		return mManufacturerPnpId;
	}

	public String getProductId() {
		return mProductId;
	}

	public int getModelYear()  {
		return mModelYear != null ? mModelYear : -1;
	}

	public int getManufactureYear()  {
		if (mManufactureDate == null) {
			return -1;
		}
		return mManufactureDate.mYear != null ? mManufactureDate.mYear : -1;
	}

	public int getManufactureWeek() {
		if (mManufactureDate == null) {
			return -1;
		}
		return mManufactureDate.mWeek != null ?  mManufactureDate.mWeek : -1;
	}

	public ManufactureDate getManufactureDate() {
		return mManufactureDate;
	}

	public int getConnectionToSinkType() {
		return mConnectionToSinkType;
	}

	@Override
	public String toString() {
		return "DeviceProductInfo{"
				+ "name="
				+ mName
				+ ", manufacturerPnpId="
				+ mManufacturerPnpId
				+ ", productId="
				+ mProductId
				+ ", modelYear="
				+ mModelYear
				+ ", manufactureDate="
				+ mManufactureDate
				+ ", connectionToSinkType="
				+ mConnectionToSinkType
				+ '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DeviceProductInfo that = (DeviceProductInfo) o;
		return Objects.equals(mName, that.mName)
				&& Objects.equals(mManufacturerPnpId, that.mManufacturerPnpId)
				&& Objects.equals(mProductId, that.mProductId)
				&& Objects.equals(mModelYear, that.mModelYear)
				&& Objects.equals(mManufactureDate, that.mManufactureDate)
				&& mConnectionToSinkType == that.mConnectionToSinkType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mName, mManufacturerPnpId, mProductId, mModelYear, mManufactureDate,
				mConnectionToSinkType);
	}

	public static final Creator<DeviceProductInfo> CREATOR =
			new Creator<DeviceProductInfo>() {
				@Override
				public DeviceProductInfo createFromParcel(Parcel in) {
					return new DeviceProductInfo(in);
				}

				@Override
				public DeviceProductInfo[] newArray(int size) {
					return new DeviceProductInfo[size];
				}
			};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeString(mManufacturerPnpId);
		dest.writeValue(mProductId);
		dest.writeValue(mModelYear);
		dest.writeValue(mManufactureDate);
		dest.writeInt(mConnectionToSinkType);
	}

	public static class ManufactureDate implements Parcelable {
		private final Integer mWeek;
		private final Integer mYear;

		public ManufactureDate(Integer week, Integer year) {
			mWeek = week;
			mYear = year;
		}

		protected ManufactureDate(Parcel in) {
			mWeek = (Integer) in.readValue(null);
			mYear = (Integer) in.readValue(null);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeValue(mWeek);
			dest.writeValue(mYear);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<ManufactureDate> CREATOR =
				new Creator<ManufactureDate>() {
					@Override
					public ManufactureDate createFromParcel(Parcel in) {
						return new ManufactureDate(in);
					}

					@Override
					public ManufactureDate[] newArray(int size) {
						return new ManufactureDate[size];
					}
				};

		public Integer getYear() {
			return mYear;
		}

		public Integer getWeek() {
			return mWeek;
		}

		@Override
		public String toString() {
			return "ManufactureDate{week=" + mWeek + ", year=" + mYear + '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ManufactureDate that = (ManufactureDate) o;
			return Objects.equals(mWeek, that.mWeek) && Objects.equals(mYear, that.mYear);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mWeek, mYear);
		}
	}
}
