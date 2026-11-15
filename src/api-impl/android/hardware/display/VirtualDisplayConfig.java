package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/* AOSP core/java/android/hardware/display/VirtualDisplayConfig.java, public API part. Parcel: the surface and the
 * brightness listener binder are not marshalled (ATL has no display service on the other end of a Parcel). */
public final class VirtualDisplayConfig implements Parcelable {
	private final String mName;
	private final int mWidth;
	private final int mHeight;
	private final int mDensityDpi;
	private final int mFlags;
	private final Surface mSurface;
	private final String mUniqueId;
	private final int mDisplayIdToMirror;
	private final boolean mWindowManagerMirroringEnabled;
	private final Set<String> mDisplayCategories;
	private final float mRequestedRefreshRate;
	private final boolean mIsHomeSupported;
	private final boolean mIgnoreActivitySizeRestrictions;
	private final float mDefaultBrightness;
	private final float mDimBrightness;
	private final BrightnessListener mBrightnessListener;
	private final Executor mBrightnessExecutor;

	private VirtualDisplayConfig(Builder b) {
		mName = b.mName;
		mWidth = b.mWidth;
		mHeight = b.mHeight;
		mDensityDpi = b.mDensityDpi;
		mFlags = b.mFlags;
		mSurface = b.mSurface;
		mUniqueId = b.mUniqueId;
		mDisplayIdToMirror = b.mDisplayIdToMirror;
		mWindowManagerMirroringEnabled = b.mWindowManagerMirroringEnabled;
		mDisplayCategories = new HashSet<>(b.mDisplayCategories);
		mRequestedRefreshRate = b.mRequestedRefreshRate;
		mIsHomeSupported = b.mIsHomeSupported;
		mIgnoreActivitySizeRestrictions = b.mIgnoreActivitySizeRestrictions;
		mDefaultBrightness = b.mDefaultBrightness;
		mDimBrightness = b.mDimBrightness;
		mBrightnessListener = b.mBrightnessListener;
		mBrightnessExecutor = b.mBrightnessExecutor;
	}

	public String getName() { return mName; }

	public int getWidth() { return mWidth; }

	public int getHeight() { return mHeight; }

	public int getDensityDpi() { return mDensityDpi; }

	public int getFlags() { return mFlags; }

	public Surface getSurface() { return mSurface; }

	public float getDefaultBrightness() { return mDefaultBrightness; }

	public float getDimBrightness() { return mDimBrightness; }

	public Set<String> getDisplayCategories() { return mDisplayCategories; }

