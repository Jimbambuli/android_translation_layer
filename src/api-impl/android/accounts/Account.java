package android.accounts;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Account implements Parcelable {
	public final String name;
	public final String type;

	/* AOSP @hide accessId: set only by system_server for accounts handed out with an access grant;
	 * ATL has no such grant, so it stays null but travels in the parcel like on AOSP */
	private final String accessId;

	public Account(String name, String type) {
		if (TextUtils.isEmpty(name)) {
			throw new IllegalArgumentException("the name must not be empty: " + name);
		}
		if (TextUtils.isEmpty(type)) {
			throw new IllegalArgumentException("the type must not be empty: " + type);
		}
		this.name = name;
		this.type = type;
		this.accessId = null;
	}

	public Account(Parcel in) {
		this.name = in.readString();
		this.type = in.readString();
		if (TextUtils.isEmpty(name)) {
			throw new android.os.BadParcelableException("the name must not be empty: " + name);
		}
		if (TextUtils.isEmpty(type)) {
			throw new android.os.BadParcelableException("the type must not be empty: " + type);
		}
		this.accessId = in.readString();
	}

	/* AOSP Account.describeContents() */
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(type);
		dest.writeString(accessId);
	}

	public static final Creator<Account> CREATOR = new Creator<Account>() {
		public Account createFromParcel(Parcel source) {
			return new Account(source);
		}

		public Account[] newArray(int size) {
			return new Account[size];
		}
	};

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Account))
			return false;
		final Account other = (Account)o;
		return name.equals(other.name) && type.equals(other.type);
	}

	public int hashCode() {
		int result = 17;
		result = 31 * result + name.hashCode();
		result = 31 * result + type.hashCode();
		return result;
	}

	private String mSafeName;

	/* AOSP Account.toSafeString(): the account for logs, with letters and digits of the name replaced */
	public String toSafeString() {
		if (mSafeName == null) {
			mSafeName = toSafeName(name, 'x');
		}
		return "Account {name=" + mSafeName + ", type=" + type + "}";
	}

	/* AOSP Account.toSafeName() */
	public static String toSafeName(String name, char replacement) {
		final StringBuilder builder = new StringBuilder(64);
		final int len = name.length();
		for (int i = 0; i < len; i++) {
			final char c = name.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				builder.append(replacement);
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
