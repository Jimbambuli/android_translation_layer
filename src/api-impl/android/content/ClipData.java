package android.content;

import android.net.Uri;
import android.text.Html;
import java.util.ArrayList;

/**
 * A clip: a description (label and MIME types) and one or more items, each holding text, HTML
 * text, an Intent or a Uri.
 */
public class ClipData {

	public static class Item {
		private final CharSequence text;
		private final String htmlText;
		private final Intent intent;
		private final Uri uri;

		public Item(CharSequence text) {
			this(text, null, null, null);
		}

		public Item(CharSequence text, String htmlText) {
			this(text, htmlText, null, null);
		}

		public Item(Intent intent) {
			this(null, null, intent, null);
		}

		public Item(Uri uri) {
			this(null, null, null, uri);
		}

		public Item(CharSequence text, Intent intent, Uri uri) {
			this(text, null, intent, uri);
		}

		public Item(CharSequence text, String htmlText, Intent intent, Uri uri) {
			this.text = text;
			this.htmlText = htmlText;
			this.intent = intent;
			this.uri = uri;
		}

		public CharSequence getText() {
			return text;
		}

		public String getHtmlText() {
			return htmlText;
		}

		public Intent getIntent() {
			return intent;
		}

		public Uri getUri() {
			return uri;
		}

		/**
		 * Returns the text, else the HTML text without markup, else the Uri or Intent as text.
		 * Unlike AOSP, the content of a Uri is not read.
		 */
		public CharSequence coerceToText(Context context) {
			if (text != null)
				return text;
			if (htmlText != null)
				return Html.fromHtml(htmlText).toString();
			if (uri != null)
				return uri.toString();
			if (intent != null)
				return intent.toString();
			return "";
		}

		public CharSequence coerceToStyledText(Context context) {
			if (text != null)
				return text;
			if (htmlText != null)
				return Html.fromHtml(htmlText);
			return coerceToText(context);
		}

		public String coerceToHtmlText(Context context) {
			if (htmlText != null)
				return htmlText;
			return Html.escapeHtml(coerceToText(context));
		}
	}

	private final ClipDescription description;
	private final ArrayList<Item> items = new ArrayList<>();

	public ClipData(CharSequence label, String[] mimeTypes, Item item) {
		this(new ClipDescription(label, mimeTypes), item);
	}

	public ClipData(ClipDescription description, Item item) {
		if (item == null)
			throw new NullPointerException("item is null");
		this.description = description;
		items.add(item);
	}

	public ClipData(ClipData other) {
		description = other.description;
		items.addAll(other.items);
	}

	public static ClipData newPlainText(CharSequence label, CharSequence text) {
		return new ClipData(label, new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, new Item(text));
	}

	public static ClipData newHtmlText(CharSequence label, CharSequence text, String htmlText) {
		return new ClipData(label, new String[] {ClipDescription.MIMETYPE_TEXT_HTML}, new Item(text, htmlText));
	}

	public static ClipData newIntent(CharSequence label, Intent intent) {
		return new ClipData(label, new String[] {ClipDescription.MIMETYPE_TEXT_INTENT}, new Item(intent));
	}

	public static ClipData newRawUri(CharSequence label, Uri uri) {
		return new ClipData(label, new String[] {ClipDescription.MIMETYPE_TEXT_URILIST}, new Item(uri));
	}

	/* AOSP: the type of a content: Uri from the ContentResolver, else text/uri-list (ATL has no stream types) */
	public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri) {
		String type = ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) ? resolver.getType(uri) : null;
		if (type == null)
			return newRawUri(label, uri);
		return new ClipData(label, new String[] {type}, new Item(uri));
	}

	public ClipDescription getDescription() {
		return description;
	}

	public void addItem(Item item) {
		if (item == null)
			throw new NullPointerException("item is null");
		items.add(item);
	}

	public void addItem(ContentResolver resolver, Item item) {
		addItem(item);
	}

	public Item getItemAt(int index) {
		return items.get(index);
	}

	public int getItemCount() {
		return items.size();
	}
}
