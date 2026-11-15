package android.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public interface MenuItem {

	public interface OnMenuItemClickListener {
		public boolean onMenuItemClick(MenuItem item);
	}

	public interface OnActionExpandListener {
		public boolean onMenuItemActionExpand(MenuItem item);
		public boolean onMenuItemActionCollapse(MenuItem item);
	}

	public MenuItem setIcon(int iconRes);

	public MenuItem setVisible(boolean visible);

	public MenuItem setChecked(boolean checked);

	public MenuItem setEnabled(boolean enabled);

	public MenuItem setCheckable(boolean checkable);

	public boolean isCheckable();

	public MenuItem setTitleCondensed(CharSequence titleCondensed);

	public MenuItem setTitle(CharSequence title);

	public MenuItem setActionView(View actionView);

	public void setShowAsAction(int action);

	public int getItemId();

	public int getGroupId();

	public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener listener);

	public MenuItem setTitle(int resId);

	public boolean isVisible();

	public Drawable getIcon();

	public SubMenu getSubMenu();

	public MenuItem setActionView(int resId);

	public View getActionView();

	public boolean hasSubMenu();

	public MenuItem setOnActionExpandListener(OnActionExpandListener listener);

	public MenuItem setIcon(Drawable icon);

	public boolean isChecked();

	public MenuItem setShowAsActionFlags(int action);

	public MenuItem setAlphabeticShortcut(char alphaChar);

	public MenuItem setShortcut(char numeric, char alpha);

	public int getOrder();

	public boolean isEnabled();

	public CharSequence getTitleCondensed();

	public CharSequence getTitle();

	public MenuItem setNumericShortcut(char numericChar);

	public boolean expandActionView();

	public boolean collapseActionView();

	public boolean isActionViewExpanded();

	public MenuItem setIntent(Intent intent);

	public MenuItem setIconTintList(ColorStateList tint);

	public MenuItem setIconTintMode(PorterDuff.Mode tintMode);

	public Intent getIntent();

	public ContextMenu.ContextMenuInfo getMenuInfo();

	public char getAlphabeticShortcut();

	public char getNumericShortcut();

	public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers);

	public MenuItem setNumericShortcut(char numericChar, int numericModifiers);

	public MenuItem setContentDescription(CharSequence contentDescription);

	public MenuItem setTooltipText(CharSequence tooltipText);

	/* the API 26 defaults of AOSP's interface (an implementation that keeps the values overrides them) */
	public default ColorStateList getIconTintList() {
		return null;
	}

	public default PorterDuff.Mode getIconTintMode() {
		return null;
	}

	public default MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers) {
		if ((alphaModifiers & Menu.SUPPORTED_MODIFIERS_MASK) == KeyEvent.META_CTRL_ON
		    && (numericModifiers & Menu.SUPPORTED_MODIFIERS_MASK) == KeyEvent.META_CTRL_ON) {
			return setShortcut(numericChar, alphaChar);
		} else {
			return this;
		}
	}

	public default int getNumericModifiers() {
		return KeyEvent.META_CTRL_ON;
	}

	public default int getAlphabeticModifiers() {
		return KeyEvent.META_CTRL_ON;
	}

	public default CharSequence getContentDescription() {
		return null;
	}

	public default CharSequence getTooltipText() {
		return null;
	}

	/* AOSP MenuItem.setActionProvider()/getActionProvider() are abstract; MenuItem
	 * implementations that support action providers override these (ATL's PopupMenu
	 * item does not yet), the defaults keep the provider unset */
	public default MenuItem setActionProvider(ActionProvider actionProvider) {
		return this;
	}

	public default ActionProvider getActionProvider() {
		return null;
	}
}
