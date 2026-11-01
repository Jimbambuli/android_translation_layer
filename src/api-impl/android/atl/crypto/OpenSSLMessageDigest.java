package android.atl.crypto;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigestSpi;

/**
 * MessageDigest in libcrypto, like Conscrypt's OpenSSLMessageDigestJDK.
 */
public abstract class OpenSSLMessageDigest extends MessageDigestSpi implements Cloneable {
	private final int size;
	private long ctx; // EVP_MD_CTX *
	private final byte[] singleByte = new byte[1];

	private OpenSSLMessageDigest(int md, int size) {
		this.size = size;
		this.ctx = NativeCrypto.EVP_MD_CTX_create(md);
	}

	@Override
	protected int engineGetDigestLength() {
		return size;
	}

	@Override
	protected void engineUpdate(byte input) {
		singleByte[0] = input;
		NativeCrypto.EVP_DigestUpdate(ctx, singleByte, 0, 1);
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		NativeCrypto.EVP_DigestUpdate(ctx, input, offset, len);
	}

	@Override
	protected void engineUpdate(ByteBuffer input) {
		if (input.hasArray()) { // without the copy of MessageDigestSpi's default
			int len = input.remaining();
			NativeCrypto.EVP_DigestUpdate(ctx, input.array(), input.arrayOffset() + input.position(), len);
			input.position(input.position() + len);
		} else {
			super.engineUpdate(input);
		}
	}

	@Override
	protected byte[] engineDigest() {
		byte[] digest = new byte[size];
		NativeCrypto.EVP_DigestFinal(ctx, digest, 0); // also starts over, as engineDigest() must
		return digest;
	}

	@Override
	protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
		if (len < size)
			throw new DigestException("partial digests not returned");
		if (buf.length - offset < size)
			throw new DigestException("insufficient space in the output buffer to store the digest");
		return NativeCrypto.EVP_DigestFinal(ctx, buf, offset);
	}

	@Override
	protected void engineReset() {
		NativeCrypto.EVP_DigestReset(ctx);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		OpenSSLMessageDigest copy = (OpenSSLMessageDigest)super.clone();
		copy.ctx = NativeCrypto.EVP_MD_CTX_copy(ctx);
		return copy;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			NativeCrypto.EVP_MD_CTX_free(ctx);
			ctx = 0;
		} finally {
			super.finalize();
		}
	}

	public static final class MD5 extends OpenSSLMessageDigest {
		public MD5() { super(NativeCrypto.MD5, 16); }
	}

	public static final class SHA1 extends OpenSSLMessageDigest {
		public SHA1() { super(NativeCrypto.SHA1, 20); }
	}

	public static final class SHA224 extends OpenSSLMessageDigest {
		public SHA224() { super(NativeCrypto.SHA224, 28); }
	}

	public static final class SHA256 extends OpenSSLMessageDigest {
		public SHA256() { super(NativeCrypto.SHA256, 32); }
	}

	public static final class SHA384 extends OpenSSLMessageDigest {
		public SHA384() { super(NativeCrypto.SHA384, 48); }
	}

	public static final class SHA512 extends OpenSSLMessageDigest {
		public SHA512() { super(NativeCrypto.SHA512, 64); }
	}
}
