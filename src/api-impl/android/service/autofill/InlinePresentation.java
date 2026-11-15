package android.service.autofill;

import android.app.slice.Slice;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.inline.InlinePresentationSpec;
import java.util.List;
import java.util.Objects;

/* AOSP InlinePresentation (codegen data class) */
public final class InlinePresentation implements Parcelable {

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

	/* AOSP InlinePresentation.writeToParcel */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		byte flg = 0;
		if (mPinned)
			flg |= 0x4;
		dest.writeByte(flg);
		dest.writeTypedObject(mSlice, flags);
		dest.writeTypedObject(mInlinePresentationSpec, flags);
	}

	/* AOSP InlinePresentation.describeContents */
	@Override
	public int describeContents() {
		return 0;
	}

	/* AOSP InlinePresentation(Parcel) */
	InlinePresentation(Parcel in) {
		byte flg = in.readByte();
		boolean pinned = (flg & 0x4) != 0;
		Slice slice = (Slice) in.readTypedObject(Slice.CREATOR);
		InlinePresentationSpec inlinePresentationSpec = (InlinePresentationSpec) in.readTypedObject(InlinePresentationSpec.CREATOR);
		this.mSlice = Objects.requireNonNull(slice);
		this.mInlinePresentationSpec = Objects.requireNonNull(inlinePresentationSpec);
		this.mPinned = pinned;
	}

	/* AOSP InlinePresentation.CREATOR */
	public static final Parcelable.Creator<InlinePresentation> CREATOR = new Parcelable.Creator<InlinePresentation>() {
		@Override
		public InlinePresentation[] newArray(int size) {
			return new InlinePresentation[size];
		}

		@Override
		public InlinePresentation createFromParcel(Parcel in) {
			return new InlinePresentation(in);
		}
	};
}
