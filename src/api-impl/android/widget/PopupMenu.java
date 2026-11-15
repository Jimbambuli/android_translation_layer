/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.widget;

import android.atl.annotation.Export;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnTouchListener;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

/**
 * A PopupMenu displays a {@link Menu} in a modal popup window anchored to a
 * {@link View}. The popup will appear below the anchor view if there is room,
 * or above it if there is not. If the IME is visible the popup will not
 * overlap it until it is touched. Touching outside of the popup will dismiss
 * it.
 */

public class PopupMenu {
	private final Context mContext;
	private final View mAnchor;
	private OnMenuItemClickListener mMenuItemClickListener;
	private OnDismissListener mOnDismissListener;
	private OnTouchListener mDragListener;
	private int mGravity;
	private long popover;
	private MenuImpl menu;

	/**
	 * Constructor to create a new popup menu with an anchor view.
	 *
	 * @param context Context the popup menu is running in, through which it
	 *        can access the current theme, resources, etc.
	 * @param anchor Anchor view for this popup. The popup will appear below
	 *        the anchor if there is room, or above it if there is not.
	 */
	public PopupMenu(Context context, View anchor) {
		this(context, anchor, Gravity.NO_GRAVITY);
	}
	/**
	 * Constructor to create a new popup menu with an anchor view and alignment
	 * gravity.
	 *
	 * @param context Context the popup menu is running in, through which it
	 *        can access the current theme, resources, etc.
	 * @param anchor Anchor view for this popup. The popup will appear below
	 *        the anchor if there is room, or above it if there is not.
	 * @param gravity The {@link Gravity} value for aligning the popup with its
	 *        anchor.
	 */
	public PopupMenu(Context context, View anchor, int gravity) {
		this(context, anchor, gravity, R.attr.popupMenuStyle, 0);
	}
	/**
	 * Constructor a create a new popup menu with a specific style.
	 *
	 * @param context Context the popup menu is running in, through which it
	 *        can access the current theme, resources, etc.
	 * @param anchor Anchor view for this popup. The popup will appear below
	 *        the anchor if there is room, or above it if there is not.
	 * @param gravity The {@link Gravity} value for aligning the popup with its
	 *        anchor.
	 * @param popupStyleAttr An attribute in the current theme that contains a
	 *        reference to a style resource that supplies default values for
	 *        the popup window. Can be 0 to not look for defaults.
	 * @param popupStyleRes A resource identifier of a style resource that
	 *        supplies default values for the popup window, used only if
	 *        popupStyleAttr is 0 or can not be found in the theme. Can be 0
	 *        to not look for defaults.
	 */
	public PopupMenu(Context context, View anchor, int gravity, int popupStyleAttr,
	                 int popupStyleRes) {
		mContext = context;
		mAnchor = anchor;
		mGravity = gravity;
		menu = new MenuImpl();
		popover = native_buildPopover(menu.menu);
		int[] colors = PopupWindow.themeColors(context);
		native_setColors(popover, colors[0], colors[1]);
	}

	/**
	 * @return a {@link MenuInflater} that can be used to inflate menu items
	 *         from XML into the menu returned by {@link #getMenu()}
	 * @see #getMenu()
	 */
	public MenuInflater getMenuInflater() {
		return new MenuInflater(mContext);
	}

