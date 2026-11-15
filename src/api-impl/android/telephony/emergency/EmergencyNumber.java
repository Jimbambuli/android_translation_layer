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

package android.telephony.emergency;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;

import android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A parcelable class that wraps and retrieves the information of number, service category(s) and
 * country code for a specific emergency number.
 */
public final class EmergencyNumber implements Parcelable, Comparable<EmergencyNumber> {

    private static final String LOG_TAG = "EmergencyNumber";

    /**
     * Defining Emergency Service Category as follows:
     *  - General emergency call, all categories;
     *  - Police;
     *  - Ambulance;
     *  - Fire Brigade;
     *  - Marine Guard;
     *  - Mountain Rescue;
     *  - Manually Initiated eCall (MIeC);
     *  - Automatically Initiated eCall (AIeC);
     *
     * Category UNSPECIFIED (General emergency call, all categories) indicates that no specific
     * services are associated with this emergency number; if the emergency number is specified,
     * it has one or more defined emergency service categories.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     *
     * @hide
     */
    @IntDef(flag = true, prefix = { "EMERGENCY_SERVICE_CATEGORY_" }, value = {
            EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
            EMERGENCY_SERVICE_CATEGORY_POLICE,
            EMERGENCY_SERVICE_CATEGORY_AMBULANCE,
            EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE,
            EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD,
            EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE,
            EMERGENCY_SERVICE_CATEGORY_MIEC,
            EMERGENCY_SERVICE_CATEGORY_AIEC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmergencyServiceCategories {}

    /**
     * Emergency Service Category UNSPECIFIED (General emergency call, all categories) bit-field
     * indicates that no specific services are associated with this emergency number; if the
     * emergency number is specified, it has one or more defined emergency service categories.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED =
            0 /* EmergencyServiceCategory.UNSPECIFIED */;
    /**
     * Bit-field that indicates Emergency Service Category for Police.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_POLICE = 1 /* EmergencyServiceCategory.POLICE */;
    /**
     * Bit-field that indicates Emergency Service Category for Ambulance.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_AMBULANCE =
            2 /* EmergencyServiceCategory.AMBULANCE */;
    /**
     * Bit-field that indicates Emergency Service Category for Fire Brigade.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE =
            4 /* EmergencyServiceCategory.FIRE_BRIGADE */;
    /**
     * Bit-field that indicates Emergency Service Category for Marine Guard.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD =
            8 /* EmergencyServiceCategory.MARINE_GUARD */;
    /**
     * Bit-field that indicates Emergency Service Category for Mountain Rescue.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE =
            16 /* EmergencyServiceCategory.MOUNTAIN_RESCUE */;
    /**
     * Bit-field that indicates Emergency Service Category for Manually Initiated eCall (MIeC)
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_MIEC = 32 /* EmergencyServiceCategory.MIEC */;
    /**
     * Bit-field that indicates Emergency Service Category for Automatically Initiated eCall (AIeC)
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_SERVICE_CATEGORY_AIEC = 64 /* EmergencyServiceCategory.AIEC */;

    private static final Set<Integer> EMERGENCY_SERVICE_CATEGORY_SET;
    static {
        EMERGENCY_SERVICE_CATEGORY_SET = new HashSet<Integer>();
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_POLICE);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_AMBULANCE);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_MIEC);
        EMERGENCY_SERVICE_CATEGORY_SET.add(EMERGENCY_SERVICE_CATEGORY_AIEC);
    }

    /**
     * The source to tell where the corresponding @1.4::EmergencyNumber comes from.
     *
     * The emergency number has one or more defined emergency number sources.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     *
     * @hide
     */
    @IntDef(flag = true, prefix = { "EMERGENCY_NUMBER_SOURCE_" }, value = {
            EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING,
            EMERGENCY_NUMBER_SOURCE_SIM,
            EMERGENCY_NUMBER_SOURCE_DATABASE,
            EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG,
            EMERGENCY_NUMBER_SOURCE_DEFAULT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmergencyNumberSources {}

    /**
     * Bit-field which indicates the number is from the network signaling.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING = 1; /* EmergencyNumber */
    /**
     * Bit-field which indicates the number is from the sim.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_NUMBER_SOURCE_SIM = 2; /* EmergencyNumber */
    /**
     * Bit-field which indicates the number is from the platform-maintained database.
     */
    public static final int EMERGENCY_NUMBER_SOURCE_DATABASE =  1 << 4;
    /**
     * Bit-field which indicates the number is from test mode.
     *
     * @hide
     */
    @TestApi
    public static final int EMERGENCY_NUMBER_SOURCE_TEST =  1 << 5;
    /** Bit-field which indicates the number is from the modem config. */
    public static final int EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG = 4; /* EmergencyNumber */
    /**
     * Bit-field which indicates the number is available as default.
     *
     * 112, 911 must always be available; additionally, 000, 08, 110, 999, 118 and 119 must be
     * available when sim is not present.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    public static final int EMERGENCY_NUMBER_SOURCE_DEFAULT = 8; /* EmergencyNumber */

    private static final Set<Integer> EMERGENCY_NUMBER_SOURCE_SET;
    static {
        EMERGENCY_NUMBER_SOURCE_SET = new HashSet<Integer>();
        EMERGENCY_NUMBER_SOURCE_SET.add(EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING);
        EMERGENCY_NUMBER_SOURCE_SET.add(EMERGENCY_NUMBER_SOURCE_SIM);
        EMERGENCY_NUMBER_SOURCE_SET.add(EMERGENCY_NUMBER_SOURCE_DATABASE);
        EMERGENCY_NUMBER_SOURCE_SET.add(EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG);
        EMERGENCY_NUMBER_SOURCE_SET.add(EMERGENCY_NUMBER_SOURCE_DEFAULT);
    }

    /**
     * Indicated the framework does not know whether an emergency call should be placed using
     * emergency or normal call routing. This means the underlying radio or IMS implementation is
     * free to determine for itself how to route the call.
     */
    public static final int EMERGENCY_CALL_ROUTING_UNKNOWN = 0;
    /**
     * Indicates the radio or IMS implementation must handle the call through emergency routing.
     */
    public static final int EMERGENCY_CALL_ROUTING_EMERGENCY = 1;
    /**
     * Indicates the radio or IMS implementation must handle the call through normal call routing.
     */
    public static final int EMERGENCY_CALL_ROUTING_NORMAL = 2;

    /**
     * The routing to tell how to handle the call for the corresponding emergency number.
     *
     * @hide
     */
    @IntDef(flag = false, prefix = { "EMERGENCY_CALL_ROUTING_" }, value = {
            EMERGENCY_CALL_ROUTING_UNKNOWN,
            EMERGENCY_CALL_ROUTING_EMERGENCY,
            EMERGENCY_CALL_ROUTING_NORMAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmergencyCallRouting {}


    private final String mNumber;
    private final String mCountryIso;
    private final String mMnc;
    private final int mEmergencyServiceCategoryBitmask;
    private final List<String> mEmergencyUrns;
    private final int mEmergencyNumberSourceBitmask;
    private final int mEmergencyCallRouting;
    /**
     * The source of the EmergencyNumber in the order of precedence.
     */
    private static final int[] EMERGENCY_NUMBER_SOURCE_PRECEDENCE;
    static {
        EMERGENCY_NUMBER_SOURCE_PRECEDENCE = new int[4];
        EMERGENCY_NUMBER_SOURCE_PRECEDENCE[0] = EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING;
        EMERGENCY_NUMBER_SOURCE_PRECEDENCE[1] = EMERGENCY_NUMBER_SOURCE_SIM;
        EMERGENCY_NUMBER_SOURCE_PRECEDENCE[2] = EMERGENCY_NUMBER_SOURCE_DATABASE;
        EMERGENCY_NUMBER_SOURCE_PRECEDENCE[3] = EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG;
    }

    /** @hide */
    public EmergencyNumber(@NonNull String number, @NonNull String countryIso, @NonNull String mnc,
                           @EmergencyServiceCategories int emergencyServiceCategories,
                           @NonNull List<String> emergencyUrns,
                           @EmergencyNumberSources int emergencyNumberSources,
                           @EmergencyCallRouting int emergencyCallRouting) {
        this.mNumber = number;
        this.mCountryIso = countryIso;
        this.mMnc = mnc;
        this.mEmergencyServiceCategoryBitmask = emergencyServiceCategories;
        this.mEmergencyUrns = emergencyUrns;
        this.mEmergencyNumberSourceBitmask = emergencyNumberSources;
        this.mEmergencyCallRouting = emergencyCallRouting;
    }

    /** @hide */
    public EmergencyNumber(Parcel source) {
        mNumber = source.readString();
        mCountryIso = source.readString();
        mMnc = source.readString();
        mEmergencyServiceCategoryBitmask = source.readInt();
        mEmergencyUrns = source.createStringArrayList();
        mEmergencyNumberSourceBitmask = source.readInt();
        mEmergencyCallRouting = source.readInt();
    }

    @Override
    /** @hide */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNumber);
        dest.writeString(mCountryIso);
        dest.writeString(mMnc);
        dest.writeInt(mEmergencyServiceCategoryBitmask);
        dest.writeStringList(mEmergencyUrns);
        dest.writeInt(mEmergencyNumberSourceBitmask);
        dest.writeInt(mEmergencyCallRouting);
    }

    public static final @NonNull Creator<EmergencyNumber> CREATOR =
            new Creator<EmergencyNumber>() {
                @Override
                public EmergencyNumber createFromParcel(Parcel in) {
                    return new EmergencyNumber(in);
                }

                @Override
                public EmergencyNumber[] newArray(int size) {
                    return new EmergencyNumber[size];
                }
            };

    /**
     * Get the dialing number of the emergency number.
     *
     * The character in the number string is only the dial pad
     * character('0'-'9', '*', '+', or '#'). For example: 911.
     *
     * If the number starts with carrier prefix, the carrier prefix is configured in
     * {@link CarrierConfigManager#KEY_EMERGENCY_NUMBER_PREFIX_STRING_ARRAY}.
     *
     * @return the dialing number.
     */
    public @NonNull String getNumber() {
        return mNumber;
    }

    /**
     * Get the country code string (lowercase character) in ISO 3166 format of the emergency number.
     *
     * @return the country code string (lowercase character) in ISO 3166 format.
     */
    public @NonNull String getCountryIso() {
        return mCountryIso;
    }

    /**
     * Get the Mobile Network Code of the emergency number.
     *
     * @return the Mobile Network Code of the emergency number.
     */
    public @NonNull String getMnc() {
        return mMnc;
    }

    /**
     * Returns the bitmask of emergency service categories of the emergency number.
     *
     * @return bitmask of the emergency service categories
     *
     * @hide
     */
    public @EmergencyServiceCategories int getEmergencyServiceCategoryBitmask() {
        return mEmergencyServiceCategoryBitmask;
    }

    

    /**
     * Returns the emergency service categories of the emergency number.
     *
     * Note: if the emergency number is in {@link #EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED}, only
     * {@link #EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED} is returned and it means the number is in
     * all categories.
     *
     * @return a list of the emergency service categories
     */
    public @NonNull List<Integer> getEmergencyServiceCategories() {
        List<Integer> categories = new ArrayList<>();
        if (serviceUnspecified()) {
            categories.add(EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED);
            return categories;
        }
        for (Integer category : EMERGENCY_SERVICE_CATEGORY_SET) {
            if (isInEmergencyServiceCategories(category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    /**
     * Returns the list of emergency Uniform Resources Names (URN) of the emergency number.
     *
     * For example, {@code urn:service:sos} is the generic URN for contacting emergency services
     * of all type.
     *
     * Reference: 3gpp 24.503, Section 5.1.6.8.1 - General;
     *            RFC 5031
     *
     * @return list of emergency Uniform Resources Names (URN) or an empty list if the emergency
     *         number does not have a specified emergency Uniform Resource Name.
     */
    public @NonNull List<String> getEmergencyUrns() {
        return Collections.unmodifiableList(mEmergencyUrns);
    }

    /**
     * Checks if the emergency service category is unspecified for the emergency number
     * {@link #EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED}.
     *
     * @return {@code true} if the emergency service category is unspecified for the emergency
     * number {@link #EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED}; {@code false} otherwise.
     */
    private boolean serviceUnspecified() {
        return mEmergencyServiceCategoryBitmask == EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED;
    }

    /**
     * Checks if the emergency number is in the supplied emergency service category(s).
     *
     * @param categories - the supplied emergency service categories
     *
     * @return {@code true} if the emergency number is in the specified emergency service
     * category(s) or if its emergency service category is
     * {@link #EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED}; {@code false} otherwise.
     */
    public boolean isInEmergencyServiceCategories(@EmergencyServiceCategories int categories) {
        if (categories == EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED) {
            return serviceUnspecified();
        }
        if (serviceUnspecified()) {
            return true;
        }
        return (mEmergencyServiceCategoryBitmask & categories) == categories;
    }

    

    /**
     * Returns a list of sources of the emergency number.
     *
     * @return a list of emergency number sources
     */
    public @NonNull List<Integer> getEmergencyNumberSources() {
        List<Integer> sources = new ArrayList<>();
        for (Integer source : EMERGENCY_NUMBER_SOURCE_SET) {
            if ((mEmergencyNumberSourceBitmask & source) == source) {
                sources.add(source);
            }
        }
        return sources;
    }

    /**
     * Checks if the emergency number is from the specified emergency number source(s).
     *
     * @return {@code true} if the emergency number is from the specified emergency number
     * source(s); {@code false} otherwise.
     *
     * @param sources - the supplied emergency number sources
     */
    public boolean isFromSources(@EmergencyNumberSources int sources) {
        return (mEmergencyNumberSourceBitmask & sources) == sources;
    }

    /**
     * Returns the emergency call routing information.
     *
     * <p>Some regions require some emergency numbers which are not routed using typical emergency
     * call processing, but are instead placed as regular phone calls. The emergency call routing
     * field provides information about how an emergency call will be routed when it is placed.
     *
     * @return the emergency call routing requirement
     */
    public @EmergencyCallRouting int getEmergencyCallRouting() {
        return mEmergencyCallRouting;
    }

    @Override
    /** @hide */
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("[EmergencyNumber: %s, countryIso=%s, mnc=%s, src=%s, routing=%s, "
                        + "categories=%s, urns=%s]",
                mNumber,
                mCountryIso,
                mMnc,
                sourceBitmaskToString(mEmergencyNumberSourceBitmask),
                routingToString(mEmergencyCallRouting),
                categoriesToString(mEmergencyServiceCategoryBitmask),
                (mEmergencyUrns == null ? "" :
                        mEmergencyUrns.stream().collect(Collectors.joining(","))));
    }

    /**
     * @param categories emergency service category bitmask
     * @return loggable string describing the category bitmask
     */
    private String categoriesToString(@EmergencyServiceCategories int categories) {
        StringBuilder sb = new StringBuilder();
        if ((categories & EMERGENCY_SERVICE_CATEGORY_AIEC) == EMERGENCY_SERVICE_CATEGORY_AIEC) {
            sb.append("auto ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_AMBULANCE)
                == EMERGENCY_SERVICE_CATEGORY_AMBULANCE) {
            sb.append("ambulance ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE)
                == EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE) {
            sb.append("fire ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD)
                == EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD) {
            sb.append("marine ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE)
                == EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE) {
            sb.append("mountain ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_POLICE) == EMERGENCY_SERVICE_CATEGORY_POLICE) {
            sb.append("police ");
        }
        if ((categories & EMERGENCY_SERVICE_CATEGORY_MIEC) == EMERGENCY_SERVICE_CATEGORY_MIEC) {
            sb.append("manual ");
        }
        return sb.toString();
    }

    /**
     * @param routing emergency call routing type
     * @return loggable string describing the routing type.
     */
    private String routingToString(@EmergencyCallRouting int routing) {
        switch (routing) {
            case EMERGENCY_CALL_ROUTING_EMERGENCY: return "emergency";
            case EMERGENCY_CALL_ROUTING_NORMAL: return "normal";
            case EMERGENCY_CALL_ROUTING_UNKNOWN: return "unknown";
            default: return "🤷";
        }
    }

    /**
     * Builds a string describing the sources for an emergency number.
     * @param sourceBitmask the source bitmask
     * @return loggable string describing the sources.
     */
    private String sourceBitmaskToString(@EmergencyNumberSources int sourceBitmask) {
        StringBuilder sb = new StringBuilder();
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING)
                == EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING) {
            sb.append("net ");
        }
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_SIM) == EMERGENCY_NUMBER_SOURCE_SIM) {
            sb.append("sim ");
        }
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_DATABASE)
                == EMERGENCY_NUMBER_SOURCE_DATABASE) {
            sb.append("db ");
        }
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG)
                == EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG) {
            sb.append("mdm ");
        }
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_DEFAULT) == EMERGENCY_NUMBER_SOURCE_DEFAULT) {
            sb.append("def ");
        }
        if ((sourceBitmask & EMERGENCY_NUMBER_SOURCE_TEST) == EMERGENCY_NUMBER_SOURCE_TEST) {
            sb.append("tst ");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!EmergencyNumber.class.isInstance(o)) {
            return false;
        }
        EmergencyNumber other = (EmergencyNumber) o;
        return mNumber.equals(other.mNumber)
                && mCountryIso.equals(other.mCountryIso)
                && mMnc.equals(other.mMnc)
                && mEmergencyServiceCategoryBitmask == other.mEmergencyServiceCategoryBitmask
                && mEmergencyUrns.equals(other.mEmergencyUrns)
                && mEmergencyNumberSourceBitmask == other.mEmergencyNumberSourceBitmask
                && mEmergencyCallRouting == other.mEmergencyCallRouting;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNumber, mCountryIso, mMnc, mEmergencyServiceCategoryBitmask,
                mEmergencyUrns, mEmergencyNumberSourceBitmask, mEmergencyCallRouting);
    }

    /**
     * Calculate the score for display priority.
     *
     * A higher display priority score means the emergency number has a higher display priority.
     * The score is higher if the source is defined for a higher display priority.
     *
     * The priority of sources are defined as follows:
     *     EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING >
     *     EMERGENCY_NUMBER_SOURCE_SIM >
     *     EMERGENCY_NUMBER_SOURCE_DATABASE >
     *     EMERGENCY_NUMBER_SOURCE_DEFAULT >
     *     EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG
     *
     */
    private int getDisplayPriorityScore() {
        int score = 0;
        if (this.isFromSources(EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING)) {
            score += 1 << 4;
        }
        if (this.isFromSources(EMERGENCY_NUMBER_SOURCE_SIM)) {
            score += 1 << 3;
        }
        if (this.isFromSources(EMERGENCY_NUMBER_SOURCE_DATABASE)) {
            score += 1 << 2;
        }
        if (this.isFromSources(EMERGENCY_NUMBER_SOURCE_DEFAULT)) {
            score += 1 << 1;
        }
        if (this.isFromSources(EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG)) {
            score += 1 << 0;
        }
        return score;
    }

    /**
     * Compare the display priority for this emergency number and the supplied emergency number.
     *
     * @param emergencyNumber the supplied emergency number
     * @return a negative value if the supplied emergency number has a lower display priority;
     *         a positive value if the supplied emergency number has a higher display priority;
     *         0 if both have equal display priority.
     */
    @Override
    public int compareTo(@NonNull EmergencyNumber emergencyNumber) {
        if (this.getDisplayPriorityScore()
                > emergencyNumber.getDisplayPriorityScore()) {
            return -1;
        } else if (this.getDisplayPriorityScore()
                < emergencyNumber.getDisplayPriorityScore()) {
            return 1;
        } else if (this.getNumber().compareTo(emergencyNumber.getNumber()) != 0) {
            return this.getNumber().compareTo(emergencyNumber.getNumber());
        } else if (this.getCountryIso().compareTo(emergencyNumber.getCountryIso()) != 0) {
            return this.getCountryIso().compareTo(emergencyNumber.getCountryIso());
        } else if (this.getMnc().compareTo(emergencyNumber.getMnc()) != 0) {
            return this.getMnc().compareTo(emergencyNumber.getMnc());
        } else if (this.getEmergencyServiceCategoryBitmask()
                != emergencyNumber.getEmergencyServiceCategoryBitmask()) {
            return this.getEmergencyServiceCategoryBitmask()
                    > emergencyNumber.getEmergencyServiceCategoryBitmask() ? -1 : 1;
        } else if (this.getEmergencyUrns().toString().compareTo(
                emergencyNumber.getEmergencyUrns().toString()) != 0) {
            return this.getEmergencyUrns().toString().compareTo(
                    emergencyNumber.getEmergencyUrns().toString());
        } else if (this.getEmergencyCallRouting()
                != emergencyNumber.getEmergencyCallRouting()) {
            return this.getEmergencyCallRouting()
                    > emergencyNumber.getEmergencyCallRouting() ? -1 : 1;
        } else {
            return 0;
        }
    }

    

    

    

    

    

    

    

    
}
