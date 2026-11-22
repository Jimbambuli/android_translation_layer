package android.telephony.ims.aidl;

import android.net.Uri;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.SipDetails;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IImsRegistrationCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IImsRegistrationCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.ims.aidl.IImsRegistrationCallback {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsRegistrationCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.ims.aidl.IImsRegistrationCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.ims.aidl.IImsRegistrationCallback) return (android.telephony.ims.aidl.IImsRegistrationCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onRegistered(ImsRegistrationAttributes attr) throws RemoteException;
    public void onRegistering(ImsRegistrationAttributes attr) throws RemoteException;
    public void onDeregistered(ImsReasonInfo info, int suggestedAction, int imsRadioTech) throws RemoteException;
    public void onDeregisteredWithTime(ImsReasonInfo info, int suggestedAction, int imsRadioTech, int throttlingTimeSec) throws RemoteException;
    public void onDeregisteredWithDetails(ImsReasonInfo info, int suggestedAction, int imsRadioTech, SipDetails detail) throws RemoteException;
    public void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) throws RemoteException;
    public void onSubscriberAssociatedUriChanged(Uri[] uris) throws RemoteException;
}
