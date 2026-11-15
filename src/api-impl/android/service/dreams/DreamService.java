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

package android.service.dreams;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;

/**
 * Extend this class to implement a custom dream (available to the user as a "Daydream").
 * AOSP core/java/android/service/dreams/DreamService.java. In AOSP the dream manager binds the service
 * through IDreamService.attach(), which creates the window and calls onAttachedToWindow() and
 * onDreamingStarted(); ATL has no dream manager, so the service stays unattached, as AOSP's before
 * attach(): no window (getWindow() null), no dream token. Dozing, overlay and activity based parts are
 * not ported, nor setContentView(int) and setContentView(View, LayoutParams) (ATL Window has no such method).
 */
public class DreamService extends Service implements Window.Callback {

	public static final String DREAM_SERVICE = "dreams";

	public static final String SERVICE_INTERFACE = "android.service.dreams.DreamService";

	public static final String DREAM_META_DATA = "android.service.dream";

	public static final boolean DEFAULT_SHOW_COMPLICATIONS = false;

	public static final int DREAM_CATEGORY_DEFAULT = 0;
	public static final int DREAM_CATEGORY_LOW_LIGHT = 1 << 0;
	public static final int DREAM_CATEGORY_HOME_PANEL = 1 << 1;

	private Window mWindow;
	private boolean mInteractive;
	private boolean mScreenBright = true;
	private boolean mFullscreen;
	private boolean mWindowless;
	private boolean mFinished;

	public DreamService() {
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (!mInteractive) {
			wakeUp();
			return true;
		}
		return mWindow.superDispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		if (!mInteractive) {
			wakeUp();
			return true;
		}
		return mWindow.superDispatchKeyShortcutEvent(event);
	}

	/* AOSP DreamService.dispatchTouchEvent */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (!mInteractive && event.getActionMasked() == MotionEvent.ACTION_UP) {
			wakeUp();
			return true;
		}
		return mWindow.superDispatchTouchEvent(event);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		if (!mInteractive) {
			wakeUp();
			return true;
		}
		return mWindow.superDispatchTrackballEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		if (!mInteractive) {
			wakeUp();
			return true;
		}
		return mWindow.superDispatchGenericMotionEvent(event);
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		return false;
	}

	@Override
	public View onCreatePanelView(int featureId) {
		return null;
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		return false;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		return false;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return false;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return false;
	}

	@Override
	public void onWindowAttributesChanged(LayoutParams attrs) {
	}

	@Override
	public void onContentChanged() {
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	}

	@Override
	public void onAttachedToWindow() {
	}

	@Override
	public void onDetachedFromWindow() {
	}

	@Override
	public void onPanelClosed(int featureId, Menu menu) {
	}

	@Override
	public boolean onSearchRequested(SearchEvent event) {
		return onSearchRequested();
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	@Override
	public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
		return null;
	}

	@Override
	public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
		return null;
	}

	@Override
	public void onActionModeStarted(ActionMode mode) {
	}

	@Override
	public void onActionModeFinished(ActionMode mode) {
	}

	/* AOSP DreamService.getWindowManager */
	public WindowManager getWindowManager() {
		return mWindow != null ? mWindow.getWindowManager() : null;
	}

	/* AOSP DreamService.getWindow: the current window, or null if the dream is not started */
	public Window getWindow() {
		return mWindow;
	}

	public void setContentView(View view) {
		getWindow().setContentView(view);
	}

	public void addContentView(View view, ViewGroup.LayoutParams params) {
		getWindow().addContentView(view, params);
	}

	@SuppressWarnings("unchecked")
	public <T extends View> T findViewById(int id) {
		return (T) getWindow().findViewById(id);
	}

	public void setInteractive(boolean interactive) {
		mInteractive = interactive;
	}

	public boolean isInteractive() {
		return mInteractive;
	}

	/* AOSP DreamService.setFullscreen: the window flag is applied by applyWindowFlags() once a window exists */
	public void setFullscreen(boolean fullscreen) {
		if (mFullscreen != fullscreen) {
			mFullscreen = fullscreen;
			int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
			applyWindowFlags(mFullscreen ? flag : 0, flag);
		}
	}

	public boolean isFullscreen() {
		return mFullscreen;
	}

	/* AOSP DreamService.setScreenBright (the preview-mode check is dropped: previews come from the system) */
	public void setScreenBright(boolean screenBright) {
		if (mScreenBright != screenBright) {
			mScreenBright = screenBright;
			int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			applyWindowFlags(mScreenBright ? flag : 0, flag);
		}
	}

	/* AOSP DreamService.isScreenBright: getWindowFlagValue(FLAG_KEEP_SCREEN_ON, mScreenBright) */
	public boolean isScreenBright() {
		if (mWindow == null) {
			return mScreenBright;
		}
		return (mWindow.getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
	}

	public void setWindowless(boolean windowless) {
		mWindowless = windowless;
	}

	public boolean isWindowless() {
		return mWindowless;
	}

	public void onDreamingStarted() {
		// hook for subclasses
	}

	public void onDreamingStopped() {
		// hook for subclasses
	}

	/* AOSP DreamService.onWakeUp: without overlay it is finish() */
	public void onWakeUp() {
		finish();
	}

	@Override
	public final IBinder onBind(Intent intent) {
		/* kein IDreamService-Stub, niemand bindet */
		return null;
	}

	/* AOSP DreamService.finish: with no dream token ("finish() called when not attached") only stopSelf() */
	public final void finish() {
		if (mFinished) {
			return;
		}
		mFinished = true;
		stopSelf();
	}

	/* AOSP DreamService.wakeUp() -> wakeUp(false); there is no overlay/dream manager to redirect to, so
	 * it ends in finish() */
	public final void wakeUp() {
		finish();
	}

	public boolean getRedirectWake() {
		return false;
	}

	/* AOSP DreamService.applyWindowFlags: only acts on an existing window */
	private void applyWindowFlags(int flags, int mask) {
		if (mWindow != null) {
			mWindow.setFlags(flags, mask);
		}
	}
}
