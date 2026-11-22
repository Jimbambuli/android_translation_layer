/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.List;
import java.util.Locale;

/**
 * PhoneNumberManager provides APIs for parsing phone numbers from various sources, such as URIs.
 */
public class PhoneNumberManager {

    private static final String TAG = "PhoneNumberManagerService";
    private static final String ANONYMIZATION_CHAR = "X";

    /**
     * Parses a phone number out of the P-Associated-URI headers of an IMS registration.
     *
     * ATL: AOSP asks PhoneNumberManagerService (IPhoneNumber) in the telephony module; ATL has no such
     * service process, so this is the service's parsePhoneNumber()/extractPhoneNumber() as it is in
     * AOSP (android-16.0.0_r4), run in the caller with the same libphonenumber.
     *
     * @param associatedUris The P-Associated-URI headers of the IMS registration.
     * @param countryIso The ISO 3166-1 alpha-2 country code of the subscription.
     * @return the parsed number
     * @throws IllegalArgumentException if associatedUris or countryIso is null or empty
     */
    @NonNull
    public ParsedPhoneNumber parsePhoneNumber(@Nullable List<Uri> associatedUris,
                @NonNull String countryIso) {
        if (associatedUris == null || associatedUris.isEmpty()) {
            Rlog.e(TAG, "IMS Registration header is invalid. Either null or empty.");
            throw new IllegalArgumentException(
              "Unable to parse number as the associatedUris received is invalid.");
        }
        if (countryIso == null || countryIso.isEmpty()) {
            Rlog.e(TAG, "Country code is invalid. Either null or empty.");
            throw new IllegalArgumentException(
              "Unable to parse number as the countryIso received is invalid.");
        }

        return extractPhoneNumber(associatedUris, countryIso);
    }

    private ParsedPhoneNumber extractPhoneNumber(List<Uri> associatedUris, String countryIso) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        int firstErrorType = ParsedPhoneNumber.ERROR_TYPE_NONE;

        for (Uri uri : associatedUris) {
            if (uri != null && uri.isOpaque()) {
                String phoneNumberCandidate = uri.getSchemeSpecificPart().split("@")[0];
                try {
                    // Converts countryIso to uppercase as required by libphonenumber.
                    PhoneNumber phoneNumber = util.parse(phoneNumberCandidate,
                            countryIso.toUpperCase(Locale.ENGLISH));

                    if (util.isValidNumber(phoneNumber)) {
                        // If a valid number is found, return it immediately.
                        return new ParsedPhoneNumber(
                            util.format(phoneNumber, PhoneNumberFormat.E164),
                            ParsedPhoneNumber.ERROR_TYPE_NONE, true);
                    } else {
                        Rlog.d(TAG, "Failed to validate the following number: {"
                                + anonymizePhoneNumberSimple(phoneNumberCandidate)
                                + "} for country: {"
                                + countryIso + "}");
                        firstErrorType = updateFirstErrorType(firstErrorType,
                            ParsedPhoneNumber
                                .ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER);
                    }
                } catch (NumberParseException e) {
                    Rlog.d(TAG, "NumberParseException for number: {"
                            + anonymizePhoneNumberSimple(phoneNumberCandidate) + "} - {"
                            + e.getMessage() + "}");
                    firstErrorType = updateFirstErrorType(firstErrorType,
                        ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION);
                }
            }
            // TODO(b/434607712): add else statement that catches error if uri is null
            // and not opaque.
        }

        // If the loop completes and no valid phone number was found, return an empty string
        // with the first error encountered (or NONE if no specific error).
        return new ParsedPhoneNumber("", firstErrorType, false);
    }

    private static String anonymizePhoneNumberSimple(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        StringBuilder anonymized = new StringBuilder();
        for (int i = 0; i < phoneNumber.length(); i++) {
            char c = phoneNumber.charAt(i);
            if (Character.isDigit(c)) {
                anonymized.append(ANONYMIZATION_CHAR);
            } else {
                anonymized.append(c);
            }
        }
        return anonymized.toString();
    }

    private int updateFirstErrorType(int currentFirstErrorType, int newErrorType) {
        if (currentFirstErrorType == ParsedPhoneNumber.ERROR_TYPE_NONE) {
            return newErrorType;
        }
        return currentFirstErrorType;
    }
}
