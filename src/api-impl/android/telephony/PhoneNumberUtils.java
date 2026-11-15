package android.telephony;

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
}
