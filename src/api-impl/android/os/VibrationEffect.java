package android.os;

public class VibrationEffect implements Parcelable {

	/* AOSP keeps a list of segments; ATL's vibrator only switches on and off, so the timings are enough:
	 * alternating off and on durations in milliseconds (starting with off) and the index to repeat from */
	final long[] timings;
	final int repeat;
	/* the amplitude of each step (0 for the off steps, DEFAULT_AMPLITUDE or 1-255 for the on steps), for the
	 * vibrators that have amplitude control (those of game controllers, InputDeviceVibrator) */
	final int[] amplitudes;

	private VibrationEffect(long[] timings, int repeat) {
		this(timings, defaultAmplitudes(timings.length), repeat);
	}

	private VibrationEffect(long[] timings, int[] amplitudes, int repeat) {
		this.timings = timings;
		this.amplitudes = amplitudes;
		this.repeat = repeat;
	}

	private static int[] defaultAmplitudes(int steps) {
		int[] amplitudes = new int[steps];
		for (int i = 1; i < steps; i += 2)
			amplitudes[i] = DEFAULT_AMPLITUDE;
		return amplitudes;
	}

	/* AOSP createOneShot(): createWaveform() of one step, with amplitude DEFAULT_AMPLITUDE or 1-255 */
	public static VibrationEffect createOneShot(long milliseconds, int amplitude) {
		if (amplitude == 0) {
			throw new IllegalArgumentException(
			    "amplitude must either be DEFAULT_AMPLITUDE, "
			    + "or between 1 and 255 inclusive (amplitude=" + amplitude + ")");
		}
		return createWaveform(new long[] {milliseconds}, new int[] {amplitude}, -1);
	}

	/* the steps as InputManagerService's VibrationInfo takes them from the StepSegments: durations, amplitudes
	 * (DEFAULT_AMPLITUDE as defaultAmplitude, else the 0-255 amplitude through AOSP's float amplitude), repeat */
	/** @hide */
	public long[] getAtlTimings() {
		return timings.clone();
	}

	/** @hide */
	public int[] getAtlAmplitudes(int defaultAmplitude) {
		int[] result = new int[amplitudes.length];
		for (int i = 0; i < amplitudes.length; i++)
			result[i] = amplitudes[i] == DEFAULT_AMPLITUDE ? defaultAmplitude : (int)(((float)amplitudes[i] / MAX_AMPLITUDE) * MAX_AMPLITUDE);
		return result;
	}

	/** @hide */
	public int getAtlRepeatIndex() {
		return repeat;
	}

	public static final int EFFECT_CLICK = 0;
	public static final int EFFECT_DOUBLE_CLICK = 1;
	public static final int EFFECT_TICK = 2;
	public static final int EFFECT_THUD = 3;
	public static final int EFFECT_POP = 4;
	public static final int EFFECT_HEAVY_CLICK = 5;
	public static final int EFFECT_TEXTURE_TICK = 21;
	/** @hide */
	public static final int[] RINGTONES = java.util.stream.IntStream.rangeClosed(6, 20).toArray();

	/**
	 * AOSP createPredefined(): a prebaked effect with fallback. The argument check is PrebakedSegment.validate(). The
	 * vibrator of ATL has no predefined effect (Vibrator.areEffectsSupported() is NO for all) and the fallback patterns
	 * are device resources of the VibratorManagerService (config_*EffectPattern; not in ATL's framework-res), so the
	 * effect does not vibrate: that of a device without a vibrator.
	 */
	public static VibrationEffect createPredefined(int effectId) {
		switch (effectId) {
		case EFFECT_CLICK:
		case EFFECT_DOUBLE_CLICK:
		case EFFECT_HEAVY_CLICK:
		case EFFECT_POP:
		case EFFECT_TEXTURE_TICK:
		case EFFECT_THUD:
		case EFFECT_TICK:
			break;
		default:
			if (effectId < RINGTONES[0] || effectId > RINGTONES[RINGTONES.length - 1])
				throw new IllegalArgumentException("Unknown prebaked effect type (value=" + effectId + ")");
		}
		return new VibrationEffect(new long[] {0}, -1);
	}

