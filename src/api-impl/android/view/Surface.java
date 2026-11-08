package android.view;

import android.annotation.IntDef;
import android.graphics.SurfaceTexture;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Surface implements android.os.Parcelable {

	/** @hide */
	@IntDef(prefix = {"ROTATION_"}, value = {ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Rotation {}

	/** Rotation constant: 0 degree rotation (natural orientation) */
	public static final int ROTATION_0 = 0;

	/** Rotation constant: 90 degree rotation. */
	public static final int ROTATION_90 = 1;

	/** Rotation constant: 180 degree rotation. */
	public static final int ROTATION_180 = 2;

	/** Rotation constant: 270 degree rotation. */
	public static final int ROTATION_270 = 3;

	public long widget;

	/* AOSP Surface.writeToParcel()/readFromParcel()/CREATOR: a Surface travels as the handle of the native surface it
	 * holds. ATL's Surface is the pointer of the GTK widget it draws into, which is valid in this process only: a
	 * parcel read in this process (what an app does with it, e.g. an in-process Bundle) gives a Surface on the same
	 * widget; there is no other process to read it in. */
	public static final android.os.Parcelable.Creator<Surface> CREATOR = new android.os.Parcelable.Creator<Surface>() {
		@Override
		public Surface createFromParcel(android.os.Parcel source) {
			try {
				Surface s = new Surface();
				s.readFromParcel(source);
				return s;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Surface[] newArray(int size) {
			return new Surface[size];
		}
	};

	public void readFromParcel(android.os.Parcel source) {
		if (source == null) {
			throw new IllegalArgumentException("source must not be null");
		}
		source.readString(); // the name AOSP gives a surface of a view, none here
		widget = source.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		if (dest == null) {
			throw new IllegalArgumentException("dest must not be null");
		}
		dest.writeString(null);
		dest.writeLong(widget);
	}

	public Surface() {}

	public Surface(SurfaceTexture surfaceTexture) {}

	public boolean isValid() {
		return widget != 0;
	}

	public void release() {}

	/* API 30/31: only a hint for the display's refresh rate, which ATL does not adjust */
	public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0;
	public static final int FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1;
	public static final int CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS = 0;
	public static final int CHANGE_FRAME_RATE_ALWAYS = 1;

	public void setFrameRate(float frameRate, int compatibility) {}

	public void setFrameRate(float frameRate, int compatibility, int changeFrameRateStrategy) {}
}
