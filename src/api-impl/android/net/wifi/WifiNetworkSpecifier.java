package android.net.wifi;

import android.net.NetworkSpecifier;
import android.os.PatternMatcher;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/* AOSP packages/modules/Wifi/framework/java/android/net/wifi/WifiNetworkSpecifier.java: Builder with SSID and WPA2
 * passphrase only, with the validations of the AOSP setters and build() for that subset */
public final class WifiNetworkSpecifier extends NetworkSpecifier {
	public final PatternMatcher ssidPatternMatcher;
	public final String wpa2PskPassphrase;

	private WifiNetworkSpecifier(PatternMatcher ssidPatternMatcher, String wpa2PskPassphrase) {
		this.ssidPatternMatcher = ssidPatternMatcher;
		this.wpa2PskPassphrase = wpa2PskPassphrase;
	}

	public static final class Builder {
		private PatternMatcher mSsidPatternMatcher;
		private String mWpa2PskPassphrase;

		public Builder() {}

		public Builder setSsid(String ssid) {
			Objects.requireNonNull(ssid);
			if (!StandardCharsets.UTF_8.newEncoder().canEncode(ssid)) {
				throw new IllegalArgumentException("SSID is not a valid unicode string");
			}
			mSsidPatternMatcher = new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL);
			return this;
		}

		public Builder setWpa2Passphrase(String passphrase) {
			Objects.requireNonNull(passphrase);
			if (!StandardCharsets.US_ASCII.newEncoder().canEncode(passphrase)) {
				throw new IllegalArgumentException("passphrase not ASCII encodable");
			}
			mWpa2PskPassphrase = passphrase;
			return this;
		}

		/* AOSP: without any pattern or band set build() throws */
		public WifiNetworkSpecifier build() {
			if (mSsidPatternMatcher == null) {
				throw new IllegalStateException("one of setSsidPattern/setSsid/setBssidPattern/setBssid/setBand should be invoked for specifier");
			}
			return new WifiNetworkSpecifier(mSsidPatternMatcher, mWpa2PskPassphrase);
		}
	}
}
