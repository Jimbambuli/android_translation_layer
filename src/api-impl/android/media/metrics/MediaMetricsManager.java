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

	public void releaseSessionId(@NonNull String sessionId) {}

	/** Creates a playback session. */
	@NonNull
	public PlaybackSession createPlaybackSession() {
		return new PlaybackSession(UUID.randomUUID().toString(), this);
	}
}
