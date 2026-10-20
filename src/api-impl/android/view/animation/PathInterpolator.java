/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.InflateException;

import com.android.internal.R;

import java.util.ArrayList;

/**
 * An interpolator that can traverse a Path that extends from <code>Point</code>
 * <code>(0, 0)</code> to <code>(1, 1)</code>. The x coordinate along the <code>Path</code>
 * is the input value and the output is the y coordinate of the line at that point.
 *
 * ATL: Path.approximate() is not available, so the curves are flattened here: quadratic and
 * cubic Béziers given by control points, and pathData made of absolute M, L, Q and C commands
 * (which is what the framework and AndroidX use). Any other Path is interpolated linearly.
 */
public class PathInterpolator extends BaseInterpolator {

	// Number of line segments a curve is flattened into (AOSP approximates to 0.002).
	private static final int SEGMENTS = 64;

	private float[] mX; // x coordinates in the line

	private float[] mY; // y coordinates in the line

	/**
	 * Create an interpolator for an arbitrary <code>Path</code>. The <code>Path</code>
	 * must begin at <code>(0, 0)</code> and end at <code>(1, 1)</code>.
	 *
	 * @param path The <code>Path</code> to use to make the line representing the interpolator.
	 */
	public PathInterpolator(Path path) {
		mX = new float[] {0, 1};
		mY = new float[] {0, 1};
	}

	/**
	 * Create an interpolator for a quadratic Bezier curve. The end points
	 * <code>(0, 0)</code> and <code>(1, 1)</code> are assumed.
	 *
	 * @param controlX The x coordinate of the quadratic Bezier control point.
	 * @param controlY The y coordinate of the quadratic Bezier control point.
	 */
	public PathInterpolator(float controlX, float controlY) {
		initQuad(controlX, controlY);
	}

	/**
	 * Create an interpolator for a cubic Bezier curve.  The end points
	 * <code>(0, 0)</code> and <code>(1, 1)</code> are assumed.
	 *
	 * @param controlX1 The x coordinate of the first control point of the cubic Bezier.
	 * @param controlY1 The y coordinate of the first control point of the cubic Bezier.
	 * @param controlX2 The x coordinate of the second control point of the cubic Bezier.
	 * @param controlY2 The y coordinate of the second control point of the cubic Bezier.
	 */
	public PathInterpolator(float controlX1, float controlY1, float controlX2, float controlY2) {
		initCubic(controlX1, controlY1, controlX2, controlY2);
	}

	public PathInterpolator(Context context, AttributeSet attrs) {
		this(context.getResources(), context.getTheme(), attrs);
	}

	/** @hide */
	public PathInterpolator(Resources res, Theme theme, AttributeSet attrs) {
		TypedArray a;
		if (theme != null) {
			a = theme.obtainStyledAttributes(attrs, R.styleable.PathInterpolator, 0, 0);
		} else {
			a = res.obtainAttributes(attrs, R.styleable.PathInterpolator);
		}
		parseInterpolatorFromTypeArray(a);
		setChangingConfiguration(a.getChangingConfigurations());
		a.recycle();
	}

	private void parseInterpolatorFromTypeArray(TypedArray a) {
		// If there is pathData defined in the xml file, then the controls points
		// will be all coming from pathData.
		if (a.hasValue(R.styleable.PathInterpolator_pathData)) {
			String pathData = a.getString(R.styleable.PathInterpolator_pathData);
			if (!initPathData(pathData)) {
				throw new InflateException("The path is null, which is created"
				                           + " from " + pathData);
			}
		} else {
			if (!a.hasValue(R.styleable.PathInterpolator_controlX1)) {
				throw new InflateException("pathInterpolator requires the controlX1 attribute");
			} else if (!a.hasValue(R.styleable.PathInterpolator_controlY1)) {
				throw new InflateException("pathInterpolator requires the controlY1 attribute");
			}
			float x1 = a.getFloat(R.styleable.PathInterpolator_controlX1, 0);
			float y1 = a.getFloat(R.styleable.PathInterpolator_controlY1, 0);

			boolean hasX2 = a.hasValue(R.styleable.PathInterpolator_controlX2);
			boolean hasY2 = a.hasValue(R.styleable.PathInterpolator_controlY2);

			if (hasX2 != hasY2) {
				throw new InflateException(
				    "pathInterpolator requires both controlX2 and controlY2 for cubic Beziers.");
			}

			if (!hasX2) {
				initQuad(x1, y1);
			} else {
				float x2 = a.getFloat(R.styleable.PathInterpolator_controlX2, 0);
				float y2 = a.getFloat(R.styleable.PathInterpolator_controlY2, 0);
				initCubic(x1, y1, x2, y2);
			}
		}
	}

	private void initQuad(float controlX, float controlY) {
		ArrayList<float[]> points = new ArrayList<>();
		points.add(new float[] {0, 0});
		addQuad(points, 0, 0, controlX, controlY, 1, 1);
		initPoints(points);
	}

