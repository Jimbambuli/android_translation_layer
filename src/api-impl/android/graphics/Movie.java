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
 * ATL: AOSP decodes the animation (GIF) frame by frame with Skia. ATL decodes a movie with BitmapFactory, which
 * gives the first image: the movie is one frame long (duration() is 0, as it is in AOSP for a GIF with one
 * image) and draw() paints that image. An animated GIF shows its first image and does not move.
 */
@Deprecated
public class Movie {
	private final Bitmap mFrame;

	private Movie(Bitmap frame) {
		mFrame = frame;
	}

	/** Return the width of the movie */
	public int width() {
		return mFrame.getWidth();
	}

	/** Return the height of the movie */
	public int height() {
		return mFrame.getHeight();
	}

	/** Return true if the movie is opaque */
	public boolean isOpaque() {
		return !mFrame.hasAlpha();
	}

	/** Return the duration of the movie in milliseconds, 0 for a movie of one frame */
	public int duration() {
		return 0;
	}

	/**
	 * Set the movie's time. Returns true if the time changed the frame, which with one frame it never does.
	 */
	public boolean setTime(int relativeMilliseconds) {
		return false;
	}

	/** Draw the movie at (x, y), with the paint, if any */
	public void draw(Canvas canvas, float x, float y, Paint paint) {
		canvas.drawBitmap(mFrame, x, y, paint);
	}

	/** Draw the movie at (x, y) */
	public void draw(Canvas canvas, float x, float y) {
		draw(canvas, x, y, null);
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
		Bitmap frame = BitmapFactory.decodeByteArray(data, offset, length);
		return frame != null ? new Movie(frame) : null;
	}

	public static Movie decodeFile(String pathName) {
		try (InputStream is = new FileInputStream(pathName)) {
			return decodeStream(is);
		} catch (IOException e) {
			return null;
		}
	}
}
