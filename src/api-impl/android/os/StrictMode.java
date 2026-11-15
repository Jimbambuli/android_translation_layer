package android.os;

import java.util.concurrent.Executor;

/**
 * Policy bookkeeping as in AOSP StrictMode: the thread policy mask is kept per thread, the VM policy
 * per process, and getThreadPolicy/getVmPolicy/allowThreadDisk* return them like AOSP does.
 *
 * ATL does not install AOSP's BlockGuard/CloseGuard checkers, so no violation is ever detected and no
 * penalty runs; an app sees what it sees on Android when its code causes no violation.
 */
public final class StrictMode {

	/* AOSP StrictMode DETECT_* / PENALTY_* bits */
	private static final int DETECT_THREAD_DISK_WRITE = 1 << 0;
	private static final int DETECT_THREAD_DISK_READ = 1 << 1;
	private static final int DETECT_THREAD_NETWORK = 1 << 2;
	private static final int DETECT_THREAD_CUSTOM = 1 << 3;
	private static final int DETECT_THREAD_RESOURCE_MISMATCH = 1 << 4;
	private static final int DETECT_THREAD_UNBUFFERED_IO = 1 << 5;
	private static final int DETECT_THREAD_EXPLICIT_GC = 1 << 6;
	private static final int DETECT_THREAD_ALL = 0x0000ffff;

	private static final int DETECT_VM_CURSOR_LEAKS = 1 << 0;
	private static final int DETECT_VM_CLOSABLE_LEAKS = 1 << 1;
	private static final int DETECT_VM_ACTIVITY_LEAKS = 1 << 2;
	private static final int DETECT_VM_REGISTRATION_LEAKS = 1 << 4;
	private static final int DETECT_VM_FILE_URI_EXPOSURE = 1 << 5;
	private static final int DETECT_VM_CLEARTEXT_NETWORK = 1 << 6;
	private static final int DETECT_VM_CONTENT_URI_WITHOUT_PERMISSION = 1 << 7;
	private static final int DETECT_VM_UNTAGGED_SOCKET = 1 << 8;
	private static final int DETECT_VM_NON_SDK_API_USAGE = 1 << 9;
	private static final int DETECT_VM_IMPLICIT_DIRECT_BOOT = 1 << 10;
	private static final int DETECT_VM_CREDENTIAL_PROTECTED_WHILE_LOCKED = 1 << 11;
	private static final int DETECT_VM_INCORRECT_CONTEXT_USE = 1 << 12;
	private static final int DETECT_VM_UNSAFE_INTENT_LAUNCH = 1 << 13;
	private static final int DETECT_VM_BACKGROUND_ACTIVITY_LAUNCH_ABORTED = 1 << 14;
	private static final int DETECT_VM_ALL = 0x0000ffff;

	/** @hide */
	public static final int PENALTY_GATHER = 1 << 31;
	/** @hide */
	public static final int PENALTY_LOG = 1 << 30;
	/** @hide */
	public static final int PENALTY_DIALOG = 1 << 29;
	/** @hide */
	public static final int PENALTY_DEATH = 1 << 28;
	/** @hide */
	public static final int PENALTY_FLASH = 1 << 27;
	/** @hide */
	public static final int PENALTY_DROPBOX = 1 << 26;
	/** @hide */
	public static final int PENALTY_DEATH_ON_NETWORK = 1 << 25;
	/** @hide */
	public static final int PENALTY_DEATH_ON_CLEARTEXT_NETWORK = 1 << 24;
	/** @hide */
	public static final int PENALTY_DEATH_ON_FILE_URI_EXPOSURE = 1 << 23;
	/** @hide */
	public static final int PENALTY_ALL = 0xffff0000;

	/* stands in for BlockGuard.getThreadPolicy().getPolicyMask() */
	private static final ThreadLocal<Integer> sThreadPolicyMask = new ThreadLocal<>();
	private static final ThreadLocal<OnThreadViolationListener> sThreadViolationListener = new ThreadLocal<>();
	private static final ThreadLocal<Executor> sThreadViolationExecutor = new ThreadLocal<>();
	private static volatile VmPolicy sVmPolicy;

	private StrictMode() {}

	public static void assertConfigurationContext(android.content.Context context, String methodName) {}

	/* AOSP StrictMode.setThreadPolicy */
	public static void setThreadPolicy(final ThreadPolicy policy) {
		setThreadPolicyMask(policy.mask);
		sThreadViolationListener.set(policy.listener);
		sThreadViolationExecutor.set(policy.callbackExecutor);
	}

