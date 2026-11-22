/*
 * PHONE_NUMBERS_EQUAL for SQLite, as Android registers it on every database (external/sqlite android/sqlite3_android.cpp
 * register_android_functions(): phone_numbers_equal with 2, 3 and 4 arguments). The comparison is AOSP 16
 * external/sqlite android/OldPhoneNumberUtils.cpp (loose) and PhoneNumberUtils.cpp (strict), unchanged but for C
 * (no namespace, stdbool) — the telephony provider (MmsSmsProvider) and contacts use it.
 */
#include <ctype.h>
#include <stdbool.h>
#include <stddef.h>
#include <string.h>
#include <sqlite3.h>

#include "sqlite3_android_phone.h"

/* ---- OldPhoneNumberUtils.cpp ---- */


static int MIN_MATCH = 7;

/** True if c is ISO-LATIN characters 0-9 */
static bool isISODigit (char c)
{
    return c >= '0' && c <= '9';
}

/** True if c is ISO-LATIN characters 0-9, *, # , +  */
static bool isNonSeparator(char c)
{
    return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
}

/**
 * Phone numbers are stored in "lookup" form in the database
 * as reversed strings to allow for caller ID lookup
 *
 * This method takes a phone number and makes a valid SQL "LIKE"
 * string that will match the lookup form
 *
 */
/** all of a up to len must be an international prefix or
 *  separators/non-dialing digits
 */
static bool matchIntlPrefix(const char* a, int len)
{
    /* '([^0-9*#+]\+[^0-9*#+] | [^0-9*#+]0(0|11)[^0-9*#+] )$' */
    /*        0    1                     2 3 45               */

    int state = 0;
    for (int i = 0 ; i < len ; i++) {
        char c = a[i];

        switch (state) {
            case 0:
                if      (c == '+') state = 1;
                else if (c == '0') state = 2;
                else if (isNonSeparator(c)) return false;
            break;

            case 2:
                if      (c == '0') state = 3;
                else if (c == '1') state = 4;
                else if (isNonSeparator(c)) return false;
            break;

            case 4:
                if      (c == '1') state = 5;
                else if (isNonSeparator(c)) return false;
            break;

            default:
                if (isNonSeparator(c)) return false;
            break;

        }
    }

    return state == 1 || state == 3 || state == 5;
}

/** all of 'a' up to len must match non-US trunk prefix ('0') */
static bool matchTrunkPrefix(const char* a, int len)
{
    bool found;

    found = false;

    for (int i = 0 ; i < len ; i++) {
        char c = a[i];

        if (c == '0' && !found) {
            found = true;
        } else if (isNonSeparator(c)) {
            return false;
        }
    }

    return found;
}

/** all of 'a' up to len must be a (+|00|011)country code)
 *  We're fast and loose with the country code. Any \d{1,3} matches */
static bool matchIntlPrefixAndCC(const char* a, int len)
{
    /*  [^0-9*#+]*(\+|0(0|11)\d\d?\d? [^0-9*#+] $ */
    /*      0       1 2 3 45  6 7  8              */

    int state = 0;
    for (int i = 0 ; i < len ; i++ ) {
        char c = a[i];

        switch (state) {
            case 0:
                if      (c == '+') state = 1;
                else if (c == '0') state = 2;
                else if (isNonSeparator(c)) return false;
            break;

            case 2:
                if      (c == '0') state = 3;
                else if (c == '1') state = 4;
                else if (isNonSeparator(c)) return false;
            break;

            case 4:
                if      (c == '1') state = 5;
                else if (isNonSeparator(c)) return false;
            break;

            case 1:
            case 3:
            case 5:
                if      (isISODigit(c)) state = 6;
                else if (isNonSeparator(c)) return false;
            break;

            case 6:
            case 7:
                if      (isISODigit(c)) state++;
                else if (isNonSeparator(c)) return false;
            break;

            default:
                if (isNonSeparator(c)) return false;
        }
    }

    return state == 6 || state == 7 || state == 8;
}

