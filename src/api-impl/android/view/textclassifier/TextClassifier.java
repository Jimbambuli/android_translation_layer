package android.view.textclassifier;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Interface for providing text classification related features (AOSP android.view.textclassifier.TextClassifier).
 *
 * ATL has no text classification service, so this interface has the constants and AOSP's (android-16.0.0_r4)
 * default methods for selection, classification, language detection and conversation actions; the result classes
 * of the other features (TextLinks, SelectionEvent) are not in ATL. NO_OP is AOSP's TextClassifier whose methods
 * all keep their defaults.
 */
public interface TextClassifier {
	String LOG_TAG = "androidtc";
	int LOCAL = 0;
	int SYSTEM = 1;
	int DEFAULT_SYSTEM = 2;
	String TYPE_UNKNOWN = "";
	String TYPE_OTHER = "other";
	String TYPE_EMAIL = "email";
	String TYPE_PHONE = "phone";
	String TYPE_ADDRESS = "address";
	String TYPE_URL = "url";
	String TYPE_DATE = "date";
	String TYPE_DATE_TIME = "datetime";
	String TYPE_FLIGHT_NUMBER = "flight";
	String TYPE_OTP_CODE = "otp_code";
	String TYPE_DICTIONARY = "dictionary";
	String HINT_TEXT_IS_EDITABLE = "android.text_is_editable";
	String HINT_TEXT_IS_NOT_EDITABLE = "android.text_is_not_editable";
	String WIDGET_TYPE_TEXTVIEW = "textview";
	String WIDGET_TYPE_EDITTEXT = "edittext";
	String WIDGET_TYPE_UNSELECTABLE_TEXTVIEW = "nosel-textview";
	String WIDGET_TYPE_WEBVIEW = "webview";
	String WIDGET_TYPE_EDIT_WEBVIEW = "edit-webview";
	String WIDGET_TYPE_CUSTOM_TEXTVIEW = "customview";
	String WIDGET_TYPE_CUSTOM_EDITTEXT = "customedit";
	String WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW = "nosel-customview";
	String WIDGET_TYPE_NOTIFICATION = "notification";
	String WIDGET_TYPE_CLIPBOARD = "clipboard";
	String WIDGET_TYPE_UNKNOWN = "unknown";
	String EXTRA_FROM_TEXT_CLASSIFIER = "android.view.textclassifier.extra.FROM_TEXT_CLASSIFIER";

	/** No-op TextClassifier (AOSP: an instance whose methods all have their default behavior). */
	TextClassifier NO_OP = new TextClassifier() {
		@Override
		public String toString() {
			return "TextClassifier.NO_OP";
		}
	};

	/** AOSP default: the selection stays as requested */
	default TextSelection suggestSelection(TextSelection.Request request) {
		Objects.requireNonNull(request);
		Utils.checkMainThread();
		return new TextSelection.Builder(request.getStartIndex(), request.getEndIndex()).build();
	}

	default TextSelection suggestSelection(CharSequence text, int selectionStartIndex, int selectionEndIndex,
	                                       LocaleList defaultLocales) {
		final TextSelection.Request request = new TextSelection.Request.Builder(text, selectionStartIndex, selectionEndIndex)
		                                          .setDefaultLocales(defaultLocales)
		                                          .build();
		return suggestSelection(request);
	}

	/** AOSP default: no classification */
	default TextClassification classifyText(TextClassification.Request request) {
		Objects.requireNonNull(request);
		Utils.checkMainThread();
		return TextClassification.EMPTY;
	}

	default TextClassification classifyText(CharSequence text, int startIndex, int endIndex, LocaleList defaultLocales) {
		final TextClassification.Request request = new TextClassification.Request.Builder(text, startIndex, endIndex)
		                                               .setDefaultLocales(defaultLocales)
		                                               .build();
		return classifyText(request);
	}

	/** AOSP default TextClassifier.detectLanguage(): no language detected */
	default TextLanguage detectLanguage(TextLanguage.Request request) {
		Objects.requireNonNull(request);
		Utils.checkMainThread();
		return TextLanguage.EMPTY;
	}

	/** AOSP default TextClassifier.suggestConversationActions(): no actions */
	default ConversationActions suggestConversationActions(ConversationActions.Request request) {
		Objects.requireNonNull(request);
		Utils.checkMainThread();
		return new ConversationActions(Collections.emptyList(), null);
	}

