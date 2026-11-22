package android.content;

import android.atl.ATLLoadedApp;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Display;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.DatabaseErrorHandler;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.UserHandle;

public class ContextWrapper extends Context {
	protected Context baseContext;

	public ContextWrapper(Context baseContext) {
		this.baseContext = baseContext;
	}

	public Context getBaseContext() {
		return baseContext;
	}

	protected void attachBaseContext(Context baseContext) {
		if (this.baseContext != null) {
			throw new IllegalStateException("Base context already set");
		}
		this.baseContext = baseContext;
	}

	public void atl_attach_base_context(Context baseContext) {
		Objects.requireNonNull(baseContext, "baseContext must not be null");
		this.attachBaseContext(baseContext);
	}

	@Override
	public Resources.Theme getTheme() {
		return this.baseContext.getTheme();
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return this.baseContext.getApplicationInfo();
	}

	@Override
	public Object getSystemService(String name) {
		return this.baseContext.getSystemService(name);
	}

	@Override
	public String getSystemServiceName(Class<?> serviceClass) {
		return this.baseContext.getSystemServiceName(serviceClass);
	}

	@Override
	public Resources getResources() {
		return this.baseContext.getResources();
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.baseContext.getClassLoader();
	}

	@Override
	public ComponentName startService(Intent intent) {
		return this.baseContext.startService(intent);
	}

	@Override
	public boolean bindService(Intent intent, ServiceConnection serviceConnection, int flags) {
		return this.baseContext.bindService(intent, serviceConnection, flags);
	}

	/** @hide */
	@Override
	public boolean atl_bindService(Intent intent, ServiceConnection serviceConnection, int flags, String instanceName) {
		return this.baseContext.atl_bindService(intent, serviceConnection, flags, instanceName);
	}

	@Override
	public void startActivities(Intent[] intents) {
		baseContext.startActivities(intents);
	}

	@Override
	public void startActivities(Intent[] intents, Bundle options) {
		baseContext.startActivities(intents, options);
	}

	@Override
	public void startActivity(Intent intent) {
		this.baseContext.startActivity(intent);
	}

	@Override
	public void setTheme(int resId) {
		this.baseContext.setTheme(resId);
	}

	@Override
	public boolean isRestricted() {
		return this.baseContext.isRestricted();
	}

	@Override
	public boolean isUiContext() {
		return baseContext == null ? false : baseContext.isUiContext();
	}

	@Override
	public boolean stopService(Intent intent) {
		return this.baseContext.stopService(intent);
	}

	@Override
	public void unbindService(ServiceConnection serviceConnection) {
		this.baseContext.unbindService(serviceConnection);
	}

	@Override
	public Context createPackageContext(String packageName, int flags) {
		return this.baseContext.createPackageContext(packageName, flags);
	}

	@Override
	public Context createConfigurationContext(Configuration configuration) {
		return this.baseContext.createConfigurationContext(configuration);
	}

	@Override
	public Context createDisplayContext(Display display) {
		return this.baseContext.createDisplayContext(display);
	}

	@Override
	public Display getDisplay() {
		return this.baseContext.getDisplay();
	}

	@Override
	public Context createWindowContext(int type, Bundle options) {
		return this.baseContext.createWindowContext(type, options);
	}

	@Override
	public Context createWindowContext(Display display, int type, Bundle options) {
		return this.baseContext.createWindowContext(display, type, options);
	}

	@Override
	public boolean isCredentialProtectedStorage() {
		return this.baseContext.isCredentialProtectedStorage();
	}

	@Override
	public boolean isDeviceProtectedStorage() {
		return this.baseContext.isDeviceProtectedStorage();
	}

	@Override
	public Context createDeviceProtectedStorageContext() {
		return this.baseContext.createDeviceProtectedStorageContext();
	}

	@Override
	public Context createCredentialProtectedStorageContext() {
		return this.baseContext.createCredentialProtectedStorageContext();
	}

	@Override
	public int getThemeResId() {
		return this.baseContext.getThemeResId();
	}

	@Override
	public ATLLoadedApp get_atl_loaded_app() {
		return this.baseContext.get_atl_loaded_app();
	}

	@Override
	public int checkCallingOrSelfPermission(String permission) {
		return baseContext.checkCallingOrSelfPermission(permission);
	}

