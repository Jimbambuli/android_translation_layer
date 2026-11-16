package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * AOSP MediaController is a FrameLayout shown in its own window over the anchor view (android16
 * core/java/android/widget/MediaController.java, layout media_controller.xml, which is not part of the AOSP sources).
 * ATL has no such window: the player, the anchor and the showing state are kept, nothing is shown.
 */
public class MediaController extends FrameLayout {
	private MediaPlayerControl mPlayer;
	private View mAnchor;
	private boolean mShowing;
	private boolean mUseFastForward = true;

	public MediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MediaController(Context context, boolean useFastForward) {
		super(context);
		mUseFastForward = useFastForward;
	}

	public MediaController(Context context) {
		this(context, true);
	}

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
	}

	public void setAnchorView(View view) {
		mAnchor = view;
	}

	public void show() {
		mShowing = true;
	}

	public void show(int timeout) {
		show();
	}

	public boolean isShowing() {
		return mShowing;
	}

	public void hide() {
		mShowing = false;
	}

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
