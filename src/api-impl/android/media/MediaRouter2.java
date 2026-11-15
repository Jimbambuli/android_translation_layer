package android.media;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/* AOSP media/java/android/media/MediaRouter2.java (public API) for a device that has no MediaRouterService and
 * no route providers: the router knows exactly the built-in speaker as system route in the system session, as
 * the system provider of AOSP lists it. No other route ever appears, so transfers to anything else fail with
 * onTransferFailure and showSystemOutputSwitcher() returns false (no SystemUI to show the dialog; AOSP returns
 * false when the call is ignored). The internal RouterImpl/stub plumbing of AOSP is left out. */
public final class MediaRouter2 {
	private static final String TAG = "MR2";
	private static final String SYSTEM_SESSION_ID = "SYSTEM_SESSION";

	private static final Object sRouterLock = new Object();
	private static MediaRouter2 sInstance;

	private final Context mContext;
	private final Object mLock = new Object();
	private final Map<String, MediaRoute2Info> mRoutes = new LinkedHashMap<>();
	private final CopyOnWriteArrayList<RouteCallbackRecord> mRouteCallbackRecords = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<TransferCallbackRecord> mTransferCallbackRecords = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<ControllerCallbackRecord> mControllerCallbackRecords = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<ListingPreferenceCallbackRecord> mListingPreferenceCallbackRecords =
	    new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<SuggestionsCallbackRecord> mDeviceSuggestionsUpdatesCallbackRecords =
	    new CopyOnWriteArrayList<>();
	private final RoutingController mSystemController;
	private volatile List<MediaRoute2Info> mFilteredRoutes = Collections.emptyList();
	private volatile OnGetControllerHintsListener mOnGetControllerHintsListener;
	private RouteListingPreference mRouteListingPreference;
	private List<SuggestedDeviceInfo> mDeviceSuggestions = Collections.emptyList();
	private int mNextScanId = 1;

	/** Gets an instance of the media router associated with the context. */
	@NonNull
	public static MediaRouter2 getInstance(@NonNull Context context) {
		Objects.requireNonNull(context, "context must not be null");
		synchronized (sRouterLock) {
			if (sInstance == null) {
				sInstance = new MediaRouter2(context.getApplicationContext());
			}
			return sInstance;
		}
	}

	/* AOSP findOrCreateProxyInstanceForCallingUser(): registering a proxy router needs MEDIA_CONTENT_CONTROL or
	 * MEDIA_ROUTING_CONTROL and a MediaRouterService; without the service the call fails like for a caller
	 * that lacks the permission. */
	@NonNull
	public static MediaRouter2 getInstance(@NonNull Context context, @NonNull String clientPackageName,
	                                       @NonNull Executor executor, @NonNull Runnable onInstanceInvalidatedListener) {
		Objects.requireNonNull(executor, "Executor must not be null");
		Objects.requireNonNull(onInstanceInvalidatedListener, "onInstanceInvalidatedListener must not be null.");
		Objects.requireNonNull(context, "context must not be null");
		if (TextUtils.isEmpty(clientPackageName)) {
			throw new IllegalArgumentException("clientPackageName must not be null or empty");
		}
		throw new SecurityException("Requires MEDIA_CONTENT_CONTROL or MEDIA_ROUTING_CONTROL permission");
	}

