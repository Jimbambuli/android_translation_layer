package android.widget;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.WordIterator;
import android.text.style.URLSpan;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import java.text.BreakIterator;

/* The parts of AOSP's Editor (android-16.0.0_r4) that select the text of a selectable TextView whose text the view
 * lays out itself with StaticLayout (a paragraph text, TextView.showParagraphLayout()); other texts are selected
 * by the GtkLabel. As there: a long press selects the word (performLongClick(), selectCurrentWordAndStartDrag())
 * and dragging on extends it word by word (SelectionModifierCursorController's DRAG_ACCELERATOR_MODE_WORD), a
 * double tap selects the word, the selection gets its handles (SelectionHandleView) and the floating text toolbar
 * (TextActionModeCallback in a TYPE_FLOATING action mode), and a tap ends it (onTouchUpEvent()).
 * Left out: the insertion cursor and its handle (the text is not editable), the magnifier, TextClassifier smart
 * selection and its link actions (SelectionActionModeHelper selects the word, as AOSP does without a classifier),
 * dragging the selection away, process-text intents, triple click, and the snapping of a dragged handle to words
 * (the handles move by character). */
final class ParagraphTextSelection {
	// Editor.ACTION_MODE_MENU_ITEM_ORDER_*
	private static final int ACTION_MODE_MENU_ITEM_ORDER_CUT = 4;
	private static final int ACTION_MODE_MENU_ITEM_ORDER_COPY = 5;
	private static final int ACTION_MODE_MENU_ITEM_ORDER_PASTE = 6;
	private static final int ACTION_MODE_MENU_ITEM_ORDER_SHARE = 7;
	private static final int ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL = 8;

	private final TextView mTextView;
	ActionMode mTextActionMode;
	boolean mDiscardNextActionUp;
	boolean mIsBeingLongClicked;
	private boolean mPreserveSelection;

	// EditorTouchState: the last down and up of the touch
	private float mLastDownX, mLastDownY;
	private float mLastUpX, mLastUpY;
	private long mLastUpTime;
	private boolean mIsDoubleTap;

	// SelectionModifierCursorController
	private int mMinTouchOffset, mMaxTouchOffset;
	private boolean mDragAcceleratorActive;
	private int mStartOffset = -1;
	private SelectionHandleView mStartHandle;
	private SelectionHandleView mEndHandle;

	private WordIterator mWordIteratorWithText;
	private final Path mHighlightPath = new Path();

	ParagraphTextSelection(TextView textView) {
		mTextView = textView;
	}

	private Spannable text() {
		return (Spannable)mTextView.getText();
	}

	/* the text changes: AOSP's Editor stops the action mode and hides the handles (prepareCursorControllers()) */
	void onTextChanged() {
		stopTextActionMode();
		hideHandles();
	}

	/* AOSP TextView.getUpdatedHighlightPath() for a selection; the highlight is drawn before the text */
	void drawHighlight(Canvas canvas, Layout layout, android.graphics.Paint paint) {
		final int selStart = mTextView.getSelectionStart();
		final int selEnd = mTextView.getSelectionEnd();
		if (!(mTextView.isFocused() || mTextView.isPressed()) || selStart < 0 || selStart == selEnd)
			return;
		mHighlightPath.reset();
		layout.getSelectionPath(selStart, selEnd, mHighlightPath);
		paint.setColor(mTextView.getHighlightColor());
		paint.setStyle(android.graphics.Paint.Style.FILL);
		canvas.drawPath(mHighlightPath, paint);
	}

