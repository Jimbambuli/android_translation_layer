package android.graphics;

public class BlurMaskFilter extends MaskFilter {

	/* AOSP BlurMaskFilter.Blur, in AOSP order (native values 0-3) */
	public static enum Blur {
		NORMAL,
		SOLID,
		OUTER,
		INNER,
	}
	;

	public BlurMaskFilter(float radius, Blur blur) {}
}
