package android.atl;

import android.app.ActivityThread;
import android.app.UiModeManager;
import android.atl.annotation.Export;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Pair;
import com.android.internal.graphics.ColorUtils;
import com.android.systemui.monet.ColorScheme;
import com.android.systemui.monet.dynamiccolor.DynamicColor;
import com.android.systemui.monet.hct.Hct;
import com.android.systemui.monet.scheme.DynamicScheme;
import com.android.systemui.monet.scheme.SchemeTonalSpot;
import com.android.systemui.theme.DynamicColors;
import java.util.ArrayList;

/**
 * The Material You colors for the accent color of the desktop (accent-color of the XDG settings portal),
 * made like SystemUI's ThemeOverlayController makes them for the system palette color of the user
 * (createOverlays(), style TONAL_SPOT): system_accent1..3 and system_neutral1..2 from ColorScheme,
 * and the dynamic colors of API 34 from DynamicColors with SchemeTonalSpot for both light and dark, at
 * the contrast of UiModeManager.getContrast(). On Android they are fabricated overlays of the android
 * package; ATL has no overlays, so the AssetManager returns these values for the overlaid resources
 * (AssetManager.setOverlayColors()). Without an accent color there is no overlay and the colors of
 * framework-res stay, as on Android without a theme overlay.
 */
public final class ATLThemeOverlay {
	/* the accent color as ARGB, 0 for none; set by native code before Context is initialized */
	static int sAccentColor;

	private final ArrayList<Integer> ids = new ArrayList<>();
	private final ArrayList<Integer> colors = new ArrayList<>();
	private final Resources system = Resources.getSystem();

	/* FabricatedOverlay.setResourceValue("android:color/" + name, TYPE_INT_COLOR_ARGB8, color) */
	private void setResourceValue(String name, int color) {
		int id = system.getIdentifier(name, "color", "android");
		if (id != 0) {
			ids.add(id);
			colors.add(color);
		}
	}

	/* ThemeOverlayController.assignTonalPaletteToOverlay() */
	private void assignTonalPaletteToOverlay(String name, int[] shades) {
		for (int i = 0; i < shades.length; i++)
			setResourceValue("system_" + name + "_" + ColorScheme.SHADE_KEYS[i], ColorUtils.setAlphaComponent(shades[i], 0xFF));
	}

	/* ThemeOverlayController.assignDynamicPaletteToOverlay() */
	private void assignDynamicPaletteToOverlay(DynamicScheme scheme, boolean isDark) {
		String suffix = isDark ? "dark" : "light";
		for (Pair<String, DynamicColor> p : DynamicColors.ALL_DYNAMIC_COLORS_MAPPED)
			setResourceValue("system_" + p.first + "_" + suffix, p.second.getArgb(scheme));
	}

	/* ThemeOverlayController.assignFixedColorsToOverlay() */
	private void assignFixedColorsToOverlay(DynamicScheme lightScheme) {
		for (Pair<String, DynamicColor> p : DynamicColors.FIXED_COLORS_MAPPED)
			setResourceValue("system_" + p.first, p.second.getArgb(lightScheme));
	}

	/* ThemeOverlayController.createOverlays(), applied right away */
	private void createOverlays(int color, boolean nightMode, float contrast) {
		ColorScheme colorScheme = new ColorScheme(color, nightMode);
		assignTonalPaletteToOverlay("neutral1", colorScheme.getNeutral1());
		assignTonalPaletteToOverlay("neutral2", colorScheme.getNeutral2());
		assignTonalPaletteToOverlay("accent1", colorScheme.getAccent1());
		assignTonalPaletteToOverlay("accent2", colorScheme.getAccent2());
		assignTonalPaletteToOverlay("accent3", colorScheme.getAccent3());
		DynamicScheme dark = new SchemeTonalSpot(Hct.fromInt(color), true, contrast);
		DynamicScheme light = new SchemeTonalSpot(Hct.fromInt(color), false, contrast);
		assignDynamicPaletteToOverlay(dark, true);
		assignDynamicPaletteToOverlay(light, false);
		assignFixedColorsToOverlay(light);
	}

	/* makes the overlay for the current accent color and contrast, or removes it */
	public static void apply() {
		int[] idArray = null;
		int[] colorArray = null;
		if (sAccentColor != 0) {
			ATLThemeOverlay overlay = new ATLThemeOverlay();
			boolean nightMode = (Context.sys_config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
			overlay.createOverlays(sAccentColor, nightMode, new UiModeManager().getContrast());
			idArray = new int[overlay.ids.size()];
			colorArray = new int[idArray.length];
			for (int i = 0; i < idArray.length; i++) {
				idArray[i] = overlay.ids.get(i);
				colorArray[i] = overlay.colors.get(i);
			}
		}
		AssetManager.setOverlayColors(idArray, colorArray);
	}

	/* The accent color or the contrast of the desktop changed while the app runs: the overlay changes
	 * and, like ActivityTaskManagerService.updateAssetConfiguration() after an overlay change, the
	 * global configuration gets a new assetsSeq (CONFIG_ASSETS_PATHS, which relaunches the activities). */
	@Export
	static void setAccentColor(int argb) {
		sAccentColor = argb;
		apply();
		Configuration config = new Configuration(Context.sys_config);
		config.assetsSeq = Context.sys_config.assetsSeq + 1;
		ActivityThread.handleConfigurationChanged(config);
	}
}
