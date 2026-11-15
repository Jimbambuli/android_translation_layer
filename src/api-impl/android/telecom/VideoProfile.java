package android.telecom;

/* The public constants of android.telecom.VideoProfile (the telecomm sources are not in the local AOSP copy, the
 * values are those of the SDK documentation); the profile object itself is not implemented. */
public class VideoProfile {
	public static final int QUALITY_UNKNOWN = 0;
	public static final int QUALITY_HIGH = 1;
	public static final int QUALITY_MEDIUM = 2;
	public static final int QUALITY_LOW = 3;
	public static final int QUALITY_DEFAULT = 4;

	public static final int STATE_AUDIO_ONLY = 0x0;
	public static final int STATE_TX_ENABLED = 0x1;
	public static final int STATE_RX_ENABLED = 0x2;
	public static final int STATE_BIDIRECTIONAL = STATE_TX_ENABLED | STATE_RX_ENABLED;
	public static final int STATE_PAUSED = 0x4;
}
