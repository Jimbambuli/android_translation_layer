package android.view.accessibility;

import android.annotation.NonNull;
import android.annotation.Nullable;
import java.lang.CharSequence;

public class AccessibilityNodeInfo {
	public static final class AccessibilityAction {
		public static final AccessibilityAction ACTION_CONTEXT_CLICK = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SET_PROGRESS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SHOW_ON_SCREEN = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_TO_POSITION = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PAGE_UP = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PAGE_DOWN = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PAGE_LEFT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PAGE_RIGHT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SHOW_TOOLTIP = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_HIDE_TOOLTIP = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_CLEAR_FOCUS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_CLEAR_SELECTION = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_COLLAPSE = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_COPY = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_CUT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_DRAG_CANCEL = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_DRAG_DROP = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_DRAG_START = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_EXPAND = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_IME_ENTER = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_NEXT_HTML_ELEMENT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PASTE = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PRESS_AND_HOLD = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_PREVIOUS_HTML_ELEMENT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_BACKWARD = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_FORWARD = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SCROLL_IN_DIRECTION = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SET_SELECTION = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SET_TEXT = new AccessibilityAction(0, null);
		public static final AccessibilityAction ACTION_SHOW_TEXT_SUGGESTIONS = new AccessibilityAction(0, null);

		public AccessibilityAction(int actionId, CharSequence label) {}

		public int getId() { return 0; }

		public CharSequence getLabel() { return null; }
	}

	/**
	 * Class with information if a node is a collection.
	 * <p>
	 * A collection of items has rows and columns and may be hierarchical.
	 * For example, a horizontal list is a collection with one column, as
	 * many rows as the list items, and is not hierarchical; A table is a
	 * collection with several rows, several columns, and is not hierarchical;
	 * A vertical tree is a hierarchical collection with one column and
	 * as many rows as the first level children.
	 * </p>
	 */
	public static final class CollectionInfo {
		/**
		 * Selection mode where items are not selectable.
		 */
		public static final int SELECTION_MODE_NONE = 0;

		/**
		 * Selection mode where a single item may be selected.
		 */
		public static final int SELECTION_MODE_SINGLE = 1;

		/**
		 * Selection mode where multiple items may be selected.
		 */
		public static final int SELECTION_MODE_MULTIPLE = 2;

		/**
		 * Constant to denote a missing collection count.
		 *
		 * This should be used for {@code mItemCount} and
		 * {@code mImportantForAccessibilityItemCount} when values for those fields are not known.
		 */
		public static final int UNDEFINED = -1;

		private int mRowCount;
		private int mColumnCount;
		private boolean mHierarchical;
		private int mSelectionMode;
		private int mItemCount;
		private int mImportantForAccessibilityItemCount;

		/**
		 * Instantiates a CollectionInfo that is a clone of another one.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionInfo#CollectionInfo} instead.
		 *
		 * @param other The instance to clone.
		 * @hide
		 */
		public static CollectionInfo obtain(CollectionInfo other) {
			return new CollectionInfo(other.mRowCount, other.mColumnCount, other.mHierarchical,
			                          other.mSelectionMode, other.mItemCount,
			                          other.mImportantForAccessibilityItemCount);
		}

		/**
		 * Obtains a pooled instance.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionInfo#CollectionInfo(int, int,
		 * boolean)} instead.
		 *
		 * @param rowCount The number of rows, or -1 if count is unknown.
		 * @param columnCount The number of columns, or -1 if count is unknown.
		 * @param hierarchical Whether the collection is hierarchical.
		 */
		public static CollectionInfo obtain(int rowCount, int columnCount,
		                                    boolean hierarchical) {
			return new CollectionInfo(rowCount, columnCount, hierarchical, SELECTION_MODE_NONE);
		}

