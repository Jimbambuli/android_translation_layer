/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.atl.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

/**
 * AES in ECB, CBC and CTR mode on an EVP_CIPHER_CTX, after Conscrypt's OpenSSLEvpCipher and OpenSSLEvpCipherAES.
 * PKCS7Padding is PKCS5Padding, as there.
 */
public abstract class OpenSSLEvpCipherAES extends OpenSSLCipher {
	private static final int AES_BLOCK_SIZE = 16;

	enum Mode { ECB, CBC, CTR }
	enum Padding { NOPADDING, PKCS5PADDING }

	private Mode mode;
	Padding padding;
	private long ctx; // EVP_CIPHER_CTX *
	private boolean calledUpdate;
	private int modeBlockSize;
	/* bytes given to the context that it has not output yet: Conscrypt reads BoringSSL's buf_len and final_used,
	 * which OpenSSL 3 does not expose */
	private int pending;

	OpenSSLEvpCipherAES(Mode mode, Padding padding) {
		this.mode = mode;
		this.padding = padding;
		this.ctx = NativeCrypto.EVP_CIPHER_CTX_new();
	}

	@Override
	protected void engineSetMode(String modeStr) throws NoSuchAlgorithmException {
		try {
			mode = Mode.valueOf(modeStr.toUpperCase(Locale.US));
		} catch (IllegalArgumentException e) {
			NoSuchAlgorithmException newE = new NoSuchAlgorithmException("No such mode: " + modeStr);
			newE.initCause(e);
			throw newE;
		}
	}

	@Override
	protected void engineSetPadding(String paddingStr) throws NoSuchPaddingException {
		String upperPaddingStr = paddingStr.toUpperCase(Locale.US);
		if (upperPaddingStr.equals("NOPADDING"))
			padding = Padding.NOPADDING;
		else if (upperPaddingStr.equals("PKCS5PADDING") || upperPaddingStr.equals("PKCS7PADDING"))
			padding = Padding.PKCS5PADDING;
		else
			throw new NoSuchPaddingException("No such padding: " + paddingStr);
	}

	@Override
	String getBaseCipherName() {
		return "AES";
	}

	@Override
	protected int engineGetBlockSize() {
		return AES_BLOCK_SIZE;
	}

