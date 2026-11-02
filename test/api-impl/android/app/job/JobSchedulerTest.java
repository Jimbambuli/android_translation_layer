/*
 * Host test for JobScheduler namespaces (API 34, used by WorkManager 2.9). Run with the runtime's dalvikvm, e.g. from
 * the build dir:
 *   javac -source 8 -target 8 -bootclasspath $PREFIX/lib64/java/core-all_classes.jar \
 *     -cp src/api-impl/hax.jar:$PREFIX/lib64/java/core-junit_classes.jar -d t ../test/api-impl/android/app/job/JobSchedulerTest.java
 *   dx --dex --output=t.jar t
 *   dalvikvm -Xbootclasspath/a:t.jar:api-impl.jar junit.textui.TestRunner android.app.job.JobSchedulerTest
 */
package android.app.job;

import android.content.ComponentName;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

public class JobSchedulerTest extends TestCase {
	private static JobInfo job(int id) {
		return new JobInfo.Builder(id, new ComponentName("p", "p.S")).build();
	}

	public void testDefaultNamespaceIsNull() {
		assertNull(new JobScheduler(null).getNamespace());
	}

	public void testForNamespace() {
		JobScheduler ns = new JobScheduler(null).forNamespace(" androidx.work.systemjobscheduler ");
		assertEquals("androidx.work.systemjobscheduler", ns.getNamespace());
	}

	public void testForNamespaceRejectsNullAndEmpty() {
		try {
			new JobScheduler(null).forNamespace(null);
			fail();
		} catch (NullPointerException expected) {
		}
		try {
			new JobScheduler(null).forNamespace("  ");
			fail();
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testNamespacesKeepTheirOwnJobs() {
		JobScheduler.pendingJobs(null).put(7, job(7));
		JobScheduler.pendingJobs("a").put(7, job(7));
		JobScheduler a = new JobScheduler(null).forNamespace("a");
		assertNotNull(a.getPendingJob(7));
		a.cancelAll();
		assertNull(a.getPendingJob(7));
		assertNotNull(new JobScheduler(null).getPendingJob(7));
		Map<String, List<JobInfo>> all = a.getPendingJobsInAllNamespaces();
		assertEquals(1, all.size());
		assertEquals(1, all.get(null).size());
		a.cancelInAllNamespaces();
		assertTrue(a.getPendingJobsInAllNamespaces().isEmpty());
	}

	public void testNetworkOnlyWhenRequired() {
		assertNull(new JobParameters(job(1), null).getNetwork());
		JobInfo net = new JobInfo.Builder(2, new ComponentName("p", "p.S"))
		                  .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
		                  .build();
		assertNotNull(new JobParameters(net, "a").getNetwork());
		assertEquals("a", new JobParameters(net, "a").getJobNamespace());
	}
}
