package android.telephony.mbms.vendor;

import android.net.Uri;
import android.telephony.mbms.IMbmsStreamingSessionCallback;
import android.telephony.mbms.IStreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IMbmsStreamingService.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IMbmsStreamingService extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements android.telephony.mbms.vendor.IMbmsStreamingService {
        private static final String DESCRIPTOR = "android.telephony.mbms.vendor.IMbmsStreamingService";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static android.telephony.mbms.vendor.IMbmsStreamingService asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof android.telephony.mbms.vendor.IMbmsStreamingService) return (android.telephony.mbms.vendor.IMbmsStreamingService) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public int initialize(IMbmsStreamingSessionCallback callback, int subId) throws RemoteException;
    public int requestUpdateStreamingServices(int subId, java.util.List<String> serviceClasses) throws RemoteException;
    public int startStreaming(int subId, String serviceId, IStreamingServiceCallback callback) throws RemoteException;
    public Uri getPlaybackUri(int subId, String serviceId) throws RemoteException;
    public void stopStreaming(int subId, String serviceId) throws RemoteException;
    public void dispose(int subId) throws RemoteException;
}
