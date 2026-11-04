/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.app;

import static android.view.WindowManager.LayoutParams.INVALID_WINDOW_TYPE;
import static android.view.WindowManager.LayoutParams.TYPE_PRESENTATION;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import java.util.Objects;

/**
 * Base class for presentations, a dialog shown on a secondary display (AOSP 14).
 * ATL has no secondary displays: DisplayManager and MediaRouter name none, so apps only need the class.
 */
public class Presentation extends Dialog {
	private static final String TAG = "Presentation";

	private final Display mDisplay;
	private final DisplayManager mDisplayManager;
	private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper(),
	                                                                    "Presentation must be constructed on a looper thread."));

	public Presentation(Context outerContext, Display display) {
		this(outerContext, display, 0);
	}

	public Presentation(Context outerContext, Display display, int theme) {
		this(outerContext, display, theme, INVALID_WINDOW_TYPE);
	}

	public Presentation(Context outerContext, Display display, int theme, int type) {
		super(createPresentationContext(outerContext, display, theme, type), theme, false);

		mDisplay = display;
		mDisplayManager = getContext().getSystemService(DisplayManager.class);

		final Window w = getWindow();
		final WindowManager.LayoutParams attr = w.getAttributes();
		w.setAttributes(attr);
		w.setGravity(Gravity.FILL);
		w.setType(getWindowType(type, display));
		setCanceledOnTouchOutside(false);
	}

	/* ATL: AOSP picks TYPE_PRIVATE_PRESENTATION for a display with Display.FLAG_PRIVATE; ATL has no such display */
	private static int getWindowType(int type, Display display) {
		if (type != INVALID_WINDOW_TYPE) {
			return type;
		}
		return TYPE_PRESENTATION;
	}

	public Display getDisplay() {
		return mDisplay;
	}

	public Resources getResources() {
		return getContext().getResources();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
	}

	@Override
	protected void onStop() {
		mDisplayManager.unregisterDisplayListener(mDisplayListener);
		super.onStop();
	}

	@Override
	public void show() {
		super.show();
	}

	public void onDisplayRemoved() {
	}

	public void onDisplayChanged() {
	}

	private void handleDisplayRemoved() {
		onDisplayRemoved();
		cancel();
	}

	private void handleDisplayChanged() {
		onDisplayChanged();
	}

	private static Context createPresentationContext(Context outerContext, Display display, int theme) {
		return createPresentationContext(outerContext, display, theme, INVALID_WINDOW_TYPE);
	}

	private static Context createPresentationContext(Context outerContext, Display display, int theme, int type) {
		if (outerContext == null) {
			throw new IllegalArgumentException("outerContext must not be null");
		}
		if (display == null) {
			throw new IllegalArgumentException("display must not be null");
		}

		Context windowContext = outerContext.createDisplayContext(display)
		                            .createWindowContext(getWindowType(type, display), null /* options */);
		if (theme == 0) {
			TypedValue outValue = new TypedValue();
			windowContext.getTheme().resolveAttribute(
			    com.android.internal.R.attr.presentationTheme, outValue, true);
			theme = outValue.resourceId;
		}
		return new ContextThemeWrapper(windowContext, theme);
	}

	private final DisplayListener mDisplayListener = new DisplayListener() {
		@Override
		public void onDisplayAdded(int displayId) {
		}

		@Override
		public void onDisplayRemoved(int displayId) {
			if (displayId == mDisplay.getDisplayId()) {
				handleDisplayRemoved();
			}
		}

		@Override
		public void onDisplayChanged(int displayId) {
			if (displayId == mDisplay.getDisplayId()) {
				handleDisplayChanged();
			}
		}
	};
}
