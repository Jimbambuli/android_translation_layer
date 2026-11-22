package android.text.method;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

/* AOSP core/java/android/text/method/LinkMovementMethod.java (Android 16): getInstance() and onTouchEvent(). ATL's
 * TextView activates the links of a label text itself (Pango markup links, TextView.onLinkActivated()); the touches
 * of a text it lays out with a StaticLayout (paragraph spans) come here from TextView.onTouchEvent(), as do those
 * of apps that call it, e.g. from their own OnTouchListener. */
public class LinkMovementMethod extends ScrollingMovementMethod {

	private static LinkMovementMethod sInstance;

	@Override
	public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
		int action = event.getAction();

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
			int x = (int)event.getX();
			int y = (int)event.getY();

			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();

			x += widget.getScrollX();
			y += widget.getScrollY();

			Layout layout = widget.getLayout();
			ClickableSpan[] links;
			if (y < 0 || y > layout.getHeight()) {
				links = null;
			} else {
				int line = layout.getLineForVertical(y);
				if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) {
					links = null;
				} else {
					int off = layout.getOffsetForHorizontal(line, x);
					links = buffer.getSpans(off, off, ClickableSpan.class);
				}
			}

			if (links != null && links.length != 0) {
				ClickableSpan link = links[0];
				if (action == MotionEvent.ACTION_UP) {
					// AOSP: a TextLinkSpan gets onClick(widget, INVOCATION_METHOD_TOUCH); ATL has no TextLinkSpan
					link.onClick(widget);
				} else if (action == MotionEvent.ACTION_DOWN) {
					// AOSP hides the floating text toolbar here first; ATL has none
					Selection.setSelection(buffer,
					                       buffer.getSpanStart(link),
					                       buffer.getSpanEnd(link));
				}
				return true;
			} else {
				Selection.removeSelection(buffer);
			}
		}

		return super.onTouchEvent(widget, buffer, event);
	}

	public static MovementMethod getInstance() {
		if (sInstance == null)
			sInstance = new LinkMovementMethod();

		return sInstance;
	}
}
