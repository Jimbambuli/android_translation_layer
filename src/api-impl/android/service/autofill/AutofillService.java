package android.service.autofill;

import android.app.Service;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.util.Log;

/* AOSP AutofillService minus the IAutoFillService stub: the system's autofill manager never binds a service in
 * ATL, so the handler message dispatch to onFillRequest/onSaveRequest/onConnected/onDisconnected has no caller */
public abstract class AutofillService extends Service {

	private static final String TAG = "AutofillService";

	public static final String SERVICE_INTERFACE = "android.service.autofill.AutofillService";
	public static final String SERVICE_META_DATA = "android.autofill";
	public static final String EXTRA_FILL_RESPONSE = "android.service.autofill.extra.FILL_RESPONSE";
	public static final String EXTRA_RESULT = "result";
	public static final String EXTRA_ERROR = "error";
	public static final String WEBVIEW_REQUESTED_CREDENTIAL_KEY = "webview_requested_credential";

	public AutofillService() {
	}

	/* AOSP AutofillService.onCreate (the Handler/defuse setup only serves the stub) */
	@Override
	public void onCreate() {
		super.onCreate();
	}

	/* AOSP AutofillService.onBind: kein IAutofillService-Stub, niemand bindet. Same warning as AOSP for a wrong action */
	@Override
	public final IBinder onBind(Intent intent) {
		if (SERVICE_INTERFACE.equals(intent.getAction())) {
			return null;
		}
		Log.w(TAG, "Tried to bind to wrong intent (should be " + SERVICE_INTERFACE + ": " + intent);
		return null;
	}

	public void onConnected() {
	}

	public abstract void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback);

	public abstract void onSaveRequest(SaveRequest request, SaveCallback callback);

	/* AOSP AutofillService.onSavedDatasetsInfoRequest */
	public void onSavedDatasetsInfoRequest(SavedDatasetsInfoCallback callback) {
		callback.onError(SavedDatasetsInfoCallback.ERROR_UNSUPPORTED);
	}

	public void onDisconnected() {
	}
}
