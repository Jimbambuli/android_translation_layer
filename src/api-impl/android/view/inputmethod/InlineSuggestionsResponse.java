package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.Objects;

/** android-16 frameworks/base core/java/android/view/inputmethod/InlineSuggestionsResponse.java. */
public final class InlineSuggestionsResponse implements Parcelable {

	private final List<InlineSuggestion> mInlineSuggestions;

	/** Hidden in AOSP (newInlineSuggestionsResponse). */
	public static InlineSuggestionsResponse newInlineSuggestionsResponse(List<InlineSuggestion> inlineSuggestions) {
		return new InlineSuggestionsResponse(inlineSuggestions);
	}

	public InlineSuggestionsResponse(List<InlineSuggestion> inlineSuggestions) {
		mInlineSuggestions = Objects.requireNonNull(inlineSuggestions);
	}

	public List<InlineSuggestion> getInlineSuggestions() {
		return mInlineSuggestions;
	}

	@Override
	public String toString() {
		return "InlineSuggestionsResponse { inlineSuggestions = " + mInlineSuggestions + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		return Objects.equals(mInlineSuggestions, ((InlineSuggestionsResponse)o).mInlineSuggestions);
	}

	@Override
	public int hashCode() {
		return 31 + Objects.hashCode(mInlineSuggestions);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(mInlineSuggestions);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<InlineSuggestionsResponse> CREATOR = new Parcelable.Creator<InlineSuggestionsResponse>() {
		@Override
		public InlineSuggestionsResponse[] newArray(int size) {
			return new InlineSuggestionsResponse[size];
		}

		@Override
		public InlineSuggestionsResponse createFromParcel(Parcel in) {
			return new InlineSuggestionsResponse(in.createTypedArrayList(InlineSuggestion.CREATOR));
		}
	};
}
