package android.telephony.mbms;

import android.telephony.mbms.FileServiceInfo;
import java.util.List;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IMbmsDownloadSessionCallback.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IMbmsDownloadSessionCallback extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.IMbmsDownloadSessionCallback {
        private static final String DESCRIPTOR = "android.telephony.mbms.IMbmsDownloadSessionCallback";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.IMbmsDownloadSessionCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.IMbmsDownloadSessionCallback) return (android.telephony.mbms.IMbmsDownloadSessionCallback) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onError(int errorCode, String message) throws RemoteException;
    public void onFileServicesUpdated(java.util.List<FileServiceInfo> services) throws RemoteException;
    public void onMiddlewareReady() throws RemoteException;
}
