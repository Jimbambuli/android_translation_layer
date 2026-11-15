package android.security.keystore;

import java.math.BigInteger;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

public final class KeyGenParameterSpec implements java.security.spec.AlgorithmParameterSpec {

	private String keystoreAlias;
	private int purposes;
	private int keySize;
	private String[] blockModes;
	private String[] encryptionPaddings;
	private boolean userAuthenticationRequired;
	private java.security.spec.AlgorithmParameterSpec algorithmParameterSpec;
	private BigInteger certificateSerialNumber;
	private X500Principal certificateSubject;
	private Date certificateNotBefore;
	private Date certificateNotAfter;
	private String[] digests;
	private String[] signaturePaddings;
	private Date keyValidityStart;
	private Date keyValidityForOriginationEnd;
	private Date keyValidityForConsumptionEnd;
	private boolean randomizedEncryptionRequired = true;
	private int userAuthenticationValidityDurationSeconds = 0;
	private boolean userPresenceRequired = false;
	private boolean userConfirmationRequired = false;
	private boolean unlockedDeviceRequired = false;
	private boolean userAuthenticationValidWhileOnBody = false;
	private boolean invalidatedByBiometricEnrollment = true;
	private boolean strongBoxBacked = false;
	private byte[] attestationChallenge;
	private boolean devicePropertiesAttestationIncluded = false;
	private boolean uniqueIdIncluded = false;
	private long boundToSpecificSecureUserId = 0;
	private boolean criticalToDeviceEncryption = false;
	private int maxUsageCount = -1;
	private String attestKeyAlias;
	private java.util.Set<String> mgf1Digests = java.util.Collections.emptySet();
	private int userAuthenticationType = 0;

	public static class Builder {
		private KeyGenParameterSpec spec = new KeyGenParameterSpec();

		public Builder(String keystoreAlias, int purposes) {
			spec.keystoreAlias = keystoreAlias;
			spec.purposes = purposes;
		}

		public Builder setKeySize(int keySize) {
			spec.keySize = keySize;
			return this;
		}

		public Builder setBlockModes(String[] blockModes) {
			spec.blockModes = blockModes;
			return this;
		}

		public Builder setEncryptionPaddings(String[] encryptionPaddings) {
			spec.encryptionPaddings = encryptionPaddings;
			return this;
		}

		public Builder setUserAuthenticationRequired(boolean userAuthenticationRequired) {
			spec.userAuthenticationRequired = userAuthenticationRequired;
			return this;
		}

		public Builder setAlgorithmParameterSpec(java.security.spec.AlgorithmParameterSpec algorithmParameterSpec) {
			spec.algorithmParameterSpec = algorithmParameterSpec;
			return this;
		}

		public Builder setCertificateSerialNumber(BigInteger certificateSerialNumber) {
			spec.certificateSerialNumber = certificateSerialNumber;
			return this;
		}

		public Builder setCertificateSubject(X500Principal certificateSubject) {
			spec.certificateSubject = certificateSubject;
			return this;
		}

		public Builder setCertificateNotBefore(Date certificateNotBefore) {
			spec.certificateNotBefore = certificateNotBefore;
			return this;
		}

		public Builder setCertificateNotAfter(Date certificateNotAfter) {
			spec.certificateNotAfter = certificateNotAfter;
			return this;
		}

		public Builder setDigests(String[] digests) {
			spec.digests = digests;
			return this;
		}

		public Builder setSignaturePaddings(String[] signaturePaddings) {
			spec.signaturePaddings = signaturePaddings;
			return this;
		}

		public Builder setKeyValidityStart(Date keyValidityStart) {
			spec.keyValidityStart = keyValidityStart;
			return this;
		}

		public Builder setKeyValidityForOriginationEnd(Date keyValidityForOriginationEnd) {
			spec.keyValidityForOriginationEnd = keyValidityForOriginationEnd;
			return this;
		}

		/* AOSP Builder.setKeyValidityEnd(): both the origination and the consumption end */
		public Builder setKeyValidityEnd(Date endDate) {
			setKeyValidityForOriginationEnd(endDate);
			setKeyValidityForConsumptionEnd(endDate);
			return this;
		}

		public Builder setKeyValidityForConsumptionEnd(Date keyValidityForConsumptionEnd) {
			spec.keyValidityForConsumptionEnd = keyValidityForConsumptionEnd;
			return this;
		}

		public Builder setRandomizedEncryptionRequired(boolean randomizedEncryptionRequired) {
			spec.randomizedEncryptionRequired = randomizedEncryptionRequired;
			return this;
		}

		public Builder setUserAuthenticationValidityDurationSeconds(int userAuthenticationValidityDurationSeconds) {
			spec.userAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
			return this;
		}

		public Builder setUserPresenceRequired(boolean userPresenceRequired) {
			spec.userPresenceRequired = userPresenceRequired;
			return this;
		}

		public Builder setUserConfirmationRequired(boolean userConfirmationRequired) {
			spec.userConfirmationRequired = userConfirmationRequired;
			return this;
		}

		public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired) {
			spec.unlockedDeviceRequired = unlockedDeviceRequired;
			return this;
		}

		public Builder setUserAuthenticationValidWhileOnBody(boolean userAuthenticationValidWhileOnBody) {
			spec.userAuthenticationValidWhileOnBody = userAuthenticationValidWhileOnBody;
			return this;
		}

		public Builder setInvalidatedByBiometricEnrollment(boolean invalidatedByBiometricEnrollment) {
			spec.invalidatedByBiometricEnrollment = invalidatedByBiometricEnrollment;
			return this;
		}

