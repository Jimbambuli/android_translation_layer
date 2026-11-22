package android.telephony;

import android.content.res.Resources;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.telephony.Rlog;
import java.util.Locale;
import android.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtils {

	/* the dial-string helpers below are the bodies of AOSP PhoneNumberUtils.java */
	public static final char PAUSE = ',';
	public static final char WAIT = ';';
	public static final char WILD = 'N';
	private static final String CLIR_ON = "*31#";
	private static final String CLIR_OFF = "#31#";

	public static boolean isDialable(char c) {
		return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
	}

	public static boolean isNonSeparator(char c) {
		return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD || c == WAIT || c == PAUSE;
	}

	public static boolean isStartsPostDial(char c) {
		return c == PAUSE || c == WAIT;
	}

	public static String extractNetworkPortion(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}
		int len = phoneNumber.length();
		StringBuilder ret = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = phoneNumber.charAt(i);
			int digit = Character.digit(c, 10);
			if (digit != -1) {
				ret.append(digit);
			} else if (c == '+') {
				String prefix = ret.toString();
				if (prefix.length() == 0 || prefix.equals(CLIR_ON) || prefix.equals(CLIR_OFF)) {
					ret.append(c);
				}
			} else if (isDialable(c)) {
				ret.append(c);
			} else if (isStartsPostDial(c)) {
				break;
			}
		}
		return ret.toString();
	}

	private static int minPositive(int a, int b) {
		if (a >= 0 && b >= 0) {
			return (a < b) ? a : b;
		} else if (a >= 0) {
			return a;
		} else if (b >= 0) {
			return b;
		} else {
			return -1;
		}
	}

	private static int indexOfLastNetworkChar(String a) {
		int trimIndex = minPositive(a.indexOf(PAUSE), a.indexOf(WAIT));
		return trimIndex < 0 ? a.length() - 1 : trimIndex - 1;
	}

	public static String extractPostDialPortion(String phoneNumber) {
		if (phoneNumber == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int trimIndex = indexOfLastNetworkChar(phoneNumber);
		for (int i = trimIndex + 1, s = phoneNumber.length(); i < s; i++) {
			char c = phoneNumber.charAt(i);
			if (isNonSeparator(c)) {
				ret.append(c);
			}
		}
		return ret.toString();
	}

	public static boolean isGlobalPhoneNumber(String phoneNumber) {
		return phoneNumber != null && (phoneNumber.startsWith("+") || phoneNumber.startsWith("00"));
	}

	public static String stripSeparators(String phoneNumber) {
		return phoneNumber.replaceAll("[ \t\n\r-]", "");
	}

	/* AOSP: only the digits and a leading '+', letters turned into their keypad digits first */
	public static String normalizeNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < phoneNumber.length(); i++) {
			char c = phoneNumber.charAt(i);
			int digit = Character.digit(c, 10);
			if (digit != -1)
				sb.append(digit);
			else if (sb.length() == 0 && c == '+')
				sb.append(c);
			else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
				return normalizeNumber(convertKeypadLettersToDigits(phoneNumber));
		}
		return sb.toString();
	}

	private static final String KEYPAD = "22233344455566677778889999"; // the digit of each letter A to Z

	public static String convertKeypadLettersToDigits(String input) {
		if (input == null)
			return input;
		char[] out = input.toCharArray();
		for (int i = 0; i < out.length; i++) {
			char c = Character.toUpperCase(out[i]);
			if (c >= 'A' && c <= 'Z')
				out[i] = KEYPAD.charAt(c - 'A');
		}
		return new String(out);
	}

	/* AOSP compares loosely (compareLoosely(): separators ignored, a match of the last 7 digits is enough, so a
	 * number with and without its trunk or country prefix is the same); here the same rule, simplified to digits */
	private static final int MIN_MATCH = 7;

	public static boolean compare(String a, String b) {
		if (a == null || b == null)
			return a == b;
		String x = a.replaceAll("[^0-9]", ""), y = b.replaceAll("[^0-9]", "");
		if (x.length() < MIN_MATCH || y.length() < MIN_MATCH)
			return x.equals(y);
		return x.substring(x.length() - MIN_MATCH).equals(y.substring(y.length() - MIN_MATCH));
	}

	/* AOSP formats with libphonenumber for the country; ATL has no number metadata and leaves the number as it is */
	public static String formatNumber(String phoneNumber, String defaultCountryIso) {
		return phoneNumber;
	}

	/* AOSP formats in place by the NANP/Japan rules; ATL has no number formatting rules and leaves the text as it is */
	@Deprecated
	public static void formatNumber(android.text.Editable text, int defaultFormattingType) {
	}

	/* AOSP: formatNumber(Editable, getFormatTypeForLocale(Locale.getDefault())) on a copy of the source */
	@Deprecated
	public static String formatNumber(String source) {
		return source;
	}

	/* AOSP formats with libphonenumber, using the E164 number to find the country; ATL leaves the number as it is */
	public static String formatNumber(String phoneNumber, String phoneNumberE164, String defaultCountryIso) {
		return phoneNumber;
	}

	/* AOSP: compare(a, b, useStrict) with useStrict from config_use_strict_phone_number_comparation (false by default) */
	@Deprecated
	public static boolean compare(android.content.Context context, String a, String b) {
		return compare(a, b);
	}

	/* PhoneNumberUtils.isWellFormedSmsAddress() */
	public static boolean isWellFormedSmsAddress(String address) {
		String networkPortion = PhoneNumberUtils.extractNetworkPortion(address);
		return (!(networkPortion.equals("+") || android.text.TextUtils.isEmpty(networkPortion))) && isDialable(networkPortion);
	}

	/* PhoneNumberUtils.isDialable(String) (private in AOSP): every character is dialable */
	private static boolean isDialable(String address) {
		for (int i = 0, count = address.length(); i < count; i++) {
			if (!isDialable(address.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/* AOSP PhoneNumberUtils: the character, TOA and digit helpers that need no libphonenumber or telephony service */
	public static final int TOA_International = 0x91;
	public static final int TOA_Unknown = 0x81;
	private static final String PREFIX_WPS = "*272";
	private static final String PREFIX_WPS_CLIR_ACTIVATE = "*31#*272";
	private static final String PREFIX_WPS_CLIR_DEACTIVATE = "#31#*272";

	public static boolean is12Key(char c) {
		return (c >= '0' && c <= '9') || c == '*' || c == '#';
	}
	public static boolean isReallyDialable(char c) {
		return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
	}
	public static boolean isISODigit(char c) {
		return c >= '0' && c <= '9';
	}
	public static String stringFromStringAndTOA(String s, int TOA) {
		if (s == null) return null;
		if (TOA == TOA_International && s.length() > 0 && s.charAt(0) != '+') {
			return "+" + s;
		}
		return s;
	}
	public static int toaFromString(String s) {
		if (s != null && s.length() > 0 && s.charAt(0) == '+') {
			return TOA_International;
		}
		return TOA_Unknown;
	}
	public static String replaceUnicodeDigits(String number) {
		StringBuilder normalizedDigits = new StringBuilder(number.length());
		for (char c : number.toCharArray()) {
			int digit = Character.digit(c, 10);
			if (digit != -1) {
				normalizedDigits.append(digit);
			} else {
				normalizedDigits.append(c);
			}
		}
		return normalizedDigits.toString();
	}
	public static boolean isWpsCallNumber(String number) {
		return (number != null) && (number.startsWith(PREFIX_WPS) || number.startsWith(PREFIX_WPS_CLIR_ACTIVATE)
		                            || number.startsWith(PREFIX_WPS_CLIR_DEACTIVATE));
	}

	/* AOSP PhoneNumberUtils: BCD (3GPP TS 24.008 Called Party BCD / TS 51.011 EF_ADN) helpers, needed by SmsMessage; AOSP text unchanged */
    /** @hide */
    @IntDef(prefix = "BCD_EXTENDED_TYPE_", value = {
            BCD_EXTENDED_TYPE_EF_ADN,
            BCD_EXTENDED_TYPE_CALLED_PARTY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BcdExtendType {}

    /*
     * The BCD extended type used to determine the extended char for the digit which is greater than
     * 9.
     *
     * see TS 51.011 section 10.5.1 EF_ADN(Abbreviated dialling numbers)
     */
    public static final int BCD_EXTENDED_TYPE_EF_ADN = 1;

    /*
     * The BCD extended type used to determine the extended char for the digit which is greater than
     * 9.
     *
     * see TS 24.008 section 10.5.4.7 Called party BCD number
     */
    public static final int BCD_EXTENDED_TYPE_CALLED_PARTY = 2;
    private static final String BCD_EF_ADN_EXTENDED = "*#,N;";
    private static final String BCD_CALLED_PARTY_EXTENDED = "*#abc";

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     *
     * @return partial string on invalid decode
     *
     * @deprecated use {@link #calledPartyBCDToString(byte[], int, int, int)} instead. Calling this
     * method is equivalent to calling {@link #calledPartyBCDToString(byte[], int, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static String calledPartyBCDToString(byte[] bytes, int offset, int length) {
        return calledPartyBCDToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     * @param bcdExtType used to determine the extended bcd coding
     * @see #BCD_EXTENDED_TYPE_EF_ADN
     * @see #BCD_EXTENDED_TYPE_CALLED_PARTY
     *
     */
    public static String calledPartyBCDToString(
            byte[] bytes, int offset, int length, @BcdExtendType int bcdExtType) {
        boolean prependPlus = false;
        StringBuilder ret = new StringBuilder(1 + length * 2);

        if (length < 2) {
            return "";
        }

        //Only TON field should be taken in consideration
        if ((bytes[offset] & 0xf0) == (TOA_International & 0xf0)) {
            prependPlus = true;
        }

        internalCalledPartyBCDFragmentToString(
                ret, bytes, offset + 1, length - 1, bcdExtType);

        if (prependPlus && ret.length() == 0) {
            // If the only thing there is a prepended plus, return ""
            return "";
        }

        if (prependPlus) {
            // This is an "international number" and should have
            // a plus prepended to the dialing number. But there
            // can also be GSM MMI codes as defined in TS 22.030 6.5.2
            // so we need to handle those also.
            //
            // http://web.telia.com/~u47904776/gsmkode.htm
            // has a nice list of some of these GSM codes.
            //
            // Examples are:
            //   **21*+886988171479#
            //   **21*8311234567#
            //   *21#
            //   #21#
            //   *#21#
            //   *31#+11234567890
            //   #31#+18311234567
            //   #31#8311234567
            //   18311234567
            //   +18311234567#
            //   +18311234567
            // Odd ball cases that some phones handled
            // where there is no dialing number so they
            // append the "+"
            //   *21#+
            //   **21#+
            String retString = ret.toString();
            Pattern p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            Matcher m = p.matcher(retString);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just
                    // append a +, this handles **21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(3));
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                    ret.append("+");
                } else {
                    // Starts with [#*] and ends with #
                    // Assume group 4 is a dialing number
                    // such as *21*+1234554#
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(retString);
                if (m.matches()) {
                    // Starts with [#*] and only one other [#*]
                    // Assume the data after last [#*] is dialing
                    // number (i.e. group 4) such as *31#+11234567890.
                    // This also includes the odd ball *21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                } else {
                    // Does NOT start with [#*] just prepend '+'
                    ret = new StringBuilder();
                    ret.append('+');
                    ret.append(retString);
                }
            }
        }

        return ret.toString();
    }

    private static void internalCalledPartyBCDFragmentToString(
            StringBuilder sb, byte [] bytes, int offset, int length,
            @BcdExtendType int bcdExtType) {
        for (int i = offset ; i < length + offset ; i++) {
            byte b;
            char c;

            c = bcdToChar((byte)(bytes[i] & 0xf), bcdExtType);

            if (c == 0) {
                return;
            }
            sb.append(c);

            // FIXME(mkf) TS 23.040 9.1.2.3 says
            // "if a mobile receives 1111 in a position prior to
            // the last semi-octet then processing shall commence with
            // the next semi-octet and the intervening
            // semi-octet shall be ignored"
            // How does this jive with 24.008 10.5.4.7

            b = (byte)((bytes[i] >> 4) & 0xf);

            if (b == 0xf && i + 1 == length + offset) {
                //ignore final 0xf
                break;
            }

            c = bcdToChar(b, bcdExtType);
            if (c == 0) {
                return;
            }

            sb.append(c);
        }

    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     *
     * @deprecated use {@link #calledPartyBCDFragmentToString(byte[], int, int, int)} instead.
     * Calling this method is equivalent to calling
     * {@link #calledPartyBCDFragmentToString(byte[], int, int, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static String calledPartyBCDFragmentToString(byte[] bytes, int offset, int length) {
        return calledPartyBCDFragmentToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     */
    public static String calledPartyBCDFragmentToString(
            byte[] bytes, int offset, int length, @BcdExtendType int bcdExtType) {
        StringBuilder ret = new StringBuilder(length * 2);
        internalCalledPartyBCDFragmentToString(ret, bytes, offset, length, bcdExtType);
        return ret.toString();
    }

    /**
     * Returns the correspond character for given {@code b} based on {@code bcdExtType}, or 0 on
     * invalid code.
     */
    private static char bcdToChar(byte b, @BcdExtendType int bcdExtType) {
        if (b < 0xa) {
            return (char) ('0' + b);
        }

        String extended = null;
        if (BCD_EXTENDED_TYPE_EF_ADN == bcdExtType) {
            extended = BCD_EF_ADN_EXTENDED;
        } else if (BCD_EXTENDED_TYPE_CALLED_PARTY == bcdExtType) {
            extended = BCD_CALLED_PARTY_EXTENDED;
        }
        if (extended == null || b - 0xa >= extended.length()) {
            return 0;
        }

        return extended.charAt(b - 0xa);
    }

    private static int charToBCD(char c, @BcdExtendType int bcdExtType) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }

        String extended = null;
        if (BCD_EXTENDED_TYPE_EF_ADN == bcdExtType) {
            extended = BCD_EF_ADN_EXTENDED;
        } else if (BCD_EXTENDED_TYPE_CALLED_PARTY == bcdExtType) {
            extended = BCD_CALLED_PARTY_EXTENDED;
        }
        if (extended == null || extended.indexOf(c) == -1) {
            throw new RuntimeException("invalid char for BCD " + c);
        }
        return 0xa + extended.indexOf(c);
    }
    private static boolean isNonSeparator(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isNonSeparator(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * Note: calls extractNetworkPortion(), so do not use for
     * SIM EF[ADN] style records
     *
     * Returns null if network portion is empty.
     */
    public static byte[] networkPortionToCalledPartyBCD(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(
                networkPortion, false, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Same as {@link #networkPortionToCalledPartyBCD}, but includes a
     * one-byte length prefix.
     */
    public static byte[] networkPortionToCalledPartyBCDWithLength(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(
                networkPortion, true, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string. If the dialing number starts with '+', set to
     * international TOA
     *
     * @return BCD byte array
     *
     * @deprecated use {@link #numberToCalledPartyBCD(String, int)} instead. Calling this method
     * is equivalent to calling {@link #numberToCalledPartyBCD(String, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static byte[] numberToCalledPartyBCD(String number) {
        return numberToCalledPartyBCD(number, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string. If the dialing number starts with '+', set to
     * international TOA
     * @param bcdExtType used to determine the extended bcd coding
     * @see #BCD_EXTENDED_TYPE_EF_ADN
     * @see #BCD_EXTENDED_TYPE_CALLED_PARTY
     *
     * @return BCD byte array
     */
    public static byte[] numberToCalledPartyBCD(String number, @BcdExtendType int bcdExtType) {
        return numberToCalledPartyBCDHelper(number, false, bcdExtType);
    }

    /**
     * If includeLength is true, prepend a one-byte length value to
     * the return array.
     */
    private static byte[] numberToCalledPartyBCDHelper(
            String number, boolean includeLength, @BcdExtendType int bcdExtType) {
        int numberLenReal = number.length();
        int numberLenEffective = numberLenReal;
        boolean hasPlus = number.indexOf('+') != -1;
        if (hasPlus) numberLenEffective--;

        if (numberLenEffective == 0) return null;

        int resultLen = (numberLenEffective + 1) / 2;  // Encoded numbers require only 4 bits each.
        int extraBytes = 1;                            // Prepended TOA byte.
        if (includeLength) extraBytes++;               // Optional prepended length byte.
        resultLen += extraBytes;

        byte[] result = new byte[resultLen];

        int digitCount = 0;
        for (int i = 0; i < numberLenReal; i++) {
            char c = number.charAt(i);
            if (c == '+') continue;
            int shift = ((digitCount & 0x01) == 1) ? 4 : 0;
            result[extraBytes + (digitCount >> 1)] |=
                    (byte)((charToBCD(c, bcdExtType) & 0x0F) << shift);
            digitCount++;
        }

        // 1-fill any trailing odd nibble/quartet.
        if ((digitCount & 0x01) == 1) result[extraBytes + (digitCount >> 1)] |= 0xF0;

        int offset = 0;
        if (includeLength) result[offset++] = (byte)(resultLen - 1);
        result[offset] = (byte)(hasPlus ? TOA_International : TOA_Unknown);

        return result;
    }

    /** Extracts the phone number from an Intent.
     *
     * @param intent the intent to get the number of
     * @param context a context to use for database access
     *
     * @return the phone number that would be called by the intent, or
     *         <code>null</code> if the number cannot be found.
     */
    public static String getNumberFromIntent(Intent intent, Context context) {
        String number = null;

        Uri uri = intent.getData();

        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }

        if (scheme.equals("tel") || scheme.equals("sip")) {
            return uri.getSchemeSpecificPart();
        }

        if (context == null) {
            return null;
        }

        String type = intent.resolveType(context);
        String phoneColumn = null;

        // Correctly read out the phone entry based on requested provider
        final String authority = uri.getAuthority();
        if (Contacts.AUTHORITY.equals(authority)) {
            phoneColumn = Contacts.People.Phones.NUMBER;
        } else if (ContactsContract.AUTHORITY.equals(authority)) {
            phoneColumn = ContactsContract.CommonDataKinds.Phone.NUMBER;
        }

        Cursor c = null;
        try {
            c = context.getContentResolver().query(uri, new String[] { phoneColumn },
                    null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    number = c.getString(c.getColumnIndex(phoneColumn));
                }
            }
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "Error getting phone number.", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return number;
    }

    /**
     * Returns the rightmost minimum matched characters in the network portion
     * in *reversed* order
     *
     * This can be used to do a database lookup against the column
     * that stores getStrippedReversed()
     *
     * Returns null if phoneNumber == null
     */
    public static String
    toCallerIDMinMatch(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);
        return internalGetStrippedReversed(np, getMinMatch());
    }

    /**
     * Returns the network portion reversed.
     * This string is intended to go into an index column for a
     * database lookup.
     *
     * Returns null if phoneNumber == null
     */
    public static String
    getStrippedReversed(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);

        if (np == null) return null;

        return internalGetStrippedReversed(np, np.length());
    }

    //================ Number formatting =========================

    /** The current locale is unknown, look for a country code or don't format */
    public static final int FORMAT_UNKNOWN = 0;

    /** NANP formatting */
    public static final int FORMAT_NANP = 1;

    /** Japanese formatting */
    public static final int FORMAT_JAPAN = 2;

    /**
     * Returns the phone number formatting type for the given locale.
     *
     * @param locale The locale of interest, usually {@link Locale#getDefault()}
     * @return The formatting type for the given locale, or FORMAT_UNKNOWN if the formatting
     * rules are not known for the given locale
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static int getFormatTypeForLocale(Locale locale) {
        String country = locale.getCountry();

        return getFormatTypeFromCountryCode(country);
    }

    /**
     * Formats a phone number in-place using the NANP formatting rules. Numbers will be formatted
     * as:
     *
     * <p><code>
     * xxxxx
     * xxx-xxxx
     * xxx-xxx-xxxx
     * 1-xxx-xxx-xxxx
     * +1-xxx-xxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with the formatting
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static void formatNanpNumber(Editable text) {
        int length = text.length();
        if (length > "+1-nnn-nnn-nnnn".length()) {
            // The string is too long to be formatted
            return;
        } else if (length <= 5) {
            // The string is either a shortcode or too short to be formatted
            return;
        }

        CharSequence saved = text.subSequence(0, length);

        // Strip the dashes first, as we're going to add them back
        removeDashes(text);
        length = text.length();

        // When scanning the number we record where dashes need to be added,
        // if they're non-0 at the end of the scan the dashes will be added in
        // the proper places.
        int dashPositions[] = new int[3];
        int numDashes = 0;

        int state = NANP_STATE_DIGIT;
        int numDigits = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '1':
                    if (numDigits == 0 || state == NANP_STATE_PLUS) {
                        state = NANP_STATE_ONE;
                        break;
                    }
                    // fall through
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    if (state == NANP_STATE_PLUS) {
                        // Only NANP number supported for now
                        text.replace(0, length, saved);
                        return;
                    } else if (state == NANP_STATE_ONE) {
                        // Found either +1 or 1, follow it up with a dash
                        dashPositions[numDashes++] = i;
                    } else if (state != NANP_STATE_DASH && (numDigits == 3 || numDigits == 6)) {
                        // Found a digit that should be after a dash that isn't
                        dashPositions[numDashes++] = i;
                    }
                    state = NANP_STATE_DIGIT;
                    numDigits++;
                    break;

                case '-':
                    state = NANP_STATE_DASH;
                    break;

                case '+':
                    if (i == 0) {
                        // Plus is only allowed as the first character
                        state = NANP_STATE_PLUS;
                        break;
                    }
                    // Fall through
                default:
                    // Unknown character, bail on formatting
                    text.replace(0, length, saved);
                    return;
            }
        }

        if (numDigits == 7) {
            // With 7 digits we want xxx-xxxx, not xxx-xxx-x
            numDashes--;
        }

        // Actually put the dashes in place
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            text.replace(pos + i, pos + i, "-");
        }

        // Remove trailing dashes
        int len = text.length();
        while (len > 0) {
            if (text.charAt(len - 1) == '-') {
                text.delete(len - 1, len);
                len--;
            } else {
                break;
            }
        }
    }

    /**
     * Formats a phone number in-place using the Japanese formatting rules.
     * Numbers will be formatted as:
     *
     * <p><code>
     * 03-xxxx-xxxx
     * 090-xxxx-xxxx
     * 0120-xxx-xxx
     * +81-3-xxxx-xxxx
     * +81-90-xxxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with
     * the formatting
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static void formatJapaneseNumber(Editable text) {
        JapanesePhoneNumberFormatter.format(text);
    }

    /**
     * Formats the specified {@code phoneNumber} to the E.164 representation.
     *
     * @param phoneNumber the phone number to format.
     * @param defaultCountryIso the ISO 3166-1 two letters country code in UPPER CASE.
     * @return the E.164 representation, or null if the given phone number is not valid.
     */
    public static String formatNumberToE164(String phoneNumber, String defaultCountryIso) {
        return formatNumberInternal(phoneNumber, defaultCountryIso, false);
    }

    /**
     * Formats the specified {@code phoneNumber} to the RFC3966 representation.
     *
     * @param phoneNumber the phone number to format.
     * @param defaultCountryIso the ISO 3166-1 two letters country code in UPPER CASE.
     * @return the RFC3966 representation, or null if the given phone number is not valid.
     */
    public static String formatNumberToRFC3966(String phoneNumber, String defaultCountryIso) {
        return formatNumberInternal(phoneNumber, defaultCountryIso, true);
    }

    /* ATL: AOSP parses with libphonenumber, which needs the number metadata of every country (not in ATL).
     * An international number (+, 7 to 15 digits, no 0 after the +) is formatted as E.164 (+digits) or as
     * RFC 3966 (tel:+digits, without the national grouping); a number without + needs its country's
     * metadata to be told valid and gives null, as AOSP does for a number it cannot parse. */
    private static String formatNumberInternal(String rawInput, String defaultCountryIso, boolean rfc3966) {
        if (rawInput == null) {
            return null;
        }
        String network = extractNetworkPortion(rawInput);
        if (network.matches("\\+[1-9][0-9]{6,14}")) {
            return rfc3966 ? "tel:" + network : network;
        }
        return null;
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)} instead.
     */
    @Deprecated
    public static boolean isEmergencyNumber(String number) {
        return isEmergencyNumber(getDefaultVoiceSubId(), number);
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)}
     *             instead.
     *
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage
    public static boolean isEmergencyNumber(int subId, String number) {
        // Return true only if the specified number *exactly* matches
        // one of the emergency numbers listed by the RIL / SIM.
        return isEmergencyNumberInternal(subId, number);
    }

    /**
     * Checks if a given number is an emergency number for the country that the user is in.
     *
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @return true if the specified number is an emergency number for the country the user
     * is currently in.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)}
     *             instead.
     */
    @Deprecated
    public static boolean isLocalEmergencyNumber(Context context, String number) {
        return isEmergencyNumberInternal(getDefaultVoiceSubId(), number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     */
    public static boolean isVoiceMailNumber(String number) {
        return isVoiceMailNumber(SubscriptionManager.getDefaultSubscriptionId(), number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @hide
     */
    public static boolean isVoiceMailNumber(int subId, String number) {
        return isVoiceMailNumber(null, subId, number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param context {@link Context}.
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @hide
     */
    @SystemApi
    public static boolean isVoiceMailNumber(@NonNull Context context, int subId,
            @Nullable String number) {
        String vmNumber, mdn;
        try {
            final TelephonyManager tm;
            if (context == null) {
                tm = TelephonyManager.getDefault();
                if (DBG) log("isVoiceMailNumber: default tm");
            } else {
                tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (DBG) log("isVoiceMailNumber: tm from context");
            }
            vmNumber = tm.getVoiceMailNumber();
            mdn = tm.getLine1Number();
            if (DBG) log("isVoiceMailNumber: mdn=" + mdn + ", vmNumber=" + vmNumber
                    + ", number=" + number);
        } catch (SecurityException ex) {
            if (DBG) log("isVoiceMailNumber: SecurityExcpetion caught");
            return false;
        }
        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);
        if (TextUtils.isEmpty(number)) {
            if (DBG) log("isVoiceMailNumber: number is empty after stripping");
            return false;
        }

        // check if the carrier considers MDN to be an additional voicemail number
        boolean compareWithMdn = false;
        if (context != null) {
            CarrierConfigManager configManager = (CarrierConfigManager)
                    context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null) {
                PersistableBundle b = configManager.getConfigForSubId(subId);
                if (b != null) {
                    compareWithMdn = b.getBoolean(CarrierConfigManager.
                            KEY_MDN_IS_ADDITIONAL_VOICEMAIL_NUMBER_BOOL);
                    if (DBG) log("isVoiceMailNumber: compareWithMdn=" + compareWithMdn);
                }
            }
        }

        if (compareWithMdn) {
            if (DBG) log("isVoiceMailNumber: treating mdn as additional vm number");
            return compare(number, vmNumber) || compare(number, mdn);
        } else {
            if (DBG) log("isVoiceMailNumber: returning regular compare");
            return compare(number, vmNumber);
        }
    }

    /**
     * Wrap the supplied {@code CharSequence} with a {@code TtsSpan}, annotating it as
     * containing a phone number in its entirety.
     *
     * @param phoneNumber A {@code CharSequence} the entirety of which represents a phone number.
     * @return A {@code CharSequence} with appropriate annotations.
     */
    public static CharSequence createTtsSpannable(CharSequence phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(phoneNumber);
        PhoneNumberUtils.addTtsSpan(spannable, 0, spannable.length());
        return spannable;
    }

    /**
     * Attach a {@link TtsSpan} to the supplied {@code Spannable} at the indicated location,
     * annotating that location as containing a phone number.
     *
     * @param s A {@code Spannable} to annotate.
     * @param start The starting character position of the phone number in {@code s}.
     * @param endExclusive The position after the ending character in the phone number {@code s}.
     */
    public static void addTtsSpan(Spannable s, int start, int endExclusive) {
        s.setSpan(createTtsSpan(s.subSequence(start, endExclusive).toString()),
                start,
                endExclusive,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Create a {@code TtsSpan} for the supplied {@code String}.
     *
     * @param phoneNumberString A {@code String} the entirety of which represents a phone number.
     * @return A {@code TtsSpan} for {@param phoneNumberString}.
     */
    public static TtsSpan createTtsSpan(String phoneNumberString) {
        if (phoneNumberString == null) {
            return null;
        }

        // ATL: no libphonenumber to split off a country code; AOSP's path for a number it cannot parse
        final TtsSpan.TelephoneBuilder builder = new TtsSpan.TelephoneBuilder();
        // Strip separators otherwise TalkBack will be silent
        builder.setNumberParts(splitAtNonNumerics(phoneNumberString));
        return builder.build();
    }

    /**
     * Determines if two phone numbers are the same.
     * <p>
     * Matching is based on <a href="https://github.com/google/libphonenumber>libphonenumber</a>.
     * Unlike {@link #compare(String, String)}, matching takes into account national
     * dialing plans rather than simply matching the last 7 digits of the two phone numbers. As a
     * result, it is expected that some numbers which would match using the previous method will no
     * longer match using this new approach.
     *
     * @param number1
     * @param number2
     * @param defaultCountryIso The lowercase two letter ISO 3166-1 country code. Used when parsing
     *                          the phone numbers where it is not possible to determine the country
     *                          associated with a phone number based on the number alone. It
     *                          is recommended to pass in
     *                          {@link TelephonyManager#getNetworkCountryIso()}.
     * @return True if the two given phone number are same.
     */
    public static boolean areSamePhoneNumber(@NonNull String number1,
            @NonNull String number2, @NonNull String defaultCountryIso) {
        // ATL: AOSP matches with libphonenumber's isNumberMatch(); compare() is the matching of ATL
        if (number1 == null || number2 == null) {
            return false;
        }
        return compare(number1, number2);
    }

    static final String LOG_TAG = "PhoneNumberUtils";

    private static final boolean DBG = false;

    private static int getMinMatch() {
        if (sMinMatch == 0) {
            sMinMatch = Resources.getSystem().getInteger(
                    com.android.internal.R.integer.config_phonenumber_compare_min_match);
        }
        return sMinMatch;
    }

    /**
     * Extracts the network address portion and canonicalize.
     *
     * This function is equivalent to extractNetworkPortion(), except
     * for allowing the PLUS character to occur at arbitrary positions
     * in the address portion, not just the first position.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static String extractNetworkPortionAlt(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean haveSeenPlus = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (c == '+') {
                if (haveSeenPlus) {
                    continue;
                }
                haveSeenPlus = true;
            }
            if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    /**
     * Returns the last numDigits of the reversed phone number
     * Returns null if np == null
     */
    private static String
    internalGetStrippedReversed(String np, int numDigits) {
        if (np == null) return null;

        StringBuilder ret = new StringBuilder(numDigits);
        int length = np.length();

        for (int i = length - 1, s = length
            ; i >= 0 && (s - i) <= numDigits ; i--
        ) {
            char c = np.charAt(i);

            ret.append(c);
        }

        return ret.toString();
    }

    private static final int NANP_STATE_DIGIT = 1;

    private static final int NANP_STATE_PLUS = 2;

    private static final int NANP_STATE_ONE = 3;

    private static final int NANP_STATE_DASH = 4;

    /**
     * Removes all dashes from the number.
     *
     * @param text the number to clear from dashes
     */
    private static void removeDashes(Editable text) {
        int p = 0;
        while (p < text.length()) {
            if (text.charAt(p) == '-') {
                text.delete(p, p + 1);
           } else {
                p++;
           }
        }
    }

    /**
     * Helper function for isEmergencyNumber(String, String) and.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is an emergency number for the specified country.
     * @hide
     */
    private static boolean isEmergencyNumberInternal(int subId, String number) {
        //TODO: remove subid later. Keep it for now in case we need it later.
        try {
                return TelephonyManager.getDefault().isEmergencyNumber(number);
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "isEmergencyNumberInternal: RuntimeException: " + ex);
        }
        return false;
    }

    // Split a phone number like "+20(123)-456#" using spaces, ignoring anything that is not
    // a digit or the characters * and #, to produce a result like "20 123 456#".
    private static String splitAtNonNumerics(CharSequence number) {
        StringBuilder sb = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            sb.append(PhoneNumberUtils.is12Key(number.charAt(i))
                    ? number.charAt(i)
                    : " ");
        }
        // It is very important to remove extra spaces. At time of writing, any leading or trailing
        // spaces, or any sequence of more than one space, will confuse TalkBack and cause the TTS
        // span to be non-functional!
        return sb.toString().replaceAll(" +", " ").trim();
    }

    private static int getFormatTypeFromCountryCode (String country) {
        // Check for the NANP countries
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return FORMAT_NANP;
            }
        }
        if ("jp".compareToIgnoreCase(country) == 0) {
            return FORMAT_JAPAN;
        }
        return FORMAT_UNKNOWN;
    }

    /**
     * Returns Default voice subscription Id.
     */
    private static int getDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    private static int sMinMatch = 0;

    /** List of country codes for countries that use the NANP */
    private static final String[] NANP_COUNTRIES = new String[] {
        "US", // United States
        "CA", // Canada
        "AS", // American Samoa
        "AI", // Anguilla
        "AG", // Antigua and Barbuda
        "BS", // Bahamas
        "BB", // Barbados
        "BM", // Bermuda
        "VG", // British Virgin Islands
        "KY", // Cayman Islands
        "DM", // Dominica
        "DO", // Dominican Republic
        "GD", // Grenada
        "GU", // Guam
        "JM", // Jamaica
        "PR", // Puerto Rico
        "MS", // Montserrat
        "MP", // Northern Mariana Islands
        "KN", // Saint Kitts and Nevis
        "LC", // Saint Lucia
        "VC", // Saint Vincent and the Grenadines
        "TT", // Trinidad and Tobago
        "TC", // Turks and Caicos Islands
        "VI", // U.S. Virgin Islands
    };
}
