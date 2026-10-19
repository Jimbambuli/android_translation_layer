package android.graphics.fonts;

import java.util.ArrayList;

public final class FontFamily {
	private final ArrayList<Font> fonts;

	private FontFamily(ArrayList<Font> fonts) {
		this.fonts = fonts;
	}

	public Font getFont(int index) {
		return fonts.get(index);
	}

	public int getSize() {
		return fonts.size();
	}

	public static final class Builder {
		private final ArrayList<Font> fonts = new ArrayList<>();

		public Builder(Font font) {
			fonts.add(font);
		}

		public Builder addFont(Font font) {
			for (Font f : fonts) {
				if (f.getStyle().getWeight() == font.getStyle().getWeight() && f.getStyle().getSlant() == font.getStyle().getSlant())
					throw new IllegalArgumentException("Font style " + font.getStyle().getWeight() + "/" + font.getStyle().getSlant() + " is already registered");
			}
			fonts.add(font);
			return this;
		}

		public FontFamily build() {
			return new FontFamily(new ArrayList<>(fonts));
		}
	}
}
