/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.view.Surface.ROTATION_0;

import android.annotation.Nullable;
import android.graphics.Path;

import android.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * A class representing the shape of a display. It provides a {@link Path} of the display shape of
 * the display shape.
 *
 * {@link DisplayShape} is immutable.
 */
public final class DisplayShape {

    /** @hide */
    public static final DisplayShape NONE = new DisplayShape("" /* displayShapeSpec */,
            0 /* displayWidth */, 0 /* displayHeight */, 0 /* physicalPixelDisplaySizeRatio */,
            0 /* rotation */);

    /** @hide */
    @VisibleForTesting
    public final String mDisplayShapeSpec;
    private final float mPhysicalPixelDisplaySizeRatio;
    private final int mDisplayWidth;
    private final int mDisplayHeight;
    private final int mRotation;
    private final int mOffsetX;
    private final int mOffsetY;
    private final float mScale;

    private DisplayShape(@NonNull String displayShapeSpec, int displayWidth, int displayHeight,
            float physicalPixelDisplaySizeRatio, int rotation) {
        this(displayShapeSpec, displayWidth, displayHeight, physicalPixelDisplaySizeRatio,
                rotation, 0, 0, 1f);
    }

    private DisplayShape(@NonNull String displayShapeSpec, int displayWidth, int displayHeight,
            float physicalPixelDisplaySizeRatio, int rotation, int offsetX, int offsetY,
            float scale) {
        mDisplayShapeSpec = displayShapeSpec;
        mDisplayWidth = displayWidth;
        mDisplayHeight = displayHeight;
        mPhysicalPixelDisplaySizeRatio = physicalPixelDisplaySizeRatio;
        mRotation = rotation;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mScale = scale;
    }

    /**
     * @hide
     */
    public DisplayShape setRotation(int rotation) {
        return new DisplayShape(mDisplayShapeSpec, mDisplayWidth, mDisplayHeight,
                mPhysicalPixelDisplaySizeRatio, rotation, mOffsetX, mOffsetY, mScale);
    }

    /**
     * @hide
     */
    public DisplayShape setOffset(int offsetX, int offsetY) {
        return new DisplayShape(mDisplayShapeSpec, mDisplayWidth, mDisplayHeight,
                mPhysicalPixelDisplaySizeRatio, mRotation, offsetX, offsetY, mScale);
    }

    /**
     * @hide
     */
    public DisplayShape setScale(float scale) {
        return new DisplayShape(mDisplayShapeSpec, mDisplayWidth, mDisplayHeight,
                mPhysicalPixelDisplaySizeRatio, mRotation, mOffsetX, mOffsetY, scale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDisplayShapeSpec, mDisplayWidth, mDisplayHeight,
                mPhysicalPixelDisplaySizeRatio, mRotation, mOffsetX, mOffsetY, mScale);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DisplayShape) {
            DisplayShape ds = (DisplayShape) o;
            return Objects.equals(mDisplayShapeSpec, ds.mDisplayShapeSpec)
                    && mDisplayWidth == ds.mDisplayWidth && mDisplayHeight == ds.mDisplayHeight
                    && mPhysicalPixelDisplaySizeRatio == ds.mPhysicalPixelDisplaySizeRatio
                    && mRotation == ds.mRotation && mOffsetX == ds.mOffsetX
                    && mOffsetY == ds.mOffsetY && mScale == ds.mScale;
        }
        return false;
    }

    @Override
    public String toString() {
        return "DisplayShape{"
                + " spec=" + mDisplayShapeSpec.hashCode()
                + " displayWidth=" + mDisplayWidth
                + " displayHeight=" + mDisplayHeight
                + " physicalPixelDisplaySizeRatio=" + mPhysicalPixelDisplaySizeRatio
                + " rotation=" + mRotation
                + " offsetX=" + mOffsetX
                + " offsetY=" + mOffsetY
                + " scale=" + mScale + "}";
    }

    /**
     * Returns a {@link Path} of the display shape.
     *
     * @return a {@link Path} of the display shape.
     */
    @NonNull
    public Path getPath() {
        // no display shape on the desktop: the path is empty (AOSP parses the config_mainDisplayShape resource)
        return new Path();
    }


}
