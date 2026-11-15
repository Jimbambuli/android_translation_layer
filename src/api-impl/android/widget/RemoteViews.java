package android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Port of the client side of android-14 frameworks/base core/java/android/widget/RemoteViews.java:
 * the setters record actions (as in AOSP addAction()) and apply()/reapply() inflate the layout and run them
 * on the views (AOSP performApply()). ATL has no app widget host or notification renderer that would call
 * apply() from another process; an app that applies its own RemoteViews gets AOSP behaviour.
 * The parcel format is ATL's own (only read back by this class), not AOSP's wire format.
 * Not ported: RemoteViewsAdapter binding for setRemoteAdapter(int, Intent) (needs the app widget service),
 * view recycling/async apply, @RemotableViewMethod checks, shared element transitions.
 */
public class RemoteViews implements Parcelable, Cloneable {

	/** AOSP RemoteViews.EXTRA_CHECKED */
	public static final String EXTRA_CHECKED = "android.widget.extra.CHECKED";
	public static final String EXTRA_SHARED_ELEMENT_BOUNDS = "android:browser.extra.shared_element_bounds";

	public static final int MARGIN_LEFT = 0;
	public static final int MARGIN_TOP = 1;
	public static final int MARGIN_RIGHT = 2;
	public static final int MARGIN_BOTTOM = 3;
	public static final int MARGIN_START = 4;
	public static final int MARGIN_END = 5;

	private static final int MODE_NORMAL = 0;
	private static final int MODE_HAS_LANDSCAPE_AND_PORTRAIT = 1;
	private static final int MODE_HAS_SIZED_REMOTEVIEWS = 2;
	/* AOSP MAX_INIT_VIEW_COUNT */
	private static final int MAX_INIT_VIEW_COUNT = 16;

	private String mPackage;
	private int mLayoutId;
	private int mViewId = View.NO_ID;
	private ArrayList<Action> mActions = new ArrayList<>();
	private RemoteViews mLandscape;
	private RemoteViews mPortrait;
	private RemoteViews[] mSizedRemoteViews;
	private SizeF mIdealSize;

	/** AOSP RemoteViews.ActionException */
	public static class ActionException extends RuntimeException {
		public ActionException(Exception ex) {
			super(ex);
		}

		public ActionException(String message) {
			super(message);
		}

		public ActionException(Throwable t) {
			super(t);
		}
	}

	public RemoteViews(String packageName, int layoutId) {
		mPackage = packageName;
		mLayoutId = layoutId;
	}

	public RemoteViews(String packageName, int layoutId, int viewId) {
		this(packageName, layoutId);
		mViewId = viewId;
	}

	/** AOSP RemoteViews(RemoteViews, RemoteViews) */
	public RemoteViews(RemoteViews landscape, RemoteViews portrait) {
		if (landscape == null || portrait == null) {
			throw new IllegalArgumentException("Both RemoteViews must be non-null");
		}
		if (!java.util.Objects.equals(landscape.mPackage, portrait.mPackage)) {
			throw new IllegalArgumentException("Both RemoteViews must share the same package and user");
		}
		mPackage = portrait.mPackage;
		mLayoutId = portrait.mLayoutId;
		mViewId = portrait.mViewId;
		mLandscape = landscape;
		mPortrait = portrait;
	}

	/** AOSP RemoteViews(Map&lt;SizeF, RemoteViews&gt;) */
	public RemoteViews(Map<SizeF, RemoteViews> remoteViews) {
		if (remoteViews.isEmpty()) {
			throw new IllegalArgumentException("The set of RemoteViews cannot be empty");
		}
		if (remoteViews.size() > MAX_INIT_VIEW_COUNT) {
			throw new IllegalArgumentException("Too many RemoteViews in constructor");
		}
		if (remoteViews.size() == 1) {
			initializeFrom(remoteViews.values().iterator().next());
			return;
		}
		ArrayList<RemoteViews> list = new ArrayList<>();
		String pkg = null;
		for (Map.Entry<SizeF, RemoteViews> e : remoteViews.entrySet()) {
			RemoteViews v = e.getValue();
			if (pkg == null)
				pkg = v.mPackage;
			else if (!java.util.Objects.equals(pkg, v.mPackage))
				throw new IllegalArgumentException("All RemoteViews must share the same package and user");
			v.mIdealSize = e.getKey();
			list.add(v);
		}
		mSizedRemoteViews = list.toArray(new RemoteViews[0]);
		RemoteViews smallest = findSmallestRemoteView();
		mPackage = smallest.mPackage;
		mLayoutId = smallest.mLayoutId;
		mViewId = smallest.mViewId;
	}

	/** AOSP RemoteViews(RemoteViews): copy of src */
	public RemoteViews(RemoteViews src) {
		initializeFrom(src);
	}

	private boolean mHasDrawInstructions;
	private DrawInstructions mDrawInstructions;

	/** AOSP RemoteViews(DrawInstructions): the instructions are kept; ATL has no RemoteCompose player to apply them */
	public RemoteViews(DrawInstructions drawInstructions) {
		java.util.Objects.requireNonNull(drawInstructions);
		mHasDrawInstructions = true;
		mDrawInstructions = drawInstructions;
	}

	/** AOSP RemoteViews.hasDrawInstructions() */
	public boolean hasDrawInstructions() {
		return mHasDrawInstructions;
	}

	/** AOSP RemoteViews.DrawInstructions (CoreDocument.DOCUMENT_API_LEVEL = 8 in Android 16) */
	public static final class DrawInstructions {
		final java.util.List<byte[]> mInstructions;

		private DrawInstructions() {
			throw new UnsupportedOperationException("DrawInstructions cannot be instantiate without instructions");
		}

		private DrawInstructions(java.util.List<byte[]> instructions) {
			mInstructions = new java.util.ArrayList<>(instructions.size());
			for (byte[] instruction : instructions) {
				mInstructions.add(instruction.clone());
			}
		}

		public static long getSupportedVersion() {
			return 8L;
		}

		public static final class Builder {
			private final java.util.List<byte[]> mInstructions;

			public Builder(final java.util.List<byte[]> instructions) {
				mInstructions = new java.util.ArrayList<>(instructions);
			}

			public DrawInstructions build() {
				return new DrawInstructions(mInstructions);
			}
		}
	}

	/** AOSP RemoteViews(Parcel) */
	public RemoteViews(Parcel parcel) {
		int mode = parcel.readInt();
		if (mode == MODE_HAS_SIZED_REMOTEVIEWS) {
			int n = parcel.readInt();
			mSizedRemoteViews = new RemoteViews[n];
			for (int i = 0; i < n; i++)
				mSizedRemoteViews[i] = new RemoteViews(parcel);
			RemoteViews smallest = findSmallestRemoteView();
			mPackage = smallest.mPackage;
			mLayoutId = smallest.mLayoutId;
			mViewId = smallest.mViewId;
		} else if (mode == MODE_HAS_LANDSCAPE_AND_PORTRAIT) {
			mLandscape = new RemoteViews(parcel);
			mPortrait = new RemoteViews(parcel);
			mPackage = mPortrait.mPackage;
			mLayoutId = mPortrait.mLayoutId;
			mViewId = mPortrait.mViewId;
		} else {
			mPackage = parcel.readString();
			mLayoutId = parcel.readInt();
			mViewId = parcel.readInt();
			if (parcel.readInt() != 0)
				mIdealSize = new SizeF(parcel.readFloat(), parcel.readFloat());
			int n = parcel.readInt();
			for (int i = 0; i < n; i++)
				mActions.add(readAction(parcel));
		}
	}

