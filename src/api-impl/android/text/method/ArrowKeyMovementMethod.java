package android.text.method;

/* AOSP: moves the cursor and the selection of an editable text with the arrow keys and touches. In ATL the GTK
 * text widget does that itself, like for the other movement methods; apps set or subclass it for their editors */
public class ArrowKeyMovementMethod extends BaseMovementMethod {

	private static ArrowKeyMovementMethod sInstance;

	public static MovementMethod getInstance() {
		if (sInstance == null)
			sInstance = new ArrowKeyMovementMethod();
		return sInstance;
	}
}
