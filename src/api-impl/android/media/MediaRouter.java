package android.media;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Display;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP android.media.MediaRouter for one device without remote displays: the system's audio route (the default
 * route, always selected for audio and video) and the user routes an app adds itself (androidx.mediarouter's
 * SystemMediaRouteProvider publishes its routes that way). ATL discovers no routes: the callbacks hear only of
 * what the app adds, removes or selects. */
public class MediaRouter {
	public static final int ROUTE_TYPE_LIVE_AUDIO = 1 << 0;
	public static final int ROUTE_TYPE_LIVE_VIDEO = 1 << 1;
	public static final int ROUTE_TYPE_REMOTE_DISPLAY = 1 << 2;
	public static final int ROUTE_TYPE_USER = 1 << 23;

	/* AOSP MediaRouter.CALLBACK_FLAG_* */
	public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1 << 0;
	public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 1 << 1;
	public static final int CALLBACK_FLAG_REQUEST_DISCOVERY = 1 << 2;
	public static final int CALLBACK_FLAG_PASSIVE_DISCOVERY = 1 << 3;

	private final CopyOnWriteArrayList<CallbackInfo> mCallbacks = new CopyOnWriteArrayList<>();
	private final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
	private final ArrayList<RouteCategory> mCategories = new ArrayList<>();
	private final RouteInfo mDefaultRoute;
	private RouteInfo mSelectedRoute;

	private static class CallbackInfo {
		final int types;
		final Callback cb;

		CallbackInfo(int types, Callback cb) {
			this.types = types;
			this.cb = cb;
		}
	}

	public MediaRouter() {
		RouteCategory system = new RouteCategory(this, "System", ROUTE_TYPE_LIVE_AUDIO | ROUTE_TYPE_LIVE_VIDEO, false);
		mCategories.add(system);
		mDefaultRoute = new RouteInfo(system);
		mDefaultRoute.mName = "Device";
		mDefaultRoute.mSupportedTypes = ROUTE_TYPE_LIVE_AUDIO | ROUTE_TYPE_LIVE_VIDEO;
		mDefaultRoute.mDeviceType = RouteInfo.DEVICE_TYPE_SPEAKER;
		mDefaultRoute.mVolumeHandling = RouteInfo.PLAYBACK_VOLUME_FIXED;
		mRoutes.add(mDefaultRoute);
		mSelectedRoute = mDefaultRoute;
	}

	public RouteInfo getDefaultRoute() {
		return mDefaultRoute;
	}

	public RouteInfo getSelectedRoute(int type) {
		return (mSelectedRoute.mSupportedTypes & type) != 0 || type == 0 ? mSelectedRoute : mDefaultRoute;
	}

	public void selectRoute(int types, RouteInfo route) {
		if (route == null)
			throw new IllegalArgumentException("Route cannot be null.");
		RouteInfo old = mSelectedRoute;
		if (old == route)
			return;
		mSelectedRoute = route;
		for (CallbackInfo ci : mCallbacks) {
			if ((ci.types & old.mSupportedTypes) != 0)
				ci.cb.onRouteUnselected(this, types, old);
			if ((ci.types & route.mSupportedTypes) != 0)
				ci.cb.onRouteSelected(this, types, route);
		}
	}

	public void addCallback(int types, Callback cb) {
		addCallback(types, cb, 0);
	}

	public void addCallback(int types, Callback cb, int flags) {
		for (CallbackInfo ci : mCallbacks)
			if (ci.cb == cb)
				return;
		mCallbacks.add(new CallbackInfo(types, cb));
	}

	public void removeCallback(Callback cb) {
		for (CallbackInfo ci : mCallbacks)
			if (ci.cb == cb)
				mCallbacks.remove(ci);
	}

	public RouteCategory createRouteCategory(CharSequence name, boolean isGroupable) {
		return new RouteCategory(this, name, ROUTE_TYPE_USER, isGroupable);
	}

	public UserRouteInfo createUserRoute(RouteCategory category) {
		return new UserRouteInfo(category);
	}

	public void addUserRoute(UserRouteInfo info) {
		if (mRoutes.contains(info))
			return;
		if (!mCategories.contains(info.mCategory))
			mCategories.add(info.mCategory);
		mRoutes.add(info);
		for (CallbackInfo ci : mCallbacks)
			if ((ci.types & info.mSupportedTypes) != 0)
				ci.cb.onRouteAdded(this, info);
	}

