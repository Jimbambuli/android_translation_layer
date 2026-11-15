/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Describes the device effects of a zen mode. AOSP core/java/android/service/notification/ZenDeviceEffects.java
 * (android-16). Only the four effects of the public API are kept (grayscale, ambient display, wallpaper dimming,
 * night mode); the hidden ones (auto brightness, tap/tilt to wake, touch, radio, doze, night light, brightness
 * cap, extra effects) are left out.
 */
public final class ZenDeviceEffects implements Parcelable {
	public static final int FIELD_GRAYSCALE = 1 << 0;
	public static final int FIELD_SUPPRESS_AMBIENT_DISPLAY = 1 << 1;
	public static final int FIELD_DIM_WALLPAPER = 1 << 2;
	public static final int FIELD_NIGHT_MODE = 1 << 3;

	private final boolean mGrayscale;
	private final boolean mSuppressAmbientDisplay;
	private final boolean mDimWallpaper;
	private final boolean mNightMode;

	private ZenDeviceEffects(boolean grayscale, boolean suppressAmbientDisplay, boolean dimWallpaper,
	                         boolean nightMode) {
		mGrayscale = grayscale;
		mSuppressAmbientDisplay = suppressAmbientDisplay;
		mDimWallpaper = dimWallpaper;
		mNightMode = nightMode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ZenDeviceEffects)) return false;
		if (obj == this) return true;
		final ZenDeviceEffects that = (ZenDeviceEffects)obj;
		return this.mGrayscale == that.mGrayscale && this.mSuppressAmbientDisplay == that.mSuppressAmbientDisplay
		       && this.mDimWallpaper == that.mDimWallpaper && this.mNightMode == that.mNightMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mGrayscale, mSuppressAmbientDisplay, mDimWallpaper, mNightMode);
	}

	@Override
	public String toString() {
		java.util.ArrayList<String> effects = new java.util.ArrayList<>(4);
		if (mGrayscale) effects.add("grayscale");
		if (mSuppressAmbientDisplay) effects.add("suppressAmbientDisplay");
		if (mDimWallpaper) effects.add("dimWallpaper");
		if (mNightMode) effects.add("nightMode");
		return "[" + String.join(", ", effects) + "]";
	}

	public static String fieldsToString(int bitmask) {
		java.util.ArrayList<String> modified = new java.util.ArrayList<>();
		if ((bitmask & FIELD_GRAYSCALE) != 0) modified.add("FIELD_GRAYSCALE");
		if ((bitmask & FIELD_SUPPRESS_AMBIENT_DISPLAY) != 0) modified.add("FIELD_SUPPRESS_AMBIENT_DISPLAY");
		if ((bitmask & FIELD_DIM_WALLPAPER) != 0) modified.add("FIELD_DIM_WALLPAPER");
		if ((bitmask & FIELD_NIGHT_MODE) != 0) modified.add("FIELD_NIGHT_MODE");
		return "{" + String.join(",", modified) + "}";
	}

	public boolean shouldDisplayGrayscale() {
		return mGrayscale;
	}

	public boolean shouldSuppressAmbientDisplay() {
		return mSuppressAmbientDisplay;
	}

	public boolean shouldDimWallpaper() {
		return mDimWallpaper;
	}

	public boolean shouldUseNightMode() {
		return mNightMode;
	}

	public boolean hasEffects() {
		return mGrayscale || mSuppressAmbientDisplay || mDimWallpaper || mNightMode;
	}

	public static final Creator<ZenDeviceEffects> CREATOR = new Creator<ZenDeviceEffects>() {
		@Override
		public ZenDeviceEffects createFromParcel(Parcel in) {
			return new ZenDeviceEffects(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
		}

		@Override
		public ZenDeviceEffects[] newArray(int size) {
			return new ZenDeviceEffects[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeBoolean(mGrayscale);
		dest.writeBoolean(mSuppressAmbientDisplay);
		dest.writeBoolean(mDimWallpaper);
		dest.writeBoolean(mNightMode);
	}

	public static final class Builder {
		private boolean mGrayscale;
		private boolean mSuppressAmbientDisplay;
		private boolean mDimWallpaper;
		private boolean mNightMode;

		public Builder() {}

		public Builder(ZenDeviceEffects zenDeviceEffects) {
			mGrayscale = zenDeviceEffects.shouldDisplayGrayscale();
			mSuppressAmbientDisplay = zenDeviceEffects.shouldSuppressAmbientDisplay();
			mDimWallpaper = zenDeviceEffects.shouldDimWallpaper();
			mNightMode = zenDeviceEffects.shouldUseNightMode();
		}

		public Builder setShouldDisplayGrayscale(boolean grayscale) {
			mGrayscale = grayscale;
			return this;
		}

		public Builder setShouldSuppressAmbientDisplay(boolean suppressAmbientDisplay) {
			mSuppressAmbientDisplay = suppressAmbientDisplay;
			return this;
		}

		public Builder setShouldDimWallpaper(boolean dimWallpaper) {
			mDimWallpaper = dimWallpaper;
			return this;
		}

		public Builder setShouldUseNightMode(boolean nightMode) {
			mNightMode = nightMode;
			return this;
		}

		public ZenDeviceEffects build() {
			return new ZenDeviceEffects(mGrayscale, mSuppressAmbientDisplay, mDimWallpaper, mNightMode);
		}
	}
}
