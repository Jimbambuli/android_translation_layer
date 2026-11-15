package android.service.autofill;

import java.util.Objects;

/* AOSP SavedDatasetsInfo (codegen data class) */
public final class SavedDatasetsInfo {

	public static final String TYPE_OTHER = "other";
	public static final String TYPE_PASSWORDS = "passwords";

	private final String mType;
	private final int mCount;

	public SavedDatasetsInfo(String type, int count) {
		this.mType = Objects.requireNonNull(type);
		if (!Objects.equals(mType, TYPE_OTHER) && !Objects.equals(mType, TYPE_PASSWORDS)) {
			throw new IllegalArgumentException("type was " + mType + " but must be one of: " + "TYPE_OTHER(" + TYPE_OTHER + "), " + "TYPE_PASSWORDS(" + TYPE_PASSWORDS + ")");
		}
		this.mCount = count;
	}

	public String getType() {
		return mType;
	}

	public int getCount() {
		return mCount;
	}

	@Override
	public String toString() {
		return "SavedDatasetsInfo { " + "type = " + mType + ", " + "count = " + mCount + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SavedDatasetsInfo that = (SavedDatasetsInfo) o;
		return Objects.equals(mType, that.mType) && mCount == that.mCount;
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + Objects.hashCode(mType);
		_hash = 31 * _hash + mCount;
		return _hash;
	}
}