/**
 * Compare phone numbers a and b, return true if they're identical
 * enough for caller ID purposes.
 *
 * - Compares from right to left
 * - requires minimum characters to match
 * - handles common trunk prefixes and international prefixes
 *   (basically, everything except the Russian trunk prefix)
 *
 * Tolerates nulls
 */
bool phone_number_compare_loose_with_minmatch(const char* a, const char* b, int min_match)
{
    int ia, ib;
    int matched;
    int numSeparatorCharsInA = 0;
    int numSeparatorCharsInB = 0;

    if (a == NULL || b == NULL) {
        return false;
    }

    ia = strlen(a);
    ib = strlen(b);
    if (ia == 0 || ib == 0) {
        return false;
    }

    // Compare from right to left
    ia--;
    ib--;

    matched = 0;

    while (ia >= 0 && ib >=0) {
        char ca, cb;
        bool skipCmp = false;

        ca = a[ia];

        if (!isNonSeparator(ca)) {
            ia--;
            skipCmp = true;
            numSeparatorCharsInA++;
        }

        cb = b[ib];

        if (!isNonSeparator(cb)) {
            ib--;
            skipCmp = true;
            numSeparatorCharsInB++;
        }

        if (!skipCmp) {
            if (cb != ca) {
                break;
            }
            ia--; ib--; matched++;
        }
    }

    if (matched < min_match) {
        const int effectiveALen = strlen(a) - numSeparatorCharsInA;
        const int effectiveBLen = strlen(b) - numSeparatorCharsInB;

        // if the number of dialable chars in a and b match, but the matched chars < min_match,
        // treat them as equal (i.e. 404-04 and 40404)
        if (effectiveALen == effectiveBLen && effectiveALen == matched) {
            return true;
        }

        return false;
    }

    // At least one string has matched completely;
    if (matched >= min_match && (ia < 0 || ib < 0)) {
        return true;
    }

    /*
     * Now, what remains must be one of the following for a
     * match:
     *
     *  - a '+' on one and a '00' or a '011' on the other
     *  - a '0' on one and a (+,00)<country code> on the other
     *     (for this, a '0' and a '00' prefix would have succeeded above)
     */

    if (matchIntlPrefix(a, ia + 1) && matchIntlPrefix(b, ib + 1)) {
        return true;
    }

    if (matchTrunkPrefix(a, ia + 1) && matchIntlPrefixAndCC(b, ib + 1)) {
        return true;
    }

    if (matchTrunkPrefix(b, ib + 1) && matchIntlPrefixAndCC(a, ia + 1)) {
        return true;
    }

    /*
     * Last resort: if the number of unmatched characters on both sides is less than or equal
     * to the length of the longest country code and only one number starts with a + accept
     * the match. This is because some countries like France and Russia have an extra prefix
     * digit that is used when dialing locally in country that does not show up when you dial
     * the number using the country code. In France this prefix digit is used to determine
     * which land line carrier to route the call over.
     */
    bool aPlusFirst = (*a == '+');
    bool bPlusFirst = (*b == '+');
    bool aIgnoreUnmatched = aPlusFirst && (ia - ib) >= 0 && (ia - ib) <= 1;
    bool bIgnoreUnmatched = bPlusFirst && (ib - ia) >= 0 && (ib - ia) <= 1;
    if (ia < 4 && ib < 4 && (aIgnoreUnmatched || bIgnoreUnmatched) && !(aPlusFirst && bPlusFirst)) {
        return true;
    }

    return false;
}

bool phone_number_compare_loose(const char* a, const char* b)
{
    return phone_number_compare_loose_with_minmatch(a, b, MIN_MATCH);
}

/* ---- PhoneNumberUtils.cpp ---- */


