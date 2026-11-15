package android.security.advancedprotection;

import android.annotation.NonNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * android-16 frameworks/base core/java/android/security/advancedprotection/AdvancedProtectionManager.java.
 * The AdvancedProtectionService of the system server is the only source of the state; a device without it
 * (ATL) is a device on which Advanced Protection mode was never switched on: isAdvancedProtectionEnabled()
 * is false and registered callbacks are never called.
 */
public final class AdvancedProtectionManager {

	private final ConcurrentHashMap<Callback, Executor> mCallbackMap = new ConcurrentHashMap<>();

	public AdvancedProtectionManager() {}

	public boolean isAdvancedProtectionEnabled() {
		return false;
	}

	public void registerAdvancedProtectionCallback(@NonNull Executor executor, @NonNull Callback callback) {
		mCallbackMap.putIfAbsent(callback, executor);
	}

	public void unregisterAdvancedProtectionCallback(@NonNull Callback callback) {
		mCallbackMap.remove(callback);
	}

	public interface Callback {
		void onAdvancedProtectionChanged(boolean enabled);
	}
}
