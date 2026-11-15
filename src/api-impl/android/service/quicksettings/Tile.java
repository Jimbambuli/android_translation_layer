package android.service.quicksettings;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/* AOSP core/java/android/service/quicksettings/Tile.java (android-16). The tile is handed out by the quick
 * settings host (SystemUI) when it binds a TileService; ATL has no such host, so a Tile only exists when the app
 * creates or unparcels one, and updateTile() has no service to report to. */
public final class Tile implements Parcelable {
	private static final String TAG = "Tile";

	public static final int STATE_UNAVAILABLE = 0;
	public static final int STATE_INACTIVE = 1;
	public static final int STATE_ACTIVE = 2;

	private IBinder mToken;
	private Icon mIcon;
	private CharSequence mLabel;
	private CharSequence mSubtitle;
	private CharSequence mContentDescription;
	private CharSequence mStateDescription;
	private PendingIntent mPendingIntent;
	// Default to inactive until clients of the new API can update.
	private int mState = STATE_INACTIVE;

	public Tile(Parcel source) {
		readFromParcel(source);
	}

	public Tile() {
	}

	public int getState() {
		return mState;
	}

	public void setState(int state) {
		mState = state;
	}

	public Icon getIcon() {
		return mIcon;
	}

	public void setIcon(Icon icon) {
		this.mIcon = icon;
	}

	public CharSequence getLabel() {
		return mLabel;
	}

	public void setLabel(CharSequence label) {
		this.mLabel = label;
	}

	public CharSequence getSubtitle() {
		return mSubtitle;
	}

	public void setSubtitle(CharSequence subtitle) {
		this.mSubtitle = subtitle;
	}

	public CharSequence getContentDescription() {
		return mContentDescription;
	}

	public CharSequence getStateDescription() {
		return mStateDescription;
	}

	public void setContentDescription(CharSequence contentDescription) {
		this.mContentDescription = contentDescription;
	}

	public void setStateDescription(CharSequence stateDescription) {
		this.mStateDescription = stateDescription;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/* AOSP: mService.updateQsTile(this, mToken); no quick settings host here */
	public void updateTile() {
		Log.e(TAG, "Couldn't update tile");
	}

	public PendingIntent getActivityLaunchForClick() {
		return mPendingIntent;
	}

	public void setActivityLaunchForClick(PendingIntent pendingIntent) {
		if (pendingIntent != null && !pendingIntent.isActivity()) {
			throw new IllegalArgumentException();
		} else {
			mPendingIntent = pendingIntent;
		}
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (mIcon != null) {
			dest.writeByte((byte)1);
			mIcon.writeToParcel(dest, flags);
		} else {
			dest.writeByte((byte)0);
		}
		if (mPendingIntent != null) {
			dest.writeByte((byte)1);
			mPendingIntent.writeToParcel(dest, flags);
		} else {
			dest.writeByte((byte)0);
		}
		dest.writeInt(mState);
		TextUtils.writeToParcel(mLabel, dest, flags);
		TextUtils.writeToParcel(mSubtitle, dest, flags);
		TextUtils.writeToParcel(mContentDescription, dest, flags);
		TextUtils.writeToParcel(mStateDescription, dest, flags);
	}

	private void readFromParcel(Parcel source) {
		if (source.readByte() != 0) {
			mIcon = Icon.CREATOR.createFromParcel(source);
		} else {
			mIcon = null;
		}
		if (source.readByte() != 0) {
			mPendingIntent = PendingIntent.CREATOR.createFromParcel(source);
		} else {
			mPendingIntent = null;
		}
		mState = source.readInt();
		mLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
		mSubtitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
		mContentDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
		mStateDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
	}

	public static final Creator<Tile> CREATOR = new Creator<Tile>() {
		@Override
		public Tile createFromParcel(Parcel source) {
			return new Tile(source);
		}

		@Override
		public Tile[] newArray(int size) {
			return new Tile[size];
		}
	};
}
