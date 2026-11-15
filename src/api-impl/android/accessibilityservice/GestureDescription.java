/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.accessibilityservice;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.Display;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessibility services with the
 * {@link android.R.styleable#AccessibilityService_canPerformGestures} property can dispatch
 * gestures. This class describes those gestures. Gestures are made up of one or more strokes.
 * Gestures are immutable once built and will be dispatched to the specified display.
 * <p>
 * Spatial dimensions throughout are in screen pixels. Time is measured in milliseconds.
 */
public final class GestureDescription {
    /** Gestures may contain no more than this many strokes */
    private static final int MAX_STROKE_COUNT = 20;

    /**
     * Upper bound on total gesture duration. Nearly all gestures will be much shorter.
     */
    private static final long MAX_GESTURE_DURATION_MS = 60 * 1000;

    private final List<StrokeDescription> mStrokes = new ArrayList<>();
    private final float[] mTempPos = new float[2];
    private final int mDisplayId;

    /**
     * Get the upper limit for the number of strokes a gesture may contain.
     *
     * @return The maximum number of strokes.
     */
    public static int getMaxStrokeCount() {
        return MAX_STROKE_COUNT;
    }

    /**
     * Get the upper limit on a gesture's duration.
     *
     * @return The maximum duration in milliseconds.
     */
    public static long getMaxGestureDuration() {
        return MAX_GESTURE_DURATION_MS;
    }

    private GestureDescription() {
       this(new ArrayList<>());
    }

    private GestureDescription(List<StrokeDescription> strokes) {
        this(strokes, Display.DEFAULT_DISPLAY);
    }

    private GestureDescription(List<StrokeDescription> strokes, int displayId) {
        mStrokes.addAll(strokes);
        mDisplayId = displayId;
    }

    /**
     * Get the number of stroke in the gesture.
     *
     * @return the number of strokes in this gesture
     */
    public int getStrokeCount() {
        return mStrokes.size();
    }

    /**
     * Read a stroke from the gesture
     *
     * @param index the index of the stroke
     *
     * @return A description of the stroke.
     */
    public StrokeDescription getStroke(@IntRange(from = 0) int index) {
        return mStrokes.get(index);
    }

    /**
     * Returns the ID of the display this gesture is sent on, for use with
     * {@link android.hardware.display.DisplayManager#getDisplay(int)}.
     *
     * @return The logical display id.
     */
    public int getDisplayId() {
        return mDisplayId;
    }

    /**
     * Return the smallest key point (where a path starts or ends) that is at least a specified
     * offset
     * @param offset the minimum start time
     * @return The next key time that is at least the offset or -1 if one can't be found
     */
    private long getNextKeyPointAtLeast(long offset) {
        long nextKeyPoint = Long.MAX_VALUE;
        for (int i = 0; i < mStrokes.size(); i++) {
            long thisStartTime = mStrokes.get(i).mStartTime;
            if ((thisStartTime < nextKeyPoint) && (thisStartTime >= offset)) {
                nextKeyPoint = thisStartTime;
            }
            long thisEndTime = mStrokes.get(i).mEndTime;
            if ((thisEndTime < nextKeyPoint) && (thisEndTime >= offset)) {
                nextKeyPoint = thisEndTime;
            }
        }
        return (nextKeyPoint == Long.MAX_VALUE) ? -1L : nextKeyPoint;
    }

    // Total duration assumes that the gesture starts at 0; waiting around to start a gesture
    // counts against total duration
    private static long getTotalDuration(List<StrokeDescription> paths) {
        long latestEnd = Long.MIN_VALUE;
        for (int i = 0; i < paths.size(); i++) {
            StrokeDescription path = paths.get(i);
            latestEnd = Math.max(latestEnd, path.mEndTime);
        }
        return Math.max(latestEnd, 0);
    }

    /**
     * Builder for a {@code GestureDescription}
     */
    public static class Builder {

        private final List<StrokeDescription> mStrokes = new ArrayList<>();
        private int mDisplayId = Display.DEFAULT_DISPLAY;

        /**
         * Adds a stroke to the gesture description. Up to
         * {@link GestureDescription#getMaxStrokeCount()} paths may be
         * added to a gesture, and the total gesture duration (earliest path start time to latest
         * path end time) may not exceed {@link GestureDescription#getMaxGestureDuration()}.
         *
         * @param strokeDescription the stroke to add.
         *
         * @return this
         */
        public Builder addStroke(@NonNull StrokeDescription strokeDescription) {
            if (mStrokes.size() >= MAX_STROKE_COUNT) {
                throw new IllegalStateException(
                        "Attempting to add too many strokes to a gesture. Maximum is "
                                + MAX_STROKE_COUNT
                                + ", got "
                                + mStrokes.size());
            }

            mStrokes.add(strokeDescription);

            if (getTotalDuration(mStrokes) > MAX_GESTURE_DURATION_MS) {
                mStrokes.remove(strokeDescription);
                throw new IllegalStateException(
                        "Gesture would exceed maximum duration with new stroke");
            }
            return this;
        }

        /**
         * Sets the id of the display to dispatch gestures.
         *
         * @param displayId The logical display id
         *
         * @return this
         */
        public @NonNull Builder setDisplayId(int displayId) {
            mDisplayId = displayId;
            return this;
        }

        public GestureDescription build() {
            if (mStrokes.size() == 0) {
                throw new IllegalStateException("Gestures must have at least one stroke");
            }
            return new GestureDescription(mStrokes, mDisplayId);
        }
    }

    /**
     * Immutable description of stroke that can be part of a gesture.
     */
    public static class StrokeDescription {
        private static final int INVALID_STROKE_ID = -1;

        static int sIdCounter;

        Path mPath;
        long mStartTime;
        long mEndTime;
        private float mTimeToLengthConversion;
        private PathMeasure mPathMeasure;
        // The tap location is only set for zero-length paths
        float[] mTapLocation;
        int mId;
        boolean mContinued;
        int mContinuedStrokeId = INVALID_STROKE_ID;

        /**
         * @param path The path to follow. Must have exactly one contour. The bounds of the path
         * must not be negative. The path must not be empty. If the path has zero length
         * (for example, a single {@code moveTo()}), the stroke is a touch that doesn't move.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         * time the stroke should start. Must not be negative.
         * @param duration The duration, in milliseconds, the stroke takes to traverse the path.
         * Must be positive.
         */
        public StrokeDescription(@NonNull Path path,
                @IntRange(from = 0) long startTime,
                @IntRange(from = 0) long duration) {
            this(path, startTime, duration, false);
        }

        /**
         * @param path The path to follow. Must have exactly one contour. The bounds of the path
         * must not be negative. The path must not be empty. If the path has zero length
         * (for example, a single {@code moveTo()}), the stroke is a touch that doesn't move.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         * time the stroke should start. Must not be negative.
         * @param duration The duration, in milliseconds, the stroke takes to traverse the path.
         * Must be positive.
         * @param willContinue {@code true} if this stroke will be continued by one in the
         * next gesture {@code false} otherwise. Continued strokes keep their pointers down when
         * the gesture completes.
         */
        public StrokeDescription(@NonNull Path path,
                @IntRange(from = 0) long startTime,
                @IntRange(from = 0) long duration,
                boolean willContinue) {
            mContinued = willContinue;
            Preconditions.checkArgument(duration > 0, "Duration must be positive");
            Preconditions.checkArgument(startTime >= 0, "Start time must not be negative");
            Preconditions.checkArgument(!path.isEmpty(), "Path is empty");
            RectF bounds = new RectF();
            path.computeBounds(bounds, false /* unused */);
            Preconditions.checkArgument((bounds.bottom >= 0) && (bounds.top >= 0)
                    && (bounds.right >= 0) && (bounds.left >= 0),
                    "Path bounds must not be negative");
            mPath = new Path(path);
            mPathMeasure = new PathMeasure(path, false);
            if (mPathMeasure.getLength() == 0) {
                // Treat zero-length paths as taps
                Path tempPath = new Path(path);
                tempPath.lineTo(-1, -1);
                mTapLocation = new float[2];
                PathMeasure pathMeasure = new PathMeasure(tempPath, false);
                pathMeasure.getPosTan(0, mTapLocation, null);
            }
            if (mPathMeasure.nextContour()) {
                throw new IllegalArgumentException("Path has more than one contour");
            }
            /*
             * Calling nextContour has moved mPathMeasure off the first contour, which is the only
             * one we care about. Set the path again to go back to the first contour.
             */
            mPathMeasure.setPath(mPath, false);
            mStartTime = startTime;
            mEndTime = startTime + duration;
            mTimeToLengthConversion = getLength() / duration;
            mId = sIdCounter++;
        }

        /**
         * Retrieve a copy of the path for this stroke
         *
         * @return A copy of the path
         */
        public Path getPath() {
            return new Path(mPath);
        }

        /**
         * Get the stroke's start time
         *
         * @return the start time for this stroke.
         */
        public long getStartTime() {
            return mStartTime;
        }

        /**
         * Get the stroke's duration
         *
         * @return the duration for this stroke
         */
        public long getDuration() {
            return mEndTime - mStartTime;
        }

        /**
         * Get the stroke's ID. The ID is used when a stroke is to be continued by another
         * stroke in a future gesture.
         *
         * @return the ID of this stroke
         * @hide
         */
        public int getId() {
            return mId;
        }

        /**
         * Create a new stroke that will continue this one. This is only possible if this stroke
         * will continue.
         *
         * @param path The path for the stroke that continues this one. The starting point of
         *             this path must match the ending point of the stroke it continues.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         *                  time this stroke should start. Must not be negative. This time is from
         *                  the start of the new gesture, not the one being continued.
         * @param duration The duration for the new stroke. Must not be negative.
         * @param willContinue {@code true} if this stroke will be continued by one in the
         *             next gesture {@code false} otherwise.
         * @return
         */
        public StrokeDescription continueStroke(Path path, long startTime, long duration,
                boolean willContinue) {
            if (!mContinued) {
                throw new IllegalStateException(
                        "Only strokes marked willContinue can be continued");
            }
            StrokeDescription strokeDescription =
                    new StrokeDescription(path, startTime, duration, willContinue);
            strokeDescription.mContinuedStrokeId = mId;
            return strokeDescription;
        }

        /**
         * Check if this stroke is marked to continue in the next gesture.
         *
         * @return {@code true} if the stroke is to be continued.
         */
        public boolean willContinue() {
            return mContinued;
        }

        /**
         * Get the ID of the stroke that this one will continue.
         *
         * @return The ID of the stroke that this stroke continues, or 0 if no such stroke exists.
         * @hide
         */
        public int getContinuedStrokeId() {
            return mContinuedStrokeId;
        }

        float getLength() {
            return mPathMeasure.getLength();
        }

        /* Assumes hasPointForTime returns true */
        boolean getPosForTime(long time, float[] pos) {
            if (mTapLocation != null) {
                pos[0] = mTapLocation[0];
                pos[1] = mTapLocation[1];
                return true;
            }
            if (time == mEndTime) {
                // Close to the end time, roundoff can be a problem
                return mPathMeasure.getPosTan(getLength(), pos, null);
            }
            float length = mTimeToLengthConversion * ((float) (time - mStartTime));
            return mPathMeasure.getPosTan(length, pos, null);
        }

        boolean hasPointForTime(long time) {
            return ((time >= mStartTime) && (time <= mEndTime));
        }
    }
}
