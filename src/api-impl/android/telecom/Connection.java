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

package android.telecom;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.Uri;
import android.os.OutcomeReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Represents a phone call or connection to a remote endpoint that carries voice and/or video
 * traffic.
 *
 * ATL: a subset of the AOSP class. ATL has no Telecom service (PackageManager reports neither
 * android.software.telecom nor android.software.connectionservice), so nothing ever listens to a
 * Connection: the setters keep the AOSP state handling but drop the listener fan-out to Telecom,
 * and getCallAudioState()/getCurrentCallEndpoint() stay null because Telecom never reports them.
 * Apps still load and use subclasses as plain call-state holders (e.g. Conversations'
 * CallIntegration).
 */
public abstract class Connection extends Conferenceable {

	/**
	 * @hide
	 */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(prefix = "STATE_", value = {
			STATE_INITIALIZING,
			STATE_NEW,
			STATE_RINGING,
			STATE_DIALING,
			STATE_ACTIVE,
			STATE_HOLDING,
			STATE_DISCONNECTED,
			STATE_PULLING_CALL
	})
	public @interface ConnectionState {}

	/**
	 * The connection is initializing. This is generally the first state for a {@code Connection}
	 * returned by a {@link ConnectionService}.
	 */
	public static final int STATE_INITIALIZING = 0;

	/**
	 * The connection is new and not connected.
	 */
	public static final int STATE_NEW = 1;

	/**
	 * An incoming connection is in the ringing state. During this state, the user's ringer or
	 * vibration feature will be activated.
	 */
	public static final int STATE_RINGING = 2;

	/**
	 * An outgoing connection is in the dialing state. In this state the other party has not yet
	 * answered the call and the user traditionally hears a ringback tone.
	 */
	public static final int STATE_DIALING = 3;

	/**
	 * A connection is active. Both parties are connected to the call and can actively communicate.
	 */
	public static final int STATE_ACTIVE = 4;

	/**
	 * A connection is on hold.
	 */
	public static final int STATE_HOLDING = 5;

	/**
	 * A connection has been disconnected. This is the final state once the user has been
	 * disconnected from a call either locally, remotely or by an error in the service.
	 */
	public static final int STATE_DISCONNECTED = 6;

	/**
	 * The state of an external connection which is in the process of being pulled from a remote
	 * device to the local device.
	 */
	public static final int STATE_PULLING_CALL = 7;

	private int mState = STATE_NEW;
	private CallAudioState mCallAudioState;
	private CallEndpoint mCallEndpoint;
	private Uri mAddress;
	private int mAddressPresentation;
	private String mCallerDisplayName;
	private int mCallerDisplayNamePresentation;
	private boolean mRingbackRequested = false;
	private int mConnectionCapabilities;
	private int mConnectionProperties;
	private int mVideoState;
	private DisconnectCause mDisconnectCause;

	/**
	 * Create a new Connection.
	 */
	public Connection() {}

	/* AOSP Connection extras and audio-mode state. ATL has no Telecom listeners, so the changes are only kept
	 * (getExtras()/getAudioModeIsVoip() answer them); nothing is forwarded. */
	public static final int PROPERTY_SELF_MANAGED = 1 << 7;

	private boolean mAudioModeIsVoip;
	private android.os.Bundle mExtras;
	private final Object mExtrasLock = new Object();
	private java.util.Set<String> mPreviousExtraKeys;

	public final boolean getAudioModeIsVoip() {
		return mAudioModeIsVoip;
	}

	public final void setAudioModeIsVoip(boolean isVoip) {
		if (!isVoip && (mConnectionProperties & PROPERTY_SELF_MANAGED) == PROPERTY_SELF_MANAGED)
			return; // AOSP ignores this for a self-managed connection
		checkImmutable();
		mAudioModeIsVoip = isVoip;
	}

	public final android.os.Bundle getExtras() {
		synchronized (mExtrasLock) {
			return mExtras != null ? new android.os.Bundle(mExtras) : null;
		}
	}

	public final void putExtras(@NonNull android.os.Bundle extras) {
		checkImmutable();
		if (extras == null)
			return;
		synchronized (mExtrasLock) {
			if (mExtras == null)
				mExtras = new android.os.Bundle();
			mExtras.putAll(extras);
		}
	}

	public final void removeExtras(List<String> keys) {
		synchronized (mExtrasLock) {
			if (mExtras != null) {
				for (String key : keys)
					mExtras.remove(key);
			}
		}
	}

	public final void removeExtras(String... keys) {
		removeExtras(java.util.Arrays.asList(keys));
	}

