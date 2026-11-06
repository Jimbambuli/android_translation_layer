package android.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

	/* the parts of AOSP's Fragment that a DialogFragment (and apps overriding it) call; the lifecycle
	 * itself is driven by the Activity (onCreate .. onDestroy) or, for a DialogFragment, by show()/dismiss() */
	Bundle arguments;
	View view;

	public void onAttach(Activity activity) {
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
	}

	public void onActivityCreated(Bundle savedInstanceState) {
	}

	public void onSaveInstanceState(Bundle outState) {
	}

	public void onDestroyView() {
	}

	public void onDetach() {
	}

	public void setArguments(Bundle args) {
		arguments = args;
	}

	public final Bundle getArguments() {
		return arguments;
	}

	public View getView() {
		return view;
	}

	public LayoutInflater getLayoutInflater() {
		return activity != null ? activity.getLayoutInflater() : null;
	}

	public Activity getActivity() {
		return activity;
	}

	public final String getTag() {
		return tag;
	}
}
