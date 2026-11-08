/*
 * Host test for PersistableBundle and Parcel.writePersistableBundle / readPersistableBundle (android-14):
 * a round trip of every permitted value type through a Parcel, null, nesting in writeValue / writeParcelable,
 * copies, the conversion from a Bundle and the length limit. Pure Java, no native code. From the build dir:
 *   javac -source 8 -target 8 -bootclasspath $PREFIX/lib64/java/core-all_classes.jar \
 *     -cp api-impl.jar:$PREFIX/lib64/java/core-junit_classes.jar -d t ../test/api-impl/android/os/PersistableBundleTest.java
 *   dx --dex --output=t.jar t
 *   dalvikvm -Xbootclasspath/a:t.jar:api-impl.jar junit.textui.TestRunner android.os.PersistableBundleTest
 */
package android.os;

import java.util.Arrays;
import junit.framework.TestCase;

public class PersistableBundleTest extends TestCase {

	private static void check(boolean b, String m) {
		assertTrue(m, b);
	}

	public void testParcelRoundTrip() throws Exception {
	PersistableBundle in=new PersistableBundle(); in.putInt("i",3); in.putLong("l",4L); in.putDouble("d",1.5); in.putString("s","x"); in.putString("n",null);
		in.putBoolean("b",true); in.putIntArray("ia",new int[]{1,2}); in.putLongArray("la",new long[]{5L}); in.putDoubleArray("da",new double[]{2.5,3.5});
		in.putStringArray("sa",new String[]{"p","q"}); in.putBooleanArray("ba",new boolean[]{true,false});
		PersistableBundle sub=PersistableBundle.forPair("k","v"); in.putPersistableBundle("sub",sub);
		Parcel p=Parcel.obtain(); p.writePersistableBundle(in); p.writePersistableBundle(null); p.writeValue(in); p.writeParcelable(in,0);
		p.setDataPosition(0);
		PersistableBundle o=p.readPersistableBundle(); check(o!=null,"nn");
		check(o.getInt("i")==3&&o.getLong("l")==4L&&o.getDouble("d")==1.5&&"x".equals(o.getString("s"))&&o.containsKey("n")&&o.getString("n")==null,"scalars");
		check(o.getBoolean("b"),"bool"); check(Arrays.equals(o.getIntArray("ia"),new int[]{1,2}),"ia"); check(Arrays.equals(o.getLongArray("la"),new long[]{5L}),"la");
		check(Arrays.equals(o.getDoubleArray("da"),new double[]{2.5,3.5}),"da"); check(Arrays.equals(o.getStringArray("sa"),new String[]{"p","q"}),"sa");
		check(Arrays.equals(o.getBooleanArray("ba"),new boolean[]{true,false}),"ba");
		check("v".equals(o.getPersistableBundle("sub").getString("k")),"sub"); check(o.size()==in.size(),"size");
		check(p.readPersistableBundle()==null,"null");
		Object v=p.readValue(null); check(v instanceof PersistableBundle&&((PersistableBundle)v).getInt("i")==3,"value");
		Object pa=p.readParcelable(ClassLoader.getSystemClassLoader()); check(pa instanceof PersistableBundle&&((PersistableBundle)pa).getInt("i")==3,"parcelable");
		check(p.dataAvail()==0,"avail "+p.dataAvail());
		PersistableBundle dc=in.deepCopy(); check(dc.getPersistableBundle("sub")!=sub,"deep"); check(((PersistableBundle)in.clone()).getPersistableBundle("sub")==sub,"shallow");
		Bundle bb=new Bundle(); bb.putInt("x",1); Bundle nb=new Bundle(); nb.putString("y","z"); bb.putBundle("nb",nb);
		PersistableBundle fb=new PersistableBundle(bb); check(fb.getInt("x")==1&&fb.getPersistableBundle("nb").getString("y").equals("z"),"fromBundle");
		bb.putFloat("bad", 1f); try{ new PersistableBundle(bb); check(false,"noexc"); }catch(IllegalArgumentException e){}
		check(PersistableBundle.EMPTY.isEmpty(),"empty"); check(in.isBundleContentsWithinLengthLimit(10),"lim"); check(!in.isBundleContentsWithinLengthLimit(0),"lim0");
}
}
