package android.media;

import android.annotation.NonNull;
import android.media.projection.MediaProjection;
import java.util.ArrayList;
import java.util.Objects;

/* AOSP media/java/android/media/AudioPlaybackCaptureConfiguration.java: which playback an AudioRecord captures
 * through a MediaProjection. ATL hands out no MediaProjection (MediaProjectionManager.getMediaProjection() is
 * null), so a configuration can not be built. AOSP keeps the criteria in an AudioMixingRule (and filters it by
 * rule type in the getters); ATL has no audiopolicy package, so the four criteria kinds (match/exclude x
 * usage/uid) are kept as lists in insertion order, which is what the getters return in AOSP too. */
public final class AudioPlaybackCaptureConfiguration {
	private final int[] mMatchingUsages;
	private final int[] mMatchingUids;
	private final int[] mExcludeUsages;
	private final int[] mExcludeUids;
	private final MediaProjection mProjection;

	private AudioPlaybackCaptureConfiguration(int[] matchingUsages, int[] matchingUids, int[] excludeUsages, int[] excludeUids, MediaProjection projection) {
		mMatchingUsages = matchingUsages;
		mMatchingUids = matchingUids;
		mExcludeUsages = excludeUsages;
		mExcludeUids = excludeUids;
		mProjection = projection;
	}

	@NonNull
	public MediaProjection getMediaProjection() {
		return mProjection;
	}

	@NonNull
	public int[] getMatchingUsages() {
		return mMatchingUsages.clone();
	}

	@NonNull
	public int[] getMatchingUids() {
		return mMatchingUids.clone();
	}

	@NonNull
	public int[] getExcludeUsages() {
		return mExcludeUsages.clone();
	}

	@NonNull
	public int[] getExcludeUids() {
		return mExcludeUids.clone();
	}

	public static final class Builder {
		private static final int MATCH_TYPE_UNSPECIFIED = 0;
		private static final int MATCH_TYPE_INCLUSIVE = 1;
		private static final int MATCH_TYPE_EXCLUSIVE = 2;
		private static final String ERROR_MESSAGE_MISMATCHED_RULES = "Inclusive and exclusive usage rules cannot be combined";

		private final MediaProjection mProjection;
		private final ArrayList<Integer> mMatchingUsages = new ArrayList<>();
		private final ArrayList<Integer> mMatchingUids = new ArrayList<>();
		private final ArrayList<Integer> mExcludeUsages = new ArrayList<>();
		private final ArrayList<Integer> mExcludeUids = new ArrayList<>();
		private int mUsageMatchType = MATCH_TYPE_UNSPECIFIED;
		private int mUidMatchType = MATCH_TYPE_UNSPECIFIED;

		/* AOSP: checkNotNull(projection), then the projection must be able to project audio */
		public Builder(@NonNull MediaProjection projection) {
			Objects.requireNonNull(projection);
			mProjection = projection;
		}

		@NonNull
		public Builder addMatchingUsage(int usage) {
			if (mUsageMatchType == MATCH_TYPE_EXCLUSIVE)
				throw new IllegalStateException(ERROR_MESSAGE_MISMATCHED_RULES);
			mMatchingUsages.add(usage);
			mUsageMatchType = MATCH_TYPE_INCLUSIVE;
			return this;
		}

		@NonNull
		public Builder addMatchingUid(int uid) {
			if (mUidMatchType == MATCH_TYPE_EXCLUSIVE)
				throw new IllegalStateException(ERROR_MESSAGE_MISMATCHED_RULES);
			mMatchingUids.add(uid);
			mUidMatchType = MATCH_TYPE_INCLUSIVE;
			return this;
		}

		@NonNull
		public Builder excludeUsage(int usage) {
			if (mUsageMatchType == MATCH_TYPE_INCLUSIVE)
				throw new IllegalStateException(ERROR_MESSAGE_MISMATCHED_RULES);
			mExcludeUsages.add(usage);
			mUsageMatchType = MATCH_TYPE_EXCLUSIVE;
			return this;
		}

		@NonNull
		public Builder excludeUid(int uid) {
			if (mUidMatchType == MATCH_TYPE_INCLUSIVE)
				throw new IllegalStateException(ERROR_MESSAGE_MISMATCHED_RULES);
			mExcludeUids.add(uid);
			mUidMatchType = MATCH_TYPE_EXCLUSIVE;
			return this;
		}

		@NonNull
		public AudioPlaybackCaptureConfiguration build() {
			return new AudioPlaybackCaptureConfiguration(toArray(mMatchingUsages), toArray(mMatchingUids), toArray(mExcludeUsages), toArray(mExcludeUids), mProjection);
		}

		private static int[] toArray(ArrayList<Integer> list) {
			int[] array = new int[list.size()];
			for (int i = 0; i < array.length; i++)
				array[i] = list.get(i);
			return array;
		}
	}
}
