/* AOSP external/cronet android/java/src/android/net/http/HeaderBlockImpl.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class HeaderBlockImpl extends android.net.http.HeaderBlock {
	private final List<Map.Entry<String, String>> mAllHeadersList;
	private Map<String, List<String>> mAllHeadersMap;

	public HeaderBlockImpl(List<Map.Entry<String, String>> allHeaders) {
		this.mAllHeadersList = Collections.unmodifiableList(allHeaders);
	}

	@NonNull
	@Override
	public List<Map.Entry<String, String>> getAsList() {
		return mAllHeadersList;
	}

	@NonNull
	@Override
	public Map<String, List<String>> getAsMap() {
		if (mAllHeadersMap != null) {
			return mAllHeadersMap;
		}
		synchronized (mAllHeadersList) {
			Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			for (Map.Entry<String, String> entry : mAllHeadersList) {
				List<String> values = new ArrayList<>();
				if (map.containsKey(entry.getKey())) {
					values.addAll(map.get(entry.getKey()));
				}
				values.add(entry.getValue());
				map.put(entry.getKey(), Collections.unmodifiableList(values));
			}
			mAllHeadersMap = Collections.unmodifiableMap(map);
		}
		return mAllHeadersMap;
	}
}
