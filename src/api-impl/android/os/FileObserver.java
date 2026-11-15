package android.os;

public class FileObserver {

	public FileObserver(String path, int mask) {}

	public FileObserver(String path) {}

	public FileObserver(java.io.File file) {}

	public FileObserver(java.io.File file, int mask) {}

	public void startWatching() {}

	public void stopWatching() {}
}
