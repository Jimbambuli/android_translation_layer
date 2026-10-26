package android.text;

public interface InputFilter {

	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend);

	/* as AOSP: keeps the text at most max chars long */
	public static class LengthFilter extends Object implements InputFilter {
		private final int max;

		public LengthFilter(int max) {
			this.max = max;
		}

		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			int keep = max - (dest.length() - (dend - dstart));
			if (keep <= 0)
				return "";
			if (keep >= end - start)
				return null; // keep the original
			keep += start;
			if (Character.isHighSurrogate(source.charAt(keep - 1))) {
				--keep;
				if (keep == start)
					return "";
			}
			return source.subSequence(start, keep);
		}

		public int getMax() {
			return max;
		}
	}
}
