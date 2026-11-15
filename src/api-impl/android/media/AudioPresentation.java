package android.media;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
/* AOSP media/java/android/media/AudioPresentation.java */
public final class AudioPresentation implements Parcelable {
	private final int mPresentationId;
	private final int mProgramId;
	private final ULocale mLanguage;
	@IntDef(
		value = {
		CONTENT_UNKNOWN,
		CONTENT_MAIN,
		CONTENT_MUSIC_AND_EFFECTS,
		CONTENT_VISUALLY_IMPAIRED,
		CONTENT_HEARING_IMPAIRED,
		CONTENT_DIALOG,
		CONTENT_COMMENTARY,
		CONTENT_EMERGENCY,
		CONTENT_VOICEOVER,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ContentClassifier {}
	public static final int CONTENT_UNKNOWN                 = -1;
	public static final int CONTENT_MAIN                    = 0;
	public static final int CONTENT_MUSIC_AND_EFFECTS       = 1;
	public static final int CONTENT_VISUALLY_IMPAIRED       = 2;
	public static final int CONTENT_HEARING_IMPAIRED        = 3;
	public static final int CONTENT_DIALOG                  = 4;
	public static final int CONTENT_COMMENTARY              = 5;
	public static final int CONTENT_EMERGENCY               = 6;
	public static final int CONTENT_VOICEOVER               = 7;
	@IntDef(
		value = {
			MASTERING_NOT_INDICATED,
			MASTERED_FOR_STEREO,
			MASTERED_FOR_SURROUND,
			MASTERED_FOR_3D,
			MASTERED_FOR_HEADPHONE,
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface MasteringIndicationType {}
	private final @MasteringIndicationType int mMasteringIndication;
	private final boolean mAudioDescriptionAvailable;
	private final boolean mSpokenSubtitlesAvailable;
	private final boolean mDialogueEnhancementAvailable;
	private final HashMap<ULocale, String> mLabels;
	public static final int MASTERING_NOT_INDICATED         = 0;
	public static final int MASTERED_FOR_STEREO             = 1;
	public static final int MASTERED_FOR_SURROUND           = 2;
	public static final int MASTERED_FOR_3D                 = 3;
	public static final int MASTERED_FOR_HEADPHONE          = 4;
	public static final int PRESENTATION_ID_UNKNOWN = -1;
	public static final int PROGRAM_ID_UNKNOWN = -1;
	private AudioPresentation(int presentationId,
							 int programId,
							 @NonNull ULocale language,
							 @MasteringIndicationType int masteringIndication,
							 boolean audioDescriptionAvailable,
							 boolean spokenSubtitlesAvailable,
							 boolean dialogueEnhancementAvailable,
							 @NonNull Map<ULocale, String> labels) {
		mPresentationId = presentationId;
		mProgramId = programId;
		mLanguage = language;
		mMasteringIndication = masteringIndication;
		mAudioDescriptionAvailable = audioDescriptionAvailable;
		mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
		mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
		mLabels = new HashMap<ULocale, String>(labels);
	}
	private AudioPresentation(@NonNull Parcel in) {
		mPresentationId = in.readInt();
		mProgramId = in.readInt();
		mLanguage = in.readSerializable(ULocale.class.getClassLoader(), ULocale.class);
		mMasteringIndication = in.readInt();
		mAudioDescriptionAvailable = in.readBoolean();
		mSpokenSubtitlesAvailable = in.readBoolean();
		mDialogueEnhancementAvailable = in.readBoolean();
		mLabels = in.readSerializable(HashMap.class.getClassLoader(), HashMap.class);
	}
	public int getPresentationId() {
		return mPresentationId;
	}
	public int getProgramId() {
		return mProgramId;
	}
	public Map<Locale, String> getLabels() {
		Map<Locale, String> localeLabels = new HashMap<Locale, String>(mLabels.size());
		for (Map.Entry<ULocale, String> entry : mLabels.entrySet()) {
			localeLabels.put(entry.getKey().toLocale(), entry.getValue());
		}
		return localeLabels;
	}
	private Map<ULocale, String> getULabels() {
		return mLabels;
	}
	public Locale getLocale() {
		return mLanguage.toLocale();
	}
	private ULocale getULocale() {
		return mLanguage;
	}
	@MasteringIndicationType
	public int getMasteringIndication() {
		return mMasteringIndication;
	}
	public boolean hasAudioDescription() {
		return mAudioDescriptionAvailable;
	}
	public boolean hasSpokenSubtitles() {
		return mSpokenSubtitlesAvailable;
	}
	public boolean hasDialogueEnhancement() {
		return mDialogueEnhancementAvailable;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AudioPresentation)) {
			return false;
		}
		AudioPresentation obj = (AudioPresentation) o;
		return mPresentationId == obj.getPresentationId()
				&& mProgramId == obj.getProgramId()
				&& mLanguage.equals(obj.getULocale())
				&& mMasteringIndication == obj.getMasteringIndication()
				&& mAudioDescriptionAvailable == obj.hasAudioDescription()
				&& mSpokenSubtitlesAvailable == obj.hasSpokenSubtitles()
				&& mDialogueEnhancementAvailable == obj.hasDialogueEnhancement()
				&& mLabels.equals(obj.getULabels());
	}
	@Override
	public int hashCode() {
		return Objects.hash(mPresentationId,
				mProgramId,
				mLanguage.hashCode(),
				mMasteringIndication,
				mAudioDescriptionAvailable,
				mSpokenSubtitlesAvailable,
				mDialogueEnhancementAvailable,
				mLabels.hashCode());
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName() + " ");
		sb.append("{ presentation id=" + mPresentationId);
		sb.append(", program id=" + mProgramId);
		sb.append(", language=" + mLanguage);
		sb.append(", labels=" + mLabels);
		sb.append(", mastering indication=" + mMasteringIndication);
		sb.append(", audio description=" + mAudioDescriptionAvailable);
		sb.append(", spoken subtitles=" + mSpokenSubtitlesAvailable);
		sb.append(", dialogue enhancement=" + mDialogueEnhancementAvailable);
		sb.append(" }");
		return sb.toString();
	}
	public static final class Builder {
		private final int mPresentationId;
		private int mProgramId = PROGRAM_ID_UNKNOWN;
		private ULocale mLanguage = new ULocale("");
		private int mMasteringIndication = MASTERING_NOT_INDICATED;
		private boolean mAudioDescriptionAvailable = false;
		private boolean mSpokenSubtitlesAvailable = false;
		private boolean mDialogueEnhancementAvailable = false;
		private HashMap<ULocale, String> mLabels = new HashMap<ULocale, String>();
		public Builder(int presentationId) {
			mPresentationId = presentationId;
		}
		public @NonNull Builder setProgramId(int programId) {
			mProgramId = programId;
			return this;
		}
		public @NonNull Builder setLocale(@NonNull ULocale language) {
			mLanguage = language;
			return this;
		}
		public @NonNull Builder setMasteringIndication(
				@MasteringIndicationType int masteringIndication) {
			if (masteringIndication != MASTERING_NOT_INDICATED
					&& masteringIndication != MASTERED_FOR_STEREO
					&& masteringIndication != MASTERED_FOR_SURROUND
					&& masteringIndication != MASTERED_FOR_3D
					&& masteringIndication != MASTERED_FOR_HEADPHONE) {
				throw new IllegalArgumentException("Unknown mastering indication: "
														+ masteringIndication);
			}
			mMasteringIndication = masteringIndication;
			return this;
		}
		public @NonNull Builder setLabels(@NonNull Map<ULocale, CharSequence> labels) {
			mLabels.clear();
			for (Map.Entry<ULocale, CharSequence> entry : labels.entrySet()) {
				mLabels.put(entry.getKey(), entry.getValue().toString());
			}
			return this;
		}
		public @NonNull Builder setHasAudioDescription(boolean audioDescriptionAvailable) {
			mAudioDescriptionAvailable = audioDescriptionAvailable;
			return this;
		}
		public @NonNull Builder setHasSpokenSubtitles(boolean spokenSubtitlesAvailable) {
			mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
			return this;
		}
		public @NonNull Builder setHasDialogueEnhancement(boolean dialogueEnhancementAvailable) {
			mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
			return this;
		}
		public @NonNull AudioPresentation build() {
			return new AudioPresentation(mPresentationId, mProgramId,
										   mLanguage, mMasteringIndication,
										   mAudioDescriptionAvailable, mSpokenSubtitlesAvailable,
										   mDialogueEnhancementAvailable, mLabels);
		}
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(getPresentationId());
		dest.writeInt(getProgramId());
		dest.writeSerializable(getULocale());
		dest.writeInt(getMasteringIndication());
		dest.writeBoolean(hasAudioDescription());
		dest.writeBoolean(hasSpokenSubtitles());
		dest.writeBoolean(hasDialogueEnhancement());
		dest.writeSerializable(mLabels);
	}
	@NonNull
	public static final Parcelable.Creator<AudioPresentation> CREATOR =
			new Parcelable.Creator<AudioPresentation>() {
			@Override
			public AudioPresentation createFromParcel(@NonNull Parcel in) {
				return new AudioPresentation(in);
			}
			@Override
			public AudioPresentation[] newArray(int size) {
				return new AudioPresentation[size];
			}
	};
}
