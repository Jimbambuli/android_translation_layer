package android.webkit;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;

/* AOSP: getInstance() = WebViewFactory.getProvider().getTracingController(). ATL has no WebView provider and
 * WebKit has no Chromium tracing, so this keeps only the state machine of the framework contract (start while
 * tracing throws IllegalStateException, stop without tracing returns false). stop() writes an empty trace
 * document: approximation, no events are recorded. */
public abstract class TracingController {
	private static TracingController instance;

	@Deprecated
	public TracingController() {}

	@NonNull
	public static synchronized TracingController getInstance() {
		if (instance == null) {
			instance = new TracingController() {
				private boolean tracing;

				@Override
				public synchronized void start(@NonNull TracingConfig tracingConfig) {
					if (tracingConfig == null)
						throw new IllegalArgumentException("tracingConfig cannot be null");
					if (tracing)
						throw new IllegalStateException("cannot start tracing: tracing is already enabled");
					tracing = true;
				}

				@Override
				public synchronized boolean stop(@Nullable OutputStream outputStream, @NonNull @CallbackExecutor Executor executor) {
					if (!tracing)
						return false;
					tracing = false;
					if (outputStream != null) {
						executor.execute(() -> {
							try {
								outputStream.write("{\"traceEvents\":[]}".getBytes());
								outputStream.close();
							} catch (IOException e) {
							}
						});
					}
					return true;
				}

				@Override
				public synchronized boolean isTracing() {
					return tracing;
				}
			};
		}
		return instance;
	}

	public abstract void start(@NonNull TracingConfig tracingConfig);

	public abstract boolean stop(@Nullable OutputStream outputStream, @NonNull @CallbackExecutor Executor executor);

	public abstract boolean isTracing();
}