	/* AOSP VibrationEffect is Parcelable: ATL's effect is the timings and the repeat index */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLongArray(timings);
		out.writeIntArray(amplitudes);
		out.writeInt(repeat);
	}

	public static final Parcelable.Creator<VibrationEffect> CREATOR = new Parcelable.Creator<VibrationEffect>() {
		public VibrationEffect createFromParcel(Parcel in) {
			long[] timings = in.createLongArray();
			int[] amplitudes = in.createIntArray();
			return new VibrationEffect(timings, amplitudes, in.readInt());
		}

		public VibrationEffect[] newArray(int size) {
			return new VibrationEffect[size];
		}
	};

	/* AOSP createRepeatingEffect(effect): startComposition().repeatEffectIndefinitely(effect).compose(); the whole
	 * effect repeats, so the repeat index is the start */
	public static VibrationEffect createRepeatingEffect(VibrationEffect effect) {
		com.android.internal.util.Preconditions.checkArgument(effect.repeat < 0,
		    "Can't repeat an indefinitely repeating effect. Consider addEffect instead.");
		return new VibrationEffect(effect.timings, effect.amplitudes, 0);
	}

	/* AOSP createRepeatingEffect(preamble, repeatingEffect): addEffect(preamble).repeatEffectIndefinitely(repeating).
	 * Even slots of the timings are off, odd ones on: a preamble that ends with an off step gets a zero-length on
	 * step, so the repeated effect starts with an off step again (as in createWaveform(long[], int[], int)) */
	public static VibrationEffect createRepeatingEffect(VibrationEffect preamble, VibrationEffect repeatingEffect) {
		com.android.internal.util.Preconditions.checkArgument(preamble.repeat < 0,
		    "Can't repeat an indefinitely repeating effect.");
		com.android.internal.util.Preconditions.checkArgument(repeatingEffect.repeat < 0,
		    "Can't repeat an indefinitely repeating effect. Consider addEffect instead.");
		int pad = preamble.timings.length % 2 == 1 ? 1 : 0;
		long[] timings = new long[preamble.timings.length + pad + repeatingEffect.timings.length];
		System.arraycopy(preamble.timings, 0, timings, 0, preamble.timings.length);
		System.arraycopy(repeatingEffect.timings, 0, timings, preamble.timings.length + pad,
		    repeatingEffect.timings.length);
		int[] amplitudes = new int[timings.length];
		System.arraycopy(preamble.amplitudes, 0, amplitudes, 0, preamble.amplitudes.length);
		System.arraycopy(repeatingEffect.amplitudes, 0, amplitudes, preamble.amplitudes.length + pad,
		    repeatingEffect.amplitudes.length);
		return new VibrationEffect(timings, amplitudes, preamble.timings.length + pad);
	}

	/**
	 * AOSP WaveformEnvelopeBuilder: control points of amplitude and frequency (PWLE). ATL's vibrator has no envelope
	 * control (Vibrator.areEnvelopeEffectsSupported() is false), so the built effect is the one that vibrates for no
	 * time, as for a composition; the checks of AOSP's PwleSegment.validate() are made.
	 */
	public static final class WaveformEnvelopeBuilder {
		private final java.util.ArrayList<float[]> mSegments = new java.util.ArrayList<>(); // startA, endA, startF, endF, duration
		private float mLastAmplitude = 0f;
		private float mLastFrequencyHz = Float.NaN;

		public WaveformEnvelopeBuilder() {}

		public WaveformEnvelopeBuilder setInitialFrequencyHz(float initialFrequencyHz) {
			if (mSegments.isEmpty()) {
				mLastFrequencyHz = initialFrequencyHz;
			} else {
				mSegments.get(0)[2] = initialFrequencyHz;
			}
			return this;
		}

		public WaveformEnvelopeBuilder addControlPoint(float amplitude, float frequencyHz, long durationMillis) {
			if (Float.isNaN(mLastFrequencyHz)) {
				mLastFrequencyHz = frequencyHz;
			}
			mSegments.add(new float[] {mLastAmplitude, amplitude, mLastFrequencyHz, frequencyHz, durationMillis});
			mLastAmplitude = amplitude;
			mLastFrequencyHz = frequencyHz;
			return this;
		}

		public VibrationEffect build() {
			if (mSegments.isEmpty()) {
				throw new IllegalStateException("WaveformEnvelopeBuilder must have at least one control point to build.");
			}
			for (float[] s : mSegments) {
				com.android.internal.util.Preconditions.checkArgumentPositive(s[2], "Start frequency must be greater than zero.");
				com.android.internal.util.Preconditions.checkArgumentPositive(s[3], "End frequency must be greater than zero.");
				com.android.internal.util.Preconditions.checkArgumentPositive(s[4], "Time must be greater than zero.");
				com.android.internal.util.Preconditions.checkArgumentInRange(s[0], 0f, 1f, "startAmplitude");
				com.android.internal.util.Preconditions.checkArgumentInRange(s[1], 0f, 1f, "endAmplitude");
			}
			return new VibrationEffect(new long[] {0}, -1);
		}
	}

	/**
	 * AOSP BasicEnvelopeBuilder: control points of intensity and sharpness; no envelope control in ATL, see
	 * WaveformEnvelopeBuilder. AOSP's BasicPwleSegment.validate() and the zero-intensity end are checked.
	 */
	public static final class BasicEnvelopeBuilder {
		private final java.util.ArrayList<float[]> mSegments = new java.util.ArrayList<>(); // startI, endI, startS, endS, duration
		private float mLastIntensity = 0f;
		private float mLastSharpness = Float.NaN;

		public BasicEnvelopeBuilder() {}

		public BasicEnvelopeBuilder setInitialSharpness(float initialSharpness) {
			if (mSegments.isEmpty()) {
				mLastSharpness = initialSharpness;
			} else {
				mSegments.get(0)[2] = initialSharpness;
			}
			return this;
		}

		public BasicEnvelopeBuilder addControlPoint(float intensity, float sharpness, long durationMillis) {
			if (Float.isNaN(mLastSharpness)) {
				mLastSharpness = sharpness;
			}
			mSegments.add(new float[] {mLastIntensity, intensity, mLastSharpness, sharpness, durationMillis});
			mLastIntensity = intensity;
			mLastSharpness = sharpness;
			return this;
		}

		public VibrationEffect build() {
			if (mSegments.isEmpty()) {
				throw new IllegalStateException("BasicEnvelopeBuilder must have at least one control point to build.");
			}
			if (mSegments.get(mSegments.size() - 1)[1] != 0) {
				throw new IllegalStateException("Basic envelope effects must end at a zero intensity control point.");
			}
			for (float[] s : mSegments) {
				com.android.internal.util.Preconditions.checkArgumentInRange(s[2], 0f, 1f, "startSharpness");
				com.android.internal.util.Preconditions.checkArgumentInRange(s[3], 0f, 1f, "endSharpness");
				com.android.internal.util.Preconditions.checkArgumentInRange(s[0], 0f, 1f, "startIntensity");
				com.android.internal.util.Preconditions.checkArgumentInRange(s[1], 0f, 1f, "endIntensity");
				com.android.internal.util.Preconditions.checkArgumentPositive(s[4], "Time must be greater than zero.");
			}
			return new VibrationEffect(new long[] {0}, -1);
		}
	}

	public static final int DEFAULT_AMPLITUDE = -1;
	public static final int MAX_AMPLITUDE = 255;

	public static VibrationEffect createWaveform(long[] pattern, int repeat) {
		return new VibrationEffect(pattern.clone(), repeat);
	}

	/* AOSP VibrationEffect.createWaveform(long[], int[], int): one step per timing, amplitude 0 is off.
	 * ATL's off/on list gets a zero-length step where two steps of the same kind follow each other,
	 * so the repeat index still points at the same step. The strength is kept for the vibrators with
	 * amplitude control (game controllers); ATL's own vibrator only switches on and off. */
	public static VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) {
		if (timings.length != amplitudes.length) {
			throw new IllegalArgumentException(
			    "timing and amplitude arrays must be of equal length"
			    + " (timings.length=" + timings.length
			    + ", amplitudes.length=" + amplitudes.length + ")");
		}
		if (repeat < -1 || repeat >= timings.length) {
			throw new IllegalArgumentException("repeat index must be within the bounds of the segments"
			    + " (segments.length=" + timings.length + ", index=" + repeat + ")");
		}
		boolean hasNonZeroDuration = false;
		for (long timing : timings)
			hasNonZeroDuration |= timing != 0;
		if (!hasNonZeroDuration)
			throw new IllegalArgumentException("at least one timing must be non-zero");
		long[] steps = new long[2 * timings.length + 1];
		int[] stepAmplitudes = new int[2 * timings.length + 1];
		int n = 0;
		int newRepeat = -1;
		for (int i = 0; i < timings.length; i++) {
			if (timings[i] < 0)
				throw new IllegalArgumentException("timings must all be >= 0");
			if (amplitudes[i] < DEFAULT_AMPLITUDE || amplitudes[i] > MAX_AMPLITUDE)
				throw new IllegalArgumentException("amplitudes must all be DEFAULT_AMPLITUDE or between 0 and 255");
			boolean on = amplitudes[i] != 0;
			if ((n % 2 == 1) != on)
				steps[n++] = 0; // even slots are off, odd slots are on
			if (i == repeat)
				newRepeat = n;
			stepAmplitudes[n] = amplitudes[i];
			steps[n++] = timings[i];
		}
		return new VibrationEffect(java.util.Arrays.copyOf(steps, n), java.util.Arrays.copyOf(stepAmplitudes, n), newRepeat);
	}

	/**
	 * AOSP VibrationEffect.startComposition(): a composition of haptic primitives. ATL's vibrator only switches on and
	 * off and has no primitive (Vibrator.areAllPrimitivesSupported() is false for all), so a composed effect is the one
	 * that vibrates for no time: AOSP's service skips the primitives a vibrator does not support.
	 */
	public static Composition startComposition() {
		return new Composition();
	}

	public static final class Composition {
		public static final int PRIMITIVE_NOOP = 0;
		public static final int PRIMITIVE_CLICK = 1;
		public static final int PRIMITIVE_THUD = 2;
		public static final int PRIMITIVE_SPIN = 3;
		public static final int PRIMITIVE_QUICK_RISE = 4;
		public static final int PRIMITIVE_SLOW_RISE = 5;
		public static final int PRIMITIVE_QUICK_FALL = 6;
		public static final int PRIMITIVE_TICK = 7;
		public static final int PRIMITIVE_LOW_TICK = 8;

		public static final int DELAY_TYPE_PAUSE = 0;
		public static final int DELAY_TYPE_RELATIVE_START_OFFSET = 1;

		private int mSegments;

		Composition() {}

		public Composition addPrimitive(int primitiveId) {
			return addPrimitive(primitiveId, 1f);
		}

		public Composition addPrimitive(int primitiveId, float scale) {
			return addPrimitive(primitiveId, scale, 0);
		}

		/* AOSP PrimitiveSegment.validate(): the primitive id, scale and delay are in range */
		public Composition addPrimitive(int primitiveId, float scale, int delay) {
			return addPrimitive(primitiveId, scale, delay, DELAY_TYPE_PAUSE);
		}

		/* AOSP addPrimitive(id, scale, delay, delayType): PrimitiveSegment.validate() also checks the delay type */
		public Composition addPrimitive(int primitiveId, float scale, int delay, int delayType) {
			com.android.internal.util.Preconditions.checkArgumentInRange(primitiveId, PRIMITIVE_NOOP, PRIMITIVE_LOW_TICK, "primitiveId");
			com.android.internal.util.Preconditions.checkArgumentInRange(scale, 0f, 1f, "scale");
			if (delay < 0)
				throw new IllegalArgumentException("delay must be >= 0");
			if (delayType != DELAY_TYPE_PAUSE && delayType != DELAY_TYPE_RELATIVE_START_OFFSET)
				throw new IllegalArgumentException("delayType");
			mSegments++;
			return this;
		}

		public VibrationEffect compose() {
			if (mSegments == 0)
				throw new IllegalStateException("Composition must have at least one element to compose.");
			return new VibrationEffect(new long[] {0}, -1);
		}
	}
}
