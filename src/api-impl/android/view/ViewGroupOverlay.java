package android.view;

public class ViewGroupOverlay extends ViewOverlay {
	ViewGroupOverlay(View hostView) {
		super(hostView);
	}

	public void add(View view) {
		if (view == null) {
			throw new IllegalArgumentException();
		}
	}

	public void remove(View view) {
		if (view == null) {
			throw new IllegalArgumentException();
		}
	}
}
