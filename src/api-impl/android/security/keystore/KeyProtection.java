package android.security.keystore;

import java.security.KeyStore.ProtectionParameter;
import java.util.Date;

/* AOSP keystore/java/android/security/keystore/KeyProtection.java: the plain parameters of the Builder. Without
 * secure hardware, a lock screen or biometrics there is nothing that evaluates the authentication, strong box, user
 * presence, confirmation or unlocked-device settings, so those are not ported. */
public final class KeyProtection implements ProtectionParameter {
	private final Date mKeyValidityStart;
	private final Date mKeyValidityForOriginationEnd;
	private final Date mKeyValidityForConsumptionEnd;
	private final int mPurposes;
	private final String[] mEncryptionPaddings;
	private final String[] mSignaturePaddings;
	private final String[] mDigests;
	private final String[] mBlockModes;
	private final boolean mRandomizedEncryptionRequired;
	private final boolean mUserAuthenticationRequired;
	private final int mUserAuthenticationValidityDurationSeconds;
	private final java.util.Set<String> mMgf1Digests;
	private final int mUserAuthenticationType;
	private final boolean mUserPresenceRequired;
	private final boolean mUserAuthenticationValidWhileOnBody;
	private final boolean mInvalidatedByBiometricEnrollment;
	private final boolean mUserConfirmationRequired;
	private final boolean mUnlockedDeviceRequired;
	private final boolean mIsStrongBoxBacked;
	private final int mMaxUsageCount;

	private KeyProtection(Builder b) {
		mKeyValidityStart = b.mKeyValidityStart != null ? (Date) b.mKeyValidityStart.clone() : null;
		mKeyValidityForOriginationEnd = b.mKeyValidityForOriginationEnd != null ? (Date) b.mKeyValidityForOriginationEnd.clone() : null;
		mKeyValidityForConsumptionEnd = b.mKeyValidityForConsumptionEnd != null ? (Date) b.mKeyValidityForConsumptionEnd.clone() : null;
		mPurposes = b.mPurposes;
		mEncryptionPaddings = clone(b.mEncryptionPaddings);
		mSignaturePaddings = clone(b.mSignaturePaddings);
		mDigests = b.mDigests != null ? b.mDigests.clone() : null;
		mBlockModes = clone(b.mBlockModes);
		mRandomizedEncryptionRequired = b.mRandomizedEncryptionRequired;
		mUserAuthenticationRequired = b.mUserAuthenticationRequired;
		mUserAuthenticationValidityDurationSeconds = b.mUserAuthenticationValidityDurationSeconds;
		mMgf1Digests = b.mMgf1Digests;
		mUserAuthenticationType = b.mUserAuthenticationType;
		mUserPresenceRequired = b.mUserPresenceRequired;
		mUserAuthenticationValidWhileOnBody = b.mUserAuthenticationValidWhileOnBody;
		mInvalidatedByBiometricEnrollment = b.mInvalidatedByBiometricEnrollment;
		mUserConfirmationRequired = b.mUserConfirmationRequired;
		mUnlockedDeviceRequired = b.mUnlockedDeviceRequired;
		mIsStrongBoxBacked = b.mIsStrongBoxBacked;
		mMaxUsageCount = b.mMaxUsageCount;
	}

	/* AOSP: nullToEmpty + cloneIfNotEmpty */
	private static String[] clone(String[] array) {
		return array == null || array.length == 0 ? new String[0] : array.clone();
	}

	public Date getKeyValidityStart() {
		return mKeyValidityStart != null ? (Date) mKeyValidityStart.clone() : null;
	}

	public Date getKeyValidityForConsumptionEnd() {
		return mKeyValidityForConsumptionEnd != null ? (Date) mKeyValidityForConsumptionEnd.clone() : null;
	}

	public Date getKeyValidityForOriginationEnd() {
		return mKeyValidityForOriginationEnd != null ? (Date) mKeyValidityForOriginationEnd.clone() : null;
	}

	public int getPurposes() {
		return mPurposes;
	}

	public String[] getEncryptionPaddings() {
		return clone(mEncryptionPaddings);
	}

	public String[] getSignaturePaddings() {
		return clone(mSignaturePaddings);
	}

	public String[] getDigests() {
		if (mDigests == null) {
			throw new IllegalStateException("Digests not specified");
		}
		return mDigests.clone();
	}

	public boolean isDigestsSpecified() {
		return mDigests != null;
	}

	public String[] getBlockModes() {
		return clone(mBlockModes);
	}

	public boolean isRandomizedEncryptionRequired() {
		return mRandomizedEncryptionRequired;
	}

	public boolean isUserAuthenticationRequired() {
		return mUserAuthenticationRequired;
	}

	public int getUserAuthenticationValidityDurationSeconds() {
		return mUserAuthenticationValidityDurationSeconds;
	}

	/* AOSP KeyProtection.getMgf1Digests() */
	public java.util.Set<String> getMgf1Digests() {
		if (mMgf1Digests.isEmpty()) {
			throw new IllegalStateException("Mask generation function (MGF) not specified");
		}
		return new java.util.HashSet<>(mMgf1Digests);
	}

	public boolean isMgf1DigestsSpecified() {
		return !mMgf1Digests.isEmpty();
	}

	public boolean isUserConfirmationRequired() {
		return mUserConfirmationRequired;
	}

	public int getUserAuthenticationType() {
		return mUserAuthenticationType;
	}

	public boolean isUserPresenceRequired() {
		return mUserPresenceRequired;
	}

