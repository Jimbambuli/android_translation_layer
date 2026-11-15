package android.graphics.pdf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/* AOSP android.graphics.pdf.PdfDocument (the Java part with the same argument checks and exceptions). AOSP records
 * the drawing commands of a page into Skia's PDF backend (nativeStartPage/nativeWriteTo); ATL's Canvas draws into
 * a bitmap, so a page is the bitmap of the page size (one pixel per PDF point, white paper) and writeTo() writes a
 * valid PDF with each page as one Flate-compressed RGB image. The content rect clips the canvas as in AOSP. */
public class PdfDocument {
	private final List<PageInfo> mPages = new ArrayList<PageInfo>();
	private final List<Bitmap> mBitmaps = new ArrayList<Bitmap>();
	private boolean mClosed;
	private Page mCurrentPage;

	public PdfDocument() {}

	public Page startPage(PageInfo pageInfo) {
		throwIfClosed();
		throwIfCurrentPageNotFinished();
		if (pageInfo == null) {
			throw new IllegalArgumentException("page cannot be null");
		}
		Bitmap bitmap = Bitmap.createBitmap(pageInfo.mPageWidth, pageInfo.mPageHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new PdfCanvas(bitmap);
		canvas.drawColor(Color.WHITE);
		canvas.clipRect(pageInfo.mContentRect.left, pageInfo.mContentRect.top, pageInfo.mContentRect.right,
		                pageInfo.mContentRect.bottom);
		mCurrentPage = new Page(canvas, pageInfo, bitmap);
		return mCurrentPage;
	}

	public void finishPage(Page page) {
		throwIfClosed();
		if (page == null) {
			throw new IllegalArgumentException("page cannot be null");
		}
		if (page != mCurrentPage) {
			throw new IllegalStateException("invalid page");
		}
		if (page.isFinished()) {
			throw new IllegalStateException("page already finished");
		}
		mPages.add(page.getInfo());
		mBitmaps.add(page.mBitmap);
		mCurrentPage = null;
		page.finish();
	}

	public void writeTo(OutputStream out) throws IOException {
		throwIfClosed();
		throwIfCurrentPageNotFinished();
		if (out == null) {
			throw new IllegalArgumentException("out cannot be null!");
		}
		int n = mPages.size();
		/* objects: 1 catalog, 2 pages, then per page 3 objects (page, content, image) */
		long[] offsets = new long[3 + n * 3];
		Counting counting = new Counting(out);
		counting.write("%PDF-1.4\n");
		offsets[1] = counting.count;
		counting.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
		offsets[2] = counting.count;
		StringBuilder kids = new StringBuilder();
		for (int i = 0; i < n; i++)
			kids.append(3 + i * 3).append(" 0 R ");
		counting.write("2 0 obj\n<< /Type /Pages /Kids [ " + kids + "] /Count " + n + " >>\nendobj\n");
		for (int i = 0; i < n; i++) {
			PageInfo info = mPages.get(i);
			Bitmap bitmap = mBitmaps.get(i);
			int page = 3 + i * 3, content = page + 1, image = page + 2;
			offsets[page] = counting.count;
			counting.write(page + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + info.mPageWidth + " " + info.mPageHeight
			               + "] /Resources << /XObject << /Im0 " + image + " 0 R >> >> /Contents " + content
			               + " 0 R >>\nendobj\n");
			String draw = "q " + info.mPageWidth + " 0 0 " + info.mPageHeight + " 0 0 cm /Im0 Do Q";
			offsets[content] = counting.count;
			counting.write(content + " 0 obj\n<< /Length " + draw.length() + " >>\nstream\n" + draw + "\nendstream\nendobj\n");
			byte[] rgb = deflate(rgbOf(bitmap));
			offsets[image] = counting.count;
			counting.write(image + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + info.mPageWidth + " /Height "
			               + info.mPageHeight + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
			               + rgb.length + " >>\nstream\n");
			counting.write(rgb);
			counting.write("\nendstream\nendobj\n");
		}
		long xref = counting.count;
		StringBuilder table = new StringBuilder("xref\n0 " + offsets.length + "\n0000000000 65535 f \n");
		for (int i = 1; i < offsets.length; i++)
			table.append(String.format("%010d 00000 n \n", offsets[i]));
		counting.write(table + "trailer\n<< /Size " + offsets.length + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
		out.flush();
	}

	public List<PageInfo> getPages() {
		return Collections.unmodifiableList(mPages);
	}

	public void close() {
		throwIfCurrentPageNotFinished();
		dispose();
	}

	private void dispose() {
		mClosed = true;
		mBitmaps.clear();
	}

	private void throwIfClosed() {
		if (mClosed) {
			throw new IllegalStateException("document is closed!");
		}
	}

	private void throwIfCurrentPageNotFinished() {
		if (mCurrentPage != null) {
			throw new IllegalStateException("Current page not finished!");
		}
	}

	private static byte[] rgbOf(Bitmap bitmap) {
		int w = bitmap.getWidth(), h = bitmap.getHeight();
		int[] pixels = new int[w * h];
		bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
		byte[] rgb = new byte[w * h * 3];
		for (int i = 0; i < pixels.length; i++) {
			rgb[i * 3] = (byte)(pixels[i] >> 16);
			rgb[i * 3 + 1] = (byte)(pixels[i] >> 8);
			rgb[i * 3 + 2] = (byte)pixels[i];
		}
		return rgb;
	}

	private static byte[] deflate(byte[] data) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DeflaterOutputStream deflater = new DeflaterOutputStream(bytes)) {
			deflater.write(data);
		}
		return bytes.toByteArray();
	}

