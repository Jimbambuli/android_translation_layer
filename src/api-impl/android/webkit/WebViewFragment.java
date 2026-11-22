package android.webkit;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/* AOSP frameworks/base core/java/android/webkit/WebViewFragment.java (Android 16) */
@Deprecated
public class WebViewFragment extends Fragment {
	private WebView mWebView;
	private boolean mIsWebViewAvailable;

	public WebViewFragment() {
	}

	/** Called to instantiate the view. Creates and returns the WebView. */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (mWebView != null) {
			mWebView.destroy();
		}
		mWebView = new WebView(getContext());
		mIsWebViewAvailable = true;
		return mWebView;
	}

	@Override
	public void onPause() {
		super.onPause();
		mWebView.onPause();
	}

	@Override
	public void onResume() {
		mWebView.onResume();
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		mIsWebViewAvailable = false;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
		super.onDestroy();
	}

	public WebView getWebView() {
		return mIsWebViewAvailable ? mWebView : null;
	}
}
