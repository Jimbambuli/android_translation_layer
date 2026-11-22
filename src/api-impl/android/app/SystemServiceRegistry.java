/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.app;

import android.accounts.AccountManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.RestrictionsManager;
import android.content.pm.CrossProfileApps;
import android.content.pm.ShortcutManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.hardware.ConsumerIrManager;
import android.hardware.SensorManager;
import android.hardware.SensorPrivacyManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.FingerprintManager;
import com.android.server.biometrics.FprintdSensor;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.media.metrics.MediaMetricsManager;
import android.media.session.MediaSessionManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.DropBoxManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.storage.StorageManager;
import android.print.PrintManager;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyRegistryManager;
import android.telephony.euicc.EuiccManager;
import android.telephony.ims.ImsManager;
import android.telephony.satellite.SatelliteManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.autofill.AutofillManager;
import android.view.contentcapture.ContentCaptureManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassificationManager;
import android.view.textservice.TextServicesManager;
import com.android.internal.policy.PhoneLayoutInflater;
import java.util.Map;

/**
 * Manages all of the system services that can be returned by {@link Context#getSystemService}.
 * Used by {@link ContextImpl}.
 *
 * ATL: the registry, the fetchers and the caching are android-14's SystemServiceRegistry. What
 * differs is what the fetchers create: the managers here are in-process classes of this layer
 * (AOSP's wrap a Binder interface obtained from ServiceManager), so no fetcher has a service
 * that could be "not found", and the registered services are the ones this layer has.
 */
public final class SystemServiceRegistry {
    private static final String TAG = "SystemServiceRegistry";

    // Service registry information.
    // This information is never changed once static initialization has completed.
    private static final Map<Class<?>, String> SYSTEM_SERVICE_NAMES =
            new ArrayMap<Class<?>, String>();
    private static final Map<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS =
            new ArrayMap<String, ServiceFetcher<?>>();

    private static int sServiceCacheSize;

    // Not instantiable.
    private SystemServiceRegistry() { }

