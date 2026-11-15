package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/* AOSP BluetoothDevice (packages/modules/Bluetooth framework/java/android/bluetooth/BluetoothDevice.java,
 * android16-release): the public constants, the address and the queries that answer without the Bluetooth service.
 * ATL has no Bluetooth stack, so every query takes AOSP's branch for service == null / Bluetooth not enabled. */
public final class BluetoothDevice implements Parcelable {
	private static final String TAG = "BluetoothDevice";

	public static final String ACTION_FOUND = "android.bluetooth.device.action.FOUND";
	public static final String ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED";
	public static final String ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";
	public static final String ACTION_ACL_DISCONNECT_REQUESTED = "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED";
	public static final String ACTION_ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED";
	public static final String ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED";
	public static final String ACTION_ALIAS_CHANGED = "android.bluetooth.device.action.ALIAS_CHANGED";
	public static final String ACTION_BOND_STATE_CHANGED = "android.bluetooth.device.action.BOND_STATE_CHANGED";
	public static final String ACTION_KEY_MISSING = "android.bluetooth.device.action.KEY_MISSING";
	public static final String ACTION_ENCRYPTION_CHANGE = "android.bluetooth.device.action.ENCRYPTION_CHANGE";
	public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
	public static final String EXTRA_NAME = "android.bluetooth.device.extra.NAME";
	public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";
	public static final String EXTRA_IS_COORDINATED_SET_MEMBER = "android.bluetooth.extra.IS_COORDINATED_SET_MEMBER";
	public static final String EXTRA_CLASS = "android.bluetooth.device.extra.CLASS";
	public static final String EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE";
	public static final String EXTRA_PREVIOUS_BOND_STATE = "android.bluetooth.device.extra.PREVIOUS_BOND_STATE";
	public static final int BOND_NONE = 10;
	public static final int BOND_BONDING = 11;
	public static final int BOND_BONDED = 12;
	public static final String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
	public static final String EXTRA_PAIRING_KEY = "android.bluetooth.device.extra.PAIRING_KEY";
	public static final String EXTRA_KEY_SIZE = "android.bluetooth.device.extra.KEY_SIZE";
	public static final String EXTRA_ENCRYPTION_ALGORITHM = "android.bluetooth.device.extra.EXTRA_ENCRYPTION_ALGORITHM";
	public static final int ENCRYPTION_ALGORITHM_NONE = 0;
	public static final int ENCRYPTION_ALGORITHM_E0 = 1;
	public static final int ENCRYPTION_ALGORITHM_AES = 2;
	public static final String EXTRA_ENCRYPTION_STATUS = "android.bluetooth.device.extra.ENCRYPTION_STATUS";
	public static final String EXTRA_ENCRYPTION_ENABLED = "android.bluetooth.device.extra.ENCRYPTION_ENABLED";
	public static final int DEVICE_TYPE_UNKNOWN = 0;
	public static final int DEVICE_TYPE_CLASSIC = 1;
	public static final int DEVICE_TYPE_LE = 2;
	public static final int DEVICE_TYPE_DUAL = 3;
	public static final String ACTION_UUID = "android.bluetooth.device.action.UUID";
	public static final String ACTION_NAME_FAILED = "android.bluetooth.device.action.NAME_FAILED";
	public static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
	public static final int PAIRING_VARIANT_PIN = 0;
	public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
	public static final String EXTRA_UUID = "android.bluetooth.device.extra.UUID";
	public static final int TRANSPORT_AUTO = 0;
	public static final int TRANSPORT_BREDR = 1;
	public static final int TRANSPORT_LE = 2;
	public static final int PHY_LE_1M = 1;
	public static final int PHY_LE_2M = 2;
	public static final int PHY_LE_CODED = 3;
	public static final int PHY_LE_1M_MASK = 1;
	public static final int PHY_LE_2M_MASK = 2;
	public static final int PHY_LE_CODED_MASK = 4;
	public static final int PHY_OPTION_NO_PREFERRED = 0;
	public static final int PHY_OPTION_S2 = 1;
	public static final int PHY_OPTION_S8 = 2;
	public static final String EXTRA_TRANSPORT = "android.bluetooth.device.extra.TRANSPORT";
	public static final int ADDRESS_TYPE_PUBLIC = 0;
	public static final int ADDRESS_TYPE_RANDOM = 1;
	public static final int ADDRESS_TYPE_UNKNOWN = 0xFFFF;
	public static final int ADDRESS_TYPE_ANONYMOUS = 0xFF;
	public static final int ERROR = Integer.MIN_VALUE;

	private final String mAddress;
	private final int mAddressType;

	/*package*/ BluetoothDevice(String address) {
		this(address, ADDRESS_TYPE_PUBLIC);
	}

