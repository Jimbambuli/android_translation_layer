package android.graphics;

import android.annotation.UnsupportedAppUsage;

/*
 * Path is implemented as a GskPath or a GskPathBuilder. It can only be one of the two at a time.
 * The methods getGskPath() and getBuilder() automatically convert between the two as needed.
 */
public class Path {

	public enum FillType {
		WINDING(0, false),         // GSK_FILL_RULE_WINDING
		EVEN_ODD(1, false),        // GSK_FILL_RULE_EVEN_ODD
		INVERSE_WINDING(0, true),  // GSK_FILL_RULE_WINDING
		INVERSE_EVEN_ODD(1, true); // GSK_FILL_RULE_EVEN_ODD

		public int fill_rule;
		boolean inverse;

		private FillType(int fill_rule, boolean inverse) {
			this.fill_rule = fill_rule;
			this.inverse = inverse;
		}
	}

	public enum Direction {
		CW,
		CCW,
	}

	public enum Op {
		DIFFERENCE(Region.Op.DIFFERENCE),
		INTERSECT(Region.Op.INTERSECT),
		UNION(Region.Op.UNION),
		XOR(Region.Op.XOR),
		REVERSE_DIFFERENCE(Region.Op.REVERSE_DIFFERENCE);

		private Region.Op region_op;

		private Op(Region.Op region_op) {
			this.region_op = region_op;
		}
	}

	private long builder;
	private long path;
	private FillType fillType = FillType.WINDING;

	public Path() {}

	/* AOSP: the copy has the fill type of the source as well */
	public Path(Path path) {
		this.path = native_ref_path(path.getGskPath());
		this.fillType = path.fillType;
	}

	long getBuilder() {
		if (builder == 0 || path != 0) {
			builder = native_create_builder(path, builder);
			path = 0;
		}
		return builder;
	}

	@UnsupportedAppUsage
	public long getGskPath() {
		if (path == 0) {
			path = native_create_path(builder);
		}
		return path;
	}

	public void reset() {
		native_reset(path, builder);
		path = 0;
		builder = 0;
	}

	public void rewind() {
		reset();
	}

	public void close() {
		native_close(getBuilder());
	}

	public void setFillType(FillType fillType) {
		this.fillType = fillType;
	}

	public FillType getFillType() {
		return fillType;
	}

	/* AOSP Path.isInverseFillType(): the inverse bit of the fill type */
	public boolean isInverseFillType() {
		return fillType.inverse;
	}

	public void moveTo(float x, float y) {
		native_move_to(getBuilder(), x, y);
	}

	public void lineTo(float x, float y) {
		native_line_to(getBuilder(), x, y);
	}

