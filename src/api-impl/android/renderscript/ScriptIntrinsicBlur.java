package android.renderscript;

/* AOSP ScriptIntrinsicBlur with the computation of frameworks/rs/cpu_ref/rsCpuIntrinsicBlur.cpp
 * (ComputeGaussianWeights(), OneVU4/OneVU1, OneHU4/OneHU1): a Gaussian of sigma = 0.4 * radius + 0.6 over
 * ceil(radius) pixels each side, first down the columns then along the rows, the edge pixels extended, the result
 * truncated to a byte. */
public class ScriptIntrinsicBlur extends ScriptIntrinsic {
	private final Element mElement;
	private Allocation mInput;
	private float mRadius = 5;

	private ScriptIntrinsicBlur(RenderScript rs, Element e) {
		super(rs);
		mElement = e;
	}

	public static ScriptIntrinsicBlur create(RenderScript rs, Element e) {
		if ((!e.isCompatible(Element.U8_4(rs))) && (!e.isCompatible(Element.U8(rs)))) {
			throw new RSIllegalArgumentException("Unsuported element type.");
		}
		return new ScriptIntrinsicBlur(rs, e);
	}

	public void setInput(Allocation ain) {
		if (ain.getType().getY() == 0) {
			throw new RSIllegalArgumentException("Input set to a 1D Allocation");
		}
		mInput = ain;
	}

	public void setRadius(float radius) {
		if (radius <= 0 || radius > 25) {
			throw new RSIllegalArgumentException("Radius out of range (0 < r <= 25).");
		}
		mRadius = radius;
	}

	public void forEach(Allocation aout) {
		checkValid();
		if (aout.getType().getY() == 0) {
			throw new RSIllegalArgumentException("Output is a 1D Allocation");
		}
		if (mInput == null) {
			throw new RSIllegalArgumentException("Input must be set before the blur runs");
		}
		final int w = mInput.getType().getX();
		final int h = mInput.getType().getY();
		final int ch = mInput.getType().getElement().getVectorSize();
		if (aout.getType().getX() != w || aout.getType().getY() != h
		    || aout.getType().getElement().getVectorSize() != ch) {
			throw new RSIllegalArgumentException("Input and output do not match in size or element");
		}

		final float[] weights = gaussianWeights(mRadius);
		final int iradius = weights.length / 2;
		final byte[] in = mInput.mData;
		final float[] tmp = new float[w * h * ch];
		// the columns (OneVU4 / OneVU1)
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				for (int c = 0; c < ch; c++) {
					float acc = 0;
					for (int r = -iradius; r <= iradius; r++) {
						int vy = Math.min(Math.max(y + r, 0), h - 1);
						acc += (in[(vy * w + x) * ch + c] & 0xff) * weights[r + iradius];
					}
					tmp[(y * w + x) * ch + c] = acc;
				}
			}
		}
		// the rows (OneHU4 / OneHU1)
		final byte[] out = aout.mData;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				for (int c = 0; c < ch; c++) {
					float acc = 0;
					for (int r = -iradius; r <= iradius; r++) {
						int vx = Math.min(Math.max(x + r, 0), w - 1);
						acc += tmp[(y * w + vx) * ch + c] * weights[r + iradius];
					}
					out[(y * w + x) * ch + c] = (byte)Math.min(Math.max((int)acc, 0), 255);
				}
			}
		}
	}

	/* RsdCpuScriptIntrinsicBlur::ComputeGaussianWeights() */
	private static float[] gaussianWeights(float radius) {
		final float e = 2.718281828459045f;
		final float pi = 3.1415926535897932f;
		final float sigma = 0.4f * radius + 0.6f;
		final float coeff1 = 1.0f / ((float)Math.sqrt(2.0f * pi) * sigma);
		final float coeff2 = -1.0f / (2.0f * sigma * sigma);
		final int iradius = (int)((float)Math.ceil(radius) + 0.5f);
		final float[] fp = new float[2 * iradius + 1];
		float normalizeFactor = 0.0f;
		for (int r = -iradius; r <= iradius; r++) {
			float floatR = (float)r;
			fp[r + iradius] = coeff1 * (float)Math.pow(e, floatR * floatR * coeff2);
			normalizeFactor += fp[r + iradius];
		}
		normalizeFactor = 1.0f / normalizeFactor;
		for (int r = -iradius; r <= iradius; r++) {
			fp[r + iradius] *= normalizeFactor;
		}
		return fp;
	}
}
