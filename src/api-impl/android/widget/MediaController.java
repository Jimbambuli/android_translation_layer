package android.widget;

public class MediaController {
	/** AOSP MediaController.MediaPlayerControl (android-14 core/java/android/widget/MediaController.java) */
	public interface MediaPlayerControl {
		void start();
		void pause();
		int getDuration();
		int getCurrentPosition();
		void seekTo(int pos);
		boolean isPlaying();
		int getBufferPercentage();
		boolean canPause();
		boolean canSeekBackward();
		boolean canSeekForward();
		int getAudioSessionId();
	}
}
