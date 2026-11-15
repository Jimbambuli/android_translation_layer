package android.media;

/* AOSP frameworks/base/media/java/android/media/FaceDetector.java (public API surface; no backing service in ATL, bodies follow the AOSP no-service path) */
public class FaceDetector {
    public FaceDetector(int arg0, int arg1, int arg2)  {}
    public int findFaces(android.graphics.Bitmap arg0, android.media.FaceDetector.Face[] arg1) { return 0; }

    public class Face {
        public float confidence() { return 0f; }
        public float eyesDistance() { return 0f; }
        public void getMidPoint(android.graphics.PointF arg0) {  }
        public float pose(int arg0) { return 0f; }
        public static final float CONFIDENCE_THRESHOLD = 0.4f;
        public static final int EULER_X = 0; // 0x0;
        public static final int EULER_Y = 1; // 0x1;
        public static final int EULER_Z = 2; // 0x2;
    }
}
