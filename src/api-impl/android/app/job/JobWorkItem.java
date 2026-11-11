package android.app.job;

import android.content.Intent;

final public class JobWorkItem {
	private final Intent mIntent;
	int mDeliveryCount;
	int mWorkId;

	public JobWorkItem(Intent intent) {
		mIntent = intent;
	}

	public Intent getIntent() {
		return mIntent;
	}

	/* AOSP JobWorkItem.getDeliveryCount(): how often the item was handed to dequeueWork() */
	public int getDeliveryCount() {
		return mDeliveryCount;
	}
}