	public final void setExtras(android.os.Bundle extras) {
		checkImmutable();
		putExtras(extras);
		if (mPreviousExtraKeys != null) {
			List<String> toRemove = new java.util.ArrayList<String>();
			for (String oldKey : mPreviousExtraKeys) {
				if (extras == null || !extras.containsKey(oldKey))
					toRemove.add(oldKey);
			}
			if (!toRemove.isEmpty())
				removeExtras(toRemove);
		}
		if (mPreviousExtraKeys == null)
			mPreviousExtraKeys = new android.util.ArraySet<String>();
		mPreviousExtraKeys.clear();
		if (extras != null)
			mPreviousExtraKeys.addAll(extras.keySet());
	}

	/**
	 * @return The address (e.g., phone number) to which this Connection is currently communicating.
	 */
	public final Uri getAddress() {
		return mAddress;
	}

	/**
	 * @return The presentation requirements for the address.
	 *         See {@link TelecomManager} for valid values.
	 */
	public final int getAddressPresentation() {
		return mAddressPresentation;
	}

	/**
	 * @return The caller display name (CNAP).
	 */
	public final String getCallerDisplayName() {
		return mCallerDisplayName;
	}

	/**
	 * @return The presentation requirements for the handle.
	 *         See {@link TelecomManager} for valid values.
	 */
	public final int getCallerDisplayNamePresentation() {
		return mCallerDisplayNamePresentation;
	}

	/**
	 * @return The state of this Connection.
	 */
	public final int getState() {
		return mState;
	}

	/**
	 * Returns the video state of the connection.
	 *
	 * @return The video state of the connection.
	 */
	public final int getVideoState() {
		return mVideoState;
	}

	/**
	 * @return The audio state of the connection, describing how its audio is currently
	 *         being routed by the system. This is {@code null} if this Connection
	 *         does not directly know about its audio state.
	 * @deprecated Use {@link #getCurrentCallEndpoint()},
	 * {@link #onAvailableCallEndpointsChanged(List)} and
	 * {@link #onMuteStateChanged(boolean)} instead.
	 */
	@Deprecated
	public final CallAudioState getCallAudioState() {
		return mCallAudioState;
	}

	/**
	 * Returns whether this connection is requesting that the system play a ringback tone
	 * on its behalf.
	 */
	public final boolean isRingbackRequested() {
		return mRingbackRequested;
	}

	/**
	 * @return The {@link DisconnectCause} for this connection.
	 */
	public final DisconnectCause getDisconnectCause() {
		return mDisconnectCause;
	}

	/**
	 * @param state An integer value of a {@code STATE_*} constant.
	 * @return A string representation of the value.
	 */
	public static String stateToString(int state) {
		switch (state) {
			case STATE_INITIALIZING:
				return "INITIALIZING";
			case STATE_NEW:
				return "NEW";
			case STATE_RINGING:
				return "RINGING";
			case STATE_DIALING:
				return "DIALING";
			case STATE_PULLING_CALL:
				return "PULLING_CALL";
			case STATE_ACTIVE:
				return "ACTIVE";
			case STATE_HOLDING:
				return "HOLDING";
			case STATE_DISCONNECTED:
				return "DISCONNECTED";
			default:
				return "UNKNOWN";
		}
	}

	/**
	 * Returns the connection's capabilities, as a bit mask of the {@code CAPABILITY_*} constants.
	 */
	public final int getConnectionCapabilities() {
		return mConnectionCapabilities;
	}

	/**
	 * Returns the connection's properties, as a bit mask of the {@code PROPERTY_*} constants.
	 */
	public final int getConnectionProperties() {
		return mConnectionProperties;
	}

	/**
	 * Sets the value of the {@link #getAddress()} property.
	 *
	 * @param address The new address.
	 * @param presentation The presentation requirements for the address.
	 *        See {@link TelecomManager} for valid values.
	 */
	public final void setAddress(Uri address, int presentation) {
		mAddress = address;
		mAddressPresentation = presentation;
	}

	/**
	 * Sets the caller display name (CNAP).
	 *
	 * @param callerDisplayName The new display name.
	 * @param presentation The presentation requirements for the handle.
	 *        See {@link TelecomManager} for valid values.
	 */
	public final void setCallerDisplayName(String callerDisplayName, int presentation) {
		checkImmutable();
		boolean nameChanged = !Objects.equals(mCallerDisplayName, callerDisplayName);
		boolean presentationChanged = mCallerDisplayNamePresentation != presentation;
		if (nameChanged) {
			// Ensure the new name is not clobbering the old one with a null value due to the caller
			// wanting to only set the presentation and not knowing the display name.
			mCallerDisplayName = callerDisplayName;
		}
		if (presentationChanged) {
			mCallerDisplayNamePresentation = presentation;
		}
	}

