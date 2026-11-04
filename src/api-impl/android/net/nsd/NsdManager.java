/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.net.nsd;

import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * The public API of AOSP 14 NsdManager (packages/modules/Connectivity). ATL has no mDNS service yet:
 * every request is answered through its listener with FAILURE_INTERNAL_ERROR, as on a device whose
 * mdnsd is not available.
 */
public final class NsdManager {
	public static final String ACTION_NSD_STATE_CHANGED = "android.net.nsd.STATE_CHANGED";
	public static final String EXTRA_NSD_STATE = "nsd_state";
	public static final int NSD_STATE_DISABLED = 1;
	public static final int NSD_STATE_ENABLED = 2;
	public static final int PROTOCOL_DNS_SD = 0x0001;

	public static final int FAILURE_INTERNAL_ERROR = 0;
	public static final int FAILURE_ALREADY_ACTIVE = 3;
	public static final int FAILURE_MAX_LIMIT = 4;
	public static final int FAILURE_OPERATION_NOT_RUNNING = 5;
	public static final int FAILURE_BAD_PARAMETERS = 6;

	public interface DiscoveryListener {
		public void onStartDiscoveryFailed(String serviceType, int errorCode);
		public void onStopDiscoveryFailed(String serviceType, int errorCode);
		public void onDiscoveryStarted(String serviceType);
		public void onDiscoveryStopped(String serviceType);
		public void onServiceFound(NsdServiceInfo serviceInfo);
		public void onServiceLost(NsdServiceInfo serviceInfo);
	}

	public interface RegistrationListener {
		public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode);
		public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode);
		public void onServiceRegistered(NsdServiceInfo serviceInfo);
		public void onServiceUnregistered(NsdServiceInfo serviceInfo);
	}

	public interface ResolveListener {
		void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);
		void onServiceResolved(NsdServiceInfo serviceInfo);
		default void onResolutionStopped(NsdServiceInfo serviceInfo) {}
		default void onStopResolutionFailed(NsdServiceInfo serviceInfo, int errorCode) {}
	}

	public interface ServiceInfoCallback {
		void onServiceInfoCallbackRegistrationFailed(int errorCode);
		void onServiceUpdated(NsdServiceInfo serviceInfo);
		void onServiceLost();
		void onServiceInfoCallbackUnregistered();
	}

	/* AOSP delivers the answers of NsdService from its ServiceHandler on the main looper */
	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private void answer(Executor executor, Runnable r) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				executor.execute(r);
			}
		});
	}

	private static final Executor DIRECT = new Executor() {
		@Override
		public void execute(Runnable r) {
			r.run();
		}
	};

	private static void checkProtocol(int protocolType) {
		if (protocolType != PROTOCOL_DNS_SD)
			throw new IllegalArgumentException("Unsupported protocol");
	}

	public void registerService(NsdServiceInfo serviceInfo, int protocolType, RegistrationListener listener) {
		registerService(serviceInfo, protocolType, DIRECT, listener);
	}

	public void registerService(final NsdServiceInfo serviceInfo, int protocolType, Executor executor, final RegistrationListener listener) {
		Objects.requireNonNull(serviceInfo, "NsdServiceInfo cannot be null");
		Objects.requireNonNull(listener, "listener cannot be null");
		checkProtocol(protocolType);
		answer(executor, new Runnable() {
			@Override
			public void run() {
				listener.onRegistrationFailed(serviceInfo, FAILURE_INTERNAL_ERROR);
			}
		});
	}

	/* nothing was registered: like AOSP for an unknown listener */
	public void unregisterService(RegistrationListener listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		throw new IllegalArgumentException("listener not registered");
	}

	public void discoverServices(String serviceType, int protocolType, DiscoveryListener listener) {
		discoverServices(serviceType, protocolType, (Network)null, DIRECT, listener);
	}

	public void discoverServices(final String serviceType, int protocolType, Network network, Executor executor, final DiscoveryListener listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		checkProtocol(protocolType);
		answer(executor, new Runnable() {
			@Override
			public void run() {
				listener.onStartDiscoveryFailed(serviceType, FAILURE_INTERNAL_ERROR);
			}
		});
	}

	public void stopServiceDiscovery(DiscoveryListener listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		throw new IllegalArgumentException("listener not registered");
	}

	public void resolveService(NsdServiceInfo serviceInfo, ResolveListener listener) {
		resolveService(serviceInfo, DIRECT, listener);
	}

	public void resolveService(final NsdServiceInfo serviceInfo, Executor executor, final ResolveListener listener) {
		Objects.requireNonNull(serviceInfo, "NsdServiceInfo cannot be null");
		Objects.requireNonNull(listener, "listener cannot be null");
		answer(executor, new Runnable() {
			@Override
			public void run() {
				listener.onResolveFailed(serviceInfo, FAILURE_INTERNAL_ERROR);
			}
		});
	}

	public void stopServiceResolution(ResolveListener listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		throw new IllegalArgumentException("listener not registered");
	}

	public void registerServiceInfoCallback(NsdServiceInfo serviceInfo, Executor executor, final ServiceInfoCallback listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		answer(executor, new Runnable() {
			@Override
			public void run() {
				listener.onServiceInfoCallbackRegistrationFailed(FAILURE_INTERNAL_ERROR);
			}
		});
	}

	public void unregisterServiceInfoCallback(ServiceInfoCallback listener) {
		Objects.requireNonNull(listener, "listener cannot be null");
		throw new IllegalArgumentException("listener not registered");
	}
}
