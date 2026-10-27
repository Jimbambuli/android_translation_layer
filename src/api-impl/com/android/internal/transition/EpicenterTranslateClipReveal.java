package com.android.internal.transition;

import android.content.Context;
import android.transition.Visibility;
import android.util.AttributeSet;

/* AOSP's clip reveal from the epicenter of a popup (transition/popup_window_enter.xml, 250 ms). ATL has
 * no transition framework to run it; the class exists so that the XML inflates. */
public class EpicenterTranslateClipReveal extends Visibility {
	public EpicenterTranslateClipReveal() {}

	public EpicenterTranslateClipReveal(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
}