	/* Editor.onTouchEvent(): the touch state, then the SelectionModifierCursorController */
	void onTouchEvent(MotionEvent event) {
		final ViewConfiguration config = ViewConfiguration.get(mTextView.getContext());
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
			// EditorTouchState.update(): a down near the last up within the double tap timeout is a double tap
			final float dx = event.getX() - mLastUpX;
			final float dy = event.getY() - mLastUpY;
			final int slop = config.getScaledDoubleTapSlop();
			mIsDoubleTap = event.getEventTime() - mLastUpTime <= ViewConfiguration.getDoubleTapTimeout()
			    && dx * dx + dy * dy <= slop * slop;
			mLastDownX = event.getX();
			mLastDownY = event.getY();
			mMinTouchOffset = mMaxTouchOffset = mTextView.getOffsetForPosition(mLastDownX, mLastDownY);
			if (mIsDoubleTap && isPositionOnText(mLastDownX, mLastDownY)) {
				selectCurrentWordAndStartDrag();
				mDiscardNextActionUp = true;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE:
			if (mDragAcceleratorActive) {
				updateWordBasedSelection(event);
			}
			break;
		case MotionEvent.ACTION_UP:
			mLastUpX = event.getX();
			mLastUpY = event.getY();
			mLastUpTime = event.getEventTime();
			if (!mDragAcceleratorActive)
				break;
			updateWordBasedSelection(event);
			// No longer dragging to select text, let the parent intercept events.
			mTextView.getParent().requestDisallowInterceptTouchEvent(false);
			mDragAcceleratorActive = false;
			mStartOffset = -1;
			if (mTextView.hasSelection()) {
				startSelectionActionMode();
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mDragAcceleratorActive) {
				mTextView.getParent().requestDisallowInterceptTouchEvent(false);
				mDragAcceleratorActive = false;
				mStartOffset = -1;
			}
			break;
		}
	}

	/* Editor.performLongClick(): a new selection of the word under the touch, or of another word while one is
	 * selected (AOSP drags a selection that is touched; here it stays) */
	boolean performLongClick(boolean handled) {
		if (!handled && mTextActionMode != null) {
			if (!touchPositionIsInSelection()) {
				stopTextActionMode();
				selectCurrentWordAndStartDrag();
			}
			handled = true;
		}
		if (!handled) {
			handled = selectCurrentWordAndStartDrag();
		}
		return handled;
	}

	/* Editor.performLongClick() and the up that ends the press (startSelectionActionMode()) for an EditText: the
	 * GtkText has put the cursor at the touch on the press, which is the touch offset */
	boolean performLongClickAtCursor() {
		mMinTouchOffset = mTextView.getSelectionStart();
		mMaxTouchOffset = mTextView.getSelectionEnd();
		if (!performLongClick(false))
			return false;
		mDragAcceleratorActive = false; // no drag: the GtkText gets the moves of the touch
		startSelectionActionMode();
		return true;
	}

	/* the GtkText moved the cursor (a tap) or changed the text: Editor.onTouchUpEvent() and sendOnTextChanged()
	 * stop the text action mode */
	void onEditTextSelectionChanged() {
		if (mTextActionMode != null && !mTextView.hasSelection())
			stopTextActionMode();
	}

	/* Editor.onTouchUpEvent(): a tap ends the selection; the cursor of a selectable text is the collapsed
	 * selection, which is not drawn */
	void onTouchUpEvent(MotionEvent event) {
		hideHandles();
		stopTextActionMode();
		CharSequence text = mTextView.getText();
		if (text.length() > 0) {
			final int offset = mTextView.getOffsetForPosition(event.getX(), event.getY());
			setSelection(offset, offset);
		}
	}

	private boolean touchPositionIsInSelection() {
		int selectionStart = mTextView.getSelectionStart();
		int selectionEnd = mTextView.getSelectionEnd();
		if (selectionStart == selectionEnd) {
			return false;
		}
		if (selectionStart > selectionEnd) {
			int tmp = selectionStart;
			selectionStart = selectionEnd;
			selectionEnd = tmp;
		}
		return mMinTouchOffset >= selectionStart && mMaxTouchOffset < selectionEnd;
	}

	/* Editor.isPositionOnText() */
	private boolean isPositionOnText(float x, float y) {
		Layout layout = mTextView.getLayout();
		if (layout == null)
			return false;
		final int line = mTextView.getLineAtCoordinate(y);
		x = mTextView.convertToLocalHorizontalCoordinate(x);
		if (x < layout.getLineLeft(line))
			return false;
		if (x > layout.getLineRight(line))
			return false;
		return true;
	}

