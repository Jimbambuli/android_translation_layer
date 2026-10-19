package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* as in AOSP: draws its drawable rotated by an angle between fromDegrees and toDegrees
 * that follows the level, around a pivot (by default the centre) */
public class RotateDrawable extends DrawableWrapper {
	private static final int MAX_LEVEL = 10000;

	private final RotateState mState;

	public RotateDrawable() {
		this(new RotateState(null, null), null);
	}

	private RotateDrawable(RotateState state, Resources res) {
		super(state, res);
		mState = state;
	}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
	    throws XmlPullParserException, IOException {
		final RotateState state = mState;
		final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.RotateDrawable);
		TypedValue tv = a.peekValue(R.styleable.RotateDrawable_pivotX);
		if (tv != null) {
			state.pivotXRel = tv.type == TypedValue.TYPE_FRACTION;
			state.pivotX = state.pivotXRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
		}
		tv = a.peekValue(R.styleable.RotateDrawable_pivotY);
		if (tv != null) {
			state.pivotYRel = tv.type == TypedValue.TYPE_FRACTION;
			state.pivotY = state.pivotYRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
		}
		state.fromDegrees = a.getFloat(R.styleable.RotateDrawable_fromDegrees, state.fromDegrees);
		state.toDegrees = a.getFloat(R.styleable.RotateDrawable_toDegrees, state.toDegrees);
		a.recycle();

		super.inflate(r, parser, attrs, theme);
	}

	@Override
	public void draw(Canvas canvas) {
		final Drawable d = getDrawable();
		if (d == null)
			return;
		final Rect bounds = d.getBounds();
		final int w = bounds.right - bounds.left;
		final int h = bounds.bottom - bounds.top;
		final RotateState st = mState;
		final float px = st.pivotXRel ? (w * st.pivotX) : st.pivotX;
		final float py = st.pivotYRel ? (h * st.pivotY) : st.pivotY;
		final float degrees = st.fromDegrees + (st.toDegrees - st.fromDegrees) * ((float)getLevel() / MAX_LEVEL);

		final int saveCount = canvas.save();
		canvas.rotate(degrees, px + bounds.left, py + bounds.top);
		d.draw(canvas);
		canvas.restoreToCount(saveCount);
	}

	@Override
	protected boolean onLevelChange(int level) {
		super.onLevelChange(level);
		invalidateSelf();
		return true;
	}

	public void setFromDegrees(float fromDegrees) {
		mState.fromDegrees = fromDegrees;
		invalidateSelf();
	}

	public float getFromDegrees() {
		return mState.fromDegrees;
	}

	public void setToDegrees(float toDegrees) {
		mState.toDegrees = toDegrees;
		invalidateSelf();
	}

	public float getToDegrees() {
		return mState.toDegrees;
	}

	static final class RotateState extends DrawableWrapper.DrawableWrapperState {
		boolean pivotXRel = true;
		float pivotX = 0.5f;
		boolean pivotYRel = true;
		float pivotY = 0.5f;
		float fromDegrees = 0.0f;
		float toDegrees = 360.0f;

		RotateState(RotateState orig, Resources res) {
			super(orig, res);
			if (orig != null) {
				pivotXRel = orig.pivotXRel;
				pivotX = orig.pivotX;
				pivotYRel = orig.pivotYRel;
				pivotY = orig.pivotY;
				fromDegrees = orig.fromDegrees;
				toDegrees = orig.toDegrees;
			}
		}

		@Override
		public Drawable newDrawable(Resources res) {
			return new RotateDrawable(this, res);
		}
	}
}
