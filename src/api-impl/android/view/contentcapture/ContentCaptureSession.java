package android.view.contentcapture;

import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import com.android.internal.util.Preconditions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AOSP ContentCaptureSession: the session a view's content is reported to for content capture. ATL has no content
 * capture service, so View.getContentCaptureSession() is null unless an app set one, and a session an app derives
 * itself (and each child it creates) is disabled: AOSP's notify*() methods do nothing while content capture is
 * disabled, and so do these.
 */
public abstract class ContentCaptureSession implements AutoCloseable {
	private static final SecureRandom ID_GENERATOR = new SecureRandom();

	private static final int NO_SESSION_ID = 0;

	private static final int INITIAL_CHILDREN_CAPACITY = 5;

	private final Object mLock = new Object();

	private final int mId;

	// Lazily created on demand.
	private ContentCaptureSessionId mContentCaptureSessionId;

	private ContentCaptureContext mClientContext;

	private ArrayList<ContentCaptureSession> mChildren;

	private boolean mDestroyed;

	protected ContentCaptureSession() {
		mId = getRandomSessionId();
	}

	// Used by ChildContentCaptureSession
	ContentCaptureSession(ContentCaptureContext initialContext) {
		this();
		mClientContext = Objects.requireNonNull(initialContext);
	}

	public final ContentCaptureSessionId getContentCaptureSessionId() {
		if (mContentCaptureSessionId == null) {
			mContentCaptureSessionId = new ContentCaptureSessionId(mId);
		}
		return mContentCaptureSessionId;
	}

	/** AOSP: the id of the session (hidden getId()) */
	public int getId() {
		return mId;
	}

	/* AOSP createContentCaptureSession(): a child session (AOSP newChild()), destroyed with this one */
	public final ContentCaptureSession createContentCaptureSession(ContentCaptureContext context) {
		final ContentCaptureSession child = new ChildContentCaptureSession(context);
		synchronized (mLock) {
			if (mChildren == null) {
				mChildren = new ArrayList<>(INITIAL_CHILDREN_CAPACITY);
			}
			mChildren.add(child);
		}
		return child;
	}

	public final void setContentCaptureContext(ContentCaptureContext context) {
		if (!isContentCaptureEnabled())
			return;

		mClientContext = context;
	}

	public final ContentCaptureContext getContentCaptureContext() {
		return mClientContext;
	}

	/* AOSP destroy(): marks the session destroyed once, children first (there is no service to tell) */
	public final void destroy() {
		synchronized (mLock) {
			if (mDestroyed) {
				return;
			}
			mDestroyed = true;

			if (mChildren != null) {
				final int numberChildren = mChildren.size();
				for (int i = 0; i < numberChildren; i++) {
					mChildren.get(i).destroy();
				}
			}
		}
	}

	/* AOSP close() (AutoCloseable): destroy() */
	@Override
	public void close() {
		destroy();
	}

	/* AOSP (package-private): whether the content is reported; MainContentCaptureSession also asks the
	 * manager, and there is no manager without the service, so never */
	boolean isContentCaptureEnabled() {
		return false;
	}

	public AutofillId newAutofillId(AutofillId hostId, long virtualChildId) {
		Objects.requireNonNull(hostId);
		Preconditions.checkArgument(hostId.isNonVirtual(), "hostId cannot be virtual: %s", hostId);
		return new AutofillId(hostId, virtualChildId, mId);
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
		return new ViewNode.ViewStructureImpl(parentId, virtualId, mId);
	}

	public final void notifyViewAppeared(ViewStructure node) {
		Objects.requireNonNull(node);
	}

	public final void notifyViewsAppeared(List<ViewStructure> appearedNodes) {
		Preconditions.checkCollectionElementsNotNull(appearedNodes, "appearedNodes");
	}

	public final void notifyViewDisappeared(AutofillId id) {
		Objects.requireNonNull(id);
	}

	public final void notifyViewsDisappeared(AutofillId hostId, long[] virtualIds) {
		Objects.requireNonNull(hostId);
		Preconditions.checkArgument(hostId.isNonVirtual(), "hostId cannot be virtual: %s", hostId);
		Preconditions.checkArgument(virtualIds != null && virtualIds.length != 0, "virtual ids cannot be empty");
	}

	public final void notifyViewTextChanged(AutofillId id, CharSequence text) {
		Objects.requireNonNull(id);
	}

	public final void notifyViewInsetsChanged(android.graphics.Insets viewInsets) {
		Objects.requireNonNull(viewInsets);
	}

	/* AOSP: reports a flush event to the service (through the main session); content capture is disabled */
	public void flush() {}

	public final void notifySessionResumed() {}

	public final void notifySessionPaused() {}

	@Override
	public String toString() {
		return Integer.toString(mId);
	}

	private static int getRandomSessionId() {
		int id;
		do {
			id = ID_GENERATOR.nextInt();
		} while (id == NO_SESSION_ID);
		return id;
	}
}
