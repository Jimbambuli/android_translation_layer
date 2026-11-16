package android.app.job;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;
import java.util.*;

public class JobScheduler {
	private static final String TAG = "JobScheduler";

	/* pending jobs per namespace, the default namespace is null like in AOSP */
	private static Map<String, Map<Integer, JobInfo>> namespaces = new HashMap<>();
	private static IdentityHashMap<Class<? extends JobService>, JobService> runningServices = new IdentityHashMap<>();

	private final Context context;
	private final String namespace;
	private final Map<Integer, JobInfo> pendingJobs;

	/* AOSP: the abstract JobScheduler has a public constructor without arguments */
	public JobScheduler() {
		this(null, null);
	}

	public JobScheduler(Context context) {
		this(context, null);
	}

	private JobScheduler(Context context, String namespace) {
		this.context = context;
		this.namespace = namespace;
		this.pendingJobs = pendingJobs(namespace);
	}

	static Map<Integer, JobInfo> pendingJobs(String namespace) {
		Map<Integer, JobInfo> jobs = namespaces.get(namespace);
		if (jobs == null) {
			jobs = new HashMap<>();
			namespaces.put(namespace, jobs);
		}
		return jobs;
	}

	/**
	 * Get a JobScheduler instance that is dedicated to a specific namespace. Any API calls using
	 * this instance will interact with jobs in that namespace, unless the API documentation says
	 * otherwise. Attempting to update a job scheduled in another namespace will not be possible
	 * but will instead create or update the job inside the current namespace. A JobScheduler
	 * instance dedicated to a namespace must be used to schedule or update jobs in that namespace.
	 */
	public JobScheduler forNamespace(String namespace) {
		if (namespace == null) {
			throw new NullPointerException("namespace cannot be null");
		}
		if (namespace.trim().isEmpty()) {
			throw new IllegalArgumentException("namespace cannot be empty");
		}
		return new JobScheduler(context, namespace.trim().intern());
	}

	/**
	 * Get the namespace this JobScheduler instance is operating in. A {@code null} value means
	 * that the app has not specified a namespace for this instance, and it is therefore using the
	 * default namespace.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Retrieve all jobs that have been scheduled by the calling application.
	 *
	 * @return a list of all of the app's scheduled jobs.  This includes jobs that are
	 *     currently started as well as those that are still waiting to run.
	 */
	public List<JobInfo> getAllPendingJobs() {
		return new ArrayList<>(pendingJobs.values());
	};

	/* the services that run jobs through a JobServiceEngine (AOSP binds them; here they live on like a JobService) */
	private static Map<String, JobServiceEngine> engines = new HashMap<>();

