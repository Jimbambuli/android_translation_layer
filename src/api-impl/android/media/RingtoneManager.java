package android.media;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Settings;

/* AOSP media/java/android/media/RingtoneManager.java. ATL's media store holds no ringtones, so the
 * ringtone list is empty; getRingtone() opens the URI with a local player (see Ringtone). */
public class RingtoneManager {
	public static final int TYPE_RINGTONE = 1;
	public static final int TYPE_NOTIFICATION = 2;
	public static final int TYPE_ALARM = 4;
	public static final int TYPE_ALL = TYPE_RINGTONE | TYPE_NOTIFICATION | TYPE_ALARM;

	public static final int ID_COLUMN_INDEX = 0;
	public static final int TITLE_COLUMN_INDEX = 1;
	public static final int URI_COLUMN_INDEX = 2;

	/* AOSP INTERNAL_COLUMNS/MEDIA_COLUMNS */
	private static final String[] COLUMNS = {"_id", "title", "title", "title_key"};

	private final Context mContext;
	private int mType = TYPE_RINGTONE;
	private Cursor mCursor;
	private boolean mStopPreviousRingtone = true;
	private Ringtone mPreviousRingtone;

	public RingtoneManager(Activity activity) {
		this((Context)activity);
	}

	public RingtoneManager(Context context) {
		mContext = context;
	}

	/* AOSP setType(): only before the ringtones are queried */
	public void setType(int type) {
		if (mCursor != null)
			throw new IllegalStateException("Setting filter columns should be done before querying for ringtones.");
		mType = type;
	}

	/* AOSP inferStreamType() */
	public int inferStreamType() {
		switch (mType) {
			case TYPE_ALARM:
				return /*AudioManager.STREAM_ALARM*/ 4;
			case TYPE_NOTIFICATION:
				return /*AudioManager.STREAM_NOTIFICATION*/ 5;
			default:
				return /*AudioManager.STREAM_RING*/ 2;
		}
	}

	public void setStopPreviousRingtone(boolean stopPreviousRingtone) {
		mStopPreviousRingtone = stopPreviousRingtone;
	}

	public boolean getStopPreviousRingtone() {
		return mStopPreviousRingtone;
	}

	public void stopPreviousRingtone() {
		if (mPreviousRingtone != null)
			mPreviousRingtone.stop();
	}

	/* AOSP getCursor(): the internal and media-store ringtones, which ATL has none of */
	public Cursor getCursor() {
		if (mCursor != null && mCursor.requery())
			return mCursor;
		return mCursor = new MatrixCursor(COLUMNS);
	}

	public Ringtone getRingtone(int position) {
		if (mStopPreviousRingtone && mPreviousRingtone != null)
			mPreviousRingtone.stop();
		mPreviousRingtone = getRingtone(mContext, getRingtoneUri(position), inferStreamType());
		return mPreviousRingtone;
	}

	/* AOSP getRingtoneUri(): null when the cursor has no such row */
	public Uri getRingtoneUri(int position) {
		if (mCursor == null || !mCursor.moveToPosition(position))
			return null;
		return Uri.parse(mCursor.getString(URI_COLUMN_INDEX) + "/" + mCursor.getLong(ID_COLUMN_INDEX));
	}

	/* AOSP getRingtonePosition(): -1 for a URI not in the list */
	public int getRingtonePosition(Uri ringtoneUri) {
		if (ringtoneUri == null)
			return -1;
		Cursor cursor = getCursor();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			if (ringtoneUri.equals(Uri.parse(cursor.getString(URI_COLUMN_INDEX) + "/" + cursor.getLong(ID_COLUMN_INDEX))))
				return cursor.getPosition();
		}
		return -1;
	}

	/* AOSP getRingtone(): a Ringtone whose local player is opened on the URI. AOSP returns null when the
	 * URI can be played neither locally nor by the remote ringtone player; ATL lacks the remote player and
	 * hands out the (silent) Ringtone instead, so callers keep the object they get on a device. */
	public static Ringtone getRingtone(Context context, Uri uri) {
		return getRingtone(context, uri, -1);
	}

	private static Ringtone getRingtone(Context context, Uri uri, int streamType) {
		Ringtone r = new Ringtone(context, true);
		if (streamType >= 0)
			r.setStreamType(streamType);
		r.setUri(uri);
		r.createLocalMediaPlayer();
		return r;
	}

	/* AOSP getDefaultUri(): the symbolic Settings.System.DEFAULT_*_URI of the type, null for another type */
	public static Uri getDefaultUri(int type) {
		if ((type & TYPE_RINGTONE) != 0)
			return Uri.parse("content://settings/system/ringtone");
		else if ((type & TYPE_NOTIFICATION) != 0)
			return Uri.parse("content://settings/system/notification_sound");
		else if ((type & TYPE_ALARM) != 0)
			return Uri.parse("content://settings/system/alarm_alert");
		return null;
	}

	/* AOSP getDefaultType(): the type of a Settings.System default URI, -1 otherwise */
	public static int getDefaultType(Uri defaultRingtoneUri) {
		if (defaultRingtoneUri == null)
			return -1;
		String s = defaultRingtoneUri.toString();
		if (s.equals("content://settings/system/ringtone"))
			return TYPE_RINGTONE;
		if (s.equals("content://settings/system/notification_sound"))
			return TYPE_NOTIFICATION;
		if (s.equals("content://settings/system/alarm_alert"))
			return TYPE_ALARM;
		return -1;
	}

	/* AOSP isDefault(): getDefaultType() != -1 */
	public static boolean isDefault(Uri ringtoneUri) {
		return getDefaultType(ringtoneUri) != -1;
	}

	/* AOSP getSettingForType() */
	private static String getSettingForType(int type) {
		if ((type & TYPE_RINGTONE) != 0)
			return "ringtone";
		else if ((type & TYPE_NOTIFICATION) != 0)
			return "notification_sound";
		else if ((type & TYPE_ALARM) != 0)
			return "alarm_alert";
		return null;
	}

	/* AOSP getActualDefaultRingtoneUri(): the Settings.System value of the type, null when unset */
	public static Uri getActualDefaultRingtoneUri(Context context, int type) {
		String setting = getSettingForType(type);
		if (setting == null)
			return null;
		String uriString = Settings.System.getString(context.getContentResolver(), setting);
		return uriString != null ? Uri.parse(uriString) : null;
	}
}
