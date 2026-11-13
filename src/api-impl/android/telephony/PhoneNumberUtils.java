package android.telephony;

public class PhoneNumberUtils {

	public static boolean isNonSeparator(char c) {
		return c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '-';
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
}
