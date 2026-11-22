package android.atl;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

/* DEBUGGING AID for atl-doctor (https://github.com/Jimbambuli/atl-doctor).
 *
 * With ATL_VIEWDUMP=<dir> set, the main thread checks every 250 ms for <dir>/request. The file
 * holds a name in its first line; the View trees of the resumed Activity, of showing Dialogs and of views added
 * through WindowManager are then written to <dir>/<name>.xml in the form of `uiautomator dump`
 * (node class/resource-id/text/content-desc/bounds), plus ATL-only attributes: real class,
 * visibility, alpha, isShown, Java bounds, and which ancestor hides a view. Popups that GTK shows
 * without Android views (a Spinner's list) and toplevels that show GTK widgets instead of
 * views (ATL's AlertDialog) follow as "gtk" windows of GTK widgets. Each
 * window names the GTK toplevel whose surface its bounds are in (atl-toplevel, 0: the activity's
 * window) and that surface's size and widget origin. <dir>/request is deleted when done. Only if a
 * further line of the request reads "shot" are the windows of the app also saved: the activity's
 * window as <dir>/shots/<name>.png, other toplevels (dialogs) as <dir>/shots/<name>.<toplevel>.png,
 * each with the popups shown on it; otherwise no image is rendered. */
public class ATLViewDump {
	private static final String DIR = System.getenv("ATL_VIEWDUMP");
	private static Handler handler;
	private static final Runnable POLL = new Runnable() {
		@Override
		public void run() {
			poll();
		}
	};
	private static WeakReference<Activity> activity = new WeakReference<>(null);
	private static final ArrayList<WeakReference<Object>> roots = new ArrayList<>();

