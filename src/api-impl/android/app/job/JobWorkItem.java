package android.app.job;

import android.content.Intent;

final public class JobWorkItem {
	private final Intent mIntent;

	public JobWorkItem(Intent intent) {
		mIntent = intent;
	}

	public Intent getIntent() {
		return mIntent;
	}
}