	/* Editor.selectCurrentWordAndStartDrag() */
	private boolean selectCurrentWordAndStartDrag() {
		if (!checkField()) {
			return false;
		}
		if (!mTextView.hasSelection() && !selectCurrentWord()) {
			// No selection and cannot select a word.
			return false;
		}
		stopTextActionModeWithPreservingSelection();
		enterDrag();
		return true;
	}

	/* Editor.checkField() */
	private boolean checkField() {
		if (!mTextView.canSelectText() || !mTextView.requestFocus()) {
			android.util.Log.w("TextView", "TextView does not support text selection. Selection cancelled.");
			return false;
		}
		return true;
	}

	/* Editor.selectCurrentWord(): a URLSpan at the touch, else the word (or the character) at it */
	private boolean selectCurrentWord() {
		final int minOffset = mMinTouchOffset;
		final int maxOffset = mMaxTouchOffset;
		final CharSequence text = mTextView.getText();

		// Safety check in case standard touch event handling has been bypassed
		if (minOffset < 0 || minOffset > text.length()) return false;
		if (maxOffset < 0 || maxOffset > text.length()) return false;

		int selectionStart, selectionEnd;

		// If a URLSpan (web address, email, phone...) is found at that position, select it.
		URLSpan[] urlSpans = ((Spanned)text).getSpans(minOffset, maxOffset, URLSpan.class);
		if (urlSpans.length >= 1) {
			URLSpan urlSpan = urlSpans[0];
			selectionStart = ((Spanned)text).getSpanStart(urlSpan);
			selectionEnd = ((Spanned)text).getSpanEnd(urlSpan);
		} else {
			final WordIterator wordIterator = mTextView.getWordIterator();
			wordIterator.setCharSequence(text, minOffset, maxOffset);

			selectionStart = wordIterator.getBeginning(minOffset);
			selectionEnd = wordIterator.getEnd(maxOffset);

			if (selectionStart == BreakIterator.DONE || selectionEnd == BreakIterator.DONE
			    || selectionStart == selectionEnd) {
				// Possible when the word iterator does not properly handle the text's language
				BreakIterator chars = BreakIterator.getCharacterInstance(mTextView.getTextLocale());
				chars.setText(text.toString());
				selectionStart = chars.isBoundary(minOffset) ? minOffset : chars.preceding(minOffset);
				selectionEnd = chars.following(minOffset);
				if (selectionStart == BreakIterator.DONE || selectionEnd == BreakIterator.DONE)
					return false;
			}
		}

		setSelection(selectionStart, selectionEnd);
		return selectionEnd > selectionStart;
	}

	private WordIterator getWordIteratorWithText() {
		if (mWordIteratorWithText == null)
			mWordIteratorWithText = new WordIterator(mTextView.getTextLocale());
		mWordIteratorWithText.setCharSequence(mTextView.getText(), 0, mTextView.getText().length());
		return mWordIteratorWithText;
	}

	/* Editor.getWordStart() */
	private int getWordStart(int offset) {
		int retOffset = getWordIteratorWithText().prevBoundary(offset);
		if (getWordIteratorWithText().isOnPunctuation(retOffset)) {
			// On punctuation boundary or within group of punctuation, find punctuation start.
			retOffset = getWordIteratorWithText().getPunctuationBeginning(offset);
		} else {
			// Not on a punctuation boundary, find the word start.
			retOffset = getWordIteratorWithText().getPrevWordBeginningOnTwoWordsBoundary(offset);
		}
		if (retOffset == BreakIterator.DONE) {
			return offset;
		}
		return retOffset;
	}

