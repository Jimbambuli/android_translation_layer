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
	public static final int BUFFER_FLAG_SYNC_FRAME = 1;
	public static final int BUFFER_FLAG_KEY_FRAME = 1;
	public static final int BUFFER_FLAG_CODEC_CONFIG = 2;
	public static final int BUFFER_FLAG_PARTIAL_FRAME = 8;
	public static final int BUFFER_FLAG_DECODE_ONLY = 32;
	public static final int CONFIGURE_FLAG_ENCODE = 1;
	public static final int CONFIGURE_FLAG_USE_BLOCK_MODEL = 2;
	public static final int CONFIGURE_FLAG_USE_CRYPTO_ASYNC = 4;
	public static final int CRYPTO_MODE_UNENCRYPTED = 0;
	public static final int CRYPTO_MODE_AES_CTR = 1;
	public static final int CRYPTO_MODE_AES_CBC = 2;
	public static final int INFO_TRY_AGAIN_LATER = -1;
	public static final int INFO_OUTPUT_FORMAT_CHANGED = -2;
	public static final int INFO_OUTPUT_BUFFERS_CHANGED = -3;
	public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
	public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
	public static final String PARAMETER_KEY_VIDEO_BITRATE = "video-bitrate";
	public static final String PARAMETER_KEY_SUSPEND = "drop-input-frames";
	public static final String PARAMETER_KEY_SUSPEND_TIME = "drop-start-time-us";
	public static final String PARAMETER_KEY_OFFSET_TIME = "time-offset-us";
	public static final String PARAMETER_KEY_REQUEST_SYNC_FRAME = "request-sync";
	public static final String PARAMETER_KEY_HDR10_PLUS_INFO = MediaFormat.KEY_HDR10_PLUS_INFO;
	public static final String PARAMETER_KEY_LOW_LATENCY = MediaFormat.KEY_LOW_LATENCY;
	public static final String PARAMETER_KEY_TUNNEL_PEEK = "tunnel-peek";
	public static final String PARAMETER_KEY_QP_OFFSET_MAP = "qp-offset-map";
	public static final String PARAMETER_KEY_QP_OFFSET_RECTS = "qp-offset-rects";

	public static final int BUFFER_FLAG_END_OF_STREAM = 0x4;

	private String codecName;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private long[] inputBufferTimestamps;
	private long native_codec;
	private boolean outputFormatSet = false;
	private MediaFormat mediaFormat;

	private Queue<Integer> freeOutputBuffers;
	private Queue<Integer> readyOutputBuffers; // decoded, not yet dequeued by the app
	private BufferInfo[] outputInfos;
	private boolean outputEos;
	private Thread decodeThread;
	private boolean decoding;
	/* AOSP MediaCodec::setState() cancels a pending dequeue on every change of state (flush, stop, release) */
	private volatile int stateGeneration;
	private Queue<Integer> queuedInputBuffers;
	private Queue<Integer> freeInputBuffers;

	private final MediaCodecInfo info;

	/* a video encoder (MediaCodecInfo.isEncoder()): FFmpeg's encoder of video_encoder.c, opened by configure();
	 * native_codec is ENCODER for it, as the decoder's natives never see it */
	private static final long ENCODER = -1;
	private final boolean encoder;
	private long native_encoder;
	private MediaFormat outputFormat;
	private int encoderColorFormat, encoderStride, encoderSliceHeight;
	private byte[] codecConfig; // the codec-specific data, given out as first output buffer (BUFFER_FLAG_CODEC_CONFIG)
	private ImageReader inputReader; // the consumer of the input surface
	private boolean inputSurfaceEos, inputEosSent;
	private Image pendingImage; // taken from the input surface, not yet taken by the encoder (EAGAIN)

	/* AOSP native_setup(): a name MediaCodecList does not list fails with NAME_NOT_FOUND, an IllegalArgumentException;
	 * other failures are IOExceptions */
	private MediaCodec(String codecName) throws IOException {
		this.codecName = codecName;
		info = MediaCodecList.getInfoByName(codecName);
		if (info == null)
			throw new IllegalArgumentException("Failed to initialize " + codecName + ", error 0xfffffffe (NAME_NOT_FOUND)");
		encoder = info.isEncoder();
		native_codec = encoder ? ENCODER : native_constructor(info.getDecoder(), info.isHardwareAccelerated());
		if (native_codec == 0) {
			throw new IOException("Unable to create MediaCodec: " + codecName);
		}
	}

	/* AOSP C2SoftAvcDec/C2SoftHevcDec/C2SoftVpxDec/C2SoftDav1dDec MaxInputSizeSetter: room for a frame at a
	 * compression ratio of 2 of the largest picture (16x16 blocks of 192 bytes for AVC, 64x64 blocks of 3072
	 * bytes otherwise), at least 2 MiB */
	private static int recommendedInputSize(String mime, int maxWidth, int maxHeight) {
		long size = "video/avc".equalsIgnoreCase(mime) ? (long)((maxWidth + 15) / 16) * ((maxHeight + 15) / 16) * 192
		                                               : (long)((maxWidth + 63) / 64) * ((maxHeight + 63) / 64) * 3072;
		return (int)Math.min(Math.max(size, 2 * 1024 * 1024), Integer.MAX_VALUE);
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
		return info;
	}

	public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
		System.out.println("MediaCodec.configure(" + format + ", " + surface + ", " + crypto + ", " + flags + "): codecName=" + codecName);
		this.mediaFormat = format;
		mHasSurface = surface != null;
		String mime = info.getSupportedTypes()[0];
		if (encoder) {
			configureEncoder(format, mime);
			return;
		}

		int maxInputSize = 262144;
		if (format.containsKey("max-input-size")) {
			maxInputSize = format.getInteger("max-input-size");
		}
		/* AOSP CCodec::configure(): a video decoder's input buffers have the size its component recommends; the
		 * client's max-input-size only makes them larger */
		if (mime.startsWith("video/")) {
			int recommended = recommendedInputSize(mime, Math.max(format.getInteger(MediaFormat.KEY_WIDTH, 0), format.getInteger(MediaFormat.KEY_MAX_WIDTH, 0)),
			                                       Math.max(format.getInteger(MediaFormat.KEY_HEIGHT, 0), format.getInteger(MediaFormat.KEY_MAX_HEIGHT, 0)));
			if (!format.containsKey("max-input-size") || maxInputSize < recommended)
				maxInputSize = recommended;
		}
		/* AOSP CCodecBufferChannel::start(): numInputSlots = inputDelay + pipelineDelay + kSmoothnessFactor (4),
		 * numOutputSlots = outputDelay + kSmoothnessFactor, where the output delay of AOSP's video decoders is 8
		 * (C2SoftAvcDec, C2SoftHevcDec), 4 (C2SoftDav1dDec) or none (C2SoftVpxDec). A player like VLC holds
		 * decoded frames until their display time and needs these slots. */
		boolean video = mime.startsWith("video/");
		int outputDelay = !video ? 0 : (mime.equals("video/avc") || mime.equals("video/hevc")) ? 8 : mime.equals("video/av01") ? 4 : 0;
		inputBuffers = new ByteBuffer[video ? 4 : 1];
		inputBufferTimestamps = new long[inputBuffers.length];
		freeInputBuffers = new ArrayDeque<>(inputBuffers.length);
		queuedInputBuffers = new ArrayDeque<>(inputBuffers.length);
		for (int i = 0; i < inputBuffers.length; i++) {
			inputBuffers[i] = ByteBuffer.allocateDirect(maxInputSize).order(ByteOrder.LITTLE_ENDIAN);
			freeInputBuffers.add(i);
		}
		outputBuffers = new ByteBuffer[outputDelay + 4];
		freeOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		readyOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		outputEos = false;
		outputFormatSet = false; // AOSP: the first output after configure() and start() is INFO_OUTPUT_FORMAT_CHANGED
		outputInfos = new BufferInfo[outputBuffers.length];
		/* a video buffer carries a frame pointer; an audio buffer holds a decoded frame in PCM16, as large as the
		 * blocks AOSP's C2SoftOpusDec takes for every output (kMaxNumSamplesPerBuffer 960 * 6 frames of up to
		 * kMaxChannels 8), which also holds a 5.1 AC-3 or HE-AAC frame */
		int outputSize = video ? 16384 : 960 * 6 * 8 * 2;
		for (int i = 0; i < outputBuffers.length; i++) {
			outputBuffers[i] = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.LITTLE_ENDIAN);
			outputInfos[i] = new BufferInfo();
			freeOutputBuffers.add(i);
		}

		if (mime.startsWith("audio/")) {
			native_configure_audio(native_codec, format.getByteBuffer("csd-0"), format.getInteger("sample-rate"), format.getInteger("channel-count"));
		} else if (mime.startsWith("video/")) {
			/* the output format (here the configured one) names the color format, which Gecko requires; frames
			 * go to the Surface: COLOR_FormatSurface */
			if (surface != null && !format.containsKey("color-format"))
				format.setInteger("color-format", 0x7F000789);
			/* the frames the app holds come on top of the decoder's own; AOSP lets a Surface's consumer hold
			 * mRenderingDepth (3) more, an ImageReader its maxImages */
			int consumerFrames = surface != null && surface.atl_imageReader != null ? surface.atl_imageReader.getMaxImages() : 3;
			native_configure_video(native_codec, format.getByteBuffer("csd-0"), format.getByteBuffer("csd-1"), surface,
			                       format.getInteger("rotation-degrees", 0), outputBuffers.length + consumerFrames);
		} else {
			System.out.println("configure: format " + format + " not implemented");
			Thread.dumpStack();
			System.exit(1);
		}
	}

	private static int number(MediaFormat format, String key, int defaultValue) {
		Number n = format.getNumber(key);
		return n != null ? n.intValue() : defaultValue;
	}

	/* AOSP CCodec::configure() of a video encoder: size, frame rate, bit rate (mode), key frame interval and the
	 * input's color format (COLOR_FormatSurface: createInputSurface()/setInputSurface()); the output format has the
	 * codec-specific data (AVC: csd-0 SPS, csd-1 PPS; HEVC: csd-0 VPS+SPS+PPS), which also comes as the first output
	 * buffer with BUFFER_FLAG_CODEC_CONFIG */
	private void configureEncoder(MediaFormat format, String mime) {
		int width = number(format, MediaFormat.KEY_WIDTH, 0), height = number(format, MediaFormat.KEY_HEIGHT, 0);
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException("encoder needs " + MediaFormat.KEY_WIDTH + " and " + MediaFormat.KEY_HEIGHT);
		Number interval = format.getNumber(MediaFormat.KEY_I_FRAME_INTERVAL);
		if (native_encoder != 0)
			native_encoderFree(native_encoder);
		native_encoder = native_encoderOpen(info.getDecoder(), width, height, number(format, MediaFormat.KEY_FRAME_RATE, 30),
		                                    number(format, MediaFormat.KEY_BIT_RATE, 0), number(format, MediaFormat.KEY_BITRATE_MODE, 1),
		                                    interval != null ? interval.floatValue() : 1);
		if (native_encoder == 0)
			throw new CodecException(0x80000000, "configure: " + codecName + " can't encode " + width + "x" + height);
		encoderColorFormat = number(format, MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
		encoderStride = number(format, MediaFormat.KEY_STRIDE, width);
		encoderSliceHeight = number(format, MediaFormat.KEY_SLICE_HEIGHT, height);
		boolean surfaceInput = encoderColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
		inputBuffers = new ByteBuffer[surfaceInput ? 0 : 4];
		inputBufferTimestamps = new long[inputBuffers.length];
		freeInputBuffers = new ArrayDeque<>(inputBuffers.length);
		queuedInputBuffers = new ArrayDeque<>(inputBuffers.length);
		for (int i = 0; i < inputBuffers.length; i++) {
			inputBuffers[i] = ByteBuffer.allocateDirect(Math.max(encoderStride, width) * Math.max(encoderSliceHeight, height) * 3 / 2);
			freeInputBuffers.add(i);
		}
		outputBuffers = new ByteBuffer[8];
		freeOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		readyOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		outputInfos = new BufferInfo[outputBuffers.length];
		for (int i = 0; i < outputBuffers.length; i++) {
			outputBuffers[i] = ByteBuffer.allocateDirect(Math.max(width * height * 3 / 2, 1 << 20));
			outputInfos[i] = new BufferInfo();
			freeOutputBuffers.add(i);
		}
		outputEos = false;
		outputFormatSet = false;
		inputSurfaceEos = inputEosSent = false;
		outputFormat = new MediaFormat();
		outputFormat.setString(MediaFormat.KEY_MIME, mime);
		outputFormat.setInteger(MediaFormat.KEY_WIDTH, width);
		outputFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
		if (format.containsKey(MediaFormat.KEY_BIT_RATE))
			outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, number(format, MediaFormat.KEY_BIT_RATE, 0));
		if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
			outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, number(format, MediaFormat.KEY_FRAME_RATE, 0));
		codecConfig = native_encoderExtradata(native_encoder);
		if (codecConfig != null) {
			if (mime.equals(MediaFormat.MIMETYPE_VIDEO_AVC)) {
				/* AOSP: csd-0 the SPS, csd-1 the PPS, each with its start code */
				int pps = ppsStart(codecConfig);
				outputFormat.setByteBuffer("csd-0", ByteBuffer.wrap(java.util.Arrays.copyOfRange(codecConfig, 0, pps)));
				if (pps < codecConfig.length)
					outputFormat.setByteBuffer("csd-1", ByteBuffer.wrap(java.util.Arrays.copyOfRange(codecConfig, pps, codecConfig.length)));
			} else {
				outputFormat.setByteBuffer("csd-0", ByteBuffer.wrap(codecConfig.clone()));
			}
		}
	}

	/* the offset of the first NAL unit after the SPS (Annex B start codes) */
	private static int ppsStart(byte[] annexB) {
		for (int i = 4; i + 3 < annexB.length; i++)
			if (annexB[i] == 0 && annexB[i + 1] == 0 && annexB[i + 2] == 1 && (annexB[i + 3] & 0x1f) != 7)
				return (i > 0 && annexB[i - 1] == 0) ? i - 1 : i;
		return annexB.length;
	}

	/* one step of an encoder: an image of the input surface (or a queued ByteBuffer) into the encoder, a packet into a
	 * free output slot */
	private boolean encodeStep() {
		boolean progress = false;
		if (inputReader != null) {
			/* AOSP GraphicBufferSource: a frame stays queued until the encoder takes it */
			Image image = pendingImage != null ? pendingImage : inputReader.acquireNextImageNoThrowISE();
			pendingImage = null;
			if (image != null) {
				long buffer = ImageReader.atl_hardwareBuffer(image);
				Image.Plane[] planes = buffer == 0 ? ImageReader.atl_queuedPlanes(image) : null;
				int n = planes != null ? planes.length : 0;
				ByteBuffer[] data = new ByteBuffer[n];
				int[] rowStrides = new int[n], pixelStrides = new int[n];
				for (int i = 0; i < n; i++) {
					data[i] = planes[i].getBuffer();
					rowStrides[i] = planes[i].getRowStride();
					pixelStrides[i] = planes[i].getPixelStride();
				}
				if (native_encoderSendImage(native_encoder, buffer, data, rowStrides, pixelStrides, image.getWidth(), image.getHeight(), image.getTimestamp() / 1000) == AVERROR_EAGAIN) {
					pendingImage = image;
				} else {
					image.close();
					progress = true;
				}
			} else if (inputSurfaceEos && !inputEosSent) {
				inputEosSent = native_encoderSendBuffer(native_encoder, null, 0, 0, 0, 0, 0, 0) != AVERROR_EAGAIN;
				progress = inputEosSent;
			}
		} else {
			int queued = queuedInputBuffers.size();
			tryProcessInputBuffer();
			progress = queuedInputBuffers.size() != queued;
		}
		Integer index = freeOutputBuffers.peek();
		if (index != null && !outputEos) {
			ByteBuffer out = outputBuffers[index];
			out.clear();
			boolean filled = false;
			if (codecConfig != null) {
				out.put(codecConfig).flip();
				outputInfos[index].set(0, codecConfig.length, 0, BUFFER_FLAG_CODEC_CONFIG);
				codecConfig = null;
				filled = true;
			} else if (native_encoderReceive(native_encoder, out, outputInfos[index]) == 0) {
				out.limit(outputInfos[index].size);
				filled = true;
			}
			if (filled) {
				freeOutputBuffers.remove();
				readyOutputBuffers.add(index);
				outputEos = (outputInfos[index].flags & BUFFER_FLAG_END_OF_STREAM) != 0;
				progress = true;
			}
		}
		return progress;
	}

	public void start() {
		System.out.println("MediaCodec.start(): codecName=" + codecName);
		if (encoder && native_encoder == 0)
			throw new IllegalStateException("start() before configure()");
		if (!encoder)
			native_start(native_codec);
		startDecoding();
		if (callback != null && !pumping) {
			pumping = true;
			pump = new Pump(stateGeneration);
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
		public static final int ERROR_INSUFFICIENT_RESOURCE = 1100;
		public static final int ERROR_RECLAIMED = 1101;

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
			if (callbackHandler != null && pump != null)
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

	/* AOSP MediaCodec.cpp: a flushed asynchronous codec offers no buffers until start() (kWhatStart in FLUSHED state
	 * calls onInputBufferAvailable()), so its callbacks reach the handler only after start(). Each start() has a pump
	 * of its own, which ends with the next flush(), stop() or reset(); a pump of before the flush that is still queued
	 * on the handler offers nothing (Chrome drops the buffers offered before the message it posts ahead of start()). */
	private volatile Pump pump;

	private final class Pump implements Runnable {
		private final int generation;

		Pump(int generation) {
			this.generation = generation;
		}

		@Override
		public void run() {
			if (!pumping)
				return;
			int index;
			while (pumping && (index = pumpBuffer(generation, null)) >= 0)
				callback.onInputBufferAvailable(MediaCodec.this, index);
			while (pumping) {
				BufferInfo info = new BufferInfo();
				index = pumpBuffer(generation, info);
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
				if (pumping && generation == stateGeneration && !callbackHandler.getLooper().getQueue().isQuitting())
					callbackHandler.postDelayed(this, 5);
			}
		}
	}

	/* a free input buffer (info null) or a decoded output buffer for the pump of this generation, -1 for an older one */
	private synchronized int pumpBuffer(int generation, BufferInfo info) {
		if (generation != stateGeneration || native_codec == 0)
			return /*INFO_TRY_AGAIN_LATER*/ -1;
		return info == null ? tryDequeueInputBuffer() : tryDequeueOutputBuffer(info);
	}

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

	/* AOSP dequeueInputBuffer()/dequeueOutputBuffer(): wait up to timeoutUs (without end if negative) for a
	 * buffer. ATL's FFmpeg codec only works inside these calls, so the wait repeats the attempt; queuing input or
	 * releasing output on another thread wakes it early. A codec released meanwhile is IllegalStateException. */
	private interface Attempt {
		int run();
	}

	private synchronized int waitForBuffer(long timeoutUs, Attempt attempt) {
		long deadline = System.nanoTime() + timeoutUs * 1000;
		int generation = stateGeneration;
		while (true) {
			/* AOSP cancelPendingDequeueOperations(): INVALID_OPERATION, which the JNI layer throws as
			 * IllegalStateException */
			if (native_codec == 0 || generation != stateGeneration)
				throw new IllegalStateException();
			int ret = attempt.run();
			if (ret != /*INFO_TRY_AGAIN_LATER*/ -1 || timeoutUs == 0)
				return ret;
			long waitNs = timeoutUs < 0 ? 10000000 : Math.min(deadline - System.nanoTime(), 10000000);
			if (waitNs <= 0)
				return ret;
			try {
				wait(waitNs / 1000000, (int)(waitNs % 1000000));
			} catch (InterruptedException e) {
				return ret;
			}
		}
	}

	/* AOSP CCodec: the component decodes on threads of its own as long as it has input and free output slots, and
	 * the app only takes the finished output buffers. ATL's FFmpeg codec works on this thread of the codec, from
	 * start() until stop(), release() or reset(), each step under the codec's lock. */
	private synchronized void startDecoding() {
		if (decodeThread != null)
			return;
		decoding = true;
		decodeThread = new Thread(() -> {
			while (decodeStep())
				;
		}, "MediaCodec " + codecName);
		decodeThread.setDaemon(true);
		decodeThread.start();
	}

	private void stopDecoding() {
		Thread thread;
		synchronized (this) {
			stateGeneration++;
			decoding = false;
			thread = decodeThread;
			decodeThread = null;
			notifyAll();
		}
		if (thread != null && thread != Thread.currentThread()) {
			boolean interrupted = false;
			while (thread.isAlive()) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	/* one step: hand a queued input buffer to the codec, take a decoded frame into a free output slot; without
	 * progress wait until the app queues input or releases output */
	private synchronized boolean decodeStep() {
		if (!decoding || native_codec == 0)
			return false;
		boolean progress = false;
		Integer index = null;
		if (encoder) {
			progress = encodeStep();
		} else {
			int queued = queuedInputBuffers.size();
			tryProcessInputBuffer();
			progress = queuedInputBuffers.size() != queued;
			index = freeOutputBuffers.peek();
		}
		if (index != null && !outputEos) {
			outputBuffers[index].clear();
			if (native_dequeueOutputBuffer(native_codec, outputBuffers[index], outputInfos[index]) == 0) {
				freeOutputBuffers.remove();
				readyOutputBuffers.add(index);
				outputEos = (outputInfos[index].flags & BUFFER_FLAG_END_OF_STREAM) != 0;
				progress = true;
			}
		}
		if (progress) {
			notifyAll();
		} else {
			try {
				wait(10);
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}

	public int dequeueOutputBuffer(BufferInfo info, long timeoutUs) {
		return waitForBuffer(timeoutUs, () -> tryDequeueOutputBuffer(info));
	}

	private int tryDequeueOutputBuffer(BufferInfo info) {
		if (!outputFormatSet) {
			outputFormatSet = true;
			return /*INFO_OUTPUT_FORMAT_CHANGED*/ -2;
		}
		Integer index = readyOutputBuffers.poll();
		if (index == null)
			return /*INFO_TRY_AGAIN_LATER*/ -1;
		BufferInfo ready = outputInfos[index];
		info.set(ready.offset, ready.size, ready.presentationTimeUs, ready.flags);
		return index;
	}

	/* AOSP MediaCodec::onReleaseOutputBuffer(): -EACCES for an index the app does not hold */
	private void checkHeldOutput(int index) {
		if (outputBuffers == null || index < 0 || index >= outputBuffers.length || freeOutputBuffers.contains(index) || readyOutputBuffers.contains(index))
			throw new IllegalStateException("output buffer " + index + " is not held by the app");
	}

	public synchronized void releaseOutputBuffer(int index, boolean render) {
		checkHeldOutput(index);
		notifyAll();
		if (!encoder)
			native_releaseOutputBuffer(native_codec, outputBuffers[index], render, 0);
		freeOutputBuffers.add(index);
	}

	// AOSP calls the parameter `presentationTimeUs`, but actually it is nanoseconds
	public synchronized void releaseOutputBuffer(int index, long releaseTimeNs) {
		checkHeldOutput(index);
		notifyAll();
		if (!encoder)
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
		return encoder ? outputFormat : mediaFormat;
	}

	/* AOSP MediaCodec.getOutputFormat(int): the format of a dequeued output buffer (IllegalStateException for an
	 * index the app does not hold); ATL's output buffers all have the codec's output format */
	public synchronized MediaFormat getOutputFormat(int index) {
		checkHeldOutput(index);
		return getOutputFormat();
	}

	public synchronized void flush() {
		stateGeneration++;
		notifyAll();
		// in asynchronous mode the codec waits for start() after a flush, as in AOSP
		pumping = false;
		/* AOSP: a flush returns all buffers to the codec; the frames in them are dropped */
		releaseAllOutput();
		if (!encoder)
			native_flush(native_codec);
		freeInputBuffers.clear();
		for (int i = 0; i < inputBuffers.length; i++)
			freeInputBuffers.add(i);
		queuedInputBuffers.clear();
		freeOutputBuffers.clear();
		for (int i = 0; i < outputBuffers.length; i++)
			freeOutputBuffers.add(i);
		outputEos = false;
		/* the output format stays (AOSP reports INFO_OUTPUT_FORMAT_CHANGED again only after stop() or a change) */
	}

	private void releaseAllOutput() {
		if (outputBuffers == null)
			return;
		for (int i = 0; i < outputBuffers.length; i++) {
			if (!encoder && !freeOutputBuffers.contains(i))
				native_releaseOutputBuffer(native_codec, outputBuffers[i], false, 0);
		}
		readyOutputBuffers.clear();
	}

	private static final int AVERROR_EAGAIN = -11; // AVERROR(EAGAIN) on Linux

	private void tryProcessInputBuffer() {
		Integer index = queuedInputBuffers.peek();
		if (index != null) {
			int ret;
			if (encoder) {
				ByteBuffer buffer = index == -1 ? null : inputBuffers[index];
				ret = native_encoderSendBuffer(native_encoder, buffer, buffer != null ? buffer.position() : 0, buffer != null ? buffer.remaining() : 0,
				                               encoderColorFormat, encoderStride, encoderSliceHeight, index == -1 ? 0 : inputBufferTimestamps[index]);
			} else if (index == -1) { // end of stream
				ret = native_queueInputBuffer(native_codec, null, 0);
			} else {
				ret = native_queueInputBuffer(native_codec, inputBuffers[index], inputBufferTimestamps[index]);
			}
			/* the codec takes the buffer unless it is full (EAGAIN); like AOSP's decoders it also takes one it cannot
			 * decode (FFmpeg's H.264 decoder refuses a codec-config buffer with only SPS/PPS as "no frame", but keeps
			 * the parameter sets), instead of offering it again for ever */
			if (ret != AVERROR_EAGAIN) {
				queuedInputBuffers.remove(index);
				if (index != -1)
					freeInputBuffers.add(index);
			}
		}
	}

	public int dequeueInputBuffer(long timeoutUs) {
		return waitForBuffer(timeoutUs, this::tryDequeueInputBuffer);
	}

	private int tryDequeueInputBuffer() {
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
		notifyAll();
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
		if (native_codec != 0 && !encoder)
			native_setVideoScalingMode(native_codec, mode == MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
	}

	public void stop() {
		stopPumping();
		stopDecoding();
	}

	/* not synchronized: the codec's thread finishes its step under the lock before it ends */
	public void release() {
		System.out.println("MediaCodec.release(): codecName=" + codecName);
		stopPumping();
		stopDecoding();
		synchronized (this) {
			if (native_codec != 0) {
				releaseAllOutput();
				if (!encoder)
					native_release(native_codec);
			}
			if (native_encoder != 0)
				native_encoderFree(native_encoder);
			native_encoder = 0;
			if (pendingImage != null)
				pendingImage.close();
			pendingImage = null;
			if (inputReader != null && !(inputReader.getSurface() instanceof PersistentSurface))
				inputReader.close();
			inputReader = null;
			native_codec = 0;
			notifyAll();
		}
	}

	/* AOSP MediaCodec.reset(): back to the Uninitialized state of a freshly created codec. The FFmpeg
	 * codec is released and created anew; if that fails, android_media_MediaCodec_reset() reports
	 * UNKNOWN_ERROR, which throwExceptionAsNecessary() throws as CodecException. */
	public final void reset() {
		release();
		synchronized (this) {
			resetState();
		}
	}

	private void resetState() {
		inputBuffers = null;
		outputBuffers = null;
		mediaFormat = null;
		outputFormatSet = false;
		outputFormat = null;
		native_codec = encoder ? ENCODER : native_constructor(info.getDecoder(), info.isHardwareAccelerated());
		if (native_codec == 0)
			throw new CodecException(0x80000000, "reset: failed to create " + codecName);
	}

	/* AOSP MediaCodec.PersistentSurface: an input surface that outlives the encoders it is given to with
	 * setInputSurface() (ATL: the Surface whose ImageReader the encoder consumes) */
	static class PersistentSurface extends Surface {
		PersistentSurface() {}
	}

	public static Surface createPersistentInputSurface() {
		return new PersistentSurface();
	}

	/* AOSP setInputSurface(): after configure() and before start(), only a PersistentSurface; the encoder
	 * consumes what is queued to it from now on */
	public void setInputSurface(Surface surface) {
		if (!(surface instanceof PersistentSurface))
			throw new IllegalArgumentException("not a PersistentSurface");
		inputReader = ImageReader.atl_newEncoderInput(surface, outputFormat.getInteger(MediaFormat.KEY_WIDTH), outputFormat.getInteger(MediaFormat.KEY_HEIGHT));
	}

	/* AOSP MediaCodec.setOutputSurface(): the video codec renders into another surface from now on (a player
	 * moves its video to another SurfaceView, e.g. into picture-in-picture, or to a TextureView's
	 * Surface(SurfaceTexture)) */
	public void setOutputSurface(Surface surface) {
		if (surface == null)
			throw new IllegalArgumentException("surface must not be null");
		if (native_codec != 0 && !encoder)
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

	/* AOSP setParameters(): an encoder makes the next frame a key frame on PARAMETER_KEY_REQUEST_SYNC_FRAME; the
	 * other parameters (a new bit rate, suspending) are not applied by ATL's encoders */
	public synchronized void setParameters(Bundle parameters) {
		if (encoder && native_encoder != 0 && parameters != null && parameters.containsKey(PARAMETER_KEY_REQUEST_SYNC_FRAME))
			native_encoderRequestSyncFrame(native_encoder);
	}

	/* AOSP createInputSurface(): after configure() and before start(), only for a video encoder configured with
	 * COLOR_FormatSurface; otherwise INVALID_OPERATION, which throwExceptionAsNecessary() throws as
	 * IllegalStateException. The frames queued to the Surface (EGL, the camera, a decoder, lockCanvas()) are encoded with
	 * their timestamps. */
	public final Surface createInputSurface() {
		if (!encoder || native_encoder == 0 || encoderColorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
			throw new IllegalStateException("createInputSurface: not an encoder configured for surface input");
		inputReader = ImageReader.atl_newEncoderInput(null, outputFormat.getInteger(MediaFormat.KEY_WIDTH), outputFormat.getInteger(MediaFormat.KEY_HEIGHT));
		return inputReader.getSurface();
	}

	/* AOSP: the end of the input surface's stream: once the frames queued so far are encoded, the last output buffer
	 * has BUFFER_FLAG_END_OF_STREAM */
	public final synchronized void signalEndOfInputStream() {
		if (inputReader == null)
			throw new IllegalStateException("signalEndOfInputStream: no input surface");
		inputSurfaceEos = true;
		notifyAll();
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
	 * NAME_NOT_FOUND and android_media_MediaCodec_native_setup() throws IllegalArgumentException. */
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
		for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
			MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
			if (info.isEncoder() && type.equalsIgnoreCase(info.getSupportedTypes()[0]))
				return new MediaCodec(info.getName());
		}
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

	private native long native_constructor(String decoder, boolean hardware);
	static native String[] native_encoders();
	private static native long native_encoderOpen(String name, int width, int height, int frameRate, int bitRate, int bitrateMode, float iFrameInterval);
	private static native byte[] native_encoderExtradata(long encoder);
	private static native int native_encoderSendBuffer(long encoder, ByteBuffer buffer, int offset, int size, int colorFormat, int stride, int sliceHeight, long presentationTimeUs);
	private static native int native_encoderSendImage(long encoder, long hardwareBuffer, ByteBuffer[] planes, int[] rowStrides, int[] pixelStrides, int width, int height, long presentationTimeUs);
	private static native int native_encoderReceive(long encoder, ByteBuffer buffer, BufferInfo info);
	private static native void native_encoderRequestSyncFrame(long encoder);
	private static native void native_encoderFree(long encoder);
	static native int[] native_hardwareProfiles();
	private native void native_setOutputSurface(long codec, Surface surface);
	private native void native_configure_audio(long codec, ByteBuffer extradata, int sampleRate, int channelCount);
	private native void native_configure_video(long codec, ByteBuffer csd0, ByteBuffer csd1, Surface surface, int rotation, int heldFrames);
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
    public android.media.MediaCodec.QueueRequest getQueueRequest(int arg0) {
        /* AOSP: only a codec configured with CONFIGURE_FLAG_USE_BLOCK_MODEL has queue requests; ATL has none */
        throw new IllegalStateException("The codec is not configured for block model");
    }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public static android.media.Image mapHardwareBuffer(android.hardware.HardwareBuffer arg0) { return null; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void queueInputBuffers(int arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void queueSecureInputBuffers(int arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1, java.util.ArrayDeque<android.media.MediaCodec.CryptoInfo> arg2) {  }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void setAudioPresentation(android.media.AudioPresentation arg0) {  }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final class LinearBlock {
        protected void finalize() {  }
        public static boolean isCodecCopyFreeCompatible(String[] arg0) { return false; }
        public boolean isMappable() { return false; }
        public java.nio.ByteBuffer map() { return null; }
        public static android.media.MediaCodec.LinearBlock obtain(int arg0, String[] arg1) { return null; }
        public void recycle() {  }
    }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public static final class OutputFrame {
        public java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> getBufferInfos() { return null; }
        public java.util.Set<java.lang.String> getChangedKeys() { return null; }
        public int getFlags() { return 0; }
        public android.media.MediaFormat getFormat() { return null; }
        public android.hardware.HardwareBuffer getHardwareBuffer() { return null; }
        public android.media.MediaCodec.LinearBlock getLinearBlock() { return null; }
        public long getPresentationTimeUs() { return 0L; }
    }

    /* AOSP 16 API; no backing hardware/service in ATL */
    public final class QueueRequest {
        public void queue() {  }
        public android.media.MediaCodec.QueueRequest setByteBufferParameter(String arg0, java.nio.ByteBuffer arg1) { return this; }
        public android.media.MediaCodec.QueueRequest setEncryptedLinearBlock(android.media.MediaCodec.LinearBlock arg0, int arg1, int arg2, android.media.MediaCodec.CryptoInfo arg3) { return this; }
        public android.media.MediaCodec.QueueRequest setFlags(int arg0) { return this; }
        public android.media.MediaCodec.QueueRequest setFloatParameter(String arg0, float arg1) { return this; }
        public android.media.MediaCodec.QueueRequest setHardwareBuffer(android.hardware.HardwareBuffer arg0) { return this; }
        public android.media.MediaCodec.QueueRequest setIntegerParameter(String arg0, int arg1) { return this; }
        public android.media.MediaCodec.QueueRequest setLinearBlock(android.media.MediaCodec.LinearBlock arg0, int arg1, int arg2) { return this; }
        public android.media.MediaCodec.QueueRequest setLongParameter(String arg0, long arg1) { return this; }
        public android.media.MediaCodec.QueueRequest setMultiFrameEncryptedLinearBlock(android.media.MediaCodec.LinearBlock arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1, java.util.ArrayDeque<android.media.MediaCodec.CryptoInfo> arg2) { return this; }
        public android.media.MediaCodec.QueueRequest setMultiFrameLinearBlock(android.media.MediaCodec.LinearBlock arg0, java.util.ArrayDeque<android.media.MediaCodec.BufferInfo> arg1) { return this; }
        public android.media.MediaCodec.QueueRequest setPresentationTimeUs(long arg0) { return this; }
        public android.media.MediaCodec.QueueRequest setStringParameter(String arg0, String arg1) { return this; }
    }
}
