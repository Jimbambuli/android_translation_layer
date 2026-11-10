package android.media;

import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.PersistableBundle;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/* API 18. DRM key management. ATL ships no DRM plugin, so no crypto scheme is supported:
 * isCryptoSchemeSupported() returns false and the constructor throws UnsupportedSchemeException,
 * exactly as a device without the requested scheme would. A player (ExoPlayer) catches that and
 * falls back to non-DRM playback instead of hitting a NoClassDefFoundError. The remaining methods
 * exist only so references to them link; no instance is ever created, so they are never run. */
public final class MediaDrm {

	public MediaDrm(UUID uuid) throws UnsupportedSchemeException {
		throw new UnsupportedSchemeException("no DRM scheme is supported");
	}

	public static boolean isCryptoSchemeSupported(UUID uuid) {
		return false;
	}

	public static boolean isCryptoSchemeSupported(UUID uuid, String mimeType) {
		return false;
	}

	/* API 30: the schemes of the installed DRM plugins, none */
	public static final List<UUID> getSupportedCryptoSchemes() {
		return new java.util.ArrayList<UUID>();
	}

	public byte[] openSession() throws MediaDrmException {
		throw new MediaDrmException("no DRM");
	}

	public void closeSession(byte[] sessionId) {
	}

	public KeyRequest getKeyRequest(byte[] scope, byte[] init, String mimeType, int keyType, HashMap<String, String> optionalParameters) throws MediaDrmException {
		throw new MediaDrmException("no DRM");
	}

	public byte[] provideKeyResponse(byte[] scope, byte[] response) throws MediaDrmException {
		throw new MediaDrmException("no DRM");
	}

	public void restoreKeys(byte[] sessionId, byte[] keySetId) {
	}

	public HashMap<String, String> queryKeyStatus(byte[] sessionId) {
		return new HashMap<String, String>();
	}

	public ProvisionRequest getProvisionRequest() {
		return new ProvisionRequest();
	}

	public void provideProvisionResponse(byte[] response) throws MediaDrmException {
		throw new MediaDrmException("no DRM");
	}

	public String getPropertyString(String propertyName) {
		return "";
	}

	public void setPropertyString(String propertyName, String value) {
	}

	public byte[] getPropertyByteArray(String propertyName) {
		return new byte[0];
	}

	public void setPropertyByteArray(String propertyName, byte[] value) {
	}

	/* No DRM metrics are collected. */
	public PersistableBundle getMetrics() {
		return new PersistableBundle();
	}

	/* Stored for parity; with no DRM backend there is no session whose keys could change or expire. */
	public void setOnExpirationUpdateListener(OnExpirationUpdateListener listener, Handler handler) {
		onExpirationUpdateListener = listener;
	}

	public void setOnKeyStatusChangeListener(OnKeyStatusChangeListener listener, Handler handler) {
		onKeyStatusChangeListener = listener;
	}

	private OnExpirationUpdateListener onExpirationUpdateListener;
	private OnKeyStatusChangeListener onKeyStatusChangeListener;

	public boolean requiresSecureDecoder(String mime) {
		return false;
	}

	public PlaybackComponent getPlaybackComponent(byte[] sessionId) {
		return null;
	}

	public void setOnEventListener(OnEventListener listener) {
	}

	public void release() {
	}

	public interface OnEventListener {
		void onEvent(MediaDrm md, byte[] sessionId, int event, int extra, byte[] data);
	}

	public interface OnExpirationUpdateListener {
		void onExpirationUpdate(MediaDrm md, byte[] sessionId, long expirationTime);
	}

	public interface OnKeyStatusChangeListener {
		void onKeyStatusChange(MediaDrm md, byte[] sessionId, List<KeyStatus> keyInformation, boolean hasNewUsableKey);
	}

	public static final class KeyStatus {
		public static final int STATUS_USABLE = 0;
		public static final int STATUS_EXPIRED = 1;
		public static final int STATUS_OUTPUT_NOT_ALLOWED = 2;
		public static final int STATUS_PENDING = 3;
		public static final int STATUS_INTERNAL_ERROR = 4;
		public static final int STATUS_USABLE_IN_FUTURE = 5;

		private final byte[] keyId;
		private final int statusCode;

		KeyStatus(byte[] keyId, int statusCode) {
			this.keyId = keyId;
			this.statusCode = statusCode;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public byte[] getKeyId() {
			return keyId;
		}
	}

	public interface PlaybackComponent {
		void setLogSessionId(LogSessionId logSessionId);
	}

	public static final class KeyRequest {
		public byte[] getData() {
			return new byte[0];
		}

		public String getDefaultUrl() {
			return "";
		}

		public int getRequestType() {
			return 0;
		}
	}

	public static final class ProvisionRequest {
		public byte[] getData() {
			return new byte[0];
		}

		public String getDefaultUrl() {
			return "";
		}
	}

	public static final class MediaDrmStateException extends IllegalStateException {
		public MediaDrmStateException(String detailMessage) {
			super(detailMessage);
		}

		public String getDiagnosticInfo() {
			return "";
		}
	}
}
