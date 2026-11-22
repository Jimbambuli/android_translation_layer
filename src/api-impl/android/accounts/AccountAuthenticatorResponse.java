package android.accounts;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/* AOSP AccountAuthenticatorResponse: the authenticator's answer to the account manager service's session */
public class AccountAuthenticatorResponse implements Parcelable {
	private IAccountAuthenticatorResponse mAccountAuthenticatorResponse;

	/** @hide */
	public AccountAuthenticatorResponse(IAccountAuthenticatorResponse response) {
		mAccountAuthenticatorResponse = response;
	}

	public AccountAuthenticatorResponse(Parcel parcel) {
		mAccountAuthenticatorResponse = IAccountAuthenticatorResponse.Stub.asInterface(parcel.readStrongBinder());
	}

	public void onResult(Bundle result) {
		try {
			mAccountAuthenticatorResponse.onResult(result);
		} catch (RemoteException e) {
			// this should never happen
		}
	}

	public void onRequestContinued() {
		try {
			mAccountAuthenticatorResponse.onRequestContinued();
		} catch (RemoteException e) {
			// this should never happen
		}
	}

	public void onError(int errorCode, String errorMessage) {
		try {
			mAccountAuthenticatorResponse.onError(errorCode, errorMessage);
		} catch (RemoteException e) {
			// this should never happen
		}
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStrongBinder(mAccountAuthenticatorResponse.asBinder());
	}

	public static final Creator<AccountAuthenticatorResponse> CREATOR = new Creator<AccountAuthenticatorResponse>() {
		public AccountAuthenticatorResponse createFromParcel(Parcel source) {
			return new AccountAuthenticatorResponse(source);
		}

		public AccountAuthenticatorResponse[] newArray(int size) {
			return new AccountAuthenticatorResponse[size];
		}
	};
}
