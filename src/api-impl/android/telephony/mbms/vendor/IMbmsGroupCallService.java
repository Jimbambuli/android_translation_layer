package android.telephony.mbms.vendor;

import android.net.Uri;
import android.telephony.mbms.IGroupCallCallback;
import android.telephony.mbms.IMbmsGroupCallSessionCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IMbmsGroupCallService.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IMbmsGroupCallService extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.vendor.IMbmsGroupCallService {
        private static final String DESCRIPTOR = "android.telephony.mbms.vendor.IMbmsGroupCallService";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.vendor.IMbmsGroupCallService asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.vendor.IMbmsGroupCallService) return (android.telephony.mbms.vendor.IMbmsGroupCallService) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public int initialize(IMbmsGroupCallSessionCallback callback, int subId) throws RemoteException;
    public void stopGroupCall(int subId, long tmgi) throws RemoteException;
    public void updateGroupCall(int subscriptionId, long tmgi, java.util.List saiList, java.util.List frequencyList) throws RemoteException;
    public int startGroupCall(int subscriptionId, long tmgi, java.util.List saiList, java.util.List frequencyList, IGroupCallCallback callback) throws RemoteException;
    public void dispose(int subId) throws RemoteException;
}
