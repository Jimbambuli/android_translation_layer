/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.graphics.Matrix;

import java.util.WeakHashMap;

/**
 * This view draws another View in an Overlay without changing the parent. It will not be drawn
 * by its parent because its visibility is set to INVISIBLE, but will be drawn
 * here using its render node. When the GhostView is set to INVISIBLE, the View it is
 * shadowing will become VISIBLE and when the GhostView becomes VISIBLE, the shadowed
 * view becomes INVISIBLE.
 *
 * ATL: AOSP draws the view with its RenderNode in the ViewOverlay of the host. ATL's views are GTK
 * widgets without a display list to replay and its ViewOverlay does not hold views, so the ghost is
 * only the bookkeeping of AOSP (reference count per view, addGhost/removeGhost/getGhost, the matrix)
 * and draws nothing: a view that ChangeTransform ghosts while it moves to another parent stays
 * visible at its place in the new parent and is animated there.
 *
 * @hide
 */
public class GhostView extends View {
	private static final WeakHashMap<View, GhostView> sGhosts = new WeakHashMap<>();

	private final View mView;
	private int mReferences;
	private final Matrix mMatrix = new Matrix();

	private GhostView(View view) {
		super(view.getContext());
		mView = view;
	}

	public void setMatrix(Matrix matrix) {
		mMatrix.set(matrix);
	}

	public static GhostView addGhost(View view, ViewGroup viewGroup, Matrix matrix) {
		if (!(view.getParent() instanceof ViewGroup)) {
			throw new IllegalArgumentException("Ghosted views must be parented by a ViewGroup");
		}
		GhostView ghostView;
		synchronized (sGhosts) {
			ghostView = sGhosts.get(view);
			if (ghostView == null) {
				ghostView = new GhostView(view);
				sGhosts.put(view, ghostView);
			}
		}
		if (matrix != null) {
			ghostView.setMatrix(matrix);
		}
		ghostView.mReferences++;
		return ghostView;
	}

	public static GhostView addGhost(View view, ViewGroup viewGroup) {
		return addGhost(view, viewGroup, null);
	}

	public static void removeGhost(View view) {
		synchronized (sGhosts) {
			GhostView ghostView = sGhosts.get(view);
			if (ghostView != null) {
				ghostView.mReferences--;
				if (ghostView.mReferences == 0) {
					sGhosts.remove(view);
				}
			}
		}
	}

	public static GhostView getGhost(View view) {
		synchronized (sGhosts) {
			return sGhosts.get(view);
		}
	}
}
