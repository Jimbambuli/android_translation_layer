/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a single algorithm that can be used by an {@link IpSecTransform}.
 *
 * AOSP modules_Connectivity framework-t/src/android/net/IpSecAlgorithm.java. The supported algorithms are
 * all of ALGO_TO_REQUIRED_FIRST_SDK: AOSP's loadAlgos() adds each of them when "ro.vendor.api_level" (default
 * 10000) is at least the SDK level that guarantees it, and the optional-algorithm resource adds nothing more.
 */
public final class IpSecAlgorithm implements Parcelable {
	public static final String CRYPT_NULL = "ecb(cipher_null)";
	public static final String CRYPT_AES_CBC = "cbc(aes)";
	public static final String CRYPT_AES_CTR = "rfc3686(ctr(aes))";
	public static final String AUTH_HMAC_MD5 = "hmac(md5)";
	public static final String AUTH_HMAC_SHA1 = "hmac(sha1)";
	public static final String AUTH_HMAC_SHA256 = "hmac(sha256)";
	public static final String AUTH_HMAC_SHA384 = "hmac(sha384)";
	public static final String AUTH_HMAC_SHA512 = "hmac(sha512)";
	public static final String AUTH_AES_XCBC = "xcbc(aes)";
	public static final String AUTH_AES_CMAC = "cmac(aes)";
	public static final String AUTH_CRYPT_AES_GCM = "rfc4106(gcm(aes))";
	public static final String AUTH_CRYPT_CHACHA20_POLY1305 = "rfc7539esp(chacha20,poly1305)";

	private static final Set<String> ENABLED_ALGOS;

	static {
		final Set<String> enabledAlgos = new HashSet<>();
		enabledAlgos.add(CRYPT_AES_CBC);
		enabledAlgos.add(AUTH_HMAC_MD5);
		enabledAlgos.add(AUTH_HMAC_SHA1);
		enabledAlgos.add(AUTH_HMAC_SHA256);
		enabledAlgos.add(AUTH_HMAC_SHA384);
		enabledAlgos.add(AUTH_HMAC_SHA512);
		enabledAlgos.add(AUTH_CRYPT_AES_GCM);
		enabledAlgos.add(CRYPT_AES_CTR);
		enabledAlgos.add(AUTH_AES_XCBC);
		enabledAlgos.add(AUTH_AES_CMAC);
		enabledAlgos.add(AUTH_CRYPT_CHACHA20_POLY1305);
		ENABLED_ALGOS = Collections.unmodifiableSet(enabledAlgos);
	}

	private final String mName;
	private final byte[] mKey;
	private final int mTruncLenBits;

	public IpSecAlgorithm(@NonNull String algorithm, @NonNull byte[] key) {
		this(algorithm, key, 0);
	}

	public IpSecAlgorithm(@NonNull String algorithm, @NonNull byte[] key, int truncLenBits) {
		mName = algorithm;
		mKey = key.clone();
		mTruncLenBits = truncLenBits;
		checkValidOrThrow(mName, mKey.length * 8, mTruncLenBits);
	}

	@NonNull
	public String getName() {
		return mName;
	}

	@NonNull
	public byte[] getKey() {
		return mKey.clone();
	}

	public int getTruncationLengthBits() {
		return mTruncLenBits;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mName);
		out.writeByteArray(mKey);
		out.writeInt(mTruncLenBits);
	}

	public static final Parcelable.Creator<IpSecAlgorithm> CREATOR = new Parcelable.Creator<IpSecAlgorithm>() {
		public IpSecAlgorithm createFromParcel(Parcel in) {
			final String name = in.readString();
			final byte[] key = in.createByteArray();
			final int truncLenBits = in.readInt();

			return new IpSecAlgorithm(name, key, truncLenBits);
		}

		public IpSecAlgorithm[] newArray(int size) {
			return new IpSecAlgorithm[size];
		}
	};

	@NonNull
	public static Set<String> getSupportedAlgorithms() {
		return ENABLED_ALGOS;
	}

	private static void checkValidOrThrow(String name, int keyLen, int truncLen) {
		final boolean isValidLen;
		final boolean isValidTruncLen;

		if (!getSupportedAlgorithms().contains(name)) {
			throw new IllegalArgumentException("Unsupported algorithm: " + name);
		}

		switch (name) {
		case CRYPT_AES_CBC:
			isValidLen = keyLen == 128 || keyLen == 192 || keyLen == 256;
			isValidTruncLen = true;
			break;
		case CRYPT_AES_CTR:
			// The keying material for AES-CTR is a key plus a 32-bit salt
			isValidLen = keyLen == 128 + 32 || keyLen == 192 + 32 || keyLen == 256 + 32;
			isValidTruncLen = true;
			break;
		case AUTH_HMAC_MD5:
			isValidLen = keyLen == 128;
			isValidTruncLen = truncLen >= 96 && truncLen <= 128;
			break;
		case AUTH_HMAC_SHA1:
			isValidLen = keyLen == 160;
			isValidTruncLen = truncLen >= 96 && truncLen <= 160;
			break;
		case AUTH_HMAC_SHA256:
			isValidLen = keyLen == 256;
			isValidTruncLen = truncLen >= 96 && truncLen <= 256;
			break;
		case AUTH_HMAC_SHA384:
			isValidLen = keyLen == 384;
			isValidTruncLen = truncLen >= 192 && truncLen <= 384;
			break;
		case AUTH_HMAC_SHA512:
			isValidLen = keyLen == 512;
			isValidTruncLen = truncLen >= 256 && truncLen <= 512;
			break;
		case AUTH_AES_XCBC:
			isValidLen = keyLen == 128;
			isValidTruncLen = truncLen == 96;
			break;
		case AUTH_AES_CMAC:
			isValidLen = keyLen == 128;
			isValidTruncLen = truncLen == 96;
			break;
		case AUTH_CRYPT_AES_GCM:
			// The keying material for GCM is a key plus a 32-bit salt
			isValidLen = keyLen == 128 + 32 || keyLen == 192 + 32 || keyLen == 256 + 32;
			isValidTruncLen = truncLen == 64 || truncLen == 96 || truncLen == 128;
			break;
		case AUTH_CRYPT_CHACHA20_POLY1305:
			// The keying material for ChaCha20Poly1305 is a key plus a 32-bit salt
			isValidLen = keyLen == 256 + 32;
			isValidTruncLen = truncLen == 128;
			break;
		default:
			// Should never hit here.
			throw new IllegalArgumentException("Couldn't find an algorithm: " + name);
		}

		if (!isValidLen) {
			throw new IllegalArgumentException("Invalid key material keyLength: " + keyLen);
		}
		if (!isValidTruncLen) {
			throw new IllegalArgumentException("Invalid truncation keyLength: " + truncLen);
		}
	}

	@Override
	@NonNull
	public String toString() {
		return new StringBuilder().append("{mName=").append(mName).append(", mTruncLenBits=").append(mTruncLenBits).append("}").toString();
	}
}