/* Generated by the following Python script. Values of country calling codes
   are from http://en.wikipedia.org/wiki/List_of_country_calling_codes

#!/usr/bin/python
import sys
ccc_set_2digits = set([0, 1, 7,
                       20, 27, 28, 30, 31, 32, 33, 34, 36, 39, 40, 43, 44, 45,
                       46, 47, 48, 49, 51, 52, 53, 54, 55, 56, 57, 58, 60, 61,
                       62, 63, 64, 65, 66, 81, 82, 83, 84, 86, 89, 90, 91, 92,
                       93, 94, 95, 98])

ONE_LINE_NUM = 10

for i in xrange(100):
  if i % ONE_LINE_NUM == 0:
    sys.stdout.write('    ')
  if i in ccc_set_2digits:
    included = 'true'
  else:
    included = 'false'
  sys.stdout.write(included + ',')
  if ((i + 1) % ONE_LINE_NUM) == 0:
    sys.stdout.write('\n')
  else:
    sys.stdout.write(' ')
*/
static bool two_length_country_code_map[100] = {
    true, true, false, false, false, false, false, true, false, false,
    false, false, false, false, false, false, false, false, false, false,
    true, false, false, false, false, false, false, true, true, false,
    true, true, true, true, true, false, true, false, false, true,
    true, false, false, true, true, true, true, true, true, true,
    false, true, true, true, true, true, true, true, true, false,
    true, true, true, true, true, true, true, false, false, false,
    false, false, false, false, false, false, false, false, false, false,
    false, true, true, true, true, false, true, false, false, true,
    true, true, true, true, true, true, false, false, true, false,
};

#define ARRAY_SIZE(a) (sizeof(a)/sizeof((a)[0]))

/**
 * Returns true if "ccc_candidate" expresses (part of ) some country calling
 * code.
 * Returns false otherwise.
 */
static bool isCountryCallingCode(int ccc_candidate) {
    return ccc_candidate > 0 &&
            ccc_candidate < (int)ARRAY_SIZE(two_length_country_code_map) &&
            two_length_country_code_map[ccc_candidate];
}

/**
 * Returns interger corresponding to the input if input "ch" is
 * ISO-LATIN characters 0-9.
 * Returns -1 otherwise
 */
static int tryGetISODigit (char ch)
{
    if ('0' <= ch && ch <= '9') {
        return ch - '0';
    } else {
        return -1;
    }
}

/**
 * True if ch is ISO-LATIN characters 0-9, *, # , +
 * Note this method current does not account for the WILD char 'N'
 */
static bool isDialable(char ch)
{
    return ('0' <= ch && ch <= '9') || ch == '*' || ch == '#' || ch == '+';
}

/** Returns true if ch is not dialable or alpha char */
static bool isSeparator(char ch)
{
    return !isDialable(ch) && (isalpha(ch) == 0);
}

/**
 * Try to store the pointer to "new_ptr" which does not have trunk prefix.
 *
 * Currently this function simply ignore the first digit assuming it is
 * trunk prefix. Actually trunk prefix is different in each country.
 *
 * e.g.
 * "+79161234567" equals "89161234567" (Russian trunk digit is 8)
 * "+33123456789" equals "0123456789" (French trunk digit is 0)
 *
 */
static bool tryGetTrunkPrefixOmittedStr(const char *str, size_t len,
                                        const char **new_ptr, size_t *new_len)
{
    for (size_t i = 0 ; i < len ; i++) {
        char ch = str[i];
        if (tryGetISODigit(ch) >= 0) {
            if (new_ptr != NULL) {
                *new_ptr = str + i + 1;
            }
            if (new_len != NULL) {
                *new_len = len - (i + 1);
            }
            return true;
        } else if (isDialable(ch)) {
            return false;
        }
    }

    return false;
}

/*
 * Note that this function does not strictly care the country calling code with
 * 3 length (like Morocco: +212), assuming it is enough to use the first two
 * digit to compare two phone numbers.
 */
