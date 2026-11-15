/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.util;


import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;



import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * StatsLog provides an API for developers to send events to statsd. The events can be used to
 * define custom metrics inside statsd.
 */
// TODO(b/375040589) Can't use ravenwood annotations on public mainline APIs?
// for now we use the policy text file instead.
//@android.ravenwood.annotation.RavenwoodKeepWholeClass
public final class StatsLog {

    // AOSP loads stats_jni here; ATL has no statsd, see write()

    private static final String TAG = "StatsLog";
    private static final boolean DEBUG = false;

    /**
     * Annotation ID constant for logging UID field.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_IS_UID = 1;

    /**
     * Annotation ID constant to indicate logged atom event's timestamp should be truncated.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_TRUNCATE_TIMESTAMP = 2;

    /**
     * Annotation ID constant for a state atom's primary field.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_PRIMARY_FIELD = 3;

    /**
     * Annotation ID constant for state atom's state field.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_EXCLUSIVE_STATE = 4;

    /**
     * Annotation ID constant to indicate the first UID in the attribution chain
     * is a primary field.
     * Should only be used for attribution chain fields.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_PRIMARY_FIELD_FIRST_UID = 5;

    /**
     * Annotation ID constant to indicate which state is default for the state atom.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_DEFAULT_STATE = 6;

    /**
     * Annotation ID constant to signal all states should be reset to the default state.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_TRIGGER_STATE_RESET = 7;

    /**
     * Annotation ID constant to indicate state changes need to account for nesting.
     * This should only be used with binary state atoms.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_STATE_NESTED = 8;

    /**
     * Annotation ID constant to indicate the restriction category of an atom.
     * This annotation must only be attached to the atom id. This is an int annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_RESTRICTION_CATEGORY = 9;

    /**
     * Annotation ID to indicate that a field of an atom contains peripheral device info.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_PERIPHERAL_DEVICE_INFO = 10;

    /**
     * Annotation ID to indicate that a field of an atom contains app usage information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_APP_USAGE = 11;

    /**
     * Annotation ID to indicate that a field of an atom contains app activity information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_APP_ACTIVITY = 12;

    /**
     * Annotation ID to indicate that a field of an atom contains health connect information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_HEALTH_CONNECT = 13;

    /**
     * Annotation ID to indicate that a field of an atom contains accessibility information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_ACCESSIBILITY = 14;

    /**
     * Annotation ID to indicate that a field of an atom contains system search information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_SYSTEM_SEARCH = 15;

    /**
     * Annotation ID to indicate that a field of an atom contains user engagement information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_USER_ENGAGEMENT = 16;

    /**
     * Annotation ID to indicate that a field of an atom contains ambient sensing information.
     * This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_AMBIENT_SENSING = 17;

    /**
     * Annotation ID to indicate that a field of an atom contains demographic classification
     * information. This is a bool annotation.
     *
     * The ID is a byte since StatsEvent.addBooleanAnnotation() and StatsEvent.addIntAnnotation()
     * accept byte as the type for annotation ids to save space.
     *
     * @hide
     */
    @SystemApi
    public static final byte ANNOTATION_ID_FIELD_RESTRICTION_DEMOGRAPHIC_CLASSIFICATION = 18;


    /** @hide */
    @IntDef(prefix = { "RESTRICTION_CATEGORY_" }, value = {
            RESTRICTION_CATEGORY_DIAGNOSTIC,
            RESTRICTION_CATEGORY_SYSTEM_INTELLIGENCE,
            RESTRICTION_CATEGORY_AUTHENTICATION,
            RESTRICTION_CATEGORY_FRAUD_AND_ABUSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RestrictionCategory {}

    /**
     * Restriction category for atoms about diagnostics.
     *
     * @hide
     */
    @SystemApi
    public static final int RESTRICTION_CATEGORY_DIAGNOSTIC = 1;

    /**
     * Restriction category for atoms about system intelligence.
     *
     * @hide
     */
    @SystemApi
    public static final int RESTRICTION_CATEGORY_SYSTEM_INTELLIGENCE = 2;

    /**
     * Restriction category for atoms about authentication.
     *
     * @hide
     */
    @SystemApi
    public static final int RESTRICTION_CATEGORY_AUTHENTICATION = 3;

    /**
     * Restriction category for atoms about fraud and abuse.
     *
     * @hide
     */
    @SystemApi
    public static final int RESTRICTION_CATEGORY_FRAUD_AND_ABUSE = 4;

    private StatsLog() {
    }

    /**
     * Writes the StatsEvent to statsd.
     *
     * ATL has no statsd: AOSP's native writeImpl() writes the buffer to the statsd socket and ignores
     * a failing write, which is what a device without statsd does. The event is released as in AOSP.
     */
    public static void write(@NonNull final StatsEvent statsEvent) {
        statsEvent.release();
    }

    /** Deprecated raw write to statsd; nothing is listening (see write()). */
    @Deprecated
    @SystemApi
    public static void writeRaw(@NonNull byte[] buffer, int size) {}
}
