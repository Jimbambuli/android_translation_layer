package com.android.internal.location.altitude.nano;

import java.io.IOException;

/** message S2TileProto of frameworks/base proto/src/altitude.proto (protobuf-nano field layout). */
public final class S2TileProto {
	private static final byte[] EMPTY_BYTES = new byte[0];

	public String tileKey = "";
	public byte[] byteBuffer = EMPTY_BYTES;
	public byte[] byteJpeg = EMPTY_BYTES;
	public byte[] bytePng = EMPTY_BYTES;

	public static S2TileProto parseFrom(byte[] data) throws IOException {
		S2TileProto msg = new S2TileProto();
		ProtoReader in = new ProtoReader(data);
		while (!in.atEnd()) {
			int tag = in.readTag();
			switch (tag) {
				case 10: msg.tileKey = in.readString(); break;
				case 18: msg.byteBuffer = in.readBytes(); break;
				case 26: msg.byteJpeg = in.readBytes(); break;
				case 34: msg.bytePng = in.readBytes(); break;
				default: in.skipField(tag); break;
			}
		}
		return msg;
	}
}
