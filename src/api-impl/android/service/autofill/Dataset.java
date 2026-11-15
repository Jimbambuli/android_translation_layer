package android.service.autofill;

import android.content.ClipData;
import android.content.Intent;
import android.content.IntentSender;
import android.util.ArrayMap;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

/* AOSP Dataset; Parcelable, toString debug output and the AIDL-only constructors left out */
public final class Dataset {

	public static final int PICK_REASON_UNKNOWN = 0;
	public static final int PICK_REASON_NO_PCC = 1;
	public static final int PICK_REASON_PROVIDER_DETECTION_ONLY = 2;
	public static final int PICK_REASON_PROVIDER_DETECTION_PREFERRED_WITH_PCC = 3;
	public static final int PICK_REASON_PCC_DETECTION_ONLY = 4;
	public static final int PICK_REASON_PCC_DETECTION_PREFERRED_WITH_PROVIDER = 5;

	private int mEligibleReason;
	private final ArrayList<AutofillId> mFieldIds;
	private final ArrayList<AutofillValue> mFieldValues;
	private final ArrayList<RemoteViews> mFieldPresentations;
	private final ArrayList<RemoteViews> mFieldDialogPresentations;
	private final ArrayList<InlinePresentation> mFieldInlinePresentations;
	private final ArrayList<InlinePresentation> mFieldInlineTooltipPresentations;
	private final ArrayList<DatasetFieldFilter> mFieldFilters;
	private final ArrayList<String> mAutofillDatatypes;
	private final ClipData mFieldContent;
	private final RemoteViews mPresentation;
	private final RemoteViews mDialogPresentation;
	private final InlinePresentation mInlinePresentation;
	private final InlinePresentation mInlineTooltipPresentation;
	private final IntentSender mAuthentication;
	private Intent mCredentialFillInIntent;
	String mId;

	private Dataset(Builder builder) {
		mFieldIds = builder.mFieldIds;
		mFieldValues = builder.mFieldValues;
		mFieldPresentations = builder.mFieldPresentations;
		mFieldDialogPresentations = builder.mFieldDialogPresentations;
		mFieldInlinePresentations = builder.mFieldInlinePresentations;
		mFieldInlineTooltipPresentations = builder.mFieldInlineTooltipPresentations;
		mFieldFilters = builder.mFieldFilters;
		mFieldContent = builder.mFieldContent;
		mPresentation = builder.mPresentation;
		mDialogPresentation = builder.mDialogPresentation;
		mInlinePresentation = builder.mInlinePresentation;
		mInlineTooltipPresentation = builder.mInlineTooltipPresentation;
		mAuthentication = builder.mAuthentication;
		mCredentialFillInIntent = builder.mCredentialFillInIntent;
		mId = builder.mId;
		mAutofillDatatypes = builder.mAutofillDatatypes;
	}

	public ArrayList<String> getAutofillDatatypes() {
		return mAutofillDatatypes;
	}

	public ArrayList<AutofillId> getFieldIds() {
		return mFieldIds;
	}

	public ArrayList<AutofillValue> getFieldValues() {
		return mFieldValues;
	}

	/* AOSP Dataset.getFieldPresentation */
	public RemoteViews getFieldPresentation(int index) {
		final RemoteViews customPresentation = mFieldPresentations.get(index);
		return customPresentation != null ? customPresentation : mPresentation;
	}

	/* AOSP Dataset.getFieldDialogPresentation */
	public RemoteViews getFieldDialogPresentation(int index) {
		final RemoteViews customPresentation = mFieldDialogPresentations.get(index);
		return customPresentation != null ? customPresentation : mDialogPresentation;
	}

	/* AOSP Dataset.getFieldInlinePresentation */
	public InlinePresentation getFieldInlinePresentation(int index) {
		final InlinePresentation inlinePresentation = mFieldInlinePresentations.get(index);
		return inlinePresentation != null ? inlinePresentation : mInlinePresentation;
	}

