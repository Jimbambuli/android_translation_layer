package android.provider;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * AOSP BlockedNumberContract (android-14): the contract of the blocked number provider. ATL has no
 * such provider (com.android.blockednumber), so the static helpers end in AOSP's "provider not ready"
 * branches, and queries of CONTENT_URI find no provider.
 */
public class BlockedNumberContract {
	private BlockedNumberContract() {}

	private static final String LOG_TAG = "BlockedNumberContract";

	/** The authority for the blocked number provider */
	public static final String AUTHORITY = "com.android.blockednumber";

	/** A content:// style uri to the authority for the blocked number provider */
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	/** Constants to interact with the blocked numbers list. */
	public static class BlockedNumbers {
		private BlockedNumbers() {}

		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "blocked");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/blocked_number";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/blocked_number";
		public static final String COLUMN_ID = "_id";
		public static final String COLUMN_ORIGINAL_NUMBER = "original_number";
		public static final String COLUMN_E164_NUMBER = "e164_number";
	}

	/** @hide */
	public static final String METHOD_IS_BLOCKED = "is_blocked";
	/** @hide */
	public static final String METHOD_UNBLOCK = "unblock";
	/** @hide */
	public static final String RES_NUMBER_IS_BLOCKED = "blocked";
	/** @hide */
	public static final String RES_NUM_ROWS_DELETED = "num_deleted";
	/** @hide */
	public static final String METHOD_CAN_CURRENT_USER_BLOCK_NUMBERS = "can_current_user_block_numbers";
	/** @hide */
	public static final String RES_CAN_BLOCK_NUMBERS = "can_block";

	public static boolean isBlocked(Context context, String phoneNumber) {
		try {
			final Bundle res = context.getContentResolver().call(AUTHORITY_URI, METHOD_IS_BLOCKED, phoneNumber, null);
			return res != null && res.getBoolean(RES_NUMBER_IS_BLOCKED, false);
		} catch (NullPointerException | IllegalArgumentException ex) {
			// The content resolver can throw an NPE or IAE; we don't want to crash Telecom if
			// either of these happen.
			Log.w(LOG_TAG, "isBlocked: provider not ready.");
			return false;
		}
	}

	public static int unblock(Context context, String phoneNumber) {
		final Bundle res = context.getContentResolver().call(AUTHORITY_URI, METHOD_UNBLOCK, phoneNumber, null);
		return res.getInt(RES_NUM_ROWS_DELETED, 0);
	}

	public static boolean canCurrentUserBlockNumbers(Context context) {
		try {
			final Bundle res = context.getContentResolver().call(AUTHORITY_URI, METHOD_CAN_CURRENT_USER_BLOCK_NUMBERS, null, null);
			return res != null && res.getBoolean(RES_CAN_BLOCK_NUMBERS, false);
		} catch (NullPointerException | IllegalArgumentException ex) {
			// The content resolver can throw an NPE or IAE; we don't want to crash Telecom if
			// either of these happen.
			Log.w(LOG_TAG, "canCurrentUserBlockNumbers: provider not ready.");
			return false;
		}
	}
}
