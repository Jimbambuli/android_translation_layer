package android.atl.crypto;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * AlgorithmParameters for the ciphers, like Conscrypt's IvParameters and GCMParameters (Android's Bouncy Castle has
 * none for AES): an IV as an ASN.1 OCTET STRING (or RAW), GCM as SEQUENCE { OCTET STRING nonce, INTEGER icvLen
 * DEFAULT 12 } after RFC 5084 section 3.2.
 */
public class IvParameters extends AlgorithmParametersSpi {
	private byte[] iv;

	@Override
	protected void engineInit(AlgorithmParameterSpec spec) throws InvalidParameterSpecException {
		if (!(spec instanceof IvParameterSpec))
			throw new InvalidParameterSpecException("Only IvParameterSpec is supported");
		iv = ((IvParameterSpec)spec).getIV().clone();
	}

	@Override
	protected void engineInit(byte[] bytes) throws IOException {
		Der der = new Der(bytes, 0, bytes.length);
		byte[] newIv = der.octetString();
		der.end();
		iv = newIv;
	}

	@Override
	protected void engineInit(byte[] bytes, String format) throws IOException {
		if (format == null || format.equals("ASN.1"))
			engineInit(bytes);
		else if (format.equals("RAW"))
			iv = bytes.clone();
		else
			throw new IOException("Unsupported format: " + format);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> spec) throws InvalidParameterSpecException {
		if (spec != IvParameterSpec.class)
			throw new InvalidParameterSpecException("Incompatible AlgorithmParametersSpec class: " + spec);
		return (T)new IvParameterSpec(iv);
	}

	@Override
	protected byte[] engineGetEncoded() {
		return Der.tlv(0x04, iv);
	}

	@Override
	protected byte[] engineGetEncoded(String format) throws IOException {
		if (format == null || format.equals("ASN.1"))
			return engineGetEncoded();
		else if (format.equals("RAW"))
			return iv.clone();
		throw new IOException("Unsupported format: " + format);
	}

	@Override
	protected String engineToString() {
		return "AndroidOpenSSL IV AlgorithmParameters";
	}

	public static class AES extends IvParameters {}

	public static class ChaCha20 extends IvParameters {}

	public static final class GCM extends AlgorithmParametersSpi {
		private static final int DEFAULT_TLEN = 96;
		private int tLen;
		private byte[] iv;

		@Override
		protected void engineInit(AlgorithmParameterSpec spec) throws InvalidParameterSpecException {
			if (!(spec instanceof GCMParameterSpec))
				throw new InvalidParameterSpecException("Only GCMParameterSpec is supported");
			tLen = ((GCMParameterSpec)spec).getTLen();
			iv = ((GCMParameterSpec)spec).getIV();
		}

		@Override
		protected void engineInit(byte[] bytes) throws IOException {
			Der outer = new Der(bytes, 0, bytes.length);
			Der seq = outer.sequence();
			outer.end();
			byte[] newIv = seq.octetString();
			int newTLen = seq.atEnd() ? DEFAULT_TLEN : 8 * seq.smallInteger();
			seq.end();
			iv = newIv;
			tLen = newTLen;
		}

		@Override
		protected void engineInit(byte[] bytes, String format) throws IOException {
			if (format == null || format.equals("ASN.1"))
				engineInit(bytes);
			else
				throw new IOException("Unsupported format: " + format);
		}

		@Override
		protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> spec) throws InvalidParameterSpecException {
			if (spec != null && spec.getName().equals("javax.crypto.spec.GCMParameterSpec"))
				return spec.cast(new GCMParameterSpec(tLen, iv));
			throw new InvalidParameterSpecException("Unsupported class: " + spec);
		}

		@Override
		protected byte[] engineGetEncoded() {
			byte[] nonce = Der.tlv(0x04, iv);
			byte[] content = tLen == DEFAULT_TLEN ? nonce : concat(nonce, Der.tlv(0x02, new byte[] {(byte)(tLen / 8)}));
			return Der.tlv(0x30, content);
		}

		@Override
		protected byte[] engineGetEncoded(String format) throws IOException {
			if (format == null || format.equals("ASN.1"))
				return engineGetEncoded();
			throw new IOException("Unsupported format: " + format);
		}

		@Override
		protected String engineToString() {
			return "AndroidOpenSSL GCM AlgorithmParameters";
		}

		private static byte[] concat(byte[] a, byte[] b) {
			byte[] r = Arrays.copyOf(a, a.length + b.length);
			System.arraycopy(b, 0, r, a.length, b.length);
			return r;
		}
	}

	/* the bit of DER these parameters need */
	static final class Der {
		private final byte[] buf;
		private int pos;
		private final int end;

		Der(byte[] buf, int pos, int end) {
			this.buf = buf;
			this.pos = pos;
			this.end = end;
		}

		static byte[] tlv(int tag, byte[] value) {
			int len = value.length;
			int lenBytes = len < 0x80 ? 0 : len < 0x100 ? 1 : len < 0x10000 ? 2 : 3;
			byte[] out = new byte[2 + lenBytes + len];
			out[0] = (byte)tag;
			out[1] = (byte)(lenBytes == 0 ? len : 0x80 | lenBytes);
			for (int i = 0; i < lenBytes; i++)
				out[2 + i] = (byte)(len >>> (8 * (lenBytes - 1 - i)));
			System.arraycopy(value, 0, out, 2 + lenBytes, len);
			return out;
		}

		boolean atEnd() {
			return pos == end;
		}

		void end() throws IOException {
			if (pos != end)
				throw new IOException("Error reading ASN.1 encoding");
		}

		/* returns the content of the next element, which must have the tag */
		private Der next(int tag) throws IOException {
			if (end - pos < 2 || (buf[pos] & 0xff) != tag)
				throw new IOException("Error reading ASN.1 encoding");
			int len = buf[pos + 1] & 0xff;
			pos += 2;
			if (len >= 0x80) {
				int n = len & 0x7f;
				if (n == 0 || n > 3 || end - pos < n)
					throw new IOException("Error reading ASN.1 encoding");
				len = 0;
				for (int i = 0; i < n; i++)
					len = (len << 8) | (buf[pos++] & 0xff);
				if (len < 0x80 || (n > 1 && len < (1 << (8 * (n - 1)))))
					throw new IOException("Error reading ASN.1 encoding"); // not DER
			}
			if (len > end - pos)
				throw new IOException("Error reading ASN.1 encoding");
			Der content = new Der(buf, pos, pos + len);
			pos += len;
			return content;
		}

		byte[] octetString() throws IOException {
			Der c = next(0x04);
			return Arrays.copyOfRange(buf, c.pos, c.end);
		}

		Der sequence() throws IOException {
			return next(0x30);
		}

		/* a non-negative INTEGER below 2^31 */
		int smallInteger() throws IOException {
			Der c = next(0x02);
			int len = c.end - c.pos;
			if (len < 1 || len > 4 || (buf[c.pos] & 0x80) != 0 || (len > 1 && buf[c.pos] == 0 && (buf[c.pos + 1] & 0x80) == 0))
				throw new IOException("Error reading ASN.1 encoding");
			int v = 0;
			for (int i = c.pos; i < c.end; i++)
				v = (v << 8) | (buf[i] & 0xff);
			return v;
		}
	}
}