	/**
	 * Set the video state for the connection.
	 *
	 * @param videoState The new video state.
	 */
	public final void setVideoState(int videoState) {
		checkImmutable();
		mVideoState = videoState;
	}

	/**
	 * Sets state to active (e.g., an ongoing connection where two or more parties can actively
	 * communicate).
	 */
	public final void setActive() {
		checkImmutable();
		setRingbackRequested(false);
		setState(STATE_ACTIVE);
	}

	/**
	 * Sets state to ringing (e.g., an inbound ringing connection).
	 */
	public final void setRinging() {
		checkImmutable();
		setState(STATE_RINGING);
	}

	/**
	 * Sets state to initializing (this Connection is not yet ready to be used).
	 */
	public final void setInitializing() {
		checkImmutable();
		setState(STATE_INITIALIZING);
	}

	/**
	 * Sets state to initialized (the Connection has been set up and is now ready to be used).
	 */
	public final void setInitialized() {
		checkImmutable();
		setState(STATE_NEW);
	}

	/**
	 * Sets state to dialing (e.g., dialing an outbound connection).
	 */
	public final void setDialing() {
		checkImmutable();
		setState(STATE_DIALING);
	}

	/**
	 * Sets state to pulling (e.g. the connection is being pulled to the local device from another
	 * device).
	 */
	public final void setPulling() {
		checkImmutable();
		setState(STATE_PULLING_CALL);
	}

	/**
	 * Sets state to be on hold.
	 */
	public final void setOnHold() {
		checkImmutable();
		setState(STATE_HOLDING);
	}

	/**
	 * Sets state to disconnected.
	 *
	 * @param disconnectCause The reason for the disconnection, as specified by
	 *         {@link DisconnectCause}.
	 */
	public final void setDisconnected(DisconnectCause disconnectCause) {
		checkImmutable();
		mDisconnectCause = disconnectCause;
		setState(STATE_DISCONNECTED);
	}

	/**
	 * Requests that the framework play a ringback tone. This is to be invoked by implementations
	 * that do not play a ringback tone themselves in the connection's audio stream.
	 *
	 * @param ringback Whether the ringback tone is to be played.
	 */
	public final void setRingbackRequested(boolean ringback) {
		checkImmutable();
		mRingbackRequested = ringback;
	}

	/**
	 * Sets the connection's capabilities as a bit mask of the {@code CAPABILITY_*} constants.
	 *
	 * @param connectionCapabilities The new connection capabilities.
	 */
	public final void setConnectionCapabilities(int connectionCapabilities) {
		checkImmutable();
		mConnectionCapabilities = connectionCapabilities;
	}

	/**
	 * Sets the connection's properties as a bit mask of the {@code PROPERTY_*} constants.
	 *
	 * @param connectionProperties The new connection properties.
	 */
	public final void setConnectionProperties(int connectionProperties) {
		checkImmutable();
		mConnectionProperties = connectionProperties;
	}

	/**
	 * Tears down the Connection object.
	 */
	public final void destroy() {
		// AOSP only notifies the Telecom listeners here; ATL has none.
	}

	/**
	 * Sets the audio route (speaker, bluetooth, etc...).  When this request is honored, there will
	 * be change to the {@link #getCallAudioState()}.
	 *
	 * @param route The audio route to use.
	 * @deprecated Use {@link #requestCallEndpointChange(CallEndpoint, Executor, OutcomeReceiver)}
	 * instead.
	 */
	@Deprecated
	public final void setAudioRoute(int route) {
		// AOSP forwards the request to the Telecom listeners; ATL has none, so it is never honored.
	}

	/**
	 * Request audio routing to a specific CallEndpoint. When this request is honored, there will
	 * be change to the {@link #getCurrentCallEndpoint()}.
	 *
	 * @param endpoint The call endpoint to use.
	 * @param executor The executor of where the callback will execute.
	 * @param callback The callback to notify the result of the endpoint change.
	 */
	public final void requestCallEndpointChange(@NonNull CallEndpoint endpoint,
			@NonNull @CallbackExecutor Executor executor,
			@NonNull OutcomeReceiver<Void, CallEndpointException> callback) {
		// AOSP forwards the request to the Telecom listeners; ATL has none, so it is never honored.
	}

	/**
	 * Obtains the current CallEndpoint.
	 *
	 * @return An object encapsulating the CallEndpoint.
	 */
	@NonNull
	public final CallEndpoint getCurrentCallEndpoint() {
		return mCallEndpoint;
	}

