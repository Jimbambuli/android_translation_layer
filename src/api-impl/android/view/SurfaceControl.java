package android.view;

import java.io.Closeable;

/**
 * AOSP SurfaceControl (API 29): a layer of the compositor. ATL has none; a SurfaceControl is the surface of a
 * SurfaceView (getSurfaceControl()) or a child of one, and a Surface made from it draws into the SurfaceView's
 * widget. The layer properties of a Transaction (visibility, buffer size, parent) are not applied: the child
 * covers its SurfaceView, as a child layer that apps like GeckoView make for their content does.
 * Parceled, it travels as the endpoint of its SurfaceView's widget, like a Surface (Surface.Endpoint).
 */
public final class SurfaceControl implements android.os.Parcelable {
	private final SurfaceView view;
	private SurfaceControl parent;
	private final String name;
	private volatile boolean released;
	/* of a SurfaceControl read from a parcel: the Surface.Endpoint it was written as, or that of another process */
	private android.os.IBinder endpoint;

	SurfaceControl(SurfaceView view, SurfaceControl parent, String name) {
		this.view = view;
		this.parent = parent;
		this.name = name;
	}

	/* the widget the surface of this layer draws into: that of its SurfaceView */
	long atl_widget() {
		if (view != null)
			return view.widget;
		if (endpoint instanceof Surface.Endpoint)
			return ((Surface.Endpoint)endpoint).widget;
		return parent != null ? parent.atl_widget() : 0;
	}

	/* the endpoint in another process a SurfaceControl read from a parcel draws into, or null */
	android.os.IBinder atl_remoteEndpoint() {
		if (view != null || endpoint instanceof Surface.Endpoint)
			return null;
		if (endpoint != null)
			return endpoint;
		return parent != null ? parent.atl_remoteEndpoint() : null;
	}

	public static final android.os.Parcelable.Creator<SurfaceControl> CREATOR = new android.os.Parcelable.Creator<SurfaceControl>() {
		@Override
		public SurfaceControl createFromParcel(android.os.Parcel source) {
			SurfaceControl sc = new SurfaceControl(null, null, source.readString());
			sc.endpoint = source.readStrongBinder();
			return sc;
		}

		@Override
		public SurfaceControl[] newArray(int size) {
			return new SurfaceControl[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel dest, int flags) {
		dest.writeString(name);
		android.os.IBinder remote = atl_remoteEndpoint();
		long widget = atl_widget();
		dest.writeStrongBinder(remote != null ? remote : widget != 0 ? new Surface.Endpoint(widget, null) : null);
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

	/* AOSP SurfaceControl.JankData: SurfaceFlinger's classification of a frame of a surface */
	public static class JankData {
		public static final int JANK_NONE = 0;
		public static final int JANK_COMPOSER = 1 << 0;
		public static final int JANK_APPLICATION = 1 << 1;
		public static final int JANK_OTHER = 1 << 2;

		private final long mFrameVsyncId;
		private final int mJankType;
		private final long mFrameIntervalNs;
		private final long mScheduledAppFrameTimeNs;
		private final long mActualAppFrameTimeNs;

		public JankData(long frameVsyncId, int jankType, long frameIntervalNs, long scheduledAppFrameTimeNs,
				long actualAppFrameTimeNs) {
			mFrameVsyncId = frameVsyncId;
			mJankType = jankType;
			mFrameIntervalNs = frameIntervalNs;
			mScheduledAppFrameTimeNs = scheduledAppFrameTimeNs;
			mActualAppFrameTimeNs = actualAppFrameTimeNs;
		}

		public long getVsyncId() {
			return mFrameVsyncId;
		}

		public int getJankType() {
			return mJankType;
		}

		public long getFrameIntervalNanos() {
			return mFrameIntervalNs;
		}

		public long getScheduledAppFrameTimeNanos() {
			return mScheduledAppFrameTimeNs;
		}

		public long getActualAppFrameTimeNanos() {
			return mActualAppFrameTimeNs;
		}

		@Override
		public String toString() {
			return "JankData{vsync=" + mFrameVsyncId
					+ ", jankType=0x" + Integer.toHexString(mJankType)
					+ ", frameInterval=" + mFrameIntervalNs + "ns"
					+ ", scheduledAppTime=" + mScheduledAppFrameTimeNs + "ns"
					+ ", actualAppTime=" + mActualAppFrameTimeNs + "ns}";
		}
	}

	/* AOSP SurfaceControl.OnJankDataListener: called with the jank classification of a batch of frames */
	public interface OnJankDataListener {
		void onJankDataAvailable(java.util.List<JankData> jankData);
	}

	/* AOSP SurfaceControl.OnJankDataListenerRegistration: the handle of a registered listener. ATL's compositor
	 * delivers no jank data, so the only one is AOSP's NONE (the registration of a surface that is not valid) */
	public static class OnJankDataListenerRegistration {
		public static final OnJankDataListenerRegistration NONE = new OnJankDataListenerRegistration();

		private OnJankDataListenerRegistration() {}

		public void flush() {}

		public void removeAfter(long afterVsync) {}

		public void release() {}
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

		/* AOSP Transaction.setCrop(): the layer's crop; ATL composites no layers (see setPosition) */
		public Transaction setCrop(SurfaceControl sc, android.graphics.Rect crop) {
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
