package android.view.autofill;

import android.graphics.Rect;
import android.view.View;

/* ATL has no autofill service: the session calls of the app do nothing, as with autofill disabled */
public class AutofillManager {

	public static abstract class AutofillCallback {
		public static final int EVENT_INPUT_SHOWN = 1;
		public static final int EVENT_INPUT_HIDDEN = 2;
		public static final int EVENT_INPUT_UNAVAILABLE = 3;

		public void onAutofillEvent(View view, int event) {}

		public void onAutofillEvent(View view, int virtualId, int event) {}
	}

	public interface AutofillClient {}

	public void registerCallback(AutofillCallback callback) {}

	public void unregisterCallback(AutofillCallback callback) {}

	public void notifyViewEntered(View view, int id, Rect bounds) {}

	public void notifyValueChanged(View view) {}

	public void notifyValueChanged(View view, int id, AutofillValue value) {}

	public void notifyViewExited(View view, int id) {}

	public void notifyViewVisibilityChanged(View view, boolean isVisible) {}

	public void notifyViewVisibilityChanged(View view, int virtualId, boolean isVisible) {}

	public void requestAutofill(View view) {}

	public void requestAutofill(View view, int virtualId, Rect absBounds) {}

	public void commit() {}

	public void cancel() {}

	public boolean isEnabled() {
		return false;
	}

	/* AOSP: no autofill service is enabled, as ATL has none */
	public boolean hasEnabledAutofillServices() {
		return false;
	}

	public boolean isAutofillSupported() {
		return false;
	}
}
