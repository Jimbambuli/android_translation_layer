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

package android.view;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Safe identifier for a window.  This currently allows you to retrieve and observe
 * the input focus state of the window.  Most applications will
 * not use this, instead relying on the simpler {@link View#isFocused() View.isFocused()}
 * and {@link View#hasWindowFocus() View.hasWindowFocus()} methods for the
 * views being used.
 *
 * ATL: AOSP's WindowId is an IWindowId binder to the window manager. ATL has none, so the id is a
 * Binder token per window root view (View.getWindowId()); isFocused() asks the root view. The focus
 * observers are only kept in the registrations (AOSP's checks of double registering stay), ATL does not
 * deliver onFocusGained()/onFocusLost() to them.
 */
public class WindowId implements Parcelable {
	private static final WeakHashMap<View, WindowId> sIds = new WeakHashMap<>();

	@NonNull private final IBinder mToken;
	private final WeakReference<View> mRoot;

	/**
	 * Subclass for observing changes to the focus state of an {@link WindowId}.
	 * You should use this to tell if a window is the front window.
	 */
	public static abstract class FocusObserver {
		final HashMap<IBinder, WindowId> mRegistrations = new HashMap<>();

		public FocusObserver() {}

		/**
		 * Called when one of the monitored windows gains input focus.
		 */
		public abstract void onFocusGained(WindowId token);

		/**
		 * Called when one of the monitored windows loses input focus.
		 */
		public abstract void onFocusLost(WindowId token);
	}

	/**
	 * Retrieve the current focus state of the window.
	 */
	public boolean isFocused() {
		View root = mRoot != null ? mRoot.get() : null;
		return root != null && root.hasWindowFocus();
	}

	/**
	 * Start monitoring for changes in the focus state of the window.
	 */
	public void registerFocusObserver(FocusObserver observer) {
		synchronized (observer.mRegistrations) {
			if (observer.mRegistrations.containsKey(mToken)) {
				throw new IllegalStateException("Focus observer already registered with input token");
			}
			observer.mRegistrations.put(mToken, this);
		}
	}

	/**
	 * Stop monitoring changes in the focus state of the window.
	 */
	public void unregisterFocusObserver(FocusObserver observer) {
		synchronized (observer.mRegistrations) {
			if (observer.mRegistrations.remove(mToken) == null) {
				throw new IllegalStateException("Focus observer not registered with input token");
			}
		}
	}

	/**
	 * Comparison operator on two IntentSender objects, such that true
	 * is returned then they both represent the same operation from the
	 * same package.
	 */
	@Override
	public boolean equals(@Nullable Object otherObj) {
		if (otherObj instanceof WindowId) {
			return mToken.equals(((WindowId)otherObj).mToken);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mToken.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("IntentSender{");
		sb.append(Integer.toHexString(System.identityHashCode(this)));
		sb.append(": ");
		sb.append(mToken);
		sb.append('}');
		return sb.toString();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeStrongBinder(mToken);
	}

	public static final @android.annotation.NonNull Parcelable.Creator<WindowId> CREATOR = new Parcelable.Creator<WindowId>() {
		@Override
		public WindowId createFromParcel(Parcel in) {
			IBinder target = in.readStrongBinder();
			return target != null ? new WindowId(target) : null;
		}

		@Override
		public WindowId[] newArray(int size) {
			return new WindowId[size];
		}
	};

	/** The id of the window whose root view is {@code root} (the same object for every call). */
	static WindowId of(View root) {
		synchronized (sIds) {
			WindowId id = sIds.get(root);
			if (id == null) {
				id = new WindowId(new Binder(), root);
				sIds.put(root, id);
			}
			return id;
		}
	}

	/** @hide */
	public WindowId(@NonNull IBinder target) {
		this(target, null);
	}

	private WindowId(IBinder target, View root) {
		mToken = target;
		mRoot = root != null ? new WeakReference<>(root) : null;
	}
}
