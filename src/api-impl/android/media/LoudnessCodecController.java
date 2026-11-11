package android.media;

import android.os.Bundle;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/* AOSP android.media.LoudnessCodecController (API 35): the AudioService's loudness configurator (CTA-2075) sends
 * DRC parameters for the AAC decoders of a session. ATL has no such service: AAC decoders are taken like AOSP
 * takes them, but no parameters ever come (getLoudnessCodecParams() is empty, the listener is not called). */
public class LoudnessCodecController implements AutoCloseable {

	public interface OnLoudnessCodecUpdateListener {
		default Bundle onLoudnessCodecUpdate(MediaCodec mediaCodec, Bundle codecValues) {
			return codecValues;
		}
	}

	private final int mSessionId;
	private final Set<MediaCodec> mMediaCodecs = new HashSet<>();

	private LoudnessCodecController(int sessionId) {
		mSessionId = sessionId;
	}

	public static LoudnessCodecController create(int sessionId) {
		return new LoudnessCodecController(sessionId);
	}

	public static LoudnessCodecController create(int sessionId, Executor executor, OnLoudnessCodecUpdateListener listener) {
		Objects.requireNonNull(executor, "Executor cannot be null");
		Objects.requireNonNull(listener, "OnLoudnessCodecUpdateListener cannot be null");
		return new LoudnessCodecController(sessionId);
	}

	/* AOSP getCodecInfo(): only a configured AAC decoder carries loudness metadata */
	private static boolean hasLoudnessInfo(MediaCodec mediaCodec) {
		try {
			return "audio/mp4a-latm".equalsIgnoreCase(mediaCodec.getInputFormat().getString("mime")); // MIMETYPE_AUDIO_AAC, KEY_MIME
		} catch (IllegalStateException e) {
			return false; // not configured
		}
	}

	public boolean addMediaCodec(MediaCodec mediaCodec) {
		Objects.requireNonNull(mediaCodec, "MediaCodec for addMediaCodec cannot be null");
		if (!hasLoudnessInfo(mediaCodec))
			return false;
		synchronized (mMediaCodecs) {
			if (!mMediaCodecs.add(mediaCodec))
				throw new IllegalArgumentException("Loudness controller already added " + mediaCodec);
		}
		return true;
	}

	public void removeMediaCodec(MediaCodec mediaCodec) {
		Objects.requireNonNull(mediaCodec, "MediaCodec for removeMediaCodec cannot be null");
		if (!hasLoudnessInfo(mediaCodec))
			throw new IllegalArgumentException("Could not extract codec loudness information");
		synchronized (mMediaCodecs) {
			if (!mMediaCodecs.remove(mediaCodec))
				throw new IllegalArgumentException("Loudness controller does not contain " + mediaCodec);
		}
	}

	public Bundle getLoudnessCodecParams(MediaCodec mediaCodec) {
		Objects.requireNonNull(mediaCodec, "MediaCodec cannot be null");
		if (!hasLoudnessInfo(mediaCodec))
			throw new IllegalArgumentException("MediaCodec does not have valid codec information");
		synchronized (mMediaCodecs) {
			if (!mMediaCodecs.contains(mediaCodec))
				throw new IllegalArgumentException("MediaCodec was not added for loudness annotation");
		}
		return new Bundle();
	}

	@Override
	public void close() {
		synchronized (mMediaCodecs) {
			mMediaCodecs.clear();
		}
	}
}
