package android.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Slog;
import java.util.HashMap;

public class AlarmManager {
	private static final String TAG = "AlarmManager";

	public static final int RTC = 1;
	public static final int RTC_WAKEUP = 0;
	public static final int ELAPSED_REALTIME = 3;
	public static final int ELAPSED_REALTIME_WAKEUP = 2;

	/* the alarms set with a PendingIntent; AOSP's AlarmManagerService keeps one alarm per
	 * PendingIntent (PendingIntent.get*() gives the same object for the same arguments) */
	private static final Handler sHandler = new Handler(Looper.getMainLooper());
	private static final HashMap<PendingIntent, Runnable> sAlarms = new HashMap<>();
	/* the pending alarm clocks (setAlarmClock()) by PendingIntent, for getNextAlarmClock(); under sAlarms */
	private static final HashMap<PendingIntent, AlarmClockInfo> sClocks = new HashMap<>();

	/* AOSP: "If there is already an alarm scheduled for the same IntentSender, that previous alarm
	 * will first be canceled." */
	private static void schedule(PendingIntent operation, Runnable r, long delay) {
		synchronized (sAlarms) {
			sClocks.remove(operation);
			Runnable old = sAlarms.put(operation, r);
			if (old != null)
				sHandler.removeCallbacks(old);
		}
		sHandler.postDelayed(r, delay);
	}

	/* a one-shot alarm is gone once it went off */
	private static void delivered(PendingIntent operation, Runnable r) {
		synchronized (sAlarms) {
			if (sAlarms.get(operation) == r) {
				sAlarms.remove(operation);
				sClocks.remove(operation);
			}
		}
	}

	public void setRepeating(int type, long triggerTime, long interval, PendingIntent operation) {
		Slog.i(TAG, "setRepeating(" + type + ", " + triggerTime + ", " + interval + ", " + operation + ") called");
		setInexactRepeating(type, triggerTime, interval, operation);
	}

	/* AOSP AlarmManager.cancel(): a null PendingIntent throws (target SDK N and later), else the
	 * alarm of that PendingIntent is removed */
	public void cancel(PendingIntent operation) {
		Slog.i(TAG, "cancel(" + operation + ") called");
		if (operation == null)
			throw new NullPointerException("cancel() called with a null PendingIntent");
		synchronized (sAlarms) {
			sClocks.remove(operation);
			Runnable old = sAlarms.remove(operation);
			if (old != null)
				sHandler.removeCallbacks(old);
		}
	}

	/* AOSP AlarmManager.cancel(OnAlarmListener): null throws, an unknown listener is only logged */
	public void cancel(OnAlarmListener listener) {
		Slog.i(TAG, "cancel(" + listener + ") called");
		if (listener == null)
			throw new NullPointerException("cancel() called with a null OnAlarmListener");
		synchronized (sAlarms) {
			ListenerAlarm old = sListenerAlarms.remove(listener);
			if (old == null) {
				Slog.w(TAG, "Unrecognized alarm listener " + listener);
				return;
			}
			old.handler.removeCallbacks(old);
		}
	}

	/* a direct-callback alarm (AOSP ListenerWrapper): one-shot, onAlarm() runs on the given Handler or, when that is
	 * null, on the main looper; setting the same listener again replaces the earlier alarm */
	private static final HashMap<OnAlarmListener, ListenerAlarm> sListenerAlarms = new HashMap<>();

	private static final class ListenerAlarm implements Runnable {
		final OnAlarmListener listener;
		final Handler handler;
		final java.util.concurrent.Executor executor;

		ListenerAlarm(OnAlarmListener listener, Handler handler) {
			this(listener, handler, null);
		}

		ListenerAlarm(OnAlarmListener listener, Handler handler, java.util.concurrent.Executor executor) {
			this.listener = listener;
			this.handler = handler;
			this.executor = executor;
		}

		@Override
		public void run() {
			synchronized (sAlarms) {
				if (sListenerAlarms.get(listener) != this)
					return;
				sListenerAlarms.remove(listener);
			}
			if (executor != null)
				executor.execute(listener::onAlarm);
			else
				listener.onAlarm();
		}
	}

	private static void scheduleListener(int type, long triggerTime, OnAlarmListener listener, Handler targetHandler) {
		scheduleListener(type, triggerTime, listener, targetHandler, null);
	}

	private static void scheduleListener(int type, long triggerTime, OnAlarmListener listener, Handler targetHandler, java.util.concurrent.Executor executor) {
		if (listener == null) { // AlarmManagerService.setImpl() (services/, not in the local sources) logs and drops it
			Slog.w(TAG, "Alarms must either supply a PendingIntent or an AlarmReceiver");
			return;
		}
		long delay = triggerTime - ((type == 2 || type == 3) ? SystemClock.elapsedRealtime() : System.currentTimeMillis());
		ListenerAlarm alarm = new ListenerAlarm(listener, targetHandler != null ? targetHandler : sHandler, executor);
		synchronized (sAlarms) {
			ListenerAlarm old = sListenerAlarms.put(listener, alarm);
			if (old != null)
				old.handler.removeCallbacks(old);
		}
		alarm.handler.postDelayed(alarm, delay);
	}

	/* AOSP set(int, long, String, OnAlarmListener, Handler): tag only names the alarm in dumpsys */
	public void set(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) {
		Slog.i(TAG, "set(" + type + ", " + triggerAtMillis + ", " + tag + ", " + listener + ") called");
		scheduleListener(type, triggerAtMillis, listener, targetHandler);
	}

