package android.view;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Rect;
import android.graphics.Region;
import android.window.InputTransferToken;

import java.util.concurrent.Executor;

/**
 * AOSP AttachedSurfaceControl (Android 16): the interface a view hierarchy's root gives to its SurfaceControl.
 * The members are AOSP's; the answers of ATL's root are in RootSurfaceControl.
 */
public interface AttachedSurfaceControl {
	@Nullable
	SurfaceControl.Transaction buildReparentTransaction(@NonNull SurfaceControl child);

	boolean applyTransactionOnDraw(@NonNull SurfaceControl.Transaction t);

	/* SurfaceControl.BUFFER_TRANSFORM_IDENTITY */
	default int getBufferTransformHint() {
		return 0;
	}

	interface OnBufferTransformHintChangedListener {
		void onBufferTransformHintChanged(int hint);
	}

	default void addOnBufferTransformHintChangedListener(@NonNull OnBufferTransformHintChangedListener listener) {
	}

	default void removeOnBufferTransformHintChangedListener(@NonNull OnBufferTransformHintChangedListener listener) {
	}

	default void setTouchableRegion(@Nullable Region r) {
	}

	default void setChildBoundingInsets(@NonNull Rect insets) {
	}

	@NonNull
	default InputTransferToken getInputTransferToken() {
		throw new UnsupportedOperationException("The getInputTransferToken needs to be "
				+ "implemented before making this call.");
	}

	@NonNull
	default SurfaceControl.OnJankDataListenerRegistration registerOnJankDataListener(
			@NonNull @CallbackExecutor Executor executor, @NonNull SurfaceControl.OnJankDataListener listener) {
		return SurfaceControl.OnJankDataListenerRegistration.NONE;
	}
}
