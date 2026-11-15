package android.content.pm;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP android.content.pm.LauncherApps (android-16.0.0_r4). AOSP forwards to LauncherAppsService in system_server;
 * ATL answers from its own PackageManager for its one user (Process.myUserHandle()): the profiles are that user, other
 * users have no activities and are not enabled. The shortcut host API needs the default-launcher role (the shortcut
 * service); ATL's app is never the launcher, so hasShortcutHostPermission() is false. */
public class LauncherApps {
    public static final String ACTION_CONFIRM_PIN_SHORTCUT = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
    public static final String ACTION_CONFIRM_PIN_APPWIDGET = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
    public static final String EXTRA_PIN_ITEM_REQUEST = "android.content.pm.extra.PIN_ITEM_REQUEST";

    private final Context mContext;
    private final PackageManager mPm;
    private final List<Callback> mCallbacks = new CopyOnWriteArrayList<>();

    /** @hide */
    public LauncherApps(Context context) {
        mContext = context;
        mPm = context.getPackageManager();
    }

    private static boolean isOwnUser(UserHandle user) {
        return user != null && user.equals(Process.myUserHandle());
    }

    public List<UserHandle> getProfiles() {
        List<UserHandle> profiles = new ArrayList<>();
        profiles.add(Process.myUserHandle());
        return profiles;
    }

    /* AOSP getActivityList(): the launcher activities (ACTION_MAIN, CATEGORY_LAUNCHER) of the package, or of all
     * packages for null */
    public List<LauncherActivityInfo> getActivityList(String packageName, UserHandle user) {
        List<LauncherActivityInfo> result = new ArrayList<>();
        if (!isOwnUser(user))
            return result;
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        if (packageName != null)
            intent.setPackage(packageName);
        List<ResolveInfo> infos = mPm.queryIntentActivities(intent, 0);
        if (infos != null)
            for (ResolveInfo info : infos)
                if (info.activityInfo != null)
                    result.add(new LauncherActivityInfo(mContext, info.activityInfo, user));
        return result;
    }

    /* AOSP resolveActivity(): the activity the intent resolves to, only if it has a component */
    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle user) {
        if (!isOwnUser(user))
            return null;
        ResolveInfo info = mPm.resolveActivity(intent, 0);
        if (info == null || info.activityInfo == null)
            return null;
        return new LauncherActivityInfo(mContext, info.activityInfo, user);
    }

    public void startMainActivity(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        if (!isOwnUser(user))
            throw new SecurityException("Cannot access profile " + user);
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(component);
        launchIntent.setSourceBounds(sourceBounds);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(launchIntent, opts);
    }

    /* AOSP startAppDetailsActivity(): the system starts the app details of the Settings app */
    public void startAppDetailsActivity(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        if (!isOwnUser(user))
            throw new SecurityException("Cannot access profile " + user);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", component.getPackageName(), null));
        intent.setSourceBounds(sourceBounds);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent, opts);
        } catch (android.content.ActivityNotFoundException e) {
            /* started from the system process in AOSP: no handler, nothing happens in the caller */
        }
    }

    public boolean isPackageEnabled(String packageName, UserHandle user) {
        if (!isOwnUser(user))
            return false;
        try {
            return mPm.getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean isActivityEnabled(ComponentName component, UserHandle user) {
        if (!isOwnUser(user))
            return false;
        try {
            return mPm.getActivityInfo(component, 0).isEnabled();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public ApplicationInfo getApplicationInfo(@NonNull String packageName, int flags, @NonNull UserHandle user)
            throws PackageManager.NameNotFoundException {
        if (packageName == null || user == null)
            throw new NullPointerException();
        if (!isOwnUser(user))
            throw new PackageManager.NameNotFoundException("Package " + packageName + " not found for user " + user);
        return mPm.getApplicationInfo(packageName, flags);
    }

    /* AOSP: the suspended-package extras are set by the device admin/parental control; ATL suspends nothing */
    public Bundle getSuspendedPackageLauncherExtras(String packageName, UserHandle user) {
        return null;
    }

    public boolean shouldHideFromSuggestions(@NonNull String packageName, @NonNull UserHandle user) {
        return false;
    }

    public boolean hasShortcutHostPermission() {
        return false;
    }

    @NonNull
    public List<PackageInstaller.SessionInfo> getAllPackageInstallerSessions() {
        return Collections.emptyList();
    }

    @NonNull
    public List<String> getPreInstalledSystemPackages(@NonNull UserHandle user) {
        return Collections.emptyList();
    }

    /* AOSP: the market activity of the installer of the package; ATL has none installed it */
    @Nullable
    public IntentSender getAppMarketActivityIntent(@Nullable String packageName, @NonNull UserHandle user) {
        return null;
    }

    /* AOSP: null when the device has no private space settings activity */
    @Nullable
    public IntentSender getPrivateSpaceSettingsIntent() {
        return null;
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler) {
        if (callback != null && !mCallbacks.contains(callback))
            mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void registerPackageInstallerSessionCallback(@NonNull Executor executor, @NonNull PackageInstaller.SessionCallback callback) {
        if (executor == null)
            throw new NullPointerException("Executor must not be null");
    }

    public void unregisterPackageInstallerSessionCallback(@NonNull PackageInstaller.SessionCallback callback) {
    }

    /* AOSP LauncherApps.Callback */
    public static abstract class Callback {
        public abstract void onPackageRemoved(String packageName, UserHandle user);
        public abstract void onPackageAdded(String packageName, UserHandle user);
        public abstract void onPackageChanged(String packageName, UserHandle user);
        public abstract void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing);
        public abstract void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing);
        public void onPackagesSuspended(String[] packageNames, UserHandle user) {}
        @Deprecated
        public void onPackagesSuspended(String[] packageNames, UserHandle user, @Nullable android.os.Bundle launcherExtras) {
            onPackagesSuspended(packageNames, user);
        }
        public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {}
        public void onShortcutsChanged(@NonNull String packageName, @NonNull List<ShortcutInfo> shortcuts, @NonNull UserHandle user) {}
        public void onPackageLoadingProgressChanged(@NonNull String packageName, @NonNull UserHandle user, float progress) {}
    }
}
