package android.view.textclassifier;

import android.os.LocaleList;
import android.os.Looper;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.Objects;

/**
 * Interface for providing text classification related features (AOSP android.view.textclassifier.TextClassifier).
 *
 * ATL has no text classification service, so this interface has the constants and AOSP's (android-16.0.0_r4)
 * default methods for selection and classification; the result classes of the other features (TextLinks,
 * TextLanguage, ConversationActions, SelectionEvent) are not in ATL. NO_OP is AOSP's TextClassifier whose methods
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
