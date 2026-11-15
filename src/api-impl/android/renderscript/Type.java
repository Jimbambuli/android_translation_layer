package android.renderscript;

/* AOSP Type, reduced to 1D and 2D types of an Element */
public class Type extends BaseObj {
	private final Element mElement;
	private final int mX;
	private final int mY;

	private Type(RenderScript rs, Element e, int x, int y) {
		super(rs);
		mElement = e;
		mX = x;
		mY = y;
	}

	public Element getElement() {
		return mElement;
	}

	public int getX() {
		return mX;
	}

	public int getY() {
		return mY;
	}

	public int getCount() {
		return mX * Math.max(mY, 1);
	}

	public static class Builder {
		private final RenderScript mRS;
		private final Element mElement;
		private int mX = 1;
		private int mY;

		public Builder(RenderScript rs, Element e) {
			e.checkValid();
			mRS = rs;
			mElement = e;
		}

		public Builder setX(int value) {
			if (value < 1) {
				throw new RSIllegalArgumentException("Values of less than 1 for Dimension X are not valid.");
			}
			mX = value;
			return this;
		}

		public Builder setY(int value) {
			if (value < 1) {
				throw new RSIllegalArgumentException("Values of less than 1 for Dimension Y are not valid.");
			}
			mY = value;
			return this;
		}

		public Type create() {
			return new Type(mRS, mElement, mX, mY);
		}
	}
}
