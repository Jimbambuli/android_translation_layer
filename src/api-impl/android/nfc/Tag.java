package android.nfc;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

/* AOSP Tag (modules/Nfc), reduced to the mock tag (mTagService == null, the only kind AOSP's createMockTag()
 * builds). ATL has no NFC service, NfcAdapter.getDefaultAdapter() is null, so the system never delivers a Tag;
 * apps can only build mock tags. No INfcTag: getTagService()/rediscover() are not carried. */
public final class Tag implements Parcelable {
	final byte[] mId;
	final int[] mTechList;
	final Bundle[] mTechExtras;
	final int mServiceHandle;
	final long mCookie;
	int mConnectedTechnology;

	private Tag(byte[] id, int[] techList, Bundle[] techListExtras, int serviceHandle, long cookie) {
		if (techList == null) {
			throw new IllegalArgumentException("rawTargets cannot be null");
		}
		mId = id;
		mTechList = Arrays.copyOf(techList, techList.length);
		mTechExtras = Arrays.copyOf(techListExtras, techList.length);
		mServiceHandle = serviceHandle;
		mCookie = cookie;
		mConnectedTechnology = -1;
	}

	public static Tag createMockTag(byte[] id, int[] techList, Bundle[] techListExtras, long cookie) {
		return new Tag(id, techList, techListExtras, 0, cookie);
	}

	public byte[] getId() {
		return mId;
	}

	public int getServiceHandle() {
		return mServiceHandle;
	}

	/* AOSP Tag.hasTech() */
	public boolean hasTech(int techType) {
		for (int tech : mTechList) {
			if (tech == techType)
				return true;
		}
		return false;
	}

	/* AOSP Tag.getTechExtras(): the extras of the first matching technology, null if the tag lacks it */
	public Bundle getTechExtras(int tech) {
		for (int idx = 0; idx < mTechList.length; idx++) {
			if (mTechList[idx] == tech) {
				return mTechExtras[idx];
			}
		}
		return null;
	}

	/* AOSP Tag.setConnectedTechnology(): only one technology can be connected at a time */
	public synchronized boolean setConnectedTechnology(int technology) {
		if (mConnectedTechnology != -1) {
			return false;
		}
		mConnectedTechnology = technology;
		return true;
	}

	public int getConnectedTechnology() {
		return mConnectedTechnology;
	}

	public void setTechnologyDisconnected() {
		mConnectedTechnology = -1;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/* AOSP Tag.writeToParcel() for a mock tag (isMock == 1, no binder follows) */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (mId == null) {
			dest.writeInt(-1);
		} else {
			dest.writeInt(mId.length);
			dest.writeByteArray(mId);
		}
		dest.writeInt(mTechList.length);
		dest.writeIntArray(mTechList);
		dest.writeTypedArray(mTechExtras, 0);
		dest.writeInt(mServiceHandle);
		dest.writeLong(mCookie);
		dest.writeInt(1);
	}

	/* AOSP Tag.CREATOR; a parcel from a real tag (isMock == 0) carries a binder to INfcTag, which ATL has none of,
	 * so it is read as a mock tag as well */
	public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
		@Override
		public Tag createFromParcel(Parcel in) {
			int len = in.readInt();
			byte[] id = null;
			if (len >= 0) {
				id = new byte[len];
				in.readByteArray(id);
			}
			int[] techList = new int[in.readInt()];
			in.readIntArray(techList);
			Bundle[] techExtras = in.createTypedArray(Bundle.CREATOR);
			int serviceHandle = in.readInt();
			long cookie = in.readLong();
			in.readInt();
			return new Tag(id, techList, techExtras, serviceHandle, cookie);
		}

		@Override
		public Tag[] newArray(int size) {
			return new Tag[size];
		}
	};
}
