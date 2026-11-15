package android.widget;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

/* AOSP frameworks/base core/java/android/widget/Toolbar.java (Android 16): a ViewGroup that lays out the
 * navigation button, logo, title/subtitle and custom child views like AOSP (constructor attributes,
 * onMeasure, onLayout and their helpers taken over).
 * Not taken over, because ATL has no ActionMenuView/MenuBuilder: the action menu (getMenu, inflateMenu,
 * setOnMenuItemClickListener, overflow menu) and expanded action views with the collapse button. The
 * overflow icon is only stored (AOSP keeps it in the ActionMenuView). */
public class Toolbar extends ViewGroup {
	private static final String TAG = "Toolbar";

	private TextView mTitleTextView;
	private TextView mSubtitleTextView;
	private ImageButton mNavButtonView;
	private ImageView mLogoView;

	private Drawable mCollapseIcon;
	private CharSequence mCollapseDescription;

	private Context mPopupContext;
	private int mPopupTheme;

	private int mTitleTextAppearance;
	private int mSubtitleTextAppearance;
	private int mNavButtonStyle;

	private int mButtonGravity;

	private int mMaxButtonHeight;

	private int mTitleMarginStart;
	private int mTitleMarginEnd;
	private int mTitleMarginTop;
	private int mTitleMarginBottom;

	private RtlSpacingHelper mContentInsets;
	private int mContentInsetStartWithNavigation;
	private int mContentInsetEndWithActions;

	private int mGravity = Gravity.START | Gravity.CENTER_VERTICAL;

	private CharSequence mTitleText;
	private CharSequence mSubtitleText;

	private int mTitleTextColor;
	private int mSubtitleTextColor;

	private boolean mEatingTouch;

	// Clear me after use.
	private final ArrayList<View> mTempViews = new ArrayList<View>();

	// Used to hold views that will be removed while we have an expanded action view.
	private final ArrayList<View> mHiddenViews = new ArrayList<>();

	private final int[] mTempMargins = new int[2];

	/* AOSP keeps the overflow icon in the ActionMenuView, which ATL does not have */
	private Drawable mOverflowIcon;

	private boolean mCollapsible;

	public Toolbar(Context context) {
		this(context, null);
	}

