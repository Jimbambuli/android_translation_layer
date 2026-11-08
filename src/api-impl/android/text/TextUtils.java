/*
 * most of this file:
 *
 * Copyright (C) 2006 The Android Open Source Project
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

package android.text;

import android.icu.text.CaseMap;
import android.icu.text.Edits;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.icu.util.ULocale;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.ReplacementSpan;
import android.text.style.SuggestionSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ScaleXSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UpdateAppearance;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextUtils {

	public static final int LINE_FEED_CODE_POINT = 10;
	private static final int NBSP_CODE_POINT = 160;
	private static final int PARCEL_SAFE_TEXT_LENGTH = 100000;
	/* like AOSP: RTL for the locales whose script is written right to left */
	public static int getLayoutDirectionFromLocale(Locale locale) {
		return (locale != null && !locale.equals(Locale.ROOT) && ULocale.forLocale(locale).isRightToLeft())
		           ? View.LAYOUT_DIRECTION_RTL
		           : View.LAYOUT_DIRECTION_LTR;
	}

	/* split */

	private static String[] EMPTY_STRING_ARRAY = new String[] {};

	public static String[] split(String text, String expression) {
		if (text.length() == 0) {
			return EMPTY_STRING_ARRAY;
		} else {
			return text.split(expression, -1);
		}
	}

	public static String[] split(String text, Pattern pattern) {
		if (text.length() == 0) {
			return EMPTY_STRING_ARRAY;
		} else {
			return pattern.split(text, -1);
		}
	}

	/* join */

	public static String join(CharSequence delimiter, Object[] tokens) {
		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Object token : tokens) {
			if (firstTime) {
				firstTime = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(token);
		}
		return sb.toString();
	}

	public static String join(CharSequence delimiter, Iterable tokens) {
		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Object token : tokens) {
			if (firstTime) {
				firstTime = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(token);
		}
		return sb.toString();
	}

	public static int indexOf(CharSequence s, char ch) {
		return indexOf(s, ch, 0);
	}

	public static int indexOf(CharSequence s, char ch, int start) {
		Class<? extends CharSequence> c = s.getClass();

		if (c == String.class)
			return ((String)s).indexOf(ch, start);

		return indexOf(s, ch, start, s.length());
	}

	public static int indexOf(CharSequence s, char ch, int start, int end) {
		Class<? extends CharSequence> c = s.getClass();

		if (s instanceof GetChars || c == StringBuffer.class
		    || c == StringBuilder.class || c == String.class) {
			final int INDEX_INCREMENT = 500;
			char[] temp = obtain(INDEX_INCREMENT);

			while (start < end) {
				int segend = start + INDEX_INCREMENT;
				if (segend > end)
					segend = end;

				getChars(s, start, segend, temp, 0);

				int count = segend - start;
				for (int i = 0; i < count; i++) {
					if (temp[i] == ch) {
						recycle(temp);
						return i + start;
					}
				}

				start = segend;
			}

			recycle(temp);
			return -1;
		}

		for (int i = start; i < end; i++)
			if (s.charAt(i) == ch)
				return i;

		return -1;
	}

	public static int lastIndexOf(CharSequence s, char ch) {
		return lastIndexOf(s, ch, s.length() - 1);
	}

	public static int lastIndexOf(CharSequence s, char ch, int last) {
		Class<? extends CharSequence> c = s.getClass();

		if (c == String.class)
			return ((String)s).lastIndexOf(ch, last);

		return lastIndexOf(s, ch, 0, last);
	}

	public static int lastIndexOf(CharSequence s, char ch,
	                              int start, int last) {
		if (last < 0)
			return -1;
		if (last >= s.length())
			last = s.length() - 1;

		int end = last + 1;

		Class<? extends CharSequence> c = s.getClass();

		if (s instanceof GetChars || c == StringBuffer.class
		    || c == StringBuilder.class || c == String.class) {
			final int INDEX_INCREMENT = 500;
			char[] temp = obtain(INDEX_INCREMENT);

			while (start < end) {
				int segstart = end - INDEX_INCREMENT;
				if (segstart < start)
					segstart = start;

				getChars(s, segstart, end, temp, 0);

				int count = end - segstart;
				for (int i = count - 1; i >= 0; i--) {
					if (temp[i] == ch) {
						recycle(temp);
						return i + segstart;
					}
				}

				end = segstart;
			}

			recycle(temp);
			return -1;
		}

		for (int i = end - 1; i >= start; i--)
			if (s.charAt(i) == ch)
				return i;

		return -1;
	}

	public static int indexOf(CharSequence s, CharSequence needle) {
		return indexOf(s, needle, 0, s.length());
	}

	public static int indexOf(CharSequence s, CharSequence needle, int start) {
		return indexOf(s, needle, start, s.length());
	}

	public static int indexOf(CharSequence s, CharSequence needle,
	                          int start, int end) {
		int nlen = needle.length();
		if (nlen == 0)
			return start;

		char c = needle.charAt(0);

		for (;;) {
			start = indexOf(s, c, start);
			if (start > end - nlen) {
				break;
			}

			if (start < 0) {
				return -1;
			}

			if (regionMatches(s, start, needle, 0, nlen)) {
				return start;
			}

			start++;
		}
		return -1;
	}

	public static boolean regionMatches(CharSequence one, int toffset,
	                                    CharSequence two, int ooffset,
	                                    int len) {
		int tempLen = 2 * len;
		if (tempLen < len) {
			// Integer overflow; len is unreasonably large
			throw new IndexOutOfBoundsException();
		}
		char[] temp = obtain(tempLen);

		getChars(one, toffset, toffset + len, temp, 0);
		getChars(two, ooffset, ooffset + len, temp, len);

		boolean match = true;
		for (int i = 0; i < len; i++) {
			if (temp[i] != temp[i + len]) {
				match = false;
				break;
			}
		}

		recycle(temp);
		return match;
	}

	/* package */ static char[] obtain(int len) {
		char[] buf;

		synchronized (sLock) {
			buf = sTemp;
			sTemp = null;
		}

		if (buf == null || buf.length < len)
			buf = ArrayUtils.newUnpaddedCharArray(len);

		return buf;
	}

	/* package */ static void recycle(char[] temp) {
		if (temp.length > 1000)
			return;

		synchronized (sLock) {
			sTemp = temp;
		}
	}
	// end of unchanged from android source

	public static CharSequence join(Iterable<CharSequence> list) {
		final CharSequence delimiter = ","; // ????
		return join(delimiter, list);
	}

	public static boolean isEmpty(CharSequence str) {
		return str == null || str.length() == 0;
	}

	/**
	 * Returns true if a and b are equal, including if they are both null.
	 * <p><i>Note: In platform versions 1.1 and earlier, this method only worked well if
	 * both the arguments were instances of String.</i></p>
	 * @param a first CharSequence to check
	 * @param b second CharSequence to check
	 * @return true if a and b are equal
	 */
	public static boolean equals(CharSequence a, CharSequence b) {
		if (a == b)
			return true;
		int length;
		if (a != null && b != null && (length = a.length()) == b.length()) {
			if (a instanceof String && b instanceof String) {
				return a.equals(b);
			} else {
				for (int i = 0; i < length; i++) {
					if (a.charAt(i) != b.charAt(i))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public enum TruncateAt {
		START,
		MIDDLE,
		END,
		MARQUEE,
		END_SMALL
	}

	/**
	 * Returns the original text if it fits in the specified width
	 * given the properties of the specified Paint,
	 * or, if it does not fit, a truncated
	 * copy with ellipsis character added at the specified edge or center.
	 */
	public static CharSequence ellipsize(CharSequence text, TextPaint p, float avail, TruncateAt where) {
		return text;
	}

	public static void getChars(CharSequence s, int start, int end, char[] dest, int destoff) {
		Class<? extends CharSequence> c = s.getClass();
		if (c == String.class)
			((String)s).getChars(start, end, dest, destoff);
		else if (c == StringBuffer.class)
			((StringBuffer)s).getChars(start, end, dest, destoff);
		else if (c == StringBuilder.class)
			((StringBuilder)s).getChars(start, end, dest, destoff);
		else if (s instanceof GetChars)
			((GetChars)s).getChars(start, end, dest, destoff);
		else {
			for (int i = start; i < end; i++)
				dest[destoff++] = s.charAt(i);
		}
	}

	/**
	 * Capitalization mode for {@link #getCapsMode}: capitalize all
	 * characters.  This value is explicitly defined to be the same as
	 * {@link InputType#TYPE_TEXT_FLAG_CAP_CHARACTERS}.
	 */
	public static final int CAP_MODE_CHARACTERS
			= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;

	/**
	 * Capitalization mode for {@link #getCapsMode}: capitalize the first
	 * character of all words.  This value is explicitly defined to be the same as
	 * {@link InputType#TYPE_TEXT_FLAG_CAP_WORDS}.
	 */
	public static final int CAP_MODE_WORDS
			= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

	/**
	 * Capitalization mode for {@link #getCapsMode}: capitalize the first
	 * character of each sentence.  This value is explicitly defined to be the same as
	 * {@link InputType#TYPE_TEXT_FLAG_CAP_SENTENCES}.
	 */
	public static final int CAP_MODE_SENTENCES
			= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

	/**
	 * Debugging tool to print the spans in a CharSequence.  The output will
	 * be printed one span per line.  If the CharSequence is not a Spanned,
	 * then the entire string will be printed on a single line.
	 */
	public static void dumpSpans(CharSequence cs, android.util.Printer printer, String prefix) {
		if (cs instanceof Spanned) {
			Spanned sp = (Spanned)cs;
			Object[] os = sp.getSpans(0, cs.length(), Object.class);

			for (int i = 0; i < os.length; i++) {
				Object o = os[i];
				printer.println(prefix + cs.subSequence(sp.getSpanStart(o), sp.getSpanEnd(o)) + ": " + Integer.toHexString(System.identityHashCode(o)) + " " + o.getClass().getCanonicalName() + " (" + sp.getSpanStart(o) + "-" + sp.getSpanEnd(o) + ") fl=#" + sp.getSpanFlags(o));
			}
		} else {
			printer.println(prefix + cs + ": (no spans)");
		}
	}

	/**
	 * Determine what caps mode should be in effect at the current offset in
	 * the text.  Only the mode bits set in <var>reqModes</var> will be
	 * checked.  Note that the caps mode flags here are explicitly defined
	 * to match those in {@link InputType}.
	 *
	 * @param cs The text that should be checked for caps modes.
	 * @param off Location in the text at which to check.
	 * @param reqModes The modes to be checked: may be any combination of
	 * {@link #CAP_MODE_CHARACTERS}, {@link #CAP_MODE_WORDS}, and
	 * {@link #CAP_MODE_SENTENCES}.
	 *
	 * @return Returns the actual capitalization modes that can be in effect
	 * at the current position, which is any combination of
	 * {@link #CAP_MODE_CHARACTERS}, {@link #CAP_MODE_WORDS}, and
	 * {@link #CAP_MODE_SENTENCES}.
	 */
	public static int getCapsMode(CharSequence cs, int off, int reqModes) {
		if (off < 0) {
			return 0;
		}

		int i;
		char c;
		int mode = 0;

		if ((reqModes&CAP_MODE_CHARACTERS) != 0) {
			mode |= CAP_MODE_CHARACTERS;
		}
		if ((reqModes&(CAP_MODE_WORDS|CAP_MODE_SENTENCES)) == 0) {
			return mode;
		}

		// Back over allowed opening punctuation.

		for (i = off; i > 0; i--) {
			c = cs.charAt(i - 1);

			if (c != '"' && c != '\'' &&
				Character.getType(c) != Character.START_PUNCTUATION) {
				break;
			}
		}

		// Start of paragraph, with optional whitespace.

		int j = i;
		while (j > 0 && ((c = cs.charAt(j - 1)) == ' ' || c == '\t')) {
			j--;
		}
		if (j == 0 || cs.charAt(j - 1) == '\n') {
			return mode | CAP_MODE_WORDS;
		}

		// Or start of word if we are that style.

		if ((reqModes&CAP_MODE_SENTENCES) == 0) {
			if (i != j) mode |= CAP_MODE_WORDS;
			return mode;
		}

		// There must be a space if not the start of paragraph.

		if (i == j) {
			return mode;
		}

		// Back over allowed closing punctuation.

		for (; j > 0; j--) {
			c = cs.charAt(j - 1);

			if (c != '"' && c != '\'' &&
				Character.getType(c) != Character.END_PUNCTUATION) {
				break;
			}
		}

		if (j > 0) {
			c = cs.charAt(j - 1);

			if (c == '.' || c == '?' || c == '!') {
				// Do not capitalize if the word ends with a period but
				// also contains a period, in which case it is an abbreviation.

				if (c == '.') {
					for (int k = j - 2; k >= 0; k--) {
						c = cs.charAt(k);

						if (c == '.') {
							return mode;
						}

						if (!Character.isLetter(c)) {
							break;
						}
					}
				}

				return mode | CAP_MODE_SENTENCES;
			}
		}

		return mode;
	}

	/**
	 * Does a comma-delimited list 'delimitedString' contain a certain item?
	 * (without allocating memory)
	 *
	 * @hide
	 */

	private static Object sLock = new Object();

	private static char[] sTemp = null;

	public static int getTrimmedLength(CharSequence s) {
		return s.toString().trim().length();
	}

	public static String htmlEncode(String s) {
		return s;
	}

	public static CharSequence concat(CharSequence... array) {
		StringBuilder sb = new StringBuilder();
		for (CharSequence cs : array) {
			sb.append(cs);
		}
		return sb;
	}

	public static String substring(CharSequence s, int start, int end) {
		return s.subSequence(start, end).toString();
	}

	public static boolean isDigitsOnly(CharSequence str) {
		final int len = str.length();
		for (int cp, i = 0; i < len; i += Character.charCount(cp)) {
			cp = Character.codePointAt(str, i);
			if (!Character.isDigit(cp)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * An interface for splitting strings according to rules that are opaque to the user of this
	 * interface. This also has less overhead than split, which uses regular expressions and
	 * allocates an array to hold the results.
	 *
	 * <p>The most efficient way to use this class is:
	 *
	 * <pre>
	 * // Once
	 * TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(delimiter);
	 *
	 * // Once per string to split
	 * splitter.setString(string);
	 * for (String s : splitter) {
	 *     ...
	 * }
	 * </pre>
	 */
	public interface StringSplitter extends Iterable<String> {
		public void setString(String string);
	}

	/**
	 * A simple string splitter.
	 *
	 * <p>If the final character in the string to split is the delimiter then no empty string will
	 * be returned for the empty string after that delimeter. That is, splitting <tt>"a,b,"</tt> on
	 * comma will return <tt>"a", "b"</tt>, not <tt>"a", "b", ""</tt>.
	 */
	public static class SimpleStringSplitter implements StringSplitter, Iterator<String> {
		private String mString;
		private char mDelimiter;
		private int mPosition;
		private int mLength;

		/**
		 * Initializes the splitter. setString may be called later.
		 * @param delimiter the delimeter on which to split
		 */
		public SimpleStringSplitter(char delimiter) {
			mDelimiter = delimiter;
		}

		/**
		 * Sets the string to split
		 * @param string the string to split
		 */
		public void setString(String string) {
			mString = string;
			mPosition = 0;
			mLength = mString.length();
		}

		public Iterator<String> iterator() {
			return this;
		}

		public boolean hasNext() {
			return mPosition < mLength;
		}

		public String next() {
			int end = mString.indexOf(mDelimiter, mPosition);
			if (end == -1) {
				end = mLength;
			}
			String nextString = mString.substring(mPosition, end);
			mPosition = end + 1; // Skip the delimiter.
			return nextString;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/* android-14 TextUtils.toUpperCase(), for InputFilter.AllCaps */
	public static CharSequence toUpperCase(Locale locale, CharSequence source,
			boolean copySpans) {
		final Edits edits = new Edits();
		if (!copySpans) { // No spans. Just uppercase the characters.
			final StringBuilder result = CaseMap.toUpper().apply(
					locale, source, new StringBuilder(), edits);
			return edits.hasChanges() ? result : source;
		}

		final SpannableStringBuilder result = CaseMap.toUpper().apply(
				locale, source, new SpannableStringBuilder(), edits);
		if (!edits.hasChanges()) {
			// No changes happened while capitalizing. We can return the source as it was.
			return source;
		}

		final Edits.Iterator iterator = edits.getFineIterator();
		final int sourceLength = source.length();
		final Spanned spanned = (Spanned) source;
		final Object[] spans = spanned.getSpans(0, sourceLength, Object.class);
		for (Object span : spans) {
			final int sourceStart = spanned.getSpanStart(span);
			final int sourceEnd = spanned.getSpanEnd(span);
			final int flags = spanned.getSpanFlags(span);
			// Make sure the indices are not at the end of the string, since in that case
			// iterator.findSourceIndex() would fail.
			final int destStart = sourceStart == sourceLength ? result.length() :
					toUpperMapToDest(iterator, sourceStart);
			final int destEnd = sourceEnd == sourceLength ? result.length() :
					toUpperMapToDest(iterator, sourceEnd);
			result.setSpan(span, destStart, destEnd, flags);
		}
		return result;
	}

	private static int toUpperMapToDest(Edits.Iterator iterator, int sourceIndex) {
		// Guaranteed to succeed if sourceIndex < source.length().
		iterator.findSourceIndex(sourceIndex);
		if (sourceIndex == iterator.sourceIndex()) {
			return iterator.destinationIndex();
		}
		// We handle the situation differently depending on if we are in the changed slice or an
		// unchanged one: In an unchanged slice, we can find the exact location the span
		// boundary was before and map there.
		//
		// But in a changed slice, we need to treat the whole destination slice as an atomic unit.
		// We adjust the span boundary to the end of that slice to reduce of the chance of adjacent
		// spans in the source overlapping in the result. (The choice for the end vs the beginning
		// is somewhat arbitrary, but was taken because we except to see slightly more spans only
		// affecting a base character compared to spans only affecting a combining character.)
		if (iterator.hasChange()) {
			return iterator.destinationIndex() + iterator.newLength();
		} else {
			// Move the index 1:1 along with this unchanged piece of text.
			return iterator.destinationIndex() + (sourceIndex - iterator.sourceIndex());
		}
	}

	/* android-14 TextUtils.stringOrSpannedString() */
	public static CharSequence stringOrSpannedString(CharSequence source) {
		if (source == null)
			return null;
		if (source instanceof SpannedString)
			return source;
		if (source instanceof Spanned)
			return new SpannedString(source);

		return source.toString();
	}

    private static final String TAG = "TextUtils";

    /** @hide */
    public static final int ALIGNMENT_SPAN = 1;
    /** @hide */
    public static final int FIRST_SPAN = ALIGNMENT_SPAN;
    /** @hide */
    public static final int FOREGROUND_COLOR_SPAN = 2;
    /** @hide */
    public static final int RELATIVE_SIZE_SPAN = 3;
    /** @hide */
    public static final int SCALE_X_SPAN = 4;
    /** @hide */
    public static final int STRIKETHROUGH_SPAN = 5;
    /** @hide */
    public static final int UNDERLINE_SPAN = 6;
    /** @hide */
    public static final int STYLE_SPAN = 7;
    /** @hide */
    public static final int BULLET_SPAN = 8;
    /** @hide */
    public static final int QUOTE_SPAN = 9;
    /** @hide */
    public static final int LEADING_MARGIN_SPAN = 10;
    /** @hide */
    public static final int URL_SPAN = 11;
    /** @hide */
    public static final int BACKGROUND_COLOR_SPAN = 12;
    /** @hide */
    public static final int TYPEFACE_SPAN = 13;
    /** @hide */
    public static final int SUPERSCRIPT_SPAN = 14;
    /** @hide */
    public static final int SUBSCRIPT_SPAN = 15;
    /** @hide */
    public static final int ABSOLUTE_SIZE_SPAN = 16;
    /** @hide */
    public static final int TEXT_APPEARANCE_SPAN = 17;
    /** @hide */
    public static final int ANNOTATION = 18;
    /** @hide */
    public static final int SUGGESTION_SPAN = 19;
    /** @hide */
    public static final int SPELL_CHECK_SPAN = 20;
    /** @hide */
    public static final int SUGGESTION_RANGE_SPAN = 21;
    /** @hide */
    public static final int EASY_EDIT_SPAN = 22;
    /** @hide */
    public static final int LOCALE_SPAN = 23;
    /** @hide */
    public static final int TTS_SPAN = 24;
    /** @hide */
    public static final int ACCESSIBILITY_CLICKABLE_SPAN = 25;
    /** @hide */
    public static final int ACCESSIBILITY_URL_SPAN = 26;
    /** @hide */
    public static final int LINE_BACKGROUND_SPAN = 27;
    /** @hide */
    public static final int LINE_HEIGHT_SPAN = 28;
    /** @hide */
    public static final int ACCESSIBILITY_REPLACEMENT_SPAN = 29;
    /** @hide */
    public static final int LAST_SPAN = ACCESSIBILITY_REPLACEMENT_SPAN;

    /**
     * Flatten a CharSequence and whatever styles can be copied across processes
     * into the parcel.
     */
    public static void writeToParcel(@Nullable CharSequence cs, @NonNull Parcel p,
            int parcelableFlags) {
        if (cs instanceof Spanned) {
            p.writeInt(0);
            p.writeString8(cs.toString());

            Spanned sp = (Spanned) cs;
            Object[] os = sp.getSpans(0, cs.length(), Object.class);

            // note to people adding to this: check more specific types
            // before more generic types.  also notice that it uses
            // "if" instead of "else if" where there are interfaces
            // so one object can be several.

            for (int i = 0; i < os.length; i++) {
                Object o = os[i];
                Object prop = os[i];

                if (prop instanceof CharacterStyle) {
                    prop = ((CharacterStyle) prop).getUnderlying();
                }

                if (prop instanceof ParcelableSpan) {
                    final ParcelableSpan ps = (ParcelableSpan) prop;
                    final int spanTypeId = ps.getSpanTypeIdInternal();
                    if (spanTypeId < FIRST_SPAN || spanTypeId > LAST_SPAN) {
                        Log.e(TAG, "External class \"" + ps.getClass().getSimpleName()
                                + "\" is attempting to use the frameworks-only ParcelableSpan"
                                + " interface");
                    } else {
                        p.writeInt(spanTypeId);
                        ps.writeToParcelInternal(p, parcelableFlags);
                        writeWhere(p, sp, o);
                    }
                }
            }

            p.writeInt(0);
        } else {
            p.writeInt(1);
            if (cs != null) {
                p.writeString8(cs.toString());
            } else {
                p.writeString8(null);
            }
        }
    }

    private static void writeWhere(Parcel p, Spanned sp, Object o) {
        p.writeInt(sp.getSpanStart(o));
        p.writeInt(sp.getSpanEnd(o));
        p.writeInt(sp.getSpanFlags(o));
    }

    public static final Parcelable.Creator<CharSequence> CHAR_SEQUENCE_CREATOR
            = new Parcelable.Creator<CharSequence>() {
        /**
         * Read and return a new CharSequence, possibly with styles,
         * from the parcel.
         */
        public CharSequence createFromParcel(Parcel p) {
            int kind = p.readInt();

            String string = p.readString8();
            if (string == null) {
                return null;
            }

            if (kind == 1) {
                return string;
            }

            SpannableString sp = new SpannableString(string);

            while (true) {
                kind = p.readInt();

                if (kind == 0)
                    break;

                switch (kind) {
                case FOREGROUND_COLOR_SPAN:
                    readSpan(p, sp, new ForegroundColorSpan(p));
                    break;

                case RELATIVE_SIZE_SPAN:
                    readSpan(p, sp, new RelativeSizeSpan(p));
                    break;

                case SCALE_X_SPAN:
                    readSpan(p, sp, new ScaleXSpan(p));
                    break;

                case STRIKETHROUGH_SPAN:
                    readSpan(p, sp, new StrikethroughSpan(p));
                    break;

                case UNDERLINE_SPAN:
                    readSpan(p, sp, new UnderlineSpan(p));
                    break;

                case STYLE_SPAN:
                    readSpan(p, sp, new StyleSpan(p));
                    break;

                case QUOTE_SPAN:
                    readSpan(p, sp, new QuoteSpan(p));
                    break;

                case LEADING_MARGIN_SPAN:
                    readSpan(p, sp, new LeadingMarginSpan.Standard(p));
                    break;

                case SUGGESTION_SPAN:
                    readSpan(p, sp, new SuggestionSpan(p));
                    break;

                case URL_SPAN:
                    readSpan(p, sp, new URLSpan(p));
                    break;

                case BACKGROUND_COLOR_SPAN:
                    readSpan(p, sp, new BackgroundColorSpan(p));
                    break;

                case TYPEFACE_SPAN:
                    readSpan(p, sp, new TypefaceSpan(p));
                    break;

                case SUPERSCRIPT_SPAN:
                    readSpan(p, sp, new SuperscriptSpan(p));
                    break;

                case SUBSCRIPT_SPAN:
                    readSpan(p, sp, new SubscriptSpan(p));
                    break;

                case ABSOLUTE_SIZE_SPAN:
                    readSpan(p, sp, new AbsoluteSizeSpan(p));
                    break;

                default:
                    throw new RuntimeException("bogus span encoding " + kind);
                }
            }

            return sp;
        }

        public CharSequence[] newArray(int size)
        {
            return new CharSequence[size];
        }
    };

    private static void readSpan(Parcel p, Spannable sp, Object o) {
        sp.setSpan(o, p.readInt(), p.readInt(), p.readInt());
    }

    public static String nullIfEmpty(@Nullable String str) {
        return isEmpty(str) ? null : str;
    }

    public static String emptyIfNull(@Nullable String str) {
        return str == null ? "" : str;
    }

    public static String firstNotEmpty(@Nullable String a, @NonNull String b) {
        return !isEmpty(a) ? a : Preconditions.checkStringNotEmpty(b);
    }

    public static int length(@Nullable String s) {
        return s != null ? s.length() : 0;
    }

    public static String safeIntern(String s) {
        return (s != null) ? s.intern() : null;
    }

    public static CharSequence replace(CharSequence template,
                                       String[] sources,
                                       CharSequence[] destinations) {
        SpannableStringBuilder tb = new SpannableStringBuilder(template);

        for (int i = 0; i < sources.length; i++) {
            int where = indexOf(tb, sources[i]);

            if (where >= 0)
                tb.setSpan(sources[i], where, where + sources[i].length(),
                           Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        for (int i = 0; i < sources.length; i++) {
            int start = tb.getSpanStart(sources[i]);
            int end = tb.getSpanEnd(sources[i]);

            if (start >= 0) {
                tb.replace(start, end, destinations[i]);
            }
        }

        return tb;
    }

    public static CharSequence expandTemplate(CharSequence template,
                                              CharSequence... values) {
        if (values.length > 9) {
            throw new IllegalArgumentException("max of 9 values are supported");
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(template);

        try {
            int i = 0;
            while (i < ssb.length()) {
                if (ssb.charAt(i) == '^') {
                    char next = ssb.charAt(i+1);
                    if (next == '^') {
                        ssb.delete(i+1, i+2);
                        ++i;
                        continue;
                    } else if (Character.isDigit(next)) {
                        int which = Character.getNumericValue(next) - 1;
                        if (which < 0) {
                            throw new IllegalArgumentException(
                                "template requests value ^" + (which+1));
                        }
                        if (which >= values.length) {
                            throw new IllegalArgumentException(
                                "template requests value ^" + (which+1) +
                                "; only " + values.length + " provided");
                        }
                        ssb.replace(i, i+2, values[which]);
                        i += values[which].length();
                        continue;
                    }
                }
                ++i;
            }
        } catch (IndexOutOfBoundsException ignore) {
            // happens when ^ is the last character in the string.
        }
        return ssb;
    }

    public static int getOffsetBefore(CharSequence text, int offset) {
        if (offset == 0)
            return 0;
        if (offset == 1)
            return 0;

        char c = text.charAt(offset - 1);

        if (c >= '\uDC00' && c <= '\uDFFF') {
            char c1 = text.charAt(offset - 2);

            if (c1 >= '\uD800' && c1 <= '\uDBFF')
                offset -= 2;
            else
                offset -= 1;
        } else {
            offset -= 1;
        }

        if (text instanceof Spanned) {
            ReplacementSpan[] spans = ((Spanned) text).getSpans(offset, offset,
                                                       ReplacementSpan.class);

            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);

                if (start < offset && end > offset)
                    offset = start;
            }
        }

        return offset;
    }

    public static int getOffsetAfter(CharSequence text, int offset) {
        int len = text.length();

        if (offset == len)
            return len;
        if (offset == len - 1)
            return len;

        char c = text.charAt(offset);

        if (c >= '\uD800' && c <= '\uDBFF') {
            char c1 = text.charAt(offset + 1);

            if (c1 >= '\uDC00' && c1 <= '\uDFFF')
                offset += 2;
            else
                offset += 1;
        } else {
            offset += 1;
        }

        if (text instanceof Spanned) {
            ReplacementSpan[] spans = ((Spanned) text).getSpans(offset, offset,
                                                       ReplacementSpan.class);

            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);

                if (start < offset && end > offset)
                    offset = end;
            }
        }

        return offset;
    }

    public static void copySpansFrom(Spanned source, int start, int end,
                                     Class kind,
                                     Spannable dest, int destoff) {
        if (kind == null) {
            kind = Object.class;
        }

        Object[] spans = source.getSpans(start, end, kind);

        for (int i = 0; i < spans.length; i++) {
            int st = source.getSpanStart(spans[i]);
            int en = source.getSpanEnd(spans[i]);
            int fl = source.getSpanFlags(spans[i]);

            if (st < start)
                st = start;
            if (en > end)
                en = end;

            dest.setSpan(spans[i], st - start + destoff, en - start + destoff,
                         fl);
        }
    }

    public static boolean isGraphic(CharSequence str) {
        final int len = str.length();
        for (int cp, i=0; i<len; i+=Character.charCount(cp)) {
            cp = Character.codePointAt(str, i);
            int gc = Character.getType(cp);
            if (gc != Character.CONTROL
                    && gc != Character.FORMAT
                    && gc != Character.SURROGATE
                    && gc != Character.UNASSIGNED
                    && gc != Character.LINE_SEPARATOR
                    && gc != Character.PARAGRAPH_SEPARATOR
                    && gc != Character.SPACE_SEPARATOR) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isGraphic(char c) {
        int gc = Character.getType(c);
        return     gc != Character.CONTROL
                && gc != Character.FORMAT
                && gc != Character.SURROGATE
                && gc != Character.UNASSIGNED
                && gc != Character.LINE_SEPARATOR
                && gc != Character.PARAGRAPH_SEPARATOR
                && gc != Character.SPACE_SEPARATOR;
    }

    public static boolean isPrintableAscii(final char c) {
        final int asciiFirst = 0x20;
        final int asciiLast = 0x7E;  // included
        return (asciiFirst <= c && c <= asciiLast) || c == '\r' || c == '\n';
    }

    public static boolean isPrintableAsciiOnly(final CharSequence str) {
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!isPrintableAscii(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean delimitedStringContains(
            String delimitedString, char delimiter, String item) {
        if (isEmpty(delimitedString) || isEmpty(item)) {
            return false;
        }
        int pos = -1;
        int length = delimitedString.length();
        while ((pos = delimitedString.indexOf(item, pos + 1)) != -1) {
            if (pos > 0 && delimitedString.charAt(pos - 1) != delimiter) {
                continue;
            }
            int expectedDelimiterPos = pos + item.length();
            if (expectedDelimiterPos == length) {
                // Match at end of string.
                return true;
            }
            if (delimitedString.charAt(expectedDelimiterPos) == delimiter) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeEmptySpans(T[] spans, Spanned spanned, Class<T> klass) {
        T[] copy = null;
        int count = 0;

        for (int i = 0; i < spans.length; i++) {
            final T span = spans[i];
            final int start = spanned.getSpanStart(span);
            final int end = spanned.getSpanEnd(span);

            if (start == end) {
                if (copy == null) {
                    copy = (T[]) Array.newInstance(klass, spans.length - 1);
                    System.arraycopy(spans, 0, copy, 0, i);
                    count = i;
                }
            } else {
                if (copy != null) {
                    copy[count] = span;
                    count++;
                }
            }
        }

        if (copy != null) {
            T[] result = (T[]) Array.newInstance(klass, count);
            System.arraycopy(copy, 0, result, 0, count);
            return result;
        } else {
            return spans;
        }
    }

    public static long packRangeInLong(int start, int end) {
        return (((long) start) << 32) | end;
    }

    public static int unpackRangeStartFromLong(long range) {
        return (int) (range >>> 32);
    }

    public static int unpackRangeEndFromLong(long range) {
        return (int) (range & 0x00000000FFFFFFFFL);
    }

    public static boolean hasStyleSpan(@NonNull Spanned spanned) {
        Preconditions.checkArgument(spanned != null);
        final Class<?>[] styleClasses = {
                CharacterStyle.class, ParagraphStyle.class, UpdateAppearance.class};
        for (Class<?> clazz : styleClasses) {
            if (spanned.nextSpanTransition(-1, spanned.length(), clazz) < spanned.length()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static CharSequence trimNoCopySpans(@Nullable CharSequence charSequence) {
        if (charSequence != null && charSequence instanceof Spanned) {
            // SpannableStringBuilder copy constructor trims NoCopySpans.
            return new SpannableStringBuilder(charSequence);
        }
        return charSequence;
    }

    public static void wrap(StringBuilder builder, String start, String end) {
        builder.insert(0, start);
        builder.append(end);
    }

    @Nullable
    public static <T extends CharSequence> T trimToParcelableSize(@Nullable T text) {
        return trimToSize(text, PARCEL_SAFE_TEXT_LENGTH);
    }

    @Nullable
    public static <T extends CharSequence> T trimToSize(@Nullable T text,
            @IntRange(from = 1) int size) {
        Preconditions.checkArgument(size > 0);
        if (TextUtils.isEmpty(text) || text.length() <= size) return text;
        if (Character.isHighSurrogate(text.charAt(size - 1))
                && Character.isLowSurrogate(text.charAt(size))) {
            size = size - 1;
        }
        return (T) text.subSequence(0, size);
    }

    @Nullable
    public static <T extends CharSequence> T trimToLengthWithEllipsis(@Nullable T text,
            @IntRange(from = 1) int size) {
        T trimmed = trimToSize(text, size);
        if (text != null && trimmed.length() < text.length()) {
            trimmed = (T) (trimmed.toString() + "...");
        }
        return trimmed;
    }

    public static boolean isNewline(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.PARAGRAPH_SEPARATOR || type == Character.LINE_SEPARATOR
                || codePoint == LINE_FEED_CODE_POINT;
    }

    public static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || codePoint == NBSP_CODE_POINT;
    }

    public static boolean isWhitespaceExceptNewline(int codePoint) {
        return isWhitespace(codePoint) && !isNewline(codePoint);
    }

    public static boolean isPunctuation(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.START_PUNCTUATION;
    }

    @Nullable
    public static String withoutPrefix(@Nullable String prefix, @Nullable String str) {
        if (prefix == null || str == null) return str;
        return str.startsWith(prefix) ? str.substring(prefix.length()) : str;
    }
}