	/**
	 * Notifies this Connection that the {@link #getCallAudioState()} property has a new value.
	 *
	 * @param state The new connection audio state.
	 * @deprecated Use {@link #onCallEndpointChanged(CallEndpoint)},
	 * {@link #onAvailableCallEndpointsChanged(List)} and
	 * {@link #onMuteStateChanged(boolean)} instead.
	 */
	@Deprecated
	public void onCallAudioStateChanged(CallAudioState state) {}

	/**
	 * Notifies this Connection that the audio endpoint has been changed.
	 *
	 * @param callEndpoint The current CallEndpoint.
	 */
	public void onCallEndpointChanged(@NonNull CallEndpoint callEndpoint) {}

	/**
	 * Notifies this Connection that the available call endpoints have been changed.
	 *
	 * @param availableEndpoints The set of available CallEndpoint.
	 */
	public void onAvailableCallEndpointsChanged(@NonNull List<CallEndpoint> availableEndpoints) {}

	/**
	 * Notifies this Connection that its audio mute state has been changed.
	 *
	 * @param isMuted The current mute state.
	 */
	public void onMuteStateChanged(boolean isMuted) {}

	/**
	 * Notifies this Connection of an internal state change. This method is called after the
	 * state is changed.
	 *
	 * @param state The new state, one of the {@code STATE_*} constants.
	 */
	public void onStateChanged(int state) {}

	/**
	 * Notifies this Connection of a request to disconnect.
	 */
	public void onDisconnect() {}

	/**
	 * Notifies this Connection of a request to abort.
	 */
	public void onAbort() {}

	/* AOSP Connection callbacks (API 23/25/26), empty unless the app overrides them; the Telecom sources are not
	 * in the local AOSP copy. ATL has no Telecom service that would call them. */
	public void onUnhold() {}

	public void onCallEvent(String event, android.os.Bundle extras) {}

	public void onExtrasChanged(android.os.Bundle extras) {}

	/* AOSP Connection.requestBluetoothAudio() (API 28): asks Telecom (through the connection's listeners) to route
	 * the call audio to that device; ATL has no Telecom service and no Bluetooth adapter, so nobody takes the
	 * request */
	public void requestBluetoothAudio(android.bluetooth.BluetoothDevice bluetoothDevice) {}

	/**
	 * Notifies this Connection, which is in {@link #STATE_RINGING}, of
	 * a request to accept.
	 *
	 * @param videoState The video state in which to answer the connection.
	 */
	public void onAnswer(int videoState) {}

	/**
	 * Notifies this Connection, which is in {@link #STATE_RINGING}, of
	 * a request to accept.
	 */
	public void onAnswer() {
		onAnswer(0 /* VideoProfile.STATE_AUDIO_ONLY */);
	}

	/**
	 * Notifies this Connection, which is in {@link #STATE_RINGING}, of
	 * a request to reject.
	 */
	public void onReject() {}

	/**
	 * Notifies this Connection, which is in {@link #STATE_RINGING}, of
	 * a request to reject with a message.
	 */
	public void onReject(String replyMessage) {}

	/**
	 * Notifies this Connection of a request to silence the ringer.
	 */
	public void onSilence() {}

	/**
	 * Notifies this {@link Connection} that its {@link ConnectionService} is responsible for
	 * displaying its incoming call user interface for the {@link Connection}.
	 */
	public void onShowIncomingCallUi() {}

	private void setState(int state) {
		checkImmutable();
		if (mState == STATE_DISCONNECTED && mState != state) {
			return;
		}
		if (mState != state) {
			mState = state;
			onStateChanged(state);
		}
	}

	private static class FailureSignalingConnection extends Connection {
		private boolean mImmutable = false;
		public FailureSignalingConnection(DisconnectCause disconnectCause) {
			setDisconnected(disconnectCause);
			mImmutable = true;
		}

		public void checkImmutable() {
			if (mImmutable) {
				throw new UnsupportedOperationException("Connection is immutable");
			}
		}
	}

	/**
	 * Return a {@code Connection} which represents a failed connection attempt. The returned
	 * {@code Connection} will have a {@link android.telecom.DisconnectCause} and as specified,
	 * and a {@link #getState()} of {@link #STATE_DISCONNECTED}.
	 *
	 * @param disconnectCause The disconnect cause, ({@see android.telecomm.DisconnectCause}).
	 * @return A {@code Connection} which indicates failure.
	 */
	public static Connection createFailedConnection(DisconnectCause disconnectCause) {
		return new FailureSignalingConnection(disconnectCause);
	}

	/**
	 * Override to throw an {@link UnsupportedOperationException} if this {@code Connection} is
	 * not intended to be mutated, e.g., if it is a marker for failure. Only for framework use;
	 * this should never be un-@hide-den.
	 *
	 * @hide
	 */
	public void checkImmutable() {}
}
