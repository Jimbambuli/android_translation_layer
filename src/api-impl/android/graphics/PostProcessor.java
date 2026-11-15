package android.graphics;

/* AOSP graphics/java/android/graphics/PostProcessor.java: draws on the decoded image (ImageDecoder.setPostProcessor())
 * and returns its opacity as a PixelFormat (UNKNOWN, TRANSLUCENT or OPAQUE) */
public interface PostProcessor {
	public int onPostProcess(Canvas canvas);
}
