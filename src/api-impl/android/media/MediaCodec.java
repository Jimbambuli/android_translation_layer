package android.media;

import android.media.MediaCodec.BufferInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;

public class MediaCodec {

	public static final int BUFFER_FLAG_END_OF_STREAM = 0x4;

	private String codecName;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private long[] inputBufferTimestamps;
	private long native_codec;
	private boolean outputFormatSet = false;
	private MediaFormat mediaFormat;

	private Queue<Integer> freeOutputBuffers;
	private Queue<Integer> queuedInputBuffers;
	private Queue<Integer> freeInputBuffers;

	private MediaCodec(String codecName) throws IOException {
		this.codecName = codecName;
		native_codec = native_constructor(codecName);
		if (native_codec == 0) {
			throw new IOException("Unable to create MediaCodec: " + codecName);
		}
	}

	public static MediaCodec createByCodecName(String codecName) throws IOException {
		return new MediaCodec(codecName);
	}

	/* AOSP MediaCodec.getName(): the name the codec was created with; IllegalStateException once released */
	public String getName() {
		if (native_codec == 0)
			throw new IllegalStateException();
		return codecName;
	}

	public MediaCodecInfo getCodecInfo() {
		for (int i = 0; i < MediaCodecList.getCodecCount(); i++)
			if (MediaCodecList.getCodecInfoAt(i).getName().equals(codecName))
				return MediaCodecList.getCodecInfoAt(i);
		return null;
	}

	public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
		System.out.println("MediaCodec.configure(" + format + ", " + surface + ", " + crypto + ", " + flags + "): codecName=" + codecName);
		this.mediaFormat = format;
		mHasSurface = surface != null;

		int maxInputSize = 262144;
		if (format.containsKey("max-input-size")) {
			maxInputSize = format.getInteger("max-input-size");
		}
		inputBuffers = new ByteBuffer[1];
		inputBufferTimestamps = new long[inputBuffers.length];
		freeInputBuffers = new ArrayDeque<>(inputBuffers.length);
		queuedInputBuffers = new ArrayDeque<>(inputBuffers.length);
		for (int i = 0; i < inputBuffers.length; i++) {
			inputBuffers[i] = ByteBuffer.allocateDirect(maxInputSize).order(ByteOrder.LITTLE_ENDIAN);
			freeInputBuffers.add(i);
		}
		outputBuffers = new ByteBuffer[4];
		freeOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		for (int i = 0; i < outputBuffers.length; i++) {
			outputBuffers[i] = ByteBuffer.allocateDirect(16384).order(ByteOrder.LITTLE_ENDIAN);
			freeOutputBuffers.add(i);
		}

