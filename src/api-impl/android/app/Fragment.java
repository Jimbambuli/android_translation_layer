package android.app;

import android.content.res.Configuration;
import android.os.Bundle;

public class Fragment {

	Activity activity;
	String tag;

	public void onCreate(Bundle savedInstanceState) {
	}

	public void onStart() {
	}

	/* AOSP Fragment.onPictureInPictureModeChanged() */
	@Deprecated
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
	}

	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
		onPictureInPictureModeChanged(isInPictureInPictureMode);
	}

	public void onResume() {
	}

	public void onPause() {
	}

	public void onStop() {
	}

	public void onDestroy() {
	}

	public Activity getActivity() {
		return activity;
	}

	public final String getTag() {
		return tag;
	}
}
