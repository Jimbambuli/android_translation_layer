/*
 * Host test for RouteDiscoveryPreference and ModuleInfo (android-14), both of which use List.of / List.copyOf of the
 * runtime's libcore: builder, unmodifiable lists, Parcel round trip, equality. Pure Java. Run like PersistableBundleTest
 * (dx needs --min-sdk-version=26) with the class android.media.RouteDiscoveryPreferenceTest.
 */
package android.media;

import android.content.pm.ModuleInfo;
import android.os.Parcel;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public class RouteDiscoveryPreferenceTest extends TestCase {

	public void testRouteDiscoveryPreferenceRoundTrip() {
		RouteDiscoveryPreference p = new RouteDiscoveryPreference.Builder(Arrays.asList("f1", "f2"), true)
		                                 .setAllowedPackages(Arrays.asList("a.b", "c.d"))
		                                 .setDeduplicationPackageOrder(Arrays.asList("c.d", "a.b"))
		                                 .build();
		assertEquals(Arrays.asList("f1", "f2"), p.getPreferredFeatures());
		assertEquals(Arrays.asList("a.b", "c.d"), p.getAllowedPackages());
		assertTrue(p.shouldPerformActiveScan());
		try {
			p.getAllowedPackages().add("x");
			fail("list must be unmodifiable");
		} catch (UnsupportedOperationException e) {
		}
		Parcel parcel = Parcel.obtain();
		p.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		RouteDiscoveryPreference q = RouteDiscoveryPreference.CREATOR.createFromParcel(parcel);
		parcel.recycle();
		assertEquals(p, q);
		assertEquals(p.hashCode(), q.hashCode());
		List<String> none = RouteDiscoveryPreference.EMPTY.getAllowedPackages();
		assertTrue(none.isEmpty());
	}

	public void testModuleInfoCopyAndRoundTrip() {
		ModuleInfo m = new ModuleInfo().setName("mod").setPackageName("p.q").setHidden(true).setApexModuleName("apex")
		                   .setApkInApexPackageNames(Arrays.asList("x", "y"));
		ModuleInfo copy = new ModuleInfo(m);
		assertEquals(m, copy);
		assertEquals(2, copy.getApkInApexPackageNames().size());
		Parcel parcel = Parcel.obtain();
		m.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		ModuleInfo r = ModuleInfo.CREATOR.createFromParcel(parcel);
		parcel.recycle();
		assertEquals(m, r);
		assertEquals("p.q", r.getPackageName());
		assertTrue(r.isHidden());
	}
}
