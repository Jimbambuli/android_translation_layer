/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.graphics;

import android.compat.annotation.UnsupportedAppUsage;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A movie of the Android framework (deprecated since API 28: use AnimatedImageDrawable).
 *
 * As in AOSP (libs/hwui/jni/Movie.cpp, GIFMovie.cpp, android-14) only a GIF is a movie, anything else decodes to
 * null. The frames are composed as GIFMovie does: the delays of the frames (hundredths of a second) add up to
 * duration(), setTime() picks the first frame whose end is not before the time, the frames up to it are drawn onto
 * the canvas in order with the disposal methods "restore to background" and "restore to previous".
 */
@Deprecated
public class Movie {
	private static final class Frame {
		int left, top, width, height;
		int[] colors; /* the colour table of the frame, ARGB; null: none */
		byte[] pixels; /* width * height indices into the table, rows in image order */
		int delay; /* milliseconds */
		boolean transparent;
		int transparentIndex = -1;
		int disposal;
	}

	private final int mWidth, mHeight;
	private final int mBackground; /* ARGB of the logical screen's background colour, 0: none */
	private final Frame[] mFrames;
	private final int mDuration;
	private int mCurrTime = 0; /* AOSP: fCurrTime */
	private int mCurrIndex = -1;
	private int mLastDrawIndex = -1;
	private int[] mCanvas; /* the composed image */
	private int[] mBackup; /* for "restore to previous" */
	private int mPaintingColor;
	private Bitmap mBitmap;

	private Movie(int width, int height, int background, Frame[] frames) {
		mWidth = width;
		mHeight = height;
		mBackground = background;
		mFrames = frames;
		int duration = 0;
		for (Frame f : frames)
			duration += f.delay;
		mDuration = duration;
	}

	/** Return the width of the movie */
	public int width() {
		return mWidth;
	}

	/** Return the height of the movie */
	public int height() {
		return mHeight;
	}

	/** Return true if the movie is opaque, which AOSP does not know for a GIF ("how to compute?") */
	public boolean isOpaque() {
		return false;
	}

	/** Return the duration of the movie in milliseconds */
	public int duration() {
		return mDuration;
	}

	/**
	 * Set the movie's time, in milliseconds within the duration (a larger time is the duration). Returns true if
	 * the time changed the frame to draw.
	 */
	public boolean setTime(int relativeMilliseconds) {
		/* SkMovie::setTime() */
		int time = Math.min(relativeMilliseconds, mDuration);
		if (time == mCurrTime)
			return false;
		mCurrTime = time;
		/* GIFMovie::onSetTime(): the first frame whose end is at or after the time */
		int dur = 0;
		for (int i = 0; i < mFrames.length; i++) {
			dur += mFrames[i].delay;
			if (dur >= time) {
				mCurrIndex = i;
				return mLastDrawIndex != mCurrIndex;
			}
		}
		mCurrIndex = mFrames.length - 1;
		return true;
	}

	/** Draw the movie at (x, y), with the paint, if any */
	public void draw(Canvas canvas, float x, float y, Paint paint) {
		Bitmap bitmap = getBitmap();
		if (bitmap != null)
			canvas.drawBitmap(bitmap, x, y, paint);
	}

	/** Draw the movie at (x, y) */
	public void draw(Canvas canvas, float x, float y) {
		draw(canvas, x, y, null);
	}

	/* GIFMovie::onGetBitmap() */
	private Bitmap getBitmap() {
		if (mLastDrawIndex >= 0 && mLastDrawIndex == mCurrIndex && mBitmap != null)
			return mBitmap;
		compose();
		if (mBitmap == null)
			mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mBitmap.setPixels(mCanvas, 0, mWidth, 0, 0, mWidth, mHeight);
		return mBitmap;
	}

	/* the frames up to the current one, composed in mCanvas */
	private void compose() {
		int startIndex = mLastDrawIndex + 1;
		if (mLastDrawIndex < 0 || mCanvas == null) {
			startIndex = 0;
			mCanvas = new int[mWidth * mHeight];
			mBackup = new int[mWidth * mHeight];
		} else if (startIndex > mCurrIndex) {
			startIndex = 0; /* rewind to the first frame for the repeat */
		}
		int lastIndex = mCurrIndex;
		if (lastIndex < 0)
			lastIndex = 0;
		else if (lastIndex > mFrames.length - 1)
			lastIndex = mFrames.length - 1;

		for (int i = startIndex; i <= lastIndex; i++) {
			Frame cur = mFrames[i];
			if (i == 0) {
				mPaintingColor = !cur.transparent && mBackground != 0 ? mBackground : 0;
				java.util.Arrays.fill(mCanvas, mPaintingColor);
				java.util.Arrays.fill(mBackup, mPaintingColor);
			} else {
				disposeFrameIfNeeded(mFrames[i - 1], cur);
			}
			/* a frame that the next one clears anyway is not drawn, unless it is the last */
			if (i == lastIndex || !(cur.disposal == 2 || cur.disposal == 3))
				drawFrame(cur);
		}
		mLastDrawIndex = lastIndex;
	}

