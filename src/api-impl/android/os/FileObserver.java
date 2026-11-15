package android.os;

public class FileObserver {

	public FileObserver(String path, int mask) {}

	public FileObserver(String path) {}

	public FileObserver(java.io.File file) {}

	public FileObserver(java.io.File file, int mask) {}

	public FileObserver(java.util.List<java.io.File> files) {}

	public FileObserver(java.util.List<java.io.File> files, int mask) {}

	public void startWatching() {}

	public void stopWatching() {}
}
