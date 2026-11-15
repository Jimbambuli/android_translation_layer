package android.media;

/* AOSP frameworks/base/media/java/android/media/ImageReader.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class ImageReader implements java.lang.AutoCloseable {
    public android.media.Image acquireLatestImage() { return null; }
    public android.media.Image acquireNextImage() { return null; }
    public void close() {  }
    public void discardFreeBuffers() {  }
    public int getDataSpace() { return 0; }
    public int getHardwareBufferFormat() { return 0; }
    public int getHeight() { return 0; }
    public int getImageFormat() { return 0; }
    public int getMaxImages() { return 0; }
    public android.view.Surface getSurface() { return null; }
    public long getUsage() { return 0L; }
    public int getWidth() { return 0; }
    public static android.media.ImageReader newInstance(int arg0, int arg1, int arg2, int arg3) { return null; }
    public static android.media.ImageReader newInstance(int arg0, int arg1, int arg2, int arg3, long arg4) { return null; }
    public void setOnImageAvailableListener(android.media.ImageReader.OnImageAvailableListener arg0, android.os.Handler arg1) {  }

    public static final class Builder {
        public Builder(int arg0, int arg1)  {}
        public android.media.ImageReader build() { return null; }
        public android.media.ImageReader.Builder setDefaultDataSpace(int arg0) { return this; }
        public android.media.ImageReader.Builder setDefaultHardwareBufferFormat(int arg0) { return this; }
        public android.media.ImageReader.Builder setImageFormat(int arg0) { return this; }
        public android.media.ImageReader.Builder setMaxImages(int arg0) { return this; }
        public android.media.ImageReader.Builder setUsage(long arg0) { return this; }
    }

    public static interface OnImageAvailableListener {
        void onImageAvailable(android.media.ImageReader arg0);
    }
}
