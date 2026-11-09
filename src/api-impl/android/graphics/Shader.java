package android.graphics;

public class Shader {

	public enum TileMode {
		CLAMP,
		MIRROR,
		REPEAT,
		/* API 31: the edge colors are not extended, outside the shader nothing is drawn */
		DECAL
	}

	protected void init(long ni) {
	}

	/* AOSP Shader: the local matrix is kept here (null while it is the identity); there is no native shader instance
	 * to discard, Canvas reads it when it draws with a BitmapShader. */
	private Matrix mLocalMatrix;

	public boolean getLocalMatrix(Matrix localM) {
		if (mLocalMatrix != null) {
			localM.set(mLocalMatrix);
			return true; // presence of mLocalMatrix means it's not identity
		}
		return false;
	}

	public void setLocalMatrix(Matrix localM) {
		if (localM == null || localM.isIdentity()) {
			mLocalMatrix = null;
		} else if (mLocalMatrix == null) {
			mLocalMatrix = new Matrix(localM);
		} else if (!mLocalMatrix.equals(localM)) {
			mLocalMatrix.set(localM);
		}
	}

	protected void copyLocalMatrix(Shader dest) {
		dest.setLocalMatrix(mLocalMatrix);
	}
}
