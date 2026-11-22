/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media;

import android.annotation.NonNull;

import java.util.Objects;

/**
 * Class to provide information about the audio devices.
 *
 * ATL: AOSP's AudioDeviceInfo describes an AudioDevicePort of the audio policy. ATL has no audio policy:
 * AudioManager.getDevices() answers the one output (TYPE_BUILTIN_SPEAKER, the default ALSA device) and
 * the one input (TYPE_BUILTIN_MIC) the app plays on and records from. The device does not tell
 * its sample rates, channel masks or encodings (the arrays are empty, as AOSP's for a device that accepts
 * all of them).
 */
public final class AudioDeviceInfo {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_BUILTIN_EARPIECE = 1;
	public static final int TYPE_BUILTIN_SPEAKER = 2;
	public static final int TYPE_WIRED_HEADSET = 3;
	public static final int TYPE_WIRED_HEADPHONES = 4;
	public static final int TYPE_LINE_ANALOG = 5;
	public static final int TYPE_LINE_DIGITAL = 6;
	public static final int TYPE_BLUETOOTH_SCO = 7;
	public static final int TYPE_BLUETOOTH_A2DP = 8;
	public static final int TYPE_HDMI = 9;
	public static final int TYPE_HDMI_ARC = 10;
	public static final int TYPE_USB_DEVICE = 11;
	public static final int TYPE_USB_ACCESSORY = 12;
	public static final int TYPE_DOCK = 13;
	public static final int TYPE_FM = 14;
	public static final int TYPE_BUILTIN_MIC = 15;
	public static final int TYPE_FM_TUNER = 16;
	public static final int TYPE_TV_TUNER = 17;
	public static final int TYPE_TELEPHONY = 18;
	public static final int TYPE_AUX_LINE = 19;

	/* AOSP AudioDeviceInfo.convertInternalDeviceToDeviceType(): INT_TO_EXT_DEVICE_MAPPING.get(intDevice, TYPE_UNKNOWN).
	 * Only the entry of the one internal device ATL hands out (AudioManager.getMicrophones():
	 * AUDIO_DEVICE_IN_BUILTIN_MIC) is carried over */
	public static int convertInternalDeviceToDeviceType(int intDevice) {
		return intDevice == 0x80000004 /* AudioSystem.DEVICE_IN_BUILTIN_MIC */ ? TYPE_BUILTIN_MIC : TYPE_UNKNOWN;
	}
	public static final int TYPE_IP = 20;
	public static final int TYPE_BUS = 21;
	public static final int TYPE_USB_HEADSET = 22;
	public static final int TYPE_HEARING_AID = 23;
	public static final int TYPE_BUILTIN_SPEAKER_SAFE = 24;
	public static final int TYPE_REMOTE_SUBMIX = 25;
	public static final int TYPE_BLE_HEADSET = 26;
	public static final int TYPE_BLE_SPEAKER = 27;
	public static final int TYPE_ECHO_REFERENCE = 28;
	public static final int TYPE_HDMI_EARC = 29;
	public static final int TYPE_BLE_BROADCAST = 30;
	public static final int TYPE_DOCK_ANALOG = 31;

	private final int mId;
	private final int mType;
	private final String mName;
	private final boolean mIsSink;
	private final String mAddress;

	/** @hide */
	AudioDeviceInfo(int id, int type, String name, boolean isSink) {
		this(id, type, name, isSink, "");
	}

	/** @hide address: the Bluetooth address for Bluetooth devices (AudioDevicePort.address()) */
	AudioDeviceInfo(int id, int type, String name, boolean isSink, String address) {
		mId = id;
		mType = type;
		mName = name;
		mIsSink = isSink;
		mAddress = address;
	}

	/**
	 * @return The internal device ID.
	 */
	public int getId() {
		return mId;
	}

	/**
	 * @return The human-readable name of the audio device.
	 */
	public CharSequence getProductName() {
		return mName;
	}

	/**
	 * @return The "address" string of the device. This generally contains device-specific
	 * parameters.
	 */
	public @NonNull String getAddress() {
		return mAddress;
	}

	/**
	 * @return true if the audio device is a source for audio data (e.g. a microphone).
	 */
	public boolean isSource() {
		return !mIsSink;
	}

	/**
	 * @return true if the audio device is a sink for audio data (e.g. a speaker).
	 */
	public boolean isSink() {
		return mIsSink;
	}

	/**
	 * @return An array of sample rates supported by the audio device, empty: any
	 */
	public @NonNull int[] getSampleRates() {
		return new int[0];
	}

	/**
	 * @return An array of channel position masks (e.g. {@link AudioFormat#CHANNEL_IN_STEREO})
	 * supported by the audio device, empty: any
	 */
	public @NonNull int[] getChannelMasks() {
		return new int[0];
	}

	/**
	 * @return An array of channel index masks supported by the audio device, empty: any
	 */
	public @NonNull int[] getChannelIndexMasks() {
		return new int[0];
	}

	/**
	 * @return An array of channel counts (1, 2, 4, ...) supported by the audio device, empty: any
	 */
	public @NonNull int[] getChannelCounts() {
		return new int[0];
	}

	/**
	 * @return An array of audio encodings (e.g. {@link AudioFormat#ENCODING_PCM_16BIT}) supported
	 * by the audio device, empty: any
	 */
	public @NonNull int[] getEncodings() {
		return new int[0];
	}

	/* AOSP AudioDeviceInfo.getEncapsulationModes(): mPort.encapsulationModes(); ATL's port is an ordinary
	 * output that supports no encapsulation, i.e. an empty array */
	public @NonNull int[] getEncapsulationModes() {
		return new int[0];
	}

	/* AOSP AudioDeviceInfo.getEncapsulationMetadataTypes(): mPort.encapsulationMetadataTypes(); none for ATL's port */
	public @NonNull int[] getEncapsulationMetadataTypes() {
		return new int[0];
	}

	/* AOSP AudioDeviceInfo.getAudioProfiles() (API 31): the profiles of the device port; ATL's ports carry none
	 * (like the empty getSampleRates()/getEncodings(): any) */
	public @NonNull java.util.List<AudioProfile> getAudioProfiles() {
		return new java.util.ArrayList<>();
	}

	/* AOSP AudioDeviceInfo.getAudioDescriptors() (API 31): the port's extra descriptors (EDID blocks), none here */
	public @NonNull java.util.List<AudioDescriptor> getAudioDescriptors() {
		return new java.util.ArrayList<>();
	}

	/**
	 * @return The speaker layout of a TYPE_BUILTIN_SPEAKER device (API 36). ATL's output is whatever ALSA
	 * plays on, whose layout is not known: AudioFormat.CHANNEL_INVALID, as for a device whose audio HAL
	 * reports none.
	 */
	public int getSpeakerLayoutChannelMask() {
		return AudioFormat.CHANNEL_INVALID;
	}

	/**
	 * @return The device type identifier of the audio device (i.e. TYPE_BUILTIN_SPEAKER).
	 */
	public int getType() {
		return mType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AudioDeviceInfo that = (AudioDeviceInfo)o;
		return mId == that.mId && mType == that.mType && mIsSink == that.mIsSink;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mId, mType, mIsSink);
	}
}
