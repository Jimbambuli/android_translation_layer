/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.print;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Objects;

/* AOSP core/java/android/print/PrintJob.java: the handle of a print job returned by PrintManager.print().
 * Without a print spooler service (see PrintManager) print() returns null and getPrintJobs() is empty, so
 * no instance is made by ATL itself; the class is complete for apps that construct none and for the
 * methods they call on one. */
public final class PrintJob {
    private final @NonNull PrintManager mPrintManager;

    private @NonNull PrintJobInfo mCachedInfo;

    PrintJob(@NonNull PrintJobInfo info, @NonNull PrintManager printManager) {
        mCachedInfo = info;
        mPrintManager = printManager;
    }

    /**
     * Gets the unique print job id.
     *
     * @return The id.
     */
    public @Nullable PrintJobId getId() {
        return mCachedInfo.getId();
    }

    /**
     * Gets the {@link PrintJobInfo} that describes this job.
     * <p>
     * <strong>Node:</strong>The returned info object is a snapshot of the
     * current print job state. Every call to this method returns a fresh
     * info object that reflects the current print job state.
     * </p>
     *
     * @return The print job info.
     */
    public @NonNull PrintJobInfo getInfo() {
        if (isInImmutableState()) {
            return mCachedInfo;
        }
        PrintJobInfo info = mPrintManager.getPrintJobInfo(mCachedInfo.getId());
        if (info != null) {
            mCachedInfo = info;
        }
        return mCachedInfo;
    }

    /**
     * Cancels this print job.
     */
    public void cancel() {
        final int state = getInfo().getState();
        if (state == PrintJobInfo.STATE_QUEUED
                || state == PrintJobInfo.STATE_STARTED
                || state == PrintJobInfo.STATE_BLOCKED
                || state == PrintJobInfo.STATE_FAILED) {
            mPrintManager.cancelPrintJob(mCachedInfo.getId());
        }
    }

    /**
     * Restarts this print job.
     */
    public void restart() {
        if (isFailed()) {
            mPrintManager.restartPrintJob(mCachedInfo.getId());
        }
    }

    /**
     * Gets whether this print job is queued.
     */
    public boolean isQueued() {
        return getInfo().getState() == PrintJobInfo.STATE_QUEUED;
    }

    /**
     * Gets whether this print job is started.
     */
    public boolean isStarted() {
        return getInfo().getState() == PrintJobInfo.STATE_STARTED;
    }

    /**
     * Gets whether this print job is blocked.
     */
    public boolean isBlocked() {
        return getInfo().getState() == PrintJobInfo.STATE_BLOCKED;
    }

    /**
     * Gets whether this print job is completed.
     */
    public boolean isCompleted() {
        return getInfo().getState() == PrintJobInfo.STATE_COMPLETED;
    }

    /**
     * Gets whether this print job is failed.
     */
    public boolean isFailed() {
        return getInfo().getState() == PrintJobInfo.STATE_FAILED;
    }

    /**
     * Gets whether this print job is cancelled.
     */
    public boolean isCancelled() {
        return getInfo().getState() == PrintJobInfo.STATE_CANCELED;
    }

    private boolean isInImmutableState() {
        final int state = mCachedInfo.getState();
        return state == PrintJobInfo.STATE_COMPLETED
                || state == PrintJobInfo.STATE_CANCELED;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PrintJob other = (PrintJob) obj;
        return Objects.equals(mCachedInfo.getId(), other.mCachedInfo.getId());
    }

    @Override
    public int hashCode() {
        PrintJobId printJobId = mCachedInfo.getId();
        if (printJobId == null) {
            return 0;
        } else {
            return printJobId.hashCode();
        }
    }
}