	private void initializeFrom(RemoteViews src) {
		mPackage = src.mPackage;
		mLayoutId = src.mLayoutId;
		mViewId = src.mViewId;
		mIdealSize = src.mIdealSize;
		mActions = new ArrayList<>(src.mActions);
		mLandscape = src.mLandscape == null ? null : new RemoteViews(src.mLandscape);
		mPortrait = src.mPortrait == null ? null : new RemoteViews(src.mPortrait);
		if (src.mSizedRemoteViews != null) {
			mSizedRemoteViews = new RemoteViews[src.mSizedRemoteViews.length];
			for (int i = 0; i < mSizedRemoteViews.length; i++)
				mSizedRemoteViews[i] = new RemoteViews(src.mSizedRemoteViews[i]);
		}
	}

	/* AOSP findSmallestRemoteView(): smallest ideal area */
	private RemoteViews findSmallestRemoteView() {
		RemoteViews best = mSizedRemoteViews[0];
		for (RemoteViews v : mSizedRemoteViews) {
			if (area(v.mIdealSize) < area(best.mIdealSize))
				best = v;
		}
		return best;
	}

	private static float area(SizeF s) {
		return s == null ? 0 : s.getWidth() * s.getHeight();
	}

	private boolean hasMultipleLayouts() {
		return mLandscape != null || mSizedRemoteViews != null;
	}

	/* AOSP checks: actions can only be added to a RemoteViews without landscape/portrait or sized layouts */
	private void addAction(Action a) {
		if (hasMultipleLayouts()) {
			throw new RuntimeException("RemoteViews specifying separate layouts for orientation or size cannot be modified. Instead, fully configure each layouts individually before constructing the combined layout.");
		}
		mActions.add(a);
	}

	public String getPackage() {
		return mPackage;
	}

	public int getLayoutId() {
		return mLayoutId;
	}

	public int getViewId() {
		return mViewId;
	}

	@Override
	public RemoteViews clone() {
		synchronized (this) {
			return new RemoteViews(this);
		}
	}

	/* AOSP getRemoteViewToApply(): orientation for landscape/portrait, smallest layout without a size */
	private RemoteViews getRemoteViewsToApply(Context context) {
		if (mLandscape != null) {
			int orientation = context.getResources().getConfiguration().orientation;
			return orientation == Configuration.ORIENTATION_LANDSCAPE ? mLandscape : mPortrait;
		}
		if (mSizedRemoteViews != null)
			return findSmallestRemoteView();
		return this;
	}

	private Context getContextForResources(Context context) {
		if (mPackage == null || mPackage.equals(context.getPackageName()))
			return context;
		try {
			Context c = context.createPackageContext(mPackage, Context.CONTEXT_RESTRICTED);
			return c != null ? c : context;
		} catch (Exception e) {
			return context;
		}
	}

	/** AOSP apply(): inflate the layout and perform the recorded actions */
	public View apply(Context context, ViewGroup parent) {
		RemoteViews rvToApply = getRemoteViewsToApply(context);
		Context inflationContext = getContextForResources(context);
		View result = android.view.LayoutInflater.from(inflationContext).inflate(rvToApply.mLayoutId, parent, false);
		if (rvToApply.mViewId != View.NO_ID)
			result.setId(rvToApply.mViewId);
		rvToApply.performApply(result, parent);
		return result;
	}

	/** AOSP reapply(): perform the actions on an already inflated hierarchy */
	public void reapply(Context context, View v) {
		RemoteViews rvToApply = getRemoteViewsToApply(context);
		rvToApply.performApply(v, (ViewGroup)(v.getParent() instanceof ViewGroup ? v.getParent() : null));
	}

	private void performApply(View v, ViewGroup parent) {
		for (Action a : new ArrayList<>(mActions))
			a.apply(v, parent);
	}

	/* ---- convenience setters (AOSP: each maps onto one of the actions below) ---- */

	public void addView(int viewId, RemoteViews nestedView) {
		addAction(nestedView == null ? new ViewGroupActionRemove(viewId) : new ViewGroupActionAdd(viewId, nestedView, -1, 0));
	}

	public void addView(int viewId, RemoteViews nestedView, int index) {
		addAction(new ViewGroupActionAdd(viewId, nestedView, index, 0));
	}

	public void addStableView(int viewId, RemoteViews nestedView, int stableId) {
		addAction(new ViewGroupActionAdd(viewId, nestedView, -1, stableId));
	}

	public void removeAllViews(int viewId) {
		addAction(new ViewGroupActionRemove(viewId));
	}

	public void setDisplayedChild(int viewId, int childIndex) {
		setInt(viewId, "setDisplayedChild", childIndex);
	}

	public void setViewVisibility(int viewId, int visibility) {
		setInt(viewId, "setVisibility", visibility);
	}

	public void setTextViewText(int viewId, CharSequence text) {
		setCharSequence(viewId, "setText", text);
	}

	/* AOSP setTextViewCompoundDrawables(): the drawables are resource ids (0 = none), set at apply time with intrinsic bounds */
	public void setTextViewCompoundDrawables(int viewId, int left, int top, int right, int bottom) {
		addAction(new TextViewDrawableAction(viewId, false, left, top, right, bottom));
	}

	public void setTextViewCompoundDrawablesRelative(int viewId, int start, int top, int end, int bottom) {
		addAction(new TextViewDrawableAction(viewId, true, start, top, end, bottom));
	}

	/* AOSP showNext()/showPrevious(): ViewContentNavigation, calls showNext()/showPrevious() of the view (ViewAnimator) */
	public void showNext(int viewId) {
		addAction(new ViewContentNavigation(viewId, true));
	}

	public void showPrevious(int viewId) {
		addAction(new ViewContentNavigation(viewId, false));
	}

	public void setTextViewTextSize(int viewId, int units, float size) {
		addAction(new TextViewSizeAction(viewId, units, size));
	}

	public void setImageViewResource(int viewId, int srcId) {
		setInt(viewId, "setImageResource", srcId);
	}

	public void setImageViewUri(int viewId, Uri uri) {
		setUri(viewId, "setImageURI", uri);
	}

	public void setImageViewBitmap(int viewId, Bitmap bitmap) {
		setBitmap(viewId, "setImageBitmap", bitmap);
	}

	public void setImageViewIcon(int viewId, Icon icon) {
		setIcon(viewId, "setImageIcon", icon);
	}

	public void setEmptyView(int viewId, int emptyViewId) {
		addAction(new SetEmptyView(viewId, emptyViewId));
	}

	public void setChronometer(int viewId, long base, String format, boolean started) {
		setLong(viewId, "setBase", base);
		setString(viewId, "setFormat", format);
		setBoolean(viewId, "setStarted", started);
	}

	public void setChronometerCountDown(int viewId, boolean isCountDown) {
		setBoolean(viewId, "setCountDown", isCountDown);
	}

	public void setProgressBar(int viewId, int max, int progress, boolean indeterminate) {
		setBoolean(viewId, "setIndeterminate", indeterminate);
		if (!indeterminate) {
			setInt(viewId, "setMax", max);
			setInt(viewId, "setProgress", progress);
		}
	}

	public void setCompoundButtonChecked(int viewId, boolean checked) {
		setBoolean(viewId, "setChecked", checked);
	}