	/** AOSP default: the maximum length of text that can be processed by generateLinks(); none here */
	default int getMaxGenerateLinksTextLength() {
		return 0;
	}

	/** AOSP default: a classifier that is not destroyed */
	default boolean isDestroyed() {
		return false;
	}

	/** AOSP default: nothing to release */
	default void destroy() {}

	/** @hide */
	@Retention(RetentionPolicy.SOURCE)
	@StringDef(prefix = {"TYPE_"}, value = {TYPE_UNKNOWN, TYPE_OTHER, TYPE_EMAIL, TYPE_PHONE, TYPE_ADDRESS, TYPE_URL,
	                                         TYPE_DATE, TYPE_DATE_TIME, TYPE_FLIGHT_NUMBER, TYPE_DICTIONARY})
	@interface EntityType {}

	/**
	 * Configuration object for specifying what entity types to identify.
	 *
	 * Configs are initially based on a predefined preset, and can be modified from there.
	 */
	final class EntityConfig implements Parcelable {
		private final List<String> mIncludedTypes;
		private final List<String> mExcludedTypes;
		private final List<String> mHints;
		private final boolean mIncludeTypesFromTextClassifier;

		private EntityConfig(
				List<String> includedEntityTypes,
				List<String> excludedEntityTypes,
				List<String> hints,
				boolean includeTypesFromTextClassifier) {
			mIncludedTypes = Objects.requireNonNull(includedEntityTypes);
			mExcludedTypes = Objects.requireNonNull(excludedEntityTypes);
			mHints = Objects.requireNonNull(hints);
			mIncludeTypesFromTextClassifier = includeTypesFromTextClassifier;
		}

		private EntityConfig(Parcel in) {
			mIncludedTypes = new ArrayList<>();
			in.readStringList(mIncludedTypes);
			mExcludedTypes = new ArrayList<>();
			in.readStringList(mExcludedTypes);
			List<String> tmpHints = new ArrayList<>();
			in.readStringList(tmpHints);
			mHints = Collections.unmodifiableList(tmpHints);
			mIncludeTypesFromTextClassifier = in.readByte() != 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeStringList(mIncludedTypes);
			parcel.writeStringList(mExcludedTypes);
			parcel.writeStringList(mHints);
			parcel.writeByte((byte) (mIncludeTypesFromTextClassifier ? 1 : 0));
		}

		/**
		 * Creates an EntityConfig.
		 *
		 * @param hints Hints for the TextClassifier to determine what types of entities to find.
		 *
		 * @deprecated Use {@link Builder} instead.
		 */
		@Deprecated
		public static EntityConfig createWithHints(@Nullable Collection<String> hints) {
			return new EntityConfig.Builder()
					.includeTypesFromTextClassifier(true)
					.setHints(hints)
					.build();
		}

		/**
		 * Creates an EntityConfig.
		 *
		 * @param hints Hints for the TextClassifier to determine what types of entities to find
		 * @param includedEntityTypes Entity types, e.g. {@link #TYPE_EMAIL}, to explicitly include
		 * @param excludedEntityTypes Entity types, e.g. {@link #TYPE_PHONE}, to explicitly exclude
		 *
		 *
		 * Note that if an entity has been excluded, the exclusion will take precedence.
		 *
		 * @deprecated Use {@link Builder} instead.
		 */
		@Deprecated
		public static EntityConfig create(@Nullable Collection<String> hints,
				@Nullable Collection<String> includedEntityTypes,
				@Nullable Collection<String> excludedEntityTypes) {
			return new EntityConfig.Builder()
					.setIncludedTypes(includedEntityTypes)
					.setExcludedTypes(excludedEntityTypes)
					.setHints(hints)
					.includeTypesFromTextClassifier(true)
					.build();
		}

		/**
		 * Creates an EntityConfig with an explicit entity list.
		 *
		 * @param entityTypes Complete set of entities, e.g. {@link #TYPE_URL} to find.
		 *
		 * @deprecated Use {@link Builder} instead.
		 */
		@Deprecated
		public static EntityConfig createWithExplicitEntityList(
				@Nullable Collection<String> entityTypes) {
			return new EntityConfig.Builder()
					.setIncludedTypes(entityTypes)
					.includeTypesFromTextClassifier(false)
					.build();
		}

