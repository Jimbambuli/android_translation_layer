package android.bluetooth;

import android.annotation.IntDef;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/* AOSP BluetoothDevice (packages/modules/Bluetooth framework, android-16.0.0_r4). The queries go to ATL's
 * AdapterService (BlueZ's Device1 of this address); without an adapter that is on they take AOSP's
 * "BT not enabled" branches. */
public final class BluetoothDevice implements Parcelable, Attributable {
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
	public static final int BOND_LOSS_REASON_BREDR_AUTH_FAILURE = 1;
	public static final int BOND_LOSS_REASON_BREDR_INCOMING_PAIRING = 2;
	public static final int BOND_LOSS_REASON_LE_ENCRYPT_FAILURE = 3;
	public static final int BOND_LOSS_REASON_LE_INCOMING_PAIRING = 4;
	public static final int BOND_LOSS_REASON_UNKNOWN = 0;
	public static final int ENCRYPTION_ALGORITHM_UNKNOWN = 3;
	public static final String EXTRA_BOND_LOSS_REASON = "android.bluetooth.device.extra.BOND_LOSS_REASON";
	public static final String EXTRA_DISCOVERY_RESULT_TYPE = "android.bluetooth.device.extra.DISCOVERY_RESULT_TYPE";
	public static final String EXTRA_UUID_LE = "android.bluetooth.device.extra.UUID_LE";
	/** @hide */
	public static final String EXTRA_UNBOND_REASON = "android.bluetooth.device.extra.REASON";
	/** @hide */
	public static final String EXTRA_REASON = EXTRA_UNBOND_REASON;
	/** @hide */
	public static final int UNBOND_REASON_AUTH_FAILED = 1;
	/** @hide */
	public static final int UNBOND_REASON_AUTH_REJECTED = 2;
	/** @hide */
	public static final int UNBOND_REASON_AUTH_CANCELED = 3;
	/** @hide */
	public static final int UNBOND_REASON_REMOTE_DEVICE_DOWN = 4;
	/** @hide */
	public static final int UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5;
	/** @hide */
	public static final int UNBOND_REASON_AUTH_TIMEOUT = 6;
	/** @hide */
	public static final int UNBOND_REASON_REPEATED_ATTEMPTS = 7;

