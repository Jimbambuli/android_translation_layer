package android.view;

import android.annotation.NonNull;
import android.graphics.Rect;
import android.graphics.Region;
import android.window.InputTransferToken;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * The AttachedSurfaceControl of a window (AOSP: ViewRootImpl). ATL's window has no SurfaceControl of its own (the
 * one of a SurfaceView is a child that covers its widget) and no input channel, so, as in AOSP for a root whose
 * surface is not valid: buildReparentTransaction() is null, and for a window that is drawn in software
 * (isHardwareEnabled() false) applyTransactionOnDraw() applies the transaction at once.
 */
final class RootSurfaceControl implements AttachedSurfaceControl {
	private final ArrayList<OnBufferTransformHintChangedListener> mTransformHintListeners = new ArrayList<>();
	private Region mTouchableRegion;
	private final Rect mChildBoundingInsets = new Rect();

	/* AOSP ViewRootImpl.buildReparentTransaction(): null while mSurfaceControl is not valid */
	@Override
	public SurfaceControl.Transaction buildReparentTransaction(@NonNull SurfaceControl child) {
		return null;
	}

	/* AOSP ViewRootImpl.applyTransactionOnDraw(): !isHardwareEnabled() -> t.apply() */
	@Override
	public boolean applyTransactionOnDraw(@NonNull SurfaceControl.Transaction t) {
		t.apply();
		return true;
	}

	/* AOSP ViewRootImpl: the hint of the display rotation, which ATL's display never has (identity) */
	@Override
	public int getBufferTransformHint() {
		return 0;
	}

	@Override
	public void addOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener) {
		Objects.requireNonNull(listener);
		if (mTransformHintListeners.contains(listener)) {
			throw new IllegalArgumentException("attempt to call addOnBufferTransformHintChangedListener() "
					+ "with a previously registered listener");
		}
		mTransformHintListeners.add(listener);
	}

	@Override
	public void removeOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener) {
		Objects.requireNonNull(listener);
		mTransformHintListeners.remove(listener);
	}

	/* AOSP ViewRootImpl.setTouchableRegion(): kept for the insets computation; ATL passes no touch through */
	@Override
	public void setTouchableRegion(Region r) {
		mTouchableRegion = r != null ? new Region(r) : null;
	}

	@Override
	public void setChildBoundingInsets(@NonNull Rect insets) {
		if (insets.left < 0 || insets.top < 0 || insets.right < 0 || insets.bottom < 0) {
			throw new IllegalArgumentException("Negative insets passed to setChildBoundingInsets.");
		}
		mChildBoundingInsets.set(insets);
	}

	/* AOSP ViewRootImpl.getInputTransferToken(): the window's input token; a window with no input channel throws */
	@Override
	public InputTransferToken getInputTransferToken() {
		throw new IllegalStateException("Called getInputTransferToken for Window with no input channel");
	}

	/* AOSP ViewRootImpl.registerOnJankDataListener(): mSurfaceControl.addOnJankDataListener(); SurfaceFlinger has
	 * no data for a surface that is not valid */
	@Override
	public SurfaceControl.OnJankDataListenerRegistration registerOnJankDataListener(Executor executor,
			SurfaceControl.OnJankDataListener listener) {
		return SurfaceControl.OnJankDataListenerRegistration.NONE;
	}
}
