/*
 * Host test for android.media.ThumbnailUtils (API 29 File/Size/CancellationSignal variants) and
 * what it builds on (ImageDecoder, ExifInterface, MediaMetadataRetriever, Bitmap).
 * It needs the native library, so the runtime's dalvikvm runs with
 *  - a display for gtk_init() (e.g. Xvfb, DISPLAY + GDK_BACKEND=x11),
 *  - LD_PRELOAD of libdl_bio.so and of a small stub with what the launcher provides to
 *    libtranslation_layer_main.so: `void *window; char *apk_path;`, an empty
 *    back_button_set_sensitive(), get_app_data_dir() returning a directory outside the
 *    temporary one, get_jni_env() via JNI_GetCreatedJavaVMs(), and a constructor calling gtk_init().
 * From the build dir:
 *   javac -source 8 -target 8 -bootclasspath $PREFIX/lib64/java/core-all_classes.jar \
 *     -cp src/api-impl/hax.jar:$PREFIX/lib64/java/core-junit_classes.jar -d t ../test/api-impl/android/media/ThumbnailUtilsTest.java
 *   dx --dex --output=t.jar t
 *   BIONIC_LD_LIBRARY_PATH=/nonexistent dalvikvm -Datl.native=$PREFIX/lib64/java/dex/android_translation_layer/natives/libtranslation_layer_main.so \
 *     [-Datl.video=some.mp4] -Xbootclasspath/a:t.jar:api-impl.jar junit.textui.TestRunner android.media.ThumbnailUtilsTest
 * (junit is on the boot class path, so the test and api-impl.jar go there too)
 */
