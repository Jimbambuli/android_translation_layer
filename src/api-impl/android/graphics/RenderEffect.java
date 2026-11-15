package android.graphics;

import android.util.Log;

/* ATL: a render effect is applied to an android.graphics.RenderNode by GSK. Blurs and offsets are drawn
 * (a blur of a blur adds the variances, GSK's blur is the same in x and y and has no edge treatment);
 * the other effects leave the content as it is. */
public final class RenderEffect {
	private static final int OFFSET = 0;
	private static final int BLUR = 1;
	private static final int OTHER = 2;

	private final int type;
	private final float x;
	private final float y;
	private final RenderEffect input;
	private final RenderEffect input2;

	private RenderEffect(int type, float x, float y, RenderEffect input, RenderEffect input2) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.input = input;
		this.input2 = input2;
	}

	public static RenderEffect createOffsetEffect(float offsetX, float offsetY) {
		return new RenderEffect(OFFSET, offsetX, offsetY, null, null);
	}

	public static RenderEffect createOffsetEffect(float offsetX, float offsetY, RenderEffect input) {
		return new RenderEffect(OFFSET, offsetX, offsetY, input, null);
	}

	public static RenderEffect createBlurEffect(float radiusX, float radiusY, RenderEffect inputEffect, Shader.TileMode edgeTreatment) {
		return new RenderEffect(BLUR, radiusX, radiusY, inputEffect, null);
	}

	public static RenderEffect createBlurEffect(float radiusX, float radiusY, Shader.TileMode edgeTreatment) {
		return new RenderEffect(BLUR, radiusX, radiusY, null, null);
	}

	public static RenderEffect createColorFilterEffect(ColorFilter colorFilter) {
		return new RenderEffect(OTHER, 0, 0, null, null);
	}

	public static RenderEffect createColorFilterEffect(ColorFilter colorFilter, RenderEffect renderEffect) {
		return new RenderEffect(OTHER, 0, 0, renderEffect, null);
	}

	/* AOSP RenderEffect.createShaderEffect()/createRuntimeShaderEffect(): the shader replaces the content;
	 * ATL draws no shaders per effect, so the content stays as it is (like the other effects) */
	public static RenderEffect createShaderEffect(Shader shader) {
		return new RenderEffect(OTHER, 0, 0, null, null);
	}

	public static RenderEffect createRuntimeShaderEffect(RuntimeShader shader, String uniformShaderName) {
		return new RenderEffect(OTHER, 0, 0, null, null);
	}

	public static RenderEffect createBlendModeEffect(RenderEffect dst, RenderEffect src, BlendMode blendMode) {
		return new RenderEffect(OTHER, 0, 0, dst, src);
	}

	public static RenderEffect createChainEffect(RenderEffect outer, RenderEffect inner) {
		return new RenderEffect(OTHER, 0, 0, inner, outer);
	}

	/* SkBlurMask::ConvertRadiusToSigma() */
	private static float radiusToSigma(float radius) {
		return radius > 0 ? 0.57735f * radius + 0.5f : 0.0f;
	}

	private static boolean warned;

	/* the blur variance and offset of this effect and its inputs: {variance, offset x, offset y} */
	void accumulate(float[] out) {
		if (input != null)
			input.accumulate(out);
		if (input2 != null)
			input2.accumulate(out);
		if (type == BLUR) {
			float sigma = radiusToSigma(Math.max(x, y));
			out[0] += sigma * sigma;
		} else if (type == OFFSET) {
			out[1] += x;
			out[2] += y;
		} else if (!warned) {
			warned = true;
			Log.w("RenderEffect", "only blur and offset effects are drawn");
		}
	}
}
