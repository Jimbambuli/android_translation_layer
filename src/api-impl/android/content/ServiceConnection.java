package android.content;

import android.os.IBinder;

public interface ServiceConnection {
	public void onServiceConnected(ComponentName name, IBinder service);

	/* abstract on AOSP; a default here only for ATL's own connections, apps implement it anyway */
	default void onServiceDisconnected(ComponentName name) {}

	default void onBindingDied(ComponentName name) {}

	default void onNullBinding(ComponentName name) {}
}