	public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
		setOnClickResponse(viewId, RemoteResponse.fromPendingIntent(pendingIntent));
	}

	public void setOnClickResponse(int viewId, RemoteResponse response) {
		addAction(new SetOnClickResponse(viewId, response.setInteractionType(RemoteResponse.INTERACTION_TYPE_CLICK)));
	}

	public void setPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate) {
		addAction(new SetPendingIntentTemplate(viewId, pendingIntentTemplate));
	}

	public void setOnClickFillInIntent(int viewId, Intent fillInIntent) {
		setOnClickResponse(viewId, RemoteResponse.fromFillInIntent(fillInIntent));
	}

	public void setOnCheckedChangeResponse(int viewId, RemoteResponse response) {
		addAction(new SetOnCheckedChangeResponse(viewId, response.setInteractionType(RemoteResponse.INTERACTION_TYPE_CHECKED_CHANGE)));
	}

	public void setTextColor(int viewId, int color) {
		setInt(viewId, "setTextColor", color);
	}

	public void setTextColor(int viewId, ColorStateList colors) {
		setColorStateList(viewId, "setTextColor", colors);
	}

	/** Needs RemoteViewsAdapter and the app widget service (not in ATL): recorded nowhere, as before. */
	public void setRemoteAdapter(int viewId, Intent intent) {}

	/* AOSP setRemoteAdapter(int appWidgetId, int viewId, Intent) (deprecated) -> setRemoteAdapter(viewId, intent) */
	public void setRemoteAdapter(int appWidgetId, int viewId, Intent intent) {
		setRemoteAdapter(viewId, intent);
	}

	public void setRemoteAdapter(int viewId, RemoteCollectionItems items) {
		addAction(new SetRemoteCollectionItemListAdapterAction(viewId, items));
	}

	public void setScrollPosition(int viewId, int position) {
		setInt(viewId, "smoothScrollToPosition", position);
	}

	public void setViewPadding(int viewId, int left, int top, int right, int bottom) {
		addAction(new ViewPaddingAction(viewId, left, top, right, bottom));
	}

	public void setViewLayoutMargin(int viewId, int type, float value, int units) {
		addAction(new LayoutParamAction(viewId, type, LayoutParamAction.VALUE_TYPE_COMPLEX_UNIT, value, units));
	}

	public void setViewLayoutMarginDimen(int viewId, int type, int dimen) {
		addAction(new LayoutParamAction(viewId, type, LayoutParamAction.VALUE_TYPE_RESOURCE, dimen, 0));
	}

	public void setViewLayoutMarginAttr(int viewId, int type, int attr) {
		addAction(new LayoutParamAction(viewId, type, LayoutParamAction.VALUE_TYPE_ATTRIBUTE, attr, 0));
	}

	public void setViewLayoutWidth(int viewId, float width, int units) {
		if (units == TypedValue.COMPLEX_UNIT_PX && (width == ViewGroup.LayoutParams.MATCH_PARENT || width == ViewGroup.LayoutParams.WRAP_CONTENT))
			addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_WIDTH, LayoutParamAction.VALUE_TYPE_RAW, width, 0));
		else
			addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_WIDTH, LayoutParamAction.VALUE_TYPE_COMPLEX_UNIT, width, units));
	}

	public void setViewLayoutWidthDimen(int viewId, int widthDimen) {
		addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_WIDTH, LayoutParamAction.VALUE_TYPE_RESOURCE, widthDimen, 0));
	}

	public void setViewLayoutWidthAttr(int viewId, int widthAttr) {
		addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_WIDTH, LayoutParamAction.VALUE_TYPE_ATTRIBUTE, widthAttr, 0));
	}

	public void setViewLayoutHeight(int viewId, float height, int units) {
		if (units == TypedValue.COMPLEX_UNIT_PX && (height == ViewGroup.LayoutParams.MATCH_PARENT || height == ViewGroup.LayoutParams.WRAP_CONTENT))
			addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_HEIGHT, LayoutParamAction.VALUE_TYPE_RAW, height, 0));
		else
			addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_HEIGHT, LayoutParamAction.VALUE_TYPE_COMPLEX_UNIT, height, units));
	}

	public void setViewLayoutHeightDimen(int viewId, int heightDimen) {
		addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_HEIGHT, LayoutParamAction.VALUE_TYPE_RESOURCE, heightDimen, 0));
	}

	public void setViewLayoutHeightAttr(int viewId, int heightAttr) {
		addAction(new LayoutParamAction(viewId, LayoutParamAction.LAYOUT_HEIGHT, LayoutParamAction.VALUE_TYPE_ATTRIBUTE, heightAttr, 0));
	}

	public void setViewOutlinePreferredRadius(int viewId, float radius, int units) {
		addAction(new SetViewOutlinePreferredRadiusAction(viewId, LayoutParamAction.VALUE_TYPE_COMPLEX_UNIT, radius, units));
	}

	public void setViewOutlinePreferredRadiusDimen(int viewId, int resId) {
		addAction(new SetViewOutlinePreferredRadiusAction(viewId, LayoutParamAction.VALUE_TYPE_RESOURCE, resId, 0));
	}

	public void setViewOutlinePreferredRadiusAttr(int viewId, int attrId) {
		addAction(new SetViewOutlinePreferredRadiusAction(viewId, LayoutParamAction.VALUE_TYPE_ATTRIBUTE, attrId, 0));
	}

	/* ---- reflection setters ---- */

	/** AOSP RemoteViews.setContentDescription(): setCharSequence(viewId, "setContentDescription", ...) */
	public void setContentDescription(int viewId, CharSequence contentDescription) {
		setCharSequence(viewId, "setContentDescription", contentDescription);
	}

	public void setBoolean(int viewId, String methodName, boolean value) {
		addAction(new ReflectionAction(viewId, methodName, T_BOOLEAN, value));
	}

	public void setByte(int viewId, String methodName, byte value) {
		addAction(new ReflectionAction(viewId, methodName, T_BYTE, value));
	}

	public void setShort(int viewId, String methodName, short value) {
		addAction(new ReflectionAction(viewId, methodName, T_SHORT, value));
	}

	public void setInt(int viewId, String methodName, int value) {
		addAction(new ReflectionAction(viewId, methodName, T_INT, value));
	}

	public void setLong(int viewId, String methodName, long value) {
		addAction(new ReflectionAction(viewId, methodName, T_LONG, value));
	}

	public void setFloat(int viewId, String methodName, float value) {
		addAction(new ReflectionAction(viewId, methodName, T_FLOAT, value));
	}

	public void setDouble(int viewId, String methodName, double value) {
		addAction(new ReflectionAction(viewId, methodName, T_DOUBLE, value));
	}

	public void setChar(int viewId, String methodName, char value) {
		addAction(new ReflectionAction(viewId, methodName, T_CHAR, value));
	}

	public void setString(int viewId, String methodName, String value) {
		addAction(new ReflectionAction(viewId, methodName, T_STRING, value));
	}

	public void setCharSequence(int viewId, String methodName, CharSequence value) {
		addAction(new ReflectionAction(viewId, methodName, T_CHAR_SEQUENCE, value));
	}

	public void setUri(int viewId, String methodName, Uri value) {
		addAction(new ReflectionAction(viewId, methodName, T_URI, value));
	}

	public void setBitmap(int viewId, String methodName, Bitmap value) {
		addAction(new ReflectionAction(viewId, methodName, T_BITMAP, value));
	}

	public void setBundle(int viewId, String methodName, Bundle value) {
		addAction(new ReflectionAction(viewId, methodName, T_BUNDLE, value));
	}

	public void setIntent(int viewId, String methodName, Intent value) {
		addAction(new ReflectionAction(viewId, methodName, T_INTENT, value));
	}

	public void setIcon(int viewId, String methodName, Icon value) {
		addAction(new ReflectionAction(viewId, methodName, T_ICON, value));
	}

	public void setIcon(int viewId, String methodName, Icon notNight, Icon night) {
		addAction(new NightModeReflectionAction(viewId, methodName, T_ICON, notNight, night));
	}

	public void setBlendMode(int viewId, String methodName, BlendMode value) {
		addAction(new ReflectionAction(viewId, methodName, T_BLEND_MODE, value));
	}

	public void setColorStateList(int viewId, String methodName, ColorStateList value) {
		addAction(new ReflectionAction(viewId, methodName, T_COLOR_STATE_LIST, value));
	}

	public void setColorStateList(int viewId, String methodName, ColorStateList notNight, ColorStateList night) {
		addAction(new NightModeReflectionAction(viewId, methodName, T_COLOR_STATE_LIST, notNight, night));
	}

	public void setColorStateList(int viewId, String methodName, int colorResource) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_COLOR_STATE_LIST, RES_COLOR, colorResource, false));
	}

	public void setColorStateListAttr(int viewId, String methodName, int colorAttr) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_COLOR_STATE_LIST, RES_COLOR, colorAttr, true));
	}

	public void setColor(int viewId, String methodName, int colorResource) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_INT, RES_COLOR, colorResource, false));
	}

	public void setColorAttr(int viewId, String methodName, int colorAttribute) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_INT, RES_COLOR, colorAttribute, true));
	}

	public void setColorInt(int viewId, String methodName, int notNight, int night) {
		addAction(new NightModeReflectionAction(viewId, methodName, T_INT, notNight, night));
	}

	public void setIntDimen(int viewId, String methodName, int dimenResource) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_INT, RES_DIMEN, dimenResource, false));
	}

	public void setIntDimen(int viewId, String methodName, float value, int unit) {
		addAction(new ComplexUnitDimensionReflectionAction(viewId, methodName, T_INT, value, unit));
	}

	public void setIntDimenAttr(int viewId, String methodName, int dimenAttr) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_INT, RES_DIMEN, dimenAttr, true));
	}

	public void setFloatDimen(int viewId, String methodName, int dimenResource) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_FLOAT, RES_DIMEN, dimenResource, false));
	}

	public void setFloatDimen(int viewId, String methodName, float value, int unit) {
		addAction(new ComplexUnitDimensionReflectionAction(viewId, methodName, T_FLOAT, value, unit));
	}

	public void setFloatDimenAttr(int viewId, String methodName, int dimenAttr) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_FLOAT, RES_DIMEN, dimenAttr, true));
	}

	public void setCharSequence(int viewId, String methodName, int stringResource) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_CHAR_SEQUENCE, RES_STRING, stringResource, false));
	}

	public void setCharSequenceAttr(int viewId, String methodName, int stringAttribute) {
		addAction(new ResourceReflectionAction(viewId, methodName, T_CHAR_SEQUENCE, RES_STRING, stringAttribute, true));
	}

	/* ---- Parcelable ---- */

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (mSizedRemoteViews != null) {
			dest.writeInt(MODE_HAS_SIZED_REMOTEVIEWS);
			dest.writeInt(mSizedRemoteViews.length);
			for (RemoteViews v : mSizedRemoteViews)
				v.writeToParcel(dest, flags);
		} else if (mLandscape != null) {
			dest.writeInt(MODE_HAS_LANDSCAPE_AND_PORTRAIT);
			mLandscape.writeToParcel(dest, flags);
			mPortrait.writeToParcel(dest, flags);
		} else {
			dest.writeInt(MODE_NORMAL);
			dest.writeString(mPackage);
			dest.writeInt(mLayoutId);
			dest.writeInt(mViewId);
			dest.writeInt(mIdealSize != null ? 1 : 0);
			if (mIdealSize != null) {
				dest.writeFloat(mIdealSize.getWidth());
				dest.writeFloat(mIdealSize.getHeight());
			}
			dest.writeInt(mActions.size());
			for (Action a : mActions) {
				dest.writeInt(a.tag());
				dest.writeInt(a.viewId);
				a.write(dest, flags);
			}
		}
	}

	public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
		@Override
		public RemoteViews createFromParcel(Parcel parcel) {
			return new RemoteViews(parcel);
		}

		@Override
		public RemoteViews[] newArray(int size) {
			return new RemoteViews[size];
		}
	};

	private static Action readAction(Parcel p) {
		int tag = p.readInt();
		int viewId = p.readInt();
		switch (tag) {
			case ReflectionAction.TAG:
				return new ReflectionAction(viewId, p);
			case NightModeReflectionAction.TAG:
				return new NightModeReflectionAction(viewId, p);
			case ResourceReflectionAction.TAG:
				return new ResourceReflectionAction(viewId, p);
			case ComplexUnitDimensionReflectionAction.TAG:
				return new ComplexUnitDimensionReflectionAction(viewId, p);
			case TextViewSizeAction.TAG:
				return new TextViewSizeAction(viewId, p.readInt(), p.readFloat());
			case TextViewDrawableAction.TAG: {
				boolean relative = p.readInt() != 0;
				if (p.readInt() != 0)
					throw new ActionException("TextViewDrawableAction with icons is not supported");
				return new TextViewDrawableAction(viewId, relative, p.readInt(), p.readInt(), p.readInt(), p.readInt());
			}
			case ViewContentNavigation.TAG:
				return new ViewContentNavigation(viewId, p.readInt() != 0);
			case ViewPaddingAction.TAG:
				return new ViewPaddingAction(viewId, p.readInt(), p.readInt(), p.readInt(), p.readInt());
			case LayoutParamAction.TAG:
				return new LayoutParamAction(viewId, p.readInt(), p.readInt(), p.readFloat(), p.readInt());
			case SetViewOutlinePreferredRadiusAction.TAG:
				return new SetViewOutlinePreferredRadiusAction(viewId, p.readInt(), p.readFloat(), p.readInt());
			case ViewGroupActionAdd.TAG:
				return new ViewGroupActionAdd(viewId, new RemoteViews(p), p.readInt(), p.readInt());
			case ViewGroupActionRemove.TAG:
				return new ViewGroupActionRemove(viewId);
			case SetEmptyView.TAG:
				return new SetEmptyView(viewId, p.readInt());
			case SetOnClickResponse.TAG:
				return new SetOnClickResponse(viewId, RemoteResponse.read(p));
			case SetOnCheckedChangeResponse.TAG:
				return new SetOnCheckedChangeResponse(viewId, RemoteResponse.read(p));
			case SetPendingIntentTemplate.TAG:
				return new SetPendingIntentTemplate(viewId, (PendingIntent)p.readParcelable(RemoteViews.class.getClassLoader()));
			case SetRemoteCollectionItemListAdapterAction.TAG:
				return new SetRemoteCollectionItemListAdapterAction(viewId, new RemoteCollectionItems(p));
			default:
				throw new ActionException("Tag " + tag + " not found");
		}
	}

	/* ---- actions ---- */

	private static abstract class Action {
		int viewId;

		abstract void apply(View root, ViewGroup rootParent);

		abstract int tag();

		abstract void write(Parcel dest, int flags);
	}

	/* AOSP BaseReflectionAction value types */
	private static final int T_BOOLEAN = 1;
	private static final int T_BYTE = 2;
	private static final int T_SHORT = 3;
	private static final int T_INT = 4;
	private static final int T_LONG = 5;
	private static final int T_FLOAT = 6;
	private static final int T_DOUBLE = 7;
	private static final int T_CHAR = 8;
	private static final int T_STRING = 9;
	private static final int T_CHAR_SEQUENCE = 10;
	private static final int T_URI = 11;
	private static final int T_BITMAP = 12;
	private static final int T_BUNDLE = 13;
	private static final int T_INTENT = 14;
	private static final int T_COLOR_STATE_LIST = 15;
	private static final int T_ICON = 16;
	private static final int T_BLEND_MODE = 17;

	private static Class<?> paramClass(int type) {
		switch (type) {
			case T_BOOLEAN: return boolean.class;
			case T_BYTE: return byte.class;
			case T_SHORT: return short.class;
			case T_INT: return int.class;
			case T_LONG: return long.class;
			case T_FLOAT: return float.class;
			case T_DOUBLE: return double.class;
			case T_CHAR: return char.class;
			case T_STRING: return String.class;
			case T_CHAR_SEQUENCE: return CharSequence.class;
			case T_URI: return Uri.class;
			case T_BITMAP: return Bitmap.class;
			case T_BUNDLE: return Bundle.class;
			case T_INTENT: return Intent.class;
			case T_COLOR_STATE_LIST: return ColorStateList.class;
			case T_ICON: return Icon.class;
			case T_BLEND_MODE: return BlendMode.class;
			default: return null;
		}
	}

	private static void writeValue(Parcel p, int type, Object v, int flags) {
		switch (type) {
			case T_BOOLEAN: p.writeInt((Boolean)v ? 1 : 0); break;
			case T_BYTE: p.writeByte((Byte)v); break;
			case T_SHORT: p.writeInt((Short)v); break;
			case T_INT: p.writeInt((Integer)v); break;
			case T_LONG: p.writeLong((Long)v); break;
			case T_FLOAT: p.writeFloat((Float)v); break;
			case T_DOUBLE: p.writeDouble((Double)v); break;
			case T_CHAR: p.writeInt((Character)v); break;
			case T_STRING: p.writeString((String)v); break;
			case T_CHAR_SEQUENCE: p.writeCharSequence((CharSequence)v); break;
			case T_BUNDLE: p.writeBundle((Bundle)v); break;
			case T_BLEND_MODE: p.writeInt(v == null ? -1 : BlendMode.toValue((BlendMode)v)); break;
			default: p.writeParcelable((Parcelable)v, flags); break;
		}
	}

	private static Object readValue(Parcel p, int type) {
		switch (type) {
			case T_BOOLEAN: return p.readInt() != 0;
			case T_BYTE: return p.readByte();
			case T_SHORT: return (short)p.readInt();
			case T_INT: return p.readInt();
			case T_LONG: return p.readLong();
			case T_FLOAT: return p.readFloat();
			case T_DOUBLE: return p.readDouble();
			case T_CHAR: return (char)p.readInt();
			case T_STRING: return p.readString();
			case T_CHAR_SEQUENCE: return p.readCharSequence();
			case T_BUNDLE: return p.readBundle();
			case T_BLEND_MODE: { int m = p.readInt(); return m == -1 ? null : BlendMode.fromValue(m); }
			default: return p.readParcelable(RemoteViews.class.getClassLoader());
		}
	}

	/* AOSP BaseReflectionAction.apply(): look the method up on the target view and call it */
	private static void invoke(View root, int viewId, String methodName, int type, Object value) {
		final View view = root.findViewById(viewId);
		if (view == null)
			return;
		Class<?> param = paramClass(type);
		Method method;
		try {
			method = view.getClass().getMethod(methodName, param);
		} catch (NoSuchMethodException e) {
			throw new ActionException("view: " + view.getClass().getName() + " doesn't have method: " + methodName + "(" + param.getName() + ")");
		}
		try {
			method.invoke(view, value);
		} catch (Throwable t) {
			throw new ActionException(t);
		}
	}

	private static final class ReflectionAction extends Action {
		static final int TAG = 2;
		final String methodName;
		final int type;
		final Object value;

		ReflectionAction(int viewId, String methodName, int type, Object value) {
			this.viewId = viewId;
			this.methodName = methodName;
			this.type = type;
			this.value = value;
		}

		ReflectionAction(int viewId, Parcel p) {
			this.viewId = viewId;
			methodName = p.readString();
			type = p.readInt();
			value = readValue(p, type);
		}

		void apply(View root, ViewGroup rootParent) {
			invoke(root, viewId, methodName, type, value);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeString(methodName);
			dest.writeInt(type);
			writeValue(dest, type, value, flags);
		}
	}

	/* AOSP NightModeReflectionAction: value chosen by the night bit of the view's configuration */
	private static final class NightModeReflectionAction extends Action {
		static final int TAG = 30;
		final String methodName;
		final int type;
		final Object lightValue;
		final Object darkValue;

		NightModeReflectionAction(int viewId, String methodName, int type, Object lightValue, Object darkValue) {
			this.viewId = viewId;
			this.methodName = methodName;
			this.type = type;
			this.lightValue = lightValue;
			this.darkValue = darkValue;
		}

		NightModeReflectionAction(int viewId, Parcel p) {
			this.viewId = viewId;
			methodName = p.readString();
			type = p.readInt();
			lightValue = readValue(p, type);
			darkValue = readValue(p, type);
		}

		void apply(View root, ViewGroup rootParent) {
			View view = root.findViewById(viewId);
			if (view == null)
				return;
			Configuration configuration = view.getResources().getConfiguration();
			boolean night = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
			invoke(root, viewId, methodName, type, night ? darkValue : lightValue);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeString(methodName);
			dest.writeInt(type);
			writeValue(dest, type, lightValue, flags);
			writeValue(dest, type, darkValue, flags);
		}
	}

	private static final int RES_DIMEN = 1;
	private static final int RES_COLOR = 2;
	private static final int RES_STRING = 3;

	/* AOSP ResourceReflectionAction and AttributeReflectionAction (attr = true): resolved at apply time */
	private static final class ResourceReflectionAction extends Action {
		static final int TAG = 32;
		final String methodName;
		final int type;
		final int resourceType;
		final int resId;
		final boolean attr;

		ResourceReflectionAction(int viewId, String methodName, int type, int resourceType, int resId, boolean attr) {
			this.viewId = viewId;
			this.methodName = methodName;
			this.type = type;
			this.resourceType = resourceType;
			this.resId = resId;
			this.attr = attr;
		}

		ResourceReflectionAction(int viewId, Parcel p) {
			this(viewId, p.readString(), p.readInt(), p.readInt(), p.readInt(), p.readInt() != 0);
		}

		void apply(View root, ViewGroup rootParent) {
			View view = root.findViewById(viewId);
			if (view == null)
				return;
			Object value;
			try {
				if (attr) {
					TypedArray a = view.getContext().obtainStyledAttributes(new int[] {resId});
					try {
						value = fromTypedArray(a);
					} finally {
						a.recycle();
					}
				} else {
					value = fromResources(view);
				}
			} catch (Throwable t) {
				throw new ActionException(t);
			}
			invoke(root, viewId, methodName, type, value);
		}

		private Object fromTypedArray(TypedArray a) {
			switch (resourceType) {
				case RES_COLOR:
					return type == T_INT ? (Object)a.getColor(0, 0) : a.getColorStateList(0);
				case RES_DIMEN:
					return type == T_INT ? (Object)a.getDimensionPixelSize(0, 0) : a.getDimension(0, 0);
				default:
					return a.getText(0);
			}
		}

		private Object fromResources(View view) {
			android.content.res.Resources res = view.getResources();
			switch (resourceType) {
				case RES_COLOR:
					if (type == T_INT)
						return resId == 0 ? 0 : res.getColor(resId, view.getContext().getTheme());
					return resId == 0 ? null : res.getColorStateList(resId, view.getContext().getTheme());
				case RES_DIMEN:
					if (type == T_INT)
						return resId == 0 ? 0 : res.getDimensionPixelSize(resId);
					return resId == 0 ? 0f : res.getDimension(resId);
				default:
					return resId == 0 ? null : res.getText(resId);
			}
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeString(methodName);
			dest.writeInt(type);
			dest.writeInt(resourceType);
			dest.writeInt(resId);
			dest.writeInt(attr ? 1 : 0);
		}
	}

	/* AOSP TypedValue.complexToDimensionPixelSize() rounding, applied to applyDimension(units, value) */
	private static int toPixelSize(float value, int units, DisplayMetrics metrics) {
		final float f = TypedValue.applyDimension(units, value, metrics);
		final int res = (int)((f >= 0) ? (f + 0.5f) : (f - 0.5f));
		if (res != 0)
			return res;
		if (value == 0)
			return 0;
		if (value > 0)
			return 1;
		return -1;
	}

	/* AOSP ComplexUnitDimensionReflectionAction */
	private static final class ComplexUnitDimensionReflectionAction extends Action {
		static final int TAG = 33;
		final String methodName;
		final int type;
		final float value;
		final int unit;

		ComplexUnitDimensionReflectionAction(int viewId, String methodName, int type, float value, int unit) {
			if (unit < TypedValue.COMPLEX_UNIT_PX || unit > TypedValue.COMPLEX_UNIT_MM)
				throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + unit);
			this.viewId = viewId;
			this.methodName = methodName;
			this.type = type;
			this.value = value;
			this.unit = unit;
		}

		ComplexUnitDimensionReflectionAction(int viewId, Parcel p) {
			this(viewId, p.readString(), p.readInt(), p.readFloat(), p.readInt());
		}

		void apply(View root, ViewGroup rootParent) {
			View view = root.findViewById(viewId);
			if (view == null)
				return;
			DisplayMetrics dm = view.getResources().getDisplayMetrics();
			Object v = type == T_INT ? (Object)toPixelSize(value, unit, dm) : TypedValue.applyDimension(unit, value, dm);
			invoke(root, viewId, methodName, type, v);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeString(methodName);
			dest.writeInt(type);
			dest.writeFloat(value);
			dest.writeInt(unit);
		}
	}

	/* AOSP TextViewSizeAction */
	private static final class TextViewSizeAction extends Action {
		static final int TAG = 13;
		final int units;
		final float size;

		TextViewSizeAction(int viewId, int units, float size) {
			this.viewId = viewId;
			this.units = units;
			this.size = size;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target instanceof TextView)
				((TextView)target).setTextSize(units, size);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(units);
			dest.writeFloat(size);
		}
	}

	/* AOSP TextViewDrawableAction (the variant with resource ids) */
	private static final class TextViewDrawableAction extends Action {
		static final int TAG = 11;
		final boolean isRelative;
		final int d1, d2, d3, d4;

		TextViewDrawableAction(int viewId, boolean isRelative, int d1, int d2, int d3, int d4) {
			this.viewId = viewId;
			this.isRelative = isRelative;
			this.d1 = d1;
			this.d2 = d2;
			this.d3 = d3;
			this.d4 = d4;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (!(target instanceof TextView))
				return;
			if (isRelative) {
				((TextView)target).setCompoundDrawablesRelativeWithIntrinsicBounds(d1, d2, d3, d4);
			} else {
				((TextView)target).setCompoundDrawablesWithIntrinsicBounds(d1, d2, d3, d4);
			}
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(isRelative ? 1 : 0);
			dest.writeInt(0); // useIcons
			dest.writeInt(d1);
			dest.writeInt(d2);
			dest.writeInt(d3);
			dest.writeInt(d4);
		}
	}

	/* AOSP ViewContentNavigation */
	private static final class ViewContentNavigation extends Action {
		static final int TAG = 5;
		final boolean mNext;

		ViewContentNavigation(int viewId, boolean next) {
			this.viewId = viewId;
			this.mNext = next;
		}

		void apply(View root, ViewGroup rootParent) {
			final View view = root.findViewById(viewId);
			if (view == null)
				return;
			String methodName = mNext ? "showNext" : "showPrevious";
			try {
				view.getClass().getMethod(methodName).invoke(view);
			} catch (NoSuchMethodException e) {
				throw new ActionException("view: " + view.getClass().getName() + " doesn't have method: " + methodName + "()");
			} catch (Throwable t) {
				throw new ActionException(t);
			}
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(mNext ? 1 : 0);
		}
	}

	/* AOSP ViewPaddingAction */
	private static final class ViewPaddingAction extends Action {
		static final int TAG = 14;
		final int left, top, right, bottom;

		ViewPaddingAction(int viewId, int left, int top, int right, int bottom) {
			this.viewId = viewId;
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target == null)
				return;
			target.setPadding(left, top, right, bottom);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(left);
			dest.writeInt(top);
			dest.writeInt(right);
			dest.writeInt(bottom);
		}
	}

	/* resolve a dimension stored as complex unit / resource / attribute (AOSP LayoutParamAction.getPixelSize) */
	private static float resolveDimension(View target, int valueType, float value, int units, boolean pixelSize) {
		try {
			switch (valueType) {
				case LayoutParamAction.VALUE_TYPE_ATTRIBUTE: {
					TypedArray a = target.getContext().obtainStyledAttributes(new int[] {(int)value});
					try {
						return pixelSize ? a.getDimensionPixelSize(0, 0) : a.getDimension(0, 0);
					} finally {
						a.recycle();
					}
				}
				case LayoutParamAction.VALUE_TYPE_RESOURCE:
					if ((int)value == 0)
						return 0;
					return pixelSize ? target.getResources().getDimensionPixelSize((int)value) : target.getResources().getDimension((int)value);
				case LayoutParamAction.VALUE_TYPE_COMPLEX_UNIT: {
					DisplayMetrics dm = target.getResources().getDisplayMetrics();
					return pixelSize ? toPixelSize(value, units, dm) : TypedValue.applyDimension(units, value, dm);
				}
				default:
					return value;
			}
		} catch (Throwable t) {
			throw new ActionException(t);
		}
	}

	/* AOSP LayoutParamAction */
	private static final class LayoutParamAction extends Action {
		static final int TAG = 19;
		static final int LAYOUT_WIDTH = 8;
		static final int LAYOUT_HEIGHT = 9;
		static final int VALUE_TYPE_RAW = 1;
		static final int VALUE_TYPE_COMPLEX_UNIT = 2;
		static final int VALUE_TYPE_RESOURCE = 3;
		static final int VALUE_TYPE_ATTRIBUTE = 4;
		final int property;
		final int valueType;
		final float value;
		final int units;

		LayoutParamAction(int viewId, int property, int valueType, float value, int units) {
			this.viewId = viewId;
			this.property = property;
			this.valueType = valueType;
			this.value = value;
			this.units = units;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target == null)
				return;
			ViewGroup.LayoutParams lp = target.getLayoutParams();
			if (lp == null)
				return;
			int px = (int)resolveDimension(target, valueType, value, units, property == LAYOUT_WIDTH || property == LAYOUT_HEIGHT);
			if (property != LAYOUT_WIDTH && property != LAYOUT_HEIGHT) {
				/* margins use AOSP getPixelOffset (truncation) */
				px = (int)resolveDimension(target, valueType, value, units, false);
				if (!(lp instanceof ViewGroup.MarginLayoutParams))
					return;
				ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams)lp;
				switch (property) {
					case MARGIN_LEFT: m.leftMargin = px; break;
					case MARGIN_TOP: m.topMargin = px; break;
					case MARGIN_RIGHT: m.rightMargin = px; break;
					case MARGIN_BOTTOM: m.bottomMargin = px; break;
					case MARGIN_START: m.setMarginStart(px); break;
					case MARGIN_END: m.setMarginEnd(px); break;
					default: throw new IllegalArgumentException("Unknown property " + property);
				}
			} else if (property == LAYOUT_WIDTH) {
				lp.width = px;
			} else {
				lp.height = px;
			}
			target.setLayoutParams(lp);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(property);
			dest.writeInt(valueType);
			dest.writeFloat(value);
			dest.writeInt(units);
		}
	}

	/* AOSP SetViewOutlinePreferredRadiusAction */
	private static final class SetViewOutlinePreferredRadiusAction extends Action {
		static final int TAG = 35;
		final int valueType;
		final float value;
		final int units;

		SetViewOutlinePreferredRadiusAction(int viewId, int valueType, float value, int units) {
			this.viewId = viewId;
			this.valueType = valueType;
			this.value = value;
			this.units = units;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target == null)
				return;
			target.setOutlineProvider(new RemoteViewOutlineProvider(resolveDimension(target, valueType, value, units, false)));
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(valueType);
			dest.writeFloat(value);
			dest.writeInt(units);
		}
	}

	/** AOSP RemoteViews.RemoteViewOutlineProvider */
	public static final class RemoteViewOutlineProvider extends ViewOutlineProvider {
		private final float mRadius;

		public RemoteViewOutlineProvider(float radius) {
			mRadius = radius;
		}

		public float getRadius() {
			return mRadius;
		}

		@Override
		public void getOutline(View view, Outline outline) {
			outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mRadius);
		}
	}

	/* AOSP ViewGroupActionAdd */
	private static final class ViewGroupActionAdd extends Action {
		static final int TAG = 4;
		final RemoteViews nestedViews;
		final int index;
		final int stableId;

		ViewGroupActionAdd(int viewId, RemoteViews nestedViews, int index, int stableId) {
			this.viewId = viewId;
			this.nestedViews = nestedViews;
			this.index = index;
			this.stableId = stableId;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (!(target instanceof ViewGroup))
				return;
			ViewGroup group = (ViewGroup)target;
			View child = nestedViews.apply(root.getContext(), group);
			if (index >= 0)
				group.addView(child, index);
			else
				group.addView(child);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			nestedViews.writeToParcel(dest, flags);
			dest.writeInt(index);
			dest.writeInt(stableId);
		}
	}

	/* AOSP ViewGroupActionRemove (removeAllViews) */
	private static final class ViewGroupActionRemove extends Action {
		static final int TAG = 7;

		ViewGroupActionRemove(int viewId) {
			this.viewId = viewId;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target instanceof ViewGroup)
				((ViewGroup)target).removeAllViews();
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {}
	}

	/* AOSP SetEmptyView */
	private static final class SetEmptyView extends Action {
		static final int TAG = 6;
		final int emptyViewId;

		SetEmptyView(int viewId, int emptyViewId) {
			this.viewId = viewId;
			this.emptyViewId = emptyViewId;
		}

		void apply(View root, ViewGroup rootParent) {
			final View view = root.findViewById(viewId);
			if (!(view instanceof AdapterView<?>))
				return;
			final View emptyView = root.findViewById(emptyViewId);
			if (emptyView == null)
				return;
			((AdapterView<?>)view).setEmptyView(emptyView);
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeInt(emptyViewId);
		}
	}

	/* AOSP SetOnClickResponse: a pending intent is sent on click; a fill-in intent is stored as tag for the
	 * template of the enclosing collection (AOSP R.id.fillInIntent) */
	private static final class SetOnClickResponse extends Action {
		static final int TAG = 1;
		final RemoteResponse response;

		SetOnClickResponse(int viewId, RemoteResponse response) {
			this.viewId = viewId;
			this.response = response;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target == null)
				return;
			if (response.mPendingIntent != null) {
				target.setOnClickListener(v -> response.send(v, null));
			} else if (response.mFillIntent != null) {
				target.setTag(com.android.internal.R.id.fillInIntent, response.mFillIntent);
			}
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			response.write(dest, flags);
		}
	}

	/* AOSP SetOnCheckedChangeResponse: EXTRA_CHECKED is added to the sent intent */
	private static final class SetOnCheckedChangeResponse extends Action {
		static final int TAG = 31;
		final RemoteResponse response;

		SetOnCheckedChangeResponse(int viewId, RemoteResponse response) {
			this.viewId = viewId;
			this.response = response;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (!(target instanceof CompoundButton))
				return;
			if (response.mPendingIntent != null) {
				((CompoundButton)target).setOnCheckedChangeListener((button, checked) -> response.send(button, new Intent().putExtra(EXTRA_CHECKED, checked)));
			} else if (response.mFillIntent != null) {
				target.setTag(com.android.internal.R.id.fillInIntent, response.mFillIntent);
			}
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			response.write(dest, flags);
		}
	}

	/* AOSP SetPendingIntentTemplate: clicks on collection items send the template with the item's fill-in intent */
	private static final class SetPendingIntentTemplate extends Action {
		static final int TAG = 8;
		final PendingIntent pendingIntentTemplate;

		SetPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate) {
			this.viewId = viewId;
			this.pendingIntentTemplate = pendingIntentTemplate;
		}

		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (!(target instanceof AdapterView<?>))
				return;
			((AdapterView<?>)target).setOnItemClickListener((parent, view, position, id) -> {
				Intent fillIn = findFillInIntent(view);
				if (fillIn == null || pendingIntentTemplate == null)
					return;
				try {
					pendingIntentTemplate.send(view.getContext(), 0, fillIn);
				} catch (PendingIntent.CanceledException e) {
					android.util.Log.e("RemoteViews", "Cannot send pending intent: ", e);
				}
			});
		}

		/* AOSP: depth-first search for the first view carrying a fill-in intent */
		private static Intent findFillInIntent(View v) {
			Object tag = v.getTag(com.android.internal.R.id.fillInIntent);
			if (tag instanceof Intent)
				return (Intent)tag;
			if (v instanceof ViewGroup) {
				ViewGroup g = (ViewGroup)v;
				for (int i = 0; i < g.getChildCount(); i++) {
					Intent r = findFillInIntent(g.getChildAt(i));
					if (r != null)
						return r;
				}
			}
			return null;
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			dest.writeParcelable(pendingIntentTemplate, flags);
		}
	}

	/* AOSP SetRemoteCollectionItemListAdapterAction (adapter: AOSP RemoteCollectionItemsAdapter) */
	private static final class SetRemoteCollectionItemListAdapterAction extends Action {
		static final int TAG = 37;
		final RemoteCollectionItems items;

		SetRemoteCollectionItemListAdapterAction(int viewId, RemoteCollectionItems items) {
			this.viewId = viewId;
			this.items = items;
		}

		@SuppressWarnings("unchecked")
		void apply(View root, ViewGroup rootParent) {
			final View target = root.findViewById(viewId);
			if (target == null)
				return;
			if (!(target instanceof AdapterView)) {
				android.util.Log.e("RemoteViews", "Cannot call setRemoteAdapter on a view which is not an AdapterView (id: " + viewId + ")");
				return;
			}
			((AdapterView<Adapter>)target).setAdapter(new CollectionAdapter(items));
		}

		int tag() { return TAG; }

		void write(Parcel dest, int flags) {
			items.writeToParcel(dest, flags);
		}
	}

	private static final class CollectionAdapter extends BaseAdapter {
		private final RemoteCollectionItems items;
		private final int[] layoutIds;

		CollectionAdapter(RemoteCollectionItems items) {
			this.items = items;
			layoutIds = Arrays.stream(items.mViews).mapToInt(RemoteViews::getLayoutId).distinct().toArray();
		}

		public int getCount() { return items.getItemCount(); }

		public Object getItem(int position) { return items.getItemView(position); }

		public long getItemId(int position) { return items.getItemId(position); }

		public boolean hasStableIds() { return items.hasStableIds(); }

		public int getViewTypeCount() { return items.getViewTypeCount(); }

		public int getItemViewType(int position) {
			int layout = items.getItemView(position).getLayoutId();
			for (int i = 0; i < layoutIds.length; i++)
				if (layoutIds[i] == layout)
					return i;
			return 0;
		}

		/* AOSP reapplies a recycled view of the same layout; ATL inflates each item anew (same result) */
		public View getView(int position, View convertView, ViewGroup parent) {
			return items.getItemView(position).apply(parent.getContext(), parent);
		}
	}

	/** AOSP RemoteViews.RemoteResponse */
	public static class RemoteResponse {
		public static final int INTERACTION_TYPE_CLICK = 0;
		public static final int INTERACTION_TYPE_CHECKED_CHANGE = 1;

		private PendingIntent mPendingIntent;
		private Intent mFillIntent;
		private int mInteractionType = INTERACTION_TYPE_CLICK;
		private ArrayList<Integer> mElementIds;
		private ArrayList<String> mElementNames;

		public static RemoteResponse fromPendingIntent(PendingIntent pendingIntent) {
			RemoteResponse response = new RemoteResponse();
			response.mPendingIntent = pendingIntent;
			return response;
		}

		public static RemoteResponse fromFillInIntent(Intent fillIntent) {
			RemoteResponse response = new RemoteResponse();
			response.mFillIntent = fillIntent;
			return response;
		}

		public RemoteResponse addSharedElement(int viewId, String sharedElementName) {
			if (mElementIds == null) {
				mElementIds = new ArrayList<>();
				mElementNames = new ArrayList<>();
			}
			mElementIds.add(viewId);
			mElementNames.add(sharedElementName);
			return this;
		}

		public RemoteResponse setInteractionType(int type) {
			mInteractionType = type;
			return this;
		}

		/* AOSP RemoteViews.startPendingIntent() without activity options */
		void send(View view, Intent fillInIntent) {
			try {
				mPendingIntent.send(view.getContext(), 0, fillInIntent);
			} catch (PendingIntent.CanceledException e) {
				android.util.Log.e("RemoteViews", "Cannot send pending intent: ", e);
			}
		}

		void write(Parcel dest, int flags) {
			dest.writeInt(mInteractionType);
			dest.writeParcelable(mPendingIntent, flags);
			dest.writeParcelable(mFillIntent, flags);
			dest.writeInt(mElementIds == null ? 0 : mElementIds.size());
			if (mElementIds != null)
				for (int i = 0; i < mElementIds.size(); i++) {
					dest.writeInt(mElementIds.get(i));
					dest.writeString(mElementNames.get(i));
				}
		}

		static RemoteResponse read(Parcel p) {
			RemoteResponse r = new RemoteResponse();
			r.mInteractionType = p.readInt();
			r.mPendingIntent = (PendingIntent)p.readParcelable(RemoteViews.class.getClassLoader());
			r.mFillIntent = (Intent)p.readParcelable(RemoteViews.class.getClassLoader());
			int n = p.readInt();
			for (int i = 0; i < n; i++)
				r.addSharedElement(p.readInt(), p.readString());
			return r;
		}
	}

	/** AOSP RemoteViews.RemoteCollectionItems */
	public static final class RemoteCollectionItems implements Parcelable {
		private final long[] mIds;
		private final RemoteViews[] mViews;
		private final boolean mHasStableIds;
		private final int mViewTypeCount;

		RemoteCollectionItems(long[] ids, RemoteViews[] views, boolean hasStableIds, int viewTypeCount) {
			mIds = ids;
			mViews = views;
			mHasStableIds = hasStableIds;
			mViewTypeCount = viewTypeCount;
			if (ids.length != views.length) {
				throw new IllegalArgumentException("RemoteCollectionItems has different number of ids and views");
			}
			if (viewTypeCount < 1) {
				throw new IllegalArgumentException("View type count must be >= 1");
			}
			int layoutIdCount = (int)Arrays.stream(views).mapToInt(RemoteViews::getLayoutId).distinct().count();
			if (layoutIdCount > viewTypeCount) {
				throw new IllegalArgumentException("View type count is set to " + viewTypeCount + ", but the collection contains " + layoutIdCount + " different layout ids");
			}
		}

		RemoteCollectionItems(Parcel in) {
			mHasStableIds = in.readInt() != 0;
			mViewTypeCount = in.readInt();
			int length = in.readInt();
			mIds = new long[length];
			mViews = new RemoteViews[length];
			for (int i = 0; i < length; i++) {
				mIds[i] = in.readLong();
				mViews[i] = new RemoteViews(in);
			}
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(mHasStableIds ? 1 : 0);
			dest.writeInt(mViewTypeCount);
			dest.writeInt(mIds.length);
			for (int i = 0; i < mIds.length; i++) {
				dest.writeLong(mIds[i]);
				mViews[i].writeToParcel(dest, flags);
			}
		}

		public long getItemId(int position) {
			return mIds[position];
		}

		public RemoteViews getItemView(int position) {
			return mViews[position];
		}

		public int getItemCount() {
			return mIds.length;
		}

		public int getViewTypeCount() {
			return mViewTypeCount;
		}

		public boolean hasStableIds() {
			return mHasStableIds;
		}

		public static final Parcelable.Creator<RemoteCollectionItems> CREATOR = new Parcelable.Creator<RemoteCollectionItems>() {
			@Override
			public RemoteCollectionItems createFromParcel(Parcel source) {
				return new RemoteCollectionItems(source);
			}

			@Override
			public RemoteCollectionItems[] newArray(int size) {
				return new RemoteCollectionItems[size];
			}
		};

		/** AOSP RemoteCollectionItems.Builder */
		public static final class Builder {
			private final ArrayList<Long> mIds = new ArrayList<>();
			private final ArrayList<RemoteViews> mViews = new ArrayList<>();
			private boolean mHasStableIds;
			private int mViewTypeCount;

			public Builder addItem(long id, RemoteViews view) {
				if (view == null)
					throw new NullPointerException();
				if (view.hasMultipleLayouts()) {
					throw new IllegalArgumentException("RemoteViews used in a RemoteCollectionItems cannot specify separate layouts for orientations or sizes.");
				}
				mIds.add(id);
				mViews.add(view);
				return this;
			}

			public Builder setHasStableIds(boolean hasStableIds) {
				mHasStableIds = hasStableIds;
				return this;
			}

			public Builder setViewTypeCount(int viewTypeCount) {
				mViewTypeCount = viewTypeCount;
				return this;
			}

			public RemoteCollectionItems build() {
				if (mViewTypeCount < 1) {
					mViewTypeCount = (int)mViews.stream().mapToInt(RemoteViews::getLayoutId).distinct().count();
				}
				long[] ids = new long[mIds.size()];
				for (int i = 0; i < ids.length; i++)
					ids[i] = mIds.get(i);
				return new RemoteCollectionItems(ids, mViews.toArray(new RemoteViews[0]), mHasStableIds, Math.max(mViewTypeCount, 1));
			}
		}
	}
}
