package android.hardware.camera2;

import android.util.AndroidException;

public class CameraAccessException extends AndroidException {
	private int mReason;

	public final int getReason() {
		return mReason;
	}
}
