package android.graphics;

/* ATL: a picture records into a bitmap of its size, which it draws (scaled when drawn into a rectangle of
 * another size); AOSP keeps the drawing commands instead. */
public class Picture {
	private Bitmap bitmap;
	private Canvas recordingCanvas;
	private int width;
	private int height;

	public Picture() {}

	public Picture(Picture src) {
		if (src != null && src.bitmap != null) {
			bitmap = Bitmap.createBitmap(src.bitmap);
			width = src.width;
			height = src.height;
		}
	}

	public Canvas beginRecording(int width, int height) {
		if (recordingCanvas != null)
			throw new IllegalStateException("Picture already recording, must call #endRecording()");
		this.width = width;
		this.height = height;
		bitmap = Bitmap.createBitmap(Math.max(width, 1), Math.max(height, 1), Bitmap.Config.ARGB_8888);
		recordingCanvas = new Canvas(bitmap);
		return recordingCanvas;
	}

	public void endRecording() {
		recordingCanvas = null;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean requiresHardwareAcceleration() {
		return false;
	}

	public void draw(Canvas canvas) {
		if (recordingCanvas != null)
			endRecording();
		if (bitmap != null)
			canvas.drawBitmap(bitmap, 0, 0, null);
	}

	public void close() {
		bitmap = null;
	}
}
