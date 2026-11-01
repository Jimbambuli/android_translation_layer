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

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Base of the symmetric ciphers, after Conscrypt's org.conscrypt.OpenSSLCipher: the key handling, IV parameters,
 * wrap/unwrap and the update() variants that size their own output.
 */
public abstract class OpenSSLCipher extends CipherSpi {
	static final byte[] EMPTY = new byte[0];

	byte[] encodedKey;
	byte[] iv;
	private boolean encrypting;

	abstract void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException;

	abstract int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen)
	    throws ShortBufferException;

	abstract String getBaseCipherName();

	abstract void checkSupportedKeySize(int keySize) throws InvalidKeyException;

	abstract int getOutputSizeForFinal(int inputLen);

	abstract int getOutputSizeForUpdate(int inputLen);

	@Override
	protected int engineGetOutputSize(int inputLen) {
		return Math.max(getOutputSizeForUpdate(inputLen), getOutputSizeForFinal(inputLen));
	}

	@Override
	protected byte[] engineGetIV() {
		return iv;
	}

	@Override
	protected AlgorithmParameters engineGetParameters() {
		if (iv != null && iv.length > 0) {
			try {
				AlgorithmParameters params = AlgorithmParameters.getInstance(getBaseCipherName());
				params.init(new IvParameterSpec(iv));
				return params;
			} catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
				return null;
			}
		}
		return null;
	}

	protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
		if (params != null) {
			try {
				return params.getParameterSpec(IvParameterSpec.class);
			} catch (InvalidParameterSpecException e) {
				throw new InvalidAlgorithmParameterException("Params must be convertible to IvParameterSpec", e);
			}
		}
		return null;
	}

	@Override
	protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
		checkAndSetEncodedKey(opmode, key);
		try {
			engineInitInternal(this.encodedKey, null, random);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException(e); // as Conscrypt
		}
	}

	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		checkAndSetEncodedKey(opmode, key);
		engineInitInternal(this.encodedKey, params, random);
	}

	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random)
	    throws InvalidKeyException, InvalidAlgorithmParameterException {
		engineInit(opmode, key, getParameterSpec(params), random);
	}

	@Override
	protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
		final int maximumLen = getOutputSizeForUpdate(inputLen);
		final byte[] output = maximumLen > 0 ? new byte[maximumLen] : EMPTY;
		final int bytesWritten;
		try {
			bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
		} catch (ShortBufferException e) {
			throw new RuntimeException("calculated buffer size was wrong: " + maximumLen);
		}
		if (output.length == bytesWritten)
			return output;
		else if (bytesWritten == 0)
			return EMPTY;
		else
			return Arrays.copyOfRange(output, 0, bytesWritten);
	}

	@Override
	protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	    throws ShortBufferException {
		return updateInternal(input, inputOffset, inputLen, output, outputOffset, getOutputSizeForUpdate(inputLen));
	}

	@Override
	protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
		try {
			byte[] encoded = key.getEncoded();
			return engineDoFinal(encoded, 0, encoded.length);
		} catch (BadPaddingException e) {
			IllegalBlockSizeException newE = new IllegalBlockSizeException();
			newE.initCause(e);
			throw newE;
		}
	}

	@Override
	protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
	    throws InvalidKeyException, NoSuchAlgorithmException {
		try {
			byte[] encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
			if (wrappedKeyType == Cipher.PUBLIC_KEY)
				return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePublic(new X509EncodedKeySpec(encoded));
			else if (wrappedKeyType == Cipher.PRIVATE_KEY)
				return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePrivate(new PKCS8EncodedKeySpec(encoded));
			else if (wrappedKeyType == Cipher.SECRET_KEY)
				return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
			else
				throw new UnsupportedOperationException("wrappedKeyType == " + wrappedKeyType);
		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException e) {
			throw new InvalidKeyException(e);
		}
	}

	@Override
	protected int engineGetKeySize(Key key) throws InvalidKeyException {
		if (!(key instanceof SecretKey))
			throw new InvalidKeyException("Only SecretKey is supported");
		byte[] encodedKey = key.getEncoded();
		if (encodedKey == null)
			throw new InvalidKeyException("key.getEncoded() == null");
		checkSupportedKeySize(encodedKey.length);
		return encodedKey.length * 8;
	}

	private void checkAndSetEncodedKey(int opmode, Key key) throws InvalidKeyException {
		if (opmode == Cipher.ENCRYPT_MODE || opmode == Cipher.WRAP_MODE)
			encrypting = true;
		else if (opmode == Cipher.DECRYPT_MODE || opmode == Cipher.UNWRAP_MODE)
			encrypting = false;
		else
			throw new InvalidParameterException("Unsupported opmode " + opmode);
		if (!(key instanceof SecretKey))
			throw new InvalidKeyException("Only SecretKey is supported");
		final byte[] encodedKey = key.getEncoded();
		if (encodedKey == null)
			throw new InvalidKeyException("key.getEncoded() == null");
		checkSupportedKeySize(encodedKey.length);
		this.encodedKey = encodedKey;
	}

	boolean isEncrypting() {
		return encrypting;
	}

	/** a new IV for encryption without one, from the caller's SecureRandom if it gave one */
	static byte[] randomIv(int len, SecureRandom random) {
		byte[] iv = new byte[len];
		if (random != null)
			random.nextBytes(iv);
		else
			NativeCrypto.getrandom(iv);
		return iv;
	}

	/** input as the native code may read it while writing the output: a copy if both overlap in the same array */
	static boolean overlaps(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int outputLen) {
		return input == output && inputOffset != outputOffset && inputOffset < outputOffset + outputLen && outputOffset < inputOffset + inputLen;
	}
}
