package com.android.internal.os;

import android.atl.annotation.Export;
import android.os.SystemProperties;
import java.util.TimeZone;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

/**
 * Main entry point for runtime initialization.  Not for
 * public consumption.
 * @hide
 */
public class RuntimeInit {
	/* the part of AOSP's commonInit() that applies here; called from main() before any app code */
	@Export
	protected static final void commonInit() {
		/*
		 * Install a TimezoneGetter subclass for ZoneInfo.db
		 */
		TimezoneGetter.setInstance(new TimezoneGetter() {
			@Override
			public String getId() {
				return SystemProperties.get("persist.sys.timezone");
			}
		});
		TimeZone.setDefault(null);
	}
}
