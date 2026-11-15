/* AOSP external/cronet android/java/src/android/net/http/HeaderBlock.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.NonNull;

import java.util.List;
import java.util.Map;

public abstract class HeaderBlock {

	@NonNull
	public abstract List<Map.Entry<String, String>> getAsList();

	@NonNull
	public abstract Map<String, List<String>> getAsMap();
}
