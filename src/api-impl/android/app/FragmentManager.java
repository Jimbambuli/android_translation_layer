package android.app;

public class FragmentManager {

	private Activity activity;

	public FragmentManager(Activity activity) {
		this.activity = activity;
	}

	/* transactions take effect immediately (see FragmentTransaction.add()) */
	public Fragment findFragmentByTag(String tag) {
		if (tag == null)
			return null;
		for (Fragment fragment : activity.fragments) {
			if (tag.equals(fragment.tag))
				return fragment;
		}
		return null;
	}

	public FragmentTransaction beginTransaction() {
		return new FragmentTransaction(activity);
	}

	public boolean executePendingTransactions() {
		return false;
	}
}
