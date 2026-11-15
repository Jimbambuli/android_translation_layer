/* AOSP external/cronet android/java/src/android/net/http/HttpException.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.Nullable;

import java.io.IOException;

public class HttpException extends IOException {

	public HttpException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
