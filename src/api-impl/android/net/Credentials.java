package android.net;

/* From AOSP frameworks/base core/java/android/net/Credentials.java. */
public class Credentials {
	private final int pid;
	private final int uid;
	private final int gid;

	public Credentials(int pid, int uid, int gid) {
		this.pid = pid;
		this.uid = uid;
		this.gid = gid;
	}

	public int getPid() {
		return pid;
	}

	public int getUid() {
		return uid;
	}

	public int getGid() {
		return gid;
	}
}
