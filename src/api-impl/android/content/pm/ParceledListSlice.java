package android.content.pm;

import android.os.Parcelable;
import java.util.Collections;
import java.util.List;

/* AOSP ParceledListSlice without the Parcelable part: in ATL it only passes a list within the process */
public class ParceledListSlice<T extends Parcelable> {
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
}