	public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
		arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
	}

	public void arcTo(RectF oval, float startAngle, float sweepAngle) {
		arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, false);
	}

	public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean forceMoveTo) {
		native_arc_to(getBuilder(), left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
	}

	public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
		native_cubic_to(getBuilder(), x1, y1, x2, y2, x3, y3);
	}

	public void quadTo(float x1, float y1, float x2, float y2) {
		native_quad_to(getBuilder(), x1, y1, x2, y2);
	}

	public void rMoveTo(float x, float y) {
		native_rel_move_to(getBuilder(), x, y);
	}

	public void rLineTo(float x, float y) {
		native_rel_line_to(getBuilder(), x, y);
	}

	public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
		native_rel_cubic_to(getBuilder(), x1, y1, x2, y2, x3, y3);
	}

	public void rQuadTo(float x1, float y1, float x2, float y2) {
		native_rel_quad_to(getBuilder(), x1, y1, x2, y2);
	}

	public void addArc(RectF oval, float startAngle, float sweepAngle) {
		addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
	}

	public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
		native_add_arc(getBuilder(), left, top, right, bottom, startAngle, sweepAngle);
	}

	public void addPath(Path path, Matrix matrix) {
		native_add_path(getBuilder(), path.getGskPath(), matrix.ni());
	}

	public void addPath(Path path, float deltaX, float deltaY) {
		Matrix matrix = new Matrix();
		matrix.setTranslate(deltaX, deltaY);
		addPath(path, matrix);
	}

	public void addPath(Path path) {
		addPath(path, Matrix.IDENTITY_MATRIX);
	}

	public void addRect(RectF rect, Direction direction) {
		addRect(rect.left, rect.top, rect.right, rect.bottom, direction);
	}

	public void addRect(float left, float top, float right, float bottom, Path.Direction dir) {
		if (dir == Direction.CCW) {
			/* GSK adds rectangles clockwise; the direction decides the winding number, e.g. for a mask
			 * built as shape (CW) plus bounds (CCW). Same points as Skia's SkPath::addRect(). */
			long builder = getBuilder();
			native_move_to(builder, left, top);
			native_line_to(builder, left, bottom);
			native_line_to(builder, right, bottom);
			native_line_to(builder, right, top);
			native_close(builder);
		} else {
			native_add_rect(getBuilder(), left, top, right, bottom);
		}
	}

	public void addRoundRect(float left, float top, float right, float bottom,
	                         float[] radii, Direction direction) {
		native_add_round_rect(getBuilder(), left, top, right, bottom, radii);
	}

	public void addRoundRect(RectF rect, float[] radii, Direction direction) {
		addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, direction);
	}

	public void addRoundRect(RectF rect, float rx, float ry, Direction direction) {
		addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, direction);
	}

	public void addRoundRect(float left, float top, float right, float bottom,
	                         float rx, float ry, Direction direction) {
		addRoundRect(left, top, right, bottom, new float[] {rx, ry, rx, ry, rx, ry, rx, ry}, direction);
	}

	public void addOval(float left, float top, float right, float bottom, Direction direction) {
		float rx = (right - left) / 2;
		float ry = (bottom - top) / 2;
		addRoundRect(left, top, right, bottom, new float[] {rx, ry, rx, ry, rx, ry, rx, ry}, direction);
	}

	public void addOval(RectF rect, Direction direction) {
		addOval(rect.left, rect.top, rect.right, rect.bottom, direction);
	}

	public void addCircle(float x, float y, float radius, Direction direction) {
		addOval(x - radius, y - radius, x + radius, y + radius, direction);
	}

	public void transform(Matrix matrix) {
		builder = native_transform(getGskPath(), matrix.ni());
		path = 0;
	}

	public void transform(Matrix matrix, Path out_path) {
		if (out_path == null)
			out_path = this;

		out_path.transform(matrix);
	}

	public void computeBounds(RectF bounds, boolean exact) {
		native_get_bounds(getGskPath(), bounds);
	}

	/* AOSP computeBounds(RectF): the exact bounds */
	public void computeBounds(RectF bounds) {
		computeBounds(bounds, true);
	}

	public boolean op(Path path, Op op) {
		return op(this, path, op);
	}

	public boolean op(Path path1, Path path2, Op op) {
		RectF bounds1 = new RectF();
		RectF bounds2 = new RectF();
		path1.computeBounds(bounds1, false);
		path2.computeBounds(bounds2, false);
		// special case for composeUI ShapeContainingUtil.isInPath which puts a tiny rectangle in path2 to represent a point
		if (op == Op.INTERSECT && bounds2.width() < 1 && bounds2.height() < 1) {
			if (native_contains(path1.path, path1.fillType.fill_rule, path1.fillType.inverse, bounds2.centerX(), bounds2.centerY()))
				set(path2);
			else
				reset();
			return true;
		} else if (op == Op.INTERSECT && bounds1.width() < 1 && bounds1.height() < 1) {
			if (native_contains(path2.path, path2.fillType.fill_rule, path2.fillType.inverse, bounds1.centerX(), bounds1.centerY()))
				set(path1);
			else
				reset();
			return true;
		}
		Region region1 = new Region();
		Region region2 = new Region();
		boolean inverse1 = path1.isInverseFillType();
		boolean inverse2 = path2.isInverseFillType();
		if (!inverse1 && !inverse2) {
			region1.setPath(path1, null);
			region2.setPath(path2, null);
			region1.op(region2, op.region_op);
			region1.getBoundaryPath(this);
			return true;
		}
		/* AOSP SkPathOps with an inverse operand: far outside both paths the result is the op applied to the
		 * inverse bits; inside their bounds it is computed on regions clipped to these bounds. If the outside
		 * belongs to the result, the result is the complement within the bounds, with an inverse fill type */
		bounds1.union(bounds2);
		Region clip = new Region((int)Math.floor(bounds1.left) - 1, (int)Math.floor(bounds1.top) - 1, (int)Math.ceil(bounds1.right) + 1, (int)Math.ceil(bounds1.bottom) + 1);
		region1.setPath(path1, clip);
		region2.setPath(path2, clip);
		region1.op(region2, op.region_op);
		boolean outside;
		switch (op) {
			case DIFFERENCE: outside = inverse1 && !inverse2; break;
			case INTERSECT: outside = inverse1 && inverse2; break;
			case UNION: outside = inverse1 || inverse2; break;
			case XOR: outside = inverse1 != inverse2; break;
			default: outside = inverse2 && !inverse1; break; // REVERSE_DIFFERENCE
		}
		if (outside)
			region1.op(clip, region1, Region.Op.DIFFERENCE);
		region1.getBoundaryPath(this);
		setFillType(outside ? FillType.INVERSE_WINDING : FillType.WINDING);
		return true;
	}

	public boolean isEmpty() {
		return path == 0 && builder == 0;
	}

	public void incReserve(int additionalPoints) {}

	/* AOSP Path.isConvex() -> SkPath::isConvex(): computed natively from the control points like Skia */
	@Deprecated
	public boolean isConvex() {
		return native_is_convex(getGskPath());
	}

	/* AOSP Path.getPathIterator() */
	public PathIterator getPathIterator() {
		return new PathIterator(this);
	}

	/* AOSP SkPath assignment: the fill type is copied as well */
	public void set(Path src) {
		if (src == this)
			return;
		reset();
		addPath(src);
		fillType = src.fillType;
	}

	public void offset(float dx, float dy) {
		Matrix matrix = new Matrix();
		matrix.setTranslate(dx, dy);
		transform(matrix);
	}

	/* AOSP offset(dx, dy, dst): a translated copy in dst, this path itself when dst is null */
	public void offset(float dx, float dy, Path dst) {
		if (dst == null) {
			offset(dx, dy);
			return;
		}
		dst.set(this);
		dst.offset(dx, dy);
	}

	/* AOSP Path.setLastPoint() -> SkPath::setLastPt(): the last point replaced, a moveTo on an empty path */
	public void setLastPoint(float dx, float dy) {
		builder = native_set_last_point(getGskPath(), dx, dy);
		path = 0;
	}

	/* AOSP SkPath::toggleInverseFillType(): flips the inverse bit of the fill type */
	public void toggleInverseFillType() {
		switch (fillType) {
			case WINDING:
				fillType = FillType.INVERSE_WINDING;
				break;
			case EVEN_ODD:
				fillType = FillType.INVERSE_EVEN_ODD;
				break;
			case INVERSE_WINDING:
				fillType = FillType.WINDING;
				break;
			case INVERSE_EVEN_ODD:
				fillType = FillType.EVEN_ODD;
				break;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			reset();
		} finally {
			super.finalize();
		}
	}

	/**
	 * Approximate the <code>Path</code> with a series of line segments.
	 * This returns float[] with the array containing point components.
	 * There are three components for each point, in order:
	 * <ul>
	 *     <li>Fraction along the length of the path that the point resides</li>
	 *     <li>The x coordinate of the point</li>
	 *     <li>The y coordinate of the point</li>
	 * </ul>
	 * <p>Two points may share the same fraction along its length when there is
	 * a move action within the Path.</p>
	 *
	 * ATL: the points are measured along each contour with PathMeasure, one point per
	 * max(2 * acceptableError, 0.25) of length (at most 4096 per contour); AOSP's Skia picks the
	 * points by the flatness of each segment.
	 *
	 * @param acceptableError The acceptable error for a line on the
	 *                        Path. Typically this would be 0.5 so that
	 *                        the error is less than half a pixel.
	 * @return An array of components for points approximating the Path.
	 */
	public float[] approximate(float acceptableError) {
		if (acceptableError < 0) {
			throw new IllegalArgumentException("AcceptableError must be greater than or equal to 0");
		}
		float step = Math.max(2 * acceptableError, 0.25f);
		PathMeasure measure = new PathMeasure(this, false);
		float total = 0;
		do {
			total += measure.getLength();
		} while (measure.nextContour());
		if (!(total > 0)) {
			return new float[] {0, 0, 0, 1, 0, 0};
		}
		float[] out = new float[3 * 256];
		int n = 0;
		float done = 0;
		float[] pos = new float[2];
		measure = new PathMeasure(this, false);
		do {
			float length = measure.getLength();
			if (!(length > 0)) {
				continue;
			}
			int count = (int)Math.min(4096, Math.ceil(length / step));
			for (int i = 0; i <= count; i++) {
				float d = i == count ? length : length * i / count;
				if (!measure.getPosTan(d, pos, null)) {
					continue;
				}
				if (n + 3 > out.length) {
					out = java.util.Arrays.copyOf(out, out.length * 2);
				}
				out[n++] = (done + d) / total;
				out[n++] = pos[0];
				out[n++] = pos[1];
			}
			done += length;
		} while (measure.nextContour());
		return java.util.Arrays.copyOf(out, n);
	}

	private static native long native_create_builder(long path, long builder);
	private static native long native_create_path(long builder);
	private static native long native_ref_path(long path);
	private static native void native_reset(long path, long builder);
	private static native void native_close(long builder);
	private static native void native_move_to(long builder, float x, float y);
	private static native void native_line_to(long builder, float x, float y);
	private static native void native_cubic_to(long builder, float x1, float y1, float x2, float y2, float x3, float y3);
	private static native void native_quad_to(long builder, float x1, float y1, float x2, float y2);
	private static native void native_arc_to(long builder, float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean forceMoveTo);
	private static native void native_rel_move_to(long builder, float x, float y);
	private static native void native_rel_line_to(long builder, float x, float y);
	private static native void native_rel_cubic_to(long builder, float x1, float y1, float x2, float y2, float x3, float y3);
	private static native void native_rel_quad_to(long builder, float x1, float y1, float x2, float y2);
	private static native void native_add_arc(long builder, float left, float top, float right, float bottom, float startAngle, float sweepAngle);
	private static native void native_add_path(long builder, long path, long matrix);
	private static native void native_add_rect(long builder, float left, float top, float right, float bottom);
	private static native void native_add_round_rect(long builder, float left, float top, float right, float bottom, float[] radii);
	private static native void native_get_bounds(long path, RectF rect);
	private static native long native_transform(long path, long matrix);
	private static native boolean native_contains(long path, int fill_rule, boolean inverse, float x, float y);
	static native float[] native_get_segments(long path);
	private static native boolean native_is_convex(long path);
	private static native long native_set_last_point(long path, float x, float y);
}
