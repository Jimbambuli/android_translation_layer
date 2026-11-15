package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

/* AOSP NfcAntennaInfo (modules/Nfc) */
public final class NfcAntennaInfo implements Parcelable {
	private final int mDeviceWidth;
	private final int mDeviceHeight;
	private final boolean mDeviceFoldable;
	private final List<AvailableNfcAntenna> mAvailableNfcAntennas;

	public NfcAntennaInfo(int deviceWidth, int deviceHeight, boolean deviceFoldable,
	                      List<AvailableNfcAntenna> availableNfcAntennas) {
		this.mDeviceWidth = deviceWidth;
		this.mDeviceHeight = deviceHeight;
		this.mDeviceFoldable = deviceFoldable;
		this.mAvailableNfcAntennas = availableNfcAntennas;
	}

	public int getDeviceWidth() {
		return mDeviceWidth;
	}

	public int getDeviceHeight() {
		return mDeviceHeight;
	}

	@Deprecated
	public boolean isDeviceFoldable() {
		return mDeviceFoldable;
	}

	public List<AvailableNfcAntenna> getAvailableNfcAntennas() {
		return mAvailableNfcAntennas;
	}

	private NfcAntennaInfo(Parcel in) {
		this.mDeviceWidth = in.readInt();
		this.mDeviceHeight = in.readInt();
		this.mDeviceFoldable = in.readByte() != 0;
		this.mAvailableNfcAntennas = new ArrayList<>();
		in.readTypedList(this.mAvailableNfcAntennas, AvailableNfcAntenna.CREATOR);
	}

	public static final Parcelable.Creator<NfcAntennaInfo> CREATOR = new Parcelable.Creator<NfcAntennaInfo>() {
		@Override
		public NfcAntennaInfo createFromParcel(Parcel in) {
			return new NfcAntennaInfo(in);
		}

		@Override
		public NfcAntennaInfo[] newArray(int size) {
			return new NfcAntennaInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mDeviceWidth);
		dest.writeInt(mDeviceHeight);
		dest.writeByte((byte)(mDeviceFoldable ? 1 : 0));
		dest.writeTypedList(mAvailableNfcAntennas, 0);
	}
}
