package android.nfc.tech;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.Bundle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* AOSP MifareClassic (modules/Nfc), reduced to mock tags (see android.nfc.Tag): no NFC service in ATL. */
public final class MifareClassic extends BasicTagTechnology {
	public static final byte[] KEY_DEFAULT = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
	public static final byte[] KEY_MIFARE_APPLICATION_DIRECTORY = {(byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5};
	public static final byte[] KEY_NFC_FORUM = {(byte)0xD3, (byte)0xF7, (byte)0xD3, (byte)0xF7, (byte)0xD3, (byte)0xF7};

	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_CLASSIC = 0;
	public static final int TYPE_PLUS = 1;
	public static final int TYPE_PRO = 2;
	public static final int SIZE_1K = 1024;
	public static final int SIZE_2K = 2048;
	public static final int SIZE_4K = 4096;
	public static final int SIZE_MINI = 320;
	public static final int BLOCK_SIZE = 16;

	private static final int MAX_BLOCK_COUNT = 256;
	private static final int MAX_SECTOR_COUNT = 40;

	private boolean mIsEmulated;
	private int mType;
	private int mSize;

	public static MifareClassic get(Tag tag) {
		if (!tag.hasTech(TagTechnology.MIFARE_CLASSIC))
			return null;
		return new MifareClassic(tag);
	}

	public MifareClassic(Tag tag) {
		super(tag, TagTechnology.MIFARE_CLASSIC);
		/* MIFARE Classic is always based on NFC A (NullPointerException if the tag lacks it, as in AOSP) */
		NfcA a = NfcA.get(tag);
		mIsEmulated = false;
		switch (a.getSak()) {
			case 0x01:
			case 0x08:
				mType = TYPE_CLASSIC;
				mSize = SIZE_1K;
				break;
			case 0x09:
				mType = TYPE_CLASSIC;
				mSize = SIZE_MINI;
				break;
			case 0x10:
				mType = TYPE_PLUS;
				mSize = SIZE_2K;
				break;
			case 0x11:
				mType = TYPE_PLUS;
				mSize = SIZE_4K;
				break;
			case 0x18:
				mType = TYPE_CLASSIC;
				mSize = SIZE_4K;
				break;
			case 0x28:
				mType = TYPE_CLASSIC;
				mSize = SIZE_1K;
				mIsEmulated = true;
				break;
			case 0x38:
				mType = TYPE_CLASSIC;
				mSize = SIZE_4K;
				mIsEmulated = true;
				break;
			case 0x88:
				mType = TYPE_CLASSIC;
				mSize = SIZE_1K;
				break;
			case 0x98:
			case 0xB8:
				mType = TYPE_PRO;
				mSize = SIZE_4K;
				break;
			default:
				throw new RuntimeException("Tag incorrectly enumerated as MIFARE Classic, SAK = " + a.getSak());
		}
	}

	public int getType() {
		return mType;
	}

	public int getSize() {
		return mSize;
	}

	public boolean isEmulated() {
		return mIsEmulated;
	}

	public int getSectorCount() {
		switch (mSize) {
			case SIZE_1K:
				return 16;
			case SIZE_2K:
				return 32;
			case SIZE_4K:
				return 40;
			case SIZE_MINI:
				return 5;
			default:
				return 0;
		}
	}

	public int getBlockCount() {
		return mSize / BLOCK_SIZE;
	}

	public int getBlockCountInSector(int sectorIndex) {
		validateSector(sectorIndex);
		if (sectorIndex < 32) {
			return 4;
		} else {
			return 16;
		}
	}

	public int blockToSector(int blockIndex) {
		validateBlock(blockIndex);
		if (blockIndex < 32 * 4) {
			return blockIndex / 4;
		} else {
			return 32 + (blockIndex - 32 * 4) / 16;
		}
	}

	public int sectorToBlock(int sectorIndex) {
		if (sectorIndex < 32) {
			return sectorIndex * 4;
		} else {
			return 32 * 4 + (sectorIndex - 32) * 16;
		}
	}

	public boolean authenticateSectorWithKeyA(int sectorIndex, byte[] key) throws IOException {
		return authenticate(sectorIndex, key, true);
	}

	public boolean authenticateSectorWithKeyB(int sectorIndex, byte[] key) throws IOException {
		return authenticate(sectorIndex, key, false);
	}

	private boolean authenticate(int sector, byte[] key, boolean keyA) throws IOException {
		validateSector(sector);
		checkConnected();

		byte[] cmd = new byte[12];
		cmd[0] = keyA ? (byte)0x60 : (byte)0x61;
		cmd[1] = (byte)sectorToBlock(sector);
		byte[] uid = getTag().getId();
		System.arraycopy(uid, uid.length - 4, cmd, 2, 4);
		System.arraycopy(key, 0, cmd, 6, 6);
		try {
			if (transceive(cmd, false) != null) {
				return true;
			}
		} catch (TagLostException e) {
			throw e;
		} catch (IOException e) {
			// No need to deal with, will return false anyway
		}
		return false;
	}

	public byte[] readBlock(int blockIndex) throws IOException {
		validateBlock(blockIndex);
		checkConnected();
		byte[] cmd = {0x30, (byte)blockIndex};
		return transceive(cmd, false);
	}

	public void writeBlock(int blockIndex, byte[] data) throws IOException {
		validateBlock(blockIndex);
		checkConnected();
		if (data.length != 16) {
			throw new IllegalArgumentException("must write 16-bytes");
		}
		byte[] cmd = new byte[data.length + 2];
		cmd[0] = (byte)0xA0;
		cmd[1] = (byte)blockIndex;
		System.arraycopy(data, 0, cmd, 2, data.length);
		transceive(cmd, false);
	}

	public void increment(int blockIndex, int value) throws IOException {
		valueOperation((byte)0xC1, blockIndex, value);
	}

	public void decrement(int blockIndex, int value) throws IOException {
		valueOperation((byte)0xC0, blockIndex, value);
	}

	private void valueOperation(byte opcode, int blockIndex, int value) throws IOException {
		validateBlock(blockIndex);
		validateValueOperand(value);
		checkConnected();
		ByteBuffer cmd = ByteBuffer.allocate(6);
		cmd.order(ByteOrder.LITTLE_ENDIAN);
		cmd.put(opcode);
		cmd.put((byte)blockIndex);
		cmd.putInt(value);
		transceive(cmd.array(), false);
	}

	public void transfer(int blockIndex) throws IOException {
		validateBlock(blockIndex);
		checkConnected();
		byte[] cmd = {(byte)0xB0, (byte)blockIndex};
		transceive(cmd, false);
	}

	public void restore(int blockIndex) throws IOException {
		validateBlock(blockIndex);
		checkConnected();
		byte[] cmd = {(byte)0xC2, (byte)blockIndex};
		transceive(cmd, false);
	}

	public byte[] transceive(byte[] data) throws IOException {
		return transceive(data, true);
	}

	public int getMaxTransceiveLength() {
		return getMaxTransceiveLengthInternal();
	}

	/* AOSP setTimeout(): mTag.getTagService() is null on a mock tag, NullPointerException */
	public void setTimeout(int timeout) {
		throw new NullPointerException();
	}

	/* AOSP getTimeout(): see setTimeout() */
	public int getTimeout() {
		throw new NullPointerException();
	}

	private static void validateSector(int sector) {
		if (sector < 0 || sector >= MAX_SECTOR_COUNT) {
			throw new IndexOutOfBoundsException("sector out of bounds: " + sector);
		}
	}

	private static void validateBlock(int block) {
		if (block < 0 || block >= MAX_BLOCK_COUNT) {
			throw new IndexOutOfBoundsException("block out of bounds: " + block);
		}
	}

	private static void validateValueOperand(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("value operand negative");
		}
	}
}
