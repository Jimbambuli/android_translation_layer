/* AOSP (Android 16) android.hardware.camera2.params.ReprocessFormatsMap, comments and annotations stripped; no other differences. */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.Arrays;

public final class ReprocessFormatsMap {
	public ReprocessFormatsMap(final int[] entry) {
		checkNotNull(entry, "entry must not be null");

		int numInputs = 0;
		int left = entry.length;
		for (int i = 0; i < entry.length; ) {
			int inputFormat = StreamConfigurationMap.checkArgumentFormatInternal(entry[i]);

			left--;
			i++;

			if (left < 1) {
				throw new IllegalArgumentException(
						String.format("Input %x had no output format length listed", inputFormat));
			}

			final int length = entry[i];
			left--;
			i++;

			for (int j = 0; j < length; ++j) {
				int outputFormat = entry[i + j];
				StreamConfigurationMap.checkArgumentFormatInternal(outputFormat);
			}

			if (length > 0) {
				if (left < length) {
					throw new IllegalArgumentException(
							String.format(
									"Input %x had too few output formats listed (actual: %d, " +
									"expected: %d)", inputFormat, left, length));
				}

				i += length;
				left -= length;
			}

			numInputs++;
		}

		mEntry = entry;
		mInputCount = numInputs;
	}

	public int[] getInputs() {
		final int[] inputs = new int[mInputCount];

		int left = mEntry.length;
		for (int i = 0, j = 0; i < mEntry.length; j++) {
			final int format = mEntry[i];

			left--;
			i++;

			if (left < 1) {
				throw new AssertionError(
						String.format("Input %x had no output format length listed", format));
			}

			final int length = mEntry[i];
			left--;
			i++;

			if (length > 0) {
				if (left < length) {
					throw new AssertionError(
							String.format(
									"Input %x had too few output formats listed (actual: %d, " +
									"expected: %d)", format, left, length));
				}

				i += length;
				left -= length;
			}

			inputs[j] = format;
		}

		return StreamConfigurationMap.imageFormatToPublic(inputs);
	}

	public int[] getOutputs(final int format) {
		int left = mEntry.length;
		for (int i = 0; i < mEntry.length; ) {
			final int inputFormat = mEntry[i];

			left--;
			i++;

			if (left < 1) {
				throw new AssertionError(
						String.format("Input %x had no output format length listed", format));
			}

			final int length = mEntry[i];
			left--;
			i++;

			if (length > 0) {
				if (left < length) {
					throw new AssertionError(
							String.format(
									"Input %x had too few output formats listed (actual: %d, " +
									"expected: %d)", format, left, length));
				}
			}

			if (inputFormat == format) {
				int[] outputs = new int[length];

				for (int k = 0; k < length; ++k) {
					outputs[k] = mEntry[i + k];
				}

				return StreamConfigurationMap.imageFormatToPublic(outputs);
			}

			i += length;
			left -= length;

		}

		throw new IllegalArgumentException(
				String.format("Input format 0x%x was not one in #getInputs", format));
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof ReprocessFormatsMap) {
			final ReprocessFormatsMap other = (ReprocessFormatsMap) obj;
			return Arrays.equals(mEntry, other.mEntry);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCode(mEntry);
	}

	private final int[] mEntry;
	private final int mInputCount;
}
