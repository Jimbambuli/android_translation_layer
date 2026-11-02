package android.app.job;

import android.atl.ATLLoadedApp;
import android.content.Context;
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

	public int enqueue(JobInfo job, JobWorkItem work) {
		return 1; //RESULT_SUCCESS
	}

	public int schedule(JobInfo job) {
		Slog.i(TAG, "JobScheduler.schedule() called with job: " + job);
		if (pendingJobs.containsKey(job.getId()))
			return 1; //RESULT_SUCCESS
		pendingJobs.put(job.getId(), job);
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					String className = job.getService().getClassName();
					Class<? extends JobService> cls = context.get_atl_loaded_app()
					                                      .loadClass(className)
					                                      .asSubclass(JobService.class);
					if (!runningServices.containsKey(cls)) {
						JobService service = cls.getConstructor().newInstance();
						ATLLoadedApp atlLoadedApp = context.get_atl_loaded_app();
						service.attachBaseContext(atlLoadedApp.createContext(null, null,
						                                                     atlLoadedApp.pkg.applicationInfo.theme));
						service.onCreate();
						runningServices.put(cls, service);
					}
					job.running = true;
					boolean result = runningServices.get(cls).onStartJob(new JobParameters(job, namespace));
					Slog.i(TAG, "onStartJob() returned " + result);
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
		}, job.minLatencyMillis);
		return 1; //RESULT_SUCCESS
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
