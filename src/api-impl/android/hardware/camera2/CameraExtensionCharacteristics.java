package android.hardware.camera2;

import android.util.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* AOSP core/java/android/hardware/camera2/CameraExtensionCharacteristics.java (android-16). The query methods
 * behave as on a device without the camera extensions proxy service: registerClient() fails, so there are no
 * extensions and the per-extension queries throw "Unsupported extensions". */
public final class CameraExtensionCharacteristics {
	public static final int EXTENSION_AUTOMATIC = 0;
	public static final int EXTENSION_FACE_RETOUCH = 1;
	@Deprecated
	public static final int EXTENSION_BEAUTY = EXTENSION_FACE_RETOUCH;
	public static final int EXTENSION_BOKEH = 2;
	public static final int EXTENSION_HDR = 3;
	public static final int EXTENSION_NIGHT = 4;

	/* AOSP CameraExtensionCharacteristics(Context, cameraId, chars), made by CameraManager */
	CameraExtensionCharacteristics() {
	}

	public List<Integer> getSupportedExtensions() {
		return Collections.unmodifiableList(new ArrayList<Integer>());
	}

	private static void unsupported() {
		throw new IllegalArgumentException("Unsupported extensions");
	}

	public boolean isPostviewAvailable(int extension) {
		unsupported();
		return false;
	}

	public List<Size> getPostviewSupportedSizes(int extension, Size captureSize, int format) {
		unsupported();
		return null;
	}

	public <T> List<Size> getExtensionSupportedSizes(int extension, Class<T> klass) {
		Objects.requireNonNull(klass, "klass must not be null");
		if (klass != android.graphics.SurfaceTexture.class && klass != android.view.SurfaceView.class)
			return new ArrayList<>();
		unsupported();
		return null;
	}

	public List<Size> getExtensionSupportedSizes(int extension, int format) {
		unsupported();
		return null;
	}

	public boolean isCaptureProcessProgressAvailable(int extension) {
		unsupported();
		return false;
	}

	public Set<CaptureRequest.Key> getAvailableCaptureRequestKeys(int extension) {
		unsupported();
		return null;
	}

	public Set<CaptureResult.Key> getAvailableCaptureResultKeys(int extension) {
		unsupported();
		return null;
	}

	public <T> T get(int extension, CameraCharacteristics.Key<T> key) {
		unsupported();
		return null;
	}

	public Set<CameraCharacteristics.Key> getKeys(int extension) {
		unsupported();
		return null;
	}

	/* AOSP: the format is checked before the extensions service */
	public android.util.Range<Long> getEstimatedCaptureLatencyRangeMillis(int extension, Size captureOutputSize, int format) {
		switch (format) {
			case android.graphics.ImageFormat.YUV_420_888:
			case android.graphics.ImageFormat.JPEG:
			case android.graphics.ImageFormat.JPEG_R:
			case android.graphics.ImageFormat.DEPTH_JPEG:
			case android.graphics.ImageFormat.YCBCR_P010:
				break;
			default:
				throw new IllegalArgumentException("Unsupported format: " + format);
		}
		unsupported();
		return null;
	}
}
