package android.nfc.tech;

import android.nfc.Tag;
import java.io.IOException;

/* AOSP BasicTagTechnology (modules/Nfc) for mock tags, which have no INfcTag service (Tag.getTagService() is null in
 * AOSP, here there is no such member). */
abstract class BasicTagTechnology implements TagTechnology {
	final Tag mTag;
	boolean mIsConnected;
	int mSelectedTechnology;

	BasicTagTechnology(Tag tag, int tech) {
		mTag = tag;
		mSelectedTechnology = tech;
	}

	@Override
	public Tag getTag() {
		return mTag;
	}

	/* AOSP checkConnected() */
	void checkConnected() {
		if ((mTag.getConnectedTechnology() != mSelectedTechnology) || (mTag.getConnectedTechnology() == -1)) {
			throw new IllegalStateException("Call connect() first!");
		}
	}

	/* AOSP connect() starts with mTag.getTagService().connect(...); on a mock tag that service is null and AOSP
	 * throws NullPointerException there */
	@Override
	public void connect() throws IOException {
		throw new NullPointerException();
	}

	/* AOSP reconnect(): IllegalStateException while not connected (and connect() cannot succeed here) */
	@Override
	public void reconnect() throws IOException {
		throw new IllegalStateException("Technology not connected yet");
	}

	/* AOSP close(): mTag.getTagService().resetTimeouts() hits the null service (NullPointerException) after which
	 * the finally block resets the connection state */
	@Override
	public void close() throws IOException {
		mIsConnected = false;
		mTag.setTechnologyDisconnected();
		throw new NullPointerException();
	}

	/* AOSP isConnected(): false until connect() succeeded */
	@Override
	public boolean isConnected() {
		return false;
	}

	/* AOSP getMaxTransceiveLengthInternal(): mTag.getTagService() is null on a mock tag (NullPointerException) */
	int getMaxTransceiveLengthInternal() {
		throw new NullPointerException();
	}

	/* AOSP transceive(data, raw): checkConnected() first, then the null service of a mock tag */
	byte[] transceive(byte[] data, boolean raw) throws IOException {
		checkConnected();
		throw new NullPointerException();
	}
}