	private static boolean covers(Frame target, Frame covered) {
		return target.left <= covered.left && covered.left + covered.width <= target.left + target.width
			&& target.top <= covered.top && covered.top + covered.height <= target.top + target.height;
	}

	private void disposeFrameIfNeeded(Frame cur, Frame next) {
		if ((cur.disposal == 2 || cur.disposal == 3) && (next.transparent || !covers(next, cur))) {
			if (cur.disposal == 2) {
				/* restore to the background colour; AOSP fills with memset(), which repeats the low byte of the colour */
				int b = mPaintingColor & 0xFF;
				int fill = b | b << 8 | b << 16 | b << 24;
				int w = Math.min(cur.width, mWidth - cur.left);
				int h = Math.min(cur.height, mHeight - cur.top);
				for (int y = 0; y < h; y++)
					java.util.Arrays.fill(mCanvas, (cur.top + y) * mWidth + cur.left, (cur.top + y) * mWidth + cur.left + Math.max(w, 0), fill);
			} else {
				int[] t = mCanvas;
				mCanvas = mBackup;
				mBackup = t;
			}
		}
		/* save the image if the disposal of the next frame is "restore to previous" */
		if (next.disposal == 3)
			System.arraycopy(mCanvas, 0, mBackup, 0, mCanvas.length);
	}

