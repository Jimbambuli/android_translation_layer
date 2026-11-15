package android.renderscript;

/* AOSP BaseObj: the base of every RenderScript object, destroyed once. The objects here (see RenderScript) hold
 * plain Java memory, so destroy() only marks them. */
public class BaseObj {
	private boolean mDestroyed;

	BaseObj(RenderScript rs) {
		rs.validate();
	}

	void checkValid() {
		if (mDestroyed) {
			throw new RSInvalidStateException("using a destroyed object.");
		}
	}

	public void destroy() {
		if (mDestroyed) {
			throw new RSInvalidStateException("Object already destroyed.");
		}
		mDestroyed = true;
	}
}
