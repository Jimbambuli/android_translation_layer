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

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * An overlay is an extra layer that sits on top of a View (the "host view") which is drawn after
 * all other content in that view (including children, if the view is a ViewGroup).
 *
 * ATL: the drawables are drawn by View.draw() of the host, as in AOSP, but the children of a ViewGroup
 * are GTK widgets that GTK draws after the view's own draw(), so the drawables show below them. Views are
 * not held (ViewGroupOverlay.add(View) only checks its argument): a view needs a place in the widget tree.
 */
public class ViewOverlay {
	private final View mHostView;
	private ArrayList<Drawable> mDrawables = null;

	ViewOverlay(View hostView) {
		mHostView = hostView;
	}

	/** Adds a Drawable to the overlay. The bounds of the drawable should be set relative to the host view. */
	public void add(Drawable drawable) {
		if (drawable == null) {
			throw new IllegalArgumentException("drawable must be non-null");
		}
		if (mDrawables == null) {
			mDrawables = new ArrayList<>();
		}
		if (!mDrawables.contains(drawable)) {
			mDrawables.add(drawable);
			mHostView.invalidate(drawable.getBounds());
			drawable.setCallback(mHostView);
			mHostView.overlayChanged();
		}
	}

	/** Removes the specified Drawable from the overlay. */
	public void remove(Drawable drawable) {
		if (drawable == null) {
			throw new IllegalArgumentException("drawable must be non-null");
		}
		if (mDrawables != null) {
			mDrawables.remove(drawable);
			mHostView.invalidate(drawable.getBounds());
			drawable.setCallback(null);
		}
	}

	/** Removes all content from the overlay. */
	public void clear() {
		if (mDrawables != null) {
			for (Drawable drawable : mDrawables) {
				drawable.setCallback(null);
			}
			mDrawables.clear();
			mHostView.invalidate();
		}
	}

	boolean isEmpty() {
		return mDrawables == null || mDrawables.isEmpty();
	}

	boolean verifyDrawable(Drawable who) {
		return mDrawables != null && mDrawables.contains(who);
	}

	/* AOSP: OverlayViewGroup.dispatchDraw() */
	void draw(Canvas canvas) {
		if (mDrawables != null) {
			for (int i = 0; i < mDrawables.size(); i++) {
				mDrawables.get(i).draw(canvas);
			}
		}
	}
}
