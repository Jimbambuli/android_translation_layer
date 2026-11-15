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

import android.annotation.NonNull;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import java.util.Collections;
import java.util.Set;

/**
 * Top-level service of the current global voice interactor, which is providing support for hotwording,
 * the back-end of a {@link android.app.VoiceInteractor}, etc.
 * AOSP core/java/android/service/voice/VoiceInteractionService.java. In AOSP the voice interaction manager
 * binds the service through IVoiceInteractionService and onReady() obtains its mSystemService; ATL has
 * no such manager, so mSystemService stays null as in AOSP before onReady(). Hotword/visual query
 * detector creation, sound trigger queries and dump are not ported.
 */
public class VoiceInteractionService extends Service {

	public static final String SERVICE_INTERFACE = "android.service.voice.VoiceInteractionService";

	public static final String SERVICE_META_DATA = "android.voice_interaction";

	public void onLaunchVoiceAssistFromKeyguard() {
	}

	public void onPrepareToShowSession(@NonNull Bundle args, int flags) {
	}

	public void onShowSessionFailed(@NonNull Bundle args) {
	}

	/* AOSP VoiceInteractionService.isActiveService: compares with the manager's current service, which
	 * ATL does not have, so no service is ever active */
	public static boolean isActiveService(Context context, ComponentName service) {
		return false;
	}

	/* AOSP: forwards to mSystemService, which does not exist before onReady(); nothing is stored */
	public void setDisabledShowContext(int flags) {
	}

	public int getDisabledShowContext() {
		return 0;
	}

	/* AOSP VoiceInteractionService.setUiHints: the hints must be given, then they go to mSystemService,
	 * which does not exist here (see setDisabledShowContext) */
	public final void setUiHints(@NonNull Bundle hints) {
		if (hints == null) {
			throw new IllegalArgumentException("Hints must be non-null");
		}
	}

	/* AOSP VoiceInteractionService.setInvocationEffectEnabled: forwarded to mSystemService, none here */
	public final void setInvocationEffectEnabled(boolean enabled) {
	}

	/* AOSP VoiceInteractionService.showSession */
	public void showSession(Bundle args, int flags) {
		throw new IllegalStateException("Not available until onReady() is called");
	}

	public Set<String> onGetSupportedVoiceActions(@NonNull Set<String> voiceActions) {
		return Collections.emptySet();
	}

	/* AOSP returns mInterface.asBinder() for SERVICE_INTERFACE; kein IVoiceInteractionService-Stub, niemand bindet */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onReady() {
	}

	public void onShutdown() {
	}
}
