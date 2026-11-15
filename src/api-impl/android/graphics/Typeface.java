package android.graphics;

import android.content.res.AssetManager;
import java.io.File;

public class Typeface {

	/* like AOSP's typeface caches (sStyledTypefaceCache, sWeightTypefaceCache): one instance per family,
	 * base weight and style instead of one per TextView (declared first, the constants below already
	 * use it). As in minikin's Typeface, baseWeight is the weight the relative styles of create(Typeface,
	 * int) start from, weight the one the text is drawn with and style the one getStyle() reports. */
	private static final java.util.HashMap<String, Typeface> cache = new java.util.HashMap<>();

	private static Typeface cached(String family, int baseWeight, int style, int weight, boolean italic) {
		String key = family + "/" + baseWeight + "/" + style + "/" + weight + "/" + italic;
		synchronized (cache) {
			Typeface ret = cache.get(key);
			if (ret == null) {
				ret = new Typeface();
				ret.family = family;
				ret.baseWeight = baseWeight;
				ret.style = style;
				ret.weight = weight;
				ret.italic = italic;
				cache.put(key, ret);
			}
			return ret;
		}
	}

	/* minikin Typeface::computeAPIStyle() */
	private static int computeAPIStyle(int weight, boolean italic) {
		return (weight >= 600 ? BOLD : NORMAL) | (italic ? ITALIC : NORMAL);
	}

	/* minikin Typeface::createAbsolute(), for nativeCreateFromTypefaceWithExactStyle() */
	private static Typeface createAbsolute(String family, int weight, boolean italic) {
		weight = Math.max(1, Math.min(1000, weight));
		return cached(family, weight, computeAPIStyle(weight, italic), weight, italic);
	}

	private static final String DEFAULT_FAMILY = "sans-serif";

	/* The families and aliases of frameworks/base/data/fonts/fonts.xml (android-14) without the
	 * fallback fonts for the scripts. ATL draws with Pango: a family is drawn with the font
	 * fontconfig chooses for the names given here (null: the default font, which is Roboto as on
	 * Android, see add_android_fonts() in main.c, as is "Roboto Condensed", Roboto at wdth 75).
	 * For the fonts ATL does not bring along, the AOSP font comes first if it is installed. */
	private static final String[][] SYSTEM_FAMILIES = {
		// name, Pango family
		{"sans-serif", null},
		{"sans-serif-condensed", "Roboto Condensed"},
		{"serif", "Noto Serif,serif"},
		{"monospace", "Droid Sans Mono,Noto Sans Mono,Noto Mono,monospace"},
		{"serif-monospace", "monospace"},
		{"casual", "cursive"},
		{"cursive", "cursive"},
		{"sans-serif-smallcaps", null},
		{"source-sans-pro", "Source Sans Pro"},
	};
	private static final Object[][] SYSTEM_ALIASES = {
		// name, original, weight
		{"sans-serif-thin", "sans-serif", 100},
		{"sans-serif-light", "sans-serif", 300},
		{"sans-serif-medium", "sans-serif", 500},
		{"sans-serif-black", "sans-serif", 900},
		{"arial", "sans-serif", 400},
		{"helvetica", "sans-serif", 400},
		{"tahoma", "sans-serif", 400},
		{"verdana", "sans-serif", 400},
		{"sans-serif-condensed-light", "sans-serif-condensed", 300},
		{"sans-serif-condensed-medium", "sans-serif-condensed", 500},
		{"serif-bold", "serif", 700},
		{"times", "serif", 400},
		{"times new roman", "serif", 400},
		{"palatino", "serif", 400},
		{"georgia", "serif", 400},
		{"baskerville", "serif", 400},
		{"goudy", "serif", 400},
		{"fantasy", "serif", 400},
		{"ITC Stone Serif", "serif", 400},
		{"sans-serif-monospace", "monospace", 400},
		{"monaco", "monospace", 400},
		{"courier", "serif-monospace", 400},
		{"courier new", "serif-monospace", 400},
		{"source-sans-pro-semi-bold", "source-sans-pro", 600},
	};

	private static final java.util.Map<String, Typeface> sSystemFontMap = new java.util.HashMap<>();
	private static final Typeface sDefaultTypeface;

