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

	/* Paint applies family, weight and slant to its Pango font. The family is only known for
	 * fonts of the app (see registerFont()); otherwise Pango picks its default font. */
	String family;
	private int weight = 400;
	private boolean italic;

	private static native String native_register_font(String path, int[] style);

	/* makes a font file known to Pango; returns null when it cannot be read as a font */
	static Typeface registerFont(File file, int[] style) {
		String family = native_register_font(file.getPath(), style);
		if (family == null)
			return null;
		Typeface ret = new Typeface();
		ret.family = family;
		ret.weight = style[0];
		ret.italic = style[1] != 0;
		return ret;
	}

	/* fonts in the APK or a buffer need a file for fontconfig; it is named after its content,
	 * so the same font is written only once */
	/** @hide for android.graphics.fonts.Font */
	public static File copyToFontFile(java.io.InputStream in) throws java.io.IOException {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		try {
			byte[] buf = new byte[65536];
			int n;
			while ((n = in.read(buf)) > 0)
				bytes.write(buf, 0, n);
		} finally {
			in.close();
		}
		byte[] data = bytes.toByteArray();
		String name;
		try {
			StringBuilder sb = new StringBuilder();
			for (byte b : java.security.MessageDigest.getInstance("SHA-1").digest(data))
				sb.append(String.format("%02x", b));
			name = sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new java.io.IOException(e);
		}
		File dir = new File(System.getProperty("java.io.tmpdir"), "atl-fonts");
		dir.mkdirs();
		File file = new File(dir, name + ".ttf");
		if (!file.exists()) {
			File tmp = File.createTempFile(name, ".tmp", dir);
			try (java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
				out.write(data);
			}
			tmp.renameTo(file);
		}
		return file;
	}

	/** @hide for android.graphics.fonts.Font */
	public static String registerFontFamily(File file, int[] style) {
		return native_register_font(file.getPath(), style);
	}

	private static Typeface fromFile(File file) {
		Typeface ret = registerFont(file, new int[2]);
		return ret != null ? ret : DEFAULT;
	}

	public static Typeface createFromAsset(AssetManager mgr, String path) {
		try {
			return fromFile(copyToFontFile(mgr.open(path)));
		} catch (java.io.IOException e) {
			throw new RuntimeException("Font asset not found " + path);
		}
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
		Typeface ret = create((String)null, style);
		if (typeface != null)
			ret.family = typeface.family;
		return ret;
	}

	public static Typeface createFromFile(String path) {
		return createFromFile(new File(path));
	}

	public static Typeface createFromFile(File file) {
		if (!file.exists())
			throw new RuntimeException("Font not found " + file);
		return fromFile(file);
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
		ret.family = family != null ? family.family : null;
		ret.weight = weight;
		ret.italic = italic;
		return ret;
	}

	/* the typeface uses the family of the first font; Pango does the fallback for missing glyphs */
	public static final class CustomFallbackBuilder {
		private android.graphics.fonts.FontStyle style;
		private final android.graphics.fonts.FontFamily fontFamily;

		public CustomFallbackBuilder(android.graphics.fonts.FontFamily family) {
			fontFamily = family;
		}

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
			android.graphics.fonts.FontStyle s = style;
			if (s == null)
				s = fontFamily.getFont(0).getStyle();
			Typeface ret = create(DEFAULT, s.getWeight(), s.getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC);
			ret.family = fontFamily.getFont(0).getFamilyName();
			return ret;
		}
	}

	public static class Builder {
		private AssetManager mgr;
		private String path;

		public Builder(AssetManager mgr, String path) {
			this.mgr = mgr;
			this.path = path;
		}

		public Builder setFontVariationSettings(String settings) {
			return this;
		}

		public Typeface build() {
			try {
				Typeface ret = registerFont(copyToFontFile(mgr.open(path)), new int[2]);
				return ret; // null when the font cannot be read, as in AOSP
			} catch (java.io.IOException e) {
				return null;
			}
		}
	}
}
