package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP AsyncNotedAppOp (android-16.0.0_r4, AsyncNotedAppOp.java): an op noted outside the thread of the call. */
public final class AsyncNotedAppOp implements Parcelable {
	private final int mOpCode;
	private final int mNotingUid;
	private final String mAttributionTag;
	private final String mMessage;
	private final long mTime;

	public AsyncNotedAppOp(int opCode, int notingUid, String attributionTag, String message, long time) {
		if (opCode < 0) {
			throw new IllegalArgumentException("opCode was " + opCode + " but must be at least 0");
		}
		if (notingUid < 0) {
			throw new IllegalArgumentException("notingUid was " + notingUid + " but must be at least 0");
		}
		Objects.requireNonNull(message);
		if (opCode > AppOpsManager._NUM_OP - 1) {
			throw new IllegalArgumentException("opCode was " + opCode + " but must be between 0 and " + (AppOpsManager._NUM_OP - 1));
		}
		mOpCode = opCode;
		mNotingUid = notingUid;
		mAttributionTag = attributionTag;
		mMessage = message;
		mTime = time;
	}

	public String getOp() {
		return AppOpsManager.opToPublicName(mOpCode);
	}

	public int getNotingUid() {
		return mNotingUid;
	}

	public String getAttributionTag() {
		return mAttributionTag;
	}

	public String getMessage() {
		return mMessage;
	}

	public long getTime() {
		return mTime;
	}

	@Override
	public String toString() {
		return "AsyncNotedAppOp { opCode = " + getOp() + ", notingUid = " + mNotingUid + ", attributionTag = " + mAttributionTag
		    + ", message = " + mMessage + ", }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AsyncNotedAppOp that = (AsyncNotedAppOp) o;
		return mOpCode == that.mOpCode && mNotingUid == that.mNotingUid && Objects.equals(mAttributionTag, that.mAttributionTag)
		    && Objects.equals(mMessage, that.mMessage) && mTime == that.mTime;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + mOpCode;
		hash = 31 * hash + mNotingUid;
		hash = 31 * hash + Objects.hashCode(mAttributionTag);
		hash = 31 * hash + Objects.hashCode(mMessage);
		hash = 31 * hash + Long.hashCode(mTime);
		return hash;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		byte flg = 0;
		if (mAttributionTag != null) flg |= 0x4;
		dest.writeByte(flg);
		dest.writeInt(mOpCode);
		dest.writeInt(mNotingUid);
		if (mAttributionTag != null) dest.writeString(mAttributionTag);
		dest.writeString(mMessage);
		dest.writeLong(mTime);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	AsyncNotedAppOp(Parcel in) {
		byte flg = in.readByte();
		int opCode = in.readInt();
		int notingUid = in.readInt();
		String attributionTag = (flg & 0x4) == 0 ? null : in.readString();
		String message = in.readString();
		long time = in.readLong();
		mOpCode = opCode;
		mNotingUid = notingUid;
		mAttributionTag = attributionTag;
		mMessage = message;
		mTime = time;
	}

	public static final Parcelable.Creator<AsyncNotedAppOp> CREATOR = new Parcelable.Creator<AsyncNotedAppOp>() {
		@Override
		public AsyncNotedAppOp[] newArray(int size) {
			return new AsyncNotedAppOp[size];
		}

		@Override
		public AsyncNotedAppOp createFromParcel(Parcel in) {
			return new AsyncNotedAppOp(in);
		}
	};
}
