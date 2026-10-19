package android.media;

import android.media.MediaCodec.BufferInfo;
import android.os.Bundle;
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

	public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
		System.out.println("MediaCodec.configure(" + format + ", " + surface + ", " + crypto + ", " + flags + "): codecName=" + codecName);
		this.mediaFormat = format;

		int maxInputSize = 262144;
		if (format.containsKey("max-input-size")) {
			maxInputSize = format.getInteger("max-input-size");
		}
		inputBuffers = new ByteBuffer[1];
		inputBufferTimestamps = new long[inputBuffers.length];
		freeInputBuffers = new ArrayDeque<>(inputBuffers.length);
		queuedInputBuffers = new ArrayDeque<>(inputBuffers.length);
		for (int i = 0; i < inputBuffers.length; i++) {
			inputBuffers[i] = ByteBuffer.allocate(maxInputSize).order(ByteOrder.LITTLE_ENDIAN);
			freeInputBuffers.add(i);
		}
		outputBuffers = new ByteBuffer[4];
		freeOutputBuffers = new ArrayDeque<>(outputBuffers.length);
		for (int i = 0; i < outputBuffers.length; i++) {
			outputBuffers[i] = ByteBuffer.allocate(16384).order(ByteOrder.LITTLE_ENDIAN);
			freeOutputBuffers.add(i);
		}

		if ("aac".equals(codecName) || "mp3".equals(codecName) || "opus".equals(codecName)) {
			native_configure_audio(native_codec, format.getByteBuffer("csd-0"), format.getInteger("sample-rate"), format.getInteger("channel-count"));
		} else if ("h264".equals(codecName) || "vp8".equals(codecName) || "vp9".equals(codecName)) {
			native_configure_video(native_codec, format.getByteBuffer("csd-0"), format.getByteBuffer("csd-1"), surface);
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
			if (pumping)
				callbackHandler.postDelayed(this, 5);
		}
	};

	public ByteBuffer[] getInputBuffers() {
		return inputBuffers;
	}

	public ByteBuffer getInputBuffer(int index) {
		return inputBuffers[index];
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

	public MediaFormat getOutputFormat() {
		return mediaFormat;
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

	public synchronized void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
		if ((flags & BUFFER_FLAG_END_OF_STREAM) != 0) {
			queuedInputBuffers.add(-1);
			tryProcessInputBuffer();
			return;
		}
		inputBufferTimestamps[index] = presentationTimeUs;
		queuedInputBuffers.add(index);
		tryProcessInputBuffer();
	}

	public void setVideoScalingMode(int mode) {
		System.out.println("MediaCodec.setVideoScalingMode(" + mode + "): codecName=" + codecName);
	}

	public void stop() {
		pumping = false;
	}

	public synchronized void release() {
		System.out.println("MediaCodec.release(): codecName=" + codecName);
		pumping = false;
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

	public void setOutputSurface(Surface surface) {
	}

	public void setParameters(Bundle parameters) {}

	public static MediaCodec createEncoderByType(String type) throws IOException {
		System.out.println("MediaCodec.createEncoderByType(" + type + ")");
		throw new IOException("encoder not yet implemented");
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
	private native void native_configure_audio(long codec, ByteBuffer extradata, int sampleRate, int channelCount);
	private native void native_configure_video(long codec, ByteBuffer csd0, ByteBuffer csd1, Surface surface);
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

	public static interface OnFrameRenderedListener {}
}
