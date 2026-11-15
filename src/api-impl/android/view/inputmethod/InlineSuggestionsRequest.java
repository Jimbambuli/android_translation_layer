package android.view.inputmethod;

import android.app.ActivityThread;
import android.os.Bundle;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.inline.InlinePresentationSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * android-16 frameworks/base core/java/android/view/inputmethod/InlineSuggestionsRequest.java (codegen data class
 * with Builder). Builder defaults as in AOSP; the default supported locales are the empty list
 * (IME_AUTOFILL_DEFAULT_SUPPORTED_LOCALES_IS_EMPTY is enabled for target SDK >= S). The host input token is not
 * parcelled (no binders in ATL).
 */
public final class InlineSuggestionsRequest implements Parcelable {

	public static final int SUGGESTION_COUNT_UNLIMITED = Integer.MAX_VALUE;

	private final int mMaxSuggestionCount;
	private final List<InlinePresentationSpec> mInlinePresentationSpecs;
	private String mHostPackageName;
	private LocaleList mSupportedLocales;
	private Bundle mExtras;
	private IBinder mHostInputToken;
	private int mHostDisplayId;
	private InlinePresentationSpec mInlineTooltipPresentationSpec;

	/** Hidden in AOSP. */
	public void setHostInputToken(IBinder hostInputToken) {
		mHostInputToken = hostInputToken;
	}

	/** Hidden in AOSP. */
	public void setHostDisplayId(int hostDisplayId) {
		mHostDisplayId = hostDisplayId;
	}

	InlineSuggestionsRequest(int maxSuggestionCount, List<InlinePresentationSpec> inlinePresentationSpecs, String hostPackageName, LocaleList supportedLocales, Bundle extras, IBinder hostInputToken, int hostDisplayId, InlinePresentationSpec inlineTooltipPresentationSpec) {
		mMaxSuggestionCount = maxSuggestionCount;
		mInlinePresentationSpecs = Objects.requireNonNull(inlinePresentationSpecs);
		mHostPackageName = Objects.requireNonNull(hostPackageName);
		mSupportedLocales = Objects.requireNonNull(supportedLocales);
		mExtras = Objects.requireNonNull(extras);
		mHostInputToken = hostInputToken;
		mHostDisplayId = hostDisplayId;
		mInlineTooltipPresentationSpec = inlineTooltipPresentationSpec;
		// onConstructed()
		if (mInlinePresentationSpecs.isEmpty())
			throw new IllegalStateException();
		if (mMaxSuggestionCount < mInlinePresentationSpecs.size())
			throw new IllegalStateException();
	}

	public int getMaxSuggestionCount() {
		return mMaxSuggestionCount;
	}

	public List<InlinePresentationSpec> getInlinePresentationSpecs() {
		return mInlinePresentationSpecs;
	}

	public String getHostPackageName() {
		return mHostPackageName;
	}

	public LocaleList getSupportedLocales() {
		return mSupportedLocales;
	}

	public Bundle getExtras() {
		return mExtras;
	}

	/** Hidden in AOSP. */
	public IBinder getHostInputToken() {
		return mHostInputToken;
	}

	/** Hidden in AOSP. */
	public int getHostDisplayId() {
		return mHostDisplayId;
	}

	public InlinePresentationSpec getInlineTooltipPresentationSpec() {
		return mInlineTooltipPresentationSpec;
	}

