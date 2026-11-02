package android.content;

import android.os.Bundle;

/* ATL: no device management, so no restrictions */
public class RestrictionsManager {
	public Bundle getApplicationRestrictions() {
		return new Bundle();
	}

	public boolean hasRestrictionsProvider() {
		return false;
	}
}
