package android.graphics;

/**
 * Measures the contours of a Path (position, tangent, length, segments), as AOSP's PathMeasure does.
 * AOSP's measuring is Skia's SkContourMeasureIter; here a native step splits the path into one GskPath
 * per contour and GskPathMeasure measures the current one.
 */
public class PathMeasure {

	private long[] mContours = new long[0]; // GskPath * of each contour
	private int mIndex = -1; // the current contour
	private long mMeasure; // GskPathMeasure * of the current contour
	private float mLength;

	/**
	 * Create an empty PathMeasure object. To uses this to measure the length
	 * of a path, and/or to find the position and tangent along it, call
	 * setPath.
	 *
	 * Note that once a path is associated with the measure object, it is
	 * undefined if the path is subsequently modified and the the measure object
	 * is used. If the path is modified, you must call setPath with the path.
	 */
	public PathMeasure() {}

	/**
	 * Create a PathMeasure object associated with the specified path object
	 * (already created and specified). The measure object can now return the
	 * path's length, and the position and tangent of any position along the
	 * path.
	 *
	 * Note that once a path is associated with the measure object, it is
	 * undefined if the path is subsequently modified and the the measure object
	 * is used. If the path is modified, you must call setPath with the path.
	 *
	 * @param path The path that will be measured by this object
	 * @param forceClosed If true, then the path will be considered as "closed"
	 *        even if its contour doesn't end in a close().
	 */
	public PathMeasure(Path path, boolean forceClosed) {
		setPath(path, forceClosed);
	}

	/**
	 * Assign a new path, or null to have none.
	 */
	public void setPath(Path path, boolean forceClosed) {
		release();
		if (path != null) {
			mContours = native_split(path.getGskPath(), forceClosed);
			select(0);
		}
	}

	/**
	 * Return the total length of the current contour, or 0 if no path is
	 * associated with this measure object.
	 */
	public float getLength() {
		return mLength;
	}

	/**
	 * Pins distance to 0 <= distance <= getLength(), and then computes the
	 * corresponding position and tangent. Returns false if there is no path,
	 * or a zero-length path was specified, in which case position and tangent
	 * are unchanged.
	 *
	 * @param distance The distance along the current contour to sample
	 * @param pos If not null, returns the sampled position (x==[0], y==[1])
	 * @param tan If not null, returns the sampled tangent (x==[0], y==[1])
	 * @return false if there was no path associated with this measure object
	*/
	public boolean getPosTan(float distance, float[] pos, float[] tan) {
		if (pos != null && pos.length < 2 || tan != null && tan.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (mMeasure == 0 || !(mLength > 0))
			return false;
		distance = Math.max(0, Math.min(distance, mLength));
		return native_pos_tan(mContours[mIndex], mMeasure, distance, pos, tan);
	}

	/**
	 * Given a start and stop distance, return in dst the intervening
	 * segment(s). If the segment is zero-length, return false, else return
	 * true. startD and stopD are pinned to legal values (0..getLength()).
	 * If startD > stopD then return false (and leave dst untouched).
	 * Begin the segment with a moveTo if startWithMoveTo is true.
	 *
	 * <p>On {@link android.os.Build.VERSION_CODES#KITKAT} and earlier
	 * releases, the resulting path may not display on a hardware-accelerated
	 * Canvas. A simple workaround is to add a single operation to this path,
	 * such as <code>dst.rLineTo(0, 0)</code>.</p>
	 */
	public boolean getSegment(float startD, float stopD, Path dst, boolean startWithMoveTo) {
		if (mMeasure == 0)
			return false;
		startD = Math.max(0, startD);
		stopD = Math.min(stopD, mLength);
		if (!(startD < stopD))
			return false;
		return native_segment(mContours[mIndex], mMeasure, startD, stopD, dst.getBuilder(), startWithMoveTo);
	}

	/**
	 * Return true if the current contour is closed()
	 */
	public boolean isClosed() {
		return mMeasure != 0 && native_is_closed(mContours[mIndex]);
	}

	/**
	 * Move to the next contour in the path. Return true if one exists, or
	 * false if we're done with the path.
	 */
	public boolean nextContour() {
		if (mIndex + 1 >= mContours.length)
			return false;
		select(mIndex + 1);
		return true;
	}

	private void select(int index) {
		if (mMeasure != 0)
			native_measure_free(mMeasure);
		mMeasure = 0;
		mLength = 0;
		mIndex = index;
		if (index < mContours.length) {
			mMeasure = native_measure_new(mContours[index]);
			mLength = native_length(mMeasure);
		}
	}

	private void release() {
		if (mMeasure != 0)
			native_measure_free(mMeasure);
		mMeasure = 0;
		for (long contour : mContours)
			native_free_path(contour);
		mContours = new long[0];
		mIndex = -1;
		mLength = 0;
	}

	@SuppressWarnings("removal")
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	private static native long[] native_split(long path, boolean forceClosed);
	private static native void native_free_path(long path);
	private static native long native_measure_new(long path);
	private static native void native_measure_free(long measure);
	private static native float native_length(long measure);
	private static native boolean native_is_closed(long path);
	private static native boolean native_pos_tan(long path, long measure, float distance, float[] pos, float[] tan);
	private static native boolean native_segment(long path, long measure, float start, float end, long dstBuilder, boolean startWithMoveTo);
}
