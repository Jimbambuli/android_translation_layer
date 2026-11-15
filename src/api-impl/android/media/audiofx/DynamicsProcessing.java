package android.media.audiofx;

import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringTokenizer;

/* AOSP media/java/android/media/audiofx/DynamicsProcessing.java. ATL has no effect engines: the constructor fails
 * in AudioEffect(UUID, UUID, int, int) as in AOSP when no DynamicsProcessing engine exists, so the engine methods are
 * never reached; Config, Channel, Eq, Mbc, Limiter and their bands are plain AOSP data classes. */
public final class DynamicsProcessing extends AudioEffect {

	private final static String TAG = "DynamicsProcessing";
	private static final int PARAM_GET_CHANNEL_COUNT = 0x10;
	private static final int PARAM_INPUT_GAIN = 0x20;
	private static final int PARAM_ENGINE_ARCHITECTURE = 0x30;
	private static final int PARAM_PRE_EQ = 0x40;
	private static final int PARAM_PRE_EQ_BAND = 0x45;
	private static final int PARAM_MBC = 0x50;
	private static final int PARAM_MBC_BAND = 0x55;
	private static final int PARAM_POST_EQ = 0x60;
	private static final int PARAM_POST_EQ_BAND = 0x65;
	private static final int PARAM_LIMITER = 0x70;

	public static final int VARIANT_FAVOR_FREQUENCY_RESOLUTION  = 0;

	public static final int VARIANT_FAVOR_TIME_RESOLUTION       = 1;

	private static final int CHANNEL_COUNT_MAX = 32;

	private int mChannelCount = 0;

	private OnParameterChangeListener mParamListener = null;

	private BaseParameterListener mBaseParamListener = null;

	private final Object mParamListenerLock = new Object();

	public DynamicsProcessing(int audioSession) {
		this(0 , audioSession);
	}

	public DynamicsProcessing(int priority, int audioSession) {
		this(priority, audioSession, null);
	}

	public DynamicsProcessing(int priority, int audioSession, Config cfg) {
		super(EFFECT_TYPE_DYNAMICS_PROCESSING, EFFECT_TYPE_NULL, priority, audioSession);
		if (audioSession == 0) {
			Log.w(TAG, "WARNING: attaching a DynamicsProcessing to global output mix is"
					+ "deprecated!");
		}
		final Config config;
		mChannelCount = getChannelCount();
		if (cfg == null) {
			DynamicsProcessing.Config.Builder builder =
					new DynamicsProcessing.Config.Builder(
							CONFIG_DEFAULT_VARIANT,
							mChannelCount,
							CONFIG_DEFAULT_USE_PREEQ,
							CONFIG_DEFAULT_PREEQ_BANDS,
							CONFIG_DEFAULT_USE_MBC,
							CONFIG_DEFAULT_MBC_BANDS,
							CONFIG_DEFAULT_USE_POSTEQ,
							CONFIG_DEFAULT_POSTEQ_BANDS,
							CONFIG_DEFAULT_USE_LIMITER);
			config = builder.build();
		} else {
			config = new DynamicsProcessing.Config(mChannelCount, cfg);
		}
		setEngineArchitecture(config.getVariant(),
				config.getPreferredFrameDuration(),
				config.isPreEqInUse(),
				config.getPreEqBandCount(),
				config.isMbcInUse(),
				config.getMbcBandCount(),
				config.isPostEqInUse(),
				config.getPostEqBandCount(),
				config.isLimiterInUse());
		for (int ch = 0; ch < mChannelCount; ch++) {
			updateEngineChannelByChannelIndex(ch, config.getChannelByChannelIndex(ch));
		}
	}

