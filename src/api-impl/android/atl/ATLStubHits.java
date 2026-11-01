package android.atl;

import android.os.SystemClock;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Counts calls of ATL's stub methods (diagnosis). The build (tools/stub_hits) puts a call
 * ATLStubHits.hit(id, "class\tmethod\tsignature\tkind") at the start of every method whose bytecode is a stub
 * (empty, returns a constant/null/this/an argument, throws UnsupportedOperationException or logs a stub text).
 *
 * Off unless the environment variable ATL_STUB_HITS names an output file: then a call costs an atomic increment,
 * the first CALLERS calls of a stub also a stack trace for the caller. A daemon thread appends what changed once a
 * second (and at exit), tab separated, t = CLOCK_BOOTTIME in us:
 *   N  id class method signature kind     the first time a stub is seen
 *   S  t id count                         cumulative call count of the stub at time t
 *   C  id caller                          a caller (first frame outside the platform), up to CALLERS per stub
 */
public final class ATLStubHits {
	private static final String OUT = System.getenv("ATL_STUB_HITS");
	public static final boolean ON = OUT != null && !OUT.isEmpty();
	private static final int MAX = 1 << 16; // ids are 16 bits wide (tools/stub_hits)
	private static final int CALLERS = 3;
	private static final int FLUSH_MS = 1000;

	private static final AtomicIntegerArray counts = ON ? new AtomicIntegerArray(MAX) : null;
	private static final String[] names = ON ? new String[MAX] : null;
	private static final String[][] callers = ON ? new String[MAX][] : null;
	private static int[] written; // last count written per id (flush thread only)
	private static int top;       // highest id seen + 1

	private ATLStubHits() {}

	static {
		if (ON) {
			written = new int[MAX];
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						try {
							Thread.sleep(FLUSH_MS);
						} catch (InterruptedException e) {
							return;
						}
						flush();
					}
				}
			}, "atl-stub-hits");
			t.setDaemon(true);
			t.start();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					flush();
				}
			}));
		}
	}

	public static void hit(int id, String name) {
		if (!ON)
			return;
		int n = counts.getAndIncrement(id);
		if (n == 0) {
			names[id] = name;
			synchronized (ATLStubHits.class) {
				if (id >= top)
					top = id + 1;
			}
		}
		if (n < CALLERS)
			caller(id, n);
	}

	/* first frame outside the platform: [0] = caller(), [1] = hit(), [2] = the stub */
	private static void caller(int id, int n) {
		StackTraceElement[] st = new Throwable().getStackTrace();
		String c = "-";
		for (int i = 3; i < st.length; i++) {
			if (!platform(st[i].getClassName())) {
				c = st[i].getClassName() + "." + st[i].getMethodName();
				break;
			}
		}
		synchronized (ATLStubHits.class) {
			String[] cs = callers[id];
			if (cs == null)
				cs = callers[id] = new String[CALLERS];
			for (int i = 0; i < CALLERS; i++) {
				if (c.equals(cs[i]))
					return;
				if (cs[i] == null) {
					cs[i] = c;
					return;
				}
			}
		}
	}

	private static boolean platform(String cls) {
		return cls.startsWith("android.") || cls.startsWith("java.") || cls.startsWith("javax.") ||
		    cls.startsWith("dalvik.") || cls.startsWith("libcore.") || cls.startsWith("sun.") ||
		    cls.startsWith("com.android.") || cls.startsWith("org.apache.harmony.");
	}

	private static final boolean[] callerWritten = ON ? new boolean[MAX * CALLERS] : null;

	private static synchronized void flush() {
		long t = SystemClock.elapsedRealtimeNanos() / 1000;
		StringBuilder sb = new StringBuilder();
		for (int id = 0; id < top; id++) {
			int n = counts.get(id);
			if (n == written[id] || names[id] == null)
				continue;
			if (written[id] == 0)
				sb.append("N\t").append(id).append('\t').append(names[id]).append('\n');
			written[id] = n;
			sb.append("S\t").append(t).append('\t').append(id).append('\t').append(n).append('\n');
			String[] cs = callers[id];
			for (int i = 0; cs != null && i < CALLERS && cs[i] != null; i++) {
				if (!callerWritten[id * CALLERS + i]) {
					callerWritten[id * CALLERS + i] = true;
					sb.append("C\t").append(id).append('\t').append(cs[i]).append('\n');
				}
			}
		}
		if (sb.length() == 0)
			return;
		try (FileOutputStream f = new FileOutputStream(OUT, true)) {
			f.write(sb.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// diagnosis only: never disturb the app
		}
	}
}
