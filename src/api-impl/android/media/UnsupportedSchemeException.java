package android.media;

/* API 18. Thrown by the MediaDrm constructor when the requested crypto scheme is not supported.
 * ATL supports no DRM scheme, so constructing a MediaDrm always throws this, which is how a
 * player (ExoPlayer) learns to fall back to non-DRM playback. */
public final class UnsupportedSchemeException extends MediaDrmException {

	public UnsupportedSchemeException(String detailMessage) {
		super(detailMessage);
	}
}
