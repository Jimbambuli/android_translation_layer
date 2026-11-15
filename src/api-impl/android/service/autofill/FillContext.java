package android.service.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.util.ArrayMap;
import android.util.SparseIntArray;
import android.view.autofill.AutofillId;
import java.util.ArrayDeque;
import java.util.Objects;

/* AOSP FillContext; Parcel code left out, nothing in ATL parcels it */
public final class FillContext {

	private final int mRequestId;
	private final AssistStructure mStructure;
	private final AutofillId mFocusedId;
	private ArrayMap<AutofillId, AssistStructure.ViewNode> mViewNodeLookupTable;

	public FillContext(int requestId, AssistStructure structure, AutofillId focusedId) {
		this.mRequestId = requestId;
		this.mStructure = Objects.requireNonNull(structure);
		this.mFocusedId = Objects.requireNonNull(focusedId);
	}

	public int getRequestId() {
		return mRequestId;
	}

	public AssistStructure getStructure() {
		return mStructure;
	}

	public AutofillId getFocusedId() {
		return mFocusedId;
	}

	/* AOSP FillContext.findViewNodesByAutofillIds */
	public ViewNode[] findViewNodesByAutofillIds(AutofillId[] ids) {
		final ArrayDeque<ViewNode> nodesToProcess = new ArrayDeque<>();
		final ViewNode[] foundNodes = new AssistStructure.ViewNode[ids.length];
		final SparseIntArray missingNodeIndexes = new SparseIntArray(ids.length);
		for (int i = 0; i < ids.length; i++) {
			if (mViewNodeLookupTable != null) {
				if (mViewNodeLookupTable.containsKey(ids[i])) {
					foundNodes[i] = mViewNodeLookupTable.get(ids[i]);
				} else {
					missingNodeIndexes.put(i, /* ignored */ 0);
				}
			} else {
				missingNodeIndexes.put(i, /* ignored */ 0);
			}
		}
		final int numWindowNodes = mStructure.getWindowNodeCount();
		for (int i = 0; i < numWindowNodes; i++) {
			nodesToProcess.add(mStructure.getWindowNodeAt(i).getRootViewNode());
		}
		while (missingNodeIndexes.size() > 0 && !nodesToProcess.isEmpty()) {
			final ViewNode node = nodesToProcess.removeFirst();
			for (int i = 0; i < missingNodeIndexes.size(); i++) {
				final int index = missingNodeIndexes.keyAt(i);
				final AutofillId id = ids[index];
				if (id != null && id.equals(node.getAutofillId())) {
					foundNodes[index] = node;
					if (mViewNodeLookupTable == null) {
						mViewNodeLookupTable = new ArrayMap<>(ids.length);
					}
					mViewNodeLookupTable.put(id, node);
					missingNodeIndexes.removeAt(i);
					break;
				}
			}
			for (int i = 0; i < node.getChildCount(); i++) {
				nodesToProcess.addLast(node.getChildAt(i));
			}
		}
		for (int i = 0; i < missingNodeIndexes.size(); i++) {
			if (mViewNodeLookupTable == null) {
				mViewNodeLookupTable = new ArrayMap<>(missingNodeIndexes.size());
			}
			mViewNodeLookupTable.put(ids[missingNodeIndexes.keyAt(i)], null);
		}
		return foundNodes;
	}
}
