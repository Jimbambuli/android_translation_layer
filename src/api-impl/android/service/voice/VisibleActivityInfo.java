/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.service.voice;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Represents a visible activity information, see VoiceInteractionSession.VisibleActivityCallback.
 * AOSP core/java/android/service/voice/VisibleActivityInfo.java (android-16).
 */
public final class VisibleActivityInfo implements Parcelable {
	public static final int TYPE_ACTIVITY_ADDED = 1;

	public static final int TYPE_ACTIVITY_REMOVED = 2;

	private final int mTaskId;
	private final IBinder mAssistToken;

	public VisibleActivityInfo(int taskId, IBinder assistToken) {
		Objects.requireNonNull(assistToken);
		mTaskId = taskId;
		mAssistToken = assistToken;
	}

	public VoiceInteractionSession.ActivityId getActivityId() {
		return new VoiceInteractionSession.ActivityId(mTaskId, mAssistToken);
	}

	@Override
	public String toString() {
		return "VisibleActivityInfo { " + "taskId = " + mTaskId + ", " + "assistToken = " + mAssistToken + " }";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VisibleActivityInfo that = (VisibleActivityInfo)o;
		return mTaskId == that.mTaskId && Objects.equals(mAssistToken, that.mAssistToken);
	}

	@Override
	public int hashCode() {
		int _hash = 1;
		_hash = 31 * _hash + mTaskId;
		_hash = 31 * _hash + Objects.hashCode(mAssistToken);
		return _hash;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mTaskId);
		dest.writeStrongBinder(mAssistToken);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	VisibleActivityInfo(Parcel in) {
		int taskId = in.readInt();
		IBinder assistToken = in.readStrongBinder();
		this.mTaskId = taskId;
		this.mAssistToken = assistToken;
		Objects.requireNonNull(mAssistToken);
	}

	public static final Parcelable.Creator<VisibleActivityInfo> CREATOR = new Parcelable.Creator<VisibleActivityInfo>() {
		@Override
		public VisibleActivityInfo[] newArray(int size) {
			return new VisibleActivityInfo[size];
		}

		@Override
		public VisibleActivityInfo createFromParcel(Parcel in) {
			return new VisibleActivityInfo(in);
		}
	};
}
