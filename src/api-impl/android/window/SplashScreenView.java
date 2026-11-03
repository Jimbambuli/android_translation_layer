/*
 * Copyright (C) 2020 The Android Open Source Project
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
package android.window;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.time.Duration;
import java.time.Instant;

/**
 * <p>The view which allows an activity to customize its splash screen exit animation.</p>
 *
 * The public API of AOSP (android-14). ATL shows no system splash screen and hands none to the
 * app (Activity.getSplashScreen()); apps (androidx core-splashscreen) only test views for it.
 */
public final class SplashScreenView extends FrameLayout {
	private boolean mHasRemoved;
	private View mIconView;
	private View mBrandingImageView;
	private int mInitBackgroundColor;
	private Duration mIconAnimationDuration;
	private Instant mIconAnimationStart;

	public SplashScreenView(Context context) {
		super(context);
	}

	public SplashScreenView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	/**
	 * Returns the duration of the icon animation if icon is animatable.
	 */
	public Duration getIconAnimationDuration() {
		return mIconAnimationDuration;
	}

	/**
	 * If the replaced icon is animatable, return the animation start time based on system clock.
	 */
	public Instant getIconAnimationStart() {
		return mIconAnimationStart;
	}

	/**
	 * <p>Remove this view and release its resource. </p>
	 */
	public void remove() {
		if (mHasRemoved) {
			return;
		}
		setVisibility(GONE);
		if (getParent() instanceof ViewGroup)
			((ViewGroup)getParent()).removeView(this);
		mHasRemoved = true;
	}

	/**
	 * Get the view containing the Splash Screen icon and its background.
	 */
	public View getIconView() {
		return mIconView;
	}

	/**
	 * Get the branding image view.
	 * @hide
	 */
	public View getBrandingView() {
		return mBrandingImageView;
	}

	/**
	 * Get the initial background color of this view.
	 * @hide
	 */
	public int getInitBackgroundColor() {
		return mInitBackgroundColor;
	}
}
