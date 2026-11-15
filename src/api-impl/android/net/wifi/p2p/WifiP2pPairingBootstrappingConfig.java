package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/p2p/WifiP2pPairingBootstrappingConfig.java */
public final class WifiP2pPairingBootstrappingConfig implements Parcelable {
	public static final int PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC = 1 << 0;
	public static final int PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE = 1 << 1;
	public static final int PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE = 1 << 2;
	public static final int PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE = 1 << 3;
	public static final int PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE = 1 << 4;
	public static final int PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND = 1 << 5;

	private int mPairingBootstrappingMethod;
	private String mPassword;

	private static boolean isValidPairingBootstrappingMethod(int method) {
		switch (method) {
			case PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC:
			case PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE:
			case PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE:
			case PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE:
			case PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE:
			case PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND:
				return true;
			default:
				return false;
		}
	}

	public int getPairingBootstrappingMethod() {
		return mPairingBootstrappingMethod;
	}

	public String getPairingBootstrappingPassword() {
		return mPassword;
	}

	public WifiP2pPairingBootstrappingConfig(int method, String password) {
		if (!isValidPairingBootstrappingMethod(method)) {
			throw new IllegalArgumentException("Invalid PairingBootstrappingMethod =" + method);
		}
		mPairingBootstrappingMethod = method;
		if (!TextUtils.isEmpty(password) && (method == PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC || method == PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE || method == PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE)) {
			throw new IllegalArgumentException("Password is not required for =" + method);
		}
		mPassword = password;
	}

	public String toString() {
		StringBuilder sbuf = new StringBuilder("WifiP2pPairingBootstrappingConfig:");
		sbuf.append("\n BootstrappingMethod: ").append(mPairingBootstrappingMethod);
		return sbuf.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mPairingBootstrappingMethod);
		dest.writeString(mPassword);
	}

	public static final Creator<WifiP2pPairingBootstrappingConfig> CREATOR = new Creator<WifiP2pPairingBootstrappingConfig>() {
		public WifiP2pPairingBootstrappingConfig createFromParcel(Parcel in) {
			int pairingBootstrappingMethod = in.readInt();
			String password = in.readString();
			return new WifiP2pPairingBootstrappingConfig(pairingBootstrappingMethod, password);
		}

		public WifiP2pPairingBootstrappingConfig[] newArray(int size) {
			return new WifiP2pPairingBootstrappingConfig[size];
		}
	};
}
