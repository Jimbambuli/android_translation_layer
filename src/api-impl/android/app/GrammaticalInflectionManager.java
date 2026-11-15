package android.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* AOSP GrammaticalInflectionManager (android-16.0.0_r4). AOSP keeps the requested gender per application in
 * the grammatical inflection service (system server, source not in the AOSP tree used here), which applies it as a
 * configuration change of the app. ATL runs one app: the gender is held in the process and applied the way
 * LocaleManager applies locales (the configuration of the application, ActivityThread.handleConfigurationChanged()).
 * The system-wide gender is the setting of the device, which an app can neither change (the service demands
 * CHANGE_CONFIGURATION) nor, without READ_SYSTEM_GRAMMATICAL_GENDER, read; ATL has none set. */
public class GrammaticalInflectionManager {
	public static final Set<Integer> VALID_GRAMMATICAL_GENDER_VALUES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
	    Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED, Configuration.GRAMMATICAL_GENDER_NEUTRAL,
	    Configuration.GRAMMATICAL_GENDER_FEMININE, Configuration.GRAMMATICAL_GENDER_MASCULINE)));

	private final Context mContext;

	/** @hide */
	public GrammaticalInflectionManager(Context context) {
		mContext = context;
	}

	/* AOSP: the grammatical gender of the application context's configuration */
	public int getApplicationGrammaticalGender() {
		return mContext.getApplicationContext().getResources().getConfiguration().getGrammaticalGender();
	}

	public void setRequestedApplicationGrammaticalGender(int grammaticalGender) {
		if (!VALID_GRAMMATICAL_GENDER_VALUES.contains(grammaticalGender)) {
			throw new IllegalArgumentException("Unknown grammatical gender");
		}
		if (Context.sys_config.getGrammaticalGender() == grammaticalGender) {
			return;
		}
		Configuration config = new Configuration(Context.sys_config);
		config.setGrammaticalGender(grammaticalGender);
		ActivityThread.handleConfigurationChanged(config);
	}

	public void setSystemWideGrammaticalGender(int grammaticalGender) {
		if (!VALID_GRAMMATICAL_GENDER_VALUES.contains(grammaticalGender)) {
			throw new IllegalArgumentException("Unknown grammatical gender");
		}
		throw new SecurityException("Neither user " + android.os.Process.myUid() + " nor current process has android.permission.CHANGE_CONFIGURATION.");
	}

	public int getSystemGrammaticalGender() {
		enforceReadPermission();
		return Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED;
	}

	public int peekSystemGrammaticalGenderByUserId(int userId) {
		enforceReadPermission();
		return Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED;
	}

	private void enforceReadPermission() {
		if (mContext.checkSelfPermission("android.permission.READ_SYSTEM_GRAMMATICAL_GENDER") != PackageManager.PERMISSION_GRANTED)
			throw new SecurityException("Permission Denial: requires android.permission.READ_SYSTEM_GRAMMATICAL_GENDER");
	}
}
