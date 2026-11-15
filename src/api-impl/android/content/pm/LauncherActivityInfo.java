package android.content.pm;

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * A representation of an activity that can belong to this user or a managed
 * profile associated with this user. It can be used to query the label, icon
 * and badged icon for the activity.
 */
public class LauncherActivityInfo {
    private final PackageManager mPm;
    private final ActivityInfo mActivityInfo;
    private final UserHandle mUser;

    /* AOSP: UnicodeSet [[:White_Space:][:Default_Ignorable_Code_Point:][:gc=Cc:]]; Java has no regex class for
     * Default_Ignorable_Code_Point, so the code points are tested by the ranges Unicode lists for it (all Cf, the
     * variation selectors, the Hangul fillers and the other Mn/Lo entries) */
    private static boolean isInvisibleCodePoint(int cp) {
        if (Character.isWhitespace(cp) || Character.isSpaceChar(cp) || Character.getType(cp) == Character.CONTROL || Character.getType(cp) == Character.FORMAT)
            return true;
        return cp == 0x034F || cp == 0x115F || cp == 0x1160 || cp == 0x17B4 || cp == 0x17B5 || (cp >= 0x180B && cp <= 0x180D) || cp == 0x180F || cp == 0x3164 || (cp >= 0xFE00 && cp <= 0xFE0F) || cp == 0xFFA0 || (cp >= 0xE0100 && cp <= 0xE01EF) || cp == 0x0085;
    }

    // Only allow 3 consecutive invisible characters in the prefix of the string.
    private static final int PREFIX_CONSECUTIVE_INVISIBLE_CHARACTERS_MAXIMUM = 3;

    /**
     * Create a launchable activity object for a given ResolveInfo and user.
     *
     * @param context The context for fetching resources.

     */
    /* AOSP: LauncherActivityInfo(Context, LauncherActivityInfoInternal), the internal object holds the ActivityInfo
     * and the user */
    LauncherActivityInfo(Context context, ActivityInfo activityInfo, UserHandle user) {
        mPm = context.getPackageManager();
        mActivityInfo = activityInfo;
        mUser = user;
    }

    /**
     * Returns the component name of this activity.
     *
     * @return ComponentName of the activity
     */
    public ComponentName getComponentName() {
        return new ComponentName(mActivityInfo.packageName, mActivityInfo.name);
    }

    /**
     * Returns the user handle of the user profile that this activity belongs to. In order to
     * persist the identity of the profile, do not store the UserHandle. Instead retrieve its
     * serial number from UserManager. You can convert the serial number back to a UserHandle
     * for later use.
     *
     * @see UserManager#getSerialNumberForUser(UserHandle)
     * @see UserManager#getUserForSerialNumber(long)
     *
     * @return The UserHandle of the profile.
     */
    public UserHandle getUser() {
        return mUser;
    }

    /**
     * Retrieves the label for the activity. If the activity's label is invisible
     * for the user, use the application's label instead. If the application's label
     * is still invisible for the user, use the package name instead.
     *
     * @return The label for the activity. If the activity's label is invisible for the user,
     *         return the application's label instead. If the application's label
     *         is still invisible for the user, return the package name instead.
     */
    public CharSequence getLabel() {
        CharSequence label = getActivityInfo().loadLabel(mPm).toString().trim();
        // If the activity label is visible to the user, return the original activity label
        if (isVisible(label)) {
            return label;
        }

        // Use application label instead
        label = getApplicationInfo().loadLabel(mPm).toString().trim();
        // If the application label is visible to the user, return the original application label
        if (isVisible(label)) {
            return label;
        }

        // Use package name instead
        return getComponentName().getPackageName();
    }

    /**
     * @return Package loading progress, range between [0, 1].
     */
    public @FloatRange(from = 0.0, to = 1.0) float getLoadingProgress() {
        return 1.0f /* no incremental installs: IncrementalStatesInfo.getProgress() of a fully loaded app */;
    }

    /**
     * Returns the icon for this activity, without any badging for the profile.
     * @param density The preferred density of the icon, zero for default density. Use
     * density DPI values from {@link DisplayMetrics}.
     * @see #getBadgedIcon(int)
     * @see DisplayMetrics
     * @return The drawable associated with the activity.
     */
    public Drawable getIcon(int density) {
        // TODO: Go through LauncherAppsService
        final int iconRes = getActivityInfo().getIconResource();
        Drawable icon = null;
        // Get the preferred density icon from the app's resources
        if (density != 0 && iconRes != 0) {
            try {
                final Resources resources = mPm.getResourcesForApplication(
                        getActivityInfo().applicationInfo);
                icon = resources.getDrawableForDensity(iconRes, density);
            } catch (NameNotFoundException | Resources.NotFoundException exc) {
            }
        }
        // Get the default density icon
        if (icon == null) {
            icon = getActivityInfo().loadIcon(mPm);
        }
        return icon;
    }

