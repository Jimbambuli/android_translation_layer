/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DebugUtils;

import java.util.ArrayList;

/**
 * Interface for an {@link Activity} to interact with the user through voice.  Use
 * {@link android.app.Activity#getVoiceInteractor() Activity.getVoiceInteractor}
 * to retrieve the interface, if the activity is currently involved in a voice interaction.
 *
 * <p>The voice interactor revolves around submitting voice interaction requests to the
 * back-end voice interaction service that is working with the user.  These requests are
 * submitted with {@link #submitRequest}, providing a new instance of a
 * {@link Request} subclass describing the type of operation to perform -- currently the
 * possible requests are {@link ConfirmationRequest} and {@link CommandRequest}.
 *
 * <p>Once a request is submitted, the voice system will process it and eventually deliver
 * the result to the request object.  The application can cancel a pending request at any
 * time.
 *
 * <p>The VoiceInteractor is integrated with Activity's state saving mechanism, so that
 * if an activity is being restarted with retained state, it will retain the current
 * VoiceInteractor and any outstanding requests.  Because of this, you should always use
 * {@link Request#getActivity() Request.getActivity} to get back to the activity of a
 * request, rather than holding on to the activity instance yourself, either explicitly
 * or implicitly through a non-static inner class.
 */
/*
 * ATL: AOSP's VoiceInteractor passes the requests to the voice interaction service of the system
 * (IVoiceInteractor, a binder). ATL has no voice interaction service, so Activity.getVoiceInteractor()
 * answers null, as AOSP does for an activity that is not in a voice interaction session, and the
 * requests can only be built. A VoiceInteractor made by hand (for tests) is destroyed from the start:
 * it submits nothing, supports no command and has no active request.
 */
public final class VoiceInteractor {
	static final boolean DEBUG = false;

	final Context mContext;
	final Activity mActivity;

	VoiceInteractor(Context context, Activity activity) {
		mContext = context;
		mActivity = activity;
	}

	/**
	 * Base class for voice interaction requests that can be submitted to the interactor.
	 * Do not instantiate this directly -- instead, use the appropriate subclass.
	 */
	public static abstract class Request {
		Context mContext;
		Activity mActivity;
		String mName;

		Request() {
		}

		/**
		 * Return the name this request was submitted through
		 * {@link #submitRequest(android.app.VoiceInteractor.Request, String)}.
		 */
		public String getName() {
			return mName;
		}

		/**
		 * Cancel this active request.
		 */
		public void cancel() {
			clear();
		}

		/**
		 * Return the current {@link Context} this request is associated with.  Only
		 * valid between {@link #onAttached(Activity)} and {@link #onDetached()}.
		 */
		public Context getContext() {
			return mContext;
		}

		/**
		 * Return the current {@link Activity} this request is associated with.  Only
		 * valid between {@link #onAttached(Activity)} and {@link #onDetached()}.
		 */
		public Activity getActivity() {
			return mActivity;
		}

		/**
		 * The request is now attached to an activity, or being re-attached to a new activity
		 * after a configuration change.
		 */
		public void onAttached(Activity activity) {
		}

		/**
		 * The request is being detached from an activity.
		 */
		public void onDetached() {
		}

		/**
		 * The request has been canceled, either by the user or by the system.
		 */
		public void onCancel() {
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(128);
			DebugUtils.buildShortClassTag(this, sb);
			sb.append(" name=");
			sb.append(mName);
			sb.append('}');
			return sb.toString();
		}

		void clear() {
			mContext = null;
			mActivity = null;
			mName = null;
		}
	}

	/**
	 * Confirms an operation with the user via the trusted system
	 * VoiceInteractionService.  This allows an Activity to complete an unsafe operation that
	 * would require the user to touch the screen when voice interaction mode is not enabled.
	 * The result of the confirmation will be returned through an asynchronous call to
	 * either {@link #onConfirmationResult(boolean, android.os.Bundle)} or
	 * {@link #onCancel()} - these methods should be overridden to define the application specific
	 *  behavior.
	 *
	 * <p>In some cases this may be a simple yes / no confirmation or the confirmation could
	 * include context information about how the action will be completed
	 * (e.g. booking a cab might include details about how long until the cab arrives)
	 * so the user can give a confirmation.
	 */
	public static class ConfirmationRequest extends Request {
		final Prompt mPrompt;
		final Bundle mExtras;

