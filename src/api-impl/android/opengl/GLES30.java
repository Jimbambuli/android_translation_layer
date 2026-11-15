package android.opengl;

/* AOSP opengl/java/android/opengl/GLES30.java (sync objects only). The host GL context provides them natively
 * (GLsync is passed as long, as in AOSP's JNI). */
public class GLES30 extends GLES20 {
	public static final int GL_MAX_SERVER_WAIT_TIMEOUT = 0x9111;
	public static final int GL_OBJECT_TYPE = 0x9112;
	public static final int GL_SYNC_CONDITION = 0x9113;
	public static final int GL_SYNC_STATUS = 0x9114;
	public static final int GL_SYNC_FLAGS = 0x9115;
	public static final int GL_SYNC_FENCE = 0x9116;
	public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
	public static final int GL_UNSIGNALED = 0x9118;
	public static final int GL_SIGNALED = 0x9119;
	public static final int GL_ALREADY_SIGNALED = 0x911A;
	public static final int GL_TIMEOUT_EXPIRED = 0x911B;
	public static final int GL_CONDITION_SATISFIED = 0x911C;
	public static final int GL_WAIT_FAILED = 0x911D;
	public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
	// GL_TIMEOUT_IGNORED is defined as 0xFFFFFFFFFFFFFFFFull in C.
	public static final long GL_TIMEOUT_IGNORED = -1;

	/* AOSP GLES30.glFenceSync()/glIsSync()/glDeleteSync()/glClientWaitSync()/glWaitSync(): native */
	public static native long glFenceSync(int condition, int flags);

	public static native boolean glIsSync(long sync);

	public static native void glDeleteSync(long sync);

	public static native int glClientWaitSync(long sync, int flags, long timeout);

	public static native void glWaitSync(long sync, int flags, long timeout);
}
