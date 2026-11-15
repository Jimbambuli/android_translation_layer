package android.net.rtp;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

/* android.net.rtp.AudioGroup (framework-current.txt). The AOSP mixer is native code on top of AudioRecord/AudioTrack;
 * ATL has none, so a stream cannot join a group (the native add() failing throws IllegalStateException). */
@Deprecated
public class AudioGroup {
	public static final int MODE_ON_HOLD = 0;
	public static final int MODE_MUTED = 1;
	public static final int MODE_NORMAL = 2;
	public static final int MODE_ECHO_SUPPRESSION = 3;

	private static final int MODE_LAST = 3;

	private final Map<AudioStream, Long> mStreams = new HashMap<AudioStream, Long>();
	private int mMode = MODE_ON_HOLD;

	public AudioGroup() {
	}

	public AudioGroup(Context context) {
	}

	public AudioStream[] getStreams() {
		synchronized (this) {
			return mStreams.keySet().toArray(new AudioStream[mStreams.size()]);
		}
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mode) {
		if (mode < 0 || mode > MODE_LAST) {
			throw new IllegalArgumentException("Invalid mode");
		}
		synchronized (this) {
			mMode = mode;
		}
	}

	synchronized void add(AudioStream stream, AudioCodec codec, int dtmfType) {
		throw new IllegalStateException("AudioGroup is not available");
	}

	synchronized void remove(AudioStream stream) {
		mStreams.remove(stream);
	}

	public void sendDtmf(int event) {
		if (event < 0 || event > 15) {
			throw new IllegalArgumentException("Invalid event");
		}
	}

	public void clear() {
		for (AudioStream stream : getStreams()) {
			stream.join(null);
		}
	}
}
