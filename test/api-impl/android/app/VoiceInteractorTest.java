package android.app;

import android.os.Parcel;
import junit.framework.TestCase;

public class VoiceInteractorTest extends TestCase {
	public void testVoiceInteractorRequests() {
		VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt("Ja oder nein");
		assertEquals(1, prompt.countVoicePrompts());
		VoiceInteractor.PickOptionRequest.Option o = new VoiceInteractor.PickOptionRequest.Option("eins", 1).addSynonym("1");
		Parcel p = Parcel.obtain();
		o.writeToParcel(p, 0);
		p.setDataPosition(0);
		assertEquals(1, VoiceInteractor.PickOptionRequest.Option.CREATOR.createFromParcel(p).countSynonyms());
	}
}
