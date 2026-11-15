/* AOSP frameworks/base/media/java/android/media/MediaCas.java
 * ATL has no CAS plugin service, as on a device without one: no CA system is supported and every
 * constructor throws UnsupportedCasException ("Unsupported casSystemId N"). */
package android.media;

import android.content.Context;
import android.os.Handler;

public final class MediaCas implements AutoCloseable {
	public static final int PLUGIN_STATUS_PHYSICAL_MODULE_CHANGED = 0;
	public static final int PLUGIN_STATUS_SESSION_NUMBER_CHANGED = 1;
	public static final int SCRAMBLING_MODE_AES128 = 9;
	public static final int SCRAMBLING_MODE_AES_CBC = 14;
	public static final int SCRAMBLING_MODE_AES_ECB = 10;
	public static final int SCRAMBLING_MODE_AES_SCTE52 = 11;
	public static final int SCRAMBLING_MODE_DVB_CISSA_V1 = 6;
	public static final int SCRAMBLING_MODE_DVB_CSA1 = 1;
	public static final int SCRAMBLING_MODE_DVB_CSA2 = 2;
	public static final int SCRAMBLING_MODE_DVB_CSA3_ENHANCE = 5;
	public static final int SCRAMBLING_MODE_DVB_CSA3_MINIMAL = 4;
	public static final int SCRAMBLING_MODE_DVB_CSA3_STANDARD = 3;
	public static final int SCRAMBLING_MODE_DVB_IDSA = 7;
	public static final int SCRAMBLING_MODE_MULTI2 = 8;
	public static final int SCRAMBLING_MODE_RESERVED = 0;
	public static final int SCRAMBLING_MODE_TDES_ECB = 12;
	public static final int SCRAMBLING_MODE_TDES_SCTE52 = 13;
	public static final int SESSION_USAGE_LIVE = 0;
	public static final int SESSION_USAGE_PLAYBACK = 1;
	public static final int SESSION_USAGE_RECORD = 2;
	public static final int SESSION_USAGE_TIMESHIFT = 3;

	public interface EventListener {
		void onEvent(MediaCas mediaCas, int event, int arg, byte[] data);

		default void onSessionEvent(MediaCas mediaCas, Session session, int event, int arg, byte[] data) {
		}

		default void onPluginStatusUpdate(MediaCas MediaCas, int status, int arg) {
		}

		default void onResourceLost(MediaCas MediaCas) {
		}
	}

	public static class PluginDescriptor {
		private final int mCASystemId;
		private final String mName;

		PluginDescriptor(int CA_system_id, String name) {
			mCASystemId = CA_system_id;
			mName = name;
		}

		public int getSystemId() {
			return mCASystemId;
		}

		public String getName() {
			return mName;
		}

		@Override
		public String toString() {
			return "{" + mCASystemId + ", " + mName + "}";
		}
	}

	public final class Session implements AutoCloseable {
		Session() {
		}

		public byte[] getSessionId() {
			throw new IllegalStateException("MediaCas is invalid");
		}

		public void setPrivateData(byte[] data) throws MediaCasException {
			throw new IllegalStateException("MediaCas is invalid");
		}

		public void processEcm(byte[] data, int offset, int length) throws MediaCasException {
			throw new IllegalStateException("MediaCas is invalid");
		}

		public void processEcm(byte[] data) throws MediaCasException {
			processEcm(data, 0, data.length);
		}

		public void sendSessionEvent(int event, int arg, byte[] data) throws MediaCasException {
			throw new IllegalStateException("MediaCas is invalid");
		}

		@Override
		public void close() {
		}
	}

	/* no service: enumeratePlugins() ends in "return null" in AOSP */
	public static PluginDescriptor[] enumeratePlugins() {
		return null;
	}

	public static boolean isSystemIdSupported(int CA_system_id) {
		return false;
	}

	public MediaCas(int CA_system_id) throws MediaCasException.UnsupportedCasException {
		throw new MediaCasException.UnsupportedCasException("Unsupported casSystemId " + CA_system_id);
	}

	public MediaCas(Context context, int CA_system_id, String tvInputServiceSessionId, int priorityHint) throws MediaCasException.UnsupportedCasException {
		this(CA_system_id);
	}

	public MediaCas(Context context, int CA_system_id, String tvInputServiceSessionId, int priorityHint, Handler handler, EventListener listener) throws MediaCasException.UnsupportedCasException {
		this(CA_system_id);
	}

	public void setEventListener(EventListener listener, Handler handler) {
	}

	public void setPrivateData(byte[] data) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public Session openSession() throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public Session openSession(int sessionUsage, int scramblingMode) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public void processEmm(byte[] data, int offset, int length) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public void processEmm(byte[] data) throws MediaCasException {
		processEmm(data, 0, data.length);
	}

	public void sendEvent(int event, int arg, byte[] data) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public void provision(String provisionString) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	public void refreshEntitlements(int refreshType, byte[] refreshData) throws MediaCasException {
		throw new IllegalStateException("MediaCas is invalid");
	}

	@Override
	public void close() {
	}

	@Override
	protected void finalize() {
		close();
	}
}