	/** @hide */
	public static void setThreadPolicyMask(int threadPolicyMask) {
		sThreadPolicyMask.set(threadPolicyMask);
	}

	/** @hide */
	public static int getThreadPolicyMask() {
		Integer mask = sThreadPolicyMask.get();
		return mask != null ? mask : 0;
	}

	/* AOSP StrictMode.getThreadPolicy */
	public static ThreadPolicy getThreadPolicy() {
		return new ThreadPolicy(getThreadPolicyMask(), sThreadViolationListener.get(), sThreadViolationExecutor.get());
	}

	/* AOSP StrictMode.allowThreadDiskWrites: returns the old policy, then permits disk writes and reads */
	public static ThreadPolicy allowThreadDiskWrites() {
		ThreadPolicy old = getThreadPolicy();
		int oldPolicyMask = getThreadPolicyMask();
		int newPolicyMask = oldPolicyMask & ~(DETECT_THREAD_DISK_WRITE | DETECT_THREAD_DISK_READ);
		if (newPolicyMask != oldPolicyMask)
			setThreadPolicyMask(newPolicyMask);
		return old;
	}

	/* AOSP StrictMode.allowThreadDiskReads: returns the old policy, then permits disk reads */
	public static ThreadPolicy allowThreadDiskReads() {
		ThreadPolicy old = getThreadPolicy();
		int oldPolicyMask = getThreadPolicyMask();
		int newPolicyMask = oldPolicyMask & ~(DETECT_THREAD_DISK_READ);
		if (newPolicyMask != oldPolicyMask)
			setThreadPolicyMask(newPolicyMask);
		return old;
	}

	/* AOSP StrictMode.enableDefaults() */
	public static void enableDefaults() {
		setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
	}

	/* AOSP StrictMode.setVmPolicy (without the CloseGuard/instance-count checkers) */
	public static void setVmPolicy(final VmPolicy policy) {
		synchronized (StrictMode.class) {
			sVmPolicy = policy;
		}
	}

	/* AOSP StrictMode.getVmPolicy */
	public static VmPolicy getVmPolicy() {
		synchronized (StrictMode.class) {
			return sVmPolicy != null ? sVmPolicy : VmPolicy.LAX;
		}
	}

	/* AOSP StrictMode.noteSlowCall reports a custom violation only if DETECT_THREAD_CUSTOM is set; ATL
	 * reports no violations. */
	public static void noteSlowCall(String tag) {}

	/* AOSP StrictMode.OnThreadViolationListener / OnVmViolationListener */
	public interface OnThreadViolationListener {
		void onThreadViolation(android.os.strictmode.Violation v);
	}

	public interface OnVmViolationListener {
		void onVmViolation(android.os.strictmode.Violation v);
	}

	public static final class ThreadPolicy {
		public static final ThreadPolicy LAX = new ThreadPolicy(0, null, null);

		final int mask;
		final OnThreadViolationListener listener;
		final Executor callbackExecutor;

		private ThreadPolicy(int mask, OnThreadViolationListener listener, Executor executor) {
			this.mask = mask;
			this.listener = listener;
			this.callbackExecutor = executor;
		}

		@Override
		public String toString() {
			return "[StrictMode.ThreadPolicy; mask=" + mask + "]";
		}

		/* AOSP StrictMode.ThreadPolicy.Builder */
		public static final class Builder {
			private int mMask = 0;
			private OnThreadViolationListener mListener;
			private Executor mExecutor;

			public Builder() {
				mMask = 0;
			}

			public Builder(ThreadPolicy policy) {
				mMask = policy.mask;
				mListener = policy.listener;
				mExecutor = policy.callbackExecutor;
			}

			public Builder detectAll() {
				detectDiskReads();
				detectDiskWrites();
				detectNetwork();
				detectCustomSlowCalls();
				detectResourceMismatches();
				detectUnbufferedIo();
				return this;
			}

			public Builder permitAll() {
				return disable(DETECT_THREAD_ALL);
			}

			public Builder detectNetwork() {
				return enable(DETECT_THREAD_NETWORK);
			}

			public Builder permitNetwork() {
				return disable(DETECT_THREAD_NETWORK);
			}

			public Builder detectDiskReads() {
				return enable(DETECT_THREAD_DISK_READ);
			}

