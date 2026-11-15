package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* AOSP ParceledListSlice: the list is parceled as a whole (no binder for the rest of a long list, as every item of an
 * ATL parcel is in the same process); writeToParcel() writes writeParcelableList(), CREATOR reads it back */
public class ParceledListSlice<T extends Parcelable> implements Parcelable {
	private final List<T> mList;

	public ParceledListSlice(List<T> list) {
		mList = list;
	}

	public static <T extends Parcelable> ParceledListSlice<T> emptyList() {
		return new ParceledListSlice<T>(Collections.<T>emptyList());
	}

	public List<T> getList() {
		return mList;
	}

	/* limits how many items AOSP writes inline into the parcel; nothing is parceled here */
	public void setInlineCountLimit(int maxCount) {}

	@Override
	public int describeContents() {
		int contents = 0;
		for (int i = 0; i < mList.size(); i++)
			contents |= mList.get(i).describeContents();
		return contents;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelableList(mList, flags);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final Parcelable.Creator<ParceledListSlice> CREATOR = new Parcelable.Creator<ParceledListSlice>() {
		@Override
		public ParceledListSlice createFromParcel(Parcel in) {
			return new ParceledListSlice(in.readParcelableList(new ArrayList(), null));
		}

		@Override
		public ParceledListSlice[] newArray(int size) {
			return new ParceledListSlice[size];
		}
	};
}
