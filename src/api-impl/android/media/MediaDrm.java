package android.media;

import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.PersistableBundle;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/* API 18. DRM key management. ATL ships no DRM plugin, so no crypto scheme is supported:
 * isCryptoSchemeSupported() returns false and the constructor throws UnsupportedSchemeException,
 * exactly as a device without the requested scheme would. A player (ExoPlayer) catches that and
 * falls back to non-DRM playback instead of hitting a NoClassDefFoundError. The remaining methods
 * exist only so references to them link; no instance is ever created, so they are never run. */
public final class MediaDrm implements AutoCloseable {

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

	/* AOSP MediaDrm security levels and offline licenses (native there). The constructor always fails
	 * (no DRM plugin), so the instance methods are never reached; their JNI side is not in the local sources. */
	public static final int SECURITY_LEVEL_UNKNOWN = 0;
	public static final int SECURITY_LEVEL_SW_SECURE_CRYPTO = 1;
	public static final int SECURITY_LEVEL_SW_SECURE_DECODE = 2;
	public static final int SECURITY_LEVEL_HW_SECURE_CRYPTO = 3;
	public static final int SECURITY_LEVEL_HW_SECURE_DECODE = 4;
	public static final int SECURITY_LEVEL_HW_SECURE_ALL = 5;
	public static final int SECURITY_LEVEL_MAX = 6;
	public static final int OFFLINE_LICENSE_STATE_UNKNOWN = 0;
	public static final int OFFLINE_LICENSE_STATE_USABLE = 1;
	public static final int OFFLINE_LICENSE_STATE_RELEASED = 2;

	public static final int getMaxSecurityLevel() {
		return SECURITY_LEVEL_MAX;
	}

	public int getSecurityLevel(byte[] sessionId) {
		throw new MediaDrmStateException("no DRM");
	}

	public List<byte[]> getOfflineLicenseKeySetIds() {
		return new java.util.ArrayList<byte[]>();
	}