	/* AOSP JobScheduler.enqueue(): the work goes to the job with this id that is running, else the job is started with
	 * it; the service takes it with JobParameters.dequeueWork() */
	public int enqueue(JobInfo job, JobWorkItem work) {
		if (work == null) {
			throw new NullPointerException("work is null");
		}
		JobInfo running = pendingJobs.get(job.getId());
		if (running != null && running.workParams != null && running.workParams.enqueueWork(work))
			return 1; //RESULT_SUCCESS
		JobParameters params = new JobParameters(job, namespace);
		params.enqueueWork(work);
		job.workParams = params;
		pendingJobs.put(job.getId(), job);
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					String className = job.getService().getClassName();
					Class<?> cls = context.get_atl_loaded_app().loadClass(className);
					job.running = true;
						boolean result;
					if (JobService.class.isAssignableFrom(cls)) {
						result = jobService(cls.asSubclass(JobService.class)).onStartJob(params);
					} else {
						result = engine(cls.asSubclass(android.app.Service.class), job).onStartJob(params);
					}
					Slog.i(TAG, "onStartJob() returned " + result);
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
		}, job.minLatencyMillis);
		return 1; //RESULT_SUCCESS
	}

	private JobService jobService(Class<? extends JobService> cls) throws ReflectiveOperationException {
		if (!runningServices.containsKey(cls)) {
			JobService service = cls.getConstructor().newInstance();
			ATLLoadedApp atlLoadedApp = context.get_atl_loaded_app();
			service.attachBaseContext(atlLoadedApp.createContext(null, null, atlLoadedApp.pkg.applicationInfo.theme));
			service.onCreate();
			runningServices.put(cls, service);
		}
		return runningServices.get(cls);
	}

	/* the engine of a service that is no JobService: created and bound like AOSP's JobServiceContext does */
	private JobServiceEngine engine(Class<? extends android.app.Service> cls, JobInfo job) throws ReflectiveOperationException {
		JobServiceEngine engine = engines.get(cls.getName());
		if (engine == null) {
			android.app.Service service = cls.getConstructor().newInstance();
			ATLLoadedApp atlLoadedApp = context.get_atl_loaded_app();
			service.attachBaseContext(atlLoadedApp.createContext(null, null, atlLoadedApp.pkg.applicationInfo.theme));
			service.onCreate();
			android.os.IBinder binder = service.onBind(new android.content.Intent().setComponent(job.getService()));
			if (!(binder instanceof JobServiceEngine.EngineBinder))
				throw new IllegalArgumentException(cls.getName() + " is no job service");
			engine = ((JobServiceEngine.EngineBinder)binder).engine;
			engines.put(cls.getName(), engine);
		}
		return engine;
	}

	/* starts a scheduled job once its delay or its trigger content change is due */
	private final class Start implements Runnable {
		private final JobInfo job;
		/* AOSP JobStatus.changedUris/changedAuthorities, handed to the service in the JobParameters */
		Uri[] changedUris;
		String[] changedAuthorities;

		Start(JobInfo job) {
			this.job = job;
		}

		@Override
		public void run() {
			if (pendingJobs.get(job.getId()) != job)
				return; // cancelled meanwhile
			try {
				String className = job.getService().getClassName();
				Class<? extends JobService> cls = context.get_atl_loaded_app()
				                                      .loadClass(className)
				                                      .asSubclass(JobService.class);
				JobParameters params = new JobParameters(job, namespace);
				params.triggeredContentUris = changedUris;
				params.triggeredContentAuthorities = changedAuthorities;
				job.running = true;
				boolean result = jobService(cls).onStartJob(params);
				Slog.i(TAG, "onStartJob() returned " + result);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	public int schedule(JobInfo job) {
		Slog.i(TAG, "JobScheduler.schedule() called with job: " + job);
		if (pendingJobs.containsKey(job.getId()))
			return 1; //RESULT_SUCCESS
		pendingJobs.put(job.getId(), job);
		Start start = new Start(job);
		if (job.getTriggerContentUris() != null)
			contentTriggers.put(job, new ContentTrigger(job, start));
		else
			new Handler(Looper.getMainLooper()).postDelayed(start, job.minLatencyMillis);
		return 1; //RESULT_SUCCESS
	}

	/* the jobs that wait for a change of their trigger content URIs */
	private static IdentityHashMap<JobInfo, ContentTrigger> contentTriggers = new IdentityHashMap<>();

	/* AOSP ContentObserverController: a job with trigger content URIs runs only after a change of one of them was
	 * reported, getTriggerContentUpdateDelay() after the last change and at most getTriggerContentMaxDelay() after
	 * the first one (JobStatus: default 10 s / 2 min, at least 500 ms / 1 s); then the observers go */
	private final class ContentTrigger extends android.database.ContentObserver {
		private final JobInfo job;
		private final Start start;
		private final Handler handler = new Handler(Looper.getMainLooper());
		private final Runnable trigger = new Runnable() {
			@Override
			public void run() {
				detach();
				start.changedUris = changedUris.toArray(new Uri[0]);
				start.changedAuthorities = changedAuthorities.toArray(new String[0]);
				handler.postDelayed(start, Math.max(0, scheduled + job.minLatencyMillis - android.os.SystemClock.elapsedRealtime()));
			}
		};
		private final long scheduled = android.os.SystemClock.elapsedRealtime();
		private boolean pending;
		/* AOSP ContentObserverController.ObserverInstance: the changed URIs (at most 50) and authorities */
		private final LinkedHashSet<Uri> changedUris = new LinkedHashSet<>();
		private final LinkedHashSet<String> changedAuthorities = new LinkedHashSet<>();

		ContentTrigger(JobInfo job, Start start) {
			super(new Handler(Looper.getMainLooper()));
			this.job = job;
			this.start = start;
			for (JobInfo.TriggerContentUri uri : job.getTriggerContentUris())
				context.getContentResolver().registerContentObserver(uri.getUri(),
				    (uri.getFlags() & JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS) != 0, this);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			if (uri != null) {
				if (changedUris.size() < 50)
					changedUris.add(uri);
				changedAuthorities.add(uri.getAuthority());
			}
			if (!pending) {
				pending = true;
				long max = job.getTriggerContentMaxDelay();
				handler.postDelayed(trigger, max < 0 ? 2 * 60 * 1000 : Math.max(max, 1000));
			}
			handler.removeCallbacks(trigger, this);
			long update = job.getTriggerContentUpdateDelay();
			handler.postAtTime(trigger, this, android.os.SystemClock.uptimeMillis() + (update < 0 ? 10 * 1000 : Math.max(update, 500)));
		}

		void detach() {
			handler.removeCallbacksAndMessages(null);
			context.getContentResolver().unregisterContentObserver(this);
			contentTriggers.remove(job);
		}
	}

	public JobInfo getPendingJob(int jobId) {
		return pendingJobs.get(jobId);
	}

	/**
	 * Cancel all jobs that have been scheduled in the current namespace by the calling
	 * application.
	 */
	public void cancelAll() {
		for (Integer id : new ArrayList<>(pendingJobs.keySet()))
			cancel(id);
	}

	/**
	 * Cancel all jobs that have been scheduled by the calling application, regardless of
	 * namespace.
	 */
	public void cancelInAllNamespaces() {
		for (String ns : new ArrayList<>(namespaces.keySet()))
			new JobScheduler(context, ns).cancelAll();
	}

	/**
	 * Retrieve all jobs that have been scheduled by the calling application, keyed by namespace.
	 */
	public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces() {
		Map<String, List<JobInfo>> result = new HashMap<>();
		for (Map.Entry<String, Map<Integer, JobInfo>> e : namespaces.entrySet())
			if (!e.getValue().isEmpty())
				result.put(e.getKey(), new ArrayList<>(e.getValue().values()));
		return result;
	}

	public void cancel(int id) {
		JobInfo job = pendingJobs.remove(id);
		ContentTrigger trigger = job != null ? contentTriggers.get(job) : null;
		if (trigger != null)
			trigger.detach();
		if (job != null && job.running) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					JobService service = runningServices.get(job.getService().getClass());
					if (service != null) {
						JobParameters params = new JobParameters(job, namespace);
						service.onStopJob(params);
						job.running = false;
					}
				}
			});
		}
	}
}
