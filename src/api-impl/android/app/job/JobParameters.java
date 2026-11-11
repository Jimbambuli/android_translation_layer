package android.app.job;

import android.net.Network;
import android.net.Uri;
import android.os.Parcelable;
import android.os.PersistableBundle;

public class JobParameters implements Parcelable {

	public static final Creator<JobParameters> CREATOR = null;

	JobInfo jobInfo;
	String namespace;

	JobParameters(JobInfo jobInfo, String namespace) {
		this.jobInfo = jobInfo;
		this.namespace = namespace;
	}

	public int getJobId() {
		return jobInfo.getId();
	}

	/**
	 * Get the namespace this job was placed in.
	 */
	public String getJobNamespace() {
		return namespace;
	}

	/**
	 * Return the network that should be used to perform any network requests
	 * for this job, or {@code null} if the job did not ask for a network.
	 */
	public Network getNetwork() {
		/* there is always a network, as in ConnectivityManager.getActiveNetwork() */
		return jobInfo.networkType != JobInfo.NETWORK_TYPE_NONE ? new Network() : null;
	}

	public PersistableBundle getExtras() {
		return jobInfo.getExtras();
	}

	/* ATL never stops jobs, so STOP_REASON_UNDEFINED */
	public int getStopReason() {
		return 0;
	}

	/* the work of JobScheduler.enqueue(), which AOSP keeps in JobStatus and hands out through IJobCallback */
	private final java.util.ArrayDeque<JobWorkItem> pendingWork = new java.util.ArrayDeque<>();
	private final java.util.ArrayList<JobWorkItem> executingWork = new java.util.ArrayList<>();
	private int nextWorkId = 1;
	private boolean finished;

	synchronized boolean enqueueWork(JobWorkItem work) {
		if (finished)
			return false;
		work.mWorkId = nextWorkId++;
		pendingWork.add(work);
		return true;
	}

	/* AOSP JobParameters.dequeueWork(): the next work item, or null when there is none left, which ends the job
	 * (JobServiceContext.doDequeueWork()) once no item is executing any more */
	public JobWorkItem dequeueWork() {
		synchronized (this) {
			JobWorkItem work = pendingWork.poll();
			if (work != null) {
				work.mDeliveryCount++;
				executingWork.add(work);
				return work;
			}
			if (!executingWork.isEmpty())
				return null;
		}
		finish();
		return null;
	}

	/* AOSP JobParameters.completeWork() */
	public void completeWork(JobWorkItem work) {
		synchronized (this) {
			if (!executingWork.remove(work)) {
				throw new IllegalArgumentException("Given work is not active: " + work);
			}
		}
	}

	/* the job is done (jobFinished() or the last work dequeued): JobScheduler forgets it */
	void finish() {
		synchronized (this) {
			if (finished)
				return;
			finished = true;
		}
		jobInfo.running = false;
		if (jobInfo.workParams == this)
			jobInfo.workParams = null;
		JobScheduler.pendingJobs(namespace).remove(jobInfo.getId());
	}

	public Uri[] getTriggeredContentUris() {
		return new Uri[0];
	}

	public String[] getTriggeredContentAuthorities() {
		return new String[0];
	}
}
