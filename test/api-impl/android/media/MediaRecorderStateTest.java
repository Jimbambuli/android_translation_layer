/*
 * Host test for MediaRecorder's rules before the microphone is opened: like AOSP, prepare() needs an audio
 * source and an output file (IllegalStateException), start() needs prepare(); video is not recorded yet
 * (IOException at prepare). Run like test/api-impl/android/os/SystemPropertiesTest.java.
 */
package android.media;

import java.io.IOException;
import junit.framework.TestCase;

public class MediaRecorderStateTest extends TestCase {
	public void testPrepareNeedsSourceAndFile() throws IOException {
		MediaRecorder r = new MediaRecorder();
		r.setAudioSource(MediaRecorder.AudioSource.MIC);
		r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		try {
			r.prepare();
			fail("prepare() without an output file");
		} catch (IllegalStateException expected) {
		}
		r.reset();
		r.setOutputFile("/nonexistent/a.m4a");
		try {
			r.prepare();
			fail("prepare() without an audio source");
		} catch (IllegalStateException expected) {
		}
	}

	public void testStartNeedsPrepare() {
		MediaRecorder r = new MediaRecorder();
		r.setAudioSource(MediaRecorder.AudioSource.MIC);
		try {
			r.start();
			fail("start() before prepare()");
		} catch (IllegalStateException expected) {
		}
		assertEquals(0, r.getMaxAmplitude());
	}

	public void testVideoIsNotRecordedYet() {
		MediaRecorder r = new MediaRecorder();
		r.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		r.setAudioSource(MediaRecorder.AudioSource.MIC);
		r.setOutputFile("/nonexistent/a.mp4");
		try {
			r.prepare();
			fail("video");
		} catch (IOException expected) {
		}
	}

	public void testAospConstants() {
		assertEquals(6, MediaRecorder.AudioEncoder.VORBIS);
		assertEquals(7, MediaRecorder.AudioEncoder.OPUS);
		assertEquals(2, MediaRecorder.OutputFormat.MPEG_4);
		assertEquals(11, MediaRecorder.OutputFormat.OGG);
	}
}
