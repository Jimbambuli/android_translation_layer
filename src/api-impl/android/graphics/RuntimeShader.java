package android.graphics;

import java.util.HashMap;

/* AOSP graphics/java/android/graphics/RuntimeShader.java (API 33): an AGSL shader whose uniforms and child shaders are
 * set by name. AOSP hands the AGSL source to Skia's SkRuntimeEffect (which also rejects unknown uniform names and
 * wrong sizes); ATL has no SkSL/AGSL compiler, so the program and the values are kept and the shader draws nothing,
 * like any Shader that Canvas does not know. */
public class RuntimeShader extends Shader {

	private final String mShader;
	private final HashMap<String, Object> mUniforms = new HashMap<>();
	private final HashMap<String, Object> mInputs = new HashMap<>();

	public RuntimeShader(String shader) {
		if (shader == null) {
			throw new NullPointerException("RuntimeShader requires a non-null AGSL string");
		}
		mShader = shader;
	}

	public void setColorUniform(String uniformName, int color) {
		setUniform(uniformName, new int[] {color});
	}

	public void setColorUniform(String uniformName, long color) {
		setUniform(uniformName, new long[] {color});
	}

	public void setColorUniform(String uniformName, Color color) {
		if (color == null) {
			throw new NullPointerException("The color parameter must not be null");
		}
		setUniform(uniformName, new float[] {color.red(), color.green(), color.blue(), color.alpha()});
	}

	public void setFloatUniform(String uniformName, float value) {
		setUniform(uniformName, new float[] {value});
	}

	public void setFloatUniform(String uniformName, float value1, float value2) {
		setUniform(uniformName, new float[] {value1, value2});
	}

	public void setFloatUniform(String uniformName, float value1, float value2, float value3) {
		setUniform(uniformName, new float[] {value1, value2, value3});
	}

	public void setFloatUniform(String uniformName, float value1, float value2, float value3, float value4) {
		setUniform(uniformName, new float[] {value1, value2, value3, value4});
	}

	public void setFloatUniform(String uniformName, float[] values) {
		setUniform(uniformName, values);
	}

	public void setIntUniform(String uniformName, int value) {
		setUniform(uniformName, new int[] {value});
	}

	public void setIntUniform(String uniformName, int value1, int value2) {
		setUniform(uniformName, new int[] {value1, value2});
	}

	public void setIntUniform(String uniformName, int value1, int value2, int value3) {
		setUniform(uniformName, new int[] {value1, value2, value3});
	}

	public void setIntUniform(String uniformName, int value1, int value2, int value3, int value4) {
		setUniform(uniformName, new int[] {value1, value2, value3, value4});
	}

	public void setIntUniform(String uniformName, int[] values) {
		setUniform(uniformName, values);
	}

	public void setInputShader(String shaderName, Shader shader) {
		if (shaderName == null) {
			throw new NullPointerException("The shaderName parameter must not be null");
		}
		if (shader == null) {
			throw new NullPointerException("The shader parameter must not be null");
		}
		mInputs.put(shaderName, shader);
	}

	public void setInputBuffer(String shaderName, BitmapShader bitmapShader) {
		if (shaderName == null) {
			throw new NullPointerException("The shaderName parameter must not be null");
		}
		if (bitmapShader == null) {
			throw new NullPointerException("The bitmapShader parameter must not be null");
		}
		mInputs.put(shaderName, bitmapShader);
	}

	private void setUniform(String uniformName, Object values) {
		if (uniformName == null) {
			throw new NullPointerException("The uniformName parameter must not be null");
		}
		if (values == null) {
			throw new NullPointerException("The uniform values parameter must not be null");
		}
		mUniforms.put(uniformName, values);
	}
}
