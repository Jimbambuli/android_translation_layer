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

package android.service.wallpaper;

import android.app.Service;
import android.app.WallpaperColors;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceHolder;

/**
 * A wallpaper service is responsible for showing a live wallpaper behind applications that would like
 * to sit on top of it.
 * AOSP core/java/android/service/wallpaper/WallpaperService.java. In AOSP the window manager binds the
 * service through IWallpaperService, which creates an IWallpaperEngineWrapper (window, surface, input) per
 * Engine; ATL has no wallpaper host, so nothing binds and every Engine stays unattached, as AOSP's
 * before onCreate(SurfaceHolder). The window related Engine members (surface, offsets, desired size,
 * ambient mode, local colors, ...) are not ported.
 */
public abstract class WallpaperService extends Service {

	public static final String SERVICE_INTERFACE = "android.service.wallpaper.WallpaperService";

	public static final String SERVICE_META_DATA = "android.service.wallpaper";

	public class Engine {

		private boolean mDestroyed;

		public Engine() {
		}

		/* AOSP Engine.getSurfaceHolder: its BaseSurfaceHolder only gets a surface when attached */
		public SurfaceHolder getSurfaceHolder() {
			return null;
		}

		/* AOSP Engine.isVisible: mReportedVisible, set only by the window manager's visibility report */
		public boolean isVisible() {
			return false;
		}

		/* AOSP Engine.isPreview: mIWallpaperEngine.mIsPreview, not attached here, so no preview */
		public boolean isPreview() {
			return false;
		}

		/* AOSP Engine.setTouchEventsEnabled: only changes the window flags, applied once created */
		public void setTouchEventsEnabled(boolean enabled) {
		}

		public void onCreate(SurfaceHolder surfaceHolder) {
		}

		public void onDestroy() {
		}

		public void onVisibilityChanged(boolean visible) {
		}

		public void onOffsetsChanged(float xOffset, float yOffset,
		                             float xOffsetStep, float yOffsetStep,
		                             int xPixelOffset, int yPixelOffset) {
		}

		public Bundle onCommand(String action, int x, int y, int z,
		                        Bundle extras, boolean resultRequested) {
			return null;
		}

		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		public void onSurfaceCreated(SurfaceHolder holder) {
		}

		public void onSurfaceDestroyed(SurfaceHolder holder) {
		}

		public WallpaperColors onComputeColors() {
			return null;
		}

		/* AOSP Engine.notifyColorsChanged: reports onComputeColors() to the IWallpaperConnection, which
		 * does not exist here (nothing attached), so there is nobody to tell */
		public void notifyColorsChanged() {
			if (mDestroyed) {
				return;
			}
		}
	}

	public abstract Engine onCreateEngine();

	@Override
	public final IBinder onBind(Intent intent) {
		/* kein IWallpaperService-Stub, niemand bindet */
		return null;
	}
}
