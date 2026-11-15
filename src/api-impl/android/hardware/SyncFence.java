package android.hardware;

import java.time.Duration;

/* AOSP core/java/android/hardware/SyncFence.java for the fences ATL can have: only the empty fence of
 * createEmpty() (ATL has no native sync fences, so mNativePtr is always 0). The methods are AOSP's paths
 * for mNativePtr == 0. */
public final class SyncFence implements AutoCloseable {
	public static final long SIGNAL_TIME_INVALID = -1;
	public static final long SIGNAL_TIME_PENDING = Long.MAX_VALUE;

	private final Object mLock = new Object();

	private SyncFence() {
	}

	/* AOSP SyncFence(SyncFence): copy of a fence; ATL fences are always empty (mNativePtr == 0), so
	 * there is no native reference to increment */
	public SyncFence(SyncFence other) {
	}

	public static SyncFence createEmpty() {
		return new SyncFence();
	}

	public boolean isValid() {
		return false;
	}

	public boolean await(Duration timeout) {
		return false;
	}

	public boolean awaitForever() {
		return false;
	}

	public long getSignalTime() {
		return SIGNAL_TIME_INVALID;
	}

	@Override
	public void close() {
	}

	public Object getLock() {
		return mLock;
	}
}
