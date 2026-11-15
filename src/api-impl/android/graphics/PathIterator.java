package android.graphics;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/* AOSP android.graphics.PathIterator. The native iterator (SkPath::RawIter) is replaced by the segments of the
 * GskPath taken when the iterator is created (Path.getSegments(): verb, then the points of the segment, a conic's
 * weight last); next(float[], int) writes the points into the array as Skia's iterator does: a move has its point,
 * a line its start and end, and so on. A change of the Path after creation makes the iterator throw
 * ConcurrentModificationException like AOSP's generation check. */
public class PathIterator implements Iterator<PathIterator.Segment> {

	private final float[] mPointsArray = new float[POINT_ARRAY_SIZE];
	private int mCachedVerb = -1;
	private boolean mDone = false;
	private final Path mPath;
	private final int mPathGenerationId;
	private final float[] mSegments;
	private int mPosition = 0;
	private static final int POINT_ARRAY_SIZE = 8;

	public static final int VERB_MOVE = 0;
	public static final int VERB_LINE = 1;
	public static final int VERB_QUAD = 2;
	public static final int VERB_CONIC = 3;
	public static final int VERB_CUBIC = 4;
	public static final int VERB_CLOSE = 5;
	public static final int VERB_DONE = 6;

	PathIterator(Path path) {
		mPath = path;
		mSegments = path.getSegments();
		mPathGenerationId = mPath.getGenerationId();
	}

	public int next(float[] points, int offset) {
		if (points.length < offset + POINT_ARRAY_SIZE) {
			throw new ArrayIndexOutOfBoundsException("points array must be able to hold at least 8 entries");
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
		if (mPathGenerationId != mPath.getGenerationId()) {
			throw new ConcurrentModificationException("Iterator cannot be used on modified Path");
		}
		if (mDone || mPosition >= mSegments.length) {
			return VERB_DONE;
		}
		return (int)mSegments[mPosition];
	}

	private int nextInternal() {
		if (mDone) {
			return VERB_DONE;
		}
		if (mPathGenerationId != mPath.getGenerationId()) {
			throw new ConcurrentModificationException("Iterator cannot be used on modified Path");
		}
		int verb = nativeNext();
		if (verb == VERB_DONE) {
			mDone = true;
		}
		return verb;
	}

	/* the iterator's native next(): the verb and its points into mPointsArray */
	private int nativeNext() {
		if (mPosition >= mSegments.length)
			return VERB_DONE;
		int verb = (int)mSegments[mPosition++];
		java.util.Arrays.fill(mPointsArray, 0);
		int count;
		switch (verb) {
		case VERB_MOVE: count = 1; break;
		case VERB_LINE: count = 2; break;
		case VERB_QUAD: count = 3; break;
		case VERB_CONIC: count = 3; break;
		case VERB_CUBIC: count = 4; break;
		default: count = 0; break;
		}
		System.arraycopy(mSegments, mPosition, mPointsArray, 0, count * 2);
		mPosition += count * 2;
		if (verb == VERB_CONIC)
			mPointsArray[6] = mSegments[mPosition++];
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
		case VERB_MOVE: return VERB_MOVE;
		case VERB_LINE: return VERB_LINE;
		case VERB_QUAD: return VERB_QUAD;
		case VERB_CONIC: return VERB_CONIC;
		case VERB_CUBIC: return VERB_CUBIC;
		case VERB_CLOSE: return VERB_CLOSE;
		case VERB_DONE: return VERB_DONE;
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
