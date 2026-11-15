package android.service.autofill;

import static android.service.autofill.AutofillServiceHelper.assertValid;
import static android.service.autofill.FillRequest.INVALID_REQUEST_ID;

import android.app.PendingIntent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.autofill.AutofillId;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP FillResponse; Parcelable (ParceledListSlice replaced by a plain List) and debug toString left out.
 * UserData, FieldClassification and the field classification members (setUserData, setFieldClassificationIds,
 * setDetectedFieldClassifications and their getters) are not ported: those classes do not exist in ATL */
public final class FillResponse {

	public static final int FLAG_TRACK_CONTEXT_COMMITED = 0x1;
	public static final int FLAG_DISABLE_ACTIVITY_ONLY = 0x2;
	public static final int FLAG_DELAY_FILL = 0x4;
	public static final int FLAG_CREDENTIAL_MANAGER_RESPONSE = 0x8;

	private final List<Dataset> mDatasets;
	private final SaveInfo mSaveInfo;
	private final Bundle mClientState;
	private final RemoteViews mPresentation;
	private final InlinePresentation mInlinePresentation;
	private final InlinePresentation mInlineTooltipPresentation;
	private final RemoteViews mDialogPresentation;
	private final RemoteViews mDialogHeader;
	private final RemoteViews mHeader;
	private final RemoteViews mFooter;
	private final IntentSender mAuthentication;
	private final AutofillId[] mAuthenticationIds;
	private final AutofillId[] mIgnoredIds;
	private final AutofillId[] mFillDialogTriggerIds;
	private final long mDisableDuration;
	private final int mFlags;
	private int mRequestId;
	private final int[] mCancelIds;
	private final boolean mSupportsInlineSuggestions;
	private final int mIconResourceId;
	private final int mServiceDisplayNameResourceId;
	private final boolean mShowFillDialogIcon;
	private final boolean mShowSaveDialogIcon;
	private final PendingIntent mDialogPendingIntent;

	private FillResponse(Builder builder) {
		mDatasets = builder.mDatasets;
		mSaveInfo = builder.mSaveInfo;
		mClientState = builder.mClientState;
		mPresentation = builder.mPresentation;
		mInlinePresentation = builder.mInlinePresentation;
		mInlineTooltipPresentation = builder.mInlineTooltipPresentation;
		mDialogPresentation = builder.mDialogPresentation;
		mDialogHeader = builder.mDialogHeader;
		mHeader = builder.mHeader;
		mFooter = builder.mFooter;
		mAuthentication = builder.mAuthentication;
		mAuthenticationIds = builder.mAuthenticationIds;
		mFillDialogTriggerIds = builder.mFillDialogTriggerIds;
		mIgnoredIds = builder.mIgnoredIds;
		mDisableDuration = builder.mDisableDuration;
		mFlags = builder.mFlags;
		mRequestId = INVALID_REQUEST_ID;
		mCancelIds = builder.mCancelIds;
		mSupportsInlineSuggestions = builder.mSupportsInlineSuggestions;
		mIconResourceId = builder.mIconResourceId;
		mServiceDisplayNameResourceId = builder.mServiceDisplayNameResourceId;
		mShowFillDialogIcon = builder.mShowFillDialogIcon;
		mShowSaveDialogIcon = builder.mShowSaveDialogIcon;
		mDialogPendingIntent = builder.mDialogPendingIntent;
	}

	public Bundle getClientState() {
		return mClientState;
	}

	public List<Dataset> getDatasets() {
		return mDatasets;
	}

	public SaveInfo getSaveInfo() {
		return mSaveInfo;
	}

	public RemoteViews getPresentation() {
		return mPresentation;
	}

	public InlinePresentation getInlinePresentation() {
		return mInlinePresentation;
	}

	public InlinePresentation getInlineTooltipPresentation() {
		return mInlineTooltipPresentation;
	}

	public RemoteViews getDialogPresentation() {
		return mDialogPresentation;
	}

	public RemoteViews getDialogHeader() {
		return mDialogHeader;
	}

	public RemoteViews getHeader() {
		return mHeader;
	}

	public RemoteViews getFooter() {
		return mFooter;
	}

	public IntentSender getAuthentication() {
		return mAuthentication;
	}

	public AutofillId[] getAuthenticationIds() {
		return mAuthenticationIds;
	}

	public AutofillId[] getFillDialogTriggerIds() {
		return mFillDialogTriggerIds;
	}

	public AutofillId[] getIgnoredIds() {
		return mIgnoredIds;
	}

	public long getDisableDuration() {
		return mDisableDuration;
	}

	public int getIconResourceId() {
		return mIconResourceId;
	}

	public int getServiceDisplayNameResourceId() {
		return mServiceDisplayNameResourceId;
	}

	public boolean getShowFillDialogIcon() {
		return mShowFillDialogIcon;
	}