	/*package*/ BluetoothDevice(String address, int addressType) {
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
		}
		if (addressType != ADDRESS_TYPE_PUBLIC && addressType != ADDRESS_TYPE_RANDOM && addressType != ADDRESS_TYPE_ANONYMOUS) {
			throw new IllegalArgumentException(addressType + " is not a Bluetooth address type");
		}
		if (addressType == ADDRESS_TYPE_ANONYMOUS && !"00:00:00:00:00:00".equals(address)) {
			throw new IllegalArgumentException("Invalid address for anonymous address type");
		}
		mAddress = address;
		mAddressType = addressType;
	}

	/*package*/ BluetoothDevice(Parcel in) {
		this(in.readString(), in.readInt());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BluetoothDevice) {
			return mAddress.equals(((BluetoothDevice)o).getAddress());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mAddress.hashCode();
	}

	/* AOSP redacts the address when the compat change CHANGE_TO_STRING_REDACTED is on; ATL has no compat
	 * framework, this is the branch with the change off */
	@Override
	public String toString() {
		return mAddress;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<BluetoothDevice> CREATOR = new Creator<BluetoothDevice>() {
		public BluetoothDevice createFromParcel(Parcel in) {
			return new BluetoothDevice(in);
		}

		public BluetoothDevice[] newArray(int size) {
			return new BluetoothDevice[size];
		}
	};

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mAddress);
		out.writeInt(mAddressType);
	}

	public String getAddress() {
		return mAddress;
	}

	public int getAddressType() {
		return mAddressType;
	}

	public String getName() {
		Log.e(TAG, "BT not enabled. Cannot get Remote Device name");
		return null;
	}

	public String getAlias() {
		Log.e(TAG, "BT not enabled. Cannot get Remote Device Alias");
		return null;
	}

	public int getType() {
		Log.e(TAG, "BT not enabled. Cannot get Remote Device type");
		return DEVICE_TYPE_UNKNOWN;
	}

	public int getBondState() {
		Log.e(TAG, "BT not enabled. Cannot get bond state");
		return BOND_NONE;
	}

	public boolean createBond() {
		Log.w(TAG, "BT not enabled, createBond failed");
		return false;
	}

	/* the adapter is off (BluetoothAdapter.getState() == STATE_OFF), so the AOSP "BT not enabled" branches apply */
	public android.os.ParcelUuid[] getUuids() {
		Log.e(TAG, "BT not enabled. Cannot get remote device Uuids");
		return null;
	}

	public boolean fetchUuidsWithSdp() {
		Log.e(TAG, "BT not enabled. Cannot fetchUuidsWithSdp");
		return false;
	}

	public BluetoothClass getBluetoothClass() {
		Log.e(TAG, "BT not enabled. Cannot get Bluetooth Class");
		return null;
	}

	public boolean setPin(byte[] pin) {
		Log.e(TAG, "BT not enabled. Cannot set Remote Device pin");
		return false;
	}

	public BluetoothSocket createRfcommSocketToServiceRecord(java.util.UUID uuid) throws java.io.IOException {
		Log.e(TAG, "Bluetooth is not enabled");
		throw new java.io.IOException();
	}

	public BluetoothSocket createInsecureRfcommSocketToServiceRecord(java.util.UUID uuid) throws java.io.IOException {
		Log.e(TAG, "Bluetooth is not enabled");
		throw new java.io.IOException();
	}

	/* AOSP BluetoothDevice.createL2capChannel() / createInsecureL2capChannel(): isBluetoothEnabled() is false
	 * without an adapter */
	public BluetoothSocket createL2capChannel(int psm) throws java.io.IOException {
		Log.e(TAG, "createL2capChannel: Bluetooth is not enabled");
		throw new java.io.IOException();
	}

	public BluetoothSocket createInsecureL2capChannel(int psm) throws java.io.IOException {
		Log.e(TAG, "createInsecureL2capChannel: Bluetooth is not enabled");
		throw new java.io.IOException();
	}

	/* AOSP: callback null -> NullPointerException; then the adapter's IBluetoothGatt is null ("BLE is not
	 * supported") and the result is null */
	public BluetoothGatt connectGatt(android.content.Context context, boolean autoConnect, BluetoothGattCallback callback) {
		return connectGatt(context, autoConnect, callback, TRANSPORT_AUTO);
	}

	public BluetoothGatt connectGatt(android.content.Context context, boolean autoConnect, BluetoothGattCallback callback,
	                                 int transport) {
		return connectGatt(context, autoConnect, callback, transport, PHY_LE_1M_MASK);
	}

	public BluetoothGatt connectGatt(android.content.Context context, boolean autoConnect, BluetoothGattCallback callback,
	                                 int transport, int phy) {
		return connectGatt(context, autoConnect, callback, transport, phy, null);
	}

	public BluetoothGatt connectGatt(android.content.Context context, boolean autoConnect, BluetoothGattCallback callback,
	                                 int transport, int phy, android.os.Handler handler) {
		return connectGatt(context, autoConnect, callback, transport, false, phy, handler);
	}

	public BluetoothGatt connectGatt(android.content.Context context, boolean autoConnect, BluetoothGattCallback callback,
	                                 int transport, boolean opportunistic, int phy, android.os.Handler handler) {
		if (callback == null) {
			throw new NullPointerException("callback is null");
		}
		return null;
	}
}
