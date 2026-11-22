package android.telephony.mbms.vendor;

import android.app.PendingIntent;
import android.net.Uri;
import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.FileInfo;
import android.telephony.mbms.IDownloadProgressListener;
import android.telephony.mbms.IDownloadStatusListener;
import android.telephony.mbms.IMbmsDownloadSessionCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IMbmsDownloadService.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IMbmsDownloadService extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.vendor.IMbmsDownloadService {
        private static final String DESCRIPTOR = "android.telephony.mbms.vendor.IMbmsDownloadService";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.vendor.IMbmsDownloadService asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.vendor.IMbmsDownloadService) return (android.telephony.mbms.vendor.IMbmsDownloadService) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public int initialize(int subId, IMbmsDownloadSessionCallback listener) throws RemoteException;
    public int requestUpdateFileServices(int subId, java.util.List<String> serviceClasses) throws RemoteException;
    public int setTempFileRootDirectory(int subId, String rootDirectoryPath) throws RemoteException;
    public int addServiceAnnouncement(int subId, byte[] contents) throws RemoteException;
    public int download(DownloadRequest downloadRequest) throws RemoteException;
    public int addStatusListener(DownloadRequest downloadRequest, IDownloadStatusListener listener) throws RemoteException;
    public int removeStatusListener(DownloadRequest downloadRequest, IDownloadStatusListener listener) throws RemoteException;
    public int addProgressListener(DownloadRequest downloadRequest, IDownloadProgressListener listener) throws RemoteException;
    public int removeProgressListener(DownloadRequest downloadRequest, IDownloadProgressListener listener) throws RemoteException;
    public java.util.List<DownloadRequest> listPendingDownloads(int subscriptionId) throws RemoteException;
    public int cancelDownload(DownloadRequest downloadRequest) throws RemoteException;
    public int requestDownloadState(DownloadRequest downloadRequest, FileInfo fileInfo) throws RemoteException;
    public int resetDownloadKnowledge(DownloadRequest downloadRequest) throws RemoteException;
    public void dispose(int subId) throws RemoteException;
}
