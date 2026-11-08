/*
 * Host test for List.of / List.copyOf / Set.of / Set.copyOf (Java 9/10, Android 11) of the runtime's libcore:
 * unmodifiable, no nulls (also for contains), duplicates in Set.of, copyOf returns an unmodifiable collection as it is.
 * Needs a runtime whose core-oj has java.util.ImmutableCollections. Run like PersistableBundleTest
 * with the class java.util.ImmutableCollectionsTest.
 */
package java.util;

import junit.framework.TestCase;

public class ImmutableCollectionsTest extends TestCase {

	private static void unsupported(Runnable r) {
		try {
			r.run();
			fail("UnsupportedOperationException expected");
		} catch (UnsupportedOperationException e) {
		}
	}

	public void testListOf() {
		assertTrue(List.of().isEmpty());
		List<String> l = List.of("a", "b", "c");
		assertEquals(Arrays.asList("a", "b", "c"), l);
		assertEquals(Arrays.asList("a", "b", "c").hashCode(), l.hashCode());
		assertEquals(l, Arrays.asList("a", "b", "c"));
		assertEquals("[a, b, c]", l.toString());
		assertEquals(1, l.indexOf("b"));
		assertEquals(-1, l.indexOf("z"));
		assertTrue(l.contains("c"));
		assertEquals(Arrays.asList("b", "c"), l.subList(1, 3));
		assertEquals(10, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).size());
		assertEquals(11, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).size());
		unsupported(() -> l.add("d"));
		unsupported(() -> l.remove("a"));
		unsupported(() -> l.set(0, "x"));
		unsupported(() -> l.clear());
		unsupported(() -> l.sort(null));
		unsupported(() -> l.iterator().remove());
		unsupported(() -> l.subList(0, 1).add("x"));
		try {
			l.get(3);
			fail();
		} catch (IndexOutOfBoundsException e) {
		}
	}

	public void testListNulls() {
		try {
			List.of("a", null);
			fail();
		} catch (NullPointerException e) {
		}
		try {
			List.of((Object[])null);
			fail();
		} catch (NullPointerException e) {
		}
		try {
			List.of("a").contains(null);
			fail();
		} catch (NullPointerException e) {
		}
		try {
			List.copyOf(Arrays.asList("a", null));
			fail();
		} catch (NullPointerException e) {
		}
	}

	public void testListCopyOf() {
		List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
		List<String> copy = List.copyOf(src);
		src.add("c");
		assertEquals(Arrays.asList("a", "b"), copy);
		assertSame(copy, List.copyOf(copy));
		unsupported(() -> copy.add("x"));
		String[] array = {"x", "y"};
		List<String> fromArray = List.of(array);
		array[0] = "changed";
		assertEquals("x", fromArray.get(0));
	}

	public void testSetOf() {
		Set<Integer> s = Set.of(1, 2, 3);
		assertEquals(3, s.size());
		assertTrue(s.contains(2));
		assertFalse(s.contains(4));
		assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), s);
		assertEquals(s, new HashSet<>(Arrays.asList(3, 2, 1)));
		assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)).hashCode(), s.hashCode());
		assertTrue(Set.of().isEmpty());
		assertEquals(11, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).size());
		unsupported(() -> s.add(9));
		unsupported(() -> s.remove(1));
		unsupported(() -> s.clear());
		unsupported(() -> s.iterator().remove());
		try {
			Set.of(1, 1);
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			Set.of(1, null);
			fail();
		} catch (NullPointerException e) {
		}
		try {
			s.contains(null);
			fail();
		} catch (NullPointerException e) {
		}
	}

	public void testSetCopyOf() {
		Set<String> copy = Set.copyOf(Arrays.asList("a", "b", "a"));
		assertEquals(2, copy.size());
		assertSame(copy, Set.copyOf(copy));
		unsupported(() -> copy.add("x"));
		try {
			Set.copyOf(Arrays.asList("a", null));
			fail();
		} catch (NullPointerException e) {
		}
	}
}
