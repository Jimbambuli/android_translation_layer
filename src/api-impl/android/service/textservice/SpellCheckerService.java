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

package android.service.textservice;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

/**
 * SpellCheckerService provides an abstract base class for a spell checker.
 * AOSP core/java/android/service/textservice/SpellCheckerService.java. The ISpellCheckerService binder and
 * InternalISpellCheckerSession are left out (the system never binds); onGetSentenceSuggestionsMultiple
 * (WordIterator based) is not ported.
 */
public abstract class SpellCheckerService extends android.app.Service {

	public static final String SERVICE_INTERFACE = "android.service.textservice.SpellCheckerService";

	@Override
	public final IBinder onBind(final Intent intent) {
		/* kein ISpellCheckerService-Stub, niemand bindet */
		return null;
	}

	public abstract Session createSession();

	public static abstract class Session {

		public abstract void onCreate();

		public abstract SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit);

		/* AOSP SpellCheckerService.Session.onGetSuggestionsMultiple */
		public SuggestionsInfo[] onGetSuggestionsMultiple(TextInfo[] textInfos,
		                                                  int suggestionsLimit, boolean sequentialWords) {
			final int length = textInfos.length;
			final SuggestionsInfo[] retval = new SuggestionsInfo[length];
			for (int i = 0; i < length; ++i) {
				retval[i] = onGetSuggestions(textInfos[i], suggestionsLimit);
				retval[i].setCookieAndSequence(
						textInfos[i].getCookie(), textInfos[i].getSequence());
			}
			return retval;
		}

		public void onCancel() {}

		public void onClose() {}

		/* AOSP: mInternalSession.getLocale(); the internal session is only attached by the binder (NPE
		 * in AOSP before that), here simply null */
		public String getLocale() {
			return null;
		}

		/* AOSP: mInternalSession.getBundle(), see getLocale() */
		public Bundle getBundle() {
			return null;
		}
	}
}
