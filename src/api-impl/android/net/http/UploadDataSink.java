/* AOSP external/cronet android/java/src/android/net/http/UploadDataSink.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.NonNull;

public abstract class UploadDataSink {
	public abstract void onReadSucceeded(boolean finalChunk);

	public abstract void onReadError(@NonNull Exception exception);

	public abstract void onRewindSucceeded();

	public abstract void onRewindError(@NonNull Exception exception);
    /* AOSP 16 API; no backing hardware/service in ATL */
    public UploadDataSink() {}
}
