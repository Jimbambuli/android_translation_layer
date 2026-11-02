package android.app;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

public class ActivityOptions {

	public static ActivityOptions makeSceneTransitionAnimation(Activity activity, Pair<View, String>... pairs) {
		return new ActivityOptions();
	}

	public static ActivityOptions makeSceneTransitionAnimation(Activity activity, View view, String name) {
		return new ActivityOptions();
	}

	/**
	 * Create an ActivityOptions specifying a custom animation to run when
	 * the activity is displayed.
	 */
	public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId) {
		return makeCustomAnimation(context, enterResId, exitResId, 0);
	}

	/**
	 * Create an ActivityOptions specifying a custom animation to run when
	 * the activity is displayed.
	 */
	public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId, int backgroundColor) {
		/* activity transitions are not animated */
		return new ActivityOptions();
	}

	public static ActivityOptions makeBasic() {
		return new ActivityOptions();
	}

	public static ActivityOptions makeScaleUpAnimation(View source, int startX, int startY, int startWidth, int startHeight) {
		return new ActivityOptions();
	}

	public Bundle toBundle() {
		return null;
	}
}
