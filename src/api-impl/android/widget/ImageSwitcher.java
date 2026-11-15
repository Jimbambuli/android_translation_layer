package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

/* AOSP ImageSwitcher (frameworks/base/core/java/android/widget/ImageSwitcher.java) */
public class ImageSwitcher extends ViewSwitcher {
	public ImageSwitcher(Context context) {
		super(context);
	}

	public ImageSwitcher(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setImageResource(int resid) {
		ImageView image = (ImageView) this.getNextView();
		image.setImageResource(resid);
		showNext();
	}

	public void setImageURI(Uri uri) {
		ImageView image = (ImageView) this.getNextView();
		image.setImageURI(uri);
		showNext();
	}

	public void setImageDrawable(Drawable drawable) {
		ImageView image = (ImageView) this.getNextView();
		image.setImageDrawable(drawable);
		showNext();
	}
}
