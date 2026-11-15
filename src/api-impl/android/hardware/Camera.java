package android.hardware;

import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/* AOSP core/java/android/hardware/Camera.java. ATL has no camera service: getNumberOfCameras() is 0 and
 * open() connects to nothing, so a Camera only exists as the unconnected object behind
 * getEmptyParameters(). Its instance methods act like AOSP's JNI (core/jni/android_hardware_Camera.cpp)
 * without a native context: release() does nothing, everything else throws get_native_camera()'s
 * RuntimeException. Parameters, Size and Area are AOSP's Java code (comments and annotations removed). */
public class Camera {
	private static final String TAG = "Camera";

	public static final int CAMERA_ERROR_UNKNOWN = 1;
	public static final int CAMERA_ERROR_EVICTED = 2;
	public static final int CAMERA_ERROR_DISABLED = 3;
	public static final int CAMERA_ERROR_SERVER_DIED = 100;

	public interface PreviewCallback {
		void onPreviewFrame(byte[] data, Camera camera);
	}

	public interface AutoFocusCallback {
		void onAutoFocus(boolean success, Camera camera);
	}

	public interface AutoFocusMoveCallback {
		void onAutoFocusMoving(boolean start, Camera camera);
	}

	public interface ShutterCallback {
		void onShutter();
	}

	public interface PictureCallback {
		void onPictureTaken(byte[] data, Camera camera);
	}

	public interface OnZoomChangeListener {
		void onZoomChange(int zoomValue, boolean stopped, Camera camera);
	}

	public interface FaceDetectionListener {
		void onFaceDetection(Face[] faces, Camera camera);
	}

	public interface ErrorCallback {
		void onError(int error, Camera camera);
	}

	public static class Face {
		public Face() {
		}

		public Rect rect;
		public int score;
		public int id = -1;
		public Point leftEye = null;
		public Point rightEye = null;
		public Point mouth = null;
	}

	public static class CameraInfo {
		public static final int CAMERA_FACING_BACK = 0;
		public static final int CAMERA_FACING_FRONT = 1;
		public int facing;
		public int orientation;
		public boolean canDisableShutterSound;
	}

	public static int getNumberOfCameras() { return 0; }

