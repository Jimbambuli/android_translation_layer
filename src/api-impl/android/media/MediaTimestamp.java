package android.media;

/* AOSP media/java/android/media/MediaTimestamp.java */
import android.annotation.FloatRange;

public final class MediaTimestamp
{

	public static final MediaTimestamp TIMESTAMP_UNKNOWN = new MediaTimestamp(-1, -1, 0.0f);

	public long getAnchorMediaTimeUs() {
		return mediaTimeUs;
	}

	@Deprecated
	public long getAnchorSytemNanoTime() {
		return getAnchorSystemNanoTime();
	}

	public long getAnchorSystemNanoTime() {
		return nanoTime;
	}

	@FloatRange(from = 0.0f, to = Float.MAX_VALUE)
	public float getMediaClockRate() {
		return clockRate;
	}

	public final long mediaTimeUs;

	public final long nanoTime;

	public final float clockRate;

	public MediaTimestamp(long mediaTimeUs, long nanoTimeNs,
			@FloatRange(from = 0.0f, to = Float.MAX_VALUE) float clockRate) {
		this.mediaTimeUs = mediaTimeUs;
		this.nanoTime = nanoTimeNs;
		this.clockRate = clockRate;
	}

	MediaTimestamp() {
		mediaTimeUs = 0;
		nanoTime = 0;
		clockRate = 1.0f;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		final MediaTimestamp that = (MediaTimestamp) obj;
		return (this.mediaTimeUs == that.mediaTimeUs)
				&& (this.nanoTime == that.nanoTime)
				&& (this.clockRate == that.clockRate);
	}

	@Override
	public String toString() {
		return getClass().getName()
				+ "{AnchorMediaTimeUs=" + mediaTimeUs
				+ " AnchorSystemNanoTime=" + nanoTime
				+ " clockRate=" + clockRate
				+ "}";
	}
}