	/* Typeface.initSystemDefaultTypefaces() with the table above */
	static {
		for (String[] family : SYSTEM_FAMILIES) {
			sSystemFontMap.put(family[0], cached(family[1], 400, Typeface.NORMAL, 400, false));
		}

		for (Object[] alias : SYSTEM_ALIASES) {
			final String name = (String)alias[0];
			if (sSystemFontMap.containsKey(name)) {
				continue; // If alias and named family are conflict, use named family.
			}
			final Typeface base = sSystemFontMap.get((String)alias[1]);
			if (base == null) {
				continue;
			}
			final int weight = (Integer)alias[2];
			// nativeCreateWeightAlias(): minikin Typeface::createWithDifferentBaseWeight()
			final Typeface newFace = weight == 400 ? base : cached(base.family, weight, computeAPIStyle(weight, base.italic), weight, base.italic);
			sSystemFontMap.put(name, newFace);
		}

		sDefaultTypeface = sSystemFontMap.get(DEFAULT_FAMILY);
	}

	private static Typeface getSystemDefaultTypeface(String familyName) {
		Typeface tf = familyName != null ? sSystemFontMap.get(familyName) : null;
		return tf == null ? sDefaultTypeface : tf;
	}

	/**
	 * The default NORMAL typeface object
	 */
	public static final Typeface DEFAULT = create(sDefaultTypeface, 0);
	/**
	 * The default BOLD typeface object. Note: this may be not actually be
	 * bold, depending on what fonts are installed. Call getStyle() to know
	 * for sure.
	 */
	public static final Typeface DEFAULT_BOLD = create(sDefaultTypeface, Typeface.BOLD);
	/**
	 * The NORMAL style of the default sans serif typeface.
	 */
	public static final Typeface SANS_SERIF = create("sans-serif", 0);
	/**
	 * The NORMAL style of the default serif typeface.
	 */
	public static final Typeface SERIF = create("serif", 0);
	/**
	 * The NORMAL style of the default monospace typeface.
	 */
	public static final Typeface MONOSPACE = create("monospace", 0);

	// Style
	/* AOSP Typeface.RESOLVE_BY_FONT_TABLE: weight or italic as the font file says */
	public static final int RESOLVE_BY_FONT_TABLE = -1;

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
	private int baseWeight = 400;
	private int style = NORMAL;

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
		ret.baseWeight = ret.weight;
		ret.style = computeAPIStyle(ret.weight, ret.italic);
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

	public static Typeface create(String familyName, int style) {
		return create(getSystemDefaultTypeface(familyName), style);
	}

	public static Typeface create(Typeface family, int style) {
		if ((style & ~BOLD_ITALIC) != 0) {
			style = NORMAL;
		}
		if (family == null) {
			family = sDefaultTypeface;
		}

		// Return early if we're asked for the same face/style
		if (family.style == style) {
			return family;
		}

		// nativeCreateFromTypeface(): minikin Typeface::createRelative() with computeRelativeStyle()
		int weight = family.baseWeight;
		if ((style & BOLD) != 0) {
			weight += 300;
		}
		weight = Math.max(1, Math.min(1000, weight));
		return cached(family.family, family.baseWeight, style, weight, (style & ITALIC) != 0);
	}

	public static Typeface create(Typeface family, int weight, boolean italic) {
		if (family == null) {
			family = sDefaultTypeface;
		}
		return createAbsolute(family.family, weight, italic);
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
		return style;
	}

	public final boolean isBold() {
		return (getStyle() & BOLD) != 0;
	}

	public final boolean isItalic() {
		return italic;
	}