	public boolean isUserAuthenticationValidWhileOnBody() {
		return mUserAuthenticationValidWhileOnBody;
	}

	public boolean isInvalidatedByBiometricEnrollment() {
		return mInvalidatedByBiometricEnrollment;
	}

	public boolean isUnlockedDeviceRequired() {
		return mUnlockedDeviceRequired;
	}

	public boolean isStrongBoxBacked() {
		return mIsStrongBoxBacked;
	}

	public int getMaxUsageCount() {
		return mMaxUsageCount;
	}

	public static final class Builder {
		private Date mKeyValidityStart;
		private Date mKeyValidityForOriginationEnd;
		private Date mKeyValidityForConsumptionEnd;
		private final int mPurposes;
		private String[] mEncryptionPaddings;
		private String[] mSignaturePaddings;
		private String[] mDigests;
		private String[] mBlockModes;
		private boolean mRandomizedEncryptionRequired = true;
		private boolean mUserAuthenticationRequired;
		private int mUserAuthenticationValidityDurationSeconds = 0;
		private java.util.Set<String> mMgf1Digests = java.util.Collections.emptySet();
		private int mUserAuthenticationType = KeyProperties.AUTH_BIOMETRIC_STRONG;
		private boolean mUserPresenceRequired = false;
		private boolean mUserAuthenticationValidWhileOnBody;
		private boolean mInvalidatedByBiometricEnrollment = true;
		private boolean mUserConfirmationRequired;
		private boolean mUnlockedDeviceRequired = false;
		private boolean mIsStrongBoxBacked = false;
		private int mMaxUsageCount = KeyProperties.UNRESTRICTED_USAGE_COUNT;

		public Builder(int purposes) {
			mPurposes = purposes;
		}

		public Builder setKeyValidityStart(Date startDate) {
			mKeyValidityStart = startDate != null ? (Date) startDate.clone() : null;
			return this;
		}

		public Builder setKeyValidityEnd(Date endDate) {
			setKeyValidityForOriginationEnd(endDate);
			return setKeyValidityForConsumptionEnd(endDate);
		}

		public Builder setKeyValidityForOriginationEnd(Date endDate) {
			mKeyValidityForOriginationEnd = endDate != null ? (Date) endDate.clone() : null;
			return this;
		}

		public Builder setKeyValidityForConsumptionEnd(Date endDate) {
			mKeyValidityForConsumptionEnd = endDate != null ? (Date) endDate.clone() : null;
			return this;
		}

		public Builder setEncryptionPaddings(String... paddings) {
			mEncryptionPaddings = paddings == null || paddings.length == 0 ? paddings : paddings.clone();
			return this;
		}

		public Builder setSignaturePaddings(String... paddings) {
			mSignaturePaddings = paddings == null || paddings.length == 0 ? paddings : paddings.clone();
			return this;
		}

		public Builder setDigests(String... digests) {
			mDigests = digests == null || digests.length == 0 ? digests : digests.clone();
			return this;
		}

		public Builder setBlockModes(String... blockModes) {
			mBlockModes = blockModes == null || blockModes.length == 0 ? blockModes : blockModes.clone();
			return this;
		}

		public Builder setRandomizedEncryptionRequired(boolean required) {
			mRandomizedEncryptionRequired = required;
			return this;
		}

		public Builder setUserAuthenticationRequired(boolean required) {
			mUserAuthenticationRequired = required;
			return this;
		}

		public Builder setUserAuthenticationValidityDurationSeconds(int seconds) {
			if (seconds < -1) {
				throw new IllegalArgumentException("seconds must be -1 or larger");
			}
			if (seconds == -1) {
				return setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
			}
			return setUserAuthenticationParameters(seconds, KeyProperties.AUTH_DEVICE_CREDENTIAL | KeyProperties.AUTH_BIOMETRIC_STRONG);
		}

		public Builder setUserAuthenticationParameters(int timeout, int type) {
			if (timeout < 0) {
				throw new IllegalArgumentException("timeout must be 0 or larger");
			}
			mUserAuthenticationValidityDurationSeconds = timeout;
			mUserAuthenticationType = type;
			return this;
		}

		public Builder setMgf1Digests(String... mgf1Digests) {
			mMgf1Digests = java.util.Set.of(mgf1Digests);
			return this;
		}

		public Builder setUserConfirmationRequired(boolean required) {
			mUserConfirmationRequired = required;
			return this;
		}

		public Builder setUserPresenceRequired(boolean required) {
			mUserPresenceRequired = required;
			return this;
		}

		public Builder setUserAuthenticationValidWhileOnBody(boolean remainsValid) {
			mUserAuthenticationValidWhileOnBody = remainsValid;
			return this;
		}

		public Builder setInvalidatedByBiometricEnrollment(boolean invalidateKey) {
			mInvalidatedByBiometricEnrollment = invalidateKey;
			return this;
		}

		public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired) {
			mUnlockedDeviceRequired = unlockedDeviceRequired;
			return this;
		}

		public Builder setIsStrongBoxBacked(boolean isStrongBoxBacked) {
			mIsStrongBoxBacked = isStrongBoxBacked;
			return this;
		}

		public Builder setMaxUsageCount(int maxUsageCount) {
			if (maxUsageCount == KeyProperties.UNRESTRICTED_USAGE_COUNT || maxUsageCount > 0) {
				mMaxUsageCount = maxUsageCount;
				return this;
			}
			throw new IllegalArgumentException("maxUsageCount is not valid");
		}

		public KeyProtection build() {
			return new KeyProtection(this);
		}
	}
}
