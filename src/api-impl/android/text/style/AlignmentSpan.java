package android.text.style;

import android.text.Layout;

public interface AlignmentSpan extends ParagraphStyle {

	Layout.Alignment getAlignment();

	public static class Standard implements AlignmentSpan {

		private final Layout.Alignment mAlignment;

		public Standard(Layout.Alignment alignment) {
			mAlignment = alignment;
		}

		/* AOSP: the alignment is read as its name */
		public Standard(android.os.Parcel src) {
			mAlignment = Layout.Alignment.valueOf(src.readString());
		}

		@Override
		public Layout.Alignment getAlignment() {
			return mAlignment;
		}
	}
}
