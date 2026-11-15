package android.content;

import android.accounts.Account;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public abstract class AbstractThreadedSyncAdapter {
	private final Context context;
	private final SyncAdapterBinder binder = new SyncAdapterBinder();

	public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize) {
		this(context, autoInitialize, false);
	}

	public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	public final IBinder getSyncAdapterBinder() {
		return binder;
	}

	public abstract void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult);

	public void onSyncCanceled() {}

	/* AOSP AbstractThreadedSyncAdapter */
	public void onSyncCanceled(Thread thread) {
		thread.interrupt();
	}

	public boolean onUnsyncableAccount() {
		return true;
	}

	public void onSecurityException(Account account, Bundle extras, String authority, android.content.SyncResult syncResult) {
	}

	/* returned from the sync adapter service's onBind, so ContentResolver.requestSync can get to the adapter */
	class SyncAdapterBinder extends Binder {
		AbstractThreadedSyncAdapter getSyncAdapter() {
			return AbstractThreadedSyncAdapter.this;
		}
	}
}
