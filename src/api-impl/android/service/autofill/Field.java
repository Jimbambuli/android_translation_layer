package android.service.autofill;

import android.view.autofill.AutofillValue;
import java.util.regex.Pattern;

/* AOSP Field (codegen data class with Builder) */
public final class Field {

	private AutofillValue mValue;
	private Dataset.DatasetFieldFilter mFilter;
	private Presentations mPresentations;

	Field(AutofillValue value, Dataset.DatasetFieldFilter filter, Presentations presentations) {
		this.mValue = value;
		this.mFilter = filter;
		this.mPresentations = presentations;
	}

	public AutofillValue getValue() {
		return mValue;
	}

	/* AOSP Field.getDatasetFieldFilter */
	public Dataset.DatasetFieldFilter getDatasetFieldFilter() {
		return mFilter;
	}

	/* AOSP Field.getFilter */
	public Pattern getFilter() {
		return mFilter == null ? null : mFilter.pattern;
	}

	public Presentations getPresentations() {
		return mPresentations;
	}

	public static final class Builder {

		private AutofillValue mValue = null;
		private Dataset.DatasetFieldFilter mFilter = null;
		private Presentations mPresentations = null;
		private boolean mDestroyed = false;

		public Builder() {
		}

		public Builder setValue(AutofillValue value) {
			checkNotUsed();
			mValue = value;
			return this;
		}

		/* AOSP Field.Builder.setFilter */
		public Builder setFilter(Pattern value) {
			checkNotUsed();
			mFilter = new Dataset.DatasetFieldFilter(value);
			return this;
		}

		public Builder setPresentations(Presentations value) {
			checkNotUsed();
			mPresentations = value;
			return this;
		}

		public Field build() {
			checkNotUsed();
			mDestroyed = true;
			return new Field(mValue, mFilter, mPresentations);
		}

		private void checkNotUsed() {
			if (mDestroyed) {
				throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
			}
		}
	}
}
