package android.transition;

import junit.framework.TestCase;

public class TransitionSetTest extends TestCase {
	public void testTransitionSet() {
		TransitionSet set = new TransitionSet();
		set.addTarget(3).addTarget(5).excludeTarget(9, true).setDuration(250);
		assertEquals(2, set.getTargetIds().size());
		assertEquals(250, set.getDuration());
		android.transition.Transition.TransitionListener l = new android.transition.TransitionListenerAdapter() {};
		set.addListener(l);
		set.removeListener(l);
		set.addTarget("name").addTarget(String.class);
		assertEquals(1, set.getTargetNames().size());
		assertEquals(1, set.getTargetTypes().size());
	}
}
