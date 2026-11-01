These files come from AOSP android-14 (frameworks/base/packages/SystemUI/monet/src/com/android/systemui/monet),
Apache License 2.0 (see the header of each file):

- the subpackages (contrast, dislike, dynamiccolor, hct, palettes, scheme, utils) are Material Color
  Utilities (https://github.com/material-foundation/material-color-utilities, Java), as AOSP copies them
  with the package com.android.systemui.monet; only the classes SchemeTonalSpot and the dynamic colors need
  are taken, unchanged except that the lambdas and method references in DynamicColor.java and
  MaterialDynamicColors.java are written as anonymous classes (dx, which builds api-impl.jar, cannot dex
  invokedynamic below min-sdk 26), with the imports this needs;
- Shades.java is unchanged except for the import of ColorInt (android.annotation instead of androidx);
- ColorScheme.java is ColorScheme.kt in Java, reduced to Style.TONAL_SPOT and a given seed color.

com/android/systemui/theme/DynamicColors.java is DynamicColors.kt of SystemUI in Java.
