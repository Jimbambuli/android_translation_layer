package android.service.autofill;

import android.content.IntentSender;
import android.os.Bundle;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.Objects;

/* AOSP FillRequest; Parcelable left out. getInlineSuggestionsRequest()/the InlineSuggestionsRequest constructor
 * parameter are not ported: InlineSuggestionsRequest does not exist in ATL */
public final class FillRequest {

	public static final int FLAG_MANUAL_REQUEST = 0x1;
	public static final int FLAG_COMPATIBILITY_MODE_REQUEST = 0x2;
	public static final int FLAG_PASSWORD_INPUT_TYPE = 0x4;
	public static final int FLAG_VIEW_NOT_FOCUSED = 0x10;
	public static final int FLAG_SUPPORTS_FILL_DIALOG = 0x40;
	public static final int FLAG_IME_SHOWING = 0x80;
	public static final int FLAG_RESET_FILL_DIALOG_STATE = 0x100;
	public static final int FLAG_PCC_DETECTION = 0x200;
	public static final int FLAG_SCREEN_HAS_CREDMAN_FIELD = 0x400;
	public static final int FLAG_VIEW_REQUESTS_CREDMAN_SERVICE = 0x800;
	public static final int INVALID_REQUEST_ID = Integer.MIN_VALUE;

	private final int mId;
	private final List<FillContext> mFillContexts;
	private final List<String> mHints;
	private final Bundle mClientState;
	private final int mFlags;
	private final IntentSender mDelayedFillIntentSender;

	/* AOSP FillRequest.FillRequest (without the InlineSuggestionsRequest parameter) */
	public FillRequest(int id, List<FillContext> fillContexts, List<String> hints, Bundle clientState, int flags, IntentSender delayedFillIntentSender) {
		this.mId = id;
		this.mFillContexts = Objects.requireNonNull(fillContexts);
		this.mHints = Objects.requireNonNull(hints);
		this.mClientState = clientState;
		this.mFlags = flags;
		Preconditions.checkFlagsArgument(mFlags, FLAG_MANUAL_REQUEST | FLAG_COMPATIBILITY_MODE_REQUEST | FLAG_PASSWORD_INPUT_TYPE | FLAG_VIEW_NOT_FOCUSED | FLAG_SUPPORTS_FILL_DIALOG | FLAG_IME_SHOWING | FLAG_RESET_FILL_DIALOG_STATE | FLAG_PCC_DETECTION | FLAG_SCREEN_HAS_CREDMAN_FIELD | FLAG_VIEW_REQUESTS_CREDMAN_SERVICE);
		this.mDelayedFillIntentSender = delayedFillIntentSender;
		/* AOSP FillRequest.onConstructed */
		Preconditions.checkCollectionElementsNotNull(mFillContexts, "contexts");
		Preconditions.checkCollectionElementsNotNull(mHints, "hints");
	}

	public int getId() {
		return mId;
	}

	public List<FillContext> getFillContexts() {
		return mFillContexts;
	}

	public List<String> getHints() {
		return mHints;
	}

	public Bundle getClientState() {
		return mClientState;
	}

	public int getFlags() {
		return mFlags;
	}

	public IntentSender getDelayedFillIntentSender() {
		return mDelayedFillIntentSender;
	}

	@Override
	public String toString() {
		return "FillRequest { " + "id = " + mId + ", " + "fillContexts = " + mFillContexts + ", " + "hints = " + mHints + ", " + "clientState = " + mClientState + ", " + "flags = " + mFlags + ", " + "delayedFillIntentSender = " + mDelayedFillIntentSender + " }";
	}
}