static int tryGetCountryCallingCode(const char *str, size_t len,
                                    const char **new_ptr, size_t *new_len,
                                    bool accept_thailand_case)
{
    // Rough regexp:
    //  ^[^0-9*#+]*((\+|0(0|11)\d\d?|166) [^0-9*#+] $
    //         0        1 2 3 45  6 7  89
    //
    // In all the states, this function ignores separator characters.
    // "166" is the special case for the call from Thailand to the US. Ugu!

    int state = 0;
    int ccc = 0;
    for (size_t i = 0 ; i < len ; i++ ) {
        char ch = str[i];
        switch (state) {
            case 0:
                if      (ch == '+') state = 1;
                else if (ch == '0') state = 2;
                else if (ch == '1') {
                    if (accept_thailand_case) {
                        state = 8;
                    } else {
                        return -1;
                    }
                } else if (isDialable(ch)) return -1;
            break;

            case 2:
                if      (ch == '0') state = 3;
                else if (ch == '1') state = 4;
                else if (isDialable(ch)) return -1;
            break;

            case 4:
                if      (ch == '1') state = 5;
                else if (isDialable(ch)) return -1;
            break;

            case 1:
            case 3:
            case 5:
            case 6:
            case 7:
                {
                    int ret = tryGetISODigit(ch);
                    if (ret > 0) {
                        ccc = ccc * 10 + ret;
                        if (ccc >= 100 || isCountryCallingCode(ccc)) {
                            if (new_ptr != NULL) {
                                *new_ptr = str + i + 1;
                            }
                            if (new_len != NULL) {
                                *new_len = len - (i + 1);
                            }
                            return ccc;
                        }
                        if (state == 1 || state == 3 || state == 5) {
                            state = 6;
                        } else {
                            state++;
                        }
                    } else if (isDialable(ch)) {
                        return -1;
                    }
                }
                break;
            case 8:
                if (ch == '6') state = 9;
                else if (isDialable(ch)) return -1;
                break;
            case 9:
                if (ch == '6') {
                    if (new_ptr != NULL) {
                        *new_ptr = str + i + 1;
                    }
                    if (new_len != NULL) {
                        *new_len = len - (i + 1);
                    }
                    return 66;
                } else {
                    return -1;
                }
                break;
            default:
                return -1;
        }
    }

    return -1;
}

/**
 * Return true if the prefix of "ch" is "ignorable". Here, "ignorable" means
 * that "ch" has only one digit and separator characters. The one digit is
 * assumed to be the trunk prefix.
 */
static bool checkPrefixIsIgnorable(const char* ch, int i) {
    bool trunk_prefix_was_read = false;
    while (i >= 0) {
        if (tryGetISODigit(ch[i]) >= 0) {
            if (trunk_prefix_was_read) {
                // More than one digit appeared, meaning that "a" and "b"
                // is different.
                return false;
            } else {
                // Ignore just one digit, assuming it is trunk prefix.
                trunk_prefix_was_read = true;
            }
        } else if (isDialable(ch[i])) {
            // Trunk prefix is a digit, not "*", "#"...
            return false;
        }
        i--;
    }

    return true;
}

/**
 * Compare phone numbers a and b, return true if they're identical
 * enough for caller ID purposes.
 *
 * Assume NULL as 0-length string.
 *
 * Detailed information:
 * Currently (as of 2009-06-12), we cannot depend on the locale given from the
 * OS. For example, current Android does not accept "en_JP", meaning
 * "the display language is English but the phone should be in Japan", but
 * en_US, es_US, etc. So we cannot identify which digit is valid trunk prefix
 * in the country where the phone is used. More specifically, "880-1234-1234"
 * is not valid phone number in Japan since the trunk prefix in Japan is not 8
 * but 0 (correct number should be "080-1234-1234"), while Russian trunk prefix
 * is 8. Also, we cannot know whether the country where users live has trunk
 * prefix itself. So, we cannot determine whether "+81-80-1234-1234" is NOT
 * same as "880-1234-1234" (while "+81-80-1234-1234" is same as "080-1234-1234"
 * and we can determine "880-1234-1234" is different from "080-1234-1234").
 *
 * In the future, we should handle trunk prefix more correctly, but as of now,
 * we just ignore it...
 */
