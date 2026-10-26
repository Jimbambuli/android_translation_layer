/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.util.Pools.SynchronizedPool;
import android.util.SparseArray;

/**
 * Helper for tracking the velocity of touch events, for implementing
 * flinging and other such gestures.
 *
 * The API is AOSP's (frameworks/base core/java/android/view/VelocityTracker.java). The native part,
 * frameworks/native libs/input/VelocityTracker.cpp, is ported to Java for the default strategy of the
 * X and Y axes: LSQ2, an unweighted second degree least squares fit of the last 20 positions within
 * 100 ms, history dropped when the pointer did not move for 40 ms. AXIS_SCROLL (IMPULSE) is not ported.
 */
public final class VelocityTracker {
	private static final SynchronizedPool<VelocityTracker> sPool =
	    new SynchronizedPool<VelocityTracker>(2);

	private static final int ACTIVE_POINTER_ID = -1;

	public static final int VELOCITY_TRACKER_STRATEGY_DEFAULT = -1;
	public static final int VELOCITY_TRACKER_STRATEGY_LSQ2 = 2;

	// VelocityTracker.cpp
	private static final long ASSUME_POINTER_STOPPED_TIME = 40; // ms
	// LeastSquaresVelocityTrackerStrategy (VelocityTracker.h)
	private static final long HORIZON = 100; // ms
	private static final int HISTORY_SIZE = 20;

	/* movements of one pointer on one axis, a circular buffer as in LeastSquaresVelocityTrackerStrategy */
	private static final class Movements {
		final long[] eventTime = new long[HISTORY_SIZE];
		final float[] position = new float[HISTORY_SIZE];
		int index = 0;
		boolean empty = true;
	}

	// per axis (X, Y): pointer id -> movements; dropped as a whole like mConfiguredStrategies
	private final SparseArray<Movements>[] mMovements = new SparseArray[] {new SparseArray<Movements>(), new SparseArray<Movements>()};
	// per axis: pointer id -> velocity of the last computeCurrentVelocity()
	private final SparseArray<Float>[] mComputedVelocity = new SparseArray[] {new SparseArray<Float>(), new SparseArray<Float>()};
	private int mCurrentPointerIdBits;
	private int mActivePointerId = -1;
	private long mLastEventTime;

	/**
	 * Retrieve a new VelocityTracker object to watch the velocity of a
	 * motion.  Be sure to call {@link #recycle} when done.  You should
	 * generally only maintain an active object while tracking a movement,
	 * so that the VelocityTracker can be re-used elsewhere.
	 *
	 * @return Returns a new VelocityTracker.
	 */
	static public VelocityTracker obtain() {
		VelocityTracker instance = sPool.acquire();
		return (instance != null) ? instance : new VelocityTracker();
	}

	public static VelocityTracker obtain(String strategy) {
		return obtain();
	}

	public static VelocityTracker obtain(int strategy) {
		return new VelocityTracker();
	}

	/**
	 * Return a VelocityTracker object back to be re-used by others.  You must
	 * not touch the object after calling this function.
	 */
	public void recycle() {
		clear();
		sPool.release(this);
	}

	public int getStrategyId() {
		return VELOCITY_TRACKER_STRATEGY_DEFAULT;
	}

	private VelocityTracker() {}

	public boolean isAxisSupported(int axis) {
		return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y;
	}

	/**
	 * Reset the velocity tracker back to its initial state.
	 */
	public void clear() {
		mCurrentPointerIdBits = 0;
		mActivePointerId = -1;
		clearMovements();
	}

	private void clearMovements() {
		mMovements[0].clear();
		mMovements[1].clear();
	}

	private void clearPointer(int pointerId) {
		mCurrentPointerIdBits &= ~(1 << pointerId);
		if (mActivePointerId == pointerId) {
			// The active pointer id is being removed. Mark it invalid and try to find a new one
			// from the remaining pointers.
			mActivePointerId = mCurrentPointerIdBits != 0 ? Integer.numberOfTrailingZeros(mCurrentPointerIdBits) : -1;
		}
		mMovements[0].remove(pointerId);
		mMovements[1].remove(pointerId);
	}

