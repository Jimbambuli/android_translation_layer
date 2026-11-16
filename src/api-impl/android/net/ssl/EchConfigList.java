package android.net.ssl;

import java.nio.ByteBuffer;

/* Conscrypt EchConfigList (org.conscrypt, repackaged into android.net.ssl by the platform build): an ECH
 * (Encrypted Client Hello) config list as found in a HTTPS DNS record. */
public class EchConfigList {
	private final byte[] rawData;

	private EchConfigList(byte[] rawData) {
		// Make a copy of the raw data to prevent the caller from being able to modify it directly.
		this.rawData = rawData.clone();
	}

	/* The raw bytes from a HTTPS DNS record are fed directly into this method.
	 * Throws NullPointerException if byteArr is null, InvalidEchDataException if the ECH data is empty, does not
	 * contain a length, or has a length mismatch. */
	public static EchConfigList fromBytes(byte[] byteArr) throws InvalidEchDataException {
		if (byteArr == null) {
			throw new NullPointerException("ECH config list should not be null");
		}

		if (byteArr.length == 0) {
			throw new InvalidEchDataException("Empty ECH config list");
		}

		if (byteArr.length < 2) {
			throw new InvalidEchDataException("ECH config list does not contain a length");
		}

		int echConfigListLength = Short.toUnsignedInt(ByteBuffer.wrap(byteArr).getShort());
		// Subtract the 2 bytes corresponding to the overall EchConfigList length
		if (echConfigListLength != byteArr.length - 2) {
			throw new InvalidEchDataException("ECH config list length does not match");
		}

		return new EchConfigList(byteArr);
	}

	/* Returns the raw byte representation of an EchConfigList. */
	public byte[] toBytes() {
		// Defensive copy, so that the caller can't modify the underlying raw data.
		return rawData.clone();
	}
}
