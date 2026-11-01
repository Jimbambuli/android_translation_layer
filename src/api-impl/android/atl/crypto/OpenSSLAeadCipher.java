/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * AES-GCM and ChaCha20-Poly1305, after Conscrypt's OpenSSLAeadCipher: input and AAD are collected until doFinal(),
 * which seals or opens the message in one native call (Conscrypt's EVP_AEAD_CTX_seal/open). A key and IV that were
 * used for encryption cannot be used again. Like Conscrypt, AES-GCM takes 128- and 256-bit keys and a 12-byte IV;
 * anything else is left to the next provider.
 */
public abstract class OpenSSLAeadCipher extends OpenSSLCipher {
	static final int DEFAULT_TAG_SIZE_BITS = 16 * 8;
	private static final int NONCE_LENGTH = 12;

	private byte[] previousKey;
	private byte[] previousIv;
	private boolean mustInitialize;
	private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
	private ByteArrayOutputStream aadBuf;
	int tagLengthInBytes;

	abstract int getAead();

	private void checkInitialization() {
		if (mustInitialize)
			throw new IllegalStateException("Cannot re-use same key and IV for multiple encryptions");
	}

	private void reset() {
		aadBuf = null;
		buf.reset();
	}

	@Override
	void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		byte[] iv;
		final int tagLenBits;
		if (params instanceof GCMParameterSpec) {
			iv = ((GCMParameterSpec)params).getIV();
			tagLenBits = ((GCMParameterSpec)params).getTLen();
		} else if (params instanceof IvParameterSpec) {
			iv = ((IvParameterSpec)params).getIV();
			tagLenBits = DEFAULT_TAG_SIZE_BITS;
		} else {
			iv = null;
			tagLenBits = DEFAULT_TAG_SIZE_BITS;
		}
		checkSupportedTagLength(tagLenBits);
		tagLengthInBytes = tagLenBits / 8;
		if (iv == null) {
			if (!isEncrypting())
				throw new InvalidAlgorithmParameterException("IV must be specified");
			iv = randomIv(NONCE_LENGTH, random);
		} else if (iv.length != NONCE_LENGTH) {
			throw new InvalidAlgorithmParameterException("Expected IV length of " + NONCE_LENGTH + " but was " + iv.length);
		}
		if (isEncrypting()) {
			if (previousKey != null && MessageDigest.isEqual(previousKey, encodedKey) && MessageDigest.isEqual(previousIv, iv)) {
				mustInitialize = true;
				throw new InvalidAlgorithmParameterException("When using AEAD key and IV must not be re-used");
			}
			previousKey = encodedKey;
			previousIv = iv;
		}
		mustInitialize = false;
		this.iv = iv;
		reset();
	}

	void checkSupportedTagLength(int tagLenBits) throws InvalidAlgorithmParameterException {
		/* Conscrypt checks only the multiple of 8 here and BoringSSL the range when it seals */
		if (tagLenBits % 8 != 0 || tagLenBits < 8 || tagLenBits > 128)
			throw new InvalidAlgorithmParameterException("Tag length must be a multiple of 8; was " + tagLenBits);
	}

	@Override
	protected void engineSetPadding(String paddingStr) throws NoSuchPaddingException {
		if (!paddingStr.toUpperCase(Locale.US).equals("NOPADDING"))
			throw new NoSuchPaddingException("Must be NoPadding for AEAD ciphers");
	}

	@Override
	int getOutputSizeForUpdate(int inputLen) {
		return 0;
	}

	@Override
	int getOutputSizeForFinal(int inputLen) {
		if (isEncrypting())
			return buf.size() + inputLen + tagLengthInBytes;
		else
			return Math.max(0, buf.size() + inputLen - tagLengthInBytes);
	}

	@Override
	int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen)
	    throws ShortBufferException {
		checkInitialization();
		buf.write(input, inputOffset, inputLen);
		return 0;
	}

	@Override
	protected void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
		checkInitialization();
		if (aadBuf == null)
			aadBuf = new ByteArrayOutputStream(inputLen);
		aadBuf.write(input, inputOffset, inputLen);
	}

	@Override
	protected void engineUpdateAAD(ByteBuffer input) {
		byte[] bytes = new byte[input.remaining()];
		input.get(bytes);
		engineUpdateAAD(bytes, 0, bytes.length);
	}

	@Override
	protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
	    throws IllegalBlockSizeException, BadPaddingException {
		final byte[] output = new byte[getOutputSizeForFinal(inputLen)];
		final int bytesWritten;
		try {
			bytesWritten = doFinalInternal(input, inputOffset, inputLen, output, 0);
		} catch (ShortBufferException e) {
			throw new RuntimeException("our calculated buffer was too small", e);
		}
		return bytesWritten == output.length ? output : Arrays.copyOf(output, bytesWritten);
	}

	@Override
	protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		if (output == null)
			throw new NullPointerException("output == null");
		if (getOutputSizeForFinal(inputLen) > output.length - outputOffset)
			throw new ShortBufferException("Insufficient output space");
		return doFinalInternal(input, inputOffset, inputLen, output, outputOffset);
	}

	private int doFinalInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		checkInitialization();
		byte[] in;
		int inOffset, inLen;
		if (buf.size() > 0) {
			if (inputLen > 0)
				buf.write(input, inputOffset, inputLen);
			in = buf.toByteArray();
			inOffset = 0;
			inLen = in.length;
		} else {
			if (inputLen == 0 && input == null) {
				in = EMPTY;
				inOffset = 0;
			} else if (overlaps(input, inputOffset, inputLen, output, outputOffset, inputLen + tagLengthInBytes)) {
				in = Arrays.copyOfRange(input, inputOffset, inputOffset + inputLen);
				inOffset = 0;
			} else {
				in = input;
				inOffset = inputOffset;
			}
			inLen = inputLen;
		}
		byte[] aad = aadBuf == null ? null : aadBuf.toByteArray();
		final int bytesWritten;
		try {
			if (isEncrypting())
				bytesWritten = NativeCrypto.AEAD_seal(getAead(), encodedKey, tagLengthInBytes, output, outputOffset, iv, in, inOffset, inLen, aad);
			else
				bytesWritten = NativeCrypto.AEAD_open(getAead(), encodedKey, tagLengthInBytes, output, outputOffset, iv, in, inOffset, inLen, aad);
		} finally {
			reset(); // also after a failed open, which Conscrypt leaves to the next init
		}
		if (isEncrypting())
			mustInitialize = true;
		return bytesWritten;
	}

	public static class AES_GCM extends OpenSSLAeadCipher {
		private static final int AES_BLOCK_SIZE = 16;

		@Override
		int getAead() {
			return NativeCrypto.AES_GCM;
		}

		@Override
		protected void engineSetMode(String modeStr) throws NoSuchAlgorithmException {
			if (!modeStr.equalsIgnoreCase("GCM"))
				throw new NoSuchAlgorithmException("Mode must be GCM");
		}

		@Override
		void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
			if (keyLength != 16 && keyLength != 32)
				throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes (must be 16 or 32)");
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
		protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
			if (params != null) {
				try {
					return params.getParameterSpec(GCMParameterSpec.class);
				} catch (InvalidParameterSpecException e) {
					return super.getParameterSpec(params);
				}
			}
			return null;
		}

		@Override
		protected AlgorithmParameters engineGetParameters() {
			if (iv == null)
				return null;
			try {
				AlgorithmParameters params = AlgorithmParameters.getInstance("GCM");
				params.init(new GCMParameterSpec(tagLengthInBytes * 8, iv));
				return params;
			} catch (NoSuchAlgorithmException e) {
				throw (Error) new AssertionError("GCM not supported").initCause(e);
			} catch (InvalidParameterSpecException e) {
				return null;
			}
		}

		public static class AES_128 extends AES_GCM {
			@Override
			void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
				if (keyLength != 16)
					throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes (must be 16)");
			}
		}

		public static class AES_256 extends AES_GCM {
			@Override
			void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
				if (keyLength != 32)
					throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes (must be 32)");
			}
		}
	}

	public static class ChaCha20Poly1305 extends OpenSSLAeadCipher {
		@Override
		int getAead() {
			return NativeCrypto.CHACHA20_POLY1305;
		}

		@Override
		protected void engineSetMode(String modeStr) throws NoSuchAlgorithmException {
			if (!modeStr.equalsIgnoreCase("Poly1305"))
				throw new NoSuchAlgorithmException("Mode must be Poly1305");
		}

		@Override
		void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
			if (keyLength != 32)
				throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes (must be 32)");
		}

		@Override
		void checkSupportedTagLength(int tagLenBits) throws InvalidAlgorithmParameterException {
			if (tagLenBits != DEFAULT_TAG_SIZE_BITS)
				throw new InvalidAlgorithmParameterException("Tag length must be " + DEFAULT_TAG_SIZE_BITS + " bits");
		}

		@Override
		String getBaseCipherName() {
			return "ChaCha20";
		}

		@Override
		protected int engineGetBlockSize() {
			return 0;
		}
	}
}
