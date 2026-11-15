/* AOSP external/cronet android/java/src/android/net/http/ConnectionMigrationOptions.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;

@SuppressLint("UserHandleName")
public class ConnectionMigrationOptions {
	private final @MigrationOptionState int mEnableDefaultNetworkMigration;
	private final @MigrationOptionState int mEnablePathDegradationMigration;
	@Nullable
	private final Boolean mAllowServerMigration;
	@Nullable
	private final Boolean mMigrateIdleConnections;
	@Nullable
	private final Duration mIdleMigrationPeriod;
	private final @MigrationOptionState int mAllowNonDefaultNetworkUsage;
	@Nullable
	private final Duration mMaxTimeOnNonDefaultNetwork;
	@Nullable
	private final Integer mMaxWriteErrorNonDefaultNetworkMigrationsCount;
	@Nullable
	private final Integer mMaxPathDegradingNonDefaultMigrationsCount;

	public static final int MIGRATION_OPTION_UNSPECIFIED = 0;

	public static final int MIGRATION_OPTION_ENABLED = 1;

	public static final int MIGRATION_OPTION_DISABLED = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef(flag = false, prefix = "MIGRATION_OPTION_", value = {
			MIGRATION_OPTION_UNSPECIFIED,
			MIGRATION_OPTION_ENABLED,
			MIGRATION_OPTION_DISABLED,
	})
	public @interface MigrationOptionState {}

	public @MigrationOptionState int getDefaultNetworkMigration() {
		return mEnableDefaultNetworkMigration;
	}

	public @MigrationOptionState int getPathDegradationMigration() {
		return mEnablePathDegradationMigration;
	}

	public @MigrationOptionState int getAllowNonDefaultNetworkUsage() {
		return mAllowNonDefaultNetworkUsage;
	}

	ConnectionMigrationOptions(@NonNull Builder builder) {
		this.mEnableDefaultNetworkMigration = builder.mEnableDefaultNetworkMigration;
		this.mEnablePathDegradationMigration = builder.mEnablePathDegradationMigration;
		this.mAllowServerMigration = builder.mAllowServerMigration;
		this.mMigrateIdleConnections = builder.mMigrateIdleConnections;
		this.mIdleMigrationPeriod = builder.mIdleConnectionMigrationPeriod;
		this.mAllowNonDefaultNetworkUsage = builder.mAllowNonDefaultNetworkUsage;
		this.mMaxTimeOnNonDefaultNetwork = builder.mMaxTimeOnNonDefaultNetwork;
		this.mMaxWriteErrorNonDefaultNetworkMigrationsCount = builder.mMaxWriteErrorNonDefaultNetworkMigrationsCount;
		this.mMaxPathDegradingNonDefaultMigrationsCount = builder.mMaxPathDegradingNonDefaultMigrationsCount;
	}

	public static final class Builder {
		private @MigrationOptionState int mEnableDefaultNetworkMigration;
		private @MigrationOptionState int mEnablePathDegradationMigration;
		@Nullable
		private Boolean mAllowServerMigration;
		@Nullable
		private Boolean mMigrateIdleConnections;
		@Nullable
		private Duration mIdleConnectionMigrationPeriod;
		private @MigrationOptionState int mAllowNonDefaultNetworkUsage;
		@Nullable
		private Duration mMaxTimeOnNonDefaultNetwork;
		@Nullable
		private Integer mMaxWriteErrorNonDefaultNetworkMigrationsCount;
		@Nullable
		private Integer mMaxPathDegradingNonDefaultMigrationsCount;

		public Builder() {}

		@NonNull
		public Builder setDefaultNetworkMigration(@MigrationOptionState int state) {
			this.mEnableDefaultNetworkMigration = state;
			return this;
		}

		@NonNull
		public Builder setPathDegradationMigration(@MigrationOptionState int state) {
			this.mEnablePathDegradationMigration = state;
			return this;
		}

		@Experimental
		@NonNull
		public Builder setAllowNonDefaultNetworkUsage(@MigrationOptionState int state) {
			this.mAllowNonDefaultNetworkUsage = state;
			return this;
		}

		@NonNull
		public ConnectionMigrationOptions build() {
			return new ConnectionMigrationOptions(this);
		}
	}

	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	public @interface Experimental {}
}
