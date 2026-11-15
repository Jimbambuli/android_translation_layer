package android.net.wifi.rtt;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.wifi.WifiSsid;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;

/* AOSP PasnConfig (modules_Wifi/framework/java/android/net/wifi/rtt/PasnConfig.java): the data and the
 * validation of the Builder; the hidden helpers (setPassword(), *FromCapabilities()) are not part of the API */
public final class PasnConfig implements Parcelable {

	public static final int AKM_NONE = 0;
	public static final int AKM_PASN = 1 << 0;
	public static final int AKM_SAE = 1 << 1;
	public static final int AKM_FT_EAP_SHA256 = 1 << 2;
	public static final int AKM_FT_PSK_SHA256 = 1 << 3;
	public static final int AKM_FT_EAP_SHA384 = 1 << 4;
	public static final int AKM_FT_PSK_SHA384 = 1 << 5;
	public static final int AKM_FILS_EAP_SHA256 = 1 << 6;
	public static final int AKM_FILS_EAP_SHA384 = 1 << 7;

	public static final int CIPHER_NONE = 0;
	public static final int CIPHER_CCMP_128 = 1 << 0;
	public static final int CIPHER_CCMP_256 = 1 << 1;
	public static final int CIPHER_GCMP_128 = 1 << 2;
	public static final int CIPHER_GCMP_256 = 1 << 3;

	private final int mBaseAkms;
	private final int mCiphers;
	private final String mPassword;
	private final WifiSsid mWifiSsid;
	private final byte[] mPasnComebackCookie;

	public int getBaseAkms() {
		return mBaseAkms;
	}

	public int getCiphers() {
		return mCiphers;
	}

	@Nullable
	public String getPassword() {
		return mPassword;
	}

	@Nullable
	public WifiSsid getWifiSsid() {
		return mWifiSsid;
	}

	@Nullable
	public byte[] getPasnComebackCookie() {
		return mPasnComebackCookie;
	}

	private PasnConfig(@NonNull Parcel in) {
		mBaseAkms = in.readInt();
		mCiphers = in.readInt();
		mPassword = in.readString();
		mWifiSsid = (WifiSsid) in.readParcelable(WifiSsid.class.getClassLoader());
		mPasnComebackCookie = in.createByteArray();
	}

	public static final @NonNull Creator<PasnConfig> CREATOR = new Creator<PasnConfig>() {
		@Override
		public PasnConfig createFromParcel(Parcel in) {
			return new PasnConfig(in);
		}

		@Override
		public PasnConfig[] newArray(int size) {
			return new PasnConfig[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(mBaseAkms);
		dest.writeInt(mCiphers);
		dest.writeString(mPassword);
		dest.writeParcelable(mWifiSsid, flags);
		dest.writeByteArray(mPasnComebackCookie);
	}

	private PasnConfig(Builder builder) {
		mBaseAkms = builder.mBaseAkms;
		mCiphers = builder.mCiphers;
		mPassword = builder.mPassword;
		mWifiSsid = builder.mWifiSsid;
		mPasnComebackCookie = builder.mPasnComebackCookie;
	}

	public static final class Builder {
		private final int mBaseAkms;
		private final int mCiphers;
		private String mPassword = null;
		private WifiSsid mWifiSsid = null;
		byte[] mPasnComebackCookie = null;

		public Builder(int baseAkms, int ciphers) {
			mBaseAkms = baseAkms;
			mCiphers = ciphers;
		}

		@NonNull
		public Builder setPassword(@NonNull String password) {
			Objects.requireNonNull(password, "Password must not be null");
			this.mPassword = password;
			return this;
		}

		@NonNull
		public Builder setWifiSsid(@NonNull WifiSsid wifiSsid) {
			Objects.requireNonNull(wifiSsid, "SSID must not be null");
			this.mWifiSsid = wifiSsid;
			return this;
		}

		@NonNull
		public Builder setPasnComebackCookie(@NonNull byte[] pasnComebackCookie) {
			Objects.requireNonNull(pasnComebackCookie, "PASN comeback cookie must not be null");
			if (pasnComebackCookie.length > 255 || pasnComebackCookie.length == 0) {
				throw new IllegalArgumentException("Cookie with invalid length "
						+ pasnComebackCookie.length);
			}
			mPasnComebackCookie = pasnComebackCookie;
			return this;
		}

		@NonNull
		public PasnConfig build() {
			return new PasnConfig(this);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PasnConfig)) return false;
		PasnConfig that = (PasnConfig) o;
		return mBaseAkms == that.mBaseAkms && mCiphers == that.mCiphers
				&& Objects.equals(mPassword, that.mPassword)
				&& Objects.equals(mWifiSsid, that.mWifiSsid)
				&& Arrays.equals(mPasnComebackCookie, that.mPasnComebackCookie);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(mBaseAkms, mCiphers, mPassword, mWifiSsid);
		result = 31 * result + Arrays.hashCode(mPasnComebackCookie);
		return result;
	}

	@Override
	public String toString() {
		String password = (mPassword != null ? "*" : "null");
		return "PasnConfig{" + "mBaseAkms=" + mBaseAkms + ", mCiphers=" + mCiphers + ", mPassword='"
				+ password + '\'' + ", mWifiSsid=" + mWifiSsid + ", mPasnComebackCookie="
				+ Arrays.toString(mPasnComebackCookie) + '}';
	}
}
