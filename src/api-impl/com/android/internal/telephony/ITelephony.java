package com.android.internal.telephony;

import android.os.IBinder;
import android.telephony.ims.aidl.IFeatureProvisioningCallback;
import com.android.internal.telephony.IImsStateCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the IMS subset of ITelephony.aidl (Android 16) The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface ITelephony extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements com.android.internal.telephony.ITelephony {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ITelephony";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static com.android.internal.telephony.ITelephony asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof com.android.internal.telephony.ITelephony) return (com.android.internal.telephony.ITelephony) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void registerFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException;
    public void unregisterFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException;
    public void setImsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException;
    public boolean getImsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException;
    public boolean getRcsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException;
    public void setRcsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException;
    public String getImsProvisioningString(int subId, int key) throws RemoteException;
    public void registerImsStateCallback(int subId, int feature, IImsStateCallback cb, String callingPackage) throws RemoteException;
    public void unregisterImsStateCallback(IImsStateCallback cb) throws RemoteException;
    public boolean isProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException;
    public boolean isRcsProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException;
}
