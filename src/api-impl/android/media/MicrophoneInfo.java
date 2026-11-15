package android.media;

import android.util.Pair;

import java.util.List;

/* AOSP media/java/android/media/MicrophoneInfo.java */
public final class MicrophoneInfo {

	public static final int LOCATION_UNKNOWN = 0;

	public static final int LOCATION_MAINBODY = 1;

	public static final int LOCATION_MAINBODY_MOVABLE = 2;

	public static final int LOCATION_PERIPHERAL = 3;

	public static final int DIRECTIONALITY_UNKNOWN = 0;

	public static final int DIRECTIONALITY_OMNI = 1;

	public static final int DIRECTIONALITY_BI_DIRECTIONAL = 2;

	public static final int DIRECTIONALITY_CARDIOID = 3;

	public static final int DIRECTIONALITY_HYPER_CARDIOID = 4;

	public static final int DIRECTIONALITY_SUPER_CARDIOID = 5;

	public static final int CHANNEL_MAPPING_DIRECT = 1;

	public static final int CHANNEL_MAPPING_PROCESSED = 2;

	public static final int GROUP_UNKNOWN = -1;

	public static final int INDEX_IN_THE_GROUP_UNKNOWN = -1;

	public static final Coordinate3F POSITION_UNKNOWN = new Coordinate3F(
			-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

	public static final Coordinate3F ORIENTATION_UNKNOWN = new Coordinate3F(0.0f, 0.0f, 0.0f);

	public static final float SENSITIVITY_UNKNOWN = -Float.MAX_VALUE;

	public static final float SPL_UNKNOWN = -Float.MAX_VALUE;
	private Coordinate3F mPosition;
	private Coordinate3F mOrientation;
	private String mDeviceId;
	private String mAddress;
	private List<Pair<Float, Float>> mFrequencyResponse;
	private List<Pair<Integer, Integer>> mChannelMapping;
	private float mMaxSpl;
	private float mMinSpl;
	private float mSensitivity;
	private int mLocation;
	private int mGroup; 
	private int mIndexInTheGroup;
	private int mPortId; 
	private int mType;
	private int mDirectionality;

	MicrophoneInfo(String deviceId, int type, String address, int location,
			int group, int indexInTheGroup, Coordinate3F position,
			Coordinate3F orientation, List<Pair<Float, Float>> frequencyResponse,
			List<Pair<Integer, Integer>> channelMapping, float sensitivity, float maxSpl,
			float minSpl, int directionality) {
		mDeviceId = deviceId;
		mType = type;
		mAddress = address;
		mLocation = location;
		mGroup = group;
		mIndexInTheGroup = indexInTheGroup;
		mPosition = position;
		mOrientation = orientation;
		mFrequencyResponse = frequencyResponse;
		mChannelMapping = channelMapping;
		mSensitivity = sensitivity;
		mMaxSpl = maxSpl;
		mMinSpl = minSpl;
		mDirectionality = directionality;
	}

	public String getDescription() {
		return mDeviceId;
	}

	public int getId() {
		return mPortId;
	}

	public int getInternalDeviceType() {
		return mType;
	}

	public int getType() {
		return AudioDeviceInfo.convertInternalDeviceToDeviceType(mType);
	}

	public String getAddress() {
		return mAddress;
	}

	public int getLocation() {
		return mLocation;
	}

	public int getGroup() {
		return mGroup;
	}

	public int getIndexInTheGroup() {
		return mIndexInTheGroup;
	}

	public Coordinate3F getPosition() {
		return mPosition;
	}

	public Coordinate3F getOrientation() {
		return mOrientation;
	}

	public List<Pair<Float, Float>> getFrequencyResponse() {
		return mFrequencyResponse;
	}

	public List<Pair<Integer, Integer>> getChannelMapping() {
		return mChannelMapping;
	}

	public float getSensitivity() {
		return mSensitivity;
	}

	public float getMaxSpl() {
		return mMaxSpl;
	}

	public float getMinSpl() {
		return mMinSpl;
	}

	public int getDirectionality() {
		return mDirectionality;
	}

	public void setId(int portId) {
		mPortId = portId;
	}

	public void setChannelMapping(List<Pair<Integer, Integer>> channelMapping) {
		mChannelMapping = channelMapping;
	}

	public static final class Coordinate3F {
		public final float x;
		public final float y;
		public final float z;

		Coordinate3F(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Coordinate3F)) {
				return false;
			}
			Coordinate3F other = (Coordinate3F) obj;
			return this.x == other.x && this.y == other.y && this.z == other.z;
		}
	}
}
