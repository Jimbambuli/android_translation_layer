/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.graphics.Rect;

/**
 * android-14 android.view.TouchDelegate with onTouchExplorationHoverEvent() from AOSP 16,
 * without getTouchDelegateInfo().
 */
public class TouchDelegate {

	private View mDelegateView;

	private Rect mBounds;

	private Rect mSlopBounds;

	private boolean mDelegateTargeted;

	public static final int ABOVE = 1;

	public static final int BELOW = 2;

	public static final int TO_LEFT = 4;

	public static final int TO_RIGHT = 8;

	private int mSlop;

	public TouchDelegate(Rect bounds, View delegateView) {
		mBounds = bounds;

		mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();
		mSlopBounds = new Rect(bounds);
		mSlopBounds.inset(-mSlop, -mSlop);
		mDelegateView = delegateView;
	}

	public boolean onTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();
		boolean sendToDelegate = false;
		boolean hit = true;
		boolean handled = false;

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mDelegateTargeted = mBounds.contains(x, y);
				sendToDelegate = mDelegateTargeted;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_MOVE:
				sendToDelegate = mDelegateTargeted;
				if (sendToDelegate) {
					Rect slopBounds = mSlopBounds;
					if (!slopBounds.contains(x, y)) {
						hit = false;
					}
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				sendToDelegate = mDelegateTargeted;
				mDelegateTargeted = false;
				break;
		}
		if (sendToDelegate) {
			if (hit) {
				// Offset event coordinates to be inside the target view
				event.setLocation(mDelegateView.getWidth() / 2, mDelegateView.getHeight() / 2);
			} else {
				// Offset event coordinates to be outside the target view (in case it does
				// something like tracking pressed state)
				int slop = mSlop;
				event.setLocation(-(slop * 2), -(slop * 2));
			}
			handled = mDelegateView.dispatchTouchEvent(event);
		}
		return handled;
	}

	/* AOSP 16 TouchDelegate.onTouchExplorationHoverEvent() */
	public boolean onTouchExplorationHoverEvent(MotionEvent event) {
		if (mBounds == null) {
			return false;
		}

		final int x = (int)event.getX();
		final int y = (int)event.getY();
		boolean hit = true;
		boolean handled = false;

		final boolean isInbound = mBounds.contains(x, y);
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_HOVER_ENTER:
				mDelegateTargeted = isInbound;
				break;
			case MotionEvent.ACTION_HOVER_MOVE:
				if (isInbound) {
					mDelegateTargeted = true;
				} else {
					// delegated previously
					if (mDelegateTargeted && !mSlopBounds.contains(x, y)) {
						hit = false;
					}
				}
				break;
			case MotionEvent.ACTION_HOVER_EXIT:
				mDelegateTargeted = true;
				break;
		}
		if (mDelegateTargeted) {
			if (hit) {
				event.setLocation(mDelegateView.getWidth() / 2, mDelegateView.getHeight() / 2);
			} else {
				mDelegateTargeted = false;
			}
			handled = mDelegateView.dispatchHoverEvent(event);
		}
		return handled;
	}
}