	@Override
	void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		byte[] iv = (params instanceof IvParameterSpec) ? ((IvParameterSpec)params).getIV() : null;
		final int expectedIvLength = mode == Mode.ECB ? 0 : AES_BLOCK_SIZE;
		if (iv == null && expectedIvLength != 0) {
			if (!isEncrypting())
				throw new InvalidAlgorithmParameterException("IV must be specified");
			iv = randomIv(expectedIvLength, random);
		} else if (expectedIvLength == 0 && iv != null) {
			throw new InvalidAlgorithmParameterException("IV not used");
		} else if (iv != null && iv.length != expectedIvLength) {
			throw new InvalidAlgorithmParameterException("expected IV length of " + expectedIvLength + " but was " + iv.length);
		}
		this.iv = iv;
		modeBlockSize = mode == Mode.CTR ? 1 : AES_BLOCK_SIZE;
		reset();
	}

	@Override
	int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen)
	    throws ShortBufferException {
		final int bytesLeft = output.length - outputOffset;
		if (bytesLeft < maximumLen)
			throw new ShortBufferException("output buffer too small during update: " + bytesLeft + " < " + maximumLen);
		if (overlaps(input, inputOffset, inputLen, output, outputOffset, maximumLen)) {
			input = Arrays.copyOfRange(input, inputOffset, inputOffset + inputLen);
			inputOffset = 0;
		}
		int written = NativeCrypto.EVP_CipherUpdate(ctx, output, outputOffset, input, inputOffset, inputLen);
		pending += inputLen - written;
		calledUpdate = true;
		return written;
	}

	int doFinalInternal(byte[] output, int outputOffset, int maximumLen)
	    throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
		/* decrypting without any input: nothing to output, and OpenSSL would complain about the missing block */
		if (!isEncrypting() && !calledUpdate)
			return 0;
		final int bytesLeft = output.length - outputOffset;
		if (bytesLeft >= maximumLen)
			return NativeCrypto.EVP_CipherFinal(ctx, output, outputOffset);
		final byte[] lastBlock = new byte[AES_BLOCK_SIZE];
		final int writtenBytes = NativeCrypto.EVP_CipherFinal(ctx, lastBlock, 0);
		if (writtenBytes > bytesLeft)
			throw new ShortBufferException("buffer is too short: " + writtenBytes + " > " + bytesLeft);
		System.arraycopy(lastBlock, 0, output, outputOffset, writtenBytes);
		return writtenBytes;
	}

	@Override
	protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
	    throws IllegalBlockSizeException, BadPaddingException {
		try {
			final int maximumLen = getOutputSizeForFinal(inputLen);
			final byte[] output = new byte[maximumLen];
			int bytesWritten = 0;
			try {
				if (inputLen > 0)
					bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
				bytesWritten += doFinalInternal(output, bytesWritten, maximumLen - bytesWritten);
			} catch (ShortBufferException e) {
				throw new RuntimeException("our calculated buffer was too small", e);
			}
			if (bytesWritten == output.length)
				return output;
			else if (bytesWritten == 0)
				return EMPTY;
			else
				return Arrays.copyOfRange(output, 0, bytesWritten);
		} finally {
			reset();
		}
	}

	@Override
	protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		boolean resetNeeded = true;
		try {
			if (output == null)
				throw new NullPointerException("output == null");
			int maximumLen = getOutputSizeForFinal(inputLen);
			int bytesWritten = 0;
			if (inputLen > 0) {
				bytesWritten = updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
				outputOffset += bytesWritten;
				maximumLen -= bytesWritten;
			}
			return bytesWritten + doFinalInternal(output, outputOffset, maximumLen);
		} catch (ShortBufferException e) {
			resetNeeded = false;
			throw e;
		} finally {
			if (resetNeeded)
				reset();
		}
	}

	@Override
	int getOutputSizeForFinal(int inputLen) {
		if (modeBlockSize == 1)
			return inputLen;
		if (padding == Padding.NOPADDING)
			return pending + inputLen;
		int totalLen = inputLen + pending;
		totalLen += ((totalLen % modeBlockSize != 0) || isEncrypting()) ? modeBlockSize : 0;
		return totalLen - (totalLen % modeBlockSize);
	}

	@Override
	int getOutputSizeForUpdate(int inputLen) {
		return getOutputSizeForFinal(inputLen);
	}

	private void reset() {
		NativeCrypto.EVP_CipherInit(ctx, mode.ordinal(), encodedKey, iv, isEncrypting(), padding == Padding.PKCS5PADDING);
		calledUpdate = false;
		pending = 0;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			NativeCrypto.EVP_CIPHER_CTX_free(ctx);
			ctx = 0;
		} finally {
			super.finalize();
		}
	}

	public static class AES extends OpenSSLEvpCipherAES {
		AES(Mode mode, Padding padding) { super(mode, padding); }

		@Override
		void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
			if (keyLength != 16 && keyLength != 24 && keyLength != 32)
				throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes");
		}

		public static class ECB_NoPadding extends AES {
			public ECB_NoPadding() { super(Mode.ECB, Padding.NOPADDING); }
		}
		public static class ECB_PKCS5Padding extends AES {
			public ECB_PKCS5Padding() { super(Mode.ECB, Padding.PKCS5PADDING); }
		}
		public static class CBC_NoPadding extends AES {
			public CBC_NoPadding() { super(Mode.CBC, Padding.NOPADDING); }
		}
		public static class CBC_PKCS5Padding extends AES {
			public CBC_PKCS5Padding() { super(Mode.CBC, Padding.PKCS5PADDING); }
		}
		public static class CTR extends AES {
			public CTR() { super(Mode.CTR, Padding.NOPADDING); }
		}
	}

	public static class AES_128 extends OpenSSLEvpCipherAES {
		AES_128(Mode mode, Padding padding) { super(mode, padding); }

		@Override
		void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
			if (keyLength != 16)
				throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes");
		}

		public static class ECB_NoPadding extends AES_128 {
			public ECB_NoPadding() { super(Mode.ECB, Padding.NOPADDING); }
		}
		public static class ECB_PKCS5Padding extends AES_128 {
			public ECB_PKCS5Padding() { super(Mode.ECB, Padding.PKCS5PADDING); }
		}
		public static class CBC_NoPadding extends AES_128 {
			public CBC_NoPadding() { super(Mode.CBC, Padding.NOPADDING); }
		}
		public static class CBC_PKCS5Padding extends AES_128 {
			public CBC_PKCS5Padding() { super(Mode.CBC, Padding.PKCS5PADDING); }
		}
	}

	public static class AES_256 extends OpenSSLEvpCipherAES {
		AES_256(Mode mode, Padding padding) { super(mode, padding); }

		@Override
		void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
			if (keyLength != 32)
				throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes");
		}

		public static class ECB_NoPadding extends AES_256 {
			public ECB_NoPadding() { super(Mode.ECB, Padding.NOPADDING); }
		}
		public static class ECB_PKCS5Padding extends AES_256 {
			public ECB_PKCS5Padding() { super(Mode.ECB, Padding.PKCS5PADDING); }
		}
		public static class CBC_NoPadding extends AES_256 {
			public CBC_NoPadding() { super(Mode.CBC, Padding.NOPADDING); }
		}
		public static class CBC_PKCS5Padding extends AES_256 {
			public CBC_PKCS5Padding() { super(Mode.CBC, Padding.PKCS5PADDING); }
		}
	}
}
