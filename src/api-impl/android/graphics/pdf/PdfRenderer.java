package android.graphics.pdf;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.IOException;

/**
 * Renders PDF pages to bitmaps (AOSP android.graphics.pdf.PdfRenderer).
 *
 * AOSP opens the document with pdfium. ATL has no pdfium, so the constructor behaves like AOSP for
 * a document pdfium cannot load: the argument checks run as in AOSP, then it throws the IOException
 * that AOSP's native nativeCreate throws for an unreadable document. Callers handle this already.
 */
public final class PdfRenderer implements AutoCloseable {
	private final int mPageCount;
	private ParcelFileDescriptor mInput;
	private Page mCurrentPage;

	public PdfRenderer(ParcelFileDescriptor input) throws IOException {
		if (input == null) {
			throw new NullPointerException("input cannot be null");
		}
		try {
			Os.lseek(input.getFileDescriptor(), 0, OsConstants.SEEK_SET);
			Os.fstat(input.getFileDescriptor());
		} catch (ErrnoException ee) {
			throw new IllegalArgumentException("file descriptor not seekable");
		}
		throw new IOException("file not in PDF format or corrupted");
	}

	public void close() {
		throwIfClosed();
		throwIfPageOpened();
		doClose();
	}

	public int getPageCount() {
		throwIfClosed();
		return mPageCount;
	}

	public boolean shouldScaleForPrinting() {
		throwIfClosed();
		return false;
	}

	public Page openPage(int index) {
		throwIfClosed();
		throwIfPageOpened();
		throwIfPageNotInDocument(index);
		mCurrentPage = new Page(index);
		return mCurrentPage;
	}

	private void doClose() {
		try {
			mInput.close();
		} catch (IOException ignored) {
		}
		mInput = null;
	}

	private void throwIfClosed() {
		if (mInput == null) {
			throw new IllegalStateException("Already closed");
		}
	}

	private void throwIfPageOpened() {
		if (mCurrentPage != null) {
			throw new IllegalStateException("Current page not closed");
		}
	}

	private void throwIfPageNotInDocument(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= mPageCount) {
			throw new IllegalArgumentException("Invalid page index");
		}
	}

	/**
	 * A PDF page. Only reachable through openPage(), which needs a loaded document.
	 */
	public final class Page implements AutoCloseable {
		public static final int RENDER_MODE_FOR_DISPLAY = 1;
		public static final int RENDER_MODE_FOR_PRINT = 2;

		private final int mIndex;
		private final int mWidth;
		private final int mHeight;
		private boolean mClosed;

		private Page(int index) {
			mIndex = index;
			mWidth = 0;
			mHeight = 0;
		}

		public int getIndex() {
			return mIndex;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}

		public void render(Bitmap destination, Rect destClip, Matrix transform, int renderMode) {
			if (mClosed) {
				throw new NullPointerException();
			}
			if (destination == null) {
				throw new NullPointerException("bitmap null");
			}
			if (destination.getConfig() != Bitmap.Config.ARGB_8888) {
				throw new IllegalArgumentException("Unsupported pixel format");
			}
			if (destClip != null) {
				if (destClip.left < 0 || destClip.top < 0
				    || destClip.right > destination.getWidth()
				    || destClip.bottom > destination.getHeight()) {
					throw new IllegalArgumentException("destBounds not in destination");
				}
			}
			if (transform != null && !transform.isAffine()) {
				throw new IllegalArgumentException("transform not affine");
			}
			if (renderMode != RENDER_MODE_FOR_PRINT && renderMode != RENDER_MODE_FOR_DISPLAY) {
				throw new IllegalArgumentException("Unsupported render mode");
			}
		}

		@Override
		public void close() {
			if (mClosed) {
				throw new IllegalStateException("Already closed");
			}
			mClosed = true;
			mCurrentPage = null;
		}
	}
}
