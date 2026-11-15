package android.view.inputmethod;

import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class InputMethodManager {

	private static long im_context = nativeInit();
	private static View activeView = null;
	private static InputConnection activeConnection = null;

	private ArrayList<InputMethodInfo> input_method_list = new ArrayList<InputMethodInfo>();

	/* AOSP InputMethodManager.RESULT_*: what the ResultReceiver of show/hide hears */
	public static final int RESULT_UNCHANGED_SHOWN = 0;
	public static final int RESULT_UNCHANGED_HIDDEN = 1;
	public static final int RESULT_SHOWN = 2;
	public static final int RESULT_HIDDEN = 3;

	public boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
		return hideSoftInputFromWindow(windowToken, flags, null);
	}

	public boolean hideSoftInputFromWindow(IBinder windowToken, int flags, ResultReceiver resultReceiver) {
		boolean wasShown = activeView != null;
		nativeHideSoftInput(im_context);
		activeView = null;
		if (resultReceiver != null)
			resultReceiver.send(wasShown ? RESULT_HIDDEN : RESULT_UNCHANGED_HIDDEN, null);
		return wasShown;
	}

	public boolean showSoftInput(View view, int flags) {
		return showSoftInput(view, flags, null);
	}

	public boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
		boolean alreadyShown = view == activeView;
		boolean shown = alreadyShown ? nativeShowSoftInput(im_context, view.widget, null, 0) : startInput(view);
		if (resultReceiver != null)
			resultReceiver.send(shown ? (alreadyShown ? RESULT_UNCHANGED_SHOWN : RESULT_SHOWN) : RESULT_UNCHANGED_HIDDEN, null);
		return shown;
	}

	/* AOSP InputMethodManager.toggleSoftInput(): hides the keyboard if it is shown (with hideFlags); showing it needs
	 * a view that is served, which the call does not name - with none active nothing is shown */
	@Deprecated
	public void toggleSoftInput(int showFlags, int hideFlags) {
		if (activeView != null)
			hideSoftInputFromWindow(null, hideFlags);
	}

	/* AOSP InputMethodManager.toggleSoftInputFromWindow(): only for the window of the served view */
	@Deprecated
	public void toggleSoftInputFromWindow(IBinder windowToken, int showFlags, int hideFlags) {
		if (activeView == null || activeView.getWindowToken() != windowToken)
			return;
		toggleSoftInput(showFlags, hideFlags);
	}

	/* AOSP: the IME-only calls go through InputMethodPrivilegedOperationsRegistry.get(imeToken); an app is no
	 * input method, so its token is unknown and the registry's empty operations do nothing / answer false */
	public boolean switchToLastInputMethod(IBinder imeToken) {
		return false;
	}

	public boolean switchToNextInputMethod(IBinder imeToken, boolean onlyCurrentIme) {
		return false;
	}

	public boolean shouldOfferSwitchingToNextInputMethod(IBinder imeToken) {
		return false;
	}

	public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {}

	/* AOSP InputMethodManager.getLastInputMethodSubtype(): none was used before (no Android IMEs) */
	public InputMethodSubtype getLastInputMethodSubtype() {
		return null;
	}

	/* AOSP InputMethodManager.getShortcutInputMethodsAndSubtypes(): the first voice subtype of the enabled
	 * input methods; the list of enabled input methods is empty here */
	public java.util.Map<InputMethodInfo, List<InputMethodSubtype>> getShortcutInputMethodsAndSubtypes() {
		return java.util.Collections.emptyMap();
	}

	/* AOSP InputMethodManager.isActive(): some view has an input connection with the input method */
	public boolean isActive() {
		return activeView != null;
	}

	/* AOSP InputMethodManager.isAcceptingText(): the served view has a connection that takes text */
	public boolean isAcceptingText() {
		return activeView != null && activeConnection != null;
	}

	/* AOSP displayCompletions(): shows the completions in the input method's candidate view; GTK's input method
	 * has none (ATL shows the drop-down of an AutoCompleteTextView itself), so there is nothing to show them in */
	@Deprecated
	public void displayCompletions(View view, CompletionInfo[] completions) {
	}

	/* AOSP showInputMethodPicker(): the system's switcher of input methods; the desktop's input method is GTK's
	 * (its own context menu), there is no picker to show */
	public void showInputMethodPicker() {
	}

	public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(InputMethodInfo imi, boolean allowsImplicitlySelectedSubtypes) {
		return new ArrayList<InputMethodSubtype>();
	}

	/* AOSP stylus handwriting delegation: handwriting is not available here, no delegation can be accepted */
	public void prepareStylusHandwritingDelegation(View delegatorView) {
		if (delegatorView == null)
			throw new NullPointerException("delegatorView");
	}

	public void prepareStylusHandwritingDelegation(View delegatorView, String delegatePackageName) {
		if (delegatePackageName == null)
			throw new NullPointerException("delegatePackageName");
		prepareStylusHandwritingDelegation(delegatorView);
	}

	public boolean acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName) {
		if (delegatorPackageName == null)
			throw new NullPointerException("delegatorPackageName");
		return acceptStylusHandwritingDelegation(delegateView);
	}

	/* AOSP: the callback is called with false when no handwriting can be started */
	public void acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName, java.util.concurrent.Executor executor, java.util.function.Consumer<Boolean> callback) {
		acceptStylusHandwritingDelegation(delegateView, delegatorPackageName, 0, executor, callback);
	}

	public void acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName, int flags, java.util.concurrent.Executor executor, java.util.function.Consumer<Boolean> callback) {
		if (delegatorPackageName == null || delegateView == null || executor == null || callback == null)
			throw new NullPointerException();
		executor.execute(() -> callback.accept(false));
	}

	public boolean acceptStylusHandwritingDelegation(View delegatorView) {
		if (delegatorView == null)
			throw new NullPointerException("delegatorView");
		return false;
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

	/* AOSP: the input method uses the whole screen for itself; GTK's input method never does */
	public boolean isFullscreenMode() {
		return false;
	}

	public boolean isActive(View view) {
		return activeView == view;
	}

	/* AOSP InputMethodManager.hasActiveInputConnection(): the input method's connection is the view's own */
	public boolean hasActiveInputConnection(View view) {
		return view != null && activeView == view && activeConnection != null;
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

	/* AOSP InputMethodManager.isStylusHandwritingAvailable(): true only if the current IME
	 * supports stylus handwriting; ATL has no Android IME, so none does */
	public boolean isStylusHandwritingAvailable() {
		return false;
	}

	/* AOSP InputMethodManager.sendAppPrivateCommand(): forwarded to the IME session and
	 * dropped when there is none (!isImeSessionAvailableLocked()); ATL never has one */
	public void sendAppPrivateCommand(View view, String action, android.os.Bundle data) {
	}

	public void dispatchKeyEventFromInputMethod(View targetView, KeyEvent event) {
		if (targetView != null)
			targetView.dispatchKeyEvent(event);
	}

	private static native long nativeInit();
	private native boolean nativeShowSoftInput(long im_context, long widget, InputConnection ic, int inputType);
	private native void nativeHideSoftInput(long im_context);
}
