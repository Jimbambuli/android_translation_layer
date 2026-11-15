package android.os;

import java.util.Objects;

/* AOSP frameworks/base/core/java/android/os/PerformanceHintManager.java: PerformanceHintManager.create() throws
 * ServiceNotFoundException when there is no performance hint HAL (nativeAcquireManager() == 0), and the service
 * registry then hands out null. ATL has no such HAL and registers no service, so apps get null from
 * getSystemService() and never hold an instance. The class is here so that code referring to it links. */
public final class PerformanceHintManager {
	private PerformanceHintManager() {}

	public long getPreferredUpdateRateNanos() {
		return -1;
	}

	/* nativeCreateSession() returns 0 without a HAL session, which createHintSession() turns into null */
	public Session createHintSession(int[] tids, long initialTargetWorkDurationNanos) {
		Objects.requireNonNull(tids, "tids cannot be null");
		if (tids.length == 0) {
			throw new IllegalArgumentException("thread id list can't be empty.");
		}
		if (initialTargetWorkDurationNanos <= 0) {
			throw new IllegalArgumentException("the hint target duration should be positive.");
		}
		return null;
	}

	public static class Session implements java.io.Closeable {
		private Session() {}

		public void updateTargetWorkDuration(long targetDurationNanos) {}

		public void reportActualWorkDuration(long actualDurationNanos) {}

		public void close() {}
	}
}