		public Builder setIsStrongBoxBacked(boolean strongBoxBacked) {
			spec.strongBoxBacked = strongBoxBacked;
			return this;
		}

		public Builder setAttestationChallenge(byte[] attestationChallenge) {
			spec.attestationChallenge = attestationChallenge;
			return this;
		}

		public Builder setDevicePropertiesAttestationIncluded(boolean devicePropertiesAttestationIncluded) {
			spec.devicePropertiesAttestationIncluded = devicePropertiesAttestationIncluded;
			return this;
		}

		public Builder setUniqueIdIncluded(boolean uniqueIdIncluded) {
			spec.uniqueIdIncluded = uniqueIdIncluded;
			return this;
		}

		public Builder setBoundToSpecificSecureUserId(long boundToSpecificSecureUserId) {
			spec.boundToSpecificSecureUserId = boundToSpecificSecureUserId;
			return this;
		}

		public Builder setCriticalToDeviceEncryption(boolean criticalToDeviceEncryption) {
			spec.criticalToDeviceEncryption = criticalToDeviceEncryption;
			return this;
		}

		public Builder setMaxUsageCount(int maxUsageCount) {
			spec.maxUsageCount = maxUsageCount;
			return this;
		}

		public Builder setAttestKeyAlias(String attestKeyAlias) {
			spec.attestKeyAlias = attestKeyAlias;
			return this;
		}

		public Builder setMgf1Digests(String... mgf1Digests) {
			spec.mgf1Digests = java.util.Set.of(mgf1Digests);
			return this;
		}

		/* AOSP: timeout 0 (or -1 with auth type) means every use needs authentication */
		public Builder setUserAuthenticationParameters(int timeout, int type) {
			if (timeout < 0)
				throw new IllegalArgumentException("timeout must be non-negative");
			spec.userAuthenticationValidityDurationSeconds = timeout;
			spec.userAuthenticationType = type;
			return this;
		}

		public KeyGenParameterSpec build() {
			return spec;
		}
	}

	public int getKeySize() {
		return keySize;
	}

	public String[] getBlockModes() {
		return blockModes;
	}

	public int getPurposes() {
		return purposes;
	}

	public String[] getEncryptionPaddings() {
		return encryptionPaddings;
	}

	public boolean isUserAuthenticationRequired() {
		return userAuthenticationRequired;
	}

	public String getKeystoreAlias() {
		return keystoreAlias;
	}

	public java.security.spec.AlgorithmParameterSpec getAlgorithmParameterSpec() {
		return algorithmParameterSpec;
	}

	public BigInteger getCertificateSerialNumber() {
		return certificateSerialNumber;
	}

	public X500Principal getCertificateSubject() {
		return certificateSubject;
	}

	public Date getCertificateNotBefore() {
		return certificateNotBefore;
	}

	public Date getCertificateNotAfter() {
		return certificateNotAfter;
	}

	/* AOSP: null means not specified, and asking then is an error */
	public String[] getDigests() {
		if (digests == null) {
			throw new IllegalStateException("Digests not specified");
		}
		return digests.clone();
	}

	public boolean isDigestsSpecified() {
		return digests != null;
	}

	public String[] getSignaturePaddings() {
		return signaturePaddings;
	}

	public Date getKeyValidityStart() {
		return keyValidityStart;
	}

	public Date getKeyValidityForOriginationEnd() {
		return keyValidityForOriginationEnd;
	}

	public Date getKeyValidityForConsumptionEnd() {
		return keyValidityForConsumptionEnd;
	}

	public boolean isRandomizedEncryptionRequired() {
		return randomizedEncryptionRequired;
	}

	public int getUserAuthenticationValidityDurationSeconds() {
		return userAuthenticationValidityDurationSeconds;
	}

	public boolean isUserPresenceRequired() {
		return userPresenceRequired;
	}

	public boolean isUserConfirmationRequired() {
		return userConfirmationRequired;
	}

	public boolean isUnlockedDeviceRequired() {
		return unlockedDeviceRequired;
	}

	public boolean isUserAuthenticationValidWhileOnBody() {
		return userAuthenticationValidWhileOnBody;
	}

	public boolean isInvalidatedByBiometricEnrollment() {
		return invalidatedByBiometricEnrollment;
	}

	public boolean isStrongBoxBacked() {
		return strongBoxBacked;
	}

	public byte[] getAttestationChallenge() {
		return attestationChallenge;
	}

	public boolean isDevicePropertiesAttestationIncluded() {
		return devicePropertiesAttestationIncluded;
	}

	public boolean isUniqueIdIncluded() {
		return uniqueIdIncluded;
	}

	public long getBoundToSpecificSecureUserId() {
		return boundToSpecificSecureUserId;
	}

	public boolean isCriticalToDeviceEncryption() {
		return criticalToDeviceEncryption;
	}

	public int getMaxUsageCount() {
		return maxUsageCount;
	}

	public String getAttestKeyAlias() {
		return attestKeyAlias;
	}

	/* AOSP KeyGenParameterSpec.getMgf1Digests() */
	public java.util.Set<String> getMgf1Digests() {
		if (mgf1Digests.isEmpty()) {
			throw new IllegalStateException("Mask generation function (MGF) not specified");
		}
		return new java.util.HashSet<>(mgf1Digests);
	}

	public boolean isMgf1DigestsSpecified() {
		return !mgf1Digests.isEmpty();
	}

	public int getUserAuthenticationType() {
		return userAuthenticationType;
	}
}
