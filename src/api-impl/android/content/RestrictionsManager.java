package android.content;

import android.os.Bundle;

public class RestrictionsManager {
	/**
	 * Returns any available set of application-specific restrictions applicable
	 * to this application. As AOSP without a device policy controller that set any
	 * (ATL has no managed configurations), the bundle is empty.
	 */
	public Bundle getApplicationRestrictions() {
		return new Bundle();
	}

	public boolean hasRestrictionsProvider() {
		return false;
	}
}
