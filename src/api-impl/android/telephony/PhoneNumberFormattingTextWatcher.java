/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.telephony;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.Locale;

/**
 * Watches a {@link android.widget.TextView} and if a phone number is entered will format it.
 *
 * AOSP formats as you type with libphonenumber's AsYouTypeFormatter. ATL has no number metadata
 * (PhoneNumberUtils.formatNumber leaves the number as it is), so the watcher keeps the text as typed.
 */
public class PhoneNumberFormattingTextWatcher implements TextWatcher {
	private final String mCountryCode;

	public PhoneNumberFormattingTextWatcher() {
		this(Locale.getDefault().getCountry());
	}

	/**
	 * The formatting is based on the given <code>countryCode</code>.
	 *
	 * @param countryCode the ISO 3166-1 two-letter country code that indicates the country/region
	 * where the phone number is being entered.
	 */
	public PhoneNumberFormattingTextWatcher(String countryCode) {
		if (countryCode == null)
			throw new IllegalArgumentException();
		mCountryCode = countryCode;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public synchronized void afterTextChanged(Editable s) {}
}
