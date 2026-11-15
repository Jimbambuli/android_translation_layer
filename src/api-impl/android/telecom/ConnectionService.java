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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code ConnectionService} is an abstract service that should be implemented by any app which
 * either:
 * <ol>
 *     <li>Can make phone calls (VoIP or otherwise) and wants those calls to be integrated into the
 *     built-in phone app.</li>
 *     <li>Is a service which can place a call on behalf of the user.</li>
 * </ol>
 * <p>
 * ATL: AOSP's class (telecomm/framework/java/android/telecom/ConnectionService.java, android-16.0.0_r4) without
 * the Telecom side. ATL has no Telecom service, so nothing binds this service: onBind() has no IConnectionService
 * stub to return (null, which makes a bindService() client see a refused connection), the on* callbacks keep
 * AOSP's default bodies, and the connection/conference bookkeeping (add*, getAll*, removeConnection) is AOSP's
 * minus the calls on the ConnectionServiceAdapter (mAdapter), which would only talk to Telecom. The Remote*
 * calls end as in AOSP when RemoteConnectionManager holds no remote service.
 */
public abstract class ConnectionService extends Service {
	/**
	 * The {@link Intent} that must be declared as handled by the service.
	 */
	public static final String SERVICE_INTERFACE = "android.telecom.ConnectionService";

	private final Map<String, Connection> mConnectionById = new HashMap<>();
	private final Map<Connection, String> mIdByConnection = new HashMap<>();
	private final Map<String, Conference> mConferenceById = new HashMap<>();
	private final Map<Conference, String> mIdByConference = new HashMap<>();
	private final Object mIdSyncRoot = new Object();
	private int mId = 0;

	private final Conference.Listener mConferenceListener = new Conference.Listener() {
		@Override
		public void onDestroyed(Conference conference) {
			removeConference(conference);
		}
	};

	private final Connection.Listener mConnectionListener = new Connection.Listener() {
		@Override
		public void onDestroyed(Connection c) {
			removeConnection(c);
		}
	};

	public ConnectionService() {}

