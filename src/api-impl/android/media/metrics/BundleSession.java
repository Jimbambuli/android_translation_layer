package android.media.metrics;

import java.util.Objects;

/* AOSP media/java/android/media/metrics/BundleSession.java (android-16), like EditingSession */
public final class BundleSession implements AutoCloseable {
	private final String mId;
	private final MediaMetricsManager mManager;
	private final LogSessionId mLogSessionId;

	public static final String KEY_STATSD_ATOM = "bundlesession-statsd-atom";

	public BundleSession(String id, MediaMetricsManager manager) {
		mId = Objects.requireNonNull(id);
		mManager = Objects.requireNonNull(manager);
		mLogSessionId = new LogSessionId(mId);
	}

	public void reportBundleMetrics(android.os.PersistableBundle metrics) {
		mManager.reportBundleMetrics(mId, metrics);
	}

	public LogSessionId getSessionId() {
		return mLogSessionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BundleSession that = (BundleSession) o;
		return Objects.equals(mId, that.mId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mId);
	}

	@Override
	public void close() {
		mManager.releaseSessionId(mLogSessionId.getStringId());
	}
}