	private void initCubic(float x1, float y1, float x2, float y2) {
		ArrayList<float[]> points = new ArrayList<>();
		points.add(new float[] {0, 0});
		addCubic(points, 0, 0, x1, y1, x2, y2, 1, 1);
		initPoints(points);
	}

	private static void addQuad(ArrayList<float[]> points, float x0, float y0, float cx, float cy, float x, float y) {
		for (int i = 1; i <= SEGMENTS; i++) {
			float t = (float)i / SEGMENTS, u = 1 - t;
			points.add(new float[] {u * u * x0 + 2 * u * t * cx + t * t * x,
			                        u * u * y0 + 2 * u * t * cy + t * t * y});
		}
	}

	private static void addCubic(ArrayList<float[]> points, float x0, float y0, float x1, float y1,
	                             float x2, float y2, float x, float y) {
		for (int i = 1; i <= SEGMENTS; i++) {
			float t = (float)i / SEGMENTS, u = 1 - t;
			points.add(new float[] {u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x,
			                        u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y});
		}
	}

	/* pathData with absolute M, L, Q and C commands, as used by the framework interpolators */
	private boolean initPathData(String pathData) {
		String[] tokens = pathData.trim().replaceAll("([MLQCmlqc])", " $1 ").split("[\\s,]+");
		ArrayList<float[]> points = new ArrayList<>();
		float x = 0, y = 0;
		char command = 0;
		try {
			for (int i = 0; i < tokens.length;) {
				if (tokens[i].isEmpty()) {
					i++;
					continue;
				}
				char c = tokens[i].charAt(0);
				if (Character.isLetter(c)) {
					command = c;
					i++;
					continue;
				}
				switch (command) {
					case 'M':
					case 'L':
						x = Float.parseFloat(tokens[i++]);
						y = Float.parseFloat(tokens[i++]);
						points.add(new float[] {x, y});
						if (command == 'M')
							command = 'L';
						break;
					case 'Q': {
						float cx = Float.parseFloat(tokens[i++]), cy = Float.parseFloat(tokens[i++]);
						float ex = Float.parseFloat(tokens[i++]), ey = Float.parseFloat(tokens[i++]);
						addQuad(points, x, y, cx, cy, ex, ey);
						x = ex;
						y = ey;
						break;
					}
					case 'C': {
						float x1 = Float.parseFloat(tokens[i++]), y1 = Float.parseFloat(tokens[i++]);
						float x2 = Float.parseFloat(tokens[i++]), y2 = Float.parseFloat(tokens[i++]);
						float ex = Float.parseFloat(tokens[i++]), ey = Float.parseFloat(tokens[i++]);
						addCubic(points, x, y, x1, y1, x2, y2, ex, ey);
						x = ex;
						y = ey;
						break;
					}
					default:
						return false;
				}
			}
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			return false;
		}
		if (points.size() < 2)
			return false;
		initPoints(points);
		return true;
	}

	private void initPoints(ArrayList<float[]> points) {
		float[] first = points.get(0);
		float[] last = points.get(points.size() - 1);
		if (first[0] != 0 || first[1] != 0 || Math.abs(last[0] - 1) > 1e-4f || Math.abs(last[1] - 1) > 1e-4f) {
			throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)");
		}
		mX = new float[points.size()];
		mY = new float[points.size()];
		float prevX = 0;
		for (int i = 0; i < points.size(); i++) {
			float[] p = points.get(i);
			if (p[0] < prevX) {
				throw new IllegalArgumentException("The Path cannot loop back on itself.");
			}
			mX[i] = p[0];
			mY[i] = p[1];
			prevX = p[0];
		}
	}

	/**
	 * Using the line in the Path in this interpolator that can be described as
	 * <code>y = f(x)</code>, finds the y coordinate of the line given <code>t</code>
	 * as the x coordinate. Values less than 0 will always return 0 and values greater
	 * than 1 will always return 1.
	 *
	 * @param t Treated as the x coordinate along the line.
	 * @return The y coordinate of the Path along the line where x = <code>t</code>.
	 * @see Interpolator#getInterpolation(float)
	 */
	@Override
	public float getInterpolation(float t) {
		if (t <= 0) {
			return 0;
		} else if (t >= 1) {
			return 1;
		}
		// Do a binary search for the correct x to interpolate between.
		int startIndex = 0;
		int endIndex = mX.length - 1;

		while (endIndex - startIndex > 1) {
			int midIndex = (startIndex + endIndex) / 2;
			if (t < mX[midIndex]) {
				endIndex = midIndex;
			} else {
				startIndex = midIndex;
			}
		}

		float xRange = mX[endIndex] - mX[startIndex];
		if (xRange == 0) {
			return mY[startIndex];
		}

		float tInRange = t - mX[startIndex];
		float fraction = tInRange / xRange;

		float startY = mY[startIndex];
		float endY = mY[endIndex];
		return startY + (fraction * (endY - startY));
	}
}