	/* AOSP Dataset.getFieldInlineTooltipPresentation */
	public InlinePresentation getFieldInlineTooltipPresentation(int index) {
		final InlinePresentation inlineTooltipPresentation = mFieldInlineTooltipPresentations.get(index);
		return inlineTooltipPresentation != null ? inlineTooltipPresentation : mInlineTooltipPresentation;
	}

	public DatasetFieldFilter getFilter(int index) {
		return mFieldFilters.get(index);
	}

	public ClipData getFieldContent() {
		return mFieldContent;
	}

	public IntentSender getAuthentication() {
		return mAuthentication;
	}

	public Intent getCredentialFillInIntent() {
		return mCredentialFillInIntent;
	}

	public void setCredentialFillInIntent(Intent intent) {
		mCredentialFillInIntent = intent;
	}

	public boolean isEmpty() {
		return mFieldIds == null || mFieldIds.isEmpty();
	}

	public String getId() {
		return mId;
	}

	public void setEligibleReasonReason(int eligibleReason) {
		this.mEligibleReason = eligibleReason;
	}

	public int getEligibleReason() {
		return mEligibleReason;
	}

	public static final class Builder {

		private ArrayList<AutofillId> mFieldIds = new ArrayList<>();
		private ArrayList<AutofillValue> mFieldValues = new ArrayList<>();
		private ArrayList<RemoteViews> mFieldPresentations = new ArrayList<>();
		private ArrayList<RemoteViews> mFieldDialogPresentations = new ArrayList<>();
		private ArrayList<InlinePresentation> mFieldInlinePresentations = new ArrayList<>();
		private ArrayList<InlinePresentation> mFieldInlineTooltipPresentations = new ArrayList<>();
		private ArrayList<DatasetFieldFilter> mFieldFilters = new ArrayList<>();
		private ArrayList<String> mAutofillDatatypes = new ArrayList<>();
		private ClipData mFieldContent;
		private RemoteViews mPresentation;
		private RemoteViews mDialogPresentation;
		private InlinePresentation mInlinePresentation;
		private InlinePresentation mInlineTooltipPresentation;
		private IntentSender mAuthentication;
		private Intent mCredentialFillInIntent;
		private boolean mDestroyed;
		private String mId;
		private ArrayMap<Field, Integer> mFieldToIndexdMap = new ArrayMap<>();

		/* AOSP Dataset.Builder(RemoteViews) */
		public Builder(RemoteViews presentation) {
			Objects.requireNonNull(presentation, "presentation must be non-null");
			mPresentation = presentation;
		}

		/* AOSP Dataset.Builder(InlinePresentation) */
		public Builder(InlinePresentation inlinePresentation) {
			Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
			mInlinePresentation = inlinePresentation;
		}

		/* AOSP Dataset.Builder(Presentations) */
		public Builder(Presentations presentations) {
			Objects.requireNonNull(presentations, "presentations must be non-null");
			mPresentation = presentations.getMenuPresentation();
			mInlinePresentation = presentations.getInlinePresentation();
			mInlineTooltipPresentation = presentations.getInlineTooltipPresentation();
			mDialogPresentation = presentations.getDialogPresentation();
		}

		public Builder() {
		}

		/* AOSP Dataset.Builder.setInlinePresentation(InlinePresentation) */
		public Builder setInlinePresentation(InlinePresentation inlinePresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
			mInlinePresentation = inlinePresentation;
			return this;
		}

		/* AOSP Dataset.Builder.setInlinePresentation(InlinePresentation, InlinePresentation) */
		public Builder setInlinePresentation(InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
			Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation must be non-null");
			mInlinePresentation = inlinePresentation;
			mInlineTooltipPresentation = inlineTooltipPresentation;
			return this;
		}

		public Builder setAuthentication(IntentSender authentication) {
			throwIfDestroyed();
			mAuthentication = authentication;
			return this;
		}

		public Builder setCredentialFillInIntent(Intent credentialFillInIntent) {
			throwIfDestroyed();
			mCredentialFillInIntent = credentialFillInIntent;
			return this;
		}

