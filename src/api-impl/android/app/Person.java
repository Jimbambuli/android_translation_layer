package android.app;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {

	private CharSequence mName;
	private Icon mIcon;
	private String mUri;
	private String mKey;
	private boolean mIsBot;
	private boolean mIsImportant;

	private Person(Builder builder) {
		mName = builder.mName;
		mIcon = builder.mIcon;
		mUri = builder.mUri;
		mKey = builder.mKey;
		mIsBot = builder.mIsBot;
		mIsImportant = builder.mIsImportant;
	}

	/* AOSP Person(Parcel) / writeToParcel() / CREATOR (notifications keep persons in their extras) */
	private Person(Parcel in) {
		mName = in.readCharSequence();
		if (in.readInt() != 0) {
			mIcon = Icon.CREATOR.createFromParcel(in);
		}
		mUri = in.readString();
		mKey = in.readString();
		mIsImportant = in.readBoolean();
		mIsBot = in.readBoolean();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeCharSequence(mName);
		if (mIcon != null) {
			dest.writeInt(1);
			mIcon.writeToParcel(dest, 0);
		} else {
			dest.writeInt(0);
		}
		dest.writeString(mUri);
		dest.writeString(mKey);
		dest.writeBoolean(mIsImportant);
		dest.writeBoolean(mIsBot);
	}

	public static final Creator<Person> CREATOR = new Creator<Person>() {
		@Override
		public Person createFromParcel(Parcel in) {
			return new Person(in);
		}

		@Override
		public Person[] newArray(int size) {
			return new Person[size];
		}
	};

	public String getUri() {
		return mUri;
	}

	public CharSequence getName() {
		return mName;
	}

	public Icon getIcon() {
		return mIcon;
	}

	public String getKey() {
		return mKey;
	}

	public boolean isBot() {
		return mIsBot;
	}

	public boolean isImportant() {
		return mIsImportant;
	}

	public static class Builder {

		private CharSequence mName;
		private Icon mIcon;
		private String mUri;
		private String mKey;
		private boolean mIsBot;
		private boolean mIsImportant;

		public Builder() {
		}

		public Person build() {
			return new Person(this);
		}

		public Builder setName(CharSequence name) {
			mName = name;
			return this;
		}

		public Builder setIcon(Icon icon) {
			mIcon = icon;
			return this;
		}

		public Builder setUri(String uri) {
			mUri = uri;
			return this;
		}

		public Builder setKey(String key) {
			mKey = key;
			return this;
		}

		public Builder setImportant(boolean isImportant) {
			mIsImportant = isImportant;
			return this;
		}

		public Builder setBot(boolean isBot) {
			mIsBot = isBot;
			return this;
		}
	}
}
