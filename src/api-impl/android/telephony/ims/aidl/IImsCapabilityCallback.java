package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IImsCapabilityCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IImsCapabilityCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.ims.aidl.IImsCapabilityCallback {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsCapabilityCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.ims.aidl.IImsCapabilityCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.ims.aidl.IImsCapabilityCallback) return (android.telephony.ims.aidl.IImsCapabilityCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onQueryCapabilityConfiguration(int capability, int radioTech, boolean enabled) throws RemoteException;
    public void onChangeCapabilityConfigurationError(int capability, int radioTech, int reason) throws RemoteException;
    public void onCapabilitiesStatusChanged(int config) throws RemoteException;
}