	public static Typeface defaultFromStyle(int style) {
		return create(sDefaultTypeface, style);
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

	/* the typeface uses the family of the first font; Pango does the fallback for missing glyphs */
	/* AOSP Typeface.createFromResources() (android-14) for Resources.getFont(): a file based font-family
	 * becomes a FontFamily of its fonts with the best match for normal style; for a provider based one AOSP
	 * asks FontsContract and returns DEFAULT when the font is not there, ATL has no font provider, so
	 * it is the system family named by the entry if there is one, else DEFAULT */
	private static final java.util.HashMap<String, Typeface> sDynamicTypefaceCache = new java.util.HashMap<>();

	/** @hide */
	public static Typeface createFromResources(android.content.res.FontResourcesParser.FamilyResourceEntry entry,
	                                          AssetManager mgr, String path) {
		if (entry instanceof android.content.res.FontResourcesParser.ProviderResourceEntry) {
			final android.content.res.FontResourcesParser.ProviderResourceEntry providerEntry =
			    (android.content.res.FontResourcesParser.ProviderResourceEntry)entry;
			String systemFontFamilyName = providerEntry.getSystemFontFamilyName();
			if (systemFontFamilyName != null && sSystemFontMap.containsKey(systemFontFamilyName)) {
				return Typeface.create(systemFontFamilyName, NORMAL);
			}
			return DEFAULT;
		}

		synchronized (sDynamicTypefaceCache) {
			Typeface typeface = sDynamicTypefaceCache.get(path);
			if (typeface != null)
				return typeface;
		}

		final android.content.res.FontResourcesParser.FontFamilyFilesResourceEntry filesEntry =
		    (android.content.res.FontResourcesParser.FontFamilyFilesResourceEntry)entry;
		Typeface typeface;
		try {
			android.graphics.fonts.FontFamily.Builder familyBuilder = null;
			for (final android.content.res.FontResourcesParser.FontFileResourceEntry fontFile : filesEntry.getEntries()) {
				final android.graphics.fonts.Font.Builder fontBuilder = new android.graphics.fonts.Font.Builder(
				    copyToFontFile(mgr.openNonAsset(fontFile.getFileName())))
				        .setTtcIndex(fontFile.getTtcIndex())
				        .setFontVariationSettings(fontFile.getVariationSettings());
				if (fontFile.getWeight() != RESOLVE_BY_FONT_TABLE) {
					fontBuilder.setWeight(fontFile.getWeight());
				}
				if (fontFile.getItalic() != RESOLVE_BY_FONT_TABLE) {
					fontBuilder.setSlant(fontFile.getItalic() == android.content.res.FontResourcesParser.FontFileResourceEntry.ITALIC
					                         ? android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC
					                         : android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT);
				}

				if (familyBuilder == null) {
					familyBuilder = new android.graphics.fonts.FontFamily.Builder(fontBuilder.build());
				} else {
					familyBuilder.addFont(fontBuilder.build());
				}
			}
			if (familyBuilder == null) {
				return Typeface.DEFAULT;
			}
			final android.graphics.fonts.FontFamily family = familyBuilder.build();
			// AOSP FontStyle.getMatchScore() against normal weight and upright
			android.graphics.fonts.Font bestFont = family.getFont(0);
			int bestScore = matchScoreToNormal(bestFont.getStyle());
			for (int i = 1; i < family.getSize(); ++i) {
				final android.graphics.fonts.Font candidate = family.getFont(i);
				final int score = matchScoreToNormal(candidate.getStyle());
				if (score < bestScore) {
					bestFont = candidate;
					bestScore = score;
				}
			}
			typeface = new Typeface.CustomFallbackBuilder(family)
			               .setStyle(bestFont.getStyle())
			               .build();
		} catch (IllegalArgumentException e) {
			// To be a compatible behavior with API28 or before, catch IllegalArgumentExcetpion
			// thrown by native code and returns null.
			return null;
		} catch (java.io.IOException e) {
			typeface = Typeface.DEFAULT;
		}
		synchronized (sDynamicTypefaceCache) {
			sDynamicTypefaceCache.put(path, typeface);
		}
		return typeface;
	}

	/* AOSP FontStyle.getMatchScore(): normal.getMatchScore(style) */
	private static int matchScoreToNormal(android.graphics.fonts.FontStyle style) {
		return Math.abs(android.graphics.fonts.FontStyle.FONT_WEIGHT_NORMAL - style.getWeight()) / 100
		    + (style.getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT ? 0 : 2);
	}

	/** @hide for Resources.getFont(): a font file of the resources */
	public static Typeface createFromResourceFile(AssetManager mgr, String path) throws java.io.IOException {
		synchronized (sDynamicTypefaceCache) {
			Typeface typeface = sDynamicTypefaceCache.get(path);
			if (typeface != null)
				return typeface;
		}
		Typeface typeface = registerFont(copyToFontFile(mgr.openNonAsset(path)), new int[2]);
		if (typeface != null) {
			synchronized (sDynamicTypefaceCache) {
				sDynamicTypefaceCache.put(path, typeface);
			}
		}
		return typeface;
	}

	/* AOSP Typeface.CustomFallbackBuilder. minikin makes a font collection of the families, then the
	 * system fallback; here it is the Pango family list "first,second,...,system" (Pango tries the
	 * families in order for each character, as the collection does) */
	public static final class CustomFallbackBuilder {
		private static final int MAX_CUSTOM_FALLBACK = 64;
		private final java.util.ArrayList<android.graphics.fonts.FontFamily> mFamilies = new java.util.ArrayList<>();
		private String mFallbackName = null;
		private android.graphics.fonts.FontStyle mStyle;

		public static int getMaxCustomFallbackCount() {
			return MAX_CUSTOM_FALLBACK;
		}

		public CustomFallbackBuilder(android.graphics.fonts.FontFamily family) {
			java.util.Objects.requireNonNull(family);
			mFamilies.add(family);
		}

		public CustomFallbackBuilder setSystemFallback(String familyName) {
			java.util.Objects.requireNonNull(familyName);
			mFallbackName = familyName;
			return this;
		}

		public CustomFallbackBuilder setStyle(android.graphics.fonts.FontStyle style) {
			mStyle = style;
			return this;
		}

		public CustomFallbackBuilder addCustomFallback(android.graphics.fonts.FontFamily family) {
			java.util.Objects.requireNonNull(family);
			if (mFamilies.size() >= getMaxCustomFallbackCount())
				throw new IllegalArgumentException(String.format("Custom fallback limit exceeded(%d)", getMaxCustomFallbackCount()));
			mFamilies.add(family);
			return this;
		}

		public Typeface build() {
			final Typeface fallbackTypeface = getSystemDefaultTypeface(mFallbackName);
			java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
			for (android.graphics.fonts.FontFamily family : mFamilies) {
				for (int i = 0; i < family.getSize(); i++) {
					String name = family.getFont(i).getFamilyName();
					if (name != null)
						names.add(name);
				}
			}
			if (fallbackTypeface.family != null)
				names.add(fallbackTypeface.family);
			final int weight = mStyle == null ? 400 : mStyle.getWeight();
			final boolean italic = mStyle != null && mStyle.getSlant() != android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT;
			return createAbsolute(names.isEmpty() ? null : String.join(",", names), weight, italic);
		}
	}

	public static class Builder {
		private AssetManager mgr;
		private String path;
		private File mFile;
		private java.io.FileDescriptor mFd;

		/** Constructs a builder from a file path (the path of a font file, not of an asset). */
		public Builder(String path) {
			this(new File(path));
		}

		public Builder(File path) {
			mFile = path;
		}

		/** Constructs a builder from a file descriptor, which is read from its start. */
		public Builder(java.io.FileDescriptor fd) {
			mFd = fd;
		}

		public Builder(AssetManager mgr, String path) {
			this.mgr = mgr;
			this.path = path;
		}

		/* AOSP Typeface.Builder keeps the axes (mAxes) for the font it builds; ATL's Typeface is a Pango
		 * family, weight and slant without variation axes, so they are parsed and kept, not applied */
		public Builder setFontVariationSettings(String variationSettings) {
			mAxes = android.graphics.fonts.FontVariationAxis.fromFontVariationSettings(variationSettings);
			return this;
		}

		public Builder setFontVariationSettings(android.graphics.fonts.FontVariationAxis[] axes) {
			mAxes = axes == null ? null : axes.clone();
			return this;
		}
		private android.graphics.fonts.FontVariationAxis[] mAxes;

		/* as in AOSP: -1 and null leave weight and slant to what the font file says */
		private int mWeight = -1;
		private Boolean mItalic;

		/**
		 * Sets weight of the font.
		 * @param weight a weight value, from 1 to 1000
		 * @throws IllegalArgumentException if the weight is outside of [1, 1000]
		 */
		public Builder setWeight(int weight) {
			if (weight < 1 || weight > 1000)
				throw new IllegalArgumentException("weight value must be [1, 1000]");
			mWeight = weight;
			return this;
		}

		/** Sets italic information of the font. */
		public Builder setItalic(boolean italic) {
			mItalic = italic;
			return this;
		}

		public Typeface build() {
			try {
				File font;
				if (mFile != null) {
					font = mFile;
				} else if (mFd != null) {
					try (java.io.FileInputStream in = new java.io.FileInputStream(mFd)) {
						font = copyToFontFile(in);
					}
				} else {
					font = copyToFontFile(mgr.open(path));
				}
				Typeface ret = registerFont(font, new int[2]);
				if (ret != null && (mWeight != -1 || mItalic != null))
					ret = createAbsolute(ret.family, mWeight != -1 ? mWeight : ret.weight, mItalic != null ? mItalic : ret.italic);
				return ret; // null when the font cannot be read, as in AOSP
			} catch (java.io.IOException e) {
				return null;
			}
		}
	}
}
