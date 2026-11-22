package android.view.textclassifier;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * This class represents the id of a text classification session.
 */
public final class TextClassificationSessionId implements Parcelable {
	@NonNull
	private final String mValue;
	@NonNull
	private final IBinder mToken;

	/**
	 * Creates a new instance.
	 *
	 * @hide
	 */
	public TextClassificationSessionId() {
		this(UUID.randomUUID().toString(), new Binder());
	}

	/**
	 * Creates a new instance.
	 *
	 * @param value The internal value.
	 *
	 * @hide
	 */
	public TextClassificationSessionId(@NonNull String value, @NonNull IBinder token) {
		mValue = Objects.requireNonNull(value);
		mToken = Objects.requireNonNull(token);
	}

	/** @hide */
	@NonNull
	public IBinder getToken() {
		return mToken;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TextClassificationSessionId that = (TextClassificationSessionId) o;
		return Objects.equals(mValue, that.mValue) && Objects.equals(mToken, that.mToken);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mValue, mToken);
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "TextClassificationSessionId {%s}", mValue);
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mValue);
		parcel.writeStrongBinder(mToken);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Returns the value of this ID.
	 */
	@NonNull
	public String getValue() {
		return mValue;
	}

	@NonNull
	public static final Parcelable.Creator<TextClassificationSessionId> CREATOR =
			new Parcelable.Creator<TextClassificationSessionId>() {
				@Override
				public TextClassificationSessionId createFromParcel(Parcel parcel) {
					return new TextClassificationSessionId(
							parcel.readString(), parcel.readStrongBinder());
				}

				@Override
				public TextClassificationSessionId[] newArray(int size) {
					return new TextClassificationSessionId[size];
				}
			};
}
