package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP SyncNotedAppOp (android-16.0.0_r4, SyncNotedAppOp.java): what an op noted on the calling thread looked like. */
public final class SyncNotedAppOp implements Parcelable {
	private final int mOpMode;
	private final int mOpCode;
	private final String mAttributionTag;
	private final String mPackageName;

	public SyncNotedAppOp(int opMode, int opCode, String attributionTag, String packageName) {
		if (opCode < 0 || opCode > AppOpsManager._NUM_OP - 1) {
			throw new IllegalArgumentException("opCode was " + opCode + " but must be between 0 and " + (AppOpsManager._NUM_OP - 1));
		}
		mOpCode = opCode;
		mAttributionTag = attributionTag;
		mOpMode = opMode;
		mPackageName = packageName;
	}

	public SyncNotedAppOp(int opCode, String attributionTag) {
		this(AppOpsManager.MODE_IGNORED, opCode, attributionTag, null);
	}

	public SyncNotedAppOp(int opCode, String attributionTag, String packageName) {
		this(AppOpsManager.MODE_IGNORED, opCode, attributionTag, packageName);
	}

	public String getOp() {
		return AppOpsManager.opToPublicName(mOpCode);
	}

	public int getOpMode() {
		return mOpMode;
	}

	public String getAttributionTag() {
		return mAttributionTag;
	}

	public String getPackageName() {
		return mPackageName;
	}

	@Override
	public String toString() {
		return "SyncNotedAppOp { opMode = " + mOpMode + ", opCode = " + getOp() + ", attributionTag = " + mAttributionTag
		    + ", packageName = " + mPackageName + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SyncNotedAppOp that = (SyncNotedAppOp) o;
		return mOpMode == that.mOpMode && mOpCode == that.mOpCode && Objects.equals(mAttributionTag, that.mAttributionTag)
		    && Objects.equals(mPackageName, that.mPackageName);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + mOpMode;
		hash = 31 * hash + mOpCode;
		hash = 31 * hash + Objects.hashCode(mAttributionTag);
		hash = 31 * hash + Objects.hashCode(mPackageName);
		return hash;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		byte flg = 0;
		if (mAttributionTag != null) flg |= 0x4;
		if (mPackageName != null) flg |= 0x8;
		dest.writeByte(flg);
		dest.writeInt(mOpMode);
		dest.writeInt(mOpCode);
		if (mAttributionTag != null) dest.writeString(mAttributionTag);
		if (mPackageName != null) dest.writeString(mPackageName);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	SyncNotedAppOp(Parcel in) {
		byte flg = in.readByte();
		int opMode = in.readInt();
		int opCode = in.readInt();
		String attributionTag = (flg & 0x4) == 0 ? null : in.readString();
		String packageName = (flg & 0x8) == 0 ? null : in.readString();
		mOpMode = opMode;
		mOpCode = opCode;
		mAttributionTag = attributionTag;
		mPackageName = packageName;
	}

	public static final Parcelable.Creator<SyncNotedAppOp> CREATOR = new Parcelable.Creator<SyncNotedAppOp>() {
		@Override
		public SyncNotedAppOp[] newArray(int size) {
			return new SyncNotedAppOp[size];
		}

		@Override
		public SyncNotedAppOp createFromParcel(Parcel in) {
			return new SyncNotedAppOp(in);
		}
	};
}