	/* ATL has no camera, so this fails like AOSP's native _getCameraInfo() for an unknown camera id */
	public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
		throw new RuntimeException("Fail to get camera info");
	}

	/* AOSP open(): the first back-facing camera, null when there is none */
	public static Camera open() {
		return null;
	}

	/* AOSP open(int) is new Camera(cameraId): without a camera service Camera::connect() returns NULL,
	 * native_setup() reports -EACCES (core/jni/android_hardware_Camera.cpp) and the constructor throws */
	public static Camera open(int cameraId) {
		throw new RuntimeException("Fail to connect to camera service");
	}

	/* AOSP Camera(): an unconnected camera, used by getEmptyParameters() */
	Camera() {
	}

	/* AOSP getEmptyParameters()/getParametersCopy(): need no camera connection */
	public static Parameters getEmptyParameters() {
		Camera camera = new Camera();
		return camera.new Parameters();
	}

	public static Parameters getParametersCopy(Camera.Parameters parameters) {
		if (parameters == null)
			throw new NullPointerException("parameters must not be null");
		Camera camera = parameters.getOuter();
		Parameters p = camera.new Parameters();
		p.copyFrom(parameters);
		return p;
	}

	private ErrorCallback mErrorCallback;
	private OnZoomChangeListener mZoomListener;
	private FaceDetectionListener mFaceListener;

	/* get_native_camera() without a native context */
	private static RuntimeException notConnected() {
		return new RuntimeException("Camera is being used after Camera.release() was called");
	}

	/* AOSP release(): native_release() of a camera without a context only clears it */
	public final void release() {
	}

	public final void unlock() { throw notConnected(); }

	public final void lock() { throw notConnected(); }

	public final void reconnect() throws IOException { throw notConnected(); }

	public final void setPreviewDisplay(SurfaceHolder holder) throws IOException { throw notConnected(); }

	public final void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException { throw notConnected(); }

	public final void startPreview() { throw notConnected(); }

	public final void stopPreview() { throw notConnected(); }

	/* AOSP enableShutterSound(): false if the sound is forced (AudioService.isCameraSoundForced(), see
	 * MediaActionSound.mustPlayShutterSound(): false), else native _enableShutterSound() of a camera without a
	 * context */
	public final boolean enableShutterSound(boolean enabled) {
		throw notConnected();
	}

	public final void setPreviewCallback(PreviewCallback cb) { throw notConnected(); }

	public final void setOneShotPreviewCallback(PreviewCallback cb) { throw notConnected(); }

	public final void setPreviewCallbackWithBuffer(PreviewCallback cb) { throw notConnected(); }

	public final void addCallbackBuffer(byte[] callbackBuffer) { throw notConnected(); }

	public final void autoFocus(AutoFocusCallback cb) { throw notConnected(); }

	public final void cancelAutoFocus() { throw notConnected(); }

	public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) { throw notConnected(); }

	public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg) { throw notConnected(); }

	public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg) { throw notConnected(); }

	public final void startSmoothZoom(int value) { throw notConnected(); }

	public final void stopSmoothZoom() { throw notConnected(); }

	public final void setDisplayOrientation(int degrees) { throw notConnected(); }

	public final void setZoomChangeListener(OnZoomChangeListener listener) {
		mZoomListener = listener;
	}

	public final void setFaceDetectionListener(FaceDetectionListener listener) {
		mFaceListener = listener;
	}

	public final void startFaceDetection() { throw notConnected(); }

	public final void stopFaceDetection() { throw notConnected(); }

	public final void setErrorCallback(ErrorCallback cb) {
		mErrorCallback = cb;
	}

	public void setParameters(Parameters params) { throw notConnected(); }

	public Parameters getParameters() { throw notConnected(); }

	public class Size {
		
		public Size(int w, int h) {
			width = w;
			height = h;
		}
		
		@Override
public boolean equals(Object obj) {
			if (!(obj instanceof Size)) {
				return false;
			}
			Size s = (Size) obj;
			return width == s.width && height == s.height;
		}
		@Override
public int hashCode() {
			return width * 32713 + height;
		}
		
		public int width;
		
		public int height;
	};

	@Deprecated
	public static class Area {
		
		public Area(Rect rect, int weight) {
			this.rect = rect;
			this.weight = weight;
		}
		
		@Override
public boolean equals(Object obj) {
			if (!(obj instanceof Area)) {
				return false;
			}
			Area a = (Area) obj;
			if (rect == null) {
				if (a.rect != null) return false;
			} else {
				if (!rect.equals(a.rect)) return false;
			}
			return weight == a.weight;
		}

		public Rect rect;

		public int weight;
	}

	@Deprecated
	public class Parameters {
		private static final String KEY_PREVIEW_SIZE = "preview-size";
		private static final String KEY_PREVIEW_FORMAT = "preview-format";
		private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
		private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
		private static final String KEY_PICTURE_SIZE = "picture-size";
		private static final String KEY_PICTURE_FORMAT = "picture-format";
		private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
		private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
		private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
		private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
		private static final String KEY_JPEG_QUALITY = "jpeg-quality";
		private static final String KEY_ROTATION = "rotation";
		private static final String KEY_GPS_LATITUDE = "gps-latitude";
		private static final String KEY_GPS_LONGITUDE = "gps-longitude";
		private static final String KEY_GPS_ALTITUDE = "gps-altitude";
		private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
		private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
		private static final String KEY_WHITE_BALANCE = "whitebalance";
		private static final String KEY_EFFECT = "effect";
		private static final String KEY_ANTIBANDING = "antibanding";
		private static final String KEY_SCENE_MODE = "scene-mode";
		private static final String KEY_FLASH_MODE = "flash-mode";
		private static final String KEY_FOCUS_MODE = "focus-mode";
		private static final String KEY_FOCUS_AREAS = "focus-areas";
		private static final String KEY_MAX_NUM_FOCUS_AREAS = "max-num-focus-areas";
		private static final String KEY_FOCAL_LENGTH = "focal-length";
		private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
		private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
		private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
		private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
		private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
		private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
		private static final String KEY_AUTO_EXPOSURE_LOCK = "auto-exposure-lock";
		private static final String KEY_AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
		private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
		private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
		private static final String KEY_METERING_AREAS = "metering-areas";
		private static final String KEY_MAX_NUM_METERING_AREAS = "max-num-metering-areas";
		private static final String KEY_ZOOM = "zoom";
		private static final String KEY_MAX_ZOOM = "max-zoom";
		private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
		private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
		private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
		private static final String KEY_FOCUS_DISTANCES = "focus-distances";
		private static final String KEY_VIDEO_SIZE = "video-size";
		private static final String KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO =
											"preferred-preview-size-for-video";
		private static final String KEY_MAX_NUM_DETECTED_FACES_HW = "max-num-detected-faces-hw";
		private static final String KEY_MAX_NUM_DETECTED_FACES_SW = "max-num-detected-faces-sw";
		private static final String KEY_RECORDING_HINT = "recording-hint";
		private static final String KEY_VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
		private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
		private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
		private static final String SUPPORTED_VALUES_SUFFIX = "-values";

		private static final String TRUE = "true";
		private static final String FALSE = "false";
		public static final String WHITE_BALANCE_AUTO = "auto";
		public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
		public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
		public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
		public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
		public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
		public static final String WHITE_BALANCE_TWILIGHT = "twilight";
		public static final String WHITE_BALANCE_SHADE = "shade";
		public static final String EFFECT_NONE = "none";
		public static final String EFFECT_MONO = "mono";
		public static final String EFFECT_NEGATIVE = "negative";
		public static final String EFFECT_SOLARIZE = "solarize";
		public static final String EFFECT_SEPIA = "sepia";
		public static final String EFFECT_POSTERIZE = "posterize";
		public static final String EFFECT_WHITEBOARD = "whiteboard";
		public static final String EFFECT_BLACKBOARD = "blackboard";
		public static final String EFFECT_AQUA = "aqua";
		public static final String ANTIBANDING_AUTO = "auto";
		public static final String ANTIBANDING_50HZ = "50hz";
		public static final String ANTIBANDING_60HZ = "60hz";
		public static final String ANTIBANDING_OFF = "off";
		
		public static final String FLASH_MODE_OFF = "off";

		public static final String FLASH_MODE_AUTO = "auto";

		public static final String FLASH_MODE_ON = "on";

		public static final String FLASH_MODE_RED_EYE = "red-eye";

		public static final String FLASH_MODE_TORCH = "torch";

		public static final String SCENE_MODE_AUTO = "auto";

		public static final String SCENE_MODE_ACTION = "action";

		public static final String SCENE_MODE_PORTRAIT = "portrait";

		public static final String SCENE_MODE_LANDSCAPE = "landscape";

		public static final String SCENE_MODE_NIGHT = "night";

		public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";

		public static final String SCENE_MODE_THEATRE = "theatre";

		public static final String SCENE_MODE_BEACH = "beach";

		public static final String SCENE_MODE_SNOW = "snow";

		public static final String SCENE_MODE_SUNSET = "sunset";

		public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";

		public static final String SCENE_MODE_FIREWORKS = "fireworks";

		public static final String SCENE_MODE_SPORTS = "sports";

		public static final String SCENE_MODE_PARTY = "party";

		public static final String SCENE_MODE_CANDLELIGHT = "candlelight";

		public static final String SCENE_MODE_BARCODE = "barcode";

		public static final String SCENE_MODE_HDR = "hdr";

		public static final String FOCUS_MODE_AUTO = "auto";

		public static final String FOCUS_MODE_INFINITY = "infinity";

		public static final String FOCUS_MODE_MACRO = "macro";

		public static final String FOCUS_MODE_FIXED = "fixed";

		public static final String FOCUS_MODE_EDOF = "edof";

		public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";

		public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
		
		public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;

		public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;

		public static final int FOCUS_DISTANCE_FAR_INDEX = 2;

		public static final int PREVIEW_FPS_MIN_INDEX = 0;

		public static final int PREVIEW_FPS_MAX_INDEX = 1;
		private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
		private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
		private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
		private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
		private static final String PIXEL_FORMAT_RGB565 = "rgb565";
		private static final String PIXEL_FORMAT_JPEG = "jpeg";
		private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";

		private final LinkedHashMap<String, String> mMap;

		private Parameters() {
			mMap = new LinkedHashMap<String, String>(64);
		}

		public void copyFrom(Parameters other) {
			if (other == null) {
				throw new NullPointerException("other must not be null");
			}

			mMap.putAll(other.mMap);
		}

		private Camera getOuter() {
			return Camera.this;
		}

		public boolean same(Parameters other) {
			if (this == other) {
				return true;
			}
			return other != null && Parameters.this.mMap.equals(other.mMap);
		}

		@Deprecated
		public void dump() {
			Log.e(TAG, "dump: size=" + mMap.size());
			for (String k : mMap.keySet()) {
				Log.e(TAG, "dump: " + k + "=" + mMap.get(k));
			}
		}

		public String flatten() {
			StringBuilder flattened = new StringBuilder(128);
			for (String k : mMap.keySet()) {
				flattened.append(k);
				flattened.append("=");
				flattened.append(mMap.get(k));
				flattened.append(";");
			}
			flattened.deleteCharAt(flattened.length()-1);
			return flattened.toString();
		}

		public void unflatten(String flattened) {
			mMap.clear();

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
			splitter.setString(flattened);
			for (String kv : splitter) {
				int pos = kv.indexOf('=');
				if (pos == -1) {
					continue;
				}
				String k = kv.substring(0, pos);
				String v = kv.substring(pos + 1);
				mMap.put(k, v);
			}
		}

		public void remove(String key) {
			mMap.remove(key);
		}

		public void set(String key, String value) {
			if (key.indexOf('=') != -1 || key.indexOf(';') != -1 || key.indexOf(0) != -1) {
				Log.e(TAG, "Key \"" + key + "\" contains invalid character (= or ; or \\0)");
				return;
			}
			if (value.indexOf('=') != -1 || value.indexOf(';') != -1 || value.indexOf(0) != -1) {
				Log.e(TAG, "Value \"" + value + "\" contains invalid character (= or ; or \\0)");
				return;
			}

			put(key, value);
		}

		public void set(String key, int value) {
			put(key, Integer.toString(value));
		}

		private void put(String key, String value) {
			
			mMap.remove(key);
			mMap.put(key, value);
		}

		private void set(String key, List<Area> areas) {
			if (areas == null) {
				set(key, "(0,0,0,0,0)");
			} else {
				StringBuilder buffer = new StringBuilder();
				for (int i = 0; i < areas.size(); i++) {
					Area area = areas.get(i);
					Rect rect = area.rect;
					buffer.append('(');
					buffer.append(rect.left);
					buffer.append(',');
					buffer.append(rect.top);
					buffer.append(',');
					buffer.append(rect.right);
					buffer.append(',');
					buffer.append(rect.bottom);
					buffer.append(',');
					buffer.append(area.weight);
					buffer.append(')');
					if (i != areas.size() - 1) buffer.append(',');
				}
				set(key, buffer.toString());
			}
		}

		public String get(String key) {
			return mMap.get(key);
		}

		public int getInt(String key) {
			return Integer.parseInt(mMap.get(key));
		}

		public void setPreviewSize(int width, int height) {
			String v = Integer.toString(width) + "x" + Integer.toString(height);
			set(KEY_PREVIEW_SIZE, v);
		}

		public Size getPreviewSize() {
			String pair = get(KEY_PREVIEW_SIZE);
			return strToSize(pair);
		}

		public List<Size> getSupportedPreviewSizes() {
			String str = get(KEY_PREVIEW_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(str);
		}

		public List<Size> getSupportedVideoSizes() {
			String str = get(KEY_VIDEO_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(str);
		}

		public Size getPreferredPreviewSizeForVideo() {
			String pair = get(KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO);
			return strToSize(pair);
		}

		public void setJpegThumbnailSize(int width, int height) {
			set(KEY_JPEG_THUMBNAIL_WIDTH, width);
			set(KEY_JPEG_THUMBNAIL_HEIGHT, height);
		}

		public Size getJpegThumbnailSize() {
			return new Size(getInt(KEY_JPEG_THUMBNAIL_WIDTH),
							getInt(KEY_JPEG_THUMBNAIL_HEIGHT));
		}

		public List<Size> getSupportedJpegThumbnailSizes() {
			String str = get(KEY_JPEG_THUMBNAIL_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(str);
		}

		public void setJpegThumbnailQuality(int quality) {
			set(KEY_JPEG_THUMBNAIL_QUALITY, quality);
		}

		public int getJpegThumbnailQuality() {
			return getInt(KEY_JPEG_THUMBNAIL_QUALITY);
		}

		public void setJpegQuality(int quality) {
			set(KEY_JPEG_QUALITY, quality);
		}

		public int getJpegQuality() {
			return getInt(KEY_JPEG_QUALITY);
		}

		@Deprecated
		public void setPreviewFrameRate(int fps) {
			set(KEY_PREVIEW_FRAME_RATE, fps);
		}

		@Deprecated
		public int getPreviewFrameRate() {
			return getInt(KEY_PREVIEW_FRAME_RATE);
		}

		@Deprecated
		public List<Integer> getSupportedPreviewFrameRates() {
			String str = get(KEY_PREVIEW_FRAME_RATE + SUPPORTED_VALUES_SUFFIX);
			return splitInt(str);
		}

		public void setPreviewFpsRange(int min, int max) {
			set(KEY_PREVIEW_FPS_RANGE, "" + min + "," + max);
		}

		public void getPreviewFpsRange(int[] range) {
			if (range == null || range.length != 2) {
				throw new IllegalArgumentException(
						"range must be an array with two elements.");
			}
			splitInt(get(KEY_PREVIEW_FPS_RANGE), range);
		}

		public List<int[]> getSupportedPreviewFpsRange() {
			String str = get(KEY_PREVIEW_FPS_RANGE + SUPPORTED_VALUES_SUFFIX);
			return splitRange(str);
		}

		public void setPreviewFormat(int pixel_format) {
			String s = cameraFormatForPixelFormat(pixel_format);
			if (s == null) {
				throw new IllegalArgumentException(
						"Invalid pixel_format=" + pixel_format);
			}

			set(KEY_PREVIEW_FORMAT, s);
		}

		public int getPreviewFormat() {
			return pixelFormatForCameraFormat(get(KEY_PREVIEW_FORMAT));
		}

		public List<Integer> getSupportedPreviewFormats() {
			String str = get(KEY_PREVIEW_FORMAT + SUPPORTED_VALUES_SUFFIX);
			ArrayList<Integer> formats = new ArrayList<Integer>();
			for (String s : split(str)) {
				int f = pixelFormatForCameraFormat(s);
				if (f == ImageFormat.UNKNOWN) continue;
				formats.add(f);
			}
			return formats;
		}

		public void setPictureSize(int width, int height) {
			String v = Integer.toString(width) + "x" + Integer.toString(height);
			set(KEY_PICTURE_SIZE, v);
		}

		public Size getPictureSize() {
			String pair = get(KEY_PICTURE_SIZE);
			return strToSize(pair);
		}

		public List<Size> getSupportedPictureSizes() {
			String str = get(KEY_PICTURE_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(str);
		}

		public void setPictureFormat(int pixel_format) {
			String s = cameraFormatForPixelFormat(pixel_format);
			if (s == null) {
				throw new IllegalArgumentException(
						"Invalid pixel_format=" + pixel_format);
			}

			set(KEY_PICTURE_FORMAT, s);
		}

		public int getPictureFormat() {
			return pixelFormatForCameraFormat(get(KEY_PICTURE_FORMAT));
		}

		public List<Integer> getSupportedPictureFormats() {
			String str = get(KEY_PICTURE_FORMAT + SUPPORTED_VALUES_SUFFIX);
			ArrayList<Integer> formats = new ArrayList<Integer>();
			for (String s : split(str)) {
				int f = pixelFormatForCameraFormat(s);
				if (f == ImageFormat.UNKNOWN) continue;
				formats.add(f);
			}
			return formats;
		}

		private String cameraFormatForPixelFormat(int pixel_format) {
			switch(pixel_format) {
			case ImageFormat.NV16:      return PIXEL_FORMAT_YUV422SP;
			case ImageFormat.NV21:      return PIXEL_FORMAT_YUV420SP;
			case ImageFormat.YUY2:      return PIXEL_FORMAT_YUV422I;
			case ImageFormat.YV12:      return PIXEL_FORMAT_YUV420P;
			case ImageFormat.RGB_565:   return PIXEL_FORMAT_RGB565;
			case ImageFormat.JPEG:      return PIXEL_FORMAT_JPEG;
			default:                    return null;
			}
		}

		private int pixelFormatForCameraFormat(String format) {
			if (format == null)
				return ImageFormat.UNKNOWN;

			if (format.equals(PIXEL_FORMAT_YUV422SP))
				return ImageFormat.NV16;

			if (format.equals(PIXEL_FORMAT_YUV420SP))
				return ImageFormat.NV21;

			if (format.equals(PIXEL_FORMAT_YUV422I))
				return ImageFormat.YUY2;

			if (format.equals(PIXEL_FORMAT_YUV420P))
				return ImageFormat.YV12;

			if (format.equals(PIXEL_FORMAT_RGB565))
				return ImageFormat.RGB_565;

			if (format.equals(PIXEL_FORMAT_JPEG))
				return ImageFormat.JPEG;

			return ImageFormat.UNKNOWN;
		}

		public void setRotation(int rotation) {
			if (rotation == 0 || rotation == 90 || rotation == 180
					|| rotation == 270) {
				set(KEY_ROTATION, Integer.toString(rotation));
			} else {
				throw new IllegalArgumentException(
						"Invalid rotation=" + rotation);
			}
		}

		public void setGpsLatitude(double latitude) {
			set(KEY_GPS_LATITUDE, Double.toString(latitude));
		}

		public void setGpsLongitude(double longitude) {
			set(KEY_GPS_LONGITUDE, Double.toString(longitude));
		}

		public void setGpsAltitude(double altitude) {
			set(KEY_GPS_ALTITUDE, Double.toString(altitude));
		}

		public void setGpsTimestamp(long timestamp) {
			set(KEY_GPS_TIMESTAMP, Long.toString(timestamp));
		}

		public void setGpsProcessingMethod(String processing_method) {
			set(KEY_GPS_PROCESSING_METHOD, processing_method);
		}

		public void removeGpsData() {
			remove(KEY_GPS_LATITUDE);
			remove(KEY_GPS_LONGITUDE);
			remove(KEY_GPS_ALTITUDE);
			remove(KEY_GPS_TIMESTAMP);
			remove(KEY_GPS_PROCESSING_METHOD);
		}

		public String getWhiteBalance() {
			return get(KEY_WHITE_BALANCE);
		}

		public void setWhiteBalance(String value) {
			String oldValue = get(KEY_WHITE_BALANCE);
			if (same(value, oldValue)) return;
			set(KEY_WHITE_BALANCE, value);
			set(KEY_AUTO_WHITEBALANCE_LOCK, FALSE);
		}

		public List<String> getSupportedWhiteBalance() {
			String str = get(KEY_WHITE_BALANCE + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public String getColorEffect() {
			return get(KEY_EFFECT);
		}

		public void setColorEffect(String value) {
			set(KEY_EFFECT, value);
		}

		public List<String> getSupportedColorEffects() {
			String str = get(KEY_EFFECT + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public String getAntibanding() {
			return get(KEY_ANTIBANDING);
		}

		public void setAntibanding(String antibanding) {
			set(KEY_ANTIBANDING, antibanding);
		}

		public List<String> getSupportedAntibanding() {
			String str = get(KEY_ANTIBANDING + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public String getSceneMode() {
			return get(KEY_SCENE_MODE);
		}

		public void setSceneMode(String value) {
			set(KEY_SCENE_MODE, value);
		}

		public List<String> getSupportedSceneModes() {
			String str = get(KEY_SCENE_MODE + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public String getFlashMode() {
			return get(KEY_FLASH_MODE);
		}

		public void setFlashMode(String value) {
			set(KEY_FLASH_MODE, value);
		}

		public List<String> getSupportedFlashModes() {
			String str = get(KEY_FLASH_MODE + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public String getFocusMode() {
			return get(KEY_FOCUS_MODE);
		}

		public void setFocusMode(String value) {
			set(KEY_FOCUS_MODE, value);
		}

		public List<String> getSupportedFocusModes() {
			String str = get(KEY_FOCUS_MODE + SUPPORTED_VALUES_SUFFIX);
			return split(str);
		}

		public float getFocalLength() {
			return Float.parseFloat(get(KEY_FOCAL_LENGTH));
		}

		public float getHorizontalViewAngle() {
			return Float.parseFloat(get(KEY_HORIZONTAL_VIEW_ANGLE));
		}

		public float getVerticalViewAngle() {
			return Float.parseFloat(get(KEY_VERTICAL_VIEW_ANGLE));
		}

		public int getExposureCompensation() {
			return getInt(KEY_EXPOSURE_COMPENSATION, 0);
		}

		public void setExposureCompensation(int value) {
			set(KEY_EXPOSURE_COMPENSATION, value);
		}

		public int getMaxExposureCompensation() {
			return getInt(KEY_MAX_EXPOSURE_COMPENSATION, 0);
		}

		public int getMinExposureCompensation() {
			return getInt(KEY_MIN_EXPOSURE_COMPENSATION, 0);
		}

		public float getExposureCompensationStep() {
			return getFloat(KEY_EXPOSURE_COMPENSATION_STEP, 0);
		}

		public void setAutoExposureLock(boolean toggle) {
			set(KEY_AUTO_EXPOSURE_LOCK, toggle ? TRUE : FALSE);
		}

		public boolean getAutoExposureLock() {
			String str = get(KEY_AUTO_EXPOSURE_LOCK);
			return TRUE.equals(str);
		}

		public boolean isAutoExposureLockSupported() {
			String str = get(KEY_AUTO_EXPOSURE_LOCK_SUPPORTED);
			return TRUE.equals(str);
		}

		public void setAutoWhiteBalanceLock(boolean toggle) {
			set(KEY_AUTO_WHITEBALANCE_LOCK, toggle ? TRUE : FALSE);
		}

		public boolean getAutoWhiteBalanceLock() {
			String str = get(KEY_AUTO_WHITEBALANCE_LOCK);
			return TRUE.equals(str);
		}

		public boolean isAutoWhiteBalanceLockSupported() {
			String str = get(KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED);
			return TRUE.equals(str);
		}

		public int getZoom() {
			return getInt(KEY_ZOOM, 0);
		}

		public void setZoom(int value) {
			set(KEY_ZOOM, value);
		}

		public boolean isZoomSupported() {
			String str = get(KEY_ZOOM_SUPPORTED);
			return TRUE.equals(str);
		}

		public int getMaxZoom() {
			return getInt(KEY_MAX_ZOOM, 0);
		}

		public List<Integer> getZoomRatios() {
			return splitInt(get(KEY_ZOOM_RATIOS));
		}

		public boolean isSmoothZoomSupported() {
			String str = get(KEY_SMOOTH_ZOOM_SUPPORTED);
			return TRUE.equals(str);
		}

		public void getFocusDistances(float[] output) {
			if (output == null || output.length != 3) {
				throw new IllegalArgumentException(
						"output must be a float array with three elements.");
			}
			splitFloat(get(KEY_FOCUS_DISTANCES), output);
		}

		public int getMaxNumFocusAreas() {
			return getInt(KEY_MAX_NUM_FOCUS_AREAS, 0);
		}

		public List<Area> getFocusAreas() {
			return splitArea(get(KEY_FOCUS_AREAS));
		}

		public void setFocusAreas(List<Area> focusAreas) {
			set(KEY_FOCUS_AREAS, focusAreas);
		}

		public int getMaxNumMeteringAreas() {
			return getInt(KEY_MAX_NUM_METERING_AREAS, 0);
		}

		public List<Area> getMeteringAreas() {
			return splitArea(get(KEY_METERING_AREAS));
		}

		public void setMeteringAreas(List<Area> meteringAreas) {
			set(KEY_METERING_AREAS, meteringAreas);
		}

		public int getMaxNumDetectedFaces() {
			return getInt(KEY_MAX_NUM_DETECTED_FACES_HW, 0);
		}

		public void setRecordingHint(boolean hint) {
			set(KEY_RECORDING_HINT, hint ? TRUE : FALSE);
		}

		public boolean isVideoSnapshotSupported() {
			String str = get(KEY_VIDEO_SNAPSHOT_SUPPORTED);
			return TRUE.equals(str);
		}

		public void setVideoStabilization(boolean toggle) {
			set(KEY_VIDEO_STABILIZATION, toggle ? TRUE : FALSE);
		}

		public boolean getVideoStabilization() {
			String str = get(KEY_VIDEO_STABILIZATION);
			return TRUE.equals(str);
		}

		public boolean isVideoStabilizationSupported() {
			String str = get(KEY_VIDEO_STABILIZATION_SUPPORTED);
			return TRUE.equals(str);
		}
		private ArrayList<String> split(String str) {
			if (str == null) return null;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(str);
			ArrayList<String> substrings = new ArrayList<String>();
			for (String s : splitter) {
				substrings.add(s);
			}
			return substrings;
		}
		private ArrayList<Integer> splitInt(String str) {
			if (str == null) return null;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(str);
			ArrayList<Integer> substrings = new ArrayList<Integer>();
			for (String s : splitter) {
				substrings.add(Integer.parseInt(s));
			}
			if (substrings.size() == 0) return null;
			return substrings;
		}

		private void splitInt(String str, int[] output) {
			if (str == null) return;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(str);
			int index = 0;
			for (String s : splitter) {
				output[index++] = Integer.parseInt(s);
			}
		}
		private void splitFloat(String str, float[] output) {
			if (str == null) return;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(str);
			int index = 0;
			for (String s : splitter) {
				output[index++] = Float.parseFloat(s);
			}
		}
		private float getFloat(String key, float defaultValue) {
			try {
				return Float.parseFloat(mMap.get(key));
			} catch (NumberFormatException ex) {
				return defaultValue;
			}
		}
		private int getInt(String key, int defaultValue) {
			try {
				return Integer.parseInt(mMap.get(key));
			} catch (NumberFormatException ex) {
				return defaultValue;
			}
		}
		private ArrayList<Size> splitSize(String str) {
			if (str == null) return null;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(str);
			ArrayList<Size> sizeList = new ArrayList<Size>();
			for (String s : splitter) {
				Size size = strToSize(s);
				if (size != null) sizeList.add(size);
			}
			if (sizeList.size() == 0) return null;
			return sizeList;
		}
		private Size strToSize(String str) {
			if (str == null) return null;

			int pos = str.indexOf('x');
			if (pos != -1) {
				String width = str.substring(0, pos);
				String height = str.substring(pos + 1);
				return new Size(Integer.parseInt(width),
								Integer.parseInt(height));
			}
			Log.e(TAG, "Invalid size parameter string=" + str);
			return null;
		}
		private ArrayList<int[]> splitRange(String str) {
			if (str == null || str.charAt(0) != '('
					|| str.charAt(str.length() - 1) != ')') {
				Log.e(TAG, "Invalid range list string=" + str);
				return null;
			}

			ArrayList<int[]> rangeList = new ArrayList<int[]>();
			int endIndex, fromIndex = 1;
			do {
				int[] range = new int[2];
				endIndex = str.indexOf("),(", fromIndex);
				if (endIndex == -1) endIndex = str.length() - 1;
				splitInt(str.substring(fromIndex, endIndex), range);
				rangeList.add(range);
				fromIndex = endIndex + 3;
			} while (endIndex != str.length() - 1);

			if (rangeList.size() == 0) return null;
			return rangeList;
		}
		private ArrayList<Area> splitArea(String str) {
			if (str == null || str.charAt(0) != '('
					|| str.charAt(str.length() - 1) != ')') {
				Log.e(TAG, "Invalid area string=" + str);
				return null;
			}

			ArrayList<Area> result = new ArrayList<Area>();
			int endIndex, fromIndex = 1;
			int[] array = new int[5];
			do {
				endIndex = str.indexOf("),(", fromIndex);
				if (endIndex == -1) endIndex = str.length() - 1;
				splitInt(str.substring(fromIndex, endIndex), array);
				Rect rect = new Rect(array[0], array[1], array[2], array[3]);
				result.add(new Area(rect, array[4]));
				fromIndex = endIndex + 3;
			} while (endIndex != str.length() - 1);

			if (result.size() == 0) return null;

			if (result.size() == 1) {
				Area area = result.get(0);
				Rect rect = area.rect;
				if (rect.left == 0 && rect.top == 0 && rect.right == 0
						&& rect.bottom == 0 && area.weight == 0) {
					return null;
				}
			}

			return result;
		}

		private boolean same(String s1, String s2) {
			if (s1 == null && s2 == null) return true;
			if (s1 != null && s1.equals(s2)) return true;
			return false;
		}
	};
}