	private void addMovement(long eventTime, int pointerId, int axis, float position) {
		if ((mCurrentPointerIdBits & (1 << pointerId)) != 0 && eventTime - mLastEventTime > ASSUME_POINTER_STOPPED_TIME) {
			// We have not received any movements for too long.  Assume that all pointers
			// have stopped.
			clearMovements();
		}
		mLastEventTime = eventTime;

		mCurrentPointerIdBits |= 1 << pointerId;
		if (mActivePointerId == -1) {
			// Let this be the new active pointer if no active pointer is currently set
			mActivePointerId = pointerId;
		}

		Movements movements = mMovements[axis].get(pointerId);
		if (movements == null) {
			movements = new Movements();
			mMovements[axis].put(pointerId, movements);
		}
		if (!movements.empty && movements.eventTime[movements.index] != eventTime) {
			// When ACTION_POINTER_DOWN happens, we will first receive ACTION_MOVE with the coordinates
			// of the existing pointers, and then ACTION_POINTER_DOWN with the coordinates that include
			// the new pointer. If the eventtimes for both events are identical, just update the data
			// for this time.
			movements.index++;
		}
		if (movements.index == HISTORY_SIZE)
			movements.index = 0;
		movements.empty = false;
		movements.eventTime[movements.index] = eventTime;
		movements.position[movements.index] = position;
	}

	/**
	 * Add a user's movement to the tracker.  You should call this for the
	 * initial {@link MotionEvent#ACTION_DOWN}, the following
	 * {@link MotionEvent#ACTION_MOVE} events that you receive, and the
	 * final {@link MotionEvent#ACTION_UP}.  You can, however, call this
	 * for whichever events you desire.
	 *
	 * @param event The MotionEvent you received and would like to track.
	 */
	public void addMovement(MotionEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_HOVER_ENTER:
				// Clear all pointers on down before adding the new movement.
				clear();
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				// Start a new movement trace for a pointer that just went down.
				// We do this on down instead of on up because the client may want to query the
				// final velocity for a pointer that just went up.
				clearPointer(event.getPointerId(event.getActionIndex()));
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_HOVER_MOVE:
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
				if (event.getEventTime() - mLastEventTime > ASSUME_POINTER_STOPPED_TIME) {
					// We have not received any movements for too long.  Assume that all pointers
					// have stopped.
					clearMovements();
				}
				// These actions because they do not convey any new information about
				// pointer movement.  We also want to preserve the last known velocity of the pointers.
				return;
			default:
				// Ignore all other actions.
				return;
		}

