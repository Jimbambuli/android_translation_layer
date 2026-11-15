package android.service.autofill;

import android.app.slice.Slice;
import android.widget.inline.InlinePresentationSpec;
import java.util.List;
import java.util.Objects;

/* AOSP InlinePresentation (codegen data class); Parcel code left out, nothing in ATL parcels it */
public final class InlinePresentation {

	private final Slice mSlice;
	private final InlinePresentationSpec mInlinePresentationSpec;
	private final boolean mPinned;

	/* AOSP InlinePresentation.getAutofillHints */
	public String[] getAutofillHints() {
		List<String> hints = mSlice.getHints();
		return hints.toArray(new String[hints.size()]);
	}

	/* AOSP InlinePresentation.createTooltipPresentation */
	public static InlinePresentation createTooltipPresentation(Slice slice, InlinePresentationSpec spec) {
		return new InlinePresentation(slice, spec, /* pinned */ false);
	}

	public InlinePresentation(Slice slice, InlinePresentationSpec inlinePresentationSpec, boolean pinned) {
		this.mSlice = Objects.requireNonNull(slice);
		this.mInlinePresentationSpec = Objects.requireNonNull(inlinePresentationSpec);
		this.mPinned = pinned;
	}

	public Slice getSlice() {
		return mSlice;
	}

	public InlinePresentationSpec getInlinePresentationSpec() {
		return mInlinePresentationSpec;
	}

	public boolean isPinned() {
		return mPinned;
	}

	@Override
	public String toString() {
		return "InlinePresentation { " + "slice = " + mSlice + ", " + "inlinePresentationSpec = " + mInlinePresentationSpec + ", " + "pinned = " + mPinned + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		InlinePresentation that = (InlinePresentation) o;
		return Objects.equals(mSlice, that.mSlice) && Objects.equals(mInlinePresentationSpec, that.mInlinePresentationSpec) && mPinned == that.mPinned;
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + Objects.hashCode(mSlice);
		_hash = 31 * _hash + Objects.hashCode(mInlinePresentationSpec);
		_hash = 31 * _hash + Boolean.hashCode(mPinned);
		return _hash;
	}
}
