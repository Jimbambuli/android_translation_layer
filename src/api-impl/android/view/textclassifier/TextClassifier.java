package android.view.textclassifier;

/**
 * Interface for providing text classification related features (AOSP android.view.textclassifier.TextClassifier).
 *
 * ATL has no TextClassificationManager and no text classification service, so this interface has the constants
 * and the part of AOSP's methods that need none of its result classes (TextSelection, TextClassification,
 * TextLinks, ...: they are not in ATL). NO_OP is AOSP's TextClassifier whose methods all keep their defaults.
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
}
