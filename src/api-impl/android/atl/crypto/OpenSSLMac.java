package android.atl.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.MacSpi;
import javax.crypto.SecretKey;

/**
 * HMAC in libcrypto, like Conscrypt's OpenSSLMac.Hmac (not cloneable, as there).
 */
public abstract class OpenSSLMac extends MacSpi {
	private final int md;
	private final int size;
	private long ctx; // EVP_MAC_CTX *
	private final byte[] singleByte = new byte[1];

	private OpenSSLMac(int md, int size) {
		this.md = md;
		this.size = size;
	}

	@Override
	protected int engineGetMacLength() {
		return size;
	}

	@Override
	protected synchronized void engineInit(Key key, AlgorithmParameterSpec params)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		if (!(key instanceof SecretKey))
			throw new InvalidKeyException("key must be a SecretKey");
		if (params != null)
			throw new InvalidAlgorithmParameterException("unknown parameter type");
		byte[] keyBytes = key.getEncoded();
		if (keyBytes == null)
			throw new InvalidKeyException("key cannot be encoded");
		long newCtx;
		try {
			newCtx = NativeCrypto.HMAC_create(md, keyBytes);
		} catch (RuntimeException e) {
			throw new InvalidKeyException("invalid key", e);
		}
		NativeCrypto.HMAC_free(ctx);
		ctx = newCtx;
	}

	@Override
	protected void engineUpdate(byte input) {
		singleByte[0] = input;
		engineUpdate(singleByte, 0, 1);
	}

	@Override
	protected synchronized void engineUpdate(byte[] input, int offset, int len) {
		NativeCrypto.HMAC_Update(ctx, input, offset, len);
	}

	@Override
	protected void engineUpdate(ByteBuffer input) {
		if (input.hasArray()) { // without the copy of MacSpi's default
			int len = input.remaining();
			engineUpdate(input.array(), input.arrayOffset() + input.position(), len);
			input.position(input.position() + len);
		} else {
			super.engineUpdate(input);
		}
	}

	@Override
	protected synchronized byte[] engineDoFinal() {
		return NativeCrypto.HMAC_Final(ctx); // also starts over with the key
	}

	@Override
	protected synchronized void engineReset() {
		if (ctx != 0)
			NativeCrypto.HMAC_Reset(ctx);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			NativeCrypto.HMAC_free(ctx);
			ctx = 0;
		} finally {
			super.finalize();
		}
	}

	public static final class HmacMD5 extends OpenSSLMac {
		public HmacMD5() { super(NativeCrypto.MD5, 16); }
	}

	public static final class HmacSHA1 extends OpenSSLMac {
		public HmacSHA1() { super(NativeCrypto.SHA1, 20); }
	}

	public static final class HmacSHA224 extends OpenSSLMac {
		public HmacSHA224() { super(NativeCrypto.SHA224, 28); }
	}

	public static final class HmacSHA256 extends OpenSSLMac {
		public HmacSHA256() { super(NativeCrypto.SHA256, 32); }
	}

	public static final class HmacSHA384 extends OpenSSLMac {
		public HmacSHA384() { super(NativeCrypto.SHA384, 48); }
	}

	public static final class HmacSHA512 extends OpenSSLMac {
		public HmacSHA512() { super(NativeCrypto.SHA512, 64); }
	}
}
