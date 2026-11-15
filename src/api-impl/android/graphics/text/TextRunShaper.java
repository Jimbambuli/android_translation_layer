package android.graphics.text;

import android.graphics.Paint;
import android.text.TextUtils;
import java.util.Objects;

/**
 * Shapes a run of text into positioned glyphs (AOSP android.graphics.text.TextRunShaper). AOSP shapes with
 * minikin; ATL shapes with Pango/HarfBuzz (see android_paint_shape_run).
 */
public class TextRunShaper {
	private TextRunShaper() {} // Do not instantiate

	public static PositionedGlyphs shapeTextRun(char[] text, int start, int count, int contextStart, int contextCount,
	                                            float xOffset, float yOffset, boolean isRtl, Paint paint) {
		Objects.requireNonNull(text);
		Objects.requireNonNull(paint);
		return new PositionedGlyphs(nativeShapeTextRun(text, start, count, contextStart, contextCount, isRtl, paint.paint),
		                            xOffset, yOffset);
	}

	public static PositionedGlyphs shapeTextRun(CharSequence text, int start, int count, int contextStart, int contextCount,
	                                            float xOffset, float yOffset, boolean isRtl, Paint paint) {
		Objects.requireNonNull(text);
		Objects.requireNonNull(paint);
		char[] buf = new char[contextCount];
		TextUtils.getChars(text, contextStart, contextStart + contextCount, buf, 0);
		return new PositionedGlyphs(nativeShapeTextRun(buf, start - contextStart, count, 0, contextCount, isRtl, paint.paint),
		                            xOffset, yOffset);
	}

	private static native Object[] nativeShapeTextRun(char[] text, int start, int count, int contextStart, int contextCount,
	                                                  boolean isRtl, long nativePaint);
}
