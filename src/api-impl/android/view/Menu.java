package android.view;

import android.content.ComponentName;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.MenuItem;

public interface Menu {

	/* AOSP: the modifiers a shortcut can have */
	static final int SUPPORTED_MODIFIERS_MASK = KeyEvent.META_META_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON
	                                            | KeyEvent.META_SHIFT_ON | KeyEvent.META_SYM_ON | KeyEvent.META_FUNCTION_ON;
	public static final int NONE = 0;

	public MenuItem add(int groupId, int itemId, int order, CharSequence title);

	public MenuItem add(int groupId, int itemId, int order, int titleRes);

	public MenuItem findItem(int id);

	public MenuItem getItem(int id);

	public void clear();

	public void removeGroup(int groupId);

	public SubMenu addSubMenu(int id);

	public MenuItem add(int id);

	public MenuItem add(CharSequence text);

	public void setGroupCheckable(int group, boolean checkable, boolean exclusive);

	public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title);

	public void setGroupVisible(int group, boolean visible);

	public void removeItem(int id);

	public int size();

	public boolean hasVisibleItems();

	public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes);

	public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems);

	public void close();

	public SubMenu addSubMenu(CharSequence title);

	public boolean isShortcutKey(int keyCode, KeyEvent event);

	public boolean performIdentifierAction(int id, int flags);

	public boolean performShortcut(int keyCode, KeyEvent event, int flags);

	public void setGroupEnabled(int group, boolean enabled);

	public void setQwertyMode(boolean isQwerty);

	/* AOSP Menu.setGroupDividerEnabled(): default method with empty body */
	default void setGroupDividerEnabled(boolean groupDividerEnabled) {
	}
}
