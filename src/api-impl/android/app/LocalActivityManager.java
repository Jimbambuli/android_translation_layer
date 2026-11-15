package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* AOSP frameworks/base/core/java/android/app/LocalActivityManager.java. ATL cannot run an Activity inside a
 * parent's window (no ActivityThread, no Window per child), so the manager keeps AOSP's records, state machine
 * and id bookkeeping, but an activity is never instantiated: startActivity() throws
 * ActivityNotFoundException, so getActivity() and getCurrentActivity() find no activity, as in AOSP for an unknown id. */
@Deprecated
public class LocalActivityManager {
	static final int RESTORED = 0;
	static final int INITIALIZING = 1;
	static final int CREATED = 2;
	static final int STARTED = 3;
	static final int RESUMED = 4;
	static final int DESTROYED = 5;

	private static class LocalActivityRecord {
		LocalActivityRecord(String _id, Intent _intent) {
			id = _id;
			intent = _intent;
		}

		final String id;
		Intent intent;
		Bundle instanceState;
		int curState = RESTORED;
	}

	private final Activity mParent;
	private LocalActivityRecord mResumed;
	private final Map<String, LocalActivityRecord> mActivities = new HashMap<String, LocalActivityRecord>();
	private final ArrayList<LocalActivityRecord> mActivityArray = new ArrayList<LocalActivityRecord>();
	private boolean mSingleMode;
	private boolean mFinishing;
	private int mCurState = INITIALIZING;

	public LocalActivityManager(Activity parent, boolean singleMode) {
		mParent = parent;
		mSingleMode = singleMode;
	}

	private void moveToState(LocalActivityRecord r, int desiredState) {
		if (r.curState == RESTORED || r.curState == DESTROYED) {
			return;
		}
		r.curState = desiredState;
	}

	public Window startActivity(String id, Intent intent) {
		if (mCurState == INITIALIZING) {
			throw new IllegalStateException("Activities can't be added until the containing group has been created.");
		}
		LocalActivityRecord r = mActivities.get(id);
		if (r == null) {
			r = new LocalActivityRecord(id, intent);
			mActivities.put(id, r);
			mActivityArray.add(r);
		} else if (r.intent != null && r.intent.filterEquals(intent) && mCurState == RESUMED && mResumed == r) {
			r.intent = intent;
			return null;
		}
		r.intent = intent;
		if (mSingleMode && mResumed != null && mResumed != r && mCurState == RESUMED) {
			moveToState(mResumed, STARTED);
		}
		r.curState = INITIALIZING;
		// ATL cannot instantiate an Activity inside a parent window: like AOSP for an activity it cannot start
		// (LocalActivityManager.startActivityNow()), report it instead of returning a window
		throw new android.content.ActivityNotFoundException("Unable to start activity " + intent + " in a LocalActivityManager");
	}

	public Window destroyActivity(String id, boolean finish) {
		LocalActivityRecord r = mActivities.get(id);
		if (r != null) {
			if (mResumed == r) {
				mResumed = null;
			}
			if (finish) {
				mActivities.remove(id);
				mActivityArray.remove(r);
			}
		}
		return null;
	}

	public Activity getCurrentActivity() {
		return null;
	}

	public String getCurrentId() {
		return mResumed != null ? mResumed.id : null;
	}

	public Activity getActivity(String id) {
		return null;
	}

	public void dispatchCreate(Bundle state) {
		if (state != null) {
			for (String id : state.keySet()) {
				try {
					Bundle astate = state.getBundle(id);
					LocalActivityRecord r = mActivities.get(id);
					if (r != null) {
						r.instanceState = astate;
					} else {
						r = new LocalActivityRecord(id, null);
						r.instanceState = astate;
						mActivities.put(id, r);
						mActivityArray.add(r);
					}
				} catch (Exception e) {
					// Recover from the case where a LocalActivityManager is created without a saved state
				}
			}
		}
		mCurState = CREATED;
	}

	public Bundle saveInstanceState() {
		Bundle state = null;
		final int N = mActivityArray.size();
		for (int i = 0; i < N; i++) {
			final LocalActivityRecord r = mActivityArray.get(i);
			if (state == null) {
				state = new Bundle();
			}
			if (r.instanceState != null) {
				state.putBundle(r.id, r.instanceState);
			}
		}
		return state;
	}

	public void dispatchResume() {
		mCurState = RESUMED;
		if (mSingleMode) {
			if (mResumed != null) {
				moveToState(mResumed, RESUMED);
			}
		} else {
			final int N = mActivityArray.size();
			for (int i = 0; i < N; i++) {
				moveToState(mActivityArray.get(i), RESUMED);
			}
		}
	}

	public void dispatchPause(boolean finishing) {
		if (finishing) {
			mFinishing = true;
		}
		mCurState = STARTED;
		if (mSingleMode) {
			if (mResumed != null) {
				moveToState(mResumed, STARTED);
			}
		} else {
			final int N = mActivityArray.size();
			for (int i = 0; i < N; i++) {
				LocalActivityRecord r = mActivityArray.get(i);
				if (r.curState == RESUMED) {
					moveToState(r, STARTED);
				}
			}
		}
	}

	public void dispatchStop() {
		mCurState = CREATED;
		final int N = mActivityArray.size();
		for (int i = 0; i < N; i++) {
			moveToState(mActivityArray.get(i), CREATED);
		}
	}

	public void removeAllActivities() {
		dispatchDestroy(true);
	}

	public void dispatchDestroy(boolean finishing) {
		final int N = mActivityArray.size();
		for (int i = 0; i < N; i++) {
			moveToState(mActivityArray.get(i), DESTROYED);
		}
		mActivities.clear();
		mActivityArray.clear();
	}
}
