/* AOSP external/cronet android/java/src/android/net/http/QuicOptions.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.SuppressLint;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressLint("UserHandleName")
public class QuicOptions {
	private final Set<String> mQuicHostAllowlist;
	private final Set<String> mEnabledQuicVersions;

	private final Set<String> mConnectionOptions;
	private final Set<String> mClientConnectionOptions;
	@Nullable
	private final Integer mInMemoryServerConfigsCacheSize;
	@Nullable
	private final String mHandshakeUserAgent;
	@Nullable
	private final Boolean mRetryWithoutAltSvcOnQuicErrors;
	@Nullable
	private final Boolean mEnableTlsZeroRtt;

	@Nullable
	private final Duration mPreCryptoHandshakeIdleTimeout;
	@Nullable
	private final Duration mCryptoHandshakeTimeout;

	@Nullable
	private final Duration mIdleConnectionTimeout;
	@Nullable
	private final Duration mRetransmittableOnWireTimeout;

	@Nullable
	private final Boolean mCloseSessionsOnIpChange;
	@Nullable
	private final Boolean mGoawaySessionsOnIpChange;

	@Nullable
	private final Duration mInitialBrokenServicePeriod;
	@Nullable
	private final Boolean mIncreaseBrokenServicePeriodExponentially;
	@Nullable
	private final Boolean mDelayJobsWithAvailableSpdySession;

	private final Set<String> mExtraQuicheFlags;

	QuicOptions(Builder builder) {
		this.mQuicHostAllowlist =
				Collections.unmodifiableSet(new LinkedHashSet<>(builder.mQuicHostAllowlist));
		this.mEnabledQuicVersions =
				Collections.unmodifiableSet(new LinkedHashSet<>(builder.mEnabledQuicVersions));
		this.mConnectionOptions =
				Collections.unmodifiableSet(new LinkedHashSet<>(builder.mConnectionOptions));
		this.mClientConnectionOptions =
				Collections.unmodifiableSet(new LinkedHashSet<>(builder.mClientConnectionOptions));
		this.mInMemoryServerConfigsCacheSize = builder.mInMemoryServerConfigsCacheSize;
		this.mHandshakeUserAgent = builder.mHandshakeUserAgent;
		this.mRetryWithoutAltSvcOnQuicErrors = builder.mRetryWithoutAltSvcOnQuicErrors;
		this.mEnableTlsZeroRtt = builder.mEnableTlsZeroRtt;
		this.mPreCryptoHandshakeIdleTimeout = builder.mPreCryptoHandshakeIdleTimeout;
		this.mCryptoHandshakeTimeout = builder.mCryptoHandshakeTimeout;
		this.mIdleConnectionTimeout = builder.mIdleConnectionTimeout;
		this.mRetransmittableOnWireTimeout = builder.mRetransmittableOnWireTimeout;
		this.mCloseSessionsOnIpChange = builder.mCloseSessionsOnIpChange;
		this.mGoawaySessionsOnIpChange = builder.mGoawaySessionsOnIpChange;
		this.mInitialBrokenServicePeriod = builder.mInitialBrokenServicePeriod;
		this.mIncreaseBrokenServicePeriodExponentially =
				builder.mIncreaseBrokenServicePeriodExponentially;
		this.mDelayJobsWithAvailableSpdySession = builder.mDelayJobsWithAvailableSpdySession;
		this.mExtraQuicheFlags =
				Collections.unmodifiableSet(new LinkedHashSet<>(builder.mExtraQuicheFlags));
	}

	@NonNull
	public Set<String> getAllowedQuicHosts() {
		return mQuicHostAllowlist;
	}

	 public boolean hasInMemoryServerConfigsCacheSize() {
		return mInMemoryServerConfigsCacheSize != null;
	 }

	public int getInMemoryServerConfigsCacheSize() {
		if (!hasInMemoryServerConfigsCacheSize()) {
			throw new IllegalStateException("InMemoryServerConfigsCacheSize is not set");
		}
		return mInMemoryServerConfigsCacheSize;
	}

	@Nullable
	public String getHandshakeUserAgent() {
		return mHandshakeUserAgent;
	}

	@Nullable
	public Duration getIdleConnectionTimeout() {
		return mIdleConnectionTimeout;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final Set<String> mQuicHostAllowlist = new LinkedHashSet<>();
		private final Set<String> mEnabledQuicVersions = new LinkedHashSet<>();
		private final Set<String> mConnectionOptions = new LinkedHashSet<>();
		private final Set<String> mClientConnectionOptions = new LinkedHashSet<>();
		@Nullable
		private Integer mInMemoryServerConfigsCacheSize;
		@Nullable
		private String mHandshakeUserAgent;
		@Nullable
		private Boolean mRetryWithoutAltSvcOnQuicErrors;
		@Nullable
		private Boolean mEnableTlsZeroRtt;
		@Nullable
		private Duration mPreCryptoHandshakeIdleTimeout;
		@Nullable
		private Duration mCryptoHandshakeTimeout;
		@Nullable
		private Duration mIdleConnectionTimeout;
		@Nullable
		private Duration mRetransmittableOnWireTimeout;
		@Nullable
		private Boolean mCloseSessionsOnIpChange;
		@Nullable
		private Boolean mGoawaySessionsOnIpChange;
		@Nullable
		private Duration mInitialBrokenServicePeriod;
		@Nullable
		private Boolean mIncreaseBrokenServicePeriodExponentially;
		@Nullable
		private Boolean mDelayJobsWithAvailableSpdySession;
		private final Set<String> mExtraQuicheFlags = new LinkedHashSet<>();

		public Builder() {}

		@NonNull
		public Builder addAllowedQuicHost(@NonNull String quicHost) {
			mQuicHostAllowlist.add(quicHost);
			return this;
		}

		@NonNull
		public Builder setInMemoryServerConfigsCacheSize(int inMemoryServerConfigsCacheSize) {
			this.mInMemoryServerConfigsCacheSize = inMemoryServerConfigsCacheSize;
			return this;
		}

		@NonNull
		public Builder setHandshakeUserAgent(@NonNull String handshakeUserAgent) {
			this.mHandshakeUserAgent = handshakeUserAgent;
			return this;
		}

		@NonNull
		public Builder setIdleConnectionTimeout(@NonNull Duration idleConnectionTimeout) {
			this.mIdleConnectionTimeout = idleConnectionTimeout;
			return this;
		}

		@NonNull
		public QuicOptions build() {
			return new QuicOptions(this);
		}
	}

	public @interface Experimental {}
}
