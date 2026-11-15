package android.graphics;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * AOSP android.graphics.PathIterator: iterates over the segments of a Path. AOSP walks the SkPath natively;
 * ATL takes the segments of the GskPath once (Path.native_get_segments(), 9 floats per segment: the verb and
 * the 8 point values as Skia's iterator has them) and walks those, with the same caching of hasNext() and
 * peek() as AOSP. A changed path is noticed by the Path having a different GskPath.
 */
public class PathIterator implements Iterator<PathIterator.Segment> {
	// these must match the values in SkPath.h
	public static final int VERB_MOVE = 0;
	public static final int VERB_LINE = 1;
	public static final int VERB_QUAD = 2;
	public static final int VERB_CONIC = 3;
	public static final int VERB_CUBIC = 4;
	public static final int VERB_CLOSE = 5;
	public static final int VERB_DONE = 6;

	private static final int POINT_ARRAY_SIZE = 8;
	private static final int SEGMENT_SIZE = 1 + POINT_ARRAY_SIZE;

	private final float[] mPointsArray = new float[POINT_ARRAY_SIZE];
	private int mCachedVerb = -1;
	private boolean mDone = false;
	private final Path mPath;
	private final long mGskPath;
	private final float[] mSegments;
	private int mNativeIndex; // the next segment, like the position of AOSP's native iterator

	PathIterator(Path path) {
		mPath = path;
		mGskPath = path.getGskPath();
		mSegments = Path.native_get_segments(mGskPath);
	}

	public int next(float[] points, int offset) {
		if (points.length < offset + POINT_ARRAY_SIZE) {
			throw new ArrayIndexOutOfBoundsException("points array must be able to "
			                                         + "hold at least 8 entries");
		}
		int returnVerb = getReturnVerb(mCachedVerb);
		mCachedVerb = -1;
		System.arraycopy(mPointsArray, 0, points, offset, POINT_ARRAY_SIZE);
		return returnVerb;
	}

	@Override
	public boolean hasNext() {
		if (mCachedVerb == -1) {
			mCachedVerb = nextInternal();
		}
		return mCachedVerb != VERB_DONE;
	}

	public int peek() {
		if (isModified()) {
			throw new ConcurrentModificationException(
			    "Iterator cannot be used on modified Path");
		}
		if (mDone) {
			return VERB_DONE;
		}
		return mNativeIndex * SEGMENT_SIZE < mSegments.length ? (int)mSegments[mNativeIndex * SEGMENT_SIZE] : VERB_DONE;
	}

	private boolean isModified() {
		return mPath.getGskPath() != mGskPath;
	}

	private int nextInternal() {
		if (mDone) {
			return VERB_DONE;
		}
		if (isModified()) {
			throw new ConcurrentModificationException(
			    "Iterator cannot be used on modified Path");
		}
		int verb;
		int i = mNativeIndex * SEGMENT_SIZE;
		if (i < mSegments.length) {
			verb = (int)mSegments[i];
			System.arraycopy(mSegments, i + 1, mPointsArray, 0, POINT_ARRAY_SIZE);
			mNativeIndex++;
		} else {
			verb = VERB_DONE;
		}
		if (verb == VERB_DONE) {
			mDone = true;
		}
		return verb;
	}

	@Override
	public Segment next() {
		int returnVerb = getReturnVerb(mCachedVerb);
		mCachedVerb = -1;
		float conicWeight = 0f;
		if (returnVerb == VERB_CONIC) {
			conicWeight = mPointsArray[6];
		}
		float[] returnPoints = new float[8];
		System.arraycopy(mPointsArray, 0, returnPoints, 0, POINT_ARRAY_SIZE);
		return new Segment(returnVerb, returnPoints, conicWeight);
	}

	private int getReturnVerb(int cachedVerb) {
		switch (cachedVerb) {
			case VERB_MOVE:
				return VERB_MOVE;
			case VERB_LINE:
				return VERB_LINE;
			case VERB_QUAD:
				return VERB_QUAD;
			case VERB_CONIC:
				return VERB_CONIC;
			case VERB_CUBIC:
				return VERB_CUBIC;
			case VERB_CLOSE:
				return VERB_CLOSE;
			case VERB_DONE:
				return VERB_DONE;
		}
		return nextInternal();
	}

	public static class Segment {
		private final int mVerb;
		private final float[] mPoints;
		private final float mConicWeight;

		public int getVerb() {
			return mVerb;
		}

		public float[] getPoints() {
			return mPoints;
		}

		public float getConicWeight() {
			return mConicWeight;
		}

		Segment(int verb, float[] points, float conicWeight) {
			mVerb = verb;
			mPoints = points;
			mConicWeight = conicWeight;
		}
	}
}
