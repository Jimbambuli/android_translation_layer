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
			/* AOSP maps -1 to (0, AUTH_BIOMETRIC_STRONG), i.e. a duration of 0; the auth type is not ported */
			mUserAuthenticationValidityDurationSeconds = seconds == -1 ? 0 : seconds;
			return this;
		}

		public KeyProtection build() {
			return new KeyProtection(this);
		}
	}
}
