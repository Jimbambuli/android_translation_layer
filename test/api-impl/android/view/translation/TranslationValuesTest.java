package android.view.translation;

import android.os.Parcel;
import android.view.autofill.AutofillId;
import junit.framework.TestCase;

public class TranslationValuesTest extends TestCase {
	public void testTranslationValues() {
		TranslationRequestValue v = TranslationRequestValue.forText("Hallo");
		assertEquals("Hallo", v.getText().toString());
		ViewTranslationRequest req = new ViewTranslationRequest.Builder(new AutofillId(7)).setValue(ViewTranslationRequest.ID_TEXT, v).build();
		assertEquals("Hallo", req.getValue(ViewTranslationRequest.ID_TEXT).getText().toString());
		Parcel p = Parcel.obtain();
		req.writeToParcel(p, 0);
		p.setDataPosition(0);
		ViewTranslationRequest back = ViewTranslationRequest.CREATOR.createFromParcel(p);
		assertEquals("Hallo", back.getValue(ViewTranslationRequest.ID_TEXT).getText().toString());
		ViewTranslationResponse res = new ViewTranslationResponse.Builder(new AutofillId(7)).setValue(ViewTranslationRequest.ID_TEXT, TranslationResponseValue.forError()).build();
		assertEquals(TranslationResponseValue.STATUS_ERROR, res.getValue(ViewTranslationRequest.ID_TEXT).getStatusCode());
	}
}