	/* writes to the stream and counts the bytes for the xref table */
	private static final class Counting {
		final OutputStream out;
		long count;

		Counting(OutputStream out) {
			this.out = out;
		}

		void write(String s) throws IOException {
			write(s.getBytes(StandardCharsets.ISO_8859_1));
		}

		void write(byte[] b) throws IOException {
			out.write(b);
			count += b.length;
		}
	}

	private final class PdfCanvas extends Canvas {
		public PdfCanvas(Bitmap bitmap) {
			super(bitmap);
		}

		@Override
		public void setBitmap(Bitmap bitmap) {
			throw new UnsupportedOperationException();
		}
	}

	public static final class PageInfo {
		private int mPageWidth;
		private int mPageHeight;
		private Rect mContentRect;
		private int mPageNumber;

		private PageInfo() {}

		public int getPageWidth() {
			return mPageWidth;
		}

		public int getPageHeight() {
			return mPageHeight;
		}

		public Rect getContentRect() {
			return mContentRect;
		}

		public int getPageNumber() {
			return mPageNumber;
		}

		public static final class Builder {
			private final PageInfo mPageInfo = new PageInfo();

			public Builder(int pageWidth, int pageHeight, int pageNumber) {
				if (pageWidth <= 0) {
					throw new IllegalArgumentException("page width must be positive");
				}
				if (pageHeight <= 0) {
					throw new IllegalArgumentException("page width must be positive");
				}
				if (pageNumber < 0) {
					throw new IllegalArgumentException("pageNumber must be non negative");
				}
				mPageInfo.mPageWidth = pageWidth;
				mPageInfo.mPageHeight = pageHeight;
				mPageInfo.mPageNumber = pageNumber;
			}

			public Builder setContentRect(Rect contentRect) {
				if (contentRect != null
				    && (contentRect.left < 0 || contentRect.top < 0 || contentRect.right > mPageInfo.mPageWidth
				        || contentRect.bottom > mPageInfo.mPageHeight)) {
					throw new IllegalArgumentException("contentRect does not fit the page");
				}
				mPageInfo.mContentRect = contentRect;
				return this;
			}

			public PageInfo create() {
				if (mPageInfo.mContentRect == null) {
					mPageInfo.mContentRect = new Rect(0, 0, mPageInfo.mPageWidth, mPageInfo.mPageHeight);
				}
				return mPageInfo;
			}
		}
	}

	public static final class Page {
		private final PageInfo mPageInfo;
		private final Bitmap mBitmap;
		private Canvas mCanvas;

		private Page(Canvas canvas, PageInfo pageInfo, Bitmap bitmap) {
			mCanvas = canvas;
			mPageInfo = pageInfo;
			mBitmap = bitmap;
		}

		public Canvas getCanvas() {
			return mCanvas;
		}

		public PageInfo getInfo() {
			return mPageInfo;
		}

		boolean isFinished() {
			return mCanvas == null;
		}

		private void finish() {
			mCanvas = null;
		}
	}
}
