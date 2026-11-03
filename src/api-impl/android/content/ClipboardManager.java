/**
 * Copyright (c) 2010, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Interface to the clipboard service, for placing and retrieving text in
 * the global clipboard.
 *
 * <p>
 * You do not instantiate this class directly; instead, retrieve it through
 * {@link android.content.Context#getSystemService}.
 *
 * <p>
 * The ClipboardManager API itself is very simple: it consists of methods
 * to atomically get and set the current primary clipboard data.  That data
 * is expressed as a {@link ClipData} object, which defines the protocol
 * for data exchange between applications.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about using the clipboard framework, read the
 * <a href="{@docRoot}guide/topics/clipboard/copy-paste.html">Copy and Paste</a>
 * developer guide.</p>
 * </div>
 *
 * @see android.content.Context#getSystemService
 */
public class ClipboardManager extends android.text.ClipboardManager {
	static final int MSG_REPORT_PRIMARY_CLIP_CHANGED = 1;
	/**
	 * Defines a listener callback that is invoked when the primary clip on the clipboard changes.
	 * Objects that want to register a listener call
	 * {@link android.content.ClipboardManager#addPrimaryClipChangedListener(OnPrimaryClipChangedListener)
	 * addPrimaryClipChangedListener()} with an
	 * object that implements OnPrimaryClipChangedListener.
	 *
	 */
	public interface OnPrimaryClipChangedListener {
		/**
		 * Callback that is invoked by {@link android.content.ClipboardManager} when the primary
		 * clip changes.
		 */
		void onPrimaryClipChanged();
	}

	/* ATL: the clipboard service's state. GDK reads the system clipboard asynchronously, so another
	 * application's clip is read when the clipboard changes and kept here, like Android's ClipboardService
	 * keeps the primary clip: text as text, an image as a PNG file in the cache that the clip names with a
	 * content URI (ContentResolver.atlUriForFile()), as an app on Android gets an image from another app.
	 * The listeners are called on the main thread after the change, as AOSP posts them to its handler. */
	private static ClipData primaryClip;
	private static final ArrayList<OnPrimaryClipChangedListener> primaryClipChangedListeners = new ArrayList<OnPrimaryClipChangedListener>();
	private static boolean nativeInitDone;
	private static Context sContext;
	private static Handler sHandler;
	private static File pastedImage;	/* the file of the desktop's last image */

	/**
	 * {@hide}
	 */
	public ClipboardManager(Context context, Handler handler) {
		if (sContext == null && context != null)
			sContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
		if (sHandler == null)
			sHandler = new Handler(Looper.getMainLooper());
		if (!nativeInitDone) {
			nativeInitDone = true;
			native_init();
		}
	}
	public ClipboardManager() {
		this(null, null);
	}
	/**
	 * Sets the current primary clip on the clipboard.  This is the clip that
	 * is involved in normal cut and paste operations.
	 *
	 * @param clip The clipped data item to set.
	 */
	public void setPrimaryClip(ClipData clip) {
		if (clip == null)
			throw new NullPointerException("clip");
		primaryClip = clip;
		publish(clip);
		postPrimaryClipChanged();
	}

	/* the clip on the desktop's clipboard (not the primary selection of middle click paste): the first item's
	 * image if the clip is one (read through its content URI), and its text */
	private static void publish(ClipData clip) {
		if (clip.getItemCount() == 0) {
			native_set_clipboard(null, null, null);
			return;
		}
		ClipData.Item item = clip.getItemAt(0);
		String type = null;
		byte[] data = null;
		Uri uri = item.getUri();
		if (uri != null && clip.getDescription().hasMimeType("image/*") && sContext != null) {
			ContentResolver resolver = sContext.getContentResolver();
			type = resolver.getType(uri);
			if (type == null || !type.startsWith("image/"))
				type = clip.getDescription().filterMimeTypes("image/*")[0];
			data = readAll(resolver, uri);
			if (data == null || type.endsWith("/*"))
				type = null;
		}
		String text = null;
		if (item.getText() != null || item.getHtmlText() != null || type == null)
			text = String.valueOf(item.coerceToText(sContext));
		native_set_clipboard(text, type, type != null ? data : null);
	}

