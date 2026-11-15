/*
 * Copyright (C) 2023 The Android Open Source Project
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

/**
 * ScrollFeedbackProvider that performs haptic feedback (AOSP android-16.0.0_r4 HapticScrollFeedbackProvider).
 *
 * ATL: the android.view.flags rotary flag is taken as off (AOSP's path without disableRotaryScrollFeedback());
 * the feedback constants are AOSP's HapticFeedbackConstants values, which ATL has no class for. With AOSP's
 * default config (ViewConfiguration) haptic scroll feedback is off for every device, so no feedback is played.
 */
public class HapticScrollFeedbackProvider implements ScrollFeedbackProvider {
	private static final int TICK_INTERVAL_NO_TICK = 0;
	private static final boolean INITIAL_END_OF_LIST_HAPTICS_ENABLED = false;

	/* HapticFeedbackConstants.SCROLL_TICK, SCROLL_ITEM_FOCUS, SCROLL_LIMIT */
	private static final int SCROLL_TICK = 18;
	private static final int SCROLL_ITEM_FOCUS = 19;
	private static final int SCROLL_LIMIT = 20;

	private final View mView;
	private final ViewConfiguration mViewConfig;
	private final boolean mIsFromView;

	private int mDeviceId = -1;
	private int mAxis = -1;
	private int mSource = -1;

	private int mTickIntervalPixels = TICK_INTERVAL_NO_TICK;
	private int mTotalScrollPixels = 0;
	private boolean mCanPlayLimitFeedback = INITIAL_END_OF_LIST_HAPTICS_ENABLED;
	private boolean mHapticScrollFeedbackEnabled = false;

	public HapticScrollFeedbackProvider(View view) {
		this(view, ViewConfiguration.get(view.getContext()), /* isFromView= */ false);
	}

	/** @hide */
	public HapticScrollFeedbackProvider(View view, ViewConfiguration viewConfig, boolean isFromView) {
		mView = view;
		mViewConfig = viewConfig;
		mIsFromView = isFromView;
	}

	@Override
	public void onScrollProgress(int inputDeviceId, int source, int axis, int deltaInPixels) {
		maybeUpdateCurrentConfig(inputDeviceId, source, axis);
		if (!mHapticScrollFeedbackEnabled) {
			return;
		}
		if (deltaInPixels != 0) {
			mCanPlayLimitFeedback = true;
		}
		if (mTickIntervalPixels == TICK_INTERVAL_NO_TICK) {
			return;
		}
		mTotalScrollPixels += deltaInPixels;
		if (Math.abs(mTotalScrollPixels) >= mTickIntervalPixels) {
			mTotalScrollPixels %= mTickIntervalPixels;
			mView.performHapticFeedbackForInputDevice(SCROLL_TICK, inputDeviceId, source, /* flags= */ 0);
		}
	}

	@Override
	public void onScrollLimit(int inputDeviceId, int source, int axis, boolean isStart) {
		maybeUpdateCurrentConfig(inputDeviceId, source, axis);
		if (!mHapticScrollFeedbackEnabled) {
			return;
		}
		if (!mCanPlayLimitFeedback) {
			return;
		}
		mView.performHapticFeedbackForInputDevice(SCROLL_LIMIT, inputDeviceId, source, /* flags= */ 0);
		mCanPlayLimitFeedback = false;
	}

	@Override
	public void onSnapToItem(int inputDeviceId, int source, int axis) {
		maybeUpdateCurrentConfig(inputDeviceId, source, axis);
		if (!mHapticScrollFeedbackEnabled) {
			return;
		}
		mView.performHapticFeedbackForInputDevice(SCROLL_ITEM_FOCUS, inputDeviceId, source, /* flags= */ 0);
		mCanPlayLimitFeedback = true;
	}

	private void maybeUpdateCurrentConfig(int deviceId, int source, int axis) {
		if (mAxis != axis || mSource != source || mDeviceId != deviceId) {
			mSource = source;
			mAxis = axis;
			mDeviceId = deviceId;
			mHapticScrollFeedbackEnabled = mViewConfig.isHapticScrollFeedbackEnabled(deviceId, axis, source);
			mCanPlayLimitFeedback = INITIAL_END_OF_LIST_HAPTICS_ENABLED;
			mTotalScrollPixels = 0;
			updateTickIntervals(deviceId, source, axis);
		}
	}

	private void updateTickIntervals(int deviceId, int source, int axis) {
		mTickIntervalPixels = mHapticScrollFeedbackEnabled
		                          ? mViewConfig.getHapticScrollFeedbackTickInterval(deviceId, axis, source)
		                          : TICK_INTERVAL_NO_TICK;
	}
}
