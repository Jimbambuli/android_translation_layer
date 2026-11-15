package android.content;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/* AOSP frameworks/base/core/java/android/content/ContextParams.java. Without the permission check of
 * setRenouncedPermissions(): ATL has no permission service to ask. */
public final class ContextParams {
	private final String mAttributionTag;
	private final AttributionSource mNext;
	private final Set<String> mRenouncedPermissions;
	private final boolean mShouldRegisterAttributionSource;

	public static final ContextParams EMPTY = new ContextParams.Builder().build();

	private ContextParams(String attributionTag, AttributionSource next, Set<String> renouncedPermissions, boolean shouldRegister) {
		mAttributionTag = attributionTag;
		mNext = next;
		mRenouncedPermissions = (renouncedPermissions != null) ? renouncedPermissions : Collections.emptySet();
		mShouldRegisterAttributionSource = shouldRegister;
	}

	public String getAttributionTag() {
		return mAttributionTag;
	}

	public Set<String> getRenouncedPermissions() {
		return mRenouncedPermissions;
	}

	public boolean isRenouncedPermission(String permission) {
		return mRenouncedPermissions.contains(permission);
	}

	public AttributionSource getNextAttributionSource() {
		return mNext;
	}

	public boolean shouldRegisterAttributionSource() {
		return mShouldRegisterAttributionSource;
	}

	public static final class Builder {
		private String mAttributionTag;
		private Set<String> mRenouncedPermissions = Collections.emptySet();
		private AttributionSource mNext;
		private boolean mShouldRegisterAttributionSource;

		public Builder() {}

		public Builder(ContextParams params) {
			Objects.requireNonNull(params);
			mAttributionTag = params.mAttributionTag;
			mRenouncedPermissions = params.mRenouncedPermissions;
			mNext = params.mNext;
		}

		public Builder setAttributionTag(String attributionTag) {
			mAttributionTag = attributionTag;
			return this;
		}

		public Builder setNextAttributionSource(AttributionSource next) {
			mNext = next;
			return this;
		}

		public Builder setShouldRegisterAttributionSource(boolean shouldRegister) {
			mShouldRegisterAttributionSource = shouldRegister;
			return this;
		}

		public Builder setRenouncedPermissions(Set<String> renouncedPermissions) {
			mRenouncedPermissions = renouncedPermissions;
			return this;
		}

		public ContextParams build() {
			return new ContextParams(mAttributionTag, mNext, mRenouncedPermissions, mShouldRegisterAttributionSource);
		}
	}
}
