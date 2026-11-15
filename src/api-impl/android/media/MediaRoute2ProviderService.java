package android.media;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Intent;
import android.app.Service;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* AOSP media/java/android/media/MediaRoute2ProviderService.java (public API). Binding the service to the
 * MediaRouterService is done with an AIDL stub (IMediaRoute2ProviderService) that ATL does not have, and
 * ATL has no MediaRouterService that would bind it: onBind() therefore hands out no binder, so AOSP's
 * mRemoteCallback stays null. All methods follow AOSP's path for that case (request ids are only ever
 * issued by the stub, so every request id other than REQUEST_ID_NONE is unknown). */
public abstract class MediaRoute2ProviderService extends Service {
	private static final String TAG = "MR2ProviderService";

	public static final String SERVICE_INTERFACE = "android.media.MediaRoute2ProviderService";
	public static final String CATEGORY_SYSTEM_MEDIA = "android.media.MediaRoute2ProviderService.SYSTEM_MEDIA";
	public static final long REQUEST_ID_NONE = 0;
	public static final int REASON_UNKNOWN_ERROR = 0;
	public static final int REASON_REJECTED = 1;
	public static final int REASON_NETWORK_ERROR = 2;
	public static final int REASON_ROUTE_NOT_AVAILABLE = 3;
	public static final int REASON_INVALID_COMMAND = 4;
	public static final int REASON_UNIMPLEMENTED = 5;
	public static final int REASON_FAILED_TO_REROUTE_SYSTEM_MEDIA = 6;

	private final Object mSessionLock = new Object();
	private final Map<String, RoutingSessionInfo> mSessionInfos = new LinkedHashMap<>();
	private volatile Map<String, MediaRoute2Info> mRoutes;

	public MediaRoute2ProviderService() {}

	@Override
	@Nullable
	public IBinder onBind(@NonNull Intent intent) {
		return null;
	}

	public abstract void onSetRouteVolume(long requestId, @NonNull String routeId, int volume);

	public abstract void onSetSessionVolume(long requestId, @NonNull String sessionId, int volume);

	@Nullable
	public final RoutingSessionInfo getSessionInfo(@NonNull String sessionId) {
		if (TextUtils.isEmpty(sessionId)) {
			throw new IllegalArgumentException("sessionId must not be empty");
		}
		synchronized (mSessionLock) {
			return mSessionInfos.get(sessionId);
		}
	}

	@NonNull
	public final List<RoutingSessionInfo> getAllSessionInfo() {
		synchronized (mSessionLock) {
			return new ArrayList<>(mSessionInfos.values());
		}
	}

	public final void notifySessionCreated(long requestId, @NonNull RoutingSessionInfo sessionInfo) {
		requireNonNull(sessionInfo, "sessionInfo must not be null");
		if (requestId != REQUEST_ID_NONE) {
			Log.w(TAG, "notifySessionCreated: The requestId doesn't exist. requestId=" + requestId);
			return;
		}
		String sessionId = sessionInfo.getId();
		synchronized (mSessionLock) {
			if (mSessionInfos.containsKey(sessionId)) {
				Log.w(TAG, "notifySessionCreated: Ignoring duplicate session id.");
				return;
			}
			mSessionInfos.put(sessionInfo.getId(), sessionInfo);
		}
	}

	/* AOSP: the request id comes from mSystemRoutingSessionCreationRequests, filled by the stub only */
	@Nullable
	public final MediaStreams notifySystemRoutingSessionCreated(long requestId, @NonNull RoutingSessionInfo sessionInfo,
	                                                            @NonNull MediaStreamsFormats formats) {
		requireNonNull(sessionInfo, "sessionInfo must not be null");
		requireNonNull(formats, "formats must not be null");
		throw new IllegalStateException("Unexpected system routing session created (request id=" + requestId + "):" + sessionInfo);
	}

	public final void notifySessionUpdated(@NonNull RoutingSessionInfo sessionInfo) {
		requireNonNull(sessionInfo, "sessionInfo must not be null");
		String sessionId = sessionInfo.getId();
		synchronized (mSessionLock) {
			if (mSessionInfos.containsKey(sessionId)) {
				mSessionInfos.put(sessionId, sessionInfo);
			} else {
				Log.w(TAG, "notifySessionUpdated: Ignoring unknown session info.");
			}
		}
	}

	public final void notifySessionReleased(@NonNull String sessionId) {
		if (TextUtils.isEmpty(sessionId)) {
			throw new IllegalArgumentException("sessionId must not be empty");
		}
		RoutingSessionInfo sessionInfo;
		synchronized (mSessionLock) {
			sessionInfo = mSessionInfos.remove(sessionId);
		}
		if (sessionInfo == null) {
			Log.w(TAG, "notifySessionReleased: Ignoring unknown session info.");
		}
	}

	/* AOSP: returns right away while mRemoteCallback is null */
	public final void notifyRequestFailed(long requestId, int reason) {}

	public abstract void onCreateSession(long requestId, @NonNull String packageName, @NonNull String routeId, @Nullable Bundle sessionHints);

	public void onCreateSystemRoutingSession(long requestId, @NonNull String routeId, @NonNull SystemRoutingSessionParams parameters) {
		notifyRequestFailed(requestId, REASON_UNIMPLEMENTED);
	}

	public abstract void onReleaseSession(long requestId, @NonNull String sessionId);

