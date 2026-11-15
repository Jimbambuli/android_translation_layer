package android.graphics;

/* AOSP graphics/java/android/graphics/LightingColorFilter.java: the Java side (mMul, mAdd and their accessors); like
 * ColorFilter itself it has no native Skia filter behind it in ATL */
public class LightingColorFilter extends ColorFilter {
	private int mMul;
	private int mAdd;

	public LightingColorFilter(int mul, int add) {
		mMul = mul;
		mAdd = add;
	}

	public int getColorMultiply() {
		return mMul;
	}

	public void setColorMultiply(int mul) {
		mMul = mul;
	}

	public int getColorAdd() {
		return mAdd;
	}

	public void setColorAdd(int add) {
		mAdd = add;
	}
}
