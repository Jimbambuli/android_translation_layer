package android.renderscript;

/* AOSP Script: the base of the intrinsics */
public abstract class Script extends BaseObj {
	Script(RenderScript rs) {
		super(rs);
	}

	/* AOSP Script.LaunchOptions: the sub-range of the output a kernel runs over; an end of 0 means the whole extent */
	public static final class LaunchOptions {
		private int xstart = 0;
		private int ystart = 0;
		private int xend = 0;
		private int yend = 0;
		private int zstart = 0;
		private int zend = 0;

		public LaunchOptions setX(int xstartArg, int xendArg) {
			if (xstartArg < 0 || xendArg <= xstartArg) {
				throw new RSIllegalArgumentException("Invalid dimensions");
			}
			xstart = xstartArg;
			xend = xendArg;
			return this;
		}

		public LaunchOptions setY(int ystartArg, int yendArg) {
			if (ystartArg < 0 || yendArg <= ystartArg) {
				throw new RSIllegalArgumentException("Invalid dimensions");
			}
			ystart = ystartArg;
			yend = yendArg;
			return this;
		}

		public LaunchOptions setZ(int zstartArg, int zendArg) {
			if (zstartArg < 0 || zendArg <= zstartArg) {
				throw new RSIllegalArgumentException("Invalid dimensions");
			}
			zstart = zstartArg;
			zend = zendArg;
			return this;
		}

		public int getXStart() {
			return xstart;
		}

		public int getXEnd() {
			return xend;
		}

		public int getYStart() {
			return ystart;
		}

		public int getYEnd() {
			return yend;
		}

		public int getZStart() {
			return zstart;
		}

		public int getZEnd() {
			return zend;
		}
	}
}