	public void removeUserRoute(UserRouteInfo info) {
		if (!mRoutes.remove(info))
			return;
		if (mSelectedRoute == info)
			selectRoute(ROUTE_TYPE_LIVE_AUDIO | ROUTE_TYPE_LIVE_VIDEO, mDefaultRoute);
		for (CallbackInfo ci : mCallbacks)
			if ((ci.types & info.mSupportedTypes) != 0)
				ci.cb.onRouteRemoved(this, info);
	}

	public void clearUserRoutes() {
		for (RouteInfo r : new ArrayList<>(mRoutes))
			if (r instanceof UserRouteInfo)
				removeUserRoute((UserRouteInfo)r);
	}

	public int getRouteCount() {
		return mRoutes.size();
	}

	public RouteInfo getRouteAt(int index) {
		return mRoutes.get(index);
	}

	public int getCategoryCount() {
		return mCategories.size();
	}

	public RouteCategory getCategoryAt(int index) {
		return mCategories.get(index);
	}

	public static class RouteCategory {
		final MediaRouter mRouter;
		final CharSequence mName;
		final int mTypes;
		final boolean mGroupable;

		RouteCategory(MediaRouter router, CharSequence name, int types, boolean groupable) {
			mRouter = router;
			mName = name;
			mTypes = types;
			mGroupable = groupable;
		}

		public CharSequence getName() {
			return mName;
		}

		public CharSequence getName(Context context) {
			return mName;
		}

		public List<RouteInfo> getRoutes(List<RouteInfo> out) {
			if (out == null)
				out = new ArrayList<>();
			else
				out.clear();
			for (RouteInfo route : mRouter.mRoutes)
				if (route.mCategory == this)
					out.add(route);
			return out;
		}

		public int getSupportedTypes() {
			return mTypes;
		}

		public boolean isGroupable() {
			return mGroupable;
		}

		public boolean isSystem() {
			return mTypes != ROUTE_TYPE_USER;
		}
	}

	public static class RouteInfo {
		public static final int PLAYBACK_TYPE_LOCAL = 0;
		public static final int PLAYBACK_TYPE_REMOTE = 1;
		public static final int PLAYBACK_VOLUME_FIXED = 0;
		public static final int PLAYBACK_VOLUME_VARIABLE = 1;
		public static final int DEVICE_TYPE_UNKNOWN = 0;
		public static final int DEVICE_TYPE_TV = 1;
		public static final int DEVICE_TYPE_SPEAKER = 2;
		public static final int DEVICE_TYPE_BLUETOOTH = 3;

		final RouteCategory mCategory;
		CharSequence mName;
		CharSequence mDescription;
		CharSequence mStatus;
		Object mTag;
		Drawable mIcon;
		RouteGroup mGroup;
		int mSupportedTypes;
		int mPlaybackType = PLAYBACK_TYPE_LOCAL;
		int mPlaybackStream = AudioManager.STREAM_MUSIC;
		int mVolume = 15;
		int mVolumeMax = 15;
		int mVolumeHandling = PLAYBACK_VOLUME_VARIABLE;
		int mDeviceType = DEVICE_TYPE_UNKNOWN;
		boolean mEnabled = true;

		RouteInfo(RouteCategory category) {
			mCategory = category;
		}

		public CharSequence getName() {
			return mName;
		}

		public CharSequence getName(Context context) {
			return mName;
		}

		public CharSequence getDescription() {
			return mDescription;
		}

		public CharSequence getStatus() {
			return mStatus;
		}

		public RouteCategory getCategory() {
			return mCategory;
		}

		public RouteGroup getGroup() {
			return mGroup;
		}

		public Drawable getIconDrawable() {
			return mIcon;
		}

		public Object getTag() {
			return mTag;
		}

		public void setTag(Object tag) {
			mTag = tag;
		}

		public int getSupportedTypes() {
			return mSupportedTypes;
		}

		public int getPlaybackType() {
			return mPlaybackType;
		}

		public int getPlaybackStream() {
			return mPlaybackStream;
		}

		public int getVolume() {
			return mVolume;
		}

		public int getVolumeMax() {
			return mVolumeMax;
		}

		public int getVolumeHandling() {
			return mVolumeHandling;
		}

		public int getDeviceType() {
			return mDeviceType;
		}

		public boolean isEnabled() {
			return mEnabled;
		}

		public boolean isConnecting() {
			return false;
		}

		/* like the default route of AOSP without a presentation display (DisplayManager names none) */
		public Display getPresentationDisplay() {
			return null;
		}

		/* the system route's volume is the desktop's; a user route asks its VolumeCallback (UserRouteInfo) */
		public void requestSetVolume(int volume) {}

