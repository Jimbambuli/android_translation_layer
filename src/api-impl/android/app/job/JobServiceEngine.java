package android.app.job;

import android.app.Service;
import android.os.Binder;
import android.os.IBinder;

/* AOSP android.app.job.JobServiceEngine: the job side of a service that runs jobs without being a JobService
 * (androidx JobIntentService). AOSP's JobScheduler binds the service and calls the IJobService binder from
 * getBinder(); ATL's JobScheduler is in the process and calls the engine behind that binder directly. */
public abstract class JobServiceEngine {

	/* the binder that onBind() of the service returns */
	static final class EngineBinder extends Binder {
		final JobServiceEngine engine;

		EngineBinder(JobServiceEngine engine) {
			this.engine = engine;
		}
	}

	private final IBinder mBinder;

	public JobServiceEngine(Service service) {
		mBinder = new EngineBinder(this);
	}

	public final IBinder getBinder() {
		return mBinder;
	}

	public abstract boolean onStartJob(JobParameters params);

	public abstract boolean onStopJob(JobParameters params);

	/* AOSP JobServiceEngine.jobFinished() */
	public void jobFinished(JobParameters params, boolean wantsReschedule) {
		if (params == null) {
			throw new NullPointerException("params");
		}
		params.finish();
	}
}
