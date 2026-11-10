package android.preference;

import android.app.Fragment;

/**
 * Only the AOSP type and its nested callback interface: activities that implement
 * OnPreferenceStartFragmentCallback must be loadable. The preference hierarchy itself
 * (PreferenceScreen, inflation, list adapter) is not implemented in ATL.
 */
@Deprecated
public abstract class PreferenceFragment extends Fragment {

	/**
	 * Interface that PreferenceFragment's containing activity should
	 * implement to be able to process preference items that wish to
	 * switch to a new fragment.
	 */
	@Deprecated
	public interface OnPreferenceStartFragmentCallback {
		/**
		 * Called when the user has clicked on a Preference that has
		 * a fragment class name associated with it.
		 */
		boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref);
	}
}