	private static byte[] readAll(ContentResolver resolver, Uri uri) {
		try (InputStream in = resolver.openInputStream(uri)) {
			if (in == null)
				return null;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[65536];
			for (int n; (n = in.read(buf)) > 0;)
				out.write(buf, 0, n);
			return out.toByteArray();
		} catch (IOException | SecurityException e) {
			Log.w("ClipboardManager", "cannot read " + uri + ": " + e);
			return null;
		}
	}
	/**
	 * Clears any current primary clip on the clipboard.
	 */
	public void clearPrimaryClip() {
		primaryClip = null;
		native_set_clipboard(null, null, null);
		postPrimaryClipChanged();
	}
	/**
	 * Returns the current primary clip on the clipboard.
	 */
	public ClipData getPrimaryClip() {
		return primaryClip;
	}
	/**
	 * Returns a description of the current primary clip on the clipboard
	 * but not a copy of its data.
	 */
	public ClipDescription getPrimaryClipDescription() {
		return primaryClip != null ? primaryClip.getDescription() : null;
	}
	/**
	 * Returns true if there is currently a primary clip on the clipboard.
	 */
	public boolean hasPrimaryClip() {
		return primaryClip != null;
	}
	public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
		synchronized (primaryClipChangedListeners) {
			primaryClipChangedListeners.add(what);
		}
	}
	public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
		synchronized (primaryClipChangedListeners) {
			primaryClipChangedListeners.remove(what);
		}
	}
	/**
	 * @deprecated Use {@link #getPrimaryClip()} instead.  This retrieves
	 * the primary clip and tries to coerce it to a string.
	 */
	public CharSequence getText() {
		ClipData clip = getPrimaryClip();
		if (clip != null && clip.getItemCount() > 0) {
			return clip.getItemAt(0).coerceToText(null);
		}
		return null;
	}
	/**
	 * @deprecated Use {@link #setPrimaryClip(ClipData)} instead.  This
	 * creates a ClippedItem holding the given text and sets it as the
	 * primary clip.  It has no label or icon.
	 */
	public void setText(CharSequence text) {
		setPrimaryClip(ClipData.newPlainText(null, text));
	}
	/**
	 * @deprecated Use {@link #hasPrimaryClip()} instead.
	 */
	public boolean hasText() {
		CharSequence text = getText();
		return text != null && text.length() > 0;
	}
	static void reportPrimaryClipChanged() {
		Object[] listeners;
		synchronized (primaryClipChangedListeners) {
			listeners = primaryClipChangedListeners.toArray();
		}
		for (Object listener : listeners) {
			((OnPrimaryClipChangedListener)listener).onPrimaryClipChanged();
		}
	}

	/* AOSP's service calls the listeners through the handler of the app's main thread */
	private static void postPrimaryClipChanged() {
		if (sHandler == null)
			sHandler = new Handler(Looper.getMainLooper());
		sHandler.post(new Runnable() {
			@Override
			public void run() {
				reportPrimaryClipChanged();
			}
		});
	}

	/* called on the main thread when another application put text (or nothing that is text) on the clipboard */
	private static void onSystemClipboardText(String text) {
		primaryClip = text != null ? ClipData.newPlainText(null, text) : null;
		postPrimaryClipChanged();
	}

	/* called on the main thread when another application put an image on the clipboard, as PNG */
	private static void onSystemClipboardImage(byte[] png) {
		if (sContext == null) {
			onSystemClipboardText(null);
			return;
		}
		File dir = new File(sContext.getCacheDir(), "clipboard");
		File file = new File(dir, "image-" + System.currentTimeMillis() + ".png");
		dir.mkdirs();
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(png);
		} catch (IOException e) {
			Log.w("ClipboardManager", "cannot keep the clipboard image: " + e);
			onSystemClipboardText(null);
			return;
		}
		if (pastedImage != null)
			pastedImage.delete();
		pastedImage = file;
		primaryClip = new ClipData(null, new String[] {"image/png"}, new ClipData.Item(ContentResolver.atlUriForFile(file.getPath())));
		postPrimaryClipChanged();
	}

	private static native void native_init();
	private static native void native_set_clipboard(String text, String mimeType, byte[] data);
}
