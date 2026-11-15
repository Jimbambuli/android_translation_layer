package android.net;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * AOSP modules_Connectivity framework/src/android/net/CaptivePortal.java: a handle on the captive portal
 * check of the connectivity service. ATL has no captive portal sign-in, so the responses
 * (ICaptivePortal.appResponse() of the system service) have nowhere to go; AOSP ignores RemoteException the
 * same way.
 */
public class CaptivePortal implements Parcelable {
	private final IBinder mBinder;

	public CaptivePortal(IBinder binder) {
		mBinder = binder;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeStrongBinder(mBinder);
	}

	public static final Parcelable.Creator<CaptivePortal> CREATOR = new Parcelable.Creator<CaptivePortal>() {
		public CaptivePortal createFromParcel(Parcel in) {
			return new CaptivePortal(in.readStrongBinder());
		}

		public CaptivePortal[] newArray(int size) {
			return new CaptivePortal[size];
		}
	};

	/* AOSP: appResponse(APP_RETURN_DISMISSED) to the service */
	public void reportCaptivePortalDismissed() {}

	/* AOSP: appResponse(APP_RETURN_UNWANTED) to the service */
	public void ignoreNetwork() {}
}