	/**
	 * Sets a listener that will be notified when the user selects an item from
	 * the menu.
	 *
	 * @param listener the listener to notify
	 */
	public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
		mMenuItemClickListener = listener;
	}

	/**
	 * Sets a listener that will be notified when this menu is dismissed.
	 *
	 * @param listener the listener to notify
	 */
	public void setOnDismissListener(OnDismissListener listener) {
		mOnDismissListener = listener;
	}

	/**
	 * Returns an {@link OnTouchListener} that can be added to the anchor view
	 * to implement drag-to-open behavior.
	 * <p>
	 * Drag-to-open (pressing the anchor and sliding into the menu) is not
	 * supported; the returned listener never consumes events, so the anchor's
	 * regular click handling stays intact.
	 *
	 * @return a touch listener that controls drag-to-open behavior
	 */
	public OnTouchListener getDragToOpenListener() {
		if (mDragListener == null) {
			mDragListener = new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return false;
				}
			};
		}
		return mDragListener;
	}

	protected native long native_init();
	protected native void native_insertItem(long menu, int position, String item, int id);
	protected native void native_insertSubmenu(long menu, int position, String item, long submenu);
	protected native void native_removeItem(long menu, int position);
	protected native long native_buildPopover(long menu);
	protected native void native_setColors(long popover, int background, int foreground);
	protected native void native_dismiss(long popover);
	protected native void native_show(long popover, long anchor, int anchorWidth, int anchorHeight, int gravity);

	/* every item has a key of its own for the GMenu action: item ids need not be unique (add(title)
	 * gives them all 0), so the id would always find the first item */
	private int nextKey;
	private final java.util.ArrayList<MenuItemImpl> keyed = new java.util.ArrayList<>();

	// callback from native code
	@Export
	protected void menuItemClickCallback(final int key) {
		if (key >= 0 && key < keyed.size())
			menu.invoke(keyed.get(key));
	}

	/**
	 * Inflate a menu resource into this PopupMenu. This is equivalent to
	 * calling {@code popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu())}.
	 *
	 * @param menuRes Menu resource to inflate
	 * @throws Exception
	 */
	public void inflate(int menuRes) throws Exception {
		getMenuInflater().inflate(menuRes, getMenu());
	}

	/* AOSP setForceShowIcon() (MenuPopupHelper): kept; ATL's menu popup shows the icons of its items whatever the setting */
	private boolean mForceShowIcon;

	public void setForceShowIcon(boolean forceShowIcon) {
		mForceShowIcon = forceShowIcon;
	}

	/**
	* Show the menu popup anchored to the view specified during construction.
	*
	* @see #dismiss()
	*/
	public void show() {
		System.out.println("PopupMenu.show() called");
		/* like MenuPopupHelper: below the anchor, aligned to its start unless END is requested */
		int gravity = Gravity.getAbsoluteGravity(mGravity == Gravity.NO_GRAVITY ? Gravity.START : mGravity,
		                                         mAnchor.getLayoutDirection());
		native_show(popover, mAnchor.widget, mAnchor.getWidth(), mAnchor.getHeight(), gravity);
	}

	public Menu getMenu() {
		return menu;
	}

	/**
	 * Sets the gravity used to align the popup menu to its anchor view.
	 *
	 * @param gravity the gravity used to align the popup window
	 */
	public void setGravity(int gravity) {
		mGravity = gravity;
	}

	/**
	 * @return the gravity used to align the popup window to its anchor view
	 */
	public int getGravity() {
		return mGravity;
	}

	/* AOSP's PopupMenu.dismiss(): dismisses the menu if it is showing */
	public void dismiss() {
		native_dismiss(popover);
	}

	/* AOSP's PopupMenu: MenuPopupHelper's OnDismissListener tells the PopupMenu's listener;
	 * called from native code when the popover is closed */
	@Export
	protected void dispatchDismiss() {
		if (mOnDismissListener != null) {
			mOnDismissListener.onDismiss(this);
		}
	}

	/**
	 * Interface responsible for receiving menu item click events if the items
	 * themselves do not have individual item click listeners.
	 */
	public interface OnMenuItemClickListener {
		/**
		 * This method will be invoked when a menu item is clicked if the item
		 * itself did not already handle the event.
		 *
		 * @param item the menu item that was clicked
		 * @return {@code true} if the event was handled, {@code false}
		 *         otherwise
		 */
		boolean onMenuItemClick(MenuItem item);
	}

	/**
	 * Callback interface used to notify the application that the menu has closed.
	 */
	public interface OnDismissListener {
		/**
		 * Called when the associated menu has been dismissed.
		 *
		 * @param menu the popup menu that was dismissed
		 */
		void onDismiss(PopupMenu menu);
	}

	private class MenuImpl implements Menu {
		long menu = native_init();

		List<MenuItemImpl> items = new ArrayList<>();
		int numVisibleItems = 0;

		/* keeps the items sorted by order, items with the same order in the order they were added */
		private void insert(MenuItemImpl item) {
			int index = items.size();
			while (index > 0 && items.get(index - 1).order > item.order)
				index--;
			items.add(index, item);
			item.setVisible(true);
		}

		@Override
		public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
			MenuItemImpl item = new MenuItemImpl(itemId, groupId, order, this, String.valueOf(title), null);
			insert(item);
			return item;
		}

		@Override
		public MenuItem add(int groupId, int itemId, int order, int titleRes) {
			return add(groupId, itemId, order, mContext.getText(titleRes));
		}

		@Override
		public MenuItem findItem(int id) {
			for (MenuItemImpl item : items) {
				if (item.id == id)
					return item;
				if (item.subMenu != null) {
					MenuItem found = item.subMenu.findItem(id);
					if (found != null)
						return found;
				}
			}
			return null;
		}

		@Override
		public MenuItem getItem(int index) {
			return items.get(index);
		}

		private void remove(MenuItemImpl item) {
			item.setVisible(false);
			items.remove(item);
		}

		@Override
		public void clear() {
			for (int i = items.size() - 1; i >= 0; i--)
				remove(items.get(i));
		}

		@Override
		public void removeGroup(int groupId) {
			for (int i = items.size() - 1; i >= 0; i--)
				if (items.get(i).groupId == groupId)
					remove(items.get(i));
		}

		@Override
		public SubMenu addSubMenu(int titleRes) {
			return addSubMenu(NONE, NONE, NONE, mContext.getText(titleRes));
		}

		@Override
		public MenuItem add(int titleRes) {
			return add(NONE, NONE, NONE, mContext.getText(titleRes));
		}

		@Override
		public MenuItem add(CharSequence title) {
			return add(NONE, NONE, NONE, title);
		}

		@Override
		public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
			for (MenuItemImpl item : items)
				if (item.groupId == group)
					item.setCheckable(checkable);
		}

		@Override
		public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
			SubMenuImpl submenu = new SubMenuImpl(itemId, groupId, order, this, String.valueOf(title));
			insert(submenu.item);
			return submenu;
		}

		@Override
		public void setGroupVisible(int group, boolean visible) {
			for (MenuItemImpl item : items)
				if (item.groupId == group)
					item.setVisible(visible);
		}

		@Override
		public void removeItem(int id) {
			for (MenuItemImpl item : items) {
				if (item.id == id) {
					remove(item);
					return;
				}
			}
		}

		@Override
		public int size() {
			return items.size();
		}

		@Override
		public boolean hasVisibleItems() {
			for (MenuItemImpl item : items)
				if (item.isVisible())
					return true;
			return false;
		}

		@Override
		public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
			return addSubMenu(groupId, itemId, order, mContext.getText(titleRes));
		}

		@Override
		public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
			return 0;
		}

		@Override
		public void close() {
			dismiss();
		}

		@Override
		public SubMenu addSubMenu(CharSequence title) {
			return addSubMenu(NONE, NONE, NONE, title);
		}

		// the GMenu based popup has no keyboard shortcuts and shows no disabled items

		@Override
		public boolean isShortcutKey(int keyCode, KeyEvent event) {
			return false;
		}

		@Override
		public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
			return false;
		}

		@Override
		public void setQwertyMode(boolean isQwerty) {}

		@Override
		public void setGroupEnabled(int group, boolean enabled) {
			for (MenuItemImpl item : items)
				if (item.groupId == group)
					item.setEnabled(enabled);
		}

		@Override
		public boolean performIdentifierAction(int id, int flags) {
			return invoke((MenuItemImpl)findItem(id));
		}

		boolean invoke(MenuItemImpl item) {
			if (item == null || item.subMenu != null || !item.enabled)
				return false;
			// like AOSP's MenuItemImpl.invoke(): the item's own listener first, then the menu's
			if (item.clickListener != null && item.clickListener.onMenuItemClick(item))
				return true;
			return mMenuItemClickListener != null && mMenuItemClickListener.onMenuItemClick(item);
		}
	}

	private class SubMenuImpl extends MenuImpl implements SubMenu {
		private MenuItemImpl item;

		public SubMenuImpl(int id, int groupId, int order, MenuImpl parent, String title) {
			item = new MenuItemImpl(id, groupId, order, parent, title, this);
		}

		@Override
		public MenuItem getItem() {
			return item;
		}

		// the GMenu based popup shows neither headers nor icons

		@Override
		public void clearHeader() {}

		@Override
		public SubMenu setIcon(Drawable icon) {
			return this;
		}

		@Override
		public SubMenu setIcon(int iconRes) {
			return this;
		}

		@Override
		public SubMenu setHeaderTitle(int titleRes) {
			return this;
		}

		@Override
		public SubMenu setHeaderTitle(CharSequence title) {
			return this;
		}

		@Override
		public SubMenu setHeaderIcon(int iconRes) {
			return this;
		}

		@Override
		public SubMenu setHeaderIcon(Drawable icon) {
			return this;
		}

		@Override
		public SubMenu setHeaderView(View view) {
			return this;
		}
	}

	private class MenuItemImpl implements MenuItem {
		private int id;
		private final int groupId;
		private final int order;
		private MenuImpl parent;
		private String title;
		private CharSequence titleCondensed;
		SubMenuImpl subMenu;
		int position; // position in list of visible items, or -1 if not visible
		private Intent intent;
		private char alphabeticShortcut;
		private char numericShortcut;
		private int alphabeticModifiers = KeyEvent.META_CTRL_ON;
		private int numericModifiers = KeyEvent.META_CTRL_ON;
		private CharSequence contentDescription;
		private CharSequence tooltipText;
		private ColorStateList iconTintList;
		private PorterDuff.Mode iconTintMode;
		private boolean checkable;
		private boolean checked;
		boolean enabled = true;
		private View actionView;
		private boolean actionViewExpanded;
		OnMenuItemClickListener clickListener;
		private OnActionExpandListener actionExpandListener;

		private final int key = nextKey++;

		private MenuItemImpl(int id, int groupId, int order, MenuImpl parent, String title, SubMenuImpl subMenu) {
			keyed.add(this);
			this.id = id;
			this.groupId = groupId;
			this.order = order;
			this.parent = parent;
			this.position = -1;
			this.title = title;
			this.subMenu = subMenu;
		}

		@Override
		public MenuItem setIcon(int iconRes) {
			return this;
		}

		@Override
		public MenuItem setIcon(Drawable icon) {
			return this;
		}

		@Override
		public MenuItem setVisible(boolean visible) {
			// GMenu doesn't support invisible items, so we remove them while they're not visible
			if (!visible && isVisible()) {
				parent.numVisibleItems--;
				for (int i = parent.items.size() - 1; i >= 0; i--) {
					MenuItemImpl item = parent.items.get(i);
					if (item != this && item.isVisible())
						item.position--;
					else if (item == this)
						break;
				}
				native_removeItem(parent.menu, position);
				position = -1;
			} else if (visible && !isVisible()) {
				position = parent.numVisibleItems++;
				for (int i = parent.items.size() - 1; i >= 0; i--) {
					MenuItemImpl item = parent.items.get(i);
					if (item != this && item.isVisible())
						position = item.position++;
					else if (item == this)
						break;
				}
				if (subMenu != null)
					native_insertSubmenu(parent.menu, position, title, subMenu.menu);
				else
					native_insertItem(parent.menu, position, title, key);
			}
			return this;
		}

		// the GMenu based popup shows plain items: check marks and the disabled state are kept, but not shown

		@Override
		public MenuItem setChecked(boolean checked) {
			this.checked = checked;
			return this;
		}

		@Override
		public MenuItem setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		@Override
		public MenuItem setCheckable(boolean checkable) {
			this.checkable = checkable;
			return this;
		}

		@Override
		public boolean isCheckable() {
			return checkable;
		}

		@Override
		public MenuItem setTitleCondensed(CharSequence titleCondensed) {
			this.titleCondensed = titleCondensed;
			return this;
		}

		@Override
		public MenuItem setTitle(CharSequence title) {
			this.title = String.valueOf(title);
			// GMenu items are immutable, so replace a visible item to show the new title
			if (isVisible()) {
				native_removeItem(parent.menu, position);
				if (subMenu != null)
					native_insertSubmenu(parent.menu, position, this.title, subMenu.menu);
				else
					native_insertItem(parent.menu, position, this.title, key);
			}
			return this;
		}

		// a popup menu has no action bar: action views are kept, but the item stays a plain item

		@Override
		public MenuItem setActionView(View actionView) {
			this.actionView = actionView;
			return this;
		}

		@Override
		public void setShowAsAction(int action) {}

		@Override
		public int getItemId() {
			return id;
		}

		@Override
		public int getGroupId() {
			return groupId;
		}

		@Override
		public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener listener) {
			clickListener = listener;
			return this;
		}

		@Override
		public MenuItem setTitle(int resId) {
			return setTitle(mContext.getText(resId));
		}

		@Override
		public boolean isVisible() {
			return position >= 0;
		}

		@Override
		public Drawable getIcon() {
			return new Drawable();
		}

		@Override
		public SubMenu getSubMenu() {
			return subMenu;
		}

		@Override
		public MenuItem setActionView(int resId) {
			return setActionView(android.view.LayoutInflater.from(mContext).inflate(resId, null, false));
		}

		@Override
		public View getActionView() {
			return actionView;
		}

		@Override
		public boolean hasSubMenu() {
			return subMenu != null;
		}

		@Override
		public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
			actionExpandListener = listener;
			return this;
		}

		@Override
		public boolean isChecked() {
			return checked;
		}

		@Override
		public MenuItem setShowAsActionFlags(int action) {
			return this;
		}

		@Override
		public MenuItem setAlphabeticShortcut(char alphaChar) {
			return setAlphabeticShortcut(alphaChar, KeyEvent.META_CTRL_ON);
		}

		@Override
		public MenuItem setShortcut(char numeric, char alpha) { return this; }

		@Override
		public int getOrder() {
			return order;
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}

		@Override
		public CharSequence getTitleCondensed() {
			return titleCondensed != null ? titleCondensed : title;
		}

		@Override
		public CharSequence getTitle() {
			return title;
		}

		@Override
		public MenuItem setNumericShortcut(char numericChar) {
			return setNumericShortcut(numericChar, KeyEvent.META_CTRL_ON);
		}

		@Override
		public boolean expandActionView() {
			if (actionView == null || actionViewExpanded)
				return false;
			if (actionExpandListener != null && !actionExpandListener.onMenuItemActionExpand(this))
				return false;
			actionViewExpanded = true;
			return true;
		}

		@Override
		public boolean collapseActionView() {
			if (!actionViewExpanded)
				return false;
			if (actionExpandListener != null && !actionExpandListener.onMenuItemActionCollapse(this))
				return false;
			actionViewExpanded = false;
			return true;
		}

		@Override
		public boolean isActionViewExpanded() {
			return actionViewExpanded;
		}

		@Override
		public MenuItem setIntent(Intent intent) {
			this.intent = intent;
			return this;
		}

		@Override
		public Intent getIntent() {
			return intent;
		}

		// the GMenu based popup shows neither icons, content descriptions nor tooltips; they are kept as AOSP's
		// MenuItemImpl keeps them

		@Override
		public MenuItem setIconTintList(ColorStateList tint) {
			iconTintList = tint;
			return this;
		}

		@Override
		public ColorStateList getIconTintList() {
			return iconTintList;
		}

		@Override
		public MenuItem setIconTintMode(PorterDuff.Mode tintMode) {
			iconTintMode = tintMode;
			return this;
		}

		@Override
		public PorterDuff.Mode getIconTintMode() {
			return iconTintMode;
		}

		@Override
		public MenuItem setContentDescription(CharSequence contentDescription) {
			this.contentDescription = contentDescription;
			return this;
		}

		@Override
		public CharSequence getContentDescription() {
			return contentDescription;
		}

		@Override
		public MenuItem setTooltipText(CharSequence tooltipText) {
			this.tooltipText = tooltipText;
			return this;
		}

		@Override
		public CharSequence getTooltipText() {
			return tooltipText;
		}

		@Override
		public int getAlphabeticModifiers() {
			return alphabeticModifiers;
		}

		@Override
		public int getNumericModifiers() {
			return numericModifiers;
		}

		@Override
		public ContextMenu.ContextMenuInfo getMenuInfo() {
			return null;
		}

		@Override
		public char getAlphabeticShortcut() {
			return alphabeticShortcut;
		}

		@Override
		public char getNumericShortcut() {
			return numericShortcut;
		}

		@Override
		public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
			alphabeticShortcut = alphaChar;
			alphabeticModifiers = KeyEvent.normalizeMetaState(alphaModifiers);
			return this;
		}

		@Override
		public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
			numericShortcut = numericChar;
			numericModifiers = KeyEvent.normalizeMetaState(numericModifiers);
			return this;
		}
	}
}
