package android.content.pm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import java.util.Collections;
import java.util.List;

/**
 * Class for handling cross profile operations. Apps can use this class to interact with its
 * instance in any profile that is in {@link #getTargetUserProfiles()}.
 *
 * ATL runs an app as a single user without managed profiles, so there is never a target profile
 * and nothing to interact with (as AOSP on a device without a work profile).
 */
public class CrossProfileApps {
	public static final String ACTION_CAN_INTERACT_ACROSS_PROFILES_CHANGED =
	    "android.content.pm.action.CAN_INTERACT_ACROSS_PROFILES_CHANGED";

	/** @hide */
	public CrossProfileApps() {}

	public void startMainActivity(ComponentName component, UserHandle targetUser) {
		throw new SecurityException("Not allowed to start activity in another profile: " + targetUser);
	}

	public void startMainActivity(ComponentName component, UserHandle targetUser, Activity callingActivity, Bundle options) {
		throw new SecurityException("Not allowed to start activity in another profile: " + targetUser);
	}

	public List<UserHandle> getTargetUserProfiles() {
		return Collections.emptyList();
	}

	public CharSequence getProfileSwitchingLabel(UserHandle userHandle) {
		throw new SecurityException("Not allowed to get the profile switching label of " + userHandle);
	}

	public Drawable getProfileSwitchingIconDrawable(UserHandle userHandle) {
		throw new SecurityException("Not allowed to get the profile switching icon of " + userHandle);
	}

	public boolean canRequestInteractAcrossProfiles() {
		return false;
	}

	public boolean canInteractAcrossProfiles() {
		return false;
	}

	public Intent createRequestInteractAcrossProfilesIntent() {
		if (!canRequestInteractAcrossProfiles()) {
			throw new SecurityException("The calling package can not request to interact across profiles.");
		}
		return null;
	}
}