		public void requestUpdateVolume(int direction) {}

		@Override
		public String toString() {
			return "RouteInfo{ name=" + mName + ", types=" + mSupportedTypes + " }";
		}
	}

	public static class UserRouteInfo extends RouteInfo {
		VolumeCallback mVolumeCallback;
		RemoteControlClient mRcc;

		UserRouteInfo(RouteCategory category) {
			super(category);
			mSupportedTypes = ROUTE_TYPE_USER;
			mPlaybackType = PLAYBACK_TYPE_REMOTE;
			mVolumeHandling = PLAYBACK_VOLUME_FIXED;
		}

		public void setName(CharSequence name) {
			mName = name;
		}

		public void setName(int resId) {}

		public void setDescription(CharSequence description) {
			mDescription = description;
		}

		public void setStatus(CharSequence status) {
			mStatus = status;
		}

		public void setIconDrawable(Drawable icon) {
			mIcon = icon;
		}

		public void setIconResource(int resId) {}

		public void setPlaybackType(int type) {
			mPlaybackType = type;
		}

		public void setPlaybackStream(int stream) {
			mPlaybackStream = stream;
		}

		public void setVolumeHandling(int volumeHandling) {
			mVolumeHandling = volumeHandling;
		}

		public void setVolumeMax(int volumeMax) {
			mVolumeMax = volumeMax;
		}

		public void setVolume(int volume) {
			mVolume = Math.max(0, Math.min(volume, mVolumeMax));
		}

		public void setVolumeCallback(VolumeCallback vcb) {
			mVolumeCallback = vcb;
		}

		public void setRemoteControlClient(RemoteControlClient rcc) {
			mRcc = rcc;
		}

		public RemoteControlClient getRemoteControlClient() {
			return mRcc;
		}

		@Override
		public void requestSetVolume(int volume) {
			if (mVolumeHandling == PLAYBACK_VOLUME_VARIABLE && mVolumeCallback != null)
				mVolumeCallback.onVolumeSetRequest(this, volume);
		}

		@Override
		public void requestUpdateVolume(int direction) {
			if (mVolumeHandling == PLAYBACK_VOLUME_VARIABLE && mVolumeCallback != null)
				mVolumeCallback.onVolumeUpdateRequest(this, direction);
		}
	}

	public static class RouteGroup extends RouteInfo {
		final ArrayList<RouteInfo> mRoutes = new ArrayList<>();

		RouteGroup(RouteCategory category) {
			super(category);
		}

		public int getRouteCount() {
			return mRoutes.size();
		}

		public RouteInfo getRouteAt(int index) {
			return mRoutes.get(index);
		}
	}

	/* AOSP MediaRouter.VolumeCallback */
	public static abstract class VolumeCallback {
		public abstract void onVolumeUpdateRequest(RouteInfo info, int direction);
		public abstract void onVolumeSetRequest(RouteInfo info, int volume);
	}

	/* AOSP MediaRouter.Callback */
	public static abstract class Callback {
		public abstract void onRouteSelected(MediaRouter router, int type, RouteInfo info);
		public abstract void onRouteUnselected(MediaRouter router, int type, RouteInfo info);
		public abstract void onRouteAdded(MediaRouter router, RouteInfo info);
		public abstract void onRouteRemoved(MediaRouter router, RouteInfo info);
		public abstract void onRouteChanged(MediaRouter router, RouteInfo info);
		public abstract void onRouteGrouped(MediaRouter router, RouteInfo info, RouteGroup group, int index);
		public abstract void onRouteUngrouped(MediaRouter router, RouteInfo info, RouteGroup group);
		public abstract void onRouteVolumeChanged(MediaRouter router, RouteInfo info);
		public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
		}
	}

	/* AOSP MediaRouter.SimpleCallback */
	public static class SimpleCallback extends Callback {
		@Override
		public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
		}

		@Override
		public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
		}

		@Override
		public void onRouteAdded(MediaRouter router, RouteInfo info) {
		}

		@Override
		public void onRouteRemoved(MediaRouter router, RouteInfo info) {
		}

		@Override
		public void onRouteChanged(MediaRouter router, RouteInfo info) {
		}

		@Override
		public void onRouteGrouped(MediaRouter router, RouteInfo info, RouteGroup group, int index) {
		}

		@Override
		public void onRouteUngrouped(MediaRouter router, RouteInfo info, RouteGroup group) {
		}

		@Override
		public void onRouteVolumeChanged(MediaRouter router, RouteInfo info) {
		}
	}
}
