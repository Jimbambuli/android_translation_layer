package android.os;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * AOSP DropBoxManager. ATL has no DropBoxManagerService: there is no stored entry to read, so
 * getNextEntry() returns null as documented for "no more entries".
 */
public class DropBoxManager {
	private static final String TAG = "DropBoxManager";

	public static final int IS_EMPTY = 1;
	public static final int IS_TEXT = 2;
	public static final int IS_GZIPPED = 4;
	private static final int HAS_BYTE_ARRAY = 8;

	public static final String ACTION_DROPBOX_ENTRY_ADDED = "android.intent.action.DROPBOX_ENTRY_ADDED";
	public static final String EXTRA_TAG = "tag";
	public static final String EXTRA_TIME = "time";
	public static final String EXTRA_DROPPED_COUNT = "android.os.extra.DROPPED_COUNT";

	/* AOSP DropBoxManager.Entry */
	public static class Entry implements Parcelable, Closeable {
		private final String mTag;
		private final long mTimeMillis;

		private final byte[] mData;
		private final ParcelFileDescriptor mFileDescriptor;
		private final int mFlags;

		public Entry(String tag, long millis) {
			if (tag == null)
				throw new NullPointerException("tag == null");

			mTag = tag;
			mTimeMillis = millis;
			mData = null;
			mFileDescriptor = null;
			mFlags = IS_EMPTY;
		}

		public Entry(String tag, long millis, String text) {
			if (tag == null)
				throw new NullPointerException("tag == null");
			if (text == null)
				throw new NullPointerException("text == null");

			mTag = tag;
			mTimeMillis = millis;
			mData = text.getBytes(StandardCharsets.UTF_8);
			mFileDescriptor = null;
			mFlags = IS_TEXT;
		}

		public Entry(String tag, long millis, byte[] data, int flags) {
			if (tag == null)
				throw new NullPointerException("tag == null");
			if (((flags & IS_EMPTY) != 0) != (data == null)) {
				throw new IllegalArgumentException("Bad flags: " + flags);
			}

			mTag = tag;
			mTimeMillis = millis;
			mData = data;
			mFileDescriptor = null;
			mFlags = flags;
		}

		public Entry(String tag, long millis, ParcelFileDescriptor data, int flags) {
			if (tag == null)
				throw new NullPointerException("tag == null");
			if (((flags & IS_EMPTY) != 0) != (data == null)) {
				throw new IllegalArgumentException("Bad flags: " + flags);
			}

			mTag = tag;
			mTimeMillis = millis;
			mData = null;
			mFileDescriptor = data;
			mFlags = flags;
		}

		public Entry(String tag, long millis, File data, int flags) throws IOException {
			if (tag == null)
				throw new NullPointerException("tag == null");
			if ((flags & IS_EMPTY) != 0)
				throw new IllegalArgumentException("Bad flags: " + flags);

			mTag = tag;
			mTimeMillis = millis;
			mData = null;
			mFileDescriptor = ParcelFileDescriptor.open(data, ParcelFileDescriptor.MODE_READ_ONLY);
			mFlags = flags;
		}

		public void close() {
			try {
				if (mFileDescriptor != null)
					mFileDescriptor.close();
			} catch (IOException e) {
			}
		}

		public String getTag() {
			return mTag;
		}

		public long getTimeMillis() {
			return mTimeMillis;
		}

		public int getFlags() {
			// getInputStream() decompresses.
			return mFlags & ~IS_GZIPPED;
		}

		public String getText(int maxBytes) {
			if ((mFlags & IS_TEXT) == 0)
				return null;
			if (mData != null)
				return new String(mData, 0, Math.min(maxBytes, mData.length));

			InputStream is = null;
			try {
				is = getInputStream();
				if (is == null)
					return null;
				byte[] buf = new byte[maxBytes];
				int readBytes = 0;
				int n = 0;
				while (n >= 0 && (readBytes += n) < maxBytes) {
					n = is.read(buf, readBytes, maxBytes - readBytes);
				}
				return new String(buf, 0, readBytes);
			} catch (IOException e) {
				return null;
			} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
				}
			}
		}

		public InputStream getInputStream() throws IOException {
			InputStream is;
			if (mData != null) {
				is = new ByteArrayInputStream(mData);
			} else if (mFileDescriptor != null) {
				is = new ParcelFileDescriptor.AutoCloseInputStream(mFileDescriptor);
			} else {
				return null;
			}
			return (mFlags & IS_GZIPPED) != 0 ? new GZIPInputStream(new BufferedInputStream(is)) : is;
		}

		public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
			public Entry[] newArray(int size) {
				return new Entry[size];
			}

			public Entry createFromParcel(Parcel in) {
				String tag = in.readString();
				long millis = in.readLong();
				int flags = in.readInt();
				if ((flags & HAS_BYTE_ARRAY) != 0) {
					return new Entry(tag, millis, in.createByteArray(), flags & ~HAS_BYTE_ARRAY);
				} else {
					ParcelFileDescriptor pfd = in.readFileDescriptor();
					return new Entry(tag, millis, pfd, flags);
				}
			}
		};

		public int describeContents() {
			return mFileDescriptor != null ? Parcelable.CONTENTS_FILE_DESCRIPTOR : 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			out.writeString(mTag);
			out.writeLong(mTimeMillis);
			if (mFileDescriptor != null) {
				out.writeInt(mFlags & ~HAS_BYTE_ARRAY); // Clear bit just to be safe
				out.writeFileDescriptor(mFileDescriptor.getFileDescriptor());
			} else {
				out.writeInt(mFlags | HAS_BYTE_ARRAY);
				out.writeByteArray(mData);
			}
		}
	}

	/* AOSP: protected; public here because SystemServiceRegistry (android.app) creates it */
	public DropBoxManager() {}

	/* AOSP DropBoxManager.getNextEntry asks DropBoxManagerService; ATL has none, so there is no entry. */
	public Entry getNextEntry(String tag, long msec) {
		return null;
	}

	/* AOSP addText/addData/addFile/isTagEnabled hand over to DropBoxManagerService; ATL has none, so the
	 * entry is dropped, like one for a blocked tag, after AOSP's argument checks. */
	public void addText(String tag, String data) {
		addData(tag, data.getBytes(java.nio.charset.StandardCharsets.UTF_8), IS_TEXT);
	}

	public void addData(String tag, byte[] data, int flags) {
		if (data == null)
			throw new NullPointerException("data == null");
	}

	public void addFile(String tag, File file, int flags) throws IOException {
		if (file == null)
			throw new NullPointerException("file == null");
		/* AOSP opens the file read-only first, so a missing file throws here as well */
		new java.io.FileInputStream(file).close();
	}

	public boolean isTagEnabled(String tag) {
		return false;
	}
}
