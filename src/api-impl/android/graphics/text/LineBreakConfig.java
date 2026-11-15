package android.graphics.text;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/* AOSP LineBreakConfig (Android 16): line break style, word style and hyphenation. Pango breaks the lines, so the
 * configuration is only kept. The target SDK comes from android.graphics.Compatibility (AOSP: VMRuntime). */
public final class LineBreakConfig implements Parcelable {
	public static final int HYPHENATION_UNSPECIFIED = -1;
	public static final int HYPHENATION_DISABLED = 0;
	public static final int HYPHENATION_ENABLED = 1;

	public static final int LINE_BREAK_STYLE_UNSPECIFIED = -1;
	public static final int LINE_BREAK_STYLE_NONE = 0;
	public static final int LINE_BREAK_STYLE_LOOSE = 1;
	public static final int LINE_BREAK_STYLE_NORMAL = 2;
	public static final int LINE_BREAK_STYLE_STRICT = 3;
	public static final int LINE_BREAK_STYLE_NO_BREAK = 4;
	public static final int LINE_BREAK_STYLE_AUTO = 5;

	public static final int LINE_BREAK_WORD_STYLE_UNSPECIFIED = -1;
	public static final int LINE_BREAK_WORD_STYLE_NONE = 0;
	public static final int LINE_BREAK_WORD_STYLE_PHRASE = 1;
	public static final int LINE_BREAK_WORD_STYLE_AUTO = 2;

	public static final class Builder {
		private int mLineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_UNSPECIFIED;
		private int mLineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_UNSPECIFIED;
		private int mHyphenation = LineBreakConfig.HYPHENATION_UNSPECIFIED;

		public Builder() {
			reset(null);
		}

		@NonNull
		public Builder merge(@NonNull LineBreakConfig config) {
			if (config.mLineBreakStyle != LINE_BREAK_STYLE_UNSPECIFIED) {
				mLineBreakStyle = config.mLineBreakStyle;
			}
			if (config.mLineBreakWordStyle != LINE_BREAK_WORD_STYLE_UNSPECIFIED) {
				mLineBreakWordStyle = config.mLineBreakWordStyle;
			}
			if (config.mHyphenation != HYPHENATION_UNSPECIFIED) {
				mHyphenation = config.mHyphenation;
			}
			return this;
		}

		@NonNull
		public Builder reset(@Nullable LineBreakConfig config) {
			if (config == null) {
				mLineBreakStyle = LINE_BREAK_STYLE_UNSPECIFIED;
				mLineBreakWordStyle = LINE_BREAK_WORD_STYLE_UNSPECIFIED;
				mHyphenation = HYPHENATION_UNSPECIFIED;
			} else {
				mLineBreakStyle = config.mLineBreakStyle;
				mLineBreakWordStyle = config.mLineBreakWordStyle;
				mHyphenation = config.mHyphenation;
			}
			return this;
		}

		@NonNull
		public Builder setLineBreakStyle(int lineBreakStyle) {
			mLineBreakStyle = lineBreakStyle;
			return this;
		}

		@NonNull
		public Builder setLineBreakWordStyle(int lineBreakWordStyle) {
			mLineBreakWordStyle = lineBreakWordStyle;
			return this;
		}

		@NonNull
		public Builder setHyphenation(int hyphenation) {
			mHyphenation = hyphenation;
			return this;
		}

		@NonNull
		public LineBreakConfig build() {
			return new LineBreakConfig(mLineBreakStyle, mLineBreakWordStyle, mHyphenation);
		}
	}

	@NonNull
	public static LineBreakConfig getLineBreakConfig(int lineBreakStyle, int lineBreakWordStyle) {
		LineBreakConfig.Builder builder = new LineBreakConfig.Builder();
		return builder.setLineBreakStyle(lineBreakStyle).setLineBreakWordStyle(lineBreakWordStyle).build();
	}

	public static final LineBreakConfig NONE = new Builder().setLineBreakStyle(LINE_BREAK_STYLE_NONE)
			.setLineBreakWordStyle(LINE_BREAK_WORD_STYLE_NONE).build();

