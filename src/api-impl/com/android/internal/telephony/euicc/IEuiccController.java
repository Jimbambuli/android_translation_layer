package com.android.internal.telephony.euicc;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import java.util.List;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/* ATL: the interface of IEuiccController.aidl (Android 16). The Stub is the in-process side; there is no proxy because no
 * remote process of this interface exists in ATL (asInterface() returns the local implementation or null). */
public interface IEuiccController extends IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends Binder implements com.android.internal.telephony.euicc.IEuiccController {
        private static final String DESCRIPTOR = "com.android.internal.telephony.euicc.IEuiccController";
        public Stub() { this.attachInterface(this, DESCRIPTOR); }
        public static com.android.internal.telephony.euicc.IEuiccController asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof com.android.internal.telephony.euicc.IEuiccController) return (com.android.internal.telephony.euicc.IEuiccController) iin;
            return null;
        }
        @Override public IBinder asBinder() { return this; }
    }
    public void continueOperation(int cardId, Intent resolutionIntent, Bundle resolutionExtras) throws RemoteException;
    public void getDownloadableSubscriptionMetadata(int cardId, DownloadableSubscription subscription, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public void getDefaultDownloadableSubscriptionList(int cardId, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public String getEid(int cardId, String callingPackage) throws RemoteException;
    public int getOtaStatus(int cardId) throws RemoteException;
    public void downloadSubscription(int cardId, DownloadableSubscription subscription, boolean switchAfterDownload, String callingPackage, Bundle resolvedBundle, PendingIntent callbackIntent) throws RemoteException;
    public EuiccInfo getEuiccInfo(int cardId) throws RemoteException;
    public void deleteSubscription(int cardId, int subscriptionId, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public void switchToSubscription(int cardId, int subscriptionId, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public void switchToSubscriptionWithPort(int cardId, int subscriptionId, int portIndex, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public void updateSubscriptionNickname(int cardId, int subscriptionId, String nickname, String callingPackage, PendingIntent callbackIntent) throws RemoteException;
    public void eraseSubscriptions(int cardId, PendingIntent callbackIntent) throws RemoteException;
    public void eraseSubscriptionsWithOptions(int cardId, int options, PendingIntent callbackIntent) throws RemoteException;
    public void retainSubscriptionsForFactoryReset(int cardId, PendingIntent callbackIntent) throws RemoteException;
    public void setSupportedCountries(boolean isSupported, java.util.List<String> countriesList) throws RemoteException;
    public java.util.List<String> getSupportedCountries(boolean isSupported) throws RemoteException;
    public boolean isSupportedCountry(String countryIso) throws RemoteException;
    public boolean isSimPortAvailable(int cardId, int portIndex, String callingPackage) throws RemoteException;
    public boolean hasCarrierPrivilegesForPackageOnAnyPhone(String callingPackage) throws RemoteException;
    public boolean isCompatChangeEnabled(String callingPackage, long changeId) throws RemoteException;
    public void setPsimConversionSupportedCarriers(int[] carrierIds) throws RemoteException;
    public boolean isPsimConversionSupported(int carrierId) throws RemoteException;
    public long getAvailableMemoryInBytes(int cardId, String callingPackage) throws RemoteException;
}
