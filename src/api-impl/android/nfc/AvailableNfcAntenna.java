package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

/* AOSP AvailableNfcAntenna (modules/Nfc) */
public final class AvailableNfcAntenna implements Parcelable {
	private final int mLocationX;
	private final int mLocationY;

	public AvailableNfcAntenna(int locationX, int locationY) {
		this.mLocationX = locationX;
		this.mLocationY = locationY;
	}

	public int getLocationX() {
		return mLocationX;
	}

	public int getLocationY() {
		return mLocationY;
	}

	private AvailableNfcAntenna(Parcel in) {
		this.mLocationX = in.readInt();
		this.mLocationY = in.readInt();
	}

	public static final Parcelable.Creator<AvailableNfcAntenna> CREATOR = new Parcelable.Creator<AvailableNfcAntenna>() {
		@Override
		public AvailableNfcAntenna createFromParcel(Parcel in) {
			return new AvailableNfcAntenna(in);
		}

		@Override
		public AvailableNfcAntenna[] newArray(int size) {
			return new AvailableNfcAntenna[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mLocationX);
		dest.writeInt(mLocationY);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mLocationX;
		result = prime * result + mLocationY;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AvailableNfcAntenna other = (AvailableNfcAntenna) obj;
		if (this.mLocationX != other.mLocationX)
			return false;
		return this.mLocationY == other.mLocationY;
	}

	@Override
	public String toString() {
		return "AvailableNfcAntenna " + "x: " + mLocationX + " y: " + mLocationY;
	}
}
