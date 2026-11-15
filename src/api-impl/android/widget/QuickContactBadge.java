package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.QuickContact;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.internal.R;

/* AOSP QuickContactBadge (frameworks/base/core/java/android/widget/QuickContactBadge.java). The lookups by e-mail address or
 * phone number (AsyncQueryHandler on ContactsContract) are not made: a device without a contacts provider finds no contact, so
 * assignContactFromEmail()/assignContactFromPhone() leave the contact Uri empty and a click on a badge that was assigned
 * that way does nothing. */
public class QuickContactBadge extends ImageView implements OnClickListener {
	private Uri mContactUri;
	private String mContactEmail;
	private String mContactPhone;
	private Drawable mOverlay;
	private Drawable mDefaultAvatar;
	private Bundle mExtras = null;
	private String mPrioritizedMimeType;
	protected String[] mExcludeMimes = null;

	public QuickContactBadge(Context context) {
		this(context, null);
	}

	public QuickContactBadge(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QuickContactBadge(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public QuickContactBadge(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray styledAttributes = context.obtainStyledAttributes(R.styleable.Theme);
		mOverlay = styledAttributes.getDrawable(com.android.internal.R.styleable.Theme_quickContactBadgeOverlay);
		styledAttributes.recycle();

		setOnClickListener(this);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		final Drawable overlay = mOverlay;
		if (overlay != null && overlay.isStateful() && overlay.setState(getDrawableState())) {
			invalidateDrawable(overlay);
		}
	}

	@Override
	public void drawableHotspotChanged(float x, float y) {
		super.drawableHotspotChanged(x, y);
		if (mOverlay != null) {
			mOverlay.setHotspot(x, y);
		}
	}

	/** This call has no effect anymore, as there is only one QuickContact mode */
	public void setMode(int size) {
	}

	public void setPrioritizedMimeType(String prioritizedMimeType) {
		mPrioritizedMimeType = prioritizedMimeType;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (!isEnabled()) {
			// Not enabled, so don't draw the overlay
			return;
		}

		if (mOverlay == null || mOverlay.getIntrinsicWidth() == 0 || mOverlay.getIntrinsicHeight() == 0) {
			return;
		}

		mOverlay.setBounds(0, 0, getWidth(), getHeight());

		if (getPaddingTop() == 0 && getPaddingLeft() == 0) {
			mOverlay.draw(canvas);
		} else {
			int saveCount = canvas.getSaveCount();
			canvas.save();
			canvas.translate(getPaddingLeft(), getPaddingTop());
			mOverlay.draw(canvas);
			canvas.restoreToCount(saveCount);
		}
	}

	private boolean isAssigned() {
		return mContactUri != null || mContactEmail != null || mContactPhone != null;
	}

	public void setImageToDefault() {
		if (mDefaultAvatar == null) {
			mDefaultAvatar = getContext().getDrawable(R.drawable.ic_contact_picture);
		}
		setImageDrawable(mDefaultAvatar);
	}

	public void assignContactUri(Uri contactUri) {
		mContactUri = contactUri;
		mContactEmail = null;
		mContactPhone = null;
		onContactUriChanged();
	}

	public void assignContactFromEmail(String emailAddress, boolean lazyLookup) {
		assignContactFromEmail(emailAddress, lazyLookup, null);
	}

	public void assignContactFromEmail(String emailAddress, boolean lazyLookup, Bundle extras) {
		mContactEmail = emailAddress;
		mExtras = extras;
		mContactUri = null;
		onContactUriChanged();
	}

	public void assignContactFromPhone(String phoneNumber, boolean lazyLookup) {
		assignContactFromPhone(phoneNumber, lazyLookup, new Bundle());
	}

	public void assignContactFromPhone(String phoneNumber, boolean lazyLookup, Bundle extras) {
		mContactPhone = phoneNumber;
		mExtras = extras;
		mContactUri = null;
		onContactUriChanged();
	}

	public void setOverlay(Drawable overlay) {
		mOverlay = overlay;
	}

	private void onContactUriChanged() {
		setEnabled(isAssigned());
	}

	@Override
	public void onClick(View v) {
		if (mContactUri != null) {
			QuickContact.showQuickContact(getContext(), QuickContactBadge.this, mContactUri, mExcludeMimes, mPrioritizedMimeType);
		}
	}

	public void setExcludeMimes(String[] excludeMimes) {
		mExcludeMimes = excludeMimes;
	}
}
