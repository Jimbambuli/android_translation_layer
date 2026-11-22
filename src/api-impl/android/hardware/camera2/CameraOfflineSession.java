/* AOSP frameworks/base/core/java/android/hardware/camera2/CameraOfflineSession.java (Android 16)
 * Differences: javadoc and @IntDef/@Retention annotation of StatusCode removed (@interface kept). */
package android.hardware.camera2;

import android.annotation.NonNull;

public abstract class CameraOfflineSession extends CameraCaptureSession {
	public static abstract class CameraOfflineSessionCallback {
		public abstract void onReady(@NonNull CameraOfflineSession session);

		public abstract void onSwitchFailed(@NonNull CameraOfflineSession session);

		public abstract void onIdle(@NonNull CameraOfflineSession session);

		public static final int STATUS_INTERNAL_ERROR = 0;

		public @interface StatusCode {};

		public abstract void onError(@NonNull CameraOfflineSession session, @StatusCode int status);

		public abstract void onClosed(@NonNull CameraOfflineSession session);
	}

	@Override
	public abstract void close();
}
