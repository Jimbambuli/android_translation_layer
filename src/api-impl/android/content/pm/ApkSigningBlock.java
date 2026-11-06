package android.content.pm;

import android.util.Slog;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Reads the signing certificates from the APK Signature Scheme v2/v3 block, for APKs that have
 * no v1 (JAR) signature. The signatures are not verified, only extracted.
 * <a href="https://source.android.com/docs/security/features/apksigning/v2#apk-signing-block">Source</a>
 */
final class ApkSigningBlock {
	private static final int ID_V2 = 0x7109871a;
	private static final int ID_V3 = 0xf05368c0;
	private static final int EOCD_SIZE = 22;

	static Signature[] readSignatures(String apkPath) {
		try (RandomAccessFile apk = new RandomAccessFile(apkPath, "r")) {
			// the end of central directory record tells where the central directory (and so the signing block before it) is
			byte[] tail = new byte[(int)Math.min(apk.length(), 0xffff + EOCD_SIZE)];
			apk.seek(apk.length() - tail.length);
			apk.readFully(tail);
			int eocd = tail.length - EOCD_SIZE;
			while (eocd >= 0 && ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN).getInt(eocd) != 0x06054b50)
				eocd--;
			long centralDir = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN).getInt(eocd + 16) & 0xffffffffL;

			// the block ends with: size (8 bytes), magic (16 bytes)
			byte[] footer = new byte[24];
			apk.seek(centralDir - footer.length);
			apk.readFully(footer);
			ByteBuffer footerBuf = ByteBuffer.wrap(footer).order(ByteOrder.LITTLE_ENDIAN);
			long size = footerBuf.getLong();
			if (footerBuf.getLong() != 0x20676953204b5041L || footerBuf.getLong() != 0x3234206b636f6c42L) // "APK Sig Block 42"
				return null;

			byte[] pairs = new byte[(int)(size - footer.length)];
			apk.seek(centralDir - size);
			apk.readFully(pairs);

			// v2 and v3 have the same signers layout, so use whichever is there
			ByteBuffer buf = ByteBuffer.wrap(pairs).order(ByteOrder.LITTLE_ENDIAN);
			while (buf.hasRemaining()) {
				ByteBuffer pair = slice(buf, (int)buf.getLong());
				int id = pair.getInt();
				if (id == ID_V2 || id == ID_V3)
					return readSigners(pair);
			}
		} catch (Exception e) { // I/O errors or a malformed block
			Slog.w("ApkSigningBlock", "Unable to read APK signing block of " + apkPath, e);
		}
		return null;
	}

	// signers { signer { signed data { digests, certificates, ... }, ... } }
	private static Signature[] readSigners(ByteBuffer block) {
		ArrayList<Signature> signatures = new ArrayList<>();
		ByteBuffer signers = lengthPrefixed(block);
		while (signers.hasRemaining()) {
			ByteBuffer signedData = lengthPrefixed(lengthPrefixed(signers));
			lengthPrefixed(signedData); // digests
			// the first certificate is the signer's own, the rest is its chain
			ByteBuffer certificate = lengthPrefixed(lengthPrefixed(signedData));
			byte[] bytes = new byte[certificate.remaining()];
			certificate.get(bytes);
			signatures.add(new Signature(bytes));
		}
		return signatures.isEmpty() ? null : signatures.toArray(new Signature[0]);
	}

	private static ByteBuffer lengthPrefixed(ByteBuffer buf) {
		return slice(buf, buf.getInt());
	}

	// returns the next `length` bytes of buf and skips them; throws if they aren't there
	private static ByteBuffer slice(ByteBuffer buf, int length) {
		ByteBuffer slice = buf.slice().order(ByteOrder.LITTLE_ENDIAN);
		slice.limit(length);
		buf.position(buf.position() + length);
		return slice;
	}
}
