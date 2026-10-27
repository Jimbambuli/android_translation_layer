package android.view;

import android.annotation.IntDef;
import android.graphics.SurfaceTexture;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Surface {

	/** @hide */
	@IntDef(prefix = {"ROTATION_"}, value = {ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Rotation {}

	/** Rotation constant: 0 degree rotation (natural orientation) */
	public static final int ROTATION_0 = 0;

	/** Rotation constant: 90 degree rotation. */
	public static final int ROTATION_90 = 1;

	/** Rotation constant: 180 degree rotation. */
	public static final int ROTATION_180 = 2;

	/** Rotation constant: 270 degree rotation. */
	public static final int ROTATION_270 = 3;

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
