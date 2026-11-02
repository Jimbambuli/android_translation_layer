package android.accounts;

import android.os.Bundle;
import android.os.Parcelable;

/* ATL: there is no account manager service on the other end, so the answers go nowhere */
public class AccountAuthenticatorResponse implements Parcelable {
	public static final Creator<AccountAuthenticatorResponse> CREATOR = null;

	public void onResult(Bundle result) {}

	public void onRequestContinued() {}

	public void onError(int errorCode, String errorMessage) {}
}