	public boolean getShowSaveDialogIcon() {
		return mShowSaveDialogIcon;
	}

	public int getFlags() {
		return mFlags;
	}

	public void setRequestId(int requestId) {
		mRequestId = requestId;
	}

	public int getRequestId() {
		return mRequestId;
	}

	public int[] getCancelIds() {
		return mCancelIds;
	}

	public boolean supportsInlineSuggestions() {
		return mSupportsInlineSuggestions;
	}

	public static final class Builder {

		private ArrayList<Dataset> mDatasets;
		private SaveInfo mSaveInfo;
		private Bundle mClientState;
		private RemoteViews mPresentation;
		private InlinePresentation mInlinePresentation;
		private InlinePresentation mInlineTooltipPresentation;
		private RemoteViews mDialogPresentation;
		private RemoteViews mDialogHeader;
		private RemoteViews mHeader;
		private RemoteViews mFooter;
		private IntentSender mAuthentication;
		private AutofillId[] mAuthenticationIds;
		private AutofillId[] mIgnoredIds;
		private long mDisableDuration;
		private AutofillId[] mFillDialogTriggerIds;
		private int mFlags;
		private boolean mDestroyed;
		private int[] mCancelIds;
		private boolean mSupportsInlineSuggestions;
		private int mIconResourceId;
		private int mServiceDisplayNameResourceId;
		private boolean mShowFillDialogIcon = true;
		private boolean mShowSaveDialogIcon = true;
		private PendingIntent mDialogPendingIntent;

		/* AOSP FillResponse.Builder() has no explicit constructor */
		public Builder() {
		}

		/* AOSP FillResponse.Builder.setAuthentication(AutofillId[], IntentSender, RemoteViews) */
		public Builder setAuthentication(AutofillId[] ids, IntentSender authentication, RemoteViews presentation) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			if (mHeader != null || mFooter != null) {
				throw new IllegalStateException("Already called #setHeader() or #setFooter()");
			}
			if (authentication == null ^ presentation == null) {
				throw new IllegalArgumentException("authentication and presentation" + " must be both non-null or null");
			}
			mAuthentication = authentication;
			mPresentation = presentation;
			mAuthenticationIds = assertValid(ids);
			return this;
		}

		/* AOSP FillResponse.Builder.setAuthentication(AutofillId[], IntentSender, RemoteViews, InlinePresentation) */
		public Builder setAuthentication(AutofillId[] ids, IntentSender authentication, RemoteViews presentation, InlinePresentation inlinePresentation) {
			return setAuthentication(ids, authentication, presentation, inlinePresentation, null);
		}

		/* AOSP FillResponse.Builder.setAuthentication(..., InlinePresentation, InlinePresentation) */
		public Builder setAuthentication(AutofillId[] ids, IntentSender authentication, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			return setAuthentication(ids, authentication, presentation, inlinePresentation, inlineTooltipPresentation, null);
		}

