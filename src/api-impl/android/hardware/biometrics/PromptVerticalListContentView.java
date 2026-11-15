package android.hardware.biometrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

public final class PromptVerticalListContentView implements PromptContentViewParcelable {
	private static final String TAG = "PromptVerticalListContentView";

	static final int MAX_ITEM_NUMBER = 20;

	static final int MAX_EACH_ITEM_CHARACTER_NUMBER = 640;

	static final int MAX_DESCRIPTION_CHARACTER_NUMBER = 225;

	private final List<PromptContentItemParcelable> mContentList;
	private final String mDescription;

	private PromptVerticalListContentView(
			List<PromptContentItemParcelable> contentList,
			String description) {
		mContentList = contentList;
		mDescription = description;
	}

	private PromptVerticalListContentView(Parcel in) {
		mContentList = in.readArrayList(
				PromptContentItemParcelable.class.getClassLoader(),
				PromptContentItemParcelable.class);
		mDescription = in.readString();
	}

	public static int getMaxItemCount() {
		return MAX_ITEM_NUMBER;
	}

	public static int getMaxEachItemCharacterNumber() {
		return MAX_EACH_ITEM_CHARACTER_NUMBER;
	}

	public String getDescription() {
		return mDescription;
	}

	public List<PromptContentItem> getListItems() {
		return new ArrayList<>(mContentList);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
		dest.writeList(mContentList);
		dest.writeString(mDescription);
	}

	public static final Creator<PromptVerticalListContentView> CREATOR = new Creator<>() {
		@Override
		public PromptVerticalListContentView createFromParcel(Parcel in) {
			return new PromptVerticalListContentView(in);
		}

		@Override
		public PromptVerticalListContentView[] newArray(int size) {
			return new PromptVerticalListContentView[size];
		}
	};

	public static final class Builder {
		private final List<PromptContentItemParcelable> mContentList = new ArrayList<>();
		private String mDescription;

		public Builder setDescription(String description) {
			if (description.length() > MAX_DESCRIPTION_CHARACTER_NUMBER) {
				Log.w(TAG, "The character number of description exceeds "
						+ MAX_DESCRIPTION_CHARACTER_NUMBER);
			}
			mDescription = description;
			return this;
		}

		public Builder addListItem(PromptContentItem listItem) {
			mContentList.add((PromptContentItemParcelable) listItem);
			checkItemLimits(listItem);
			return this;
		}

		public Builder addListItem(PromptContentItem listItem, int index) {
			mContentList.add(index, (PromptContentItemParcelable) listItem);
			checkItemLimits(listItem);
			return this;
		}

		private void checkItemLimits(PromptContentItem listItem) {
			if (doesListItemExceedsCharLimit(listItem)) {
				Log.w(TAG, "The character number of list item exceeds "
						+ MAX_EACH_ITEM_CHARACTER_NUMBER);
			}
			if (mContentList.size() > MAX_ITEM_NUMBER) {
				throw new IllegalArgumentException(
						"The number of list items exceeds " + MAX_ITEM_NUMBER);
			}
		}

		private boolean doesListItemExceedsCharLimit(PromptContentItem listItem) {
			if (listItem instanceof PromptContentItemPlainText) {
				return ((PromptContentItemPlainText) listItem).getText().length()
						> MAX_EACH_ITEM_CHARACTER_NUMBER;
			} else if (listItem instanceof PromptContentItemBulletedText) {
				return ((PromptContentItemBulletedText) listItem).getText().length()
						> MAX_EACH_ITEM_CHARACTER_NUMBER;
			} else {
				return false;
			}
		}

		public PromptVerticalListContentView build() {
			return new PromptVerticalListContentView(mContentList, mDescription);
		}
	}
}