		long eventTime = event.getEventTime();
		for (int i = 0; i < event.getPointerCount(); i++) {
			int pointerId = event.getPointerId(i);
			addMovement(eventTime, pointerId, MotionEvent.AXIS_X, event.getX(i));
			addMovement(eventTime, pointerId, MotionEvent.AXIS_Y, event.getY(i));
		}
	}

	/* LeastSquaresVelocityTrackerStrategy::getEstimator() for degree 2 without weighting: coeff[1], or null */
	private static Float getVelocity(Movements movements) {
		if (movements == null || movements.empty)
			return null;
		// Iterate over movement samples in reverse time order and collect samples.
		float[] time = new float[HISTORY_SIZE];
		float[] position = new float[HISTORY_SIZE];
		int m = 0;
		int index = movements.index;
		long newest = movements.eventTime[index];
		do {
			long age = newest - movements.eventTime[index];
			if (age > HORIZON)
				break;
			if (movements.eventTime[index] == 0 && index != 0)
				break;
			position[m] = movements.position[index];
			time[m] = -age * 0.001f;
			m++;
			index = (index == 0 ? HISTORY_SIZE : index) - 1;
		} while (m < HISTORY_SIZE);

		if (m >= 3)
			return solveUnweightedLeastSquaresDeg2(time, position, m);
		if (m == 2) // degree reduced to m - 1: the straight line through both
			return time[0] != time[1] ? (position[0] - position[1]) / (time[0] - time[1]) : null;
		return null; // degree 0, no velocity
	}

	/* solveUnweightedLeastSquaresDeg2(): y = a*x^2 + b*x + c, returns b */
	private static Float solveUnweightedLeastSquaresDeg2(float[] x, float[] y, int count) {
		float sxi = 0, sxiyi = 0, syi = 0, sxi2 = 0, sxi3 = 0, sxi2yi = 0, sxi4 = 0;
		for (int i = 0; i < count; i++) {
			float xi = x[i];
			float yi = y[i];
			float xi2 = xi * xi;
			float xi3 = xi2 * xi;
			float xi4 = xi3 * xi;
			float xiyi = xi * yi;
			float xi2yi = xi2 * yi;

			sxi += xi;
			sxi2 += xi2;
			sxiyi += xiyi;
			sxi2yi += xi2yi;
			syi += yi;
			sxi3 += xi3;
			sxi4 += xi4;
		}

		float Sxx = sxi2 - sxi * sxi / count;
		float Sxy = sxiyi - sxi * syi / count;
		float Sxx2 = sxi3 - sxi * sxi2 / count;
		float Sx2y = sxi2yi - sxi2 * syi / count;
		float Sx2x2 = sxi4 - sxi2 * sxi2 / count;

		float denominator = Sxx * Sx2x2 - Sxx2 * Sxx2;
		if (denominator == 0)
			return null;
		return (Sxy * Sx2x2 - Sx2y * Sxx2) / denominator;
	}

	/**
	 * Equivalent to invoking {@link #computeCurrentVelocity(int, float)} with a maximum
	 * velocity of Float.MAX_VALUE.
	 *
	 * @see #computeCurrentVelocity(int, float)
	 */
	public void computeCurrentVelocity(int units) {
		computeCurrentVelocity(units, Float.MAX_VALUE);
	}

	/**
	 * Compute the current velocity based on the points that have been
	 * collected.  Only call this when you actually want to retrieve velocity
	 * information, as it is relatively expensive.  You can then retrieve
	 * the velocity with {@link #getXVelocity()} and
	 * {@link #getYVelocity()}.
	 *
	 * @param units The units you would like the velocity in.  A value of 1
	 * provides pixels per millisecond, 1000 provides pixels per second, etc.
	 * @param maxVelocity The maximum velocity that can be computed by this method.
	 * This value must be declared in the same unit as the units parameter. This value
	 * must be positive.
	 */
	public void computeCurrentVelocity(int units, float maxVelocity) {
		for (int axis = 0; axis < 2; axis++) {
			mComputedVelocity[axis].clear();
			for (int bits = mCurrentPointerIdBits; bits != 0; bits &= bits - 1) {
				int id = Integer.numberOfTrailingZeros(bits);
				Float velocity = getVelocity(mMovements[axis].get(id));
				if (velocity != null)
					mComputedVelocity[axis].put(id, Math.max(-maxVelocity, Math.min(velocity * units / 1000, maxVelocity)));
			}
		}
	}

	/**
	 * Retrieve the last computed X velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @return The previously computed X velocity.
	 */
	public float getXVelocity() {
		return getXVelocity(ACTIVE_POINTER_ID);
	}

	/**
	 * Retrieve the last computed Y velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @return The previously computed Y velocity.
	 */
	public float getYVelocity() {
		return getYVelocity(ACTIVE_POINTER_ID);
	}

	/**
	 * Retrieve the last computed X velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @param id Which pointer's velocity to return.
	 * @return The previously computed X velocity.
	 */
	public float getXVelocity(int id) {
		return getAxisVelocity(MotionEvent.AXIS_X, id);
	}

	/**
	 * Retrieve the last computed Y velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @param id Which pointer's velocity to return.
	 * @return The previously computed Y velocity.
	 */
	public float getYVelocity(int id) {
		return getAxisVelocity(MotionEvent.AXIS_Y, id);
	}

	/* android_view_VelocityTracker.cpp: VelocityTrackerState::getVelocity() */
	public float getAxisVelocity(int axis, int id) {
		if (!isAxisSupported(axis))
			return 0;
		if (id == ACTIVE_POINTER_ID)
			id = mActivePointerId;
		Float velocity = mComputedVelocity[axis].get(id);
		return velocity != null ? velocity : 0;
	}

	public float getAxisVelocity(int axis) {
		return getAxisVelocity(axis, ACTIVE_POINTER_ID);
	}
}
