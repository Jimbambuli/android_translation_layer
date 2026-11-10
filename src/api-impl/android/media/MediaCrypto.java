package android.media;

import java.util.UUID;

/* ATL has no DRM backend; MediaCrypto is a parity stub matching AOSP's no-secure-decoder path. */
public final class MediaCrypto {

	private byte[] sessionId;

	public MediaCrypto(UUID uuid, byte[] sessionId) throws MediaCryptoException {
		this.sessionId = sessionId;
	}

	public static boolean isCryptoSchemeSupported(UUID uuid) {
		return false;
	}

	/* No secure decoder exists in ATL: clear decode is always used. */
	public boolean requiresSecureDecoderComponent(String mime) {
		return false;
	}

	public void setMediaDrmSession(byte[] sessionId) throws MediaCryptoException {
		this.sessionId = sessionId;
	}

	public void release() {
	}
}
