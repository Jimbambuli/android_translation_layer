package android.os;

import android.atl.ATLLoadedApp;
import android.atl.annotation.Export;
import android.content.Context;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parcel {

	private DataInputStream is;
	private DataOutputStream os;
	private byte[] data;
	private int pos;
	private int size;
	private boolean hasFileDescriptors;
	private ArrayList<IBinder> binders;
	private ArrayList<FileDescriptor> fds;
	/* received from another process: the parcel owns its descriptors; the first read takes one over */
	private boolean[] fdTaken;
	/* descriptors to close once the parcel is sent (PARCELABLE_WRITE_RETURN_VALUE) */
	private ArrayList<FileDescriptor> fdsToClose;

	protected Parcel() {
		data = new byte[0];
		is = new DataInputStream(new InputStream() {
			@Override
			public int read() {
				return readByte() & 0xff;
			}
		});
		os = new DataOutputStream(new OutputStream() {
			@Override
			public void write(int b) {
				writeByte((byte)b);
			}
		});
	}

	public static Parcel obtain() {
		return new Parcel();
	}

	public void recycle() {}

	public void writeByte(byte value) {
		if (pos >= data.length)
			data = Arrays.copyOf(data, data.length * 2 + 8);
		data[pos++] = (byte)value;
		if (pos > size)
			size = pos;
	}

	public byte readByte() {
		if (pos < size)
			return data[pos++];
		// like AOSP, reading past the end yields zeros. Apps rely on this, e.g. when a newer client
		// reads trailing fields which an older implementation of the other side doesn't write
		return 0;
	}

	public void writeInt(int value) {
		try {
			os.writeInt(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int readInt() {
		try {
			return is.readInt();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeShort(short value) {
		try {
			os.writeShort(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public short readShort() {
		try {
			return is.readShort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeLong(long value) {
		try {
			os.writeLong(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public long readLong() {
		try {
			return is.readLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeFloat(float value) {
		try {
			os.writeFloat(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public float readFloat() {
		try {
			return is.readFloat();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeDouble(double value) {
		try {
			os.writeDouble(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public double readDouble() {
		try {
			return is.readDouble();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/* like AOSP, a boolean is an int: GMS SafeParcelReader reads typed arrays and lists through
	 * createTypedArray(), whose items start with readInt() != 0 (AOSP readTypedObject()) */
	public void writeBoolean(boolean value) {
		writeInt(value ? 1 : 0);
	}

	public boolean readBoolean() {
		return readInt() != 0;
	}

	public void writeIntArray(int[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeInt(value[i]);
	}

	public void readIntArray(int[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readInt();
	}

	public int[] createIntArray() {
		int len = readInt();
		if (len == -1)
			return null;
		int[] value = new int[len];
		for (int i = 0; i < len; i++)
			value[i] = readInt();
		return value;
	}

	public void writeLongArray(long[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeLong(value[i]);
	}

	public void readLongArray(long[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readLong();
	}

	public long[] createLongArray() {
		int len = readInt();
		if (len == -1)
			return null;
		long[] value = new long[len];
		for (int i = 0; i < len; i++)
			value[i] = readLong();
		return value;
	}

	public void writeByteArray(byte[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeByte(value[i]);
	}

	public void writeByteArray(byte[] value, int offset, int length) {
		if (value == null) {
			writeInt(-1);
			return;
		}
		if (offset < 0 || length < 0 || offset + length > value.length)
			throw new ArrayIndexOutOfBoundsException();
		writeInt(length);
		// Copy AOSP bug to pass CTS test
		if (ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo.targetSdkVersion < 11)
			offset = 0;
		for (int i = 0; i < length; i++)
			writeByte(value[offset + i]);
	}

	public void readByteArray(byte[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readByte();
	}

	public byte[] createByteArray() {
		int len = readInt();
		if (len == -1)
			return null;
		byte[] value = new byte[len];
		for (int i = 0; i < len; i++)
			value[i] = readByte();
		return value;
	}

	public void writeCharArray(char[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeShort((short)value[i]);
	}

	public void readCharArray(char[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = (char)readShort();
	}

	public char[] createCharArray() {
		int len = readInt();
		if (len == -1)
			return null;
		char[] value = new char[len];
		for (int i = 0; i < len; i++)
			value[i] = (char)readShort();
		return value;
	}

	public void writeBooleanArray(boolean[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeBoolean(value[i]);
	}

	public void readBooleanArray(boolean[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readBoolean();
	}

	public boolean[] createBooleanArray() {
		int len = readInt();
		if (len == -1)
			return null;
		boolean[] value = new boolean[len];
		for (int i = 0; i < len; i++)
			value[i] = readBoolean();
		return value;
	}

	public void writeFloatArray(float[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeFloat(value[i]);
	}

	public void readFloatArray(float[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readFloat();
	}

	public float[] createFloatArray() {
		int len = readInt();
		if (len == -1)
			return null;
		float[] value = new float[len];
		for (int i = 0; i < len; i++)
			value[i] = readFloat();
		return value;
	}

	public void writeDoubleArray(double[] value) {
		int len = value == null ? -1 : value.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeDouble(value[i]);
	}

	public void readDoubleArray(double[] value) {
		int len = readInt();
		if (value.length != len)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < len; i++)
			value[i] = readDouble();
	}

	public double[] createDoubleArray() {
		int len = readInt();
		if (len == -1)
			return null;
		double[] value = new double[len];
		for (int i = 0; i < len; i++)
			value[i] = readDouble();
		return value;
	}

	public void writeString(String value) {
		try {
			if (value == null)
				os.writeShort(-1);
			else
				os.writeUTF(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String readString() {
		if (pos + 2 > size)
			return null; // past the end, like AOSP
		try {
			int len = is.readShort();
			if (len == -1)
				return null;
			pos -= 2;
			return is.readUTF();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeBundle(Bundle value) {
		writeMap(value == null ? null : value.mMap);
	}

	public Bundle readBundle(ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		if (size == -1)
			return null;
		Bundle bundle = new Bundle();
		for (int i = 0; i < size; i++) {
			String key = readString();
			Object value = readValue(loader);
			bundle.mMap.put(key, value);
		}
		return bundle;
	}

	/** As in android-14: a Bundle that cannot be read throws the unchecked BadParcelableException. */
	public Bundle readBundle() {
		try {
			return readBundle(getClass().getClassLoader());
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
	}

	/** Flatten a PersistableBundle into the parcel at the current dataPosition(); null is written as -1. */
	public final void writePersistableBundle(PersistableBundle val) {
		writeMap(val == null ? null : val.mMap);
	}

	/** Read and return a new PersistableBundle object from the parcel at the current dataPosition(), or null. */
	public final PersistableBundle readPersistableBundle() {
		return readPersistableBundle(null);
	}

	/** Same as {@link #readPersistableBundle()} but explicitly specifying the ClassLoader. */
	public final PersistableBundle readPersistableBundle(ClassLoader loader) {
		int size = readInt();
		if (size == -1)
			return null;
		PersistableBundle bundle = new PersistableBundle(size);
		try {
			for (int i = 0; i < size; i++) {
				String key = readString();
				bundle.mMap.put(key, readValue(loader));
			}
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
		return bundle;
	}

	public void writeInterfaceToken(String value) {
		writeString(value);
	}

	public void enforceInterface(String value) {
		if (!value.equals(readString()))
			throw new SecurityException("Interface mismatch");
	}

	@Export
	public void writeParcelable(Parcelable p, int flags) {
		writeString(p != null ? p.getClass().getName() : null);
		if (p != null)
			p.writeToParcel(this, flags);
	}

	@Export
	public Parcelable readParcelable(ClassLoader loader) {
		String className = readString();
		if (className == null)
			return null;
		try {
			Parcelable.Creator<?> creator = (Parcelable.Creator<?>)loader.loadClass(className).getField("CREATOR").get(null);
			return (Parcelable)creator.createFromParcel(this);
		} catch (ReflectiveOperationException | IllegalArgumentException e) {
			throw new BadParcelableException(e);
		}
	}

	/** android-14 Parcel.writeString8(): a null-safe UTF-8 string (here the same wire format as writeString()). */
	public void writeString8(String val) {
		writeString(val);
	}

	public String readString8() {
		return readString();
	}

	/** android-14 Parcel.writeString8Array(): the same wire format as writeStringArray(). */
	public void writeString8Array(String[] val) {
		writeStringArray(val);
	}

	public String[] createString8Array() {
		return createStringArray();
	}

	/** android-14 Parcel.writeCharSequence(): the same format as TextUtils.writeToParcel(). */
	public void writeCharSequence(CharSequence val) {
		TextUtils.writeToParcel(val, this, 0);
	}

	public CharSequence readCharSequence() {
		return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(this);
	}

	/** android-14 Parcel.writeCharSequenceArray(): the length (-1 for null), then each element as writeCharSequence(). */
	public void writeCharSequenceArray(CharSequence[] val) {
		if (val != null) {
			writeInt(val.length);
			for (CharSequence cs : val) {
				writeCharSequence(cs);
			}
		} else {
			writeInt(-1);
		}
	}

	public CharSequence[] readCharSequenceArray() {
		int n = readInt();
		if (n < 0) {
			return null;
		}
		CharSequence[] array = new CharSequence[n];
		for (int i = 0; i < n; i++) {
			array[i] = readCharSequence();
		}
		return array;
	}

	/** android-14 Parcel.writeArraySet(): the size (-1 for null), then each value as writeValue(). */
	public void writeArraySet(android.util.ArraySet<? extends Object> val) {
		int size = val != null ? val.size() : -1;
		writeInt(size);
		for (int i = 0; i < size; i++) {
			writeValue(val.valueAt(i));
		}
	}

	public android.util.ArraySet<? extends Object> readArraySet(ClassLoader loader) {
		int size = readInt();
		if (size < 0) {
			return null;
		}
		android.util.ArraySet<Object> set = new android.util.ArraySet<>(size);
		try {
			for (int i = 0; i < size; i++) {
				set.add(readValue(loader));
			}
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
		return set;
	}

	/** android-14 Parcel.writeTypedObject(): a presence flag, then the object. */
	public <T extends Parcelable> void writeTypedObject(T val, int parcelableFlags) {
		if (val != null) {
			writeInt(1);
			val.writeToParcel(this, parcelableFlags);
		} else {
			writeInt(0);
		}
	}

	public <T> T readTypedObject(Parcelable.Creator<T> c) {
		if (readInt() != 0) {
			return c.createFromParcel(this);
		} else {
			return null;
		}
	}

	/** android-14 Parcel.readParcelable(ClassLoader, Class): the result must be of the requested type. */
	public <T> T readParcelable(ClassLoader loader, Class<T> clazz) {
		Parcelable p = readParcelable(loader != null ? loader : getClass().getClassLoader());
		if (p != null && !clazz.isInstance(p)) {
			throw new BadParcelableException("Parcelable " + p.getClass().getName() + " is not a " + clazz.getName());
		}
		return (T)p;
	}

	public void writeStringArray(String[] strings) {
		int len = strings == null ? -1 : strings.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeString(strings[i]);
	}

	public void readStringArray(String[] strings) {
		int size = readInt();
		if (strings.length != size)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < size; i++)
			strings[i] = readString();
	}

	/** android-14 Parcel.readStringArray(): same as createStringArray(). */
	public String[] readStringArray() {
		return createStringArray();
	}

	public String[] createStringArray() {
		int len = readInt();
		if (len == -1)
			return null;
		String[] strings = new String[len];
		for (int i = 0; i < len; i++)
			strings[i] = readString();
		return strings;
	}

	public void writeStringList(List<String> strings) {
		int len = strings == null ? -1 : strings.size();
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeString(strings.get(i));
	}

	public void readStringList(List<String> strings) {
		int size = readInt();
		strings.clear();
		for (int i = 0; i < size; i++)
			strings.add(readString());
	}

	public ArrayList<String> createStringArrayList() {
		int len = readInt();
		if (len == -1)
			return null;
		ArrayList<String> strings = new ArrayList<String>(len);
		for (int i = 0; i < len; i++)
			strings.add(readString());
		return strings;
	}

	public void writeList(List<?> list) {
		int len = list == null ? -1 : list.size();
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeValue(list.get(i));
	}

	public void readList(List<Object> list, ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		list.clear();
		for (int i = 0; i < size; i++)
			list.add(readValue(loader));
	}

	public void writeMap(Map<String, ?> map) {
		if (map == null) {
			writeInt(-1);
			return;
		}
		writeInt(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			writeString(entry.getKey());
			writeValue(entry.getValue());
		}
	}

	/* AOSP Parcel.readMap(): no checked exception, a class that cannot be loaded is a BadParcelableException */
	public final void readMap(Map outVal, ClassLoader loader) {
		int size = readInt();
		outVal.clear();
		try {
			for (int i = 0; i < size; i++) {
				String key = readString();
				Object value = readValue(loader);
				outVal.put(key, value);
			}
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
	}

	public HashMap<String, Object> readHashMap(ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		if (size == -1)
			return null;
		HashMap<String, Object> map = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			String key = readString();
			Object value = readValue(loader);
			map.put(key, value);
		}
		return map;
	}

	public void writeSparseArray(SparseArray<?> array) {
		int len = array == null ? -1 : array.size();
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeInt(array.keyAt(i));
			writeValue(array.valueAt(i));
		}
	}

	/* AOSP Parcel.readSparseArray(): no checked exception, a class that cannot be loaded is a BadParcelableException */
	public <T> SparseArray<T> readSparseArray(ClassLoader loader) {
		int size = readInt();
		if (size == -1)
			return null;
		SparseArray<T> array = new SparseArray<>(size);
		try {
			for (int i = 0; i < size; i++) {
				int key = readInt();
				T value = (T)readValue(loader);
				array.put(key, value);
			}
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
		return array;
	}

	public void writeSparseBooleanArray(SparseBooleanArray array) {
		int len = array == null ? -1 : array.size();
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeInt(array.keyAt(i));
			writeBoolean(array.valueAt(i));
		}
	}

	public SparseBooleanArray readSparseBooleanArray() {
		int size = readInt();
		if (size == -1)
			return null;
		SparseBooleanArray array = new SparseBooleanArray(size);
		for (int i = 0; i < size; i++) {
			int key = readInt();
			boolean value = readBoolean();
			array.put(key, value);
		}
		return array;
	}

	public void writeParcelableArray(Parcelable[] array, int flags) {
		int len = array == null ? -1 : array.length;
		writeInt(len);
		for (int i = 0; i < len; i++)
			writeParcelable(array[i], flags);
	}

	public Parcelable[] readParcelableArray(ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		if (size == -1)
			return null;
		Parcelable[] array = new Parcelable[size];
		for (int i = 0; i < size; i++)
			array[i] = readParcelable(loader);
		return array;
	}

	public void writeTypedArray(Parcelable[] array, int flags) {
		int len = array == null ? -1 : array.length;
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeBoolean(array[i] != null);
			if (array[i] != null)
				array[i].writeToParcel(this, flags);
		}
	}

	public void readTypedArray(Object[] array, Parcelable.Creator<Parcelable> creator) throws ReflectiveOperationException {
		int size = readInt();
		if (array.length != size)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < size; i++) {
			if (readBoolean())
				array[i] = creator.createFromParcel(this);
			else
				array[i] = null;
		}
	}

	public <T> T[] createTypedArray(Parcelable.Creator<T> creator) {
		int size = readInt();
		if (size == -1)
			return null;
		T[] array = creator.newArray(size);
		for (int i = 0; i < size; i++) {
			if (readBoolean())
				array[i] = creator.createFromParcel(this);
		}
		return array;
	}

	/**
	 * Flatten a List containing arbitrary Parcelable objects into the parcel at the current dataPosition()
	 * (a null list as -1); read back with {@link #readParcelableList(List, ClassLoader, Class)}.
	 */
	public <T extends Parcelable> void writeParcelableList(List<T> val, int flags) {
		if (val == null) {
			writeInt(-1);
			return;
		}
		int n = val.size();
		writeInt(n);
		for (int i = 0; i < n; i++)
			writeParcelable(val.get(i), flags);
	}

	/**
	 * Read into the given List items Parcelable objects that were written with writeParcelableList().
	 * Existing items of the list are reused as far as it reaches, the rest is added or removed, as in AOSP.
	 * @return the list that was passed in (cleared if a null list was written)
	 */
	public <T> List<T> readParcelableList(List<T> list, ClassLoader cl, Class<? extends T> clazz) {
		int n = readInt();
		if (n == -1) {
			list.clear();
			return list;
		}
		int i = 0;
		for (; i < n; i++) {
			T item = (T)readParcelable(cl, clazz);
			if (i < list.size())
				list.set(i, item);
			else
				list.add(item);
		}
		for (; i < list.size(); i++)
			list.remove(list.size() - 1);
		return list;
	}

	public <T extends Parcelable> void writeTypedList(List<T> list) {
		int len = list == null ? -1 : list.size();
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeBoolean(list.get(i) != null);
			if (list.get(i) != null)
				list.get(i).writeToParcel(this, 0);
		}
	}

	public <T> void readTypedList(List<T> list, Parcelable.Creator<T> creator) {
		int size = readInt();
		list.clear();
		for (int i = 0; i < size; i++) {
			if (readBoolean())
				list.add(creator.createFromParcel(this));
			else
				list.add(null);
		}
	}

	public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> creator) {
		int size = readInt();
		if (size == -1)
			return null;
		ArrayList<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			if (readBoolean())
				list.add(creator.createFromParcel(this));
			else
				list.add(null);
		}
		return list;
	}

	public void writeArray(Object[] array) {
		int len = array == null ? -1 : array.length;
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeValue(array[i]);
		}
	}

	public Object[] readArray(ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		if (size == -1)
			return null;
		Object[] array = new Object[size];
		for (int i = 0; i < size; i++)
			array[i] = readValue(loader);
		return array;
	}

	public ArrayList<Object> readArrayList(ClassLoader loader) throws ReflectiveOperationException {
		int size = readInt();
		if (size == -1)
			return null;
		ArrayList<Object> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			list.add(readValue(loader));
		return list;
	}

	public void writeSerializable(Serializable serializable) {
		try {
			new ObjectOutputStream(os).writeObject(serializable);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Serializable readSerializable() throws ReflectiveOperationException {
		try {
			return (Serializable) new ObjectInputStream(is).readObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read a new Serializable object from a Parcel, as in android-14: the class is looked up with the given
	 * loader (if not null), and an object that is not an instance of clazz throws a BadParcelableException.
	 */
	public <T> T readSerializable(ClassLoader loader, Class<T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		Object object;
		try {
			object = new ObjectInputStream(is) {
				@Override
				protected Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					if (loader != null) {
						try {
							return Class.forName(desc.getName(), false, loader);
						} catch (ClassNotFoundException e) {
							// fall back to the default lookup
						}
					}
					return super.resolveClass(desc);
				}
			}.readObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Parcelable encountered ClassNotFoundException reading a Serializable object (name = " + e.getMessage() + ")", e);
		}
		if (object != null && !clazz.isInstance(object)) {
			throw new BadParcelableException("Serializable object " + object.getClass().getName() + " is not a subclass of required class " + clazz.getName() + " provided in the parameter");
		}
		return clazz.cast(object);
	}

	public void writeException(Exception e) throws Exception {
		if (!(e instanceof BadParcelableException || e instanceof IllegalArgumentException || e instanceof IllegalStateException || e instanceof NullPointerException || e instanceof SecurityException || e instanceof UnsupportedOperationException))
			throw e;
		writeSerializable(e);
	}

	public void writeNoException() {
		writeSerializable(null);
	}

	public void readException() {
		if (pos >= size)
			return;
		RuntimeException e;
		try {
			e = (RuntimeException)readSerializable();
		} catch (ReflectiveOperationException exception) {
			throw new RuntimeException(exception);
		}
		if (e != null)
			throw e;
	}

	/* like the binders, the descriptors are kept beside the data (the data holds the index), so that they
	 * can be passed to another process of the app (ATLBinderConnection) */
	public void writeFileDescriptor(FileDescriptor fd) {
		if (fd == null) {
			writeInt(-1);
			return;
		}
		if (fds == null)
			fds = new ArrayList<>();
		fds.add(fd);
		writeInt(fds.size() - 1);
		hasFileDescriptors = true;
	}

	/* AOSP android_os_Parcel_readFileDescriptor(): the reader gets its own duplicate */
	public ParcelFileDescriptor readFileDescriptor() {
		if (!hasFileDescriptors)
			return null;
		int index = readInt();
		if (index < 0 || fds == null || index >= fds.size())
			return null;
		if (fdTaken != null && !fdTaken[index]) {
			fdTaken[index] = true;
			return new ParcelFileDescriptor(fds.get(index));
		}
		try {
			return ParcelFileDescriptor.dup(fds.get(index));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean hasFileDescriptors() {
		return hasFileDescriptors;
	}

	public void writeStrongBinder(IBinder binder) {
		if (binder != null) {
			if (binders == null)
				binders = new ArrayList<>();
			binders.add(binder);
			writeInt(binders.size() - 1);
		} else {
			writeInt(-1);
		}
	}

	public IBinder readStrongBinder() {
		int index = readInt();
		if (index != -1)
			return binders.get(index);
		else
			return null;
	}

	public void writeStrongInterface(IInterface binder) {
		writeStrongBinder(binder == null ? null : binder.asBinder());
	}

	/** @hide the binders and file descriptors the data refers to by index, for ATLBinderConnection */
	public ArrayList<IBinder> atl_getBinders() {
		return binders;
	}

	/** @hide */
	public ArrayList<FileDescriptor> atl_getFileDescriptors() {
		return fds;
	}

	/** @hide a parcel received from another process, which owns the descriptors */
	public void atl_setObjects(ArrayList<IBinder> binders, ArrayList<FileDescriptor> fds) {
		this.binders = binders;
		this.fds = fds;
		hasFileDescriptors = fds != null && !fds.isEmpty();
		fdTaken = fds != null ? new boolean[fds.size()] : null;
	}

	/** @hide the received descriptors nobody read */
	public ArrayList<FileDescriptor> atl_untakenFileDescriptors() {
		ArrayList<FileDescriptor> ret = new ArrayList<>();
		for (int i = 0; fdTaken != null && i < fdTaken.length; i++)
			if (!fdTaken[i])
				ret.add(fds.get(i));
		return ret;
	}

	/** @hide writeFileDescriptor() of a descriptor the parcel closes once it is sent */
	public void atl_writeOwnedFileDescriptor(FileDescriptor fd) {
		writeFileDescriptor(fd);
		if (fdsToClose == null)
			fdsToClose = new ArrayList<>();
		fdsToClose.add(fd);
	}

	/** @hide */
	public ArrayList<FileDescriptor> atl_fileDescriptorsToClose() {
		return fdsToClose;
	}

	public void writeBinderArray(IBinder[] array) {
		int len = array == null ? -1 : array.length;
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeStrongBinder(array[i]);
		}
	}

	public void readBinderArray(IBinder[] array) {
		int size = readInt();
		if (array.length != size)
			throw new RuntimeException("array length mismatch");
		for (int i = 0; i < size; i++)
			array[i] = readStrongBinder();
	}

	public IBinder[] createBinderArray() {
		int size = readInt();
		if (size == -1)
			return null;
		IBinder[] array = new IBinder[size];
		for (int i = 0; i < size; i++)
			array[i] = readStrongBinder();
		return array;
	}

	public void writeBinderList(List<IBinder> list) {
		int len = list == null ? -1 : list.size();
		writeInt(len);
		for (int i = 0; i < len; i++) {
			writeStrongBinder(list.get(i));
		}
	}

	public void readBinderList(List<IBinder> list) {
		int size = readInt();
		list.clear();
		for (int i = 0; i < size; i++)
			list.add(readStrongBinder());
	}

	public ArrayList<IBinder> createBinderArrayList() {
		int size = readInt();
		if (size == -1)
			return null;
		ArrayList<IBinder> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			list.add(readStrongBinder());
		return list;
	}

	@SuppressWarnings("unchecked")
	public void writeValue(Object value) {
		if (value == null) {
			writeInt(0);
		} else if (value instanceof Byte) {
			writeInt(1);
			writeByte(((Byte)value).byteValue());
		} else if (value instanceof Short) {
			writeInt(2);
			writeShort(((Short)value).shortValue());
		} else if (value instanceof Integer) {
			writeInt(3);
			writeInt(((Integer)value).intValue());
		} else if (value instanceof Long) {
			writeInt(4);
			writeLong(((Long)value).longValue());
		} else if (value instanceof Float) {
			writeInt(5);
			writeFloat(((Float)value).floatValue());
		} else if (value instanceof Double) {
			writeInt(6);
			writeDouble(((Double)value).doubleValue());
		} else if (value instanceof Boolean) {
			writeInt(7);
			writeBoolean(((Boolean)value).booleanValue());
		} else if (value instanceof String) {
			writeInt(8);
			writeString((String)value);
		} else if (value instanceof byte[]) {
			writeInt(9);
			writeByteArray((byte[])value);
		} else if (value instanceof int[]) {
			writeInt(10);
			writeIntArray((int[])value);
		} else if (value instanceof long[]) {
			writeInt(11);
			writeLongArray((long[])value);
		} else if (value instanceof boolean[]) {
			writeInt(12);
			writeBooleanArray((boolean[])value);
		} else if (value instanceof String[]) {
			writeInt(13);
			writeStringArray((String[])value);
		} else if (value instanceof Bundle) {
			writeInt(14);
			writeBundle((Bundle)value);
		} else if (value instanceof PersistableBundle) {
			writeInt(25);
			writePersistableBundle((PersistableBundle)value);
		} else if (value instanceof Parcelable) {
			writeInt(15);
			writeParcelable((Parcelable)value, 0);
		} else if (value instanceof Parcelable[]) {
			writeInt(16);
			writeParcelableArray((Parcelable[])value, 0);
		} else if (value instanceof Object[]) {
			writeInt(17);
			writeArray((Object[])value);
		} else if (value instanceof List) {
			writeInt(18);
			writeList((List<?>)value);
		} else if (value instanceof Map) {
			writeInt(19);
			writeMap((Map<String, ?>)value);
		} else if (value instanceof SparseArray) {
			writeInt(20);
			writeSparseArray((SparseArray<?>)value);
		} else if (value instanceof Binder) {
			writeInt(21);
		} else if (value instanceof Serializable) {
			writeInt(22);
			writeSerializable((Serializable)value);
		} else {
			throw new RuntimeException("Unsupported value type: " + value.getClass().getName());
		}
	}

	public Object readValue(ClassLoader loader) throws ReflectiveOperationException {
		int type;
		switch (type = readInt()) {
			case 0:
				return null;
			case 1:
				return readByte();
			case 2:
				return readShort();
			case 3:
				return readInt();
			case 4:
				return readLong();
			case 5:
				return readFloat();
			case 6:
				return readDouble();
			case 7:
				return readBoolean();
			case 8:
				return readString();
			case 9:
				return createByteArray();
			case 10:
				return createIntArray();
			case 11:
				return createLongArray();
			case 12:
				return createBooleanArray();
			case 13:
				return createStringArray();
			case 14:
				return readBundle(loader);
			case 15:
				return readParcelable(loader);
			case 16:
				return readParcelableArray(loader);
			case 17:
				return readArray(loader);
			case 18:
				return readArrayList(loader);
			case 19:
				return readHashMap(loader);
			case 20:
				return readSparseArray(loader);
			case 21:
				return new Binder();
			case 22:
				return readSerializable();
			case 25:
				return readPersistableBundle(loader);
			default:
				throw new RuntimeException("Unsupported value type: " + type);
		}
	}

	public void setDataPosition(int pos) {
		this.pos = pos;
	}

	public int dataPosition() {
		return pos;
	}

	public void setDataSize(int size) {
		this.size = size;
	}

	public int dataSize() {
		return size;
	}

	public int dataAvail() {
		return size - pos;
	}

	public void setDataCapacity(int capacity) {
		if (capacity > data.length)
			data = Arrays.copyOf(data, capacity);
	}

	public int dataCapacity() {
		return data.length;
	}

	public void appendFrom(Parcel source, int start, int size) {
		try {
			os.write(source.data, start, size);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] marshall() {
		return Arrays.copyOf(data, size);
	}

	public void unmarshall(byte[] data, int offset, int length) {
		this.data = data;
		this.pos = offset;
		this.size = offset + length;
	}

	/* AOSP Parcel.readValue(ClassLoader, Class) type check: a value of another class is a BadParcelableException */
	private static <T> T checkValueType(Object object, Class<T> clazz) {
		if (object != null && clazz != null && !clazz.isInstance(object)) {
			throw new BadParcelableException("Unparcelled object " + object + " is not an instance of required class "
			                                 + clazz.getName() + " provided in the parameter");
		}
		return (T)object;
	}

	private Object readValueUnchecked(ClassLoader loader) {
		try {
			return readValue(loader);
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
	}

	/* The type-safe readers of AOSP Parcel (API 33): same wire format as the untyped ones, plus the type check. */
	public <T> void readList(List<? super T> outVal, ClassLoader loader, Class<T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		int size = readInt();
		outVal.clear();
		for (int i = 0; i < size; i++)
			outVal.add(checkValueType(readValueUnchecked(loader), clazz));
	}

	public <T> ArrayList<T> readArrayList(ClassLoader loader, Class<? extends T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		int size = readInt();
		if (size == -1)
			return null;
		ArrayList<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			list.add(checkValueType(readValueUnchecked(loader), clazz));
		return list;
	}

	public <T> T[] readArray(ClassLoader loader, Class<T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		int size = readInt();
		if (size == -1)
			return null;
		T[] array = (T[])java.lang.reflect.Array.newInstance(clazz, size);
		for (int i = 0; i < size; i++)
			array[i] = checkValueType(readValueUnchecked(loader), clazz);
		return array;
	}

	public <K, V> HashMap<K, V> readHashMap(ClassLoader loader, Class<? extends K> clazzKey, Class<? extends V> clazzValue) {
		java.util.Objects.requireNonNull(clazzKey);
		java.util.Objects.requireNonNull(clazzValue);
		int size = readInt();
		if (size == -1)
			return null;
		HashMap<K, V> map = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			K key = checkValueType(readString(), clazzKey);
			V value = checkValueType(readValueUnchecked(loader), clazzValue);
			map.put(key, value);
		}
		return map;
	}

	public <K, V> void readMap(Map<? super K, ? super V> outVal, ClassLoader loader, Class<K> clazzKey, Class<V> clazzValue) {
		java.util.Objects.requireNonNull(clazzKey);
		java.util.Objects.requireNonNull(clazzValue);
		int size = readInt();
		outVal.clear();
		for (int i = 0; i < size; i++) {
			K key = checkValueType(readString(), clazzKey);
			V value = checkValueType(readValueUnchecked(loader), clazzValue);
			outVal.put(key, value);
		}
	}

	public <T> SparseArray<T> readSparseArray(ClassLoader loader, Class<? extends T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		int size = readInt();
		if (size == -1)
			return null;
		SparseArray<T> array = new SparseArray<>(size);
		for (int i = 0; i < size; i++) {
			int key = readInt();
			array.put(key, checkValueType(readValueUnchecked(loader), clazz));
		}
		return array;
	}

	public <T> T[] readParcelableArray(ClassLoader loader, Class<T> clazz) {
		java.util.Objects.requireNonNull(clazz);
		int size = readInt();
		if (size == -1)
			return null;
		T[] array = (T[])java.lang.reflect.Array.newInstance(clazz, size);
		for (int i = 0; i < size; i++)
			array[i] = readParcelable(loader, clazz);
		return array;
	}

	/* AOSP Parcel.readParcelableList(List, ClassLoader): readParcelableListInternal without a class check */
	public <T extends Parcelable> List<T> readParcelableList(List<T> list, ClassLoader cl) {
		int n = readInt();
		if (n == -1) {
			list.clear();
			return list;
		}
		int i = 0;
		for (; i < n; i++) {
			T item = (T)readParcelable(cl);
			if (i < list.size())
				list.set(i, item);
			else
				list.add(item);
		}
		for (; i < list.size(); i++)
			list.remove(list.size() - 1);
		return list;
	}
}
