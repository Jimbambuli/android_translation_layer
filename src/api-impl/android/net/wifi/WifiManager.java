package android.net.wifi;

import android.net.DhcpInfo;

public class WifiManager {

	public static final int WIFI_STATE_DISABLED = 1;
	public static final int WIFI_STATE_DISABLING = 0;
	public static final int WIFI_STATE_ENABLED = 3;
	public static final int WIFI_STATE_ENABLING = 2;
	public static final int WIFI_STATE_UNKNOWN = 4;

	public static final String UNKNOWN_SSID = "<unknown ssid>";

	/* AOSP WifiManager.WifiLock: the reference counting of acquire()/release(); there is no Wi-Fi power saving
	 * on the desktop for the lock to hold off */
	public class WifiLock {
		private int mRefCount;
		private boolean mRefCounted = true;
		private boolean mHeld;

		public void setReferenceCounted(boolean refCounted) {
			mRefCounted = refCounted;
		}

		public void acquire() {
			synchronized (this) {
				if (mRefCounted ? (++mRefCount == 1) : (!mHeld))
					mHeld = true;
			}
		}

		public void release() {
			synchronized (this) {
				if (mRefCounted ? (--mRefCount == 0) : (mHeld))
					mHeld = false;
				if (mRefCount < 0)
					throw new RuntimeException("WifiLock under-locked");
			}
		}

		public boolean isHeld() {
			synchronized (this) {
				return mHeld;
			}
		}
	}

	/* AOSP WifiManager.MulticastLock: the same reference counting as WifiLock; ATL does not filter multicast */
	public class MulticastLock {
		private int mRefCount;
		private boolean mRefCounted = true;
		private boolean mHeld;

		public void setReferenceCounted(boolean refCounted) {
			mRefCounted = refCounted;
		}

		public void acquire() {
			synchronized (this) {
				if (mRefCounted ? (++mRefCount == 1) : (!mHeld))
					mHeld = true;
			}
		}

		public void release() {
			synchronized (this) {
				if (mRefCounted ? (--mRefCount == 0) : (mHeld))
					mHeld = false;
				if (mRefCount < 0)
					throw new RuntimeException("MulticastLock under-locked");
			}
		}

		public boolean isHeld() {
			synchronized (this) {
				return mHeld;
			}
		}
	}

	public MulticastLock createMulticastLock(String tag) {
		return new MulticastLock();
	}

	public WifiLock createWifiLock(int lockType, String tag) {
		return new WifiLock();
	}

	public WifiLock createWifiLock(String tag) {
		return new WifiLock();
	}

	public WifiInfo getConnectionInfo() {
		return new WifiInfo();
	}

	/* like WifiServiceImpl without a DHCP lease: all fields 0 */
	@Deprecated
	public DhcpInfo getDhcpInfo() {
		return new DhcpInfo();
	}

	public int getWifiState() {
		return WIFI_STATE_UNKNOWN;
	}

	public boolean isWifiEnabled() {
		return false;
	}

	/* no Wi-Fi control from inside the sandbox: AOSP returns false when the request is not carried out */
	public boolean setWifiEnabled(boolean enabled) {
		return false;
	}

	public boolean startScan() {
		return false;
	}

	public java.util.List getScanResults() {
		return new java.util.ArrayList();
	}

	public boolean is5GHzBandSupported() {
		return false;
	}

	public boolean is6GHzBandSupported() {
		return false;
	}

	public boolean isScanThrottleEnabled() {
		return false;
	}
}
