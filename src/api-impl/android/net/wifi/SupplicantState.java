package android.net.wifi;

/* AOSP android.net.wifi.SupplicantState: the states of wpa_supplicant (the Parcelable part is left out) */
public enum SupplicantState {
	DISCONNECTED,
	INTERFACE_DISABLED,
	INACTIVE,
	SCANNING,
	AUTHENTICATING,
	ASSOCIATING,
	ASSOCIATED,
	FOUR_WAY_HANDSHAKE,
	GROUP_HANDSHAKE,
	COMPLETED,
	DORMANT,
	UNINITIALIZED,
	INVALID;

	public static boolean isValidState(SupplicantState state) {
		return state != UNINITIALIZED && state != INVALID;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
