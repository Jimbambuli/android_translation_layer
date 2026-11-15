package android.media;

import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseIntArray;

/* AOSP media/java/android/media/MediaMetadataEditor.java (comments and annotations removed) */
@Deprecated
public abstract class MediaMetadataEditor {

	private final static String TAG = "MediaMetadataEditor";
	
	protected MediaMetadataEditor() {
	}
	
	public final static int BITMAP_KEY_ARTWORK =
			100 /*RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK*/;

	public final static int RATING_KEY_BY_OTHERS = 101;

	public final static int RATING_KEY_BY_USER = 0x10000001;

	public final static int KEY_EDITABLE_MASK = 0x1FFFFFFF;

	public abstract void apply();

	protected long mEditableKeys;

	protected boolean mMetadataChanged = false;

	protected boolean mApplied = false;

	protected boolean mArtworkChanged = false;

	protected Bitmap mEditorArtwork;

	protected Bundle mEditorMetadata;

	protected MediaMetadata.Builder mMetadataBuilder;

	public synchronized void clear() {
		if (mApplied) {
			Log.e(TAG, "Can't clear a previously applied MediaMetadataEditor");
			return;
		}
		mEditorMetadata.clear();
		mEditorArtwork = null;
		mMetadataBuilder = new MediaMetadata.Builder();
	}

	public synchronized void addEditableKey(int key) {
		if (mApplied) {
			Log.e(TAG, "Can't change editable keys of a previously applied MetadataEditor");
			return;
		}
		if (key == RATING_KEY_BY_USER) {
			mEditableKeys |= (KEY_EDITABLE_MASK & key);
			mMetadataChanged = true;
		} else {
			Log.e(TAG, "Metadata key " + key + " cannot be edited");
		}
	}

	public synchronized void removeEditableKeys() {
		if (mApplied) {
			Log.e(TAG, "Can't remove all editable keys of a previously applied MetadataEditor");
			return;
		}
		if (mEditableKeys != 0) {
			mEditableKeys = 0;
			mMetadataChanged = true;
		}
	}

	public synchronized int[] getEditableKeys() {
		if (mEditableKeys == RATING_KEY_BY_USER) {
			int[] keys = { RATING_KEY_BY_USER };
			return keys;
		} else {
			return null;
		}
	}

