package android.graphics;

/* AOSP android.graphics.HardwareRenderer. ATL has no render thread: the root node holds the content as in AOSP
 * (setContentRoot()), and FrameRenderRequest.syncAndDraw() draws it right away into a canvas of the surface
 * (Surface.lockHardwareCanvas()/unlockCanvasAndPost()). */
public class HardwareRenderer {
	public static void setContextPriority(int priority) {}

	public static final int SYNC_OK = 0;
	public static final int SYNC_REDRAW_REQUESTED = 1 << 0;
	public static final int SYNC_LOST_SURFACE_REWARD_IF_FOUND = 1 << 1;
	public static final int SYNC_CONTEXT_IS_STOPPED = 1 << 2;
	public static final int SYNC_FRAME_DROPPED = 1 << 3;

	private final RenderNode mRootNode;
	private android.view.Surface mSurface;

	public HardwareRenderer() {
		mRootNode = new RenderNode("RootNode");
		mRootNode.setClipToBounds(false);
	}

	/* AOSP HardwareRenderer.setContentRoot(): the root node records a drawRenderNode() of the content */
	public void setContentRoot(RenderNode content) {
		RecordingCanvas canvas = mRootNode.beginRecording();
		if (content != null) {
			canvas.drawRenderNode(content);
		}
		mRootNode.endRecording();
	}

	/* AOSP HardwareRenderer.setSurface() -> setSurface(surface, false) */
	public void setSurface(android.view.Surface surface) {
		if (surface != null && !surface.isValid()) {
			throw new IllegalArgumentException("Surface is invalid. surface.isValid() == false.");
		}
		mSurface = surface;
	}

	/* AOSP HardwareRenderer.destroy(): releases the surface and the drawing resources; the renderer can be used
	 * again after a new setSurface() */
	public void destroy() {
		mSurface = null;
	}

	private boolean mOpaque = true;
	private boolean mStopped;
	private static boolean sDrawingEnabled = true;

	/* AOSP stop()/start() -> nSetStopped(): a stopped renderer draws no frames (syncAndDraw() reports
	 * SYNC_CONTEXT_IS_STOPPED) */
	public void stop() {
		mStopped = true;
	}

	public void start() {
		mStopped = false;
	}

	/* AOSP setOpaque()/isOpaque(); ATL's buffers keep their alpha either way */
	public void setOpaque(boolean opaque) {
		mOpaque = opaque;
	}

	public boolean isOpaque() {
		return mOpaque;
	}

	/* AOSP setDrawingEnabled()/isDrawingEnabled() -> nSetDrawingEnabled(): with drawing off no frame is drawn */
	public static void setDrawingEnabled(boolean drawingEnabled) {
		sDrawingEnabled = drawingEnabled;
	}

	public static boolean isDrawingEnabled() {
		return sDrawingEnabled;
	}

	/* AOSP setName() (a name for the debug output), notifyFramePending() (a hint for the scheduling),
	 * clearContent() (frees the GPU resources): nothing to do here */
	public void setName(String name) {}

	public void notifyFramePending() {}

	public void clearContent() {}

	private final FrameRenderRequest mRenderRequest = new FrameRenderRequest();

	/* AOSP HardwareRenderer.createRenderRequest(): the one request of the renderer, reset */
	public FrameRenderRequest createRenderRequest() {
		mRenderRequest.reset();
		return mRenderRequest;
	}

	/* AOSP HardwareRenderer.FrameRenderRequest. The vsync time and the frame commit callback steer the render
	 * thread, which ATL does not have; the drawing is done when syncAndDraw() returns, so waiting for present
	 * changes nothing. */
	public final class FrameRenderRequest {
		private boolean mWaitForPresent;
		private java.util.concurrent.Executor mCommitExecutor;
		private Runnable mCommitCallback;

		private FrameRenderRequest() {}

		private void reset() {
			mWaitForPresent = false;
		}

		public FrameRenderRequest setVsyncTime(long vsyncTime) {
			return this;
		}

		/* AOSP nSetFrameCommitCallback(): run on the executor once the next frame is done, drawn or not */
		public FrameRenderRequest setFrameCommitCallback(java.util.concurrent.Executor executor, Runnable frameCommitCallback) {
			mCommitExecutor = executor;
			mCommitCallback = frameCommitCallback;
			return this;
		}

		public FrameRenderRequest setWaitForPresent(boolean shouldWait) {
			mWaitForPresent = shouldWait;
			return this;
		}

		/* AOSP syncAndDraw() -> hwui DrawFrameTask (not in the local sources): without a usable surface the frame
		 * is dropped and the lost surface reported */
		public int syncAndDraw() {
			int result = SYNC_OK;
			android.view.Surface surface = mSurface;
			if (mStopped) {
				result = SYNC_CONTEXT_IS_STOPPED | SYNC_FRAME_DROPPED;
			} else if (surface == null || !surface.isValid()) {
				result = SYNC_LOST_SURFACE_REWARD_IF_FOUND | SYNC_FRAME_DROPPED;
			} else if (!sDrawingEnabled) {
				result = SYNC_FRAME_DROPPED;
			} else {
				Canvas canvas = surface.lockHardwareCanvas();
				try {
					canvas.drawRenderNode(mRootNode);
				} finally {
					surface.unlockCanvasAndPost(canvas);
				}
			}
			java.util.concurrent.Executor executor = mCommitExecutor;
			Runnable callback = mCommitCallback;
			mCommitExecutor = null;
			mCommitCallback = null;
			if (callback != null)
				executor.execute(callback);
			return result;
		}
	}

	/* AOSP HardwareRenderer.setLightSourceGeometry()/setLightSourceAlpha(): the argument checks; the light only feeds
	 * the native shadow drawing (nSetLightGeometry), which ATL does not do */
	public void setLightSourceGeometry(float lightX, float lightY, float lightZ, float lightRadius) {
		validateFinite(lightX, "lightX");
		validateFinite(lightY, "lightY");
		validatePositive(lightZ, "lightZ");
		validatePositive(lightRadius, "lightRadius");
	}

	public void setLightSourceAlpha(float ambientShadowAlpha, float spotShadowAlpha) {
		validateAlpha(ambientShadowAlpha, "ambientShadowAlpha");
		validateAlpha(spotShadowAlpha, "spotShadowAlpha");
	}

	private static void validateAlpha(float alpha, String argumentName) {
		if (!(alpha >= 0.0f && alpha <= 1.0f)) {
			throw new IllegalArgumentException(argumentName + " must be a valid alpha, "
			                                   + alpha + " is not in the range of 0.0f to 1.0f");
		}
	}

	private static void validatePositive(float f, String argumentName) {
		if (!(Float.isFinite(f) && f >= 0.0f)) {
			throw new IllegalArgumentException(argumentName
			                                   + " must be a finite positive, given=" + f);
		}
	}

	private static void validateFinite(float f, String argumentName) {
		if (!Float.isFinite(f)) {
			throw new IllegalArgumentException(argumentName + " must be finite, given=" + f);
		}
	}
}
