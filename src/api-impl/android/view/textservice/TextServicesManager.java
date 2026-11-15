package android.view.textservice;

import android.os.Bundle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/* AOSP frameworks/base core/java/android/view/textservice/TextServicesManager.java.
 * ATL has no spell checker service: there is no current or enabled spell checker. */
public class TextServicesManager {

	/* AOSP isSpellCheckerEnabled() asks TextServicesManagerService; ATL has none to enable */
	public boolean isSpellCheckerEnabled() {
		return false;
	}

	/* AOSP getEnabledSpellCheckerInfos(): empty list when there are no enabled spell checkers */
	public List<SpellCheckerInfo> getEnabledSpellCheckerInfos() {
		return Collections.emptyList();
	}

	/* AOSP getCurrentSpellCheckerInfo(): null without a current spell checker */
	public SpellCheckerInfo getCurrentSpellCheckerInfo() {
		return null;
	}

	/* AOSP newSpellCheckerSession(Bundle, Locale, SpellCheckerSessionListener, boolean):
	 * argument checks as AOSP, then null because there is no spell checker (sci == null) */
	public SpellCheckerSession newSpellCheckerSession(Bundle bundle, Locale locale,
	                                                  SpellCheckerSession.SpellCheckerSessionListener listener,
	                                                  boolean referToSpellCheckerLanguageSettings) {
		if (listener == null) {
			throw new NullPointerException();
		}
		if (!referToSpellCheckerLanguageSettings && locale == null) {
			throw new IllegalArgumentException("Locale should not be null if you don't refer"
			                                   + " settings.");
		}
		return null;
	}
}
