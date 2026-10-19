package android.view;

import android.graphics.SurfaceTexture;

public class Surface {
	public long widget;

	public Surface() {}

	public Surface(SurfaceTexture surfaceTexture) {}

	public boolean isValid() {
		return widget != 0;
	}

	public void release() {}

	/* API 30/31: only a hint for the display's refresh rate, which ATL does not adjust */
	public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0;
	public static final int FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1;
	public static final int CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS = 0;
	public static final int CHANGE_FRAME_RATE_ALWAYS = 1;

	public void setFrameRate(float frameRate, int compatibility) {}

	public void setFrameRate(float frameRate, int compatibility, int changeFrameRateStrategy) {}
}