		/**
		 * Obtains a pooled instance.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionInfo#CollectionInfo(int, int,
		 * boolean, int)} instead.
		 *
		 * @param rowCount The number of rows.
		 * @param columnCount The number of columns.
		 * @param hierarchical Whether the collection is hierarchical.
		 * @param selectionMode The collection's selection mode, one of:
		 *            <ul>
		 *            <li>{@link #SELECTION_MODE_NONE}
		 *            <li>{@link #SELECTION_MODE_SINGLE}
		 *            <li>{@link #SELECTION_MODE_MULTIPLE}
		 *            </ul>
		 */
		public static CollectionInfo obtain(int rowCount, int columnCount,
		                                    boolean hierarchical, int selectionMode) {
			return new CollectionInfo(rowCount, columnCount, hierarchical, selectionMode);
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowCount The number of rows.
		 * @param columnCount The number of columns.
		 * @param hierarchical Whether the collection is hierarchical.
		 */
		public CollectionInfo(int rowCount, int columnCount, boolean hierarchical) {
			this(rowCount, columnCount, hierarchical, SELECTION_MODE_NONE);
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowCount The number of rows.
		 * @param columnCount The number of columns.
		 * @param hierarchical Whether the collection is hierarchical.
		 * @param selectionMode The collection's selection mode.
		 */
		public CollectionInfo(int rowCount, int columnCount, boolean hierarchical,
		                      int selectionMode) {
			mRowCount = rowCount;
			mColumnCount = columnCount;
			mHierarchical = hierarchical;
			mSelectionMode = selectionMode;
			mItemCount = UNDEFINED;
			mImportantForAccessibilityItemCount = UNDEFINED;
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowCount The number of rows.
		 * @param columnCount The number of columns.
		 * @param hierarchical Whether the collection is hierarchical.
		 * @param selectionMode The collection's selection mode.
		 * @param itemCount The collection's item count, which includes items that are unimportant
		 *                  for accessibility. When ViewGroups map cleanly to both row and column
		 *                  semantics, clients should populate the row and column counts and
		 *                  optionally populate this field. In all other cases, clients should
		 *                  populate this field so that accessibility services can use it to relay
		 *                  the collection size to users. This should be set to {@code UNDEFINED} if
		 *                  the item count is not known.
		 * @param importantForAccessibilityItemCount The count of the collection's views considered
		 *                                           important for accessibility.
		 * @hide
		 */
		public CollectionInfo(int rowCount, int columnCount, boolean hierarchical,
		                      int selectionMode, int itemCount, int importantForAccessibilityItemCount) {
			mRowCount = rowCount;
			mColumnCount = columnCount;
			mHierarchical = hierarchical;
			mSelectionMode = selectionMode;
			mItemCount = itemCount;
			mImportantForAccessibilityItemCount = importantForAccessibilityItemCount;
		}

		/**
		 * Gets the number of rows.
		 *
		 * @return The row count, or -1 if count is unknown.
		 */
		public int getRowCount() {
			return mRowCount;
		}

		/**
		 * Gets the number of columns.
		 *
		 * @return The column count, or -1 if count is unknown.
		 */
		public int getColumnCount() {
			return mColumnCount;
		}

		/**
		 * Gets if the collection is a hierarchically ordered.
		 *
		 * @return Whether the collection is hierarchical.
		 */
		public boolean isHierarchical() {
			return mHierarchical;
		}

		/**
		 * Gets the collection's selection mode.
		 *
		 * @return The collection's selection mode, one of:
		 *         <ul>
		 *         <li>{@link #SELECTION_MODE_NONE}
		 *         <li>{@link #SELECTION_MODE_SINGLE}
		 *         <li>{@link #SELECTION_MODE_MULTIPLE}
		 *         </ul>
		 */
		public int getSelectionMode() {
			return mSelectionMode;
		}

		/**
		 * Gets the number of items in the collection.
		 *
		 * @return The count of items, which may be {@code UNDEFINED} if the count is not known.
		 */
		public int getItemCount() {
			return mItemCount;
		}

		/**
		 * Gets the number of items in the collection considered important for accessibility.
		 *
		 * @return The count of items important for accessibility, which may be {@code UNDEFINED}
		 * if the count is not known.
		 */
		public int getImportantForAccessibilityItemCount() {
			return mImportantForAccessibilityItemCount;
		}

		/**
		 * Previously would recycle this instance.
		 *
		 * @deprecated Object pooling has been discontinued. Calling this function now will have
		 * no effect.
		 */
		@Deprecated
		void recycle() {}

		private void clear() {
			mRowCount = 0;
			mColumnCount = 0;
			mHierarchical = false;
			mSelectionMode = SELECTION_MODE_NONE;
			mItemCount = UNDEFINED;
			mImportantForAccessibilityItemCount = UNDEFINED;
		}

		/**
		 * The builder for CollectionInfo.
		 */

		public static final class Builder {
			private int mRowCount = 0;
			private int mColumnCount = 0;
			private boolean mHierarchical = false;
			private int mSelectionMode;
			private int mItemCount = UNDEFINED;
			private int mImportantForAccessibilityItemCount = UNDEFINED;

			/**
			 * Creates a new Builder.
			 */
			public Builder() {
			}

			/**
			 * Sets the row count.
			 * @param rowCount The number of rows in the collection.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setRowCount(int rowCount) {
				mRowCount = rowCount;
				return this;
			}

			/**
			 * Sets the column count.
			 * @param columnCount The number of columns in the collection.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setColumnCount(int columnCount) {
				mColumnCount = columnCount;
				return this;
			}
			/**
			 * Sets whether the collection is hierarchical.
			 * @param hierarchical Whether the collection is hierarchical.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setHierarchical(boolean hierarchical) {
				mHierarchical = hierarchical;
				return this;
			}

			/**
			 * Sets the selection mode.
			 * @param selectionMode The selection mode.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setSelectionMode(int selectionMode) {
				mSelectionMode = selectionMode;
				return this;
			}

			/**
			 * Sets the number of items in the collection. Can be optionally set for ViewGroups with
			 * clear row and column semantics; should be set for all other clients.
			 *
			 * @param itemCount The number of items in the collection. This should be set to
			 *                  {@code UNDEFINED} if the item count is not known.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setItemCount(int itemCount) {
				mItemCount = itemCount;
				return this;
			}

			/**
			 * Sets the number of views considered important for accessibility.
			 * @param importantForAccessibilityItemCount The number of items important for
			 *                                            accessibility.
			 * @return This builder.
			 */
			@NonNull
			public CollectionInfo.Builder setImportantForAccessibilityItemCount(
			    int importantForAccessibilityItemCount) {
				mImportantForAccessibilityItemCount = importantForAccessibilityItemCount;
				return this;
			}

			/**
			 * Creates a new {@link CollectionInfo} instance.
			 */
			@NonNull
			public CollectionInfo build() {
				CollectionInfo collectionInfo = new CollectionInfo(mRowCount, mColumnCount,
				                                                   mHierarchical);
				collectionInfo.mSelectionMode = mSelectionMode;
				collectionInfo.mItemCount = mItemCount;
				collectionInfo.mImportantForAccessibilityItemCount =
				    mImportantForAccessibilityItemCount;
				return collectionInfo;
			}
		}
	}

	/**
	 * Class with information if a node is a collection item.
	 * <p>
	 * A collection item is contained in a collection, it starts at
	 * a given row and column in the collection, and spans one or
	 * more rows and columns. For example, a header of two related
	 * table columns starts at the first row and the first column,
	 * spans one row and two columns.
	 * </p>
	 */
	public static final class CollectionItemInfo {
		/**
		 * Instantiates a CollectionItemInfo that is a clone of another one.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionItemInfo#CollectionItemInfo}
		 * instead.
		 *
		 * @param other The instance to clone.
		 * @hide
		 */
		@Deprecated
		public static CollectionItemInfo obtain(CollectionItemInfo other) {
			return new CollectionItemInfo(other.mRowTitle, other.mRowIndex, other.mRowSpan,
			                              other.mColumnTitle, other.mColumnIndex, other.mColumnSpan, other.mHeading,
			                              other.mSelected);
		}

		/**
		 * Instantiates a new CollectionItemInfo.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionItemInfo#CollectionItemInfo(int,
		 * int, int, int, boolean)} instead.
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading. (Prefer
		 *                {@link AccessibilityNodeInfo#setHeading(boolean)}).
		 */
		@Deprecated
		public static CollectionItemInfo obtain(int rowIndex, int rowSpan,
		                                        int columnIndex, int columnSpan, boolean heading) {
			return new CollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading,
			                              false);
		}

		/**
		 * Instantiates a new CollectionItemInfo.
		 *
		 * @deprecated Object pooling has been discontinued. Create a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionItemInfo#CollectionItemInfo(int,
		 * int, int, int, boolean)} instead.
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading. (Prefer
		 *                {@link AccessibilityNodeInfo#setHeading(boolean)}).
		 * @param selected Whether the item is selected.
		 */
		@Deprecated
		public static CollectionItemInfo obtain(int rowIndex, int rowSpan,
		                                        int columnIndex, int columnSpan, boolean heading, boolean selected) {
			return new CollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading,
			                              selected);
		}

		/**
		 * Instantiates a new CollectionItemInfo.
		 *
		 * @deprecated Object pooling has been discontinued. Creates a new instance using the
		 * constructor {@link
		 * AccessibilityNodeInfo.CollectionItemInfo#CollectionItemInfo(int,
		 * int, int, int, boolean, boolean)} instead.
		 *
		 * @param rowTitle The row title at which the item is located.
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnTitle The column title at which the item is located.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading. (Prefer
		 *                {@link AccessibilityNodeInfo#setHeading(boolean)})
		 * @param selected Whether the item is selected.
		 * @removed
		 */
		@Deprecated
		@NonNull
		public static CollectionItemInfo obtain(@Nullable String rowTitle, int rowIndex,
		                                        int rowSpan, @Nullable String columnTitle, int columnIndex, int columnSpan,
		                                        boolean heading, boolean selected) {
			return new CollectionItemInfo(rowTitle, rowIndex, rowSpan, columnTitle, columnIndex,
			                              columnSpan, heading, selected);
		}

		private boolean mHeading;
		private int mColumnIndex;
		private int mRowIndex;
		private int mColumnSpan;
		private int mRowSpan;
		private boolean mSelected;
		private String mRowTitle;
		private String mColumnTitle;

		private CollectionItemInfo() {
			/* do nothing */
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading.
		 */
		public CollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan,
		                          boolean heading) {
			this(rowIndex, rowSpan, columnIndex, columnSpan, heading, false);
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading.
		 * @param selected Whether the item is selected.
		 */
		public CollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan,
		                          boolean heading, boolean selected) {
			this(null, rowIndex, rowSpan, null, columnIndex, columnSpan,
			     heading, selected);
		}

		/**
		 * Creates a new instance.
		 *
		 * @param rowTitle The row title at which the item is located.
		 * @param rowIndex The row index at which the item is located.
		 * @param rowSpan The number of rows the item spans.
		 * @param columnTitle The column title at which the item is located.
		 * @param columnIndex The column index at which the item is located.
		 * @param columnSpan The number of columns the item spans.
		 * @param heading Whether the item is a heading.
		 * @param selected Whether the item is selected.
		 * @hide
		 */
		public CollectionItemInfo(@Nullable String rowTitle, int rowIndex, int rowSpan,
		                          @Nullable String columnTitle, int columnIndex, int columnSpan, boolean heading,
		                          boolean selected) {
			mRowIndex = rowIndex;
			mRowSpan = rowSpan;
			mColumnIndex = columnIndex;
			mColumnSpan = columnSpan;
			mHeading = heading;
			mSelected = selected;
			mRowTitle = rowTitle;
			mColumnTitle = columnTitle;
		}

		/**
		 * Gets the column index at which the item is located.
		 *
		 * @return The column index.
		 */
		public int getColumnIndex() {
			return mColumnIndex;
		}

		/**
		 * Gets the row index at which the item is located.
		 *
		 * @return The row index.
		 */
		public int getRowIndex() {
			return mRowIndex;
		}

		/**
		 * Gets the number of columns the item spans.
		 *
		 * @return The column span.
		 */
		public int getColumnSpan() {
			return mColumnSpan;
		}

		/**
		 * Gets the number of rows the item spans.
		 *
		 * @return The row span.
		 */
		public int getRowSpan() {
			return mRowSpan;
		}

		/**
		 * Gets if the collection item is a heading. For example, section
		 * heading, table header, etc.
		 *
		 * @return If the item is a heading.
		 * @deprecated Use {@link AccessibilityNodeInfo#isHeading()}
		 */
		public boolean isHeading() {
			return mHeading;
		}

		/**
		 * Gets if the collection item is selected.
		 *
		 * @return If the item is selected.
		 */
		public boolean isSelected() {
			return mSelected;
		}

		/**
		 * Gets the row title at which the item is located.
		 *
		 * @return The row title.
		 */
		@Nullable
		public String getRowTitle() {
			return mRowTitle;
		}

		/**
		 * Gets the column title at which the item is located.
		 *
		 * @return The column title.
		 */
		@Nullable
		public String getColumnTitle() {
			return mColumnTitle;
		}

		/**
		 * Recycles this instance.
		 *
		 * @deprecated Object pooling has been discontinued. Calling this function now will have
		 * no effect.
		 */
		@Deprecated
		void recycle() {}

		private void clear() {
			mColumnIndex = 0;
			mColumnSpan = 0;
			mRowIndex = 0;
			mRowSpan = 0;
			mHeading = false;
			mSelected = false;
			mRowTitle = null;
			mColumnTitle = null;
		}

		/**
		 * Builder for creating {@link CollectionItemInfo} objects.
		 */
		public static final class Builder {
			private boolean mHeading;
			private int mColumnIndex;
			private int mRowIndex;
			private int mColumnSpan;
			private int mRowSpan;
			private boolean mSelected;
			private String mRowTitle;
			private String mColumnTitle;

			/**
			 * Creates a new Builder.
			 */
			public Builder() {
			}

			/**
			 * Sets the collection item is a heading.
			 *
			 * @param heading The heading state
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setHeading(boolean heading) {
				mHeading = heading;
				return this;
			}

			/**
			 * Sets the column index at which the item is located.
			 *
			 * @param columnIndex The column index
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setColumnIndex(int columnIndex) {
				mColumnIndex = columnIndex;
				return this;
			}

			/**
			 * Sets the row index at which the item is located.
			 *
			 * @param rowIndex The row index
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setRowIndex(int rowIndex) {
				mRowIndex = rowIndex;
				return this;
			}

			/**
			 * Sets the number of columns the item spans.
			 *
			 * @param columnSpan The number of columns spans
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setColumnSpan(int columnSpan) {
				mColumnSpan = columnSpan;
				return this;
			}

			/**
			 * Sets the number of rows the item spans.
			 *
			 * @param rowSpan The number of rows spans
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setRowSpan(int rowSpan) {
				mRowSpan = rowSpan;
				return this;
			}

			/**
			 * Sets the collection item is selected.
			 *
			 * @param selected The number of rows spans
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setSelected(boolean selected) {
				mSelected = selected;
				return this;
			}

			/**
			 * Sets the row title at which the item is located.
			 *
			 * @param rowTitle The row title
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setRowTitle(@Nullable String rowTitle) {
				mRowTitle = rowTitle;
				return this;
			}

			/**
			 * Sets the column title at which the item is located.
			 *
			 * @param columnTitle The column title
			 * @return This builder
			 */
			@NonNull
			public CollectionItemInfo.Builder setColumnTitle(@Nullable String columnTitle) {
				mColumnTitle = columnTitle;
				return this;
			}

			/**
			 * Builds and returns a {@link CollectionItemInfo}.
			 */
			@NonNull
			public CollectionItemInfo build() {
				CollectionItemInfo collectionItemInfo = new CollectionItemInfo();
				collectionItemInfo.mHeading = mHeading;
				collectionItemInfo.mColumnIndex = mColumnIndex;
				collectionItemInfo.mRowIndex = mRowIndex;
				collectionItemInfo.mColumnSpan = mColumnSpan;
				collectionItemInfo.mRowSpan = mRowSpan;
				collectionItemInfo.mSelected = mSelected;
				collectionItemInfo.mRowTitle = mRowTitle;
				collectionItemInfo.mColumnTitle = mColumnTitle;

				return collectionItemInfo;
			}
		}
	}

	public class RangeInfo {
		public static final int RANGE_TYPE_INT = 0;
		public static final int RANGE_TYPE_FLOAT = 1;
		public static final int RANGE_TYPE_PERCENT = 2;
	}

	public static AccessibilityNodeInfo obtain() { return null; }

	public void setClassName(CharSequence className) {}

	public void addAction(int action) {}

	public void setCheckable(boolean checkable) {}

	public void setChecked(boolean checked) {}

	public void setClickable(boolean clickable) {}

	public void setContentDescription(CharSequence description) {}

	public void setEditable(boolean editable) {}

	public void setEnabled(boolean enabled) {}

	public void setFocusable(boolean focusable) {}

	public void setFocused(boolean focused) {}

	public void setScrollable(boolean scrollable) {}

	public void setVisibleToUser(boolean visible) {}

	public void setTextSelection(int start, int end) {}

	public void setRangeInfo(RangeInfo info) {}
}
