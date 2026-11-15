package android.renderscript;

/* AOSP Element, reduced to the unsigned 8 bit data the blur works on: U8 (1 byte), U8_4 and RGBA_8888 (4 bytes) */
public class Element extends BaseObj {
	private final int mVectorSize;
	private final boolean mPixelRGBA;

	private Element(RenderScript rs, int vectorSize, boolean pixelRGBA) {
		super(rs);
		mVectorSize = vectorSize;
		mPixelRGBA = pixelRGBA;
	}

	public static Element U8(RenderScript rs) {
		return new Element(rs, 1, false);
	}

	public static Element U8_4(RenderScript rs) {
		return new Element(rs, 4, false);
	}

	public static Element RGBA_8888(RenderScript rs) {
		return new Element(rs, 4, true);
	}

	public int getBytesSize() {
		return mVectorSize;
	}

	public int getVectorSize() {
		return mVectorSize;
	}

	/* AOSP Element.isCompatible(): the same data type and vector size (a pixel kind does not matter) */
	public boolean isCompatible(Element e) {
		return mVectorSize == e.mVectorSize;
	}
}