			public Builder permitDiskReads() {
				return disable(DETECT_THREAD_DISK_READ);
			}

			public Builder detectCustomSlowCalls() {
				return enable(DETECT_THREAD_CUSTOM);
			}

			public Builder permitCustomSlowCalls() {
				return disable(DETECT_THREAD_CUSTOM);
			}

			public Builder permitResourceMismatches() {
				return disable(DETECT_THREAD_RESOURCE_MISMATCH);
			}

			public Builder detectUnbufferedIo() {
				return enable(DETECT_THREAD_UNBUFFERED_IO);
			}

			public Builder permitUnbufferedIo() {
				return disable(DETECT_THREAD_UNBUFFERED_IO);
			}

			public Builder detectResourceMismatches() {
				return enable(DETECT_THREAD_RESOURCE_MISMATCH);
			}

			public Builder detectDiskWrites() {
				return enable(DETECT_THREAD_DISK_WRITE);
			}

			public Builder permitDiskWrites() {
				return disable(DETECT_THREAD_DISK_WRITE);
			}

			public Builder detectExplicitGc() {
				return enable(DETECT_THREAD_EXPLICIT_GC);
			}

			public Builder permitExplicitGc() {
				return disable(DETECT_THREAD_EXPLICIT_GC);
			}

			public Builder penaltyDialog() {
				return enable(PENALTY_DIALOG);
			}

			public Builder penaltyDeath() {
				return enable(PENALTY_DEATH);
			}

			public Builder penaltyDeathOnNetwork() {
				return enable(PENALTY_DEATH_ON_NETWORK);
			}

			public Builder penaltyFlashScreen() {
				return enable(PENALTY_FLASH);
			}

			public Builder penaltyLog() {
				return enable(PENALTY_LOG);
			}

			public Builder penaltyDropBox() {
				return enable(PENALTY_DROPBOX);
			}

			public Builder penaltyListener(Executor executor, OnThreadViolationListener listener) {
				if (executor == null) {
					throw new NullPointerException("executor must not be null");
				}
				mListener = listener;
				mExecutor = executor;
				return this;
			}

			private Builder enable(int mask) {
				mMask |= mask;
				return this;
			}

			private Builder disable(int mask) {
				mMask &= ~mask;
				return this;
			}

			public ThreadPolicy build() {
				// If there are detection bits set but no violation penalties
				// specified, default to logging.
				if (mListener == null && mMask != 0 && (mMask & (PENALTY_DEATH | PENALTY_LOG | PENALTY_DROPBOX | PENALTY_DIALOG)) == 0) {
					penaltyLog();
				}
				return new ThreadPolicy(mMask, mListener, mExecutor);
			}
		}
	}

	public static final class VmPolicy {
		public static final VmPolicy LAX = new VmPolicy(0, null, null);

		final int mask;
		final OnVmViolationListener mListener;
		final Executor mCallbackExecutor;

		private VmPolicy(int mask, OnVmViolationListener listener, Executor executor) {
			this.mask = mask;
			mListener = listener;
			mCallbackExecutor = executor;
		}

		@Override
		public String toString() {
			return "[StrictMode.VmPolicy; mask=" + mask + "]";
		}

		/* AOSP StrictMode.VmPolicy.Builder (without classInstanceLimit, which only feeds the checker) */
		public static final class Builder {
			private int mMask;
			private OnVmViolationListener mListener;
			private Executor mExecutor;

			public Builder() {
				mMask = 0;
			}

			public Builder(VmPolicy base) {
				mMask = base.mask;
				mListener = base.mListener;
				mExecutor = base.mCallbackExecutor;
			}

			public Builder setClassInstanceLimit(Class klass, int instanceLimit) {
				if (klass == null) {
					throw new NullPointerException("klass == null");
				}
				return this;
			}

			public Builder detectActivityLeaks() {
				return enable(DETECT_VM_ACTIVITY_LEAKS);
			}

			public Builder permitActivityLeaks() {
				return disable(DETECT_VM_ACTIVITY_LEAKS);
			}

			public Builder detectNonSdkApiUsage() {
				return enable(DETECT_VM_NON_SDK_API_USAGE);
			}

			public Builder permitNonSdkApiUsage() {
				return disable(DETECT_VM_NON_SDK_API_USAGE);
			}

