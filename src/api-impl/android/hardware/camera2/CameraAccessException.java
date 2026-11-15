package android.hardware.camera2;

import android.util.AndroidException;

/* AOSP core/java/android/hardware/camera2/CameraAccessException.java */
public class CameraAccessException extends AndroidException {
	public static final int CAMERA_IN_USE = 4;
	public static final int MAX_CAMERAS_IN_USE = 5;
	public static final int CAMERA_DISABLED = 1;
	public static final int CAMERA_DISCONNECTED = 2;
	public static final int CAMERA_ERROR = 3;
	/** @hide */
	public static final int CAMERA_DEPRECATED_HAL = 1000;

	private static final long serialVersionUID = 5630338637471475675L;

	private final int mReason;

	public final int getReason() {
		return mReason;
	}

	public CameraAccessException(int problem) {
		super(getDefaultMessage(problem));
		mReason = problem;
	}

	public CameraAccessException(int problem, String message) {
		super(getCombinedMessage(problem, message));
		mReason = problem;
	}

	public CameraAccessException(int problem, String message, Throwable cause) {
		super(getCombinedMessage(problem, message), cause);
		mReason = problem;
	}

	public CameraAccessException(int problem, Throwable cause) {
		super(getDefaultMessage(problem), cause);
		mReason = problem;
	}

	/** @hide */
	public static String getDefaultMessage(int problem) {
		switch (problem) {
			case CAMERA_IN_USE:
				return "The camera device is in use already";
			case MAX_CAMERAS_IN_USE:
				return "The system-wide limit for number of open cameras has been reached, "
						+ "and more camera devices cannot be opened until previous instances "
						+ "are closed.";
			case CAMERA_DISCONNECTED:
				return "The camera device is removable and has been disconnected from the "
						+ "Android device, or the camera service has shut down the connection due "
						+ "to a higher-priority access request for the camera device.";
			case CAMERA_DISABLED:
				return "The camera is disabled due to a device policy, and cannot be opened.";
			case CAMERA_ERROR:
				return "The camera device is currently in the error state; "
						+ "no further calls to it will succeed.";
		}
		return null;
	}

	private static String getCombinedMessage(int problem, String message) {
		String problemString = getProblemString(problem);
		return String.format("%s (%d): %s", problemString, problem, message);
	}

	private static String getProblemString(int problem) {
		String problemString;
		switch (problem) {
			case CAMERA_IN_USE:
				problemString = "CAMERA_IN_USE";
				break;
			case MAX_CAMERAS_IN_USE:
				problemString = "MAX_CAMERAS_IN_USE";
				break;
			case CAMERA_DISCONNECTED:
				problemString = "CAMERA_DISCONNECTED";
				break;
			case CAMERA_DISABLED:
				problemString = "CAMERA_DISABLED";
				break;
			case CAMERA_ERROR:
				problemString = "CAMERA_ERROR";
				break;
			case CAMERA_DEPRECATED_HAL:
				problemString = "CAMERA_DEPRECATED_HAL";
				break;
			default:
				problemString = "<UNKNOWN ERROR>";
		}
		return problemString;
	}
}
