package android.media.metrics;

/* API 31. There is no media metrics service: Context.getSystemService("media_metrics") returns
 * null, as on devices without one, so apps only need the class to exist for their null check. */
public final class MediaMetricsManager {
	private MediaMetricsManager() {}
}