		public Builder setId(String id) {
			throwIfDestroyed();
			mId = id;
			return this;
		}

		/* AOSP Dataset.Builder.setContent */
		public Builder setContent(AutofillId id, ClipData content) {
			throwIfDestroyed();
			if (content != null) {
				for (int i = 0; i < content.getItemCount(); i++) {
					Preconditions.checkArgument(content.getItemAt(i).getIntent() == null, "Content items cannot contain an Intent: content=" + content);
				}
			}
			setLifeTheUniverseAndEverything(id, null, null, null, null, null, null);
			mFieldContent = content;
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value) {
			throwIfDestroyed();
			setLifeTheUniverseAndEverything(id, value, null, null, null, null, null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, null, null, null, null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, Pattern filter) {
			throwIfDestroyed();
			Preconditions.checkState(mPresentation != null, "Dataset presentation not set on constructor");
			setLifeTheUniverseAndEverything(id, value, null, null, null, new DatasetFieldFilter(filter), null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, null, null, new DatasetFieldFilter(filter), null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, null, null, null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
			Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, inlineTooltipPresentation, null, null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation, InlinePresentation inlinePresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, null, new DatasetFieldFilter(filter), null);
			return this;
		}

		public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(presentation, "presentation cannot be null");
			Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
			Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, inlineTooltipPresentation, new DatasetFieldFilter(filter), null);
			return this;
		}

		/* AOSP Dataset.Builder.setField(AutofillId, Field) */
		public Builder setField(AutofillId id, Field field) {
			throwIfDestroyed();
			if (mFieldToIndexdMap.containsKey(field)) {
				int index = mFieldToIndexdMap.get(field);
				if (mFieldIds.get(index) == null) {
					mFieldIds.set(index, id);
					return this;
				}
			}
			int index;
			if (field == null) {
				index = setLifeTheUniverseAndEverything(id, null, null, null, null, null, null);
			} else {
				final DatasetFieldFilter filter = field.getDatasetFieldFilter();
				final Presentations presentations = field.getPresentations();
				if (presentations == null) {
					index = setLifeTheUniverseAndEverything(id, field.getValue(), null, null, null, filter, null);
				} else {
					index = setLifeTheUniverseAndEverything(id, field.getValue(), presentations.getMenuPresentation(), presentations.getInlinePresentation(), presentations.getInlineTooltipPresentation(), filter, presentations.getDialogPresentation());
				}
			}
			mFieldToIndexdMap.put(field, index);
			return this;
		}

		/* AOSP Dataset.Builder.setField(String, Field) */
		public Builder setField(String hint, Field field) {
			throwIfDestroyed();
			if (mFieldToIndexdMap.containsKey(field)) {
				int index = mFieldToIndexdMap.get(field);
				if (mAutofillDatatypes.get(index) == null) {
					mAutofillDatatypes.set(index, hint);
					return this;
				}
			}
			int index;
			final DatasetFieldFilter filter = field.getDatasetFieldFilter();
			final Presentations presentations = field.getPresentations();
			if (presentations == null) {
				index = setLifeTheUniverseAndEverything(hint, field.getValue(), null, null, null, filter, null);
			} else {
				index = setLifeTheUniverseAndEverything(hint, field.getValue(), presentations.getMenuPresentation(), presentations.getInlinePresentation(), presentations.getInlineTooltipPresentation(), filter, presentations.getDialogPresentation());
			}
			mFieldToIndexdMap.put(field, index);
			return this;
		}

		/* AOSP Dataset.Builder.setFieldForAllHints */
		public Builder setFieldForAllHints(Field field) {
			/* AutofillManager.ANY_HINT is "any" in AOSP; ATL's AutofillManager does not define the constant */
			return setField("any", field);
		}

		/* AOSP Dataset.Builder.setFieldInlinePresentation */
		public Builder setFieldInlinePresentation(AutofillId id, AutofillValue value, Pattern filter, InlinePresentation inlinePresentation) {
			throwIfDestroyed();
			Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
			setLifeTheUniverseAndEverything(id, value, null, inlinePresentation, null, new DatasetFieldFilter(filter), null);
			return this;
		}

