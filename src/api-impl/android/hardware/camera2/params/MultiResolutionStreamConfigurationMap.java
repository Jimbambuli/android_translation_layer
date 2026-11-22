/* AOSP MultiResolutionStreamConfigurationMap.java (Android 16) */
package android.hardware.camera2.params;

import static com.android.internal.util.Preconditions.*;

import android.hardware.camera2.utils.HashCodeHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MultiResolutionStreamConfigurationMap {
	public MultiResolutionStreamConfigurationMap(Map<String, StreamConfiguration[]> configurations) {
		checkNotNull(configurations, "multi-resolution configurations must not be null");
		if (configurations.size() == 0) {
			throw new IllegalArgumentException("multi-resolution configurations must not be empty");
		}

		mConfigurations = configurations;

		// For each multi-resolution stream configuration, track how many formats and sizes there
		// are available to configure
		for (Map.Entry<String, StreamConfiguration[]> entry : mConfigurations.entrySet()) {
			String cameraId = entry.getKey();
			StreamConfiguration[] configs = entry.getValue();

			for (int i = 0; i < configs.length; i++) {
				StreamConfiguration config = configs[i];
				int format = config.getFormat();
				MultiResolutionStreamInfo multiResolutionStreamInfo = new MultiResolutionStreamInfo(config.getWidth(), config.getHeight(), cameraId);

				Map<Integer, List<MultiResolutionStreamInfo>> destMap;
				if (config.isInput()) {
					destMap = mMultiResolutionInputConfigs;
				} else {
					destMap = mMultiResolutionOutputConfigs;
				}

				if (!destMap.containsKey(format)) {
					List<MultiResolutionStreamInfo> multiResolutionStreamInfoList = new ArrayList<MultiResolutionStreamInfo>();
					destMap.put(format, multiResolutionStreamInfoList);
				}
				destMap.get(format).add(multiResolutionStreamInfo);
			}
		}
	}

	public static class SizeComparator implements Comparator<MultiResolutionStreamInfo> {
		@Override
		public int compare(MultiResolutionStreamInfo lhs, MultiResolutionStreamInfo rhs) {
			return StreamConfigurationMap.compareSizes(lhs.getWidth(), lhs.getHeight(), rhs.getWidth(), rhs.getHeight());
		}
	}

	public int[] getOutputFormats() {
		return getPublicImageFormats(/*output*/ true);
	}

	public int[] getInputFormats() {
		return getPublicImageFormats(/*output*/ false);
	}

	// Get the list of publicly visible multi-resolution input/output stream formats
	private int[] getPublicImageFormats(boolean output) {
		Map<Integer, List<MultiResolutionStreamInfo>> multiResolutionConfigs = output ? mMultiResolutionOutputConfigs : mMultiResolutionInputConfigs;
		int formatCount = multiResolutionConfigs.size();

		int[] formats = new int[formatCount];
		int i = 0;
		for (Integer format : multiResolutionConfigs.keySet()) {
			formats[i++] = StreamConfigurationMap.imageFormatToPublic(format);
		}

		return formats;
	}

	public Collection<MultiResolutionStreamInfo> getOutputInfo(int format) {
		return getInfo(format, /*false*/ true);
	}

	public Collection<MultiResolutionStreamInfo> getInputInfo(int format) {
		return getInfo(format, /*false*/ false);
	}

	// Get multi-resolution stream info for a particular format
	private Collection<MultiResolutionStreamInfo> getInfo(int format, boolean output) {
		int internalFormat = StreamConfigurationMap.imageFormatToInternal(format);

		Map<Integer, List<MultiResolutionStreamInfo>> multiResolutionConfigs = output ? mMultiResolutionOutputConfigs : mMultiResolutionInputConfigs;

		if (multiResolutionConfigs.containsKey(internalFormat)) {
			return Collections.unmodifiableCollection(multiResolutionConfigs.get(internalFormat));
		} else {
			return Collections.emptyList();
		}
	}

	private void appendConfigurationsString(StringBuilder sb, boolean output) {
		sb.append(output ? "Outputs(" : "Inputs(");
		int[] formats = getPublicImageFormats(output);
		if (formats != null) {
			for (int format : formats) {
				Collection<MultiResolutionStreamInfo> streamInfoList = getInfo(format, output);
				sb.append("[" + StreamConfigurationMap.formatToString(format) + ":");
				for (MultiResolutionStreamInfo streamInfo : streamInfoList) {
					sb.append(String.format("[w:%d, h:%d, id:%s], ", streamInfo.getWidth(), streamInfo.getHeight(), streamInfo.getPhysicalCameraId()));
				}
				// Remove the pending ", "
				if (sb.charAt(sb.length() - 1) == ' ') {
					sb.delete(sb.length() - 2, sb.length());
				}
				sb.append("]");
			}
		}
		sb.append(")");
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof MultiResolutionStreamConfigurationMap) {
			final MultiResolutionStreamConfigurationMap other = (MultiResolutionStreamConfigurationMap) obj;
			if (!mConfigurations.keySet().equals(other.mConfigurations.keySet())) {
				return false;
			}

			for (String id : mConfigurations.keySet()) {
				if (!Arrays.equals(mConfigurations.get(id), other.mConfigurations.get(id))) {
					return false;
				}
			}

			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HashCodeHelpers.hashCodeGeneric(mConfigurations, mMultiResolutionOutputConfigs, mMultiResolutionInputConfigs);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MultiResolutionStreamConfigurationMap(");
		appendConfigurationsString(sb, /*output*/ true);
		sb.append(",");
		appendConfigurationsString(sb, /*output*/ false);
		sb.append(")");
		return sb.toString();
	}

	private final Map<String, StreamConfiguration[]> mConfigurations;
	private final Map<Integer, List<MultiResolutionStreamInfo>> mMultiResolutionOutputConfigs = new HashMap<Integer, List<MultiResolutionStreamInfo>>();
	private final Map<Integer, List<MultiResolutionStreamInfo>> mMultiResolutionInputConfigs = new HashMap<Integer, List<MultiResolutionStreamInfo>>();
}
