package android.app;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.LocaleList;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

/* AOSP LocaleConfig without the Parcelable part: the locales of the app's android:localeConfig XML resource.
 * AOSP first asks LocaleManager for an override set by the installer (setOverrideLocaleConfig); ATL has none */
public class LocaleConfig implements android.os.Parcelable {
	private static final String TAG = "LocaleConfig";
	public static final String TAG_LOCALE_CONFIG = "locale-config";
	public static final String TAG_LOCALE = "locale";
	private LocaleList mLocales;
	private int mStatus = STATUS_NOT_SPECIFIED;

	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_NOT_SPECIFIED = 1;
	public static final int STATUS_PARSING_FAILED = 2;

	public LocaleConfig(Context context) {
		this(context, true);
	}

	public static LocaleConfig fromContextIgnoringOverride(Context context) {
		return new LocaleConfig(context, false);
	}

	private LocaleConfig(Context context, boolean allowOverride) {
		int resId = context.getApplicationInfo().getLocaleConfigRes();
		if (resId == 0) {
			mStatus = STATUS_NOT_SPECIFIED;
			return;
		}
		Resources res = context.getResources();
		try {
			XmlResourceParser parser = res.getXml(resId);
			parseLocaleConfig(parser, res);
		} catch (Resources.NotFoundException e) {
			Slog.w(TAG, "The resource file pointed to by the given resource ID isn't found.");
			mStatus = STATUS_NOT_SPECIFIED;
		} catch (XmlPullParserException | IOException e) {
			Slog.w(TAG, "Failed to parse XML configuration from " + res.getResourceEntryName(resId), e);
			mStatus = STATUS_PARSING_FAILED;
		}
	}

	public LocaleConfig(LocaleList locales) {
		mStatus = STATUS_SUCCESS;
		mLocales = locales;
	}

	private void parseLocaleConfig(XmlResourceParser parser, Resources res) throws IOException, XmlPullParserException {
		XmlUtils.beginDocument(parser, TAG_LOCALE_CONFIG);
		int outerDepth = parser.getDepth();
		AttributeSet attrs = Xml.asAttributeSet(parser);
		Set<String> localeNames = new HashSet<String>();
		while (XmlUtils.nextElementWithin(parser, outerDepth)) {
			if (TAG_LOCALE.equals(parser.getName())) {
				final TypedArray attributes = res.obtainAttributes(attrs, com.android.internal.R.styleable.LocaleConfig_Locale);
				String nameAttr = attributes.getString(com.android.internal.R.styleable.LocaleConfig_Locale_name);
				localeNames.add(nameAttr);
				attributes.recycle();
			} else {
				XmlUtils.skipCurrentTag(parser);
			}
		}
		mStatus = STATUS_SUCCESS;
		mLocales = LocaleList.forLanguageTags(String.join(",", localeNames));
	}

	public LocaleList getSupportedLocales() {
		return mLocales;
	}

	public int getStatus() {
		return mStatus;
	}

	public boolean isSameLocaleConfig(LocaleConfig other) {
		if (other == this)
			return true;
		if (other != null) {
			if (mStatus != other.mStatus)
				return false;
			LocaleList otherLocales = other.mLocales;
			if (mLocales == null && otherLocales == null) {
				return true;
			} else if (mLocales != null && otherLocales != null) {
				List<String> hostStrList = Arrays.asList(mLocales.toLanguageTags().split(","));
				List<String> targetStrList = Arrays.asList(otherLocales.toLanguageTags().split(","));
				Collections.sort(hostStrList);
				Collections.sort(targetStrList);
				return hostStrList.equals(targetStrList);
			}
		}
		return false;
	}

	public boolean containsLocale(Locale locale) {
		if (mLocales == null)
			return false;
		for (int i = 0; i < mLocales.size(); i++) {
			if (LocaleList.matchesLanguageAndScript(mLocales.get(i), locale))
				return true;
		}
		return false;
	}

	private LocaleConfig(android.os.Parcel in) {
		mStatus = in.readInt();
		mLocales = in.readTypedObject(LocaleList.CREATOR);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeInt(mStatus);
		dest.writeTypedObject(mLocales, flags);
	}

	public static final android.os.Parcelable.Creator<LocaleConfig> CREATOR = new android.os.Parcelable.Creator<LocaleConfig>() {
		@Override
		public LocaleConfig createFromParcel(android.os.Parcel source) {
			return new LocaleConfig(source);
		}

		@Override
		public LocaleConfig[] newArray(int size) {
			return new LocaleConfig[size];
		}
	};
}
