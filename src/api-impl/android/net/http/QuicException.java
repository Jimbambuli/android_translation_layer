/* AOSP external/cronet android/java/src/android/net/http/QuicException.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.Nullable;

public abstract class QuicException extends NetworkException {

	protected QuicException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
