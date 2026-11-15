package android.media;

import android.annotation.FloatRange;

/* AOSP media/java/android/media/MicrophoneDirection.java */
public interface MicrophoneDirection {
	int MIC_DIRECTION_UNSPECIFIED = 0;
	int MIC_DIRECTION_TOWARDS_USER = 1;
	int MIC_DIRECTION_AWAY_FROM_USER = 2;
	int MIC_DIRECTION_EXTERNAL = 3;

	boolean setPreferredMicrophoneDirection(int direction);

	boolean setPreferredMicrophoneFieldDimension(@FloatRange(from = -1.0, to = 1.0) float zoom);
}
