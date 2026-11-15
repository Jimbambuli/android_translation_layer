package android.opengl;

import com.google.android.gles_jni.EGLImpl;
import javax.microedition.khronos.egl.EGL10;

public class EGL14 {

	private static EGL10 impl = new EGLImpl();

	public static final EGLDisplay EGL_NO_DISPLAY = new EGLDisplay(0);
	public static final EGLContext EGL_NO_CONTEXT = new EGLContext(0);
	public static final EGLSurface EGL_NO_SURFACE = new EGLSurface(0);

	public static EGLDisplay eglGetDisplay(int display) {
		javax.microedition.khronos.egl.EGLDisplay eglDisplay = impl.eglGetDisplay(display);
		if (eglDisplay == EGL10.EGL_NO_DISPLAY)
			return EGL_NO_DISPLAY;
		else
			return new EGLDisplay(eglDisplay.native_egl_display);
	}

	public static boolean eglInitialize(EGLDisplay display, int[] major, int major_offset, int[] minor, int minor_offset) {
		int major_minor[] = new int[2];
		boolean ret = impl.eglInitialize(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), major_minor);
		if (major != null)
			major[major_offset] = major_minor[0];
		if (minor != null)
			minor[minor_offset] = major_minor[1];
		return ret;
	}

	public static boolean eglChooseConfig(EGLDisplay display, int[] attrib_list, int attrib_list_offset, EGLConfig[] configs, int config_offset, int config_size, int[] num_config, int num_config_offset) {
		int[] raw_attrib_list = new int[attrib_list.length - attrib_list_offset];
		System.arraycopy(attrib_list, attrib_list_offset, raw_attrib_list, 0, attrib_list.length - attrib_list_offset);
		javax.microedition.khronos.egl.EGLConfig[] raw_configs = new javax.microedition.khronos.egl.EGLConfig[config_size];
		int raw_num_config[] = new int[1];
		boolean ret = impl.eglChooseConfig(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), raw_attrib_list, raw_configs, config_size, raw_num_config);
		if (configs != null) {
			for (int i = 0; i < configs.length; i++) {
				configs[i + config_offset] = new EGLConfig(raw_configs[i].native_egl_config);
			}
		}
		if (num_config != null)
			num_config[num_config_offset] = raw_num_config[0];
		return ret;
	}

	public static EGLContext eglCreateContext(EGLDisplay display, EGLConfig config, EGLContext share_context, int[] attrib_list, int attrib_list_offset) {
		int[] raw_attrib_list = new int[attrib_list.length - attrib_list_offset];
		System.arraycopy(attrib_list, attrib_list_offset, raw_attrib_list, 0, attrib_list.length - attrib_list_offset);
		javax.microedition.khronos.egl.EGLConfig raw_config = new javax.microedition.khronos.egl.EGLConfig(config.getNativeHandle());
		javax.microedition.khronos.egl.EGLContext raw_share_context = (share_context != null) ? new javax.microedition.khronos.egl.EGLContext(share_context.getNativeHandle()) : null;
		javax.microedition.khronos.egl.EGLContext raw_egl_context = impl.eglCreateContext(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), raw_config, raw_share_context, raw_attrib_list);
		if (raw_egl_context == EGL10.EGL_NO_CONTEXT)
			return EGL_NO_CONTEXT;
		else
			return new EGLContext(raw_egl_context.native_egl_context);
	}

	public static EGLSurface eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, int[] attrib_list, int attrib_list_offset) {
		int[] raw_attrib_list = new int[attrib_list.length - attrib_list_offset];
		System.arraycopy(attrib_list, attrib_list_offset, raw_attrib_list, 0, attrib_list.length - attrib_list_offset);
		javax.microedition.khronos.egl.EGLConfig raw_config = new javax.microedition.khronos.egl.EGLConfig(config.getNativeHandle());
		javax.microedition.khronos.egl.EGLSurface raw_egl_surface = impl.eglCreatePbufferSurface(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), raw_config, raw_attrib_list);
		if (raw_egl_surface == EGL10.EGL_NO_SURFACE)
			return EGL_NO_SURFACE;
		else
			return new EGLSurface(((com.google.android.gles_jni.EGLSurfaceImpl)raw_egl_surface).mEGLSurface);
	}

	public static boolean eglBindAPI(int api) {
		return EGLImpl.native_eglBindAPI(api);
	}

	public static int eglGetError() {
		return EGLImpl.native_eglGetError();
	}

	/* AOSP: win is a Surface, SurfaceTexture or SurfaceHolder */
	public static EGLSurface eglCreateWindowSurface(EGLDisplay display, EGLConfig config, Object win, int[] attrib_list, int attrib_list_offset) {
		int[] raw_attrib_list = null;
		if (attrib_list != null) {
			raw_attrib_list = new int[attrib_list.length - attrib_list_offset];
			System.arraycopy(attrib_list, attrib_list_offset, raw_attrib_list, 0, attrib_list.length - attrib_list_offset);
		}
		javax.microedition.khronos.egl.EGLConfig raw_config = new javax.microedition.khronos.egl.EGLConfig(config.getNativeHandle());
		javax.microedition.khronos.egl.EGLSurface raw_egl_surface = impl.eglCreateWindowSurface(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), raw_config, win, raw_attrib_list);
		if (raw_egl_surface == EGL10.EGL_NO_SURFACE)
			return EGL_NO_SURFACE;
		else
			return new EGLSurface(((com.google.android.gles_jni.EGLSurfaceImpl)raw_egl_surface).mEGLSurface);
	}

	public static boolean eglSwapBuffers(EGLDisplay display, EGLSurface surface) {
		javax.microedition.khronos.egl.EGLDisplay raw_display = new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle());
		return impl.eglSwapBuffers(raw_display, new com.google.android.gles_jni.EGLSurfaceImpl(surface.getNativeHandle()));
	}

	public static boolean eglDestroySurface(EGLDisplay display, EGLSurface surface) {
		javax.microedition.khronos.egl.EGLDisplay raw_display = new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle());
		javax.microedition.khronos.egl.EGLSurface raw_surface = new com.google.android.gles_jni.EGLSurfaceImpl(surface.getNativeHandle());
		return impl.eglDestroySurface(raw_display, raw_surface);
	}

	public static boolean eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context) {
		javax.microedition.khronos.egl.EGLDisplay raw_display = new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle());
		javax.microedition.khronos.egl.EGLSurface raw_draw = new com.google.android.gles_jni.EGLSurfaceImpl(draw.getNativeHandle());
		javax.microedition.khronos.egl.EGLSurface raw_read = new com.google.android.gles_jni.EGLSurfaceImpl(read.getNativeHandle());
		javax.microedition.khronos.egl.EGLContext raw_context = (context != null) ? new javax.microedition.khronos.egl.EGLContext(context.getNativeHandle()) : EGL10.EGL_NO_CONTEXT;
		return impl.eglMakeCurrent(raw_display, raw_draw, raw_read, raw_context);
	}

	public static boolean eglDestroyContext(EGLDisplay display, EGLContext context) {
		javax.microedition.khronos.egl.EGLDisplay raw_display = new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle());
		javax.microedition.khronos.egl.EGLContext raw_context = new javax.microedition.khronos.egl.EGLContext(context.getNativeHandle());
		return impl.eglDestroyContext(raw_display, raw_context);
	}

	public static EGLContext eglGetCurrentContext() {
		javax.microedition.khronos.egl.EGLContext raw_context = impl.eglGetCurrentContext();
		if (raw_context == EGL10.EGL_NO_CONTEXT)
			return EGL_NO_CONTEXT;
		else
			return new EGLContext(raw_context.native_egl_context);
	}

	/* AOSP EGL14.eglGetCurrentSurface()/eglQueryContext()/eglQuerySurface() (android_opengl_EGL14.cpp): the EGL
	 * functions; the array variants check value and offset (IllegalArgumentException) and write value[offset] */
	public static EGLSurface eglGetCurrentSurface(int readdraw) {
		javax.microedition.khronos.egl.EGLSurface raw_surface = impl.eglGetCurrentSurface(readdraw);
		long handle = ((com.google.android.gles_jni.EGLSurfaceImpl)raw_surface).mEGLSurface;
		return handle == 0 ? EGL_NO_SURFACE : new EGLSurface(handle);
	}

	public static boolean eglQueryContext(EGLDisplay display, EGLContext context, int attribute, int[] value, int offset) {
		checkValueArray(value, offset);
		int[] raw_value = new int[1];
		boolean ret = impl.eglQueryContext(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), new javax.microedition.khronos.egl.EGLContext(context.getNativeHandle()), attribute, raw_value);
		value[offset] = raw_value[0];
		return ret;
	}

	public static boolean eglQuerySurface(EGLDisplay display, EGLSurface surface, int attribute, int[] value, int offset) {
		checkValueArray(value, offset);
		int[] raw_value = new int[1];
		boolean ret = impl.eglQuerySurface(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), new com.google.android.gles_jni.EGLSurfaceImpl(surface.getNativeHandle()), attribute, raw_value);
		value[offset] = raw_value[0];
		return ret;
	}

	private static void checkValueArray(int[] value, int offset) {
		if (value == null)
			throw new IllegalArgumentException("value == null");
		if (offset < 0)
			throw new IllegalArgumentException("offset < 0");
		if (value.length - offset < 1)
			throw new IllegalArgumentException("length - offset < 1 < needed");
	}

	public static String eglQueryString(EGLDisplay display, int name) {
		return impl.eglQueryString(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), name);
	}

	public static boolean eglReleaseThread() {
		return impl.eglReleaseThread();
	}

	public static boolean eglTerminate(EGLDisplay display) {
		javax.microedition.khronos.egl.EGLDisplay raw_display = new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle());
		return impl.eglTerminate(raw_display);
	}

	public static boolean eglGetConfigAttrib(EGLDisplay display, EGLConfig config, int attribute, int[] value, int offset) {
		int[] raw_value = new int[1];
		boolean ret = impl.eglGetConfigAttrib(new javax.microedition.khronos.egl.EGLDisplay(display.getNativeHandle()), new javax.microedition.khronos.egl.EGLConfig(config.getNativeHandle()), attribute, raw_value);
		if (value != null)
			value[offset] = raw_value[0];
		return ret;
	}

	/* AOSP EGL14 (android_opengl_EGL14.cpp, generated, not in the local sources): the EGL functions on the
	 * native handles; array arguments are checked like AOSP's other EGL14 array variants */
	public static EGLDisplay eglGetCurrentDisplay() {
		long handle = native_eglGetCurrentDisplay();
		return handle == 0 ? EGL_NO_DISPLAY : new EGLDisplay(handle);
	}

	public static boolean eglGetConfigs(EGLDisplay dpy, EGLConfig[] configs, int configsOffset, int config_size, int[] num_config, int num_configOffset) {
		if (configs == null)
			throw new IllegalArgumentException("configs == null");
		if (configsOffset < 0)
			throw new IllegalArgumentException("configsOffset < 0");
		if (configs.length - configsOffset < config_size)
			throw new IllegalArgumentException("length - configsOffset < config_size < needed");
		if (num_config == null)
			throw new IllegalArgumentException("num_config == null");
		if (num_configOffset < 0)
			throw new IllegalArgumentException("num_configOffset < 0");
		if (num_config.length - num_configOffset < 1)
			throw new IllegalArgumentException("length - num_configOffset < 1 < needed");
		long[] handles = new long[Math.max(config_size, 0)];
		int[] count = new int[1];
		boolean ret = native_eglGetConfigs(dpy.getNativeHandle(), handles, count);
		for (int i = 0; ret && i < count[0]; i++)
			configs[configsOffset + i] = new EGLConfig(handles[i]);
		num_config[num_configOffset] = count[0];
		return ret;
	}

	public static int eglQueryAPI() {
		return native_eglQueryAPI();
	}

	public static boolean eglWaitClient() {
		return native_eglWaitClient();
	}

	public static boolean eglWaitGL() {
		return native_eglWaitGL();
	}

	public static boolean eglWaitNative(int engine) {
		return native_eglWaitNative(engine);
	}

	public static boolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, int attribute, int value) {
		return native_eglSurfaceAttrib(dpy.getNativeHandle(), surface.getNativeHandle(), attribute, value);
	}

	public static boolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, int buffer) {
		return native_eglBindTexImage(dpy.getNativeHandle(), surface.getNativeHandle(), buffer);
	}

	public static boolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, int buffer) {
		return native_eglReleaseTexImage(dpy.getNativeHandle(), surface.getNativeHandle(), buffer);
	}

	public static boolean eglSwapInterval(EGLDisplay dpy, int interval) {
		return native_eglSwapInterval(dpy.getNativeHandle(), interval);
	}

	public static EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, int buftype, int buffer, EGLConfig config, int[] attrib_list, int offset) {
		return eglCreatePbufferFromClientBuffer(dpy, buftype, (long)buffer, config, attrib_list, offset);
	}

	/** @hide */
	public static EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, int buftype, long buffer, EGLConfig config, int[] attrib_list, int offset) {
		if (attrib_list != null && offset < 0)
			throw new IllegalArgumentException("offset < 0");
		if (attrib_list != null && attrib_list.length - offset < 1)
			throw new IllegalArgumentException("length - offset < 1 < needed");
		int[] attribs = attrib_list == null ? null : java.util.Arrays.copyOfRange(attrib_list, offset, attrib_list.length);
		long handle = native_eglCreatePbufferFromClientBuffer(dpy.getNativeHandle(), buftype, buffer, config.getNativeHandle(), attribs);
		return handle == 0 ? EGL_NO_SURFACE : new EGLSurface(handle);
	}

	/* AOSP: Android has no native pixmaps, its JNI throws UnsupportedOperationException for both */
	@Deprecated
	public static EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, int pixmap, int[] attrib_list, int offset) {
		throw new UnsupportedOperationException("eglCreatePixmapSurface");
	}

	public static boolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, int target) {
		throw new UnsupportedOperationException("eglCopyBuffers");
	}

	private static native long native_eglGetCurrentDisplay();
	private static native boolean native_eglGetConfigs(long dpy, long[] configs, int[] num_config);
	private static native int native_eglQueryAPI();
	private static native boolean native_eglWaitClient();
	private static native boolean native_eglWaitGL();
	private static native boolean native_eglWaitNative(int engine);
	private static native boolean native_eglSurfaceAttrib(long dpy, long surface, int attribute, int value);
	private static native boolean native_eglBindTexImage(long dpy, long surface, int buffer);
	private static native boolean native_eglReleaseTexImage(long dpy, long surface, int buffer);
	private static native boolean native_eglSwapInterval(long dpy, int interval);
	private static native long native_eglCreatePbufferFromClientBuffer(long dpy, int buftype, long buffer, long config, int[] attrib_list);
}
