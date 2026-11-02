package android.graphics;

import android.atl.GskCanvas;

/**
 * A Canvas implementation that records view system drawing operations for deferred rendering.
 * This is returned by {@link RenderNode#beginRecording()} and is tracked by the RenderNode for
 * recording.
 *
 * In ATL it records into a GTK snapshot, like the canvas of android.view.RenderNode; a render
 * node drawn into it becomes a child of the node that records.
 */
public final class RecordingCanvas extends GskCanvas {
	private final RenderNode node;

	RecordingCanvas(RenderNode node, long snapshot) {
		super(snapshot);
		this.node = node;
	}

	@Override
	public void drawRenderNode(android.view.RenderNode renderNode) {
		node.addChild(snapshot, renderNode);
	}

	@Override
	public void drawRenderNode(RenderNode renderNode) {
		node.addChild(snapshot, renderNode);
	}
}
