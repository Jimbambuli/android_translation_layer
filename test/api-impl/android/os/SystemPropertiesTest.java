/*
 * Host test for the persist.sys.timezone lookup in SystemProperties. It needs the host's
 * /usr/share/zoneinfo (Europe/Zurich). Run with the runtime's dalvikvm, e.g. from the build dir:
 *   javac -source 8 -target 8 -bootclasspath $PREFIX/lib64/java/core-all_classes.jar \
 *     -cp src/api-impl/hax.jar:$PREFIX/lib64/java/core-junit_classes.jar -d t ../test/api-impl/android/os/SystemPropertiesTest.java
 *   dx --dex --output=t.jar t
 *   dalvikvm -Xbootclasspath/a:t.jar:api-impl.jar junit.textui.TestRunner android.os.SystemPropertiesTest
 * (junit is on the boot class path, so the test and api-impl.jar go there too)
 */
package android.os;

import junit.framework.TestCase;

public class SystemPropertiesTest extends TestCase {
	public void testTzZoneId() {
		assertEquals("Europe/Zurich", SystemProperties.hostTimeZoneId("Europe/Zurich", null, null));
	}

	public void testTzColonPath() {
		assertEquals("Europe/Zurich", SystemProperties.hostTimeZoneId(":/usr/share/zoneinfo/Europe/Zurich", "Asia/Tokyo", null));
		assertEquals("Europe/Zurich", SystemProperties.hostTimeZoneId(":Europe/Zurich", null, null));
	}

	public void testTzPosixRuleIsNoId() {
		assertEquals("Asia/Tokyo", SystemProperties.hostTimeZoneId("CET-1CEST,M3.5.0,M10.5.0/3", null, "../usr/share/zoneinfo/Asia/Tokyo"));
		assertNull(SystemProperties.hostTimeZoneId("CET-1CEST", null, null));
	}

	public void testEtcTimezone() {
		assertEquals("Europe/Berlin", SystemProperties.hostTimeZoneId(null, "Europe/Berlin\n", "/usr/share/zoneinfo/Etc/UTC"));
		assertEquals("Europe/Berlin", SystemProperties.hostTimeZoneId("", "Europe/Berlin\n", null));
	}

	public void testEmptyEtcTimezoneFallsThrough() {
		assertEquals("America/New_York", SystemProperties.hostTimeZoneId(null, " \n", "/usr/share/zoneinfo/America/New_York"));
	}

	public void testLocaltimeSymlink() {
		assertEquals("Europe/Zurich", SystemProperties.hostTimeZoneId(null, null, "../usr/share/zoneinfo/Europe/Zurich"));
	}

	public void testNothingLeavesItToLibcore() {
		assertNull(SystemProperties.hostTimeZoneId(null, null, null));
	}

	private static String written(String content) throws java.io.IOException {
		java.io.File f = java.io.File.createTempFile("firmware", null);
		f.deleteOnExit();
		try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
			out.write(content.getBytes("UTF-8"));
		}
		return f.getPath();
	}

	public void testFirmwareNameDmiLine() throws Exception {
		assertEquals("HP ProDesk 600 G6 Small Form Factor PC", SystemProperties.firmwareName(written("HP ProDesk 600 G6 Small Form Factor PC\n")));
	}

	public void testFirmwareNameDeviceTreeNul() throws Exception {
		assertEquals("PINE64 PinePhone", SystemProperties.firmwareName(written("PINE64 PinePhone\0")));
	}

	public void testFirmwareNamePlaceholderAndMissing() throws Exception {
		assertNull(SystemProperties.firmwareName(written("To Be Filled By O.E.M.\n")));
		assertNull(SystemProperties.firmwareName(written("System Product Name\n")));
		assertNull(SystemProperties.firmwareName(written(" \n")));
		assertNull(SystemProperties.firmwareName("/nonexistent/sys_vendor"));
	}
}
