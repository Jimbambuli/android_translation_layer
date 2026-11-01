/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.systemui.monet;

import android.annotation.ColorInt;
import android.graphics.Color;
import com.android.internal.graphics.cam.Cam;

/**
 * ColorScheme.kt of SystemUI (android-14) in Java, with the style ThemeOverlayController uses by
 * default (Style.TONAL_SPOT) and a seed color given directly (no WallpaperColors): the five tonal
 * palettes system_accent1..3 and system_neutral1..2 (shades 10..1000).
 */
public class ColorScheme {
	public static final int GOOGLE_BLUE = 0xFF1b6ef3;
	public static final int MIN_CHROMA = 5;
	public static final int[] SHADE_KEYS = {10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

	/* Style.TONAL_SPOT: TonalSpec(hue, chroma) of a1, a2, a3, n1, n2 as {HueAdd degrees, ChromaConstant};
	 * HueSource() is HueAdd(0.0) */
	private static final double[][] TONAL_SPOT = {
		{0.0, 36.0}, // a1 = TonalSpec(HueSource(), ChromaConstant(36.0))
		{0.0, 16.0}, // a2 = TonalSpec(HueSource(), ChromaConstant(16.0))
		{60.0, 24.0}, // a3 = TonalSpec(HueAdd(60.0), ChromaConstant(24.0))
		{0.0, 6.0}, // n1 = TonalSpec(HueSource(), ChromaConstant(6.0))
		{0.0, 8.0}, // n2 = TonalSpec(HueSource(), ChromaConstant(8.0))
	};

	public final int seed;
	public final boolean darkTheme;
	/* accent1, accent2, accent3, neutral1, neutral2: TonalPalette.allShades, in the order of SHADE_KEYS */
	private final int[][] palettes = new int[5][];

	public ColorScheme(@ColorInt int seed, boolean darkTheme) {
		this.seed = seed;
		this.darkTheme = darkTheme;
		Cam proposedSeedCam = Cam.fromInt(seed);
		int seedArgb;
		if (seed == Color.TRANSPARENT) {
			seedArgb = GOOGLE_BLUE;
		} else if (proposedSeedCam.getChroma() < 5) {
			seedArgb = GOOGLE_BLUE;
		} else {
			seedArgb = seed;
		}
		/* TonalPalette(spec, seedArgb): TonalSpec.shades(seedCam) */
		Cam seedCam = Cam.fromInt(seedArgb);
		for (int i = 0; i < palettes.length; i++) {
			double hue = wrapDegreesDouble(seedCam.getHue() + TONAL_SPOT[i][0]);
			double chroma = TONAL_SPOT[i][1];
			palettes[i] = Shades.of((float)hue, (float)chroma);
		}
	}

	public int[] getAccent1() { return palettes[0]; }
	public int[] getAccent2() { return palettes[1]; }
	public int[] getAccent3() { return palettes[2]; }
	public int[] getNeutral1() { return palettes[3]; }
	public int[] getNeutral2() { return palettes[4]; }

	public static double wrapDegreesDouble(double degrees) {
		if (degrees < 0) {
			return (degrees % 360) + 360;
		} else if (degrees >= 360) {
			return degrees % 360;
		} else {
			return degrees;
		}
	}
}