package android.media;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class ThumbnailUtilsTest extends TestCase {
	private static boolean loaded;
	private File dir;

	@Override
	protected void setUp() throws Exception {
		if (!loaded) {
			System.load(System.getProperty("atl.native"));
			loaded = true;
		}
		dir = File.createTempFile("thumb", "");
		dir.delete();
		dir.mkdir();
	}

	@Override
	protected void tearDown() {
		for (File f : dir.listFiles())
			f.delete();
		dir.delete();
	}

	/* 200x100, left quarter blue, rest red */
	private static Bitmap testBitmap() {
		int[] colors = new int[200 * 100];
		for (int i = 0; i < colors.length; i++)
			colors[i] = i % 200 < 50 ? Color.BLUE : Color.RED;
		Bitmap b = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
		b.setPixels(colors, 0, 200, 0, 0, 200, 100);
		return b;
	}

	/* getPixel() only reads whole bitmaps here, so read all and pick one */
	private static int pixel(Bitmap b, int x, int y) {
		int[] pixels = new int[b.getWidth() * b.getHeight()];
		b.getPixels(pixels, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
		return pixels[y * b.getWidth() + x];
	}

	private File write(String name, byte[] data) throws IOException {
		File f = new File(dir, name);
		try (FileOutputStream out = new FileOutputStream(f)) {
			out.write(data);
		}
		return f;
	}

	private static byte[] compress(Bitmap b, Bitmap.CompressFormat format) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		b.compress(format, 95, out);
		return out.toByteArray();
	}

	/* inserts an APP1 Exif segment (IFD0: Orientation) right after SOI */
	private static byte[] withOrientation(byte[] jpeg, int orientation) {
		byte[] app1 = {(byte)0xff, (byte)0xe1, 0, 34, 'E', 'x', 'i', 'f', 0, 0,
		               'M', 'M', 0, 0x2a, 0, 0, 0, 8, 0, 1,
		               0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte)orientation, 0, 0,
		               0, 0, 0, 0};
		byte[] out = new byte[jpeg.length + app1.length];
		System.arraycopy(jpeg, 0, out, 0, 2);
		System.arraycopy(app1, 0, out, 2, app1.length);
		System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
		return out;
	}

	/* ID3v2.3 tag (optionally with an APIC cover) + 200 silent MPEG-1 Layer III frames */
	private static byte[] mp3(byte[] cover) throws IOException {
		ByteArrayOutputStream frames = new ByteArrayOutputStream();
		frame(frames, "TIT2", "\0Testlied".getBytes("ISO-8859-1"));
		if (cover != null) {
			ByteArrayOutputStream apic = new ByteArrayOutputStream();
			apic.write("\0image/jpeg\0\3Cover\0".getBytes("ISO-8859-1"));
			apic.write(cover);
			frame(frames, "APIC", apic.toByteArray());
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int n = frames.size();
		out.write(new byte[] {'I', 'D', '3', 3, 0, 0, (byte)((n >> 21) & 0x7f), (byte)((n >> 14) & 0x7f),
		                      (byte)((n >> 7) & 0x7f), (byte)(n & 0x7f)});
		frames.writeTo(out);
		byte[] frame = new byte[417];
		frame[0] = (byte)0xff; frame[1] = (byte)0xfb; frame[2] = (byte)0x90; frame[3] = 0x64;
		for (int i = 0; i < 200; i++)
			out.write(frame);
		return out.toByteArray();
	}

	private static void frame(ByteArrayOutputStream out, String id, byte[] data) throws IOException {
		out.write(id.getBytes("ISO-8859-1"));
		int n = data.length;
		out.write(new byte[] {(byte)(n >> 24), (byte)(n >> 16), (byte)(n >> 8), (byte)n, 0, 0});
		out.write(data);
	}

	public void testImageThumbnailPng() throws IOException {
		File f = write("bild.png", compress(testBitmap(), Bitmap.CompressFormat.PNG));
		Bitmap t = ThumbnailUtils.createImageThumbnail(f, new Size(50, 50), null);
		/* Resizer: sample = max(200/50, 100/50) = 4 -> 50x25 */
		assertEquals(50, t.getWidth());
		assertEquals(25, t.getHeight());
		assertEquals(Color.BLUE, pixel(t, 2, 12));
		assertEquals(Color.RED, pixel(t, 40, 12));
	}

	public void testImageThumbnailJpegSmallerThanSizeKeepsSize() throws IOException {
		File f = write("bild.jpg", compress(testBitmap(), Bitmap.CompressFormat.JPEG));
		Bitmap t = ThumbnailUtils.createImageThumbnail(f, new Size(512, 384), null);
		assertEquals(200, t.getWidth());
		assertEquals(100, t.getHeight());
	}

	public void testImageThumbnailAppliesExifOrientation() throws IOException {
		/* Orientation 6 = rotate 90 clockwise: the blue band ends up at the top */
		File f = write("bild-rot90.jpg", withOrientation(compress(testBitmap(), Bitmap.CompressFormat.JPEG), 6));
		Bitmap t = ThumbnailUtils.createImageThumbnail(f, new Size(512, 384), null);
		assertEquals(100, t.getWidth());
		assertEquals(200, t.getHeight());
		int top = pixel(t, 50, 10), bottom = pixel(t, 50, 190);
		assertTrue("top should be blue: " + Integer.toHexString(top), Color.blue(top) > 200 && Color.red(top) < 60);
		assertTrue("bottom should be red: " + Integer.toHexString(bottom), Color.red(bottom) > 200 && Color.blue(bottom) < 60);
	}

	public void testImageThumbnailUndecodableThrowsIOException() throws IOException {
		File f = write("kaputt.jpg", new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
		try {
			ThumbnailUtils.createImageThumbnail(f, new Size(96, 96), null);
			fail("expected IOException");
		} catch (IOException e) {
		}
	}

	public void testImageThumbnailMissingFileThrowsIOException() {
		try {
			ThumbnailUtils.createImageThumbnail(new File(dir, "fehlt.png"), new Size(96, 96), null);
			fail("expected IOException");
		} catch (IOException e) {
		}
	}

	public void testCanceledSignalThrows() throws IOException {
		File f = write("bild.png", compress(testBitmap(), Bitmap.CompressFormat.PNG));
		CancellationSignal signal = new CancellationSignal();
		signal.cancel();
		try {
			ThumbnailUtils.createImageThumbnail(f, new Size(96, 96), signal);
			fail("expected OperationCanceledException");
		} catch (OperationCanceledException e) {
		}
	}

	public void testDeprecatedImageThumbnailByPath() throws IOException {
		File f = write("bild.png", compress(testBitmap(), Bitmap.CompressFormat.PNG));
		Bitmap t = ThumbnailUtils.createImageThumbnail(f.getPath(), 3 /* MICRO_KIND, 96x96 */);
		assertEquals(100, t.getWidth());
		assertEquals(50, t.getHeight());
		assertNull(ThumbnailUtils.createImageThumbnail(new File(dir, "fehlt.png").getPath(), 3));
	}

	public void testAudioThumbnailFromEmbeddedCover() throws IOException {
		Bitmap cover = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
		cover.eraseColor(Color.GREEN);
		File f = write("lied-cover.mp3", mp3(compress(cover, Bitmap.CompressFormat.JPEG)));
		Bitmap t = ThumbnailUtils.createAudioThumbnail(f, new Size(512, 512), null);
		assertEquals(120, t.getWidth());
		assertEquals(80, t.getHeight());
		assertTrue(Color.green(pixel(t, 60, 40)) > 200);
	}

	public void testAudioThumbnailWithoutCoverThrowsIOException() throws IOException {
		File f = write("lied.mp3", mp3(null));
		try {
			ThumbnailUtils.createAudioThumbnail(f, new Size(512, 512), null);
			fail("expected IOException");
		} catch (IOException e) {
		}
	}

	public void testAudioThumbnailIgnoresPicturesOutsideExternalStorage() throws IOException {
		/* as on Android, only files on external storage get a picture from their folder; the
		 * temporary folder is not below the app's external storage (here the data dir) */
		Bitmap cover = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
		cover.eraseColor(Color.GREEN);
		write("albumart.jpg", compress(cover, Bitmap.CompressFormat.JPEG));
		File f = write("lied.mp3", mp3(null));
		try {
			ThumbnailUtils.createAudioThumbnail(f, new Size(512, 512), null);
			fail("expected IOException");
		} catch (IOException e) {
		}
	}

	public void testDurationWithoutPlayer() throws IOException {
		/* 200 MPEG-1 Layer III frames of 1152 samples at 44.1 kHz: 5.22 s */
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(write("lied.mp3", mp3(null)).getPath());
		long ms = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		retriever.release();
		assertTrue("duration " + ms, ms > 5000 && ms < 5400);
	}

	public void testVideoThumbnail() throws IOException {
		String video = System.getProperty("atl.video");
		if (video == null)
			return;
		Bitmap t = ThumbnailUtils.createVideoThumbnail(new File(video), new Size(96, 96), null);
		assertTrue(t.getWidth() > 0 && t.getWidth() <= 96);
		assertTrue(t.getHeight() > 0 && t.getHeight() <= 96);
		/* the frame's pixels, not an empty bitmap of the right size */
		boolean opaque = false;
		for (int x = 0; x < t.getWidth(); x++)
			opaque |= Color.alpha(pixel(t, x, t.getHeight() / 2)) == 255;
		assertTrue(opaque);
	}

	public void testExtractThumbnailCropsCenter() {
		Bitmap t = ThumbnailUtils.extractThumbnail(testBitmap(), 50, 50);
		assertEquals(50, t.getWidth());
		assertEquals(50, t.getHeight());
		/* centre crop of 200x100 -> 100x100 from x=50: all red, no stretched blue */
		assertEquals(Color.RED, pixel(t, 2, 25));
	}
}
