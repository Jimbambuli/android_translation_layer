package android.media;

import android.content.Context;
import android.view.Surface;
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

	/* AOSP StagefrightRecorder's defaults for a video track */
	private int videoEncoder;
	private int videoWidth = 176, videoHeight = 144, videoFrameRate = 30, videoBitRate = 192000;
	private int orientationHint;
	private android.hardware.Camera camera;
	private Surface inputSurface; // a PersistentSurface of setInputSurface()
	private ImageReader videoInput; // the consumer of the video source's frames
	private java.util.concurrent.ExecutorService videoThread;

	private static native long nativePrepare(String path, int fd, int outputFormat, boolean audio, int audioEncoder, int samplingRate, int channels, int bitRate,
	                                         String videoMime, int width, int height, int frameRate, int videoBitRate, int orientation);
	private static native int nativeWriteVideo(long recorder, long hardwareBuffer, java.nio.ByteBuffer[] planes, int[] rowStrides, int[] pixelStrides, int width, int height, long timestampNs);
	private static native void nativeStart(long recorder);
	private static native void nativePause(long recorder, boolean paused);
	private static native int nativeGetMaxAmplitude(long recorder);
	private static native boolean nativeStop(long recorder);

	public void setAudioSource(int audioSource) {
		this.audioSource = audioSource;
	}

	/* AOSP StagefrightRecorder::setVideoSource(): DEFAULT is the camera */
	public void setVideoSource(int videoSource) {
		this.videoSource = videoSource == VideoSource.DEFAULT ? VideoSource.CAMERA : videoSource;
	}

	public void setOutputFormat(int outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setAudioEncoder(int audioEncoder) {
		this.audioEncoder = audioEncoder;
	}

	public void setVideoEncoder(int videoEncoder) {
		this.videoEncoder = videoEncoder;
	}

	/* MediaRecorder.VideoEncoder as the encoder's type; DEFAULT as AOSP's setDefaultVideoEncoderIfNecessary(): VP8 for
	 * WebM, else the codec of the camera's QUALITY_LOW profile, which is H.264 in ATL's profiles */
	private String videoMime(int encoder) {
		if (encoder == VideoEncoder.DEFAULT && outputFormat == OutputFormat.WEBM)
			return MediaFormat.MIMETYPE_VIDEO_VP8;
		switch (encoder) {
			case VideoEncoder.H263:
				return MediaFormat.MIMETYPE_VIDEO_H263;
			case VideoEncoder.MPEG_4_SP:
				return MediaFormat.MIMETYPE_VIDEO_MPEG4;
			case VideoEncoder.VP8:
				return MediaFormat.MIMETYPE_VIDEO_VP8;
			case VideoEncoder.HEVC:
				return MediaFormat.MIMETYPE_VIDEO_HEVC;
			case 6: /* VP9 */
				return MediaFormat.MIMETYPE_VIDEO_VP9;
			case 8: /* AV1 */
				return MediaFormat.MIMETYPE_VIDEO_AV1;
			default:
				return MediaFormat.MIMETYPE_VIDEO_AVC;
		}
	}

	/* AOSP MediaRecorder.setVideoEncodingProfileLevel(): the checks, then setParameter() for the video encoder; ATL's
	 * encoders take their default profile */
	public void setVideoEncodingProfileLevel(int profile, int level) {
		if (profile < 0) {
			throw new IllegalArgumentException("Video encoding profile is not positive");
		}
		if (level < 0) {
			throw new IllegalArgumentException("Video encoding level is not positive");
		}
	}

	/* AOSP MediaRecorder.setCamera() (deprecated): hands the camera to the native recorder, used for a CAMERA video
	 * source; its preview frames are recorded */
	public void setCamera(android.hardware.Camera c) {
		camera = c;
	}

	/* AOSP MediaRecorder.getSurface(): the input surface of a recorder prepared with VIDEO_SOURCE_SURFACE, else
	 * IllegalStateException */
	public Surface getSurface() {
		if (videoInput == null || videoSource != VideoSource.SURFACE)
			throw new IllegalStateException("failed to get surface");
		return videoInput.getSurface();
	}

	public void setVideoSize(int width, int height) {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException("Video size must be positive");
		videoWidth = width;
		videoHeight = height;
	}

	public void setVideoFrameRate(int rate) {
		if (rate > 0)
			videoFrameRate = rate;
	}

	public void setVideoEncodingBitRate(int bitRate) {
		if (bitRate <= 0)
			throw new IllegalArgumentException("Video encoding bit rate is not positive");
		videoBitRate = bitRate;
	}

	/* AOSP: 0, 90, 180 or 270, written as the video's rotation in the container */
	public void setOrientationHint(int degrees) {
		if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270)
			throw new IllegalArgumentException("Unsupported angle: " + degrees);
		orientationHint = degrees;
	}
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
		if (audioSource < 0 && videoSource < 0)
			throw new IllegalStateException("No audio or video source");
		if (audioSource >= 0 && !AudioRecord.microphoneAvailable())
			throw new IOException("no microphone (outside a Flatpak only with ATL_UGLY_ENABLE_MICROPHONE)");
		recorder = nativePrepare(path, fd != null ? fd.getInt$() : -1, outputFormat, audioSource >= 0, audioEncoder, samplingRate, channels, bitRate,
		                         videoSource >= 0 ? videoMime(videoEncoder) : null, videoWidth, videoHeight, videoFrameRate, videoBitRate, orientationHint);
		if (recorder == 0)
			throw new IOException("prepare failed (microphone, encoder or file)");
		/* the frames of the video source come to an encoder's input surface: the app's (SURFACE) or the camera's
		 * recording stream (CAMERA, AOSP CameraSource) */
		if (videoSource >= 0)
			videoInput = ImageReader.atl_newEncoderInput(inputSurface, videoWidth, videoHeight);
	}

	/* the frames of the input surface into the recording, on a thread of the recorder (AOSP: the encoder's
	 * GraphicBufferSource) */
	private void writeVideo(ImageReader reader) {
		Image image;
		while ((image = reader.acquireNextImageNoThrowISE()) != null) {
			long buffer = ImageReader.atl_hardwareBuffer(image);
			Image.Plane[] planes = buffer == 0 ? ImageReader.atl_queuedPlanes(image) : null;
			int n = planes != null ? planes.length : 0;
			java.nio.ByteBuffer[] data = new java.nio.ByteBuffer[n];
			int[] rowStrides = new int[n], pixelStrides = new int[n];
			for (int i = 0; i < n; i++) {
				data[i] = planes[i].getBuffer();
				rowStrides[i] = planes[i].getRowStride();
				pixelStrides[i] = planes[i].getPixelStride();
			}
			synchronized (this) {
				if (recorder != 0 && started)
					nativeWriteVideo(recorder, buffer, data, rowStrides, pixelStrides, image.getWidth(), image.getHeight(), image.getTimestamp());
			}
			image.close();
		}
	}

	private void stopVideo() {
		if (camera != null && videoSource == VideoSource.CAMERA)
			camera.atl_setRecordingReader(null);
		if (videoThread != null) {
			videoInput.setOnImageAvailableListener(null, null);
			videoThread.shutdown();
			try {
				videoThread.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
			videoThread = null;
		}
	}

	private void closeVideoInput() {
		if (videoInput != null && inputSurface == null)
			videoInput.close();
		videoInput = null;
	}

	public void start() {
		if (recorder == 0 || started)
			throw new IllegalStateException();
		nativeStart(recorder);
		started = true;
		if (videoInput != null) {
			videoThread = java.util.concurrent.Executors.newSingleThreadExecutor();
			ImageReader reader = videoInput;
			videoInput.setOnImageAvailableListenerWithExecutor(r -> writeVideo(reader), videoThread);
			if (videoSource == VideoSource.CAMERA) {
				if (camera == null)
					throw new IllegalStateException("no camera for VIDEO_SOURCE_CAMERA (setCamera())");
				camera.atl_setRecordingReader(videoInput);
			}
		}
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
		stopVideo();
		boolean ok;
		synchronized (this) {
			ok = nativeStop(recorder);
			recorder = 0;
			started = false;
		}
		closeVideoInput();
		/* AOSP: "a RuntimeException is intentionally thrown to the application, if no valid audio/video data has been
		 * received when stop() is called" */
		if (!ok)
			throw new RuntimeException("stop failed.");
	}

	public void reset() {
		stopVideo();
		synchronized (this) {
			if (recorder != 0)
				nativeStop(recorder);
			recorder = 0;
			started = false;
		}
		closeVideoInput();
		audioSource = videoSource = -1;
		outputFormat = audioEncoder = samplingRate = bitRate = 0;
		videoEncoder = orientationHint = 0;
		videoWidth = 176;
		videoHeight = 144;
		videoFrameRate = 30;
		videoBitRate = 192000;
		camera = null;
		inputSurface = null;
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
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setNextOutputFile(java.io.File arg0) throws java.io.IOException {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setNextOutputFile(java.io.FileDescriptor arg0) throws java.io.IOException {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setPrivacySensitive(boolean arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setCaptureRate(double arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setPreferredMicrophoneDirection(int arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void addOnRoutingChangedListener(android.media.AudioRouting.OnRoutingChangedListener arg0, android.os.Handler arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    protected void finalize() {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.MicrophoneInfo> getActiveMicrophones() throws java.io.IOException { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.AudioRecordingConfiguration getActiveRecordingConfiguration() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final int getAudioSourceMax() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.metrics.LogSessionId getLogSessionId() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.os.PersistableBundle getMetrics() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.AudioDeviceInfo getPreferredDevice() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.AudioDeviceInfo getRoutedDevice() { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public java.util.List<android.media.AudioDeviceInfo> getRoutedDevices() { return new java.util.ArrayList<>(); }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean isPrivacySensitive() { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void registerAudioRecordingCallback(java.util.concurrent.Executor arg0, android.media.AudioManager.AudioRecordingCallback arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void removeOnRoutingChangedListener(android.media.AudioRouting.OnRoutingChangedListener arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setAudioProfile(android.media.EncoderProfiles.AudioProfile arg0) {  }
    /* AOSP setInputSurface(): a PersistentSurface (MediaCodec.createPersistentInputSurface()) as the input of
     * VIDEO_SOURCE_SURFACE, before prepare() */
    public void setInputSurface(Surface surface) {
        if (!(surface instanceof MediaCodec.PersistentSurface))
            throw new IllegalArgumentException("not a PersistentSurface");
        inputSurface = surface;
    }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setLogSessionId(android.media.metrics.LogSessionId arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setPreferredDevice(android.media.AudioDeviceInfo arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public boolean setPreferredMicrophoneFieldDimension(float arg0) { return false; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setVideoProfile(android.media.EncoderProfiles.VideoProfile arg0) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void unregisterAudioRecordingCallback(android.media.AudioManager.AudioRecordingCallback arg0) {  }
}