	public Toolbar(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.toolbarStyle);
	}

	public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Toolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Toolbar,
				defStyleAttr, defStyleRes);
		saveAttributeDataForStyleable(context, R.styleable.Toolbar,
				attrs, a, defStyleAttr, defStyleRes);

		mTitleTextAppearance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, 0);
		mSubtitleTextAppearance = a.getResourceId(R.styleable.Toolbar_subtitleTextAppearance, 0);
		mNavButtonStyle = a.getResourceId(R.styleable.Toolbar_navigationButtonStyle, 0);
		mGravity = a.getInteger(R.styleable.Toolbar_gravity, mGravity);
		mButtonGravity = a.getInteger(R.styleable.Toolbar_buttonGravity, Gravity.TOP);
		mTitleMarginStart = mTitleMarginEnd = mTitleMarginTop = mTitleMarginBottom =
				a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargin, 0);

		final int marginStart = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginStart, -1);
		if (marginStart >= 0) {
			mTitleMarginStart = marginStart;
		}

		final int marginEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginEnd, -1);
		if (marginEnd >= 0) {
			mTitleMarginEnd = marginEnd;
		}

		final int marginTop = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginTop, -1);
		if (marginTop >= 0) {
			mTitleMarginTop = marginTop;
		}

		final int marginBottom = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginBottom,
				-1);
		if (marginBottom >= 0) {
			mTitleMarginBottom = marginBottom;
		}

		mMaxButtonHeight = a.getDimensionPixelSize(R.styleable.Toolbar_maxButtonHeight, -1);

		final int contentInsetStart =
				a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetStart,
						RtlSpacingHelper.UNDEFINED);
		final int contentInsetEnd =
				a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEnd,
						RtlSpacingHelper.UNDEFINED);
		final int contentInsetLeft =
				a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetLeft, 0);
		final int contentInsetRight =
				a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetRight, 0);

		ensureContentInsets();
		mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);

		if (contentInsetStart != RtlSpacingHelper.UNDEFINED ||
				contentInsetEnd != RtlSpacingHelper.UNDEFINED) {
			mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
		}

		mContentInsetStartWithNavigation = a.getDimensionPixelOffset(
				R.styleable.Toolbar_contentInsetStartWithNavigation, RtlSpacingHelper.UNDEFINED);
		mContentInsetEndWithActions = a.getDimensionPixelOffset(
				R.styleable.Toolbar_contentInsetEndWithActions, RtlSpacingHelper.UNDEFINED);

		mCollapseIcon = a.getDrawable(R.styleable.Toolbar_collapseIcon);
		mCollapseDescription = a.getText(R.styleable.Toolbar_collapseContentDescription);

		final CharSequence title = a.getText(R.styleable.Toolbar_title);
		if (!TextUtils.isEmpty(title)) {
			setTitle(title);
		}

		final CharSequence subtitle = a.getText(R.styleable.Toolbar_subtitle);
		if (!TextUtils.isEmpty(subtitle)) {
			setSubtitle(subtitle);
		}

		// Set the default context, since setPopupTheme() may be a no-op.
		mPopupContext = getContext();
		setPopupTheme(a.getResourceId(R.styleable.Toolbar_popupTheme, 0));

		final Drawable navIcon = a.getDrawable(R.styleable.Toolbar_navigationIcon);
		if (navIcon != null) {
			setNavigationIcon(navIcon);
		}

		final CharSequence navDesc = a.getText(
				R.styleable.Toolbar_navigationContentDescription);
		if (!TextUtils.isEmpty(navDesc)) {
			setNavigationContentDescription(navDesc);
		}

		final Drawable logo = a.getDrawable(R.styleable.Toolbar_logo);
		if (logo != null) {
			setLogo(logo);
		}

		final CharSequence logoDesc = a.getText(R.styleable.Toolbar_logoDescription);
		if (!TextUtils.isEmpty(logoDesc)) {
			setLogoDescription(logoDesc);
		}

		if (a.hasValue(R.styleable.Toolbar_titleTextColor)) {
			setTitleTextColor(a.getColor(R.styleable.Toolbar_titleTextColor, 0xffffffff));
		}

		if (a.hasValue(R.styleable.Toolbar_subtitleTextColor)) {
			setSubtitleTextColor(a.getColor(R.styleable.Toolbar_subtitleTextColor, 0xffffffff));
		}
		a.recycle();
	}

	public void setPopupTheme(int resId) {
		if (mPopupTheme != resId) {
			mPopupTheme = resId;
			if (resId == 0) {
				mPopupContext = getContext();
			} else {
				mPopupContext = new ContextThemeWrapper(getContext(), resId);
			}
		}
	}

	public int getPopupTheme() {
		return mPopupTheme;
	}

	public void setTitleMargin(int start, int top, int end, int bottom) {
		mTitleMarginStart = start;
		mTitleMarginTop = top;
		mTitleMarginEnd = end;
		mTitleMarginBottom = bottom;

		requestLayout();
	}

	public int getTitleMarginStart() {
		return mTitleMarginStart;
	}

	public void setTitleMarginStart(int margin) {
		mTitleMarginStart = margin;

		requestLayout();
	}

	public int getTitleMarginTop() {
		return mTitleMarginTop;
	}

	public void setTitleMarginTop(int margin) {
		mTitleMarginTop = margin;

		requestLayout();
	}

	public int getTitleMarginEnd() {
		return mTitleMarginEnd;
	}

	public void setTitleMarginEnd(int margin) {
		mTitleMarginEnd = margin;

		requestLayout();
	}

	public int getTitleMarginBottom() {
		return mTitleMarginBottom;
	}

	public void setTitleMarginBottom(int margin) {
		mTitleMarginBottom = margin;
		requestLayout();
	}

	@Override
	public void onRtlPropertiesChanged(int layoutDirection) {
		super.onRtlPropertiesChanged(layoutDirection);
		ensureContentInsets();
		mContentInsets.setDirection(layoutDirection == LAYOUT_DIRECTION_RTL);
	}

	public void setLogo(int resId) {
		setLogo(getContext().getDrawable(resId));
	}

	/* AOSP: without an ActionMenuView there is no overflow menu */
	public boolean canShowOverflowMenu() {
		return false;
	}

	public boolean isOverflowMenuShowing() {
		return false;
	}

	public boolean showOverflowMenu() {
		return false;
	}

	public boolean hideOverflowMenu() {
		return false;
	}

	public void dismissPopupMenus() {}

	public boolean isTitleTruncated() {
		if (mTitleTextView == null) {
			return false;
		}

		final Layout titleLayout = mTitleTextView.getLayout();
		if (titleLayout == null) {
			return false;
		}

		final int lineCount = titleLayout.getLineCount();
		for (int i = 0; i < lineCount; i++) {
			if (titleLayout.getEllipsisCount(i) > 0) {
				return true;
			}
		}
		return false;
	}

	public void setLogo(Drawable drawable) {
		if (drawable != null) {
			ensureLogoView();
			if (!isChildOrHidden(mLogoView)) {
				addSystemView(mLogoView, true);
			}
		} else if (mLogoView != null && isChildOrHidden(mLogoView)) {
			removeView(mLogoView);
			mHiddenViews.remove(mLogoView);
		}
		if (mLogoView != null) {
			mLogoView.setImageDrawable(drawable);
		}
	}

	public Drawable getLogo() {
		return mLogoView != null ? mLogoView.getDrawable() : null;
	}

	public void setLogoDescription(int resId) {
		setLogoDescription(getContext().getText(resId));
	}

	public void setLogoDescription(CharSequence description) {
		if (!TextUtils.isEmpty(description)) {
			ensureLogoView();
		}
		if (mLogoView != null) {
			mLogoView.setContentDescription(description);
		}
	}

	public CharSequence getLogoDescription() {
		return mLogoView != null ? mLogoView.getContentDescription() : null;
	}

	private void ensureLogoView() {
		if (mLogoView == null) {
			mLogoView = new ImageView(getContext());
		}
	}

	/* AOSP: no expanded action view without the menu */
	public boolean hasExpandedActionView() {
		return false;
	}

	public void collapseActionView() {}

	public CharSequence getTitle() {
		return mTitleText;
	}

	public void setTitle(int resId) {
		setTitle(getContext().getText(resId));
	}

	public void setTitle(CharSequence title) {
		if (!TextUtils.isEmpty(title)) {
			if (mTitleTextView == null) {
				final Context context = getContext();
				mTitleTextView = new TextView(context);
				mTitleTextView.setSingleLine();
				mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
				if (mTitleTextAppearance != 0) {
					mTitleTextView.setTextAppearance(mTitleTextAppearance);
				}
				if (mTitleTextColor != 0) {
					mTitleTextView.setTextColor(mTitleTextColor);
				}
			}
			if (!isChildOrHidden(mTitleTextView)) {
				addSystemView(mTitleTextView, true);
			}
		} else if (mTitleTextView != null && isChildOrHidden(mTitleTextView)) {
			removeView(mTitleTextView);
			mHiddenViews.remove(mTitleTextView);
		}
		if (mTitleTextView != null) {
			mTitleTextView.setText(title);
		}
		mTitleText = title;
	}

	public CharSequence getSubtitle() {
		return mSubtitleText;
	}

	public void setSubtitle(int resId) {
		setSubtitle(getContext().getText(resId));
	}

	public void setSubtitle(CharSequence subtitle) {
		if (!TextUtils.isEmpty(subtitle)) {
			if (mSubtitleTextView == null) {
				final Context context = getContext();
				mSubtitleTextView = new TextView(context);
				mSubtitleTextView.setSingleLine();
				mSubtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
				if (mSubtitleTextAppearance != 0) {
					mSubtitleTextView.setTextAppearance(mSubtitleTextAppearance);
				}
				if (mSubtitleTextColor != 0) {
					mSubtitleTextView.setTextColor(mSubtitleTextColor);
				}
			}
			if (!isChildOrHidden(mSubtitleTextView)) {
				addSystemView(mSubtitleTextView, true);
			}
		} else if (mSubtitleTextView != null && isChildOrHidden(mSubtitleTextView)) {
			removeView(mSubtitleTextView);
			mHiddenViews.remove(mSubtitleTextView);
		}
		if (mSubtitleTextView != null) {
			mSubtitleTextView.setText(subtitle);
		}
		mSubtitleText = subtitle;
	}

	public void setTitleTextAppearance(Context context, int resId) {
		mTitleTextAppearance = resId;
		if (mTitleTextView != null) {
			mTitleTextView.setTextAppearance(resId);
		}
	}

	public void setSubtitleTextAppearance(Context context, int resId) {
		mSubtitleTextAppearance = resId;
		if (mSubtitleTextView != null) {
			mSubtitleTextView.setTextAppearance(resId);
		}
	}

	public void setTitleTextColor(int color) {
		mTitleTextColor = color;
		if (mTitleTextView != null) {
			mTitleTextView.setTextColor(color);
		}
	}

	public void setSubtitleTextColor(int color) {
		mSubtitleTextColor = color;
		if (mSubtitleTextView != null) {
			mSubtitleTextView.setTextColor(color);
		}
	}

	public CharSequence getNavigationContentDescription() {
		return mNavButtonView != null ? mNavButtonView.getContentDescription() : null;
	}

	public void setNavigationContentDescription(int resId) {
		setNavigationContentDescription(resId != 0 ? getContext().getText(resId) : null);
	}

	public void setNavigationContentDescription(CharSequence description) {
		if (!TextUtils.isEmpty(description)) {
			ensureNavButtonView();
		}
		if (mNavButtonView != null) {
			mNavButtonView.setContentDescription(description);
		}
	}

	public void setNavigationIcon(int resId) {
		setNavigationIcon(getContext().getDrawable(resId));
	}

	public void setNavigationIcon(Drawable icon) {
		if (icon != null) {
			ensureNavButtonView();
			if (!isChildOrHidden(mNavButtonView)) {
				addSystemView(mNavButtonView, true);
			}
		} else if (mNavButtonView != null && isChildOrHidden(mNavButtonView)) {
			removeView(mNavButtonView);
			mHiddenViews.remove(mNavButtonView);
		}
		if (mNavButtonView != null) {
			mNavButtonView.setImageDrawable(icon);
		}
	}

	public Drawable getNavigationIcon() {
		return mNavButtonView != null ? mNavButtonView.getDrawable() : null;
	}

	public void setNavigationOnClickListener(OnClickListener listener) {
		ensureNavButtonView();
		mNavButtonView.setOnClickListener(listener);
	}

	public View getNavigationView() {
		return mNavButtonView;
	}

	/* AOSP collapse button: only shown with an expanded action view (menu), so ATL keeps the values */
	public CharSequence getCollapseContentDescription() {
		return mCollapseDescription;
	}

	public void setCollapseContentDescription(int resId) {
		setCollapseContentDescription(resId != 0 ? getContext().getText(resId) : null);
	}

	public void setCollapseContentDescription(CharSequence description) {
		mCollapseDescription = description;
	}

	public Drawable getCollapseIcon() {
		return mCollapseIcon;
	}

	public void setCollapseIcon(int resId) {
		setCollapseIcon(getContext().getDrawable(resId));
	}

	public void setCollapseIcon(Drawable icon) {
		if (icon != null) {
			mCollapseIcon = icon;
		}
	}

	public void setOverflowIcon(Drawable icon) {
		mOverflowIcon = icon;
	}

	public Drawable getOverflowIcon() {
		return mOverflowIcon;
	}

	public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
		ensureContentInsets();
		mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
	}

	public int getContentInsetStart() {
		return mContentInsets != null ? mContentInsets.getStart() : 0;
	}

	public int getContentInsetEnd() {
		return mContentInsets != null ? mContentInsets.getEnd() : 0;
	}

	public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight) {
		ensureContentInsets();
		mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
	}

	public int getContentInsetLeft() {
		return mContentInsets != null ? mContentInsets.getLeft() : 0;
	}

	public int getContentInsetRight() {
		return mContentInsets != null ? mContentInsets.getRight() : 0;
	}

	public int getContentInsetStartWithNavigation() {
		return mContentInsetStartWithNavigation != RtlSpacingHelper.UNDEFINED
				? mContentInsetStartWithNavigation
				: getContentInsetStart();
	}

	public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
		if (insetStartWithNavigation < 0) {
			insetStartWithNavigation = RtlSpacingHelper.UNDEFINED;
		}
		if (insetStartWithNavigation != mContentInsetStartWithNavigation) {
			mContentInsetStartWithNavigation = insetStartWithNavigation;
			if (getNavigationIcon() != null) {
				requestLayout();
			}
		}
	}

	public int getContentInsetEndWithActions() {
		return mContentInsetEndWithActions != RtlSpacingHelper.UNDEFINED
				? mContentInsetEndWithActions
				: getContentInsetEnd();
	}

	public void setContentInsetEndWithActions(int insetEndWithActions) {
		if (insetEndWithActions < 0) {
			insetEndWithActions = RtlSpacingHelper.UNDEFINED;
		}
		if (insetEndWithActions != mContentInsetEndWithActions) {
			mContentInsetEndWithActions = insetEndWithActions;
			if (getNavigationIcon() != null) {
				requestLayout();
			}
		}
	}

	public int getCurrentContentInsetStart() {
		return getNavigationIcon() != null
				? Math.max(getContentInsetStart(), Math.max(mContentInsetStartWithNavigation, 0))
				: getContentInsetStart();
	}

	/* AOSP adds the actions inset only when the menu has visible items; ATL has no menu */
	public int getCurrentContentInsetEnd() {
		return getContentInsetEnd();
	}

	public int getCurrentContentInsetLeft() {
		return isLayoutRtl()
				? getCurrentContentInsetEnd()
				: getCurrentContentInsetStart();
	}

	public int getCurrentContentInsetRight() {
		return isLayoutRtl()
				? getCurrentContentInsetStart()
				: getCurrentContentInsetEnd();
	}

	private void ensureNavButtonView() {
		if (mNavButtonView == null) {
			mNavButtonView = new ImageButton(getContext(), null, 0, mNavButtonStyle);
			final LayoutParams lp = generateDefaultLayoutParams();
			lp.gravity = Gravity.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
			mNavButtonView.setLayoutParams(lp);
		}
	}

	private void addSystemView(View v, boolean allowHide) {
		final ViewGroup.LayoutParams vlp = v.getLayoutParams();
		final LayoutParams lp;
		if (vlp == null) {
			lp = generateDefaultLayoutParams();
		} else if (!checkLayoutParams(vlp)) {
			lp = generateLayoutParams(vlp);
		} else {
			lp = (LayoutParams)vlp;
		}
		lp.mViewType = LayoutParams.SYSTEM;
		addView(v, lp);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Toolbars always eat touch events, but should still respect the touch event dispatch
		// contract. If the normal View implementation doesn't want the events, we'll just silently
		// eat the rest of the gesture without reporting the events to the default implementation
		// since that's what it expects.

		final int action = ev.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			mEatingTouch = false;
		}

		if (!mEatingTouch) {
			final boolean handled = super.onTouchEvent(ev);
			if (action == MotionEvent.ACTION_DOWN && !handled) {
				mEatingTouch = true;
			}
		}

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			mEatingTouch = false;
		}

		return true;
	}

	private void measureChildConstrained(View child, int parentWidthSpec, int widthUsed,
			int parentHeightSpec, int heightUsed, int heightConstraint) {
		final MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();

		int childWidthSpec = getChildMeasureSpec(parentWidthSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
						+ widthUsed, lp.width);
		int childHeightSpec = getChildMeasureSpec(parentHeightSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
						+ heightUsed, lp.height);

		final int childHeightMode = MeasureSpec.getMode(childHeightSpec);
		if (childHeightMode != MeasureSpec.EXACTLY && heightConstraint >= 0) {
			final int size = childHeightMode != MeasureSpec.UNSPECIFIED ?
					Math.min(MeasureSpec.getSize(childHeightSpec), heightConstraint) :
					heightConstraint;
			childHeightSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	/**
	 * Returns the width + uncollapsed margins
	 */
	private int measureChildCollapseMargins(View child,
			int parentWidthMeasureSpec, int widthUsed,
			int parentHeightMeasureSpec, int heightUsed, int[] collapsingMargins) {
		final MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();

		final int leftDiff = lp.leftMargin - collapsingMargins[0];
		final int rightDiff = lp.rightMargin - collapsingMargins[1];
		final int leftMargin = Math.max(0, leftDiff);
		final int rightMargin = Math.max(0, rightDiff);
		final int hMargins = leftMargin + rightMargin;
		collapsingMargins[0] = Math.max(0, -leftDiff);
		collapsingMargins[1] = Math.max(0, -rightDiff);

		final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + hMargins + widthUsed, lp.width);
		final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
						+ heightUsed, lp.height);

		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
		return child.getMeasuredWidth() + hMargins;
	}

	/**
	 * Returns true if the Toolbar is collapsible and has no child views with a measured size > 0.
	 */
	private boolean shouldCollapse() {
		if (!mCollapsible)
			return false;

		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			if (shouldLayout(child) && child.getMeasuredWidth() > 0 &&
					child.getMeasuredHeight() > 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = 0;
		int height = 0;
		int childState = 0;

		final int[] collapsingMargins = mTempMargins;
		final int marginStartIndex;
		final int marginEndIndex;
		if (isLayoutRtl()) {
			marginStartIndex = 1;
			marginEndIndex = 0;
		} else {
			marginStartIndex = 0;
			marginEndIndex = 1;
		}

		// System views measure first.

		int navWidth = 0;
		if (shouldLayout(mNavButtonView)) {
			measureChildConstrained(mNavButtonView, widthMeasureSpec, width, heightMeasureSpec, 0,
					mMaxButtonHeight);
			navWidth = mNavButtonView.getMeasuredWidth() + getHorizontalMargins(mNavButtonView);
			height = Math.max(height, mNavButtonView.getMeasuredHeight() +
					getVerticalMargins(mNavButtonView));
			childState = combineMeasuredStates(childState, mNavButtonView.getMeasuredState());
		}

		final int contentInsetStart = getCurrentContentInsetStart();
		width += Math.max(contentInsetStart, navWidth);
		collapsingMargins[marginStartIndex] = Math.max(0, contentInsetStart - navWidth);

		/* AOSP measures the ActionMenuView here; ATL has none (menuWidth stays 0) */
		final int contentInsetEnd = getCurrentContentInsetEnd();
		width += contentInsetEnd;
		collapsingMargins[marginEndIndex] = Math.max(0, contentInsetEnd);

		if (shouldLayout(mLogoView)) {
			width += measureChildCollapseMargins(mLogoView, widthMeasureSpec, width,
					heightMeasureSpec, 0, collapsingMargins);
			height = Math.max(height, mLogoView.getMeasuredHeight() +
					getVerticalMargins(mLogoView));
			childState = combineMeasuredStates(childState, mLogoView.getMeasuredState());
		}

		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams)child.getLayoutParams();
			if (lp.mViewType != LayoutParams.CUSTOM || !shouldLayout(child)) {
				// We already got all system views above. Skip them and GONE views.
				continue;
			}

			width += measureChildCollapseMargins(child, widthMeasureSpec, width,
					heightMeasureSpec, 0, collapsingMargins);
			height = Math.max(height, child.getMeasuredHeight() + getVerticalMargins(child));
			childState = combineMeasuredStates(childState, child.getMeasuredState());
		}

		int titleWidth = 0;
		int titleHeight = 0;
		final int titleVertMargins = mTitleMarginTop + mTitleMarginBottom;
		final int titleHorizMargins = mTitleMarginStart + mTitleMarginEnd;
		if (shouldLayout(mTitleTextView)) {
			titleWidth = measureChildCollapseMargins(mTitleTextView, widthMeasureSpec,
					width + titleHorizMargins, heightMeasureSpec, titleVertMargins,
					collapsingMargins);
			titleWidth = mTitleTextView.getMeasuredWidth() + getHorizontalMargins(mTitleTextView);
			titleHeight = mTitleTextView.getMeasuredHeight() + getVerticalMargins(mTitleTextView);
			childState = combineMeasuredStates(childState, mTitleTextView.getMeasuredState());
		}
		if (shouldLayout(mSubtitleTextView)) {
			titleWidth = Math.max(titleWidth, measureChildCollapseMargins(mSubtitleTextView,
					widthMeasureSpec, width + titleHorizMargins,
					heightMeasureSpec, titleHeight + titleVertMargins,
					collapsingMargins));
			titleHeight += mSubtitleTextView.getMeasuredHeight() +
					getVerticalMargins(mSubtitleTextView);
			childState = combineMeasuredStates(childState, mSubtitleTextView.getMeasuredState());
		}

		width += titleWidth;
		height = Math.max(height, titleHeight);

		// Measurement already took padding into account for available space for the children,
		// add it in for the final size.
		width += getPaddingLeft() + getPaddingRight();
		height += getPaddingTop() + getPaddingBottom();

		final int measuredWidth = resolveSizeAndState(
				Math.max(width, getSuggestedMinimumWidth()),
				widthMeasureSpec, childState & MEASURED_STATE_MASK);
		final int measuredHeight = resolveSizeAndState(
				Math.max(height, getSuggestedMinimumHeight()),
				heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT);

		setMeasuredDimension(measuredWidth, shouldCollapse() ? 0 : measuredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
		final int width = getWidth();
		final int height = getHeight();
		final int paddingLeft = getPaddingLeft();
		final int paddingRight = getPaddingRight();
		final int paddingTop = getPaddingTop();
		final int paddingBottom = getPaddingBottom();
		int left = paddingLeft;
		int right = width - paddingRight;

		final int[] collapsingMargins = mTempMargins;
		collapsingMargins[0] = collapsingMargins[1] = 0;

		// Align views within the minimum toolbar height, if set.
		final int minHeight = getMinimumHeight();
		final int alignmentHeight = minHeight >= 0 ? Math.min(minHeight, b - t) : 0;

		if (shouldLayout(mNavButtonView)) {
			if (isRtl) {
				right = layoutChildRight(mNavButtonView, right, collapsingMargins,
						alignmentHeight);
			} else {
				left = layoutChildLeft(mNavButtonView, left, collapsingMargins,
						alignmentHeight);
			}
		}

		final int contentInsetLeft = getCurrentContentInsetLeft();
		final int contentInsetRight = getCurrentContentInsetRight();
		collapsingMargins[0] = Math.max(0, contentInsetLeft - left);
		collapsingMargins[1] = Math.max(0, contentInsetRight - (width - paddingRight - right));
		left = Math.max(left, contentInsetLeft);
		right = Math.min(right, width - paddingRight - contentInsetRight);

		if (shouldLayout(mLogoView)) {
			if (isRtl) {
				right = layoutChildRight(mLogoView, right, collapsingMargins,
						alignmentHeight);
			} else {
				left = layoutChildLeft(mLogoView, left, collapsingMargins,
						alignmentHeight);
			}
		}

		final boolean layoutTitle = shouldLayout(mTitleTextView);
		final boolean layoutSubtitle = shouldLayout(mSubtitleTextView);
		int titleHeight = 0;
		if (layoutTitle) {
			final LayoutParams lp = (LayoutParams)mTitleTextView.getLayoutParams();
			titleHeight += lp.topMargin + mTitleTextView.getMeasuredHeight() + lp.bottomMargin;
		}
		if (layoutSubtitle) {
			final LayoutParams lp = (LayoutParams)mSubtitleTextView.getLayoutParams();
			titleHeight += lp.topMargin + mSubtitleTextView.getMeasuredHeight() + lp.bottomMargin;
		}

		if (layoutTitle || layoutSubtitle) {
			int titleTop;
			final View topChild = layoutTitle ? mTitleTextView : mSubtitleTextView;
			final View bottomChild = layoutSubtitle ? mSubtitleTextView : mTitleTextView;
			final LayoutParams toplp = (LayoutParams)topChild.getLayoutParams();
			final LayoutParams bottomlp = (LayoutParams)bottomChild.getLayoutParams();
			final boolean titleHasWidth = layoutTitle && mTitleTextView.getMeasuredWidth() > 0
					|| layoutSubtitle && mSubtitleTextView.getMeasuredWidth() > 0;

			switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
					titleTop = getPaddingTop() + toplp.topMargin + mTitleMarginTop;
					break;
				default:
				case Gravity.CENTER_VERTICAL:
					final int space = height - paddingTop - paddingBottom;
					int spaceAbove = (space - titleHeight) / 2;
					if (spaceAbove < toplp.topMargin + mTitleMarginTop) {
						spaceAbove = toplp.topMargin + mTitleMarginTop;
					} else {
						final int spaceBelow = height - paddingBottom - titleHeight -
								spaceAbove - paddingTop;
						if (spaceBelow < toplp.bottomMargin + mTitleMarginBottom) {
							spaceAbove = Math.max(0, spaceAbove -
									(bottomlp.bottomMargin + mTitleMarginBottom - spaceBelow));
						}
					}
					titleTop = paddingTop + spaceAbove;
					break;
				case Gravity.BOTTOM:
					titleTop = height - paddingBottom - bottomlp.bottomMargin - mTitleMarginBottom -
							titleHeight;
					break;
			}
			if (isRtl) {
				final int rd = (titleHasWidth ? mTitleMarginStart : 0) - collapsingMargins[1];
				right -= Math.max(0, rd);
				collapsingMargins[1] = Math.max(0, -rd);
				int titleRight = right;
				int subtitleRight = right;

				if (layoutTitle) {
					final LayoutParams lp = (LayoutParams)mTitleTextView.getLayoutParams();
					final int titleLeft = titleRight - mTitleTextView.getMeasuredWidth();
					final int titleBottom = titleTop + mTitleTextView.getMeasuredHeight();
					mTitleTextView.layout(titleLeft, titleTop, titleRight, titleBottom);
					titleRight = titleLeft - mTitleMarginEnd;
					titleTop = titleBottom + lp.bottomMargin;
				}
				if (layoutSubtitle) {
					final LayoutParams lp = (LayoutParams)mSubtitleTextView.getLayoutParams();
					titleTop += lp.topMargin;
					final int subtitleLeft = subtitleRight - mSubtitleTextView.getMeasuredWidth();
					final int subtitleBottom = titleTop + mSubtitleTextView.getMeasuredHeight();
					mSubtitleTextView.layout(subtitleLeft, titleTop, subtitleRight, subtitleBottom);
					subtitleRight = subtitleRight - mTitleMarginEnd;
					titleTop = subtitleBottom + lp.bottomMargin;
				}
				if (titleHasWidth) {
					right = Math.min(titleRight, subtitleRight);
				}
			} else {
				final int ld = (titleHasWidth ? mTitleMarginStart : 0) - collapsingMargins[0];
				left += Math.max(0, ld);
				collapsingMargins[0] = Math.max(0, -ld);
				int titleLeft = left;
				int subtitleLeft = left;

				if (layoutTitle) {
					final LayoutParams lp = (LayoutParams)mTitleTextView.getLayoutParams();
					final int titleRight = titleLeft + mTitleTextView.getMeasuredWidth();
					final int titleBottom = titleTop + mTitleTextView.getMeasuredHeight();
					mTitleTextView.layout(titleLeft, titleTop, titleRight, titleBottom);
					titleLeft = titleRight + mTitleMarginEnd;
					titleTop = titleBottom + lp.bottomMargin;
				}
				if (layoutSubtitle) {
					final LayoutParams lp = (LayoutParams)mSubtitleTextView.getLayoutParams();
					titleTop += lp.topMargin;
					final int subtitleRight = subtitleLeft + mSubtitleTextView.getMeasuredWidth();
					final int subtitleBottom = titleTop + mSubtitleTextView.getMeasuredHeight();
					mSubtitleTextView.layout(subtitleLeft, titleTop, subtitleRight, subtitleBottom);
					subtitleLeft = subtitleRight + mTitleMarginEnd;
					titleTop = subtitleBottom + lp.bottomMargin;
				}
				if (titleHasWidth) {
					left = Math.max(titleLeft, subtitleLeft);
				}
			}
		}

		// Get all remaining children sorted for layout. This is all prepared
		// such that absolute layout direction can be used below.

		addCustomViewsWithGravity(mTempViews, Gravity.LEFT);
		final int leftViewsCount = mTempViews.size();
		for (int i = 0; i < leftViewsCount; i++) {
			left = layoutChildLeft(mTempViews.get(i), left, collapsingMargins,
					alignmentHeight);
		}

		addCustomViewsWithGravity(mTempViews, Gravity.RIGHT);
		final int rightViewsCount = mTempViews.size();
		for (int i = 0; i < rightViewsCount; i++) {
			right = layoutChildRight(mTempViews.get(i), right, collapsingMargins,
					alignmentHeight);
		}

		// Centered views try to center with respect to the whole bar, but views pinned
		// to the left or right can push the mass of centered views to one side or the other.
		addCustomViewsWithGravity(mTempViews, Gravity.CENTER_HORIZONTAL);
		final int centerViewsWidth = getViewListMeasuredWidth(mTempViews, collapsingMargins);
		final int parentCenter = paddingLeft + (width - paddingLeft - paddingRight) / 2;
		final int halfCenterViewsWidth = centerViewsWidth / 2;
		int centerLeft = parentCenter - halfCenterViewsWidth;
		final int centerRight = centerLeft + centerViewsWidth;
		if (centerLeft < left) {
			centerLeft = left;
		} else if (centerRight > right) {
			centerLeft -= centerRight - right;
		}

		final int centerViewsCount = mTempViews.size();
		for (int i = 0; i < centerViewsCount; i++) {
			centerLeft = layoutChildLeft(mTempViews.get(i), centerLeft, collapsingMargins,
					alignmentHeight);
		}

		mTempViews.clear();
	}

	private int getViewListMeasuredWidth(List<View> views, int[] collapsingMargins) {
		int collapseLeft = collapsingMargins[0];
		int collapseRight = collapsingMargins[1];
		int width = 0;
		final int count = views.size();
		for (int i = 0; i < count; i++) {
			final View v = views.get(i);
			final LayoutParams lp = (LayoutParams)v.getLayoutParams();
			final int l = lp.leftMargin - collapseLeft;
			final int r = lp.rightMargin - collapseRight;
			final int leftMargin = Math.max(0, l);
			final int rightMargin = Math.max(0, r);
			collapseLeft = Math.max(0, -l);
			collapseRight = Math.max(0, -r);
			width += leftMargin + v.getMeasuredWidth() + rightMargin;
		}
		return width;
	}

	private int layoutChildLeft(View child, int left, int[] collapsingMargins,
			int alignmentHeight) {
		final LayoutParams lp = (LayoutParams)child.getLayoutParams();
		final int l = lp.leftMargin - collapsingMargins[0];
		left += Math.max(0, l);
		collapsingMargins[0] = Math.max(0, -l);
		final int top = getChildTop(child, alignmentHeight);
		final int childWidth = child.getMeasuredWidth();
		child.layout(left, top, left + childWidth, top + child.getMeasuredHeight());
		left += childWidth + lp.rightMargin;
		return left;
	}

	private int layoutChildRight(View child, int right, int[] collapsingMargins,
			int alignmentHeight) {
		final LayoutParams lp = (LayoutParams)child.getLayoutParams();
		final int r = lp.rightMargin - collapsingMargins[1];
		right -= Math.max(0, r);
		collapsingMargins[1] = Math.max(0, -r);
		final int top = getChildTop(child, alignmentHeight);
		final int childWidth = child.getMeasuredWidth();
		child.layout(right - childWidth, top, right, top + child.getMeasuredHeight());
		right -= childWidth + lp.leftMargin;
		return right;
	}

	private int getChildTop(View child, int alignmentHeight) {
		final LayoutParams lp = (LayoutParams)child.getLayoutParams();
		final int childHeight = child.getMeasuredHeight();
		final int alignmentOffset = alignmentHeight > 0 ? (childHeight - alignmentHeight) / 2 : 0;
		switch (getChildVerticalGravity(lp.gravity)) {
			case Gravity.TOP:
				return getPaddingTop() - alignmentOffset;

			case Gravity.BOTTOM:
				return getHeight() - getPaddingBottom() - childHeight - lp.bottomMargin - alignmentOffset;

			default:
			case Gravity.CENTER_VERTICAL:
				final int paddingTop = getPaddingTop();
				final int paddingBottom = getPaddingBottom();
				final int height = getHeight();
				final int space = height - paddingTop - paddingBottom;
				int spaceAbove = (space - childHeight) / 2;
				if (spaceAbove < lp.topMargin) {
					spaceAbove = lp.topMargin;
				} else {
					final int spaceBelow = height - paddingBottom - childHeight -
							spaceAbove - paddingTop;
					if (spaceBelow < lp.bottomMargin) {
						spaceAbove = Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow));
					}
				}
				return paddingTop + spaceAbove;
		}
	}

	private int getChildVerticalGravity(int gravity) {
		final int vgrav = gravity & Gravity.VERTICAL_GRAVITY_MASK;
		switch (vgrav) {
			case Gravity.TOP:
			case Gravity.BOTTOM:
			case Gravity.CENTER_VERTICAL:
				return vgrav;
			default:
				return mGravity & Gravity.VERTICAL_GRAVITY_MASK;
		}
	}

	/**
	 * Prepare a list of non-SYSTEM child views. If the layout direction is RTL
	 * this will be in reverse child order.
	 */
	private void addCustomViewsWithGravity(List<View> views, int gravity) {
		final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
		final int childCount = getChildCount();
		final int absGrav = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());

		views.clear();

		if (isRtl) {
			for (int i = childCount - 1; i >= 0; i--) {
				final View child = getChildAt(i);
				final LayoutParams lp = (LayoutParams)child.getLayoutParams();
				if (lp.mViewType == LayoutParams.CUSTOM && shouldLayout(child) &&
						getChildHorizontalGravity(lp.gravity) == absGrav) {
					views.add(child);
				}
			}
		} else {
			for (int i = 0; i < childCount; i++) {
				final View child = getChildAt(i);
				final LayoutParams lp = (LayoutParams)child.getLayoutParams();
				if (lp.mViewType == LayoutParams.CUSTOM && shouldLayout(child) &&
						getChildHorizontalGravity(lp.gravity) == absGrav) {
					views.add(child);
				}
			}
		}
	}

	private int getChildHorizontalGravity(int gravity) {
		final int ld = getLayoutDirection();
		final int absGrav = Gravity.getAbsoluteGravity(gravity, ld);
		final int hGrav = absGrav & Gravity.HORIZONTAL_GRAVITY_MASK;
		switch (hGrav) {
			case Gravity.LEFT:
			case Gravity.RIGHT:
			case Gravity.CENTER_HORIZONTAL:
				return hGrav;
			default:
				return ld == LAYOUT_DIRECTION_RTL ? Gravity.RIGHT : Gravity.LEFT;
		}
	}

	private boolean shouldLayout(View view) {
		return view != null && view.getParent() == this && view.getVisibility() != GONE;
	}

	private int getHorizontalMargins(View v) {
		final MarginLayoutParams mlp = (MarginLayoutParams)v.getLayoutParams();
		return mlp.getMarginStart() + mlp.getMarginEnd();
	}

	private int getVerticalMargins(View v) {
		final MarginLayoutParams mlp = (MarginLayoutParams)v.getLayoutParams();
		return mlp.topMargin + mlp.bottomMargin;
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		if (p instanceof LayoutParams) {
			return new LayoutParams((LayoutParams)p);
		} else if (p instanceof ActionBar.LayoutParams) {
			return new LayoutParams((ActionBar.LayoutParams)p);
		} else if (p instanceof MarginLayoutParams) {
			return new LayoutParams((MarginLayoutParams)p);
		} else {
			return new LayoutParams(p);
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return super.checkLayoutParams(p) && p instanceof LayoutParams;
	}

	private boolean isChildOrHidden(View child) {
		return child.getParent() == this || mHiddenViews.contains(child);
	}

	/** @hide */
	public void setCollapsible(boolean collapsible) {
		mCollapsible = collapsible;
		requestLayout();
	}

	private void ensureContentInsets() {
		if (mContentInsets == null) {
			mContentInsets = new RtlSpacingHelper();
		}
	}

	/**
	 * Layout information for child views of Toolbars.
	 */
	public static class LayoutParams extends ActionBar.LayoutParams {
		static final int CUSTOM = 0;
		static final int SYSTEM = 1;
		static final int EXPANDED = 2;

		int mViewType = CUSTOM;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
			this.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
		}

		public LayoutParams(int width, int height, int gravity) {
			super(width, height);
			this.gravity = gravity;
		}

		public LayoutParams(int gravity) {
			this(WRAP_CONTENT, MATCH_PARENT, gravity);
		}

		public LayoutParams(LayoutParams source) {
			super(source);

			mViewType = source.mViewType;
		}

		public LayoutParams(ActionBar.LayoutParams source) {
			super(source);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
			// ActionBar.LayoutParams doesn't have a MarginLayoutParams constructor.
			// Fake it here and copy over the relevant data.
			copyMarginsFrom(source);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}

	/**
	 * Interface responsible for receiving menu item click events if the items themselves
	 * do not have individual item click listeners.
	 */
	public interface OnMenuItemClickListener {
		public boolean onMenuItemClick(android.view.MenuItem item);
	}
}
