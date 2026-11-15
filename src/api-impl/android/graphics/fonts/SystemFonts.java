package android.graphics.fonts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* AOSP graphics/fonts/SystemFonts.getAvailableFonts() reads the font files of /system/fonts and the OEM font
 * directory into an unmodifiable set (cached in sAvailableFonts until resetAvailableFonts()). The font
 * directories of a desktop are the ones fontconfig uses; their .ttf/.otf files take that place.
 * (.ttc collections are left out: Font here carries no per-face handling of a collection.) */
public final class SystemFonts {
	private static final Object LOCK = new Object();
	private static Set<Font> sAvailableFonts;

	private SystemFonts() {}

	public static Set<Font> getAvailableFonts() {
		synchronized (LOCK) {
			if (sAvailableFonts != null)
				return sAvailableFonts;
			List<File> files = new ArrayList<>();
			String home = System.getProperty("user.home");
			String[] dirs = {"/usr/share/fonts", "/usr/local/share/fonts", home + "/.local/share/fonts", home + "/.fonts"};
			for (String dir : dirs)
				collect(new File(dir), files);
			Set<Font> set = new HashSet<>();
			for (File file : files) {
				try {
					set.add(new Font.Builder(file).build());
				} catch (IOException e) {
					// as in AOSP buildSystemFallback(): a file that cannot be read is skipped
				}
			}
			sAvailableFonts = Collections.unmodifiableSet(set);
			return sAvailableFonts;
		}
	}

	/** @hide */
	public static void resetAvailableFonts() {
		synchronized (LOCK) {
			sAvailableFonts = null;
		}
	}

	private static void collect(File dir, List<File> out) {
		File[] entries = dir.listFiles();
		if (entries == null)
			return;
		for (File f : entries) {
			if (f.isDirectory()) {
				collect(f, out);
			} else {
				String n = f.getName().toLowerCase();
				if (n.endsWith(".ttf") || n.endsWith(".otf"))
					out.add(f);
			}
		}
	}
}
