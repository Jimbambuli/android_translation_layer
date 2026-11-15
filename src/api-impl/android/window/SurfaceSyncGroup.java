/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.window;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceControl.Transaction;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/*
 * AOSP android16 frameworks/base/core/java/android/window/SurfaceSyncGroup.java. Only the in-process part is
 * ported (groups added to each other, transaction merging, completion callbacks); ATL has no WindowManager
 * service, binder-remote sync groups and the 1 s timeout are not implemented.
 * add(AttachedSurfaceControl, Runnable) and add(SurfaceControlViewHost.SurfacePackage, Runnable) are missing
 * because those android.view types do not exist in ATL yet.
 */
public final class SurfaceSyncGroup {
	private static final String TAG = "SurfaceSyncGroup";
	private static final int MAX_COUNT = 100;
	private static final AtomicInteger sCounter = new AtomicInteger(0);

	private final Object mLock = new Object();
	private final String mName;
	private final Transaction mTransaction = new Transaction();
	private boolean mSyncReady;
	private boolean mFinished;
	private int mPendingChildren;
	private SurfaceSyncGroup mParentSyncGroup;
	private final Consumer<Transaction> mTransactionReadyConsumer;
	private final ArraySet<Pair<Executor, Runnable>> mSyncCompleteCallbacks = new ArraySet<>();
	private Runnable mAddedToSyncListener;

	public SurfaceSyncGroup(@NonNull String name) {
		this(name, transaction -> {
			if (transaction != null) {
				transaction.apply();
			}
		});
	}

	public SurfaceSyncGroup(String name, Consumer<Transaction> transactionReadyConsumer) {
		if (sCounter.get() >= MAX_COUNT) {
			sCounter.set(0);
		}
		mName = name + "#" + sCounter.getAndIncrement();
		mTransactionReadyConsumer = transactionReadyConsumer;
	}

	public void addSyncCompleteCallback(Executor executor, Runnable runnable) {
		synchronized (mLock) {
			if (mFinished) {
				executor.execute(runnable);
				return;
			}
			mSyncCompleteCallbacks.add(new Pair<>(executor, runnable));
		}
	}

	public void markSyncReady() {
		synchronized (mLock) {
			mSyncReady = true;
		}
		checkIfSyncIsComplete();
	}

	public boolean add(@NonNull SurfaceSyncGroup surfaceSyncGroup, @Nullable Runnable runnable) {
		synchronized (mLock) {
			if (mSyncReady) {
				Log.w(TAG, "Trying to add to sync when already marked as ready " + mName);
				return false;
			}
		}
		if (runnable != null) {
			runnable.run();
		}
		return surfaceSyncGroup.setParent(this);
	}

	/* AOSP add(AttachedSurfaceControl, Runnable): the control's own sync group; AttachedSurfaceControl.getOrCreateSurfaceSyncGroup()
	 * returns null by default and ATL's controls have none, so there is nothing to wait for */
	public boolean add(@Nullable android.view.AttachedSurfaceControl attachedSurfaceControl, @Nullable Runnable runnable) {
		if (attachedSurfaceControl == null) {
			return false;
		}
		return false;
	}

	private boolean setParent(SurfaceSyncGroup parent) {
		Runnable listener;
		synchronized (mLock) {
			if (mFinished) {
				return true;
			}
			if (mParentSyncGroup != null && mParentSyncGroup != parent) {
				Log.e(TAG, "Trying to add to " + parent.mName + " but already part of sync group " + mParentSyncGroup.mName + " "
						+ mName);
				return false;
			}
			if (mParentSyncGroup == parent) {
				return true;
			}
			mParentSyncGroup = parent;
			listener = mAddedToSyncListener;
		}
		synchronized (parent.mLock) {
			parent.mPendingChildren++;
		}
		if (listener != null) {
			listener.run();
		}
		return true;
	}

	public void addTransaction(@NonNull Transaction transaction) {
		synchronized (mLock) {
			if (mFinished) {
				Log.w(TAG, "Adding transaction to a completed SurfaceSyncGroup(" + mName + "). Applying immediately");
				transaction.apply();
			} else {
				mTransaction.merge(transaction);
			}
		}
	}

	public void setAddedToSyncListener(Runnable addedToSyncListener) {
		synchronized (mLock) {
			mAddedToSyncListener = addedToSyncListener;
		}
	}

	private void childFinished(Transaction transaction) {
		synchronized (mLock) {
			if (transaction != null) {
				mTransaction.merge(transaction);
			}
			mPendingChildren--;
		}
		checkIfSyncIsComplete();
	}

	private void checkIfSyncIsComplete() {
		SurfaceSyncGroup parent;
		ArraySet<Pair<Executor, Runnable>> callbacks;
		synchronized (mLock) {
			if (mFinished || !mSyncReady || mPendingChildren > 0) {
				return;
			}
			mFinished = true;
			parent = mParentSyncGroup;
			callbacks = new ArraySet<>(mSyncCompleteCallbacks);
		}
		if (parent != null) {
			parent.childFinished(mTransaction);
		} else {
			mTransactionReadyConsumer.accept(mTransaction);
		}
		for (Pair<Executor, Runnable> cb : callbacks) {
			cb.first.execute(cb.second);
		}
	}
}
