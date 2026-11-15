package android.widget.inline;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceControl;
import android.view.ViewGroup;

/* AOSP InlineContentView (frameworks/base/core/java/android/widget/inline/InlineContentView.java): the view that shows the
 * content of an inline suggestion of an autofill/IME service. ATL has no such service, so no surface package is ever handed
 * to it and the view stays empty: it keeps its clip bounds and callback, getSurfaceControl() is null (as AOSP before the
 * surface exists). */
public class InlineContentView extends ViewGroup {
	public interface SurfaceControlCallback {
		void onCreated(SurfaceControl surfaceControl);

		void onDestroyed(SurfaceControl surfaceControl);
	}

	private SurfaceControlCallback mSurfaceControlCallback;

	public InlineContentView(Context context) {
		this(context, null);
	}

	public InlineContentView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public InlineContentView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public InlineContentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
	}

	public SurfaceControl getSurfaceControl() {
		return null;
	}

	public void setSurfaceControlCallback(SurfaceControlCallback callback) {
		mSurfaceControlCallback = callback;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// The surface of the content fills this view; there is none here
	}
}
