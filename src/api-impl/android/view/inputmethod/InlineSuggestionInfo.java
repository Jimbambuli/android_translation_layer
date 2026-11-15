package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.inline.InlinePresentationSpec;
import java.util.Arrays;
import java.util.Objects;

/**
 * android-16 frameworks/base core/java/android/view/inputmethod/InlineSuggestionInfo.java (codegen data class):
 * metadata of an inline suggestion. Source/type values are validated like the AOSP constructor.
 */
public final class InlineSuggestionInfo implements Parcelable {

	public static final String SOURCE_AUTOFILL = "android:autofill";
	public static final String SOURCE_PLATFORM = "android:platform";
	public static final String TYPE_SUGGESTION = "android:autofill:suggestion";
	public static final String TYPE_ACTION = "android:autofill:action";

	private final InlinePresentationSpec mInlinePresentationSpec;
	private final String mSource;
	private final String[] mAutofillHints;
	private final String mType;
	private final boolean mPinned;
	private final InlineSuggestion mTooltip;

	public static InlineSuggestionInfo newInlineSuggestionInfo(InlinePresentationSpec presentationSpec, String source, String[] autofillHints, String type, boolean isPinned) {
		return new InlineSuggestionInfo(presentationSpec, source, autofillHints, type, isPinned, null);
	}

	public static InlineSuggestionInfo newInlineSuggestionInfo(InlinePresentationSpec presentationSpec, String source, String[] autofillHints, String type, boolean isPinned, InlineSuggestion tooltip) {
		return new InlineSuggestionInfo(presentationSpec, source, autofillHints, type, isPinned, tooltip);
	}

	public InlineSuggestionInfo(InlinePresentationSpec inlinePresentationSpec, String source, String[] autofillHints, String type, boolean pinned, InlineSuggestion tooltip) {
		mInlinePresentationSpec = Objects.requireNonNull(inlinePresentationSpec);
		if (!Objects.equals(source, SOURCE_AUTOFILL) && !Objects.equals(source, SOURCE_PLATFORM)) {
			throw new IllegalArgumentException("source was " + source + " but must be one of: SOURCE_AUTOFILL(" + SOURCE_AUTOFILL + "), SOURCE_PLATFORM(" + SOURCE_PLATFORM + ")");
		}
		mSource = source;
		mAutofillHints = autofillHints;
		if (!Objects.equals(type, TYPE_SUGGESTION) && !Objects.equals(type, TYPE_ACTION)) {
			throw new IllegalArgumentException("type was " + type + " but must be one of: TYPE_SUGGESTION(" + TYPE_SUGGESTION + "), TYPE_ACTION(" + TYPE_ACTION + ")");
		}
		mType = type;
		mPinned = pinned;
		mTooltip = tooltip;
	}

	public InlinePresentationSpec getInlinePresentationSpec() {
		return mInlinePresentationSpec;
	}

	public String getSource() {
		return mSource;
	}

	public String[] getAutofillHints() {
		return mAutofillHints;
	}

	public String getType() {
		return mType;
	}

	public boolean isPinned() {
		return mPinned;
	}

	/** Hidden in AOSP. */
	public InlineSuggestion getTooltip() {
		return mTooltip;
	}

	@Override
	public String toString() {
		return "InlineSuggestionInfo { inlinePresentationSpec = " + mInlinePresentationSpec + ", source = " + mSource + ", autofillHints = " + Arrays.toString(mAutofillHints) + ", type = " + mType + ", pinned = " + mPinned + ", tooltip = " + mTooltip + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		InlineSuggestionInfo that = (InlineSuggestionInfo)o;
		return Objects.equals(mInlinePresentationSpec, that.mInlinePresentationSpec) && Objects.equals(mSource, that.mSource) && Arrays.equals(mAutofillHints, that.mAutofillHints) && Objects.equals(mType, that.mType) && mPinned == that.mPinned && Objects.equals(mTooltip, that.mTooltip);
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + Objects.hashCode(mInlinePresentationSpec);
		_hash = 31 * _hash + Objects.hashCode(mSource);
		_hash = 31 * _hash + Arrays.hashCode(mAutofillHints);
		_hash = 31 * _hash + Objects.hashCode(mType);
		_hash = 31 * _hash + Boolean.hashCode(mPinned);
		_hash = 31 * _hash + Objects.hashCode(mTooltip);
		return _hash;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedObject(mInlinePresentationSpec, flags);
		dest.writeString(mSource);
		dest.writeStringArray(mAutofillHints);
		dest.writeString(mType);
		dest.writeInt(mPinned ? 1 : 0);
		dest.writeTypedObject(mTooltip, flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<InlineSuggestionInfo> CREATOR = new Parcelable.Creator<InlineSuggestionInfo>() {
		@Override
		public InlineSuggestionInfo[] newArray(int size) {
			return new InlineSuggestionInfo[size];
		}

		@Override
		public InlineSuggestionInfo createFromParcel(Parcel in) {
			InlinePresentationSpec spec = in.readTypedObject(InlinePresentationSpec.CREATOR);
			String source = in.readString();
			String[] hints = in.createStringArray();
			String type = in.readString();
			boolean pinned = in.readInt() != 0;
			InlineSuggestion tooltip = in.readTypedObject(InlineSuggestion.CREATOR);
			return new InlineSuggestionInfo(spec, source, hints, type, pinned, tooltip);
		}
	};
}
