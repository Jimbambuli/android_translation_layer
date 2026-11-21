/* AOSP (Android 16) android.hardware.camera2.params.StreamConfiguration, comments and annotations stripped; no other differences. */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;
import static android.hardware.camera2.params.StreamConfigurationMap.checkArgumentFormatInternal;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Size;

public class StreamConfiguration {
	public StreamConfiguration(
			final int format, final int width, final int height, final boolean input) {
		mFormat = checkArgumentFormatInternal(format);
		mWidth = checkArgumentPositive(width, "width must be positive");
		mHeight = checkArgumentPositive(height, "height must be positive");
		mInput = input;
	}

	public final int getFormat() {
		return mFormat;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public Size getSize() {
		return new Size(mWidth, mHeight);
	}

	public boolean isInput() {
		return mInput;
	}

	public boolean isOutput() {
		return !mInput;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof StreamConfiguration) {
			final StreamConfiguration other = (StreamConfiguration) obj;
			return mFormat == other.mFormat &&
					mWidth == other.mWidth &&
					mHeight == other.mHeight &&
					mInput == other.mInput;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mFormat, mWidth, mHeight, mInput ? 1 : 0);
	}

	protected int mFormat;
	protected int mWidth;
	protected int mHeight;
	protected boolean mInput;
}
