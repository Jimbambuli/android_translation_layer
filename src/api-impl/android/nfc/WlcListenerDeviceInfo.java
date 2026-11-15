package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP WlcListenerDeviceInfo (modules/Nfc) */
public final class WlcListenerDeviceInfo implements Parcelable {
	public static final int STATE_DISCONNECTED = 1;
	public static final int STATE_CONNECTED_CHARGING = 2;
	public static final int STATE_CONNECTED_DISCHARGING = 3;

	private int mProductId;
	private double mTemperature;
	private double mBatteryLevel;
	private int mState;

	public WlcListenerDeviceInfo(int productId, double temperature, double batteryLevel, int state) {
		this.mProductId = productId;
		this.mTemperature = temperature;
		this.mBatteryLevel = batteryLevel;
		this.mState = state;
	}

	public int getProductId() {
		return mProductId;
	}

	public double getTemperature() {
		return mTemperature;
	}

	public double getBatteryLevel() {
		return mBatteryLevel;
	}

	public int getState() {
		return mState;
	}

	private WlcListenerDeviceInfo(Parcel in) {
		this.mProductId = in.readInt();
		this.mTemperature = in.readDouble();
		this.mBatteryLevel = in.readDouble();
		this.mState = in.readInt();
	}

	public static final Parcelable.Creator<WlcListenerDeviceInfo> CREATOR = new Parcelable.Creator<WlcListenerDeviceInfo>() {
		@Override
		public WlcListenerDeviceInfo createFromParcel(Parcel in) {
			return new WlcListenerDeviceInfo(in);
		}

		@Override
		public WlcListenerDeviceInfo[] newArray(int size) {
			return new WlcListenerDeviceInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mProductId);
		dest.writeDouble(mTemperature);
		dest.writeDouble(mBatteryLevel);
		dest.writeInt(mState);
	}
}
