package android.media.metrics;

import java.util.Objects;

/* AOSP media/java/android/media/metrics/RecordingSession.java (android-16), like EditingSession */
public final class RecordingSession implements AutoCloseable {
	private final String mId;
	private final MediaMetricsManager mManager;
	private final LogSessionId mLogSessionId;
	private boolean mClosed = false;

	public RecordingSession(String id, MediaMetricsManager manager) {
		mId = Objects.requireNonNull(id);
		mManager = Objects.requireNonNull(manager);
		mLogSessionId = new LogSessionId(mId);
	}

	public LogSessionId getSessionId() {
		return mLogSessionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RecordingSession that = (RecordingSession) o;
		return Objects.equals(mId, that.mId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mId);
	}

	@Override
	public void close() {
		mClosed = true;
		mManager.releaseSessionId(mLogSessionId.getStringId());
	}
}
