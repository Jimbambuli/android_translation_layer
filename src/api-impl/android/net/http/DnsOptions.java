/* AOSP external/cronet android/java/src/android/net/http/DnsOptions.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;

@SuppressLint("UserHandleName")
public final class DnsOptions {
	private final @DnsOptionState int mUseHttpStackDnsResolver;
	private final @DnsOptionState int mPersistHostCache;
	private final @DnsOptionState int mEnableStaleDns;
	@Nullable
	private final Duration mPersistHostCachePeriod;

	private final @DnsOptionState int mPreestablishConnectionsToStaleDnsResults;
	@Nullable
	private final StaleDnsOptions mStaleDnsOptions;

	DnsOptions(Builder builder) {
		this.mEnableStaleDns = builder.mEnableStaleDns;
		this.mStaleDnsOptions = builder.mStaleDnsOptions;
		this.mPersistHostCachePeriod = builder.mPersistHostCachePeriod;
		this.mPreestablishConnectionsToStaleDnsResults =
				builder.mPreestablishConnectionsToStaleDnsResults;
		this.mUseHttpStackDnsResolver = builder.mUseHttpStackDnsResolver;
		this.mPersistHostCache = builder.mPersistHostCache;
	}

	public static final int DNS_OPTION_UNSPECIFIED = 0;

	public static final int DNS_OPTION_ENABLED = 1;

	public static final int DNS_OPTION_DISABLED = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef(flag = false, prefix = "DNS_OPTION_", value = {
			DNS_OPTION_UNSPECIFIED,
			DNS_OPTION_ENABLED,
			DNS_OPTION_DISABLED,
	})
	public @interface DnsOptionState {}

	public @DnsOptionState int getUseHttpStackDnsResolver() {
		return mUseHttpStackDnsResolver;
	}

	public @DnsOptionState int getPersistHostCache() {
		return mPersistHostCache;
	}

	public @DnsOptionState int getStaleDns() {
		return mEnableStaleDns;
	}

	@Nullable
	public Duration getPersistHostCachePeriod() {
		return mPersistHostCachePeriod;
	}

	public @DnsOptionState int getPreestablishConnectionsToStaleDnsResults() {
		return mPreestablishConnectionsToStaleDnsResults;
	}

	@Nullable
	public StaleDnsOptions getStaleDnsOptions() {
		return mStaleDnsOptions;
	}

	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	@SuppressLint("UserHandleName")
	public static class StaleDnsOptions {
		@Nullable
		public Duration getFreshLookupTimeout() {
			return mFreshLookupTimeout;
		}

		@Nullable
		public Duration getMaxExpiredDelay() {
			return mMaxExpiredDelay;
		}

		public @DnsOptionState int getAllowCrossNetworkUsage() {
			return mAllowCrossNetworkUsage;
		}
		public @DnsOptionState int getUseStaleOnNameNotResolved() {
			return mUseStaleOnNameNotResolved;
		}

		@NonNull
		public static Builder builder() {
			return new Builder();
		}

		@Nullable
		private final Duration mFreshLookupTimeout;
		@Nullable
		private final Duration mMaxExpiredDelay;
		private final @DnsOptionState int mAllowCrossNetworkUsage;
		private final @DnsOptionState int mUseStaleOnNameNotResolved;

		StaleDnsOptions(@NonNull Builder builder) {
			this.mFreshLookupTimeout = builder.mFreshLookupTimeout;
			this.mMaxExpiredDelay = builder.mMaxExpiredDelay;
			this.mAllowCrossNetworkUsage = builder.mAllowCrossNetworkUsage;
			this.mUseStaleOnNameNotResolved = builder.mUseStaleOnNameNotResolved;
		}

		public static final class Builder {
			private Duration mFreshLookupTimeout;
			private Duration mMaxExpiredDelay;
			private @DnsOptionState int mAllowCrossNetworkUsage;
			private @DnsOptionState int mUseStaleOnNameNotResolved;

			public Builder() {}

			@NonNull
			public Builder setFreshLookupTimeout(@NonNull Duration freshLookupTimeout) {
				this.mFreshLookupTimeout = freshLookupTimeout;
				return this;
			}

			@NonNull
			public Builder setMaxExpiredDelay(@NonNull Duration maxExpiredDelay) {
				this.mMaxExpiredDelay = maxExpiredDelay;
				return this;
			}

			@NonNull
			public Builder setAllowCrossNetworkUsage(@DnsOptionState int state) {
				this.mAllowCrossNetworkUsage = state;
				return this;
			}

			@NonNull
			public Builder setUseStaleOnNameNotResolved(@DnsOptionState int state) {
				this.mUseStaleOnNameNotResolved = state;
				return this;
			}

			@NonNull
			public StaleDnsOptions build() {
				return new StaleDnsOptions(this);
			}
		}
	}

	public static final class Builder {
		private @DnsOptionState int mUseHttpStackDnsResolver;
		private @DnsOptionState int mEnableStaleDns;
		@Nullable
		private StaleDnsOptions mStaleDnsOptions;
		private @DnsOptionState int mPersistHostCache;
		@Nullable
		private Duration mPersistHostCachePeriod;
		private @DnsOptionState int mPreestablishConnectionsToStaleDnsResults;

		public Builder() {}

		@NonNull
		public Builder setUseHttpStackDnsResolver(@DnsOptionState int state) {
			this.mUseHttpStackDnsResolver = state;
			return this;
		}

		@NonNull
		public Builder setStaleDns(@DnsOptionState int state) {
			this.mEnableStaleDns = state;
			return this;
		}

		@NonNull
		public Builder setStaleDnsOptions(@NonNull StaleDnsOptions staleDnsOptions) {
			this.mStaleDnsOptions = staleDnsOptions;
			return this;
		}

		@Experimental
		@NonNull
		public Builder setPreestablishConnectionsToStaleDnsResults(
				@DnsOptionState int state) {
			this.mPreestablishConnectionsToStaleDnsResults = state;
			return this;
		}

		@NonNull
		public Builder setPersistHostCache(@DnsOptionState int state) {
			this.mPersistHostCache = state;
			return this;
		}

		@NonNull
		public Builder setPersistHostCachePeriod(@NonNull Duration persistHostCachePeriod) {
			this.mPersistHostCachePeriod = persistHostCachePeriod;
			return this;
		}

		@NonNull
		public DnsOptions build() {
			return new DnsOptions(this);
		}
	}

	public @interface Experimental {}
}
