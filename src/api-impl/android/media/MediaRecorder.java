package android.media;

import android.content.Context;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/*
 * Sound recording like AOSP's MediaRecorder: the microphone (ALSA, in the Flatpak PulseAudio) encoded and muxed
 * by FFmpeg (android_media_MediaRecorder.c). Video sources are not recorded yet.
 */
public class MediaRecorder {
	public final class AudioSource {
		private AudioSource() {}
		public static final int DEFAULT = 0;
		public static final int MIC = 1;
		public static final int VOICE_UPLINK = 2;
		public static final int VOICE_DOWNLINK = 3;
		public static final int VOICE_CALL = 4;
		public static final int CAMCORDER = 5;
		public static final int VOICE_RECOGNITION = 6;
		public static final int VOICE_COMMUNICATION = 7;
		public static final int REMOTE_SUBMIX = 8;
		public static final int UNPROCESSED = 9;
		public static final int VOICE_PERFORMANCE = 10;
	}

	public final class VideoSource {
		private VideoSource() {}
		public static final int DEFAULT = 0;
		public static final int CAMERA = 1;
		public static final int SURFACE = 2;
	}

	public final class OutputFormat {
		private OutputFormat() {}
		public static final int DEFAULT = 0;
		public static final int THREE_GPP = 1;
		public static final int MPEG_4 = 2;
		public static final int RAW_AMR = 3;
		public static final int AMR_NB = 3;
		public static final int AMR_WB = 4;
		public static final int AAC_ADIF = 5;
		public static final int AAC_ADTS = 6;
		public static final int MPEG_2_TS = 8;
		public static final int WEBM = 9;
		public static final int HEIF = 10;
		public static final int OGG = 11;
	}

	public final class AudioEncoder {
		private AudioEncoder() {}
		public static final int DEFAULT = 0;
		public static final int AMR_NB = 1;
		public static final int AMR_WB = 2;
		public static final int AAC = 3;
		public static final int HE_AAC = 4;
		public static final int AAC_ELD = 5;
		public static final int VORBIS = 6;
		public static final int OPUS = 7;
	}

	public final class VideoEncoder {
		private VideoEncoder() {}
		public static final int DEFAULT = 0;
		public static final int H263 = 1;
		public static final int H264 = 2;
		public static final int MPEG_4_SP = 3;
		public static final int VP8 = 4;
		public static final int HEVC = 5;
		public static final int VP9 = 6;
		public static final int DOLBY_VISION = 7;
		public static final int AV1 = 8;
	}

	public class MetricsConstants {}

	public interface OnErrorListener {
		void onError(MediaRecorder mr, int what, int extra);
	}

	public interface OnInfoListener {
		void onInfo(MediaRecorder mr, int what, int extra);
	}

	public MediaRecorder() {}
	public MediaRecorder(Context context) {}

	private int audioSource = -1;
	private int videoSource = -1;
	private int outputFormat;
	private int audioEncoder;
	private int samplingRate;
	private int channels = 1;
	private int bitRate;
	private String path;
	private FileDescriptor fd;
	private long recorder;
	private boolean started;

	private static native long nativePrepare(String path, int fd, int outputFormat, int audioEncoder, int samplingRate, int channels, int bitRate);
	private static native void nativeStart(long recorder);
	private static native void nativePause(long recorder, boolean paused);
	private static native int nativeGetMaxAmplitude(long recorder);
	private static native void nativeStop(long recorder);

	public void setAudioSource(int audioSource) {
		this.audioSource = audioSource;
	}

	public void setVideoSource(int videoSource) {
		this.videoSource = videoSource;
	}

