package android.app;

import android.os.Bundle;

/* AOSP BroadcastOptions (android-16), the subset apps use: makeBasic(), the share identity flag
 * and toBundle() with AOSP's key. ATL's broadcasts do not read the options. */
public class BroadcastOptions {
	private static final String KEY_FLAGS = "android:broadcast.flags";
	private static final int FLAG_SHARE_IDENTITY = 1 << 4;

	private int mFlags;

	public static BroadcastOptions makeBasic() {
		return new BroadcastOptions();
	}

	public BroadcastOptions() {}

	public BroadcastOptions setShareIdentityEnabled(boolean shareIdentityEnabled) {
		if (shareIdentityEnabled) {
			mFlags |= FLAG_SHARE_IDENTITY;
		} else {
			mFlags &= ~FLAG_SHARE_IDENTITY;
		}
		return this;
	}

	public boolean isShareIdentityEnabled() {
		return (mFlags & FLAG_SHARE_IDENTITY) != 0;
	}

	public Bundle toBundle() {
		Bundle b = new Bundle();
		if (mFlags != 0) {
			b.putInt(KEY_FLAGS, mFlags);
		}
		return b;
	}
}
