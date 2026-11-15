package android.os;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/* AOSP FileObserver: the inotify thread (ObserverThread with its JNI init/observe/startWatching) is not
 * taken over, so startWatching() registers nothing and onEvent() is never called. */
public abstract class FileObserver {
	public static final int ACCESS = 0x00000001;
	public static final int MODIFY = 0x00000002;
	public static final int ATTRIB = 0x00000004;
	public static final int CLOSE_WRITE = 0x00000008;
	public static final int CLOSE_NOWRITE = 0x00000010;
	public static final int OPEN = 0x00000020;
	public static final int MOVED_FROM = 0x00000040;
	public static final int MOVED_TO = 0x00000080;
	public static final int CREATE = 0x00000100;
	public static final int DELETE = 0x00000200;
	public static final int DELETE_SELF = 0x00000400;
	public static final int MOVE_SELF = 0x00000800;
	public static final int ALL_EVENTS = ACCESS | MODIFY | ATTRIB | CLOSE_WRITE | CLOSE_NOWRITE | OPEN | MOVED_FROM |
	                                     MOVED_TO | DELETE | CREATE | DELETE_SELF | MOVE_SELF;

	private final List<File> mFiles;
	private boolean mWatching;
	private final int mMask;

	public FileObserver(String path) {
		this(new File(path));
	}

	public FileObserver(File file) {
		this(Arrays.asList(file));
	}

	public FileObserver(List<File> files) {
		this(files, ALL_EVENTS);
	}

	public FileObserver(String path, int mask) {
		this(new File(path), mask);
	}

	public FileObserver(File file, int mask) {
		this(Arrays.asList(file), mask);
	}

	public FileObserver(List<File> files, int mask) {
		mFiles = files;
		mMask = mask;
	}

	protected void finalize() {
		stopWatching();
	}

	public void startWatching() {
		mWatching = true;
	}

	public void stopWatching() {
		mWatching = false;
	}

	public abstract void onEvent(int event, String path);
}