	public abstract void onSelectRoute(long requestId, @NonNull String sessionId, @NonNull String routeId);

	public abstract void onDeselectRoute(long requestId, @NonNull String sessionId, @NonNull String routeId);

	public abstract void onTransferToRoute(long requestId, @NonNull String sessionId, @NonNull String routeId);

	public void onDiscoveryPreferenceChanged(@NonNull RouteDiscoveryPreference preference) {}

	public void onDiscoveryPreferenceChanged(@NonNull RouteDiscoveryPreference compositePreference,
	                                         @NonNull Map<String, RouteDiscoveryPreference> perAppPreferences) {
		onDiscoveryPreferenceChanged(compositePreference);
	}

	public final void notifyRoutes(@NonNull Collection<MediaRoute2Info> routes) {
		requireNonNull(routes, "routes must not be null");
		Map<String, MediaRoute2Info> sanitized = new LinkedHashMap<>();
		for (MediaRoute2Info route : routes) {
			int types = route.getSupportedRoutingTypes();
			if ((types & MediaRoute2Info.FLAG_ROUTING_TYPE_REMOTE) != 0
			    && (types & (MediaRoute2Info.FLAG_ROUTING_TYPE_SYSTEM_AUDIO | MediaRoute2Info.FLAG_ROUTING_TYPE_SYSTEM_VIDEO)) != 0
			    && route.getDeduplicationIds().isEmpty()) {
				throw new IllegalArgumentException(String.format(
				    "Route with id='%s' name='%s' supports both system media and remote type routing, but doesn't contain a "
				        + "deduplication id, which it needs. You can add the route id as a deduplication id.",
				    route.getOriginalId(), route.getName()));
			}
			if (isSystemRouteType(route.getType())) {
				Log.w(TAG, "Attempting to add a system route type from a non-system route provider. Overriding type to "
				               + "TYPE_UNKNOWN. Route: " + route);
				route = new MediaRoute2Info.Builder(route).setType(MediaRoute2Info.TYPE_UNKNOWN).build();
			}
			sanitized.put(route.getId(), route);
		}
		mRoutes = sanitized;
	}

	/* AOSP MediaRoute2Info.isSystemRouteType() */
	private static boolean isSystemRouteType(int type) {
		switch (type) {
		case MediaRoute2Info.TYPE_BUILTIN_SPEAKER:
		case AudioDeviceInfo.TYPE_AUX_LINE:
		case MediaRoute2Info.TYPE_BLUETOOTH_A2DP:
		case MediaRoute2Info.TYPE_DOCK:
		case MediaRoute2Info.TYPE_BLE_HEADSET:
		case MediaRoute2Info.TYPE_HEARING_AID:
		case MediaRoute2Info.TYPE_HDMI:
		case AudioDeviceInfo.TYPE_HDMI_ARC:
		case AudioDeviceInfo.TYPE_HDMI_EARC:
		case AudioDeviceInfo.TYPE_LINE_DIGITAL:
		case AudioDeviceInfo.TYPE_LINE_ANALOG:
		case MediaRoute2Info.TYPE_USB_ACCESSORY:
		case MediaRoute2Info.TYPE_USB_DEVICE:
		case MediaRoute2Info.TYPE_USB_HEADSET:
		case MediaRoute2Info.TYPE_WIRED_HEADPHONES:
		case MediaRoute2Info.TYPE_WIRED_HEADSET:
			return true;
		default:
			return false;
		}
	}

	public static final class MediaStreams {
		private final AudioRecord mAudioRecord;

		private MediaStreams(AudioRecord audioRecord) {
			mAudioRecord = audioRecord;
		}

		@Nullable
		public AudioRecord getAudioRecord() {
			return mAudioRecord;
		}
	}

	public static final class SystemRoutingSessionParams {
		private final String mPackageName;
		private final Bundle mExtras;

		private SystemRoutingSessionParams(Builder builder) {
			mPackageName = builder.mPackageName;
			mExtras = builder.mExtras;
		}

		@NonNull
		public String getPackageName() {
			return mPackageName;
		}

		@NonNull
		public Bundle getExtras() {
			return mExtras;
		}

		public static final class Builder {
			private String mPackageName;
			private Bundle mExtras;

			public Builder() {
				mPackageName = "";
				mExtras = Bundle.EMPTY;
			}

			@NonNull
			public Builder setExtras(@NonNull Bundle extras) {
				mExtras = java.util.Objects.requireNonNull(extras);
				return this;
			}

			@NonNull
			public Builder setPackageName(@NonNull String packageName) {
				mPackageName = java.util.Objects.requireNonNull(packageName);
				return this;
			}

			@NonNull
			public SystemRoutingSessionParams build() {
				return new SystemRoutingSessionParams(this);
			}
		}
	}

	public static final class MediaStreamsFormats {
		private final AudioFormat mAudioFormat;

		private MediaStreamsFormats(Builder builder) {
			mAudioFormat = builder.mAudioFormat;
		}

		@Nullable
		public AudioFormat getAudioFormat() {
			return mAudioFormat;
		}

		public static final class Builder {
			private AudioFormat mAudioFormat;

			@NonNull
			public Builder setAudioFormat(@NonNull AudioFormat audioFormat) {
				mAudioFormat = requireNonNull(audioFormat);
				return this;
			}

			@NonNull
			public MediaStreamsFormats build() {
				return new MediaStreamsFormats(this);
			}
		}
	}
}
