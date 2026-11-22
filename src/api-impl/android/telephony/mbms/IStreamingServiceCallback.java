package android.telephony.mbms;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IStreamingServiceCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IStreamingServiceCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.IStreamingServiceCallback {
        private static final String DESCRIPTOR = "android.telephony.mbms.IStreamingServiceCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.IStreamingServiceCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.IStreamingServiceCallback) return (android.telephony.mbms.IStreamingServiceCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onError(int errorCode, String message) throws RemoteException;
    public void onStreamStateUpdated(int state, int reason) throws RemoteException;
    public void onMediaDescriptionUpdated() throws RemoteException;
    public void onBroadcastSignalStrengthUpdated(int signalStrength) throws RemoteException;
    public void onStreamMethodUpdated(int methodType) throws RemoteException;
}
