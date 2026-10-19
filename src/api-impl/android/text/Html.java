package android.text;

import android.graphics.drawable.Drawable;
import org.xml.sax.XMLReader;

public class Html {

	public static interface ImageGetter {
		public Drawable getDrawable(String source);
	}

	public static Spanned fromHtml(String source) {
		return Html.fromHtml(source, 0);
	}

	/* Minimal HTML-to-Spanned conversion (AOSP uses TagSoup, which we don't have): tags are stripped,
	 * block elements become line breaks, b/strong and i/em become StyleSpans, a href becomes a URLSpan,
	 * common entities are decoded. */
	public static Spanned fromHtml(String source, int flags) {
		SpannableStringBuilder out = new SpannableStringBuilder();
		int boldStart = -1, italicStart = -1, linkStart = -1;
		String href = null;
		int i = 0, n = source.length();
		boolean pendingSpace = false;
		while (i < n) {
			char c = source.charAt(i);
			if (c == '<') {
				int end = source.indexOf('>', i);
				if (end < 0)
					break;
				String tag = source.substring(i + 1, end).trim();
				i = end + 1;
				if (tag.startsWith("!--")) {
					int ce = source.indexOf("-->", i);
					i = ce < 0 ? n : ce + 3;
					continue;
				}
				boolean closing = tag.startsWith("/");
				String name = (closing ? tag.substring(1) : tag).split("[\\s/]")[0].toLowerCase();
				if (name.equals("br")) {
					out.append('\n');
					pendingSpace = false;
				} else if (name.equals("p") || name.equals("div") || name.equals("li") || name.equals("tr") || name.equals("header") ||
				           name.equals("section") || name.equals("article") || name.equals("blockquote") ||
				           (name.length() == 2 && name.charAt(0) == 'h' && Character.isDigit(name.charAt(1)))) {
					if (out.length() > 0 && out.charAt(out.length() - 1) != '\n')
						out.append('\n');
					if (!closing && name.equals("li"))
						out.append("• ");
					pendingSpace = false;
				} else if (name.equals("b") || name.equals("strong")) {
					if (!closing) {
						boldStart = out.length();
					} else if (boldStart >= 0 && boldStart < out.length()) {
						out.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), boldStart, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						boldStart = -1;
					}
				} else if (name.equals("a")) {
					if (!closing) {
						// the space before the link belongs to the text before it
						if (pendingSpace && out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
							out.append(' ');
							pendingSpace = false;
						}
						java.util.regex.Matcher m = java.util.regex.Pattern.compile("href\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(tag);
						href = m.find() ? (m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3)) : null;
						linkStart = out.length();
					} else if (linkStart >= 0 && linkStart < out.length() && href != null) {
						out.setSpan(new android.text.style.URLSpan(href), linkStart, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						linkStart = -1;
					}
				} else if (name.equals("i") || name.equals("em")) {
					if (!closing) {
						italicStart = out.length();
					} else if (italicStart >= 0 && italicStart < out.length()) {
						out.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), italicStart, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						italicStart = -1;
					}
				}
				continue;
			}
			if (c == '&') {
				int end = source.indexOf(';', i);
				if (end > i && end - i <= 8) {
					String rep = decodeEntity(source.substring(i + 1, end));
					if (rep != null) {
						if (pendingSpace && out.length() > 0)
							out.append(' ');
						pendingSpace = false;
						out.append(rep);
						i = end + 1;
						continue;
					}
				}
			}
			if (Character.isWhitespace(c)) {
				pendingSpace = true;
			} else {
				if (pendingSpace && out.length() > 0 && out.charAt(out.length() - 1) != '\n')
					out.append(' ');
				pendingSpace = false;
				out.append(c);
			}
			i++;
		}
		while (out.length() > 0 && out.charAt(out.length() - 1) == '\n')
			out.delete(out.length() - 1, out.length());
		return out;
	}

	private static String decodeEntity(String entity) {
		switch (entity) {
			case "amp":
				return "&";
			case "lt":
				return "<";
			case "gt":
				return ">";
			case "quot":
				return "\"";
			case "apos":
				return "'";
			case "nbsp":
				return "\u00a0";
		}
		try {
			if (entity.startsWith("#x") || entity.startsWith("#X"))
				return new String(Character.toChars(Integer.parseInt(entity.substring(2), 16)));
			else if (entity.startsWith("#"))
				return new String(Character.toChars(Integer.parseInt(entity.substring(1))));
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	public static Spanned fromHtml(String source, ImageGetter imageGetter, TagHandler tagHandler) {
		return fromHtml(source, 0);
	}

	public static Spanned fromHtml(String source, int flags, ImageGetter imageGetter, TagHandler tagHandler) {
		return fromHtml(source, flags);
	}

	public static String toHtml(Spanned text) {
		return text.toString();
	}

	public static String escapeHtml(CharSequence source) {
		StringBuilder out = new StringBuilder(source.length());
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'' || c > 0x7F) {
				out.append("&#");
				out.append((int)c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	public static interface TagHandler {
		/**
		 * This method will be called whenn the HTML parser encounters
		 * a tag that it does not know how to interpret.
		 */
		public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader);
	}
}
