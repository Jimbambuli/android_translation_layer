package android.app.wallpaper;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/* AOSP android.app.wallpaper.WallpaperDescription (Android 16): the immutable description of one wallpaper instance of a
 * wallpaper service. Without the XML storage (hidden) and the Map<Point, Rect> crop hints (WallpaperManager.getOrientation()
 * is not in ATL); the crop hints are kept by orientation as in AOSP. */
public final class WallpaperDescription implements Parcelable {
	/* WallpaperManager.ORIENTATION_UNKNOWN, _PORTRAIT, _LANDSCAPE, _SQUARE_PORTRAIT, _SQUARE_LANDSCAPE (parcel order) */
	private static final int[] ORIENTATIONS = {-1, 0, 1, 2, 3};

	@Nullable private final ComponentName mComponent;
	@Nullable private final String mId;
	@Nullable private final Uri mThumbnail;
	@Nullable private final CharSequence mTitle;
	@NonNull private final List<CharSequence> mDescription;
	@Nullable private final Uri mContextUri;
	@Nullable private final CharSequence mContextDescription;
	@NonNull private final PersistableBundle mContent;
	@NonNull private final SparseArray<Rect> mCropHints;
	private final float mSampleSize;

	private WallpaperDescription(@Nullable ComponentName component, @Nullable String id, @Nullable Uri thumbnail,
			@Nullable CharSequence title, @Nullable List<CharSequence> description, @Nullable Uri contextUri,
			@Nullable CharSequence contextDescription, @Nullable PersistableBundle content,
			@NonNull SparseArray<Rect> cropHints, float sampleSize) {
		this.mComponent = component;
		this.mId = id;
		this.mThumbnail = thumbnail;
		this.mTitle = title;
		this.mDescription = (description != null) ? description : new ArrayList<>();
		this.mContextUri = contextUri;
		this.mContextDescription = contextDescription;
		this.mContent = (content != null) ? content : new PersistableBundle();
		this.mCropHints = cropHints;
		this.mSampleSize = sampleSize;
	}

	@Nullable public ComponentName getComponent() { return mComponent; }
	@Nullable public String getId() { return mId; }
	@Nullable public Uri getThumbnail() { return mThumbnail; }
	@Nullable public CharSequence getTitle() { return mTitle; }
	@NonNull public List<CharSequence> getDescription() { return mDescription; }
	@Nullable public Uri getContextUri() { return mContextUri; }
	@Nullable public CharSequence getContextDescription() { return mContextDescription; }
	@NonNull public PersistableBundle getContent() { return mContent; }
	@NonNull public SparseArray<Rect> getCropHints() { return mCropHints; }
	public float getSampleSize() { return mSampleSize; }

	@Override
	public String toString() {
		String component = (mComponent != null) ? mComponent.toString() : "{null}";
		return component + ":" + mId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof WallpaperDescription)) return false;
		WallpaperDescription that = (WallpaperDescription) o;
		return Objects.equals(mComponent, that.mComponent) && Objects.equals(mId, that.mId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mComponent, mId);
	}

	WallpaperDescription(@NonNull Parcel in) {
		mComponent = ComponentName.readFromParcel(in);
		mId = in.readString8();
		mThumbnail = Uri.CREATOR.createFromParcel(in);
		mTitle = in.readCharSequence();
		mDescription = new ArrayList<>(Arrays.asList(in.readCharSequenceArray()));
		mContextUri = Uri.CREATOR.createFromParcel(in);
		mContextDescription = in.readCharSequence();
		mContent = PersistableBundle.CREATOR.createFromParcel(in);
		mCropHints = new SparseArray<>();
		for (int orientation : ORIENTATIONS) {
			Rect crop = in.readTypedObject(Rect.CREATOR);
			if (crop != null) mCropHints.put(orientation, crop);
		}
		mSampleSize = in.readFloat();
	}

	@NonNull
	public static final Creator<WallpaperDescription> CREATOR = new Creator<WallpaperDescription>() {
		@Override
		public WallpaperDescription createFromParcel(Parcel source) {
			return new WallpaperDescription(source);
		}

		@Override
		public WallpaperDescription[] newArray(int size) {
			return new WallpaperDescription[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		ComponentName.writeToParcel(mComponent, dest);
		dest.writeString8(mId);
		Uri.writeToParcel(dest, mThumbnail);
		dest.writeCharSequence(mTitle);
		dest.writeCharSequenceArray(mDescription.toArray(new CharSequence[0]));
		Uri.writeToParcel(dest, mContextUri);
		dest.writeCharSequence(mContextDescription);
		dest.writePersistableBundle(mContent);
		for (int orientation : ORIENTATIONS) {
			dest.writeTypedObject(mCropHints.get(orientation), flags);
		}
		dest.writeFloat(mSampleSize);
	}

	@NonNull
	public Builder toBuilder() {
		return new Builder()
				.setComponent(mComponent)
				.setId(mId)
				.setThumbnail(mThumbnail)
				.setTitle(mTitle)
				.setDescription(mDescription)
				.setContextUri(mContextUri)
				.setContextDescription(mContextDescription)
				.setContent(mContent)
				.setCropHints(mCropHints)
				.setSampleSize(mSampleSize);
	}

	public static final class Builder {
		@Nullable private ComponentName mComponent;
		@Nullable private String mId;
		@Nullable private Uri mThumbnail;
		@Nullable private CharSequence mTitle;
		@NonNull private List<CharSequence> mDescription = new ArrayList<>();
		@Nullable private Uri mContextUri;
		@Nullable private CharSequence mContextDescription;
		@NonNull private PersistableBundle mContent = new PersistableBundle();
		@NonNull private SparseArray<Rect> mCropHints = new SparseArray<>();
		private float mSampleSize = 1f;

		public Builder() {}

		@NonNull public Builder setComponent(@Nullable ComponentName component) { mComponent = component; return this; }
		@NonNull public Builder setId(@Nullable String id) { mId = id; return this; }
		@NonNull public Builder setThumbnail(@Nullable Uri thumbnail) { mThumbnail = thumbnail; return this; }
		@NonNull public Builder setTitle(@Nullable CharSequence title) { mTitle = title; return this; }
		@NonNull public Builder setDescription(@NonNull List<CharSequence> description) { mDescription = description; return this; }
		@NonNull public Builder setContextUri(@Nullable Uri contextUri) { mContextUri = contextUri; return this; }
		@NonNull public Builder setContextDescription(@Nullable CharSequence contextDescription) { mContextDescription = contextDescription; return this; }
		@NonNull public Builder setContent(@NonNull PersistableBundle content) { mContent = content; return this; }
		@NonNull public Builder setCropHints(@NonNull SparseArray<Rect> cropHints) { mCropHints = cropHints; return this; }
		@NonNull public Builder setSampleSize(float sampleSize) { mSampleSize = sampleSize; return this; }

		@NonNull
		public WallpaperDescription build() {
			return new WallpaperDescription(mComponent, mId, mThumbnail, mTitle, mDescription,
					mContextUri, mContextDescription, mContent, mCropHints, mSampleSize);
		}
	}
}
