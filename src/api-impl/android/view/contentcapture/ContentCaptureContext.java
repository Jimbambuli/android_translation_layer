package android.view.contentcapture;

import android.app.assist.ActivityId;
import android.content.ComponentName;
import android.content.LocusId;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Display;
import com.android.internal.util.Preconditions;
import java.util.Objects;

/* AOSP ContentCaptureContext: the context of a session, set by the app (locus id, extras) or by the service when
 * the session starts (activity, display, window, flags) */
public final class ContentCaptureContext implements Parcelable {
	private static final int NO_SESSION_ID = 0; // AOSP ContentCaptureSession.NO_SESSION_ID

	private final boolean mHasClientContext;

	// Fields below are set by app on Builder
	private final Bundle mExtras;
	private final LocusId mId;

	// Fields below are set by server when the session starts
	private final ComponentName mComponentName;
	private final int mFlags;
	private final int mDisplayId;
	private final ActivityId mActivityId;
	private final IBinder mWindowToken;

	// Fields below are set by the service upon "delivery" and are not marshalled in the parcel
	private int mParentSessionId = NO_SESSION_ID;

	/** @hide */
	public ContentCaptureContext(ContentCaptureContext clientContext, ActivityId activityId,
	                             ComponentName componentName, int displayId, IBinder windowToken, int flags) {
		if (clientContext != null) {
			mHasClientContext = true;
			mExtras = clientContext.mExtras;
			mId = clientContext.mId;
		} else {
			mHasClientContext = false;
			mExtras = null;
			mId = null;
		}
		mComponentName = Objects.requireNonNull(componentName);
		mFlags = flags;
		mDisplayId = displayId;
		mActivityId = activityId;
		mWindowToken = windowToken;
	}

	private ContentCaptureContext(Builder builder) {
		mHasClientContext = true;
		mExtras = builder.mExtras;
		mId = builder.mId;

		mComponentName = null;
		mFlags = 0;
		mDisplayId = Display.INVALID_DISPLAY;
		mActivityId = null;
		mWindowToken = null;
	}

	public Bundle getExtras() {
		return mExtras;
	}

	public LocusId getLocusId() {
		return mId;
	}

	public static ContentCaptureContext forLocusId(String id) {
		return new Builder(new LocusId(id)).build();
	}

	public static final class Builder {
		private Bundle mExtras;
		private final LocusId mId;
		private boolean mDestroyed;

		public Builder(LocusId id) {
			mId = Objects.requireNonNull(id);
		}

		public Builder setExtras(Bundle extras) {
			mExtras = Objects.requireNonNull(extras);
			throwIfDestroyed();
			return this;
		}

		public ContentCaptureContext build() {
			throwIfDestroyed();
			mDestroyed = true;
			return new ContentCaptureContext(this);
		}

		private void throwIfDestroyed() {
			Preconditions.checkState(!mDestroyed, "Already called #build()");
		}
	}

	private boolean fromServer() {
		return mComponentName != null;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("Context[");
		if (fromServer()) {
			builder.append("act=").append(mComponentName.flattenToShortString())
			    .append(", activityId=").append(mActivityId)
			    .append(", displayId=").append(mDisplayId)
			    .append(", windowToken=").append(mWindowToken)
			    .append(", flags=").append(mFlags);
		} else {
			builder.append("id=").append(mId);
			if (mExtras != null) {
				// NOTE: cannot print because it could contain PII
				builder.append(", hasExtras");
			}
		}
		if (mParentSessionId != NO_SESSION_ID) {
			builder.append(", parentId=").append(mParentSessionId);
		}
		return builder.append(']').toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mHasClientContext ? 1 : 0);
		if (mHasClientContext) {
			parcel.writeParcelable(mId, flags);
			parcel.writeBundle(mExtras);
		}
		parcel.writeParcelable(mComponentName, flags);
		if (fromServer()) {
			parcel.writeInt(mDisplayId);
			parcel.writeStrongBinder(mWindowToken);
			parcel.writeInt(mFlags);
			mActivityId.writeToParcel(parcel, flags);
		}
	}

	public static final Parcelable.Creator<ContentCaptureContext> CREATOR = new Parcelable.Creator<ContentCaptureContext>() {
		@Override
		public ContentCaptureContext createFromParcel(Parcel parcel) {
			final boolean hasClientContext = parcel.readInt() == 1;

			final ContentCaptureContext clientContext;
			if (hasClientContext) {
				// Must reconstruct the client context using the Builder API
				final LocusId id = parcel.readParcelable(null, LocusId.class);
				final Bundle extras = parcel.readBundle();
				final Builder builder = new Builder(id);
				if (extras != null)
					builder.setExtras(extras);
				clientContext = new ContentCaptureContext(builder);
			} else {
				clientContext = null;
			}
			final ComponentName componentName = parcel.readParcelable(null, ComponentName.class);
			if (componentName == null) {
				// Client-state only
				return clientContext;
			} else {
				final int displayId = parcel.readInt();
				final IBinder windowToken = parcel.readStrongBinder();
				final int flags = parcel.readInt();
				final ActivityId activityId = new ActivityId(parcel);

				return new ContentCaptureContext(clientContext, activityId, componentName,
				                                 displayId, windowToken, flags);
			}
		}

		@Override
		public ContentCaptureContext[] newArray(int size) {
			return new ContentCaptureContext[size];
		}
	};
}