	public void setOutputFormat(int outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setAudioEncoder(int audioEncoder) {
		this.audioEncoder = audioEncoder;
	}

	public void setVideoEncoder(int videoEncoder) {}
	public void setVideoSize(int width, int height) {}
	public void setVideoFrameRate(int rate) {}
	public void setVideoEncodingBitRate(int bitRate) {}
	public void setOrientationHint(int degrees) {}
	public void setLocation(float latitude, float longitude) {}
	public void setMaxDuration(int maxDurationMs) {}
	public void setMaxFileSize(long maxFilesizeBytes) {}
	public void setOnErrorListener(OnErrorListener listener) {}
	public void setOnInfoListener(OnInfoListener listener) {}

	public void setAudioEncodingBitRate(int bitRate) {
		if (bitRate <= 0)
			throw new IllegalArgumentException("Audio encoding bit rate is not positive");
		this.bitRate = bitRate;
	}

	public void setAudioSamplingRate(int samplingRate) {
		if (samplingRate <= 0)
			throw new IllegalArgumentException("Audio sampling rate is not positive");
		this.samplingRate = samplingRate;
	}

	public void setAudioChannels(int numChannels) {
		if (numChannels <= 0)
			throw new IllegalArgumentException("Number of channels is not positive");
		this.channels = numChannels;
	}

	public void setOutputFile(String path) {
		this.path = path;
		this.fd = null;
	}

	public void setOutputFile(File file) {
		setOutputFile(file.getPath());
	}

	public void setOutputFile(FileDescriptor fd) {
		this.fd = fd;
		this.path = null;
	}

	public void prepare() throws IOException {
		if (path == null && fd == null)
			throw new IllegalStateException("No valid output file");
		if (audioSource < 0)
			throw new IllegalStateException("No audio source");
		if (videoSource >= 0)
			throw new IOException("ATL records no video yet");
		if (!AudioRecord.microphoneAvailable())
			throw new IOException("no microphone (outside a Flatpak only with ATL_UGLY_ENABLE_MICROPHONE)");
		recorder = nativePrepare(path, fd != null ? fd.getInt$() : -1, outputFormat, audioEncoder, samplingRate, channels, bitRate);
		if (recorder == 0)
			throw new IOException("prepare failed (microphone, encoder or file)");
	}

	public void start() {
		if (recorder == 0 || started)
			throw new IllegalStateException();
		nativeStart(recorder);
		started = true;
	}

	public void pause() {
		if (!started)
			throw new IllegalStateException();
		nativePause(recorder, true);
	}

	public void resume() {
		if (!started)
			throw new IllegalStateException();
		nativePause(recorder, false);
	}

	public int getMaxAmplitude() {
		return started ? nativeGetMaxAmplitude(recorder) : 0;
	}

	public void stop() {
		if (!started)
			throw new IllegalStateException();
		nativeStop(recorder);
		recorder = 0;
		started = false;
	}

	public void reset() {
		if (recorder != 0)
			nativeStop(recorder);
		recorder = 0;
		started = false;
		audioSource = videoSource = -1;
		outputFormat = audioEncoder = samplingRate = bitRate = 0;
		channels = 1;
		path = null;
		fd = null;
	}

	public void release() {
		reset();
	}

	/* AOSP MediaRecorder.setPreviewDisplay(): only remembers the surface (mSurface) for prepare() */
	private android.view.Surface mSurface;

	public void setPreviewDisplay(android.view.Surface sv) {
		mSurface = sv;
	}

	/* AOSP MediaRecorder.setProfile(): applies the profile through the public setters */
	public void setProfile(CamcorderProfile profile) {
		setOutputFormat(profile.fileFormat);
		setVideoFrameRate(profile.videoFrameRate);
		setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
		setVideoEncodingBitRate(profile.videoBitRate);
		setVideoEncoder(profile.videoCodec);
		if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
		    profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
			// Nothing needs to be done. Call to setCaptureRate() enables
			// time lapse video recording.
		} else {
			setAudioEncodingBitRate(profile.audioBitRate);
			setAudioChannels(profile.audioChannels);
			setAudioSamplingRate(profile.audioSampleRate);
			setAudioEncoder(profile.audioCodec);
		}
	}
}
