package android.hardware.camera2.impl;

import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.os.Parcel;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;
import java.util.Map;

/** @hide The parcel form of a metadata map, in place of AOSP's CameraMetadataNative.writeToParcel()/
 * readFromParcel() (which write the native camera_metadata_t): the number of entries, then per entry the key name
 * and the value. Values Parcel writes itself (boxed primitives, primitive arrays, String, Parcelables such as Rect
 * and Location) go through writeValue(); the value types of the camera keys that are no Parcelables are written
 * field by field, as AOSP's marshalers (camera2/marshal) put them into the metadata. */
public final class MetadataParcel {
	private static final int VALUE = 0;
	private static final int SIZE = 1;
	private static final int RANGE = 2;
	private static final int METERING_RECTANGLES = 3;
	private static final int RGGB_CHANNEL_VECTOR = 4;
	private static final int COLOR_SPACE_TRANSFORM = 5;
	private static final int TONEMAP_CURVE = 6;
	private static final int SIZE_F = 7;
	private static final int RATIONAL = 8;

	private MetadataParcel() {}

	/** the names of the keys of a map, for writing */
	public interface KeyNames<K> {
		String nameOf(K key);

		/** the public key of that name, or null */
		K keyNamed(String name);
	}

	public static <K> void write(Parcel dest, Map<K, Object> values, KeyNames<K> names) {
		dest.writeInt(values.size());
		for (Map.Entry<K, Object> entry : values.entrySet()) {
			dest.writeString(names.nameOf(entry.getKey()));
			writeValue(dest, entry.getValue());
		}
	}

	public static <K> void read(Parcel in, Map<K, Object> values, KeyNames<K> names) {
		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			K key = names.keyNamed(in.readString());
			Object value = readValue(in);
			if (key != null)
				values.put(key, value);
		}
	}

	private static void writeValue(Parcel dest, Object value) {
		if (value instanceof Size) {
			dest.writeInt(SIZE);
			dest.writeInt(((Size)value).getWidth());
			dest.writeInt(((Size)value).getHeight());
		} else if (value instanceof SizeF) {
			dest.writeInt(SIZE_F);
			dest.writeFloat(((SizeF)value).getWidth());
			dest.writeFloat(((SizeF)value).getHeight());
		} else if (value instanceof Rational) {
			dest.writeInt(RATIONAL);
			dest.writeInt(((Rational)value).getNumerator());
			dest.writeInt(((Rational)value).getDenominator());
		} else if (value instanceof Range) {
			dest.writeInt(RANGE);
			dest.writeValue(((Range<?>)value).getLower());
			dest.writeValue(((Range<?>)value).getUpper());
		} else if (value instanceof MeteringRectangle[]) {
			MeteringRectangle[] rects = (MeteringRectangle[])value;
			dest.writeInt(METERING_RECTANGLES);
			dest.writeInt(rects.length);
			for (MeteringRectangle r : rects) {
				dest.writeInt(r.getX());
				dest.writeInt(r.getY());
				dest.writeInt(r.getWidth());
				dest.writeInt(r.getHeight());
				dest.writeInt(r.getMeteringWeight());
			}
		} else if (value instanceof RggbChannelVector) {
			RggbChannelVector v = (RggbChannelVector)value;
			dest.writeInt(RGGB_CHANNEL_VECTOR);
			dest.writeFloat(v.getRed());
			dest.writeFloat(v.getGreenEven());
			dest.writeFloat(v.getGreenOdd());
			dest.writeFloat(v.getBlue());
		} else if (value instanceof ColorSpaceTransform) {
			int[] elements = new int[18];
			((ColorSpaceTransform)value).copyElements(elements, 0);
			dest.writeInt(COLOR_SPACE_TRANSFORM);
			dest.writeIntArray(elements);
		} else if (value instanceof TonemapCurve) {
			TonemapCurve curve = (TonemapCurve)value;
			dest.writeInt(TONEMAP_CURVE);
			for (int channel = TonemapCurve.CHANNEL_RED; channel <= TonemapCurve.CHANNEL_BLUE; channel++) {
				float[] points = new float[curve.getPointCount(channel) * TonemapCurve.POINT_SIZE];
				curve.copyColorCurve(channel, points, 0);
				dest.writeFloatArray(points);
			}
		} else {
			dest.writeInt(VALUE);
			dest.writeValue(value);
		}
	}

	/* ATL's Parcel.readValue() declares the reflective failures of a Parcelable's CREATOR */
	private static Object readObject(Parcel in) {
		try {
			return in.readValue(MetadataParcel.class.getClassLoader());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object readValue(Parcel in) {
		switch (in.readInt()) {
			case SIZE:
				return new Size(in.readInt(), in.readInt());
			case SIZE_F:
				return new SizeF(in.readFloat(), in.readFloat());
			case RATIONAL:
				return new Rational(in.readInt(), in.readInt());
			case RANGE: {
				Comparable lower = (Comparable)readObject(in);
				Comparable upper = (Comparable)readObject(in);
				return new Range(lower, upper);
			}
			case METERING_RECTANGLES: {
				MeteringRectangle[] rects = new MeteringRectangle[in.readInt()];
				for (int i = 0; i < rects.length; i++)
					rects[i] = new MeteringRectangle(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
				return rects;
			}
			case RGGB_CHANNEL_VECTOR:
				return new RggbChannelVector(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
			case COLOR_SPACE_TRANSFORM:
				return new ColorSpaceTransform(in.createIntArray());
			case TONEMAP_CURVE:
				return new TonemapCurve(in.createFloatArray(), in.createFloatArray(), in.createFloatArray());
			default:
				return readObject(in);
		}
	}
}
