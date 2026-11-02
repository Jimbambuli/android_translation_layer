package android.content.pm;

/**
 * Information pertaining to the signing certificates used to sign a package.
 *
 * As AOSP's SigningInfo over PackageParser.SigningDetails, with only the APK's signers
 * (ATL knows no key rotation, so there are no past signing certificates).
 */
public final class SigningInfo {
	private final Signature[] signatures;

	public SigningInfo() {
		this.signatures = null;
	}

	/** @hide */
	public SigningInfo(Signature[] signatures) {
		this.signatures = signatures;
	}

	/**
	 * Although relatively uncommon, packages may be signed by more than one signer, in which case
	 * their identity is viewed as being the set of all signers, not just any one.
	 */
	public boolean hasMultipleSigners() {
		return signatures != null && signatures.length > 1;
	}

	/**
	 * APK Signature Scheme v3 enables packages to provide a proof-of-rotation record that the
	 * platform verifies, and uses, to allow the use of new signing certificates.
	 */
	public boolean hasPastSigningCertificates() {
		return false;
	}

	/**
	 * Returns the signing certificates this package has proven it is authorized to use. Returns
	 * null if the package is signed by multiple signers.
	 */
	public Signature[] getSigningCertificateHistory() {
		if (hasMultipleSigners()) {
			return null;
		}
		return getApkContentsSigners();
	}

	/**
	 * Returns the signing certificates used to sign the APK contents of this application.
	 */
	public Signature[] getApkContentsSigners() {
		return signatures == null ? null : signatures.clone();
	}
}
