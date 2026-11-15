package android.opengl;

/* AOSP opengl/java/android/opengl/EGLExt.java (only the part the app side needs here). */
public class EGLExt {
	public static final int EGL_RECORDABLE_ANDROID = 0x3142;

	/* AOSP eglPresentationTimeANDROID(): native. ATL's EGL layer answers EGL_ANDROID_presentation_time itself
	 * (libandroid/egl.c, desktop Mesa does not have the extension; the time is only a hint) */
	public static native boolean eglPresentationTimeANDROID(EGLDisplay dpy, EGLSurface sur, long time);
}
