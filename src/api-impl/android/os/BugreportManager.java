package android.os;

import android.content.Context;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;

/* AOSP BugreportManager (frameworks/base/core/java/android/os). It talks to the native dumpstate service, which ATL
 * does not have: the argument checks are AOSP's, a bug report cannot be taken, so startConnectivityBugreport()
 * reports BUGREPORT_ERROR_RUNTIME through the callback (as DumpstateListener.onError() would for a failed
 * dumpstate run) and cancelBugreport() has nothing to cancel. */
public final class BugreportManager {
	private final Context mContext;

	BugreportManager(Context context) {
		mContext = context;
	}

	/* AOSP BugreportManager.BugreportCallback */
	public abstract static class BugreportCallback {
		public static final int BUGREPORT_ERROR_INVALID_INPUT = 1;
		public static final int BUGREPORT_ERROR_RUNTIME = 2;
		public static final int BUGREPORT_ERROR_USER_DENIED_CONSENT = 3;
		public static final int BUGREPORT_ERROR_USER_CONSENT_TIMED_OUT = 4;
		public static final int BUGREPORT_ERROR_ANOTHER_REPORT_IN_PROGRESS = 5;
		public static final int BUGREPORT_ERROR_NO_BUGREPORT_TO_RETRIEVE = 6;

		public BugreportCallback() {}

		public void onProgress(float progress) {}

		public void onError(int errorCode) {}

		public void onFinished() {}

		public void onEarlyReportFinished() {}
	}

	public void startConnectivityBugreport(ParcelFileDescriptor bugreportFd, Executor executor,
	                                       BugreportCallback callback) {
		Objects.requireNonNull(bugreportFd);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		try {
			bugreportFd.close();
		} catch (IOException e) {
		}
		executor.execute(() -> callback.onError(BugreportCallback.BUGREPORT_ERROR_RUNTIME));
	}

	public void cancelBugreport() {}
}