		/**
		 * Returns a final list of entity types to find.
		 *
		 * @param entityTypes Entity types we think should be found before factoring in
		 *                    includes/excludes
		 *
		 * This method is intended for use by TextClassifier implementations.
		 */
		public Collection<String> resolveEntityListModifications(
				@NonNull Collection<String> entityTypes) {
			final Set<String> finalSet = new HashSet<>();
			if (mIncludeTypesFromTextClassifier) {
				finalSet.addAll(entityTypes);
			}
			finalSet.addAll(mIncludedTypes);
			finalSet.removeAll(mExcludedTypes);
			return finalSet;
		}

		/**
		 * Retrieves the list of hints.
		 *
		 * @return An unmodifiable collection of the hints.
		 */
		public Collection<String> getHints() {
			return mHints;
		}

		/**
		 * Return whether the client allows the text classifier to include its own list of
		 * default types. If this function returns {@code true}, a default list of types suggested
		 * from a text classifier will be taking into account.
		 *
		 * <p>NOTE: This method is intended for use by a text classifier.
		 *
		 * @see #resolveEntityListModifications(Collection)
		 */
		public boolean shouldIncludeTypesFromTextClassifier() {
			return mIncludeTypesFromTextClassifier;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final @android.annotation.NonNull Parcelable.Creator<EntityConfig> CREATOR =
				new Parcelable.Creator<EntityConfig>() {
					@Override
					public EntityConfig createFromParcel(Parcel in) {
						return new EntityConfig(in);
					}

					@Override
					public EntityConfig[] newArray(int size) {
						return new EntityConfig[size];
					}
				};



		/** Builder class to construct the {@link EntityConfig} object. */
		public static final class Builder {
			@Nullable
			private Collection<String> mIncludedTypes;
			@Nullable
			private Collection<String> mExcludedTypes;
			@Nullable
			private Collection<String> mHints;
			private boolean mIncludeTypesFromTextClassifier = true;

			/**
			 * Sets a collection of types that are explicitly included.
			 */
			@NonNull
			public Builder setIncludedTypes(@Nullable Collection<String> includedTypes) {
				mIncludedTypes = includedTypes;
				return this;
			}

			/**
			 * Sets a collection of types that are explicitly excluded.
			 */
			@NonNull
			public Builder setExcludedTypes(@Nullable Collection<String> excludedTypes) {
				mExcludedTypes = excludedTypes;
				return this;
			}

			/**
			 * Specifies whether or not to include the types suggested by the text classifier. By
			 * default, it is included.
			 */
			@NonNull
			public Builder includeTypesFromTextClassifier(boolean includeTypesFromTextClassifier) {
				mIncludeTypesFromTextClassifier = includeTypesFromTextClassifier;
				return this;
			}


			/**
			 * Sets the hints for the TextClassifier to determine what types of entities to find.
			 * These hints will only be used if {@link #includeTypesFromTextClassifier} is
			 * set to be true.
			 */
			@NonNull
			public Builder setHints(@Nullable Collection<String> hints) {
				mHints = hints;
				return this;
			}

			/**
			 * Combines all of the options that have been set and returns a new {@link EntityConfig}
			 * object.
			 */
			@NonNull
			public EntityConfig build() {
				return new EntityConfig(
						mIncludedTypes == null
								? Collections.emptyList()
								: new ArrayList<>(mIncludedTypes),
						mExcludedTypes == null
								? Collections.emptyList()
								: new ArrayList<>(mExcludedTypes),
						mHints == null
								? Collections.emptyList()
								: Collections.unmodifiableList(new ArrayList<>(mHints)),
						mIncludeTypesFromTextClassifier);
			}
		}
	}

	/** Utility functions for TextClassifier methods (AOSP TextClassifier.Utils, the part ATL needs). @hide */
	final class Utils {
		static void checkArgument(CharSequence text, int startIndex, int endIndex) {
			Preconditions.checkArgument(text != null);
			Preconditions.checkArgument(startIndex >= 0);
			Preconditions.checkArgument(endIndex <= text.length());
			Preconditions.checkArgument(endIndex > startIndex);
		}

		static boolean checkTextLength(CharSequence text, int maxLength) {
			int textLength = text.length();
			return textLength >= 0 && textLength <= maxLength;
		}

		static void checkMainThread() {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				Log.w(LOG_TAG, "TextClassifier called on main thread");
			}
		}
	}
}