    static {
        registerService(Context.WINDOW_SERVICE, WindowManager.class,
                new CachedServiceFetcher<WindowManager>() {
            @Override
            public WindowManager createService(ContextImpl ctx) {
                return new WindowManagerImpl();
            }});

        registerService(Context.CLIPBOARD_SERVICE, ClipboardManager.class,
                new CachedServiceFetcher<ClipboardManager>() {
            @Override
            public ClipboardManager createService(ContextImpl ctx) {
                return new ClipboardManager(ctx, null);
            }});

        registerService(Context.TEXT_CLASSIFICATION_SERVICE, TextClassificationManager.class,
                new CachedServiceFetcher<TextClassificationManager>() {
            @Override
            public TextClassificationManager createService(ContextImpl ctx) {
                return new TextClassificationManager(ctx);
            }});

        registerService(Context.LOCALE_SERVICE, LocaleManager.class,
                new CachedServiceFetcher<LocaleManager>() {
            @Override
            public LocaleManager createService(ContextImpl ctx) {
                return new LocaleManager(ctx);
            }});

        registerService(Context.GRAMMATICAL_INFLECTION_SERVICE, GrammaticalInflectionManager.class,
                new CachedServiceFetcher<GrammaticalInflectionManager>() {
            @Override
            public GrammaticalInflectionManager createService(ContextImpl ctx) {
                return new GrammaticalInflectionManager(ctx);
            }});

        registerService(Context.SENSOR_SERVICE, SensorManager.class,
                new CachedServiceFetcher<SensorManager>() {
            @Override
            public SensorManager createService(ContextImpl ctx) {
                return new SensorManager();
            }});

        registerService(Context.SENSOR_PRIVACY_SERVICE, SensorPrivacyManager.class,
                new CachedServiceFetcher<SensorPrivacyManager>() {
            @Override
            public SensorPrivacyManager createService(ContextImpl ctx) {
                return SensorPrivacyManager.getInstance(ctx);
            }});

        registerService(Context.CONNECTIVITY_SERVICE, ConnectivityManager.class,
                new CachedServiceFetcher<ConnectivityManager>() {
            @Override
            public ConnectivityManager createService(ContextImpl ctx) {
                return new ConnectivityManager();
            }});

        registerService(Context.KEYGUARD_SERVICE, KeyguardManager.class,
                new CachedServiceFetcher<KeyguardManager>() {
            @Override
            public KeyguardManager createService(ContextImpl ctx) {
                return new KeyguardManager();
            }});

        registerService(Context.TELEPHONY_SERVICE, TelephonyManager.class,
                new CachedServiceFetcher<TelephonyManager>() {
            @Override
            public TelephonyManager createService(ContextImpl ctx) {
                return new TelephonyManager(ctx);
            }});

        registerService(Context.AUDIO_SERVICE, AudioManager.class,
                new CachedServiceFetcher<AudioManager>() {
            @Override
            public AudioManager createService(ContextImpl ctx) {
                return new AudioManager();
            }});

        registerService(Context.ACTIVITY_SERVICE, ActivityManager.class,
                new CachedServiceFetcher<ActivityManager>() {
            @Override
            public ActivityManager createService(ContextImpl ctx) {
                return new ActivityManager();
            }});

        registerService(Context.USB_SERVICE, UsbManager.class,
                new CachedServiceFetcher<UsbManager>() {
            @Override
            public UsbManager createService(ContextImpl ctx) {
                return new UsbManager();
            }});

        registerService(Context.VIBRATOR_SERVICE, Vibrator.class,
                new CachedServiceFetcher<Vibrator>() {
            @Override
            public Vibrator createService(ContextImpl ctx) {
                return new Vibrator();
            }});

        registerService(Context.VIBRATOR_MANAGER_SERVICE, VibratorManager.class,
                new CachedServiceFetcher<VibratorManager>() {
            @Override
            public VibratorManager createService(ContextImpl ctx) {
                return new VibratorManager(ctx);
            }});

        registerService(Context.POWER_SERVICE, PowerManager.class,
                new CachedServiceFetcher<PowerManager>() {
            @Override
            public PowerManager createService(ContextImpl ctx) {
                return new PowerManager();
            }});

        registerService(Context.DISPLAY_SERVICE, DisplayManager.class,
                new CachedServiceFetcher<DisplayManager>() {
            @Override
            public DisplayManager createService(ContextImpl ctx) {
                return new DisplayManager();
            }});

        registerService(Context.MEDIA_ROUTER_SERVICE, MediaRouter.class,
                new CachedServiceFetcher<MediaRouter>() {
            @Override
            public MediaRouter createService(ContextImpl ctx) {
                return new MediaRouter();
            }});

        registerService(Context.NOTIFICATION_SERVICE, NotificationManager.class,
                new CachedServiceFetcher<NotificationManager>() {
            @Override
            public NotificationManager createService(ContextImpl ctx) {
                return new NotificationManager();
            }});

        registerService(Context.ALARM_SERVICE, AlarmManager.class,
                new CachedServiceFetcher<AlarmManager>() {
            @Override
            public AlarmManager createService(ContextImpl ctx) {
                return new AlarmManager();
            }});

        registerService(Context.INPUT_SERVICE, InputManager.class,
                new CachedServiceFetcher<InputManager>() {
            @Override
            public InputManager createService(ContextImpl ctx) {
                return new InputManager(ctx);
            }});

        registerService(Context.LOCATION_SERVICE, LocationManager.class,
                new CachedServiceFetcher<LocationManager>() {
            @Override
            public LocationManager createService(ContextImpl ctx) {
                return new LocationManager(ctx);
            }});

        /* AOSP: RoleFrameworkInitializer of the Permission module */
        registerService(Context.ROLE_SERVICE, android.app.role.RoleManager.class,
                new CachedServiceFetcher<android.app.role.RoleManager>() {
            @Override
            public android.app.role.RoleManager createService(ContextImpl ctx) {
                return new android.app.role.RoleManager(ctx);
            }});

        registerService(Context.UI_MODE_SERVICE, UiModeManager.class,
                new CachedServiceFetcher<UiModeManager>() {
            @Override
            public UiModeManager createService(ContextImpl ctx) {
                return new UiModeManager();
            }});

        registerService(Context.INPUT_METHOD_SERVICE, InputMethodManager.class,
                new CachedServiceFetcher<InputMethodManager>() {
            @Override
            public InputMethodManager createService(ContextImpl ctx) {
                return new InputMethodManager();
            }});

        registerService(Context.ACCESSIBILITY_SERVICE, AccessibilityManager.class,
                new CachedServiceFetcher<AccessibilityManager>() {
            @Override
            public AccessibilityManager createService(ContextImpl ctx) {
                return AccessibilityManager.getInstance(ctx);
            }});

        registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class,
                new CachedServiceFetcher<LayoutInflater>() {
            @Override
            public LayoutInflater createService(ContextImpl ctx) {
                return new PhoneLayoutInflater(ctx);
            }});

