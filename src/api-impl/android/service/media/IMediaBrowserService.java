package android.service.media;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;

/* AOSP IMediaBrowserService.aidl as the Java interface aidl generates, without the Proxy and onTransact():
 * the service runs in the app's process, so Stub.asInterface() returns the implementation itself */
public interface IMediaBrowserService extends android.os.IInterface {
	public static abstract class Stub extends android.os.Binder implements IMediaBrowserService {
		private static final String DESCRIPTOR = "android.service.media.IMediaBrowserService";

		public Stub() {
			attachInterface(this, DESCRIPTOR);
		}

		public static IMediaBrowserService asInterface(IBinder obj) {
			if (obj == null)
				return null;
			return (IMediaBrowserService)obj.queryLocalInterface(DESCRIPTOR);
		}

		@Override
		public IBinder asBinder() {
			return this;
		}
	}

	void connect(String pkg, Bundle rootHints, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void disconnect(IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void addSubscriptionDeprecated(String uri, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void removeSubscriptionDeprecated(String uri, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void getMediaItem(String uri, ResultReceiver cb, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void addSubscription(String uri, IBinder token, Bundle options, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
	void removeSubscription(String uri, IBinder token, IMediaBrowserServiceCallbacks callbacks) throws RemoteException;
}