		if ("aac".equals(codecName) || "mp3".equals(codecName) || "opus".equals(codecName)) {
			native_configure_audio(native_codec, format.getByteBuffer("csd-0"), format.getInteger("sample-rate"), format.getInteger("channel-count"));
		} else if ("h264".equals(codecName) || "vp8".equals(codecName) || "vp9".equals(codecName)) {
			/* the output format (here the configured one) names the color format, which Gecko requires; frames
			 * go to the Surface: COLOR_FormatSurface */
			if (surface != null && !format.containsKey("color-format"))
				format.setInteger("color-format", 0x7F000789);
			native_configure_video(native_codec, format.getByteBuffer("csd-0"), format.getByteBuffer("csd-1"), surface,
			                       format.getInteger("rotation-degrees", 0));
		} else {
			System.out.println("configure: format " + format + " not implemented");
			Thread.dumpStack();
			System.exit(1);
		}
	}

	public void start() {
		System.out.println("MediaCodec.start(): codecName=" + codecName);
		native_start(native_codec);
		if (callback != null && !pumping) {
			pumping = true;
			callbackHandler.post(pump);
		}
	}

	/* Asynchronous mode. The codec is driven synchronously underneath: a loop on the callback's
	 * handler hands out every free input buffer and every decoded output buffer through the
	 * callback, which then owns it, like AOSP's MediaCodec does with its own callbacks. */
	public static abstract class Callback {
		public abstract void onInputBufferAvailable(MediaCodec codec, int index);
		public abstract void onOutputBufferAvailable(MediaCodec codec, int index, BufferInfo info);
		public abstract void onError(MediaCodec codec, CodecException e);
		public abstract void onOutputFormatChanged(MediaCodec codec, MediaFormat format);

		/* AOSP MediaCodec.Callback (frameworks/base/media/java/android/media/MediaCodec.java) */
		public void onOutputBuffersAvailable(MediaCodec codec, int index, ArrayDeque<BufferInfo> infos) {
			throw new IllegalStateException("Client must override onOutputBuffersAvailable when codec is configured to operate with multiple access units");
		}

		public void onCryptoError(MediaCodec codec, CryptoException e) {
			throw new IllegalStateException("Client must override onCryptoError when the codec is configured with CONFIGURE_FLAG_USE_CRYPTO_ASYNC.", e);
		}

		public void onMetricsFlushed(MediaCodec codec, PersistableBundle metrics) {
		}
	}

	public static final class CodecException extends IllegalStateException {
		private final int errorCode;

		public CodecException(int errorCode, String detailMessage) {
			super(detailMessage);
			this.errorCode = errorCode;
		}

		public int getErrorCode() { return errorCode; }
		public boolean isTransient() { return false; }
		public boolean isRecoverable() { return false; }
		public String getDiagnosticInfo() { return getMessage(); }
	}

	private Callback callback;
	private android.os.Handler callbackHandler;
	private volatile boolean pumping;
	private final Object mListenerLock = new Object();

	/* AOSP MediaCodec.stop(): drop the callback messages still queued on the callback handler. */
	private void stopPumping() {
		synchronized (mListenerLock) {
			pumping = false;
			if (callbackHandler != null)
				callbackHandler.removeCallbacks(pump);
		}
	}

	public void setCallback(Callback cb) {
		setCallback(cb, null);
	}

	public void setCallback(Callback cb, android.os.Handler handler) {
		callback = cb;
		if (handler == null) {
			android.os.Looper looper = android.os.Looper.myLooper();
			handler = new android.os.Handler(looper != null ? looper : android.os.Looper.getMainLooper());
		}
		callbackHandler = handler;
	}

	private final Runnable pump = new Runnable() {
		@Override
		public void run() {
			if (!pumping)
				return;
			int index;
			while (pumping && (index = dequeueInputBuffer(0)) >= 0)
				callback.onInputBufferAvailable(MediaCodec.this, index);
			while (pumping) {
				BufferInfo info = new BufferInfo();
				index = dequeueOutputBuffer(info, 0);
				if (index == /*INFO_OUTPUT_FORMAT_CHANGED*/ -2)
					callback.onOutputFormatChanged(MediaCodec.this, getOutputFormat());
				else if (index >= 0)
					callback.onOutputBufferAvailable(MediaCodec.this, index, info);
				else
					break;
			}
			/* Like AOSP's stop(), which removes the pending callback messages under mListenerLock, so
			 * that nothing is posted to a handler whose thread is gone once the codec is stopped.
			 * ExoPlayer quits the callback thread before it releases the codec, hence the check of the queue. */
			synchronized (mListenerLock) {
				if (pumping && !callbackHandler.getLooper().getQueue().isQuitting())
					callbackHandler.postDelayed(this, 5);
			}
		}
	};

	public ByteBuffer[] getInputBuffers() {
		return inputBuffers;
	}

	/* AOSP: the input buffer comes cleared (position 0, limit = capacity) */
	public ByteBuffer getInputBuffer(int index) {
		inputBuffers[index].clear();
		return inputBuffers[index];
	}

	/* AOSP getInputImage(): null unless the input buffer holds a raw video frame; the input of ATL's
	 * decoders is always compressed data */
	public Image getInputImage(int index) {
		return null;
	}

	public ByteBuffer[] getOutputBuffers() {
		return outputBuffers;
	}

	public ByteBuffer getOutputBuffer(int index) {
		return outputBuffers[index];
	}

	public synchronized int dequeueOutputBuffer(BufferInfo info, long timeoutUs) {
		if (!outputFormatSet) {
			outputFormatSet = true;
			return /*INFO_OUTPUT_FORMAT_CHANGED*/ -2;
		}
		Integer index = freeOutputBuffers.poll();
		if (index == null) {
			return /*INFO_TRY_AGAIN_LATER*/ -1;
		}
		outputBuffers[index].clear();
		int ret = native_dequeueOutputBuffer(native_codec, outputBuffers[index], info);
		if (ret == 0) {
			ret = index;
		} else {
			freeOutputBuffers.add(index);
			ret = /*INFO_TRY_AGAIN_LATER*/ -1;
		}
		return ret;
	}

	public synchronized void releaseOutputBuffer(int index, boolean render) {
		native_releaseOutputBuffer(native_codec, outputBuffers[index], render, 0);
		freeOutputBuffers.add(index);
	}

	// AOSP calls the parameter `presentationTimeUs`, but actually it is nanoseconds
	public synchronized void releaseOutputBuffer(int index, long releaseTimeNs) {
		native_releaseOutputBuffer(native_codec, outputBuffers[index], true, releaseTimeNs);
		freeOutputBuffers.add(index);
	}

	/* AOSP MediaCodec.getInputFormat(): the format the codec was configured with */
	public MediaFormat getInputFormat() {
		if (mediaFormat == null)
			throw new IllegalStateException();
		return mediaFormat;
	}

	public MediaFormat getOutputFormat() {
		return mediaFormat;
	}

	/* AOSP MediaCodec.getOutputFormat(int): the format of a dequeued output buffer (IllegalStateException for an
	 * index the app does not hold); ATL's output buffers all have the codec's output format */
	public synchronized MediaFormat getOutputFormat(int index) {
		if (outputBuffers == null || index < 0 || index >= outputBuffers.length || freeOutputBuffers.contains(index))
			throw new IllegalStateException();
		return getOutputFormat();
	}

	public synchronized void flush() {
		// in asynchronous mode the codec waits for start() after a flush, as in AOSP
		pumping = false;
		native_flush(native_codec);
		freeInputBuffers.clear();
		for (int i = 0; i < inputBuffers.length; i++)
			freeInputBuffers.add(i);
		queuedInputBuffers.clear();
		freeOutputBuffers.clear();
		for (int i = 0; i < outputBuffers.length; i++)
			freeOutputBuffers.add(i);
		outputFormatSet = false;
	}

	private void tryProcessInputBuffer() {
		Integer index = queuedInputBuffers.peek();
		if (index != null) {
			int ret;
			if (index == -1) { // end of stream
				ret = native_queueInputBuffer(native_codec, null, 0);
			} else {
				ret = native_queueInputBuffer(native_codec, inputBuffers[index], inputBufferTimestamps[index]);
			}
			if (ret == 0) {
				queuedInputBuffers.remove(index);
				if (index != -1)
					freeInputBuffers.add(index);
			}
		}
	}

	public synchronized int dequeueInputBuffer(long timeoutUs) {
		tryProcessInputBuffer();
		Integer index = freeInputBuffers.poll();
		if (index == null) {
			return /*INFO_TRY_AGAIN_LATER*/ -1;
		} else {
			return index;
		}
	}

	/* AOSP MediaCodec.queueSecureInputBuffer(): ATL has no DRM (MediaDrm supports no scheme), so no codec
	 * decrypts; AOSP's MediaCodec refuses a secure buffer without crypto or descrambler with BAD_VALUE, which
	 * the JNI layer throws as IllegalArgumentException */
	public final void queueSecureInputBuffer(int index, int offset, CryptoInfo info, long presentationTimeUs, int flags) {
		throw new IllegalArgumentException("queuing secure buffer without mCrypto or mDescrambler!");
	}

	public synchronized void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
		if ((flags & BUFFER_FLAG_END_OF_STREAM) != 0) {
			queuedInputBuffers.add(-1);
			tryProcessInputBuffer();
			return;
		}
		inputBufferTimestamps[index] = presentationTimeUs;
		/* AOSP: the data is `size` bytes at `offset`, whatever the buffer's position and limit (Gecko writes into
		 * the buffer natively, leaving both untouched) */
		inputBuffers[index].limit(offset + size).position(offset);
		queuedInputBuffers.add(index);
		tryProcessInputBuffer();
	}

	/* AOSP: SCALE_TO_FIT (the default) stretches the frames over the Surface, SCALE_TO_FIT_WITH_CROPPING covers it
	 * keeping their aspect ratio; like AOSP's compositor this applies to a SurfaceView, a TextureView shows the
	 * whole frame */
	public void setVideoScalingMode(int mode) {
		if (mode != MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT && mode != MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
			throw new IllegalArgumentException();
		if (native_codec != 0)
			native_setVideoScalingMode(native_codec, mode == MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
	}

	public void stop() {
		stopPumping();
	}

	public synchronized void release() {
		System.out.println("MediaCodec.release(): codecName=" + codecName);
		stopPumping();
		if (native_codec != 0) {
			if (outputBuffers != null) {
				for (int i = 0; i < outputBuffers.length; i++) {
					if (!freeOutputBuffers.contains(i))
						releaseOutputBuffer(i, false);
				}
			}
			native_release(native_codec);
		}
		native_codec = 0;
	}

	/* AOSP MediaCodec.reset(): back to the Uninitialized state of a freshly created codec. The FFmpeg
	 * codec is released and created anew; if that fails, android_media_MediaCodec_reset() reports
	 * UNKNOWN_ERROR, which throwExceptionAsNecessary() throws as CodecException. */
	public final synchronized void reset() {
		release();
		inputBuffers = null;
		outputBuffers = null;
		mediaFormat = null;
		outputFormatSet = false;
		native_codec = native_constructor(codecName);
		if (native_codec == 0)
			throw new CodecException(0x80000000, "reset: failed to create " + codecName);
	}

	/* AOSP createPersistentInputSurface(): null when MediaCodec::CreatePersistentInputSurface() fails, as
	 * it does without an encoder (ATL has only decoders) */
	public static Surface createPersistentInputSurface() {
		return null;
	}

	/* AOSP setInputSurface(): only a PersistentSurface is accepted, and ATL never creates one */
	public void setInputSurface(Surface surface) {
		throw new IllegalArgumentException("not a PersistentSurface");
	}

	/* AOSP MediaCodec.setOutputSurface(): the video codec renders into another surface from now on (a player
	 * moves its video to another SurfaceView, e.g. into picture-in-picture, or to a TextureView's
	 * Surface(SurfaceTexture)) */
	public void setOutputSurface(Surface surface) {
		if (surface == null)
			throw new IllegalArgumentException("surface must not be null");
		if (native_codec != 0)
			native_setOutputSurface(native_codec, surface);
	}

	/* AOSP MediaCodec.detachOutputSurface(): no ATL codec can detach its surface (the AOSP
	 * nullOutputSurfaceSupport() case off), so it fails as there */
	public static final int CONFIGURE_FLAG_DETACHED_SURFACE = 8;
	private boolean mHasSurface = false;

	public void detachOutputSurface() {
		if (!mHasSurface)
			throw new IllegalStateException("codec was not configured for an output surface");
		throw new IllegalStateException("codec does not support detaching output surface");
	}

	public void setParameters(Bundle parameters) {}

	/* AOSP createInputSurface()/signalEndOfInputStream() are native and only valid on a video encoder;
	 * ATL provides decoders only (createEncoderByType() fails). The call is answered as an invalid
	 * operation, which media/jni/android_media_MediaCodec.cpp (throwExceptionAsNecessary) throws as
	 * IllegalStateException. */
	public final Surface createInputSurface() {
		throw new IllegalStateException("createInputSurface: not an encoder");
	}

	public final void signalEndOfInputStream() {
		throw new IllegalStateException("signalEndOfInputStream: not an encoder");
	}

	/* API 31. The FFmpeg codecs have no vendor parameters, so there is nothing to subscribe to. */
	public java.util.List<String> getSupportedVendorParameters() {
		return new java.util.ArrayList<String>();
	}

	public void subscribeToVendorParameters(java.util.List<String> names) {
		java.util.Objects.requireNonNull(names);
	}

	public void unsubscribeFromVendorParameters(java.util.List<String> names) {
		java.util.Objects.requireNonNull(names);
	}

	private OnFrameRenderedListener onFrameRenderedListener;

	/* Stored for parity; ATL produces no rendered output frames, so the callback is never invoked. */
	public void setOnFrameRenderedListener(OnFrameRenderedListener listener, android.os.Handler handler) {
		onFrameRenderedListener = listener;
	}

	/* No codec metrics are collected; AOSP may likewise return an empty/absent bundle. */
	public PersistableBundle getMetrics() {
		return new PersistableBundle();
	}

	/* AOSP createDecoderByType()/createEncoderByType(): new MediaCodec(type, true, encoder) instantiates the
	 * first codec of the list for the type; if there is none, MediaCodec::CreateByType() reports
	 * NAME_NOT_FOUND and android_media_MediaCodec_native_setup() throws IllegalArgumentException. ATL's
	 * list has only decoders, so no encoder is ever found. */
	public static MediaCodec createDecoderByType(String type) throws IOException {
		if (type == null)
			throw new NullPointerException();
		for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
			MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
			if (!info.isEncoder() && type.equalsIgnoreCase(info.getSupportedTypes()[0]))
				return new MediaCodec(info.getName());
		}
		throw new IllegalArgumentException("Failed to initialize " + type + ", error 0xfffffffe (NAME_NOT_FOUND)");
	}

	public static MediaCodec createEncoderByType(String type) throws IOException {
		if (type == null)
			throw new NullPointerException();
		throw new IllegalArgumentException("Failed to initialize " + type + ", error 0xfffffffe (NAME_NOT_FOUND)");
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			release();
		}
	}

	private native long native_constructor(String codecName);
	private native void native_setOutputSurface(long codec, Surface surface);
	private native void native_configure_audio(long codec, ByteBuffer extradata, int sampleRate, int channelCount);
	private native void native_configure_video(long codec, ByteBuffer csd0, ByteBuffer csd1, Surface surface, int rotation);
	private native void native_setVideoScalingMode(long codec, boolean crop);
	private native void native_start(long codec);
	private native int native_queueInputBuffer(long codec, ByteBuffer buffer, long presentationTimeUs);
	private native int native_dequeueOutputBuffer(long codec, ByteBuffer buffer, BufferInfo info);
	private native void native_releaseOutputBuffer(long codec, ByteBuffer buffer, boolean render, long releaseTimeNs);
	private native void native_flush(long codec);
	private native void native_release(long codec);

	public static final class CryptoInfo {
		public static final class Pattern {
			private int blocksToEncrypt;
			private int blocksToSkip;

			public Pattern(int blocksToEncrypt, int blocksToSkip) {
				this.blocksToEncrypt = blocksToEncrypt;
				this.blocksToSkip = blocksToSkip;
			}

			public void set(int blocksToEncrypt, int blocksToSkip) {
				this.blocksToEncrypt = blocksToEncrypt;
				this.blocksToSkip = blocksToSkip;
			}

			public int getEncryptBlocks() {
				return blocksToEncrypt;
			}

			public int getSkipBlocks() {
				return blocksToSkip;
			}
		}

		public int numSubSamples;
		public int[] numBytesOfClearData;
		public int[] numBytesOfEncryptedData;
		public byte[] key;
		public byte[] iv;
		public int mode;

		private static final Pattern ZERO_PATTERN = new Pattern(0, 0);
		private Pattern pattern = ZERO_PATTERN;

		public void set(int newNumSubSamples, int[] newNumBytesOfClearData, int[] newNumBytesOfEncryptedData,
		                byte[] newKey, byte[] newIV, int newMode) {
			numSubSamples = newNumSubSamples;
			numBytesOfClearData = newNumBytesOfClearData;
			numBytesOfEncryptedData = newNumBytesOfEncryptedData;
			key = newKey;
			iv = newIV;
			mode = newMode;
			pattern = ZERO_PATTERN;
		}

		public Pattern getPattern() {
			return new Pattern(pattern.getEncryptBlocks(), pattern.getSkipBlocks());
		}

		public void setPattern(Pattern newPattern) {
			if (newPattern == null)
				newPattern = ZERO_PATTERN;
			pattern = new Pattern(newPattern.getEncryptBlocks(), newPattern.getSkipBlocks());
		}
	}

	public static final class CryptoException extends RuntimeException implements MediaDrmThrowable {
		public CryptoException(int errorCode, String detailMessage) {
			this(detailMessage, errorCode, 0, 0, 0, null);
		}

		/* AOSP: @hide */
		CryptoException(String message, int errorCode, int vendorError, int oemError, int errorContext, CryptoInfo cryptoInfo) {
			super(message);
			mErrorCode = errorCode;
			mVendorError = vendorError;
			mOemError = oemError;
			mErrorContext = errorContext;
			mCryptoInfo = cryptoInfo;
		}

		public static final int ERROR_NO_KEY = MediaDrm.ErrorCodes.ERROR_NO_KEY;
		public static final int ERROR_KEY_EXPIRED = MediaDrm.ErrorCodes.ERROR_KEY_EXPIRED;
		public static final int ERROR_RESOURCE_BUSY = MediaDrm.ErrorCodes.ERROR_RESOURCE_BUSY;
		public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = MediaDrm.ErrorCodes.ERROR_INSUFFICIENT_OUTPUT_PROTECTION;
		public static final int ERROR_SESSION_NOT_OPENED = MediaDrm.ErrorCodes.ERROR_SESSION_NOT_OPENED;
		public static final int ERROR_UNSUPPORTED_OPERATION = MediaDrm.ErrorCodes.ERROR_UNSUPPORTED_OPERATION;
		public static final int ERROR_INSUFFICIENT_SECURITY = MediaDrm.ErrorCodes.ERROR_INSUFFICIENT_SECURITY;
		public static final int ERROR_FRAME_TOO_LARGE = MediaDrm.ErrorCodes.ERROR_FRAME_TOO_LARGE;
		public static final int ERROR_LOST_STATE = MediaDrm.ErrorCodes.ERROR_LOST_STATE;

		public int getErrorCode() {
			return mErrorCode;
		}

		@Override
		public int getVendorError() {
			return mVendorError;
		}

		@Override
		public int getOemError() {
			return mOemError;
		}

		@Override
		public int getErrorContext() {
			return mErrorContext;
		}

		public CryptoInfo getCryptoInfo() {
			return mCryptoInfo;
		}

		private final int mErrorCode, mVendorError, mOemError, mErrorContext;
		private final CryptoInfo mCryptoInfo;
	}

	public static final class BufferInfo {
		public int size;
		public int flags;
		public int offset;
		public long presentationTimeUs;

		public void set(int newOffset, int newSize, long newTimeUs, int newFlags) {
			offset = newOffset;
			size = newSize;
			presentationTimeUs = newTimeUs;
			flags = newFlags;
		}
	}

	public static interface OnFrameRenderedListener {
		void onFrameRendered(MediaCodec codec, long presentationTimeUs, long nanoTime);
	}

	public interface OnFirstTunnelFrameReadyListener {
		void onFirstTunnelFrameReady(MediaCodec codec);
	}

	private OnFirstTunnelFrameReadyListener onFirstTunnelFrameReadyListener;

	/* Stored for parity; ATL has no tunneled playback, so the callback is never invoked. */
	public void setOnFirstTunnelFrameReadyListener(android.os.Handler handler, OnFirstTunnelFrameReadyListener listener) {
		synchronized (mListenerLock) {
			onFirstTunnelFrameReadyListener = listener;
		}
	}

	/* AOSP MediaCodec.configure(MediaFormat, Surface, int, MediaDescrambler): a MediaDescrambler can only exist
	 * for a CAS plugin, and ATL has none (MediaDescrambler's constructor throws), so descrambler is always
	 * null here; AOSP then reduces to configure(format, surface, null crypto, flags). */
	public void configure(MediaFormat format, Surface surface, int flags, MediaDescrambler descrambler) {
		configure(format, surface, (MediaCrypto) null, flags);
	}

	/* AOSP MediaCodec.getParameterDescriptor(): null for a name the component does not know; ATL's codecs
	 * describe no parameters */
	public ParameterDescriptor getParameterDescriptor(String name) {
		java.util.Objects.requireNonNull(name);
		return null;
	}

	public static class ParameterDescriptor {
		private ParameterDescriptor() {
		}

		public String getName() {
			return mName;
		}

		public int getType() {
			return mType;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (!(o instanceof ParameterDescriptor)) {
				return false;
			}
			ParameterDescriptor other = (ParameterDescriptor) o;
			return this.mName.equals(other.mName) && this.mType == other.mType;
		}

		@Override
		public int hashCode() {
			return java.util.Arrays.asList((Object) mName, (Object) Integer.valueOf(mType)).hashCode();
		}

		private String mName;
		private int mType;
	}

	public class IncompatibleWithBlockModelException extends RuntimeException {
		IncompatibleWithBlockModelException() {
		}

		IncompatibleWithBlockModelException(String message) {
			super(message);
		}

		IncompatibleWithBlockModelException(String message, Throwable cause) {
			super(message, cause);
		}

		IncompatibleWithBlockModelException(Throwable cause) {
			super(cause);
		}
	}

	public class InvalidBufferFlagsException extends RuntimeException {
		InvalidBufferFlagsException(String message) {
			super(message);
		}
	}

	public static final class MetricsConstants {
		private MetricsConstants() {
		}

		public static final String CODEC = "android.media.mediacodec.codec";
		public static final String ENCODER = "android.media.mediacodec.encoder";
		public static final String HEIGHT = "android.media.mediacodec.height";
		public static final String MIME_TYPE = "android.media.mediacodec.mime";
		public static final String MODE = "android.media.mediacodec.mode";
		public static final String MODE_AUDIO = "audio";
		public static final String MODE_VIDEO = "video";
		public static final String ROTATION = "android.media.mediacodec.rotation";
		public static final String SECURE = "android.media.mediacodec.secure";
		public static final String WIDTH = "android.media.mediacodec.width";
	}

	/* AOSP MediaCodec.getCanonicalName(): the name of the underlying codec component; ATL's codecs are no aliases
	 * (MediaCodecInfo.isAlias()), so it is the name the codec was created with */
	public final String getCanonicalName() {
		return codecName;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.MediaCodec.OutputFrame getOutputFrame(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.Image getOutputImage(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public android.media.MediaCodec.QueueRequest getQueueRequest(int arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static android.media.Image mapHardwareBuffer(android.hardware.HardwareBuffer arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void queueInputBuffers(int arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void queueSecureInputBuffers(int arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1, java.util.ArrayDeque<android.media.MediaCodec.CryptoInfo> arg2) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setAudioPresentation(android.media.AudioPresentation arg0) {  }
}
