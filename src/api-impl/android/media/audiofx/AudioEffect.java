package android.media.audiofx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

		public Descriptor(android.os.Parcel in) {
			type = UUID.fromString(in.readString());
			uuid = UUID.fromString(in.readString());
			connectMode = in.readString();
			name = in.readString();
			implementor = in.readString();
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(type, uuid, connectMode, name, implementor);
		}

		public void writeToParcel(android.os.Parcel dest) {
			dest.writeString(type.toString());
			dest.writeString(uuid.toString());
			dest.writeString(connectMode);
			dest.writeString(name);
			dest.writeString(implementor);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Descriptor)) return false;
			Descriptor that = (Descriptor)o;
			return (type.equals(that.type) && uuid.equals(that.uuid) && connectMode.equals(that.connectMode)
			        && name.equals(that.name) && implementor.equals(that.implementor));
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

	private Descriptor mDescriptor;

	/* AOSP finalize(): native_finalize() releases the native effect engine; ATL has none */
	@Override
	protected void finalize() {}

	/* AOSP getDescriptor(): checkState() then mDescriptor */
	public Descriptor getDescriptor() throws IllegalStateException {
		checkState("getDescriptor()");
		return mDescriptor;
	}

	/* AOSP AudioEffect.checkState(): no ATL effect has an engine, so none is ever STATE_INITIALIZED */
	public void checkState(String methodName) throws IllegalStateException {
		throw (new IllegalStateException(methodName + " called on uninitialized AudioEffect."));
	}

	/* AOSP setParameter()/getParameter()/getId()/hasControl(): checkState() before the native call */
	public int setParameter(byte[] param, byte[] value) throws IllegalStateException {
		checkState("setParameter()");
		return ERROR_NO_INIT;
	}

	public int setParameter(int param, int value) throws IllegalStateException {
		byte[] p = intToByteArray(param);
		byte[] v = intToByteArray(value);
		return setParameter(p, v);
	}

	public int setParameter(int param, short value) throws IllegalStateException {
		byte[] p = intToByteArray(param);
		byte[] v = shortToByteArray(value);
		return setParameter(p, v);
	}

	public int setParameter(int param, byte[] value) throws IllegalStateException {
		byte[] p = intToByteArray(param);
		return setParameter(p, value);
	}

	public int getParameter(byte[] param, byte[] value) throws IllegalStateException {
		checkState("getParameter()");
		return ERROR_NO_INIT;
	}

	public int getParameter(int param, byte[] value) throws IllegalStateException {
		byte[] p = intToByteArray(param);
		return getParameter(p, value);
	}

	/* AOSP AudioEffect get/setParameter() overloads over int/short arrays; they end in the byte[] ones */
	public int setParameter(int[] param, int[] value)
			throws IllegalStateException {
		if (param.length > 2 || value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}
		byte[] v = intToByteArray(value[0]);
		if (value.length > 1) {
			byte[] v2 = intToByteArray(value[1]);
			v = concatArrays(v, v2);
		}
		return setParameter(p, v);
	}

	public int setParameter(int[] param, short[] value)
			throws IllegalStateException {
		if (param.length > 2 || value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}

		byte[] v = shortToByteArray(value[0]);
		if (value.length > 1) {
			byte[] v2 = shortToByteArray(value[1]);
			v = concatArrays(v, v2);
		}
		return setParameter(p, v);
	}

	public int setParameter(int[] param, byte[] value)
			throws IllegalStateException {
		if (param.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}
		return setParameter(p, value);
	}

	public int getParameter(int param, int[] value)
			throws IllegalStateException {
		if (value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param);

		byte[] v = new byte[value.length * 4];

		int status = getParameter(p, v);

		if (status == 4 || status == 8) {
			value[0] = byteArrayToInt(v);
			if (status == 8) {
				value[1] = byteArrayToInt(v, 4);
			}
			status /= 4;
		} else {
			status = ERROR;
		}
		return status;
	}

	public int getParameter(int param, short[] value)
			throws IllegalStateException {
		if (value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param);

		byte[] v = new byte[value.length * 2];

		int status = getParameter(p, v);

		if (status == 2 || status == 4) {
			value[0] = byteArrayToShort(v);
			if (status == 4) {
				value[1] = byteArrayToShort(v, 2);
			}
			status /= 2;
		} else {
			status = ERROR;
		}
		return status;
	}

	public int getParameter(int[] param, int[] value)
			throws IllegalStateException {
		if (param.length > 2 || value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}
		byte[] v = new byte[value.length * 4];

		int status = getParameter(p, v);

		if (status == 4 || status == 8) {
			value[0] = byteArrayToInt(v);
			if (status == 8) {
				value[1] = byteArrayToInt(v, 4);
			}
			status /= 4;
		} else {
			status = ERROR;
		}
		return status;
	}

	public int getParameter(int[] param, short[] value)
			throws IllegalStateException {
		if (param.length > 2 || value.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}
		byte[] v = new byte[value.length * 2];

		int status = getParameter(p, v);

		if (status == 2 || status == 4) {
			value[0] = byteArrayToShort(v);
			if (status == 4) {
				value[1] = byteArrayToShort(v, 2);
			}
			status /= 2;
		} else {
			status = ERROR;
		}
		return status;
	}

	public int getParameter(int[] param, byte[] value)
			throws IllegalStateException {
		if (param.length > 2) {
			return ERROR_BAD_VALUE;
		}
		byte[] p = intToByteArray(param[0]);
		if (param.length > 1) {
			byte[] p2 = intToByteArray(param[1]);
			p = concatArrays(p, p2);
		}

		return getParameter(p, value);
	}

	public int getId() throws IllegalStateException {
		checkState("getId()");
		return 0;
	}

	public boolean hasControl() throws IllegalStateException {
		checkState("hasControl()");
		return false;
	}

	/* AOSP listeners: stored; ATL has no engine that would post the events (NativeEventHandler) */
	public final Object mListenerLock = new Object();
	private OnEnableStatusChangeListener mEnableStatusChangeListener = null;
	private OnControlStatusChangeListener mControlChangeStatusListener = null;
	private OnParameterChangeListener mParameterChangeListener = null;

	public void setEnableStatusListener(OnEnableStatusChangeListener listener) {
		synchronized (mListenerLock) {
			mEnableStatusChangeListener = listener;
		}
	}

	public void setControlStatusListener(OnControlStatusChangeListener listener) {
		synchronized (mListenerLock) {
			mControlChangeStatusListener = listener;
		}
	}

	public void setParameterListener(OnParameterChangeListener listener) {
		synchronized (mListenerLock) {
			mParameterChangeListener = listener;
		}
	}

	public interface OnEnableStatusChangeListener {
		void onEnableStatusChange(AudioEffect effect, boolean enabled);
	}

	public interface OnControlStatusChangeListener {
		void onControlStatusChange(AudioEffect effect, boolean controlGranted);
	}

	public interface OnParameterChangeListener {
		void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value);
	}

	public void checkStatus(int status) {
		if (isError(status)) {
			switch (status) {
			case AudioEffect.ERROR_BAD_VALUE:
				throw (new IllegalArgumentException("AudioEffect: bad parameter value"));
			case AudioEffect.ERROR_INVALID_OPERATION:
				throw (new UnsupportedOperationException("AudioEffect: invalid parameter operation"));
			default:
				throw (new RuntimeException("AudioEffect: set/get parameter error"));
			}
		}
	}

	public static boolean isError(int status) {
		return (status < 0);
	}

	public static int byteArrayToInt(byte[] valueBuf) {
		return byteArrayToInt(valueBuf, 0);
	}

	public static int byteArrayToInt(byte[] valueBuf, int offset) {
		ByteBuffer converter = ByteBuffer.wrap(valueBuf);
		converter.order(ByteOrder.nativeOrder());
		return converter.getInt(offset);
	}

	public static byte[] intToByteArray(int value) {
		ByteBuffer converter = ByteBuffer.allocate(4);
		converter.order(ByteOrder.nativeOrder());
		converter.putInt(value);
		return converter.array();
	}

	public static short byteArrayToShort(byte[] valueBuf) {
		return byteArrayToShort(valueBuf, 0);
	}

	public static short byteArrayToShort(byte[] valueBuf, int offset) {
		ByteBuffer converter = ByteBuffer.wrap(valueBuf);
		converter.order(ByteOrder.nativeOrder());
		return converter.getShort(offset);
	}

	public static byte[] shortToByteArray(short value) {
		ByteBuffer converter = ByteBuffer.allocate(2);
		converter.order(ByteOrder.nativeOrder());
		converter.putShort(value);
		return converter.array();
	}

	public static float byteArrayToFloat(byte[] valueBuf) {
		return byteArrayToFloat(valueBuf, 0);
	}

	public static float byteArrayToFloat(byte[] valueBuf, int offset) {
		ByteBuffer converter = ByteBuffer.wrap(valueBuf);
		converter.order(ByteOrder.nativeOrder());
		return converter.getFloat(offset);
	}

	public static byte[] floatToByteArray(float value) {
		ByteBuffer converter = ByteBuffer.allocate(4);
		converter.order(ByteOrder.nativeOrder());
		converter.putFloat(value);
		return converter.array();
	}

	public static byte[] concatArrays(byte[]... arrays) {
		int len = 0;
		for (byte[] a : arrays) {
			len += a.length;
		}
		byte[] b = new byte[len];
		int offs = 0;
		for (byte[] a : arrays) {
			System.arraycopy(a, 0, b, offs, a.length);
			offs += a.length;
		}
		return b;
	}
}
