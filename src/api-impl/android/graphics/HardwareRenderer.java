package android.graphics;

public class HardwareRenderer {
	public static void setContextPriority(int priority) {}

	/* AOSP HardwareRenderer.setLightSourceGeometry()/setLightSourceAlpha(): the argument checks; the light only feeds
	 * the native shadow drawing (nSetLightGeometry), which ATL does not do */
	public void setLightSourceGeometry(float lightX, float lightY, float lightZ, float lightRadius) {
		validateFinite(lightX, "lightX");
		validateFinite(lightY, "lightY");
		validatePositive(lightZ, "lightZ");
		validatePositive(lightRadius, "lightRadius");
	}

	public void setLightSourceAlpha(float ambientShadowAlpha, float spotShadowAlpha) {
		validateAlpha(ambientShadowAlpha, "ambientShadowAlpha");
		validateAlpha(spotShadowAlpha, "spotShadowAlpha");
	}

	private static void validateAlpha(float alpha, String argumentName) {
		if (!(alpha >= 0.0f && alpha <= 1.0f)) {
			throw new IllegalArgumentException(argumentName + " must be a valid alpha, "
			                                   + alpha + " is not in the range of 0.0f to 1.0f");
		}
	}

	private static void validatePositive(float f, String argumentName) {
		if (!(Float.isFinite(f) && f >= 0.0f)) {
			throw new IllegalArgumentException(argumentName
			                                   + " must be a finite positive, given=" + f);
		}
	}

	private static void validateFinite(float f, String argumentName) {
		if (!Float.isFinite(f)) {
			throw new IllegalArgumentException(argumentName + " must be finite, given=" + f);
		}
	}
}
