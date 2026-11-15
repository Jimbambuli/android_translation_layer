package android.view.inputmethod;

import java.util.HashMap;
import java.util.Locale;

/**
 * This class is used to specify meta information of a subtype contained in an input method (AOSP
 * InputMethodSubtype). The desktop's input method is GTK's, so the subtype that
 * InputMethodManager.getCurrentInputMethodSubtype() answers is a plain one for the locale of the
 * process; the builder makes the subtypes an app describes itself.
 */
public class InputMethodSubtype {
	private final int mSubtypeNameResId;
	private final int mSubtypeIconResId;
	private final String mSubtypeLocale;
	private final String mSubtypeLanguageTag;
	private final String mSubtypeMode;
	private final String mSubtypeExtraValue;
	private final boolean mIsAuxiliary;
	private final boolean mOverridesImplicitlyEnabledSubtype;
	private final boolean mIsAsciiCapable;
	private final int mSubtypeId;
	private final CharSequence mSubtypeNameOverride;
	private HashMap<String, String> mExtraValueHashMapCache;

	public InputMethodSubtype() {
		this(new InputMethodSubtypeBuilder().setSubtypeLocale(Locale.getDefault().toString()).setLanguageTag(Locale.getDefault().toLanguageTag()).setSubtypeMode("keyboard"));
	}

	private InputMethodSubtype(InputMethodSubtypeBuilder b) {
		mSubtypeNameResId = b.mSubtypeNameResId;
		mSubtypeIconResId = b.mSubtypeIconResId;
		mSubtypeLocale = b.mSubtypeLocale != null ? b.mSubtypeLocale : "";
		mSubtypeLanguageTag = b.mSubtypeLanguageTag != null ? b.mSubtypeLanguageTag : "";
		mSubtypeMode = b.mSubtypeMode != null ? b.mSubtypeMode : "";
		mSubtypeExtraValue = b.mSubtypeExtraValue != null ? b.mSubtypeExtraValue : "";
		mIsAuxiliary = b.mIsAuxiliary;
		mOverridesImplicitlyEnabledSubtype = b.mOverridesImplicitlyEnabledSubtype;
		mIsAsciiCapable = b.mIsAsciiCapable;
		mSubtypeId = b.mSubtypeId;
		mSubtypeNameOverride = b.mSubtypeNameOverride;
	}

	public int getNameResId() {
		return mSubtypeNameResId;
	}

	public int getIconResId() {
		return mSubtypeIconResId;
	}

	@Deprecated
	public String getLocale() {
		return mSubtypeLocale;
	}

	public String getLanguageTag() {
		return mSubtypeLanguageTag;
	}

	public Locale getLocaleObject() {
		return Locale.forLanguageTag(mSubtypeLanguageTag.isEmpty() ? mSubtypeLocale.replace('_', '-') : mSubtypeLanguageTag);
	}

	public String getMode() {
		return mSubtypeMode;
	}

	public String getExtraValue() {
		return mSubtypeExtraValue;
	}

	/* AOSP InputMethodSubtype.getNameOverride() */
	public CharSequence getNameOverride() {
		return mSubtypeNameOverride;
	}

	/* AOSP InputMethodSubtype.containsExtraValueKey()/getExtraValueOf()/getExtraValueHashMap(): the extra value
	 * is "key1=value1,key2,..." */
	public boolean containsExtraValueKey(String key) {
		return getExtraValueHashMap().containsKey(key);
	}

	public String getExtraValueOf(String key) {
		return getExtraValueHashMap().get(key);
	}

	private HashMap<String, String> getExtraValueHashMap() {
		synchronized (this) {
			HashMap<String, String> extraValueMap = mExtraValueHashMapCache;
			if (extraValueMap != null)
				return extraValueMap;
			extraValueMap = new HashMap<>();
			final String[] pairs = mSubtypeExtraValue.split(",");
			for (int i = 0; i < pairs.length; ++i) {
				final String[] pair = pairs[i].split("=");
				if (pair.length == 1)
					extraValueMap.put(pair[0], null);
				else if (pair.length > 1)
					extraValueMap.put(pair[0], pair[1]);
			}
			mExtraValueHashMapCache = extraValueMap;
			return extraValueMap;
		}
	}

	public boolean isAuxiliary() {
		return mIsAuxiliary;
	}

	public boolean overridesImplicitlyEnabledSubtype() {
		return mOverridesImplicitlyEnabledSubtype;
	}

	public boolean isAsciiCapable() {
		return mIsAsciiCapable;
	}

	public int hashCode() {
		return mSubtypeId != 0 ? mSubtypeId : (mSubtypeLocale + mSubtypeMode + mSubtypeExtraValue).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof InputMethodSubtype) {
			InputMethodSubtype subtype = (InputMethodSubtype)o;
			return hashCode() == subtype.hashCode();
		}
		return false;
	}

	/** AOSP InputMethodSubtype.InputMethodSubtypeBuilder */
	public static class InputMethodSubtypeBuilder {
		private boolean mIsAuxiliary = false;
		private boolean mOverridesImplicitlyEnabledSubtype = false;
		private boolean mIsAsciiCapable = false;
		private int mSubtypeIconResId = 0;
		private int mSubtypeNameResId = 0;
		private int mSubtypeId = 0;
		private String mSubtypeLocale = "";
		private String mSubtypeLanguageTag = "";
		private String mSubtypeMode = "";
		private String mSubtypeExtraValue = "";
		private CharSequence mSubtypeNameOverride = "";

		/* AOSP InputMethodSubtypeBuilder.setSubtypeNameOverride() */
		public InputMethodSubtypeBuilder setSubtypeNameOverride(CharSequence nameOverride) {
			mSubtypeNameOverride = nameOverride;
			return this;
		}

		public InputMethodSubtypeBuilder setIsAuxiliary(boolean isAuxiliary) {
			mIsAuxiliary = isAuxiliary;
			return this;
		}

		public InputMethodSubtypeBuilder setOverridesImplicitlyEnabledSubtype(boolean overridesImplicitlyEnabledSubtype) {
			mOverridesImplicitlyEnabledSubtype = overridesImplicitlyEnabledSubtype;
			return this;
		}

		public InputMethodSubtypeBuilder setIsAsciiCapable(boolean isAsciiCapable) {
			mIsAsciiCapable = isAsciiCapable;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeIconResId(int subtypeIconResId) {
			mSubtypeIconResId = subtypeIconResId;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeNameResId(int subtypeNameResId) {
			mSubtypeNameResId = subtypeNameResId;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeId(int subtypeId) {
			mSubtypeId = subtypeId;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeLocale(String subtypeLocale) {
			mSubtypeLocale = subtypeLocale == null ? "" : subtypeLocale;
			return this;
		}

		public InputMethodSubtypeBuilder setLanguageTag(String languageTag) {
			mSubtypeLanguageTag = languageTag == null ? "" : languageTag;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeMode(String subtypeMode) {
			mSubtypeMode = subtypeMode == null ? "" : subtypeMode;
			return this;
		}

		public InputMethodSubtypeBuilder setSubtypeExtraValue(String subtypeExtraValue) {
			mSubtypeExtraValue = subtypeExtraValue == null ? "" : subtypeExtraValue;
			return this;
		}

		public InputMethodSubtype build() {
			return new InputMethodSubtype(this);
		}
	}
}
