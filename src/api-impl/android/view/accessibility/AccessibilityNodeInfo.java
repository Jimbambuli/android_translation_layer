package android.view.accessibility;

import android.annotation.NonNull;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
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

	/* AOSP: constants, ids and flags of the node id and of the prefetching of the nodes of an accessibility
	 * service; the ids are what AccessibilityRecord.setSource() stores */
	public static final int ACTION_FOCUS = 0x00000001;
	public static final int ACTION_CLEAR_FOCUS = 0x00000002;
	public static final int ACTION_SELECT = 0x00000004;
	public static final int ACTION_CLEAR_SELECTION = 0x00000008;
	public static final int ACTION_CLICK = 0x00000010;
	public static final int ACTION_LONG_CLICK = 0x00000020;
	public static final int ACTION_ACCESSIBILITY_FOCUS = 0x00000040;
	public static final int ACTION_CLEAR_ACCESSIBILITY_FOCUS = 0x00000080;
	public static final int ACTION_SCROLL_FORWARD = 0x00001000;
	public static final int ACTION_SCROLL_BACKWARD = 0x00002000;
	public static final int UNDEFINED_ITEM_ID = Integer.MAX_VALUE;
	public static final int ROOT_ITEM_ID = Integer.MAX_VALUE - 1;
	private static final long VIRTUAL_DESCENDANT_ID_MASK = 0xffffffff00000000L;
	private static final int VIRTUAL_DESCENDANT_ID_SHIFT = 32;
	public static final long UNDEFINED_NODE_ID = makeNodeId(UNDEFINED_ITEM_ID, UNDEFINED_ITEM_ID);
	public static final long ROOT_NODE_ID = makeNodeId(ROOT_ITEM_ID, AccessibilityNodeProvider.HOST_VIEW_ID);
	public static final int FLAG_PREFETCH_ANCESTORS = 1 /* << 0 */;
	public static final int FLAG_PREFETCH_SIBLINGS = 1 << 1;
	public static final int FLAG_PREFETCH_DESCENDANTS_HYBRID = 1 << 2;
	public static final int FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST = 1 << 3;
	public static final int FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST = 1 << 4;
	public static final int FLAG_PREFETCH_UNINTERRUPTIBLE = 1 << 5;
	public static final int FLAG_PREFETCH_MASK = 0x0000003F;
	public static final int FLAG_PREFETCH_DESCENDANTS_MASK = 0x0000001C;

	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
	public @interface PrefetchingStrategy {
	}

	public static int getAccessibilityViewId(long accessibilityNodeId) {
		return (int)accessibilityNodeId;
	}

	public static int getVirtualDescendantId(long accessibilityNodeId) {
		return (int)((accessibilityNodeId & VIRTUAL_DESCENDANT_ID_MASK) >> VIRTUAL_DESCENDANT_ID_SHIFT);
	}

	public static long makeNodeId(int accessibilityViewId, int virtualDescendantId) {
		return (((long)virtualDescendantId) << VIRTUAL_DESCENDANT_ID_SHIFT) | accessibilityViewId;
	}

	/* AOSP keeps a pool of nodes, obtain() takes one from it; the pool is deprecated and recycle() only
	 * matters to it, so a new node is as good */
	public static AccessibilityNodeInfo obtain() {
		return new AccessibilityNodeInfo();
	}

	public static AccessibilityNodeInfo obtain(View source) {
		AccessibilityNodeInfo info = obtain();
		info.setSource(source);
		return info;
	}

	public static AccessibilityNodeInfo obtain(View root, int virtualDescendantId) {
		AccessibilityNodeInfo info = obtain();
		info.setSource(root, virtualDescendantId);
		return info;
	}

	public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo info) {
		AccessibilityNodeInfo copy = obtain();
		copy.init(info);
		return copy;
	}

	public void recycle() {}

	private long mSourceNodeId = UNDEFINED_NODE_ID;

	public void setSource(View source) {
		setSource(source, AccessibilityNodeProvider.HOST_VIEW_ID);
	}

	public void setSource(View root, int virtualDescendantId) {
		int rootAccessibilityViewId = root != null ? root.getAccessibilityViewId() : UNDEFINED_ITEM_ID;
		mSourceNodeId = makeNodeId(rootAccessibilityViewId, virtualDescendantId);
	}

	private void init(AccessibilityNodeInfo other) {
		mSourceNodeId = other.mSourceNodeId;
		mClassName = other.mClassName;
		mPackageName = other.mPackageName;
		mText = other.mText;
		mContentDescription = other.mContentDescription;
		mViewIdResourceName = other.mViewIdResourceName;
		mBoundsInParent.set(other.mBoundsInParent);
		mBoundsInScreen.set(other.mBoundsInScreen);
		mExtras = other.mExtras != null ? new Bundle(other.mExtras) : null;
		mParentRoot = other.mParentRoot;
		mParentVirtualDescendantId = other.mParentVirtualDescendantId;
		mFlags = other.mFlags;
	}

	private static final int FLAG_CHECKABLE = 1 << 0;
	private static final int FLAG_CHECKED = 1 << 1;
	private static final int FLAG_CLICKABLE = 1 << 2;
	private static final int FLAG_EDITABLE = 1 << 3;
	private static final int FLAG_ENABLED = 1 << 4;
	private static final int FLAG_FOCUSABLE = 1 << 5;
	private static final int FLAG_FOCUSED = 1 << 6;
	private static final int FLAG_SCROLLABLE = 1 << 7;
	private static final int FLAG_VISIBLE_TO_USER = 1 << 8;
	private static final int FLAG_LONG_CLICKABLE = 1 << 9;
	private static final int FLAG_SELECTED = 1 << 10;
	private static final int FLAG_PASSWORD = 1 << 11;
	private static final int FLAG_ACCESSIBILITY_FOCUSED = 1 << 12;
	private static final int FLAG_IMPORTANT = 1 << 13;
	private static final int FLAG_CONTEXT_CLICKABLE = 1 << 14;
	private static final int FLAG_DISMISSABLE = 1 << 15;
	private static final int FLAG_SCREEN_READER_FOCUSABLE = 1 << 16;
	private static final int FLAG_HEADING = 1 << 17;
	private static final int FLAG_DATA_SENSITIVE = 1 << 18;
	/* AOSP: the importance is true unless told otherwise */
	private int mFlags = FLAG_IMPORTANT;

	private void setFlag(int flag, boolean value) {
		mFlags = value ? (mFlags | flag) : (mFlags & ~flag);
	}

	private boolean getFlag(int flag) {
		return (mFlags & flag) != 0;
	}

	/* state as AOSP's AccessibilityNodeInfo; there is no accessibility service in ATL that reads it */
	private CharSequence mClassName;
	private CharSequence mPackageName;
	private CharSequence mText;
	private CharSequence mContentDescription;
	private String mViewIdResourceName;
	private final Rect mBoundsInParent = new Rect();
	private final Rect mBoundsInScreen = new Rect();
	private Bundle mExtras;
	private View mParentRoot;
	private int mParentVirtualDescendantId = View.NO_ID;

	public void setClassName(CharSequence className) {
		mClassName = className;
	}

	public CharSequence getClassName() {
		return mClassName;
	}

	public void setPackageName(CharSequence packageName) {
		mPackageName = packageName;
	}

	public CharSequence getPackageName() {
		return mPackageName;
	}

	public void setText(CharSequence text) {
		mText = text;
	}

	/** The text of this node (AOSP replaces clickable spans by accessibility ones; there is no service here). */
	public CharSequence getText() {
		return mText;
	}

	public void setViewIdResourceName(String viewIdResName) {
		mViewIdResourceName = viewIdResName;
	}

	public String getViewIdResourceName() {
		return mViewIdResourceName;
	}

	public void setBoundsInParent(Rect bounds) {
		mBoundsInParent.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public void getBoundsInParent(Rect outBounds) {
		outBounds.set(mBoundsInParent.left, mBoundsInParent.top, mBoundsInParent.right, mBoundsInParent.bottom);
	}

	public void setBoundsInScreen(Rect bounds) {
		mBoundsInScreen.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public void getBoundsInScreen(Rect outBounds) {
		outBounds.set(mBoundsInScreen.left, mBoundsInScreen.top, mBoundsInScreen.right, mBoundsInScreen.bottom);
	}

	/** Gets an unparcelled Bundle with additional data; created on first use, as in AOSP. */
	public Bundle getExtras() {
		if (mExtras == null) {
			mExtras = new Bundle();
		}
		return mExtras;
	}

	public void setParent(View parent) {
		setParent(parent, View.NO_ID);
	}

	public void setParent(View root, int virtualDescendantId) {
		mParentRoot = root;
		mParentVirtualDescendantId = virtualDescendantId;
	}

	public void addAction(int action) {}

	public void setCheckable(boolean checkable) {
		setFlag(FLAG_CHECKABLE, checkable);
	}

	public boolean isCheckable() {
		return getFlag(FLAG_CHECKABLE);
	}

	public void setChecked(boolean checked) {
		setFlag(FLAG_CHECKED, checked);
	}

	public boolean isChecked() {
		return getFlag(FLAG_CHECKED);
	}

	public void setClickable(boolean clickable) {
		setFlag(FLAG_CLICKABLE, clickable);
	}

	public boolean isClickable() {
		return getFlag(FLAG_CLICKABLE);
	}

	public void setContentDescription(CharSequence description) {
		mContentDescription = description;
	}

	public CharSequence getContentDescription() {
		return mContentDescription;
	}

	public void setEditable(boolean editable) {
		setFlag(FLAG_EDITABLE, editable);
	}

	public boolean isEditable() {
		return getFlag(FLAG_EDITABLE);
	}

	public void setEnabled(boolean enabled) {
		setFlag(FLAG_ENABLED, enabled);
	}

	public boolean isEnabled() {
		return getFlag(FLAG_ENABLED);
	}

	public void setFocusable(boolean focusable) {
		setFlag(FLAG_FOCUSABLE, focusable);
	}

	public boolean isFocusable() {
		return getFlag(FLAG_FOCUSABLE);
	}

	public void setFocused(boolean focused) {
		setFlag(FLAG_FOCUSED, focused);
	}

	public boolean isFocused() {
		return getFlag(FLAG_FOCUSED);
	}

	public void setScrollable(boolean scrollable) {
		setFlag(FLAG_SCROLLABLE, scrollable);
	}

	public boolean isScrollable() {
		return getFlag(FLAG_SCROLLABLE);
	}

	public void setVisibleToUser(boolean visible) {
		setFlag(FLAG_VISIBLE_TO_USER, visible);
	}

	public boolean isVisibleToUser() {
		return getFlag(FLAG_VISIBLE_TO_USER);
	}

	public void setTextSelection(int start, int end) {}

	public void setRangeInfo(RangeInfo info) {}

	public void setLongClickable(boolean longClickable) {
		setFlag(FLAG_LONG_CLICKABLE, longClickable);
	}

	public boolean isLongClickable() {
		return getFlag(FLAG_LONG_CLICKABLE);
	}

	public void setSelected(boolean selected) {
		setFlag(FLAG_SELECTED, selected);
	}

	public boolean isSelected() {
		return getFlag(FLAG_SELECTED);
	}

	public void setPassword(boolean password) {
		setFlag(FLAG_PASSWORD, password);
	}

	public boolean isPassword() {
		return getFlag(FLAG_PASSWORD);
	}

	public void setAccessibilityFocused(boolean accessibilityFocused) {
		setFlag(FLAG_ACCESSIBILITY_FOCUSED, accessibilityFocused);
	}

	public boolean isAccessibilityFocused() {
		return getFlag(FLAG_ACCESSIBILITY_FOCUSED);
	}

	public void setContextClickable(boolean contextClickable) {
		setFlag(FLAG_CONTEXT_CLICKABLE, contextClickable);
	}

	public boolean isContextClickable() {
		return getFlag(FLAG_CONTEXT_CLICKABLE);
	}

	public void setDismissable(boolean dismissable) {
		setFlag(FLAG_DISMISSABLE, dismissable);
	}

	public boolean isDismissable() {
		return getFlag(FLAG_DISMISSABLE);
	}

	public void setScreenReaderFocusable(boolean screenReaderFocusable) {
		setFlag(FLAG_SCREEN_READER_FOCUSABLE, screenReaderFocusable);
	}

	public boolean isScreenReaderFocusable() {
		return getFlag(FLAG_SCREEN_READER_FOCUSABLE);
	}

	public void setHeading(boolean heading) {
		setFlag(FLAG_HEADING, heading);
	}

	public boolean isHeading() {
		return getFlag(FLAG_HEADING);
	}

	public void setImportantForAccessibility(boolean important) {
		setFlag(FLAG_IMPORTANT, important);
	}

	public boolean isImportantForAccessibility() {
		return getFlag(FLAG_IMPORTANT);
	}

	public void setAccessibilityDataSensitive(boolean dataSensitive) {
		setFlag(FLAG_DATA_SENSITIVE, dataSensitive);
	}

	public boolean isAccessibilityDataSensitive() {
		return getFlag(FLAG_DATA_SENSITIVE);
	}
}
