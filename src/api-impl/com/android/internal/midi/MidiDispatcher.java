package com.android.internal.midi;

import android.media.midi.MidiReceiver;
import android.media.midi.MidiSender;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/* AOSP core/java/com/android/internal/midi/MidiDispatcher.java (used by MidiOutputPort) */
public final class MidiDispatcher extends MidiReceiver {
	public interface MidiReceiverFailureHandler {
		void onReceiverFailure(MidiReceiver receiver, IOException failure);
	}

	private final MidiReceiverFailureHandler mFailureHandler;
	private final CopyOnWriteArrayList<MidiReceiver> mReceivers = new CopyOnWriteArrayList<MidiReceiver>();

	private final MidiSender mSender = new MidiSender() {
		@Override
		public void onConnect(MidiReceiver receiver) {
			mReceivers.add(receiver);
		}

		@Override
		public void onDisconnect(MidiReceiver receiver) {
			mReceivers.remove(receiver);
		}
	};

	public MidiDispatcher() {
		this(null);
	}

	public MidiDispatcher(MidiReceiverFailureHandler failureHandler) {
		mFailureHandler = failureHandler;
	}

	public int getReceiverCount() {
		return mReceivers.size();
	}

	public MidiSender getSender() {
		return mSender;
	}

	@Override
	public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
		for (MidiReceiver receiver : mReceivers) {
			try {
				receiver.send(msg, offset, count, timestamp);
			} catch (IOException e) {
				mReceivers.remove(receiver);
				if (mFailureHandler != null) {
					mFailureHandler.onReceiverFailure(receiver, e);
				}
			}
		}
	}

	@Override
	public void onFlush() throws IOException {
		for (MidiReceiver receiver : mReceivers) {
			try {
				receiver.flush();
			} catch (IOException e) {
				mReceivers.remove(receiver);
				if (mFailureHandler != null) {
					mFailureHandler.onReceiverFailure(receiver, e);
				}
			}
		}
	}
}
