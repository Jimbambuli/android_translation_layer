package android.media;

/* AOSP media/java/android/media/SyncParams.java */
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.annotation.IntDef;

public final class SyncParams {

	@IntDef(
		value = {
				SYNC_SOURCE_DEFAULT,
				SYNC_SOURCE_SYSTEM_CLOCK,
				SYNC_SOURCE_AUDIO,
				SYNC_SOURCE_VSYNC,
		}
	)
	@Retention(RetentionPolicy.SOURCE)
	public @interface SyncSource {}

	public static final int SYNC_SOURCE_DEFAULT = 0;

	public static final int SYNC_SOURCE_SYSTEM_CLOCK = 1;

	public static final int SYNC_SOURCE_AUDIO = 2;

	public static final int SYNC_SOURCE_VSYNC = 3;

	@IntDef(
		value = {
				AUDIO_ADJUST_MODE_DEFAULT,
				AUDIO_ADJUST_MODE_STRETCH,
				AUDIO_ADJUST_MODE_RESAMPLE,
		}
	)
	@Retention(RetentionPolicy.SOURCE)
	public @interface AudioAdjustMode {}

	public static final int AUDIO_ADJUST_MODE_DEFAULT = 0;

	public static final int AUDIO_ADJUST_MODE_STRETCH = 1;

	public static final int AUDIO_ADJUST_MODE_RESAMPLE = 2;

	// flags to indicate which params are actually set
	private static final int SET_SYNC_SOURCE		 = 1 << 0;
	private static final int SET_AUDIO_ADJUST_MODE   = 1 << 1;
	private static final int SET_TOLERANCE		   = 1 << 2;
	private static final int SET_FRAME_RATE		  = 1 << 3;
	private int mSet = 0;

	// params
	private int mAudioAdjustMode = AUDIO_ADJUST_MODE_DEFAULT;
	private int mSyncSource = SYNC_SOURCE_DEFAULT;
	private float mTolerance = 0.f;
	private float mFrameRate = 0.f;

	public SyncParams allowDefaults() {
		mSet |= SET_SYNC_SOURCE | SET_AUDIO_ADJUST_MODE | SET_TOLERANCE;
		return this;
	}

	public SyncParams setAudioAdjustMode(@AudioAdjustMode int audioAdjustMode) {
		mAudioAdjustMode = audioAdjustMode;
		mSet |= SET_AUDIO_ADJUST_MODE;
		return this;
	}

	public @AudioAdjustMode int getAudioAdjustMode() {
		if ((mSet & SET_AUDIO_ADJUST_MODE) == 0) {
			throw new IllegalStateException("audio adjust mode not set");
		}
		return mAudioAdjustMode;
	}

	public SyncParams setSyncSource(@SyncSource int syncSource) {
		mSyncSource = syncSource;
		mSet |= SET_SYNC_SOURCE;
		return this;
	}

	public @SyncSource int getSyncSource() {
		if ((mSet & SET_SYNC_SOURCE) == 0) {
			throw new IllegalStateException("sync source not set");
		}
		return mSyncSource;
	}

	public SyncParams setTolerance(float tolerance) {
		if (tolerance < 0.f || tolerance >= 1.f) {
			throw new IllegalArgumentException("tolerance must be less than one and non-negative");
		}
		mTolerance = tolerance;
		mSet |= SET_TOLERANCE;
		return this;
	}

	public float getTolerance() {
		if ((mSet & SET_TOLERANCE) == 0) {
			throw new IllegalStateException("tolerance not set");
		}
		return mTolerance;
	}

	public SyncParams setFrameRate(float frameRate) {
		mFrameRate = frameRate;
		mSet |= SET_FRAME_RATE;
		return this;
	}

	public float getFrameRate() {
		if ((mSet & SET_FRAME_RATE) == 0) {
			throw new IllegalStateException("frame rate not set");
		}
		return mFrameRate;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public SyncParams() {}
}
