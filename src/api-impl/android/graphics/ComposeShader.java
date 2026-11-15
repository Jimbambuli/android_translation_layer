package android.graphics;

public class ComposeShader extends Shader {

	/* AOSP ComposeShader(Shader, Shader, Xfermode) (deprecated): like the other forms, not drawn by ATL */
	public ComposeShader(Shader shaderA, Shader shaderB, Xfermode mode) {
		if (shaderA == null || shaderB == null)
			throw new IllegalArgumentException("Shader parameters must not be null");
	}

	public ComposeShader(Shader shader1, Shader shader2, PorterDuff.Mode mode) {}

	/* AOSP ComposeShader(Shader, Shader, BlendMode); like the PorterDuff.Mode form not drawn by ATL */
	public ComposeShader(Shader dst, Shader src, BlendMode blendMode) {}
}
