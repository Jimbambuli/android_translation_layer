package android.content;

/* AOSP android.content.ComponentCallbacks2 */
public interface ComponentCallbacks2 extends ComponentCallbacks {
	static final int TRIM_MEMORY_COMPLETE = 80;
	static final int TRIM_MEMORY_MODERATE = 60;
	static final int TRIM_MEMORY_BACKGROUND = 40;
	static final int TRIM_MEMORY_UI_HIDDEN = 20;
	static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;
	static final int TRIM_MEMORY_RUNNING_LOW = 10;
	static final int TRIM_MEMORY_RUNNING_MODERATE = 5;

	void onTrimMemory(int level);
}
