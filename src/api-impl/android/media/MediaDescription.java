package android.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

public class MediaDescription {

	public Uri iconUri;
	public CharSequence title;
	public CharSequence subtitle;
	private String media_id;
	private CharSequence description;
	private Bitmap icon_bitmap;
	private Bundle extras;
	private Uri media_uri;

	public String getMediaId() {
		return media_id;
	}

	public CharSequence getTitle() {
		return title;
	}

	public CharSequence getSubtitle() {
		return subtitle;
	}

	public CharSequence getDescription() {
		return description;
	}

	public Bitmap getIconBitmap() {
		return icon_bitmap;
	}

	public Uri getIconUri() {
		return iconUri;
	}

	public Bundle getExtras() {
		return extras;
	}

	public Uri getMediaUri() {
		return media_uri;
	}

	public static class Builder {

		MediaDescription description = new MediaDescription();

		public Builder setMediaId(String mediaId) {
			description.media_id = mediaId;
			return this;
		}

		public Builder setTitle(CharSequence title) {
			description.title = title;
			return this;
		}

		public Builder setSubtitle(CharSequence subtitle) {
			description.subtitle = subtitle;
			return this;
		}

		public Builder setDescription(CharSequence description) {
			this.description.description = description;
			return this;
		}

		public Builder setIconBitmap(Bitmap iconBitmap) {
			description.icon_bitmap = iconBitmap;
			return this;
		}

		public Builder setIconUri(Uri iconUri) {
			description.iconUri = iconUri;
			return this;
		}

		public Builder setExtras(Bundle extras) {
			description.extras = extras;
			return this;
		}

		public Builder setMediaUri(Uri mediaUri) {
			description.media_uri = mediaUri;
			return this;
		}

		public MediaDescription build() {
			return description;
		}
	}
}