	public static void resumed(Activity a) {
		if (DIR == null)
			return;
		activity = new WeakReference<>(a);
		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(POLL, 250);
		}
	}

	/* a Dialog, a PopupWindow or a root View added through WindowManager */
	public static void addRoot(Object root) {
		if (DIR != null)
			roots.add(new WeakReference<>(root));
	}

	public static void removeRoot(Object root) {
		for (Iterator<WeakReference<Object>> it = roots.iterator(); it.hasNext();) {
			Object o = it.next().get();
			if (o == null || o == root)
				it.remove();
		}
	}

	/* a window image that waits for the next frame (see shot()): its name and how often it was tried */
	private static String pendingShot;
	private static int shotTries;
	private static final int SHOT_TRIES = 40; // every 50 ms, for 2 s

	private static void poll() {
		File req = new File(DIR, "request");
		if (pendingShot != null) { // the request is answered (deleted) when the image is there or given up
			if (shot(pendingShot, ++shotTries >= SHOT_TRIES)) {
				pendingShot = null;
				req.delete();
			}
			handler.postDelayed(POLL, pendingShot != null ? 50 : 250);
			return;
		}
		if (req.exists()) {
			try {
				String[] lines = new String(Files.readAllBytes(req.toPath()), StandardCharsets.UTF_8).trim().split("\\s*\\n\\s*");
				String name = lines[0].trim();
				boolean wantShot = false;
				for (int i = 1; i < lines.length; i++)
					wantShot |= lines[i].equals("shot");
				File tmp = new File(DIR, name + ".xml.tmp");
				try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
					dump(w);
				}
				tmp.renameTo(new File(DIR, name + ".xml"));
				if (wantShot && !shot(name, false)) {
					pendingShot = name;
					shotTries = 0;
					handler.postDelayed(POLL, 50);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			req.delete();
		}
		handler.postDelayed(POLL, 250);
	}

	private static long mainWindow() {
		Activity a = activity.get();
		return a != null && a.getWindow() != null ? a.getWindow().native_window : 0;
	}

	/* the windows of the app (not the screen) as <dir>/shots/<name>.png (the activity's window) and
	 * <name>.<toplevel>.png (further toplevels), the same name as the tree */
	/* false: the image waits for the next frame (a window that is to be drawn anew has no drawing to render
	 * until then: GTK drops a widget's render node when it queues a redraw, e.g. during an animation); with
	 * last, it is given up instead */
	private static boolean shot(String name, boolean last) {
		long window = mainWindow();
		if (window == 0) {
			System.err.println("ATLViewDump: no window, no screenshot");
			return true;
		}
		File dir = new File(DIR, "shots");
		dir.mkdirs();
		int n = native_screenshot(window, new File(dir, name).getPath(), last);
		if (n < 0)
			return false;
		if (n > 0)
			System.err.println("ATLViewDump: window image " + name + (n > 1 ? " (" + n + " toplevels)" : ""));
		else
			System.err.println("ATLViewDump: screenshot failed");
		return true;
	}

	/* renders each toplevel with its popups to <prefix>.png / <prefix>.<toplevel>.png (written as .tmp
	 * and renamed); returns the number of images written, or -1 if a toplevel has no drawing until its next
	 * frame (unless last) */
	private static native int native_screenshot(long window, String prefix, boolean last);
	/* the index of the toplevel that holds the widget (0: window), -1 if it is in none */
	private static native int native_toplevel(long window, long widget);
	/* " atl-surface=..." attributes of a toplevel: surface size and the widget origin in it */
	private static native String native_surface(long window, int toplevel);
	/* <window> elements for the mapped GtkPopovers that are not the natives of the given widgets, and for
	 * the further toplevels that hold none of them */
	private static native String native_gtkPopups(long window, long[] androidRoots);

	private static void dump(Writer w) throws java.io.IOException {
		w.write("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n<hierarchy rotation=\"0\" atl=\"1\">\n");
		long main = mainWindow();
		ArrayList<Long> shown = new ArrayList<>();
		Activity a = activity.get();
		if (a != null)
			window(w, "activity " + a.getClass().getName(), a.getWindow().getDecorView(), main, shown, 0);
		for (WeakReference<Object> ref : roots) {
			Object o = ref.get();
			if (o instanceof Dialog && ((Dialog)o).isShowing())
				window(w, "dialog " + o.getClass().getName(), ((Dialog)o).getWindow().getDecorView(), main, shown, ((Dialog)o).getWindow().native_window);
			else if (o instanceof android.widget.PopupWindow && ((android.widget.PopupWindow)o).isShowing())
				window(w, "popup " + o.getClass().getName(), ((android.widget.PopupWindow)o).getContentView().getRootView(), main, shown, 0);
			else if (o instanceof View)
				window(w, "wm " + o.getClass().getName(), (View)o, main, shown, 0);
		}
		if (main != 0) {
			long[] widgets = new long[shown.size()];
			for (int i = 0; i < widgets.length; i++)
				widgets[i] = shown.get(i);
			w.write(native_gtkPopups(main, widgets));
		}
		w.write("</hierarchy>\n");
	}

	/* in: a widget of the toplevel the window is shown in (0: the root's widget; a dialog names its GtkWindow,
	 * as ATL's AlertDialog shows GTK widgets instead of its decor view) */
	private static void window(Writer w, String what, View root, long main, ArrayList<Long> shown, long in) throws java.io.IOException {
		if (in == 0)
			in = root.widget;
		int toplevel = main != 0 && in != 0 ? native_toplevel(main, in) : -1;
		if (root.widget != 0)
			shown.add(root.widget);
		w.write("<window atl-what=\"" + esc(what) + "\"" + (toplevel >= 0 ? " atl-toplevel=\"" + toplevel + "\"" + native_surface(main, toplevel) : "") + ">\n");
		node(w, root, 0, 0, 0, null, 1);
		w.write("</window>\n");
	}

	/* the class TalkBack/uiautomator would report: the nearest framework widget class */
	private static String a11yClass(Class<?> c) {
		for (; c != null; c = c.getSuperclass()) {
			String n = c.getName();
			if (n.startsWith("android.widget.") || n.startsWith("android.view.") || n.startsWith("android.webkit."))
				return n;
		}
		return "android.view.View";
	}

	/* x, y: Java position of the parent's content origin in the window; hider: first ancestor that is
	 * not VISIBLE */
	private static void node(Writer w, View v, int index, int x, int y, String hider, int depth) throws java.io.IOException {
		int l = x + v.getLeft() + Math.round(v.getTranslationX());
		int t = y + v.getTop() + Math.round(v.getTranslationY());
		String id = "";
		if (v.getId() != View.NO_ID) {
			try {
				id = v.getResources().getResourceName(v.getId());
			} catch (Exception e) {
				id = "0x" + Integer.toHexString(v.getId());
			}
		}
		CharSequence text = v instanceof TextView ? ((TextView)v).getText() : null;
		CharSequence desc = v.getContentDescription();
		Rect g = new Rect();
		boolean gok = v.getGlobalVisibleRect(g);
		String vis = v.getVisibility() == View.VISIBLE ? "visible" : v.getVisibility() == View.INVISIBLE ? "invisible" : "gone";
		if (hider == null && v.getVisibility() != View.VISIBLE)
			hider = (id.isEmpty() ? v.getClass().getName() : id) + "=" + vis;
		indent(w, depth);
		w.write("<node index=\"" + index + "\" text=\"" + esc(text) + "\" resource-id=\"" + esc(id) + "\" class=\"" + a11yClass(v.getClass())
		        + "\" content-desc=\"" + esc(desc) + "\" checkable=\"" + (v instanceof Checkable) + "\" checked=\"" + (v instanceof Checkable && ((Checkable)v).isChecked())
		        + "\" clickable=\"" + v.isClickable() + "\" enabled=\"" + v.isEnabled() + "\" focusable=\"" + v.isFocusable() + "\" selected=\"" + v.isSelected()
		        + "\" bounds=\"" + (gok ? "[" + g.left + "," + g.top + "][" + g.right + "," + g.bottom + "]" : "[" + l + "," + t + "][" + (l + v.getWidth()) + "," + (t + v.getHeight()) + "]")
		        + "\" atl-class=\"" + esc(v.getClass().getName()) + "\" atl-visibility=\"" + vis + "\" atl-alpha=\"" + v.getAlpha() + "\" atl-shown=\"" + v.isShown()
		        + "\" atl-gtk-rect=\"" + gok + "\" atl-jbounds=\"[" + l + "," + t + "][" + (l + v.getWidth()) + "," + (t + v.getHeight()) + "]\""
		        + (hider != null ? " atl-hidden-by=\"" + esc(hider) + "\"" : "")
		        + (v.getAnimation() != null ? " atl-animation=\"" + esc(v.getAnimation().getClass().getName()) + "\"" : "")
		        + (v instanceof android.widget.ImageView ? " atl-drawable=\"" + esc(drawableInfo(((android.widget.ImageView)v).getDrawable())) + "\"" : ""));
		if (v instanceof ViewGroup && ((ViewGroup)v).getChildCount() > 0) {
			w.write(">\n");
			ViewGroup vg = (ViewGroup)v;
			for (int i = 0; i < vg.getChildCount(); i++)
				node(w, vg.getChildAt(i), i, l - v.getScrollX(), t - v.getScrollY(), hider, depth + 1);
			indent(w, depth);
			w.write("</node>\n");
		} else {
			w.write(" />\n");
		}
	}

	private static void indent(Writer w, int depth) throws java.io.IOException {
		for (int i = 0; i < depth; i++)
			w.write("  ");
	}

	private static String esc(CharSequence s) {
		if (s == null)
			return "";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&': b.append("&amp;"); break;
				case '<': b.append("&lt;"); break;
				case '>': b.append("&gt;"); break;
				case '"': b.append("&quot;"); break;
				case '\n': b.append("&#10;"); break;
				default: if (c >= 0x20 || c == '\t') b.append(c);
			}
		}
		return b.toString();
	}

	/* the class of a drawable and, for a layer list, of its layers (debugging aid) */
	private static String drawableInfo(android.graphics.drawable.Drawable d) {
		if (d == null)
			return "";
		StringBuilder sb = new StringBuilder(d.getClass().getName());
		if (d instanceof android.graphics.drawable.LayerDrawable) {
			android.graphics.drawable.LayerDrawable ld = (android.graphics.drawable.LayerDrawable)d;
			sb.append('[');
			for (int i = 0; i < ld.getNumberOfLayers(); i++)
				sb.append(i > 0 ? "," : "").append(drawableInfo(ld.getDrawable(i)));
			sb.append(']');
		}
		return sb.toString();
	}
}