	/* Editor.getWordEnd() */
	private int getWordEnd(int offset) {
		int retOffset = getWordIteratorWithText().nextBoundary(offset);
		if (getWordIteratorWithText().isAfterPunctuation(retOffset)) {
			// On punctuation boundary or within group of punctuation, find punctuation end.
			retOffset = getWordIteratorWithText().getPunctuationEnd(offset);
		} else {
			// Not on a punctuation boundary, find the word end.
			retOffset = getWordIteratorWithText().getNextWordEndOnTwoWordBoundary(offset);
		}
		if (retOffset == BreakIterator.DONE) {
			return offset;
		}
		return retOffset;
	}

	/* SelectionModifierCursorController.enterDrag(DRAG_ACCELERATOR_MODE_WORD) */
	private void enterDrag() {
		mDragAcceleratorActive = true;
		// Start location of selection.
		mStartOffset = mTextView.getOffsetForPosition(mLastDownX, mLastDownY);
		// Don't show the handles until user has lifted finger.
		hideHandles();
		// This stops scrolling parents from intercepting the touch event, allowing the user to continue
		// dragging across the screen to select text.
		mTextView.getParent().requestDisallowInterceptTouchEvent(true);
		mTextView.cancelLongPress();
	}

	/* SelectionModifierCursorController.updateWordBasedSelection(), without the finger offset between lines */
	private void updateWordBasedSelection(MotionEvent event) {
		int startOffset;
		int offset = mTextView.getOffsetForPosition(event.getX(), event.getY());
		if (mStartOffset < 0 || offset < 0)
			return;
		// Snap to word boundaries.
		if (mStartOffset < offset) {
			// Expanding with end handle.
			offset = getWordEnd(offset);
			startOffset = getWordStart(mStartOffset);
		} else {
			// Expanding with start handle.
			offset = getWordStart(offset);
			startOffset = getWordEnd(mStartOffset);
			if (startOffset == offset) {
				offset = Math.min(offset + 1, mTextView.getText().length());
			}
		}
		// updateSelectionInternal(): the selection runs from the smaller offset
		setSelection(Math.min(startOffset, offset), Math.max(startOffset, offset));
	}

	private void setSelection(int start, int end) {
		if (mTextView instanceof EditText) { // the GtkText holds the selection
			mTextView.setSelection(start, end);
			return;
		}
		Selection.setSelection(text(), start, end);
		mTextView.onSelectionChanged(start, end);
		mTextView.invalidate();
	}

