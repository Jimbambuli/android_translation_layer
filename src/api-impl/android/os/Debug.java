package android.os;

import dalvik.system.VMDebug;
import java.io.IOException;
import java.util.Map;

public final class Debug {
	/* AOSP fills it from /proc/<pid>/smaps; ATL fills nothing (getMemoryInfo()), so all values are 0 */
	public static class MemoryInfo {
		/** The proportional set size for dalvik heap. */
		public int dalvikPss;
		/** The private dirty pages used by dalvik heap. */
		public int dalvikPrivateDirty;
		/** The shared dirty pages used by dalvik heap. */
		public int dalvikSharedDirty;
		/** The proportional set size for the native heap. */
		public int nativePss;
		/** The private dirty pages used by the native heap. */
		public int nativePrivateDirty;
		/** The shared dirty pages used by the native heap. */
		public int nativeSharedDirty;
		/** The proportional set size for everything else. */
		public int otherPss;
		/** The private dirty pages used by everything else. */
		public int otherPrivateDirty;
		/** The shared dirty pages used by everything else. */
		public int otherSharedDirty;

		/**
		 * Return total PSS memory usage in kB.
		 */
		public int getTotalPss() {
			return dalvikPss + nativePss + otherPss;
		}

		/**
		 * Returns the value of a particular memory statistic or {@code null} if no
		 * such memory statistic exists.
		 */
		public String getMemoryStat(String statName) {
			switch (statName) {
				case "summary.java-heap":
				case "summary.native-heap":
				case "summary.code":
				case "summary.stack":
				case "summary.graphics":
				case "summary.private-other":
				case "summary.system":
				case "summary.total-swap":
					return "0";
				case "summary.total-pss":
					return Integer.toString(getTotalPss());
				default:
					return null;
			}
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