	@Override
	public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
		return baseContext.checkCallingOrSelfUriPermission(uri, modeFlags);
	}

	@Override
	public int checkCallingPermission(String permission) {
		return baseContext.checkCallingPermission(permission);
	}

	@Override
	public int checkCallingUriPermission(Uri uri, int modeFlags) {
		return baseContext.checkCallingUriPermission(uri, modeFlags);
	}

	@Override
	public int checkPermission(String permission, int pid, int uid) {
		return baseContext.checkPermission(permission, pid, uid);
	}

	@Override
	public int checkSelfPermission(String permission) {
		return baseContext.checkSelfPermission(permission);
	}

	@Override
	public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
		return baseContext.checkUriPermission(uri, pid, uid, modeFlags);
	}

	@Override
	public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
		return baseContext.checkUriPermission(uri, readPermission, writePermission,
		pid, uid, modeFlags);
	}

	@Override
	public void clearWallpaper() throws IOException {
		baseContext.clearWallpaper();
	}

	@Override
	public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
		return baseContext.createContextForSplit(splitName);
	}

	@Override
	public String[] databaseList() {
		return baseContext.databaseList();
	}

	@Override
	public boolean deleteDatabase(String name) {
		return baseContext.deleteDatabase(name);
	}

	@Override
	public boolean deleteFile(String name) {
		return baseContext.deleteFile(name);
	}

	@Override
	public boolean deleteSharedPreferences(String name) {
		return baseContext.deleteSharedPreferences(name);
	}

	@Override
	public void enforceCallingOrSelfPermission( String permission, String message) {
		baseContext.enforceCallingOrSelfPermission(permission, message);
	}

	@Override
	public void enforceCallingOrSelfUriPermission( Uri uri, int modeFlags, String message) {
		baseContext.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
	}

	@Override
	public void enforceCallingPermission(String permission, String message) {
		baseContext.enforceCallingPermission(permission, message);
	}

	@Override
	public void enforceCallingUriPermission( Uri uri, int modeFlags, String message) {
		baseContext.enforceCallingUriPermission(uri, modeFlags, message);
	}

	@Override
	public void enforcePermission( String permission, int pid, int uid, String message) {
		baseContext.enforcePermission(permission, pid, uid, message);
	}

	@Override
	public void enforceUriPermission( Uri uri, int pid, int uid, int modeFlags, String message) {
		baseContext.enforceUriPermission(uri, pid, uid, modeFlags, message);
	}

	@Override
	public void enforceUriPermission( Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
		baseContext.enforceUriPermission(
		uri, readPermission, writePermission, pid, uid, modeFlags,
		message);
	}

	@Override
	public String[] fileList() {
		return baseContext.fileList();
	}

	@Override
	public Context getApplicationContext() {
		return baseContext.getApplicationContext();
	}

	@Override
	public AssetManager getAssets() {
		return baseContext.getAssets();
	}

	@Override
	public File getCacheDir() {
		return baseContext.getCacheDir();
	}

	@Override
	public File getCodeCacheDir() {
		return baseContext.getCodeCacheDir();
	}

	@Override
	public ContentResolver getContentResolver() {
		return baseContext.getContentResolver();
	}

	@Override
	public File getDataDir() {
		return baseContext.getDataDir();
	}

	@Override
	public File getDatabasePath(String name) {
		return baseContext.getDatabasePath(name);
	}

	@Override
	public File getDir(String name, int mode) {
		return baseContext.getDir(name, mode);
	}

	@Override
	public File getExternalCacheDir() {
		return baseContext.getExternalCacheDir();
	}

	@Override
	public File[] getExternalCacheDirs() {
		return baseContext.getExternalCacheDirs();
	}

	@Override
	public File getExternalFilesDir(String type) {
		return baseContext.getExternalFilesDir(type);
	}

	@Override
	public File[] getExternalFilesDirs(String type) {
		return baseContext.getExternalFilesDirs(type);
	}

	@Override
	public File[] getExternalMediaDirs() {
		return baseContext.getExternalMediaDirs();
	}

	@Override
	public File getFileStreamPath(String name) {
		return baseContext.getFileStreamPath(name);
	}

	@Override
	public File getFilesDir() {
		return baseContext.getFilesDir();
	}

	@Override
	public Looper getMainLooper() {
		return baseContext.getMainLooper();
	}

	@Override
	public File getNoBackupFilesDir() {
		return baseContext.getNoBackupFilesDir();
	}

	@Override
	public File getObbDir() {
		return baseContext.getObbDir();
	}

	@Override
	public File[] getObbDirs() {
		return baseContext.getObbDirs();
	}

	@Override
	public String getPackageCodePath() {
		return baseContext.getPackageCodePath();
	}

	@Override
	public PackageManager getPackageManager() {
		return baseContext.getPackageManager();
	}

	@Override
	public String getPackageName() {
		return baseContext.getPackageName();
	}

	@Override
	public String getPackageResourcePath() {
		return baseContext.getPackageResourcePath();
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return baseContext.getSharedPreferences(name, mode);
	}

	@Override
	public Drawable getWallpaper() {
		return baseContext.getWallpaper();
	}

	@Override
	public int getWallpaperDesiredMinimumHeight() {
		return baseContext.getWallpaperDesiredMinimumHeight();
	}

	@Override
	public int getWallpaperDesiredMinimumWidth() {
		return baseContext.getWallpaperDesiredMinimumWidth();
	}

	@Override
	public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
		baseContext.grantUriPermission(toPackage, uri, modeFlags);
	}

	@Override
	public boolean moveDatabaseFrom(Context sourceContext, String name) {
		return baseContext.moveDatabaseFrom(sourceContext, name);
	}

	@Override
	public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
		return baseContext.moveSharedPreferencesFrom(sourceContext, name);
	}

	@Override
	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		return baseContext.openFileInput(name);
	}

	@Override
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		return baseContext.openFileOutput(name, mode);
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
		return baseContext.openOrCreateDatabase(name, mode, factory);
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory, DatabaseErrorHandler errorHandler) {
		return baseContext.openOrCreateDatabase(name, mode, factory, errorHandler);
	}

	@Override
	public Drawable peekWallpaper() {
		return baseContext.peekWallpaper();
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		return baseContext.registerReceiver(receiver, filter);
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
		return baseContext.registerReceiver(receiver, filter, flags);
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
		return baseContext.registerReceiver(receiver, filter, broadcastPermission,
		scheduler);
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
		return baseContext.registerReceiver(receiver, filter, broadcastPermission,
		scheduler, flags);
	}

	@Override
	public void removeStickyBroadcast(Intent intent) {
		baseContext.removeStickyBroadcast(intent);
	}

	@Override
	public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
		baseContext.removeStickyBroadcastAsUser(intent, user);
	}

	@Override
	public void revokeUriPermission(Uri uri, int modeFlags) {
		baseContext.revokeUriPermission(uri, modeFlags);
	}

	@Override
	public void revokeUriPermission(String targetPackage, Uri uri, int modeFlags) {
		baseContext.revokeUriPermission(targetPackage, uri, modeFlags);
	}

	@Override
	public void sendBroadcast(Intent intent) {
		baseContext.sendBroadcast(intent);
	}

	@Override
	public void sendBroadcast(Intent intent, String receiverPermission) {
		baseContext.sendBroadcast(intent, receiverPermission);
	}

	@Override
	public void sendBroadcastAsUser(Intent intent, UserHandle user) {
		baseContext.sendBroadcastAsUser(intent, user);
	}

	@Override
	public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
		baseContext.sendBroadcastAsUser(intent, user, receiverPermission);
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, int initialCode, String receiverPermission, String receiverAppOp, BroadcastReceiver resultReceiver, Handler scheduler, String initialData, Bundle initialExtras, Bundle options) {
		baseContext.sendOrderedBroadcast(intent, initialCode, receiverPermission, receiverAppOp,
		resultReceiver, scheduler, initialData, initialExtras, options);
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
		baseContext.sendOrderedBroadcast(intent, receiverPermission);
	}

	@Override
	public void sendOrderedBroadcast( Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		baseContext.sendOrderedBroadcast(intent, receiverPermission,
		resultReceiver, scheduler, initialCode,
		initialData, initialExtras);
	}

	@Override
	public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		baseContext.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver,
		scheduler, initialCode, initialData, initialExtras);
	}

	@Override
	public void sendStickyBroadcast(Intent intent) {
		baseContext.sendStickyBroadcast(intent);
	}

	@Override
	public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
		baseContext.sendStickyBroadcastAsUser(intent, user);
	}

	@Override
	public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		baseContext.sendStickyOrderedBroadcast(intent,
		resultReceiver, scheduler, initialCode,
		initialData, initialExtras);
	}

	@Override
	public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		baseContext.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver,
		scheduler, initialCode, initialData, initialExtras);
	}

	@Override
	public void setWallpaper(Bitmap bitmap) throws IOException {
		baseContext.setWallpaper(bitmap);
	}

	@Override
	public void setWallpaper(InputStream data) throws IOException {
		baseContext.setWallpaper(data);
	}

	@Override
	public void startActivity(Intent intent, Bundle options) {
		baseContext.startActivity(intent, options);
	}

	@Override
	public ComponentName startForegroundService(Intent service) {
		return baseContext.startForegroundService(service);
	}

	@Override
	public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
		return baseContext.startInstrumentation(className, profileFile, arguments);
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
		baseContext.startIntentSender(intent, fillInIntent, flagsMask,
		flagsValues, extraFlags);
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
		baseContext.startIntentSender(intent, fillInIntent, flagsMask,
		flagsValues, extraFlags, options);
	}

	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		baseContext.unregisterReceiver(receiver);
	}
}
