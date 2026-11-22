package android.webkit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/* Chromium's FullScreenView (android_webview): the view that holds the page while it is in fullscreen; the
 * WebView puts it into a FrameLayout and hands that to WebChromeClient.onShowCustomView(). Input goes to the page
 * as in the WebView; Back ends the fullscreen (FullScreenView.dispatchKeyEvent(): ACTION_UP of KEYCODE_BACK ->
 * AwContents.requestExitFullscreen()). */
final class AtlFullScreenView extends View {
	private final WebView webView;

	AtlFullScreenView(Context context, WebView webView) {
		super(context);
		this.webView = webView;
		setFocusable(true);
		setFocusableInTouchMode(true);
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return native_gtkEventPropagation();
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_SCROLL)
			return native_gtkScrollTarget(widget);
		return super.onGenericMotionEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
			webView.requestExitFullscreen();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}
}
