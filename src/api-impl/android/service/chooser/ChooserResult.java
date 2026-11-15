package android.service.chooser;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* AOSP ChooserResult (the result reported to Intent.EXTRA_CHOOSER_RESULT_INTENT_SENDER). ATL has no system
 * sharesheet that sends one, so apps only receive instances they build themselves; fields, parceling and
 * equality follow AOSP. */
public final class ChooserResult implements Parcelable {
	public static final int CHOOSER_RESULT_UNKNOWN = -1;
	public static final int CHOOSER_RESULT_SELECTED_COMPONENT = 0;
	public static final int CHOOSER_RESULT_COPY = 1;
	public static final int CHOOSER_RESULT_EDIT = 2;

	private final int mType;
	private final ComponentName mSelectedComponent;
	private final boolean mIsShortcut;

	private ChooserResult(Parcel source) {
		mType = source.readInt();
		mSelectedComponent = ComponentName.readFromParcel(source);
		mIsShortcut = source.readBoolean();
	}

	public ChooserResult(int type, ComponentName componentName, boolean isShortcut) {
		mType = type;
		mSelectedComponent = componentName;
		mIsShortcut = isShortcut;
	}

	/* AOSP ChooserResult.isShortcut(): whether the selected target was a shortcut */
	public boolean isShortcut() {
		return mIsShortcut;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<ChooserResult> CREATOR = new Parcelable.Creator<ChooserResult>() {
		@Override
		public ChooserResult createFromParcel(Parcel source) {
			return new ChooserResult(source);
		}

		/* AOSP returns new ChooserResult[0] here (sic) */
		@Override
		public ChooserResult[] newArray(int size) {
			return new ChooserResult[0];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mType);
		ComponentName.writeToParcel(mSelectedComponent, dest);
		dest.writeBoolean(mIsShortcut);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ChooserResult that = (ChooserResult)o;
		return mType == that.mType && mIsShortcut == that.mIsShortcut && Objects.equals(mSelectedComponent, that.mSelectedComponent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mType, mSelectedComponent, mIsShortcut);
	}
}
