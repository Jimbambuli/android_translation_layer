package android.os;

import dalvik.system.VMDebug;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Debug {
	/* AOSP fills it from /proc/<pid>/smaps; ATL fills nothing (getMemoryInfo()), so all values are 0 */
	public static class MemoryInfo implements Parcelable {
		/** The proportional set size for dalvik heap.  (Doesn't include other Dalvik overhead.) */
		public int dalvikPss;
		/** The proportional set size that is swappable for dalvik heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int dalvikSwappablePss;
		/** @hide The resident set size for dalvik heap.  (Without other Dalvik overhead.) */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int dalvikRss;
		/** The private dirty pages used by dalvik heap. */
		public int dalvikPrivateDirty;
		/** The shared dirty pages used by dalvik heap. */
		public int dalvikSharedDirty;
		/** The private clean pages used by dalvik heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int dalvikPrivateClean;
		/** The shared clean pages used by dalvik heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int dalvikSharedClean;
		/** The dirty dalvik pages that have been swapped out. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int dalvikSwappedOut;
		/** The dirty dalvik pages that have been swapped out, proportional. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int dalvikSwappedOutPss;

		/** The proportional set size for the native heap. */
		public int nativePss;
		/** The proportional set size that is swappable for the native heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int nativeSwappablePss;
		/** @hide The resident set size for the native heap. */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int nativeRss;
		/** The private dirty pages used by the native heap. */
		public int nativePrivateDirty;
		/** The shared dirty pages used by the native heap. */
		public int nativeSharedDirty;
		/** The private clean pages used by the native heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int nativePrivateClean;
		/** The shared clean pages used by the native heap. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int nativeSharedClean;
		/** The dirty native pages that have been swapped out. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int nativeSwappedOut;
		/** The dirty native pages that have been swapped out, proportional. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int nativeSwappedOutPss;

		/** The proportional set size for everything else. */
		public int otherPss;
		/** The proportional set size that is swappable for everything else. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int otherSwappablePss;
		/** @hide The resident set size for everything else. */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int otherRss;
		/** The private dirty pages used by everything else. */
		public int otherPrivateDirty;
		/** The shared dirty pages used by everything else. */
		public int otherSharedDirty;
		/** The private clean pages used by everything else. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int otherPrivateClean;
		/** The shared clean pages used by everything else. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int otherSharedClean;
		/** The dirty pages used by anyting else that have been swapped out. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage
		public int otherSwappedOut;
		/** The dirty pages used by anyting else that have been swapped out, proportional. */
		/** @hide We may want to expose this, eventually. */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public int otherSwappedOutPss;

		/** Whether the kernel reports proportional swap usage */
		/** @hide */
		//@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
		public boolean hasSwappedOutPss;

		/** @hide */
		public static final int HEAP_UNKNOWN = 0;
		/** @hide */
		public static final int HEAP_DALVIK = 1;
		/** @hide */
		public static final int HEAP_NATIVE = 2;

		/** @hide */
		public static final int OTHER_DALVIK_OTHER = 0;
		/** @hide */
		public static final int OTHER_STACK = 1;
		/** @hide */
		public static final int OTHER_CURSOR = 2;
		/** @hide */
		public static final int OTHER_ASHMEM = 3;
		/** @hide */
		public static final int OTHER_GL_DEV = 4;
		/** @hide */
		public static final int OTHER_UNKNOWN_DEV = 5;
		/** @hide */
		public static final int OTHER_SO = 6;
		/** @hide */
		public static final int OTHER_JAR = 7;
		/** @hide */
		public static final int OTHER_APK = 8;
		/** @hide */
		public static final int OTHER_TTF = 9;
		/** @hide */
		public static final int OTHER_DEX = 10;
		/** @hide */
		public static final int OTHER_OAT = 11;
		/** @hide */
		public static final int OTHER_ART = 12;
		/** @hide */
		public static final int OTHER_UNKNOWN_MAP = 13;
		/** @hide */
		public static final int OTHER_GRAPHICS = 14;
		/** @hide */
		public static final int OTHER_GL = 15;
		/** @hide */
		public static final int OTHER_OTHER_MEMTRACK = 16;

		// Needs to be declared here for the DVK_STAT ranges below.
		/** @hide */
		//@UnsupportedAppUsage
		public static final int NUM_OTHER_STATS = 17;

		// Dalvik subsections.
		/** @hide */
		public static final int OTHER_DALVIK_NORMAL = 17;
		/** @hide */
		public static final int OTHER_DALVIK_LARGE = 18;
		/** @hide */
		public static final int OTHER_DALVIK_ZYGOTE = 19;
		/** @hide */
		public static final int OTHER_DALVIK_NON_MOVING = 20;
		// Section begins and ends for dumpsys, relative to the DALVIK categories.
		/** @hide */
		public static final int OTHER_DVK_STAT_DALVIK_START =
				OTHER_DALVIK_NORMAL - NUM_OTHER_STATS;
		/** @hide */
		public static final int OTHER_DVK_STAT_DALVIK_END =
				OTHER_DALVIK_NON_MOVING - NUM_OTHER_STATS;

		// Dalvik Other subsections.
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_LINEARALLOC = 21;
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_ACCOUNTING = 22;
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_ZYGOTE_CODE_CACHE = 23;
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_APP_CODE_CACHE = 24;
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_COMPILER_METADATA = 25;
		/** @hide */
		public static final int OTHER_DALVIK_OTHER_INDIRECT_REFERENCE_TABLE = 26;
		/** @hide */
		public static final int OTHER_DVK_STAT_DALVIK_OTHER_START =
				OTHER_DALVIK_OTHER_LINEARALLOC - NUM_OTHER_STATS;
		/** @hide */
		public static final int OTHER_DVK_STAT_DALVIK_OTHER_END =
				OTHER_DALVIK_OTHER_INDIRECT_REFERENCE_TABLE - NUM_OTHER_STATS;

		// Dex subsections (Boot vdex, App dex, and App vdex).
		/** @hide */
		public static final int OTHER_DEX_BOOT_VDEX = 27;
		/** @hide */
		public static final int OTHER_DEX_APP_DEX = 28;
		/** @hide */
		public static final int OTHER_DEX_APP_VDEX = 29;
		/** @hide */
		public static final int OTHER_DVK_STAT_DEX_START = OTHER_DEX_BOOT_VDEX - NUM_OTHER_STATS;
		/** @hide */
		public static final int OTHER_DVK_STAT_DEX_END = OTHER_DEX_APP_VDEX - NUM_OTHER_STATS;

		// Art subsections (App image, boot image).
		/** @hide */
		public static final int OTHER_ART_APP = 30;
		/** @hide */
		public static final int OTHER_ART_BOOT = 31;
		/** @hide */
		public static final int OTHER_DVK_STAT_ART_START = OTHER_ART_APP - NUM_OTHER_STATS;
		/** @hide */
		public static final int OTHER_DVK_STAT_ART_END = OTHER_ART_BOOT - NUM_OTHER_STATS;

		/** @hide */
		//@UnsupportedAppUsage
		public static final int NUM_DVK_STATS = OTHER_ART_BOOT + 1 - OTHER_DALVIK_NORMAL;

		/** @hide */
		public static final int NUM_CATEGORIES = 9;

		/** @hide */
		public static final int OFFSET_PSS = 0;
		/** @hide */
		public static final int OFFSET_SWAPPABLE_PSS = 1;
		/** @hide */
		public static final int OFFSET_RSS = 2;
		/** @hide */
		public static final int OFFSET_PRIVATE_DIRTY = 3;
		/** @hide */
		public static final int OFFSET_SHARED_DIRTY = 4;
		/** @hide */
		public static final int OFFSET_PRIVATE_CLEAN = 5;
		/** @hide */
		public static final int OFFSET_SHARED_CLEAN = 6;
		/** @hide */
		public static final int OFFSET_SWAPPED_OUT = 7;
		/** @hide */
		public static final int OFFSET_SWAPPED_OUT_PSS = 8;

		//@UnsupportedAppUsage
		private int[] otherStats = new int[(NUM_OTHER_STATS+NUM_DVK_STATS)*NUM_CATEGORIES];

		public MemoryInfo() {
		}

		/**
		 * @hide Copy contents from another object.
		 */
		public void set(MemoryInfo other) {
			dalvikPss = other.dalvikPss;
			dalvikSwappablePss = other.dalvikSwappablePss;
			dalvikRss = other.dalvikRss;
			dalvikPrivateDirty = other.dalvikPrivateDirty;
			dalvikSharedDirty = other.dalvikSharedDirty;
			dalvikPrivateClean = other.dalvikPrivateClean;
			dalvikSharedClean = other.dalvikSharedClean;
			dalvikSwappedOut = other.dalvikSwappedOut;
			dalvikSwappedOutPss = other.dalvikSwappedOutPss;

			nativePss = other.nativePss;
			nativeSwappablePss = other.nativeSwappablePss;
			nativeRss = other.nativeRss;
			nativePrivateDirty = other.nativePrivateDirty;
			nativeSharedDirty = other.nativeSharedDirty;
			nativePrivateClean = other.nativePrivateClean;
			nativeSharedClean = other.nativeSharedClean;
			nativeSwappedOut = other.nativeSwappedOut;
			nativeSwappedOutPss = other.nativeSwappedOutPss;

			otherPss = other.otherPss;
			otherSwappablePss = other.otherSwappablePss;
			otherRss = other.otherRss;
			otherPrivateDirty = other.otherPrivateDirty;
			otherSharedDirty = other.otherSharedDirty;
			otherPrivateClean = other.otherPrivateClean;
			otherSharedClean = other.otherSharedClean;
			otherSwappedOut = other.otherSwappedOut;
			otherSwappedOutPss = other.otherSwappedOutPss;

			hasSwappedOutPss = other.hasSwappedOutPss;

			System.arraycopy(other.otherStats, 0, otherStats, 0, otherStats.length);
		}

		/**
		 * Return total PSS memory usage in kB.
		 */
		public int getTotalPss() {
			return dalvikPss + nativePss + otherPss + getTotalSwappedOutPss();
		}

		/**
		 * @hide Return total PSS memory usage in kB.
		 */
		//@UnsupportedAppUsage
		public int getTotalUss() {
			return dalvikPrivateClean + dalvikPrivateDirty
					+ nativePrivateClean + nativePrivateDirty
					+ otherPrivateClean + otherPrivateDirty;
		}

		/**
		 * Return total PSS memory usage in kB mapping a file of one of the following extension:
		 * .so, .jar, .apk, .ttf, .dex, .odex, .oat, .art .
		 */
		public int getTotalSwappablePss() {
			return dalvikSwappablePss + nativeSwappablePss + otherSwappablePss;
		}

		/**
		 * @hide Return total RSS memory usage in kB.
		 */
		public int getTotalRss() {
			return dalvikRss + nativeRss + otherRss;
		}

		/**
		 * Return total private dirty memory usage in kB.
		 */
		public int getTotalPrivateDirty() {
			return dalvikPrivateDirty + nativePrivateDirty + otherPrivateDirty;
		}

		/**
		 * Return total shared dirty memory usage in kB.
		 */
		public int getTotalSharedDirty() {
			return dalvikSharedDirty + nativeSharedDirty + otherSharedDirty;
		}

		/**
		 * Return total shared clean memory usage in kB.
		 */
		public int getTotalPrivateClean() {
			return dalvikPrivateClean + nativePrivateClean + otherPrivateClean;
		}

		/**
		 * Return total shared clean memory usage in kB.
		 */
		public int getTotalSharedClean() {
			return dalvikSharedClean + nativeSharedClean + otherSharedClean;
		}

		/**
		 * Return total swapped out memory in kB.
		 * @hide
		 */
		public int getTotalSwappedOut() {
			return dalvikSwappedOut + nativeSwappedOut + otherSwappedOut;
		}

		/**
		 * Return total swapped out memory in kB, proportional.
		 * @hide
		 */
		public int getTotalSwappedOutPss() {
			return dalvikSwappedOutPss + nativeSwappedOutPss + otherSwappedOutPss;
		}

		/** @hide */
		//@UnsupportedAppUsage
		public int getOtherPss(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_PSS];
		}

		/** @hide */
		public int getOtherSwappablePss(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_SWAPPABLE_PSS];
		}

		/** @hide */
		public int getOtherRss(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_RSS];
		}

		/** @hide */
		//@UnsupportedAppUsage
		public int getOtherPrivateDirty(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_PRIVATE_DIRTY];
		}

		/** @hide */
		//@UnsupportedAppUsage
		public int getOtherSharedDirty(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_SHARED_DIRTY];
		}

		/** @hide */
		public int getOtherPrivateClean(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_PRIVATE_CLEAN];
		}

		/** @hide */
		//@UnsupportedAppUsage
		public int getOtherPrivate(int which) {
		  return getOtherPrivateClean(which) + getOtherPrivateDirty(which);
		}

		/** @hide */
		public int getOtherSharedClean(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_SHARED_CLEAN];
		}

		/** @hide */
		public int getOtherSwappedOut(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_SWAPPED_OUT];
		}

		/** @hide */
		public int getOtherSwappedOutPss(int which) {
			return otherStats[which * NUM_CATEGORIES + OFFSET_SWAPPED_OUT_PSS];
		}

		/** @hide */
		//@UnsupportedAppUsage
		public static String getOtherLabel(int which) {
			switch (which) {
				case OTHER_DALVIK_OTHER: return "Dalvik Other";
				case OTHER_STACK: return "Stack";
				case OTHER_CURSOR: return "Cursor";
				case OTHER_ASHMEM: return "Ashmem";
				case OTHER_GL_DEV: return "Gfx dev";
				case OTHER_UNKNOWN_DEV: return "Other dev";
				case OTHER_SO: return ".so mmap";
				case OTHER_JAR: return ".jar mmap";
				case OTHER_APK: return ".apk mmap";
				case OTHER_TTF: return ".ttf mmap";
				case OTHER_DEX: return ".dex mmap";
				case OTHER_OAT: return ".oat mmap";
				case OTHER_ART: return ".art mmap";
				case OTHER_UNKNOWN_MAP: return "Other mmap";
				case OTHER_GRAPHICS: return "EGL mtrack";
				case OTHER_GL: return "GL mtrack";
				case OTHER_OTHER_MEMTRACK: return "Other mtrack";
				case OTHER_DALVIK_NORMAL: return ".Heap";
				case OTHER_DALVIK_LARGE: return ".LOS";
				case OTHER_DALVIK_ZYGOTE: return ".Zygote";
				case OTHER_DALVIK_NON_MOVING: return ".NonMoving";
				case OTHER_DALVIK_OTHER_LINEARALLOC: return ".LinearAlloc";
				case OTHER_DALVIK_OTHER_ACCOUNTING: return ".GC";
				case OTHER_DALVIK_OTHER_ZYGOTE_CODE_CACHE: return ".ZygoteJIT";
				case OTHER_DALVIK_OTHER_APP_CODE_CACHE: return ".AppJIT";
				case OTHER_DALVIK_OTHER_COMPILER_METADATA: return ".CompilerMetadata";
				case OTHER_DALVIK_OTHER_INDIRECT_REFERENCE_TABLE: return ".IndirectRef";
				case OTHER_DEX_BOOT_VDEX: return ".Boot vdex";
				case OTHER_DEX_APP_DEX: return ".App dex";
				case OTHER_DEX_APP_VDEX: return ".App vdex";
				case OTHER_ART_APP: return ".App art";
				case OTHER_ART_BOOT: return ".Boot art";
				default: return "????";
			}
		}

	  /**
	   * Returns the value of a particular memory statistic or {@code null} if no
	   * such memory statistic exists.
	   *
	   * <p>The following table lists the memory statistics that are supported.
	   * Note that memory statistics may be added or removed in a future API level.</p>
	   *
	   * <table>
	   *     <thead>
	   *         <tr>
	   *             <th>Memory statistic name</th>
	   *             <th>Meaning</th>
	   *             <th>Example</th>
	   *             <th>Supported (API Levels)</th>
	   *         </tr>
	   *     </thead>
	   *     <tbody>
	   *         <tr>
	   *             <td>summary.java-heap</td>
	   *             <td>The private Java Heap usage in kB. This corresponds to the Java Heap field
	   *                 in the App Summary section output by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.native-heap</td>
	   *             <td>The private Native Heap usage in kB. This corresponds to the Native Heap
	   *                 field in the App Summary section output by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.code</td>
	   *             <td>The memory usage for static code and resources in kB. This corresponds to
	   *                 the Code field in the App Summary section output by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.stack</td>
	   *             <td>The stack usage in kB. This corresponds to the Stack field in the
	   *                 App Summary section output by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.graphics</td>
	   *             <td>The graphics usage in kB. This corresponds to the Graphics field in the
	   *                 App Summary section output by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.private-other</td>
	   *             <td>Other private memory usage in kB. This corresponds to the Private Other
	   *                 field output in the App Summary section by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.system</td>
	   *             <td>Shared and system memory usage in kB. This corresponds to the System
	   *                 field output in the App Summary section by dumpsys meminfo.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.total-pss</td>
	   *             <td>Total PSS memory usage in kB.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *         <tr>
	   *             <td>summary.total-swap</td>
	   *             <td>Total swap usage in kB.</td>
	   *             <td>{@code 1442}</td>
	   *             <td>23</td>
	   *         </tr>
	   *     </tbody>
	   * </table>
	   */
	   public String getMemoryStat(String statName) {
			switch(statName) {
				case "summary.java-heap":
					return Integer.toString(getSummaryJavaHeap());
				case "summary.native-heap":
					return Integer.toString(getSummaryNativeHeap());
				case "summary.code":
					return Integer.toString(getSummaryCode());
				case "summary.stack":
					return Integer.toString(getSummaryStack());
				case "summary.graphics":
					return Integer.toString(getSummaryGraphics());
				case "summary.private-other":
					return Integer.toString(getSummaryPrivateOther());
				case "summary.system":
					return Integer.toString(getSummarySystem());
				case "summary.total-pss":
					return Integer.toString(getSummaryTotalPss());
				case "summary.total-swap":
					return Integer.toString(getSummaryTotalSwap());
				default:
					return null;
			}
		}

		/**
		 * Returns a map of the names/values of the memory statistics
		 * that {@link #getMemoryStat(String)} supports.
		 *
		 * @return a map of the names/values of the supported memory statistics.
		 */
		public Map<String, String> getMemoryStats() {
			Map<String, String> stats = new HashMap<String, String>();
			stats.put("summary.java-heap", Integer.toString(getSummaryJavaHeap()));
			stats.put("summary.native-heap", Integer.toString(getSummaryNativeHeap()));
			stats.put("summary.code", Integer.toString(getSummaryCode()));
			stats.put("summary.stack", Integer.toString(getSummaryStack()));
			stats.put("summary.graphics", Integer.toString(getSummaryGraphics()));
			stats.put("summary.private-other", Integer.toString(getSummaryPrivateOther()));
			stats.put("summary.system", Integer.toString(getSummarySystem()));
			stats.put("summary.total-pss", Integer.toString(getSummaryTotalPss()));
			stats.put("summary.total-swap", Integer.toString(getSummaryTotalSwap()));
			return stats;
		}

		/**
		 * Pss of Java Heap bytes in KB due to the application.
		 * Notes:
		 *  * OTHER_ART is the boot image. Anything private here is blamed on
		 *    the application, not the system.
		 *  * dalvikPrivateDirty includes private zygote, which means the
		 *    application dirtied something allocated by the zygote. We blame
		 *    the application for that memory, not the system.
		 *  * Does not include OTHER_DALVIK_OTHER, which is considered VM
		 *    Overhead and lumped into Private Other.
		 *  * We don't include dalvikPrivateClean, because there should be no
		 *    such thing as private clean for the Java Heap.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryJavaHeap() {
			return dalvikPrivateDirty + getOtherPrivate(OTHER_ART);
		}

		/**
		 * Pss of Native Heap bytes in KB due to the application.
		 * Notes:
		 *  * Includes private dirty malloc space.
		 *  * We don't include nativePrivateClean, because there should be no
		 *    such thing as private clean for the Native Heap.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryNativeHeap() {
			return nativePrivateDirty;
		}

		/**
		 * Pss of code and other static resource bytes in KB due to
		 * the application.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryCode() {
			return getOtherPrivate(OTHER_SO)
			  + getOtherPrivate(OTHER_JAR)
			  + getOtherPrivate(OTHER_APK)
			  + getOtherPrivate(OTHER_TTF)
			  + getOtherPrivate(OTHER_DEX)
				+ getOtherPrivate(OTHER_OAT)
				+ getOtherPrivate(OTHER_DALVIK_OTHER_ZYGOTE_CODE_CACHE)
				+ getOtherPrivate(OTHER_DALVIK_OTHER_APP_CODE_CACHE);
		}

		/**
		 * Pss in KB of the stack due to the application.
		 * Notes:
		 *  * Includes private dirty stack, which includes both Java and Native
		 *    stack.
		 *  * Does not include private clean stack, because there should be no
		 *    such thing as private clean for the stack.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryStack() {
			return getOtherPrivateDirty(OTHER_STACK);
		}

		/**
		 * Pss in KB of graphics due to the application.
		 * Notes:
		 *  * Includes private Gfx, EGL, and GL.
		 *  * Warning: These numbers can be misreported by the graphics drivers.
		 *  * We don't include shared graphics. It may make sense to, because
		 *    shared graphics are likely buffers due to the application
		 *    anyway, but it's simpler to implement to just group all shared
		 *    memory into the System category.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryGraphics() {
			return getOtherPrivate(OTHER_GL_DEV)
			  + getOtherPrivate(OTHER_GRAPHICS)
			  + getOtherPrivate(OTHER_GL);
		}

		/**
		 * Pss in KB due to the application that haven't otherwise been
		 * accounted for.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummaryPrivateOther() {
			return getTotalPrivateClean()
			  + getTotalPrivateDirty()
			  - getSummaryJavaHeap()
			  - getSummaryNativeHeap()
			  - getSummaryCode()
			  - getSummaryStack()
			  - getSummaryGraphics();
		}

		/**
		 * Pss in KB due to the system.
		 * Notes:
		 *  * Includes all shared memory.
		 * @hide
		 */
		//@UnsupportedAppUsage
		public int getSummarySystem() {
			return getTotalPss()
			  - getTotalPrivateClean()
			  - getTotalPrivateDirty();
		}

		/**
		 * Rss of Java Heap bytes in KB due to the application.
		 * @hide
		 */
		public int getSummaryJavaHeapRss() {
			return dalvikRss + getOtherRss(OTHER_ART);
		}

		/**
		 * Rss of Native Heap bytes in KB due to the application.
		 * @hide
		 */
		public int getSummaryNativeHeapRss() {
			return nativeRss;
		}

		/**
		 * Rss of code and other static resource bytes in KB due to
		 * the application.
		 * @hide
		 */
		public int getSummaryCodeRss() {
			return getOtherRss(OTHER_SO)
				+ getOtherRss(OTHER_JAR)
				+ getOtherRss(OTHER_APK)
				+ getOtherRss(OTHER_TTF)
				+ getOtherRss(OTHER_DEX)
				+ getOtherRss(OTHER_OAT)
				+ getOtherRss(OTHER_DALVIK_OTHER_ZYGOTE_CODE_CACHE)
				+ getOtherRss(OTHER_DALVIK_OTHER_APP_CODE_CACHE);
		}

		/**
		 * Rss in KB of the stack due to the application.
		 * @hide
		 */
		public int getSummaryStackRss() {
			return getOtherRss(OTHER_STACK);
		}

		/**
		 * Rss in KB of graphics due to the application.
		 * @hide
		 */
		public int getSummaryGraphicsRss() {
			return getOtherRss(OTHER_GL_DEV)
				+ getOtherRss(OTHER_GRAPHICS)
				+ getOtherRss(OTHER_GL);
		}

		/**
		 * Rss in KB due to either the application or system that haven't otherwise been
		 * accounted for.
		 * @hide
		 */
		public int getSummaryUnknownRss() {
			return getTotalRss()
				- getSummaryJavaHeapRss()
				- getSummaryNativeHeapRss()
				- getSummaryCodeRss()
				- getSummaryStackRss()
				- getSummaryGraphicsRss();
		}

		/**
		 * Total Pss in KB.
		 * @hide
		 */
		public int getSummaryTotalPss() {
			return getTotalPss();
		}

		/**
		 * Total Swap in KB.
		 * Notes:
		 *  * Some of this memory belongs in other categories, but we don't
		 *    know if the Swap memory is shared or private, so we don't know
		 *    what to blame on the application and what on the system.
		 *    For now, just lump all the Swap in one place.
		 *    For kernels reporting SwapPss {@link #getSummaryTotalSwapPss()}
		 *    will report the application proportional Swap.
		 * @hide
		 */
		public int getSummaryTotalSwap() {
			return getTotalSwappedOut();
		}

		/**
		 * Total proportional Swap in KB.
		 * Notes:
		 *  * Always 0 if {@link #hasSwappedOutPss} is false.
		 * @hide
		 */
		public int getSummaryTotalSwapPss() {
			return getTotalSwappedOutPss();
		}

		/**
		 * Return true if the kernel is reporting pss swapped out...  that is, if
		 * {@link #getSummaryTotalSwapPss()} will return non-0 values.
		 * @hide
		 */
		public boolean hasSwappedOutPss() {
			return hasSwappedOutPss;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(dalvikPss);
			dest.writeInt(dalvikSwappablePss);
			dest.writeInt(dalvikRss);
			dest.writeInt(dalvikPrivateDirty);
			dest.writeInt(dalvikSharedDirty);
			dest.writeInt(dalvikPrivateClean);
			dest.writeInt(dalvikSharedClean);
			dest.writeInt(dalvikSwappedOut);
			dest.writeInt(dalvikSwappedOutPss);
			dest.writeInt(nativePss);
			dest.writeInt(nativeSwappablePss);
			dest.writeInt(nativeRss);
			dest.writeInt(nativePrivateDirty);
			dest.writeInt(nativeSharedDirty);
			dest.writeInt(nativePrivateClean);
			dest.writeInt(nativeSharedClean);
			dest.writeInt(nativeSwappedOut);
			dest.writeInt(nativeSwappedOutPss);
			dest.writeInt(otherPss);
			dest.writeInt(otherSwappablePss);
			dest.writeInt(otherRss);
			dest.writeInt(otherPrivateDirty);
			dest.writeInt(otherSharedDirty);
			dest.writeInt(otherPrivateClean);
			dest.writeInt(otherSharedClean);
			dest.writeInt(otherSwappedOut);
			dest.writeInt(hasSwappedOutPss ? 1 : 0);
			dest.writeInt(otherSwappedOutPss);
			dest.writeIntArray(otherStats);
		}

		public void readFromParcel(Parcel source) {
			dalvikPss = source.readInt();
			dalvikSwappablePss = source.readInt();
			dalvikRss = source.readInt();
			dalvikPrivateDirty = source.readInt();
			dalvikSharedDirty = source.readInt();
			dalvikPrivateClean = source.readInt();
			dalvikSharedClean = source.readInt();
			dalvikSwappedOut = source.readInt();
			dalvikSwappedOutPss = source.readInt();
			nativePss = source.readInt();
			nativeSwappablePss = source.readInt();
			nativeRss = source.readInt();
			nativePrivateDirty = source.readInt();
			nativeSharedDirty = source.readInt();
			nativePrivateClean = source.readInt();
			nativeSharedClean = source.readInt();
			nativeSwappedOut = source.readInt();
			nativeSwappedOutPss = source.readInt();
			otherPss = source.readInt();
			otherSwappablePss = source.readInt();
			otherRss = source.readInt();
			otherPrivateDirty = source.readInt();
			otherSharedDirty = source.readInt();
			otherPrivateClean = source.readInt();
			otherSharedClean = source.readInt();
			otherSwappedOut = source.readInt();
			hasSwappedOutPss = source.readInt() != 0;
			otherSwappedOutPss = source.readInt();
			otherStats = source.createIntArray();
		}

		public static final @android.annotation.NonNull Creator<MemoryInfo> CREATOR = new Creator<MemoryInfo>() {
			public MemoryInfo createFromParcel(Parcel source) {
				return new MemoryInfo(source);
			}
			public MemoryInfo[] newArray(int size) {
				return new MemoryInfo[size];
			}
		};

		private MemoryInfo(Parcel source) {
			readFromParcel(source);
		}
	}

	private static int globalAllocCount;
	private static int globalAllocSize;
	private static int globalFreedCount;
	private static int globalFreedSize;
	private static int globalExternalAllocCount;
	private static int globalExternalAllocSize;
	private static int globalExternalFreedCount;
	private static int globalExternalFreedSize;
	private static int threadAllocCount;
	private static int threadAllocSize;
	private static int threadExternalAllocCount;
	private static int threadExternalAllocSize;
	private static int threadGcInvocationCount;
	private static int globalGcInvocationCount;

	public static void waitForDebugger() {
	}

	public static class InstructionCount {
		public InstructionCount() {
		}
	}

	public static boolean isDebuggerConnected() {
		return false;
	}

	public static long getNativeHeapFreeSize() {
		return 0;
	}

	public static long getNativeHeapAllocatedSize() {
		return 0;
	}

	public static boolean waitingForDebugger() {
		return false;
	}

	public static long threadCpuTimeNanos() {
		return VMDebug.threadCpuTimeNanos();
	}

	public static void getMemoryInfo(MemoryInfo memoryInfo) {}

	public static void stopAllocCounting() {}

	public static void startAllocCounting() {
		globalAllocCount = 100;
		globalAllocSize = 10000;
	}

	public static long getNativeHeapSize() {
		return 0;
	}

	public static void resetAllCounts() {
		globalAllocCount = 0;
		globalAllocSize = 0;
		globalFreedCount = 0;
		globalFreedSize = 0;
		globalExternalAllocCount = 0;
		globalExternalAllocSize = 0;
		globalExternalFreedCount = 0;
		globalExternalFreedSize = 0;
		threadAllocCount = 0;
		threadAllocSize = 0;
		threadExternalAllocCount = 0;
		threadExternalAllocSize = 0;
		threadGcInvocationCount = 0;
		globalGcInvocationCount = 0;
	}

	public static int getGlobalAllocCount() {
		return globalAllocCount;
	}

	public static int getGlobalAllocSize() {
		return globalAllocSize;
	}

	public static int getGlobalFreedCount() {
		return globalFreedCount;
	}

	public static int getGlobalFreedSize() {
		return globalFreedSize;
	}

	public static int getGlobalExternalAllocCount() {
		return globalExternalAllocCount;
	}

	public static int getGlobalExternalAllocSize() {
		return globalExternalAllocSize;
	}

	public static int getGlobalExternalFreedCount() {
		return globalExternalFreedCount;
	}

	public static int getGlobalExternalFreedSize() {
		return globalExternalFreedSize;
	}

	public static int getGlobalGcInvocationCount() {
		return globalGcInvocationCount;
	}

	public static int getLoadedClassCount() {
		return 0;
	}

	public static int getThreadAllocCount() {
		return threadAllocCount;
	}

	public static int getThreadAllocSize() {
		return threadAllocSize;
	}

	public static int getThreadExternalAllocCount() {
		return threadExternalAllocCount;
	}

	public static int getThreadExternalAllocSize() {
		return threadExternalAllocSize;
	}

	public static int getThreadGcInvocationCount() {
		return threadGcInvocationCount;
	}

	public static int getBinderDeathObjectCount() {
		return 0;
	}

	public static int getBinderLocalObjectCount() {
		return 0;
	}

	public static int getBinderProxyObjectCount() {
		return 0;
	}

	public static int getBinderReceivedTransactions() {
		return 0;
	}

	public static int getBinderSentTransactions() {
		return 0;
	}

	public static void resetGlobalAllocCount() {
		globalAllocCount = 0;
	}

	public static void resetGlobalAllocSize() {
		globalAllocSize = 0;
	}

	public static void resetGlobalExternalAllocCount() {
		globalExternalAllocCount = 0;
	}

	public static void resetGlobalExternalAllocSize() {
		globalExternalAllocSize = 0;
	}

	public static void resetGlobalExternalFreedCount() {
		globalExternalFreedCount = 0;
	}

	public static void resetGlobalExternalFreedSize() {
		globalExternalFreedSize = 0;
	}

	public static void resetGlobalFreedCount() {
		globalFreedCount = 0;
	}

	public static void resetGlobalFreedSize() {
		globalFreedSize = 0;
	}

	public static void resetGlobalGcInvocationCount() {
		globalGcInvocationCount = 0;
	}

	public static void resetThreadAllocCount() {
		threadAllocCount = 0;
	}

	public static void resetThreadAllocSize() {
		threadAllocSize = 0;
	}

	public static void resetThreadExternalAllocCount() {
		threadExternalAllocCount = 0;
	}

	public static void resetThreadExternalAllocSize() {
		threadExternalAllocSize = 0;
	}

	public static void resetThreadGcInvocationCount() {
		threadGcInvocationCount = 0;
	}

	public static void dumpHprofData(String fileName) throws IOException {}

	public static void printLoadedClasses(int flags) {}

	public static void startMethodTracing() {}

	public static void startMethodTracing(String traceName) {}

	public static void startMethodTracing(String traceName, int bufferSize) {}

	public static void startMethodTracing(String traceName, int bufferSize, int flags) {}

	/* like startMethodTracing(): ATL has no VM tracer behind VMDebug.startMethodTracing */
	public static void startMethodTracingSampling(String traceName, int bufferSize, int intervalUs) {}

	public static void stopMethodTracing() {}

	public static void startNativeTracing() {}

	public static void stopNativeTracing() {}

	/* AOSP Debug.getCaller()/getCallers() */
	private static String getCaller(StackTraceElement callStack[], int depth) {
		// callStack[4] is the caller of the method that called getCallers()
		if (4 + depth >= callStack.length) {
			return "<bottom of call stack>";
		}
		StackTraceElement caller = callStack[4 + depth];
		return caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber();
	}

	public static String getCallers(final int depth) {
		final StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append(getCaller(callStack, i)).append(" ");
		}
		return sb.toString();
	}

	/**
	 * Returns the value of a particular runtime statistic or {@code null} if no
	 * such runtime statistic exists.
	 */
	public static String getRuntimeStat(String statName) {
		return VMDebug.getRuntimeStat(statName);
	}

	/**
	 * Returns a map of the names/values of the runtime statistics
	 * that {@link #getRuntimeStat(String)} supports.
	 */
	public static Map<String, String> getRuntimeStats() {
		return VMDebug.getRuntimeStats();
	}
}
