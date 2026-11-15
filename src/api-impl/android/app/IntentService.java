package android.app;

import android.content.Intent;
import android.os.IBinder;

public abstract class IntentService extends Service {
	public IntentService(String name) {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId); // AOSP IntentService.onStartCommand()
		return 0;
	}

	/* AOSP IntentService.onStart() hands the intent to onHandleIntent() (there on the worker thread, here
	 * in place); subclasses overriding onStart() are called as on Android */
	@Override
	public void onStart(Intent intent, int startId) {
		this.onHandleIntent(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected abstract void onHandleIntent(Intent intent);
}
