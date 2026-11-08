package android.os;

import android.util.ArrayMap;
import android.util.Log;

/**
 * A mapping from String keys to values of restricted types that can be
 * persisted. Taken from AOSP (android-14) without the XML persistence and the
 * lazily parcelled data, which ATL's BaseBundle does not have.
 */
public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable {
	private static final String TAG = "PersistableBundle";

	public static final PersistableBundle EMPTY;

	static {
		EMPTY = new PersistableBundle();
		EMPTY.mMap = ArrayMap.EMPTY;
	}

	/** Checks whether a value is of a type that a PersistableBundle may hold. */
	public static boolean isValidType(Object value) {
		return (value instanceof Integer) || (value instanceof Long) || (value instanceof Double) || (value instanceof String) || (value instanceof int[]) || (value instanceof long[]) || (value instanceof double[]) || (value instanceof String[]) || (value instanceof PersistableBundle) || (value == null) || (value instanceof Boolean) || (value instanceof boolean[]);
	}

	/** Constructs a new, empty PersistableBundle. */
	public PersistableBundle() {
		super();
	}

	/** Constructs a new, empty PersistableBundle sized to hold the given number of elements. */
	public PersistableBundle(int capacity) {
		super(capacity);
	}

	/** Constructs a PersistableBundle containing a copy of the mappings from the given PersistableBundle. */
	public PersistableBundle(PersistableBundle b) {
		super(b);
	}

	/**
	 * Constructs a PersistableBundle from a Bundle.
	 * @throws IllegalArgumentException if the Bundle contains a value that cannot be persisted
	 */
	public PersistableBundle(Bundle b) {
		this(b, true);
	}

	private PersistableBundle(Bundle b, boolean throwException) {
		this(b.getItemwiseMap(), throwException);
	}

	private PersistableBundle(ArrayMap<String, Object> map, boolean throwException) {
		super();
		putAll(map);

		// Now type check each item, and convert nested ArrayMaps to PersistableBundles.
		final int n = mMap.size();
		for (int i = n - 1; i >= 0; --i) {
			Object value = mMap.valueAt(i);
			if (value instanceof ArrayMap) {
				mMap.setValueAt(i, new PersistableBundle((ArrayMap<String, Object>)value, throwException));
			} else if (value instanceof Bundle) {
				mMap.setValueAt(i, new PersistableBundle((Bundle)value, throwException));
			} else if (!isValidType(value)) {
				final String errorMsg = "Bad value in PersistableBundle key=" + mMap.keyAt(i) + " value=" + value;
				if (throwException) {
					throw new IllegalArgumentException(errorMsg);
				} else {
					Log.wtf(TAG, errorMsg);
					mMap.removeAt(i);
				}
			}
		}
	}

	PersistableBundle(PersistableBundle from, boolean deep) {
		super(from, deep);
	}

	/** Make a PersistableBundle for a single key/value pair. */
	public static PersistableBundle forPair(String key, String value) {
		PersistableBundle b = new PersistableBundle(1);
		b.putString(key, value);
		return b;
	}

	/**
	 * Clones the current PersistableBundle. The internal map is cloned, but the
	 * keys and values to which it refers are copied by reference.
	 */
	@Override
	public Object clone() {
		return new PersistableBundle(this);
	}

	/** Make a deep copy of the given bundle. */
	public PersistableBundle deepCopy() {
		return new PersistableBundle(this, /* deep */ true);
	}

	/** Inserts a PersistableBundle value into the mapping of this Bundle, replacing any existing value for the given key. */
	public void putPersistableBundle(String key, PersistableBundle value) {
		mMap.put(key, value);
	}

	/** Returns the value associated with the given key, or null if no mapping of the desired type exists. */
	public PersistableBundle getPersistableBundle(String key) {
		Object o = mMap.get(key);
		if (o == null) {
			return null;
		}
		try {
			return (PersistableBundle)o;
		} catch (ClassCastException e) {
			typeWarning(key, o, "Bundle", e);
			return null;
		}
	}

	public static final Parcelable.Creator<PersistableBundle> CREATOR = new Parcelable.Creator<PersistableBundle>() {
		@Override
		public PersistableBundle createFromParcel(Parcel in) {
			return in.readPersistableBundle();
		}

		@Override
		public PersistableBundle[] newArray(int size) {
			return new PersistableBundle[size];
		}
	};

	/** Checks that no key or string value is longer than limit. */
	public boolean isBundleContentsWithinLengthLimit(int limit) {
		if (mMap == null) {
			return true;
		}
		for (int i = 0; i < mMap.size(); i++) {
			if (mMap.keyAt(i) != null && mMap.keyAt(i).length() > limit) {
				return false;
			}
			final Object value = mMap.valueAt(i);
			if (value instanceof String && ((String)value).length() > limit) {
				return false;
			} else if (value instanceof String[]) {
				String[] stringArray = (String[])value;
				for (int j = 0; j < stringArray.length; j++) {
					if (stringArray[j] != null && stringArray[j].length() > limit) {
						return false;
					}
				}
			} else if (value instanceof PersistableBundle && !((PersistableBundle)value).isBundleContentsWithinLengthLimit(limit)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeMap(mMap);
	}

	@Override
	public synchronized String toString() {
		return "PersistableBundle[" + mMap.toString() + "]";
	}

	public synchronized String toShortString() {
		return mMap.toString();
	}
}
