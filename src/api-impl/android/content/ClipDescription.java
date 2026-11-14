package android.content;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;

public class ClipDescription implements Parcelable {
	public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
	public static final String MIMETYPE_TEXT_HTML = "text/html";
	public static final String MIMETYPE_TEXT_URILIST = "text/uri-list";
	public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";
	public static final String MIMETYPE_APPLICATION_ACTIVITY = "application/vnd.android.activity";
	public static final String MIMETYPE_APPLICATION_SHORTCUT = "application/vnd.android.shortcut";
	public static final String MIMETYPE_APPLICATION_TASK = "application/vnd.android.task";
	public static final String MIMETYPE_UNKNOWN = "application/octet-stream";
	public static final String EXTRA_PENDING_INTENT = "android.intent.extra.PENDING_INTENT";
	public static final String EXTRA_ACTIVITY_OPTIONS = "android.intent.extra.ACTIVITY_OPTIONS";
	public static final String EXTRA_LOGGING_INSTANCE_ID = "android.intent.extra.LOGGING_INSTANCE_ID";
	public static final String EXTRA_HIDE_DRAG_SOURCE_TASK_ID = "android.intent.extra.HIDE_DRAG_SOURCE_TASK_ID";
	public static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";
	public static final String EXTRA_IS_REMOTE_DEVICE = "android.content.extra.IS_REMOTE_DEVICE";
	public static final int CLASSIFICATION_NOT_COMPLETE = 1;
	public static final int CLASSIFICATION_NOT_PERFORMED = 2;
	public static final int CLASSIFICATION_COMPLETE = 3;

	private final CharSequence label;
	private final String[] mimeTypes;
	private PersistableBundle extras;
	private long timeStamp;
	private boolean isStyledText;
	private int classificationStatus = CLASSIFICATION_NOT_COMPLETE;

	public ClipDescription(CharSequence label, String[] mimeTypes) {
		if (mimeTypes == null)
			throw new NullPointerException("mimeTypes is null");
		this.label = label;
		this.mimeTypes = mimeTypes;
		timeStamp = System.currentTimeMillis();
	}

	public ClipDescription(ClipDescription o) {
		this(o.label, o.mimeTypes);
		timeStamp = o.timeStamp;
	}

	public void setTimestamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public long getTimestamp() {
		return timeStamp;
	}

	public PersistableBundle getExtras() {
		return extras;
	}

	public void setExtras(PersistableBundle extras) {
		this.extras = new PersistableBundle(extras);
	}

	public boolean isStyledText() {
		return isStyledText;
	}

	public int getClassificationStatus() {
		return classificationStatus;
	}

	/* AOSP: scores exist only after classification; ATL has no text classifier, so a
	 * completed status (e.g. from a parcel) reports 0 for every entity */
	public float getConfidenceScore(String entity) {
		if (classificationStatus != CLASSIFICATION_COMPLETE)
			throw new IllegalStateException("Confidence scores not yet computed");
		return 0f;
	}

	/* AOSP ClipDescription(Parcel) / writeToParcel() / CREATOR (android-14 wire format; ATL has no text
	 * classifier, so the confidence scores are an empty Bundle) */
	ClipDescription(Parcel in) {
		label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
		java.util.ArrayList<String> types = in.createStringArrayList();
		mimeTypes = types.toArray(new String[0]);
		extras = in.readPersistableBundle();
		timeStamp = in.readLong();
		isStyledText = in.readBoolean();
		classificationStatus = in.readInt();
		in.readBundle();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		TextUtils.writeToParcel(label, dest, flags);
		dest.writeStringList(java.util.Arrays.asList(mimeTypes));
		dest.writePersistableBundle(extras);
		dest.writeLong(timeStamp);
		dest.writeBoolean(isStyledText);
		dest.writeInt(classificationStatus);
		dest.writeBundle(new Bundle());
	}

	public static final Parcelable.Creator<ClipDescription> CREATOR = new Parcelable.Creator<ClipDescription>() {
		public ClipDescription createFromParcel(Parcel source) {
			return new ClipDescription(source);
		}

		public ClipDescription[] newArray(int size) {
			return new ClipDescription[size];
		}
	};

	/**
	 * Compares a MIME type with a filter that may contain wildcards ("text/*", "*&#47;*").
	 */
	public static boolean compareMimeTypes(String concreteType, String desiredType) {
		if (desiredType.equals("*/*") || desiredType.equals(concreteType))
			return true;
		if (desiredType.endsWith("/*")) {
			String prefix = desiredType.substring(0, desiredType.length() - 1);
			return concreteType.startsWith(prefix);
		}
		return false;
	}

	public CharSequence getLabel() {
		return label;
	}

	public boolean hasMimeType(String mimeType) {
		for (String type : mimeTypes)
			if (compareMimeTypes(type, mimeType))
				return true;
		return false;
	}

	public String[] filterMimeTypes(String mimeType) {
		java.util.ArrayList<String> result = new java.util.ArrayList<>();
		for (String type : mimeTypes)
			if (compareMimeTypes(type, mimeType))
				result.add(type);
		return result.isEmpty() ? null : result.toArray(new String[0]);
	}

	public int getMimeTypeCount() {
		return mimeTypes.length;
	}

	public String getMimeType(int index) {
		return mimeTypes[index];
	}
}
