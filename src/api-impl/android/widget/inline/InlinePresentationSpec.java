package android.widget.inline;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import java.util.Objects;

/**
 * android-14 frameworks/base core/java/android/widget/inline/InlinePresentationSpec.java (codegen data class):
 * size range and style of an inline autofill suggestion. Default style = empty Bundle (AOSP defaultStyle()).
 */
public final class InlinePresentationSpec implements Parcelable {

	private final Size mMinSize;
	private final Size mMaxSize;
	private final Bundle mStyle;

	InlinePresentationSpec(Size minSize, Size maxSize, Bundle style) {
		mMinSize = Objects.requireNonNull(minSize);
		mMaxSize = Objects.requireNonNull(maxSize);
		mStyle = Objects.requireNonNull(style);
	}

	public Size getMinSize() {
		return mMinSize;
	}

	public Size getMaxSize() {
		return mMaxSize;
	}

	public Bundle getStyle() {
		return mStyle;
	}

	@Override
	public String toString() {
		return "InlinePresentationSpec { minSize = " + mMinSize + ", maxSize = " + mMaxSize + ", style = " + mStyle + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		InlinePresentationSpec that = (InlinePresentationSpec)o;
		return Objects.equals(mMinSize, that.mMinSize) && Objects.equals(mMaxSize, that.mMaxSize) && Objects.equals(mStyle, that.mStyle);
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + Objects.hashCode(mMinSize);
		_hash = 31 * _hash + Objects.hashCode(mMaxSize);
		_hash = 31 * _hash + Objects.hashCode(mStyle);
		return _hash;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mMinSize.getWidth());
		dest.writeInt(mMinSize.getHeight());
		dest.writeInt(mMaxSize.getWidth());
		dest.writeInt(mMaxSize.getHeight());
		dest.writeBundle(mStyle);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<InlinePresentationSpec> CREATOR = new Parcelable.Creator<InlinePresentationSpec>() {
		@Override
		public InlinePresentationSpec[] newArray(int size) {
			return new InlinePresentationSpec[size];
		}

		@Override
		public InlinePresentationSpec createFromParcel(Parcel in) {
			Size min = new Size(in.readInt(), in.readInt());
			Size max = new Size(in.readInt(), in.readInt());
			return new InlinePresentationSpec(min, max, in.readBundle());
		}
	};

	public static final class Builder {
		private Size mMinSize;
		private Size mMaxSize;
		private Bundle mStyle;
		private boolean mBuilt;

		public Builder(Size minSize, Size maxSize) {
			mMinSize = Objects.requireNonNull(minSize);
			mMaxSize = Objects.requireNonNull(maxSize);
		}

		public Builder setStyle(Bundle value) {
			checkNotUsed();
			mStyle = value;
			return this;
		}

		public InlinePresentationSpec build() {
			checkNotUsed();
			mBuilt = true;
			if (mStyle == null)
				mStyle = Bundle.EMPTY;
			return new InlinePresentationSpec(mMinSize, mMaxSize, mStyle);
		}

		private void checkNotUsed() {
			if (mBuilt) {
				throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
			}
		}
	}
}
