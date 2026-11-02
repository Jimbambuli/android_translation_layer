/*
 * Host test for the color longs and color spaces of AOSP's Color/ColorSpace (API 26), which Compose uses through
 * Paint.setColor(long). Run like test/api-impl/android/app/job/JobSchedulerTest.java.
 */
package android.graphics;

import junit.framework.TestCase;

public class ColorTest extends TestCase {
	public void testSrgbPackRoundTrip() {
		long c = Color.pack(0x80ff4020);
		assertEquals(0x80ff4020, Color.toArgb(c));
		assertTrue(Color.isSrgb(c));
		assertEquals(ColorSpace.get(ColorSpace.Named.SRGB), Color.colorSpace(c));
	}

	public void testDisplayP3ConvertsToSrgb() {
		ColorSpace p3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3);
		long red = Color.pack(1.0f, 0.0f, 0.0f, 1.0f, p3);
		assertFalse(Color.isSrgb(red));
		/* P3 red lies outside sRGB and is clamped to pure red */
		assertEquals(0xffff0000, Color.toArgb(red));
		long srgbRedInP3 = Color.convert(0xffff0000, p3);
		assertEquals(0xffff0000, Color.toArgb(srgbRedInP3));
	}

	public void testColorSpaceRgbTransferParameters() {
		ColorSpace.Rgb srgb = (ColorSpace.Rgb)ColorSpace.get(ColorSpace.Named.SRGB);
		assertNotNull(srgb.getTransferParameters());
		assertEquals(2.4, srgb.getTransferParameters().g, 1e-9);
		ColorSpace.Rgb linear = (ColorSpace.Rgb)ColorSpace.get(ColorSpace.Named.LINEAR_SRGB);
		assertEquals(0.5, linear.getEotf().applyAsDouble(0.5), 1e-9);
		float[] v = ColorSpace.connect(srgb, linear).transform(1.0f, 0.5f, 0.0f);
		assertEquals(1.0f, v[0], 1e-4f);
		assertEquals(0.214f, v[1], 1e-3f);
	}

	public void testHsvLikeSkia() {
		float[] hsv = new float[3];
		Color.colorToHSV(0xff808080, hsv);
		assertEquals(0f, hsv[0]);
		assertEquals(0f, hsv[1]);
		Color.colorToHSV(0xff0000ff, hsv);
		assertEquals(240f, hsv[0]);
		Color.colorToHSV(0xffff00ff, hsv);
		assertEquals(300f, hsv[0]);
		assertEquals(0xff0000ff, Color.HSVToColor(new float[] {240f, 1f, 1f}));
		assertEquals(0x80ff00ff, Color.HSVToColor(0x80, new float[] {300f, 1f, 1f}));
		assertEquals(0xff808080, Color.HSVToColor(new float[] {0f, 0f, 128f / 255f}));
	}
}
