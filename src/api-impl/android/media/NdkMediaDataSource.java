package android.media;

/* libmediandk's NdkDataSource: the AMediaDataSource of AMediaExtractor_setDataSourceCustom() (a copy of its callbacks at
 * ptr) as the source of a MediaExtractor. libandroid registers the natives; it calls the AMediaDataSource's close
 * callback itself in AMediaExtractor_delete(), as ~NuMediaExtractor() does. */
final class NdkMediaDataSource extends MediaDataSource {
	private final long ptr;

	NdkMediaDataSource(long ptr) {
		this.ptr = ptr;
	}

	@Override
	public int readAt(long position, byte[] buffer, int offset, int size) {
		return native_readAt(ptr, position, buffer, offset, size);
	}

	@Override
	public long getSize() {
		return native_getSize(ptr);
	}

	@Override
	public void close() {
	}

	private static native int native_readAt(long ptr, long position, byte[] buffer, int offset, int size);
	private static native long native_getSize(long ptr);
}
