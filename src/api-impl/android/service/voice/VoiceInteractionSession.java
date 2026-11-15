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

import android.annotation.Nullable;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;

/**
 * An active voice interaction session, providing a facility for the implementation to interact with
 * the user in the voice interaction layer.
 * AOSP core/java/android/service/voice/VoiceInteractionSession.java. mToken is set by doCreate() when the
 * voice interaction manager hands over the session; ATL has no manager, so it stays null and the calls
 * that go to the manager throw as in AOSP before onCreate(). The window (getWindow(), setContentView(),
 * VoiceInteractionWindow and its layout), requests, assist data and the other manager calls are not ported.
 */
public class VoiceInteractionSession implements KeyEvent.Callback, ComponentCallbacks2 {

	public static final int SHOW_WITH_ASSIST = 1<<0;
	public static final int SHOW_WITH_SCREENSHOT = 1<<1;
	public static final int SHOW_SOURCE_ASSIST_GESTURE = 1<<2;
	public static final int SHOW_SOURCE_APPLICATION = 1<<3;
	public static final int SHOW_SOURCE_ACTIVITY = 1<<4;
	public static final int SHOW_SOURCE_PUSH_TO_TALK = 1 << 5;
	public static final int SHOW_SOURCE_NOTIFICATION = 1 << 6;
	public static final int SHOW_SOURCE_AUTOMOTIVE_SYSTEM_UI = 1 << 7;

	public static final int VOICE_INTERACTION_ACTIVITY_EVENT_START = 1;
	public static final int VOICE_INTERACTION_ACTIVITY_EVENT_RESUME = 2;
	public static final int VOICE_INTERACTION_ACTIVITY_EVENT_PAUSE = 3;
	public static final int VOICE_INTERACTION_ACTIVITY_EVENT_STOP = 4;

	public static final String KEY_SHOW_SESSION_ID = "android.service.voice.SHOW_SESSION_ID";

	final Context mContext;
	final Handler mHandler;
	Object mToken;

	public VoiceInteractionSession(Context context) {
		this(context, new Handler());
	}

	public VoiceInteractionSession(Context context, Handler handler) {
		mHandler = handler;
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	/* AOSP VoiceInteractionSession.show */
	public void show(Bundle args, int flags) {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	/* AOSP VoiceInteractionSession.hide */
	public void hide() {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	/* AOSP VoiceInteractionSession.closeSystemDialogs */
	public void closeSystemDialogs() {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	/* AOSP VoiceInteractionSession.getLayoutInflater: ensureWindowCreated() reads it from the context */
	public LayoutInflater getLayoutInflater() {
		return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/* AOSP VoiceInteractionSession.finish */
	public void finish() {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	public void onCreate() {
	}

	public void onShow(@Nullable Bundle args, int showFlags) {
	}

	public void onHide() {
	}

	public void onDestroy() {
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return false;
	}

	public void onBackPressed() {
		hide();
	}

	public void onCloseSystemDialogs() {
		hide();
	}

	public void onLockscreenShown() {
		hide();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

	@Override
	public void onLowMemory() {
	}

	@Override
	public void onTrimMemory(int level) {
	}
}
