package android.media.metrics;

import android.annotation.NonNull;

import java.util.UUID;

/* API 31. ATL has no media metrics service. As on AOSP without one, sessions still hand out a LogSessionId
 * (a local random id, nothing resolves it) and every report is dropped, so apps that report playback
 * metrics (media3) run unchanged. */
public final class MediaMetricsManager {
	/** Value indicating that an invalid timestamp was supplied. */
	public static final long INVALID_TIMESTAMP = -1;

	public MediaMetricsManager() {}

	public void reportPlaybackMetrics(@NonNull String sessionId, PlaybackMetrics metrics) {}

	public void reportNetworkEvent(@NonNull String sessionId, NetworkEvent event) {}

	public void reportPlaybackStateEvent(@NonNull String sessionId, PlaybackStateEvent event) {}

	public void reportTrackChangeEvent(@NonNull String sessionId, TrackChangeEvent event) {}

	public void reportPlaybackErrorEvent(@NonNull String sessionId, PlaybackErrorEvent event) {}

	/* AOSP MediaMetricsManager.reportBundleMetrics() (@hide, used by BundleSession): no service, dropped */
	public void reportBundleMetrics(@NonNull String sessionId, android.os.PersistableBundle metrics) {}

	public void releaseSessionId(@NonNull String sessionId) {}

	public void reportEditingEndedEvent(@NonNull String sessionId, EditingEndedEvent editingEndedEvent) {}

	/** Creates a playback session. */
	@NonNull
	public PlaybackSession createPlaybackSession() {
		return new PlaybackSession(UUID.randomUUID().toString(), this);
	}

	/** Creates an editing session (android-16), with a local id like createPlaybackSession(). */
	@NonNull
	public EditingSession createEditingSession() {
		return new EditingSession(UUID.randomUUID().toString(), this);
	}

	/** Creates a recording session (android-16), with a local id like createPlaybackSession(). */
	@NonNull
	public RecordingSession createRecordingSession() {
		return new RecordingSession(UUID.randomUUID().toString(), this);
	}

	/** Creates a transcoding session (android-16), with a local id like createPlaybackSession(). */
	@NonNull
	public TranscodingSession createTranscodingSession() {
		return new TranscodingSession(UUID.randomUUID().toString(), this);
	}

	/** Creates a bundle session (android-16), with a local id like createPlaybackSession(). */
	@NonNull
	public BundleSession createBundleSession() {
		return new BundleSession(UUID.randomUUID().toString(), this);
	}
}
