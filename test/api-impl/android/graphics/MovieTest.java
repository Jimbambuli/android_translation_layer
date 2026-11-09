package android.graphics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import junit.framework.TestCase;

/* a GIF of 4x4 pixels, three images: all red (100 ms), a blue 2x2 square at 1,1 (200 ms), all green (300 ms) */
public class MovieTest extends TestCase {
	private static final String GIF = "R0lGODlhBAAEAIEAAP8AAAAA/wD/AAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQECgAAACwAAAAABAAEAAAICQABCBxIsCCAgAAh+QQEFAAAACwBAAEAAgACAIH/AAAAAP8A/wAAAAAIBgADCAwQEAAh+QQEHgAAACwAAAAABAAEAIH/AAAAAP8A/wAAAAAICQAFCBxIsKCAgAA7";

	private static Movie movie() {
		byte[] data = Base64.getDecoder().decode(GIF);
		return Movie.decodeByteArray(data, 0, data.length);
	}

	private static int[] pixels(Movie movie) throws Exception {
		Method compose = Movie.class.getDeclaredMethod("compose");
		compose.setAccessible(true);
		compose.invoke(movie);
		Field canvas = Movie.class.getDeclaredField("mCanvas");
		canvas.setAccessible(true);
		return (int[])canvas.get(movie);
	}

	public void testSizeAndDuration() {
		Movie m = movie();
		assertNotNull(m);
		assertEquals(4, m.width());
		assertEquals(4, m.height());
		assertEquals(600, m.duration());
		assertFalse(m.isOpaque());
	}

	public void testNotAGif() {
		byte[] png = {(byte)0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 0};
		assertNull(Movie.decodeByteArray(png, 0, png.length));
	}

	public void testSetTime() {
		Movie m = movie();
		/* the time 0 is the time the movie starts at: no change */
		assertFalse(m.setTime(0));
		assertTrue(m.setTime(150)); /* in the second image */
		assertTrue(m.setTime(1000)); /* after the end: the duration, the last image */
		assertFalse(m.setTime(600));
	}

	public void testFrames() throws Exception {
		Movie m = movie();
		m.setTime(50);
		int red = 0xFFFF0000, blue = 0xFF0000FF, green = 0xFF00FF00;
		int[] p = pixels(m);
		assertEquals(red, p[0]);
		assertEquals(red, p[5]);
		/* the second image is drawn over the first: the square is blue, the rest stays red */
		m.setTime(250);
		p = pixels(m);
		assertEquals(red, p[0]);
		assertEquals(blue, p[1 * 4 + 1]);
		assertEquals(blue, p[2 * 4 + 2]);
		assertEquals(red, p[3 * 4 + 3]);
		m.setTime(500);
		p = pixels(m);
		assertEquals(green, p[0]);
		assertEquals(green, p[1 * 4 + 1]);
		/* the repeat starts at the first image again */
		m.setTime(10);
		p = pixels(m);
		assertEquals(red, p[1 * 4 + 1]);
	}
}
