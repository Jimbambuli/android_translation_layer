package android.graphics;

/* AOSP graphics/java/android/graphics/RegionIterator.java: the rectangles of a region, in the order of
 * SkRegion::Iterator (top to bottom, left to right). The native iterator is a private copy of the region; here the
 * rectangles of the region are copied when the iterator is made, which is the same. */
public class RegionIterator {

	private final int[] rects;
	private int index;

	public RegionIterator(Region region) {
		rects = Region.nativeGetBoundaryPathRects(region.region);
	}

	/* AOSP: false and r unchanged when there are no more rectangles */
	public final boolean next(Rect r) {
		if (r == null) {
			throw new NullPointerException("The Rect must be provided");
		}
		if (index >= rects.length) {
			return false;
		}
		r.set(rects[index], rects[index + 1], rects[index + 2], rects[index + 3]);
		index += 4;
		return true;
	}
}
