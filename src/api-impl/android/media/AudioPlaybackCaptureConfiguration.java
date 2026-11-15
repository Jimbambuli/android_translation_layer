package android.media;

import android.media.projection.MediaProjection;
import java.util.ArrayList;
import java.util.Objects;

/* AOSP android.media.AudioPlaybackCaptureConfiguration: which playback an AudioRecord captures through a
 * MediaProjection. ATL hands out no MediaProjection (MediaProjectionManager.getMediaProjection() is null), so a
 * configuration can not be built. The usages are kept as a list instead of AOSP's AudioMixingRule. */
public final class AudioPlaybackCaptureConfiguration {
	private final int[] mMatchingUsages;
	private final MediaProjection mProjection;

	private AudioPlaybackCaptureConfiguration(int[] matchingUsages, MediaProjection projection) {
		mMatchingUsages = matchingUsages;
		mProjection = projection;
	}

	public MediaProjection getMediaProjection() {
		return mProjection;
	}

	public int[] getMatchingUsages() {
		return mMatchingUsages.clone();
	}

	public static final class Builder {
		private static final int MATCH_TYPE_UNSPECIFIED = 0;
		private static final int MATCH_TYPE_INCLUSIVE = 1;
		private static final int MATCH_TYPE_EXCLUSIVE = 2;
		private static final String ERROR_MESSAGE_MISMATCHED_RULES = "Inclusive and exclusive usage rules cannot be combined";

		private final MediaProjection mProjection;
		private final ArrayList<Integer> mUsages = new ArrayList<>();
		private int mUsageMatchType = MATCH_TYPE_UNSPECIFIED;

		/* AOSP: checkNotNull(projection), then the projection must be able to project audio */
		public Builder(MediaProjection projection) {
			mProjection = Objects.requireNonNull(projection);
		}

		public Builder addMatchingUsage(int usage) {
			if (mUsageMatchType == MATCH_TYPE_EXCLUSIVE)
				throw new IllegalStateException(ERROR_MESSAGE_MISMATCHED_RULES);
			mUsages.add(usage);
			mUsageMatchType = MATCH_TYPE_INCLUSIVE;
			return this;
		}

		public AudioPlaybackCaptureConfiguration build() {
			int[] usages = new int[mUsages.size()];
			for (int i = 0; i < usages.length; i++)
				usages[i] = mUsages.get(i);
			return new AudioPlaybackCaptureConfiguration(usages, mProjection);
		}
	}
}