	private MediaRouter2(Context appContext) {
		mContext = appContext;
		MediaRoute2Info systemRoute = new MediaRoute2Info.Builder(MediaRoute2Info.ROUTE_ID_DEFAULT, "Device")
		                                  .setSystemRoute(true)
		                                  .setType(MediaRoute2Info.TYPE_BUILTIN_SPEAKER)
		                                  .addFeature(MediaRoute2Info.FEATURE_LIVE_AUDIO)
		                                  .addFeature(MediaRoute2Info.FEATURE_LIVE_VIDEO)
		                                  .addFeature(MediaRoute2Info.FEATURE_LOCAL_PLAYBACK)
		                                  .setVolumeHandling(MediaRoute2Info.PLAYBACK_VOLUME_FIXED)
		                                  .build();
		mRoutes.put(systemRoute.getId(), systemRoute);
		RoutingSessionInfo session = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, mContext.getPackageName())
		                                 .setName("Device")
		                                 .addSelectedRoute(systemRoute.getId())
		                                 .setVolumeHandling(MediaRoute2Info.PLAYBACK_VOLUME_FIXED)
		                                 .setSystemSession(true)
		                                 .build();
		mSystemController = new SystemRoutingController(session);
	}

	/* AOSP requestScan(): the scanning state goes to the MediaRouterService; here there is nothing to scan. */
	@NonNull
	public ScanToken requestScan(@NonNull ScanRequest scanRequest) {
		Objects.requireNonNull(scanRequest, "scanRequest must not be null.");
		synchronized (mLock) {
			return new ScanToken(mNextScanId++);
		}
	}

	public void cancelScanRequest(@NonNull ScanToken token) {
		Objects.requireNonNull(token, "token must not be null");
	}

	public void registerRouteCallback(@NonNull @CallbackExecutor Executor executor, @NonNull RouteCallback routeCallback,
	                                  @NonNull RouteDiscoveryPreference preference) {
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(routeCallback, "callback must not be null");
		Objects.requireNonNull(preference, "preference must not be null");
		RouteCallbackRecord record = new RouteCallbackRecord(executor, routeCallback, preference);
		mRouteCallbackRecords.remove(record);
		mRouteCallbackRecords.addIfAbsent(record);
		List<MediaRoute2Info> previous = mFilteredRoutes;
		List<MediaRoute2Info> routes = updateFilteredRoutes();
		if (!routes.equals(previous)) {
			dispatchRoutesUpdated(routes);
		}
	}

	public void unregisterRouteCallback(@NonNull RouteCallback routeCallback) {
		Objects.requireNonNull(routeCallback, "callback must not be null");
		if (!mRouteCallbackRecords.remove(new RouteCallbackRecord(null, routeCallback, null))) {
			Log.w(TAG, "unregisterRouteCallback: Ignoring unknown callback");
			return;
		}
		List<MediaRoute2Info> previous = mFilteredRoutes;
		List<MediaRoute2Info> routes = updateFilteredRoutes();
		if (!routes.equals(previous)) {
			dispatchRoutesUpdated(routes);
		}
	}

	public void registerRouteListingPreferenceUpdatedCallback(@NonNull @CallbackExecutor Executor executor,
	                                                          @NonNull Consumer<RouteListingPreference> routeListingPreferenceCallback) {
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(routeListingPreferenceCallback, "callback must not be null");
		ListingPreferenceCallbackRecord record = new ListingPreferenceCallbackRecord(executor, routeListingPreferenceCallback);
		mListingPreferenceCallbackRecords.remove(record);
		mListingPreferenceCallbackRecords.add(record);
	}

	public void unregisterRouteListingPreferenceUpdatedCallback(@NonNull Consumer<RouteListingPreference> callback) {
		Objects.requireNonNull(callback, "callback must not be null");
		mListingPreferenceCallbackRecords.remove(new ListingPreferenceCallbackRecord(null, callback));
	}

	public void registerDeviceSuggestionsUpdatesCallback(@NonNull @CallbackExecutor Executor executor,
	                                                     @NonNull DeviceSuggestionsUpdatesCallback deviceSuggestionsUpdatesCallback) {
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(deviceSuggestionsUpdatesCallback, "callback must not be null");
		SuggestionsCallbackRecord record = new SuggestionsCallbackRecord(executor, deviceSuggestionsUpdatesCallback);
		mDeviceSuggestionsUpdatesCallbackRecords.remove(record);
		mDeviceSuggestionsUpdatesCallbackRecords.add(record);
	}

	public void unregisterDeviceSuggestionsUpdatesCallback(@NonNull DeviceSuggestionsUpdatesCallback callback) {
		Objects.requireNonNull(callback, "callback must not be null");
		mDeviceSuggestionsUpdatesCallbackRecords.remove(new SuggestionsCallbackRecord(null, callback));
	}

	public boolean showSystemOutputSwitcher() {
		return false;
	}

	public boolean showSystemOutputSwitcher(@NonNull MediaSession.Token sessionToken) {
		Objects.requireNonNull(sessionToken, "sessionToken must not be null");
		return false;
	}

	/* AOSP LocalMediaRouter2Impl.setRouteListingPreference(): stores the preference and tells the listeners;
	 * ATL has no manager that would forward it to a UI. */
	public void setRouteListingPreference(@Nullable RouteListingPreference routeListingPreference) {
		synchronized (mLock) {
			if (Objects.equals(mRouteListingPreference, routeListingPreference)) {
				return;
			}
			mRouteListingPreference = routeListingPreference;
		}
		for (ListingPreferenceCallbackRecord record : mListingPreferenceCallbackRecords) {
			record.mExecutor.execute(() -> record.mCallback.accept(routeListingPreference));
		}
	}

	@Nullable
	public RouteListingPreference getRouteListingPreference() {
		synchronized (mLock) {
			return mRouteListingPreference;
		}
	}

	public void setDeviceSuggestions(@NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {
		Objects.requireNonNull(suggestedDeviceInfo, "suggestedDeviceInfo must not be null");
		synchronized (mLock) {
			mDeviceSuggestions = List.copyOf(suggestedDeviceInfo);
		}
	}

	public void clearDeviceSuggestions() {
		synchronized (mLock) {
			mDeviceSuggestions = Collections.emptyList();
		}
	}

	/* AOSP: the suggestions of all suggesting packages, keyed by package name; only this app can suggest here */
	@NonNull
	public Map<String, List<SuggestedDeviceInfo>> getDeviceSuggestions() {
		synchronized (mLock) {
			if (mDeviceSuggestions.isEmpty()) {
				return Collections.emptyMap();
			}
			return Collections.singletonMap(mContext.getPackageName(), mDeviceSuggestions);
		}
	}

	@NonNull
	public List<MediaRoute2Info> getRoutes() {
		return mFilteredRoutes;
	}

	public void registerTransferCallback(@NonNull @CallbackExecutor Executor executor, @NonNull TransferCallback callback) {
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(callback, "callback must not be null");
		TransferCallbackRecord record = new TransferCallbackRecord(executor, callback);
		if (!mTransferCallbackRecords.addIfAbsent(record)) {
			Log.w(TAG, "registerTransferCallback: Ignoring the same callback");
		}
	}

	public void unregisterTransferCallback(@NonNull TransferCallback callback) {
		Objects.requireNonNull(callback, "callback must not be null");
		if (!mTransferCallbackRecords.remove(new TransferCallbackRecord(null, callback))) {
			Log.w(TAG, "unregisterTransferCallback: Ignoring an unknown callback");
		}
	}

	public void registerControllerCallback(@NonNull @CallbackExecutor Executor executor, @NonNull ControllerCallback callback) {
		Objects.requireNonNull(executor, "executor must not be null");
		Objects.requireNonNull(callback, "callback must not be null");
		ControllerCallbackRecord record = new ControllerCallbackRecord(executor, callback);
		if (!mControllerCallbackRecords.addIfAbsent(record)) {
			Log.w(TAG, "registerControllerCallback: Ignoring the same callback");
		}
	}

	public void unregisterControllerCallback(@NonNull ControllerCallback callback) {
		Objects.requireNonNull(callback, "callback must not be null");
		if (!mControllerCallbackRecords.remove(new ControllerCallbackRecord(null, callback))) {
			Log.w(TAG, "unregisterControllerCallback: Ignoring an unknown callback");
		}
	}

	public void setOnGetControllerHintsListener(@Nullable OnGetControllerHintsListener listener) {
		mOnGetControllerHintsListener = listener;
	}

	/* AOSP LocalMediaRouter2Impl.transferTo(): an unknown route is a failure; the system route is the only
	 * route and already selected by the system controller, so transferring to it changes nothing (AOSP
	 * tryTransferWithinProvider: system-route reselection returns true). Other routes cannot exist. */
	public void transferTo(@NonNull MediaRoute2Info route) {
		Log.v(TAG, "Transferring to route: " + route);
		boolean routeFound;
		synchronized (mLock) {
			routeFound = mRoutes.containsKey(route.getId());
		}
		if (!routeFound || !route.isSystemRoute()) {
			notifyTransferFailure(route);
		}
	}

	/* AOSP LocalMediaRouter2Impl.stop(): releases the current controller, which is the system controller;
	 * that one cannot be released. */
	public void stop() {
		getCurrentController().release();
	}

	@NonNull
	public RoutingController getSystemController() {
		return mSystemController;
	}

	@Nullable
	public RoutingController getController(@NonNull String id) {
		Objects.requireNonNull(id, "id must not be null");
		for (RoutingController controller : getControllers()) {
			if (TextUtils.equals(id, controller.getId())) {
				return controller;
			}
		}
		return null;
	}

	@NonNull
	public List<RoutingController> getControllers() {
		List<RoutingController> result = new ArrayList<>();
		result.add(mSystemController);
		return result;
	}

	/* AOSP LocalMediaRouter2Impl.setRouteVolume(): only proxy routers may set route volumes */
	public void setRouteVolume(@NonNull MediaRoute2Info route, int volume) {
		Objects.requireNonNull(route, "route must not be null");
		throw new UnsupportedOperationException("setRouteVolume is only supported by proxy routers. See javadoc.");
	}

	@NonNull
	private RoutingController getCurrentController() {
		List<RoutingController> controllers = getControllers();
		return controllers.get(controllers.size() - 1);
	}

	@NonNull
	private List<MediaRoute2Info> getRoutesWithIds(@NonNull List<String> routeIds) {
		synchronized (mLock) {
			return routeIds.stream().map(mRoutes::get).filter(Objects::nonNull).collect(Collectors.toList());
		}
	}

	/* AOSP filterRoutesWithCompositePreferenceLocked(): the routes that match the preferred features and allowed
	 * packages of the registered callbacks (union over all callbacks, no deduplication as there is one route) */
	private List<MediaRoute2Info> updateFilteredRoutes() {
		List<MediaRoute2Info> result = new ArrayList<>();
		synchronized (mLock) {
			for (MediaRoute2Info route : mRoutes.values()) {
				for (RouteCallbackRecord record : mRouteCallbackRecords) {
					if (matches(route, record.mPreference)) {
						result.add(route);
						break;
					}
				}
			}
		}
		result = Collections.unmodifiableList(result);
		mFilteredRoutes = result;
		return result;
	}

	private static boolean matches(MediaRoute2Info route, RouteDiscoveryPreference preference) {
		if (!route.hasAnyFeatures(preference.getPreferredFeatures())) {
			return false;
		}
		return preference.getAllowedPackages().isEmpty()
		    || (route.getProviderPackageName() != null && preference.getAllowedPackages().contains(route.getProviderPackageName()));
	}

	private void dispatchRoutesUpdated(List<MediaRoute2Info> routes) {
		for (RouteCallbackRecord record : mRouteCallbackRecords) {
			List<MediaRoute2Info> filtered = new ArrayList<>();
			for (MediaRoute2Info route : routes) {
				if (matches(route, record.mPreference)) {
					filtered.add(route);
				}
			}
			record.mExecutor.execute(() -> record.mRouteCallback.onRoutesUpdated(filtered));
		}
	}

	private void notifyTransferFailure(MediaRoute2Info route) {
		for (TransferCallbackRecord record : mTransferCallbackRecords) {
			record.mExecutor.execute(() -> record.mTransferCallback.onTransferFailure(route));
		}
	}

	/** Callback for receiving events about device suggestions */
	public interface DeviceSuggestionsUpdatesCallback {
		void onSuggestionsUpdated(@NonNull String suggestingPackageName, @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo);

		void onSuggestionsCleared(@NonNull String suggestingPackageName);

		void onSuggestionsRequested();
	}

	/** Callback for receiving events about media route discovery. */
	public abstract static class RouteCallback {
		@Deprecated
		public void onRoutesAdded(@NonNull List<MediaRoute2Info> routes) {}

		@Deprecated
		public void onRoutesRemoved(@NonNull List<MediaRoute2Info> routes) {}

		@Deprecated
		public void onRoutesChanged(@NonNull List<MediaRoute2Info> routes) {}

		public void onRoutesUpdated(@NonNull List<MediaRoute2Info> routes) {}
	
}

	/** Callback for receiving events on route transfer. */
	public abstract static class TransferCallback {
		public void onTransfer(@NonNull RoutingController oldController, @NonNull RoutingController newController) {}

		public void onTransferFailure(@NonNull MediaRoute2Info requestedRoute) {}

		public void onStop(@NonNull RoutingController controller) {}
	
}

	/** Callback for receiving events about {@link RoutingController} changes. */
	public abstract static class ControllerCallback {
		public void onControllerUpdated(@NonNull RoutingController controller) {}
	
}

	/** A listener interface to send optional app-specific hints when creating a RoutingController. */
	public interface OnGetControllerHintsListener {
		@Nullable
		Bundle onGetControllerHints(@NonNull MediaRoute2Info route);
	}

	public static final class ScanToken {
		private final int mId;

		private ScanToken(int id) {
			mId = id;
		}
	}

	public static final class ScanRequest {
		private final boolean mIsScreenOffScan;

		private ScanRequest(boolean isScreenOffScan) {
			mIsScreenOffScan = isScreenOffScan;
		}

		public boolean isScreenOffScan() {
			return mIsScreenOffScan;
		}

		public static final class Builder {
			boolean mIsScreenOffScan;

			public Builder() {}

			@NonNull
			public Builder setScreenOffScan(boolean isScreenOffScan) {
				mIsScreenOffScan = isScreenOffScan;
				return this;
			}

			@NonNull
			public ScanRequest build() {
				return new ScanRequest(mIsScreenOffScan);
			}
		}
	}

	/** A controller that controls the routes of a routing session. */
	public class RoutingController {
		private final Object mControllerLock = new Object();
		private RoutingSessionInfo mSessionInfo;
		private boolean mReleased;

		RoutingController(@NonNull RoutingSessionInfo sessionInfo) {
			mSessionInfo = sessionInfo;
		}

		@NonNull
		public String getId() {
			synchronized (mControllerLock) {
				return mSessionInfo.getId();
			}
		}

		@Nullable
		public Bundle getControlHints() {
			synchronized (mControllerLock) {
				return mSessionInfo.getControlHints();
			}
		}

		@NonNull
		public List<MediaRoute2Info> getSelectedRoutes() {
			synchronized (mControllerLock) {
				return getRoutesWithIds(mSessionInfo.getSelectedRoutes());
			}
		}

		@NonNull
		public List<MediaRoute2Info> getSelectableRoutes() {
			synchronized (mControllerLock) {
				return getRoutesWithIds(mSessionInfo.getSelectableRoutes());
			}
		}

		@NonNull
		public List<MediaRoute2Info> getDeselectableRoutes() {
			synchronized (mControllerLock) {
				return getRoutesWithIds(mSessionInfo.getDeselectableRoutes());
			}
		}

		@NonNull
		public List<MediaRoute2Info> getTransferableRoutes() {
			synchronized (mControllerLock) {
				return getRoutesWithIds(mSessionInfo.getTransferableRoutes());
			}
		}

		/* AOSP: asks the impl whether this app started the transfer; no transfer is ever started here */
		public boolean wasTransferInitiatedBySelf() {
			return false;
		}

		@NonNull
		public RoutingSessionInfo getRoutingSessionInfo() {
			synchronized (mControllerLock) {
				return mSessionInfo;
			}
		}

		public int getVolumeHandling() {
			synchronized (mControllerLock) {
				return mSessionInfo.getVolumeHandling();
			}
		}

		public int getVolumeMax() {
			synchronized (mControllerLock) {
				return mSessionInfo.getVolumeMax();
			}
		}

		public int getVolume() {
			synchronized (mControllerLock) {
				return mSessionInfo.getVolume();
			}
		}

		public boolean isReleased() {
			synchronized (mControllerLock) {
				return mReleased;
			}
		}

		public void selectRoute(@NonNull MediaRoute2Info route) {
			Objects.requireNonNull(route, "route must not be null");
			if (isReleased()) {
				Log.w(TAG, "selectRoute: Called on released controller. Ignoring.");
				return;
			}
			if (containsRouteInfoWithId(getSelectedRoutes(), route.getId())) {
				Log.w(TAG, "Ignoring selecting a route that is already selected. route=" + route);
				return;
			}
			if (!containsRouteInfoWithId(getSelectableRoutes(), route.getId())) {
				Log.w(TAG, "Ignoring selecting a non-selectable route=" + route);
			}
		}

		public void deselectRoute(@NonNull MediaRoute2Info route) {
			Objects.requireNonNull(route, "route must not be null");
			if (isReleased()) {
				Log.w(TAG, "deselectRoute: called on released controller. Ignoring.");
				return;
			}
			if (!containsRouteInfoWithId(getSelectedRoutes(), route.getId())) {
				Log.w(TAG, "Ignoring deselecting a route that is not selected. route=" + route);
				return;
			}
			if (!containsRouteInfoWithId(getDeselectableRoutes(), route.getId())) {
				Log.w(TAG, "Ignoring deselecting a non-deselectable route=" + route);
			}
		}

		public void setVolume(int volume) {
			if (getVolumeHandling() == MediaRoute2Info.PLAYBACK_VOLUME_FIXED) {
				Log.w(TAG, "setVolume: The routing session has fixed volume. Ignoring.");
				return;
			}
			if (volume < 0 || volume > getVolumeMax()) {
				Log.w(TAG, "setVolume: The target volume is out of range. Ignoring");
				return;
			}
			if (isReleased()) {
				Log.w(TAG, "setVolume: Called on released controller. Ignoring.");
			}
		}

		public void release() {
			releaseInternal();
		}

		void releaseInternal() {
			synchronized (mControllerLock) {
				mReleased = true;
			}
		}

		@Override
		public String toString() {
			return "RoutingController{ id=" + getId() + ", selectedRoutes={" + idsOf(getSelectedRoutes()) + "}, selectableRoutes={"
			    + idsOf(getSelectableRoutes()) + "}, deselectableRoutes={" + idsOf(getDeselectableRoutes()) + "} }";
		}

		private List<String> idsOf(List<MediaRoute2Info> routes) {
			return routes.stream().map(MediaRoute2Info::getId).collect(Collectors.toList());
		}

		private boolean containsRouteInfoWithId(@NonNull List<MediaRoute2Info> routeList, @NonNull String routeId) {
			for (MediaRoute2Info info : routeList) {
				if (TextUtils.equals(routeId, info.getId())) {
					return true;
				}
			}
			return false;
		}
	}

	class SystemRoutingController extends RoutingController {
		SystemRoutingController(@NonNull RoutingSessionInfo sessionInfo) {
			super(sessionInfo);
		}

		@Override
		public boolean isReleased() {
			return false;
		}

		@Override
		void releaseInternal() {}
	}

	static final class RouteCallbackRecord {
		final Executor mExecutor;
		final RouteCallback mRouteCallback;
		final RouteDiscoveryPreference mPreference;

		RouteCallbackRecord(@Nullable Executor executor, @NonNull RouteCallback routeCallback, @Nullable RouteDiscoveryPreference preference) {
			mRouteCallback = routeCallback;
			mExecutor = executor;
			mPreference = preference;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof RouteCallbackRecord && mRouteCallback == ((RouteCallbackRecord) obj).mRouteCallback;
		}

		@Override
		public int hashCode() {
			return mRouteCallback.hashCode();
		}
	}

	private static final class ListingPreferenceCallbackRecord {
		final Executor mExecutor;
		final Consumer<RouteListingPreference> mCallback;

		ListingPreferenceCallbackRecord(Executor executor, Consumer<RouteListingPreference> callback) {
			mExecutor = executor;
			mCallback = callback;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ListingPreferenceCallbackRecord && mCallback == ((ListingPreferenceCallbackRecord) obj).mCallback;
		}

		@Override
		public int hashCode() {
			return mCallback.hashCode();
		}
	}

	private static final class SuggestionsCallbackRecord {
		final Executor mExecutor;
		final DeviceSuggestionsUpdatesCallback mCallback;

		SuggestionsCallbackRecord(Executor executor, DeviceSuggestionsUpdatesCallback callback) {
			mExecutor = executor;
			mCallback = callback;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof SuggestionsCallbackRecord && mCallback == ((SuggestionsCallbackRecord) obj).mCallback;
		}

		@Override
		public int hashCode() {
			return mCallback.hashCode();
		}
	}

	static final class TransferCallbackRecord {
		final Executor mExecutor;
		final TransferCallback mTransferCallback;

		TransferCallbackRecord(Executor executor, TransferCallback transferCallback) {
			mExecutor = executor;
			mTransferCallback = transferCallback;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TransferCallbackRecord && mTransferCallback == ((TransferCallbackRecord) obj).mTransferCallback;
		}

		@Override
		public int hashCode() {
			return mTransferCallback.hashCode();
		}
	}

	static final class ControllerCallbackRecord {
		final Executor mExecutor;
		final ControllerCallback mCallback;

		ControllerCallbackRecord(Executor executor, ControllerCallback callback) {
			mExecutor = executor;
			mCallback = callback;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ControllerCallbackRecord && mCallback == ((ControllerCallbackRecord) obj).mCallback;
		}

		@Override
		public int hashCode() {
			return mCallback.hashCode();
		}
	}
}