        registerService(Context.WIFI_SERVICE, WifiManager.class,
                new CachedServiceFetcher<WifiManager>() {
            @Override
            public WifiManager createService(ContextImpl ctx) {
                // WifiFrameworkInitializer: no WifiManager on a device without Wi-Fi
                if (!ctx.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI)) {
                    return null;
                }
                return new WifiManager(ctx);
            }});

        registerService(Context.BLUETOOTH_SERVICE, BluetoothManager.class,
                new CachedServiceFetcher<BluetoothManager>() {
            @Override
            public BluetoothManager createService(ContextImpl ctx) {
                return new BluetoothManager();
            }});

        registerService(Context.JOB_SCHEDULER_SERVICE, JobScheduler.class,
                new CachedServiceFetcher<JobScheduler>() {
            @Override
            public JobScheduler createService(ContextImpl ctx) {
                return new JobScheduler(ctx);
            }});

        registerService(Context.APP_OPS_SERVICE, AppOpsManager.class,
                new CachedServiceFetcher<AppOpsManager>() {
            @Override
            public AppOpsManager createService(ContextImpl ctx) {
                return new AppOpsManager();
            }});

        registerService(Context.USER_SERVICE, UserManager.class,
                new CachedServiceFetcher<UserManager>() {
            @Override
            public UserManager createService(ContextImpl ctx) {
                return new UserManager();
            }});

        registerService(Context.LAUNCHER_APPS_SERVICE, android.content.pm.LauncherApps.class,
                new CachedServiceFetcher<android.content.pm.LauncherApps>() {
            @Override
            public android.content.pm.LauncherApps createService(ContextImpl ctx) {
                return new android.content.pm.LauncherApps(ctx);
            }});

        registerService(Context.CROSS_PROFILE_APPS_SERVICE, CrossProfileApps.class,
                new CachedServiceFetcher<CrossProfileApps>() {
            @Override
            public CrossProfileApps createService(ContextImpl ctx) {
                return new CrossProfileApps();
            }});

        registerService(Context.CAPTIONING_SERVICE, CaptioningManager.class,
                new CachedServiceFetcher<CaptioningManager>() {
            @Override
            public CaptioningManager createService(ContextImpl ctx) {
                return new CaptioningManager();
            }});

        registerService(Context.STATUS_BAR_SERVICE, StatusBarManager.class,
                new CachedServiceFetcher<StatusBarManager>() {
            @Override
            public StatusBarManager createService(ContextImpl ctx) {
                return new StatusBarManager(ctx);
            }});

        registerService(Context.CAMERA_SERVICE, CameraManager.class,
                new CachedServiceFetcher<CameraManager>() {
            @Override
            public CameraManager createService(ContextImpl ctx) {
                return new CameraManager();
            }});

        registerService(Context.COLOR_DISPLAY_SERVICE, ColorDisplayManager.class,
                new CachedServiceFetcher<ColorDisplayManager>() {
            @Override
            public ColorDisplayManager createService(ContextImpl ctx) {
                return new ColorDisplayManager();
            }});

        registerService(Context.SEARCH_SERVICE, SearchManager.class,
                new CachedServiceFetcher<SearchManager>() {
            @Override
            public SearchManager createService(ContextImpl ctx) {
                return new SearchManager();
            }});

        registerService(Context.STORAGE_SERVICE, StorageManager.class,
                new CachedServiceFetcher<StorageManager>() {
            @Override
            public StorageManager createService(ContextImpl ctx) {
                return new StorageManager();
            }});

        registerService(Context.BATTERY_SERVICE, BatteryManager.class,
                new CachedServiceFetcher<BatteryManager>() {
            @Override
            public BatteryManager createService(ContextImpl ctx) {
                return new BatteryManager();
            }});

        registerService(Context.DOWNLOAD_SERVICE, DownloadManager.class,
                new CachedServiceFetcher<DownloadManager>() {
            @Override
            public DownloadManager createService(ContextImpl ctx) {
                return new DownloadManager(ctx);
            }});

        registerService(Context.ACCOUNT_SERVICE, AccountManager.class,
                new CachedServiceFetcher<AccountManager>() {
            @Override
            public AccountManager createService(ContextImpl ctx) {
                return new AccountManager(ctx);
            }});

        registerService(Context.DEVICE_POLICY_SERVICE, DevicePolicyManager.class,
                new CachedServiceFetcher<DevicePolicyManager>() {
            @Override
            public DevicePolicyManager createService(ContextImpl ctx) {
                return new DevicePolicyManager();
            }});

        registerService(Context.DROPBOX_SERVICE, DropBoxManager.class,
                new CachedServiceFetcher<DropBoxManager>() {
            @Override
            public DropBoxManager createService(ContextImpl ctx) {
                return new DropBoxManager();
            }});

        registerService(Context.TEXT_SERVICES_MANAGER_SERVICE, TextServicesManager.class,
                new CachedServiceFetcher<TextServicesManager>() {
            @Override
            public TextServicesManager createService(ContextImpl ctx) {
                return new TextServicesManager();
            }});

        registerService(Context.TELECOM_SERVICE, TelecomManager.class,
                new CachedServiceFetcher<TelecomManager>() {
            @Override
            public TelecomManager createService(ContextImpl ctx) {
                return new TelecomManager();
            }});

        registerService(Context.WALLPAPER_SERVICE, WallpaperManager.class,
                new CachedServiceFetcher<WallpaperManager>() {
            @Override
            public WallpaperManager createService(ContextImpl ctx) {
                return new WallpaperManager();
            }});

        registerService(Context.RESTRICTIONS_SERVICE, RestrictionsManager.class,
                new CachedServiceFetcher<RestrictionsManager>() {
            @Override
            public RestrictionsManager createService(ContextImpl ctx) {
                return new RestrictionsManager();
            }});

        registerService(Context.PRINT_SERVICE, PrintManager.class,
                new CachedServiceFetcher<PrintManager>() {
            @Override
            public PrintManager createService(ContextImpl ctx) {
                return new PrintManager();
            }});

        registerService(Context.CONSUMER_IR_SERVICE, ConsumerIrManager.class,
                new CachedServiceFetcher<ConsumerIrManager>() {
            @Override
            public ConsumerIrManager createService(ContextImpl ctx) {
                return new ConsumerIrManager();
            }});

        registerService(Context.FINGERPRINT_SERVICE, FingerprintManager.class,
                new CachedServiceFetcher<FingerprintManager>() {
            @Override
            public FingerprintManager createService(ContextImpl ctx) {
                /* AOSP: ServiceManager.getServiceOrThrow() for apps targeting O and later; the service runs only
                 * on a device with FEATURE_FINGERPRINT (SystemServer), here the fprintd sensor */
                if (!FprintdSensor.isPresent()
                        && ctx.getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.O)
                    return null;
                return new FingerprintManager(ctx);
            }});

        /* AOSP: the AuthService runs on every device */
        registerService(Context.BIOMETRIC_SERVICE, BiometricManager.class,
                new CachedServiceFetcher<BiometricManager>() {
            @Override
            public BiometricManager createService(ContextImpl ctx) {
                return new BiometricManager();
            }});

        registerService(Context.TV_INPUT_SERVICE, TvInputManager.class,
                new CachedServiceFetcher<TvInputManager>() {
            @Override
            public TvInputManager createService(ContextImpl ctx) {
                return new TvInputManager();
            }});

        registerService(Context.USAGE_STATS_SERVICE, UsageStatsManager.class,
                new CachedServiceFetcher<UsageStatsManager>() {
            @Override
            public UsageStatsManager createService(ContextImpl ctx) {
                return new UsageStatsManager();
            }});

        registerService(Context.STORAGE_STATS_SERVICE, StorageStatsManager.class,
                new CachedServiceFetcher<StorageStatsManager>() {
            @Override
            public StorageStatsManager createService(ContextImpl ctx) {
                return new StorageStatsManager(ctx);
            }});

        registerService(Context.MEDIA_PROJECTION_SERVICE, MediaProjectionManager.class,
                new CachedServiceFetcher<MediaProjectionManager>() {
            @Override
            public MediaProjectionManager createService(ContextImpl ctx) {
                return new MediaProjectionManager();
            }});

        registerService(Context.APPWIDGET_SERVICE, AppWidgetManager.class,
                new CachedServiceFetcher<AppWidgetManager>() {
            @Override
            public AppWidgetManager createService(ContextImpl ctx) {
                return new AppWidgetManager();
            }});

        registerService(Context.SHORTCUT_SERVICE, ShortcutManager.class,
                new CachedServiceFetcher<ShortcutManager>() {
            @Override
            public ShortcutManager createService(ContextImpl ctx) {
                return new ShortcutManager();
            }});

        registerService(Context.DOMAIN_VERIFICATION_SERVICE, DomainVerificationManager.class,
                new CachedServiceFetcher<DomainVerificationManager>() {
            @Override
            public DomainVerificationManager createService(ContextImpl ctx) {
                return new DomainVerificationManager(ctx);
            }});

        registerService(Context.AUTOFILL_MANAGER_SERVICE, AutofillManager.class,
                new CachedServiceFetcher<AutofillManager>() {
            @Override
            public AutofillManager createService(ContextImpl ctx) {
                return new AutofillManager();
            }});

        registerService(Context.CONTENT_CAPTURE_MANAGER_SERVICE, ContentCaptureManager.class,
                new CachedServiceFetcher<ContentCaptureManager>() {
            @Override
            public ContentCaptureManager createService(ContextImpl ctx) {
                // AOSP: no manager when the content capture service is absent (ATL has none)
                return null;
            }});

        registerService(Context.TELEPHONY_SUBSCRIPTION_SERVICE, SubscriptionManager.class,
                new CachedServiceFetcher<SubscriptionManager>() {
            @Override
            public SubscriptionManager createService(ContextImpl ctx) {
                return new SubscriptionManager();
            }});

        registerService(Context.CARRIER_CONFIG_SERVICE, CarrierConfigManager.class,
                new CachedServiceFetcher<CarrierConfigManager>() {
            @Override
            public CarrierConfigManager createService(ContextImpl ctx) {
                return new CarrierConfigManager(ctx);
            }});

        registerService(Context.EUICC_SERVICE, EuiccManager.class,
                new CachedServiceFetcher<EuiccManager>() {
            @Override
            public EuiccManager createService(ContextImpl ctx) {
                return new EuiccManager(ctx);
            }});

        registerService(Context.TELEPHONY_IMS_SERVICE, ImsManager.class,
                new CachedServiceFetcher<ImsManager>() {
            @Override
            public ImsManager createService(ContextImpl ctx) {
                return new ImsManager(ctx);
            }});

        registerService(Context.SATELLITE_SERVICE, SatelliteManager.class,
                new CachedServiceFetcher<SatelliteManager>() {
            @Override
            public SatelliteManager createService(ContextImpl ctx) {
                return new SatelliteManager(ctx);
            }});

        registerService(Context.TELEPHONY_REGISTRY_SERVICE, TelephonyRegistryManager.class,
                new CachedServiceFetcher<TelephonyRegistryManager>() {
            @Override
            public TelephonyRegistryManager createService(ContextImpl ctx) {
                return new TelephonyRegistryManager(ctx);
            }});

        registerService(Context.NSD_SERVICE, NsdManager.class,
                new CachedServiceFetcher<NsdManager>() {
            @Override
            public NsdManager createService(ContextImpl ctx) {
                return new NsdManager();
            }});

        registerService(Context.WIFI_P2P_SERVICE, WifiP2pManager.class,
                new CachedServiceFetcher<WifiP2pManager>() {
            @Override
            public WifiP2pManager createService(ContextImpl ctx) {
                return new WifiP2pManager();
            }});

        registerService(Context.NFC_SERVICE, NfcManager.class,
                new CachedServiceFetcher<NfcManager>() {
            @Override
            public NfcManager createService(ContextImpl ctx) {
                return new NfcManager();
            }});

        registerService(Context.MEDIA_SESSION_SERVICE, MediaSessionManager.class,
                new CachedServiceFetcher<MediaSessionManager>() {
            @Override
            public MediaSessionManager createService(ContextImpl ctx) {
                return new MediaSessionManager();
            }});

        registerService(Context.MEDIA_METRICS_SERVICE, MediaMetricsManager.class,
                new CachedServiceFetcher<MediaMetricsManager>() {
            @Override
            public MediaMetricsManager createService(ContextImpl ctx) {
                return new MediaMetricsManager();
            }});
    }

    /** Returns an array of service objects, one per cached service fetcher. */
    public static Object[] createServiceCache() {
        return new Object[sServiceCacheSize];
    }

    /**
     * Gets a system service from a given context.
     * @hide
     */
    public static Object getSystemService(ContextImpl ctx, String name) {
        if (name == null) {
            return null;
        }
        final ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
        if (fetcher == null) {
            return null;
        }
        return fetcher.getService(ctx);
    }

    /**
     * Gets the name of the system-level service that is represented by the specified class.
     * @hide
     */
    public static String getSystemServiceName(Class<?> serviceClass) {
        if (serviceClass == null) {
            return null;
        }
        return SYSTEM_SERVICE_NAMES.get(serviceClass);
    }

    /**
     * Statically registers a system service with the context.
     * This method must be called during static initialization only.
     */
    private static <T> void registerService(String serviceName,
            Class<T> serviceClass, ServiceFetcher<T> serviceFetcher) {
        SYSTEM_SERVICE_NAMES.put(serviceClass, serviceName);
        SYSTEM_SERVICE_FETCHERS.put(serviceName, serviceFetcher);
    }

    /**
     * Base interface for classes that fetch services.
     * These objects must only be created during static initialization.
     */
    static abstract interface ServiceFetcher<T> {
        T getService(ContextImpl ctx);
    }

    /**
     * Override this class when the system service constructor needs a
     * ContextImpl and should be cached and retained by that context.
     */
    static abstract class CachedServiceFetcher<T> implements ServiceFetcher<T> {
        private final int mCacheIndex;

        CachedServiceFetcher() {
            // Note this class must be instantiated only by the static initializer of the
            // outer class (SystemServiceRegistry), which already does the synchronization,
            // so bare access to sServiceCacheSize is okay here.
            mCacheIndex = sServiceCacheSize++;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final T getService(ContextImpl ctx) {
            final Object[] cache = ctx.mServiceCache;
            final int[] gates = ctx.mServiceInitializationStateArray;
            boolean interrupted = false;

            T ret = null;

            for (;;) {
                boolean doInitialize = false;
                synchronized (cache) {
                    // Return it if we already have a cached instance.
                    T service = (T) cache[mCacheIndex];
                    if (service != null) {
                        ret = service;
                        break; // exit the for (;;)
                    }

                    // If we get here, there's no cached instance.

                    // Grr... if gate is STATE_READY, then this means we initialized the service
                    // once but someone cleared it.
                    // We start over from STATE_UNINITIALIZED.
                    // Similarly, if the previous attempt returned null, we'll retry again.
                    if (gates[mCacheIndex] == ContextImpl.STATE_READY
                            || gates[mCacheIndex] == ContextImpl.STATE_NOT_FOUND) {
                        gates[mCacheIndex] = ContextImpl.STATE_UNINITIALIZED;
                    }

                    // It's possible for multiple threads to get here at the same time, so
                    // use the "gate" to make sure only the first thread will call createService().

                    // At this point, the gate must be either UNINITIALIZED or INITIALIZING.
                    if (gates[mCacheIndex] == ContextImpl.STATE_UNINITIALIZED) {
                        doInitialize = true;
                        gates[mCacheIndex] = ContextImpl.STATE_INITIALIZING;
                    }
                }

                if (doInitialize) {
                    // Only the first thread gets here.

                    T service = null;
                    int newState = ContextImpl.STATE_NOT_FOUND;
                    try {
                        // This thread is the first one to get here. Instantiate the service
                        // *without* the cache lock held.
                        service = createService(ctx);
                        newState = ContextImpl.STATE_READY;

                    } finally {
                        synchronized (cache) {
                            cache[mCacheIndex] = service;
                            gates[mCacheIndex] = newState;
                            cache.notifyAll();
                        }
                    }
                    ret = service;
                    break; // exit the for (;;)
                }
                // The other threads will wait for the first thread to call notifyAll(),
                // and go back to the top and retry.
                synchronized (cache) {
                    // Repeat until the state becomes STATE_READY or STATE_NOT_FOUND.
                    // We can't respond to interrupts here; just like we can't in the "doInitialize"
                    // path, so we remember the interrupt state here and re-interrupt later.
                    while (gates[mCacheIndex] < ContextImpl.STATE_READY) {
                        try {
                            // Clear the interrupt state.
                            interrupted |= Thread.interrupted();
                            cache.wait();
                        } catch (InterruptedException e) {
                            // This shouldn't normally happen, but if someone interrupts the
                            // thread, it will.
                            Log.w(TAG, "getService() interrupted");
                            interrupted = true;
                        }
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return ret;
        }

        public abstract T createService(ContextImpl ctx);
    }
}