	/** @hide */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"ENCRYPTION_ALGORITHM_"}, value = {ENCRYPTION_ALGORITHM_NONE, ENCRYPTION_ALGORITHM_E0, ENCRYPTION_ALGORITHM_AES, ENCRYPTION_ALGORITHM_UNKNOWN})
	public @interface EncryptionAlgorithm {}

	/** @hide */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = {"ADDRESS_TYPE_"}, value = {ADDRESS_TYPE_PUBLIC, ADDRESS_TYPE_RANDOM, ADDRESS_TYPE_ANONYMOUS, ADDRESS_TYPE_UNKNOWN})
	public @interface AddressType {}

	private static final String NULL_MAC_ADDRESS = "00:00:00:00:00:00";

	private final String mAddress;
	private final int mAddressType;
	private AttributionSource mAttributionSource;

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
		if (addressType == ADDRESS_TYPE_ANONYMOUS && !NULL_MAC_ADDRESS.equals(address)) {
			throw new IllegalArgumentException("Invalid address for anonymous address type");
		}
		mAddress = address;
		mAddressType = addressType;
	}

	/*package*/ BluetoothDevice(Parcel in) {
		this(in.readString(), in.readInt());
	}

	/** @hide */
	@Override
	public void setAttributionSource(AttributionSource attributionSource) {
		mAttributionSource = attributionSource;
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

	/** @hide AOSP isBluetoothEnabled() */
	static boolean isBluetoothEnabled() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return adapter != null && adapter.isEnabled();
	}

	private static AdapterService service() {
		return AdapterService.get();
	}

	public BluetoothAddress getIdentityAddressWithType() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get identity address with type");
			return new BluetoothAddress(null, ADDRESS_TYPE_UNKNOWN);
		}
		/* AdapterServiceBinder: BLUETOOTH_CONNECT, then BLUETOOTH_PRIVILEGED is enforced */
		AdapterService.checkConnectPermission(TAG, "getIdentityAddressWithType");
		if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
			throw new SecurityException("Need BLUETOOTH_PRIVILEGED permission");
		/* BlueZ resolves private addresses itself: the device's address is its identity address */
		return new BluetoothAddress(mAddress, service().getRemoteAddressType(mAddress));
	}

	public String getName() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get Remote Device name");
			return null;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getRemoteName"))
			return null;
		String name = service().getRemoteName(mAddress);
		if (name != null) {
			return name.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
		}
		return null;
	}

	public int getType() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get Remote Device type");
			return DEVICE_TYPE_UNKNOWN;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getRemoteType"))
			return DEVICE_TYPE_UNKNOWN;
		return service().getRemoteType(mAddress);
	}

	public String getAlias() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get Remote Device Alias");
			return null;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getRemoteAlias"))
			return null;
		String alias = service().getRemoteAlias(mAddress);
		if (alias == null) {
			return getName();
		}
		return alias.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
	}

	/* AdapterServiceBinder.setRemoteAlias(): only for a device the app is associated with (CompanionDeviceManager),
	 * which ATL has none of */
	public int setAlias(String alias) {
		if (alias != null && alias.isEmpty()) {
			throw new IllegalArgumentException("alias cannot be the empty string");
		}
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot set Remote Device name");
			return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
		}
		if (!AdapterService.checkConnectPermission(TAG, "setRemoteAlias"))
			return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION;
		if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
			throw new SecurityException("The application with package name " + AdapterService.context().getPackageName() + " does not have a CDM association with the Bluetooth Device");
		if (service().deviceProperties(mAddress) == null)
			return BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED;
		service().setRemoteAlias(mAddress, alias);
		return BluetoothStatusCodes.SUCCESS;
	}

	public int getBondState() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get bond state");
			return BOND_NONE;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getBondState"))
			return BOND_NONE;
		return service().getBondState(mAddress);
	}

	public boolean createBond() {
		return createBond(TRANSPORT_AUTO);
	}

	/** @hide */
	public boolean createBond(int transport) {
		if (!isBluetoothEnabled()) {
			Log.w(TAG, "BT not enabled, createBond failed");
			return false;
		}
		if (NULL_MAC_ADDRESS.equals(mAddress)) {
			Log.e(TAG, "Unable to create bond, invalid address " + mAddress);
			return false;
		}
		return service().createBond(mAddress);
	}

	/** @hide */
	public boolean cancelBondProcess() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot cancel Remote Device bond");
			return false;
		}
		return service().cancelBond(mAddress);
	}

	/** @hide */
	public boolean removeBond() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot remove Remote Device bond");
			return false;
		}
		return service().removeBond(mAddress);
	}

	/** @hide */
	public boolean isBondingInitiatedLocally() {
		return getBondState() == BOND_BONDING;
	}

	/* BlueZ reports one Connected state for the device (ACL on either transport) */
	public boolean isConnected() {
		if (!isBluetoothEnabled()) {
			return false;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getConnectionState"))
			return false;
		return service().isConnected(mAddress);
	}

	public boolean isConnected(int transport) {
		if (transport != TRANSPORT_BREDR && transport != TRANSPORT_LE && transport != TRANSPORT_AUTO) {
			throw new IllegalArgumentException("Transport(" + transport + ") is not supported");
		}
		if (!isConnected())
			return false;
		if (transport == TRANSPORT_AUTO)
			return true;
		int type = service().getRemoteType(mAddress);
		return transport == TRANSPORT_LE ? type != DEVICE_TYPE_CLASSIC : type != DEVICE_TYPE_LE;
	}

	/** @hide */
	public boolean isEncrypted() {
		return isConnected() && getBondState() == BOND_BONDED;
	}

	/* BlueZ gives no link key size or algorithm over D-Bus */
	public EncryptionStatus getEncryptionStatus(int transport) {
		if (transport != TRANSPORT_BREDR && transport != TRANSPORT_LE) {
			throw new IllegalArgumentException("Transport(" + transport + ") is not supported");
		}
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled. Cannot get link status.");
		}
		return null;
	}

	/* BlueZ does not count missing keys */
	public int getKeyMissingCount() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled. Cannot get key missing counter.");
			return -1;
		}
		AdapterService.checkConnectPermission(TAG, "getKeyMissingCount");
		if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
			throw new SecurityException("Need BLUETOOTH_PRIVILEGED permission");
		return -1;
	}

	public ParcelUuid[] getUuids() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get remote device Uuids");
			return null;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getRemoteUuids"))
			return null;
		return service().getRemoteUuids(mAddress);
	}

	public boolean fetchUuidsWithSdp() {
		return fetchUuidsWithSdp(TRANSPORT_AUTO);
	}

	/** @hide */
	public boolean fetchUuidsWithSdp(int transport) {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot fetchUuidsWithSdp");
			return false;
		}
		return service().fetchUuidsWithSdp(mAddress);
	}

	public BluetoothClass getBluetoothClass() {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot get Bluetooth Class");
			return null;
		}
		if (!AdapterService.checkConnectPermission(TAG, "getRemoteClass"))
			return null;
		int classInt = service().getRemoteClass(mAddress);
		if (classInt == BluetoothClass.ERROR) {
			return null;
		}
		return new BluetoothClass(classInt);
	}

	/* PIN and confirmation answer a pairing request; with BlueZ the desktop's agent asks the user */
	public boolean setPin(byte[] pin) {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot set Remote Device pin");
			return false;
		}
		AdapterService.checkConnectPermission(TAG, "setPin");
		return false;
	}

	public boolean setPairingConfirmation(boolean confirm) {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "BT not enabled. Cannot set pairing confirmation");
			return false;
		}
		AdapterService.checkConnectPermission(TAG, "setPairingConfirmation");
		if (!AdapterService.hasPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED))
			throw new SecurityException("Need BLUETOOTH_PRIVILEGED permission");
		return false;
	}

	/* ---- sockets ---- */

	public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_RFCOMM, true, true, -1, new ParcelUuid(uuid));
	}

	public BluetoothSocket createInsecureRfcommSocketToServiceRecord(UUID uuid) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_RFCOMM, false, false, -1, new ParcelUuid(uuid));
	}

	/** @hide */
	public BluetoothSocket createRfcommSocket(int channel) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_RFCOMM, true, true, channel, null);
	}

	/** @hide */
	public BluetoothSocket createInsecureRfcommSocket(int port) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_RFCOMM, false, false, port, null);
	}

	public BluetoothSocket createL2capChannel(int psm) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "createL2capChannel: Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_L2CAP_LE, true, true, psm, null);
	}

	public BluetoothSocket createInsecureL2capChannel(int psm) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "createInsecureL2capChannel: Bluetooth is not enabled");
			throw new IOException();
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_L2CAP_LE, false, false, psm, null);
	}

	public BluetoothSocket createUsingSocketSettings(BluetoothSocketSettings settings) throws IOException {
		if (!isBluetoothEnabled()) {
			Log.e(TAG, "createUsingSocketSettings: Bluetooth is not enabled");
			throw new IOException();
		}
		if (settings.getSocketType() == BluetoothSocket.TYPE_RFCOMM) {
			return new BluetoothSocket(this, BluetoothSocket.TYPE_RFCOMM, settings.isAuthenticationRequired(), settings.isEncryptionRequired(), -1,
			                           new ParcelUuid(settings.getRfcommUuid()));
		}
		return new BluetoothSocket(this, BluetoothSocket.TYPE_L2CAP_LE, settings.isAuthenticationRequired(), settings.isEncryptionRequired(),
		                           settings.getL2capPsm(), null);
	}

	/* ---- GATT (AOSP code) ---- */

	public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback) {
		return (connectGatt(context, autoConnect, callback, TRANSPORT_AUTO));
	}

	public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport) {
		return (connectGatt(context, autoConnect, callback, transport, PHY_LE_1M_MASK));
	}

	public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy) {
		return connectGatt(context, autoConnect, callback, transport, phy, null);
	}

	public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy,
	                                 Handler handler) {
		return connectGatt(context, autoConnect, callback, transport, false, phy, handler);
	}

	/** @hide */
	public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport,
	                                 boolean opportunistic, int phy, Handler handler) {
		if (callback == null) {
			throw new NullPointerException("callback is null");
		}
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		IBluetoothGatt iGatt = adapter == null ? null : adapter.getBluetoothGatt();
		if (iGatt == null) {
			// BLE is not supported
			return null;
		} else if (NULL_MAC_ADDRESS.equals(mAddress)) {
			Log.e(TAG, "Unable to connect gatt, invalid address " + mAddress);
			return null;
		}
		BluetoothGatt gatt = new BluetoothGatt(iGatt, this, transport, opportunistic, phy, mAttributionSource);
		gatt.connect(autoConnect, callback, handler);
		return gatt;
	}

	/* ---- BluetoothAddress (AOSP) ---- */

	public static final class BluetoothAddress implements Parcelable {
		private final String mAddress;
		private final int mAddressType;

		public BluetoothAddress(String address, int addressType) {
			mAddress = address;
			mAddressType = addressType;
		}

		public String getAddress() {
			return mAddress;
		}

		public int getAddressType() {
			return mAddressType;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			BluetoothUtils.writeStringToParcel(out, mAddress);
			out.writeInt(mAddressType);
		}

		private BluetoothAddress(Parcel in) {
			this(in.readString(), in.readInt());
		}

		public static final Creator<BluetoothAddress> CREATOR = new Creator<BluetoothAddress>() {
			public BluetoothAddress createFromParcel(Parcel in) {
				return new BluetoothAddress(in);
			}

			public BluetoothAddress[] newArray(int size) {
				return new BluetoothAddress[size];
			}
		};
	}
}
