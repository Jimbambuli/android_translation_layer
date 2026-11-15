package android.graphics.fonts;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/* The font file is handed to fontconfig (see Typeface), which Pango then draws with; the Font
 * keeps its family name and the attributes it was built with. */
public final class Font {
	private final FontStyle style;
	private final int ttcIndex;
	private final FontVariationAxis[] axes;
	private final String familyName;
	private final File file;

	private Font(FontStyle style, int ttcIndex, FontVariationAxis[] axes, String familyName, File file) {
		this.file = file;
		this.style = style;
		this.ttcIndex = ttcIndex;
		this.axes = axes;
		this.familyName = familyName;
	}

	/** @hide */
	public String getFamilyName() {
		return familyName;
	}

	/* AOSP Font.getFile(): the file the font was read from, null when it was built from a buffer in AOSP; here
	 * buffers and streams are copied to a file (see Builder.copy), so it is set for those as well */
	public File getFile() {
		return file;
	}

	public FontStyle getStyle() {
		return style;
	}

	public int getTtcIndex() {
		return ttcIndex;
	}

	public FontVariationAxis[] getAxes() {
		return axes;
	}

	public LocaleList getLocaleList() {
		return LocaleList.getEmptyLocaleList();
	}

	public static final class Builder {
		private int weight = -1; // from the font file unless set
		private int slant = -1;
		private int ttcIndex = 0;
		private FontVariationAxis[] axes = null;
		private File file;
		private String familyName;
		private IOException error;

		public Builder(ByteBuffer buffer) {
			byte[] bytes = new byte[buffer.remaining()];
			buffer.duplicate().get(bytes);
			copy(new java.io.ByteArrayInputStream(bytes));
		}
		public Builder(File file) {
			this.file = file;
		}
		public Builder(ParcelFileDescriptor fd) {
			copy(new java.io.FileInputStream(fd.getFileDescriptor()));
		}
		public Builder(ParcelFileDescriptor fd, long offset, long size) {
			try {
				java.io.FileInputStream in = new java.io.FileInputStream(fd.getFileDescriptor());
				in.getChannel().position(offset);
				byte[] bytes = new byte[(int)size];
				new java.io.DataInputStream(in).readFully(bytes);
				copy(new java.io.ByteArrayInputStream(bytes));
			} catch (IOException e) {
				error = e;
			}
		}
		public Builder(AssetManager am, String path) {
			try {
				copy(am.open(path));
			} catch (IOException e) {
				error = e;
			}
		}
		public Builder(Resources res, int resId) {
			copy(res.openRawResource(resId));
		}
		public Builder(Font font) {
			weight = font.style.getWeight();
			slant = font.style.getSlant();
			ttcIndex = font.ttcIndex;
			axes = font.axes;
			familyName = font.familyName;
		}

		private void copy(java.io.InputStream in) {
			try {
				file = android.graphics.Typeface.copyToFontFile(in);
			} catch (IOException e) {
				error = e;
			}
		}

		public Builder setWeight(int weight) {
			this.weight = weight;
			return this;
		}

		public Builder setSlant(int slant) {
			this.slant = slant;
			return this;
		}

		public Builder setTtcIndex(int ttcIndex) {
			this.ttcIndex = ttcIndex;
			return this;
		}

		public Builder setFontVariationSettings(String variationSettings) {
			axes = FontVariationAxis.fromFontVariationSettings(variationSettings);
			return this;
		}

		public Builder setFontVariationSettings(FontVariationAxis[] axes) {
			this.axes = axes;
			return this;
		}

		public Font build() throws IOException {
			if (error != null)
				throw error;
			int[] style = {FontStyle.FONT_WEIGHT_NORMAL, 0};
			if (file != null) {
				familyName = android.graphics.Typeface.registerFontFamily(file, style);
				if (familyName == null)
					throw new IOException("Failed to read font " + file);
			}
			int w = weight != -1 ? weight : style[0];
			int s = slant != -1 ? slant : (style[1] != 0 ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
			return new Font(new FontStyle(w, s), ttcIndex, axes, familyName, file);
		}
	}
}
