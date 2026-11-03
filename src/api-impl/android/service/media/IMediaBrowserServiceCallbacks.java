package android.service.media;

import android.content.pm.ParceledListSlice;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/* AOSP IMediaBrowserServiceCallbacks.aidl as the Java interface aidl generates, without the Proxy and
 * onTransact(): the browser runs in the app's process, so Stub.asInterface() returns the implementation itself */
public interface IMediaBrowserServiceCallbacks extends android.os.IInterface {
	public static abstract class Stub extends android.os.Binder implements IMediaBrowserServiceCallbacks {
		private static final String DESCRIPTOR = "android.service.media.IMediaBrowserServiceCallbacks";

		public Stub() {
			attachInterface(this, DESCRIPTOR);
		}

		public static IMediaBrowserServiceCallbacks asInterface(IBinder obj) {
			if (obj == null)
				return null;
			return (IMediaBrowserServiceCallbacks)obj.queryLocalInterface(DESCRIPTOR);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}
	}

	void onConnect(String root, MediaSession.Token session, Bundle extras) throws RemoteException;
	void onConnectFailed() throws RemoteException;
	void onLoadChildren(String mediaId, ParceledListSlice list, Bundle options) throws RemoteException;
}