	private void drawFrame(Frame f) {
		if (f.colors == null)
			return;
		int w = Math.min(f.width, mWidth - f.left);
		int h = Math.min(f.height, mHeight - f.top);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int index = f.pixels[y * f.width + x] & 0xFF;
				if (index != f.transparentIndex && index < f.colors.length)
					mCanvas[(f.top + y) * mWidth + f.left + x] = f.colors[index];
			}
		}
	}

	public static Movie decodeStream(InputStream is) {
		if (is == null) {
			return null;
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int n;
			while ((n = is.read(buffer)) > 0) {
				bytes.write(buffer, 0, n);
			}
			return decodeByteArray(bytes.toByteArray(), 0, bytes.size());
		} catch (IOException e) {
			return null;
		}
	}

	public static Movie decodeByteArray(byte[] data, int offset, int length) {
		if (data == null)
			throw new NullPointerException();
		if ((offset | length) < 0 || offset + length > data.length)
			throw new ArrayIndexOutOfBoundsException();
		try {
			return parseGif(data, offset, offset + length);
		} catch (RuntimeException e) {
			return null; /* a damaged file */
		}
	}

	public static Movie decodeFile(String pathName) {
		try (InputStream is = new FileInputStream(pathName)) {
			return decodeStream(is);
		} catch (IOException e) {
			return null;
		}
	}

	private static int u16(byte[] d, int p) {
		return (d[p] & 0xFF) | (d[p + 1] & 0xFF) << 8;
	}

	private static int[] colorTable(byte[] d, int p, int size) {
		int[] table = new int[size];
		for (int i = 0; i < size; i++)
			table[i] = 0xFF000000 | (d[p + i * 3] & 0xFF) << 16 | (d[p + i * 3 + 1] & 0xFF) << 8 | (d[p + i * 3 + 2] & 0xFF);
		return table;
	}

	/* GIF87a/GIF89a: header, logical screen, then extensions and images up to the trailer */
	private static Movie parseGif(byte[] d, int p, int end) {
		if (end - p < 13 || d[p] != 'G' || d[p + 1] != 'I' || d[p + 2] != 'F' || d[p + 3] != '8' || (d[p + 4] != '7' && d[p + 4] != '9') || d[p + 5] != 'a')
			return null;
		int width = u16(d, p + 6), height = u16(d, p + 8);
		int flags = d[p + 10] & 0xFF;
		int backgroundIndex = d[p + 11] & 0xFF;
		p += 13;
		int[] global = null;
		if ((flags & 0x80) != 0) {
			int size = 2 << (flags & 7);
			global = colorTable(d, p, size);
			p += size * 3;
		}
		java.util.ArrayList<Frame> frames = new java.util.ArrayList<>();
		/* the graphic control extension applies to the next image */
		int delay = 0, disposal = 0, transparentIndex = -1;
		boolean transparent = false;
		while (p < end) {
			int block = d[p++] & 0xFF;
			if (block == 0x3B) /* trailer */
				break;
			if (block == 0x21) { /* extension */
				int label = d[p++] & 0xFF;
				if (label == 0xF9 && (d[p] & 0xFF) == 4) { /* graphic control: the delay is in 1/100 s */
					int packed = d[p + 1] & 0xFF;
					transparent = (packed & 1) != 0;
					disposal = (packed >> 2) & 7;
					delay = u16(d, p + 2) * 10;
					transparentIndex = transparent ? d[p + 4] & 0xFF : -1;
				}
				while (p < end && d[p] != 0) /* sub-blocks */
					p += (d[p] & 0xFF) + 1;
				p++;
			} else if (block == 0x2C) { /* image */
				Frame f = new Frame();
				f.left = u16(d, p);
				f.top = u16(d, p + 2);
				f.width = u16(d, p + 4);
				f.height = u16(d, p + 6);
				int iflags = d[p + 8] & 0xFF;
				p += 9;
				f.colors = global;
				if ((iflags & 0x80) != 0) {
					int size = 2 << (iflags & 7);
					f.colors = colorTable(d, p, size);
					p += size * 3;
				}
				int minCode = d[p++] & 0xFF;
				ByteArrayOutputStream data = new ByteArrayOutputStream();
				while (p < end && d[p] != 0) {
					int n = d[p] & 0xFF;
					data.write(d, p + 1, Math.min(n, end - p - 1));
					p += n + 1;
				}
				p++;
				f.pixels = lzw(data.toByteArray(), minCode, f.width * f.height);
				if ((iflags & 0x40) != 0)
					f.pixels = deinterlace(f.pixels, f.width, f.height);
				f.delay = delay;
				f.disposal = disposal;
				f.transparent = transparent;
				f.transparentIndex = transparentIndex;
				frames.add(f);
				delay = 0;
				disposal = 0;
				transparent = false;
				transparentIndex = -1;
			} else {
				return null;
			}
		}
		if (frames.isEmpty() || width <= 0 || height <= 0)
			return null;
		int background = 0;
		if (global != null && backgroundIndex < global.length)
			background = global[backgroundIndex];
		return new Movie(width, height, background, frames.toArray(new Frame[0]));
	}

	private static byte[] lzw(byte[] in, int minCode, int count) {
		byte[] out = new byte[count];
		if (minCode < 1 || minCode > 11)
			return out;
		int clear = 1 << minCode, eoi = clear + 1, next = clear + 2, size = minCode + 1;
		short[] prefix = new short[4096];
		byte[] suffix = new byte[4096];
		byte[] stack = new byte[4097];
		for (int i = 0; i < clear; i++)
			suffix[i] = (byte)i;
		int bits = 0, nbits = 0, pos = 0, o = 0, old = -1;
		byte first = 0;
		while (o < count) {
			while (nbits < size && pos < in.length) {
				bits |= (in[pos++] & 0xFF) << nbits;
				nbits += 8;
			}
			if (nbits < size)
				break;
			int code = bits & ((1 << size) - 1);
			bits >>>= size;
			nbits -= size;
			if (code == clear) {
				size = minCode + 1;
				next = clear + 2;
				old = -1;
				continue;
			}
			if (code == eoi)
				break;
			if (old == -1) {
				if (code >= clear)
					break;
				out[o++] = suffix[code];
				old = code;
				first = suffix[code];
				continue;
			}
			int in_code = code, sp = 0;
			if (code >= next) {
				if (code > next)
					break;
				stack[sp++] = first;
				code = old;
			}
			while (code >= clear) {
				stack[sp++] = suffix[code];
				code = prefix[code];
			}
			first = suffix[code];
			stack[sp++] = first;
			if (next < 4096) {
				prefix[next] = (short)old;
				suffix[next] = first;
				next++;
				if (next == (1 << size) && size < 12)
					size++;
			}
			old = in_code;
			while (sp > 0 && o < count)
				out[o++] = stack[--sp];
		}
		return out;
	}

	/* the rows of an interlaced image come in four passes: every 8th row from 0, every 8th from 4, every 4th from 2, every 2nd from 1 */
	private static byte[] deinterlace(byte[] in, int width, int height) {
		byte[] out = new byte[in.length];
		int src = 0;
		int[][] passes = {{0, 8}, {4, 8}, {2, 4}, {1, 2}};
		for (int[] pass : passes)
			for (int row = pass[0]; row < height; row += pass[1], src++)
				System.arraycopy(in, src * width, out, row * width, width);
		return out;
	}
}
