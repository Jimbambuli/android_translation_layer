package android.telephony.mbms;

import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.FileInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IDownloadStatusListener.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IDownloadStatusListener extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.IDownloadStatusListener {
        private static final String DESCRIPTOR = "android.telephony.mbms.IDownloadStatusListener";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.IDownloadStatusListener asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.IDownloadStatusListener) return (android.telephony.mbms.IDownloadStatusListener) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void onStatusUpdated(DownloadRequest request, FileInfo fileInfo, int status) throws RemoteException;
}
