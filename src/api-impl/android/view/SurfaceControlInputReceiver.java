package android.view;

import android.annotation.NonNull;

/**
 * AOSP SurfaceControlInputReceiver: receives the input events of a SurfaceControl that registered it with
 * WindowManager.registerBatchedSurfaceControlInputReceiver() or registerUnbatchedSurfaceControlInputReceiver().
 */
public interface SurfaceControlInputReceiver {
	boolean onInputEvent(@NonNull InputEvent event);
}
