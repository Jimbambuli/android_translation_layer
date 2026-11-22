package com.android.internal.location.altitude.nano;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads the protobuf wire format of the two messages of frameworks/base proto/src/altitude.proto.
 * AOSP generates MapParamsProto and S2TileProto with protobuf-nano, which ATL does not ship;
 * like the nano parser, unknown fields are skipped and malformed input throws an IOException
 * (InvalidProtocolBufferNanoException extends IOException).
 */
final class ProtoReader {
	private final byte[] buf;
	private int pos;

	ProtoReader(byte[] buf) {
		this.buf = buf;
	}

	boolean atEnd() {
		return pos >= buf.length;
	}

	/** Returns the next tag (field number << 3 | wire type). */
	int readTag() throws IOException {
		int tag = (int)readVarint();
		if ((tag >>> 3) == 0)
			throw new IOException("Protocol message contained an invalid tag (zero).");
		return tag;
	}

	long readVarint() throws IOException {
		long result = 0;
		for (int shift = 0; shift < 64; shift += 7) {
			byte b = readByte();
			result |= (long)(b & 0x7F) << shift;
			if ((b & 0x80) == 0)
				return result;
		}
		throw new IOException("CodedInputStream encountered a malformed varint.");
	}

	double readDouble() throws IOException {
		long bits = 0;
		for (int i = 0; i < 8; i++)
			bits |= (long)(readByte() & 0xFF) << (8 * i);
		return Double.longBitsToDouble(bits);
	}

	byte[] readBytes() throws IOException {
		int len = (int)readVarint();
		if (len < 0 || len > buf.length - pos)
			throw new IOException("While parsing a protocol message, the input ended unexpectedly in the middle of a field.");
		byte[] out = new byte[len];
		System.arraycopy(buf, pos, out, 0, len);
		pos += len;
		return out;
	}

	String readString() throws IOException {
		return new String(readBytes(), StandardCharsets.UTF_8);
	}

	void skipField(int tag) throws IOException {
		switch (tag & 7) {
			case 0: readVarint(); break;
			case 1: skip(8); break;
			case 2: readBytes(); break;
			case 5: skip(4); break;
			default: throw new IOException("Protocol message tag had invalid wire type.");
		}
	}

	private void skip(int n) throws IOException {
		if (n > buf.length - pos)
			throw new IOException("While parsing a protocol message, the input ended unexpectedly in the middle of a field.");
		pos += n;
	}

	private byte readByte() throws IOException {
		if (pos >= buf.length)
			throw new IOException("While parsing a protocol message, the input ended unexpectedly in the middle of a field.");
		return buf[pos++];
	}
}
