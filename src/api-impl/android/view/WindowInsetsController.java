package android.view;

import android.view.WindowInsets.Type.InsetsType;

public interface WindowInsetsController {
	public static final int BEHAVIOR_DEFAULT = 1;
	public static final int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

	void setSystemBarsAppearance(int appearance, int mask);
	int getSystemBarsAppearance();
	void setSystemBarsBehavior(int behavior);
	int getSystemBarsBehavior();
	void show(@InsetsType int types);
	void hide(@InsetsType int types);
}
