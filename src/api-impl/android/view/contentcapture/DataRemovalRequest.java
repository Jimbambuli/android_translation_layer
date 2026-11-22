package android.view.contentcapture;

import android.app.ActivityThread;
import android.content.LocusId;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* AOSP DataRemovalRequest: which content capture data of the app the service should remove */
public final class DataRemovalRequest implements Parcelable {
	public static final int FLAG_IS_PREFIX = 0x1;

	private final String mPackageName;
	private final boolean mForEverything;
	private ArrayList<LocusIdRequest> mLocusIdRequests;

	private DataRemovalRequest(Builder builder) {
		mPackageName = ActivityThread.currentActivityThread().getApplication().getPackageName();
		mForEverything = builder.mForEverything;
		if (builder.mLocusIds != null) {
			final int size = builder.mLocusIds.size();
			mLocusIdRequests = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				mLocusIdRequests.add(new LocusIdRequest(builder.mLocusIds.get(i), builder.mFlags.get(i)));
			}
		}
	}

	private DataRemovalRequest(Parcel parcel) {
		mPackageName = parcel.readString();
		mForEverything = parcel.readBoolean();
		if (!mForEverything) {
			final int size = parcel.readInt();
			mLocusIdRequests = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				mLocusIdRequests.add(new LocusIdRequest((LocusId)readValue(parcel), parcel.readInt()));
			}
		}
	}

	/* AOSP calls Parcel.readValue(null) directly; ATL's readValue() declares ReflectiveOperationException, which
	 * becomes a BadParcelableException as in Parcel.readMap() */
	private static Object readValue(Parcel parcel) {
		try {
			return parcel.readValue(null);
		} catch (ReflectiveOperationException e) {
			throw new BadParcelableException(e);
		}
	}

	public String getPackageName() {
		return mPackageName;
	}

	public boolean isForEverything() {
		return mForEverything;
	}

	public List<LocusIdRequest> getLocusIdRequests() {
		return mLocusIdRequests;
	}

	public static final class Builder {
		private boolean mForEverything;
		private ArrayList<LocusId> mLocusIds;
		private ArrayList<Integer> mFlags; // AOSP: android.util.IntArray
		private boolean mDestroyed;

		public Builder() {}

		public Builder forEverything() {
			throwIfDestroyed();
			Preconditions.checkState(mLocusIds == null, "Already added LocusIds");
			mForEverything = true;
			return this;
		}

		public Builder addLocusId(LocusId locusId, int flags) {
			throwIfDestroyed();
			Preconditions.checkState(!mForEverything, "Already is for everything");
			Objects.requireNonNull(locusId);
			if (mLocusIds == null) {
				mLocusIds = new ArrayList<>();
				mFlags = new ArrayList<>();
			}
			mLocusIds.add(locusId);
			mFlags.add(flags);
			return this;
		}

		public DataRemovalRequest build() {
			throwIfDestroyed();
			Preconditions.checkState(mForEverything || mLocusIds != null,
			                         "must call either #forEverything() or add one #addLocusId()");
			mDestroyed = true;
			return new DataRemovalRequest(this);
		}

		private void throwIfDestroyed() {
			Preconditions.checkState(!mDestroyed, "Already destroyed!");
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mPackageName);
		parcel.writeBoolean(mForEverything);
		if (!mForEverything) {
			final int size = mLocusIdRequests.size();
			parcel.writeInt(size);
			for (int i = 0; i < size; i++) {
				final LocusIdRequest request = mLocusIdRequests.get(i);
				parcel.writeValue(request.getLocusId());
				parcel.writeInt(request.getFlags());
			}
		}
	}

	public static final Parcelable.Creator<DataRemovalRequest> CREATOR = new Parcelable.Creator<DataRemovalRequest>() {
		@Override
		public DataRemovalRequest createFromParcel(Parcel parcel) {
			return new DataRemovalRequest(parcel);
		}

		@Override
		public DataRemovalRequest[] newArray(int size) {
			return new DataRemovalRequest[size];
		}
	};

	/* AOSP: an inner (non-static) class */
	public final class LocusIdRequest {
		private final LocusId mLocusId;
		private final int mFlags;

		private LocusIdRequest(LocusId locusId, int flags) {
			this.mLocusId = locusId;
			this.mFlags = flags;
		}

		public LocusId getLocusId() {
			return mLocusId;
		}

		public int getFlags() {
			return mFlags;
		}
	}
}
