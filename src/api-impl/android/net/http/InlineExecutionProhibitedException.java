/* AOSP external/cronet android/java/src/android/net/http/InlineExecutionProhibitedException.java (android-16.0.0_r4) */
package android.net.http;

import java.util.concurrent.RejectedExecutionException;

public final class InlineExecutionProhibitedException extends RejectedExecutionException {
	public InlineExecutionProhibitedException() {
		super("Inline execution is prohibited for this request");
	}
}
