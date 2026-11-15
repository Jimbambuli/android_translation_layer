package android.graphics;

public class ComposeShader extends Shader {

	public ComposeShader(Shader shader1, Shader shader2, PorterDuff.Mode mode) {}

	/* AOSP ComposeShader(Shader, Shader, BlendMode); like the PorterDuff.Mode form not drawn by ATL */
	public ComposeShader(Shader dst, Shader src, BlendMode blendMode) {}
}
