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

package android.service.notification;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

/**
 * A service that provides conditions about boolean state.
 * AOSP core/java/android/service/notification/ConditionProviderService.java. The system binds it through
 * IConditionProvider and sets mIsConnected on the first onConnected(); ATL has no notification manager
 * that binds, so the service stays "not yet bound" as in AOSP before that call. requestRebind() and
 * requestUnbind() ask that manager, which only acts on a provider the user enabled: no-op / disconnect here.
 */
@Deprecated
public abstract class ConditionProviderService extends Service {
	private final String TAG = ConditionProviderService.class.getSimpleName()
			+ "[" + getClass().getSimpleName() + "]";

	boolean mIsConnected;

	public static final String SERVICE_INTERFACE = "android.service.notification.ConditionProviderService";

	@Deprecated
	public static final String META_DATA_RULE_TYPE = "android.service.zen.automatic.ruleType";

	@Deprecated
	public static final String META_DATA_CONFIGURATION_ACTIVITY =
			"android.service.zen.automatic.configurationActivity";

	@Deprecated
	public static final String META_DATA_RULE_INSTANCE_LIMIT =
			"android.service.zen.automatic.ruleInstanceLimit";

	@Deprecated
	public static final String EXTRA_RULE_ID = "android.service.notification.extra.RULE_ID";

	abstract public void onConnected();

	public void onRequestConditions(int relevance) {}

	abstract public void onSubscribe(Uri conditionId);

	abstract public void onUnsubscribe(Uri conditionId);

	@Deprecated
	public final void notifyCondition(Condition condition) {
		if (condition == null) return;
		notifyConditions(new Condition[]{ condition });
	}

	/* AOSP: returns unless isBound(); the notification manager is only reached when bound, so here nothing
	 * is ever sent */
	@Deprecated
	public final void notifyConditions(Condition... conditions) {
		if (!isBound() || conditions == null) return;
	}

	/* AOSP requestBindProvider: the manager rebinds a provider that is enabled, none is here */
	@Deprecated
	public static final void requestRebind(ComponentName componentName) {}

	/* AOSP requestUnbindProvider, then disables future messages */
	@Deprecated
	public final void requestUnbind() {
		mIsConnected = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		/* kein IConditionProvider-Stub, niemand bindet */
		return null;
	}

	/* AOSP ConditionProviderService.isBound */
	public boolean isBound() {
		if (!mIsConnected) {
			Log.w(TAG, "Condition provider service not yet bound.");
		}
		return mIsConnected;
	}
}
