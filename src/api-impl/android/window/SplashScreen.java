package android.window;

import android.annotation.NonNull;
import android.annotation.StyleRes;

/* AOSP: SplashScreen of an Activity. ATL shows no splash screen. */
public interface SplashScreen {
	void setOnExitAnimationListener(@NonNull SplashScreen.OnExitAnimationListener listener);

	void clearOnExitAnimationListener();

	void setSplashScreenTheme(@StyleRes int themeId);

	interface OnExitAnimationListener {
		void onSplashScreenExit(@NonNull SplashScreenView view);
	}
}
