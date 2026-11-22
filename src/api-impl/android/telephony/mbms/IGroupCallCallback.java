package android.telephony.mbms;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IGroupCallCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IGroupCallCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.IGroupCallCallback {
        private static final String DESCRIPTOR = "android.telephony.mbms.IGroupCallCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.IGroupCallCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.IGroupCallCallback) return (android.telephony.mbms.IGroupCallCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onError(int errorCode, String message) throws RemoteException;
    public void onGroupCallStateChanged(int state, int reason) throws RemoteException;
    public void onBroadcastSignalStrengthUpdated(int signalStrength) throws RemoteException;
}
