package android.graphics.fonts;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/* ATL draws text with Pango and does not load app fonts (see Typeface), so a Font only
 * keeps the attributes it was built with. */
public final class Font {
	private final FontStyle style;
	private final int ttcIndex;
	private final FontVariationAxis[] axes;

	private Font(FontStyle style, int ttcIndex, FontVariationAxis[] axes) {
		this.style = style;
		this.ttcIndex = ttcIndex;
		this.axes = axes;
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
		private int weight = FontStyle.FONT_WEIGHT_NORMAL;
		private int slant = FontStyle.FONT_SLANT_UPRIGHT;
		private int ttcIndex = 0;
		private FontVariationAxis[] axes = null;

		public Builder(ByteBuffer buffer) {}
		public Builder(File file) {}
		public Builder(ParcelFileDescriptor fd) {}
		public Builder(ParcelFileDescriptor fd, long offset, long size) {}
		public Builder(AssetManager am, String path) {}
		public Builder(Resources res, int resId) {}
		public Builder(Font font) {
			weight = font.style.getWeight();
			slant = font.style.getSlant();
			ttcIndex = font.ttcIndex;
			axes = font.axes;
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
			return new Font(new FontStyle(weight, slant), ttcIndex, axes);
		}
	}
}
