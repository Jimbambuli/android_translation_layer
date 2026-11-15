package android.net;

import android.os.CancellationSignal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/* AOSP DnsResolver (packages/modules/Connectivity framework/src/android/net/DnsResolver.java): the constants,
 * callback types and DnsException are the AOSP ones. AOSP sends the query through netd's native resolver
 * (resNetworkQuery/resNetworkSend), which ATL does not have. The lookup methods therefore ask the host resolver:
 * query() resolves with InetAddress, an unknown name is answered as NXDOMAIN (rcode 3) with an empty list.
 * rawQuery() needs the DNS wire format of the native resolver; it reports the failure the way AOSP reports a
 * failed resNetworkQuery(): onError(DnsException(ERROR_SYSTEM, cause)). */
public final class DnsResolver {
	public static final int CLASS_IN = 1;
	public static final int TYPE_A = 1;
	public static final int TYPE_AAAA = 28;
	public static final int TYPE_PTR = 12;
	public static final int TYPE_TXT = 16;
	public static final int TYPE_SRV = 33;
	public static final int TYPE_ANY = 255;
	public static final int FLAG_EMPTY = 0;
	public static final int FLAG_NO_RETRY = 1 << 0;
	public static final int FLAG_NO_CACHE_STORE = 1 << 1;
	public static final int FLAG_NO_CACHE_LOOKUP = 1 << 2;
	public static final int ERROR_PARSE = 0;
	public static final int ERROR_SYSTEM = 1;
	private static final int RCODE_NXDOMAIN = 3;

	private static final DnsResolver sInstance = new DnsResolver();

	public static DnsResolver getInstance() {
		return sInstance;
	}

	private DnsResolver() {}

	public interface Callback<T> {
		void onAnswer(T answer, int rcode);

		void onError(DnsException error);
	}

	public static class DnsException extends Exception {
		public final int code;

		public DnsException(int code, Throwable cause) {
			super(cause);
			this.code = code;
		}
	}

	public void rawQuery(Network network, byte[] query, int flags, Executor executor, CancellationSignal cancellationSignal,
	                     Callback<? super byte[]> callback) {
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			return;
		}
		executor.execute(() -> callback.onError(new DnsException(ERROR_SYSTEM, new java.io.IOException("no native resolver"))));
	}

	public void rawQuery(Network network, String domain, int nsClass, int nsType, int flags, Executor executor,
	                     CancellationSignal cancellationSignal, Callback<? super byte[]> callback) {
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			return;
		}
		executor.execute(() -> callback.onError(new DnsException(ERROR_SYSTEM, new java.io.IOException("no native resolver"))));
	}

	public void query(Network network, String domain, int flags, Executor executor, CancellationSignal cancellationSignal,
	                  Callback<? super List<InetAddress>> callback) {
		lookup(domain, 0, executor, cancellationSignal, callback);
	}

	public void query(Network network, String domain, int nsType, int flags, Executor executor,
	                  CancellationSignal cancellationSignal, Callback<? super List<InetAddress>> callback) {
		lookup(domain, nsType, executor, cancellationSignal, callback);
	}

	private static void lookup(String domain, int nsType, Executor executor, CancellationSignal cancellationSignal,
	                           Callback<? super List<InetAddress>> callback) {
		if (cancellationSignal != null && cancellationSignal.isCanceled()) {
			return;
		}
		new Thread(() -> {
			List<InetAddress> answers = new ArrayList<InetAddress>();
			int rcode = 0;
			try {
				for (InetAddress a : InetAddress.getAllByName(domain)) {
					if (nsType == TYPE_A && !(a instanceof Inet4Address))
						continue;
					if (nsType == TYPE_AAAA && !(a instanceof Inet6Address))
						continue;
					answers.add(a);
				}
			} catch (UnknownHostException e) {
				answers = Collections.emptyList();
				rcode = RCODE_NXDOMAIN;
			}
			if (cancellationSignal != null && cancellationSignal.isCanceled()) {
				return;
			}
			final List<InetAddress> result = answers;
			final int resultCode = rcode;
			executor.execute(() -> callback.onAnswer(result, resultCode));
		}).start();
	}
}
