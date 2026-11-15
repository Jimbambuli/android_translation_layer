package android.media;

import android.media.audiofx.AudioEffect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* AOSP media/java/android/media/AudioRecordingConfiguration.java. ATL has no AudioService; the
 * configurations describe the app's own active AudioRecords (AudioRecord.activeRecordingConfigurations()),
 * which is what AudioService reports to an unprivileged app (anonymized, uid -1, no package name). The
 * device is ATL's one input (AudioManager.getDevices()). */
public final class AudioRecordingConfiguration {
	private final int mClientUid;
	private final int mClientSessionId;
	private final int mClientSource;
	private final AudioFormat mClientFormat;
	private final AudioFormat mDeviceFormat;
	private final int mPatchHandle;
	private final String mClientPackageName;
	private final int mClientPortId;
	private final boolean mClientSilenced;
	private final int mDeviceSource;
	private final AudioEffect.Descriptor[] mClientEffects;
	private final AudioEffect.Descriptor[] mDeviceEffects;

	public AudioRecordingConfiguration(int uid, int session, int source, AudioFormat clientFormat,
	                                   AudioFormat devFormat, int patchHandle, String packageName, int clientPortId,
	                                   boolean clientSilenced, int deviceSource,
	                                   AudioEffect.Descriptor[] clientEffects, AudioEffect.Descriptor[] deviceEffects) {
		mClientUid = uid;
		mClientSessionId = session;
		mClientSource = source;
		mClientFormat = clientFormat;
		mDeviceFormat = devFormat;
		mPatchHandle = patchHandle;
		mClientPackageName = packageName;
		mClientPortId = clientPortId;
		mClientSilenced = clientSilenced;
		mDeviceSource = deviceSource;
		mClientEffects = clientEffects;
		mDeviceEffects = deviceEffects;
	}

	public AudioRecordingConfiguration(int uid, int session, int source, AudioFormat clientFormat,
	                                   AudioFormat devFormat, int patchHandle, String packageName) {
		this(uid, session, source, clientFormat, devFormat, patchHandle, packageName, 0, false,
		     /*MediaRecorder.AudioSource.DEFAULT*/ 0, new AudioEffect.Descriptor[0], new AudioEffect.Descriptor[0]);
	}

	public int getClientAudioSource() { return mClientSource; }

	public int getClientAudioSessionId() { return mClientSessionId; }

	public AudioFormat getFormat() { return mDeviceFormat; }

	public AudioFormat getClientFormat() { return mClientFormat; }

	public String getClientPackageName() { return mClientPackageName; }

	public int getClientUid() { return mClientUid; }

	public int getClientPortId() { return mClientPortId; }

	public boolean isClientSilenced() { return mClientSilenced; }

	public int getAudioSource() { return mDeviceSource; }

	/* AOSP: the input device of the recording's audio patch; ATL records from its one input */
	public AudioDeviceInfo getAudioDevice() {
		AudioDeviceInfo[] inputs = new AudioManager().getDevices(AudioManager.GET_DEVICES_INPUTS);
		return inputs.length > 0 ? inputs[0] : null;
	}

	public List<AudioEffect.Descriptor> getClientEffects() {
		return new ArrayList<AudioEffect.Descriptor>(Arrays.asList(mClientEffects));
	}

	public List<AudioEffect.Descriptor> getEffects() {
		return new ArrayList<AudioEffect.Descriptor>(Arrays.asList(mDeviceEffects));
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(mClientSessionId, mClientSource);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AudioRecordingConfiguration))
			return false;
		AudioRecordingConfiguration that = (AudioRecordingConfiguration)o;
		return mClientUid == that.mClientUid && mClientSessionId == that.mClientSessionId
		    && mClientSource == that.mClientSource && mPatchHandle == that.mPatchHandle
		    && java.util.Objects.equals(mClientFormat, that.mClientFormat)
		    && java.util.Objects.equals(mDeviceFormat, that.mDeviceFormat)
		    && java.util.Objects.equals(mClientPackageName, that.mClientPackageName)
		    && mClientPortId == that.mClientPortId && mClientSilenced == that.mClientSilenced
		    && mDeviceSource == that.mDeviceSource;
	}
    /* AOSP 16 API; no backing hardware/service in ATL */
    public int describeContents() { return 0; }
    /* AOSP 16 API; no backing hardware/service in ATL */
    public void writeToParcel(android.os.Parcel arg0, int arg1) {  }
}
