package android.app;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/*
 * AOSP's DialogFragment on ATL's Fragment: a Fragment that shows a Dialog. ATL's fragments have no
 * saved state, back stack or separate container, so show() adds the fragment, creates the dialog
 * (onCreateDialog() or a Dialog with the theme and the content of onCreateView()) and shows it;
 * dismiss() removes it again and runs the rest of the lifecycle.
 */
public class DialogFragment extends Fragment implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
	public static final int STYLE_NORMAL = 0;
	public static final int STYLE_NO_TITLE = 1;
	public static final int STYLE_NO_FRAME = 2;
	public static final int STYLE_NO_INPUT = 3;

	int mStyle = STYLE_NORMAL;
	int mTheme = 0;
	boolean mCancelable = true;
	boolean mShowsDialog = true;
	boolean mDismissed;
	boolean mShownByMe;
	Dialog mDialog;

	public DialogFragment() {
	}

	public void setStyle(int style, int theme) {
		mStyle = style;
		if (mStyle == STYLE_NO_FRAME || mStyle == STYLE_NO_INPUT) {
			mTheme = android.R.style.Theme_Panel;
		}
		if (theme != 0) {
			mTheme = theme;
		}
	}

	public void show(FragmentManager manager, String tag) {
		mDismissed = false;
		mShownByMe = true;
		FragmentTransaction ft = manager.beginTransaction();
		ft.add(this, tag);
		ft.commit();
		start();
	}

	public int show(FragmentTransaction transaction, String tag) {
		mDismissed = false;
		mShownByMe = true;
		transaction.add(this, tag);
		int id = transaction.commit();
		start();
		return id;
	}

	/* what the Activity does for fragments added in a layout: attach, create, show */
	private void start() {
		if (activity == null)
			return;
		onAttach(activity);
		onCreate(null);
		if (!mShowsDialog)
			return;
		mDialog = onCreateDialog(null);
		if (mDialog == null)
			return;
		setupDialog(mDialog, mStyle);
		View content = onCreateView(activity.getLayoutInflater(), null, null);
		if (content != null) {
			view = content;
			mDialog.setContentView(content);
			onViewCreated(content, null);
		}
		mDialog.setOwnerActivity(activity);
		mDialog.setCancelable(mCancelable);
		mDialog.setOnCancelListener(this);
		mDialog.setOnDismissListener(this);
		onActivityCreated(null);
		onStart();
		mDialog.show();
		onResume();
	}

	public void setupDialog(Dialog dialog, int style) {
		switch (style) {
			case STYLE_NO_INPUT:
				dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
				// fall through
			case STYLE_NO_FRAME:
			case STYLE_NO_TITLE:
				dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		}
	}

	public void dismiss() {
		dismissInternal(false);
	}

	public void dismissAllowingStateLoss() {
		dismissInternal(true);
	}

	void dismissInternal(boolean allowStateLoss) {
		if (mDismissed)
			return;
		mDismissed = true;
		mShownByMe = false;
		if (mDialog != null) {
			Dialog dialog = mDialog;
			mDialog = null;
			dialog.setOnDismissListener(null);
			dialog.dismiss();
			onDismiss(dialog);
		}
		onPause();
		onStop();
		onDestroyView();
		view = null;
		onDestroy();
		onDetach();
		if (activity != null)
			activity.fragments.remove(this);
	}

	public Dialog getDialog() {
		return mDialog;
	}

	public int getTheme() {
		return mTheme;
	}

	public void setCancelable(boolean cancelable) {
		mCancelable = cancelable;
		if (mDialog != null)
			mDialog.setCancelable(cancelable);
	}

	public boolean isCancelable() {
		return mCancelable;
	}

	public void setShowsDialog(boolean showsDialog) {
		mShowsDialog = showsDialog;
	}

	public boolean getShowsDialog() {
		return mShowsDialog;
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(getActivity(), getTheme());
	}

	@Override
	public void onCancel(DialogInterface dialog) {
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (!mDismissed) {
			// dismissed by the user: the same end as dismiss()
			mDismissed = true;
			mShownByMe = false;
			mDialog = null;
			onPause();
			onStop();
			onDestroyView();
			view = null;
			onDestroy();
			onDetach();
			if (activity != null)
				activity.fragments.remove(this);
		}
	}
}