	@Override
	public String toString() {
		return "InlineSuggestionsRequest { maxSuggestionCount = " + mMaxSuggestionCount + ", inlinePresentationSpecs = " + mInlinePresentationSpecs + ", hostPackageName = " + mHostPackageName + ", supportedLocales = " + mSupportedLocales + ", extras = " + mExtras + ", hostInputToken = " + mHostInputToken + ", hostDisplayId = " + mHostDisplayId + ", inlineTooltipPresentationSpec = " + mInlineTooltipPresentationSpec + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		InlineSuggestionsRequest that = (InlineSuggestionsRequest)o;
		return mMaxSuggestionCount == that.mMaxSuggestionCount && Objects.equals(mInlinePresentationSpecs, that.mInlinePresentationSpecs) && Objects.equals(mHostPackageName, that.mHostPackageName) && Objects.equals(mSupportedLocales, that.mSupportedLocales) && Objects.equals(mExtras, that.mExtras) && Objects.equals(mHostInputToken, that.mHostInputToken) && mHostDisplayId == that.mHostDisplayId && Objects.equals(mInlineTooltipPresentationSpec, that.mInlineTooltipPresentationSpec);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mMaxSuggestionCount, mInlinePresentationSpecs, mHostPackageName, mSupportedLocales, mExtras, mHostInputToken, mHostDisplayId, mInlineTooltipPresentationSpec);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mMaxSuggestionCount);
		dest.writeTypedList(mInlinePresentationSpecs);
		dest.writeString(mHostPackageName);
		dest.writeTypedObject(mSupportedLocales, flags);
		dest.writeBundle(mExtras);
		dest.writeInt(mHostDisplayId);
		dest.writeTypedObject(mInlineTooltipPresentationSpec, flags);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<InlineSuggestionsRequest> CREATOR = new Parcelable.Creator<InlineSuggestionsRequest>() {
		@Override
		public InlineSuggestionsRequest[] newArray(int size) {
			return new InlineSuggestionsRequest[size];
		}

		@Override
		public InlineSuggestionsRequest createFromParcel(Parcel in) {
			int max = in.readInt();
			List<InlinePresentationSpec> specs = in.createTypedArrayList(InlinePresentationSpec.CREATOR);
			String host = in.readString();
			LocaleList locales = in.readTypedObject(LocaleList.CREATOR);
			Bundle extras = in.readBundle();
			int displayId = in.readInt();
			InlinePresentationSpec tooltip = in.readTypedObject(InlinePresentationSpec.CREATOR);
			return new InlineSuggestionsRequest(max, specs, host, locales, extras, null, displayId, tooltip);
		}
	};

	public static final class Builder {

		private int mMaxSuggestionCount = SUGGESTION_COUNT_UNLIMITED;
		private List<InlinePresentationSpec> mInlinePresentationSpecs;
		private String mHostPackageName;
		private LocaleList mSupportedLocales;
		private Bundle mExtras;
		private IBinder mHostInputToken;
		private int mHostDisplayId = -1; // Display.INVALID_DISPLAY
		private InlinePresentationSpec mInlineTooltipPresentationSpec;
		private boolean mUsed;

		public Builder(List<InlinePresentationSpec> inlinePresentationSpecs) {
			mInlinePresentationSpecs = Objects.requireNonNull(inlinePresentationSpecs);
		}

		public Builder setMaxSuggestionCount(int value) {
			checkNotUsed();
			mMaxSuggestionCount = value;
			return this;
		}

		public Builder setInlinePresentationSpecs(List<InlinePresentationSpec> value) {
			checkNotUsed();
			mInlinePresentationSpecs = value;
			return this;
		}

		public Builder addInlinePresentationSpecs(InlinePresentationSpec value) {
			if (mInlinePresentationSpecs == null)
				setInlinePresentationSpecs(new ArrayList<>());
			mInlinePresentationSpecs.add(value);
			return this;
		}

		Builder setHostPackageName(String value) {
			checkNotUsed();
			mHostPackageName = value;
			return this;
		}

		public Builder setSupportedLocales(LocaleList value) {
			checkNotUsed();
			mSupportedLocales = value;
			return this;
		}

		public Builder setExtras(Bundle value) {
			checkNotUsed();
			mExtras = value;
			return this;
		}

		Builder setHostInputToken(IBinder value) {
			checkNotUsed();
			mHostInputToken = value;
			return this;
		}

		Builder setHostDisplayId(int value) {
			checkNotUsed();
			mHostDisplayId = value;
			return this;
		}

		public Builder setInlineTooltipPresentationSpec(InlinePresentationSpec value) {
			checkNotUsed();
			mInlineTooltipPresentationSpec = value;
			return this;
		}

		public InlineSuggestionsRequest build() {
			checkNotUsed();
			mUsed = true;
			if (mHostPackageName == null)
				mHostPackageName = ActivityThread.currentPackageName();
			if (mSupportedLocales == null)
				mSupportedLocales = LocaleList.getEmptyLocaleList();
			if (mExtras == null)
				mExtras = Bundle.EMPTY;
			return new InlineSuggestionsRequest(mMaxSuggestionCount, mInlinePresentationSpecs, mHostPackageName, mSupportedLocales, mExtras, mHostInputToken, mHostDisplayId, mInlineTooltipPresentationSpec);
		}

		private void checkNotUsed() {
			if (mUsed)
				throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
		}
	}
}
