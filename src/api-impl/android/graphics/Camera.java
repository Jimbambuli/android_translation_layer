package android.graphics;

/* AOSP's Camera is Skia's Sk3DView (SkCamera.cpp, with SK_BUILD_FOR_ANDROID_FRAMEWORK); this is that code in
 * Java: a stack of 3D matrices, and a camera that projects the transformed plane z = 0 onto the screen */
public class Camera {
	/* SkM44, row-major, column vectors */
	private static final class Rec {
		final float[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
		Rec next;
	}

	private Rec rec = new Rec();

	/* SkCamera3D */
	private float locationX = 0, locationY = 0, locationZ = -576; // 8 inches backward
	private float observerX = 0, observerY = 0, observerZ = -576;
	private float[] orientation;

	public void save() {
		Rec r = new Rec();
		System.arraycopy(rec.m, 0, r.m, 0, 16);
		r.next = rec;
		rec = r;
	}

	public void restore() {
		if (rec.next != null)
			rec = rec.next;
	}

	private void preConcat(float[] b) {
		float[] a = rec.m;
		float[] r = new float[16];
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				r[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j] + a[i * 4 + 3] * b[12 + j];
		System.arraycopy(r, 0, a, 0, 16);
	}

	public void translate(float x, float y, float z) {
		preConcat(new float[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1});
	}

	/* SkM44::setRotateUnitSinCos() */
	private void rotate(float x, float y, float z, float deg) {
		double rad = deg * Math.PI / 180;
		float s = (float)Math.sin(rad);
		float c = (float)Math.cos(rad);
		float t = 1 - c;
		preConcat(new float[] {t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0,
		                       t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0,
		                       t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0,
		                       0, 0, 0, 1});
	}

	public void rotateX(float deg) {
		rotate(1, 0, 0, deg);
	}

	public void rotateY(float deg) {
		rotate(0, -1, 0, deg);
	}

	public void rotateZ(float deg) {
		rotate(0, 0, 1, deg);
	}

	public void rotate(float x, float y, float z) {
		rotateX(x);
		rotateY(y);
		rotateZ(z);
	}

	/* the location is given in inches, kept in points */
	public void setLocation(float x, float y, float z) {
		locationX = x * 72;
		locationY = y * 72;
		locationZ = z * 72;
		observerX = 0;
		observerY = 0;
		observerZ = locationZ;
		orientation = null;
	}

	public float getLocationX() { return locationX / 72; }
	public float getLocationY() { return locationY / 72; }
	public float getLocationZ() { return locationZ / 72; }

	/* SkPatch3D after the matrix: u = M (1, 0, 0), v = M (0, -1, 0), origin = M (0, 0, 0) */
	private float[] patch() {
		float[] m = rec.m;
		return new float[] {m[0], m[4], m[8], -m[1], -m[5], -m[9], m[3], m[7], m[11]};
	}

	/* SkCamera3D::doUpdate() for the axis (0, 0, 1) and the zenith (0, -1, 0): cross = (1, 0, 0) */
	private float[] orientation() {
		if (orientation == null) {
			float x = observerX, y = observerY, z = observerZ;
			orientation = new float[] {-z, 0, x, 0, z, y, 0, 0, 1};
		}
		return orientation;
	}

	/* SkCamera3D::patchToMatrix() */
	public void getMatrix(Matrix matrix) {
		float[] p = patch();
		float[] o = orientation();
		float dx = p[6] - locationX, dy = p[7] - locationY, dz = p[8] - locationZ;
		float dot = dx * o[6] + dy * o[7] + dz * o[8];
		float[] v = new float[9];
		v[Matrix.MSCALE_X] = (p[0] * o[0] + p[1] * o[1] + p[2] * o[2]) / dot;
		v[Matrix.MSKEW_Y] = (p[0] * o[3] + p[1] * o[4] + p[2] * o[5]) / dot;
		v[Matrix.MPERSP_0] = (p[0] * o[6] + p[1] * o[7] + p[2] * o[8]) / dot;
		v[Matrix.MSKEW_X] = (p[3] * o[0] + p[4] * o[1] + p[5] * o[2]) / dot;
		v[Matrix.MSCALE_Y] = (p[3] * o[3] + p[4] * o[4] + p[5] * o[5]) / dot;
		v[Matrix.MPERSP_1] = (p[3] * o[6] + p[4] * o[7] + p[5] * o[8]) / dot;
		v[Matrix.MTRANS_X] = (dx * o[0] + dy * o[1] + dz * o[2]) / dot;
		v[Matrix.MTRANS_Y] = (dx * o[3] + dy * o[4] + dz * o[5]) / dot;
		v[Matrix.MPERSP_2] = 1;
		matrix.setValues(v);
	}

	public void applyToCanvas(Canvas canvas) {
		Matrix matrix = new Matrix();
		getMatrix(matrix);
		canvas.concat(matrix);
	}

	/* Sk3DView::dotWithNormal(): the normal of the patch is u x v */
	public float dotWithNormal(float dx, float dy, float dz) {
		float[] p = patch();
		float cx = p[1] * p[5] - p[2] * p[4];
		float cy = p[2] * p[3] - p[0] * p[4];
		float cz = p[0] * p[4] - p[1] * p[3];
		return cx * dx + cy * dy + cz * dz;
	}
}