		/* AOSP FillResponse.Builder.setAuthentication(AutofillId[], IntentSender, Presentations) */
		public Builder setAuthentication(AutofillId[] ids, IntentSender authentication, Presentations presentations) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			if (presentations == null) {
				return setAuthentication(ids, authentication, null, null, null, null);
			}
			return setAuthentication(ids, authentication, presentations.getMenuPresentation(), presentations.getInlinePresentation(), presentations.getInlineTooltipPresentation(), presentations.getDialogPresentation());
		}

		/* AOSP FillResponse.Builder.setAuthentication (private variant with dialogPresentation) */
		private Builder setAuthentication(AutofillId[] ids, IntentSender authentication, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation, RemoteViews dialogPresentation) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			if (mHeader != null || mFooter != null) {
				throw new IllegalStateException("Already called #setHeader() or #setFooter()");
			}
			if (authentication == null ^ (presentation == null && inlinePresentation == null)) {
				throw new IllegalArgumentException("authentication and presentation " + "(dropdown or inline), must be both non-null or null");
			}
			mAuthentication = authentication;
			mPresentation = presentation;
			mInlinePresentation = inlinePresentation;
			mInlineTooltipPresentation = inlineTooltipPresentation;
			mDialogPresentation = dialogPresentation;
			mAuthenticationIds = assertValid(ids);
			return this;
		}

		public Builder setIgnoredIds(AutofillId... ids) {
			throwIfDestroyed();
			mIgnoredIds = ids;
			return this;
		}

		/* AOSP FillResponse.Builder.addDataset */
		public Builder addDataset(Dataset dataset) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			if (dataset == null) {
				return this;
			}
			if (mDatasets == null) {
				mDatasets = new ArrayList<>();
			}
			mDatasets.add(dataset);
			return this;
		}

		public Builder setDatasets(ArrayList<Dataset> dataset) {
			mDatasets = dataset;
			return this;
		}

		public Builder setSaveInfo(SaveInfo saveInfo) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			mSaveInfo = saveInfo;
			return this;
		}

		public Builder setClientState(Bundle clientState) {
			throwIfDestroyed();
			throwIfDisableAutofillCalled();
			mClientState = clientState;
			return this;
		}

		/* AOSP FillResponse.Builder.setFlags */
		public Builder setFlags(int flags) {
			throwIfDestroyed();
			mFlags = Preconditions.checkFlagsArgument(flags, FLAG_TRACK_CONTEXT_COMMITED | FLAG_DISABLE_ACTIVITY_ONLY | FLAG_DELAY_FILL | FLAG_CREDENTIAL_MANAGER_RESPONSE);
			return this;
		}

		/* AOSP FillResponse.Builder.disableAutofill */
		public Builder disableAutofill(long duration) {
			throwIfDestroyed();
			if (duration <= 0) {
				throw new IllegalArgumentException("duration must be greater than 0");
			}
			if (mAuthentication != null || mDatasets != null || mSaveInfo != null || mClientState != null) {
				throw new IllegalStateException("disableAutofill() must be the only method called");
			}
			mDisableDuration = duration;
			return this;
		}

		public Builder setIconResourceId(int id) {
			throwIfDestroyed();
			mIconResourceId = id;
			return this;
		}

		public Builder setServiceDisplayNameResourceId(int id) {
			throwIfDestroyed();
			mServiceDisplayNameResourceId = id;
			return this;
		}

		public Builder setShowFillDialogIcon(boolean show) {
			throwIfDestroyed();
			mShowFillDialogIcon = show;
			return this;
		}

		public Builder setShowSaveDialogIcon(boolean show) {
			throwIfDestroyed();
			mShowSaveDialogIcon = show;
			return this;
		}

		/* AOSP FillResponse.Builder.setHeader */
		public Builder setHeader(RemoteViews header) {
			throwIfDestroyed();
			throwIfAuthenticationCalled();
			mHeader = Objects.requireNonNull(header);
			return this;
		}

		/* AOSP FillResponse.Builder.setFooter */
		public Builder setFooter(RemoteViews footer) {
			throwIfDestroyed();
			throwIfAuthenticationCalled();
			mFooter = Objects.requireNonNull(footer);
			return this;
		}

		public Builder setPresentationCancelIds(int[] ids) {
			throwIfDestroyed();
			mCancelIds = ids;
			return this;
		}

		/* AOSP FillResponse.Builder.setDialogHeader */
		public Builder setDialogHeader(RemoteViews header) {
			throwIfDestroyed();
			Objects.requireNonNull(header);
			mDialogHeader = header;
			return this;
		}

		/* AOSP FillResponse.Builder.setFillDialogTriggerIds */
		public Builder setFillDialogTriggerIds(AutofillId... ids) {
			throwIfDestroyed();
			Preconditions.checkArrayElementsNotNull(ids, "ids");
			mFillDialogTriggerIds = ids;
			return this;
		}

		/* AOSP FillResponse.Builder.setDialogPendingIntent */
		public Builder setDialogPendingIntent(PendingIntent pendingIntent) {
			throwIfDestroyed();
			Objects.requireNonNull(pendingIntent, "can't pass a null object to setDialogPendingIntent");
			mDialogPendingIntent = pendingIntent;
			return this;
		}

		/* AOSP FillResponse.Builder.build (without the field classification condition) */
		public FillResponse build() {
			throwIfDestroyed();
			if (mAuthentication == null && mDatasets == null && mSaveInfo == null && mDisableDuration == 0 && mClientState == null) {
				throw new IllegalStateException("need to provide: at least one DataSet, or a " + "SaveInfo, or an authentication with a presentation, " + "or a FieldsDetection, or a client state, or disable autofill");
			}
			if (mDatasets == null && (mHeader != null || mFooter != null)) {
				throw new IllegalStateException("must add at least 1 dataset when using header or footer");
			}
			if (mDatasets != null) {
				for (final Dataset dataset : mDatasets) {
					if (dataset.getFieldInlinePresentation(0) != null) {
						mSupportsInlineSuggestions = true;
						break;
					}
				}
			} else if (mInlinePresentation != null) {
				mSupportsInlineSuggestions = true;
			}
			mDestroyed = true;
			return new FillResponse(this);
		}

		private void throwIfDestroyed() {
			if (mDestroyed) {
				throw new IllegalStateException("Already called #build()");
			}
		}

		private void throwIfDisableAutofillCalled() {
			if (mDisableDuration > 0) {
				throw new IllegalStateException("Already called #disableAutofill()");
			}
		}

		private void throwIfAuthenticationCalled() {
			if (mAuthentication != null) {
				throw new IllegalStateException("Already called #setAuthentication()");
			}
		}
	}
}