		/* AOSP Dataset.Builder.setLifeTheUniverseAndEverything(String, ...) */
		private int setLifeTheUniverseAndEverything(String datatype, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation tooltip, DatasetFieldFilter filter, RemoteViews dialogPresentation) {
			Objects.requireNonNull(datatype, "datatype cannot be null");
			final int existingIdx = mAutofillDatatypes.indexOf(datatype);
			if (existingIdx >= 0) {
				mAutofillDatatypes.add(datatype);
				mFieldValues.set(existingIdx, value);
				mFieldPresentations.set(existingIdx, presentation);
				mFieldDialogPresentations.set(existingIdx, dialogPresentation);
				mFieldInlinePresentations.set(existingIdx, inlinePresentation);
				mFieldInlineTooltipPresentations.set(existingIdx, tooltip);
				mFieldFilters.set(existingIdx, filter);
				return existingIdx;
			}
			mFieldIds.add(null);
			mAutofillDatatypes.add(datatype);
			mFieldValues.add(value);
			mFieldPresentations.add(presentation);
			mFieldDialogPresentations.add(dialogPresentation);
			mFieldInlinePresentations.add(inlinePresentation);
			mFieldInlineTooltipPresentations.add(tooltip);
			mFieldFilters.add(filter);
			return mFieldIds.size() - 1;
		}

		/* AOSP Dataset.Builder.setLifeTheUniverseAndEverything(AutofillId, ...) */
		private int setLifeTheUniverseAndEverything(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation tooltip, DatasetFieldFilter filter, RemoteViews dialogPresentation) {
			Objects.requireNonNull(id, "id cannot be null");
			final int existingIdx = mFieldIds.indexOf(id);
			if (existingIdx >= 0) {
				mFieldValues.set(existingIdx, value);
				mFieldPresentations.set(existingIdx, presentation);
				mFieldDialogPresentations.set(existingIdx, dialogPresentation);
				mFieldInlinePresentations.set(existingIdx, inlinePresentation);
				mFieldInlineTooltipPresentations.set(existingIdx, tooltip);
				mFieldFilters.set(existingIdx, filter);
				return existingIdx;
			}
			mFieldIds.add(id);
			mAutofillDatatypes.add(null);
			mFieldValues.add(value);
			mFieldPresentations.add(presentation);
			mFieldDialogPresentations.add(dialogPresentation);
			mFieldInlinePresentations.add(inlinePresentation);
			mFieldInlineTooltipPresentations.add(tooltip);
			mFieldFilters.add(filter);
			return mFieldIds.size() - 1;
		}

		/* AOSP Dataset.Builder.build */
		public Dataset build() {
			throwIfDestroyed();
			mDestroyed = true;
			if (mFieldIds == null && mAutofillDatatypes == null) {
				throw new IllegalStateException("at least one of field or datatype must be set");
			}
			if (mFieldIds != null && mAutofillDatatypes != null) {
				if (mFieldIds.size() == 0 && mAutofillDatatypes.size() == 0) {
					throw new IllegalStateException("at least one of field or datatype must be set");
				}
			}
			if (mFieldContent != null) {
				if (mFieldIds.size() > 1) {
					throw new IllegalStateException("when filling content, only one field can be filled");
				}
				if (mFieldValues.get(0) != null) {
					throw new IllegalStateException("cannot fill both content and values");
				}
			}
			return new Dataset(this);
		}

		private void throwIfDestroyed() {
			if (mDestroyed) {
				throw new IllegalStateException("Already called #build()");
			}
		}
	}

	/* AOSP Dataset.DatasetFieldFilter */
	public static final class DatasetFieldFilter {

		public final Pattern pattern;

		DatasetFieldFilter(Pattern pattern) {
			this.pattern = pattern;
		}

		public Pattern getPattern() {
			return pattern;
		}
	}
}
