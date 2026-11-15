package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information pertaining to the signing certificates used to sign a package.
 *
 * As AOSP's SigningInfo over PackageParser.SigningDetails, with only the APK's signers
 * (ATL knows no key rotation, so there are no past signing certificates).
 */
public final class SigningInfo implements Parcelable {
	private final Signature[] signatures;

	/* AOSP SigningInfo(Parcel) / writeToParcel() / CREATOR: the wire format of android-14's SigningDetails (unknown flag,
	 * signers, signature scheme version, public keys, past signing certificates). ATL knows neither the scheme
	 * version (written as 0, SignatureSchemeVersion.UNKNOWN), the public keys nor rotation (both written as null). */
	private SigningInfo(Parcel in) {
		if (in.readBoolean()) {
			signatures = null;
			return;
		}
		signatures = in.createTypedArray(Signature.CREATOR);
		in.readInt();
		in.readArraySet(null);
		in.createTypedArray(Signature.CREATOR);
	}

	@Override
	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeBoolean(signatures == null);
		if (signatures == null) {
			return;
		}
		dest.writeTypedArray(signatures, parcelableFlags);
		dest.writeInt(0);
		dest.writeArraySet(null);
		dest.writeTypedArray(null, parcelableFlags);
	}

	public static final Parcelable.Creator<SigningInfo> CREATOR = new Parcelable.Creator<SigningInfo>() {
		@Override
		public SigningInfo createFromParcel(Parcel source) {
			return new SigningInfo(source);
		}

		@Override
		public SigningInfo[] newArray(int size) {
			return new SigningInfo[size];
		}
	};

	public SigningInfo() {
		this.signatures = null;
	}

	/** @hide */
	public SigningInfo(Signature[] signatures) {
		this.signatures = signatures;
	}

	@Override
	public int describeContents() {
		return 0;
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
