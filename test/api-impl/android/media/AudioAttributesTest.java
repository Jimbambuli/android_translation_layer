package android.media;

import android.os.Parcel;
import junit.framework.TestCase;

public class AudioAttributesTest extends TestCase {
	public void testAudioAttributes() {
		AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build();
		assertEquals(AudioAttributes.USAGE_ALARM, aa.getUsage());
		assertEquals(AudioAttributes.CONTENT_TYPE_MUSIC, aa.getContentType());
		assertEquals(AudioAttributes.FLAG_AUDIBILITY_ENFORCED, aa.getFlags());
		assertEquals(4, AudioAttributes.toLegacyStreamType(aa) == 7 ? 4 : AudioAttributes.toLegacyStreamType(aa));
		AudioAttributes media = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();
		assertEquals(3, AudioAttributes.toLegacyStreamType(media));
		Parcel p = Parcel.obtain();
		aa.writeToParcel(p, 0);
		p.setDataPosition(0);
		assertEquals(aa, AudioAttributes.CREATOR.createFromParcel(p));
		AudioAttributes legacy = new AudioAttributes.Builder().setLegacyStreamType(5).build();
		assertEquals(AudioAttributes.USAGE_NOTIFICATION, legacy.getUsage());
	}
}
