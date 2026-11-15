/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.app;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;
import java.util.Objects;

/**
 * This class gives access to system locale services. These services allow applications to
 * control granular locale settings (such as per-app locales).
 *
 * ATL: AOSP keeps the locales per package in the system's locale service and the system applies them as
 * a configuration change of the app. ATL runs one app: its application locales are held in the process (they
 * are not kept for the next start of the app) and applied the way ATL applies a change of the dark mode: the
 * default Locale and the configuration change, with ActivityThread.handleConfigurationChanged(). The override
 * LocaleConfig is kept in the process as well.
 */
public class LocaleManager {
	private static LocaleList sSystemLocales;
	private static LocaleList sApplicationLocales = LocaleList.getEmptyLocaleList();

	private final Context mContext;

	/** @hide */
	public LocaleManager(Context context) {
		mContext = context;
		synchronized (LocaleManager.class) {
			if (sSystemLocales == null) {
				sSystemLocales = LocaleList.getDefault();
			}
		}
	}

	/**
	 * Sets the UI locales for the calling app.
	 *
	 * @param locales the desired locales for the calling app, an empty list follows the system
	 */
	public void setApplicationLocales(@NonNull LocaleList locales) {
		setApplicationLocales(mContext.getPackageName(), locales);
	}

	/** @hide */
	public void setApplicationLocales(@NonNull String appPackageName, @NonNull LocaleList locales) {
		Objects.requireNonNull(appPackageName);
		Objects.requireNonNull(locales);
		if (!appPackageName.equals(mContext.getPackageName())) {
			return;
		}
		Locale target;
		synchronized (LocaleManager.class) {
			if (locales.equals(sApplicationLocales)) {
				return;
			}
			sApplicationLocales = locales;
			target = locales.isEmpty() ? sSystemLocales.get(0) : locales.get(0);
		}
		Locale.setDefault(target);
		Configuration config = new Configuration(Context.sys_config);
		config.setLocale(target);
		ActivityThread.handleConfigurationChanged(config);
	}

	/**
	 * Returns the UI locales for the calling app.
	 *
	 * @return the current UI locales for the app, an empty list if it follows the system
	 */
	@NonNull
	public LocaleList getApplicationLocales() {
		return getApplicationLocales(mContext.getPackageName());
	}

	/** @hide */
	@NonNull
	public LocaleList getApplicationLocales(@NonNull String appPackageName) {
		Objects.requireNonNull(appPackageName);
		synchronized (LocaleManager.class) {
			return appPackageName.equals(mContext.getPackageName()) ? sApplicationLocales : LocaleList.getEmptyLocaleList();
		}
	}

	/**
	 * Returns the current system locales, ignoring app-specific overrides.
	 *
	 * <p><b>Note:</b> Apps should generally access the user's locale preferences as indicated in
	 * their in-process {@link LocaleList}s. However, in case an app-specific locale is set, this
	 * method helps cater to rare use-cases which might require specifically knowing the system
	 * locale.
	 */
	@NonNull
	public LocaleList getSystemLocales() {
		synchronized (LocaleManager.class) {
			return sSystemLocales;
		}
	}

	private static LocaleConfig sOverrideLocaleConfig;

	/* AOSP setOverrideLocaleConfig(): the service stores it per package (and tells the system to reload the
	 * supported locales); null removes the override. ATL runs one app and keeps it in the process. */
	public void setOverrideLocaleConfig(LocaleConfig localeConfig) {
		synchronized (LocaleManager.class) {
			sOverrideLocaleConfig = localeConfig;
		}
	}

	/* AOSP getOverrideLocaleConfig(): the override, null if there is none */
	public LocaleConfig getOverrideLocaleConfig() {
		synchronized (LocaleManager.class) {
			return sOverrideLocaleConfig;
		}
	}
}
