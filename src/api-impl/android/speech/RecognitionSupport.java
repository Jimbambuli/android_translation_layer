package android.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

import java.util.List;

@DataClass(
		genConstructor = false,
		genBuilder = true,
		genEqualsHashCode = true,
		genHiddenConstDefs = true,
		genParcelable = true,
		genToString = true
)
/* AOSP core/java/android/speech/RecognitionSupport.java */
public final class RecognitionSupport implements Parcelable {

	@DataClass.PluralOf("installedOnDeviceLanguage")
	private List<String> mInstalledOnDeviceLanguages = List.of();

	@DataClass.PluralOf("pendingOnDeviceLanguage")
	private List<String> mPendingOnDeviceLanguages = List.of();

	@DataClass.PluralOf("supportedOnDeviceLanguage")
	private List<String> mSupportedOnDeviceLanguages = List.of();

	@DataClass.PluralOf("onlineLanguage")
	private List<String> mOnlineLanguages = List.of();

	@DataClass.Generated.Member
	 RecognitionSupport(
			List<String> installedOnDeviceLanguages,
			List<String> pendingOnDeviceLanguages,
			List<String> supportedOnDeviceLanguages,
			List<String> onlineLanguages) {
		this.mInstalledOnDeviceLanguages = installedOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mInstalledOnDeviceLanguages);
		this.mPendingOnDeviceLanguages = pendingOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mPendingOnDeviceLanguages);
		this.mSupportedOnDeviceLanguages = supportedOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mSupportedOnDeviceLanguages);
		this.mOnlineLanguages = onlineLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mOnlineLanguages);
	}

	@DataClass.Generated.Member
	public List<String> getInstalledOnDeviceLanguages() {
		return mInstalledOnDeviceLanguages;
	}

	@DataClass.Generated.Member
	public List<String> getPendingOnDeviceLanguages() {
		return mPendingOnDeviceLanguages;
	}

	@DataClass.Generated.Member
	public List<String> getSupportedOnDeviceLanguages() {
		return mSupportedOnDeviceLanguages;
	}

	@DataClass.Generated.Member
	public List<String> getOnlineLanguages() {
		return mOnlineLanguages;
	}

	@Override
	@DataClass.Generated.Member
	public String toString() {

		return "RecognitionSupport { " +
				"installedOnDeviceLanguages = " + mInstalledOnDeviceLanguages + ", " +
				"pendingOnDeviceLanguages = " + mPendingOnDeviceLanguages + ", " +
				"supportedOnDeviceLanguages = " + mSupportedOnDeviceLanguages + ", " +
				"onlineLanguages = " + mOnlineLanguages +
		" }";
	}

	@Override
	@DataClass.Generated.Member
	public boolean equals(@android.annotation.Nullable Object o) {

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		@SuppressWarnings("unchecked")
		RecognitionSupport that = (RecognitionSupport) o;
		return true
				&& java.util.Objects.equals(mInstalledOnDeviceLanguages, that.mInstalledOnDeviceLanguages)
				&& java.util.Objects.equals(mPendingOnDeviceLanguages, that.mPendingOnDeviceLanguages)
				&& java.util.Objects.equals(mSupportedOnDeviceLanguages, that.mSupportedOnDeviceLanguages)
				&& java.util.Objects.equals(mOnlineLanguages, that.mOnlineLanguages);
	}

	@Override
	@DataClass.Generated.Member
	public int hashCode() {

		int _hash = 1;
		_hash = 31 * _hash + java.util.Objects.hashCode(mInstalledOnDeviceLanguages);
		_hash = 31 * _hash + java.util.Objects.hashCode(mPendingOnDeviceLanguages);
		_hash = 31 * _hash + java.util.Objects.hashCode(mSupportedOnDeviceLanguages);
		_hash = 31 * _hash + java.util.Objects.hashCode(mOnlineLanguages);
		return _hash;
	}

	@Override
	@DataClass.Generated.Member
	public void writeToParcel(android.os.Parcel dest, int flags) {

		dest.writeStringList(mInstalledOnDeviceLanguages);
		dest.writeStringList(mPendingOnDeviceLanguages);
		dest.writeStringList(mSupportedOnDeviceLanguages);
		dest.writeStringList(mOnlineLanguages);
	}

	@Override
	@DataClass.Generated.Member
	public int describeContents() { return 0; }

	@SuppressWarnings({"unchecked", "RedundantCast"})
	@DataClass.Generated.Member
	 RecognitionSupport(android.os.Parcel in) {

		List<String> installedOnDeviceLanguages = new java.util.ArrayList<>();
		in.readStringList(installedOnDeviceLanguages);
		List<String> pendingOnDeviceLanguages = new java.util.ArrayList<>();
		in.readStringList(pendingOnDeviceLanguages);
		List<String> supportedOnDeviceLanguages = new java.util.ArrayList<>();
		in.readStringList(supportedOnDeviceLanguages);
		List<String> onlineLanguages = new java.util.ArrayList<>();
		in.readStringList(onlineLanguages);

		this.mInstalledOnDeviceLanguages = installedOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mInstalledOnDeviceLanguages);
		this.mPendingOnDeviceLanguages = pendingOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mPendingOnDeviceLanguages);
		this.mSupportedOnDeviceLanguages = supportedOnDeviceLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mSupportedOnDeviceLanguages);
		this.mOnlineLanguages = onlineLanguages;
		com.android.internal.util.AnnotationValidations.validate(
				android.annotation.NonNull.class, null, mOnlineLanguages);
	}

	@DataClass.Generated.Member
	public static final Parcelable.Creator<RecognitionSupport> CREATOR
			= new Parcelable.Creator<RecognitionSupport>() {
		@Override
		public RecognitionSupport[] newArray(int size) {
			return new RecognitionSupport[size];
		}

		@Override
		public RecognitionSupport createFromParcel(android.os.Parcel in) {
			return new RecognitionSupport(in);
		}
	};

	@SuppressWarnings("WeakerAccess")
	@DataClass.Generated.Member
	public static final class Builder {

		private List<String> mInstalledOnDeviceLanguages;
		private List<String> mPendingOnDeviceLanguages;
		private List<String> mSupportedOnDeviceLanguages;
		private List<String> mOnlineLanguages;

		private long mBuilderFieldsSet = 0L;

		public Builder() {
		}

		@DataClass.Generated.Member
		public Builder setInstalledOnDeviceLanguages(List<String> value) {
			checkNotUsed();
			mBuilderFieldsSet |= 0x1;
			mInstalledOnDeviceLanguages = value;
			return this;
		}

		@DataClass.Generated.Member
		public Builder addInstalledOnDeviceLanguage(String value) {
			if (mInstalledOnDeviceLanguages == null) setInstalledOnDeviceLanguages(new java.util.ArrayList<>());
			mInstalledOnDeviceLanguages.add(value);
			return this;
		}

		@DataClass.Generated.Member
		public Builder setPendingOnDeviceLanguages(List<String> value) {
			checkNotUsed();
			mBuilderFieldsSet |= 0x2;
			mPendingOnDeviceLanguages = value;
			return this;
		}

		@DataClass.Generated.Member
		public Builder addPendingOnDeviceLanguage(String value) {
			if (mPendingOnDeviceLanguages == null) setPendingOnDeviceLanguages(new java.util.ArrayList<>());
			mPendingOnDeviceLanguages.add(value);
			return this;
		}

		@DataClass.Generated.Member
		public Builder setSupportedOnDeviceLanguages(List<String> value) {
			checkNotUsed();
			mBuilderFieldsSet |= 0x4;
			mSupportedOnDeviceLanguages = value;
			return this;
		}

		@DataClass.Generated.Member
		public Builder addSupportedOnDeviceLanguage(String value) {
			if (mSupportedOnDeviceLanguages == null) setSupportedOnDeviceLanguages(new java.util.ArrayList<>());
			mSupportedOnDeviceLanguages.add(value);
			return this;
		}

		@DataClass.Generated.Member
		public Builder setOnlineLanguages(List<String> value) {
			checkNotUsed();
			mBuilderFieldsSet |= 0x8;
			mOnlineLanguages = value;
			return this;
		}

		@DataClass.Generated.Member
		public Builder addOnlineLanguage(String value) {
			if (mOnlineLanguages == null) setOnlineLanguages(new java.util.ArrayList<>());
			mOnlineLanguages.add(value);
			return this;
		}

		public RecognitionSupport build() {
			checkNotUsed();
			mBuilderFieldsSet |= 0x10; // Mark builder used

			if ((mBuilderFieldsSet & 0x1) == 0) {
				mInstalledOnDeviceLanguages = List.of();
			}
			if ((mBuilderFieldsSet & 0x2) == 0) {
				mPendingOnDeviceLanguages = List.of();
			}
			if ((mBuilderFieldsSet & 0x4) == 0) {
				mSupportedOnDeviceLanguages = List.of();
			}
			if ((mBuilderFieldsSet & 0x8) == 0) {
				mOnlineLanguages = List.of();
			}
			RecognitionSupport o = new RecognitionSupport(
					mInstalledOnDeviceLanguages,
					mPendingOnDeviceLanguages,
					mSupportedOnDeviceLanguages,
					mOnlineLanguages);
			return o;
		}

		private void checkNotUsed() {
			if ((mBuilderFieldsSet & 0x10) != 0) {
				throw new IllegalStateException(
						"This Builder should not be reused. Use a new Builder instance instead");
			}
		}
	}


}