static bool phone_number_compare_inter(const char* const org_a, const char* const org_b,
                                       bool accept_thailand_case)
{
    const char* a = org_a;
    const char* b = org_b;
    size_t len_a = 0;
    size_t len_b = 0;
    if (a == NULL) {
        a = "";
    } else {
        len_a = strlen(a);
    }
    if (b == NULL) {
        b = "";
    } else {
        len_b = strlen(b);
    }

    const char* tmp_a = NULL;
    const char* tmp_b = NULL;
    size_t tmp_len_a = len_a;
    size_t tmp_len_b = len_b;

    int ccc_a = tryGetCountryCallingCode(a, len_a, &tmp_a, &tmp_len_a, accept_thailand_case);
    int ccc_b = tryGetCountryCallingCode(b, len_b, &tmp_b, &tmp_len_b, accept_thailand_case);
    bool both_have_ccc = false;
    bool may_ignore_prefix = true;
    bool trunk_prefix_is_omitted_a = false;
    bool trunk_prefix_is_omitted_b = false;
    if (ccc_a >= 0 && ccc_b >= 0) {
        if (ccc_a != ccc_b) {
            // Different Country Calling Code. Must be different phone number.
            return false;
        }
        // When both have ccc, do not ignore trunk prefix. Without this,
        // "+81123123" becomes same as "+810123123" (+81 == Japan)
        may_ignore_prefix = false;
        both_have_ccc = true;
    } else if (ccc_a < 0 && ccc_b < 0) {
        // When both do not have ccc, do not ignore trunk prefix. Without this,
        // "123123" becomes same as "0123123"
        may_ignore_prefix = false;
    } else {
        if (ccc_a < 0) {
            tryGetTrunkPrefixOmittedStr(a, len_a, &tmp_a, &tmp_len_a);
            trunk_prefix_is_omitted_a = true;
        }
        if (ccc_b < 0) {
            tryGetTrunkPrefixOmittedStr(b, len_b, &tmp_b, &tmp_len_b);
            trunk_prefix_is_omitted_b = true;
        }
    }

    if (tmp_a != NULL) {
        a = tmp_a;
        len_a = tmp_len_a;
    }
    if (tmp_b != NULL) {
        b = tmp_b;
        len_b = tmp_len_b;
    }

    int i_a = len_a - 1;
    int i_b = len_b - 1;
    while (i_a >= 0 && i_b >= 0) {
        bool skip_compare = false;
        char ch_a = a[i_a];
        char ch_b = b[i_b];
        if (isSeparator(ch_a)) {
            i_a--;
            skip_compare = true;
        }
        if (isSeparator(ch_b)) {
            i_b--;
            skip_compare = true;
        }

        if (!skip_compare) {
            if (ch_a != ch_b) {
                return false;
            }
            i_a--;
            i_b--;
        }
    }

    if (may_ignore_prefix) {
        bool trunk_prefix_ignorable_a = checkPrefixIsIgnorable(a, i_a);
        if ((trunk_prefix_is_omitted_a && i_a >= 0) || !trunk_prefix_ignorable_a) {
            if (accept_thailand_case) {
                // Maybe the code handling the special case for Thailand makes the
                // result garbled, so disable the code and try again.
                // e.g. "16610001234" must equal to "6610001234", but with
                //      Thailand-case handling code, they become equal to each other.
                //
                // Note: we select simplicity rather than adding some complicated
                //       logic here for performance(like "checking whether remaining
                //       numbers are just 66 or not"), assuming inputs are small
                //       enough.
                return phone_number_compare_inter(org_a, org_b, false);
            } else {
                return false;
            }
        } else if (trunk_prefix_ignorable_a && trunk_prefix_is_omitted_b) {
            bool cmp_prefixes = i_a == 0 && isDialable(a[i_a]);
            if (cmp_prefixes && org_b[i_a] != a[i_a]) {
                // Unmatched trunk prefix
                return false;
            }
        }

        bool trunk_prefix_ignorable_b = checkPrefixIsIgnorable(b, i_b);
        if ((trunk_prefix_is_omitted_b && i_b >= 0) || !trunk_prefix_ignorable_b) {
            if (accept_thailand_case) {
                return phone_number_compare_inter(org_a, org_b, false);
            } else {
                return false;
            }
        } else if (trunk_prefix_ignorable_b && trunk_prefix_is_omitted_a) {
            bool cmp_prefixes = i_b == 0 && isDialable(b[i_b]);
            if (cmp_prefixes && org_a[i_b] != b[i_b]) {
                // Unmatched trunk prefix
                return false;
            }
        }
    } else {
        // In the US, 1-650-555-1234 must be equal to 650-555-1234,
        // while 090-1234-1234 must not be equal to 90-1234-1234 in Japan.
        // This request exists just in US (with 1 trunk (NDD) prefix).
        // In addition, "011 11 7005554141" must not equal to "+17005554141",
        // while "011 1 7005554141" must equal to "+17005554141"
        //
        // In this comparison, we ignore the prefix '1' just once, when
        // - at least either does not have CCC, or
        // - the remaining non-separator number is 1
        bool may_be_namp = !both_have_ccc;
        while (i_a >= 0) {
            const char ch_a = a[i_a];
            if (isDialable(ch_a)) {
                if (may_be_namp && tryGetISODigit(ch_a) == 1) {
                    may_be_namp = false;
                } else {
                    return false;
                }
            }
            i_a--;
        }
        while (i_b >= 0) {
            const char ch_b = b[i_b];
            if (isDialable(ch_b)) {
                if (may_be_namp && tryGetISODigit(ch_b) == 1) {
                    may_be_namp = false;
                } else {
                    return false;
                }
            }
            i_b--;
        }
    }

    return true;
}

