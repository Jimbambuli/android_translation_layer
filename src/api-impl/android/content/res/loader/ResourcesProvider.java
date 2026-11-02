package android.content.res.loader;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/* ATL: of a resources table only the plain color values are used (the colors Material's dynamic color
 * puts into a table with ResourcesLoader); a provider from an APK or a directory provides nothing */
public class ResourcesProvider implements AutoCloseable {
	private static final String TAG = "ResourcesProvider";
	private final Map<Integer, Integer> colors;

	private ResourcesProvider(Map<Integer, Integer> colors) {
		this.colors = colors;
	}

	public static ResourcesProvider empty(AssetsProvider assetsProvider) {
		return new ResourcesProvider(new LinkedHashMap<>());
	}

	public static ResourcesProvider loadFromTable(ParcelFileDescriptor fileDescriptor, AssetsProvider assetsProvider) throws IOException {
		return new ResourcesProvider(parseColors(readAll(fileDescriptor.getFileDescriptor())));
	}

	public static ResourcesProvider loadFromApk(ParcelFileDescriptor fileDescriptor) throws IOException {
		return loadFromApk(fileDescriptor, null);
	}

	public static ResourcesProvider loadFromApk(ParcelFileDescriptor fileDescriptor, AssetsProvider assetsProvider) throws IOException {
		Log.w(TAG, "loadFromApk: not supported, the provider is empty");
		return empty(assetsProvider);
	}

	public static ResourcesProvider loadFromSplit(android.content.Context context, String splitName) throws IOException {
		Log.w(TAG, "loadFromSplit: not supported, the provider is empty");
		return empty(null);
	}

	public static ResourcesProvider loadFromDirectory(String path, AssetsProvider assetsProvider) throws IOException {
		Log.w(TAG, "loadFromDirectory: not supported, the provider is empty");
		return empty(assetsProvider);
	}

	/* resource id -> ARGB of the plain color entries */
	public Map<Integer, Integer> atl_getColors() {
		return colors;
	}

	@Override
	public void close() {}

	/* the whole file from its start, whatever the offset of the descriptor */
	private static byte[] readAll(FileDescriptor fd) throws IOException {
		try {
			long size = Os.fstat(fd).st_size;
			byte[] data = new byte[(int)size];
			int done = 0;
			while (done < data.length) {
				int n = Os.pread(fd, data, done, data.length - done, done);
				if (n <= 0)
					throw new IOException("short read of resources table");
				done += n;
			}
			return data;
		} catch (ErrnoException e) {
			throw new IOException(e);
		}
	}

	private static final int RES_STRING_POOL_TYPE = 0x0001;
	private static final int RES_TABLE_TYPE = 0x0002;
	private static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
	private static final int RES_TABLE_TYPE_TYPE = 0x0201;
	private static final int TYPE_FIRST_COLOR_INT = 0x1c;
	private static final int TYPE_LAST_COLOR_INT = 0x1f;
	private static final int FLAG_COMPLEX = 0x0001;
	private static final int FLAG_COMPACT = 0x0008;
	private static final int TYPE_FLAG_SPARSE = 0x01;
	private static final int TYPE_FLAG_OFFSET16 = 0x02;

	static Map<Integer, Integer> parseColors(byte[] data) throws IOException {
		Map<Integer, Integer> colors = new LinkedHashMap<>();
		ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		try {
			if (b.getShort(0) != RES_TABLE_TYPE)
				throw new IOException("not a resources table");
			int pos = b.getShort(2) & 0xffff;
			int end = Math.min(b.getInt(4), data.length);
			while (pos + 8 <= end) {
				int type = b.getShort(pos) & 0xffff;
				int size = b.getInt(pos + 4);
				if (size < 8)
					break;
				if (type == RES_TABLE_PACKAGE_TYPE)
					parsePackage(b, pos, size, colors);
				pos += size;
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IOException("broken resources table", e);
		}
		return colors;
	}

	private static void parsePackage(ByteBuffer b, int start, int size, Map<Integer, Integer> colors) {
		int pkgId = b.getInt(start + 8);
		int pos = start + (b.getShort(start + 2) & 0xffff);
		int end = start + size;
		while (pos + 8 <= end) {
			int type = b.getShort(pos) & 0xffff;
			int headerSize = b.getShort(pos + 2) & 0xffff;
			int chunkSize = b.getInt(pos + 4);
			if (chunkSize < 8)
				break;
			if (type == RES_TABLE_TYPE_TYPE) {
				int typeId = b.get(pos + 8) & 0xff;
				int flags = b.get(pos + 9) & 0xff;
				int entryCount = b.getInt(pos + 12);
				int entriesStart = pos + b.getInt(pos + 16);
				int offsets = pos + headerSize;
				for (int i = 0; i < entryCount; i++) {
					int index, offset;
					if ((flags & TYPE_FLAG_SPARSE) != 0) {
						index = b.getShort(offsets + 4 * i) & 0xffff;
						offset = (b.getShort(offsets + 4 * i + 2) & 0xffff) * 4;
					} else if ((flags & TYPE_FLAG_OFFSET16) != 0) {
						index = i;
						int o = b.getShort(offsets + 2 * i) & 0xffff;
						if (o == 0xffff)
							continue;
						offset = o * 4;
					} else {
						index = i;
						offset = b.getInt(offsets + 4 * i);
						if (offset == -1)
							continue;
					}
					int e = entriesStart + offset;
					int entryFlags = b.getShort(e + 2) & 0xffff;
					int dataType, value;
					if ((entryFlags & FLAG_COMPACT) != 0) {
						dataType = entryFlags >> 8;
						value = b.getInt(e + 4);
					} else {
						if ((entryFlags & FLAG_COMPLEX) != 0)
							continue;
						int v = e + (b.getShort(e) & 0xffff);
						dataType = b.get(v + 3) & 0xff;
						value = b.getInt(v + 4);
					}
					if (dataType >= TYPE_FIRST_COLOR_INT && dataType <= TYPE_LAST_COLOR_INT)
						colors.put((pkgId << 24) | (typeId << 16) | index, value);
				}
			}
			pos += chunkSize;
		}
	}
}