	private final int mLineBreakStyle;
	private final int mLineBreakWordStyle;
	private final int mHyphenation;

	public LineBreakConfig(int lineBreakStyle, int lineBreakWordStyle, int hyphenation) {
		mLineBreakStyle = lineBreakStyle;
		mLineBreakWordStyle = lineBreakWordStyle;
		mHyphenation = hyphenation;
	}

	public int getLineBreakStyle() {
		return mLineBreakStyle;
	}

	public static int getResolvedLineBreakStyle(@Nullable LineBreakConfig config) {
		final int defaultStyle = android.graphics.Compatibility.getTargetSdkVersion() >= Build.VERSION_CODES.VANILLA_ICE_CREAM
				? LINE_BREAK_STYLE_AUTO : LINE_BREAK_STYLE_NONE;
		if (config == null) {
			return defaultStyle;
		}
		return config.mLineBreakStyle == LINE_BREAK_STYLE_UNSPECIFIED ? defaultStyle : config.mLineBreakStyle;
	}

	public int getLineBreakWordStyle() {
		return mLineBreakWordStyle;
	}

	public static int getResolvedLineBreakWordStyle(@Nullable LineBreakConfig config) {
		final int defaultWordStyle = android.graphics.Compatibility.getTargetSdkVersion() >= Build.VERSION_CODES.VANILLA_ICE_CREAM
				? LINE_BREAK_WORD_STYLE_AUTO : LINE_BREAK_WORD_STYLE_NONE;
		if (config == null) {
			return defaultWordStyle;
		}
		return config.mLineBreakWordStyle == LINE_BREAK_WORD_STYLE_UNSPECIFIED ? defaultWordStyle : config.mLineBreakWordStyle;
	}

	public int getHyphenation() {
		return mHyphenation;
	}

	public static int getResolvedHyphenation(@Nullable LineBreakConfig config) {
		if (config == null) {
			return HYPHENATION_ENABLED;
		}
		return config.mHyphenation == HYPHENATION_UNSPECIFIED ? HYPHENATION_ENABLED : config.mHyphenation;
	}

	@NonNull
	public LineBreakConfig merge(@NonNull LineBreakConfig config) {
		return new LineBreakConfig(
				config.mLineBreakStyle == LINE_BREAK_STYLE_UNSPECIFIED ? mLineBreakStyle : config.mLineBreakStyle,
				config.mLineBreakWordStyle == LINE_BREAK_WORD_STYLE_UNSPECIFIED ? mLineBreakWordStyle : config.mLineBreakWordStyle,
				config.mHyphenation == HYPHENATION_UNSPECIFIED ? mHyphenation : config.mHyphenation);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (this == o) return true;
		if (!(o instanceof LineBreakConfig)) return false;
		LineBreakConfig that = (LineBreakConfig)o;
		return (mLineBreakStyle == that.mLineBreakStyle) && (mLineBreakWordStyle == that.mLineBreakWordStyle)
				&& (mHyphenation == that.mHyphenation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mLineBreakStyle, mLineBreakWordStyle, mHyphenation);
	}

	@Override
	public String toString() {
		return "LineBreakConfig{" + "mLineBreakStyle=" + mLineBreakStyle + ", mLineBreakWordStyle=" + mLineBreakWordStyle
				+ ", mHyphenation= " + mHyphenation + '}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(mLineBreakStyle);
		dest.writeInt(mLineBreakWordStyle);
		dest.writeInt(mHyphenation);
	}

	public static final Creator<LineBreakConfig> CREATOR = new Creator<LineBreakConfig>() {
		@Override
		public LineBreakConfig createFromParcel(Parcel source) {
			final int lineBreakStyle = source.readInt();
			final int lineBreakWordStyle = source.readInt();
			final int hyphenation = source.readInt();
			return new LineBreakConfig(lineBreakStyle, lineBreakWordStyle, hyphenation);
		}

		@Override
		public LineBreakConfig[] newArray(int size) {
			return new LineBreakConfig[size];
		}
	};
}
