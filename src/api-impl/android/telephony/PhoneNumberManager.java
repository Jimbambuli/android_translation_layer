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

import java.util.List;

/**
 * PhoneNumberManager provides APIs for parsing phone numbers from various sources, such as URIs.
 */
public class PhoneNumberManager {

    /**
     * Parses a phone number out of the P-Associated-URI headers of an IMS registration.
     *
     * ATL: AOSP asks PhoneNumberManagerService (IPhoneNumber) in the telephony module, which parses
     * with libphonenumber. ATL has no libphonenumber (no number metadata): the service's argument
     * checks and its walk over the URIs are the same, a candidate counts as valid when it is an
     * international number (+, 7 to 15 digits, no leading 0 after the +, ITU-T E.164); a national
     * number cannot be resolved without the metadata of its country and is a parse error.
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
            Rlog.e("PhoneNumberManagerService", "IMS Registration header is invalid. Either null or empty.");
            throw new IllegalArgumentException(
              "Unable to parse number as the associatedUris received is invalid.");
        }
        if (countryIso == null || countryIso.isEmpty()) {
            Rlog.e("PhoneNumberManagerService", "Country code is invalid. Either null or empty.");
            throw new IllegalArgumentException(
              "Unable to parse number as the countryIso received is invalid.");
        }
        int firstErrorType = ParsedPhoneNumber.ERROR_TYPE_NONE;
        for (Uri uri : associatedUris) {
            if (uri != null && uri.isOpaque()) {
                String candidate = uri.getSchemeSpecificPart().split("@")[0];
                int error;
                if (candidate.matches("\\+[1-9][0-9]{6,14}")) {
                    return new ParsedPhoneNumber(candidate, ParsedPhoneNumber.ERROR_TYPE_NONE, true);
                } else if (candidate.startsWith("+")) {
                    error = ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER;
                } else {
                    error = ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION;
                }
                if (firstErrorType == ParsedPhoneNumber.ERROR_TYPE_NONE) {
                    firstErrorType = error;
                }
            }
        }
        // no valid phone number was found: an empty string with the first error encountered
        return new ParsedPhoneNumber("", firstErrorType, false);
    }
}
