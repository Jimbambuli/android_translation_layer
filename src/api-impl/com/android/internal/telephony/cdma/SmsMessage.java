/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.telephony.cdma;

import android.telephony.Rlog;

import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.cdma.sms.UserData;

/**
 * ATL NOTE: CDMA (3GPP2) SMS is NOT ported. The full AOSP class pulls in cdma/sms/BearerData,
 * CdmaSmsAddress, SmsEnvelope, SmsCbMessage and friends (about 5000 lines). This file only keeps
 * the AOSP signatures that android.telephony.SmsMessage and android.telephony.gsm.SmsMessage
 * dispatch to, so their AOSP branch structure stays unchanged. ATL reports PHONE_TYPE_GSM
 * (TelephonyManager.getCurrentPhoneType()), so the CDMA branches are not taken; if one is
 * reached anyway it throws UnsupportedOperationException instead of inventing behaviour.
 * Replace this file with the AOSP one to enable CDMA.
 */
public class SmsMessage extends SmsMessageBase {
	static final String LOG_TAG = "SmsMessage";

	private static UnsupportedOperationException notPorted() {
		return new UnsupportedOperationException("CDMA (3GPP2) SMS is not ported to ATL");
	}

	public SmsMessage() {}

	public static class SubmitPdu extends SubmitPduBase {
		public SubmitPdu() {
		}
	}

	public static SmsMessage createFromPdu(byte[] pdu) {
		throw notPorted();
	}

	public static SmsMessage createFromEfRecord(int index, byte[] data) {
		throw notPorted();
	}

	/* AOSP body */
	public static int getTPLayerLengthForPDU(String pdu) {
		Rlog.w(LOG_TAG, "getTPLayerLengthForPDU: is not supported in CDMA mode.");
		return 0;
	}

	public static SubmitPdu getSubmitPdu(String scAddr, String destAddr, String message,
			boolean statusReportRequested, SmsHeader smsHeader) {
		throw notPorted();
	}

	public static SubmitPdu getSubmitPdu(String scAddr, String destAddr, String message,
			boolean statusReportRequested, SmsHeader smsHeader, int priority) {
		throw notPorted();
	}

	public static SubmitPdu getSubmitPdu(String scAddr, String destAddr, int destPort,
			byte[] data, boolean statusReportRequested) {
		throw notPorted();
	}

	public static SubmitPdu getSubmitPdu(String destAddr, UserData userData,
			boolean statusReportRequested) {
		throw notPorted();
	}

	public static TextEncodingDetails calculateLength(CharSequence messageBody,
			boolean use7bitOnly, boolean isEntireMsg) {
		throw notPorted();
	}

	public static SubmitPdu getDeliverPdu(String origAddr, String message, long date) {
		throw notPorted();
	}

	@Override
	public SmsConstants.MessageClass getMessageClass() { throw notPorted(); }
	@Override
	public int getProtocolIdentifier() { throw notPorted(); }
	@Override
	public boolean isReplace() { throw notPorted(); }
	@Override
	public boolean isCphsMwiMessage() { throw notPorted(); }
	@Override
	public boolean isMWIClearMessage() { throw notPorted(); }
	@Override
	public boolean isMWISetMessage() { throw notPorted(); }
	@Override
	public boolean isMwiDontStore() { throw notPorted(); }
	@Override
	public int getStatus() { throw notPorted(); }
	@Override
	public boolean isStatusReportMessage() { throw notPorted(); }
	@Override
	public boolean isReplyPathPresent() { throw notPorted(); }
}
