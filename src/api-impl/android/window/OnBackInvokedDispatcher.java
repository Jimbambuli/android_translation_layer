package android.window;

public interface OnBackInvokedDispatcher {
	int PRIORITY_DEFAULT = 0;
	int PRIORITY_OVERLAY = 1000000;
	int PRIORITY_SYSTEM = -1;

	void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback);

	void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback);
}
