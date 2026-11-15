package android.media.audiofx;

import java.util.UUID;

/* AOSP media/java/android/media/audiofx/AudioEffect.java. ATL has no effect engines: queryEffects() lists
 * none, so isEffectTypeAvailable() is false and the effect constructors fail the way AOSP's do when
 * native_setup() finds no effect of the requested type (ERROR_BAD_VALUE -> IllegalArgumentException
 * "Effect type: ... not supported."). */
public class AudioEffect {
	public static final UUID EFFECT_TYPE_ENV_REVERB = UUID.fromString("c2e5d5f0-94bd-4763-9cac-4e234d06839e");
	public static final UUID EFFECT_TYPE_PRESET_REVERB = UUID.fromString("47382d60-ddd8-11db-bf3a-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_EQUALIZER = UUID.fromString("0bed4300-ddd6-11db-8f34-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_BASS_BOOST = UUID.fromString("0634f220-ddd4-11db-a0fc-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_VIRTUALIZER = UUID.fromString("37cc2c00-dddd-11db-8577-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_AGC = UUID.fromString("0a8abfe0-654c-11e0-ba26-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_AEC = UUID.fromString("7b491460-8d4d-11e0-bd61-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_NS = UUID.fromString("58b4b260-8e06-11e0-aa8e-0002a5d5c51b");
	public static final UUID EFFECT_TYPE_LOUDNESS_ENHANCER = UUID.fromString("fe3199be-aed0-413f-87bb-11260eb63cf1");
	public static final UUID EFFECT_TYPE_DYNAMICS_PROCESSING = UUID.fromString("7261676f-6d75-7369-6364-28e2fd3ac39e");
	public static final UUID EFFECT_TYPE_HAPTIC_GENERATOR = UUID.fromString("1411e6d6-aecd-4021-a1cf-a6aceb0d71e5");
	public static final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");

	public static final int SUCCESS = 0;
	public static final int ERROR = -1;
	public static final int ALREADY_EXISTS = -2;
	public static final int ERROR_NO_INIT = -3;
	public static final int ERROR_BAD_VALUE = -4;
	public static final int ERROR_INVALID_OPERATION = -5;
	public static final int ERROR_NO_MEMORY = -6;
	public static final int ERROR_DEAD_OBJECT = -7;

	public static class Descriptor {
		public UUID type;
		public UUID uuid;
		public String connectMode;
		public String name;
		public String implementor;

		public Descriptor() {
		}

		public Descriptor(String type, String uuid, String connectMode, String name, String implementor) {
			this.type = UUID.fromString(type);
			this.uuid = UUID.fromString(uuid);
			this.connectMode = connectMode;
			this.name = name;
			this.implementor = implementor;
		}
	}

	AudioEffect() {
	}

	/* AOSP AudioEffect(UUID, UUID, int, int): native_setup() finds no engine for the type */
	public AudioEffect(UUID type, UUID uuid, int priority, int audioSession) {
		throw new IllegalArgumentException("Effect type: " + type + " not supported.");
	}

	public static Descriptor[] queryEffects() {
		return new Descriptor[0];
	}

	/* AOSP isEffectTypeAvailable(): looks the type up in queryEffects() */
	public static boolean isEffectTypeAvailable(UUID type) {
		Descriptor[] desc = queryEffects();
		if (desc == null)
			return false;
		for (int i = 0; i < desc.length; i++) {
			if (desc[i].type.equals(type))
				return true;
		}
		return false;
	}

	public boolean getEnabled() { return false; }

	public int setEnabled(boolean enable) { return SUCCESS; }

	/* AOSP releases the native effect engine; ATL has none */
	public void release() {}
}
