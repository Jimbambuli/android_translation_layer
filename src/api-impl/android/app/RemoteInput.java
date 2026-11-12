package android.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteInput implements Parcelable {

	/* AOSP: the results travel in the ClipData of the intent, as an intent with this label */
	public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";
	public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";
	private static final String EXTRA_DATA_TYPE_RESULTS_DATA = "android.remoteinput.dataTypeResultsData";
	private static final String EXTRA_RESULTS_SOURCE = "android.remoteinput.resultsSource";

	public static final int SOURCE_FREE_FORM_INPUT = 0;
	public static final int SOURCE_CHOICE = 1;

	public static final int EDIT_CHOICES_BEFORE_SENDING_AUTO = 0;
	public static final int EDIT_CHOICES_BEFORE_SENDING_DISABLED = 1;
	public static final int EDIT_CHOICES_BEFORE_SENDING_ENABLED = 2;

	private static final int FLAG_ALLOW_FREE_FORM_INPUT = 0x1;

	private final String mResultKey;
	private final CharSequence mLabel;
	private final CharSequence[] mChoices;
	private final int mFlags;
	private final int mEditChoicesBeforeSending;
	private final Bundle mExtras;
	private final ArraySet<String> mAllowedDataTypes;

	private RemoteInput(String resultKey, CharSequence label, CharSequence[] choices, int flags, int editChoicesBeforeSending,
	                    Bundle extras, ArraySet<String> allowedDataTypes) {
		mResultKey = resultKey;
		mLabel = label;
		mChoices = choices;
		mFlags = flags;
		mEditChoicesBeforeSending = editChoicesBeforeSending;
		mExtras = extras;
		mAllowedDataTypes = allowedDataTypes;
		if (getEditChoicesBeforeSending() == EDIT_CHOICES_BEFORE_SENDING_ENABLED && !getAllowFreeFormInput()) {
			throw new IllegalArgumentException("setEditChoicesBeforeSending requires setAllowFreeFormInput");
		}
	}

	public String getResultKey() {
		return mResultKey;
	}

	public CharSequence getLabel() {
		return mLabel;
	}

	public CharSequence[] getChoices() {
		return mChoices;
	}

	public Set<String> getAllowedDataTypes() {
		return mAllowedDataTypes;
	}

	public boolean isDataOnly() {
		return !getAllowFreeFormInput() && (getChoices() == null || getChoices().length == 0) && !getAllowedDataTypes().isEmpty();
	}

	public boolean getAllowFreeFormInput() {
		return (mFlags & FLAG_ALLOW_FREE_FORM_INPUT) != 0;
	}

	public int getEditChoicesBeforeSending() {
		return mEditChoicesBeforeSending;
	}

	public Bundle getExtras() {
		return mExtras;
	}

	public static class Builder {
		private final String mResultKey;
		private final Bundle mExtras = new Bundle();
		private final ArraySet<String> mAllowedDataTypes = new ArraySet<>();
		private CharSequence mLabel;
		private CharSequence[] mChoices;
		private int mFlags = FLAG_ALLOW_FREE_FORM_INPUT;
		private int mEditChoicesBeforeSending = EDIT_CHOICES_BEFORE_SENDING_AUTO;

		public Builder(String resultKey) {
			if (resultKey == null) {
				throw new IllegalArgumentException("Result key can't be null");
			}
			mResultKey = resultKey;
		}

		public Builder setLabel(CharSequence label) {
			mLabel = label;
			return this;
		}

		public Builder setChoices(CharSequence[] choices) {
			mChoices = choices != null ? choices.clone() : null;
			return this;
		}

		public Builder setAllowFreeFormInput(boolean allowFreeFormInput) {
			if (allowFreeFormInput) {
				mFlags |= FLAG_ALLOW_FREE_FORM_INPUT;
			} else {
				mFlags &= ~FLAG_ALLOW_FREE_FORM_INPUT;
			}
			return this;
		}

		public Builder setAllowDataType(String mimeType, boolean doAllow) {
			if (doAllow) {
				mAllowedDataTypes.add(mimeType);
			} else {
				mAllowedDataTypes.remove(mimeType);
			}
			return this;
		}

		public Builder setEditChoicesBeforeSending(int editChoicesBeforeSending) {
			mEditChoicesBeforeSending = editChoicesBeforeSending;
			return this;
		}

		public Bundle getExtras() {
			return mExtras;
		}

		public Builder addExtras(Bundle extras) {
			if (extras != null) {
				mExtras.putAll(extras);
			}
			return this;
		}

		public RemoteInput build() {
			return new RemoteInput(mResultKey, mLabel, mChoices, mFlags, mEditChoicesBeforeSending, mExtras, mAllowedDataTypes);
		}
	}

	/* AOSP RemoteInput(Parcel) / writeToParcel() / CREATOR, wire format of android-14 */
	@SuppressWarnings("unchecked")
	private RemoteInput(Parcel in) {
		mResultKey = in.readString();
		mLabel = in.readCharSequence();
		mChoices = in.readCharSequenceArray();
		mFlags = in.readInt();
		mEditChoicesBeforeSending = in.readInt();
		mExtras = in.readBundle();
		mAllowedDataTypes = (ArraySet<String>)in.readArraySet(null);
	}

	/* AOSP getDataResultsFromIntent() .. getResultsSource() */
	public static Map<String, Uri> getDataResultsFromIntent(Intent intent, String remoteInputResultKey) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			return null;
		}
		Map<String, Uri> results = new HashMap<>();
		Bundle extras = clipDataIntent.getExtras();
		for (String key : extras.keySet()) {
			if (key.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA)) {
				String mimeType = key.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length());
				if (mimeType.isEmpty()) {
					continue;
				}
				String uriStr = clipDataIntent.getBundleExtra(key).getString(remoteInputResultKey);
				if (uriStr == null || uriStr.isEmpty()) {
					continue;
				}
				results.put(mimeType, Uri.parse(uriStr));
			}
		}
		return results.isEmpty() ? null : results;
	}

	public static Bundle getResultsFromIntent(Intent intent) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			return null;
		}
		return clipDataIntent.getExtras().getParcelable(EXTRA_RESULTS_DATA, Bundle.class);
	}

	public static void addResultsToIntent(RemoteInput[] remoteInputs, Intent intent, Bundle results) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			clipDataIntent = new Intent(); // First time we've added a result.
		}
		Bundle resultsBundle = clipDataIntent.getBundleExtra(EXTRA_RESULTS_DATA);
		if (resultsBundle == null) {
			resultsBundle = new Bundle();
		}
		for (RemoteInput remoteInput : remoteInputs) {
			Object result = results.get(remoteInput.getResultKey());
			if (result instanceof CharSequence) {
				resultsBundle.putCharSequence(remoteInput.getResultKey(), (CharSequence)result);
			}
		}
		clipDataIntent.putExtra(EXTRA_RESULTS_DATA, resultsBundle);
		intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
	}

	public static void addDataResultToIntent(RemoteInput remoteInput, Intent intent, Map<String, Uri> results) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			clipDataIntent = new Intent(); // First time we've added a result.
		}
		for (Map.Entry<String, Uri> entry : results.entrySet()) {
			String mimeType = entry.getKey();
			if (mimeType == null) {
				continue;
			}
			Bundle resultsBundle = clipDataIntent.getBundleExtra(EXTRA_DATA_TYPE_RESULTS_DATA + mimeType);
			if (resultsBundle == null) {
				resultsBundle = new Bundle();
			}
			resultsBundle.putString(remoteInput.getResultKey(), entry.getValue().toString());
			clipDataIntent.putExtra(EXTRA_DATA_TYPE_RESULTS_DATA + mimeType, resultsBundle);
		}
		intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
	}

	public static void setResultsSource(Intent intent, int source) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			clipDataIntent = new Intent(); // First time we've added a result.
		}
		clipDataIntent.putExtra(EXTRA_RESULTS_SOURCE, source);
		intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
	}

	public static int getResultsSource(Intent intent) {
		Intent clipDataIntent = getClipDataIntentFromIntent(intent);
		if (clipDataIntent == null) {
			return SOURCE_FREE_FORM_INPUT;
		}
		return clipDataIntent.getExtras().getInt(EXTRA_RESULTS_SOURCE, SOURCE_FREE_FORM_INPUT);
	}

	private static Intent getClipDataIntentFromIntent(Intent intent) {
		ClipData clipData = intent.getClipData();
		if (clipData == null) {
			return null;
		}
		ClipDescription clipDescription = clipData.getDescription();
		if (!clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) {
			return null;
		}
		if (!clipDescription.getLabel().equals(RESULTS_CLIP_LABEL)) {
			return null;
		}
		return clipData.getItemAt(0).getIntent();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mResultKey);
		out.writeCharSequence(mLabel);
		out.writeCharSequenceArray(mChoices);
		out.writeInt(mFlags);
		out.writeInt(mEditChoicesBeforeSending);
		out.writeBundle(mExtras);
		out.writeArraySet(mAllowedDataTypes);
	}

	public static final Creator<RemoteInput> CREATOR = new Creator<RemoteInput>() {
		@Override
		public RemoteInput createFromParcel(Parcel in) {
			return new RemoteInput(in);
		}

		@Override
		public RemoteInput[] newArray(int size) {
			return new RemoteInput[size];
		}
	};
}
