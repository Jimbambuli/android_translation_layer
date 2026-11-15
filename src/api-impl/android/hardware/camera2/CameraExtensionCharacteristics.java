package android.hardware.camera2;

/* AOSP core/java/android/hardware/camera2/CameraExtensionCharacteristics.java, constants only: ATL has no
 * camera service, so CameraManager.getCameraExtensionCharacteristics() fails before creating one. */
public final class CameraExtensionCharacteristics {
	public static final int EXTENSION_AUTOMATIC = 0;
	public static final int EXTENSION_FACE_RETOUCH = 1;
	@Deprecated
	public static final int EXTENSION_BEAUTY = EXTENSION_FACE_RETOUCH;
	public static final int EXTENSION_BOKEH = 2;
	public static final int EXTENSION_HDR = 3;
	public static final int EXTENSION_NIGHT = 4;

	private CameraExtensionCharacteristics() {
	}
}
