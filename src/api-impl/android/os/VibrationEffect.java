package android.os;

public class VibrationEffect {

	/* AOSP keeps a list of segments; ATL's vibrator only switches on and off, so the timings are enough:
	 * alternating off and on durations in milliseconds (starting with off) and the index to repeat from */
	final long[] timings;
	final int repeat;

	private VibrationEffect(long[] timings, int repeat) {
		this.timings = timings;
		this.repeat = repeat;
	}

	public static VibrationEffect createOneShot(long milliseconds, int amplitude) {
		return new VibrationEffect(new long[] {0, milliseconds}, -1);
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

	public static final int DEFAULT_AMPLITUDE = -1;
	public static final int MAX_AMPLITUDE = 255;

	public static VibrationEffect createWaveform(long[] pattern, int repeat) {
		return new VibrationEffect(pattern.clone(), repeat);
	}

	/* AOSP VibrationEffect.createWaveform(long[], int[], int): one step per timing, amplitude 0 is off.
	 * ATL's off/on list gets a zero-length step where two steps of the same kind follow each other,
	 * so the repeat index still points at the same step. The strength itself is dropped (no amplitude
	 * control, see Vibrator.hasAmplitudeControl). */
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
			steps[n++] = timings[i];
		}
		return new VibrationEffect(java.util.Arrays.copyOf(steps, n), newRepeat);
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
			com.android.internal.util.Preconditions.checkArgumentInRange(primitiveId, PRIMITIVE_NOOP, PRIMITIVE_LOW_TICK, "primitiveId");
			com.android.internal.util.Preconditions.checkArgumentInRange(scale, 0f, 1f, "scale");
			if (delay < 0)
				throw new IllegalArgumentException("delay must be >= 0");
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
