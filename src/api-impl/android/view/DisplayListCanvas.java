package android.view;

import android.graphics.Canvas;

/* AOSP DisplayListCanvas: every canvas ATL records into is a GskCanvas, which draws the node */
public abstract class DisplayListCanvas extends Canvas {

	public abstract void drawRenderNode(RenderNode node);

	@Override
	public boolean isHardwareAccelerated() {
		return true;
	}
}