bool phone_number_compare_strict(const char* a, const char* b)
{
    return phone_number_compare_inter(a, b, true);
}

/**
 * Imitates the Java method PhoneNumberUtils.getStrippedReversed.
 * Used for API compatibility with Android 1.6 and earlier.
 */
bool phone_number_stripped_reversed_inter(const char* in, char* out, const int len, int *outlen) {
    int in_len = strlen(in);
    int out_len = 0;
    bool have_seen_plus = false;
    for (int i = in_len; --i >= 0;) {
        char c = in[i];
        if ((c >= '0' && c <= '9') || c == '*' || c == '#' || c == 'N') {
            if (out_len < len) {
                out[out_len++] = c;
            }
        } else {
            switch (c) {
              case '+':
                  if (!have_seen_plus) {
                      if (out_len < len) {
                          out[out_len++] = c;
                      }
                      have_seen_plus = true;
                  }
                  break;
              case ',':
              case ';':
                  out_len = 0;
                  break;
          }
        }
    }

    *outlen = out_len;
    return true;
}

/* ---- sqlite3_android.cpp ---- */
static void phone_numbers_equal(sqlite3_context * context, int argc, sqlite3_value ** argv)
{
    if (argc != 2 && argc != 3 && argc != 4) {
        sqlite3_result_int(context, 0);
        return;
    }

    char const * num1 = (char const *)sqlite3_value_text(argv[0]);
    char const * num2 = (char const *)sqlite3_value_text(argv[1]);

    bool use_strict = false;
    int min_match = 0;
    if (argc == 3 || argc == 4) {
        use_strict = (sqlite3_value_int(argv[2]) != 0);
        if (!use_strict && argc == 4) {
            min_match = sqlite3_value_int(argv[3]);
        }
    }

    if (num1 == NULL || num2 == NULL) {
        sqlite3_result_null(context);
        return;
    }

    bool equal =
        (use_strict ? phone_number_compare_strict(num1, num2)
                    : ((min_match > 0)
                           ? phone_number_compare_loose_with_minmatch(
                                 num1, num2, min_match)
                           : phone_number_compare_loose(num1, num2)));

    if (equal) {
        sqlite3_result_int(context, 1);
    } else {
        sqlite3_result_int(context, 0);
    }
}

int register_phone_numbers_equal(sqlite3 *handle)
{
	for (int argc = 2; argc <= 4; argc++) {
		int err = sqlite3_create_function(handle, "PHONE_NUMBERS_EQUAL", argc, SQLITE_UTF8, NULL, phone_numbers_equal, NULL, NULL);
		if (err != SQLITE_OK)
			return err;
	}
	return SQLITE_OK;
}