	public void removeOfflineLicense(byte[] keySetId) {
		throw new MediaDrmStateException("no DRM");
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

	/* AOSP MediaDrm.requiresSecureDecoder(String, int) (API 31); no MediaDrm instance exists on ATL (no scheme is
	 * supported), as for the variant without a security level */
	public boolean requiresSecureDecoder(String mime, int level) {
		return false;
	}

	public PlaybackComponent getPlaybackComponent(byte[] sessionId) {
		return null;
	}

	public void setOnEventListener(OnEventListener listener) {
		onEventListener = listener;
	}

	public void release() {
	}

	/* AOSP MediaDrm.close() (AutoCloseable): release() */

	/* AOSP MediaDrm (frameworks/base/media/java/android/media/MediaDrm.java): events, HDCP levels, key types, property names */
	public static final int EVENT_KEY_EXPIRED = 3;
	public static final int EVENT_KEY_REQUIRED = 2;
	public static final int EVENT_PROVISION_REQUIRED = 1;
	public static final int EVENT_SESSION_RECLAIMED = 5;
	public static final int EVENT_VENDOR_DEFINED = 4;
	public static final int HDCP_LEVEL_UNKNOWN = 0;
	public static final int HDCP_NONE = 1;
	public static final int HDCP_NO_DIGITAL_OUTPUT = 2147483647;
	public static final int HDCP_V1 = 2;
	public static final int HDCP_V2 = 3;
	public static final int HDCP_V2_1 = 4;
	public static final int HDCP_V2_2 = 5;
	public static final int HDCP_V2_3 = 6;
	public static final int KEY_TYPE_OFFLINE = 2;
	public static final int KEY_TYPE_RELEASE = 3;
	public static final int KEY_TYPE_STREAMING = 1;
	public static final String PROPERTY_ALGORITHMS = "algorithms";
	public static final String PROPERTY_DESCRIPTION = "description";
	public static final String PROPERTY_DEVICE_UNIQUE_ID = "deviceUniqueId";
	public static final String PROPERTY_VENDOR = "vendor";
	public static final String PROPERTY_VERSION = "version";



	/* Without a DRM plugin no MediaDrm instance can exist (the constructor throws UnsupportedSchemeException);
	 * a method on an instance would, in AOSP, find the native object invalid and throw IllegalStateException. */
	private static IllegalStateException invalid() {
		return new IllegalStateException("MediaDrm Java object is not valid");
	}

	@Override
	public void close() {
		release();
	}

	public byte[] openSession(int level) throws NotProvisionedException, ResourceBusyException {
		throw invalid();
	}

	public void clearOnEventListener() {
		onEventListener = null;
	}

	public void clearOnExpirationUpdateListener() {
		onExpirationUpdateListener = null;
	}

	public void clearOnKeyStatusChangeListener() {
		onKeyStatusChangeListener = null;
	}

	public void clearOnSessionLostStateListener() {
		onSessionLostStateListener = null;
	}

	public void setOnEventListener(OnEventListener listener, Handler handler) {
		onEventListener = listener;
	}

	public void setOnEventListener(Executor executor, OnEventListener listener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(listener);
		onEventListener = listener;
	}

	public void setOnExpirationUpdateListener(Executor executor, OnExpirationUpdateListener listener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(listener);
		onExpirationUpdateListener = listener;
	}

	public void setOnKeyStatusChangeListener(Executor executor, OnKeyStatusChangeListener listener) {
		java.util.Objects.requireNonNull(executor);
		java.util.Objects.requireNonNull(listener);
		onKeyStatusChangeListener = listener;
	}

	public void setOnSessionLostStateListener(OnSessionLostStateListener listener, Handler handler) {
		onSessionLostStateListener = listener;
	}

	public void setOnSessionLostStateListener(Executor executor, OnSessionLostStateListener listener) {
		java.util.Objects.requireNonNull(executor);
		onSessionLostStateListener = listener;
	}

	private OnEventListener onEventListener;
	private OnSessionLostStateListener onSessionLostStateListener;

	public static boolean isCryptoSchemeSupported(UUID uuid, String mimeType, int securityLevel) {
		return false;
	}

	public int getConnectedHdcpLevel() {
		throw invalid();
	}

	public int getMaxHdcpLevel() {
		throw invalid();
	}

	public int getOpenSessionCount() {
		throw invalid();
	}

	public int getMaxSessionCount() {
		throw invalid();
	}

	public int getOfflineLicenseState(byte[] keySetId) {
		throw invalid();
	}

	public void removeKeys(byte[] sessionId) {
		throw invalid();
	}

	public CryptoSession getCryptoSession(byte[] sessionId, String cipherAlgorithm, String macAlgorithm) {
		throw invalid();
	}

	public List<LogMessage> getLogMessages() {
		throw invalid();
	}

	/* deprecated secure stop API */
	public List<byte[]> getSecureStops() {
		throw invalid();
	}

	public List<byte[]> getSecureStopIds() {
		throw invalid();
	}

	public byte[] getSecureStop(byte[] ssid) {
		throw invalid();
	}

	public void releaseSecureStops(byte[] ssRelease) {
		throw invalid();
	}

	public void removeSecureStop(byte[] ssid) {
		throw invalid();
	}

	public void releaseAllSecureStops() {
		throw invalid();
	}

	public void removeAllSecureStops() {
		throw invalid();
	}

	public interface OnSessionLostStateListener {
		void onSessionLostState(MediaDrm md, byte[] sessionId);
	}

	public final class CryptoSession {
		CryptoSession() {
		}

		public byte[] encrypt(byte[] keyid, byte[] input, byte[] iv) {
			throw invalid();
		}

		public byte[] decrypt(byte[] keyid, byte[] input, byte[] iv) {
			throw invalid();
		}

		public byte[] sign(byte[] keyid, byte[] message) {
			throw invalid();
		}

		public boolean verify(byte[] keyid, byte[] message, byte[] signature) {
			throw invalid();
		}
	}

	/* AOSP MediaDrm.LogMessage */
	public static final class LogMessage {
		private final long timestampMillis;
		private final int priority;
		private final String message;

		public final long getTimestampMillis() {
			return timestampMillis;
		}

		public final int getPriority() {
			return priority;
		}

		public final String getMessage() {
			return message;
		}

		private LogMessage(long timestampMillis, int priority, String message) {
			this.timestampMillis = timestampMillis;
			if (priority < android.util.Log.VERBOSE || priority > android.util.Log.ASSERT) {
				throw new IllegalArgumentException("invalid log priority " + priority);
			}
			this.priority = priority;
			this.message = message;
		}

		@Override
		public String toString() {
			return String.format("LogMessage(%s, %s, %s)", timestampMillis, priority, message);
		}
	}

	public static final class ErrorCodes {
		private ErrorCodes() {
		}

		public static final int ERROR_CERTIFICATE_MALFORMED = 10;
		public static final int ERROR_CERTIFICATE_MISSING = 11;
		public static final int ERROR_CRYPTO_LIBRARY = 12;
		public static final int ERROR_FRAME_TOO_LARGE = 8;
		public static final int ERROR_GENERIC_OEM = 13;
		public static final int ERROR_GENERIC_PLUGIN = 14;
		public static final int ERROR_INIT_DATA = 15;
		public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = 4;
		public static final int ERROR_INSUFFICIENT_SECURITY = 7;
		public static final int ERROR_KEY_EXPIRED = 2;
		public static final int ERROR_KEY_NOT_LOADED = 16;
		public static final int ERROR_LICENSE_PARSE = 17;
		public static final int ERROR_LICENSE_POLICY = 18;
		public static final int ERROR_LICENSE_RELEASE = 19;
		public static final int ERROR_LICENSE_REQUEST_REJECTED = 20;
		public static final int ERROR_LICENSE_RESTORE = 21;
		public static final int ERROR_LICENSE_STATE = 22;
		public static final int ERROR_LOST_STATE = 9;
		public static final int ERROR_MEDIA_FRAMEWORK = 23;
		public static final int ERROR_NO_KEY = 1;
		public static final int ERROR_PROVISIONING_CERTIFICATE = 24;
		public static final int ERROR_PROVISIONING_CONFIG = 25;
		public static final int ERROR_PROVISIONING_PARSE = 26;
		public static final int ERROR_PROVISIONING_REQUEST_REJECTED = 27;
		public static final int ERROR_PROVISIONING_RETRY = 28;
		public static final int ERROR_RESOURCE_BUSY = 3;
		public static final int ERROR_RESOURCE_CONTENTION = 29;
		public static final int ERROR_SECURE_STOP_RELEASE = 30;
		public static final int ERROR_SESSION_NOT_OPENED = 5;
		public static final int ERROR_STORAGE_READ = 31;
		public static final int ERROR_STORAGE_WRITE = 32;
		public static final int ERROR_UNKNOWN = 0;
		public static final int ERROR_UNSUPPORTED_OPERATION = 6;
		public static final int ERROR_ZERO_SUBSAMPLES = 33;

	}

	public static final class MetricsConstants {
		private MetricsConstants() {
		}

		public static final String CLOSE_SESSION_ERROR_COUNT = "drm.mediadrm.close_session.error.count";
		public static final String CLOSE_SESSION_ERROR_LIST = "drm.mediadrm.close_session.error.list";
		public static final String CLOSE_SESSION_OK_COUNT = "drm.mediadrm.close_session.ok.count";
		public static final String EVENT_KEY_EXPIRED_COUNT = "drm.mediadrm.event.KEY_EXPIRED.count";
		public static final String EVENT_KEY_NEEDED_COUNT = "drm.mediadrm.event.KEY_NEEDED.count";
		public static final String EVENT_PROVISION_REQUIRED_COUNT = "drm.mediadrm.event.PROVISION_REQUIRED.count";
		public static final String EVENT_SESSION_RECLAIMED_COUNT = "drm.mediadrm.event.SESSION_RECLAIMED.count";
		public static final String EVENT_VENDOR_DEFINED_COUNT = "drm.mediadrm.event.VENDOR_DEFINED.count";
		public static final String GET_DEVICE_UNIQUE_ID_ERROR_COUNT = "drm.mediadrm.get_device_unique_id.error.count";
		public static final String GET_DEVICE_UNIQUE_ID_ERROR_LIST = "drm.mediadrm.get_device_unique_id.error.list";
		public static final String GET_DEVICE_UNIQUE_ID_OK_COUNT = "drm.mediadrm.get_device_unique_id.ok.count";
		public static final String GET_KEY_REQUEST_ERROR_COUNT = "drm.mediadrm.get_key_request.error.count";
		public static final String GET_KEY_REQUEST_ERROR_LIST = "drm.mediadrm.get_key_request.error.list";
		public static final String GET_KEY_REQUEST_OK_COUNT = "drm.mediadrm.get_key_request.ok.count";
		public static final String GET_KEY_REQUEST_OK_TIME_MICROS = "drm.mediadrm.get_key_request.ok.average_time_micros";
		public static final String GET_PROVISION_REQUEST_ERROR_COUNT = "drm.mediadrm.get_provision_request.error.count";
		public static final String GET_PROVISION_REQUEST_ERROR_LIST = "drm.mediadrm.get_provision_request.error.list";
		public static final String GET_PROVISION_REQUEST_OK_COUNT = "drm.mediadrm.get_provision_request.ok.count";
		public static final String KEY_STATUS_EXPIRED_COUNT = "drm.mediadrm.key_status.EXPIRED.count";
		public static final String KEY_STATUS_INTERNAL_ERROR_COUNT = "drm.mediadrm.key_status.INTERNAL_ERROR.count";
		public static final String KEY_STATUS_OUTPUT_NOT_ALLOWED_COUNT = "drm.mediadrm.key_status_change.OUTPUT_NOT_ALLOWED.count";
		public static final String KEY_STATUS_PENDING_COUNT = "drm.mediadrm.key_status_change.PENDING.count";
		public static final String KEY_STATUS_USABLE_COUNT = "drm.mediadrm.key_status_change.USABLE.count";
		public static final String OPEN_SESSION_ERROR_COUNT = "drm.mediadrm.open_session.error.count";
		public static final String OPEN_SESSION_ERROR_LIST = "drm.mediadrm.open_session.error.list";
		public static final String OPEN_SESSION_OK_COUNT = "drm.mediadrm.open_session.ok.count";
		public static final String PROVIDE_KEY_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_key_response.error.count";
		public static final String PROVIDE_KEY_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_key_response.error.list";
		public static final String PROVIDE_KEY_RESPONSE_OK_COUNT = "drm.mediadrm.provide_key_response.ok.count";
		public static final String PROVIDE_KEY_RESPONSE_OK_TIME_MICROS = "drm.mediadrm.provide_key_response.ok.average_time_micros";
		public static final String PROVIDE_PROVISION_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_provision_response.error.count";
		public static final String PROVIDE_PROVISION_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_provision_response.error.list";
		public static final String PROVIDE_PROVISION_RESPONSE_OK_COUNT = "drm.mediadrm.provide_provision_response.ok.count";
		public static final String SESSION_END_TIMES_MS = "drm.mediadrm.session_end_times_ms";
		public static final String SESSION_START_TIMES_MS = "drm.mediadrm.session_start_times_ms";

	}

	public static final class SessionException extends RuntimeException implements MediaDrmThrowable {
		public SessionException(int errorCode, String detailMessage) {
			this(detailMessage, errorCode, 0, 0, 0);
		}

		SessionException(String detailMessage, int errorCode, int vendorError, int oemError, int errorContext) {
			super(detailMessage);
			mErrorCode = errorCode;
			mVendorError = vendorError;
			mOemError = oemError;
			mErrorContext = errorContext;
		}

		public static final int ERROR_UNKNOWN = 0;
		public static final int ERROR_RESOURCE_CONTENTION = 1;

		public int getErrorCode() {
			return mErrorCode;
		}

		@Override
		public int getVendorError() {
			return mVendorError;
		}

		@Override
		public int getOemError() {
			return mOemError;
		}

		@Override
		public int getErrorContext() {
			return mErrorContext;
		}

		public boolean isTransient() {
			return mErrorCode == ERROR_RESOURCE_CONTENTION;
		}

		private final int mErrorCode, mVendorError, mOemError, mErrorContext;
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

	/* AOSP MediaDrm.PlaybackComponent; with no native backend the id is only kept */
	public final class PlaybackComponent {
		private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;

		PlaybackComponent() {
		}

		public void setLogSessionId(LogSessionId logSessionId) {
			java.util.Objects.requireNonNull(logSessionId);
			if (logSessionId.getStringId() == null) {
				throw new IllegalArgumentException("playbackId is null");
			}
			mLogSessionId = logSessionId;
		}

		public LogSessionId getLogSessionId() {
			return mLogSessionId;
		}
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

	/* AOSP MediaDrm.MediaDrmStateException implements MediaDrmThrowable: error code, vendor/OEM error and
	 * context, isTransient() and the diagnostic info built from the error code. ATL's own
	 * (String) constructor stands for ErrorCodes.ERROR_UNKNOWN (0). */
	public static final class MediaDrmStateException extends IllegalStateException implements MediaDrmThrowable {
		private final int mErrorCode, mVendorError, mOemError, mErrorContext;
		private final String mDiagnosticInfo;

		public MediaDrmStateException(String detailMessage) {
			this(detailMessage, 0, 0, 0, 0);
		}

		/** @hide */
		public MediaDrmStateException(int errorCode, String detailMessage) {
			this(detailMessage, errorCode, 0, 0, 0);
		}

		/** @hide */
		public MediaDrmStateException(String detailMessage, int errorCode, int vendorError, int oemError, int errorContext) {
			super(detailMessage);
			mErrorCode = errorCode;
			mVendorError = vendorError;
			mOemError = oemError;
			mErrorContext = errorContext;

			final String sign = errorCode < 0 ? "neg_" : "";
			mDiagnosticInfo = "android.media.MediaDrm.error_" + sign + Math.abs(errorCode);
		}

		public int getErrorCode() {
			return mErrorCode;
		}

		@Override
		public int getVendorError() {
			return mVendorError;
		}

		@Override
		public int getOemError() {
			return mOemError;
		}

		@Override
		public int getErrorContext() {
			return mErrorContext;
		}

		public boolean isTransient() {
			return mErrorCode == ErrorCodes.ERROR_PROVISIONING_RETRY || mErrorCode == ErrorCodes.ERROR_RESOURCE_CONTENTION;
		}

		public String getDiagnosticInfo() {
			return mDiagnosticInfo;
		}
	}
}
