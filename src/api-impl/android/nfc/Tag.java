package android.nfc;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcBarcode;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import java.io.IOException;
import java.util.Arrays;

/* AOSP Tag (modules/Nfc), reduced to the mock tag (mTagService == null, the only kind AOSP's createMockTag()
 * builds). ATL has no NFC service, NfcAdapter.getDefaultAdapter() is null, so the system never delivers a Tag;
 * apps can only build mock tags. No INfcTag: getTagService()/rediscover() are not carried. */
public final class Tag implements Parcelable {
	final byte[] mId;
	final int[] mTechList;
	final String[] mTechStringList;
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
		mTechStringList = generateTechStringList(techList);
		mTechExtras = Arrays.copyOf(techListExtras, techList.length);
		mServiceHandle = serviceHandle;
		mCookie = cookie;
		mConnectedTechnology = -1;
	}

	public static Tag createMockTag(byte[] id, int[] techList, Bundle[] techListExtras, long cookie) {
		return new Tag(id, techList, techListExtras, 0, cookie);
	}

	/* AOSP Tag.generateTechStringList() */
	private String[] generateTechStringList(int[] techList) {
		final int size = techList.length;
		String[] strings = new String[size];
		for (int i = 0; i < size; i++) {
			switch (techList[i]) {
				case TagTechnology.ISO_DEP:
					strings[i] = IsoDep.class.getName();
					break;
				case TagTechnology.MIFARE_CLASSIC:
					strings[i] = MifareClassic.class.getName();
					break;
				case TagTechnology.MIFARE_ULTRALIGHT:
					strings[i] = MifareUltralight.class.getName();
					break;
				case TagTechnology.NDEF:
					strings[i] = Ndef.class.getName();
					break;
				case TagTechnology.NDEF_FORMATABLE:
					strings[i] = NdefFormatable.class.getName();
					break;
				case TagTechnology.NFC_A:
					strings[i] = NfcA.class.getName();
					break;
				case TagTechnology.NFC_B:
					strings[i] = NfcB.class.getName();
					break;
				case TagTechnology.NFC_F:
					strings[i] = NfcF.class.getName();
					break;
				case TagTechnology.NFC_V:
					strings[i] = NfcV.class.getName();
					break;
				case TagTechnology.NFC_BARCODE:
					strings[i] = NfcBarcode.class.getName();
					break;
				default:
					throw new IllegalArgumentException("Unknown tech type " + techList[i]);
			}
		}
		return strings;
	}

	public byte[] getId() {
		return mId;
	}

	public String[] getTechList() {
		return mTechStringList;
	}

	public int[] getTechCodeList() {
		return mTechList;
	}

	/* AOSP Tag.rediscover(): a mock tag has no service */
	public Tag rediscover() throws IOException {
		if (getConnectedTechnology() != -1) {
			throw new IllegalStateException("Close connection to the technology first!");
		}
		throw new IOException("Mock tags don't support this operation.");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TAG: Tech [");
		String[] techList = getTechList();
		int length = techList.length;
		for (int i = 0; i < length; i++) {
			sb.append(techList[i]);
			if (i < length - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
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