		/**
		 * Create a new confirmation request.
		 * @param prompt Optional confirmation to speak to the user or null if nothing
		 *     should be spoken.
		 * @param extras Additional optional information or null.
		 */
		public ConfirmationRequest(@Nullable Prompt prompt, @Nullable Bundle extras) {
			mPrompt = prompt;
			mExtras = extras;
		}

		/**
		 * Create a new confirmation request.
		 * @param prompt Optional confirmation to speak to the user or null if nothing
		 *     should be spoken.
		 * @param extras Additional optional information or null.
		 * @hide
		 */
		public ConfirmationRequest(CharSequence prompt, Bundle extras) {
			mPrompt = (prompt != null ? new Prompt(prompt) : null);
			mExtras = extras;
		}

		/**
		 * Handle the confirmation result. Override this method to define
		 * the behavior when the user confirms or rejects the operation.
		 * @param confirmed Whether the user confirmed or rejected the operation.
		 * @param result Additional result information or null.
		 */
		public void onConfirmationResult(boolean confirmed, Bundle result) {
		}

		String getRequestTypeName() {
			return "Confirmation";
		}

	}

	/**
	 * Select a single option from multiple potential options with the user via the trusted system
	 * VoiceInteractionService. Typically, the application would present this visually as
	 * a list view to allow selecting the option by touch.
	 * The result of the confirmation will be returned through an asynchronous call to
	 * either {@link #onPickOptionResult} or {@link #onCancel()} - these methods should
	 * be overridden to define the application specific behavior.
	 */
	public static class PickOptionRequest extends Request {
		final Prompt mPrompt;
		final Option[] mOptions;
		final Bundle mExtras;

		/**
		 * Represents a single option that the user may select using their voice. The
		 * {@link #getIndex()} method should be used as a unique ID to identify the option
		 * when it is returned from the voice interactor.
		 */
		public static final class Option implements Parcelable {
			final CharSequence mLabel;
			final int mIndex;
			ArrayList<CharSequence> mSynonyms;
			Bundle mExtras;

			/**
			 * Creates an option that a user can select with their voice by matching the label
			 * or one of several synonyms.
			 * @param label The label that will both be matched against what the user speaks
			 *     and displayed visually.
			 * @hide
			 */
			public Option(CharSequence label) {
				mLabel = label;
				mIndex = -1;
			}

			/**
			 * Creates an option that a user can select with their voice by matching the label
			 * or one of several synonyms.
			 * @param label The label that will both be matched against what the user speaks
			 *     and displayed visually.
			 * @param index The location of this option within the overall set of options.
			 *     Can be used to help identify the option when it is returned from the
			 *     voice interactor.
			 */
			public Option(CharSequence label, int index) {
				mLabel = label;
				mIndex = index;
			}

			/**
			 * Add a synonym term to the option to indicate an alternative way the content
			 * may be matched.
			 * @param synonym The synonym that will be matched against what the user speaks,
			 *     but not displayed.
			 */
			public Option addSynonym(CharSequence synonym) {
				if (mSynonyms == null) {
					mSynonyms = new ArrayList<>();
				}
				mSynonyms.add(synonym);
				return this;
			}

			public CharSequence getLabel() {
				return mLabel;
			}

			/**
			 * Return the index that was supplied in the constructor.
			 * If the option was constructed without an index, -1 is returned.
			 */
			public int getIndex() {
				return mIndex;
			}

			public int countSynonyms() {
				return mSynonyms != null ? mSynonyms.size() : 0;
			}

			public CharSequence getSynonymAt(int index) {
				return mSynonyms != null ? mSynonyms.get(index) : null;
			}

			/**
			 * Set optional extra information associated with this option.  Note that this
			 * method takes ownership of the supplied extras Bundle.
			 */
			public void setExtras(Bundle extras) {
				mExtras = extras;
			}

			/**
			 * Return any optional extras information associated with this option, or null
			 * if there is none.  Note that this method returns a reference to the actual
			 * extras Bundle in the option, so modifications to it will directly modify the
			 * extras in the option.
			 */
			public Bundle getExtras() {
				return mExtras;
			}