	public synchronized MediaMetadataEditor putString(int key, String value)
			throws IllegalArgumentException {
		if (mApplied) {
			Log.e(TAG, "Can't edit a previously applied MediaMetadataEditor");
			return this;
		}
		if (METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID) != METADATA_TYPE_STRING) {
			throw(new IllegalArgumentException("Invalid type 'String' for key "+ key));
		}
		mEditorMetadata.putString(String.valueOf(key), value);
		mMetadataChanged = true;
		return this;
	}

	public synchronized MediaMetadataEditor putLong(int key, long value)
			throws IllegalArgumentException {
		if (mApplied) {
			Log.e(TAG, "Can't edit a previously applied MediaMetadataEditor");
			return this;
		}
		if (METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID) != METADATA_TYPE_LONG) {
			throw(new IllegalArgumentException("Invalid type 'long' for key "+ key));
		}
		mEditorMetadata.putLong(String.valueOf(key), value);
		mMetadataChanged = true;
		return this;
	}

	public synchronized MediaMetadataEditor putBitmap(int key, Bitmap bitmap)
			throws IllegalArgumentException {
		if (mApplied) {
			Log.e(TAG, "Can't edit a previously applied MediaMetadataEditor");
			return this;
		}
		if (key != BITMAP_KEY_ARTWORK) {
			throw(new IllegalArgumentException("Invalid type 'Bitmap' for key "+ key));
		}
		mEditorArtwork = bitmap;
		mArtworkChanged = true;
		return this;
	}

	public synchronized MediaMetadataEditor putObject(int key, Object value)
			throws IllegalArgumentException {
		if (mApplied) {
			Log.e(TAG, "Can't edit a previously applied MediaMetadataEditor");
			return this;
		}
		switch(METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID)) {
			case METADATA_TYPE_LONG:
				if (value instanceof Long) {
					return putLong(key, ((Long)value).longValue());
				} else {
					throw(new IllegalArgumentException("Not a non-null Long for key "+ key));
				}
			case METADATA_TYPE_STRING:
				if ((value == null) || (value instanceof String)) {
					return putString(key, (String) value);
				} else {
					throw(new IllegalArgumentException("Not a String for key "+ key));
				}
			case METADATA_TYPE_RATING:
				mEditorMetadata.putParcelable(String.valueOf(key), (Parcelable)value);
				mMetadataChanged = true;
				break;
			case METADATA_TYPE_BITMAP:
				if ((value == null) || (value instanceof Bitmap))  {
					return putBitmap(key, (Bitmap) value);
				} else {
					throw(new IllegalArgumentException("Not a Bitmap for key "+ key));
				}
			default:
				throw(new IllegalArgumentException("Invalid key "+ key));
		}
		return this;
	}

	public synchronized long getLong(int key, long defaultValue)
			throws IllegalArgumentException {
		if (METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID) != METADATA_TYPE_LONG) {
			throw(new IllegalArgumentException("Invalid type 'long' for key "+ key));
		}
		return mEditorMetadata.getLong(String.valueOf(key), defaultValue);
	}

	public synchronized String getString(int key, String defaultValue)
			throws IllegalArgumentException {
		if (METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID) != METADATA_TYPE_STRING) {
			throw(new IllegalArgumentException("Invalid type 'String' for key "+ key));
		}
		return mEditorMetadata.getString(String.valueOf(key), defaultValue);
	}

	public synchronized Bitmap getBitmap(int key, Bitmap defaultValue)
			throws IllegalArgumentException {
		if (key != BITMAP_KEY_ARTWORK) {
			throw(new IllegalArgumentException("Invalid type 'Bitmap' for key "+ key));
		}
		return (mEditorArtwork != null ? mEditorArtwork : defaultValue);
	}

	public synchronized Object getObject(int key, Object defaultValue)
			throws IllegalArgumentException {
		switch (METADATA_KEYS_TYPE.get(key, METADATA_TYPE_INVALID)) {
			case METADATA_TYPE_LONG:
				if (mEditorMetadata.containsKey(String.valueOf(key))) {
					return mEditorMetadata.getLong(String.valueOf(key));
				} else {
					return defaultValue;
				}
			case METADATA_TYPE_STRING:
				if (mEditorMetadata.containsKey(String.valueOf(key))) {
					return mEditorMetadata.getString(String.valueOf(key));
				} else {
					return defaultValue;
				}
			case METADATA_TYPE_RATING:
				if (mEditorMetadata.containsKey(String.valueOf(key))) {
					return mEditorMetadata.getParcelable(String.valueOf(key));
				} else {
					return defaultValue;
				}
			case METADATA_TYPE_BITMAP:
				if (key == BITMAP_KEY_ARTWORK) {
					return (mEditorArtwork != null ? mEditorArtwork : defaultValue);
				} // else: fall through to invalid key handling
			default:
				throw(new IllegalArgumentException("Invalid key "+ key));
		}
	}

	protected static final int METADATA_TYPE_INVALID = -1;
	
	protected static final int METADATA_TYPE_LONG = 0;

	protected static final int METADATA_TYPE_STRING = 1;

	protected static final int METADATA_TYPE_BITMAP = 2;

	protected static final int METADATA_TYPE_RATING = 3;

	protected static final SparseIntArray METADATA_KEYS_TYPE;

	static {
		METADATA_KEYS_TYPE = new SparseIntArray(17);
		METADATA_KEYS_TYPE.put(
				MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, METADATA_TYPE_LONG);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, METADATA_TYPE_LONG);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_DURATION, METADATA_TYPE_LONG);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_YEAR, METADATA_TYPE_LONG);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_ALBUM, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(
				MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_TITLE, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_ARTIST, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_AUTHOR, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(
				MediaMetadataRetriever.METADATA_KEY_COMPILATION, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_COMPOSER, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_DATE, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_GENRE, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(MediaMetadataRetriever.METADATA_KEY_WRITER, METADATA_TYPE_STRING);
		METADATA_KEYS_TYPE.put(BITMAP_KEY_ARTWORK, METADATA_TYPE_BITMAP);
		METADATA_KEYS_TYPE.put(RATING_KEY_BY_OTHERS, METADATA_TYPE_RATING);
		METADATA_KEYS_TYPE.put(RATING_KEY_BY_USER, METADATA_TYPE_RATING);
	}
}
