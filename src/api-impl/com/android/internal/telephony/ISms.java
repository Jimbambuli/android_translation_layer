/*
 * Copyright 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Bundle;

/* ATL: ISms.aidl as the Java interface aidl generates, without Proxy and onTransact(): the SMS service runs in the
 * app's process (ModemManagerPhone.getSmsService()). Shape as IMediaBrowserService: extends IInterface, Stub with asInterface() */
public interface ISms extends android.os.IInterface {
    /* ATL: aidl's Stub, local only */
    public static abstract class Stub extends android.os.Binder implements ISms {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ISms";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISms asInterface(android.os.IBinder obj) {
            if (obj == null)
                return null;
            return (ISms) obj.queryLocalInterface(DESCRIPTOR);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }
    }

    /**
     * Retrieves all messages currently stored on ICC.
     * @param subId the subId id.
     * @return list of SmsRawData of all sms on ICC
     */
    java.util.List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int subId, String callingPkg) throws android.os.RemoteException;

    /**
     * Update the specified message on the ICC.
     *
     * @param messageIndex record index of message to update
     * @param newStatus new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @param subId the subId id.
     * @return success or not
     *
     */
     boolean updateMessageOnIccEfForSubscriber(int subId, String callingPkg,
             int messageIndex, int newStatus, byte[] pdu) throws android.os.RemoteException;

    /**
     * Copy a raw SMS PDU to the ICC.
     *
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param subId the subId id.
     * @return success or not
     *
     */
    boolean copyMessageToIccEfForSubscriber(int subId, String callingPkg, int status,
            byte[] pdu, byte[] smsc) throws android.os.RemoteException;

    /**
     * Send a data SMS.
     *
     * @param smsc the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param subId the subId id.
     */
    void sendDataForSubscriber(int subId, String callingPkg, String callingattributionTag,
            String destAddr, String scAddr, int destPort,byte[] data,
            PendingIntent sentIntent, PendingIntent deliveryIntent) throws android.os.RemoteException;

    /**
     * Send an SMS.
     *
     * @param smsc the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param subId the subId on which the SMS has to be sent.
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *   be automatically persisted in the SMS db. It only affects messages sent
     *   by a non-default SMS app. Currently only the carrier app can set this
     *   parameter to false to skip auto message persistence.
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *   Used for logging and diagnostics purposes. The id may be 0.
     */
    void sendTextForSubscriber(int subId, String callingPkg, String callingAttributionTag,
            String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp,
            long messageId) throws android.os.RemoteException;

    /**
     * Send an SMS with options using Subscription Id.
     *
     * @param subId the subId on which the SMS has to be sent.
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *   be automatically persisted in the SMS db. It only affects messages sent
     *   by a non-default SMS app. Currently only the carrier app can set this
     *   parameter to false to skip auto message persistence.
     * @param priority Priority level of the message
     *  Refer specification See 3GPP2 C.S0015-B, v2.0, table 4.5.9-1
     *  ---------------------------------
     *  PRIORITY      | Level of Priority
     *  ---------------------------------
     *      '00'      |     Normal
     *      '01'      |     Interactive
     *      '10'      |     Urgent
     *      '11'      |     Emergency
     *  ----------------------------------
     *  Any Other values included Negative considered as Invalid Priority Indicator of the message.
     * @param expectMore is a boolean to indicate the sending message is multi segmented or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     */
    void sendTextForSubscriberWithOptions(int subId, String callingPkg,
            String callingAttributionTag, String destAddr, String scAddr, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore,
            int validityPeriod) throws android.os.RemoteException;

    /**
     * Inject an SMS PDU into the android platform.
     *
     * @param subId the subId on which the SMS has to be injected.
     * @param pdu is the byte array of pdu to be injected into android application framework
     * @param format is the format of SMS pdu (android.telephony.SmsMessage.FORMAT_3GPP or
     * android.telephony.SmsMessage.FORMAT_3GPP2)
     * @param receivedIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully received by the
     *  android application framework. This intent is broadcasted at
     *  the same time an SMS received from radio is acknowledged back.
     */
    void injectSmsPduForSubscriber(
            int subId, byte[] pdu, String format, PendingIntent receivedIntent) throws android.os.RemoteException;

    /**
     * Send a multi-part text based SMS.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @param subId the subId on which the SMS has to be sent.
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *   be automatically persisted in the SMS db. It only affects messages sent
     *   by a non-default SMS app. Currently only the carrier app can set this
     *   parameter to false to skip auto message persistence.
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *   Used for logging and diagnostics purposes. The id may be 0.
     */
    void sendMultipartTextForSubscriber(int subId, String callingPkg,
            String callingAttributionTag, String destinationAddress, String scAddress,
            java.util.List<String> parts, java.util.List<PendingIntent> sentIntents,
            java.util.List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp,
            long messageId) throws android.os.RemoteException;

    /**
     * Send a multi-part text based SMS with options using Subscription Id.
     *
     * @param subId the subId on which the SMS has to be sent.
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *   be automatically persisted in the SMS db. It only affects messages sent
     *   by a non-default SMS app. Currently only the carrier app can set this
     *   parameter to false to skip auto message persistence.
     * @param priority Priority level of the message
     *  Refer specification See 3GPP2 C.S0015-B, v2.0, table 4.5.9-1
     *  ---------------------------------
     *  PRIORITY      | Level of Priority
     *  ---------------------------------
     *      '00'      |     Normal
     *      '01'      |     Interactive
     *      '10'      |     Urgent
     *      '11'      |     Emergency
     *  ----------------------------------
     *  Any Other values included Negative considered as Invalid Priority Indicator of the message.
     * @param expectMore is a boolean to indicate the sending message is multi segmented or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     */
    void sendMultipartTextForSubscriberWithOptions(int subId, String callingPkg,
            String callingAttributionTag, String destinationAddress, String scAddress,
            java.util.List<String> parts, java.util.List<PendingIntent> sentIntents,
            java.util.List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp,
            int priority, boolean expectMore, int validityPeriod) throws android.os.RemoteException;

    /**
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier and RAN type. The RAN type specify this message ID
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients
     * enable the same message identifier, they must both disable it for the
     * device to stop receiving those messages.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param subId for which the broadcast has to be enabled
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #disableCellBroadcastForSubscriber(int, int, int)
     */
    boolean enableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws android.os.RemoteException;

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier and RAN type. The RAN type specify this message ID
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients
     * enable the same message identifier, they must both disable it for the
     * device to stop receiving those messages.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param subId for which the broadcast has to be disabled
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcastForSubscriber(int, int, int)
     */
    boolean disableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws android.os.RemoteException;

    /*
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range and RAN type. The RAN type specify this message ID range
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients enable
     * a message identifier range, they must both disable it for the device
     * to stop receiving those messages.
     *
     * @param startMessageId first message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param endMessageId last message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param subId for which the broadcast has to be enabled
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #disableCellBroadcastRangeForSubscriber(int, int, int, int)
     */
    boolean enableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId,
            int ranType) throws android.os.RemoteException;

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range and RAN type. The RAN type specify this message ID range
     * belong to 3GPP (GSM) or 3GPP2(CDMA). Note that if two different clients enable
     * a message identifier range, they must both disable it for the device
     * to stop receiving those messages.
     *
     * @param startMessageId first message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param endMessageId last message identifier as specified in TS 23.041 (3GPP) or
     *   C.R1001-G (3GPP2)
     * @param subId for which the broadcast has to be disabled
     * @param ranType as defined in class SmsManager, the value can be one of these:
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_GSM
     *    android.telephony.SmsMessage.CELL_BROADCAST_RAN_TYPE_CDMA
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcastRangeForSubscriber(int, int, int, int)
     */
    boolean disableCellBroadcastRangeForSubscriber(int subId, int startMessageId,
            int endMessageId, int ranType) throws android.os.RemoteException;

    /**
     * Returns the premium SMS send permission for the specified package.
     * Requires system permission.
     */
    int getPremiumSmsPermission(String packageName) throws android.os.RemoteException;

    /**
     * Returns the premium SMS send permission for the specified package.
     * Requires system permission.
     */
    int getPremiumSmsPermissionForSubscriber(int subId, String packageName) throws android.os.RemoteException;

    /**
     * Set the SMS send permission for the specified package.
     * Requires system permission.
     */
    void setPremiumSmsPermission(String packageName, int permission) throws android.os.RemoteException;

     /**
     * Set the SMS send permission for the specified package.
     * Requires system permission.
     */
    void setPremiumSmsPermissionForSubscriber(int subId, String packageName, int permission) throws android.os.RemoteException;

    /**
     * SMS over IMS is supported if IMS is registered and SMS is supported
     * on IMS.
     * @param subId for subId which isImsSmsSupported is queried
     * @return true if SMS over IMS is supported, false otherwise
     *
     * @see #getImsSmsFormatForSubscriber(int)
     */
    boolean isImsSmsSupportedForSubscriber(int subId) throws android.os.RemoteException;

    /**
     * User needs to pick SIM for SMS if multiple SIMs present and if current subId passed in is not
     * active/valid.
     * @param subId current subId for sending SMS
     * @return true if SIM for SMS sending needs to be chosen
     */
    boolean isSmsSimPickActivityNeeded(int subId) throws android.os.RemoteException;

    /*
     * get user prefered SMS subId
     * @return subId id
     */
    int getPreferredSmsSubscription() throws android.os.RemoteException;

    /**
     * Gets SMS format supported on IMS.  SMS over IMS format is
     * either 3GPP or 3GPP2.
     * @param subId for subId which getImsSmsFormat is queried
     * @return android.telephony.SmsMessage.FORMAT_3GPP,
     *         android.telephony.SmsMessage.FORMAT_3GPP2
     *      or android.telephony.SmsMessage.FORMAT_UNKNOWN
     *
     * @see #isImsSmsSupportedForSubscriber(int)
     */
    String getImsSmsFormatForSubscriber(int subId) throws android.os.RemoteException;

    /*
     * Get SMS prompt property,  enabled or not
     * @return true if enabled, false otherwise
     */
    boolean isSMSPromptEnabled() throws android.os.RemoteException;

    /**
     * Send a system stored text message.
     *
     * This is used for sending a previously sent, but failed-to-send, message or
     * for sending a text message that has been stored as a draft.
     *
     * @param subId the SIM id.
     * @param callingPkg the package name of the calling app
     * @param callingAttributionTag the attribution tag of calling context
     * @param messageUri the URI of the stored message
     * @param scAddress is the service center address or null to use the current default SMSC
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    void sendStoredText(int subId, String callingPkg, String callingAttributionTag,
            Uri messageUri, String scAddress, PendingIntent sentIntent,
            PendingIntent deliveryIntent) throws android.os.RemoteException;

    /**
     * Send a system stored multi-part text message.
     *
     * This is used for sending a previously sent, but failed-to-send, message or
     * for sending a text message that has been stored as a draft.
     * The provided <code>PendingIntent</code> lists should match the part number of the
     * divided text of the stored message by using <code>divideMessage</code>
     *
     * @param subId the SIM id.
     * @param callingPkg the package name of the calling app
     * @param callingAttributeTag the attribute tag of the calling context
     * @param messageUri the URI of the stored message
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK</code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applications,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    void sendStoredMultipartText(int subId, String callingPkg, String callingAttributeTag,
                Uri messageUri, String scAddress, java.util.List<PendingIntent> sentIntents,
                java.util.List<PendingIntent> deliveryIntents) throws android.os.RemoteException;

    /**
     * Get carrier-dependent configuration values.
     *
     * @param subId the subscription Id
     */
    Bundle getCarrierConfigValuesForSubscriber(int subId) throws android.os.RemoteException;

    /**
     * Create an app-only incoming SMS request for the calling package.
     *
     * If an incoming text contains the token returned by this method the provided
     * <code>PendingIntent</code> will be sent containing the SMS data.
     *
     * @param subId the SIM id.
     * @param callingPkg the package name of the calling app.
     * @param intent PendingIntent to be sent when an SMS is received containing the token.
     */
    String createAppSpecificSmsToken(int subId, String callingPkg, PendingIntent intent) throws android.os.RemoteException;

    /**
     * Create an app-only incoming SMS request for the calling package.
     *
     * If an incoming text contains the token returned by this method the provided
     * <code>PendingIntent</code> will be sent containing the SMS data.
     *
     * @param subId the SIM id.
     * @param callingPkg the package name of the calling app.
     * @param prefixes the caller provided prefixes
     * @param intent PendingIntent to be sent when a SMS is received containing the token and one
     *   of the prefixes
     */
    String createAppSpecificSmsTokenWithPackageInfo(
            int subId, String callingPkg, String prefixes, PendingIntent intent) throws android.os.RemoteException;

    /**
     * set Memory Status in SmsStorageMonitor
     *
     * @param subId the subscription Id.
     * @param callingPackage the package name of the calling app.
     * @param isStorageAvailable sets StorageAvailable to false or true
     *   for testing behaviour of SmsStorageMonitor
     */
    void setStorageMonitorMemoryStatusOverride(int subId, boolean isStorageAvailable) throws android.os.RemoteException;

     /**
     * reset Memory Status change made by TestApi setStorageMonitorMemoryStatusOverride
     * in SmsStorageMonitor
     *
     * @param subId the subscription Id.
     * @param callingPackage the package name of the calling app.
     */
    void clearStorageMonitorMemoryStatusOverride(int subId) throws android.os.RemoteException;

    /**
     * Check if the destination is a possible premium short code.
     *
     * @param destAddress the destination address to test for possible short code
     */
    int checkSmsShortCodeDestination(int subId, String callingApk, String callingFeatureId,
            String destAddress, String countryIso) throws android.os.RemoteException;

    /**
     * Gets the SMSC address from (U)SIM.
     *
     * @param subId the subscription Id.
     * @param callingPackage the package name of the calling app.
     * @return the SMSC address string, null if failed.
     */
    String getSmscAddressFromIccEfForSubscriber(int subId, String callingPackage) throws android.os.RemoteException;

    /**
     * Sets the SMSC address on (U)SIM.
     *
     * @param smsc the SMSC address string.
     * @param subId the subscription Id.
     * @param callingPackage the package name of the calling app.
     * @return true for success, false otherwise.
     */
    boolean setSmscAddressOnIccEfForSubscriber(String smsc, int subId, String callingPackage) throws android.os.RemoteException;

    /**
     * Get the capacity count of sms on Icc card.
     *
     * @param subId for subId which getSmsCapacityOnIcc is queried.
     * @return capacity of ICC
     */
    int getSmsCapacityOnIccForSubscriber(int subId) throws android.os.RemoteException;

    /**
     * Reset all cell broadcast ranges. Previously enabled ranges will become invalid after this.
     *
     * @param subId Subscription index
     * @return {@code true} if succeeded, otherwise {@code false}.
     *
     * @hide
     */
    boolean resetAllCellBroadcastRanges(int subId) throws android.os.RemoteException;

    /**
     * Gets the message size of a WAP from the cache.
     *
     * @param locationUrl the location to use as a key for looking up the size in the cache.
     * The locationUrl may or may not have the transactionId appended to the url.
     *
     * @return long representing the message size
     * @throws java.util.NoSuchElementException if the WAP push doesn't exist in the cache
     *
     * @hide
     */
    long getWapMessageSize(String locationUrl) throws android.os.RemoteException;
}