	public Config getConfig() {
		Number[] params = { PARAM_ENGINE_ARCHITECTURE };
		Number[] values = { 0 ,
				0.0f ,
				0 ,
				0 ,
				0 ,
				0 ,
				0 ,
				0 ,
				0 };
		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);
		byteArrayToNumberArray(valueBytes, values);
		DynamicsProcessing.Config.Builder builder =
				new DynamicsProcessing.Config.Builder(
						values[0].intValue(),
						mChannelCount,
						values[2].intValue() > 0 ,
						values[3].intValue() ,
						values[4].intValue() > 0 ,
						values[5].intValue() ,
						values[6].intValue() > 0 ,
						values[7].intValue(),
						values[8].intValue() > 0 ).
				setPreferredFrameDuration(values[1].floatValue());
		Config config = builder.build();
		for (int ch = 0; ch < mChannelCount; ch++) {
			Channel channel = queryEngineByChannelIndex(ch);
			config.setChannelTo(ch, channel);
		}
		return config;
	}

	private static final int CONFIG_DEFAULT_VARIANT = VARIANT_FAVOR_FREQUENCY_RESOLUTION;
	private static final boolean CONFIG_DEFAULT_USE_PREEQ = true;
	private static final int CONFIG_DEFAULT_PREEQ_BANDS = 6;
	private static final boolean CONFIG_DEFAULT_USE_MBC = true;
	private static final int CONFIG_DEFAULT_MBC_BANDS = 6;
	private static final boolean CONFIG_DEFAULT_USE_POSTEQ = true;
	private static final int CONFIG_DEFAULT_POSTEQ_BANDS = 6;
	private static final boolean CONFIG_DEFAULT_USE_LIMITER = true;

	private static final float CHANNEL_DEFAULT_INPUT_GAIN = 0; // dB
	private static final float CONFIG_PREFERRED_FRAME_DURATION_MS = 10.0f; //milliseconds

	private static final float EQ_DEFAULT_GAIN = 0; // dB
	private static final boolean PREEQ_DEFAULT_ENABLED = true;
	private static final boolean POSTEQ_DEFAULT_ENABLED = true;

	private static final boolean MBC_DEFAULT_ENABLED = true;
	private static final float MBC_DEFAULT_ATTACK_TIME = 3; // ms
	private static final float MBC_DEFAULT_RELEASE_TIME = 80; // ms
	private static final float MBC_DEFAULT_RATIO = 1; // N:1
	private static final float MBC_DEFAULT_THRESHOLD = -45; // dB
	private static final float MBC_DEFAULT_KNEE_WIDTH = 0; // dB
	private static final float MBC_DEFAULT_NOISE_GATE_THRESHOLD = -90; // dB
	private static final float MBC_DEFAULT_EXPANDER_RATIO = 1; // 1:N
	private static final float MBC_DEFAULT_PRE_GAIN = 0; // dB
	private static final float MBC_DEFAULT_POST_GAIN = 0; // dB

	private static final boolean LIMITER_DEFAULT_ENABLED = true;
	private static final int LIMITER_DEFAULT_LINK_GROUP = 0;//;
	private static final float LIMITER_DEFAULT_ATTACK_TIME = 1; // ms
	private static final float LIMITER_DEFAULT_RELEASE_TIME = 60; // ms
	private static final float LIMITER_DEFAULT_RATIO = 10; // N:1
	private static final float LIMITER_DEFAULT_THRESHOLD = -2; // dB
	private static final float LIMITER_DEFAULT_POST_GAIN = 0; // dB

	private static final float DEFAULT_MIN_FREQUENCY = 220; // Hz
	private static final float DEFAULT_MAX_FREQUENCY = 20000; // Hz
	private static final float mMinFreqLog = (float)Math.log10(DEFAULT_MIN_FREQUENCY);
	private static final float mMaxFreqLog = (float)Math.log10(DEFAULT_MAX_FREQUENCY);

	public static class Stage {
		private boolean mInUse;
		private boolean mEnabled;
		
		public Stage(boolean inUse, boolean enabled) {
			mInUse = inUse;
			mEnabled = enabled;
		}

		public boolean isEnabled() {
			return mEnabled;
		}
		
		public void setEnabled(boolean enabled) {
			mEnabled = enabled;
		}

		public boolean isInUse() {
			return mInUse;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format(" Stage InUse: %b\n", isInUse()));
			if (isInUse()) {
				sb.append(String.format(" Stage Enabled: %b\n", mEnabled));
			}
			return sb.toString();
		}
	}

	public static class BandStage extends Stage{
		private int mBandCount;
		
		public BandStage(boolean inUse, boolean enabled, int bandCount) {
			super(inUse, enabled);
			mBandCount = isInUse() ? bandCount : 0;
		}

		public int getBandCount() {
			return mBandCount;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			if (isInUse()) {
				sb.append(String.format(" Band Count: %d\n", mBandCount));
			}
			return sb.toString();
		}
	}

	public static class BandBase {
		private boolean mEnabled;
		private float mCutoffFrequency;
		
		public BandBase(boolean enabled, float cutoffFrequency) {
			mEnabled = enabled;
			mCutoffFrequency = cutoffFrequency;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format(" Enabled: %b\n", mEnabled));
			sb.append(String.format(" CutoffFrequency: %f\n", mCutoffFrequency));
			return sb.toString();
		}

		public boolean isEnabled() {
			return mEnabled;
		}
		
		public void setEnabled(boolean enabled) {
			mEnabled = enabled;
		}

		public float getCutoffFrequency() {
			return mCutoffFrequency;
		}

		public void setCutoffFrequency(float frequency) {
			mCutoffFrequency = frequency;
		}
	}

	public final static class EqBand extends BandBase {
		private float mGain;
		
		public EqBand(boolean enabled, float cutoffFrequency, float gain) {
			super(enabled, cutoffFrequency);
			mGain = gain;
		}

		public EqBand(EqBand cfg) {
			super(cfg.isEnabled(), cfg.getCutoffFrequency());
			mGain = cfg.mGain;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			sb.append(String.format(" Gain: %f\n", mGain));
			return sb.toString();
		}

		public float getGain() {
			return mGain;
		}

		public void setGain(float gain) {
			mGain = gain;
		}
	}

	public final static class MbcBand extends BandBase{
		private float mAttackTime;
		private float mReleaseTime;
		private float mRatio;
		private float mThreshold;
		private float mKneeWidth;
		private float mNoiseGateThreshold;
		private float mExpanderRatio;
		private float mPreGain;
		private float mPostGain;
		
		public MbcBand(boolean enabled, float cutoffFrequency, float attackTime, float releaseTime,
				float ratio, float threshold, float kneeWidth, float noiseGateThreshold,
				float expanderRatio, float preGain, float postGain) {
			super(enabled, cutoffFrequency);
			mAttackTime = attackTime;
			mReleaseTime = releaseTime;
			mRatio = ratio;
			mThreshold = threshold;
			mKneeWidth = kneeWidth;
			mNoiseGateThreshold = noiseGateThreshold;
			mExpanderRatio = expanderRatio;
			mPreGain = preGain;
			mPostGain = postGain;
		}

		public MbcBand(MbcBand cfg) {
			super(cfg.isEnabled(), cfg.getCutoffFrequency());
			mAttackTime = cfg.mAttackTime;
			mReleaseTime = cfg.mReleaseTime;
			mRatio = cfg.mRatio;
			mThreshold = cfg.mThreshold;
			mKneeWidth = cfg.mKneeWidth;
			mNoiseGateThreshold = cfg.mNoiseGateThreshold;
			mExpanderRatio = cfg.mExpanderRatio;
			mPreGain = cfg.mPreGain;
			mPostGain = cfg.mPostGain;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			sb.append(String.format(" AttackTime: %f (ms)\n", mAttackTime));
			sb.append(String.format(" ReleaseTime: %f (ms)\n", mReleaseTime));
			sb.append(String.format(" Ratio: 1:%f\n", mRatio));
			sb.append(String.format(" Threshold: %f (dB)\n", mThreshold));
			sb.append(String.format(" NoiseGateThreshold: %f(dB)\n", mNoiseGateThreshold));
			sb.append(String.format(" ExpanderRatio: %f:1\n", mExpanderRatio));
			sb.append(String.format(" PreGain: %f (dB)\n", mPreGain));
			sb.append(String.format(" PostGain: %f (dB)\n", mPostGain));
			return sb.toString();
		}

		public float getAttackTime() { return mAttackTime; }
		
		public void setAttackTime(float attackTime) { mAttackTime = attackTime; }
		
		public float getReleaseTime() { return mReleaseTime; }
		
		public void setReleaseTime(float releaseTime) { mReleaseTime = releaseTime; }
		
		public float getRatio() { return mRatio; }
		
		public void setRatio(float ratio) { mRatio = ratio; }
		
		public float getThreshold() { return mThreshold; }
		
		public void setThreshold(float threshold) { mThreshold = threshold; }
		
		public float getKneeWidth() { return mKneeWidth; }
		
		public void setKneeWidth(float kneeWidth) { mKneeWidth = kneeWidth; }
		
		public float getNoiseGateThreshold() { return mNoiseGateThreshold; }
		
		public void setNoiseGateThreshold(float noiseGateThreshold) {
			mNoiseGateThreshold = noiseGateThreshold; }
		
		public float getExpanderRatio() { return mExpanderRatio; }
		
		public void setExpanderRatio(float expanderRatio) { mExpanderRatio = expanderRatio; }
		
		public float getPreGain() { return mPreGain; }
		
		public void setPreGain(float preGain) { mPreGain = preGain; }
		
		public float getPostGain() { return mPostGain; }
		
		public void setPostGain(float postGain) { mPostGain = postGain; }
	}

	public final static class Eq extends BandStage {
		private final EqBand[] mBands;
		
		public Eq(boolean inUse, boolean enabled, int bandCount) {
			super(inUse, enabled, bandCount);
			if (isInUse()) {
				mBands = new EqBand[bandCount];
				for (int b = 0; b < bandCount; b++) {
					float freq = DEFAULT_MAX_FREQUENCY;
					if (bandCount > 1) {
						freq = (float)Math.pow(10, mMinFreqLog +
								b * (mMaxFreqLog - mMinFreqLog)/(bandCount -1));
					}
					mBands[b] = new EqBand(true, freq, EQ_DEFAULT_GAIN);
				}
			} else {
				mBands = null;
			}
		}
		
		public Eq(Eq cfg) {
			super(cfg.isInUse(), cfg.isEnabled(), cfg.getBandCount());
			if (isInUse()) {
				mBands = new EqBand[cfg.mBands.length];
				for (int b = 0; b < mBands.length; b++) {
					mBands[b] = new EqBand(cfg.mBands[b]);
				}
			} else {
				mBands = null;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			if (isInUse()) {
				sb.append("--->EqBands: " + mBands.length + "\n");
				for (int b = 0; b < mBands.length; b++) {
					sb.append(String.format("  Band %d\n", b));
					sb.append(mBands[b].toString());
				}
			}
			return sb.toString();
		}
		
		private void checkBand(int band) {
			if (mBands == null || band < 0 || band >= mBands.length) {
				throw new IllegalArgumentException("band index " + band +" out of bounds");
			}
		}
		
		public void setBand(int band, EqBand bandCfg) {
			checkBand(band);
			mBands[band] = new EqBand(bandCfg);
		}
		
		public EqBand getBand(int band) {
			checkBand(band);
			return mBands[band];
		}
	}

	public final static class Mbc extends BandStage {
		private final MbcBand[] mBands;
		
		public Mbc(boolean inUse, boolean enabled, int bandCount) {
			super(inUse, enabled, bandCount);
			if (isInUse()) {
				mBands = new MbcBand[bandCount];
				for (int b = 0; b < bandCount; b++) {
					float freq = DEFAULT_MAX_FREQUENCY;
					if (bandCount > 1) {
						freq = (float)Math.pow(10, mMinFreqLog +
								b * (mMaxFreqLog - mMinFreqLog)/(bandCount -1));
					}
					mBands[b] = new MbcBand(true, freq, MBC_DEFAULT_ATTACK_TIME,
							MBC_DEFAULT_RELEASE_TIME, MBC_DEFAULT_RATIO,
							MBC_DEFAULT_THRESHOLD, MBC_DEFAULT_KNEE_WIDTH,
							MBC_DEFAULT_NOISE_GATE_THRESHOLD, MBC_DEFAULT_EXPANDER_RATIO,
							MBC_DEFAULT_PRE_GAIN, MBC_DEFAULT_POST_GAIN);
				}
			} else {
				mBands = null;
			}
		}
		
		public Mbc(Mbc cfg) {
			super(cfg.isInUse(), cfg.isEnabled(), cfg.getBandCount());
			if (isInUse()) {
				mBands = new MbcBand[cfg.mBands.length];
				for (int b = 0; b < mBands.length; b++) {
					mBands[b] = new MbcBand(cfg.mBands[b]);
				}
			} else {
				mBands = null;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			if (isInUse()) {
				sb.append("--->MbcBands: " + mBands.length + "\n");
				for (int b = 0; b < mBands.length; b++) {
					sb.append(String.format("  Band %d\n", b));
					sb.append(mBands[b].toString());
				}
			}
			return sb.toString();
		}
		
		private void checkBand(int band) {
			if (mBands == null || band < 0 || band >= mBands.length) {
				throw new IllegalArgumentException("band index " + band +" out of bounds");
			}
		}
		
		public void setBand(int band, MbcBand bandCfg) {
			checkBand(band);
			mBands[band] = new MbcBand(bandCfg);
		}
		
		public MbcBand getBand(int band) {
			checkBand(band);
			return mBands[band];
		}
	}

	public final static class Limiter extends Stage {
		private int mLinkGroup;
		private float mAttackTime;
		private float mReleaseTime;
		private float mRatio;
		private float mThreshold;
		private float mPostGain;

		public Limiter(boolean inUse, boolean enabled, int linkGroup, float attackTime,
				float releaseTime, float ratio, float threshold, float postGain) {
			super(inUse, enabled);
			mLinkGroup = linkGroup;
			mAttackTime = attackTime;
			mReleaseTime = releaseTime;
			mRatio = ratio;
			mThreshold = threshold;
			mPostGain = postGain;
		}

		public Limiter(Limiter cfg) {
			super(cfg.isInUse(), cfg.isEnabled());
			mLinkGroup = cfg.mLinkGroup;
			mAttackTime = cfg.mAttackTime;
			mReleaseTime = cfg.mReleaseTime;
			mRatio = cfg.mRatio;
			mThreshold = cfg.mThreshold;
			mPostGain = cfg.mPostGain;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			if (isInUse()) {
				sb.append(String.format(" LinkGroup: %d (group)\n", mLinkGroup));
				sb.append(String.format(" AttackTime: %f (ms)\n", mAttackTime));
				sb.append(String.format(" ReleaseTime: %f (ms)\n", mReleaseTime));
				sb.append(String.format(" Ratio: 1:%f\n", mRatio));
				sb.append(String.format(" Threshold: %f (dB)\n", mThreshold));
				sb.append(String.format(" PostGain: %f (dB)\n", mPostGain));
			}
			return sb.toString();
		}
		
		public int getLinkGroup() { return mLinkGroup; }
		
		public void setLinkGroup(int linkGroup) { mLinkGroup = linkGroup; }
		
		public float getAttackTime() { return mAttackTime; }
		
		public void setAttackTime(float attackTime) { mAttackTime = attackTime; }
		
		public float getReleaseTime() { return mReleaseTime; }
		
		public void setReleaseTime(float releaseTime) { mReleaseTime = releaseTime; }
		
		public float getRatio() { return mRatio; }
		
		public void setRatio(float ratio) { mRatio = ratio; }
		
		public float getThreshold() { return mThreshold; }
		
		public void setThreshold(float threshold) { mThreshold = threshold; }
		
		public float getPostGain() { return mPostGain; }
		
		public void setPostGain(float postGain) { mPostGain = postGain; }
	}

	public final static class Channel {
		private float   mInputGain;
		private Eq      mPreEq;
		private Mbc     mMbc;
		private Eq      mPostEq;
		private Limiter mLimiter;

		public Channel (float inputGain,
				boolean preEqInUse, int preEqBandCount,
				boolean mbcInUse, int mbcBandCount,
				boolean postEqInUse, int postEqBandCount,
				boolean limiterInUse) {
			mInputGain = inputGain;
			mPreEq = new Eq(preEqInUse, PREEQ_DEFAULT_ENABLED, preEqBandCount);
			mMbc = new Mbc(mbcInUse, MBC_DEFAULT_ENABLED, mbcBandCount);
			mPostEq = new Eq(postEqInUse, POSTEQ_DEFAULT_ENABLED,
					postEqBandCount);
			mLimiter = new Limiter(limiterInUse,
					LIMITER_DEFAULT_ENABLED, LIMITER_DEFAULT_LINK_GROUP,
					LIMITER_DEFAULT_ATTACK_TIME, LIMITER_DEFAULT_RELEASE_TIME,
					LIMITER_DEFAULT_RATIO, LIMITER_DEFAULT_THRESHOLD, LIMITER_DEFAULT_POST_GAIN);
		}

		public Channel(Channel cfg) {
			mInputGain = cfg.mInputGain;
			mPreEq = new Eq(cfg.mPreEq);
			mMbc = new Mbc(cfg.mMbc);
			mPostEq = new Eq(cfg.mPostEq);
			mLimiter = new Limiter(cfg.mLimiter);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format(" InputGain: %f\n", mInputGain));
			sb.append("-->PreEq\n");
			sb.append(mPreEq.toString());
			sb.append("-->MBC\n");
			sb.append(mMbc.toString());
			sb.append("-->PostEq\n");
			sb.append(mPostEq.toString());
			sb.append("-->Limiter\n");
			sb.append(mLimiter.toString());
			return sb.toString();
		}
		
		public float getInputGain() {
			return mInputGain;
		}
		
		public void setInputGain(float inputGain) {
			mInputGain = inputGain;
		}

		public Eq getPreEq() {
			return mPreEq;
		}
		
		public void setPreEq(Eq preEq) {
			if (preEq.getBandCount() != mPreEq.getBandCount()) {
				throw new IllegalArgumentException("PreEqBandCount changed from " +
						mPreEq.getBandCount() + " to " + preEq.getBandCount());
			}
			mPreEq = new Eq(preEq);
		}
		
		public EqBand getPreEqBand(int band) {
			return mPreEq.getBand(band);
		}
		
		public void setPreEqBand(int band, EqBand preEqBand) {
			mPreEq.setBand(band, preEqBand);
		}

		public Mbc getMbc() {
			return mMbc;
		}
		
		public void setMbc(Mbc mbc) {
			if (mbc.getBandCount() != mMbc.getBandCount()) {
				throw new IllegalArgumentException("MbcBandCount changed from " +
						mMbc.getBandCount() + " to " + mbc.getBandCount());
			}
			mMbc = new Mbc(mbc);
		}
		
		public MbcBand getMbcBand(int band) {
			return mMbc.getBand(band);
		}
		
		public void setMbcBand(int band, MbcBand mbcBand) {
			mMbc.setBand(band, mbcBand);
		}

		public Eq getPostEq() {
			return mPostEq;
		}
		
		public void setPostEq(Eq postEq) {
			if (postEq.getBandCount() != mPostEq.getBandCount()) {
				throw new IllegalArgumentException("PostEqBandCount changed from " +
						mPostEq.getBandCount() + " to " + postEq.getBandCount());
			}
			mPostEq = new Eq(postEq);
		}
		
		public EqBand getPostEqBand(int band) {
			return mPostEq.getBand(band);
		}
		
		public void setPostEqBand(int band, EqBand postEqBand) {
			mPostEq.setBand(band, postEqBand);
		}

		public Limiter getLimiter() {
			return mLimiter;
		}
		
		public void setLimiter(Limiter limiter) {
			mLimiter = new Limiter(limiter);
		}
	}

	public final static class Config {
		private final int mVariant;
		private final int mChannelCount;
		private final boolean mPreEqInUse;
		private final int mPreEqBandCount;
		private final boolean mMbcInUse;
		private final int mMbcBandCount;
		private final boolean mPostEqInUse;
		private final int mPostEqBandCount;
		private final boolean mLimiterInUse;
		private final float mPreferredFrameDuration;
		private final Channel[] mChannel;

		public Config(int variant, float frameDurationMs, int channelCount,
				boolean preEqInUse, int preEqBandCount,
				boolean mbcInUse, int mbcBandCount,
				boolean postEqInUse, int postEqBandCount,
				boolean limiterInUse,
				Channel[] channel) {
			mVariant = variant;
			mPreferredFrameDuration = frameDurationMs;
			mChannelCount = channelCount;
			mPreEqInUse = preEqInUse;
			mPreEqBandCount = preEqBandCount;
			mMbcInUse = mbcInUse;
			mMbcBandCount = mbcBandCount;
			mPostEqInUse = postEqInUse;
			mPostEqBandCount = postEqBandCount;
			mLimiterInUse = limiterInUse;

			mChannel = new Channel[mChannelCount];
			for (int ch = 0; ch < mChannelCount; ch++) {
				if (ch < channel.length) {
					mChannel[ch] = new Channel(channel[ch]); //copy create
				} else {
				}
			}
		}
		
		public Config(int channelCount, Config cfg) {
			mVariant = cfg.mVariant;
			mPreferredFrameDuration = cfg.mPreferredFrameDuration;
			mChannelCount = cfg.mChannelCount;
			mPreEqInUse = cfg.mPreEqInUse;
			mPreEqBandCount = cfg.mPreEqBandCount;
			mMbcInUse = cfg.mMbcInUse;
			mMbcBandCount = cfg.mMbcBandCount;
			mPostEqInUse = cfg.mPostEqInUse;
			mPostEqBandCount = cfg.mPostEqBandCount;
			mLimiterInUse = cfg.mLimiterInUse;

			if (mChannelCount != cfg.mChannel.length) {
				throw new IllegalArgumentException("configuration channel counts differ " +
						mChannelCount + " !=" + cfg.mChannel.length);
			}
			if (channelCount < 1) {
				throw new IllegalArgumentException("channel resizing less than 1 not allowed");
			}

			mChannel = new Channel[channelCount];
			for (int ch = 0; ch < channelCount; ch++) {
				if (ch < mChannelCount) {
					mChannel[ch] = new Channel(cfg.mChannel[ch]);
				} else {
					mChannel[ch] = new Channel(cfg.mChannel[mChannelCount-1]);
				}
			}
		}

		public Config(Config cfg) {
			this(cfg.mChannelCount, cfg);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Variant: %d\n", mVariant));
			sb.append(String.format("PreferredFrameDuration: %f\n", mPreferredFrameDuration));
			sb.append(String.format("ChannelCount: %d\n", mChannelCount));
			sb.append(String.format("PreEq inUse: %b, bandCount:%d\n",mPreEqInUse,
					mPreEqBandCount));
			sb.append(String.format("Mbc inUse: %b, bandCount: %d\n",mMbcInUse, mMbcBandCount));
			sb.append(String.format("PostEq inUse: %b, bandCount: %d\n", mPostEqInUse,
					mPostEqBandCount));
			sb.append(String.format("Limiter inUse: %b\n", mLimiterInUse));
			for (int ch = 0; ch < mChannel.length; ch++) {
				sb.append(String.format("==Channel %d\n", ch));
				sb.append(mChannel[ch].toString());
			}
			return sb.toString();
		}
		private void checkChannel(int channelIndex) {
			if (channelIndex < 0 || channelIndex >= mChannel.length) {
				throw new IllegalArgumentException("ChannelIndex out of bounds");
			}
		}
		
		public int getVariant() {
			return mVariant;
		}
		
		public float getPreferredFrameDuration() {
			return mPreferredFrameDuration;
		}
		
		public boolean isPreEqInUse() {
			return mPreEqInUse;
		}
		
		public int getPreEqBandCount() {
			return mPreEqBandCount;
		}
		
		public boolean isMbcInUse() {
			return mMbcInUse;
		}
		
		public int getMbcBandCount() {
			return mMbcBandCount;
		}
		
		public boolean isPostEqInUse() {
			return mPostEqInUse;
		}
		
		public int getPostEqBandCount() {
			return mPostEqBandCount;
		}
		
		public boolean isLimiterInUse() {
			return mLimiterInUse;
		}
		
		public Channel getChannelByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex];
		}

		public void setChannelTo(int channelIndex, Channel channel) {
			checkChannel(channelIndex);
			if (mMbcBandCount != channel.getMbc().getBandCount()) {
				throw new IllegalArgumentException("MbcBandCount changed from " +
						mMbcBandCount + " to " + channel.getPreEq().getBandCount());
			}
			if (mPreEqBandCount != channel.getPreEq().getBandCount()) {
				throw new IllegalArgumentException("PreEqBandCount changed from " +
						mPreEqBandCount + " to " + channel.getPreEq().getBandCount());
			}
			if (mPostEqBandCount != channel.getPostEq().getBandCount()) {
				throw new IllegalArgumentException("PostEqBandCount changed from " +
						mPostEqBandCount + " to " + channel.getPostEq().getBandCount());
			}
			mChannel[channelIndex] = new Channel(channel);
		}

		public void setAllChannelsTo(Channel channel) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				setChannelTo(ch, channel);
			}
		}
		
		public float getInputGainByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getInputGain();
		}
		
		public void setInputGainByChannelIndex(int channelIndex, float inputGain) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setInputGain(inputGain);
		}
		
		public void setInputGainAllChannelsTo(float inputGain) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setInputGain(inputGain);
			}
		}
		
		public Eq getPreEqByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getPreEq();
		}
		
		public void setPreEqByChannelIndex(int channelIndex, Eq preEq) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setPreEq(preEq);
		}
		
		public void setPreEqAllChannelsTo(Eq preEq) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setPreEq(preEq);
			}
		}
		public EqBand getPreEqBandByChannelIndex(int channelIndex, int band) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getPreEqBand(band);
		}
		public void setPreEqBandByChannelIndex(int channelIndex, int band, EqBand preEqBand) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setPreEqBand(band, preEqBand);
		}
		public void setPreEqBandAllChannelsTo(int band, EqBand preEqBand) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setPreEqBand(band, preEqBand);
			}
		}
		public Mbc getMbcByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getMbc();
		}
		public void setMbcByChannelIndex(int channelIndex, Mbc mbc) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setMbc(mbc);
		}
		public void setMbcAllChannelsTo(Mbc mbc) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setMbc(mbc);
			}
		}
		public MbcBand getMbcBandByChannelIndex(int channelIndex, int band) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getMbcBand(band);
		}
		public void setMbcBandByChannelIndex(int channelIndex, int band, MbcBand mbcBand) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setMbcBand(band, mbcBand);
		}
		public void setMbcBandAllChannelsTo(int band, MbcBand mbcBand) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setMbcBand(band, mbcBand);
			}
		}
		public Eq getPostEqByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getPostEq();
		}
		public void setPostEqByChannelIndex(int channelIndex, Eq postEq) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setPostEq(postEq);
		}
		public void setPostEqAllChannelsTo(Eq postEq) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setPostEq(postEq);
			}
		}
		public EqBand getPostEqBandByChannelIndex(int channelIndex, int band) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getPostEqBand(band);
		}
		public void setPostEqBandByChannelIndex(int channelIndex, int band, EqBand postEqBand) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setPostEqBand(band, postEqBand);
		}
		public void setPostEqBandAllChannelsTo(int band, EqBand postEqBand) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setPostEqBand(band, postEqBand);
			}
		}
		public Limiter getLimiterByChannelIndex(int channelIndex) {
			checkChannel(channelIndex);
			return mChannel[channelIndex].getLimiter();
		}
		public void setLimiterByChannelIndex(int channelIndex, Limiter limiter) {
			checkChannel(channelIndex);
			mChannel[channelIndex].setLimiter(limiter);
		}
		public void setLimiterAllChannelsTo(Limiter limiter) {
			for (int ch = 0; ch < mChannel.length; ch++) {
				mChannel[ch].setLimiter(limiter);
			}
		}

		public final static class Builder {
			private int mVariant;
			private int mChannelCount;
			private boolean mPreEqInUse;
			private int mPreEqBandCount;
			private boolean mMbcInUse;
			private int mMbcBandCount;
			private boolean mPostEqInUse;
			private int mPostEqBandCount;
			private boolean mLimiterInUse;
			private float mPreferredFrameDuration = CONFIG_PREFERRED_FRAME_DURATION_MS;
			private Channel[] mChannel;

			public Builder(int variant, int channelCount,
					boolean preEqInUse, int preEqBandCount,
					boolean mbcInUse, int mbcBandCount,
					boolean postEqInUse, int postEqBandCount,
					boolean limiterInUse) {
				mVariant = variant;
				mChannelCount = channelCount;
				mPreEqInUse = preEqInUse;
				mPreEqBandCount = preEqBandCount;
				mMbcInUse = mbcInUse;
				mMbcBandCount = mbcBandCount;
				mPostEqInUse = postEqInUse;
				mPostEqBandCount = postEqBandCount;
				mLimiterInUse = limiterInUse;
				mChannel = new Channel[mChannelCount];
				for (int ch = 0; ch < mChannelCount; ch++) {
					this.mChannel[ch] = new Channel(CHANNEL_DEFAULT_INPUT_GAIN,
							this.mPreEqInUse, this.mPreEqBandCount,
							this.mMbcInUse, this.mMbcBandCount,
							this.mPostEqInUse, this.mPostEqBandCount,
							this.mLimiterInUse);
				}
			}

			private void checkChannel(int channelIndex) {
				if (channelIndex < 0 || channelIndex >= mChannel.length) {
					throw new IllegalArgumentException("ChannelIndex out of bounds");
				}
			}

			public Builder setPreferredFrameDuration(float frameDuration) {
				if (frameDuration < 0) {
					throw new IllegalArgumentException("Expected positive frameDuration");
				}
				mPreferredFrameDuration = frameDuration;
				return this;
			}

			public Builder setInputGainByChannelIndex(int channelIndex, float inputGain) {
				checkChannel(channelIndex);
				mChannel[channelIndex].setInputGain(inputGain);
				return this;
			}
			public Builder setInputGainAllChannelsTo(float inputGain) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					mChannel[ch].setInputGain(inputGain);
				}
				return this;
			}

			public Builder setChannelTo(int channelIndex, Channel channel) {
				checkChannel(channelIndex);
				if (mMbcBandCount != channel.getMbc().getBandCount()) {
					throw new IllegalArgumentException("MbcBandCount changed from " +
							mMbcBandCount + " to " + channel.getPreEq().getBandCount());
				}
				if (mPreEqBandCount != channel.getPreEq().getBandCount()) {
					throw new IllegalArgumentException("PreEqBandCount changed from " +
							mPreEqBandCount + " to " + channel.getPreEq().getBandCount());
				}
				if (mPostEqBandCount != channel.getPostEq().getBandCount()) {
					throw new IllegalArgumentException("PostEqBandCount changed from " +
							mPostEqBandCount + " to " + channel.getPostEq().getBandCount());
				}
				mChannel[channelIndex] = new Channel(channel);
				return this;
			}
			public Builder setAllChannelsTo(Channel channel) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					setChannelTo(ch, channel);
				}
				return this;
			}

			public Builder setPreEqByChannelIndex(int channelIndex, Eq preEq) {
				checkChannel(channelIndex);
				mChannel[channelIndex].setPreEq(preEq);
				return this;
			}
			public Builder setPreEqAllChannelsTo(Eq preEq) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					setPreEqByChannelIndex(ch, preEq);
				}
				return this;
			}

			public Builder setMbcByChannelIndex(int channelIndex, Mbc mbc) {
				checkChannel(channelIndex);
				mChannel[channelIndex].setMbc(mbc);
				return this;
			}
			public Builder setMbcAllChannelsTo(Mbc mbc) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					setMbcByChannelIndex(ch, mbc);
				}
				return this;
			}

			public Builder setPostEqByChannelIndex(int channelIndex, Eq postEq) {
				checkChannel(channelIndex);
				mChannel[channelIndex].setPostEq(postEq);
				return this;
			}
			public Builder setPostEqAllChannelsTo(Eq postEq) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					setPostEqByChannelIndex(ch, postEq);
				}
				return this;
			}

			public Builder setLimiterByChannelIndex(int channelIndex, Limiter limiter) {
				checkChannel(channelIndex);
				mChannel[channelIndex].setLimiter(limiter);
				return this;
			}
			public Builder setLimiterAllChannelsTo(Limiter limiter) {
				for (int ch = 0; ch < mChannel.length; ch++) {
					setLimiterByChannelIndex(ch, limiter);
				}
				return this;
			}

			public Config build() {
				return new Config(mVariant, mPreferredFrameDuration, mChannelCount,
						mPreEqInUse, mPreEqBandCount,
						mMbcInUse, mMbcBandCount,
						mPostEqInUse, mPostEqBandCount,
						mLimiterInUse, mChannel);
			}
		}
	}
	public Channel getChannelByChannelIndex(int channelIndex) {
		return queryEngineByChannelIndex(channelIndex);
	}

	public void setChannelTo(int channelIndex, Channel channel) {
		updateEngineChannelByChannelIndex(channelIndex, channel);
	}

	public void setAllChannelsTo(Channel channel) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setChannelTo(ch, channel);
		}
	}
	public float getInputGainByChannelIndex(int channelIndex) {
		return getTwoFloat(PARAM_INPUT_GAIN, channelIndex);
	}
	public void setInputGainbyChannel(int channelIndex, float inputGain) {
		setTwoFloat(PARAM_INPUT_GAIN, channelIndex, inputGain);
	}
	public void setInputGainAllChannelsTo(float inputGain) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setInputGainbyChannel(ch, inputGain);
		}
	}
	public Eq getPreEqByChannelIndex(int channelIndex) {
		return queryEngineEqByChannelIndex(PARAM_PRE_EQ, channelIndex);
	}
	public void setPreEqByChannelIndex(int channelIndex, Eq preEq) {
		updateEngineEqByChannelIndex(PARAM_PRE_EQ, channelIndex, preEq);
	}
	public void setPreEqAllChannelsTo(Eq preEq) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setPreEqByChannelIndex(ch, preEq);
		}
	}
	public EqBand getPreEqBandByChannelIndex(int channelIndex, int band) {
		return queryEngineEqBandByChannelIndex(PARAM_PRE_EQ_BAND, channelIndex, band);
	}
	public void setPreEqBandByChannelIndex(int channelIndex, int band, EqBand preEqBand) {
		updateEngineEqBandByChannelIndex(PARAM_PRE_EQ_BAND, channelIndex, band, preEqBand);
	}
	public void setPreEqBandAllChannelsTo(int band, EqBand preEqBand) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setPreEqBandByChannelIndex(ch, band, preEqBand);
		}
	}
	public Mbc getMbcByChannelIndex(int channelIndex) {
		return queryEngineMbcByChannelIndex(channelIndex);
	}
	public void setMbcByChannelIndex(int channelIndex, Mbc mbc) {
		updateEngineMbcByChannelIndex(channelIndex, mbc);
	}
	public void setMbcAllChannelsTo(Mbc mbc) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setMbcByChannelIndex(ch, mbc);
		}
	}
	public MbcBand getMbcBandByChannelIndex(int channelIndex, int band) {
		return queryEngineMbcBandByChannelIndex(channelIndex, band);
	}
	public void setMbcBandByChannelIndex(int channelIndex, int band, MbcBand mbcBand) {
		updateEngineMbcBandByChannelIndex(channelIndex, band, mbcBand);
	}
	public void setMbcBandAllChannelsTo(int band, MbcBand mbcBand) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setMbcBandByChannelIndex(ch, band, mbcBand);
		}
	}
	public Eq getPostEqByChannelIndex(int channelIndex) {
		return queryEngineEqByChannelIndex(PARAM_POST_EQ, channelIndex);
	}
	public void setPostEqByChannelIndex(int channelIndex, Eq postEq) {
		updateEngineEqByChannelIndex(PARAM_POST_EQ, channelIndex, postEq);
	}
	public void setPostEqAllChannelsTo(Eq postEq) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setPostEqByChannelIndex(ch, postEq);
		}
	}
	public EqBand getPostEqBandByChannelIndex(int channelIndex, int band) {
		return queryEngineEqBandByChannelIndex(PARAM_POST_EQ_BAND, channelIndex, band);
	}
	public void setPostEqBandByChannelIndex(int channelIndex, int band, EqBand postEqBand) {
		updateEngineEqBandByChannelIndex(PARAM_POST_EQ_BAND, channelIndex, band, postEqBand);
	}
	public void setPostEqBandAllChannelsTo(int band, EqBand postEqBand) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setPostEqBandByChannelIndex(ch, band, postEqBand);
		}
	}
	public Limiter getLimiterByChannelIndex(int channelIndex) {
		return queryEngineLimiterByChannelIndex(channelIndex);
	}
	public void setLimiterByChannelIndex(int channelIndex, Limiter limiter) {
		updateEngineLimiterByChannelIndex(channelIndex, limiter);
	}
	public void setLimiterAllChannelsTo(Limiter limiter) {
		for (int ch = 0; ch < mChannelCount; ch++) {
			setLimiterByChannelIndex(ch, limiter);
		}
	}

	public int getChannelCount() {
		return getOneInt(PARAM_GET_CHANNEL_COUNT);
	}
	private void setEngineArchitecture(int variant, float preferredFrameDuration,
			boolean preEqInUse, int preEqBandCount, boolean mbcInUse, int mbcBandCount,
			boolean postEqInUse, int postEqBandCount, boolean limiterInUse) {

		Number[] params = { PARAM_ENGINE_ARCHITECTURE };
		Number[] values = { variant ,
				preferredFrameDuration,
				(preEqInUse ? 1 : 0),
				preEqBandCount,
				(mbcInUse ? 1 : 0),
				mbcBandCount,
				(postEqInUse ? 1 : 0),
				postEqBandCount,
				(limiterInUse ? 1 : 0)};
		setNumberArray(params, values);
	}

	private void updateEngineEqBandByChannelIndex(int param, int channelIndex, int bandIndex,
			EqBand eqBand) {
		Number[] params = {param,
				channelIndex,
				bandIndex};
		Number[] values = {(eqBand.isEnabled() ? 1 : 0),
				eqBand.getCutoffFrequency(),
				eqBand.getGain()};
		setNumberArray(params, values);
	}
	private Eq queryEngineEqByChannelIndex(int param, int channelIndex) {

		Number[] params = {param == PARAM_PRE_EQ ? PARAM_PRE_EQ : PARAM_POST_EQ,
				channelIndex};
		Number[] values = {0 ,
							0 ,
							0 };
		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);
		byteArrayToNumberArray(valueBytes, values);
		int bandCount = values[2].intValue();
		Eq eq = new Eq(values[0].intValue() > 0 ,
				values[1].intValue() > 0 ,
				bandCount );
		for (int b = 0; b < bandCount; b++) {
			EqBand eqBand = queryEngineEqBandByChannelIndex(param == PARAM_PRE_EQ ?
					PARAM_PRE_EQ_BAND : PARAM_POST_EQ_BAND, channelIndex, b);
			eq.setBand(b, eqBand);
		}
		return eq;
	}
	private EqBand queryEngineEqBandByChannelIndex(int param, int channelIndex, int bandIndex) {
		Number[] params = {param,
				channelIndex,
				bandIndex};
		Number[] values = {0 ,
							0.0f ,
							0.0f };

		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);

		byteArrayToNumberArray(valueBytes, values);

		return new EqBand(values[0].intValue() > 0 ,
				values[1].floatValue() ,
				values[2].floatValue() );
	}
	private void updateEngineEqByChannelIndex(int param, int channelIndex, Eq eq) {
		int bandCount = eq.getBandCount();
		Number[] params = {param,
				channelIndex};
		Number[] values = { (eq.isInUse() ? 1 : 0),
				(eq.isEnabled() ? 1 : 0),
				bandCount};
		setNumberArray(params, values);
		for (int b = 0; b < bandCount; b++) {
			EqBand eqBand = eq.getBand(b);
			updateEngineEqBandByChannelIndex(param == PARAM_PRE_EQ ?
					PARAM_PRE_EQ_BAND : PARAM_POST_EQ_BAND, channelIndex, b, eqBand);
		}
	}

	private Mbc queryEngineMbcByChannelIndex(int channelIndex) {
		Number[] params = {PARAM_MBC,
				channelIndex};
		Number[] values = {0 ,
							0 ,
							0 };
		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);
		byteArrayToNumberArray(valueBytes, values);
		int bandCount = values[2].intValue();
		Mbc mbc = new Mbc(values[0].intValue() > 0 ,
				values[1].intValue() > 0 ,
				bandCount );
		for (int b = 0; b < bandCount; b++) {
			MbcBand mbcBand = queryEngineMbcBandByChannelIndex(channelIndex, b);
			mbc.setBand(b, mbcBand);
		}
		return mbc;
	}
	private MbcBand queryEngineMbcBandByChannelIndex(int channelIndex, int bandIndex) {
		Number[] params = {PARAM_MBC_BAND,
				channelIndex,
				bandIndex};
		Number[] values = {0 ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f };

		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);

		byteArrayToNumberArray(valueBytes, values);

		return new MbcBand(values[0].intValue() > 0 ,
				values[1].floatValue() ,
				values[2].floatValue(),
				values[3].floatValue(),
				values[4].floatValue(),
				values[5].floatValue(),
				values[6].floatValue(),
				values[7].floatValue(),
				values[8].floatValue(),
				values[9].floatValue(),
				values[10].floatValue());
	}
	private void updateEngineMbcBandByChannelIndex(int channelIndex, int bandIndex,
			MbcBand mbcBand) {
		Number[] params = { PARAM_MBC_BAND,
				channelIndex,
				bandIndex};
		Number[] values = {(mbcBand.isEnabled() ? 1 : 0),
				mbcBand.getCutoffFrequency(),
				mbcBand.getAttackTime(),
				mbcBand.getReleaseTime(),
				mbcBand.getRatio(),
				mbcBand.getThreshold(),
				mbcBand.getKneeWidth(),
				mbcBand.getNoiseGateThreshold(),
				mbcBand.getExpanderRatio(),
				mbcBand.getPreGain(),
				mbcBand.getPostGain()};
		setNumberArray(params, values);
	}

	private void updateEngineMbcByChannelIndex(int channelIndex, Mbc mbc) {
		int bandCount = mbc.getBandCount();
		Number[] params = { PARAM_MBC,
				channelIndex};
		Number[] values = {(mbc.isInUse() ? 1 : 0),
				(mbc.isEnabled() ? 1 : 0),
				bandCount};
		setNumberArray(params, values);
		for (int b = 0; b < bandCount; b++) {
			MbcBand mbcBand = mbc.getBand(b);
			updateEngineMbcBandByChannelIndex(channelIndex, b, mbcBand);
		}
	}

	private void updateEngineLimiterByChannelIndex(int channelIndex, Limiter limiter) {
		Number[] params = { PARAM_LIMITER,
				channelIndex};
		Number[] values = {(limiter.isInUse() ? 1 : 0),
				(limiter.isEnabled() ? 1 : 0),
				limiter.getLinkGroup(),
				limiter.getAttackTime(),
				limiter.getReleaseTime(),
				limiter.getRatio(),
				limiter.getThreshold(),
				limiter.getPostGain()};
		setNumberArray(params, values);
	}

	private Limiter queryEngineLimiterByChannelIndex(int channelIndex) {
		Number[] params = {PARAM_LIMITER,
				channelIndex};
		Number[] values = {0 ,
				0 ,
				0 ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f ,
				0.0f };

		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values); //just interest in the byte size.
		getParameter(paramBytes, valueBytes);
		byteArrayToNumberArray(valueBytes, values);

		return new Limiter(values[0].intValue() > 0 ,
				values[1].intValue() > 0 ,
				values[2].intValue() ,
				values[3].floatValue() ,
				values[4].floatValue() ,
				values[5].floatValue() ,
				values[6].floatValue() ,
				values[7].floatValue() );
	}

	private Channel queryEngineByChannelIndex(int channelIndex) {
		float inputGain = getTwoFloat(PARAM_INPUT_GAIN, channelIndex);
		Eq preEq = queryEngineEqByChannelIndex(PARAM_PRE_EQ, channelIndex);
		Mbc mbc = queryEngineMbcByChannelIndex(channelIndex);
		Eq postEq = queryEngineEqByChannelIndex(PARAM_POST_EQ, channelIndex);
		Limiter limiter = queryEngineLimiterByChannelIndex(channelIndex);

		Channel channel = new Channel(inputGain,
				preEq.isInUse(), preEq.getBandCount(),
				mbc.isInUse(), mbc.getBandCount(),
				postEq.isInUse(), postEq.getBandCount(),
				limiter.isInUse());
		channel.setInputGain(inputGain);
		channel.setPreEq(preEq);
		channel.setMbc(mbc);
		channel.setPostEq(postEq);
		channel.setLimiter(limiter);
		return channel;
	}

	private void updateEngineChannelByChannelIndex(int channelIndex, Channel channel) {
		setTwoFloat(PARAM_INPUT_GAIN, channelIndex, channel.getInputGain());
		Eq preEq = channel.getPreEq();
		updateEngineEqByChannelIndex(PARAM_PRE_EQ, channelIndex, preEq);
		Mbc mbc = channel.getMbc();
		updateEngineMbcByChannelIndex(channelIndex, mbc);
		Eq postEq = channel.getPostEq();
		updateEngineEqByChannelIndex(PARAM_POST_EQ, channelIndex, postEq);
		Limiter limiter = channel.getLimiter();
		updateEngineLimiterByChannelIndex(channelIndex, limiter);
	}
	private int getOneInt(int param) {
		final int[] params = { param };
		final int[] result = new int[1];

		checkStatus(getParameter(params, result));
		return result[0];
	}

	private void setTwoFloat(int param, int paramA, float valueSet) {
		final int[] params = { param, paramA };
		final byte[] value;

		value = floatToByteArray(valueSet);
		checkStatus(setParameter(params, value));
	}

	private byte[] numberArrayToByteArray(Number[] values) {
		int expectedBytes = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Integer) {
				expectedBytes += Integer.BYTES;
			} else if (values[i] instanceof Float) {
				expectedBytes += Float.BYTES;
			} else {
				throw new IllegalArgumentException("unknown value type " +
						values[i].getClass());
			}
		}
		ByteBuffer converter = ByteBuffer.allocate(expectedBytes);
		converter.order(ByteOrder.nativeOrder());
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Integer) {
				converter.putInt(values[i].intValue());
			} else if (values[i] instanceof Float) {
				converter.putFloat(values[i].floatValue());
			}
		}
		return converter.array();
	}

	private void byteArrayToNumberArray(byte[] valuesIn, Number[] valuesOut) {
		int inIndex = 0;
		int outIndex = 0;
		while (inIndex < valuesIn.length && outIndex < valuesOut.length) {
			if (valuesOut[outIndex] instanceof Integer) {
				valuesOut[outIndex++] = byteArrayToInt(valuesIn, inIndex);
				inIndex += Integer.BYTES;
			} else if (valuesOut[outIndex] instanceof Float) {
				valuesOut[outIndex++] = byteArrayToFloat(valuesIn, inIndex);
				inIndex += Float.BYTES;
			} else {
				throw new IllegalArgumentException("can't convert " +
						valuesOut[outIndex].getClass());
			}
		}
		if (outIndex != valuesOut.length) {
			throw new IllegalArgumentException("only converted " + outIndex +
					" values out of "+ valuesOut.length + " expected");
		}
	}

	private void setNumberArray(Number[] params, Number[] values) {
		byte[] paramBytes = numberArrayToByteArray(params);
		byte[] valueBytes = numberArrayToByteArray(values);
		checkStatus(setParameter(paramBytes, valueBytes));
	}

	private float getTwoFloat(int param, int paramA) {
		final int[] params = { param, paramA };
		final byte[] result = new byte[4];

		checkStatus(getParameter(params, result));
		return byteArrayToFloat(result);
	}

	public interface OnParameterChangeListener {
		
		void onParameterChange(DynamicsProcessing effect, int param, int value);
	}

	private void updateEffectArchitecture() {
		mChannelCount = getChannelCount();
	}

	private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
		private BaseParameterListener() {

		}
		public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
			if (status != AudioEffect.SUCCESS) {
				return;
			}
			OnParameterChangeListener l = null;
			synchronized (mParamListenerLock) {
				if (mParamListener != null) {
					l = mParamListener;
				}
			}
			if (l != null) {
				int p = -1;
				int v = Integer.MIN_VALUE;

				if (param.length == 4) {
					p = byteArrayToInt(param, 0);
				}
				if (value.length == 4) {
					v = byteArrayToInt(value, 0);
				}
				if (p != -1 && v != Integer.MIN_VALUE) {
					l.onParameterChange(DynamicsProcessing.this, p, v);
				}
			}
		}
	}

	public void setParameterListener(OnParameterChangeListener listener) {
		synchronized (mParamListenerLock) {
			if (mParamListener == null) {
				mBaseParamListener = new BaseParameterListener();
				super.setParameterListener(mBaseParamListener);
			}
			mParamListener = listener;
		}
	}

	public static class Settings {
		public int channelCount;
		public float[] inputGain;

		public Settings() {
		}

		public Settings(String settings) {
			StringTokenizer st = new StringTokenizer(settings, "=;");
			if (st.countTokens() != 3) {
				throw new IllegalArgumentException("settings: " + settings);
			}
			String key = st.nextToken();
			if (!key.equals("DynamicsProcessing")) {
				throw new IllegalArgumentException(
						"invalid settings for DynamicsProcessing: " + key);
			}
			try {
				key = st.nextToken();
				if (!key.equals("channelCount")) {
					throw new IllegalArgumentException("invalid key name: " + key);
				}
				channelCount = Short.parseShort(st.nextToken());
				if (channelCount > CHANNEL_COUNT_MAX) {
					throw new IllegalArgumentException("too many channels Settings:" + settings);
				}
				if (st.countTokens() != channelCount*1) { //check expected parameters.
					throw new IllegalArgumentException("settings: " + settings);
				}
				inputGain = new float[channelCount];
				for (int ch = 0; ch < channelCount; ch++) {
					key = st.nextToken();
					if (!key.equals(ch +"_inputGain")) {
						throw new IllegalArgumentException("invalid key name: " + key);
					}
					inputGain[ch] = Float.parseFloat(st.nextToken());
				}
			 } catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("invalid value for key: " + key);
			}
		}

		@Override
		public String toString() {
			String str = new String (
					"DynamicsProcessing"+
					";channelCount="+Integer.toString(channelCount));
					for (int ch = 0; ch < channelCount; ch++) {
						str = str.concat(";"+ch+"_inputGain="+Float.toString(inputGain[ch]));
					}
			return str;
		}
	};

	public DynamicsProcessing.Settings getProperties() {
		Settings settings = new Settings();

		settings.channelCount = getChannelCount();

		if (settings.channelCount > CHANNEL_COUNT_MAX) {
			throw new IllegalArgumentException("too many channels Settings:" + settings);
		}

		{ // get inputGainmB per channel
			settings.inputGain = new float [settings.channelCount];
			for (int ch = 0; ch < settings.channelCount; ch++) {
			}
		}
		return settings;
	}

	public void setProperties(DynamicsProcessing.Settings settings) {

		if (settings.channelCount != settings.inputGain.length ||
				settings.channelCount != mChannelCount) {
				throw new IllegalArgumentException("settings invalid channel count: "
				+ settings.channelCount);
			}
		for (int ch = 0; ch < mChannelCount; ch++) {
		}
	}
}
