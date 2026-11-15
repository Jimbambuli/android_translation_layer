/* AOSP external/cronet android/java/src/android/net/http/UploadDataProvider.java (android-16.0.0_r4) */
package android.net.http;

import android.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class UploadDataProvider implements Closeable {

	public abstract long getLength() throws IOException;

	public abstract void read(@NonNull UploadDataSink uploadDataSink,
			@NonNull ByteBuffer byteBuffer) throws IOException;

	public abstract void rewind(@NonNull UploadDataSink uploadDataSink) throws IOException;

	@Override
	public void close() throws IOException {}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public UploadDataProvider() {}
}
