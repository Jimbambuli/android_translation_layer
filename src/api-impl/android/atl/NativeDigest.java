package android.atl;

import java.security.MessageDigestSpi;
import java.security.Provider;

/**
 * MessageDigest in native code, as on Android.
 *
 * Android's first security provider is Conscrypt ("AndroidOpenSSL"), whose OpenSSLMessageDigestJDK
 * computes MD5 and the SHA digests in BoringSSL. Without it the digests come from BouncyCastle,
 * which computes them in Java. That is much slower whenever Java runs slowly (interpreter, method
 * trace, debugger), and an app that hashes on a background thread then takes much longer for it
 * than it does on Android. This provider computes the digests with GLib's GChecksum; it has the
 * algorithms and aliases of Conscrypt's OpenSSLProvider that GChecksum supports (not SHA-224).
 * RuntimeInit.commonInit() puts it first, like Conscrypt on Android.
 */
public abstract class NativeDigest extends MessageDigestSpi implements Cloneable {
	/* GChecksumType */
	private static final int G_CHECKSUM_MD5 = 0;
	private static final int G_CHECKSUM_SHA1 = 1;
	private static final int G_CHECKSUM_SHA256 = 2;
	private static final int G_CHECKSUM_SHA512 = 3;
	private static final int G_CHECKSUM_SHA384 = 4;

	private final int size;
	private long checksum; // GChecksum *
	private final byte[] singleByte = new byte[1];

	private NativeDigest(int type, int size) {
		this.size = size;
		this.checksum = native_new(type);
	}

	@Override
	protected int engineGetDigestLength() {
		return size;
	}

	@Override
	protected void engineUpdate(byte input) {
		singleByte[0] = input;
		native_update(checksum, singleByte, 0, 1);
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		native_update(checksum, input, offset, len);
	}

	@Override
	protected byte[] engineDigest() {
		byte[] digest = new byte[size];
		native_digest(checksum, digest); // also resets, as engineDigest() must
		return digest;
	}

	@Override
	protected void engineReset() {
		native_reset(checksum);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		NativeDigest copy = (NativeDigest)super.clone();
		copy.checksum = native_copy(checksum);
		return copy;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			native_free(checksum);
			checksum = 0;
		} finally {
			super.finalize();
		}
	}

	private static native long native_new(int type);
	private static native void native_update(long checksum, byte[] input, int offset, int len);
	private static native void native_digest(long checksum, byte[] digest);
	private static native void native_reset(long checksum);
	private static native long native_copy(long checksum);
	private static native void native_free(long checksum);

	public static final class MD5 extends NativeDigest {
		public MD5() { super(G_CHECKSUM_MD5, 16); }
	}

	public static final class SHA1 extends NativeDigest {
		public SHA1() { super(G_CHECKSUM_SHA1, 20); }
	}

	public static final class SHA256 extends NativeDigest {
		public SHA256() { super(G_CHECKSUM_SHA256, 32); }
	}

	public static final class SHA384 extends NativeDigest {
		public SHA384() { super(G_CHECKSUM_SHA384, 48); }
	}

	public static final class SHA512 extends NativeDigest {
		public SHA512() { super(G_CHECKSUM_SHA512, 64); }
	}

	public static final class NativeDigestProvider extends Provider {
		public NativeDigestProvider() {
			super("ATLNativeDigest", 1.0, "MessageDigest in native code (GLib GChecksum), like Conscrypt's on Android");
			final String prefix = NativeDigest.class.getName() + "$";
			put("MessageDigest.MD5", prefix + "MD5");
			put("Alg.Alias.MessageDigest.1.2.840.113549.2.5", "MD5");
			put("MessageDigest.SHA-1", prefix + "SHA1");
			put("Alg.Alias.MessageDigest.SHA1", "SHA-1");
			put("Alg.Alias.MessageDigest.SHA", "SHA-1");
			put("Alg.Alias.MessageDigest.1.3.14.3.2.26", "SHA-1");
			put("MessageDigest.SHA-256", prefix + "SHA256");
			put("Alg.Alias.MessageDigest.SHA256", "SHA-256");
			put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.1", "SHA-256");
			put("MessageDigest.SHA-384", prefix + "SHA384");
			put("Alg.Alias.MessageDigest.SHA384", "SHA-384");
			put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.2", "SHA-384");
			put("MessageDigest.SHA-512", prefix + "SHA512");
			put("Alg.Alias.MessageDigest.SHA512", "SHA-512");
			put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.3", "SHA-512");
		}
	}
}