			Option(Parcel in) {
				mLabel = in.readCharSequence();
				mIndex = in.readInt();
				int size = in.readInt();
				mSynonyms = size < 0 ? null : new ArrayList<CharSequence>(size);
				for (int i = 0; i < size; i++)
					mSynonyms.add(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in));
				mExtras = in.readBundle();
			}

			@Override
			public int describeContents() {
				return 0;
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeCharSequence(mLabel);
				dest.writeInt(mIndex);
				dest.writeInt(mSynonyms == null ? -1 : mSynonyms.size());
				if (mSynonyms != null)
					for (CharSequence synonym : mSynonyms)
						TextUtils.writeToParcel(synonym, dest, flags);
				dest.writeBundle(mExtras);
			}

			public static final @android.annotation.NonNull Parcelable.Creator<Option> CREATOR = new Parcelable.Creator<Option>() {
				public Option createFromParcel(Parcel in) {
					return new Option(in);
				}

				public Option[] newArray(int size) {
					return new Option[size];
				}
			};
		};

		/**
		 * Create a new pick option request.
		 * @param prompt Optional question to be asked of the user when the options are
		 *     presented or null if nothing should be asked.
		 * @param options The set of {@link Option}s the user is selecting from.
		 * @param extras Additional optional information or null.
		 */
		public PickOptionRequest(@Nullable Prompt prompt, Option[] options,
		                         @Nullable Bundle extras) {
			mPrompt = prompt;
			mOptions = options;
			mExtras = extras;
		}

		/**
		 * Create a new pick option request.
		 * @param prompt Optional question to be asked of the user when the options are
		 *     presented or null if nothing should be asked.
		 * @param options The set of {@link Option}s the user is selecting from.
		 * @param extras Additional optional information or null.
		 * @hide
		 */
		public PickOptionRequest(CharSequence prompt, Option[] options, Bundle extras) {
			mPrompt = (prompt != null ? new Prompt(prompt) : null);
			mOptions = options;
			mExtras = extras;
		}

		/**
		 * Called when a single option is confirmed or narrowed to one of several options. Override
		 * this method to define the behavior when the user selects an option or narrows down the
		 * set of options.
		 * @param finished True if the voice interaction has finished making a selection, in
		 *     which case {@code selections} contains the final result.  If false, this request is
		 *     still active and you will continue to get calls on it.
		 * @param selections Either a single {@link Option} or one of several {@link Option}s the
		 *     user has narrowed the choices down to.
		 * @param result Additional optional information.
		 */
		public void onPickOptionResult(boolean finished, Option[] selections, Bundle result) {
		}

