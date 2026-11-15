/* AOSP frameworks/base/media/java/android/media/MediaDescrambler.java
 * ATL has no CAS plugin service, as on a device without one: no CA system is supported and every
 * constructor throws UnsupportedCasException ("Unsupported casSystemId N"). */
package android.media;

import java.nio.ByteBuffer;

public final class MediaDescrambler implements AutoCloseable {
	public static final byte SCRAMBLE_CONTROL_EVEN_KEY = 2;
	public static final byte SCRAMBLE_CONTROL_ODD_KEY = 3;
	public static final byte SCRAMBLE_CONTROL_RESERVED = 1;
	public static final byte SCRAMBLE_CONTROL_UNSCRAMBLED = 0;
	public static final byte SCRAMBLE_FLAG_PES_HEADER = 1;

	public MediaDescrambler(int CA_system_id) throws MediaCasException.UnsupportedCasException {
		throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + CA_system_id);
	}

	public boolean requiresSecureDecoderComponent(String mime) {
		throw new IllegalStateException("Invalid descrambler");
	}

	public void setMediaCasSession(MediaCas.Session session) {
		throw new IllegalStateException("Invalid descrambler");
	}

	public int descramble(ByteBuffer srcBuf, ByteBuffer dstBuf, MediaCodec.CryptoInfo cryptoInfo) {
		throw new IllegalStateException("Invalid descrambler");
	}

	@Override
	public void close() {
	}

	@Override
	protected void finalize() {
		close();
	}
}
