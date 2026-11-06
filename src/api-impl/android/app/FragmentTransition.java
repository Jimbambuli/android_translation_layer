package android.app;

import java.util.ArrayList;

/*
 * AOSP's FragmentTransition runs the enter/exit/shared element Transitions of a fragment transaction through
 * TransitionManager. ATL has no TransitionManager (android.transition.Transition only carries the properties
 * that XML sets), so the fragments change without a transition, like on a device that has none.
 */
class FragmentTransition {
	static void startTransitions(FragmentManagerImpl fragmentManager, ArrayList<BackStackRecord> records,
	                             ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, boolean isReordered) {
	}
}