		String getRequestTypeName() {
			return "PickOption";
		}

	}

	/**
	 * Reports that the current interaction was successfully completed with voice, so the
	 * application can report the final status to the user. When the response comes back, the
	 * voice system has handled the request and is ready to switch; at that point the
	 * application can start a new non-voice activity or finish.  Be sure when starting the new
	 * activity to use {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK
	 * Intent.FLAG_ACTIVITY_NEW_TASK} to keep the new activity out of the current voice
	 * interaction task.
	 */
	public static class CompleteVoiceRequest extends Request {
		final Prompt mPrompt;
		final Bundle mExtras;

		/**
		 * Create a new completed voice interaction request.
		 * @param prompt Optional message to speak to the user about the completion status of
		 *     the task or null if nothing should be spoken.
		 * @param extras Additional optional information or null.
		 */
		public CompleteVoiceRequest(@Nullable Prompt prompt, @Nullable Bundle extras) {
			mPrompt = prompt;
			mExtras = extras;
		}

		/**
		 * Create a new completed voice interaction request.
		 * @param message Optional message to speak to the user about the completion status of
		 *     the task or null if nothing should be spoken.
		 * @param extras Additional optional information or null.
		 * @hide
		 */
		public CompleteVoiceRequest(CharSequence message, Bundle extras) {
			mPrompt = (message != null ? new Prompt(message) : null);
			mExtras = extras;
		}

		public void onCompleteResult(Bundle result) {
		}

		String getRequestTypeName() {
			return "CompleteVoice";
		}

	}

	/**
	 * Reports that the current interaction can not be complete with voice, so the
	 * application will need to switch to a traditional input UI.  Applications should
	 * only use this when they need to completely bail out of the voice interaction
	 * and switch to a traditional UI.  When the response comes back, the voice
	 * system has handled the request and is ready to switch; at that point the application
	 * can start a new non-voice activity.  Be sure when starting the new activity
	 * to use {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK
	 * Intent.FLAG_ACTIVITY_NEW_TASK} to keep the new activity out of the current voice
	 * interaction task.
	 */
	public static class AbortVoiceRequest extends Request {
		final Prompt mPrompt;
		final Bundle mExtras;

		/**
		 * Create a new voice abort request.
		 * @param prompt Optional message to speak to the user indicating why the task could
		 *     not be completed by voice or null if nothing should be spoken.
		 * @param extras Additional optional information or null.
		 */
		public AbortVoiceRequest(@Nullable Prompt prompt, @Nullable Bundle extras) {
			mPrompt = prompt;
			mExtras = extras;
		}

		/**
		 * Create a new voice abort request.
		 * @param message Optional message to speak to the user indicating why the task could
		 *     not be completed by voice or null if nothing should be spoken.
		 * @param extras Additional optional information or null.
		 * @hide
		 */
		public AbortVoiceRequest(CharSequence message, Bundle extras) {
			mPrompt = (message != null ? new Prompt(message) : null);
			mExtras = extras;
		}

		public void onAbortResult(Bundle result) {
		}

		String getRequestTypeName() {
			return "AbortVoice";
		}

	}

	/**
	 * Execute a vendor-specific command using the trusted system VoiceInteractionService.
	 * This allows an Activity to request additional information from the user needed to
	 * complete an action (e.g. booking a table might have several possible times that the
	 * user could select from or an app might need the user to agree to a terms of service).
	 * The result of the confirmation will be returned through an asynchronous call to
	 * either {@link #onCommandResult(boolean, android.os.Bundle)} or
	 * {@link #onCancel()}.
	 *
	 * <p>The command is a string that describes the generic operation to be performed.
	 * The command will determine how the properties in extras are interpreted and the set of
	 * available commands is expected to grow over time.  An example might be
	 * "com.google.voice.commands.REQUEST_NUMBER_BAGS" to request the number of bags as part of
	 * airline check-in.  (This is not an actual working example.)
	 */
	public static class CommandRequest extends Request {
		final String mCommand;
		final Bundle mArgs;

		/**
		 * Create a new generic command request.
		 * @param command The desired command to perform.
		 * @param args Additional arguments to control execution of the command.
		 */
		public CommandRequest(String command, Bundle args) {
			mCommand = command;
			mArgs = args;
		}

		/**
		 * Results for CommandRequest can be returned in partial chunks.
		 * The isCompleted is set to true iff all results have been returned, indicating the
		 * CommandRequest has completed.
		 */
		public void onCommandResult(boolean isCompleted, Bundle result) {
		}

		String getRequestTypeName() {
			return "Command";
		}

	}

	/**
	 * A set of voice prompts to use with the voice interaction system to confirm an action, select
	 * an option, or do similar operations. Multiple voice prompts may be provided for variety. A
	 * visual prompt must be provided, which might not match the spoken version. For example, the
	 * confirmation "Are you sure you want to purchase this item?" might use a visual label like
	 * "Purchase item".
	 */
	public static class Prompt implements Parcelable {
		// Mandatory voice prompt. Must contain at least one item, which must not be null.
		private final CharSequence[] mVoicePrompts;

		// Mandatory visual prompt.
		private final CharSequence mVisualPrompt;

		/**
		 * Constructs a prompt set.
		 * @param voicePrompts An array of one or more voice prompts. Must not be empty or null.
		 * @param visualPrompt A prompt to display on the screen. Must not be null.
		 */
		public Prompt(@NonNull CharSequence[] voicePrompts, @NonNull CharSequence visualPrompt) {
			if (voicePrompts == null) {
				throw new NullPointerException("voicePrompts must not be null");
			}
			if (voicePrompts.length == 0) {
				throw new IllegalArgumentException("voicePrompts must not be empty");
			}
			if (visualPrompt == null) {
				throw new NullPointerException("visualPrompt must not be null");
			}
			this.mVoicePrompts = voicePrompts;
			this.mVisualPrompt = visualPrompt;
		}

		/**
		 * Constructs a prompt set with single prompt used for all interactions. This is most useful
		 * in test apps. Non-trivial apps should prefer the detailed constructor.
		 */
		public Prompt(@NonNull CharSequence prompt) {
			this.mVoicePrompts = new CharSequence[] {prompt};
			this.mVisualPrompt = prompt;
		}

		/**
		 * Returns a prompt to use for voice interactions.
		 */
		@NonNull
		public CharSequence getVoicePromptAt(int index) {
			return mVoicePrompts[index];
		}

		/**
		 * Returns the number of different voice prompts.
		 */
		public int countVoicePrompts() {
			return mVoicePrompts.length;
		}

		/**
		 * Returns the prompt to use for visual display.
		 */
		@NonNull
		public CharSequence getVisualPrompt() {
			return mVisualPrompt;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(128);
			DebugUtils.buildShortClassTag(this, sb);
			if (mVisualPrompt != null && mVoicePrompts != null && mVoicePrompts.length == 1
			    && mVisualPrompt.equals(mVoicePrompts[0])) {
				sb.append(" ");
				sb.append(mVisualPrompt);
			} else {
				if (mVisualPrompt != null) {
					sb.append(" visual=");
					sb.append(mVisualPrompt);
				}
				if (mVoicePrompts != null) {
					sb.append(", voice=");
					for (int i = 0; i < mVoicePrompts.length; i++) {
						if (i > 0)
							sb.append(" | ");
						sb.append(mVoicePrompts[i]);
					}
				}
			}
			sb.append('}');
			return sb.toString();
		}

		/**
		 * Constructor to support Parcelable behavior.
		 */
		Prompt(Parcel in) {
			mVoicePrompts = in.readCharSequenceArray();
			mVisualPrompt = in.readCharSequence();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeCharSequenceArray(mVoicePrompts);
			dest.writeCharSequence(mVisualPrompt);
		}

		public static final @android.annotation.NonNull Creator<Prompt> CREATOR = new Creator<Prompt>() {
			public Prompt createFromParcel(Parcel in) {
				return new Prompt(in);
			}

			public Prompt[] newArray(int size) {
				return new Prompt[size];
			}
		};
	}


	/**
	 * Submit a new {@link Request} to the voice interaction service.
	 *
	 * @return false: ATL has no voice interaction service, the request is not submitted
	 */
	public boolean submitRequest(Request request) {
		return submitRequest(request, null);
	}

	/**
	 * Submit a new {@link Request} to the voice interaction service.
	 *
	 * @return false: ATL has no voice interaction service, the request is not submitted
	 */
	public boolean submitRequest(Request request, String name) {
		return false;
	}

	/**
	 * Return all currently active requests.
	 */
	public Request[] getActiveRequests() {
		return new Request[0];
	}

	/**
	 * Return any currently active request that was submitted with the given name.
	 */
	public Request getActiveRequest(String name) {
		return null;
	}

	/**
	 * Queries the supported commands available from the VoiceInteractionService.
	 *
	 * @return one boolean for each command, false: ATL has no voice interaction service
	 */
	public boolean[] supportsCommands(String[] commands) {
		return new boolean[commands.length];
	}

	/**
	 * @return whether the voice interactor has been destroyed, true: ATL has no voice interaction service
	 */
	public boolean isDestroyed() {
		return true;
	}

	/**
	 * Registers a callback to be called when the VoiceInteractor is destroyed.
	 *
	 * @return false: the VoiceInteractor is destroyed
	 */
	public boolean registerOnDestroyedCallback(@NonNull java.util.concurrent.Executor executor, @NonNull Runnable callback) {
		return false;
	}

	/**
	 * Unregisters a previously registered onDestroy callback
	 */
	public boolean unregisterOnDestroyedCallback(@NonNull Runnable callback) {
		return false;
	}

	/**
	 * Notifies the assist framework that the direct actions of the activity changed.
	 */
	public void notifyDirectActionsChanged() {
	}

	/**
	 * Returns the package name of the voice interaction service, null: ATL has none
	 */
	public String getPackageName() {
		return null;
	}
}
