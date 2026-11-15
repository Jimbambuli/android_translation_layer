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
import android.app.Dialog;
import android.app.DirectAction;
import android.app.VoiceInteractor;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An active voice interaction session, providing a facility for the implementation to interact with
 * the user in the voice interaction layer.
 * AOSP core/java/android/service/voice/VoiceInteractionSession.java. mToken is set by doCreate() when the
 * voice interaction manager hands over the session; ATL has no manager, so it stays null and the calls
 * that go to the manager throw as in AOSP before onCreate(). The requests (and their callbacks to the
 * VoiceInteractor binder), the visible activity notifications and the manager's show/hide/assist
 * deliveries only exist when that manager is there, so nothing ever creates a Request or an
 * AssistState here. The window is a Dialog with a content frame (AOSP's voice_interaction_session layout,
 * VoiceInteractionWindow's window type, token and insets listener are left out).
 */
public class VoiceInteractionSession implements KeyEvent.Callback, ComponentCallbacks2 {
	static final String TAG = "VoiceInteractionSession";
	static final boolean DEBUG = false;

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

	public static final String KEY_FOREGROUND_ACTIVITIES = "android.service.voice.FOREGROUND_ACTIVITIES";

	final Context mContext;
	final Handler mHandler;
	final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();
	Object mToken;

	int mTheme = 0;
	LayoutInflater mInflater;
	View mRootView;
	FrameLayout mContentFrame;
	VoiceInteractionWindow mWindow;

	boolean mUiEnabled = true;
	boolean mInitialized;
	boolean mWindowAdded;
	boolean mWindowVisible;
	boolean mWindowWasVisible;
	boolean mInShowWindow;

	final LinkedHashSet<Request> mActiveRequests = new LinkedHashSet<Request>();
	final Insets mTmpInsets = new Insets();
	final WeakReference<VoiceInteractionSession> mWeakRef = new WeakReference<VoiceInteractionSession>(this);

	private final Map<VisibleActivityCallback, Executor> mVisibleActivityCallbacks = new HashMap<>();
	private final List<VisibleActivityInfo> mVisibleActivityInfos = new ArrayList<>();

	/* AOSP VoiceInteractionWindow: a Dialog that gives the key events and Back to the session. Its window
	 * type, gravity, token handshake and touch clamping belong to the window manager and are left out */
	static final class VoiceInteractionWindow extends Dialog {
		private final VoiceInteractionSession mSession;

		VoiceInteractionWindow(Context context, int theme, VoiceInteractionSession session) {
			super(context, theme);
			mSession = session;
		}

		@Override
		public void onWindowFocusChanged(boolean hasFocus) {
			super.onWindowFocusChanged(hasFocus);
			mSession.mDispatcherState.reset();
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			if (mSession.onKeyDown(keyCode, event)) {
				return true;
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		public boolean onKeyLongPress(int keyCode, KeyEvent event) {
			if (mSession.onKeyLongPress(keyCode, event)) {
				return true;
			}
			return super.onKeyLongPress(keyCode, event);
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			if (mSession.onKeyUp(keyCode, event)) {
				return true;
			}
			return super.onKeyUp(keyCode, event);
		}

		@Override
		public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
			if (mSession.onKeyMultiple(keyCode, count, event)) {
				return true;
			}
			return super.onKeyMultiple(keyCode, count, event);
		}

		@Override
		public void onBackPressed() {
			mSession.onBackPressed();
		}
	}

	/* AOSP VoiceInteractionSession.Request: the VoiceInteractor binder callback (mCallback, mInterface) is
	 * the manager's side and does not exist here, so the result is only checked against the active requests */
	public static class Request {
		final String mCallingPackage;
		final int mCallingUid;
		final WeakReference<VoiceInteractionSession> mSession;
		final Bundle mExtras;

		Request(String packageName, int uid, VoiceInteractionSession session, Bundle extras) {
			mCallingPackage = packageName;
			mCallingUid = uid;
			mSession = session.mWeakRef;
			mExtras = extras;
		}

		public int getCallingUid() {
			return mCallingUid;
		}

		public String getCallingPackage() {
			return mCallingPackage;
		}

		public Bundle getExtras() {
			return mExtras;
		}

		public boolean isActive() {
			VoiceInteractionSession session = mSession.get();
			if (session == null) {
				return false;
			}
			return session.isRequestActive(this);
		}

		void finishRequest() {
			VoiceInteractionSession session = mSession.get();
			if (session == null) {
				throw new IllegalStateException("VoiceInteractionSession has been destroyed");
			}
			Request req = session.removeRequest(this);
			if (req == null) {
				throw new IllegalStateException("Request not active: " + this);
			} else if (req != this) {
				throw new IllegalStateException("Current active request " + req + " not same as calling request " + this);
			}
		}

		public void cancel() {
			if (DEBUG) Log.d(TAG, "sendCancelResult: req=" + this);
			finishRequest();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(128);
			sb.append(getClass().getSimpleName());
			sb.append('{').append(Integer.toHexString(System.identityHashCode(this)));
			sb.append(" pkg=");
			sb.append(mCallingPackage);
			sb.append(" uid=");
			sb.append(mCallingUid);
			sb.append('}');
			return sb.toString();
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			writer.print(prefix); writer.print("mCallingPackage="); writer.print(mCallingPackage);
			writer.print(" mCallingUid="); writer.println(mCallingUid);
			if (mExtras != null) {
				writer.print(prefix); writer.print("mExtras=");
				writer.println(mExtras);
			}
		}
	}

	public static final class ConfirmationRequest extends Request {
		final VoiceInteractor.Prompt mPrompt;

		ConfirmationRequest(String packageName, int uid, VoiceInteractionSession session,
		                    VoiceInteractor.Prompt prompt, Bundle extras) {
			super(packageName, uid, session, extras);
			mPrompt = prompt;
		}

		@Nullable
		public VoiceInteractor.Prompt getVoicePrompt() {
			return mPrompt;
		}

		@Deprecated
		@Nullable
		public CharSequence getPrompt() {
			return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
		}

		public void sendConfirmationResult(boolean confirmed, Bundle result) {
			if (DEBUG) Log.d(TAG, "sendConfirmationResult: req=" + this + " confirmed=" + confirmed + " result=" + result);
			finishRequest();
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			super.dump(prefix, fd, writer, args);
			writer.print(prefix); writer.print("mPrompt=");
			writer.println(mPrompt);
		}
	}

	public static final class PickOptionRequest extends Request {
		final VoiceInteractor.Prompt mPrompt;
		final VoiceInteractor.PickOptionRequest.Option[] mOptions;

		PickOptionRequest(String packageName, int uid, VoiceInteractionSession session,
		                  VoiceInteractor.Prompt prompt, VoiceInteractor.PickOptionRequest.Option[] options,
		                  Bundle extras) {
			super(packageName, uid, session, extras);
			mPrompt = prompt;
			mOptions = options;
		}

		@Nullable
		public VoiceInteractor.Prompt getVoicePrompt() {
			return mPrompt;
		}

		@Deprecated
		@Nullable
		public CharSequence getPrompt() {
			return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
		}

		public VoiceInteractor.PickOptionRequest.Option[] getOptions() {
			return mOptions;
		}

		void sendPickOptionResult(boolean finished, VoiceInteractor.PickOptionRequest.Option[] selections,
		                          Bundle result) {
			if (DEBUG) Log.d(TAG, "sendPickOptionResult: req=" + this + " finished=" + finished + " selections="
			                          + Arrays.toString(selections) + " result=" + result);
			if (finished) {
				finishRequest();
			}
		}

		public void sendIntermediatePickOptionResult(VoiceInteractor.PickOptionRequest.Option[] selections,
		                                             Bundle result) {
			sendPickOptionResult(false, selections, result);
		}

		public void sendPickOptionResult(VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
			sendPickOptionResult(true, selections, result);
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			super.dump(prefix, fd, writer, args);
			writer.print(prefix); writer.print("mPrompt=");
			writer.println(mPrompt);
			if (mOptions != null) {
				writer.print(prefix); writer.println("Options:");
				for (int i=0; i<mOptions.length; i++) {
					VoiceInteractor.PickOptionRequest.Option op = mOptions[i];
					writer.print(prefix); writer.print("  #"); writer.print(i); writer.println(":");
					writer.print(prefix); writer.print("    mLabel=");
					writer.println(op.getLabel());
					writer.print(prefix); writer.print("    mIndex=");
					writer.println(op.getIndex());
					if (op.countSynonyms() > 0) {
						writer.print(prefix); writer.println("    Synonyms:");
						for (int j=0; j<op.countSynonyms(); j++) {
							writer.print(prefix); writer.print("      #"); writer.print(j);
							writer.print(": "); writer.println(op.getSynonymAt(j));
						}
					}
					if (op.getExtras() != null) {
						writer.print(prefix); writer.print("    mExtras=");
						writer.println(op.getExtras());
					}
				}
			}
		}
	}

	public static final class CompleteVoiceRequest extends Request {
		final VoiceInteractor.Prompt mPrompt;

		CompleteVoiceRequest(String packageName, int uid, VoiceInteractionSession session,
		                     VoiceInteractor.Prompt prompt, Bundle extras) {
			super(packageName, uid, session, extras);
			mPrompt = prompt;
		}

		@Nullable
		public VoiceInteractor.Prompt getVoicePrompt() {
			return mPrompt;
		}

		@Deprecated
		@Nullable
		public CharSequence getMessage() {
			return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
		}

		public void sendCompleteResult(Bundle result) {
			if (DEBUG) Log.d(TAG, "sendCompleteVoiceResult: req=" + this + " result=" + result);
			finishRequest();
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			super.dump(prefix, fd, writer, args);
			writer.print(prefix); writer.print("mPrompt=");
			writer.println(mPrompt);
		}
	}

	public static final class AbortVoiceRequest extends Request {
		final VoiceInteractor.Prompt mPrompt;

		AbortVoiceRequest(String packageName, int uid, VoiceInteractionSession session,
		                  VoiceInteractor.Prompt prompt, Bundle extras) {
			super(packageName, uid, session, extras);
			mPrompt = prompt;
		}

		@Nullable
		public VoiceInteractor.Prompt getVoicePrompt() {
			return mPrompt;
		}

		@Deprecated
		@Nullable
		public CharSequence getMessage() {
			return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
		}

		public void sendAbortResult(Bundle result) {
			if (DEBUG) Log.d(TAG, "sendConfirmResult: req=" + this + " result=" + result);
			finishRequest();
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			super.dump(prefix, fd, writer, args);
			writer.print(prefix); writer.print("mPrompt=");
			writer.println(mPrompt);
		}
	}

	public static final class CommandRequest extends Request {
		final String mCommand;

		CommandRequest(String packageName, int uid, VoiceInteractionSession session, String command, Bundle extras) {
			super(packageName, uid, session, extras);
			mCommand = command;
		}

		public String getCommand() {
			return mCommand;
		}

		void sendCommandResult(boolean finished, Bundle result) {
			if (DEBUG) Log.d(TAG, "sendCommandResult: req=" + this + " result=" + result);
			if (finished) {
				finishRequest();
			}
		}

		public void sendIntermediateResult(Bundle result) {
			sendCommandResult(false, result);
		}

		public void sendResult(Bundle result) {
			sendCommandResult(true, result);
		}

		void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
			super.dump(prefix, fd, writer, args);
			writer.print(prefix); writer.print("mCommand=");
			writer.println(mCommand);
		}
	}

	/* AOSP VoiceInteractionSession.Insets; the TOUCHABLE_INSETS_* values are those of
	 * ViewTreeObserver.InternalInsetsInfo */
	public static final class Insets {
		public final Rect contentInsets = new Rect();

		public final Region touchableRegion = new Region();

		public static final int TOUCHABLE_INSETS_FRAME = 0;

		public static final int TOUCHABLE_INSETS_CONTENT = 1;

		public static final int TOUCHABLE_INSETS_REGION = 3;

		public int touchableInsets;

		public Insets() {}
	}

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

	void addRequest(Request req) {
		synchronized (this) {
			mActiveRequests.add(req);
		}
	}

	boolean isRequestActive(Request req) {
		synchronized (this) {
			return mActiveRequests.contains(req);
		}
	}

	Request removeRequest(Request req) {
		synchronized (this) {
			return mActiveRequests.remove(req) ? req : null;
		}
	}

	void doCreate(Object token) {
		mToken = token;
		onCreate();
	}

	void doShow(Bundle args, int flags) {
		if (mInShowWindow) {
			Log.w(TAG, "Re-entrance in to showWindow");
			return;
		}
		try {
			mInShowWindow = true;
			onPrepareShow(args, flags);
			if (!mWindowVisible) {
				ensureWindowAdded();
			}
			onShow(args, flags);
			if (!mWindowVisible) {
				mWindowVisible = true;
				if (mUiEnabled) {
					showWindow();
				}
			}
		} finally {
			mWindowWasVisible = true;
			mInShowWindow = false;
		}
	}

	void doHide() {
		if (mWindowVisible) {
			ensureWindowHidden();
			mWindowVisible = false;
			onHide();
		}
	}

	void doDestroy() {
		onDestroy();
		if (mInitialized) {
			if (mWindowAdded) {
				mWindow.dismiss();
				mWindowAdded = false;
			}
			mInitialized = false;
		}
	}

	/* AOSP doNotifyVisibleActivityInfoChanged */
	private void doNotifyVisibleActivityInfoChanged(VisibleActivityInfo visibleActivityInfo, int type) {
		if (mVisibleActivityCallbacks.isEmpty()) {
			return;
		}
		switch (type) {
		case VisibleActivityInfo.TYPE_ACTIVITY_ADDED:
			notifyVisibleActivityChanged(visibleActivityInfo, type);
			mVisibleActivityInfos.add(visibleActivityInfo);
			break;
		case VisibleActivityInfo.TYPE_ACTIVITY_REMOVED:
			notifyVisibleActivityChanged(visibleActivityInfo, type);
			mVisibleActivityInfos.remove(visibleActivityInfo);
			break;
		}
	}

	/* AOSP doRegisterVisibleActivityCallback, without the manager's startListeningVisibleActivityChanged */
	private void doRegisterVisibleActivityCallback(Executor executor, VisibleActivityCallback callback) {
		if (mVisibleActivityCallbacks.containsKey(callback)) {
			if (DEBUG) {
				Log.d(TAG, "doRegisterVisibleActivityCallback: callback has registered");
			}
			return;
		}
		int preCallbackCount = mVisibleActivityCallbacks.size();
		mVisibleActivityCallbacks.put(callback, executor);
		if (preCallbackCount != 0) {
			for (int i = 0; i < mVisibleActivityInfos.size(); i++) {
				final VisibleActivityInfo visibleActivityInfo = mVisibleActivityInfos.get(i);
				executor.execute(() -> callback.onVisible(visibleActivityInfo));
			}
		}
	}

	/* AOSP doUnregisterVisibleActivityCallback, without the manager's stopListeningVisibleActivityChanged */
	private void doUnregisterVisibleActivityCallback(VisibleActivityCallback callback) {
		mVisibleActivityCallbacks.remove(callback);
		if (mVisibleActivityCallbacks.size() == 0) {
			mVisibleActivityInfos.clear();
		}
	}

	private void notifyVisibleActivityChanged(VisibleActivityInfo visibleActivityInfo, int type) {
		for (Map.Entry<VisibleActivityCallback, Executor> e : mVisibleActivityCallbacks.entrySet()) {
			final Executor executor = e.getValue();
			final VisibleActivityCallback visibleActivityCallback = e.getKey();
			switch (type) {
			case VisibleActivityInfo.TYPE_ACTIVITY_ADDED:
				executor.execute(() -> visibleActivityCallback.onVisible(visibleActivityInfo));
				break;
			case VisibleActivityInfo.TYPE_ACTIVITY_REMOVED:
				executor.execute(() -> visibleActivityCallback.onInvisible(visibleActivityInfo.getActivityId()));
				break;
			}
		}
	}

	/* AOSP ensureWindowCreated: the content is a frame (AOSP inflates voice_interaction_session, whose
	 * android.R.id.content is a FrameLayout) in a Dialog of the session's theme */
	void ensureWindowCreated() {
		if (mInitialized) {
			return;
		}
		if (!mUiEnabled) {
			throw new IllegalStateException("setUiEnabled is false");
		}
		mInitialized = true;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mWindow = new VoiceInteractionWindow(mContext, mTheme, this);
		mContentFrame = new FrameLayout(mContext);
		mContentFrame.setId(android.R.id.content);
		mRootView = mContentFrame;
		mWindow.setContentView(mRootView);
		mWindow.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	void ensureWindowAdded() {
		if (mUiEnabled && !mWindowAdded) {
			mWindowAdded = true;
			ensureWindowCreated();
			View v = onCreateContentView();
			if (v != null) {
				setContentView(v);
			}
		}
	}

	void showWindow() {
		if (mWindow != null) {
			mWindow.show();
		}
	}

	void ensureWindowHidden() {
		if (mWindow != null) {
			mWindow.hide();
		}
	}

	/* AOSP VoiceInteractionSession.setDisabledShowContext: forwards to the manager, none here */
	public void setDisabledShowContext(int flags) {
	}

	/* AOSP: the manager's value, 0 when it cannot be reached */
	public int getDisabledShowContext() {
		return 0;
	}

	public int getUserDisabledShowContext() {
		return 0;
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

	public void setUiEnabled(boolean enabled) {
		if (mUiEnabled != enabled) {
			mUiEnabled = enabled;
			if (mWindowVisible) {
				if (enabled) {
					ensureWindowAdded();
					showWindow();
				} else {
					ensureWindowHidden();
				}
			}
		}
	}

	public void setTheme(int theme) {
		if (mWindow != null) {
			throw new IllegalStateException("Must be called before onCreate()");
		}
		mTheme = theme;
	}

	public void startVoiceActivity(Intent intent) {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	public void startAssistantActivity(Intent intent) {
		startAssistantActivity(intent, new Bundle());
	}

	public void startAssistantActivity(Intent intent, Bundle bundle) {
		Objects.requireNonNull(bundle);
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	/* AOSP requestDirectActions: the checks before the call to the manager */
	public final void requestDirectActions(ActivityId activityId, @Nullable CancellationSignal cancellationSignal,
	                                       Executor resultExecutor, Consumer<List<DirectAction>> callback) {
		Objects.requireNonNull(activityId);
		Objects.requireNonNull(resultExecutor);
		Objects.requireNonNull(callback);
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
		if (cancellationSignal != null) {
			cancellationSignal.throwIfCanceled();
		}
	}

	public void onDirectActionsInvalidated(ActivityId activityId) {
	}

	/* AOSP performDirectAction: the checks before the call to the manager */
	public final void performDirectAction(DirectAction action, @Nullable Bundle extras,
	                                      @Nullable CancellationSignal cancellationSignal, Executor resultExecutor,
	                                      Consumer<Bundle> resultListener) {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
		Objects.requireNonNull(resultExecutor);
		Objects.requireNonNull(resultListener);
		if (cancellationSignal != null) {
			cancellationSignal.throwIfCanceled();
		}
	}

	public void setKeepAwake(boolean keepAwake) {
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

	public LayoutInflater getLayoutInflater() {
		ensureWindowCreated();
		return mInflater;
	}

	public Dialog getWindow() {
		ensureWindowCreated();
		return mWindow;
	}

	/* AOSP VoiceInteractionSession.finish */
	public void finish() {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
	}

	public void onCreate() {
	}

	public void onPrepareShow(Bundle args, int showFlags) {
	}

	public void onShow(@Nullable Bundle args, int showFlags) {
	}

	public void onHide() {
	}

	public void onDestroy() {
	}

	public View onCreateContentView() {
		return null;
	}

	public void setContentView(View view) {
		ensureWindowCreated();
		mContentFrame.removeAllViews();
		mContentFrame.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
		                                                         ViewGroup.LayoutParams.MATCH_PARENT));
		mContentFrame.requestApplyInsets();
	}

	void doOnHandleAssist(int taskId, IBinder assistToken, Bundle data, AssistStructure structure, Throwable failure,
	                      AssistContent content, int index, int count) {
		if (failure != null) {
			onAssistStructureFailure(failure);
		}
		AssistState assistState = new AssistState(new ActivityId(taskId, assistToken), data, structure, content, index, count);
		onHandleAssist(assistState);
	}

	public void onAssistStructureFailure(Throwable failure) {
	}

	@Deprecated
	public void onHandleAssist(@Nullable Bundle data, @Nullable AssistStructure structure,
	                           @Nullable AssistContent content) {
	}

	public void onHandleAssist(AssistState state) {
		if (state.getAssistData() == null && state.getAssistStructure() == null && state.getAssistContent() == null) {
			return;
		} else if (state.getIndex() == 0) {
			onHandleAssist(state.getAssistData(), state.getAssistStructure(), state.getAssistContent());
		} else {
			onHandleAssistSecondary(state.getAssistData(), state.getAssistStructure(), state.getAssistContent(),
			                        state.getIndex(), state.getCount());
		}
	}

	@Deprecated
	public void onHandleAssistSecondary(@Nullable Bundle data, @Nullable AssistStructure structure,
	                                    @Nullable AssistContent content, int index, int count) {
	}

	public void onHandleScreenshot(@Nullable Bitmap screenshot) {
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

	public void onComputeInsets(Insets outInsets) {
		outInsets.contentInsets.left = 0;
		outInsets.contentInsets.bottom = 0;
		outInsets.contentInsets.right = 0;
		View decor = getWindow().getWindow().getDecorView();
		outInsets.contentInsets.top = decor.getHeight();
		outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME;
		outInsets.touchableRegion.setEmpty();
	}

	public void onTaskStarted(Intent intent, int taskId) {
	}

	public void onTaskFinished(Intent intent, int taskId) {
		hide();
	}

	public boolean[] onGetSupportedCommands(String[] commands) {
		return new boolean[commands.length];
	}

	public void onRequestConfirmation(ConfirmationRequest request) {
	}

	public void onRequestPickOption(PickOptionRequest request) {
	}

	public void onRequestCompleteVoice(CompleteVoiceRequest request) {
	}

	public void onRequestAbortVoice(AbortVoiceRequest request) {
	}

	public void onRequestCommand(CommandRequest request) {
	}

	public void onCancelRequest(Request request) {
	}

	public final void registerVisibleActivityCallback(Executor executor, VisibleActivityCallback callback) {
		if (mToken == null) {
			throw new IllegalStateException("Can't call before onCreate()");
		}
		Objects.requireNonNull(executor);
		Objects.requireNonNull(callback);
		mHandler.post(() -> doRegisterVisibleActivityCallback(executor, callback));
	}

	public final void unregisterVisibleActivityCallback(VisibleActivityCallback callback) {
		Objects.requireNonNull(callback);
		mHandler.post(() -> doUnregisterVisibleActivityCallback(callback));
	}

	public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
		writer.print(prefix); writer.print("mToken="); writer.println(mToken);
		writer.print(prefix); writer.print("mTheme=#"); writer.println(Integer.toHexString(mTheme));
		writer.print(prefix); writer.print("mUiEnabled="); writer.println(mUiEnabled);
		writer.print(" mInitialized="); writer.println(mInitialized);
		writer.print(prefix); writer.print("mWindowAdded="); writer.print(mWindowAdded);
		writer.print(" mWindowVisible="); writer.println(mWindowVisible);
		writer.print(prefix); writer.print("mWindowWasVisible="); writer.print(mWindowWasVisible);
		writer.print(" mInShowWindow="); writer.println(mInShowWindow);
		if (mActiveRequests.size() > 0) {
			writer.print(prefix); writer.println("Active requests:");
			String innerPrefix = prefix + "    ";
			int i = 0;
			for (Request req : mActiveRequests) {
				writer.print(prefix); writer.print("  #"); writer.print(i++);
				writer.print(": ");
				writer.println(req);
				req.dump(innerPrefix, fd, writer, args);
			}
		}
	}

	public interface VisibleActivityCallback {
		default void onVisible(VisibleActivityInfo activityInfo) {}

		default void onInvisible(ActivityId activityId) {}
	}

	public static final class AssistState {
		private final ActivityId mActivityId;
		private final int mIndex;
		private final int mCount;
		private final @Nullable Bundle mData;
		private final @Nullable AssistStructure mStructure;
		private final @Nullable AssistContent mContent;

		AssistState(ActivityId activityId, @Nullable Bundle data, @Nullable AssistStructure structure,
		            @Nullable AssistContent content, int index, int count) {
			mActivityId = activityId;
			mIndex = index;
			mCount = count;
			mData = data;
			mStructure = structure;
			mContent = content;
		}

		public boolean isFocused() {
			return mIndex == 0;
		}

		public int getIndex() {
			return mIndex;
		}

		public int getCount() {
			return mCount;
		}

		public ActivityId getActivityId() {
			return mActivityId;
		}

		@Nullable
		public Bundle getAssistData() {
			return mData;
		}

		@Nullable
		public AssistStructure getAssistStructure() {
			return mStructure;
		}

		@Nullable
		public AssistContent getAssistContent() {
			return mContent;
		}
	}

	public static class ActivityId {
		private final int mTaskId;
		private final IBinder mAssistToken;

		ActivityId(int taskId, IBinder assistToken) {
			mTaskId = taskId;
			mAssistToken = assistToken;
		}

		public int getTaskId() {
			return mTaskId;
		}

		public IBinder getAssistToken() {
			return mAssistToken;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ActivityId that = (ActivityId)o;
			if (mTaskId != that.mTaskId) {
				return false;
			}
			return mAssistToken != null ? mAssistToken.equals(that.mAssistToken) : that.mAssistToken == null;
		}

		@Override
		public int hashCode() {
			int result = mTaskId;
			result = 31 * result + (mAssistToken != null ? mAssistToken.hashCode() : 0);
			return result;
		}
	}
}
