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
public abstract class ContentCaptureSession {
	private static final int NO_SESSION_ID = 0;

	private int mId = NO_SESSION_ID;

	protected ContentCaptureSession() {}

	/** AOSP: the id of the session (hidden getId()); ATL's sessions have none until an app gives one */
	public int getId() {
		return mId;
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
