package android.view.inputmethod;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import android.view.ViewGroup;
import android.widget.inline.InlineContentView;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * android-16 frameworks/base core/java/android/view/inputmethod/InlineSuggestion.java: an inline suggestion
 * with its info and (in AOSP) a remote content provider. ATL has no autofill service that supplies content
 * surfaces, so mContentProvider is always null; AOSP inflate() then reports a null view to the callback.
 */
public final class InlineSuggestion implements Parcelable {

	private final InlineSuggestionInfo mInfo;

	/** Hidden in AOSP (newInlineSuggestion). */
	public static InlineSuggestion newInlineSuggestion(InlineSuggestionInfo info) {
		return new InlineSuggestion(info);
	}

	public InlineSuggestion(InlineSuggestionInfo info) {
		mInfo = Objects.requireNonNull(info);
	}

	public InlineSuggestionInfo getInfo() {
		return mInfo;
	}

	public void inflate(Context context, Size size, Executor callbackExecutor, Consumer<InlineContentView> callback) {
		Size minSize = mInfo.getInlinePresentationSpec().getMinSize();
		Size maxSize = mInfo.getInlinePresentationSpec().getMaxSize();
		if (!isValid(size.getWidth(), minSize.getWidth(), maxSize.getWidth()) || !isValid(size.getHeight(), minSize.getHeight(), maxSize.getHeight())) {
			throw new IllegalArgumentException("size is neither between min:" + minSize + " and max:" + maxSize + ", nor wrap_content");
		}
		// mContentProvider == null
		callbackExecutor.execute(() -> callback.accept(null));
	}

	private static boolean isValid(int actual, int min, int max) {
		if (actual == ViewGroup.LayoutParams.WRAP_CONTENT)
			return true;
		return actual >= min && actual <= max;
	}

	@Override
	public String toString() {
		return "InlineSuggestion { info = " + mInfo + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		return Objects.equals(mInfo, ((InlineSuggestion)o).mInfo);
	}

	@Override
	public int hashCode() {
		return 31 + Objects.hashCode(mInfo);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedObject(mInfo, flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<InlineSuggestion> CREATOR = new Parcelable.Creator<InlineSuggestion>() {
		@Override
		public InlineSuggestion[] newArray(int size) {
			return new InlineSuggestion[size];
		}

		@Override
		public InlineSuggestion createFromParcel(Parcel in) {
			return new InlineSuggestion(in.readTypedObject(InlineSuggestionInfo.CREATOR));
		}
	};
}
