package android.service.autofill;

import static android.service.autofill.AutofillServiceHelper.assertValid;

import android.content.IntentSender;
import android.view.autofill.AutofillId;
import com.android.internal.util.Preconditions;
import java.util.Objects;

/* AOSP SaveInfo; Parcelable and debug toString left out. CustomDescription, Validator and Sanitizer (with their
 * getters and the Builder methods setCustomDescription/setValidator/addSanitizer) are not ported: those classes
 * do not exist in ATL */
public final class SaveInfo {

	public static final int SAVE_DATA_TYPE_GENERIC = 0x0;
	public static final int SAVE_DATA_TYPE_PASSWORD = 0x01;
	public static final int SAVE_DATA_TYPE_ADDRESS = 0x02;
	public static final int SAVE_DATA_TYPE_CREDIT_CARD = 0x04;
	public static final int SAVE_DATA_TYPE_USERNAME = 0x08;
	public static final int SAVE_DATA_TYPE_EMAIL_ADDRESS = 0x10;
	public static final int SAVE_DATA_TYPE_DEBIT_CARD = 0x20;
	public static final int SAVE_DATA_TYPE_PAYMENT_CARD = 0x40;
	public static final int SAVE_DATA_TYPE_GENERIC_CARD = 0x80;

	public static final int NEGATIVE_BUTTON_STYLE_CANCEL = 0;
	public static final int NEGATIVE_BUTTON_STYLE_REJECT = 1;
	public static final int NEGATIVE_BUTTON_STYLE_NEVER = 2;

	public static final int POSITIVE_BUTTON_STYLE_SAVE = 0;
	public static final int POSITIVE_BUTTON_STYLE_CONTINUE = 1;

	public static final int FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE = 0x1;
	public static final int FLAG_DONT_SAVE_ON_FINISH = 0x2;
	public static final int FLAG_DELAY_SAVE = 0x4;

	private final int mType;
	private final int mNegativeButtonStyle;
	private final int mPositiveButtonStyle;
	private final IntentSender mNegativeActionListener;
	private final AutofillId[] mRequiredIds;
	private final AutofillId[] mOptionalIds;
	private final CharSequence mDescription;
	private final int mFlags;
	private final AutofillId mTriggerId;

	/* AOSP SaveInfo.copy */
	public static SaveInfo copy(SaveInfo s, AutofillId[] optionalIds) {
		return new SaveInfo(s.mType, s.mNegativeButtonStyle, s.mPositiveButtonStyle, s.mNegativeActionListener, s.mRequiredIds, assertValid(optionalIds), s.mDescription, s.mFlags, s.mTriggerId);
	}

	private SaveInfo(int type, int negativeButtonStyle, int positiveButtonStyle, IntentSender negativeActionListener, AutofillId[] requiredIds, AutofillId[] optionalIds, CharSequence description, int flags, AutofillId triggerId) {
		mType = type;
		mNegativeButtonStyle = negativeButtonStyle;
		mNegativeActionListener = negativeActionListener;
		mPositiveButtonStyle = positiveButtonStyle;
		mRequiredIds = requiredIds;
		mOptionalIds = optionalIds;
		mDescription = description;
		mFlags = flags;
		mTriggerId = triggerId;
	}

	private SaveInfo(Builder builder) {
		mType = builder.mType;
		mNegativeButtonStyle = builder.mNegativeButtonStyle;
		mNegativeActionListener = builder.mNegativeActionListener;
		mPositiveButtonStyle = builder.mPositiveButtonStyle;
		mRequiredIds = builder.mRequiredIds;
		mOptionalIds = builder.mOptionalIds;
		mDescription = builder.mDescription;
		mFlags = builder.mFlags;
		mTriggerId = builder.mTriggerId;
	}

	public int getNegativeActionStyle() {
		return mNegativeButtonStyle;
	}

	public IntentSender getNegativeActionListener() {
		return mNegativeActionListener;
	}

	public int getPositiveActionStyle() {
		return mPositiveButtonStyle;
	}

	public AutofillId[] getRequiredIds() {
		return mRequiredIds;
	}

	public AutofillId[] getOptionalIds() {
		return mOptionalIds;
	}

	public int getType() {
		return mType;
	}

	public int getFlags() {
		return mFlags;
	}

	public CharSequence getDescription() {
		return mDescription;
	}

	public AutofillId getTriggerId() {
		return mTriggerId;
	}

	public static final class Builder {

		private final int mType;
		private int mNegativeButtonStyle = NEGATIVE_BUTTON_STYLE_CANCEL;
		private int mPositiveButtonStyle = POSITIVE_BUTTON_STYLE_SAVE;
		private IntentSender mNegativeActionListener;
		private final AutofillId[] mRequiredIds;
		private AutofillId[] mOptionalIds;
		private CharSequence mDescription;
		private boolean mDestroyed;
		private int mFlags;
		private AutofillId mTriggerId;

		public Builder(int type, AutofillId[] requiredIds) {
			mType = type;
			mRequiredIds = assertValid(requiredIds);
		}

		public Builder(int type) {
			mType = type;
			mRequiredIds = null;
		}

		/* AOSP SaveInfo.Builder.setFlags */
		public Builder setFlags(int flags) {
			throwIfDestroyed();
			mFlags = Preconditions.checkFlagsArgument(flags, FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE | FLAG_DONT_SAVE_ON_FINISH | FLAG_DELAY_SAVE);
			return this;
		}

		/* AOSP SaveInfo.Builder.setOptionalIds */
		public Builder setOptionalIds(AutofillId[] ids) {
			throwIfDestroyed();
			mOptionalIds = assertValid(ids);
			return this;
		}

		/* AOSP SaveInfo.Builder.setDescription (without the CustomDescription exclusion check) */
		public Builder setDescription(CharSequence description) {
			throwIfDestroyed();
			mDescription = description;
			return this;
		}

		/* AOSP SaveInfo.Builder.setNegativeAction */
		public Builder setNegativeAction(int style, IntentSender listener) {
			throwIfDestroyed();
			Preconditions.checkArgumentInRange(style, NEGATIVE_BUTTON_STYLE_CANCEL, NEGATIVE_BUTTON_STYLE_NEVER, "style");
			mNegativeButtonStyle = style;
			mNegativeActionListener = listener;
			return this;
		}

		/* AOSP SaveInfo.Builder.setPositiveAction */
		public Builder setPositiveAction(int style) {
			throwIfDestroyed();
			Preconditions.checkArgumentInRange(style, POSITIVE_BUTTON_STYLE_SAVE, POSITIVE_BUTTON_STYLE_CONTINUE, "style");
			mPositiveButtonStyle = style;
			return this;
		}

		/* AOSP SaveInfo.Builder.setTriggerId */
		public Builder setTriggerId(AutofillId id) {
			throwIfDestroyed();
			mTriggerId = Objects.requireNonNull(id);
			return this;
		}

		/* AOSP SaveInfo.Builder.build */
		public SaveInfo build() {
			throwIfDestroyed();
			mDestroyed = true;
			return new SaveInfo(this);
		}

		private void throwIfDestroyed() {
			if (mDestroyed) {
				throw new IllegalStateException("Already called #build()");
			}
		}
	}
}
