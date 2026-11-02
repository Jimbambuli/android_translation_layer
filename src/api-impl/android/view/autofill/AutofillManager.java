package android.view.autofill;

import android.graphics.Rect;
import android.view.View;

public class AutofillManager {

	public static abstract class AutofillCallback {}

	public interface AutofillClient {}

	public void registerCallback(AutofillCallback callback) {}

	public void unregisterCallback(AutofillCallback callback) {}

	public void notifyViewEntered(View view, int id, Rect bounds) {}

	public void notifyValueChanged(View view) {}

	public void notifyValueChanged(View view, int id, AutofillValue value) {}

	public void notifyViewExited(View view, int id) {}

	public void notifyViewVisibilityChanged(View view, boolean isVisible) {}

	public void notifyViewVisibilityChanged(View view, int virtualId, boolean isVisible) {}
}
