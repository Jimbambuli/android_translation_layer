package android.service.credentials;

import android.credentials.CredentialOption;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.Objects;

/**
 * android-16 frameworks/base core/java/android/service/credentials/GetCredentialRequest.java: a request to get
 * credentials, given to a CredentialProviderService (ATL has none; the class is a plain value type).
 */
public final class GetCredentialRequest implements Parcelable {

	private final CallingAppInfo mCallingAppInfo;
	private final List<CredentialOption> mCredentialOptions;

	public GetCredentialRequest(CallingAppInfo callingAppInfo, List<CredentialOption> credentialOptions) {
		this.mCallingAppInfo = Objects.requireNonNull(callingAppInfo, "callingAppInfo must not be null");
		this.mCredentialOptions = Objects.requireNonNull(credentialOptions, "credentialOptions must not be null");
	}

	private GetCredentialRequest(Parcel in) {
		mCallingAppInfo = Objects.requireNonNull(in.readTypedObject(CallingAppInfo.CREATOR));
		mCredentialOptions = Objects.requireNonNull(in.createTypedArrayList(CredentialOption.CREATOR));
	}

	public static final Creator<GetCredentialRequest> CREATOR = new Creator<GetCredentialRequest>() {
		@Override
		public GetCredentialRequest createFromParcel(Parcel in) {
			return new GetCredentialRequest(in);
		}

		@Override
		public GetCredentialRequest[] newArray(int size) {
			return new GetCredentialRequest[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedObject(mCallingAppInfo, flags);
		dest.writeTypedList(mCredentialOptions, flags);
	}

	public CallingAppInfo getCallingAppInfo() {
		return mCallingAppInfo;
	}

	public List<CredentialOption> getCredentialOptions() {
		return mCredentialOptions;
	}
}
