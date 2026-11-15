package android.media;

/* AOSP frameworks/base/media/java/android/media/ImageWriter.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class ImageWriter implements java.lang.AutoCloseable {
    public void close() {  }
    public android.media.Image dequeueInputImage() { return null; }
    public int getDataSpace() { return 0; }
    public int getFormat() { return 0; }
    public int getHardwareBufferFormat() { return 0; }
    public int getHeight() { return 0; }
    public int getMaxImages() { return 0; }
    public long getUsage() { return 0L; }
    public int getWidth() { return 0; }
    public static android.media.ImageWriter newInstance(android.view.Surface arg0, int arg1) { return null; }
    public static android.media.ImageWriter newInstance(android.view.Surface arg0, int arg1, int arg2) { return null; }
    public void queueInputImage(android.media.Image arg0) {  }
    public void setOnImageReleasedListener(android.media.ImageWriter.OnImageReleasedListener arg0, android.os.Handler arg1) {  }

    public static final class Builder {
        public Builder(android.view.Surface arg0)  {}
        public android.media.ImageWriter build() { return null; }
        public android.media.ImageWriter.Builder setDataSpace(int arg0) { return this; }
        public android.media.ImageWriter.Builder setHardwareBufferFormat(int arg0) { return this; }
        public android.media.ImageWriter.Builder setImageFormat(int arg0) { return this; }
        public android.media.ImageWriter.Builder setMaxImages(int arg0) { return this; }
        public android.media.ImageWriter.Builder setUsage(long arg0) { return this; }
        public android.media.ImageWriter.Builder setWidthAndHeight(int arg0, int arg1) { return this; }
    }

    public static interface OnImageReleasedListener {
        void onImageReleased(android.media.ImageWriter arg0);
    }
}
