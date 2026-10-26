package android.view;

import android.os.IBinder;

public interface WindowManager {
	public android.view.Display getDefaultDisplay();

	public void addView(View view, ViewGroup.LayoutParams params);

	public void updateViewLayout(View view, ViewGroup.LayoutParams params);

	public void removeView(View view);

	public void removeViewImmediate(View view);

	public class LayoutParams extends ViewGroup.LayoutParams {
		public static final int FLAG_KEEP_SCREEN_ON = 0;
		public static final int FLAG_DIM_BEHIND = 2;
		public static final int FLAG_NOT_FOCUSABLE = 8;
		public static final int FLAG_NOT_TOUCH_MODAL = 32;
		public static final int FLAG_LAYOUT_IN_SCREEN = 0x00000100;
		public static final int FLAG_FULLSCREEN = 0x00000400;
		public static final int FLAG_LAYOUT_INSET_DECOR = 0x00010000;
		public static final int FLAG_SHOW_WALLPAPER = 0x00100000;
		public static final int FLAG_SPLIT_TOUCH = 0x00800000;
		public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
		public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
		public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;

		public static final int SOFT_INPUT_STATE_UNSPECIFIED = 0;

		public float screenBrightness = -1;
		public int softInputMode;
		public int x;
		public int y;
		public int windowAnimations;
		public int flags;
		public float dimAmount = 1.0f;
		public float alpha;
		public int type;
		public IBinder token;
		public int format;
		public int layoutInDisplayCutoutMode;
		public String packageName;
		public int rotationAnimation;

		public LayoutParams(int w, int h, int type, int flags, int format) {
			super(w, h);
		}

		public LayoutParams(int type) {
			super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			this.type = type;
		}

		public LayoutParams() {}

		public void setTitle(CharSequence title) {}

		private int colorMode;
		public void setColorMode(int colorMode) { this.colorMode = colorMode; }
		public int getColorMode() { return colorMode; }

		public boolean preferMinimalPostProcessing;
	}
}