	/* AOSP setExact(int, long, String, OnAlarmListener, Handler) */
	public void setExact(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) {
		Slog.i(TAG, "setExact(" + type + ", " + triggerAtMillis + ", " + tag + ", " + listener + ") called");
		scheduleListener(type, triggerAtMillis, listener, targetHandler);
	}

	/* AOSP setWindow(int, long, long, String, OnAlarmListener, Handler): the window start is the earliest delivery,
	 * taken here as for the PendingIntent variant */
	public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, OnAlarmListener listener, Handler targetHandler) {
		Slog.i(TAG, "setWindow(" + type + ", " + windowStartMillis + ", " + windowLengthMillis + ", " + tag + ", " + listener + ") called");
		scheduleListener(type, windowStartMillis, listener, targetHandler);
	}

	/* AOSP setWindow(int, long, long, String, Executor, OnAlarmListener): like the Handler variant, onAlarm() runs
	 * on the executor (AOSP: a null executor throws, setImpl() wraps it in a ListenerWrapper) */
	public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, java.util.concurrent.Executor executor, OnAlarmListener listener) {
		if (executor == null)
			throw new NullPointerException("executor cannot be null");
		Slog.i(TAG, "setWindow(" + type + ", " + windowStartMillis + ", " + windowLengthMillis + ", " + tag + ", " + listener + ") called");
		scheduleListener(type, windowStartMillis, listener, null, executor);
	}

	/* AOSP AlarmManager.setTime(): AlarmManagerService.setTime() enforces android.permission.SET_TIME
	 * (signature|privileged), which an app does not hold */
	public void setTime(long millis) {
		throw new SecurityException("setTime: Neither user " + android.os.Process.myUid() + " nor current process has android.permission.SET_TIME.");
	}

	/* AOSP AlarmManager.setTimeZone(): empty is ignored, an unknown zone is rejected (target SDK M and later), then
	 * AlarmManagerService enforces android.permission.SET_TIME_ZONE (signature|privileged) */
	public void setTimeZone(String timeZone) {
		if (android.text.TextUtils.isEmpty(timeZone))
			return;
		if (!java.util.Arrays.asList(java.util.TimeZone.getAvailableIDs()).contains(timeZone))
			throw new IllegalArgumentException("Timezone: " + timeZone + " is not an Olson ID");
		throw new SecurityException("setTimeZone: Neither user " + android.os.Process.myUid() + " nor current process has android.permission.SET_TIME_ZONE.");
	}

	public void setInexactRepeating(int type, long triggerTime, long interval, PendingIntent operation) {
		Slog.i(TAG, "setInexactRepeating(" + type + ", " + triggerTime + ", " + interval + ", " + operation + ") called");
		long delay = triggerTime - ((type == 2 || type == 3) ? SystemClock.elapsedRealtime() : System.currentTimeMillis());
		Slog.i(TAG, "setInexactRepeating() delay: " + DateUtils.formatElapsedTime(delay) + " interval: " + DateUtils.formatElapsedTime(interval));
		schedule(operation, new Runnable() {
			@Override
			public void run() {
				Slog.i(TAG, "delivering repeating alarm: " + operation);
				operation.send();
				sHandler.postDelayed(this, interval);
			}
		}, delay);
	}

	public void setExact(int type, long triggerTime, PendingIntent operation) {
		Slog.i(TAG, "setExact(" + type + ", " + triggerTime + ", " + operation + ") called");
		long delay = triggerTime - ((type == 2 || type == 3) ? SystemClock.elapsedRealtime() : System.currentTimeMillis());
		Slog.i(TAG, "setExact() delay: " + DateUtils.formatElapsedTime(delay));
		schedule(operation, new Runnable() {
			@Override
			public void run() {
				delivered(operation, this);
				Slog.i(TAG, "delivering alarm: " + operation);
				operation.send();
			}
		}, delay);
	}

	public void set(int type, long triggerTime, PendingIntent operation) {
		setExact(type, triggerTime, operation);
	}

	public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
		setExact(type, triggerAtMillis, operation);
	}

	/* AOSP setAlarmClock(): an exact RTC_WAKEUP alarm at the trigger time that the system also reports as the
	 * user's next alarm (getNextAlarmClock()) */
	public void setAlarmClock(AlarmClockInfo info, PendingIntent operation) {
		setExact(RTC_WAKEUP, info.getTriggerTime(), operation);
		synchronized (sAlarms) {
			sClocks.put(operation, info);
		}
	}

	/* AOSP getNextAlarmClock(): the alarm clock that goes off next; the other apps' are not known to ATL */
	public AlarmClockInfo getNextAlarmClock() {
		synchronized (sAlarms) {
			AlarmClockInfo next = null;
			for (AlarmClockInfo info : sClocks.values())
				if (next == null || info.getTriggerTime() < next.getTriggerTime())
					next = info;
			return next;
		}
	}

	/* AOSP setWindow(): delivered somewhere in [windowStartMillis, windowStartMillis + windowLengthMillis];
	 * the start is the earliest delivery, taken here */
	public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
		setExact(type, windowStartMillis, operation);
	}

	public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
		setExact(type, triggerAtMillis, operation);
	}

	public boolean canScheduleExactAlarms() {
		return true;
	}

	public static class AlarmClockInfo implements Parcelable {
		private long mTriggerTime;
		private PendingIntent mPendingIntent;
		public AlarmClockInfo(long triggerTime, PendingIntent showIntent) {
			mTriggerTime = triggerTime;
			mPendingIntent = showIntent;
		}

		public long getTriggerTime() {
			return mTriggerTime;
		}

		public PendingIntent getShowIntent() {
			return mPendingIntent;
		}
	}

	public static interface OnAlarmListener {
		void onAlarm();
	}
}
