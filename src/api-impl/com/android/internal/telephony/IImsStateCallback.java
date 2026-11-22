package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IImsStateCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IImsStateCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements com.android.internal.telephony.IImsStateCallback {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IImsStateCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static com.android.internal.telephony.IImsStateCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof com.android.internal.telephony.IImsStateCallback) return (com.android.internal.telephony.IImsStateCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onUnavailable(int reason) throws RemoteException;
    public void onAvailable() throws RemoteException;
}
