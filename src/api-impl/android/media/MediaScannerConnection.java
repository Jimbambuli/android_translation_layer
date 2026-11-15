package android.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/* AOSP media/java/android/media/MediaScannerConnection.java (API 30+ form, which talks to the
 * MediaStore provider instead of the scanner service). ATL has no MediaStore.scanFile(), so the scan
 * itself fails the way AOSP's scanFileQuietly() handles any exception: it logs and reports a null Uri.
 * Callbacks run on one background thread as with AOSP's BackgroundThread executor, so apps waiting for
 * onScanCompleted() / onMediaScannerConnected() are not left waiting forever. */
public class MediaScannerConnection implements ServiceConnection {
	private static final String TAG = "MediaScannerConnection";

	private static Executor sBackgroundExecutor;

	private final Context mContext;
	private final MediaScannerConnectionClient mClient;
	private boolean mConnected;

	public interface OnScanCompletedListener {
		public void onScanCompleted(String path, Uri uri);
	}

	public interface MediaScannerConnectionClient extends OnScanCompletedListener {
		public void onMediaScannerConnected();
	}

	public MediaScannerConnection(Context context, MediaScannerConnectionClient client) {
		mContext = context;
		mClient = client;
	}

	/* AOSP acquires the MediaStore provider client here; there is none to hold, the state is the same */
	public void connect() {
		synchronized (this) {
			if (!mConnected) {
				mConnected = true;
				if (mClient != null) {
					mClient.onMediaScannerConnected();
				}
			}
		}
	}

	public void disconnect() {
		synchronized (this) {
			mConnected = false;
		}
	}

	public synchronized boolean isConnected() {
		return mConnected;
	}

	public void scanFile(String path, String mimeType) {
		synchronized (this) {
			if (!mConnected) {
				throw new IllegalStateException("not connected to MediaScannerService");
			}
			getExecutor().execute(() -> {
				final Uri uri = scanFileQuietly(new File(path));
				runCallBack(mContext, mClient, path, uri);
			});
		}
	}

	public static void scanFile(Context context, String[] paths, String[] mimeTypes, OnScanCompletedListener callback) {
		getExecutor().execute(() -> {
			for (String path : paths) {
				final Uri uri = scanFileQuietly(new File(path));
				runCallBack(context, callback, path, uri);
			}
		});
	}

	private static synchronized Executor getExecutor() {
		if (sBackgroundExecutor == null) {
			sBackgroundExecutor = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "android.bg");
				t.setDaemon(true);
				return t;
			});
		}
		return sBackgroundExecutor;
	}

	private static Uri scanFileQuietly(File file) {
		Log.w(TAG, "Failed to scan " + file + ": no media provider");
		return null;
	}

	private static void runCallBack(Context context, OnScanCompletedListener callback, String path, Uri uri) {
		if (callback != null) {
			// Ignore exceptions from callback to avoid calling app from crashing.
			// Don't ignore exceptions for apps targeting 'R' or higher.
			try {
				callback.onScanCompleted(path, uri);
			} catch (Throwable e) {
				if (context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.R) {
					throw e;
				} else {
					Log.w(TAG, "Ignoring exception from callback for backward compatibility", e);
				}
			}
		}
	}

	/* Part of the ServiceConnection interface. Do not call. */
	public void onServiceConnected(ComponentName className, IBinder service) {
	}

	/* Part of the ServiceConnection interface. Do not call. */
	public void onServiceDisconnected(ComponentName className) {
	}
}