			public Builder detectAll() {
				detectLeakedSqlLiteObjects();
				detectActivityLeaks();
				detectLeakedClosableObjects();
				detectLeakedRegistrationObjects();
				detectFileUriExposure();
				detectCleartextNetwork();
				detectContentUriWithoutPermission();
				detectUntaggedSockets();
				detectImplicitDirectBoot();
				detectCredentialProtectedWhileLocked();
				detectIncorrectContextUse();
				detectUnsafeIntentLaunch();
				return this;
			}

			public Builder detectLeakedSqlLiteObjects() {
				return enable(DETECT_VM_CURSOR_LEAKS);
			}

			public Builder detectLeakedClosableObjects() {
				return enable(DETECT_VM_CLOSABLE_LEAKS);
			}

			public Builder detectLeakedRegistrationObjects() {
				return enable(DETECT_VM_REGISTRATION_LEAKS);
			}

			public Builder detectFileUriExposure() {
				return enable(DETECT_VM_FILE_URI_EXPOSURE);
			}

			public Builder detectCleartextNetwork() {
				return enable(DETECT_VM_CLEARTEXT_NETWORK);
			}

			public Builder detectContentUriWithoutPermission() {
				return enable(DETECT_VM_CONTENT_URI_WITHOUT_PERMISSION);
			}

			public Builder detectUntaggedSockets() {
				return enable(DETECT_VM_UNTAGGED_SOCKET);
			}

			public Builder permitUntaggedSockets() {
				return disable(DETECT_VM_UNTAGGED_SOCKET);
			}

			public Builder detectImplicitDirectBoot() {
				return enable(DETECT_VM_IMPLICIT_DIRECT_BOOT);
			}

			public Builder permitImplicitDirectBoot() {
				return disable(DETECT_VM_IMPLICIT_DIRECT_BOOT);
			}

			public Builder detectBlockedBackgroundActivityLaunch() {
				return enable(DETECT_VM_BACKGROUND_ACTIVITY_LAUNCH_ABORTED);
			}

			public Builder ignoreBlockedBackgroundActivityLaunch() {
				return disable(DETECT_VM_BACKGROUND_ACTIVITY_LAUNCH_ABORTED);
			}

			public Builder detectCredentialProtectedWhileLocked() {
				return enable(DETECT_VM_CREDENTIAL_PROTECTED_WHILE_LOCKED);
			}

			public Builder permitCredentialProtectedWhileLocked() {
				return disable(DETECT_VM_CREDENTIAL_PROTECTED_WHILE_LOCKED);
			}

			public Builder detectIncorrectContextUse() {
				return enable(DETECT_VM_INCORRECT_CONTEXT_USE);
			}

			public Builder permitIncorrectContextUse() {
				return disable(DETECT_VM_INCORRECT_CONTEXT_USE);
			}

			public Builder detectUnsafeIntentLaunch() {
				return enable(DETECT_VM_UNSAFE_INTENT_LAUNCH);
			}

			public Builder permitUnsafeIntentLaunch() {
				return disable(DETECT_VM_UNSAFE_INTENT_LAUNCH);
			}

			public Builder penaltyDeath() {
				return enable(PENALTY_DEATH);
			}

			public Builder penaltyDeathOnCleartextNetwork() {
				return enable(PENALTY_DEATH_ON_CLEARTEXT_NETWORK);
			}

			public Builder penaltyDeathOnFileUriExposure() {
				return enable(PENALTY_DEATH_ON_FILE_URI_EXPOSURE);
			}

			public Builder penaltyLog() {
				return enable(PENALTY_LOG);
			}

			public Builder penaltyDropBox() {
				return enable(PENALTY_DROPBOX);
			}

			public Builder penaltyListener(Executor executor, OnVmViolationListener listener) {
				if (executor == null) {
					throw new NullPointerException("executor must not be null");
				}
				mListener = listener;
				mExecutor = executor;
				return this;
			}

			private Builder enable(int mask) {
				mMask |= mask;
				return this;
			}

			Builder disable(int mask) {
				mMask &= ~mask;
				return this;
			}

			public VmPolicy build() {
				// If there are detection bits set but no violation penalties
				// specified, default to logging.
				if (mListener == null && mMask != 0 && (mMask & (PENALTY_DEATH | PENALTY_LOG | PENALTY_DROPBOX | PENALTY_DIALOG)) == 0) {
					penaltyLog();
				}
				return new VmPolicy(mMask, mListener, mExecutor);
			}
		}
	}
}
