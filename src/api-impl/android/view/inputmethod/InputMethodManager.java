package android.view.inputmethod;

import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class InputMethodManager {

	private static long im_context = nativeInit();
	private static View activeView = null;
	private static InputConnection activeConnection = null;

	private ArrayList<InputMethodInfo> input_method_list = new ArrayList<InputMethodInfo>();

	public boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
		nativeHideSoftInput(im_context);
		activeView = null;
		return false;
	}

	public boolean showSoftInput(View view, int flags) {
		if (view == activeView) {
			return nativeShowSoftInput(im_context, view.widget, null, 0);
		}
		return startInput(view);
	}

	/* like AOSP's startInputInner(): the view fills a fresh EditorInfo and hands out a new connection, the old
	 * connection is closed */
	private boolean startInput(View view) {
		EditorInfo outAttrs = new EditorInfo();
		outAttrs.packageName = view.getContext().getPackageName();
		outAttrs.fieldId = view.getId();
		InputConnection ic = view.onCreateInputConnection(outAttrs);
		if (ic != null) {
			if (activeConnection != null)
				activeConnection.closeConnection();
			activeView = view;
			activeConnection = ic;
			return nativeShowSoftInput(im_context, view.widget, ic, outAttrs.inputType);
		}
		return false;
	}

	public boolean isFullscreenMode() { return false; }

	public boolean isActive(View view) {
		return activeView == view;
	}

	public List<InputMethodInfo> getEnabledInputMethodList() {
		return input_method_list;
	}

	public List<InputMethodInfo> getInputMethodList() {
		return input_method_list;
	}

	public void restartInput(View view) {
		if (view == activeView)
			startInput(view);
	}

	public void updateSelection(View view, int selStart, int selEnd, int candidatesStart, int candidatesEnd) {}

	public InputMethodSubtype getCurrentInputMethodSubtype() {
		return new InputMethodSubtype();
	}

	public void updateCursorAnchorInfo(View view, CursorAnchorInfo cursorAnchorInfo) {}

	public void updateExtractedText(View view, int token, ExtractedText text) {}

	public void startStylusHandwriting(View view) {}

	public void dispatchKeyEventFromInputMethod(View targetView, KeyEvent event) {
		if (targetView != null)
			targetView.dispatchKeyEvent(event);
	}

	private static native long nativeInit();
	private native boolean nativeShowSoftInput(long im_context, long widget, InputConnection ic, int inputType);
	private native void nativeHideSoftInput(long im_context);
}