	/* SelectionActionModeHelper.startSelectionActionModeAsync() without a TextClassifier: Editor
	 * startActionModeInternal(TextActionMode.SELECTION), then the handles */
	void startSelectionActionMode() {
		if (mTextActionMode != null) {
			// Text action mode is already started
			mTextActionMode.invalidate();
			showHandles();
			return;
		}
		if (!checkField() || !mTextView.hasSelection()) {
			return;
		}
		ActionMode.Callback actionModeCallback = new TextActionModeCallback();
		mTextActionMode = mTextView.startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING);
		showHandles();
	}

	/* the selection changed by selectAll: Editor.invalidateActionModeAsync() and the handles follow */
	void onSelectionChangedByMenu() {
		mTextView.invalidate();
		if (mTextActionMode != null)
			mTextActionMode.invalidate();
		if (mStartHandle != null && mStartHandle.isShowing())
			showHandles();
	}

	/* Editor.stopTextActionMode() */
	void stopTextActionMode() {
		if (mTextActionMode != null) {
			// This will hide the handles
			mTextActionMode.finish();
		}
	}

	private void stopTextActionModeWithPreservingSelection() {
		mPreserveSelection = true;
		stopTextActionMode();
		mPreserveSelection = false;
	}

	/* SelectionModifierCursorController.show()/initHandles() */
	private void showHandles() {
		// the GtkText of an EditText has no Layout to place the handles at; its selection is moved by its own drag
		if (mTextView instanceof EditText)
			return;
		if (mStartHandle == null) {
			mStartHandle = new SelectionHandleView(mTextView.getTextSelectHandleLeft(), true);
			mEndHandle = new SelectionHandleView(mTextView.getTextSelectHandleRight(), false);
		}
		mStartHandle.show();
		mEndHandle.show();
	}

	void hideHandles() {
		if (mStartHandle != null) {
			mStartHandle.hide();
			mEndHandle.hide();
		}
	}

	/* Editor.TextActionModeCallback for a selection: copy, share and select all (cut and paste need an
	 * editable text), the app's custom selection callback, the content rect of the selection */
	private class TextActionModeCallback extends ActionMode.Callback2 {
		private final Path mSelectionPath = new Path();
		private final RectF mSelectionBounds = new RectF();
		private final int mHandleHeight;

		TextActionModeCallback() {
			Drawable left = mTextView.getTextSelectHandleLeft();
			Drawable right = mTextView.getTextSelectHandleRight();
			mHandleHeight = Math.max(left != null ? left.getMinimumHeight() : 0, right != null ? right.getMinimumHeight() : 0);
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.setTitle(null);
			mode.setSubtitle(null);
			mode.setTitleOptionalHint(true);
			populateMenuWithItems(menu);

			ActionMode.Callback customCallback = mTextView.getCustomSelectionActionModeCallback();
			if (customCallback != null) {
				if (!customCallback.onCreateActionMode(mode, menu)) {
					// The custom mode can choose to cancel the action mode, dismiss selection.
					setSelection(mTextView.getSelectionEnd(), mTextView.getSelectionEnd());
					return false;
				}
			}
			return true;
		}

		private void populateMenuWithItems(Menu menu) {
			if (mTextView.canCut()) {
				menu.add(Menu.NONE, android.R.id.cut, ACTION_MODE_MENU_ITEM_ORDER_CUT,
				    com.android.internal.R.string.cut)
				    .setAlphabeticShortcut('x')
				    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (mTextView.canCopy()) {
				menu.add(Menu.NONE, android.R.id.copy, ACTION_MODE_MENU_ITEM_ORDER_COPY,
				    com.android.internal.R.string.copy)
				    .setAlphabeticShortcut('c')
				    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (mTextView.canPaste()) {
				menu.add(Menu.NONE, android.R.id.paste, ACTION_MODE_MENU_ITEM_ORDER_PASTE,
				    com.android.internal.R.string.paste)
				    .setAlphabeticShortcut('v')
				    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (mTextView.canShare()) {
				menu.add(Menu.NONE, com.android.internal.R.id.shareText, ACTION_MODE_MENU_ITEM_ORDER_SHARE,
				    com.android.internal.R.string.share)
				    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			}

			updateSelectAllItem(menu);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			updateSelectAllItem(menu);

			ActionMode.Callback customCallback = mTextView.getCustomSelectionActionModeCallback();
			if (customCallback != null) {
				return customCallback.onPrepareActionMode(mode, menu);
			}
			return true;
		}

		private void updateSelectAllItem(Menu menu) {
			boolean canSelectAll = mTextView.canSelectAllText();
			boolean selectAllItemExists = menu.findItem(android.R.id.selectAll) != null;
			if (canSelectAll && !selectAllItemExists) {
				menu.add(Menu.NONE, android.R.id.selectAll, ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL,
				    com.android.internal.R.string.selectAll)
				    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			} else if (!canSelectAll && selectAllItemExists) {
				menu.removeItem(android.R.id.selectAll);
			}
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ActionMode.Callback customCallback = mTextView.getCustomSelectionActionModeCallback();
			if (customCallback != null && customCallback.onActionItemClicked(mode, item)) {
				return true;
			}
			return mTextView.onTextContextMenuItem(item.getItemId());
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Clear mTextActionMode not to recursively destroy action mode by clearing selection.
			mTextActionMode = null;
			ActionMode.Callback customCallback = mTextView.getCustomSelectionActionModeCallback();
			if (customCallback != null) {
				customCallback.onDestroyActionMode(mode);
			}

			if (!mPreserveSelection) {
				// Leave current selection when we tentatively destroy action mode for the selection.
				setSelection(mTextView.getSelectionEnd(), mTextView.getSelectionEnd());
			}

			hideHandles();
		}

		@Override
		public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
			// an EditText's text is laid out by its GtkText, which has no Layout: the rect of the view
			if (!view.equals(mTextView) || mTextView.getLayout() == null || mTextView instanceof EditText) {
				super.onGetContentRect(mode, view, outRect);
				return;
			}
			final int selectionStart = mTextView.getSelectionStart();
			final int selectionEnd = mTextView.getSelectionEnd();
			final Layout layout = mTextView.getLayout();
			mSelectionPath.reset();
			layout.getSelectionPath(selectionStart, selectionEnd, mSelectionPath);
			mSelectionPath.computeBounds(mSelectionBounds, true);
			mSelectionBounds.bottom += mHandleHeight;
			// Take TextView's padding and scroll into account.
			int textHorizontalOffset = mTextView.getCompoundPaddingLeft() - mTextView.getScrollX();
			int textVerticalOffset = mTextView.getTotalPaddingTop() - mTextView.getScrollY();
			outRect.set(
			    (int)Math.floor(mSelectionBounds.left + textHorizontalOffset),
			    (int)Math.floor(mSelectionBounds.top + textVerticalOffset),
			    (int)Math.ceil(mSelectionBounds.right + textHorizontalOffset),
			    (int)Math.ceil(mSelectionBounds.bottom + textVerticalOffset));
		}
	}

	/* Editor.HandleView/SelectionHandleView for left-to-right text: the handle drawable in a popup below the line
	 * of its end of the selection; dragging it moves that end (by character, never across the other end) */
	private final class SelectionHandleView extends View {
		private final Drawable mDrawable;
		private final PopupWindow mContainer;
		private final boolean mIsStartHandle;
		private final int mHotspotX;
		private final int mMinSize;
		private final float mTouchOffsetY;
		private final float mIdealVerticalOffset;
		private int mPositionX, mPositionY;
		private float mTouchToWindowOffsetX, mTouchToWindowOffsetY;
		private int mLastParentXOnScreen, mLastParentYOnScreen;
		private int mLastParentX, mLastParentY;
		private final int[] mTmp = new int[2];

		SelectionHandleView(Drawable drawable, boolean isStartHandle) {
			super(mTextView.getContext());
			mDrawable = drawable;
			mIsStartHandle = isStartHandle;
			mContainer = new PopupWindow(mTextView.getContext(), null,
			    com.android.internal.R.attr.textSelectHandleWindowStyle);
			mContainer.setSplitTouchEnabled(true);
			mContainer.setClippingEnabled(false);
			mContainer.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
			mContainer.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
			mContainer.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			mContainer.setContentView(this);
			mMinSize = mTextView.getContext().getResources().getDimensionPixelSize(
			    com.android.internal.R.dimen.text_handle_min_size);
			// getHotspotX() for a left-to-right run: the start handle points with its right part, the end handle
			// with its left part
			final int width = drawable != null ? drawable.getIntrinsicWidth() : 0;
			mHotspotX = isStartHandle ? (width * 3) / 4 : width / 4;
			final int handleHeight = getPreferredHeight();
			mTouchOffsetY = -0.3f * handleHeight;
			mIdealVerticalOffset = 0.7f * handleHeight;
		}

		private int getPreferredWidth() {
			return Math.max(mDrawable != null ? mDrawable.getIntrinsicWidth() : 0, mMinSize);
		}

		private int getPreferredHeight() {
			return Math.max(mDrawable != null ? mDrawable.getIntrinsicHeight() : 0, mMinSize);
		}

		/* getHorizontalOffset() with getHorizontalGravity(): RIGHT for the start handle, LEFT for the end handle */
		private int getHorizontalOffset() {
			final int drawWidth = mDrawable != null ? mDrawable.getIntrinsicWidth() : 0;
			return mIsStartHandle ? getPreferredWidth() - drawWidth : 0;
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(getPreferredWidth(), getPreferredHeight());
		}

		@Override
		public void onDraw(Canvas c) {
			if (mDrawable == null)
				return;
			final int drawWidth = mDrawable.getIntrinsicWidth();
			final int left = getHorizontalOffset();
			mDrawable.setBounds(left, 0, left + drawWidth, mDrawable.getIntrinsicHeight());
			mDrawable.draw(c);
		}

		private int getCurrentCursorOffset() {
			return mIsStartHandle ? mTextView.getSelectionStart() : mTextView.getSelectionEnd();
		}

		boolean isShowing() {
			return mContainer.isShowing();
		}

		void hide() {
			mContainer.dismiss();
		}

		/* positionAtCursorOffset() and updatePosition(int, int, boolean, boolean): the popup at the handle's
		 * position in the window */
		void show() {
			final Layout layout = mTextView.getLayout();
			final int offset = getCurrentCursorOffset();
			if (layout == null || offset < 0) {
				hide();
				return;
			}
			final int line = layout.getLineForOffset(offset);
			mPositionX = (int)(layout.getPrimaryHorizontal(offset) - 0.5f) - mHotspotX - getHorizontalOffset();
			mPositionY = layout.getLineBottom(line);
			// Take TextView's padding and scroll into account.
			mPositionX += mTextView.getCompoundPaddingLeft() - mTextView.getScrollX();
			mPositionY += mTextView.getTotalPaddingTop() - mTextView.getScrollY();
			mTextView.getLocationInWindow(mTmp);
			final int x = mTmp[0] + mPositionX;
			final int y = mTmp[1] + mPositionY;
			if (isShowing()) {
				mContainer.update(x, y, -1, -1);
			} else {
				mContainer.showAtLocation(mTextView, Gravity.NO_GRAVITY, x, y);
			}
		}

		/* HandleView.onTouchEvent(): the touch keeps its distance to the handle; the floating toolbar hides while
		 * the handle is dragged (Editor.updateFloatingToolbarVisibility()) */
		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				mTextView.getLocationInWindow(mTmp);
				mLastParentX = mTmp[0];
				mLastParentY = mTmp[1];
				mTextView.getLocationOnScreen(mTmp);
				mLastParentXOnScreen = mTmp[0];
				mLastParentYOnScreen = mTmp[1];
				final float xInWindow = ev.getRawX() - mLastParentXOnScreen + mLastParentX;
				final float yInWindow = ev.getRawY() - mLastParentYOnScreen + mLastParentY;
				mTouchToWindowOffsetX = xInWindow - mPositionX;
				mTouchToWindowOffsetY = yInWindow - mPositionY;
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				if (mTextActionMode != null)
					mTextActionMode.hide(ActionMode.DEFAULT_HIDE_DURATION);
				final float xInWindow = ev.getRawX() - mLastParentXOnScreen + mLastParentX;
				final float yInWindow = ev.getRawY() - mLastParentYOnScreen + mLastParentY;
				final float newPosX = xInWindow - mTouchToWindowOffsetX + mHotspotX + getHorizontalOffset();
				final float newPosY = yInWindow - mTouchToWindowOffsetY + mTouchOffsetY;
				updatePosition(newPosX, newPosY);
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mTextActionMode != null)
					mTextActionMode.hide(0);
				break;
			}
			return true;
		}

		/* SelectionHandleView.updatePosition(float, float, boolean) by character: the offset at the point, kept
		 * one character away from the other end; positionAtCursorOffset() moves the handle */
		private void updatePosition(float x, float y) {
			int offset = mTextView.getOffsetForPosition(x, y);
			if (offset < 0)
				return;
			final int start = mTextView.getSelectionStart();
			final int end = mTextView.getSelectionEnd();
			if (mIsStartHandle) {
				offset = Math.min(offset, end - 1);
				if (offset != start) {
					setSelection(offset, end);
					mTextView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
				}
			} else {
				offset = Math.max(offset, start + 1);
				if (offset != end) {
					setSelection(start, offset);
					mTextView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
				}
			}
			mStartHandle.show();
			mEndHandle.show();
			if (mTextActionMode != null)
				mTextActionMode.invalidateContentRect();
		}
	}
}
