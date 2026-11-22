package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IImsConfigCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IImsConfigCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.ims.aidl.IImsConfigCallback {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsConfigCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.ims.aidl.IImsConfigCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.ims.aidl.IImsConfigCallback) return (android.telephony.ims.aidl.IImsConfigCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onIntConfigChanged(int item, int value) throws RemoteException;
    public void onStringConfigChanged(int item, String value) throws RemoteException;
}
