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
 * holds a name; the View trees of the resumed Activity, of showing Dialogs and of views added
 * through WindowManager are then written to <dir>/<name>.xml in the form of `uiautomator dump`
 * (node class/resource-id/text/content-desc/bounds), plus ATL-only attributes: real class,
 * visibility, alpha, isShown, Java bounds, and which ancestor hides a view. <dir>/request is
 * deleted when done. */
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

	/* a Dialog or a root View added through WindowManager */
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

	private static void poll() {
		File req = new File(DIR, "request");
		if (req.exists()) {
			try {
				String name = new String(Files.readAllBytes(req.toPath()), StandardCharsets.UTF_8).trim();
				File tmp = new File(DIR, name + ".xml.tmp");
				try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
					dump(w);
				}
				tmp.renameTo(new File(DIR, name + ".xml"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			req.delete();
		}
		handler.postDelayed(POLL, 250);
	}

	private static void dump(Writer w) throws java.io.IOException {
		w.write("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n<hierarchy rotation=\"0\" atl=\"1\">\n");
		Activity a = activity.get();
		if (a != null)
			window(w, "activity " + a.getClass().getName(), a.getWindow().getDecorView());
		for (WeakReference<Object> ref : roots) {
			Object o = ref.get();
			if (o instanceof Dialog && ((Dialog)o).isShowing())
				window(w, "dialog " + o.getClass().getName(), ((Dialog)o).getWindow().getDecorView());
			else if (o instanceof View)
				window(w, "wm " + o.getClass().getName(), (View)o);
		}
		w.write("</hierarchy>\n");
	}

	private static void window(Writer w, String what, View root) throws java.io.IOException {
		w.write("<window atl-what=\"" + esc(what) + "\">\n");
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
