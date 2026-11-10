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

	public Uri[] getTriggeredContentUris() {
		return new Uri[0];
	}

	public String[] getTriggeredContentAuthorities() {
		return new String[0];
	}
}