    /**
     * Returns the application flags from the ApplicationInfo of the activity.
     *
     * @return Application flags
     * @hide remove before shipping
     */
    public int getApplicationFlags() {
        return getActivityInfo().flags;
    }

    /**
     * Returns the ActivityInfo of the activity.
     *
     * @return Activity Info
     */
    @NonNull
    public ActivityInfo getActivityInfo() {
        return mActivityInfo;
    }

    /**
     * Returns the application info for the application this activity belongs to.
     * @return
     */
    public ApplicationInfo getApplicationInfo() {
        return getActivityInfo().applicationInfo;
    }

    /**
     * Returns the time at which the package was first installed.
     *
     * @return The time of installation of the package, in milliseconds.
     */
    public long getFirstInstallTime() {
        try {
            // TODO: Go through LauncherAppsService
            return mPm.getPackageInfo(getActivityInfo().packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES /* = MATCH_UNINSTALLED_PACKAGES */).firstInstallTime;
        } catch (NameNotFoundException nnfe) {
            // Sorry, can't find package
            return 0;
        }
    }

    /**
     * Returns the name for the activity from  android:name in the manifest.
     * @return the name from android:name for the activity.
     */
    public String getName() {
        return getActivityInfo().name;
    }

    /**
     * Returns the activity icon with badging appropriate for the profile.
     * @param density Optional density for the icon, or 0 to use the default density. Use
     * {@link DisplayMetrics} for DPI values.
     * @see DisplayMetrics
     * @return A badged icon for the activity.
     */
    public Drawable getBadgedIcon(int density) {
        Drawable originalIcon = getIcon(density);

        return mPm.getUserBadgedIcon(originalIcon, mUser);
    }

    /**
     * Returns whether this activity supports (and can be launched in) multiple instances.
     * @hide
     */
    public boolean supportsMultiInstance() {
        /* LauncherActivityInfoInternal.supportsMultiInstance(): the manifest property PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI, which ATL does not parse per activity, so not declared */
        return false;
    }

    /**
     * Check whether the {@code sequence} is visible to the user or not.
     * <p>
     * Return {@code false} when one of these conditions are satisfied:
     * 1. The {@code sequence} starts with at least consecutive three invisible characters.
     * 2. The sequence is composed of the invisible characters and non-glyph characters.
     * <p>
     * Invisible character is one of the Default_Ignorable_Code_Point in
     * <a href="
     * https://www.unicode.org/Public/UCD/latest/ucd/DerivedCoreProperties.txt">
     * DerivedCoreProperties.txt</a>, the White_Space in <a href=
     * "https://www.unicode.org/Public/UCD/latest/ucd/PropList.txt">PropList.txt
     * </a> or category Cc.
     * <p>
     * Non-glyph character means the character is not supported in the current system font.
     * {@link android.graphics.Paint#hasGlyph(String)}
     * <p>
     *
     * @hide
     */
    @VisibleForTesting
    public static boolean isVisible(@NonNull CharSequence sequence) {
        Objects.requireNonNull(sequence);
        if (TextUtils.isEmpty(sequence)) {
            return false;
        }

        final Paint paint = new Paint();
        int invisibleCharCount = 0;
        int notSupportedCharCount = 0;
        final int[] codePoints = sequence.codePoints().toArray();
        for (int i = 0, length = codePoints.length; i < length; i++) {
            String ch = new String(new int[]{codePoints[i]}, /* offset= */ 0, /* count= */ 1);

            // The check steps:
            // 1. If the character is contained in INVISIBLE_CHARACTERS, invisibleCharCount++.
            //    1.1 Check whether the invisibleCharCount is larger or equal to
            //        PREFIX_INVISIBLE_CHARACTERS_MAXIMUM when notSupportedCharCount is zero.
            //        It means that there are three consecutive invisible characters at the
            //        start of the string, return false.
            //    Otherwise, continue.
            // 2. If the character is not supported on the system:
            //    notSupportedCharCount++, continue
            // 3. If it does not continue or return on the above two cases, it means the
            //    character is visible and supported on the system, break.
            // After going through the whole string, if the sum of invisibleCharCount
            // and notSupportedCharCount is smaller than the length of the string, it
            // means the string has the other visible characters, return true.
            // Otherwise, return false.
            if (isInvisibleCodePoint(codePoints[i])) {
                invisibleCharCount++;
                // If there are three successive invisible characters at the start of the
                // string, it is hard to visible to the user.
                if (notSupportedCharCount == 0
                        && invisibleCharCount >= PREFIX_CONSECUTIVE_INVISIBLE_CHARACTERS_MAXIMUM) {
                    return false;
                }
                continue;
            }

            // The character is not supported on the system, but it may not be an invisible
            // character. E.g. tofu (a rectangle).
            if (!paint.hasGlyph(ch)) {
                notSupportedCharCount++;
                continue;
            }
            // The character is visible and supported on the system, break the for loop
            break;
        }

        return (invisibleCharCount + notSupportedCharCount < codePoints.length);
    }
}
