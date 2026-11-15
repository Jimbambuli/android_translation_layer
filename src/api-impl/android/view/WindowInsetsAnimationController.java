/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.graphics.Insets;
import android.view.WindowInsets.Type.InsetsType;

/**
 * Controller for app-driven animation of system windows (AOSP android-16.0.0_r4 WindowInsetsAnimationController,
 * public part). ATL's InsetsController has no controllable insets and never hands one out.
 */
public interface WindowInsetsAnimationController {
	Insets getHiddenStateInsets();

	Insets getShownStateInsets();

	Insets getCurrentInsets();

	float getCurrentFraction();

	float getCurrentAlpha();

	@InsetsType int getTypes();

	void setInsetsAndAlpha(Insets insets, float alpha, float fraction);

	void finish(boolean shown);

	default boolean isReady() {
		return !isFinished() && !isCancelled();
	}

	boolean isFinished();

	boolean isCancelled();
}
