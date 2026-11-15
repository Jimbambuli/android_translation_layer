package android.app;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class AlertDialog extends Dialog implements DialogInterface {

	private Button[] buttons;

	private native void nativeConstruct(long ptr, long button_positive, long button_negative, long button_neutral);
	private native void nativeSetMessage(long ptr, String message);
	private native void nativeSetItems(long ptr, String[] items, DialogInterface.OnClickListener listener);
	private native void nativeSetView(long ptr);

	private boolean hasView;
	private ListView listView;

	public AlertDialog(Context context) {
		this(context, 0);
	}

	/* AOSP AlertDialog(Context, boolean, OnCancelListener) */
	public AlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		this(context, 0);
		setCancelable(cancelable);
		setOnCancelListener(cancelListener);
	}

	public AlertDialog(Context context, int themeResId) {
		super(context, themeResId);
		Context themeless = ATLLoadedApp.getSystemApplication().createContext(null, null, 0);
		// three buttons: positive, negative, neutral
		buttons = new Button[] {new Button(themeless), new Button(themeless), new Button(themeless)};
		buttons[0].setVisibility(View.GONE);
		buttons[1].setVisibility(View.GONE);
		buttons[2].setVisibility(View.GONE);
		nativeConstruct(nativePtr, buttons[0].widget, buttons[1].widget, buttons[2].widget);
	}

	public void setMessage(CharSequence message) {
		System.out.println("AlertDialog setMessage called with: '" + message + "'");
		nativeSetMessage(nativePtr, String.valueOf(message));
	}

	public void setButton(int whichButton, CharSequence text, OnClickListener listener) {
		setButton(whichButton, text, listener, null);
	}

	/* AOSP AlertDialog.setButton(int, CharSequence, Message): a click sends a copy of the message */
	public void setButton(int whichButton, CharSequence text, Message msg) {
		setButton(whichButton, text, null, msg);
	}

	/* AOSP deprecated setButton(CharSequence, ...): the positive button */
	@Deprecated
	public void setButton(CharSequence text, Message msg) {
		setButton(BUTTON_POSITIVE, text, msg);
	}

	@Deprecated
	public void setButton(CharSequence text, final OnClickListener listener) {
		setButton(BUTTON_POSITIVE, text, listener);
	}

	private void setButton(int whichButton, CharSequence text, OnClickListener listener, Message msg) {
		Button button = getButton(whichButton);
		button.setText(text);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// AOSP AlertController: a button without a listener only dismisses the dialog
				if (listener != null)
					listener.onClick(AlertDialog.this, whichButton);
				else if (msg != null)
					Message.obtain(msg).sendToTarget();
				dismiss();
			}
		});
		button.setVisibility(View.VISIBLE);
	}

	public Button getButton(int whichButton) {
		// BUTTON_POSITIVE = -1, BUTTON_NEGATIVE = -2, BUTTON_NEUTRAL = -3
		return buttons[-1 - whichButton];
	}

	/* AOSP's customPanel: the view goes below the message and the list, above the buttons */
	public void setView(View view) {
		setContentView(view);
		if (!hasView) {
			hasView = true;
			nativeSetView(nativePtr);
		}
	}

	/* AOSP AlertDialog.setView(View, int, int, int, int): the spacing is the padding of the custom panel around the view */
	public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
		FrameLayout panel = new FrameLayout(getContext());
		panel.setPadding(viewSpacingLeft, viewSpacingTop, viewSpacingRight, viewSpacingBottom);
		panel.addView(view);
		setView(panel);
	}

	/* AOSP AlertDialog.getListView(): the list from setAdapter(), setSingleChoiceItems() or
	 * setMultiChoiceItems(), null without one. A list from setItems() is the native one here, so
	 * there is no ListView for it. */
	public ListView getListView() {
		return listView;
	}

	/* AOSP AlertController.AlertParams.createListView() and AlertController.setupView(): a
	 * ListView with the adapter, the choice mode and the checked item. It takes the place of a
	 * view set with setView() (AOSP shows the list above the custom view). */
	private ListView createListView() {
		listView = new ListView(getContext());
		return listView;
	}

	private void showListView(ListAdapter adapter, int choiceMode, int checkedItem) {
		listView.setAdapter(adapter);
		listView.setChoiceMode(choiceMode);
		if (checkedItem > -1) {
			listView.setItemChecked(checkedItem, true);
			listView.setSelection(checkedItem);
		}
		setView(listView);
	}

	/* AOSP AlertController.CheckedItemAdapter */
	private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
		public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	public static class Builder {
		private AlertDialog dialog;
		private Context context;
		private boolean cancelable = true;
		private OnCancelListener onCancelListener;
		private DialogInterface.OnKeyListener onKeyListener;

		public Builder(Context context) {
			System.out.println("making an AlertDialog$Builder as we speak, my word!");
			this.context = context;
			dialog = new AlertDialog(context);
		}

		public Builder(Context context, int themeResId) {
			this.context = context;
			dialog = new AlertDialog(context, themeResId);
		}

		public Context getContext() {
			return context;
		}

		public AlertDialog.Builder setPositiveButton(int textId, DialogInterface.OnClickListener listener) {
			return setPositiveButton(dialog.getContext().getText(textId), listener);
		}

		public AlertDialog.Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
			System.out.println("AlertDialog.Builder setPositiveButton called with text: '" + text + "'");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, text, listener);
			return this;
		}

		public AlertDialog.Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
			System.out.println("AlertDialog.Builder setNegativeButton called with text: '" + text + "'");
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, text, listener);
			return this;
		}

		public AlertDialog.Builder setNegativeButton(int textId, DialogInterface.OnClickListener listener) {
			return setNegativeButton(dialog.getContext().getText(textId), listener);
		}

		public AlertDialog.Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
			System.out.println("AlertDialog.Builder setNeutralButton called with text: '" + text + "'");
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, text, listener);
			return this;
		}

		public AlertDialog.Builder setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
			return setNeutralButton(dialog.getContext().getText(textId), listener);
		}

		/* AOSP AlertDialog.Builder.create(): the dialog takes the builder's cancelable flag (true by
		 * default) and then also cancels on a touch outside */
		public AlertDialog.Builder setCancelable(boolean cancelable) {
			this.cancelable = cancelable;
			return this;
		}

		public AlertDialog.Builder setIcon(int iconId) {
			return this;
		}

		/* AOSP Builder.setIcon(Drawable): like setIcon(int), ATL's dialog shows no icon */
		public AlertDialog.Builder setIcon(android.graphics.drawable.Drawable icon) {
			return this;
		}

		/* AOSP Builder.setIconAttribute(): resolves the theme attribute to a drawable resource and uses it as the
		 * icon, like setIcon(int); ATL's dialog shows no icon, so this too only returns the builder */
		public AlertDialog.Builder setIconAttribute(int attrId) {
			return this;
		}

		public AlertDialog.Builder setTitle(CharSequence title) {
			System.out.println("AlertDialog.Builder setTitle called with: '" + title + "'");
			dialog.setTitle(title);
			return this;
		}

		public AlertDialog.Builder setTitle(int title) {
			return setTitle(dialog.getContext().getText(title));
		}

		public AlertDialog.Builder setMessage(CharSequence message) {
			System.out.println("AlertDialog.Builder setMessage called with: '" + message + "'");
			dialog.setMessage(message);
			return this;
		}

		public AlertDialog.Builder setMessage(int message) {
			return setMessage(dialog.getContext().getText(message));
		}

		public AlertDialog.Builder setView(View view) {
			dialog.setView(view);
			return this;
		}

		/* AOSP: the layout is inflated into the custom panel */
		public AlertDialog.Builder setView(int layoutResId) {
			return setView(LayoutInflater.from(dialog.getContext()).inflate(layoutResId, null));
		}

		public AlertDialog.Builder setItems(CharSequence[] items, final DialogInterface.OnClickListener listener) {
			String[] stringItems = new String[items.length];
			for (int i = 0; i < items.length; i++) {
				stringItems[i] = String.valueOf(items[i]);
			}
			/* AOSP AlertController.AlertParams.createListView(): a click on an item of a plain
			 * list calls the listener and dismisses the dialog; without a listener nothing happens */
			dialog.nativeSetItems(dialog.nativePtr, stringItems, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which) {
					if (listener != null) {
						listener.onClick(d, which);
						dialog.dismiss();
					}
				}
			});
			return this;
		}

		public AlertDialog.Builder setItems(int itemsId, final DialogInterface.OnClickListener listener) {
			return setItems(dialog.getContext().getResources().getTextArray(itemsId), listener);
		}

		/* AOSP AlertController.AlertParams.createListView(): the adapter's own views; a click calls
		 * the listener and dismisses the dialog */
		public AlertDialog.Builder setAdapter(ListAdapter adapter, final DialogInterface.OnClickListener listener) {
			ListView listView = dialog.createListView();
			if (listener != null) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
						listener.onClick(dialog, position);
						dialog.dismiss();
					}
				});
			}
			dialog.showListView(adapter, ListView.CHOICE_MODE_NONE, -1);
			return this;
		}

		/* AOSP: CHOICE_MODE_SINGLE, a click calls the listener but leaves the dialog open */
		public AlertDialog.Builder setSingleChoiceItems(ListAdapter adapter, int checkedItem, final DialogInterface.OnClickListener listener) {
			ListView listView = dialog.createListView();
			if (listener != null) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
						listener.onClick(dialog, position);
					}
				});
			}
			dialog.showListView(adapter, ListView.CHOICE_MODE_SINGLE, checkedItem);
			return this;
		}

		/* AOSP: a SimpleCursorAdapter on the label column, otherwise as the single choice list */
		public AlertDialog.Builder setSingleChoiceItems(Cursor cursor, int checkedItem, String labelColumn, final DialogInterface.OnClickListener listener) {
			return setSingleChoiceItems(new SimpleCursorAdapter(dialog.getContext(), com.android.internal.R.layout.select_dialog_singlechoice_material, cursor, new String[] {labelColumn}, new int[] {android.R.id.text1}), checkedItem, listener);
		}

		public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, final DialogInterface.OnClickListener listener) {
			Context context = dialog.getContext();
			return setSingleChoiceItems(new CheckedItemAdapter(context, com.android.internal.R.layout.select_dialog_singlechoice_material, android.R.id.text1, items), checkedItem, listener);
		}

		public AlertDialog.Builder setSingleChoiceItems(int itemsId, int checkedItem, final DialogInterface.OnClickListener listener) {
			return setSingleChoiceItems(dialog.getContext().getResources().getTextArray(itemsId), checkedItem, listener);
		}

		/* AOSP: CHOICE_MODE_MULTIPLE; checkedItems is the caller's array and follows the clicks */
		public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, final boolean[] checkedItems, final DialogInterface.OnMultiChoiceClickListener listener) {
			final ListView listView = dialog.createListView();
			ListAdapter adapter = new ArrayAdapter<CharSequence>(dialog.getContext(), com.android.internal.R.layout.select_dialog_multichoice_material, android.R.id.text1, items) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					View view = super.getView(position, convertView, parent);
					if (checkedItems != null && checkedItems[position])
						listView.setItemChecked(position, true);
					return view;
				}
			};
			if (listener != null) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
						if (checkedItems != null)
							checkedItems[position] = listView.isItemChecked(position);
						listener.onClick(dialog, position, listView.isItemChecked(position));
					}
				});
			}
			dialog.showListView(adapter, ListView.CHOICE_MODE_MULTIPLE, -1);
			return this;
		}

		/* AOSP: a CursorAdapter whose bindView() sets the label and checks the item when the column is 1 */
		public AlertDialog.Builder setMultiChoiceItems(Cursor cursor, final String isCheckedColumn, final String labelColumn, final DialogInterface.OnMultiChoiceClickListener listener) {
			final ListView listView = dialog.createListView();
			final LayoutInflater inflater = LayoutInflater.from(dialog.getContext());
			ListAdapter adapter = new CursorAdapter(dialog.getContext(), cursor, false) {
				private final int mLabelIndex = getCursor().getColumnIndexOrThrow(labelColumn);
				private final int mIsCheckedIndex = getCursor().getColumnIndexOrThrow(isCheckedColumn);

				@Override
				public void bindView(View view, Context context, Cursor cursor) {
					CheckedTextView text = (CheckedTextView) view.findViewById(android.R.id.text1);
					text.setText(cursor.getString(mLabelIndex));
					listView.setItemChecked(cursor.getPosition(), cursor.getInt(mIsCheckedIndex) == 1);
				}

				@Override
				public View newView(Context context, Cursor cursor, ViewGroup parent) {
					return inflater.inflate(com.android.internal.R.layout.select_dialog_multichoice_material, parent, false);
				}
			};
			if (listener != null) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
						listener.onClick(dialog, position, listView.isItemChecked(position));
					}
				});
			}
			dialog.showListView(adapter, ListView.CHOICE_MODE_MULTIPLE, -1);
			return this;
		}

		public AlertDialog.Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener) {
			return setMultiChoiceItems(dialog.getContext().getResources().getTextArray(itemsId), checkedItems, listener);
		}

		public Builder setOnCancelListener(OnCancelListener onCancelListener) {
			this.onCancelListener = onCancelListener;
			return this;
		}

		public Builder setOnKeyListener(DialogInterface.OnKeyListener onKeyListener) {
			this.onKeyListener = onKeyListener;
			return this;
		}

		public Builder setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
			dialog.setOnDismissListener(onDismissListener);
			return this;
		}

		public AlertDialog create() {
			dialog.setCancelable(cancelable);
			if (cancelable) {
				dialog.setCanceledOnTouchOutside(true);
			}
			dialog.setOnCancelListener(onCancelListener);
			if (onKeyListener != null) {
				dialog.setOnKeyListener(onKeyListener);
			}
			return dialog;
		}

		/* AOSP: create() and show() */
		public AlertDialog show() {
			create();
			dialog.show();
			return dialog;
		}
	}
}
