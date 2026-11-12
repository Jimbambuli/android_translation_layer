package android.view;

import java.io.Closeable;

/**
 * AOSP SurfaceControl (API 29): a layer of the compositor. ATL has none; a SurfaceControl is the surface of a
 * SurfaceView (getSurfaceControl()) or a child of one, and a Surface made from it draws into the SurfaceView's
 * widget. The layer properties of a Transaction (visibility, buffer size, parent) are not applied: the child
 * covers its SurfaceView, as a child layer that apps like GeckoView make for their content does.
 */
public final class SurfaceControl {
	private final SurfaceView view;
	private SurfaceControl parent;
	private final String name;
	private volatile boolean released;

	SurfaceControl(SurfaceView view, SurfaceControl parent, String name) {
		this.view = view;
		this.parent = parent;
		this.name = name;
	}

	/* the widget the surface of this layer draws into: that of its SurfaceView */
	long atl_widget() {
		if (view != null)
			return view.widget;
		return parent != null ? parent.atl_widget() : 0;
	}

	public boolean isValid() {
		return !released;
	}

	public void release() {
		released = true;
	}

	@Override
	public String toString() {
		return "SurfaceControl(" + name + ")";
	}

	public static class Builder {
		private SurfaceControl parent;
		private String name;

		public Builder() {}

		public Builder setParent(SurfaceControl parent) {
			this.parent = parent;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setBufferSize(int width, int height) {
			return this;
		}

		public Builder setFormat(int format) {
			return this;
		}

		public Builder setOpaque(boolean opaque) {
			return this;
		}

		public Builder setHidden(boolean hidden) {
			return this;
		}

		public SurfaceControl build() {
			return new SurfaceControl(null, parent, name);
		}
	}

	public static class Transaction implements Closeable {
		public Transaction() {}

		public Transaction setVisibility(SurfaceControl sc, boolean visible) {
			return this;
		}

		public Transaction setBufferSize(SurfaceControl sc, int w, int h) {
			return this;
		}

		public Transaction setPosition(SurfaceControl sc, float x, float y) {
			return this;
		}

		public Transaction setLayer(SurfaceControl sc, int z) {
			return this;
		}

		public Transaction setAlpha(SurfaceControl sc, float alpha) {
			return this;
		}

		/* a layer without parent is not shown; ATL's child draws into its SurfaceView, so it is only detached */
		public Transaction reparent(SurfaceControl sc, SurfaceControl newParent) {
			sc.parent = newParent;
			return this;
		}

		public Transaction merge(Transaction other) {
			return this;
		}

		public void apply() {}

		@Override
		public void close() {}
	}
}