	public float getRequestedRefreshRate() { return mRequestedRefreshRate; }

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeInt(mWidth);
		dest.writeInt(mHeight);
		dest.writeInt(mDensityDpi);
		dest.writeInt(mFlags);
		dest.writeString(mUniqueId);
		dest.writeInt(mDisplayIdToMirror);
		dest.writeInt(mWindowManagerMirroringEnabled ? 1 : 0);
		dest.writeStringArray(mDisplayCategories.toArray(new String[0]));
		dest.writeFloat(mRequestedRefreshRate);
		dest.writeInt(mIsHomeSupported ? 1 : 0);
		dest.writeInt(mIgnoreActivitySizeRestrictions ? 1 : 0);
		dest.writeFloat(mDefaultBrightness);
		dest.writeFloat(mDimBrightness);
	}

	@Override
	public int describeContents() { return 0; }

	public static final Parcelable.Creator<VirtualDisplayConfig> CREATOR = new Parcelable.Creator<VirtualDisplayConfig>() {
		@Override
		public VirtualDisplayConfig[] newArray(int size) {
			return new VirtualDisplayConfig[size];
		}

		@Override
		public VirtualDisplayConfig createFromParcel(Parcel in) {
			Builder b = new Builder(in.readString(), Math.max(1, in.readInt()), Math.max(1, in.readInt()), Math.max(1, in.readInt()));
			b.setFlags(in.readInt());
			b.mUniqueId = in.readString();
			b.mDisplayIdToMirror = in.readInt();
			b.mWindowManagerMirroringEnabled = in.readInt() != 0;
			String[] categories = in.createStringArray();
			if (categories != null)
				for (String c : categories)
					b.addDisplayCategory(c);
			b.mRequestedRefreshRate = in.readFloat();
			b.mIsHomeSupported = in.readInt() != 0;
			b.mIgnoreActivitySizeRestrictions = in.readInt() != 0;
			b.mDefaultBrightness = in.readFloat();
			b.mDimBrightness = in.readFloat();
			return b.build();
		}
	};

	/* AOSP VirtualDisplayConfig.BrightnessListener */
	public interface BrightnessListener {
		void onBrightnessChanged(float brightness);
	}

	public static final class Builder {
		private final String mName;
		private final int mWidth;
		private final int mHeight;
		private final int mDensityDpi;
		private int mFlags = 0;
		private Surface mSurface = null;
		private String mUniqueId = null;
		private int mDisplayIdToMirror = android.view.Display.DEFAULT_DISPLAY;
		private boolean mWindowManagerMirroringEnabled = false;
		private final Set<String> mDisplayCategories = new HashSet<>();
		private float mRequestedRefreshRate = 0.0f;
		private boolean mIsHomeSupported = false;
		private boolean mIgnoreActivitySizeRestrictions = false;
		private float mDefaultBrightness = 0.0f;
		private float mDimBrightness = -1.0f; // PowerManager.BRIGHTNESS_INVALID
		private BrightnessListener mBrightnessListener = null;
		private Executor mBrightnessExecutor = null;

		public Builder(String name, int width, int height, int densityDpi) {
			if (name == null) {
				throw new IllegalArgumentException("Virtual display name is required");
			}
			if (width <= 0) {
				throw new IllegalArgumentException("Virtual display width must be positive");
			}
			if (height <= 0) {
				throw new IllegalArgumentException("Virtual display height must be positive");
			}
			if (densityDpi <= 0) {
				throw new IllegalArgumentException("Virtual display density must be positive");
			}
			mName = name;
			mWidth = width;
			mHeight = height;
			mDensityDpi = densityDpi;
		}

		public Builder setFlags(int flags) {
			mFlags = flags;
			return this;
		}

		public Builder setSurface(Surface surface) {
			mSurface = surface;
			return this;
		}

		public Builder setDisplayCategories(Set<String> displayCategories) {
			mDisplayCategories.clear();
			mDisplayCategories.addAll(java.util.Objects.requireNonNull(displayCategories));
			return this;
		}

		public Builder addDisplayCategory(String displayCategory) {
			mDisplayCategories.add(java.util.Objects.requireNonNull(displayCategory));
			return this;
		}

		public Builder setRequestedRefreshRate(float requestedRefreshRate) {
			if (requestedRefreshRate < 0.0f) {
				throw new IllegalArgumentException("Virtual display requested refresh rate must be non-negative");
			}
			mRequestedRefreshRate = requestedRefreshRate;
			return this;
		}

		public Builder setDefaultBrightness(float brightness) {
			if (brightness < 0.0f || brightness > 1.0f) {
				throw new IllegalArgumentException("Brightness must be between 0.0 and 1.0");
			}
			mDefaultBrightness = brightness;
			return this;
		}

		public Builder setDimBrightness(float brightness) {
			if (brightness < 0.0f || brightness > 1.0f) {
				throw new IllegalArgumentException("Brightness must be between 0.0 and 1.0");
			}
			mDimBrightness = brightness;
			return this;
		}

		public Builder setBrightnessListener(Executor executor, BrightnessListener listener) {
			mBrightnessExecutor = java.util.Objects.requireNonNull(executor);
			mBrightnessListener = java.util.Objects.requireNonNull(listener);
			return this;
		}

		public VirtualDisplayConfig build() {
			if (mDimBrightness != -1.0f && mDimBrightness > mDefaultBrightness) {
				throw new IllegalArgumentException("The dim brightness must not be greater than the default brightness");
			}
			return new VirtualDisplayConfig(this);
		}
	}
}
