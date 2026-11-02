package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public class IntentSender implements Parcelable {
	public class SendIntentException extends Exception {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {}
}
