package android.telephony;

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
}
