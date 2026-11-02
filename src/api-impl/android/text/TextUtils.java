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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.icu.util.ULocale;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
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
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import com.android.internal.util.ArrayUtils;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextUtils {
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

	public static int getCapsMode(CharSequence cs, int off, int reqModes) {
		return 0;
	}

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
}
