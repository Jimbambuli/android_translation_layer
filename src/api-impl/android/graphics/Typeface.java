package android.graphics;

import android.content.res.AssetManager;
import java.io.File;

public class Typeface {

	/**
	 * The default NORMAL typeface object
	 */
	public static final Typeface DEFAULT = create((String)null, 0);
	/**
	 * The default BOLD typeface object. Note: this may be not actually be
	 * bold, depending on what fonts are installed. Call getStyle() to know
	 * for sure.
	 */
	public static final Typeface DEFAULT_BOLD = create((String)null, 1);
	/**
	 * The NORMAL style of the default sans serif typeface.
	 */
	public static final Typeface SANS_SERIF = create((String)null, 0);
	/**
	 * The NORMAL style of the default serif typeface.
	 */
	public static final Typeface SERIF = create((String)null, 0);
	/**
	 * The NORMAL style of the default monospace typeface.
	 */
	public static final Typeface MONOSPACE = create((String)null, 0);

	// Style
	public static final int NORMAL = 0;
	public static final int BOLD = 1;
	public static final int ITALIC = 2;
	public static final int BOLD_ITALIC = 3;

	public long native_instance = 0; // directly accessed by androidx

	/* the family is not kept (Pango picks the font), only weight and slant, which Paint applies */
	private int weight = 400;
	private boolean italic;

	public static Typeface createFromAsset(AssetManager mgr, String path) {
		return DEFAULT;
	}

	public static Typeface create(String family_name, int style) {
		Typeface ret = new Typeface();
		ret.weight = (style & BOLD) != 0 ? 700 : 400;
		ret.italic = (style & ITALIC) != 0;
		return ret;
	}

	public static Typeface create(Typeface typeface, int style) {
		if (typeface != null && typeface.getStyle() == style)
			return typeface;
		return create((String)null, style);
	}

	public static Typeface createFromFile(String path) {
		return DEFAULT;
	}

	public static Typeface createFromFile(File file) {
		return DEFAULT;
	}

	public int getStyle() {
		return (weight >= 600 ? BOLD : 0) | (italic ? ITALIC : 0);
	}

	public final boolean isBold() {
		return (getStyle() & BOLD) != 0;
	}

	public final boolean isItalic() {
		return italic;
	}

	public static Typeface defaultFromStyle(int style) {
		return create((String)null, style);
	}

	public static Typeface createFromFamiliesWithDefault(FontFamily[] families) {
		return DEFAULT;
	}

	public static Typeface createFromFamiliesWithDefault(FontFamily[] families, int dummy1, int dummy2) {
		return createFromFamiliesWithDefault(families);
	}

	public int getWeight() {
		return weight;
	}

	public static Typeface create(Typeface family, int weight, boolean italic) {
		Typeface ret = new Typeface();
		ret.weight = weight;
		ret.italic = italic;
		return ret;
	}

	/* app fonts are not loaded (see Builder), so this gives the default typeface in the requested style */
	public static final class CustomFallbackBuilder {
		private android.graphics.fonts.FontStyle style;

		public CustomFallbackBuilder(android.graphics.fonts.FontFamily family) {}

		public CustomFallbackBuilder setSystemFallback(String familyName) {
			return this;
		}

		public CustomFallbackBuilder setStyle(android.graphics.fonts.FontStyle style) {
			this.style = style;
			return this;
		}

		public CustomFallbackBuilder addCustomFallback(android.graphics.fonts.FontFamily family) {
			return this;
		}

		public Typeface build() {
			if (style == null)
				return DEFAULT;
			return create(DEFAULT, style.getWeight(), style.getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC);
		}
	}

	public static class Builder {
		public Builder(AssetManager mgr, String path) {}

		public Builder setFontVariationSettings(String settings) {
			return this;
		}

		public Typeface build() {
			return DEFAULT;
		}
	}
}
