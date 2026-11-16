package android.opengl;

import android.hardware.SyncFence;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/* AOSP opengl/java/android/opengl/EGLExt.java (only the part the app side needs here). */
public class EGLExt {
	// EGL_KHR_create_context
	public static final int EGL_CONTEXT_MAJOR_VERSION_KHR = 0x3098;
	public static final int EGL_CONTEXT_MINOR_VERSION_KHR = 0x30FB;
	public static final int EGL_CONTEXT_FLAGS_KHR = 0x30FC;
	public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
	public static final int EGL_RECORDABLE_ANDROID = 0x3142;

	// EGL_ANDROID_native_fence_sync
	public static final int EGL_SYNC_NATIVE_FENCE_ANDROID = 0x3144;
	public static final int EGL_SYNC_NATIVE_FENCE_FD_ANDROID = 0x3145;
	public static final int EGL_SYNC_NATIVE_FENCE_SIGNALED_ANDROID = 0x3146;
	public static final int EGL_NO_NATIVE_FENCE_FD_ANDROID = -1;

	public EGLExt() {}

	/* AOSP eglDupNativeFenceFDANDROID(): the fd of the EGL_ANDROID_native_fence_sync call of the desktop EGL
	 * (android_opengl_EGLExt.c), wrapped in a SyncFence; an invalid SyncFence for EGL_NO_NATIVE_FENCE_FD_ANDROID */
	public static SyncFence eglDupNativeFenceFDANDROID(EGLDisplay display, EGLSync sync) {
		int fd = eglDupNativeFenceFDANDROIDImpl(display, sync);
		Log.d("EGL", "eglDupNativeFence returned " + fd);
		if (fd >= 0) {
			return SyncFence.create(ParcelFileDescriptor.adoptFd(fd));
		} else {
			return SyncFence.createEmpty();
		}
	}

	private static native int eglDupNativeFenceFDANDROIDImpl(EGLDisplay display, EGLSync sync);

	/* AOSP eglPresentationTimeANDROID(): native. ATL's EGL layer answers EGL_ANDROID_presentation_time itself
	 * (libandroid/egl.c, desktop Mesa does not have the extension; the time is only a hint) */
	public static native boolean eglPresentationTimeANDROID(EGLDisplay dpy, EGLSurface sur, long time);
}
