package android.net.wifi.rtt;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP SecureRangingConfig (modules_Wifi/framework/java/android/net/wifi/rtt/SecureRangingConfig.java) */
public final class SecureRangingConfig implements Parcelable {
	private final boolean mEnableSecureHeLtf;
	private final boolean mEnableRangingFrameProtection;
	private final PasnConfig mPasnConfig;

	private SecureRangingConfig(@NonNull Parcel in) {
		mEnableSecureHeLtf = in.readByte() != 0;
		mEnableRangingFrameProtection = in.readByte() != 0;
		mPasnConfig = (PasnConfig) in.readParcelable(PasnConfig.class.getClassLoader());
	}

	public static final @NonNull Creator<SecureRangingConfig> CREATOR =
			new Creator<SecureRangingConfig>() {
				@Override
				public SecureRangingConfig createFromParcel(Parcel in) {
					return new SecureRangingConfig(in);
				}

				@Override
				public SecureRangingConfig[] newArray(int size) {
					return new SecureRangingConfig[size];
				}
			};

	private SecureRangingConfig(Builder builder) {
		mEnableSecureHeLtf = builder.mEnableSecureHeLtf;
		mEnableRangingFrameProtection = builder.mEnableRangingFrameProtection;
		mPasnConfig = builder.mPasnConfig;
	}

	public boolean isSecureHeLtfEnabled() {
		return mEnableSecureHeLtf;
	}

	public boolean isRangingFrameProtectionEnabled() {
		return mEnableRangingFrameProtection;
	}

	@NonNull
	public PasnConfig getPasnConfig() {
		return mPasnConfig;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeByte((byte) (mEnableSecureHeLtf ? 1 : 0));
		dest.writeByte((byte) (mEnableRangingFrameProtection ? 1 : 0));
		dest.writeParcelable(mPasnConfig, flags);
	}

	@Override
	public String toString() {
		return "SecureRangingConfig{" + "mEnableSecureHeLtf=" + mEnableSecureHeLtf
				+ ", mEnableRangingProtection=" + mEnableRangingFrameProtection + ", mPasnConfig="
				+ mPasnConfig + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SecureRangingConfig)) return false;
		SecureRangingConfig that = (SecureRangingConfig) o;
		return mEnableSecureHeLtf == that.mEnableSecureHeLtf
				&& mEnableRangingFrameProtection == that.mEnableRangingFrameProtection
				&& Objects.equals(mPasnConfig, that.mPasnConfig);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mEnableSecureHeLtf, mEnableRangingFrameProtection, mPasnConfig);
	}

	public static final class Builder {
		private boolean mEnableSecureHeLtf = true;
		private boolean mEnableRangingFrameProtection = true;
		private final PasnConfig mPasnConfig;

		public Builder(@NonNull PasnConfig pasnConfig) {
			Objects.requireNonNull(pasnConfig, "pasnConfig must not be null");
			mPasnConfig = pasnConfig;
		}

		@NonNull
		public Builder setSecureHeLtfEnabled(boolean enableSecureHeLtf) {
			this.mEnableSecureHeLtf = enableSecureHeLtf;
			return this;
		}

		@NonNull
		public Builder setRangingFrameProtectionEnabled(boolean enableRangingFrameProtection) {
			this.mEnableRangingFrameProtection = enableRangingFrameProtection;
			return this;
		}

		@NonNull
		public SecureRangingConfig build() {
			return new SecureRangingConfig(this);
		}
	}
}
