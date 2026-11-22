package android.nfc.tech;

import android.nfc.Tag;
import java.io.IOException;
import java.util.HashMap;

/* AOSP BasicTagTechnology (modules/Nfc). Mock tags have no INfcTag service (Tag.getTagService() is null in
 * AOSP, here there is no such member); a neard tag (Tag.getNeardPath()) is served by neard, which reads the NDEF
 * message but gives no raw access: connect() works while the tag is in range, transceive() fails. */
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
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		/* AOSP: INfcTag.connect() fails with an error code -> IOException */
		if (!com.android.server.nfc.NeardAdapter.isTagPresent(mTag.getNeardPath()))
			throw new IOException();
		mTag.setConnectedTechnology(mSelectedTechnology);
		mIsConnected = true;
	}

	/* AOSP reconnect(): IllegalStateException while not connected (and connect() cannot succeed here) */
	@Override
	public void reconnect() throws IOException {
		if (!mIsConnected)
			throw new IllegalStateException("Technology not connected yet");
		if (!com.android.server.nfc.NeardAdapter.isTagPresent(mTag.getNeardPath())) {
			mIsConnected = false;
			mTag.setTechnologyDisconnected();
			throw new IOException();
		}
	}

	/* AOSP close(): mTag.getTagService().resetTimeouts() hits the null service (NullPointerException) after which
	 * the finally block resets the connection state */
	@Override
	public void close() throws IOException {
		mIsConnected = false;
		mTag.setTechnologyDisconnected();
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		/* INfcTag.resetTimeouts() */
		synchronized (sTimeouts) {
			sTimeouts.clear();
		}
	}

	/* the controller's transceive timeouts per technology (AOSP NfcTag::resetAllTransceiveTimeouts() defaults);
	 * neard transceives nothing, so they are only kept */
	private static final HashMap<Integer, Integer> sTimeouts = new HashMap<>();

	private static int defaultTimeout(int tech) {
		switch (tech) {
			case TagTechnology.NFC_F:
				return 255;
			case TagTechnology.NFC_A:
			case TagTechnology.ISO_DEP:
			case TagTechnology.MIFARE_CLASSIC:
			case TagTechnology.MIFARE_ULTRALIGHT:
				return 618;
			default:
				return 1000;
		}
	}

	/* AOSP setTimeout() of the technologies: INfcTag.setTimeout(); the null service of a mock tag */
	void setTimeoutInternal(int tech, int timeout) {
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		synchronized (sTimeouts) {
			sTimeouts.put(tech, timeout);
		}
	}

	/* AOSP getTimeout() of the technologies: INfcTag.getTimeout() */
	int getTimeoutInternal(int tech) {
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		synchronized (sTimeouts) {
			Integer timeout = sTimeouts.get(tech);
			return timeout != null ? timeout : defaultTimeout(tech);
		}
	}

	/* AOSP isConnected(): false until connect() succeeded, then whether the tag is still in range */
	@Override
	public boolean isConnected() {
		if (!mIsConnected)
			return false;
		return com.android.server.nfc.NeardAdapter.isTagPresent(mTag.getNeardPath());
	}

	/* AOSP getMaxTransceiveLengthInternal(): mTag.getTagService() is null on a mock tag (NullPointerException);
	 * neard transceives nothing (INfcTag.getMaxTransceiveLength() of a technology without raw access: 0) */
	int getMaxTransceiveLengthInternal() {
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		return 0;
	}

	/* AOSP transceive(data, raw): checkConnected() first, then the null service of a mock tag; neard has no raw
	 * access, so INfcTag.transceive() gives no result: "transceive failed" */
	byte[] transceive(byte[] data, boolean raw) throws IOException {
		checkConnected();
		if (mTag.getNeardPath() == null)
			throw new NullPointerException();
		throw new IOException("transceive failed");
	}
}
