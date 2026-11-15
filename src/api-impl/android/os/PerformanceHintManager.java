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

		/* AOSP reportActualWorkDuration(WorkDuration): the checks, then the (absent) HAL gets the report */
		public void reportActualWorkDuration(WorkDuration workDuration) {
			if (workDuration.getWorkPeriodStartTimestampNanos() <= 0) {
				throw new IllegalArgumentException("the work period start timestamp should be greater than zero.");
			}
			if (workDuration.getActualTotalDurationNanos() <= 0) {
				throw new IllegalArgumentException("the actual total duration should be greater than zero.");
			}
			if (workDuration.getActualCpuDurationNanos() < 0) {
				throw new IllegalArgumentException("the actual CPU duration should be greater than or equal to zero.");
			}
			if (workDuration.getActualGpuDurationNanos() < 0) {
				throw new IllegalArgumentException("the actual GPU duration should be greater than or equal to zero.");
			}
			if (workDuration.getActualCpuDurationNanos() + workDuration.getActualGpuDurationNanos() <= 0) {
				throw new IllegalArgumentException(
				    "either the actual CPU duration or the actual GPU duration should be greater" + "than zero.");
			}
		}

		public void close() {}
	}
}
