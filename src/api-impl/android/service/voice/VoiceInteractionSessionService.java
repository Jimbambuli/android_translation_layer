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

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;

/**
 * An active voice interaction session, providing a facility for the implementation to interact with
 * the user in the voice interaction layer.
 * AOSP core/java/android/service/voice/VoiceInteractionSessionService.java. AOSP creates the session in
 * IVoiceInteractionSessionService.newSession() from the voice interaction manager; ATL has no manager, so
 * onNewSession() is never called and mSession stays null.
 */
public abstract class VoiceInteractionSessionService extends Service {

	VoiceInteractionSession mSession;

	public abstract VoiceInteractionSession onNewSession(Bundle args);

	/* AOSP returns mInterface.asBinder(); kein IVoiceInteractionSessionService-Stub, niemand bindet */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mSession != null) {
			mSession.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if (mSession != null) {
			mSession.onLowMemory();
		}
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		if (mSession != null) {
			mSession.onTrimMemory(level);
		}
	}
}
