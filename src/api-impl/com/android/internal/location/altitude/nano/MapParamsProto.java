package com.android.internal.location.altitude.nano;

import java.io.IOException;

/** message MapParamsProto of frameworks/base proto/src/altitude.proto (protobuf-nano field layout). */
public final class MapParamsProto {
	public int mapS2Level;
	public int cacheTileS2Level;
	public int diskTileS2Level;
	public double modelAMeters;
	public double modelBMeters;
	public double modelRmseMeters;

	public static MapParamsProto parseFrom(byte[] data) throws IOException {
		MapParamsProto msg = new MapParamsProto();
		ProtoReader in = new ProtoReader(data);
		while (!in.atEnd()) {
			int tag = in.readTag();
			switch (tag) {
				case 8: msg.mapS2Level = (int)in.readVarint(); break;
				case 16: msg.cacheTileS2Level = (int)in.readVarint(); break;
				case 24: msg.diskTileS2Level = (int)in.readVarint(); break;
				case 33: msg.modelAMeters = in.readDouble(); break;
				case 41: msg.modelBMeters = in.readDouble(); break;
				case 49: msg.modelRmseMeters = in.readDouble(); break;
				default: in.skipField(tag); break;
			}
		}
		return msg;
	}
}
