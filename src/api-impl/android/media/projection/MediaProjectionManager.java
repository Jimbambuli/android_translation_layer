package android.media.projection;

import android.content.ComponentName;
import android.content.Intent;

/* AOSP media/java/android/media/projection/MediaProjectionManager.java. The capture intent names the
 * permission dialog of AOSP's config_mediaProjectionPermissionDialogComponent (SystemUI, which ATL does
 * not have); no result ever carries a projection token, so getMediaProjection() returns null. */
public class MediaProjectionManager {
	public static final String EXTRA_MEDIA_PROJECTION = "android.media.projection.extra.EXTRA_MEDIA_PROJECTION";

	public Intent createScreenCaptureIntent() {
		Intent i = new Intent();
		i.setComponent(ComponentName.unflattenFromString(
		    "com.android.systemui/com.android.systemui.media.MediaProjectionPermissionActivity"));
		return i;
	}

	/* AOSP: null unless RESULT_OK with an EXTRA_MEDIA_PROJECTION binder, which ATL never hands out */
	public MediaProjection getMediaProjection(int resultCode, Intent resultData) {
		return null;
	}
}