	@Override
	public final IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		endAllConnections();
		return super.onUnbind(intent);
	}

	public final void addConferenceFromConnection(@NonNull Conference conference,
			@NonNull Connection originalConnection) {
		if (originalConnection == null || conference == null) {
			throw new IllegalArgumentException("A NonNull parameter is null");
		}
		String id = mIdByConnection.get(originalConnection);
		if (id != null) {
			mConnectionById.remove(id);
			mIdByConnection.remove(originalConnection);
			onConnectionRemoved(originalConnection);
		} else {
			throw new IllegalArgumentException("Original connection not found in CS");
		}
		if (mIdByConference.containsKey(conference)) {
			throw new IllegalArgumentException("This conference has already been added");
		} else {
			mConferenceById.put(id, conference);
			mIdByConference.put(conference, id);
			conference.addListener(mConferenceListener);
		}
		conference.setTelecomCallId(id);
		onConferenceAdded(conference);
	}

	/**
	 * Adds a new conference call. When a conference call is created either as a result of an
	 * explicit request via {@link #onConference} or otherwise, the connection service should supply
	 * an instance of {@link Conference} by invoking this method.
	 */
	public final void addConference(Conference conference) {
		String id = addConferenceInternal(conference);
		if (id != null) {
			conference.setTelecomCallId(id);
			onConferenceAdded(conference);
		}
	}

	/**
	 * Adds a connection created by the {@link ConnectionService} and informs telecom of the new
	 * connection.
	 */
	public final void addExistingConnection(PhoneAccountHandle phoneAccountHandle,
			Connection connection) {
		addExistingConnection(phoneAccountHandle, connection, null);
	}

	/**
	 * Call to inform Telecom that your {@link ConnectionService} has released call resource
	 * (see {@link #onConnectionServiceFocusLost()}). AOSP forwards this to Telecom through the adapter.
	 */
	public final void connectionServiceFocusReleased() {}

	/* AOSP: public @SystemApi overload, used by addExistingConnection(PhoneAccountHandle, Connection) */
	public final void addExistingConnection(@NonNull PhoneAccountHandle phoneAccountHandle,
			@NonNull Connection connection, @Nullable Conference conference) {
		addExistingConnectionInternal(phoneAccountHandle, connection);
	}

	/**
	 * Gets the list of {@link Connection}s created by this {@link ConnectionService}.
	 */
	public final Collection<Connection> getAllConnections() {
		return mConnectionById.values();
	}

	/**
	 * Gets the list of {@link Conference}s created by this {@link ConnectionService}.
	 */
	public final Collection<Conference> getAllConferences() {
		return mConferenceById.values();
	}

	public Connection onCreateIncomingConnection(
			PhoneAccountHandle connectionManagerPhoneAccount,
			ConnectionRequest request) {
		return null;
	}

	public @Nullable Conference onCreateIncomingConference(
			@NonNull PhoneAccountHandle connectionManagerPhoneAccount,
			@NonNull ConnectionRequest request) {
		return null;
	}

	public void onCreateConnectionComplete(@NonNull Connection connection) {}

	public void onCreateConferenceComplete(@NonNull Conference conference) {}

	public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
			ConnectionRequest request) {}

	public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
			ConnectionRequest request) {}

	public void onCreateIncomingConferenceFailed(
			@Nullable PhoneAccountHandle connectionManagerPhoneAccount,
			@Nullable ConnectionRequest request) {}

	public void onCreateOutgoingConferenceFailed(
			@NonNull PhoneAccountHandle connectionManagerPhoneAccount,
			@NonNull ConnectionRequest request) {}

	public void triggerConferenceRecalculate() {}

	public Connection onCreateOutgoingConnection(
			PhoneAccountHandle connectionManagerPhoneAccount,
			ConnectionRequest request) {
		return null;
	}

	public @Nullable Conference onCreateOutgoingConference(
			@NonNull PhoneAccountHandle connectionManagerPhoneAccount,
			@NonNull ConnectionRequest request) {
		return null;
	}

	public Connection onCreateOutgoingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle,
			ConnectionRequest request) {
		return null;
	}

	public Connection onCreateIncomingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle,
			ConnectionRequest request) {
		return null;
	}

	public void onHandoverFailed(ConnectionRequest request, int error) {
		return;
	}

	public @Nullable Connection onCreateUnknownConnection(
			@NonNull PhoneAccountHandle connectionManagerPhoneAccount,
			@NonNull ConnectionRequest request) {
		return null;
	}

	public void onConference(Connection connection1, Connection connection2) {}

	public void onConnectionAdded(Connection connection) {}

	public void onConnectionRemoved(Connection connection) {}

	public void onConferenceAdded(Conference conference) {}

	public void onConferenceRemoved(Conference conference) {}

	public void onConnectionServiceFocusLost() {}

	public void onConnectionServiceFocusGained() {}

	public boolean containsConference(Conference conference) {
		return mIdByConference.containsKey(conference);
	}

	private String addExistingConnectionInternal(PhoneAccountHandle handle, Connection connection) {
		String id;
		if (connection.getExtras() != null && connection.getExtras()
				.containsKey(Connection.EXTRA_ORIGINAL_CONNECTION_ID)) {
			id = connection.getExtras().getString(Connection.EXTRA_ORIGINAL_CONNECTION_ID);
		} else if (handle == null) {
			// If no phone account handle was provided, we cannot be sure the call ended up
			// using the same ConnectionService, so use a random UUID.
			id = UUID.randomUUID().toString();
		} else {
			id = handle.getComponentName().getClassName() + "@" + getNextCallId();
		}
		addConnection(handle, id, connection);
		return id;
	}

	private void addConnection(PhoneAccountHandle handle, String callId, Connection connection) {
		connection.setTelecomCallId(callId);
		mConnectionById.put(callId, connection);
		mIdByConnection.put(connection, callId);
		connection.addConnectionListener(mConnectionListener);
		connection.setConnectionService(this);
		connection.setPhoneAccountHandle(handle);
		onConnectionAdded(connection);
	}

	protected void removeConnection(Connection connection) {
		connection.unsetConnectionService(this);
		connection.removeConnectionListener(mConnectionListener);
		String id = mIdByConnection.get(connection);
		if (id != null) {
			mConnectionById.remove(id);
			mIdByConnection.remove(connection);
			onConnectionRemoved(connection);
		}
	}

	private String addConferenceInternal(Conference conference) {
		String originalId = null;
		if (conference.getExtras() != null && conference.getExtras()
				.containsKey(Connection.EXTRA_ORIGINAL_CONNECTION_ID)) {
			originalId = conference.getExtras().getString(Connection.EXTRA_ORIGINAL_CONNECTION_ID);
		}
		if (mIdByConference.containsKey(conference)) {
			// already added: AOSP only logs a warning
		} else if (conference != null) {
			String id = originalId == null ? UUID.randomUUID().toString() : originalId;
			mConferenceById.put(id, conference);
			mIdByConference.put(conference, id);
			conference.addListener(mConferenceListener);
			return id;
		}
		return null;
	}

	private void removeConference(Conference conference) {
		if (mIdByConference.containsKey(conference)) {
			conference.removeListener(mConferenceListener);
			String id = mIdByConference.get(conference);
			mConferenceById.remove(id);
			mIdByConference.remove(conference);
			onConferenceRemoved(conference);
		}
	}

	private void endAllConnections() {
		// Unbound, we need to disconnect all of the connections.
		for (Connection connection : new ArrayList<Connection>(mIdByConnection.keySet())) {
			if (connection.getConference() == null) {
				connection.onDisconnect();
			}
		}
		for (Conference conference : new ArrayList<Conference>(mIdByConference.keySet())) {
			conference.onDisconnect();
		}
	}

	private int getNextCallId() {
		synchronized (mIdSyncRoot) {
			return ++mId;
		}
	}
}
