package android.view.contentcapture;

import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import java.util.List;
import java.util.Objects;

/**
 * AOSP ContentCaptureSession: the session a view's content is reported to for content capture. ATL has no content
 * capture service, so View.getContentCaptureSession() is null unless an app set one, and a session an app derives
 * itself is disabled: AOSP's notify*() methods do nothing while content capture is disabled, and so do these.
 */
public abstract class ContentCaptureSession implements AutoCloseable {
	private static final int NO_SESSION_ID = 0;

	private int mId = NO_SESSION_ID;

	protected ContentCaptureSession() {}

	/** AOSP: the id of the session (hidden getId()); ATL's sessions have none until an app gives one */
	public int getId() {
		return mId;
	}

	private boolean mDestroyed;

	/* AOSP destroy(): marks the session destroyed once (ATL's sessions have no child sessions and no
	 * service to tell) */
	public final void destroy() {
		synchronized (this) {
			if (mDestroyed) {
				return;
			}
			mDestroyed = true;
		}
	}

	/* AOSP close() (AutoCloseable): destroy() */
	@Override
	public void close() {
		destroy();
	}

	/** AOSP: whether the content is reported; no service here, so never */
	public boolean isContentCaptureEnabled() {
		return false;
	}

	public final AutofillId newAutofillId(AutofillId hostId, long virtualChildId) {
		Objects.requireNonNull(hostId);
		if (!hostId.isNonVirtual()) {
			throw new IllegalArgumentException("hostId cannot be virtual: " + hostId);
		}
		return new AutofillId(hostId, virtualChildId, getId());
	}

	/**
	 * Creates a {@link ViewStructure} for a "standard" view.
	 *
	 * <p>This method should be called after a {@link View} is laid out, so the view structure can be
	 * filled and then passed to {@link #notifyViewAppeared(ViewStructure)}.
	 */
	public final ViewStructure newViewStructure(View view) {
		return new ViewNode.ViewStructureImpl(view);
	}

	/**
	 * Creates a {@link ViewStructure} for a "virtual" view, so it can be passed to
	 * {@link #notifyViewAppeared(ViewStructure)} by the view managing the virtual view hierarchy.
	 *
	 * @param parentId id of the virtual view parent (it can be obtained by calling
	 * {@link ViewStructure#getAutofillId()} on the parent).
	 * @param virtualId id of the virtual child, relative to the parent.
	 */
	public final ViewStructure newVirtualViewStructure(AutofillId parentId, long virtualId) {
		return new ViewNode.ViewStructureImpl(parentId, virtualId, getId());
	}

	public final void notifyViewAppeared(ViewStructure node) {
		Objects.requireNonNull(node);
	}

	public final void notifyViewsAppeared(List<ViewStructure> appearedNodes) {
		Objects.requireNonNull(appearedNodes);
	}

	public final void notifyViewDisappeared(AutofillId id) {
		Objects.requireNonNull(id);
	}

	public final void notifyViewsDisappeared(AutofillId hostId, long[] virtualIds) {
		Objects.requireNonNull(hostId);
		if (!hostId.isNonVirtual()) {
			throw new IllegalArgumentException("hostId cannot be virtual: " + hostId);
		}
		Objects.requireNonNull(virtualIds);
	}

	public final void notifyViewTextChanged(AutofillId id, CharSequence text) {
		Objects.requireNonNull(id);
	}

	public final void notifyViewInsetsChanged(android.graphics.Insets viewInsets) {
		Objects.requireNonNull(viewInsets);
	}
}
