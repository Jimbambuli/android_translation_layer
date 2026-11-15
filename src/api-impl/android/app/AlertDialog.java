package android.app;

import android.atl.ATLLoadedApp;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

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
		Button button = getButton(whichButton);
		button.setText(text);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// AOSP AlertController: a button without a listener only dismisses the dialog
				if (listener != null)
					listener.onClick(AlertDialog.this, whichButton);
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

		public AlertDialog.Builder setCancelable(boolean cancelable) {
			return this;
		}

		public AlertDialog.Builder setIcon(int iconId) {
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

		public AlertDialog.Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener) {
			return setMultiChoiceItems(dialog.getContext().getResources().getTextArray(itemsId), checkedItems, listener);
		}

		public Builder setOnCancelListener(OnCancelListener onCancelListener) {
			return this;
		}

		public Builder setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
			dialog.setOnDismissListener(onDismissListener);
			return this;
		}

		public AlertDialog create() {
			return dialog;
		}

		public AlertDialog show() {
			dialog.show();
			return dialog;
		}
	}
}
