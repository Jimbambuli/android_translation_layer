package android.app.job;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;

public abstract class JobService extends Service {
	private static final String TAG = "JobService";

	public static final int JOB_END_NOTIFICATION_POLICY_DETACH = 0;
	public static final int JOB_END_NOTIFICATION_POLICY_REMOVE = 1;

	/* notification id and end policy per job, as set by setNotification() */
	private final java.util.HashMap<JobParameters, int[]> mNotifications = new java.util.HashMap<>();

	/* AOSP JobServiceEngine.setNotification(): null checks; the JobScheduler then shows the notification for the
	 * job and applies the end policy when the job stops. Here it is posted for the app and removed in
	 * jobFinished() for POLICY_REMOVE. */
	public final void setNotification(JobParameters params, int notificationId, Notification notification, int jobEndNotificationPolicy) {
		if (params == null) {
			throw new NullPointerException("params");
		}
		if (notification == null) {
			throw new NullPointerException("notification");
		}
		synchronized (mNotifications) {
			mNotifications.put(params, new int[] {notificationId, jobEndNotificationPolicy});
		}
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(notificationId, notification);
	}

	public abstract boolean onStartJob(JobParameters params);

	public abstract boolean onStopJob(JobParameters params);

	public void jobFinished(JobParameters params, boolean needsReschedule) {
		Slog.i(TAG, "jobFinished(" + params + ", " + needsReschedule + ") called");
		int[] notification;
		synchronized (mNotifications) {
			notification = mNotifications.remove(params);
		}
		if (notification != null && notification[1] == JOB_END_NOTIFICATION_POLICY_REMOVE) {
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notification[0]);
		}
		params.jobInfo.running = false;
		if (needsReschedule || params.jobInfo.periodicMillis != 0) {
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					params.jobInfo.running = true;
					boolean result = onStartJob(params);
					Slog.i(TAG, "onStartJob() returned " + result);
				}
			}, needsReschedule ? params.jobInfo.initialBackoffMillis : params.jobInfo.periodicMillis);
		} else {
			JobScheduler.pendingJobs(params.namespace).remove(params.jobInfo.getId());
		}
	}
}
