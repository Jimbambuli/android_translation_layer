package android.media;

import android.view.Display;
import java.util.concurrent.CopyOnWriteArrayList;

public class MediaRouter {
	public static final int ROUTE_TYPE_LIVE_VIDEO = 0x2;

	/* AOSP MediaRouter.CALLBACK_FLAG_* */
	public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1 << 0;
	public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 1 << 1;
	public static final int CALLBACK_FLAG_REQUEST_DISCOVERY = 1 << 2;
	public static final int CALLBACK_FLAG_PASSIVE_DISCOVERY = 1 << 3;

	/* ATL has only the default route and it never changes: the callbacks are kept, but never called */
	private final CopyOnWriteArrayList<Callback> mCallbacks = new CopyOnWriteArrayList<>();

	public static class RouteInfo {
		/* like the default route of AOSP without a presentation display (DisplayManager names none) */
		public Display getPresentationDisplay() {
			return null;
		}
	}

	public static class RouteGroup extends RouteInfo {
	}

	public RouteInfo getSelectedRoute(int type) {
		return new RouteInfo();
	}

	public void addCallback(int types, Callback cb) {
		addCallback(types, cb, 0);
	}

	public void addCallback(int types, Callback cb, int flags) {
		if (!mCallbacks.contains(cb))
			mCallbacks.add(cb);
	}

	public void removeCallback(Callback cb) {
		mCallbacks.remove(cb);
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
