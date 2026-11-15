package android.renderscript;

import android.content.Context;

/* AOSP frameworks/base/rs/java/android/renderscript/RenderScript.java (deprecated since API 31). AOSP's context
 * lives in the RenderScript service/driver; ATL has none, so the objects of this package (Element, Type, Allocation,
 * ScriptIntrinsicBlur) are plain Java, computing like AOSP's CPU reference implementation (frameworks/rs/cpu_ref).
 * Not provided: custom scripts (ScriptC), the other intrinsics, 2D/3D/cubemap and mipmaps. */
public class RenderScript {
	private boolean mDestroyed;
	private RSMessageHandler mMessageHandler;

	private RenderScript() {
	}

	public static RenderScript create(Context ctx) {
		return new RenderScript();
	}

	void validate() {
		if (mDestroyed) {
			throw new RSInvalidStateException("Calling RS with an invalid or destroyed context.");
		}
	}

	public void destroy() {
		validate();
		mDestroyed = true;
	}

	public static void releaseAllContexts() {
	}

	public void setMessageHandler(RSMessageHandler msg) {
		mMessageHandler = msg;
	}

	public RSMessageHandler getMessageHandler() {
		return mMessageHandler;
	}

	public static class RSMessageHandler implements Runnable {
		protected int[] mData;
		protected int mID;
		protected int mLength;

		public void run() {
		}
	}
}
