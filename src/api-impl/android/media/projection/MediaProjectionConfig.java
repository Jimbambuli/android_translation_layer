package android.media.projection;

import static android.view.Display.DEFAULT_DISPLAY;


import android.os.Parcelable;

import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.DataClass;


@DataClass(
		genEqualsHashCode = true,
		genAidl = true,
		genSetters = false,
		genConstructor = false,
		genBuilder = false,
		genToString = false,
		genHiddenConstDefs = true,
		genHiddenGetters = true,
		genConstDefs = false
)
/* AOSP media/java/android/media/projection/MediaProjectionConfig.java (DataClass validation of the
 * local IntDef CaptureRegion dropped with the annotation) */
public final class MediaProjectionConfig implements Parcelable {

	public static final int CAPTURE_REGION_USER_CHOICE = 0;

	public static final int CAPTURE_REGION_FIXED_DISPLAY = 1;
	private int mDisplayToCapture;

	private int mRegionToCapture = CAPTURE_REGION_USER_CHOICE;

	private MediaProjectionConfig() {
	}

	private MediaProjectionConfig(int captureRegion) {
		mRegionToCapture = captureRegion;
	}

	public static MediaProjectionConfig createConfigForDefaultDisplay() {
		MediaProjectionConfig config = new MediaProjectionConfig(CAPTURE_REGION_FIXED_DISPLAY);
		config.mDisplayToCapture = DEFAULT_DISPLAY;
		return config;
	}

	public static MediaProjectionConfig createConfigForUserChoice() {
		return new MediaProjectionConfig(CAPTURE_REGION_USER_CHOICE);
	}

	private static String captureRegionToString(int value) {
		switch (value) {
			case CAPTURE_REGION_USER_CHOICE:
				return "CAPTURE_REGION_USERS_CHOICE";
			case CAPTURE_REGION_FIXED_DISPLAY:
				return "CAPTURE_REGION_GIVEN_DISPLAY";
			default:
				return Integer.toHexString(value);
		}
	}

	@Override
	public String toString() {
		return "MediaProjectionConfig { "
				+ "displayToCapture = " + mDisplayToCapture + ", "
				+ "regionToCapture = " + captureRegionToString(mRegionToCapture)
				+ " }";
	}

	@DataClass.Generated.Member
	public int getDisplayToCapture() {
		return mDisplayToCapture;
	}

	@DataClass.Generated.Member
	public int getRegionToCapture() {
		return mRegionToCapture;
	}

	@Override
	@DataClass.Generated.Member
	public boolean equals(Object o) {

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		@SuppressWarnings("unchecked")
		MediaProjectionConfig that = (MediaProjectionConfig) o;
		return true
				&& mDisplayToCapture == that.mDisplayToCapture
				&& mRegionToCapture == that.mRegionToCapture;
	}

	@Override
	@DataClass.Generated.Member
	public int hashCode() {

		int _hash = 1;
		_hash = 31 * _hash + mDisplayToCapture;
		_hash = 31 * _hash + mRegionToCapture;
		return _hash;
	}

	@Override
	@DataClass.Generated.Member
	public void writeToParcel(android.os.Parcel dest, int flags) {

		dest.writeInt(mDisplayToCapture);
		dest.writeInt(mRegionToCapture);
	}

	@Override
	@DataClass.Generated.Member
	public int describeContents() { return 0; }

	@SuppressWarnings({"unchecked", "RedundantCast"})
	@DataClass.Generated.Member
	 MediaProjectionConfig(android.os.Parcel in) {

		int displayToCapture = in.readInt();
		int regionToCapture = in.readInt();

		this.mDisplayToCapture = displayToCapture;
		AnnotationValidations.validate(
				android.annotation.IntRange.class, null, mDisplayToCapture,
				"from", DEFAULT_DISPLAY,
				"to", DEFAULT_DISPLAY);
		this.mRegionToCapture = regionToCapture;
	}

	@DataClass.Generated.Member
	public static final Parcelable.Creator<MediaProjectionConfig> CREATOR
			= new Parcelable.Creator<MediaProjectionConfig>() {
		@Override
		public MediaProjectionConfig[] newArray(int size) {
			return new MediaProjectionConfig[size];
		}

		@Override
		public MediaProjectionConfig createFromParcel(android.os.Parcel in) {
			return new MediaProjectionConfig(in);
		}
	};


}
