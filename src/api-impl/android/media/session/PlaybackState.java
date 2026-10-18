package android.media.session;

import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

public class PlaybackState {

	public int state;
	public long position;
	public float playbackSpeed;
	public long updateTime;
	public long actions;
	public long activeQueueItemId;
	private long bufferedPosition;
	private CharSequence errorMessage;
	private List<CustomAction> customActions = new ArrayList<>();
	private Bundle extras;

	public int getState() {
		return state;
	}

	public long getPosition() {
		return position;
	}

	public long getBufferedPosition() {
		return bufferedPosition;
	}

	public float getPlaybackSpeed() {
		return playbackSpeed;
	}

	public long getActions() {
		return actions;
	}

	public List<CustomAction> getCustomActions() {
		return customActions;
	}

	public CharSequence getErrorMessage() {
		return errorMessage;
	}

	public long getLastPositionUpdateTime() {
		return updateTime;
	}

	public long getActiveQueueItemId() {
		return activeQueueItemId;
	}

	public Bundle getExtras() {
		return extras;
	}

	public static class Builder {

		PlaybackState state;

		public Builder() {
			state = new PlaybackState();
		}

		public Builder(PlaybackState from) {
			state = from;
		}

		public Builder setState(int state, long position, float playbackSpeed, long updateTime) {
			this.state.state = state;
			this.state.position = position;
			this.state.playbackSpeed = playbackSpeed;
			this.state.updateTime = updateTime;
			return this;
		}

		public Builder setBufferedPosition(long bufferedPosition) {
			state.bufferedPosition = bufferedPosition;
			return this;
		}

		public Builder setActions(long actions) {
			state.actions = actions;
			return this;
		}

		public Builder setErrorMessage(CharSequence errorMessage) {
			state.errorMessage = errorMessage;
			return this;
		}

		public Builder setActiveQueueItemId(long activeQueueItemId) {
			state.activeQueueItemId = activeQueueItemId;
			return this;
		}

		public Builder addCustomAction(CustomAction action) {
			state.customActions.add(action);
			return this;
		}

		public Builder setExtras(Bundle bundle) {
			state.extras = bundle;
			return this;
		}

		public PlaybackState build() {
			return state;
		}
	}

	public static class CustomAction {
		private String action;
		private CharSequence name;
		private int icon;
		private Bundle extras;

		public String getAction() {
			return action;
		}

		public CharSequence getName() {
			return name;
		}

		public int getIcon() {
			return icon;
		}

		public Bundle getExtras() {
			return extras;
		}

		public static class Builder {
			private final CustomAction customAction = new CustomAction();

			public Builder(String action, CharSequence name, int icon) {
				customAction.action = action;
				customAction.name = name;
				customAction.icon = icon;
			}

			public Builder setExtras(Bundle extras) {
				customAction.extras = extras;
				return this;
			}

			public CustomAction build() {
				return customAction;
			}
		}
	}
}
