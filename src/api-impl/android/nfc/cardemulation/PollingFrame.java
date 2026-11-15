package android.nfc.cardemulation;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/* AOSP PollingFrame (modules/Nfc) */
public final class PollingFrame implements Parcelable {
	public static final int POLLING_LOOP_TYPE_A = 'A';
	public static final int POLLING_LOOP_TYPE_B = 'B';
	public static final int POLLING_LOOP_TYPE_F = 'F';
	public static final int POLLING_LOOP_TYPE_ON = 'O';
	public static final int POLLING_LOOP_TYPE_OFF = 'X';
	public static final int POLLING_LOOP_TYPE_UNKNOWN = 'U';

	private static final String KEY_POLLING_LOOP_TYPE = "android.nfc.cardemulation.TYPE";
	private static final String KEY_POLLING_LOOP_DATA = "android.nfc.cardemulation.DATA";
	private static final String KEY_POLLING_LOOP_GAIN = "android.nfc.cardemulation.GAIN";
	private static final String KEY_POLLING_LOOP_TIMESTAMP = "android.nfc.cardemulation.TIMESTAMP";
	private static final String KEY_POLLING_LOOP_TRIGGERED_AUTOTRANSACT = "android.nfc.cardemulation.TRIGGERED_AUTOTRANSACT";

	private final int mType;
	private final byte[] mData;
	private final int mGain;
	private final long mTimestamp;
	private boolean mTriggeredAutoTransact;

	public static final Parcelable.Creator<PollingFrame> CREATOR = new Parcelable.Creator<PollingFrame>() {
		@Override
		public PollingFrame createFromParcel(Parcel source) {
			return new PollingFrame(source.readBundle());
		}

		@Override
		public PollingFrame[] newArray(int size) {
			return new PollingFrame[size];
		}
	};

	private PollingFrame(Bundle frame) {
		mType = frame.getInt(KEY_POLLING_LOOP_TYPE);
		byte[] data = frame.getByteArray(KEY_POLLING_LOOP_DATA);
		mData = (data == null) ? new byte[0] : data;
		mGain = frame.getInt(KEY_POLLING_LOOP_GAIN, -1);
		mTimestamp = frame.getLong(KEY_POLLING_LOOP_TIMESTAMP);
		mTriggeredAutoTransact = frame.containsKey(KEY_POLLING_LOOP_TRIGGERED_AUTOTRANSACT) &&
		                         frame.getBoolean(KEY_POLLING_LOOP_TRIGGERED_AUTOTRANSACT);
	}

	public PollingFrame(int type, byte[] data, int gain, long timestampMicros, boolean triggeredAutoTransact) {
		mType = type;
		mData = data == null ? new byte[0] : data;
		mGain = gain;
		mTimestamp = timestampMicros;
		mTriggeredAutoTransact = triggeredAutoTransact;
	}

	public int getType() {
		return mType;
	}

	public byte[] getData() {
		return mData;
	}

	public int getVendorSpecificGain() {
		return mGain;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public void setTriggeredAutoTransact(boolean triggeredAutoTransact) {
		mTriggeredAutoTransact = triggeredAutoTransact;
	}

	public boolean getTriggeredAutoTransact() {
		return mTriggeredAutoTransact;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeBundle(toBundle());
	}

	private Bundle toBundle() {
		Bundle frame = new Bundle();
		frame.putInt(KEY_POLLING_LOOP_TYPE, getType());
		if (getVendorSpecificGain() != -1) {
			frame.putInt(KEY_POLLING_LOOP_GAIN, (byte)getVendorSpecificGain());
		}
		frame.putByteArray(KEY_POLLING_LOOP_DATA, getData());
		frame.putLong(KEY_POLLING_LOOP_TIMESTAMP, getTimestamp());
		frame.putBoolean(KEY_POLLING_LOOP_TRIGGERED_AUTOTRANSACT, getTriggeredAutoTransact());
		return frame;
	}

	@Override
	public String toString() {
		return "PollingFrame { Type: " + (char)getType() + ", gain: " + getVendorSpecificGain() +
		       ", timestamp: " + Long.toUnsignedString(getTimestamp()) + ", data: [" +
		       hex(getData()) + "] }";
	}

	/* HexFormat.ofDelimiter(" ").formatHex() (Java 17) by hand */
	private static String hex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(String.format("%02x", data[i] & 0xff));
		}
		return sb.toString();
	}
}
